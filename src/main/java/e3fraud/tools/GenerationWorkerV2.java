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
import e3fraud.model.FraudModelGenerator;
import e3fraud.tools.SettingsObjects.GenerationSettings;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;

public class GenerationWorkerV2 extends SwingWorker<java.util.HashMap<String, java.util.Set<E3Model>>, String> {

    boolean debug = true;
    static private final String newline = "\n";
    private final E3Model baseModel;
    private final HashMap<String,Resource> trustedActors;
    private final int collusions;
    private final java.util.HashMap<String, java.util.Set<E3Model>> groupedSubIdealModels;
    private final int hiddenTransfersPerExchange;
    private final boolean generateNonOccurring, generateHidden, generateCollusions;
    private List<String> typesOfNonOccurringTransfers;

    /**
     *
     * @param baseModel the model to analyze
     * @param trustedActors
     * @param advancedGenerationSettings
     */
    public GenerationWorkerV2(E3Model baseModel, HashMap<String,Resource> trustedActors, GenerationSettings advancedGenerationSettings) {
        this.baseModel = baseModel;
        this.trustedActors = trustedActors;
        this.groupedSubIdealModels = new HashMap<>();
        this.generateNonOccurring = advancedGenerationSettings.isGenerateNonOccurring();
        this.generateHidden = advancedGenerationSettings.isGenerateHidden();
        this.generateCollusions = advancedGenerationSettings.isGenerateCollusion();
        this.collusions = advancedGenerationSettings.getColludingActors()-1; //collusions = number of colluding actors - 1
        this.hiddenTransfersPerExchange = advancedGenerationSettings.getNumberOfHiddenTransfersPerExchange();
        this.typesOfNonOccurringTransfers = advancedGenerationSettings.getTypesOfNonOccurringTransfers();
    }

    @Override
    protected java.util.HashMap<String, java.util.Set<E3Model>> doInBackground() throws Exception {
        //Analyze the baseModel (so we can compare the fraud scenarios to it)
        //baseModel.generateSeriesAndComputeAverages(selectedNeed, startValue, endValue, true);

        // Start generation
        if(debug){
            String generationAnnouncement=" Generating sub-ideal models containing: ";
            if (generateNonOccurring){generationAnnouncement+="non-occurring transfers (of types "+typesOfNonOccurringTransfers+"), ";} 
            if (generateHidden){generationAnnouncement+="hidden transfers (of "+hiddenTransfersPerExchange+" different values), ";} 
            if (generateCollusions){generationAnnouncement+="collusion (of up to "+collusions+" actors).";} 
            System.out.println(currentTime.currentTime() + generationAnnouncement  + newline);
        }

        FraudModelGenerator subIdealModelGenerator = new FraudModelGenerator();

        int size = 0;
        Set<E3Model> colludedAndNonColludedModels = new HashSet<>();
        if (generateCollusions) {
            //generate colluded models               
            colludedAndNonColludedModels = subIdealModelGenerator.generateCollusions(baseModel, trustedActors, collusions);
        }
        //use base model as a basis for the non-collusion group
        colludedAndNonColludedModels.add(baseModel);

        //for each type of collusion
        for (E3Model model : colludedAndNonColludedModels) {
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
                intermediaryModels.addAll(subIdealModelGenerator.generateNonoccurringTransactions(model,trustedActors,typesOfNonOccurringTransfers));
            }
            
            subIdealModels.addAll(intermediaryModels);
            intermediaryModels.add(model);
            int i = 1;
            if (generateHidden) {
                for (E3Model intermediaryModel : intermediaryModels) {
                    intermediaryModel.enhance();
                    subIdealModels.addAll(subIdealModelGenerator.generateHiddenTransactions(intermediaryModel, trustedActors,hiddenTransfersPerExchange));
                }
            }

            size += subIdealModels.size();
            //System.out.println("\t\tGenerated " + subIdealModels.size() + " sub-ideal models for category " + category + ":");
            groupedSubIdealModels.put(category, subIdealModels);
            
        }
        // generation done
        System.out.println(currentTime.currentTime() + " Generated : " + size + " sub-ideal models (" + colludedAndNonColludedModels.size() + " groups)!" + newline);
        return groupedSubIdealModels;
    }

  
}
