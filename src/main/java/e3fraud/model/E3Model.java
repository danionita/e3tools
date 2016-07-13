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
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import design.main.Utils;
import e3fraud.vocabulary.E3value;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;

/**
 *
 * @author IonitaD
 */
public class E3Model {

    private final Model model;
    private String description;
    private boolean isFraud;
    private Utils.GraphDelta fraudChanges;
    private String prefix;

    public boolean isIsFraud() {
        return isFraud;
    }

    public void setIsFraud(boolean isFraud) {
        this.isFraud = isFraud;
    }

    public Utils.GraphDelta getFraudChanges() {
        return fraudChanges;
    }

    public void setFraudChanges(Utils.GraphDelta fraudChanges) {
        this.fraudChanges = fraudChanges;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String colludedActorURI;
    public String newActorURI;

    //the following are needed to save time on calcualting the averages and deltas everytime when sorting 
    private double lastKnownAverage;
    private Map<Resource, Double> lastKnownAverages;
    private double lastKnownTopDelta;
    private Resource topDeltaActor;

    public Resource getTopDeltaActor() {
        return topDeltaActor;
    }

    public void setTopDeltaActor(Resource topDeltaActor) {
        this.topDeltaActor = topDeltaActor;
    }

    public double getLastKnownTopDelta() {
        return lastKnownTopDelta;
    }

    public void setLastKnownTopDelta(double lastKnownTopDelta) {
        this.lastKnownTopDelta = lastKnownTopDelta;
    }

    public E3Model(Model jenaModel) {
        this.model = jenaModel;
        this.description = "Base Model";
    }

    /**
     *
     * @param jenaModel the jena model to include
     * @param baseModel the E3model to take description and collusion info from
     */
    public E3Model(Model jenaModel, E3Model baseModel) {
        this.model = jenaModel;
        this.description = baseModel.getDescription();
        this.colludedActorURI = baseModel.colludedActorURI;
        this.newActorURI = baseModel.newActorURI;
    }

    /**
     * Computes and appends occurrence rates to each ValueInterface (to allow
     * easier computation of Profit per actor by getTotalForActor(Resource
     * actor). ATTENTION: this method MUST be ran every time an update is done
     * to the model (such as a parameter change).
     */
    public void enhance() {
        //get a list of Start Stimuli
        ResIterator startStimuli = model.listSubjectsWithProperty(RDF.type, E3value.start_stimulus);

        //for each Start Stimulus        
        while (startStimuli.hasNext()) {
            Resource startStimulus = startStimuli.next();
            //System.err.println(startStimulus.getProperty(E3value.e3_has_name));
            double occurences = 0;
            //get occurences of this stimuli
            StmtIterator startStimulusFormulas = startStimulus.listProperties(E3value.e3_has_formula);
            while (startStimulusFormulas.hasNext()) {
                Statement formula = startStimulusFormulas.next();
                if (formula.getString().split("=", 2)[0].equals("OCCURRENCES")) {
                    occurences = Float.valueOf(formula.getString().split("=", 2)[1]);
                }
            }
            //get nextElement down the line
            Resource nextElement = startStimulus.getProperty(E3value.de_down_ce).getResource();
            //and go down the depdendecy path until the end
            traverse(nextElement, occurences);
            //System.out.println("\t...Finished!\n");
        }
    }

    /**
     * Starting from nextElement, goes down the dependency path, until reaching
     * the end stimulus (or stimuli if AND/OR forks are present), updating
     * OCCURENCE rates of Value Interfaces along the way
     *
     * @param nextElement the element in the graph from which to start
     * traversing downwards
     * @param occurences the occurrence rate of nextElement
     */
    private void traverse(Resource nextElement, double occurences) {
        //While this is not the last element (i.e. an end stimulus)
        while (!nextElement.hasProperty(RDF.type, E3value.end_stimulus)) {
            //System.out.println("\t\t...moved to element: " + nextElement.getProperty(E3value.e3_has_name).getString());
            //if it is a ValueInterface
            if (nextElement.hasProperty(RDF.type, E3value.value_interface)) {
                //add the respective OCCURRENCE rate
                if (nextElement.hasProperty(E3value.e3_has_formula)) {
                    nextElement.getProperty(E3value.e3_has_formula).changeObject("OCCURRENCES=" + occurences);
                    //System.out.println("\t\t\t...updated OCCURRENCES=" + occurences + " to " + nextElement.getProperty(E3value.e3_has_name).getString());
                } else {
                    nextElement.addProperty(E3value.e3_has_formula, "OCCURRENCES=" + occurences);
                    //System.out.println("\t\t\t...added OCCURRENCES=" + occurences + " to " + nextElement.getProperty(E3value.e3_has_name).getString());
                }
            }

            //then move to next element, depending on the type of element the current one is:
            if (nextElement.hasProperty(RDF.type, E3value.OR_node)) { //if it's a OR node
                StmtIterator nodes = nextElement.listProperties(E3value.de_down_ce);//get outgoing connection elements
                if (nodes.hasNext()) {
                    List<Statement> nodeList = nodes.toList();//get list() of outgoing elements (for more control)
                    //System.out.println("\t\t\t ...found OR node with " + nodeList.size() + " outgoing ports");
                    //First, sum up the fractions of all outgoing Connection Elements
                    double totalFractions = 0;
                    for (Statement node : nodeList) {
                        totalFractions += node.getResource().getProperty(E3value.up_fraction).getFloat();
                    }
                    //Second, go down each path using occurence = OCCURENCE*FRACTION/Total_FRACTIONs
                    for (Statement node : nodeList) {
                        traverse(node.getResource(), occurences * node.getResource().getProperty(E3value.up_fraction).getFloat() / totalFractions);
                    }
                    return;
                }
            } else if (nextElement.hasProperty(RDF.type, E3value.AND_node)) { //if it's a AND node
                StmtIterator nodes = nextElement.listProperties(E3value.de_down_ce);//get outgoing connection elements
                if (nodes.hasNext()) {
                    List<Statement> nodeList = nodes.toList();//get list() of outgoing elements (for more control)
                    //System.out.println("\t\t\t ...found AND  node with " + nodeList.size() + " outgoing ports");
                    //Second, go down each path using occurence = OCCURENCE*fraction)
                    for (Statement node : nodeList) {
                        traverse(node.getResource(), occurences * node.getResource().getProperty(E3value.up_fraction).getFloat());
                    }
                    return;
                }
            } else if (nextElement.hasProperty(RDF.type, E3value.connection_element)) {
                //System.out.println("\t\tfound connection element");//if it's a ConnectionElement
                nextElement = nextElement.getProperty(E3value.ce_with_down_de).getResource();//choose the next element (Value Interface or AND/OR node)
            } else if (nextElement.hasProperty(RDF.type, E3value.value_interface) && nextElement.hasProperty(E3value.de_up_ce)) { //if it's a ValueInterface with an incoming ConnectionElement (meaning we need to go down the ValueExchange)
                //System.out.println("\t\tfound ValueInterface with an incoming ConnectionElement");
                StmtIterator valueOfferings = nextElement.listProperties(E3value.vi_consists_of_of);  //get it's ValueOfferings
                //and choose the outgoing ValueOffering
                while (valueOfferings.hasNext()) {
                    Statement valueOffering = valueOfferings.next();
                    if (valueOffering.getResource().getProperty(E3value.e3_has_name).getString().equals("out")) {
                        nextElement = valueOffering.getResource();
                    }
                }
            } else if (nextElement.hasProperty(RDF.type, E3value.value_offering) && nextElement.getProperty(E3value.e3_has_name).getString().equals("out")) {//if it's a (outgoing) ValueOffering
                //and it has ports
                //System.out.println("\t\tfound ValueInterface with an (outgoing) ValueOffering");
                if (nextElement.getProperty(E3value.vo_consists_of_vp) != null) {
                    //System.out.println("\t\tand it has outgoing ports");
                    nextElement = nextElement.getProperty(E3value.vo_consists_of_vp).getResource();//choose one of it's ports                    
                } //otherwise, (this happens when the transaction is non-reciprocal so there are no out nodes)
                else {
                    //System.out.println("\t\tand it does not have outgoing ports");
                    //go back to its respective Value Interface
                    nextElement = nextElement.getProperty(E3value.vo_in_vi).getResource();
                    //and choose the incoming ValueOffering
                    StmtIterator valueOfferings = nextElement.listProperties(E3value.vi_consists_of_of);  //get it's ValueOfferings
                    while (valueOfferings.hasNext()) {
                        Statement valueOffering = valueOfferings.next();
                        if (valueOffering.getResource().getProperty(E3value.e3_has_name).getString().equals("in")) {
                            nextElement = valueOffering.getResource();
                            nextElement = nextElement.getProperty(E3value.vo_consists_of_vp).getResource();//choose one of it's ports
                            nextElement = nextElement.getProperty(E3value.vp_in_connects_ve).getResource();//choose it's respective (incoming) ValueExchange                        
                            nextElement = nextElement.getProperty(E3value.ve_has_out_po).getResource();//choose it's next (outgoing) ValuePort
                            nextElement = nextElement.getProperty(E3value.vp_in_vo).getResource();//choose it's correspongind (incoming) ValueOffering
                            nextElement = nextElement.getProperty(E3value.vo_in_vi).getResource();
                        }
                    }
                }
            } else if (nextElement.hasProperty(E3value.vp_out_connects_ve)) {//if it's an (outgoing) ValuePort
                nextElement = nextElement.getProperty(E3value.vp_out_connects_ve).getResource();//choose it's respective ValueExchange
            } else if (nextElement.hasProperty(RDF.type, E3value.value_exchange)) {//if it's a ValueExchange
                nextElement = nextElement.getProperty(E3value.ve_has_in_po).getResource();//choose it's next (incoming) ValuePort
            } else if (nextElement.hasProperty(E3value.vp_in_connects_ve)) {//if it's an (incoming) ValuePort
                nextElement = nextElement.getProperty(E3value.vp_in_vo).getResource();//choose it's correspongind (incoming) ValueOffering
            } else if (nextElement.hasProperty(RDF.type, E3value.value_offering) && nextElement.getProperty(E3value.e3_has_name).getString().equals("in")) {//if it's an (incoming) ValueOffering
                nextElement = nextElement.getProperty(E3value.vo_in_vi).getResource();//choose it's respective ValueInterface
            } else if (nextElement.hasProperty(RDF.type, E3value.value_interface) && nextElement.hasProperty(E3value.de_down_ce)) { //if it's a ValueInterface with an outgoing ConnectionElement (meaning we need to go down this ConnectionElement)
                nextElement = nextElement.getProperty(E3value.de_down_ce).getResource();//choose the next Connection Element
            }
        }
        //System.out.println("\t...reached end stimulus!");
    }

    public Model getJenaModel() {
        return model;
    }

    public double getLastKnownAverage() {
        return lastKnownAverage;
    }

    private Resource getModelResource() {
        return model.listResourcesWithProperty(RDF.type, E3value.model).next();
    }

    private Resource getDiagramResource() {
        return model.listResourcesWithProperty(RDF.type, E3value.diagram).next();
    }

    private Resource getMoneyResource() {
        ResIterator iter = model.listResourcesWithProperty(RDF.type, E3value.value_object);
        while (iter.hasNext()) {
            Resource valueObject = iter.next();

            String valueObjectString = valueObject.getProperty(E3value.e3_has_name).getLiteral().toString();
            //and if that value object is money
            if (valueObjectString.equals("MONEY")) {
                return valueObject;
            }
        }
        return null;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String newDescription) {
        description = newDescription;
    }

    public void appendDescription(String newDescription) {
        if (description.length() < 5) {
            description = newDescription;
        } else {
            description += "<br>" + newDescription;
        }
    }

    /**
     * return a set of start-stimuli
     *
     * @return
     */
    public Set<Resource> getNeeds() {
        // select all the resources with a E3value.elementary_actor property
        ResIterator iter = model.listSubjectsWithProperty(RDF.type, E3value.start_stimulus);
        return iter.toSet();
    }

    /**
     *
     * @return A map of non-duplicate Strings of needs and their respective IDs
     */
    public Map<String, Resource> getNeedsMap() {
        Map<String, Resource> needsMap = new HashMap();
        Set<Resource> needs = getNeeds();
        //temporary occurence interval
        int startValue = 0, endValue = 0;
        //get a list of the needs as Strings
        for (Resource need : needs) {
            String needString = need.getProperty(E3value.e3_has_name).getLiteral().toString();
            //rename duplicate names
            int n = 1;
            if (needsMap.containsKey(needString)) {
                while (needsMap.containsKey(needString.concat("[" + n + "]"))) {
                    n++;
                }
                needString = needString.concat("[" + n + "]");
            }
            needsMap.put(needString, need);
        }
        return needsMap;
    }

    public Set<Resource> getActors() {
        // select all the resources with a ,E3value.elementary_actor property
        ResIterator iter = model.listSubjectsWithProperty(RDF.type, E3value.elementary_actor);
        return iter.toSet();
    }

    public Set<String> getActorStrings() {
        // select all the resources with a ,E3value.elementary_actor property
        ResIterator iter = model.listSubjectsWithProperty(RDF.type, E3value.elementary_actor);
        Set<String> actorStrings = new HashSet<>();
        while (iter.hasNext()) {
            Resource res = iter.next();
            actorStrings.add(res.getProperty(E3value.e3_has_name).getLiteral().toString());
        }
        return actorStrings;
    }

    /**
     *
     * @return A map of non-duplicate Strings of actors and their respective IDs
     */
    public Map<String, Resource> getActorsMap() {
        Map<String, Resource> actorsMap = new HashMap();
        Set<Resource> actors = getActors();
        //get a list of the actors as Strings
        for (Resource need : actors) {
            String actorString = need.getProperty(E3value.e3_has_name).getLiteral().toString();
            //rename duplicate names
            int n = 1;
            if (actorsMap.containsKey(actorString)) {
                while (actorsMap.containsKey(actorString.concat("[" + n + "]"))) {
                    n++;
                }
                actorString = actorString.concat("[" + n + "]");
            }
            actorsMap.put(actorString, need);
        }
        return actorsMap;
    }

    public Set<Resource> getExchanges() {
        // select all the resources with a ,E3value.elementary_actor property
        ResIterator iter = model.listSubjectsWithProperty(RDF.type, E3value.value_exchange);
        return iter.toSet();
    }

    public Set<Resource> getMoneyExchanges() {
        // select all the resources with a ,E3value.elementary_actor property
        ResIterator iter = model.listSubjectsWithProperty(RDF.type, E3value.value_exchange);
        Set<Resource> moneyExchanges = new HashSet<>();
        while (iter.hasNext()) {
            Resource exchange = iter.nextResource();
            if (isMoney(exchange)) {
                moneyExchanges.add(exchange);
            }
        }
        return moneyExchanges;
    }

    private boolean isMoney(Resource exchange) {
        exchange = model.getResource(exchange.getURI());
        //find the value object it belongs to
        Resource port = exchange.getPropertyResourceValue(E3value.ve_has_in_po);
        Resource valueObject = port.getPropertyResourceValue(E3value.vp_requests_offers_vo);
        //if a it has been allocated to a value object
        if (valueObject != null) {
            String valueObjectString = valueObject.getProperty(E3value.e3_has_name).getLiteral().toString();
            //and if that value object is money
            if (valueObjectString.equals("MONEY")) {
                return true;
            }
        }
        return false;
    }

    public boolean changeNeedOccurrence(Resource need, double occurrence) {
        //first, check if input is really a need:
        if (!need.hasProperty(RDF.type, E3value.start_stimulus)) {
            System.err.println("Not a need!");
            return false;
        }
        Statement formula = need.getProperty(E3value.e3_has_formula);
        formula.changeObject("OCCURRENCES=" + occurrence);
        return true;
    }

    /**
     * For a given actor, calculates total profit for the dependency path
     * corresponding to given valueInterface by taking into account valuation of
     * each individual (in/out) value port, as well as the cardinality of the
     * respective (in/out) transactions
     *
     * @param actor the actor to calculate the profit for
     * @param selectedValueInterface the ValueInterface that is used as filter
     * @param ideal whether or not the calculation should be done for the
     * expected case (taking into consideration only solid and dotted lines ) or
     * for the real case (taking into consideration only solid and dashed
     * lines). True means expected. False means real.
     * @return the profit for that actor in the selected case
     */
    public double getTotalForActorPerOccurence(Resource actor, Resource selectedValueInterface, boolean ideal) {
        double result = 0;
        actor = model.getResource(actor.getURI());

        //as preparation, select the valueInterfaces that the calculation is to be done for
        Set<Resource> selectedValueInterfaces = new HashSet<>();
        selectedValueInterfaces.add(selectedValueInterface);
        Resource nextElement;
        if (selectedValueInterface.hasProperty(E3value.de_down_ce)) {
            nextElement = selectedValueInterface.getProperty(E3value.de_down_ce).getResource().getProperty(E3value.ce_with_down_de).getResource();
            // if next element is aa value interface
            if (nextElement.hasProperty(RDF.type, E3value.value_interface)) {
                //just add it
                selectedValueInterfaces.add(nextElement);
            }
            //in case the next element is not a value interface (but an AND/OR/EXPLOSION node)
            if (!nextElement.hasProperty(RDF.type, E3value.value_interface)) {
                //get all its next elements
                StmtIterator ce = selectedValueInterface.listProperties(E3value.de_down_ce);
                while (ce.hasNext()) {
                    //and add them
                    nextElement = ce.next().getResource().getProperty(E3value.ce_with_down_de).getResource();
                    if (nextElement.hasProperty(RDF.type, E3value.value_interface)) {
                        selectedValueInterfaces.add(nextElement);
                    }
                }
            }
        } else if (selectedValueInterface.hasProperty(E3value.de_up_ce)) {
            nextElement = selectedValueInterface.getProperty(E3value.de_up_ce).getResource().getProperty(E3value.ce_with_up_de).getResource();
            // if next element is aa value interface
            if (nextElement.hasProperty(RDF.type, E3value.value_interface)) {
                //just add it
                selectedValueInterfaces.add(nextElement);
            }
            //in case the next element is not a value interface (but an AND/OR/EXPLOSION node)
            if (!nextElement.hasProperty(RDF.type, E3value.value_interface)) {
                //get all its next elements
                StmtIterator elementsIter = selectedValueInterface.listProperties(E3value.de_up_ce);
                while (elementsIter.hasNext()) {
                    //and add them
                    nextElement = elementsIter.next().getResource().getProperty(E3value.ce_with_up_de).getResource();
                    if (nextElement.hasProperty(RDF.type, E3value.value_interface)) {
                        selectedValueInterfaces.add(nextElement);
                    }
                }
            }
        }

        //First, check if input is really an actor:
        if (!actor.hasProperty(RDF.type, E3value.elementary_actor)) {
            System.err.println("Not an actor!");
            return 0;
        }

        //Second, take into account Investment, Expenses and Interest:
        if (actor.hasProperty(E3value.e3_has_formula)) {
            //get list of formulas (INVESTMENT, EXPENSES, INTEREST)
            StmtIterator actorFormulas = actor.listProperties(E3value.e3_has_formula);
            while (actorFormulas.hasNext()) {
                Statement formula = actorFormulas.next();
                double value = Float.valueOf(formula.getString().split("=", 2)[1]);
                result -= value;
            }
        }

        //Third, calculate the income/loss per Value Interface:
        //if actor has a Value Interfaces
        if (actor.hasProperty(E3value.ac_has_vi)) {
            //get list of ValueInterfaces
            StmtIterator actorValueInterfaces = actor.listProperties(E3value.ac_has_vi);
            //for each VAlueInterface,
            while (actorValueInterfaces.hasNext()) {
                Statement valueInterface = actorValueInterfaces.next();
                //if it is part of the desired ones 
                if (selectedValueInterfaces.contains(valueInterface.getResource())) {
                    double occurences = 0;
                    //get number of occurences
                    StmtIterator valueInterfaceFormulas = valueInterface.getResource().listProperties(E3value.e3_has_formula);
                    while (valueInterfaceFormulas.hasNext()) {
                        Statement formula = valueInterfaceFormulas.next();
                        if (formula.getString().split("=", 2)[0].equals("OCCURRENCES")) {
                            occurences = Float.valueOf(formula.getString().split("=", 2)[1]);
                        }
                    }
                    //and then get ValueOfferings (groups of equally directed ports)
                    StmtIterator valueOfferings = valueInterface.getResource().listProperties(E3value.vi_consists_of_of);
                    //and for each ValueOffering (group of equally directed ports)
                    while (valueOfferings.hasNext()) {
                        Statement valueOffering = valueOfferings.next();
                        //determine valuation
                        //by getting a list of it's individual ports
                        double valueOfferingValuation = 0;
                        StmtIterator valuePorts = valueOffering.getResource().listProperties(E3value.vo_consists_of_vp);
                        //and for each valuePort
                        while (valuePorts.hasNext()) {
                            Statement valuePort = valuePorts.next();
                            //determine value
                            //by getting list of EXPENSES, VALUATION,
                            double valuePortValuation = 0;
                            StmtIterator valuePortFormulas = valuePort.getResource().listProperties(E3value.e3_has_formula);
                            //adding them up
                            while (valuePortFormulas.hasNext()) {
                                Statement formula = valuePortFormulas.next();
                                double value = Float.valueOf(formula.getString().split("=", 2)[1]);
                                valuePortValuation += value;
                            }

                            //multiplying it with the CARDINALITY of the Port's associated Value Exchange and nullifying if necessary
                            if (valuePort.getResource().hasProperty(E3value.vp_out_connects_ve)) {//if it's an (outgoing) ValuePort
                                if (ideal == true) {//If we want the expected case
                                    if (isDashed(valuePort.getResource().getProperty(E3value.vp_out_connects_ve).getResource())) {//and it's respective (outgoing) Value Exchange is dashed
                                        valuePortValuation = 0; //nullify it
                                    }
                                } else//If we want the real case
                                if (isDotted(valuePort.getResource().getProperty(E3value.vp_out_connects_ve).getResource())) {//and it's respective (outgoing) Value Exchange is dotted
                                    valuePortValuation = 0; //nullify it
                                }
                                valuePortValuation *= getCardinality(valuePort.getResource().getProperty(E3value.vp_out_connects_ve).getResource());// then multiply with cardinality of respective (outgoing) ve                             
                            } else if (valuePort.getResource().hasProperty(E3value.vp_in_connects_ve)) {//if it's an (incoming) ValuePort
                                if (ideal == true) {//If we want the expected case
                                    if (isDashed(valuePort.getResource().getProperty(E3value.vp_in_connects_ve).getResource())) {//and it's respective (incoming) Value Exchange is dashed
                                        valuePortValuation = 0; //nullify it
                                    }
                                } else//If we want the real case
                                if (isDotted(valuePort.getResource().getProperty(E3value.vp_in_connects_ve).getResource())) {//and it's respective (incoming) Value Exchange is dotted
                                    valuePortValuation = 0; //nullify it
                                }
                                valuePortValuation *= getCardinality(valuePort.getResource().getProperty(E3value.vp_in_connects_ve).getResource());// then multiply with cardinality of respective (incoming) ve
                            }

                            //and then multiplying them with the occurence rate
                            //valuePortValuation = valuePortValuation * occurences;
                            //add it up to determine valueOfferingValuation
                            valueOfferingValuation += valuePortValuation;
                        }
                        //finally, if it was an incoming ValueOFfering,
                        if (valueOffering.getResource().getProperty(E3value.e3_has_name).getString().equals("in")) {
                            //add it to the actors profit.
                            result += valueOfferingValuation;
                            //else, if it was outgoing,
                        } else {
                            //deduct it from the actor's profit.
                            result -= valueOfferingValuation;
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * For a given actor, calculates total profit, by taking into account the
     * number of occurences and valuation of each individual (in/out) value
     * port, as well as the cardinality of the respective (in/out) transactions
     *
     * @param actor the actor to calculate the profit for
     * @param ideal whether or not the calculation should be done for the
     * expected case (taking into consideration only solid and dotted lines ) or
     * for the real case (taking into consideration only solid and dashed
     * lines). True means expected. False means real.
     * @return the profit for that actor in the selected case
     */
    public double getTotalForActor(Resource actor, boolean ideal) {
        double result = 0;
        actor = model.getResource(actor.getURI());

        //First, check if input is really an actor:
        if (!actor.hasProperty(RDF.type, E3value.elementary_actor)) {
            System.err.println("Not an actor!");
            return 0;
        }

        //Second, take into account Investment, Expenses and Interest:
        if (actor.hasProperty(E3value.e3_has_formula)) {
            //get list of formulas (INVESTMENT, EXPENSES, INTEREST)
            StmtIterator actorFormulas = actor.listProperties(E3value.e3_has_formula);
            while (actorFormulas.hasNext()) {
                Statement formula = actorFormulas.next();
                double value = Float.valueOf(formula.getString().split("=", 2)[1]);
                result -= value;
            }
        }

        //Third, calculate the income/loss per Value Interface:
        //if actor has a Value Interfaces
        if (actor.hasProperty(E3value.ac_has_vi)) {
            //get list of ValueInterfaces
            StmtIterator actorValueInterfaces = actor.listProperties(E3value.ac_has_vi);
            //for each VAlueInterface,
            while (actorValueInterfaces.hasNext()) {
                Statement valueInterface = actorValueInterfaces.next();
                double occurences = 0;
                //get number of occurences
                StmtIterator valueInterfaceFormulas = valueInterface.getResource().listProperties(E3value.e3_has_formula);
                while (valueInterfaceFormulas.hasNext()) {
                    Statement formula = valueInterfaceFormulas.next();
                    if (formula.getString().split("=", 2)[0].equals("OCCURRENCES")) {
                        occurences = Float.valueOf(formula.getString().split("=", 2)[1]);
                    }
                }
                //and then get ValueOfferings (groups of equally directed ports)
                StmtIterator valueOfferings = valueInterface.getResource().listProperties(E3value.vi_consists_of_of);
                //and for each ValueOffering (group of equally directed ports)
                while (valueOfferings.hasNext()) {
                    Statement valueOffering = valueOfferings.next();
                    //determine valuation
                    //by getting a list of it's individual ports
                    double valueOfferingValuation = 0;
                    StmtIterator valuePorts = valueOffering.getResource().listProperties(E3value.vo_consists_of_vp);
                    //and for each valuePort
                    while (valuePorts.hasNext()) {
                        Statement valuePort = valuePorts.next();
                        //determine value
                        //by getting list of EXPENSES, VALUATION,
                        double valuePortValuation = 0;
                        StmtIterator valuePortFormulas = valuePort.getResource().listProperties(E3value.e3_has_formula);
                        //adding them up
                        while (valuePortFormulas.hasNext()) {
                            Statement formula = valuePortFormulas.next();
                            double value = Float.valueOf(formula.getString().split("=", 2)[1]);
                            valuePortValuation += value;
                        }

                        //multiplying it with the CARDINALITY of the Port's associated Value Exchange and nullifying if necessary
                        if (valuePort.getResource().hasProperty(E3value.vp_out_connects_ve)) {//if it's an (outgoing) ValuePort
                            if (ideal == true) {//If we want the expected case
                                if (isDashed(valuePort.getResource().getProperty(E3value.vp_out_connects_ve).getResource())) {//and it's respective (outgoing) Value Exchange is dashed
                                    valuePortValuation = 0; //nullify it
                                }
                            } else//If we want the real case
                            if (isDotted(valuePort.getResource().getProperty(E3value.vp_out_connects_ve).getResource())) {//and it's respective (outgoing) Value Exchange is dotted
                                valuePortValuation = 0; //nullify it
                            }
                            valuePortValuation *= getCardinality(valuePort.getResource().getProperty(E3value.vp_out_connects_ve).getResource());// then multiply with cardinality of respective (outgoing) ve                             
                        } else if (valuePort.getResource().hasProperty(E3value.vp_in_connects_ve)) {//if it's an (incoming) ValuePort
                            if (ideal == true) {//If we want the expected case
                                if (isDashed(valuePort.getResource().getProperty(E3value.vp_in_connects_ve).getResource())) {//and it's respective (incoming) Value Exchange is dashed
                                    valuePortValuation = 0; //nullify it
                                }
                            } else//If we want the real case
                            if (isDotted(valuePort.getResource().getProperty(E3value.vp_in_connects_ve).getResource())) {//and it's respective (incoming) Value Exchange is dotted
                                valuePortValuation = 0; //nullify it
                            }
                            valuePortValuation *= getCardinality(valuePort.getResource().getProperty(E3value.vp_in_connects_ve).getResource());// then multiply with cardinality of respective (incoming) ve
                        }

                        //and then multiplying them with the occurence rate
                        valuePortValuation = valuePortValuation * occurences;
                        //add it up to determine valueOfferingValuation
                        valueOfferingValuation += valuePortValuation;
                    }
                    //finally, if it was an incoming ValueOFfering,
                    if (valueOffering.getResource().getProperty(E3value.e3_has_name).getString().equals("in")) {
                        //add it to the actors profit.
                        result += valueOfferingValuation;
                        //else, if it was outgoing,
                    } else {
                        //deduct it from the actor's profit.
                        result -= valueOfferingValuation;
                    }
                }
            }
        }
        return result;
    }

    /**
     *
     * @param actor the actor to plot the graph for
     * @param need the need to use on X-axis
     * @param startValue minimum occurrence rate of need
     * @param endValue maximum occurence rate of need
     * @param ideal ideal or sub-ideal case
     * @return an XY series representing the profitability graph of the selected
     * actor. The X-axis represented the number of occurrences of need (in the
     * interval startValue endValue) and the Y-axis represents the profit
     */
    public XYSeries getTotalForActor(Resource actor, Resource need, int startValue, int endValue, boolean ideal) {
        XYSeries actorSeries = new XYSeries(actor.getProperty(E3value.e3_has_name).getString());

        //make sure the resources are from this model
        actor = model.getResource(actor.getURI());
        need = model.getResource(need.getURI());
        try {
            /* Step - 1: Define the data for the series  */
            //we only need 50 values so divide interval to 50
            double step = (endValue - (double) startValue) / 50;
            //calculate profit for each (occurence) value:
            for (double i = startValue; i < endValue; i += step) {
                this.changeNeedOccurrence(need, i);
                this.enhance();
                //add it's profit to the relevant series
                actorSeries.add(i, this.getTotalForActor(actor, ideal));
            }
        } catch (Exception i) {
            System.err.println(i);
        }
        return actorSeries;
    }

    /**
     *
     * @param need the need to use on X-axis
     * @param startValue minimum occurrence rate of need
     * @param endValue maximum occurrence rate of need
     * @param ideal ideal or sub-ideal case
     * @return an XY series representing the profitability graph of the selected
     * actor. The X-axis represented the number of occurrences of need (in the
     * interval startValue endValue) and the Y-axis represents the profit
     */
    public Map<Resource, XYSeries> getTotalForActors(Resource need, int startValue, int endValue, boolean ideal) {
        //make sure the resources are from this model
        need = model.getResource(need.getURI());
        Map<Resource, XYSeries> actorSeriesMap = new HashMap();
        Set<Resource> actors = this.getActors();
        for (Resource actor : actors) {
            XYSeries actorSeries = new XYSeries(actor.getProperty(E3value.e3_has_name).getString());
            actorSeriesMap.put(actor, actorSeries);
        }

        try {
            /* Step - 1: Define the data for the series  */
            //we only need 50 values so divide interval to 50
            double step = (endValue - (double) startValue) / 50;
            //calculate profit for each (occurence) value:
            for (double i = startValue; i < endValue; i += step) {
                this.changeNeedOccurrence(need, i);
                this.enhance();
                //For each actor
                for (Resource actor : actors) {
                    //add it's profit to the relevant series
                    actorSeriesMap.get(actor).add(i, this.getTotalForActor(actor, ideal));
                }
            }
            if (startValue == endValue) {
                double i = startValue;
                this.changeNeedOccurrence(need, i);
                this.enhance();
                //For each actor
                for (Resource actor : actors) {
                    //add it's profit to the relevant series
                    actorSeriesMap.get(actor).add(i, this.getTotalForActor(actor, ideal));
                }
            }
        } catch (Exception i) {
            System.err.println(i);
        }
        return actorSeriesMap;
    }

    /**
     *
     * @param need the need to use on X-axis
     * @param startValue minimum occurrence rate of need
     * @param endValue maximum occurrence rate of need
     * @param ideal ideal or sub-ideal case
     * @return an XY series representing the profitability graph of the selected
     * actor. The X-axis represented the number of occurrences of need (in the
     * interval startValue endValue) and the Y-axis represents the profit
     */
    public CategoryDataset getTotalsForActors(Resource need, int startValue, int endValue, boolean ideal) {
        //make sure the resources are from this model
        need = model.getResource(need.getURI());

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        Set<Resource> actors = this.getActors();

        try {
            /* Step - 1: Define the data for the series  */
            //we only need 50 values so divide interval to 50
            double step = (endValue - (double) startValue) / 50;
            //calculate profit for each (occurence) value:
            for (double i = startValue; i < endValue; i += step) {
                this.changeNeedOccurrence(need, i);
                this.enhance();
                //For each actor
                for (Resource actor : actors) {
                    //add it's profit to the relevant series
                    dataset.addValue(this.getTotalForActor(actor, ideal), actor.getProperty(E3value.e3_has_name).getString(), "\"" + i + "\"");
                }
            }
        } catch (Exception i) {
            System.err.println(i);
        }

        return dataset;

    }

    @Override
    public String toString() {
        //boring decalarations
        return description;
    }

    public double getCardinality(Resource res) {
        //make sure the resources are from this model
        res = model.getResource(res.getURI());
        double cardinality = 1;
        StmtIterator resFormulas = res.listProperties(E3value.e3_has_formula);
        while (resFormulas.hasNext()) {
            Statement formula = resFormulas.next();
            if (formula.getString().split("=", 2)[0].equals("CARDINALITY")) {
                cardinality = Float.valueOf(formula.getString().split("=", 2)[1]);
            }
        }
        return cardinality;
    }

    private boolean isDotted(Resource valueExchange) {
        Resource exchange = model.getResource(valueExchange.getURI());
        StmtIterator veFormulas = exchange.listProperties(E3value.e3_has_formula);
        while (veFormulas.hasNext()) {
            Statement formula = veFormulas.next();
            if (formula.getString().split("=", 2)[0].equals("DOTTED")) {
                return true;
            }
        }
        return false;
    }

    private boolean isDashed(Resource valueExchange) {
        //make sure the resources are from this model
        valueExchange = model.getResource(valueExchange.getURI());
        StmtIterator veFormulas = valueExchange.listProperties(E3value.e3_has_formula);
        while (veFormulas.hasNext()) {
            Statement formula = veFormulas.next();
            if (formula.getString().split("=", 2)[0].equals("DASHED")) {
                return true;
            }
        }
        return false;
    }

    public void makeDashed(Resource valueExchange) {
        valueExchange = model.getResource(valueExchange.getURI());        //make sure the resources are from this model
        valueExchange.addProperty(E3value.e3_has_formula, "DASHED=1");
    }

    public void makeDotted(Resource valueExchange) {
        valueExchange = model.getResource(valueExchange.getURI());        //make sure the resources are from this model
        valueExchange.addProperty(E3value.e3_has_formula, "DOTTED=1");
        this.fraudChanges.addNonOccurringTransaction(valueExchange.getProperty(E3value.e3_has_uid).getLong());
    }

    public int countDotted() {
        ResIterator resIterator = model.listResourcesWithProperty(E3value.e3_has_formula);
        int i = 0;
        while (resIterator.hasNext()) {
            Resource resource = resIterator.next();
            if (isDotted(resource)) {
                i++;
            }

        }
        return i;
    }

    /**
     * Adds a transfer from interface 1 of actor1 to interface2 of actor2 of the
     * indicated value.
     *
     * CAUTION: interface1 must belong to actor1 and interface2 must belong to
     * actor2
     *
     *
     * @param valueInterface1 the Value Interface that holds the outgoingport
     * @param valueInterface2 the ValueInterface that will hold the incoming
     * @param value
     */
    public void addTransfer(Resource valueInterface1, Resource valueInterface2, double value) {
        //make sure the resources are from this model
        valueInterface1 = model.getResource(valueInterface1.getURI());
        valueInterface2 = model.getResource(valueInterface2.getURI());

        Resource port1, port2, exchange;
        String URIbase = this.getActors().iterator().next().getURI().split("#")[0];
        int id = model.listStatements().toSet().size();
        //System.out.println(URIbase + " " + id);
        Resource modelResource = this.getModelResource();
        Resource moneyResource = this.getMoneyResource();
        Resource diagramResource = this.getDiagramResource();
        StmtIterator valueOfferings1 = valueInterface1.listProperties(E3value.vi_consists_of_of);
        StmtIterator valueOfferings2 = valueInterface2.listProperties(E3value.vi_consists_of_of);
        Resource vo1 = null, vo2 = null;
        while (valueOfferings1.hasNext()) {
            Resource vo = valueOfferings1.next().getResource();
            if (vo.hasProperty(RDF.type, E3value.value_offering) && vo.getProperty(E3value.e3_has_name).getString().equals("out")) {
                vo1 = vo;
            }
        }
        while (valueOfferings2.hasNext()) {
            Resource vo = valueOfferings2.next().getResource();
            if (vo.hasProperty(RDF.type, E3value.value_offering) && vo.getProperty(E3value.e3_has_name).getString().equals("in")) {
                vo2 = vo;
            }
        }

        port1 = model.createResource(URIbase + "#" + Integer.toString(id), E3value.value_port);
        port1.addProperty(E3value.e3_has_name, "vp" + Integer.toString(id));
        port1.addProperty(E3value.e3_has_uid, Integer.toString(id));
        port1.addProperty(E3value.vp_has_dir, "true");
        port1.addProperty(E3value.mc_in_mo, modelResource);
        modelResource.addProperty(E3value.mo_has_mc, port1);
        //System.out.println("MONEY RESOURCE: " + moneyResource);
        port1.addProperty(E3value.vp_requests_offers_vo, moneyResource);
        moneyResource.addProperty(E3value.vo_offered_requested_by_vp, port1);
        port1.addProperty(E3value.mc_in_di, diagramResource);
        diagramResource.addProperty(E3value.mo_has_mc, port1);
        port1.addProperty(E3value.vp_in_vo, vo1);
        vo1.addProperty(E3value.vo_consists_of_vp, port1);
        port1.addProperty(E3value.e3_has_formula, "VALUATION=" + value);

        id += 1;
        port2 = model.createResource(URIbase + "#" + Integer.toString(id), E3value.value_port);
        port2.addProperty(E3value.e3_has_name, "vp" + Integer.toString(id));
        port2.addProperty(E3value.e3_has_uid, Integer.toString(id));
        port2.addProperty(E3value.vp_has_dir, "true");
        port2.addProperty(E3value.mc_in_mo, modelResource);
        modelResource.addProperty(E3value.mo_has_mc, port2);
        port2.addProperty(E3value.vp_requests_offers_vo, moneyResource);
        moneyResource.addProperty(E3value.vo_offered_requested_by_vp, port2);
        port2.addProperty(E3value.mc_in_di, diagramResource);
        diagramResource.addProperty(E3value.mo_has_mc, port2);
        port2.addProperty(E3value.vp_in_vo, vo2);
        vo2.addProperty(E3value.vo_consists_of_vp, port2);
        port2.addProperty(E3value.e3_has_formula, "VALUATION=" + value);

        id += 1;
        exchange = model.createResource(URIbase + "#" + Integer.toString(id), E3value.value_exchange);
        exchange.addProperty(E3value.e3_has_name, "Hidden transfer");
        exchange.addProperty(E3value.e3_has_uid, Integer.toString(id));
        exchange.addProperty(E3value.mc_in_mo, modelResource);
        modelResource.addProperty(E3value.mo_has_mc, exchange);
        moneyResource.addProperty(E3value.mo_has_mc, exchange);
        exchange.addProperty(E3value.ve_has_in_po, port2);
        exchange.addProperty(E3value.ve_has_out_po, port1);
        exchange.addProperty(E3value.e3_has_formula, "CARDINALITY=1");

        port1.addProperty(E3value.vp_out_connects_ve, exchange);

        port2.addProperty(E3value.vp_in_connects_ve, exchange);

    }

    /**
     * Turns actor1 into actor1+actor2 and deletes actor 2
     *
     * @param actor1
     * @param actor2
     */
    void collude(Resource actor1, Resource actor2) {

        int actor1ID = Integer.parseInt(actor1.getProperty(E3value.e3_has_uid).getString());
        int actor2ID = Integer.parseInt(actor2.getProperty(E3value.e3_has_uid).getString());
        this.fraudChanges.addColludedActor(actor1ID);
        this.fraudChanges.addColludedActor(actor2ID);

        //make sure the resources are from this model
        actor1 = model.getResource(actor1.getURI());
        actor2 = model.getResource(actor2.getURI());

        //First, merge their names
        String actor1Name = actor1.getProperty(E3value.e3_has_name).getString();
        String actor2Name = actor2.getProperty(E3value.e3_has_name).getString();
        actor1.getProperty(E3value.e3_has_name).changeObject(actor1Name + " + " + actor2Name);

        //Next add up their expenses and investment
        if (actor1.hasProperty(E3value.e3_has_formula)) {
            //get list of formulas (INVESTMENT, EXPENSES, INTEREST) of the actors
            StmtIterator actor1Formulas = actor1.listProperties(E3value.e3_has_formula);
            Set<String> newActor1Formulas = new HashSet<>();
            StmtIterator actor2Formulas = actor2.listProperties(E3value.e3_has_formula);
            //and for each one 
            while (actor1Formulas.hasNext()) {
                Statement actor1Formula = actor1Formulas.next();
                String actor1FormulaType = actor1Formula.getString().split("=", 2)[0];
                double actor1FormulaValue = Float.parseFloat(actor1Formula.getString().split("=", 2)[1]);
                //check if actor2 has a matching one
                while (actor2Formulas.hasNext()) {
                    Statement actor2Formula = actor2Formulas.next();
                    String actor2FormulaType = actor2Formula.getString().split("=", 2)[0];
                    double actor2FormulaValue = Float.parseFloat(actor2Formula.getString().split("=", 2)[1]);
                    //if it does
                    if (actor1FormulaType.equals(actor2FormulaType)) {
                        //add them up                        
                        actor1FormulaValue = actor1FormulaValue + actor2FormulaValue;

                    }
                    //save the (updated or not) formula
                    newActor1Formulas.add(actor1FormulaType + "=" + actor1FormulaValue);
                }
            }
            //remove the old ones
            actor1.removeAll(E3value.e3_has_formula);
            //add the new
            for (String formula : newActor1Formulas) {
                actor1.addProperty(E3value.e3_has_formula, formula);
            }
        }

        //Then, copy all of actor2's value interfaces to actor1
        StmtIterator actor2ValueInterfaces = actor2.listProperties(E3value.ac_has_vi);
        //for each value interface of actor2
        while (actor2ValueInterfaces.hasNext()) {
            Statement actor2Interface = actor2ValueInterfaces.next();
            //add it to actor 1
            actor1.addProperty(E3value.ac_has_vi, actor2Interface.getObject());
            //and update its reference
            actor2Interface.getResource().getProperty(E3value.vi_assigned_to_ac).changeObject((RDFNode) actor1);
        }

        colludedActorURI = actor2.getURI();
        newActorURI = actor1.getURI();

        //and remove them from actor2
        actor2.removeAll(E3value.ac_has_vi);

        //Finally, delete actor2      
        actor2.removeProperties();

    }

    /**
     * Returns unique pairs of interfaces (one from each actor) that contain
     * transfers between the two actors. Direction is ignored.
     *
     * @param actor1
     * @param actor2
     * @return
     */
    public Map<Resource, Resource> getInterfacesBetween(Resource actor1, Resource actor2) {
        Map<Resource, Resource> interfaces = new HashMap<>();
        //make sure the resources are from this model
        actor1 = model.getResource(actor1.getURI());
        actor2 = model.getResource(actor2.getURI());

        //get value interfaces of actor1
        StmtIterator viStatements1 = actor1.listProperties(E3value.ac_has_vi);
        //and for each one
        while (viStatements1.hasNext()) {
            Statement viStatement1 = viStatements1.next();
            Resource vi1 = viStatement1.getObject().asResource();
            //get its value offerings (group of equally directed ports)
            StmtIterator vofStatements1 = vi1.listProperties(E3value.vi_consists_of_of);
            //and for each value offering
            while (vofStatements1.hasNext()) {
                Statement vofStatement1 = vofStatements1.next();
                Resource vof = vofStatement1.getObject().asResource();
                //get its ports
                StmtIterator vpStatements1 = vof.listProperties(E3value.vo_consists_of_vp);
                //and for each port
                while (vpStatements1.hasNext()) {
                    Statement vpStatement1 = vpStatements1.next();
                    Resource vp = vpStatement1.getObject().asResource();
                    Resource ve;
                    Resource vi2;
                    //if it's an (outgoing) ValuePort
                    if (vp.hasProperty(E3value.vp_out_connects_ve)) {
                        ve = vp.getProperty(E3value.vp_out_connects_ve).getResource();//choose it's respective Value Exchange                                                   
                        vp = ve.getProperty(E3value.ve_has_in_po).getResource();//and get the ValuPort on the other end
                        //Now check if it belongs to actor2
                        vof = vp.getProperty(E3value.vp_in_vo).getResource();
                        vi2 = vof.getProperty(E3value.vo_in_vi).getResource();
                        if (actor2.hasProperty(E3value.ac_has_vi, (RDFNode) vi2)) {
                            //and if it does, add the pair to the result
                            interfaces.put(vi1, vi2);
                            break;
                        }
                    } //if it's an (incoming) ValuePort
                    else if (vp.hasProperty(E3value.vp_in_connects_ve)) {
                        ve = vp.getProperty(E3value.vp_in_connects_ve).getResource();//choose it's respective Value Exchange                            
                        vp = ve.getProperty(E3value.ve_has_out_po).getResource();//and get the ValuePort on the other end
                        //Now check if it belongs to actor2
                        vof = vp.getProperty(E3value.vp_in_vo).getResource();
                        vi2 = vof.getProperty(E3value.vo_in_vi).getResource();
                        if (actor2.hasProperty(E3value.ac_has_vi, (RDFNode) vi2)) {
                            //and if it does, add the pair to the result
                            interfaces.put(vi1, vi2);
                            break;
                        }
                    }

                }
            }

        }

        return interfaces;

    }

    /**
     * Average for individual actor
     *
     * @param actor
     * @param need
     * @param startValue
     * @param endValue
     * @param ideal
     * @return
     */
    public double getAverageForActor(Resource actor, Resource need, int startValue, int endValue, boolean ideal) {

        actor = model.getResource(actor.getURI());
        need = model.getResource(need.getURI());

        XYSeries series = getTotalForActor(actor, need, startValue, endValue, ideal);
        double sum = 0;
        for (Object dataItemObject : series.getItems()) {
            XYDataItem dataItem = (XYDataItem) dataItemObject;
            sum += dataItem.getY().doubleValue();
        }
        double mean = sum / series.getItemCount();
        this.lastKnownAverage = mean;
        return mean;
    }

    /**
     * Averages for all actors
     *
     * @param actor main actor
     * @param need
     * @param startValue
     * @param endValue
     * @param ideal
     * @return
     */
    public Map<Resource, Double> getAveragesForActors(Resource need, int startValue, int endValue, boolean ideal) {

        need = model.getResource(need.getURI());
        Map<Resource, Double> averagesMap = new HashMap<>();
        Map<Resource, XYSeries> seriesMap = getTotalForActors(need, startValue, endValue, ideal);

        for (Resource actor : seriesMap.keySet()) {
            double sum = 0;
            for (Object dataItemObject : seriesMap.get(actor).getItems()) {
                XYDataItem dataItem = (XYDataItem) dataItemObject;
                sum += dataItem.getY().doubleValue();
            }
            double mean = sum / seriesMap.get(actor).getItemCount();
            averagesMap.put(actor, mean);
        }
        this.lastKnownAverages = averagesMap;
        return averagesMap;
    }

    public Map<Resource, Double> getLastKnownAverages() {
        return this.lastKnownAverages;
    }

}
