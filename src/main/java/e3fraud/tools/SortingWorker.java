/*
 * Copyright (C) 2015, 2016 Dan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as System.out.printlned by
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
package e3fraud.tools;

/**
 *
 * @author Dan
 */
import com.hp.hpl.jena.rdf.model.Resource;
import e3fraud.gui.ResultObject;
import e3fraud.model.E3Model;
import e3fraud.model.FraudModelRanker;
import e3fraud.tools.SettingsObjects.FilteringSettings;
import e3fraud.tools.SettingsObjects.GenerationSettings;
import e3fraud.tools.SettingsObjects.SortingAndGroupingSettings;
import e3fraud.vocabulary.E3value;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class SortingWorker extends SwingWorker<ResultObject, String> {

    private static boolean debug = false;

    static private final String newline = "\n";
    private final E3Model baseModel;
    private final String selectedActorString;
    private final String selectedNeedString;
    private final Resource selectedActor;
    private final Resource selectedNeed;
    private final int startValue;
    private final int endValue;
    private final int sortCriteria;
    private final int groupingCriteria;
    private final Double lossMin, lossMax, gainMin, gainMax;
    private java.util.List<E3Model> sortedSubIdealModels;
    private final java.util.HashMap<String, java.util.Set<E3Model>> groupedSubIdealModels;
    private final Set<E3Model> subIdealModels;
    private final DefaultMutableTreeNode root;
    private int numberOfSubIdealModels;
    int i;

    /**
     *
     * @param groupedSubIdealModels the ranked and grouped sub-ideal models
     * @param baseModel the model to analyze
     * @param generationSettings
     * @param sortingAndGroupingSettings
     * @param filteringSettings
     *
     */
    public SortingWorker(java.util.HashMap<String, java.util.Set<E3Model>> groupedSubIdealModels, E3Model baseModel, GenerationSettings generationSettings, SortingAndGroupingSettings sortingAndGroupingSettings, FilteringSettings filteringSettings) {
        this.baseModel = baseModel;
        this.selectedActorString = generationSettings.getSelectedActorString();
        this.selectedActor = generationSettings.getSelectedActor();
        this.selectedNeed = generationSettings.getSelectedNeed();
        this.selectedNeedString = generationSettings.getSelectedNeedString();
        this.startValue = generationSettings.getStartValue();
        this.endValue = generationSettings.getEndValue();
        this.sortCriteria = sortingAndGroupingSettings.getSortCriteria();
        this.groupingCriteria = sortingAndGroupingSettings.getGroupingCriteria();
        this.lossMin = filteringSettings.getLossMin();
        this.lossMax = filteringSettings.getLossMax();
        this.gainMin = filteringSettings.getGainMin();
        this.gainMax = filteringSettings.getGainMax();
        this.sortedSubIdealModels = null;
        this.root = new DefaultMutableTreeNode("root");
        this.groupedSubIdealModels = groupedSubIdealModels;
        this.subIdealModels = new HashSet<>();
    }

    @Override
    protected ResultObject doInBackground() throws Exception {
        DecimalFormat df = new DecimalFormat("#.##");
        i = 0;
        //grouped case
        if (groupingCriteria == 1) {
            if (sortCriteria == 1) {
                //sort by gain only, then loss
                if (debug) {
                    System.out.println(currentTime.currentTime() + " Ranking each group " + newline + "\tbased on average \u0394gain of the any actor  in the model except \"" + selectedActorString + "\"" + newline + "\twhen \"" + selectedNeedString + "\" " + "\toccurs " + startValue + " to " + endValue + " times..." + newline);
                    System.out.println(currentTime.currentTime() + " Only displaying models with " + gainMin + "<gain<" + gainMax + " and " + lossMin + "<loss<" + lossMax);
                }
                numberOfSubIdealModels = groupedSubIdealModels.size();
                for (Map.Entry<String, java.util.Set<E3Model>> cursor : groupedSubIdealModels.entrySet()) {
                    DefaultMutableTreeNode category = new DefaultMutableTreeNode(cursor.getKey());
                    root.add(category);
                    sortedSubIdealModels = FraudModelRanker.sortByGainThenLoss(null, baseModel, cursor.getValue(), selectedActor, selectedNeed, startValue, endValue, false);
                    for (E3Model subIdealModel : sortedSubIdealModels) {
                        Resource topDeltaActor = subIdealModel.getTopDeltaActor();
                        Double topDelta = subIdealModel.getLastKnownTopDelta();
                        Double idealAverageForTopGainActor = subIdealModel.getLastKnownIdealAverageForTopGainActor();
                        Double subIdealAverageForTopGainActor = subIdealModel.getLastKnownAverages().get(topDeltaActor);
                        Double subIdealAverageForMainActor = subIdealModel.getLastKnownAverages().get(selectedActor);
                        Double idealAverageForMainActor = baseModel.getLastKnownAverages().get(selectedActor);
                        Double loss = idealAverageForMainActor - subIdealAverageForMainActor;
                        if (gainMin < topDelta && topDelta <= gainMax && lossMin < loss && loss <= lossMax) {
                            subIdealModel.setPrefix(
                                    "Average gain of <b>"
                                    + df.format(topDelta)
                                    + " </b> for "
                                    + topDeltaActor.getProperty(E3value.e3_has_name).getString()
                                    + " ("
                                    + df.format(subIdealAverageForTopGainActor)
                                    + " instead of "
                                    + df.format(idealAverageForTopGainActor)
                                    + ") due to: <br>");
                            category.add(new DefaultMutableTreeNode(subIdealModel));
                        }
                    }
                    setProgress(100 * i / numberOfSubIdealModels);
                    i++;
                }
            } else if (sortCriteria == 0) {
                //sort by loss first, then gain
                if (debug) {
                    System.out.println(currentTime.currentTime() + " Ranking each group " + newline + "\tbased on average loss for \"" + selectedActorString + "\"" + newline + "\t and on average \u0394gain of the other actors in the model " + newline + "\twhen \"" + selectedNeedString + "\" " + "\toccurs " + startValue + " to " + endValue + " times..." + newline);
                    System.out.println(currentTime.currentTime() + " Only displaying models with " + gainMin + "<gain<" + gainMax + " and " + lossMin + "<loss<" + lossMax);
                }
                i = 0;
                numberOfSubIdealModels = groupedSubIdealModels.size();
                for (Map.Entry<String, java.util.Set<E3Model>> cursor : groupedSubIdealModels.entrySet()) {
                    DefaultMutableTreeNode category = new DefaultMutableTreeNode(cursor.getKey());
                    root.add(category);
                    sortedSubIdealModels = FraudModelRanker.sortByLossThenGain(null, baseModel, cursor.getValue(), selectedActor, selectedNeed, startValue, endValue, false);
                    for (E3Model subIdealModel : sortedSubIdealModels) {
                        Double topDelta = subIdealModel.getLastKnownTopDelta();
                        Double subIdealAverageForMainActor = subIdealModel.getLastKnownAverages().get(selectedActor);
                        Double idealAverageForMainActor = baseModel.getLastKnownAverages().get(selectedActor);
                        Double loss = idealAverageForMainActor - subIdealAverageForMainActor;
                        if (gainMin < topDelta && topDelta <= gainMax && lossMin < loss && loss <= lossMax) {
                            //reset old prefix    
                            subIdealModel.setPrefix(
                                    "Average loss of <b>"
                                    + df.format(loss)
                                    + " </b>("
                                    + df.format(subIdealAverageForMainActor)
                                    + " instead of "
                                    + df.format(idealAverageForMainActor)
                                    + ") for "
                                    + selectedActor.getProperty(E3value.e3_has_name).getString()
                                    //+ " = " + subIdealModel.getLastKnownAverages().get(selectedActor)
                                    + " due to: <br>");
                            category.add(new DefaultMutableTreeNode(subIdealModel));
                        }
                    }
                    setProgress(100 * i / numberOfSubIdealModels);
                    i++;
                }
            }

            //ungrouped case
        } else if (groupingCriteria == 0) {
            //ungroup
            for (Set<E3Model> subSetOfSubIdealModels : groupedSubIdealModels.values()) {
                subIdealModels.addAll(subSetOfSubIdealModels);
            }
            numberOfSubIdealModels = subIdealModels.size();
            //then rank
            if (sortCriteria == 1) {
                //by gain then loss
                if (debug) {
                    System.out.println(currentTime.currentTime() + " Ranking sub-ideal models " + newline + "\tbased on average \u0394gain of the any actor  in the model except \"" + selectedActorString + "...");
                    System.out.println(currentTime.currentTime() + " Only displaying models with " + gainMin + "<gain<" + gainMax + " and " + lossMin + "<loss<" + lossMax);
                }
                sortedSubIdealModels = FraudModelRanker.sortByGainThenLoss(this, baseModel, subIdealModels, selectedActor, selectedNeed, startValue, endValue, false);
                Double lastTopDelta = Double.MAX_VALUE;
                Double lastLoss = -Double.MAX_VALUE;
                DefaultMutableTreeNode lastNode = null;
                int similarModelCounter = 0;
                for (E3Model subIdealModel : sortedSubIdealModels) {
                    Resource topDeltaActor = subIdealModel.getTopDeltaActor();
                    Double topDelta = subIdealModel.getLastKnownTopDelta();
                    Double idealAverageForTopGainActor = subIdealModel.getLastKnownIdealAverageForTopGainActor();
                    Double subIdealAverageForTopGainActor = subIdealModel.getLastKnownAverages().get(topDeltaActor);
                    Double subIdealAverageForMainActor = subIdealModel.getLastKnownAverages().get(selectedActor);
                    Double idealAverageForMainActor = baseModel.getLastKnownAverages().get(selectedActor);
                    Double loss = idealAverageForMainActor - subIdealAverageForMainActor;

                    if (gainMin < topDelta && topDelta <= gainMax && lossMin < loss && loss <= lossMax) {
                        if (!(Objects.equals(topDelta, lastTopDelta) && Objects.equals(loss, lastLoss))) {
                            subIdealModel.setPrefix("Average gain of <b>"
                                    + df.format(topDelta)
                                    + " </b> for "
                                    + topDeltaActor.getProperty(E3value.e3_has_name).getString()
                                    + " ("
                                    + df.format(subIdealAverageForTopGainActor)
                                    + " instead of "
                                    + df.format(idealAverageForTopGainActor)
                                    + ") due to: <br>");
                            lastNode = new DefaultMutableTreeNode(subIdealModel);
                            root.add(lastNode);
                        } else if (lastNode.isLeaf()) {                            
                            similarModelCounter=1;
                            DefaultMutableTreeNode similarModelsNode = new DefaultMutableTreeNode("1 other model with similar results");
                            lastNode.add(similarModelsNode);
                            similarModelsNode.add(new DefaultMutableTreeNode(subIdealModel));
                        } else {
                            DefaultMutableTreeNode similarModelsNode = (DefaultMutableTreeNode) lastNode.getChildAt(0);
                            similarModelsNode.add(new DefaultMutableTreeNode(subIdealModel));
                            similarModelCounter++;
                            similarModelsNode.setUserObject(similarModelCounter + " other models with similar results (ordered by complexity)");
                        }
                        lastTopDelta = topDelta;
                        lastLoss = loss;
                    }
                    setProgress(100 * i / numberOfSubIdealModels);
                    i++;
                }
            } else if (sortCriteria == 0) {
                //by loss then gain
                if (debug) {
                    System.out.println(currentTime.currentTime() + " Ranking sub-ideal models " + newline + "\tbased on average loss for \"" + selectedActorString + "\"" + newline + "\t and on average \u0394gain of the other actors in the model...");
                    System.out.println(currentTime.currentTime() + " Only displaying models with " + gainMin + "<gain<" + gainMax + " and " + lossMin + "<loss<" + lossMax);
                }
                sortedSubIdealModels = FraudModelRanker.sortByLossThenGain(this, baseModel, subIdealModels, selectedActor, selectedNeed, startValue, endValue, false);
                Double lastTopDelta = Double.MAX_VALUE;
                Double lastLoss = -Double.MAX_VALUE;
                DefaultMutableTreeNode lastNode = null;
                int similarModelCounter = 0;
                for (E3Model subIdealModel : sortedSubIdealModels) {
                    Double topDelta = subIdealModel.getLastKnownTopDelta();
                    Double subIdealAverageForMainActor = subIdealModel.getLastKnownAverages().get(selectedActor);
                    Double idealAverageForMainActor = baseModel.getLastKnownAverages().get(selectedActor);
                    Double loss = idealAverageForMainActor - subIdealAverageForMainActor;
                    
                    if (gainMin < topDelta && topDelta <= gainMax && lossMin < loss && loss <= lossMax) {
                        if (!(Objects.equals(topDelta, lastTopDelta) && Objects.equals(loss, lastLoss))) {
                            subIdealModel.setPrefix(
                                    "Average loss of <b>"
                                    + df.format(loss)
                                    + " </b>("
                                    + df.format(subIdealAverageForMainActor)
                                    + " instead of "
                                    + df.format(idealAverageForMainActor)
                                    + ") for "
                                    + selectedActor.getProperty(E3value.e3_has_name).getString()
                                    + " due to: <br>");
                            lastNode = new DefaultMutableTreeNode(subIdealModel);
                            root.add(lastNode);
                        } else if (lastNode.isLeaf()) {
                            similarModelCounter=1;
                            DefaultMutableTreeNode similarModelsNode = new DefaultMutableTreeNode("1 other model with similar results");
                            lastNode.add(similarModelsNode);
                            similarModelsNode.add(new DefaultMutableTreeNode(subIdealModel));
                        } else {
                            DefaultMutableTreeNode similarModelsNode = (DefaultMutableTreeNode) lastNode.getChildAt(0);
                            similarModelsNode.add(new DefaultMutableTreeNode(subIdealModel));
                            similarModelCounter++;
                            similarModelsNode.setUserObject(similarModelCounter + " other models with similar results (ordered by complexity)");
                        }
                        lastTopDelta = topDelta;
                        lastLoss = loss;
                        lastTopDelta = topDelta;
                        lastLoss = loss;
                    }
                    setProgress(100 * i / numberOfSubIdealModels);
                    i++;
                }
            }
        }
        int shownModels = 0;
        Enumeration e = root.breadthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            if (node.getUserObject() instanceof E3Model) {
                shownModels++;
            }
        }

        //ranking done
        return new ResultObject(numberOfSubIdealModels, shownModels, root);
    }

}
