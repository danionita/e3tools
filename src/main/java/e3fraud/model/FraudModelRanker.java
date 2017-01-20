/*
 * Copyright (C) 2015, 2016 Dan Ionita 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package e3fraud.model;

import com.hp.hpl.jena.rdf.model.Resource;
import design.Utils.GraphDelta;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.SwingWorker;

/**
 *
 * @author Dan
 */
public class FraudModelRanker {

  

    /**
     * Transforms the Set of models into an ordered List of models, ranked by
     * largest gain of any other actor (except ToA) compared to baseModel
     *
     * @param worker the worker thread running this
     * @param baseModel baseModel to compare gain to
     * @param actor the actor whose loss to sort by
     * @param models the set of models to sort
     * @param need the need whose occurrence varies
     * @param startValue the minimum occurrence rate of the need
     * @param endValue the maximum occurrence rate of the need
     * @param ideal whether or not we should calculate this for the ideal case
     * or a sub-ideal case
     * @return a sorted list from highest to lowest loss for Actor
     */
    public static List<E3Model> sortByGainThenLoss(SwingWorker worker, E3Model baseModel, Set<E3Model> models, Resource actor, Resource need, int startValue, int endValue, boolean ideal) {
        int total = models.size();
        int i = 0;

        List<E3Model> sortedList = new ArrayList<>();
        //For each model, find highest delta
        for (E3Model modelToPlace : models) {
            //subIdealModel.enhance();
            //pre-compute averages and top gains
            modelToPlace.getAveragesForActors(need, startValue, endValue, false);
            computeTopGain(modelToPlace, baseModel, actor);            
            Double gainDeltaToPlace = modelToPlace.getLastKnownTopDelta();
            //Then,                 
            //If the list is empty
            if (sortedList.isEmpty()) {
                //add it directly
                //System.out.println("ADDED FIRST MODEL TO THE LIST");
                sortedList.add(modelToPlace);
                //otherwise
            } else {
                //For each model already in the sorted list     
                Iterator<E3Model> iterator = sortedList.listIterator();
                while (iterator.hasNext()) {
                    E3Model modelInList = iterator.next();
                    double gainDeltaInList = modelInList.getLastKnownTopDelta();
                    //If the top delta of the model to place is higher than the one in the list
                    if (gainDeltaToPlace > gainDeltaInList) {
                        //add the model to place it right before the model in the list
                        //System.out.println("Found lower gain. ADDED ANOTHER MODEL TO THE LIST");
                        sortedList.add(sortedList.indexOf(modelInList), modelToPlace);
                        break;
                    } //if two models have equal gain, 
                    else if (gainDeltaToPlace == gainDeltaInList) {
                        //then sort by loss of main actor:
                        double modelToPlaceMainActorAverage = modelToPlace.getLastKnownAverages().get(actor);
                        double modelInListMainActorAverage = modelInList.getLastKnownAverages().get(actor);
                        if (modelToPlaceMainActorAverage < modelInListMainActorAverage) {
                            sortedList.add(sortedList.indexOf(modelInList), modelToPlace);
                            break;
                        }
                        //if it is still equal, we need to sort by complexity
                        else if(gainDeltaToPlace==gainDeltaInList){
                            int modelToPlaceComplexity = modelToPlace.getFraudChanges().getComplexity();
                            int modelInListComplexity = modelInList.getFraudChanges().getComplexity();
                            if(modelToPlaceComplexity < modelInListComplexity){
                             //and add it right before it
                            //System.out.println("Found lower complexity. ADDED ANOTHER MODEL TO THE LIST");
                            sortedList.add(sortedList.indexOf(modelInList), modelToPlace);
                            break; 
                            }
                        }
                    }

                }
                //if it hasn't been added in the steps above, it is the smallest, so add it last
                if (!sortedList.contains(modelToPlace)) {
                    sortedList.add(modelToPlace);
                    //System.out.println("ADDED ANOTHER MODEL TO botton of the  LIST");   

                }
            }
            if (worker != null) {
                worker.firePropertyChange("progress", 100 * (i - 1) / total, 100 * i / total);
                i++;
            }
        }

        return sortedList;
    }

    /**
     * Transforms the Set of models into an ordered List of models, ranked by
     * average loss of given actor across a specified interval of occurrence of
     * a given need. In case two models are equal in terms of loss, they are
     * ranked by largest gain of any other actor compared to baseModel
     *
     * @param worker
     * @param baseModel baseModel to compare gain to
     * @param actor the actor whose loss to sort by
     * @param models the set of models to sort
     * @param need the need whose occurrence varies
     * @param startValue the minimum occurrence rate of the need
     * @param endValue the maximum occurrence rate of the need
     * @param ideal whether or not we should calculate this for the ideal case
     * or a sub-ideal case
     * @return a sorted list from highest to lowest loss for Actor
     */
    public static List<E3Model> sortByLossThenGain(SwingWorker worker, E3Model baseModel, Set<E3Model> models, Resource actor, Resource need, int startValue, int endValue, boolean ideal) {
        int total = models.size();
        int i = 0;

        List<E3Model> sortedList = new ArrayList<>();
        //for each model
        for (E3Model modelToPlace : models) {
            //subIdealModel.enhance();
            //pre-compute averages and top gains
            modelToPlace.getAveragesForActors(need, startValue, endValue, false);
            computeTopGain(modelToPlace, baseModel, actor);
            
            Map<Resource, Double> modelToPlaceAverages = modelToPlace.getLastKnownAverages();
            //if the list is empty
            if (sortedList.isEmpty()) {
                //add it directly
                //System.out.println("ADDED FIRST MODEL TO THE LIST");
                sortedList.add(modelToPlace);
                //otherwise
            } else {
                Iterator<E3Model> iterator = sortedList.listIterator();
                //for each model already in the sorted list
                while (iterator.hasNext()) {
                    //check it's average
                    E3Model modelInList = iterator.next();
                    Map<Resource, Double> modelInListAverages = modelInList.getLastKnownAverages();
                    //when we find a bigger one  (for selected actor)
                    //System.out.println("Comparing "+ modelToPlaceAverages.get(actor)+ " with "+ modeInListAverages.get(actor));
                    if (modelToPlaceAverages.get(actor) < modelInListAverages.get(actor)) {
                        //add it right before it
                        sortedList.add(sortedList.indexOf(modelInList), modelToPlace);
                        //System.out.println("Found bigger loss. ADDED ANOTHER MODEL TO THE LIST");    
                        break;
                    } //if it happens to be equal, we need to sort by delta of gain of other actors
                    else if (modelToPlaceAverages.get(actor).equals(modelInListAverages.get(actor))) {
                        //check if any actor has higher gain DELTA (!)
                        double gainDeltaToPlace = modelToPlace.getLastKnownTopDelta();
                        double gainDeltaInList = modelInList.getLastKnownTopDelta();
                        if (gainDeltaToPlace > gainDeltaInList) {
                            //and add it right before it
                            //System.out.println("Found lower gain. ADDED ANOTHER MODEL TO THE LIST");
                            sortedList.add(sortedList.indexOf(modelInList), modelToPlace);
                            break;
                        }
                        //if it is still equal, we need to sort by complexity
                        else if(gainDeltaToPlace==gainDeltaInList){
                            int modelToPlaceComplexity = modelToPlace.getFraudChanges().getComplexity();
                            int modelInListComplexity = modelInList.getFraudChanges().getComplexity();
                            if(modelToPlaceComplexity < modelInListComplexity){
                             //and add it right before it
                            //System.out.println("Found lower complexity. ADDED ANOTHER MODEL TO THE LIST");
                            sortedList.add(sortedList.indexOf(modelInList), modelToPlace);
                            break; 
                            }
                        }
                    }
                }
                //otherwise add it last
                if (!sortedList.contains(modelToPlace)) {
                    sortedList.add(modelToPlace);
                    //System.out.println("ADDED ANOTHER MODEL TO botton of the  LIST");   

                }
            }
            if (worker != null) {
                worker.firePropertyChange("progress", 100 * (i - 1) / total, 100 * i / total);
                i++;
            }
        }
        return sortedList;
    }
    
        
      private static void computeTopGain(E3Model subIdealModel, E3Model baseModel, Resource mainActor) {
        Map<Resource, Double> modelToPlaceAverages = subIdealModel.getLastKnownAverages();
        Map<Resource, Double> baseModelAverages = baseModel.getLastKnownAverages();
        //First, find the actor with the largest Delta gain in the model to place
        double highestDelta = -Double.MAX_VALUE;
        double averageIdealGainOfTopGainActor = -Double.MAX_VALUE;
        Resource highestDeltaActor = null;
        for (Resource actorInSubIdealModel : subIdealModel.getActorsAndMarketSegments()) {
            //If it is part of a colluded actor
            if (actorInSubIdealModel.getURI().equals(subIdealModel.newActorURI)) {
                Resource colludedActor = baseModel.getJenaModel().getResource(subIdealModel.colludedActorURI);
                //deduct the base profit of both actors                    
                double delta = modelToPlaceAverages.get(actorInSubIdealModel) - baseModelAverages.get(actorInSubIdealModel) - baseModelAverages.get(colludedActor);
                if (delta > highestDelta) {
                    highestDelta = delta;
                    highestDeltaActor = actorInSubIdealModel;
                    averageIdealGainOfTopGainActor = baseModelAverages.get(actorInSubIdealModel) + baseModelAverages.get(colludedActor);//this workaround is needed because when actors are colluded, we cannot query the baseModel for their ideal average
                }
            } else if (!actorInSubIdealModel.getURI().equals(mainActor.getURI())) {
                //otherwise, deduct the base profit
                double delta = modelToPlaceAverages.get(actorInSubIdealModel) - baseModelAverages.get(actorInSubIdealModel);
                if (delta > highestDelta) {
                    highestDelta = delta;
                    highestDeltaActor = actorInSubIdealModel;
                    averageIdealGainOfTopGainActor = baseModelAverages.get(actorInSubIdealModel);
                }
                
            }
        }
        subIdealModel.setLastKnownTopDelta(highestDelta);
        subIdealModel.setLastKnownIdealAverageForTopGainActor(averageIdealGainOfTopGainActor);
        subIdealModel.setTopDeltaActor(highestDeltaActor);
    }

}
