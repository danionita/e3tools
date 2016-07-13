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
package e3fraud.gui;

/**
 *
 * @author Dan
 */
import com.hp.hpl.jena.rdf.model.Resource;
import e3fraud.model.E3Model;
import e3fraud.model.ModelRanker;
import e3fraud.tools.currentTime;
import e3fraud.vocabulary.E3value;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;

public class SortingWorker extends SwingWorker<DefaultMutableTreeNode, String> {

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
     * @param selectedActorString the main actor's name
     * @param selectedActor the main actor's RDF resource
     * @param selectedNeed the selected need's RDF resource
     * @param selectedNeedString the selected need's name
     * @param startValue the min occurrence rate of need
     * @param endValue the max occurrence rate of need
     * @param sortCriteria 0 - do not sort, 1 - sort by loss first, 2- sort by
     * gain first
     * @param groupingCriteria 0 - do not group, 1 - group based on generated
     * collusion groups
     * @param lossMin Filter by loss - minimum value
     * @param lossMax Filter by loss - maximum value
     * @param gainMin Filter by gain - minimum value
     * @param gainMax Filter by gain - maximum value
     */
    public SortingWorker(java.util.HashMap<String, java.util.Set<E3Model>> groupedSubIdealModels, E3Model baseModel, String selectedActorString, Resource selectedActor, Resource selectedNeed, String selectedNeedString, int startValue, int endValue, int sortCriteria, int groupingCriteria, Double lossMin, Double lossMax, Double gainMin, Double gainMax) {
        this.baseModel = baseModel;
        this.selectedActorString = selectedActorString;
        this.selectedActor = selectedActor;
        this.selectedNeed = selectedNeed;
        this.selectedNeedString = selectedNeedString;
        this.startValue = startValue;
        this.endValue = endValue;
        this.sortCriteria = sortCriteria;
        this.groupingCriteria = groupingCriteria;
        this.lossMin = lossMin;
        this.lossMax = lossMax;
        this.gainMin = gainMin;
        this.gainMax = gainMax;
        this.sortedSubIdealModels = null;
        this.root = new DefaultMutableTreeNode("root");
        this.groupedSubIdealModels = groupedSubIdealModels;
        this.subIdealModels = new HashSet<>();
    }

    @Override
    protected DefaultMutableTreeNode doInBackground() throws Exception {
        DecimalFormat df = new DecimalFormat("#.##");

        //grouped case
        if (groupingCriteria == 1) {
            if (sortCriteria == 1) {
                //sort by gain only, then loss
                System.out.println(currentTime.currentTime() + " Ranking each group " + newline + "\tbased on average \u0394gain of the any actor  in the model except \"" + selectedActorString + "\"" + newline + "\twhen \"" + selectedNeedString + "\" " + "\toccurs " + startValue + " to " + endValue + " times..." + newline);
                i = 0;
                numberOfSubIdealModels = groupedSubIdealModels.size();
                for (Map.Entry<String, java.util.Set<E3Model>> cursor : groupedSubIdealModels.entrySet()) {
                    DefaultMutableTreeNode category = new DefaultMutableTreeNode(cursor.getKey());
                    root.add(category);
                    sortedSubIdealModels = ModelRanker.sortByGainThenLoss(null, baseModel, cursor.getValue(), selectedActor, selectedNeed, startValue, endValue, false);
                    for (E3Model subIdealModel : sortedSubIdealModels) {
                        Resource topGainActor = subIdealModel.getTopDeltaActor();
                        Double topGain = subIdealModel.getLastKnownTopDelta();
                        Double idealAverageForTopGainActor = baseModel.getLastKnownAverages().get(topGainActor);
                        Double subAverageForTopGainActor = subIdealModel.getLastKnownAverages().get(topGainActor);
                        Double subIdealAverageForMainActor = subIdealModel.getLastKnownAverages().get(selectedActor);
                        Double idealAverageForMainActor = baseModel.getLastKnownAverages().get(selectedActor);
                        Double loss = idealAverageForMainActor - subIdealAverageForMainActor;
                        if (gainMin < topGain || topGain < gainMax || lossMin < loss || loss < lossMax) {
                            subIdealModel.setPrefix(
                                    "Average gain of <b>"
                                    + df.format(topGain)
                                    + " </b>("
                                    + df.format(subAverageForTopGainActor)
                                    + "instead of "
                                    + df.format(idealAverageForTopGainActor)
                                    + ") for "
                                    + topGainActor.getProperty(E3value.e3_has_name).getString()
                                    //+ " = " + subIdealModel.getLastKnownAverages().get(selectedActor)
                                    + " due to: <br>");
                            category.add(new DefaultMutableTreeNode(subIdealModel));
                        }
                    }
                    setProgress(100 * i / numberOfSubIdealModels);
                    i++;
                }
            } else if (sortCriteria == 0) {
                //sort by loss first, then gain
                System.out.println(currentTime.currentTime() + " Ranking each group " + newline + "\tbased on average loss for \"" + selectedActorString + "\"" + newline + "\t and on average \u0394gain of the other actors in the model " + newline + "\twhen \"" + selectedNeedString + "\" " + "\toccurs " + startValue + " to " + endValue + " times..." + newline);
                i = 0;
                numberOfSubIdealModels = groupedSubIdealModels.size();
                for (Map.Entry<String, java.util.Set<E3Model>> cursor : groupedSubIdealModels.entrySet()) {
                    DefaultMutableTreeNode category = new DefaultMutableTreeNode(cursor.getKey());
                    root.add(category);
                    sortedSubIdealModels = ModelRanker.sortByLossThenGain(null, baseModel, cursor.getValue(), selectedActor, selectedNeed, startValue, endValue, false);
                    for (E3Model subIdealModel : sortedSubIdealModels) {
                        Resource topGainActor = subIdealModel.getTopDeltaActor();
                        Double topGain = subIdealModel.getLastKnownTopDelta();
                        Double idealAverageForTopGainActor = baseModel.getLastKnownAverages().get(topGainActor);
                        Double subAverageForTopGainActor = subIdealModel.getLastKnownAverages().get(topGainActor);
                        Double subIdealAverageForMainActor = subIdealModel.getLastKnownAverages().get(selectedActor);
                        Double idealAverageForMainActor = baseModel.getLastKnownAverages().get(selectedActor);
                        Double loss = idealAverageForMainActor - subIdealAverageForMainActor;
                        if (gainMin < topGain || topGain < gainMax || lossMin < loss || loss < lossMax) {
                            //reset old prefix    
                            subIdealModel.setPrefix(
                                    "Average loss of <b>"
                                    + df.format(baseModel.getLastKnownAverages().get(selectedActor) - (subIdealModel.getLastKnownAverages().get(selectedActor)))
                                    + " </b>("
                                    + df.format(subIdealModel.getLastKnownAverages().get(selectedActor))
                                    + " instead of "
                                    + df.format(baseModel.getLastKnownAverages().get(selectedActor))
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
            //then rank
            if (sortCriteria == 1) {
                //by gain then loss
                System.out.println(currentTime.currentTime() + " Ranking sub-ideal models " + newline + "\tbased on average \u0394gain of the any actor  in the model except \"" + selectedActorString + "...");
                sortedSubIdealModels = ModelRanker.sortByGainThenLoss(this, baseModel, subIdealModels, selectedActor, selectedNeed, startValue, endValue, false);
                for (E3Model subIdealModel : sortedSubIdealModels) {
                    Resource topGainActor = subIdealModel.getTopDeltaActor();
                    Double topGain = subIdealModel.getLastKnownTopDelta();
                    Double idealAverageForTopGainActor = baseModel.getLastKnownAverages().get(topGainActor);
                    Double subIdealAverageForTopGainActor = subIdealModel.getLastKnownAverages().get(topGainActor);
                    Double subIdealAverageForMainActor = subIdealModel.getLastKnownAverages().get(selectedActor);
                    Double idealAverageForMainActor = baseModel.getLastKnownAverages().get(selectedActor);
                    Double loss = idealAverageForMainActor - subIdealAverageForMainActor;
                    if (gainMin < topGain || topGain < gainMax || lossMin < loss || loss < lossMax) {
                        subIdealModel.setPrefix("Average gain of <b>"
                                + df.format(topGain)
                                + " </b>("
                                + df.format(subIdealAverageForTopGainActor)
                                + "instead of "
                                + df.format(idealAverageForTopGainActor)
                                + ") for "
                                + topGainActor.getProperty(E3value.e3_has_name).getString()
                                //+ " = " + subIdealModel.getLastKnownAverages().get(selectedActor)
                                + " due to: <br>");
                        root.add(new DefaultMutableTreeNode(subIdealModel));
                    }
                }
            } else if (sortCriteria == 0) {
                //by loss then gain
                System.out.println(currentTime.currentTime() + " Ranking sub-ideal models " + newline + "\tbased on average loss for \"" + selectedActorString + "\"" + newline + "\t and on average \u0394gain of the other actors in the model...");
                sortedSubIdealModels = ModelRanker.sortByLossThenGain(this, baseModel, subIdealModels, selectedActor, selectedNeed, startValue, endValue, false);
                for (E3Model subIdealModel : sortedSubIdealModels) {
                    Resource topGainActor = subIdealModel.getTopDeltaActor();
                    Double topGain = subIdealModel.getLastKnownTopDelta();
                    Double idealAverageForTopGainActor = baseModel.getLastKnownAverages().get(topGainActor);
                    Double subAverageForTopGainActor = subIdealModel.getLastKnownAverages().get(topGainActor);
                    Double subIdealAverageForMainActor = subIdealModel.getLastKnownAverages().get(selectedActor);
                    Double idealAverageForMainActor = baseModel.getLastKnownAverages().get(selectedActor);
                    Double loss = idealAverageForMainActor - subIdealAverageForMainActor;
                    if (gainMin < topGain || topGain < gainMax || lossMin < loss || loss < lossMax) {
                        subIdealModel.setPrefix(
                                "Average loss of <b>"
                                + df.format(baseModel.getLastKnownAverages().get(selectedActor) - (subIdealModel.getLastKnownAverages().get(selectedActor)))
                                + " </b>("
                                + df.format(subIdealModel.getLastKnownAverages().get(selectedActor))
                                + " instead of "
                                + df.format(baseModel.getLastKnownAverages().get(selectedActor))
                                + ") for "
                                + selectedActor.getProperty(E3value.e3_has_name).getString()
                                //+ " = " + subIdealModel.getLastKnownAverages().get(selectedActor)
                                + " due to: <br>");
                        root.add(new DefaultMutableTreeNode(subIdealModel));
                    }
                }
            }
        }

        //ranking done
        return root;
    }

//    @Override
//    protected void process(List<String> chunks) {
//        for (final String string : chunks) {
//            log.append(string);
//            log.append("\n");
//        }
//    }
}
