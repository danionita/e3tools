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
import e3fraud.model.FraudModelRanker.ActorResult;
import e3fraud.tools.SettingsObjects.FilteringSettings;
import e3fraud.tools.SettingsObjects.SortingAndGroupingSettings;
import e3fraud.vocabulary.E3value;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;

public class SortingAndFilteringWorker extends SwingWorker<ResultObject, String> {

    private static final boolean DEBUG = false;
    
    static private final String NEWLINE = "\n";
    
    private final E3Model baseModel;
    private final HashMap<String,Resource> trustedActors;
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
    public SortingAndFilteringWorker(java.util.HashMap<String, java.util.Set<E3Model>> groupedSubIdealModels, E3Model baseModel, HashMap<String,Resource> trustedActors, SortingAndGroupingSettings sortingAndGroupingSettings, FilteringSettings filteringSettings) {
        this.baseModel = baseModel;
        this.trustedActors = trustedActors;
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
                if (DEBUG) {
                    System.out.println(currentTime.currentTime() + " Ranking sub-ideal models " + NEWLINE + "\t based on the highest  \u0394gain of an untrusted actor..." + NEWLINE);
                    System.out.println(currentTime.currentTime() + " Only displaying models with " + gainMin + "<gain<" + gainMax + " and " + lossMin + "<loss<" + lossMax);
                }
                numberOfSubIdealModels = groupedSubIdealModels.size();
                for (Map.Entry<String, java.util.Set<E3Model>> cursor : groupedSubIdealModels.entrySet()) {
                    DefaultMutableTreeNode category = new DefaultMutableTreeNode(cursor.getKey());
                    root.add(category);
                    sortedSubIdealModels = FraudModelRanker.sortByGainThenLoss(this, baseModel, cursor.getValue(), trustedActors);
                    for (E3Model subIdealModel : sortedSubIdealModels) {
                        
                        ActorResult topGainResult = FraudModelRanker.computeTopGain(subIdealModel, baseModel, trustedActors);      
                        Resource topGainActor = topGainResult.getActor();
                        Double topGainValue = topGainResult.getResult();
                        ActorResult topLossResult = FraudModelRanker.computeTopGain(subIdealModel, baseModel, trustedActors);     
                        Resource topLossActor = topLossResult.getActor();
                        Double topLossValue = topLossResult.getResult();
                        
                        if (gainMin < topGainValue && topGainValue <= gainMax && lossMin < topLossValue && topLossValue <= lossMax) {
                            subIdealModel.setPrefix("Average gain of <b>"
                                    + df.format(topGainValue)
                                    + " </b> for "
                                    + topGainActor.getProperty(E3value.e3_has_name).getString()
                                    + " due to: <br>");
                            category.add(new DefaultMutableTreeNode(subIdealModel));
                        }
                    }
                    setProgress(100 * i / numberOfSubIdealModels);
                    i++;
                }
                
                
            } else if (sortCriteria == 0) {
                //sort by loss first, then gain
                if (DEBUG) {
                    System.out.println(currentTime.currentTime() + " Ranking each group " + NEWLINE + "\tbased the highest loss of any untrusted actor" + NEWLINE);
                    System.out.println(currentTime.currentTime() + " Only displaying models with " + gainMin + "<gain<" + gainMax + " and " + lossMin + "<loss<" + lossMax);
                }
                i = 0;
                numberOfSubIdealModels = groupedSubIdealModels.size();
                for (Map.Entry<String, java.util.Set<E3Model>> cursor : groupedSubIdealModels.entrySet()) {
                    DefaultMutableTreeNode category = new DefaultMutableTreeNode(cursor.getKey());
                    root.add(category);
                    sortedSubIdealModels = FraudModelRanker.sortByLossThenGain(this, baseModel, cursor.getValue(), trustedActors);
                    for (E3Model subIdealModel : sortedSubIdealModels) {
                        
                        ActorResult topGainResult = FraudModelRanker.computeTopGain(subIdealModel, baseModel, trustedActors);      
                        Resource topGainActor = topGainResult.getActor();
                        Double topGainValue = topGainResult.getResult();
                        ActorResult topLossResult = FraudModelRanker.computeTopGain(subIdealModel, baseModel, trustedActors);     
                        Resource topLossActor = topLossResult.getActor();
                        Double topLossValue = topLossResult.getResult();
                        
                        if (gainMin < topGainValue && topGainValue <= gainMax && lossMin < topLossValue && topLossValue <= lossMax) {
                            //reset old prefix    
                            subIdealModel.setPrefix("Average loss of <b>"
                                    + df.format(topLossValue)
                                    + " </b> for "
                                    + topLossActor.getProperty(E3value.e3_has_name).getString()
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
                if (DEBUG) {
                    System.out.println(currentTime.currentTime() + " Ranking sub-ideal models " + NEWLINE + "\tbased on the highest  \u0394gain of an untrusted actor...");
                    System.out.println(currentTime.currentTime() + " Only displaying models with " + gainMin + "<gain<" + gainMax + " and " + lossMin + "<loss<" + lossMax);
                }
                sortedSubIdealModels = FraudModelRanker.sortByGainThenLoss(this, baseModel, subIdealModels, trustedActors);
                Double lastTopGainValue = Double.MAX_VALUE;
                Double lastLoss = -Double.MAX_VALUE;
                DefaultMutableTreeNode lastNode = null;
                int similarModelCounter = 0;
                for (E3Model subIdealModel : sortedSubIdealModels) {
                        
                        ActorResult topGainResult = FraudModelRanker.computeTopGain(subIdealModel, baseModel, trustedActors);      
                        Resource topGainActor = topGainResult.getActor();
                        Double topGainValue = topGainResult.getResult();
                        ActorResult topLossResult = FraudModelRanker.computeTopGain(subIdealModel, baseModel, trustedActors);     
                        Resource topLossActor = topLossResult.getActor();
                        Double topLossValue = topLossResult.getResult();          

                    if (gainMin < topGainValue && topGainValue <= gainMax && lossMin < topLossValue && topLossValue <= lossMax) {
                        if (!(Objects.equals(topGainValue, lastTopGainValue) && Objects.equals(topLossValue, lastLoss))) {
                            subIdealModel.setPrefix("Gain of <b>"
                                    + df.format(topGainValue)
                                    + " </b> for "
                                    + topGainActor.getProperty(E3value.e3_has_name).getString()
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
                        lastTopGainValue = topGainValue;
                        lastLoss = topLossValue;
                    }
                    setProgress(100 * i / numberOfSubIdealModels);
                    i++;
                }
                
                
            } else if (sortCriteria == 0) {
                //by loss then gain
                if (DEBUG) {
                    System.out.println(currentTime.currentTime() + " Ranking sub-ideal models " + NEWLINE + "\tbased on the highest loss of any untrusted actor...");
                    System.out.println(currentTime.currentTime() + " Only displaying models with " + gainMin + "<gain<" + gainMax + " and " + lossMin + "<loss<" + lossMax);
                }
                sortedSubIdealModels = FraudModelRanker.sortByLossThenGain(this, baseModel, subIdealModels, trustedActors);
                Double lastTopGain = Double.MAX_VALUE;
                Double lastLoss = -Double.MAX_VALUE;
                DefaultMutableTreeNode lastNode = null;
                int similarModelCounter = 0;
                for (E3Model subIdealModel : sortedSubIdealModels) {
                    
                        ActorResult topGainResult = FraudModelRanker.computeTopGain(subIdealModel, baseModel, trustedActors);      
                        Resource topGainActor = topGainResult.getActor();
                        Double topGainValue = topGainResult.getResult();
                        ActorResult topLossResult = FraudModelRanker.computeTopGain(subIdealModel, baseModel, trustedActors);     
                        Resource topLossActor = topLossResult.getActor();
                        Double topLossValue = topLossResult.getResult();                  
                    
                    if (gainMin < topGainValue && topGainValue <= gainMax && lossMin < topLossValue && topLossValue <= lossMax) {
                        if (!(Objects.equals(topGainValue, lastTopGain) && Objects.equals(topLossValue, lastLoss))) {
                            subIdealModel.setPrefix("Loss of <b>"
                                    + df.format(topLossValue)
                                    + " </b> for "
                                    + topLossActor.getProperty(E3value.e3_has_name).getString()
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
                        lastTopGain = topGainValue;
                        lastLoss = topLossValue;
                    }
                    setProgress(100 * i / numberOfSubIdealModels);
                    i++;
                }
            }
        }
                
        //Count how many models were not filtered out
        int shownModels = 0;
        Enumeration e = root.breadthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            if (node.getUserObject() instanceof E3Model) {
                shownModels++;
            }
        }
        
        System.out.println(currentTime.currentTime() + " Sorting finished. Showing " + shownModels+ " out of " + numberOfSubIdealModels + " fraud models." + NEWLINE);
        return new ResultObject(numberOfSubIdealModels, shownModels, root);
    }

}
