/*
 * Copyright (C) 2015, 2016 Dan
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
package e3fraud.gui;

/**
 *
 * @author Dan
 */
import com.hp.hpl.jena.rdf.model.Resource;
import e3fraud.model.E3Model;
import e3fraud.model.ModelRanker;
import e3fraud.model.SubIdealModelGenerator;
import e3fraud.tools.currentTime;
import e3fraud.vocabulary.E3value;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextArea;

import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;

public class GenerationWorker extends SwingWorker<DefaultMutableTreeNode, String> {

    static private final String newline = "\n";
    private final E3Model baseModel;
    private final String selectedActorString;
    private final String selectedNeedString;
    private final Resource selectedActor;
    private final Resource selectedNeed;
    private final int startValue;
    private final int endValue;
    private final int sortCriteria;
    private final int collusions;
    private final int groupingCriteria;
    private java.util.List<E3Model> sortedSubIdealModels;
    private final java.util.HashMap<String, java.util.Set<E3Model>> groupedSubIdealModels;
    private final DefaultMutableTreeNode root;
    private int numberOfSubIdealModels;
    int i;
    /**
     * 
     * @param baseModel the model to analyze
     * @param selectedActorString the main actor's name
     * @param selectedActor the main actor's RDF resource
     * @param selectedNeed the selected need's RDF resource
     * @param selectedNeedString the selected need's name
     * @param startValue the min occurrence rate of need
     * @param endValue the max occurrence rate of need
     * @param sortCriteria 0 - do not sort, 1 - sort by loss first, 2- sort by gain first
     * @param groupingCriteria 0 - do not group, 1 - group based on generated collusion groups
     * @param collusions maximum number of actors which can be part of a single colluded actor
     */
    public GenerationWorker(E3Model baseModel, String selectedActorString, Resource selectedActor, Resource selectedNeed, String selectedNeedString, int startValue, int endValue, int sortCriteria, int groupingCriteria, int collusions) {
        this.baseModel = baseModel;
        this.selectedActorString = selectedActorString;
        this.selectedActor = selectedActor;
        this.selectedNeed = selectedNeed;
        this.selectedNeedString = selectedNeedString;
        this.startValue = startValue;
        this.endValue = endValue;
        this.collusions = collusions;
        this.sortCriteria = sortCriteria;
        this.groupingCriteria = groupingCriteria;
        this.sortedSubIdealModels = null;
        this.root = new DefaultMutableTreeNode("root");
        this.groupedSubIdealModels = new HashMap<>();      
        
    }

    @Override
    protected DefaultMutableTreeNode doInBackground() throws Exception {
DecimalFormat df = new DecimalFormat("#.##");             
    // Start generation
        System.out.println(currentTime.currentTime() + " Generating sub-ideal models...." + newline);
        SubIdealModelGenerator subIdealModelGenerator = new SubIdealModelGenerator();

        //grouped case
        if (groupingCriteria==1) {
            int size = 0;

            //generate colluded models            
            Set<E3Model> colludedAndNonColludedModels = subIdealModelGenerator.generateCollusions(baseModel, selectedActor, collusions);
            //use base model as a basis for the non-collusion group
            colludedAndNonColludedModels.add(baseModel);

            //for each type of collusion
            for (E3Model model : colludedAndNonColludedModels) {
                Set<E3Model> hiddenAndNonOccuringModels = new HashSet<>();
                Set<E3Model> intermediaryModels = new HashSet<>();
                Set<E3Model> subIdealModels = new HashSet<>();
                String category;
                //create a category for it
                if (model.getDescription().equals("Base Model")) {
                    category = "No collusion";
                } else {
                    category = model.getDescription().substring(25);
                    //and add the colluded models to the result 
                    subIdealModels.add(model);
                }
                //then generate
                intermediaryModels.addAll(subIdealModelGenerator.generateNonoccurringTransactions(model));
                subIdealModels.addAll(intermediaryModels);
                intermediaryModels.add(model);
                int i = 1;
                for (E3Model intermediaryModel : intermediaryModels) {
                    subIdealModels.addAll(subIdealModelGenerator.generateHiddenTransactions(intermediaryModel, selectedActor));
                }
                size += subIdealModels.size();
                System.out.println("\nGenerated " + subIdealModels.size() + " sub-ideal models for category " + category + ":");
                groupedSubIdealModels.put(category, subIdealModels);
            }
            // generation done
            System.out.println(currentTime.currentTime() + " Generated : " + size + " sub-ideal models (" + colludedAndNonColludedModels.size() + " groups)!" + newline);

            //now rank
                 if (sortCriteria==2) {
               //sort by gain only
                firePropertyChange("phase", "whatever","ranking...");
                System.out.println(currentTime.currentTime() + " Ranking each group " + newline + "\tbased on average \u0394gain of the any actor  in the model except \"" + selectedActorString + "\"" + newline + "\twhen \"" + selectedNeedString + "\" " + "\toccurs " + startValue + " to " + endValue + " times..." + newline);
                i=0;
                numberOfSubIdealModels = groupedSubIdealModels.size();
                for (Map.Entry<String, java.util.Set<E3Model>> cursor : groupedSubIdealModels.entrySet()) {
                    DefaultMutableTreeNode category = new DefaultMutableTreeNode(cursor.getKey());
                    root.add(category);
                    sortedSubIdealModels = ModelRanker.sortByGain(null, baseModel, cursor.getValue(), selectedActor, selectedNeed, startValue, endValue, false);
                    for (E3Model subIdealModel : sortedSubIdealModels) {
                        category.add(new DefaultMutableTreeNode(subIdealModel));
                    }                    
                    setProgress(100*i/numberOfSubIdealModels);
                    i++;
                }
            } else if (sortCriteria==1) {
                //sort by loss first
                firePropertyChange("phase", "whatever","ranking...");
                System.out.println(currentTime.currentTime() + " Ranking each group " + newline + "\tbased on average loss for \"" + selectedActorString + "\"" + newline + "\t and on average \u0394gain of the other actors in the model " + newline + "\twhen \"" + selectedNeedString + "\" " + "\toccurs " + startValue + " to " + endValue + " times..." + newline);
                                i=0;
                numberOfSubIdealModels = groupedSubIdealModels.size();
                for (Map.Entry<String, java.util.Set<E3Model>> cursor : groupedSubIdealModels.entrySet()) {
                    DefaultMutableTreeNode category = new DefaultMutableTreeNode(cursor.getKey());
                    root.add(category);
                    sortedSubIdealModels = ModelRanker.sortByLossThenGain(null,baseModel, cursor.getValue(), selectedActor, selectedNeed, startValue, endValue, false);
                    for (E3Model subIdealModel : sortedSubIdealModels) {
                    subIdealModel.setDescription(
                                "Average loss of <b>"
                                + df.format(subIdealModel.getLastKnownAverages().get(selectedActor))
                                + " </b>(instead of <b>"
                                + df.format(baseModel.getLastKnownAverages().get(selectedActor))
                                + "</b>) for "
                                + selectedActor.getProperty(E3value.e3_has_name).getString()
                                //+ " = " + subIdealModel.getLastKnownAverages().get(selectedActor)
                                + " due to: <br>"
                                + subIdealModel.getDescription());
                        category.add(new DefaultMutableTreeNode(subIdealModel));
                    }                    
                    setProgress(100*i/numberOfSubIdealModels);
                    i++;
                }
            }
            

        //ungrouped case
        } else {
            //generate
            Set<E3Model> subIdealModels = subIdealModelGenerator.generateAll(baseModel, selectedActor, collusions);
            // generation done
            System.out.println(currentTime.currentTime() + " Generated : " + subIdealModels.size() + " sub-ideal models!" + newline);
            // start ranking       
            if (sortCriteria==2) {
                firePropertyChange("phase", "whatever","ranking...");
                System.out.println(currentTime.currentTime() + " Ranking sub-ideal models " + newline + "\tbased on average \u0394gain of the any actor  in the model except \"" + selectedActorString + "\"" + newline + "\twhen \"" + selectedNeedString + "\" " + "\toccurs " + startValue + " to " + endValue + " times..." + newline);
                sortedSubIdealModels = ModelRanker.sortByGain(this, baseModel, subIdealModels, selectedActor, selectedNeed, startValue, endValue, false);
                for (E3Model subIdealModel : sortedSubIdealModels) {
                    root.add(new DefaultMutableTreeNode(subIdealModel));
                }
            } else if (sortCriteria==1) {
                firePropertyChange("phase", "whatever","ranking...");
                System.out.println(currentTime.currentTime() + " Ranking sub-ideal models " + newline + "\tbased on average loss for \"" + selectedActorString + "\"" + newline + "\t and on average \u0394gain of the other actors in the model " + newline + "\twhen \"" + selectedNeedString + "\" " + "\toccurs " + startValue + " to " + endValue + " times..." + newline);
                sortedSubIdealModels = ModelRanker.sortByLossThenGain(this,baseModel, subIdealModels, selectedActor, selectedNeed, startValue, endValue, false);
                for (E3Model subIdealModel : sortedSubIdealModels) {
                    subIdealModel.setDescription(
                                "Average loss of <b>"
                                + df.format(subIdealModel.getLastKnownAverages().get(selectedActor))
                                + " </b>(instead of <b>"
                                + df.format(baseModel.getLastKnownAverages().get(selectedActor))
                                + "</b>) for "
                                + selectedActor.getProperty(E3value.e3_has_name).getString()
                                //+ " = " + subIdealModel.getLastKnownAverages().get(selectedActor)
                                + " due to: <br>"
                                + subIdealModel.getDescription());
                    root.add(new DefaultMutableTreeNode(subIdealModel));
                }
            }
        }

        //ranking done
        System.out.println(currentTime.currentTime() + " Ranking complete! " + newline);
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
