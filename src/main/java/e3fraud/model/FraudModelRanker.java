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
import e3fraud.vocabulary.E3value;
import java.util.ArrayList;
import java.util.HashMap;
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
     * @param trustedActors the actor(s) whose loss to sort by
     * @param models the set of models to sort
     * @return a sorted list from highest to lowest loss for Actor
     */
    public static List<E3Model> sortByGainThenLoss(SwingWorker worker, E3Model baseModel, Set<E3Model> models, HashMap<String, Resource> trustedActors) {
        int total = models.size();
        int i = 0;

        List<E3Model> sortedList = new ArrayList<>();
        //For each model, find highest delta
        for (E3Model modelToPlace : models) {

            //If the list is empty
            if (sortedList.isEmpty()) {
                //add it as the first element
                sortedList.add(modelToPlace);

            } else {                
                Double largestGainInModelToPlace = computeTopGain(modelToPlace, baseModel, trustedActors).getResult();
                
                //For each model already in the sorted list     
                Iterator<E3Model> iterator = sortedList.listIterator();
                while (iterator.hasNext()) {
                    E3Model modelInList = iterator.next();
                    double largestGainModelInList = computeTopGain(modelInList, baseModel, trustedActors).getResult();

                    //If the top delta of the model to place is higher than the one in the list
                    if (largestGainInModelToPlace > largestGainModelInList) {
                        //add the model to place it right before the model in the list
                        sortedList.add(sortedList.indexOf(modelInList), modelToPlace);
                        break;
                    } //If two models have equal gain, 
                    else if (largestGainInModelToPlace == largestGainModelInList) {
                        //then sort by loss of main actor:
                        double largestLossInModelToPlace = computeTopLoss(modelToPlace, baseModel, trustedActors).getResult();
                        double largestLossInModelInList = computeTopLoss(modelInList, baseModel, trustedActors).getResult();

                        if (largestLossInModelToPlace < largestLossInModelInList) {
                            sortedList.add(sortedList.indexOf(modelInList), modelToPlace);
                            break;
                        } //if it is still equal, we need to sort by complexity
                        else if (largestGainInModelToPlace == largestGainModelInList) {
                            int modelToPlaceComplexity = modelToPlace.getFraudChanges().getComplexity();
                            int modelInListComplexity = modelInList.getFraudChanges().getComplexity();
                            if (modelToPlaceComplexity < modelInListComplexity) {
                                //and add it right before it
                                sortedList.add(sortedList.indexOf(modelInList), modelToPlace);
                                break;
                            }
                        }
                    }
                }
                //if it hasn't been added in the steps above, it is last
                if (!sortedList.contains(modelToPlace)) {
                    sortedList.add(modelToPlace);
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
     * @param trustedActor the actor whose loss to sort by
     * @param models the set of models to sort
     * @return a sorted list from highest to lowest loss for Actor
     */
    public static List<E3Model> sortByLossThenGain(SwingWorker worker, E3Model baseModel, Set<E3Model> models, HashMap<String, Resource> trustedActors) {
        int total = models.size();
        int i = 0;

        List<E3Model> sortedList = new ArrayList<>();
        //for each model
        for (E3Model modelToPlace : models) {            
            
            //if the list is empty
            if (sortedList.isEmpty()) {
                sortedList.add(modelToPlace);

            } else {                
                double largestLossInModelToPlace = computeTopLoss(modelToPlace, baseModel, trustedActors).getResult();                
                //for each model already in the sorted list
                Iterator<E3Model> iterator = sortedList.listIterator();
                while (iterator.hasNext()) {
                    E3Model modelInList = iterator.next();
                    double LargestLossInModelInList = computeTopLoss(modelInList, baseModel, trustedActors).getResult();

                    //when we find a bigger one                 
                    if (largestLossInModelToPlace < LargestLossInModelInList) {
                        //add it right before
                        sortedList.add(sortedList.indexOf(modelInList), modelToPlace);
                        break;
                    } //if it happens to be equal, we need to sort by delta of gain of other actors
                    else if (largestLossInModelToPlace == LargestLossInModelInList) {
                        //check if any actor has higher gain DELTA (!)
                        double gainDeltaToPlace = computeTopGain(modelToPlace, baseModel, trustedActors).getResult();
                        double gainDeltaInList = computeTopGain(modelInList, baseModel, trustedActors).getResult();

                        if (gainDeltaToPlace > gainDeltaInList) {
                            sortedList.add(sortedList.indexOf(modelInList), modelToPlace);
                            break;
                        } //if it is still equal, we need to sort by complexity
                        else if (gainDeltaToPlace == gainDeltaInList) {
                            int modelToPlaceComplexity = modelToPlace.getFraudChanges().getComplexity();
                            int modelInListComplexity = modelInList.getFraudChanges().getComplexity();
                            if (modelToPlaceComplexity < modelInListComplexity) {
                                //and add it right before it
                                sortedList.add(sortedList.indexOf(modelInList), modelToPlace);
                                break;
                            }
                        }
                    }
                }

                //otherwise add it last
                if (!sortedList.contains(modelToPlace)) {
                    sortedList.add(modelToPlace);
                }
            }

            if (worker != null) {
                worker.firePropertyChange("progress", 100 * (i - 1) / total, 100 * i / total);
                i++;
            }
        }
        return sortedList;
    }

public static ActorResult computeTopGain(E3Model fraudModel, E3Model valueModel, HashMap<String, Resource> trustedActors) {

        double largestGain = -Double.MAX_VALUE;
        Resource untrustedActorWithLargestGain = null;
        List<String> trustedActorURIs = new ArrayList<>(); //we use URIs to check actor identity across models
        for (Resource trustedActor : trustedActors.values()) {
            trustedActorURIs.add(trustedActor.getURI());
        }

        List<Long> colludedActors = fraudModel.getFraudChanges().colludedActors;

        //For each untrusted actor in the fraud model
        for (Resource actorInFraudModel : fraudModel.getActorsAndMarketSegments()) {
            if (!trustedActorURIs.contains(actorInFraudModel.getURI())) {

                String actorInFraudModelUID = actorInFraudModel.getProperty(E3value.e3_has_uid).getString();
                double delta = fraudModel.getTotalForActor(actorInFraudModel, false);

                //if the actor consists of one or more colluding actors
                if (colludedActors.contains(Long.parseLong(actorInFraudModelUID))) {
                    //deduct the base profit of all the actors involved in the collusion        
                    for (long colludedActorUID : colludedActors) {
                        Resource colludedActor = valueModel.getJenaModel().listResourcesWithProperty(E3value.e3_has_uid, String.valueOf(colludedActorUID)).next();
                        delta -= valueModel.getTotalForActor(colludedActor, true);
                    }

                //otherwise, deduct their base profit (if un-trusted) 
                } else {
                    Resource actor = valueModel.getJenaModel().listResourcesWithProperty(E3value.e3_has_uid, actorInFraudModelUID).next();
                    delta -= valueModel.getTotalForActor(actor, true);
                }

                if (delta > largestGain) {
                    largestGain = delta;
                    untrustedActorWithLargestGain = actorInFraudModel;
                }
            }
        }        
        return new ActorResult(untrustedActorWithLargestGain, largestGain);
    }

public static ActorResult computeTopLoss(E3Model fraudModel, E3Model valueModel, HashMap<String, Resource> trustedActors) {

        double largestLoss = Double.MAX_VALUE;
        Resource trustedActorWithLargestLoss = null;
        List<String> trustedActorURIs = new ArrayList<>(); //we use URIs to check actor identity across models
        for (Resource trustedActor : trustedActors.values()) {
            trustedActorURIs.add(trustedActor.getURI());
        }

        //For each trusted actor in the fraud model
        for (Resource actorInFraudModel : fraudModel.getActorsAndMarketSegments()) {
            if (trustedActorURIs.contains(actorInFraudModel.getURI())) {

                String actorInFraudModelUID = actorInFraudModel.getProperty(E3value.e3_has_uid).getString();                
                Resource actorInValueModel = valueModel.getJenaModel().listResourcesWithProperty(E3value.e3_has_uid, actorInFraudModelUID).next();
                
                double delta = valueModel.getTotalForActor(actorInValueModel, true) - fraudModel.getTotalForActor(actorInFraudModel, false);

                if (delta > largestLoss) {
                    largestLoss = delta;
                    trustedActorWithLargestLoss = actorInFraudModel;
                }
            }
        }
        return new ActorResult(trustedActorWithLargestLoss, largestLoss);
    }



public static class ActorResult{
    private final Resource actor;
    private final double result;

        public ActorResult(Resource actor, double result) {
            this.actor = actor;
            this.result = result;
        }

        public Resource getActor() {
            return actor;
        }


        public double getResult() {
            return result;
        }

    
}
}
