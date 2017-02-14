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
     * largest gain of any actor. In case two models are equal in terms of
     * gains, they are ranked by largest loss of any non-colluding actor. If
     * still equal, they are ranked by complexity.
     *
     * @param worker the worker thread running this
     * @param baseModel baseModel to compare gain to
     * @param models the set of models to sort
     * @return a sorted list from highest to lowest loss for Actor
     */
    public static List<E3Model> sortByTopGain(SwingWorker worker, E3Model baseModel, Set<E3Model> models) {
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
                Double largestGainInModelToPlace = computeTopGain(modelToPlace, baseModel).getResult();

                //For each model already in the sorted list     
                Iterator<E3Model> iterator = sortedList.listIterator();
                while (iterator.hasNext()) {
                    E3Model modelInList = iterator.next();
                    double largestGainModelInList = computeTopGain(modelInList, baseModel).getResult();

                    //If the top delta of the model to place is higher than the one in the list
                    if (largestGainInModelToPlace > largestGainModelInList) {
                        //add the model to place it right before the model in the list
                        sortedList.add(sortedList.indexOf(modelInList), modelToPlace);
                        break;
                    } //If two models have equal gain, 
                    else if (largestGainInModelToPlace == largestGainModelInList) {
                        //then sort by loss of main actor:
                        double largestLossInModelToPlace = computeTopLoss(modelToPlace, baseModel).getResult();
                        double largestLossInModelInList = computeTopLoss(modelInList, baseModel).getResult();

                        if (largestLossInModelToPlace > largestLossInModelInList) {
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
     * the highest loss encountered by any non-colluding actor. In case two
     * models are equal in terms of loss, they are ranked by largest gain of any
     * other actor compared to baseModel. If still equal, they are ranked by
     * complexity.
     *
     * @param worker
     * @param baseModel baseModel to compare gain to
     * @param models the set of models to sort
     * @return a sorted list from highest to lowest loss for Actor
     */
    public static List<E3Model> sortByTopLoss(SwingWorker worker, E3Model baseModel, Set<E3Model> models) {
        int total = models.size();
        int i = 0;

        List<E3Model> sortedList = new ArrayList<>();
        //for each model
        for (E3Model modelToPlace : models) {

            //if the list is empty
            if (sortedList.isEmpty()) {
                sortedList.add(modelToPlace);

            } else {
                double largestLossInModelToPlace = computeTopLoss(modelToPlace, baseModel).getResult();
                //for each model already in the sorted list
                Iterator<E3Model> iterator = sortedList.listIterator();
                while (iterator.hasNext()) {
                    E3Model modelInList = iterator.next();
                    double LargestLossInModelInList = computeTopLoss(modelInList, baseModel).getResult();

                    //when we find a bigger one                 
                    if (largestLossInModelToPlace > LargestLossInModelInList) {
                        //add it right before
                        sortedList.add(sortedList.indexOf(modelInList), modelToPlace);
                        break;
                    } //if it happens to be equal, we need to sort by delta of gain of other actors
                    else if (largestLossInModelToPlace == LargestLossInModelInList) {
                        //check if any actor has higher gain DELTA (!)
                        double gainDeltaToPlace = computeTopGain(modelToPlace, baseModel).getResult();
                        double gainDeltaInList = computeTopGain(modelInList, baseModel).getResult();

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

    /**
     * Transforms the Set of models into an ordered List of models, ranked by
     * the gain encountered a specific actor. In case two models are equal in
     * terms of gain, they are ranked by largest loss of any other actor
     * compared to baseModel. If still equal, they are ranked by complexity.
     *
     * @param worker
     * @param baseModel baseModel to compare gain to
     * @param models the set of models to sort
     * @param actor the actor whose loss to rank by
     * @return a sorted list from highest to lowest loss for Actor
     */
    public static List<E3Model> sortByGainOfActor(SwingWorker worker, E3Model baseModel, Set<E3Model> models, Resource actor) {
        int total = models.size();
        int i = 0;

        List<E3Model> sortedList = new ArrayList<>();
        //For each model, find highest gain
        for (E3Model modelToPlace : models) {

            //If the list is empty
            if (sortedList.isEmpty()) {
                //add it as the first element
                sortedList.add(modelToPlace);

            } else {
                Double gainInModelToPlace = computeGain(modelToPlace, baseModel, actor);

                //For each model already in the sorted list     
                Iterator<E3Model> iterator = sortedList.listIterator();
                while (iterator.hasNext()) {
                    E3Model modelInList = iterator.next();
                    double gainInModelInList = computeGain(modelInList, baseModel, actor);

                    //If the top delta of the model to place is higher than the one in the list
                    if (gainInModelToPlace > gainInModelInList) {
                        //add the model to place it right before the model in the list
                        sortedList.add(sortedList.indexOf(modelInList), modelToPlace);
                        break;
                    } //If two models have equal gain, 
                    else if (gainInModelToPlace == gainInModelInList) {
                        //then sort by largest loss:
                        double largestLossInModelToPlace = computeTopLoss(modelToPlace, baseModel).getResult();
                        double largestLossInModelInList = computeTopLoss(modelInList, baseModel).getResult();

                        if (largestLossInModelToPlace > largestLossInModelInList) {
                            sortedList.add(sortedList.indexOf(modelInList), modelToPlace);
                            break;
                        } //if it is still equal, we need to sort by complexity
                        else if (gainInModelToPlace == gainInModelInList) {
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
     * the highest loss encountered a specific actor. In case two models are
     * equal in terms of loss, they are ranked by largest gain of any other
     * actor compared to baseModel. If still equal, they are ranked by
     * complexity.
     *
     * @param worker
     * @param baseModel baseModel to compare gain to
     * @param fraudModels the set of models to sort
     * @param actor the actor whose loss to rank by
     * @return a sorted list from highest to lowest loss for Actor
     */
    public static List<E3Model> sortByLossOfActor(SwingWorker worker, E3Model baseModel, Set<E3Model> fraudModels, Resource actor) {

        int total = fraudModels.size();
        int i = 0;

        List<E3Model> sortedList = new ArrayList<>();
        //for each model
        for (E3Model modelToPlace : fraudModels) {

            //if the list is empty
            if (sortedList.isEmpty()) {
                sortedList.add(modelToPlace);

            } else {
                double lossInModelToPlace = computeLoss(modelToPlace, baseModel, actor);
                //for each model already in the sorted list
                Iterator<E3Model> iterator = sortedList.listIterator();
                while (iterator.hasNext()) {
                    E3Model modelInList = iterator.next();
                    double LossInModelInList = computeLoss(modelInList, baseModel, actor);

                    //when we find a bigger one                 
                    if (lossInModelToPlace > LossInModelInList) {
                        //add it right before
                        sortedList.add(sortedList.indexOf(modelInList), modelToPlace);
                        break;
                    } //if it happens to be equal, we need to sort by delta of gain of other actors
                    else if (lossInModelToPlace == LossInModelInList) {
                        //check if any actor has higher gain DELTA (!)
                        double gainDeltaToPlace = computeTopGain(modelToPlace, baseModel).getResult();
                        double gainDeltaInList = computeTopGain(modelInList, baseModel).getResult();

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

    public static ActorResult computeTopGain(E3Model fraudModel, E3Model valueModel) {

        double largestGain = -Double.MAX_VALUE;
        Resource untrustedActorWithLargestGain = null;

        List<Long> colludedActors = fraudModel.getFraudChanges().colludedActors;

        //For each actor in the fraud model
        for (Resource actorInFraudModel : fraudModel.getActorsAndMarketSegments()) {
            // if (!trustedActorURIs.contains(actorInFraudModel.getURI())) {

            String actorInFraudModelUID = actorInFraudModel.getProperty(E3value.e3_has_uid).getString();
            double gain = fraudModel.getTotalForActor(actorInFraudModel, false);

            //if the actor consists of one or more colluding actors
            if (colludedActors.contains(Long.parseLong(actorInFraudModelUID))) {
                //deduct the base profit of all the actors involved in the collusion        
                for (long colludedActorUID : colludedActors) {
                    Resource colludedActor = valueModel.getJenaModel().listResourcesWithProperty(E3value.e3_has_uid, String.valueOf(colludedActorUID)).next();
                    gain -= valueModel.getTotalForActor(colludedActor, true);
                }

                //otherwise, deduct their base profit (if un-trusted) 
            } else {
                Resource actor = valueModel.getJenaModel().listResourcesWithProperty(E3value.e3_has_uid, actorInFraudModelUID).next();
                gain -= valueModel.getTotalForActor(actor, true);
            }

            if (gain > largestGain) {
                largestGain = gain;
                untrustedActorWithLargestGain = actorInFraudModel;
            }
            // }
        }
        return new ActorResult(untrustedActorWithLargestGain, largestGain);
    }

    public static ActorResult computeTopLoss(E3Model fraudModel, E3Model valueModel) {
        double largestLoss = -Double.MAX_VALUE;
        Resource trustedActorWithLargestLoss = null;

        List<Long> colludedActors = fraudModel.getFraudChanges().colludedActors;

        //For each  actor in the fraud model
        for (Resource actorInFraudModel : fraudModel.getActorsAndMarketSegments()) {
            //if (trustedActorURIs.contains(actorInFraudModel.getURI())) {
            String actorInFraudModelUID = actorInFraudModel.getProperty(E3value.e3_has_uid).getString();
            //ignore colluded actors
            if (!colludedActors.contains(Long.parseLong(actorInFraudModelUID))) {
                Resource actorInValueModel = valueModel.getJenaModel().listResourcesWithProperty(E3value.e3_has_uid, actorInFraudModelUID).next();

                double loss = valueModel.getTotalForActor(actorInValueModel, true) - fraudModel.getTotalForActor(actorInFraudModel, false);

                if (loss > largestLoss) {
                    largestLoss = loss;
                    trustedActorWithLargestLoss = actorInFraudModel;
                }
            }
            //}
        }
        return new ActorResult(trustedActorWithLargestLoss, largestLoss);
    }

    public static Double computeLoss(E3Model fraudModel, E3Model valueModel, Resource actorFromValueModel) {
        String actorFromValueModelUID = actorFromValueModel.getProperty(E3value.e3_has_uid).getLiteral().toString();
        List<Long> colludingActors = fraudModel.getFraudChanges().colludedActors;
        Double loss = null;

        //if the actor was involved in collusion
        if (colludingActors.contains(Long.parseLong(actorFromValueModelUID))) {
            //add up the base profit of all the actors involved in the collusion  
            loss = 0.0;
            for (long colludingActorUID : colludingActors) {
                Resource colludingActor = valueModel.getJenaModel().listResourcesWithProperty(E3value.e3_has_uid, String.valueOf(colludingActorUID)).next();
                loss += valueModel.getTotalForActor(colludingActor, true);
            }
            //then deduct the base result        
            Resource colludedActor = fraudModel.getJenaModel().listResourcesWithProperty(E3value.e3_has_uid, String.valueOf(colludingActors.get(0))).next();
            loss -= fraudModel.getTotalForActor(colludedActor, false);
        } else {

            Resource actorInValueModel = valueModel.getJenaModel().listResourcesWithProperty(E3value.e3_has_uid, actorFromValueModelUID).next();
            loss = valueModel.getTotalForActor(actorFromValueModel, true) - fraudModel.getTotalForActor(actorInValueModel, false);
        }

        return loss;
    }

    public static Double computeGain(E3Model fraudModel, E3Model valueModel, Resource actorFromValueModel) {
        String actorFromValueModelUID = actorFromValueModel.getProperty(E3value.e3_has_uid).getLiteral().toString();
        List<Long> colludingActors = fraudModel.getFraudChanges().colludedActors;
        Double gain;

        //if the actor was involved in collusion
        if (colludingActors.contains(Long.parseLong(actorFromValueModelUID))) {
            Resource colludedActor = fraudModel.getJenaModel().listResourcesWithProperty(E3value.e3_has_uid, String.valueOf(colludingActors.get(0))).next();
            String colludedActorUID = colludedActor.getProperty(E3value.e3_has_uid).getLiteral().toString();
            Resource actorFromFraudModel = fraudModel.getJenaModel().listResourcesWithProperty(E3value.e3_has_uid, colludedActorUID).next();
            gain = fraudModel.getTotalForActor(actorFromFraudModel, false);
            //deduct the base profit of all the actors involved in the same collusion        
            for (long colludingActorUID : colludingActors) {
                Resource colludingActor = valueModel.getJenaModel().listResourcesWithProperty(E3value.e3_has_uid, String.valueOf(colludingActorUID)).next();
                gain -= valueModel.getTotalForActor(colludingActor, true);
            }

            //otherwise, deduct their base profit
        } else {
            Resource actorFromFraudModel = fraudModel.getJenaModel().listResourcesWithProperty(E3value.e3_has_uid, actorFromValueModelUID).next();
            gain = fraudModel.getTotalForActor(actorFromFraudModel, false )- valueModel.getTotalForActor(actorFromValueModel, true);
        }

        return gain;
    }

    public static class ActorResult {

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
