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

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import design.main.Utils.GraphDelta;
import e3fraud.vocabulary.E3value;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.paukov.combinatorics.Factory;
import org.paukov.combinatorics.Generator;
import org.paukov.combinatorics.ICombinatoricsVector;

/**
 *
 * @author Dan
 */
public class SubIdealModelGenerator {
DecimalFormat df = new DecimalFormat("#.####"); 
int STEPS = 3;

    public Set<E3Model> generateAll(E3Model baseModel, Resource mainActor, int maxCollusions) {

        System.out.println("GENERATING MODELS...\n\n\n");
        Set<E3Model> subIdealModels = new HashSet<>();
        System.out.println("GENERATING collusions...");
        Set<E3Model> colludedModels = generateCollusions(baseModel, mainActor, maxCollusions);
        System.out.println("GENERATING hidden...");
        Set<E3Model> hiddenModels = generateHiddenTransactions(baseModel, mainActor);
        System.out.println("GENERATING nonOcurring...");
        Set<E3Model> nonOccuringModels = generateNonoccurringTransactions(baseModel);
        Set<E3Model> colludedAndNonOccuringModels = new HashSet<>();
        Set<E3Model> hiddenAndNonOccuringModels = new HashSet<>();
        Set<E3Model> colludedAndHiddenModels = new HashSet<>();
        Set<E3Model> colludedHiddenAndNonOccuringModels = new HashSet<>();

        //for each combination of collusion
        for (E3Model colludedModel : colludedModels) {
            //generate all possible combinations of non-occuring transactions to the result
            colludedAndNonOccuringModels.addAll(generateNonoccurringTransactions(colludedModel));
            colludedAndHiddenModels.addAll(generateHiddenTransactions(colludedModel, mainActor));
        }

        //for each combination of nonOccuraning transactions
        int i = nonOccuringModels.size();
                int j=1;
        for (E3Model nonOccuringModel : nonOccuringModels) {
            //generate all possible combinations of hidden transactions to the result            
            //System.out.println("adding hidden transfers to nonOccuring model "+j+" out of "+i);
            j++;
            hiddenAndNonOccuringModels.addAll(generateHiddenTransactions(nonOccuringModel, mainActor));
        }

        //for each combination of collusion and non-occuring transaction
                i = colludedAndNonOccuringModels.size();
                j=1;
        for (E3Model colludedAndNonOccuringModel : colludedAndNonOccuringModels) {
            //generate all possible combinations of hidden transactions                         
            //System.out.println("adding hidden transfers to nonOccuring  and colluded model "+j+" out of "+i);
            j++;
            colludedHiddenAndNonOccuringModels.addAll(generateHiddenTransactions(colludedAndNonOccuringModel, mainActor));
        }

        //*********TEST STUFF***************
//        System.out.println("\nGENERATING colludedModels");
//        for (E3Model generatedModel : colludedModels) {
//            System.out.println("Generated:" + generatedModel.getDescription());
//        }
//        System.out.println("\nGENERATING hiddenModels");
//        for (E3Model generatedModel : hiddenModels) {
//            System.out.println("generated:" + generatedModel.getDescription());
//        }
//        System.out.println("\nGENERATING nonOccuringModels");
//        for (E3Model generatedModel : nonOccuringModels) {
//            System.out.println("Generated:" + generatedModel.getDescription());
//        }
//        System.out.println("\nGENERATING hiddenAndNonOccuringModels");
//        for (E3Model generatedModel : hiddenAndNonOccuringModels) {
//            System.out.println("Generated:" + generatedModel.getDescription());
//        }
//        System.out.println("\nGENERATING colludedAndNonOccuringModels");
//        for (E3Model generatedModel : colludedAndNonOccuringModels) {
//            System.out.println("Generated:" + generatedModel.getDescription());
//        }
//        System.out.println("\nGENERATING colludedHiddenAndNonOccuringModels");
//        for (E3Model generatedModel : colludedHiddenAndNonOccuringModels) {
//            System.out.println("Generated:" + generatedModel.getDescription());
//        }
        //*******END OF TEST STUFF*************
        subIdealModels.addAll(colludedModels);
        System.out.println("colludedModels.size()= " + colludedModels.size());
        subIdealModels.addAll(hiddenModels);
        System.out.println("hiddenModels.size()= " + hiddenModels.size());
        subIdealModels.addAll(nonOccuringModels);
        System.out.println("nonOccuringModels.size()= " + nonOccuringModels.size());
        subIdealModels.addAll(hiddenAndNonOccuringModels);
        System.out.println("hiddenAndNonOccuringModels.size()= " + hiddenAndNonOccuringModels.size());
        subIdealModels.addAll(colludedAndNonOccuringModels);
        System.out.println("colludedAndNonOccuringModels.size()= " + colludedAndNonOccuringModels.size());
        subIdealModels.addAll(colludedAndHiddenModels);
        System.out.println("colludedAndHiddenModels.size()= " + colludedAndHiddenModels.size());
        subIdealModels.addAll(colludedHiddenAndNonOccuringModels);
        System.out.println("colludedHiddenAndNonOccuringModels.size()= " + colludedHiddenAndNonOccuringModels.size());

        return subIdealModels;
    }

    public Set<E3Model> generateCollusions(E3Model baseModel, Resource mainActor, int maxCollusions) {
        Set<E3Model> subIdealModels = new HashSet<E3Model>();
        Set<Resource> secondaryActors = baseModel.getActors();
        secondaryActors.remove(mainActor);
        
            //System.out.println("maxCollusions = "+ maxCollusions);
        //for each size of actor collusion
        for(int i=1; i<=maxCollusions; i++){
            //System.out.println("Generating collusions of "+ i + " actors...");
        //generate combinations of i secondary actors
        ICombinatoricsVector<Resource> secondayActorsVector = Factory.createVector(secondaryActors);
        Generator<Resource> secondaryActorsCombinations = Factory.createSimpleCombinationGenerator(secondayActorsVector, i+1);

        //for each combination of  secondary actors:
        for (ICombinatoricsVector<Resource> secondaryActorsCombination : secondaryActorsCombinations) {

            //Create an empty model
            Model model = ModelFactory.createDefaultModel();
            model.add(baseModel.getJenaModel());
            E3Model generatedModel = new E3Model(model, baseModel);

            //just in case previous method did not change anything(if this method was called directly, instead of calling generateAll)
            if (generatedModel.getDescription().equals("Base Model")) {
                generatedModel.setDescription("");
                generatedModel.setIsFraud(true);
                generatedModel.setFraudChanges(new GraphDelta());
            }

            generatedModel.setFraudChanges(new GraphDelta(baseModel.getFraudChanges()));
            
            Resource actor1 = secondaryActorsCombination.getValue(0);
           
            
            String description = ("<b>Colluding actors</b>  \"" + actor1.getProperty(E3value.e3_has_name).getLiteral().toString() + "\"" );
            for(int j=0; j<i; j++){
                Resource actorj = secondaryActorsCombination.getValue(j+1);
                generatedModel.collude(actor1, actorj);                
                description += " and \"" + actorj.getProperty(E3value.e3_has_name).getLiteral().toString() + "\"";
            }
            generatedModel.appendDescription(description);
           //generatedModel.collude(actor1, actor2);
            subIdealModels.add(generatedModel);
        }
        }
        return subIdealModels;
    }

    /**
     *
     * @param baseModel
     * @return a set of models derived from baseModel with all possible
     * combinations of non-occurring (dotted) transactions
     */
    public Set<E3Model> generateNonoccurringTransactions(E3Model baseModel) {
        Set<E3Model> subIdealModels = new HashSet<>();
        Set<Resource> moneyExchanges = baseModel.getMoneyExchanges();

        // Create the initial vector
        ICombinatoricsVector<Resource> initialMoneyExchangesVector = Factory.createVector(moneyExchanges);
        for (int i = 1; i <= moneyExchanges.size(); i++) {
            Generator<Resource> moneyExchangeCombinations = Factory.createSimpleCombinationGenerator(initialMoneyExchangesVector, i);

            for (ICombinatoricsVector<Resource> moneyExchangeCombination : moneyExchangeCombinations) {

                //Create an empty model
                Model model = ModelFactory.createDefaultModel();
                model.add(baseModel.getJenaModel());
                E3Model generatedModel = new E3Model(model, baseModel);

                //just in case previous method did not change anything(if this method was called directly, instead of calling generateAll)
                if (generatedModel.getDescription().equals("Base Model") || generatedModel.getDescription().equals("No collusion")) {
                    generatedModel.setDescription("");
                    generatedModel.setIsFraud(false);
                }
                generatedModel.setFraudChanges(new GraphDelta(baseModel.getFraudChanges()));

                //iterate through the elements of the combination
                Iterator<Resource> moneyExchangeIterator = moneyExchangeCombination.iterator();
                while (moneyExchangeIterator.hasNext()) {
                    //and update new model accordingly
                    Resource exchange = moneyExchangeIterator.next();
                    generatedModel.makeNonOccurring(exchange);
                    generatedModel.appendDescription("<b>Non-occuring</b> exchange " + exchange.getProperty(E3value.e3_has_name).getLiteral().toString());
                }
                //System.out.println("Generated:" + generatedModel.getDescription());
                subIdealModels.add(generatedModel);
            }
        }

        return subIdealModels;
    }

    /**
     *
     * @param baseModel
     * @param mainActor
     * @return a set of models derived from baseModel with all possible
     * combinations of hidden (dotted) transactions
     */
    public Set<E3Model> generateHiddenTransactions(E3Model baseModel, Resource mainActor) {
        Set<E3Model> subIdealModels = new HashSet<>();
        Set<Resource> secondaryActors = baseModel.getActors();
        secondaryActors.remove(mainActor);
        baseModel.enhance();    //<-------- IT HANGS HERE!!!
        
        double value;
        double step;

        //System.out.println("\t generating hidden transactions for model: "+baseModel.getDescription());
        //generate combinations of 2 secondary actors
        ICombinatoricsVector<Resource> secondayActorsVector = Factory.createVector(secondaryActors);
        Generator<Resource> secondaryActorsCombinations = Factory.createSimpleCombinationGenerator(secondayActorsVector, 2);
        //for each combination of two secondary actors:
        for (ICombinatoricsVector<Resource> secondaryActorsCombination : secondaryActorsCombinations) {
               
        //System.out.println("\t\t parsing a combination");
            Resource actor1 = secondaryActorsCombination.getValue(0);
            Resource actor2 = secondaryActorsCombination.getValue(1);

            
        //System.out.println("\t\t\t checking if they have a transaction between them");
            //check if they  have a transaction between them
            Map<Resource, Resource> commonInterfaces = baseModel.getInterfacesBetween(actor1, actor2);

            //and if they do
            if (!commonInterfaces.isEmpty()) {
                    
            //System.out.println("\t\t\t\t found "+ commonInterfaces.size()+ " transaction between them");
                Iterator commonInterfaceIterator = commonInterfaces.entrySet().iterator();
                //for each pair of (unique) common interfaces
                while (commonInterfaceIterator.hasNext()) {                 
                    Map.Entry<Resource, Resource> commonInterfacePair = (Map.Entry) commonInterfaceIterator.next();
                    
                    
                    //System.out.println("\t\t\t\t\t getting totals");           
                    //First we get the total of each corresponding actor FOR THE CORRESPONDING DEPENDENCY PATH, in this model
                    Resource interface1 = commonInterfacePair.getKey();
                    double actor1Total = baseModel.getTotalForActorPerOccurence(actor1, interface1, false);
                    Resource interface2 = commonInterfacePair.getValue();
                    double actor2Total = baseModel.getTotalForActorPerOccurence(actor2, interface2, false);
                    
                    //System.out.println("\t\t\t\t\t total for actor 1 is "+ actor1Total +" and total for actor 2 is "+ actor2Total); 
                    //Then, we create outgoing hidden transactions for each actor of up to the total we just computed:
                    //System.out.println("\t\t\t\t\tcreating hidden transactions"); 
                    
                         //To do so, we generate models with money flows in each direction , ranging from 0 to the total Profit of the actor:           
                    //if actor1 has a positive financial result
                    if (actor1Total > 0) {
                        //divide this result into 10 values (9 + the "zero" value)
                                         
                        step = actor1Total / STEPS;

                        //and for each value
                        for (value = step; value < actor1Total; value = value + step) {
                            //System.out.println("\t\t\t\t\t\tcreating new model"); 
                            //Create an empty model
                            Model model = ModelFactory.createDefaultModel();
                            model.add(baseModel.getJenaModel());
                            E3Model generatedModel = new E3Model(model, baseModel);
                            //System.out.println("\t\t\t\t\t adding a hidden transfer of "+ value+ " between \""+ actor1.getProperty(E3value.e3_has_name).getLiteral().toString() + "\" and \""+ actor2.getProperty(E3value.e3_has_name).getLiteral().toString()+ "\"");     
                    
                            //just in case previous method did not change anything (if this method was called directly, instead of calling generateAll)
                            if (generatedModel.getDescription().equals("Base Model") || generatedModel.getDescription().equals("No collusion")) {
                                generatedModel.setDescription("");
                                generatedModel.setIsFraud(true);
                                generatedModel.setFraudChanges(new GraphDelta());
                            }
                            
                            generatedModel.setFraudChanges(new GraphDelta(baseModel.getFraudChanges()));
                            //add a transfer from actor1 to actor 2 of the value
                            
                           // System.out.println("\t\t\t\t\t\tadding a hidden transfer"); 
                            generatedModel.addTransfer(interface1, interface2, (float)value);
                            int interface1ID = interface1.getProperty(E3value.e3_has_uid).getInt(); // Integer.parseInt(interface1.getProperty(E3value.e3_has_uid).toString());
                            int interface2ID = interface2.getProperty(E3value.e3_has_uid).getInt(); //  Integer.parseInt(interface2.getProperty(E3value.e3_has_uid).toString());
                            generatedModel.getFraudChanges().addHiddenTransaction(interface1ID,interface2ID);
                            generatedModel.appendDescription("<b>Hidden</b> transfer of value " + df.format(value) + " (out of " + df.format(actor1Total) + ") per occurence from \"" + actor1.getProperty(E3value.e3_has_name).getLiteral().toString() + "\" to \"" + actor2.getProperty(E3value.e3_has_name).getLiteral().toString() + "\"");
                            
                            //System.out.println("\t\t\t\t\t\tadding the new model to the list"); 
                            subIdealModels.add(generatedModel);
                        }
                    }

                    //if actor2 has a positive financial result
                    if (actor2Total > 0) {
                        //divide this result into 10 values
                        step = actor2Total / STEPS;

                        //and for each value
                        for (value = step; value < actor2Total; value = value + step) {

                            //Create an empty model
                            Model model = ModelFactory.createDefaultModel();
                            model.add(baseModel.getJenaModel());
                            E3Model generatedModel = new E3Model(model, baseModel);
                            

                            //just in case previous method did not change anything (if this method was called directly, instead of calling generateAll)
                            if (generatedModel.getDescription().equals("Base Model")) {
                                generatedModel.setDescription("");
                                generatedModel.setIsFraud(true);
                                generatedModel.setFraudChanges(new GraphDelta());
                            }

                            generatedModel.setFraudChanges(new GraphDelta(baseModel.getFraudChanges()));
                            
                            //add a transfer from actor1 to actor 2 of the value
                            generatedModel.addTransfer(interface2, interface1, value);
                            generatedModel.appendDescription("<b>Hidden</b>  transfer of value " + df.format(value) + " (out of " + df.format(actor2Total) + ") from \"" + actor2.getProperty(E3value.e3_has_name).getLiteral().toString() + "\" to \"" + actor1.getProperty(E3value.e3_has_name).getLiteral().toString() + "\"");
                            subIdealModels.add(generatedModel);
                        }
                    }
                }
            }

        }
        return subIdealModels;
    }
    
    
}
