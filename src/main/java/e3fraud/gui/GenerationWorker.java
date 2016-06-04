/*
 * Copyright (C) 2015 Dan
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
    private final JTextArea log;
    private final JRadioButton lossButton, gainButton, lossGainButton;
    private final JSpinner collusionsButton;
    private final JCheckBox groupingButton;
    private java.util.List<E3Model> sortedSubIdealModels;
    private final java.util.HashMap<String, java.util.Set<E3Model>> groupedSubIdealModels;
    private final DefaultMutableTreeNode root;
    private int numberOfSubIdealModels;
    int i;
    
    public GenerationWorker(E3Model baseModel, String selectedActorString, Resource selectedActor, Resource selectedNeed, String selectedNeedString, int startValue, int endValue, JTextArea log, JRadioButton lossButton, JRadioButton gainButton, JRadioButton lossGainButton, JRadioButton gainLossButton, JCheckBox groupingButton, JSpinner collusionsButton) {
        this.baseModel = baseModel;
        this.selectedActorString = selectedActorString;
        this.selectedActor = selectedActor;
        this.selectedNeed = selectedNeed;
        this.selectedNeedString = selectedNeedString;
        this.startValue = startValue;
        this.endValue = endValue;
        this.log = log;
        this.collusionsButton = collusionsButton;
        this.lossButton = lossButton;
        this.gainButton = gainButton;
        this.lossGainButton = lossGainButton;
        this.groupingButton = groupingButton;
        this.sortedSubIdealModels = null;
        this.root = new DefaultMutableTreeNode("root");
        this.groupedSubIdealModels = new HashMap<String, java.util.Set<E3Model>>();      
        
    }

    @Override
    protected DefaultMutableTreeNode doInBackground() throws Exception {
DecimalFormat df = new DecimalFormat("#.##");             
    // Start generation
        publish(currentTime.currentTime() + " Generating sub-ideal models...." + newline);
        SubIdealModelGenerator subIdealModelGenerator = new SubIdealModelGenerator();

        //grouped case
        if (groupingButton.isSelected()) {
            int size = 0;

            //generate colluded models            
            Set<E3Model> colludedAndNonColludedModels = subIdealModelGenerator.generateCollusions(baseModel, selectedActor, (int)collusionsButton.getValue());
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
            publish(currentTime.currentTime() + " Generated : " + size + " sub-ideal models (" + colludedAndNonColludedModels.size() + " groups)!" + newline);

            //now rank
            if (lossButton.isSelected()) {
                firePropertyChange("phase", "whatever","ranking...");
                publish(currentTime.currentTime() + " Ranking each group " + newline + "\tbased on average loss for \"" + selectedActorString + "\"" + newline + "\twhen \"" + selectedNeedString + "\" " + "\toccurs " + startValue + " to " + endValue + " times..." + newline); 
                i=0;
                numberOfSubIdealModels = groupedSubIdealModels.size();
                for (Map.Entry<String, java.util.Set<E3Model>> cursor : groupedSubIdealModels.entrySet()) {
                    DefaultMutableTreeNode category = new DefaultMutableTreeNode(cursor.getKey());
                    root.add(category);
                    sortedSubIdealModels = ModelRanker.sortByLoss(null, cursor.getValue(), selectedActor, selectedNeed, startValue, endValue, false);
                    for (E3Model subIdealModel : sortedSubIdealModels) {
                    subIdealModel.setDescription(
                                "Average loss of "
                                + df.format(subIdealModel.getLastKnownAverages().get(selectedActor) - baseModel.getLastKnownAverages().get(selectedActor)) 
                                + " for "
                                + selectedActor.getProperty(E3value.e3_has_name).getString()
                                //+ " = " + subIdealModel.getLastKnownAverages().get(selectedActor)
                                + " due to: <br>"
                                + subIdealModel.getDescription());
                        category.add(new DefaultMutableTreeNode(subIdealModel));
                    }
                    setProgress(100*i/numberOfSubIdealModels);
                    i++;
                }
            } else if (gainButton.isSelected()) {
                firePropertyChange("phase", "whatever","ranking...");
                publish(currentTime.currentTime() + " Ranking each group " + newline + "\tbased on average \u0394gain of the any actor  in the model except \"" + selectedActorString + "\"" + newline + "\twhen \"" + selectedNeedString + "\" " + "\toccurs " + startValue + " to " + endValue + " times..." + newline);
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
            } else if (lossGainButton.isSelected()) {
                firePropertyChange("phase", "whatever","ranking...");
                publish(currentTime.currentTime() + " Ranking each group " + newline + "\tbased on average loss for \"" + selectedActorString + "\"" + newline + "\t and on average \u0394gain of the other actors in the model " + newline + "\twhen \"" + selectedNeedString + "\" " + "\toccurs " + startValue + " to " + endValue + " times..." + newline);
                                i=0;
                numberOfSubIdealModels = groupedSubIdealModels.size();
                for (Map.Entry<String, java.util.Set<E3Model>> cursor : groupedSubIdealModels.entrySet()) {
                    DefaultMutableTreeNode category = new DefaultMutableTreeNode(cursor.getKey());
                    root.add(category);
                    sortedSubIdealModels = ModelRanker.sortByLossandGain(null,baseModel, cursor.getValue(), selectedActor, selectedNeed, startValue, endValue, false);
                    for (E3Model subIdealModel : sortedSubIdealModels) {
                    subIdealModel.setDescription(
                                "Average of <b>"
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
            Set<E3Model> subIdealModels = subIdealModelGenerator.generateAll(baseModel, selectedActor, (int)collusionsButton.getValue());
            // generation done
            publish(currentTime.currentTime() + " Generated : " + subIdealModels.size() + " sub-ideal models!" + newline);
            // start ranking       
            if (lossButton.isSelected()) {
                firePropertyChange("phase", "whatever","ranking...");
                publish(currentTime.currentTime() + " Ranking sub-ideal models " + newline + "\tbased on average loss for \"" + selectedActorString + "\"" + newline + "\twhen \"" + selectedNeedString + "\" " + "\toccurs " + startValue + " to " + endValue + " times..." + newline);
                sortedSubIdealModels = ModelRanker.sortByLoss(this,subIdealModels, selectedActor, selectedNeed, startValue, endValue, false);
                for (E3Model subIdealModel : sortedSubIdealModels) {
                    subIdealModel.setDescription(
                                "Average loss of "
                                + df.format(subIdealModel.getLastKnownAverages().get(selectedActor) - baseModel.getLastKnownAverages().get(selectedActor)) 
                                + " for "
                                + selectedActor.getProperty(E3value.e3_has_name).getString()
                                //+ " = " + subIdealModel.getLastKnownAverages().get(selectedActor)
                                + " due to: <br>"
                                + subIdealModel.getDescription());
                    root.add(new DefaultMutableTreeNode(subIdealModel));
                }
            } else if (gainButton.isSelected()) {
                firePropertyChange("phase", "whatever","ranking...");
                publish(currentTime.currentTime() + " Ranking sub-ideal models " + newline + "\tbased on average \u0394gain of the any actor  in the model except \"" + selectedActorString + "\"" + newline + "\twhen \"" + selectedNeedString + "\" " + "\toccurs " + startValue + " to " + endValue + " times..." + newline);
                sortedSubIdealModels = ModelRanker.sortByGain(this, baseModel, subIdealModels, selectedActor, selectedNeed, startValue, endValue, false);
                for (E3Model subIdealModel : sortedSubIdealModels) {
                    root.add(new DefaultMutableTreeNode(subIdealModel));
                }
            } else if (lossGainButton.isSelected()) {
                firePropertyChange("phase", "whatever","ranking...");
                publish(currentTime.currentTime() + " Ranking sub-ideal models " + newline + "\tbased on average loss for \"" + selectedActorString + "\"" + newline + "\t and on average \u0394gain of the other actors in the model " + newline + "\twhen \"" + selectedNeedString + "\" " + "\toccurs " + startValue + " to " + endValue + " times..." + newline);
                sortedSubIdealModels = ModelRanker.sortByLossandGain(this,baseModel, subIdealModels, selectedActor, selectedNeed, startValue, endValue, false);
                for (E3Model subIdealModel : sortedSubIdealModels) {
                    subIdealModel.setDescription(
                                "Average of <b>"
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
        publish(currentTime.currentTime() + " Ranking complete! Results displayed above." + newline);
        return root;
    }

    @Override
    protected void process(List<String> chunks) {
        for (final String string : chunks) {
            log.append(string);
            log.append("\n");
        }
    }
}
