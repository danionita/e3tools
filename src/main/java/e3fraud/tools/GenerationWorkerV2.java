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
package e3fraud.tools;

/**
 *
 * @author Dan
 */
import com.hp.hpl.jena.rdf.model.Resource;
import e3fraud.model.E3Model;
import e3fraud.model.SubIdealModelGenerator;
import e3fraud.tools.currentTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;

public class GenerationWorkerV2 extends SwingWorker<java.util.HashMap<String, java.util.Set<E3Model>>, String> {

    static private final String newline = "\n";
    private final E3Model baseModel;
    private final String selectedNeedString;
    private final Resource mainActor;
    private final Resource selectedNeed;
    private final int startValue;
    private final int endValue;
    private final int collusions;
    private final java.util.HashMap<String, java.util.Set<E3Model>> groupedSubIdealModels;
    private final DefaultMutableTreeNode root;
    private int numberOfSubIdealModels;
    int i;
    private final boolean generateNonOccurring, generateHidden, generateCollusions;

    /**
     *
     * @param baseModel the model to analyze
     * @param mainActorString the main actor's name
     * @param mainActor the main actor's RDF resource
     * @param selectedNeed the selected need's RDF resource
     * @param selectedNeedString the selected need's name
     * @param startValue the min occurrence rate of need
     * @param generateNonOccurring
     * @param endValue the max occurrence rate of need
     * @param generateCollusions
     * @param generateHidden
     * @param advancedGenerationSettings
     * @param sortCriteria 0 - do not sort, 1 - sort by loss first, 2- sort by
     * gain first
     * @param groupingCriteria 0 - do not group, 1 - group based on generated
     * collusion groups
     * @param collusions maximum number of actors which can be part of a single
     * colluded actor
     */
    public GenerationWorkerV2(E3Model baseModel, Resource mainActor, Resource selectedNeed, String selectedNeedString, int startValue, int endValue, boolean generateNonOccurring, boolean generateHidden, boolean generateCollusions, int collusions) {
        this.baseModel = baseModel;
        this.mainActor = mainActor;
        this.selectedNeed = selectedNeed;
        this.selectedNeedString = selectedNeedString;
        this.startValue = startValue;
        this.endValue = endValue;
        this.root = new DefaultMutableTreeNode("root");
        this.groupedSubIdealModels = new HashMap<>();
        this.generateNonOccurring = generateNonOccurring;
        this.generateHidden = generateHidden;
        this.generateCollusions = generateCollusions;
        this.collusions = collusions;
    }

    @Override
    protected java.util.HashMap<String, java.util.Set<E3Model>> doInBackground() throws Exception {

        //baseModel.enhance();
        baseModel.getAveragesForActors(selectedNeed, startValue, endValue, true);

        // Start generation
        System.out.println(currentTime.currentTime() + " Generating sub-ideal models...." + newline + "\t with need \"" + selectedNeedString + "\" " + "\toccuring " + startValue + " to " + endValue + " times..." + newline);
        SubIdealModelGenerator subIdealModelGenerator = new SubIdealModelGenerator();

        int size = 0;
        Set<E3Model> colludedAndNonColludedModels = new HashSet<>();
        if (generateCollusions) {
            //generate colluded models               
            colludedAndNonColludedModels = subIdealModelGenerator.generateCollusions(baseModel, mainActor, collusions);
        }
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
            if (generateNonOccurring) {
                intermediaryModels.addAll(subIdealModelGenerator.generateNonoccurringTransactions(model));
            }
            subIdealModels.addAll(intermediaryModels);
            intermediaryModels.add(model);
            int i = 1;
            if (generateHidden) {
                for (E3Model intermediaryModel : intermediaryModels) {
                    subIdealModels.addAll(subIdealModelGenerator.generateHiddenTransactions(intermediaryModel, mainActor));
                }
            }


            size += subIdealModels.size();
            //System.out.println("\t\tGenerated " + subIdealModels.size() + " sub-ideal models for category " + category + ":");
            groupedSubIdealModels.put(category, subIdealModels);
        }
        // generation done
        System.out.println(currentTime.currentTime() + " Generated : " + size + " sub-ideal models (" + colludedAndNonColludedModels.size() + " groups)!" + newline);

        return groupedSubIdealModels;
//    @Override
//    protected void process(List<String> chunks) {
//        for (final String string : chunks) {
//            log.append(string);
//            log.append("\n");
//        }
//    }
    }

  
}
