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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

import design.Utils;
import e3fraud.gui.PopUps;
import e3fraud.vocabulary.E3value;
import java.text.DecimalFormat;
import java.util.Optional;

/**
 *
 * @author IonitaD
 */
public class E3Model {

    private boolean debug = false; //use to toggle printing traversal steps

    private final Model model;
    private String description;
    private boolean isFraud;
    private Utils.GraphDelta fraudChanges;
    private String prefix;
    private ExpressionEvaluator evaluatedModel;

//constructors
    /**
     * Create e new E3Model object containing the jenaModel
     *
     * @param jenaModel the Jena model to use as a source
     */
    public E3Model(Model jenaModel) {
        this.model = jenaModel;
        this.evaluatedModel = ExpressionEvaluator.evaluateModel(model).get();
        this.prefix = "";
        this.description = "Base Model";
        this.fraudChanges = null;
    }

    /**
     * Create a new E3Model object containing the jenaModel and metadata from a
     * different E3Model. This method is used when wanting to duplicate an
     * E3Model.
     *
     * @param baseModel the E3model to take description and collusion info from
     */
    public E3Model(E3Model baseModel) {
        Model newJenaModel = ModelFactory.createDefaultModel();
        newJenaModel.add(baseModel.getJenaModel());
        this.model = newJenaModel;
        this.prefix = "";
        this.description = baseModel.getDescription();
        this.evaluatedModel = null;
    }

//getters and setters
    public Model getJenaModel() {
        return model;
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

    public boolean isFraud() {
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

    @Override
    public String toString() {
        //boring decalarations
        return description;
    }

/// Methods for interacting with the underlying Jena (RDF) model 
    private Resource getModelResource() {
        return model.listResourcesWithProperty(RDF.type, E3value.model).next();
    }

    private Resource getDiagramResource() {
        return model.listResourcesWithProperty(RDF.type, E3value.diagram).next();
    }

    /**
     * Returns the Resource corresponding MONEY ValueObjects
     *
     * @return the Resource corresponding to the MONEY ValueObject from the Jena
     * model of this E3Model
     */
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

    /**
     * Returns a set containing all Start Stimuli (Needs)
     *
     * @return a set of all start-stimuli elements found in the underlying Jena
     * model
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
        // select all the resources with a ,E3value.elementary_actor
        ResIterator elementaryActorIter = model.listSubjectsWithProperty(RDF.type, E3value.elementary_actor);
        Set<Resource> actorSet = elementaryActorIter.toSet();
        return actorSet;
    }

    public Set<Resource> getActorsAndMarketSegments() {
        // select all the resources with a ,E3value.elementary_actor
        ResIterator elementaryActorIter = model.listSubjectsWithProperty(RDF.type, E3value.elementary_actor);
        ResIterator msIter = model.listSubjectsWithProperty(RDF.type, E3value.market_segment);
        Set<Resource> actorSet = elementaryActorIter.toSet();
        actorSet.addAll(msIter.toSet());
        return actorSet;
    }

    public Set<Resource> getMarketSegments() {
        // select all the resources with a ,E3value.elementary_actor
        ResIterator msIter = model.listSubjectsWithProperty(RDF.type, E3value.market_segment);
        Set msSet = msIter.toSet();
        return msSet;
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

    public Set<String> getValueObjectStrings() {
        // select all the resources with a ,E3value.value_object property
        ResIterator iter = model.listSubjectsWithProperty(RDF.type, E3value.value_object);
        Set<String> actorStrings = new HashSet<>();
        while (iter.hasNext()) {
            Resource res = iter.next();
            actorStrings.add(res.getProperty(E3value.e3_has_name).getLiteral().toString());
        }
        return actorStrings;
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
     * Returns all actor Resources + their names
     *
     * @return A map of non-duplicate Strings of actors and their respective IDs
     */
    public HashMap<String, Resource> getActorsMap() {
        HashMap<String, Resource> actorsMap = new HashMap();
        Set<Resource> actors = getActorsAndMarketSegments();
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

    /**
     * Returns all actor Resources + their names
     *
     * @return A map of non-duplicate Strings of actors and their respective IDs
     */
    public Map<String, Resource> getMSMap() {
        Map<String, Resource> msMap = new HashMap();
        Set<Resource> marketSegments = getMarketSegments();
        //get a list of the actors as Strings
        for (Resource marketSegment : marketSegments) {
            String marketSegmentString = marketSegment.getProperty(E3value.e3_has_name).getLiteral().toString();
            //rename duplicate names
            int n = 1;
            if (msMap.containsKey(marketSegmentString)) {
                while (msMap.containsKey(marketSegmentString.concat("[" + n + "]"))) {
                    n++;
                }
                marketSegmentString = marketSegmentString.concat("[" + n + "]");
            }
            msMap.put(marketSegmentString, marketSegment);
        }
        return msMap;
    }

    /**
     * Returns all Value Exchange objects
     *
     * @return a set of resources corresponding to Value Exchange elements of
     * the underlying Jena (RDF) model
     */
    public Set<Resource> getExchanges() {
        // select all the resources with a ,E3value.elementary_actor property
        ResIterator iter = model.listSubjectsWithProperty(RDF.type, E3value.value_exchange);
        return iter.toSet();
    }

    /**
     * Returns all transfers mapped to certain types (i.e. Value Object)
     *
     * @param types a List of ValueObject strings
     * @return all transfers mapped to the requested ValueObjects
     */
    public Set<Resource> getExchangesOfTypes(List<String> types) {
        // select all the resources with a ,E3value.elementary_actor property
        ResIterator iter = model.listSubjectsWithProperty(RDF.type, E3value.value_exchange);
        Set<Resource> moneyExchanges = new HashSet<>();
        while (iter.hasNext()) {
            Resource exchange = iter.nextResource();
            for (String type : types) {
                if (isOfType(exchange, type)) {
                    moneyExchanges.add(exchange);
                }
            }
        }
        return moneyExchanges;
    }

    /**
     * Returns all transfers mapped to certain type (i.e. Value Object)
     *
     * @param type a ValueObject string
     * @return all transfers mapped to the requested ValueObject
     */
    public Set<Resource> getExchangesOfType(String type) {
        // select all the resources with a ,E3value.elementary_actor property
        ResIterator iter = model.listSubjectsWithProperty(RDF.type, E3value.value_exchange);
        Set<Resource> moneyExchanges = new HashSet<>();
        while (iter.hasNext()) {
            Resource exchange = iter.nextResource();
            if (isOfType(exchange, type)) {
                moneyExchanges.add(exchange);
            }
        }
        return moneyExchanges;
    }

    /**
     * Checks if a ValueTransfer is of a certain type (i.e. has a certain
     * ValueObject attached).
     *
     * @param transfer
     * @param type
     * @return true iff the value object attached to the transfer has the same
     * string as the requested type
     */
    private boolean isOfType(Resource transfer, String type) {
        transfer = model.getResource(transfer.getURI());
        Resource port = null;
        //find the value object it belongs to
        if (transfer.hasProperty(E3value.ve_has_in_po)) {
            port = transfer.getPropertyResourceValue(E3value.ve_has_in_po);
        } else if (transfer.hasProperty(E3value.ve_has_first_vp)) {
            port = transfer.getPropertyResourceValue(E3value.ve_has_first_vp);
        }
        Resource valueObject = port.getPropertyResourceValue(E3value.vp_requests_offers_vo);
        //if it has been allocated  a value object
        if (valueObject != null) {
            String valueObjectString = valueObject.getProperty(E3value.e3_has_name).getLiteral().toString();
            //and if that value object is money
            if (valueObjectString.equals(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns all outgoing transfers of a given actor
     *
     * @param actor the actor resource whose outgoing transfers to return
     * @return a set of ValueTransfers which are connected to outgoing ports
     * attached to ValueInterfaces attached to the actor
     */
    public Set<Resource> getExchangesPerformedBy(Resource actor) {
        //NodeIterator iter = model.listObjectsOfProperty(actor, E3value.ac_has_vi);
        ResIterator actorValueInterfaces = model.listResourcesWithProperty(E3value.vi_assigned_to_ac, actor);
        Set<Resource> exchanges = new HashSet<>();
        while (actorValueInterfaces.hasNext()) {
            Resource exchange = (Resource) actorValueInterfaces.next();
            StmtIterator actorOfferings = exchange.listProperties(E3value.vi_consists_of_of);
            while (actorOfferings.hasNext()) {
                Resource vo = actorOfferings.next().getResource();
                if (vo.getProperty(E3value.e3_has_name).getString().equals("out")) {
                    StmtIterator outgoingPorts = vo.listProperties(E3value.vo_consists_of_vp);
                    while (outgoingPorts.hasNext()) {
                        Resource outgoingPort = outgoingPorts.next().getResource();
                        StmtIterator outgoingExchanges = outgoingPort.listProperties(E3value.vp_out_connects_ve);
                        while (outgoingExchanges.hasNext()) {
                            exchanges.add(outgoingExchanges.next().getResource());
                        }
                        outgoingExchanges = outgoingPort.listProperties(E3value.vp_first_connects_ve);
                        while (outgoingExchanges.hasNext()) {
                            exchanges.add(outgoingExchanges.next().getResource());
                        }
                        outgoingExchanges = outgoingPort.listProperties(E3value.vp_second_connects_ve);
                        while (outgoingExchanges.hasNext()) {
                            exchanges.add(outgoingExchanges.next().getResource());
                        }
                    }
                }
            }
        }
        return exchanges;
    }

    /**
     * Updates the formula of the given Resource. ATTENTION: this method assumes
     * the given resource has only ONE formula.
     *
     * @param valueInterface
     * @param newOccurrenceExpression
     */
    private void updateValueInterfaceOccurrences(Resource valueInterface, String newOccurrenceExpression) {
        String ID = valueInterface.getURI().split("#")[1];
        //add the respective OCCURRENCE rate
        if (valueInterface.hasProperty(E3value.e3_has_formula)) {
            valueInterface.getProperty(E3value.e3_has_formula).changeObject("OCCURRENCES=" + newOccurrenceExpression);
            evaluatedModel.changeExistingFormula("#" + ID + ".OCCURRENCES", ID, newOccurrenceExpression);
            if (debug) {
                System.out.println("\t\t\t...updated OCCURRENCES=" + newOccurrenceExpression + " to " + valueInterface.getProperty(E3value.e3_has_name).getString());
            }
        } else {
            evaluatedModel.addNewFormula("#" + ID + ".OCCURRENCES", ID, newOccurrenceExpression);
            valueInterface.addProperty(E3value.e3_has_formula, "OCCURRENCES=" + newOccurrenceExpression);
            if (debug) {
                System.out.println("\t\t\t...added OCCURRENCES=" + newOccurrenceExpression + " to " + valueInterface.getProperty(E3value.e3_has_name).getString());
            }
        }

    }

    /**
     * Updates the OCCURRENCE attribute of a need to a given number..
     *
     * @param need
     * @param occurrence
     * @return
     */
    private boolean updateNeedOccurrence(Resource need, double occurrence) {
        String ID = need.getURI().split("#")[1];
        //first, check if input is really a need:
        if (!need.hasProperty(RDF.type, E3value.start_stimulus)) {
            System.err.println("Attempted to set occurence rate on a node which is not a need!");
            return false;
        }
        StmtIterator formulas = need.listProperties(E3value.e3_has_formula);
        while (formulas.hasNext()) {
            Statement formula = formulas.next();
            String attribute = formula.getString().split("=", 2)[0];
            if (attribute.equals("OCCURRENCES")) {
                formula.changeObject("OCCURRENCES=" + occurrence);
                evaluatedModel.changeExistingFormula("#" + ID + ".OCCURRENCES", ID, occurrence);
                break;
            }
        }
        this.enhance();
        return true;
    }

    /**
     * Updates the COUNT attribute of a Market Segment to a given number..
     *
     * @param ms the market segment to update
     * @param count the new COUNT value
     * @return
     */
    private boolean updateCount(Resource ms, double count) {
        String ID = ms.getURI().split("#")[1];
        //first, check if input is really a need:
        if (!ms.hasProperty(RDF.type, E3value.market_segment)) {
            System.err.println("Attempted to set count rate on a node which is not a market segment!");
            return false;
        }
        StmtIterator formulas = ms.listProperties(E3value.e3_has_formula);
        while (formulas.hasNext()) {
            Statement formula = formulas.next();
            String attribute = formula.getString().split("=", 2)[0];
            if (attribute.equals("COUNT")) {
                formula.changeObject("COUNT=" + count);
                evaluatedModel.changeExistingFormula("#" + ID + ".COUNT", ID, count);
                break;
            }
        }
        this.enhance();
        return true;
    }

    public String getNeedOccurrence(Resource need) {
        String value = null;
        StmtIterator formulas = need.listProperties(E3value.e3_has_formula);
        while (formulas.hasNext()) {
            Statement formula = formulas.next();
            String attribute = formula.getString().split("=", 2)[0];
            if (attribute.equals("OCCURRENCES")) {
                value = formula.getString().split("=", 2)[1];
                break;
            }
        }
        return value;
    }

    public String getMarketSegmentCount(Resource ms) {
        String value = null;
        StmtIterator formulas = ms.listProperties(E3value.e3_has_formula);
        while (formulas.hasNext()) {
            Statement formula = formulas.next();
            String attribute = formula.getString().split("=", 2)[0];
            if (attribute.equals("COUNT")) {
                value = formula.getString().split("=", 2)[1];
                break;
            }
        }
        return value;
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
        String URIbase = this.getActorsAndMarketSegments().iterator().next().getURI().split("#")[0];
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

        long id = Utils.getUnusedID(URIbase, model);
        if (debug) {
            System.out.println("Creating a VP: " + id);
        }
        port1 = model.createResource(URIbase + "#" + id, E3value.value_port);
        port1.addProperty(E3value.e3_has_name, "vp" + id);
        port1.addProperty(E3value.e3_has_uid, "" + id);
        port1.addProperty(E3value.vp_has_dir, "true");
        port1.addProperty(E3value.mc_in_mo, modelResource);
        modelResource.addProperty(E3value.mo_has_mc, port1);
        port1.addProperty(E3value.vp_requests_offers_vo, moneyResource);
        moneyResource.addProperty(E3value.vo_offered_requested_by_vp, port1);
        port1.addProperty(E3value.mc_in_di, diagramResource);
        diagramResource.addProperty(E3value.mo_has_mc, port1);
        port1.addProperty(E3value.vp_in_vo, vo1);
        vo1.addProperty(E3value.vo_consists_of_vp, port1);
        port1.addProperty(E3value.e3_has_formula, "VALUATION=" + value);

        id = Utils.getUnusedID(URIbase, model);
        port2 = model.createResource(URIbase + "#" + id, E3value.value_port);
        port2.addProperty(E3value.e3_has_name, "vp" + id);
        port2.addProperty(E3value.e3_has_uid, "" + id);
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

        id = Utils.getUnusedID(URIbase, model);
        exchange = model.createResource(URIbase + "#" + id, E3value.value_exchange);
        exchange.addProperty(E3value.e3_has_name, "Hidden transfer");
        exchange.addProperty(E3value.e3_has_uid, "" + id);
        exchange.addProperty(E3value.mc_in_mo, modelResource);
        modelResource.addProperty(E3value.mo_has_mc, exchange);
        moneyResource.addProperty(E3value.mo_has_mc, exchange);
        exchange.addProperty(E3value.ve_has_in_po, port2);
        exchange.addProperty(E3value.ve_has_out_po, port1);
        exchange.addProperty(E3value.e3_has_formula, "CARDINALITY=1");

        port1.addProperty(E3value.vp_out_connects_ve, exchange);

        port2.addProperty(E3value.vp_in_connects_ve, exchange);
    }

    public void makeHidden(Resource valueExchange) {
        valueExchange = model.getResource(valueExchange.getURI());        //make sure the resources are from this model
        valueExchange.addProperty(E3value.e3_has_formula, "DOTTED=1");
    }

    public void makeNonOccurring(Resource valueExchange) {
        valueExchange = model.getResource(valueExchange.getURI());        //make sure the resources are from this model
        valueExchange.addProperty(E3value.e3_has_formula, "DASHED=1");
        this.fraudChanges.addNonOccurringTransaction(valueExchange.getProperty(E3value.e3_has_uid).getLong());
    }

    public double getCardinality(Resource res) {
        //make sure the resources are from this model
        res = model.getResource(res.getURI());
        double cardinality = 1;
        StmtIterator resFormulas = res.listProperties(E3value.e3_has_formula);
        while (resFormulas.hasNext()) {
            Statement formula = resFormulas.next();
            String attribute = formula.getString().split("=", 2)[0];
            if (attribute.equals("CARDINALITY")) {
                cardinality = valueOf(res, attribute);
            }
        }
        return cardinality;
    }

    private boolean isNonOccurring(Resource valueExchange) {
        Resource exchange = model.getResource(valueExchange.getURI());
        StmtIterator veFormulas = exchange.listProperties(E3value.e3_has_formula);
        while (veFormulas.hasNext()) {
            Statement formula = veFormulas.next();
            String attribute = formula.getString().split("=", 2)[0];
            if (attribute.equals("DASHED")) {
                return true;
            }
        }
        return false;
    }

    private boolean isHidden(Resource valueExchange) {
        //make sure the resources are from this model
        valueExchange = model.getResource(valueExchange.getURI());
        StmtIterator veFormulas = valueExchange.listProperties(E3value.e3_has_formula);
        while (veFormulas.hasNext()) {
            Statement formula = veFormulas.next();
            String attribute = formula.getString().split("=", 2)[0];
            if (attribute.equals("DOTTED")) {
                return true;
            }
        }
        return false;
    }

    public int countNonOccurring() {
        ResIterator resIterator = model.listResourcesWithProperty(E3value.e3_has_formula);
        int i = 0;
        while (resIterator.hasNext()) {
            Resource resource = resIterator.next();
            if (isNonOccurring(resource)) {
                i++;
            }

        }
        return i;
    }

    /**
     * Turns actor1 into actor1+actor2 and deletes actor 2
     *
     * @param actor1
     * @param actor2
     */
    void collude(Resource actor1, Resource actor2) {

        long actor1ID = Long.parseLong(actor1.getProperty(E3value.e3_has_uid).getString());
        long actor2ID = Long.parseLong(actor2.getProperty(E3value.e3_has_uid).getString());
        this.fraudChanges.addColludedActor(actor1ID);
        this.fraudChanges.addColludedActor(actor2ID);

        //make sure the resources are from this model
        actor1 = model.getResource(actor1.getURI());
        actor2 = model.getResource(actor2.getURI());

        //First, merge their names
        String actor1Name = actor1.getProperty(E3value.e3_has_name).getString();
        String actor2Name = actor2.getProperty(E3value.e3_has_name).getString();
        String newActorName = actor1Name + " + " + actor2Name;
        actor1.getProperty(E3value.e3_has_name).changeObject(newActorName);

        //Next add up their expenses and investment
        if (actor1.hasProperty(E3value.e3_has_formula)) {

            //First add up common attributes of both actors
            //by getting a list of actor1sformulas
            StmtIterator actor1Formulas = actor1.listProperties(E3value.e3_has_formula);
            Set<String> newActorFormulas = new HashSet<>();
            //and for each one
            while (actor1Formulas.hasNext()) {
                Statement actor1Formula = actor1Formulas.next();
                String actor1FormulaAttribute = actor1Formula.getString().split("=", 2)[0];
                String actor1FormulaValue = actor1Formula.getString().split("=", 2)[1];
                //check if actor2 has a matching one
                StmtIterator actor2Formulas = actor2.listProperties(E3value.e3_has_formula);
                while (actor2Formulas.hasNext()) {
                    Statement actor2Formula = actor2Formulas.next();
                    String actor2FormulaAttribute = actor2Formula.getString().split("=", 2)[0];
                    String actor2FormulaValue = actor2Formula.getString().split("=", 2)[1];
                    //if it does
                    if (actor1FormulaAttribute.equals(actor2FormulaAttribute) && !actor2FormulaAttribute.equals("COUNT")) {
                        //add up actor1's and actor2's values or expressions  (except for COUNT)                     
                        String newActorFormulaValue = actor1FormulaValue + " + " + actor2FormulaValue;
                        String newActorFormulaAttribute = actor1FormulaAttribute;
                        newActorFormulas.add(newActorFormulaAttribute + "=" + newActorFormulaValue);
                    }
                }
            }

            //Then, add the non-overlapping formulas
            //by listing of formulas of actor 1,2
            actor1Formulas = actor1.listProperties(E3value.e3_has_formula);
            StmtIterator actor2Formulas = actor2.listProperties(E3value.e3_has_formula);
            //and for each one
            while (actor2Formulas.hasNext()) {
                Statement actor2Formula = actor2Formulas.next();
                String actor2FormulaAttribute = actor2Formula.getString().split("=", 2)[0];
                String actor2FormulaValue = actor2Formula.getString().split("=", 2)[1];
                //check if newactor has a matching one
                boolean found = false;
                for (String newActorFormula : newActorFormulas) {
                    String newActorFormulaAttribute = newActorFormula.split("=", 2)[0];
                    if (newActorFormulaAttribute.equals(actor2FormulaAttribute)) {
                        found = true;
                    }
                }
                if (!found) {
                    newActorFormulas.add(actor2FormulaAttribute + "=" + actor2FormulaValue);
                }
            }
            //and for each one
            while (actor1Formulas.hasNext()) {
                Statement actor1Formula = actor1Formulas.next();

                String actor1FormulaAttribute = actor1Formula.getString().split("=", 2)[0];
                String actor1FormulaValue = actor1Formula.getString().split("=", 2)[1];
                //check if newactor has a matching one
                boolean found = false;
                for (String newActorFormula : newActorFormulas) {
                    String newActorFormulaAttribute = newActorFormula.split("=", 2)[0];
                    if (newActorFormulaAttribute.equals(actor1FormulaAttribute)) {
                        found = true;
                    }
                }
                if (!found) {
                    newActorFormulas.add(actor1FormulaAttribute + "=" + actor1FormulaValue);
                }
            }

            //remove the old ones
            actor1.removeAll(E3value.e3_has_formula);
            //add the new
            for (String formula : newActorFormulas) {
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

        //and remove them from actor2
        actor2.removeAll(E3value.ac_has_vi);

        //Finally, delete actor2      
        actor2.removeProperties();

        //To make sure ar references are still resolvable,
        //cycle through all expressions in the model and replace references as needed
        String emptyString = null;
        StmtIterator expressionStatements = model.listStatements(null,E3value.e3_has_formula,emptyString);       
        List<Statement> expressionStatementsList = expressionStatements.toList();
        for(Statement expressionStatement : expressionStatementsList) {
            String expression = expressionStatement.getLiteral().toString();
            expression = expression.replace("'"+actor1Name+"'", "'"+newActorName+"'");
            expression = expression.replace("'"+actor2Name+"'", "'"+newActorName+"'");
            expression = expression.replace("#"+actor2ID, "#"+actor1ID);   
            expressionStatement.changeObject(expression);
        }

    }

    /**
     * This method finds all interfaces which are directly connected via Value
     * Exchanges (not connection elements)
     *
     * @param nextElement the Value Interface resource for which to find
     * connected Value Interfaces
     * @param inner Whether or not to return interfaces connecting to
     * sub-components (e.g. actor to sub-actor or actor to value activity or
     * reverse)
     * @param outer Whether or not to return interfaces connection to other
     * components (e.g. actor to actor or actor to MS or reverse)
     * @return a Set of Value Interface resources which are connected to the
     * given Value Interfaces and satisfy the two criteria (inner and outer)
     */
    private HashSet<Resource> getConnectedInterfaces(Resource nextElement, boolean inner, boolean outer) {
        HashSet<Resource> connectedValueInterfaces = new HashSet<Resource>();
        StmtIterator valueOfferings = nextElement.listProperties(E3value.vi_consists_of_of);  //get it's ValueOfferings
        while (valueOfferings.hasNext()) {
            Resource valueOffering = valueOfferings.next().getResource();
            StmtIterator valuePorts = valueOffering.listProperties(E3value.vo_consists_of_vp);  //get the valuePorts
            while (valuePorts.hasNext()) {
                Resource valuePort = valuePorts.next().getResource();
                if (valuePort.hasProperty(E3value.vp_in_connects_ve) && outer) {
                    StmtIterator valueExchanges = valuePort.listProperties(E3value.vp_in_connects_ve);
                    while (valueExchanges.hasNext()) {
                        Resource valueExchange = valueExchanges.next().getResource();
                        Resource reciprocalValuePort = valueExchange.getProperty(E3value.ve_has_out_po).getResource();
                        Resource reciprocalValueOffering = reciprocalValuePort.getProperty(E3value.vp_in_vo).getResource();
                        Resource reciprocalValueInterface = reciprocalValueOffering.getProperty(E3value.vo_in_vi).getResource();
                        connectedValueInterfaces.add(reciprocalValueInterface);
                    }
                }
                if (valuePort.hasProperty(E3value.vp_out_connects_ve) && outer) {
                    StmtIterator valueExchanges = valuePort.listProperties(E3value.vp_out_connects_ve);
                    while (valueExchanges.hasNext()) {
                        Resource valueExchange = valueExchanges.next().getResource();
                        Resource reciprocalValuePort = valueExchange.getProperty(E3value.ve_has_in_po).getResource();
                        Resource reciprocalValueOffering = reciprocalValuePort.getProperty(E3value.vp_in_vo).getResource();
                        Resource reciprocalValueInterface = reciprocalValueOffering.getProperty(E3value.vo_in_vi).getResource();
                        connectedValueInterfaces.add(reciprocalValueInterface);
                    }
                }
                if (valuePort.hasProperty(E3value.vp_first_connects_ve) && inner) {
                    StmtIterator valueExchanges = valuePort.listProperties(E3value.vp_first_connects_ve);
                    while (valueExchanges.hasNext()) {
                        Resource valueExchange = valueExchanges.next().getResource();
                        Resource reciprocalValuePort = valueExchange.getProperty(E3value.ve_has_second_vp).getResource();
                        Resource reciprocalValueOffering = reciprocalValuePort.getProperty(E3value.vp_in_vo).getResource();
                        Resource reciprocalValueInterface = reciprocalValueOffering.getProperty(E3value.vo_in_vi).getResource();
                        connectedValueInterfaces.add(reciprocalValueInterface);
                    }
                }
                if (valuePort.hasProperty(E3value.vp_second_connects_ve) && inner) {
                    StmtIterator valueExchanges = valuePort.listProperties(E3value.vp_second_connects_ve);
                    while (valueExchanges.hasNext()) {
                        Resource valueExchange = valueExchanges.next().getResource();
                        Resource reciprocalValuePort = valueExchange.getProperty(E3value.ve_has_first_vp).getResource();
                        Resource reciprocalValueOffering = reciprocalValuePort.getProperty(E3value.vp_in_vo).getResource();
                        Resource reciprocalValueInterface = reciprocalValueOffering.getProperty(E3value.vo_in_vi).getResource();
                        connectedValueInterfaces.add(reciprocalValueInterface);
                    }
                }
            }
        }
        return connectedValueInterfaces;
    }

    public String divideByCount(Resource marketSegment, String occurrences) {
        double count = 1;
        StmtIterator formulas = marketSegment.listProperties(E3value.e3_has_formula);
        while (formulas.hasNext()) {
            Statement formula = formulas.next();
            String attribute = formula.getString().split("=", 2)[0];
            if (attribute.equals("COUNT")) {
                count = valueOf(marketSegment, attribute);
            }
        }
        if (debug) {
            System.out.println("\t\t\t... Entering MS '" + marketSegment.getProperty(E3value.e3_has_name).getLiteral().toString() + "'. Dividing occurences by " + count);
        }

        //If the occurrence rate is a number
        if (occurrences.matches("\\d*\\.?\\d*")) {
            //Compute it now to keep it a number
            occurrences = Double.toString(Double.valueOf(occurrences) / count);
        } //if it is an expression
        else {
            //Keep it as an expression
            occurrences = "(" + occurrences + ")/" + count;
        }
        return occurrences;
    }

    public String multiplyByCount(Resource marketSegment, String occurrences) {
        double count = 1;
        StmtIterator formulas = marketSegment.listProperties(E3value.e3_has_formula);
        while (formulas.hasNext()) {
            Statement formula = formulas.next();
            String attribute = formula.getString().split("=", 2)[0];
            if (attribute.equals("COUNT")) {
                count = valueOf(marketSegment, attribute);
            }
        }
        if (debug) {
            System.out.println("\t\t\t... Exiting  MS; Multiplying occurences by " + count);
        }
        //If the occurrence rate is a number
        if (occurrences.matches("\\d*\\.?\\d*")) {
            //Compute it now to keep it a number
            occurrences = Double.toString(Double.valueOf(occurrences) * count);
        } //if it is an expression
        else {
            //Keep it as an expression
            occurrences = "(" + occurrences + ")*" + count;
        }
        return occurrences;
    }

    /**
     * Re(creates) this E3Model's expressionEvaluator from this E3Model's Jena
     * Model.
     */
    public void createExpressionEvaluator() {
        this.evaluatedModel = ExpressionEvaluator.evaluateModel(this.getJenaModel()).get();
    }

/// Enhance and traverse methods
    /**
     * Computes and appends occurrence expressions to all ValueInterface and
     * resolves all references. * This is to allow easier computation of Profit
     * per actor by getTotalForActor and getSeries methods.
     *
     */
    public void enhance() {
        if (evaluatedModel == null) {
            createExpressionEvaluator();
        }

        //get a list of Start Stimuli
        ResIterator startStimuli = model.listSubjectsWithProperty(RDF.type, E3value.start_stimulus);

        //for each Start Stimulus        
        while (startStimuli.hasNext()) {
            Resource startStimulus = startStimuli.next();
            String occurrences = null;
            //get occurences expressions of this stimuli
            StmtIterator startStimulusFormulas = startStimulus.listProperties(E3value.e3_has_formula);
            while (startStimulusFormulas.hasNext()) {
                Statement formula = startStimulusFormulas.next();
                String attribute = formula.getString().split("=", 2)[0];
                String expression = formula.getString().split("=", 2)[1];
                if (attribute.equals("OCCURRENCES")) {
                    occurrences = expression;
                }
            }
            //get nextElement down the line
            Resource nextElement = startStimulus.getProperty(E3value.de_down_ce).getResource();

            if (debug) {
                System.out.println("\t...Starting traversal from " + startStimulus.getProperty(E3value.e3_has_name).getLiteral().toString() + " with OCCURRENCE =  " + occurrences);
            }
            //and go down the depdendecy path until the end, adding OCCURENCE rates to value interfaces
            traverse(nextElement, occurrences);
        }
        if (debug) {
            System.out.println("\t...Finished!\n");
        }
        evaluatedModel.reEvaluate();
    }

    /**
     * Starting from nextElement, goes down the dependency path, until reaching
     * the end stimulus (or stimuli if AND/OR forks are present), updating
     * OCCURENCE rates of Value Interfaces along the way. This is a recursive
     * helper method for enhance().
     *
     * @param nextElement the element in the graph from which to start
     * traversing downwards
     * @param occurences the occurrence rate of nextElement
     */
    private void traverse(Resource nextElement, String occurrences) {
        //While this is not the last element (i.e. an end stimulus)
        while (!nextElement.hasProperty(RDF.type, E3value.end_stimulus)) {
            if (debug) {
                System.out.println("\t\t...moved to element: " + nextElement.getProperty(E3value.e3_has_name).getString());
                System.out.println("\t\t\t...with e3type: " + nextElement.getProperty(RDF.type).toString());
            }

            //then depending on the type element, decide what to do next:
            //###  OR node  ###
            if (nextElement.hasProperty(RDF.type, E3value.OR_node)) {
                StmtIterator outgoingNodes = nextElement.listProperties(E3value.de_down_ce);//get outgoing connection elements
                StmtIterator incomingNodes = nextElement.listProperties(E3value.de_up_ce);
                List<Statement> outgoingNodeList = outgoingNodes.toList();//get list() of outgoing elements (for more control)
                List<Statement> incomingNodeList = incomingNodes.toList();

                //## OR join ##
                if (incomingNodeList.size() > 1) {
                    if (debug) {
                        System.out.println("\t\t\t ...found OR  join with " + incomingNodeList.size() + " incoming ports.");
                    }

                    //check if all incoming paths have been considered,
                    for (Statement incomingNode : incomingNodeList) {
                        //and if not
                        if (!incomingNode.getResource().hasProperty(E3value.e3_has_formula, "VISITED=1")) {
                            System.out.println("\t\t\t ...waiting for the other incoming paths to be computed");
                            //wait for the other paths
                            return;
                        }
                    }

                    //If all paths were computed,     
                    //sum up the occurrence of the incoming path 
                    String outgoingOccurrences = "0";
                    for (Statement incomingNodeStatement : incomingNodeList) {
                        String incomingOccurrence = incomingNodeStatement.getResource().getProperty(E3value.down_fraction).getString();
                        if (outgoingOccurrences.matches("\\d*\\.?\\d*") && incomingOccurrence.matches("\\d*\\.?\\d*")) {
                            outgoingOccurrences = Double.toString(Double.valueOf(outgoingOccurrences) + Double.valueOf(incomingOccurrence));
                        } else {
                            outgoingOccurrences = "(" + outgoingOccurrences + ")+(" + incomingOccurrence + ")";
                        }
                    }
                    if (debug) {
                        System.out.println("\t\t\t ... sum of incoming occurrences = " + outgoingOccurrences);
                    }

                    //and then go down outgoing CE
                    traverse(nextElement.getProperty(E3value.de_down_ce).getResource(), outgoingOccurrences);
                    return;

                    //## OR fork ##
                } else {
                    if (debug) {
                        System.out.println("\t\t\t ...found OR  fork with " + outgoingNodeList.size() + " outgoing ports.");
                    }

                    //First, sum up the fractions of all outgoing Connection Elements
                    double totalFractions = 0;
                    for (Statement node : outgoingNodeList) {
                        totalFractions += node.getResource().getProperty(E3value.up_fraction).getFloat();
                    }

                    //Second, go down each path using occurence = OCCURENCE*FRACTION/Total_FRACTIONs
                    for (Statement node : outgoingNodeList) {
                        double ratio = node.getResource().getProperty(E3value.up_fraction).getFloat() / totalFractions;
                        //If the occurrence rate is a number
                        if (occurrences.matches("\\d*\\.?\\d*")) {
                            //Compute it now to keep it a number
                            occurrences = Double.toString(Double.valueOf(occurrences) * ratio);
                        } //if it is an expression
                        else {
                            //Keep it as an expression
                            occurrences = "(" + occurrences + ")*" + ratio;
                        }
                        traverse(node.getResource(), occurrences);
                    }
                    return;
                }

                //###  AND node  ###
            } else if (nextElement.hasProperty(RDF.type, E3value.AND_node)) { //if it's a AND node
                StmtIterator outgoingNodes = nextElement.listProperties(E3value.de_down_ce);//get outgoing connection elements
                StmtIterator incomingNodes = nextElement.listProperties(E3value.de_up_ce);
                List<Statement> outgoingNodeList = outgoingNodes.toList();
                List<Statement> incomingNodeList = incomingNodes.toList();

//                //## AND join ##
                if (incomingNodeList.size() > 1) {
                    if (debug) {
                        System.out.println("\t\t\t ...it is an AND  join with " + incomingNodeList.size() + " incoming ports.");
                    }

                    //check if all incoming paths have been considered,
                    for (Statement incomingNode : incomingNodeList) {
                        //and if not
                        if (!incomingNode.getResource().hasProperty(E3value.e3_has_formula, "VISITED=1")) {
                            if (debug) {
                                System.out.println("\t\t\t ...waiting for the other incoming paths to be computed");
                            }
                            //wait for the other paths
                            return;
                        }
                    }

                    //If all paths were computed,     
                    //take the smallest 
                    String outgoingOccurrences = incomingNodeList.get(0).getResource().getProperty(E3value.down_fraction).getString();
                    for (int i = 1; i < incomingNodeList.size(); i++) {
                        String incomingOccurrence = incomingNodeList.get(i).getResource().getProperty(E3value.down_fraction).getString();
                        if (outgoingOccurrences.matches("\\d*\\.?\\d*") && incomingOccurrence.matches("\\d*\\.?\\d*")) {
                            int comparissonResult = (Double.valueOf(outgoingOccurrences).compareTo(Double.valueOf(incomingOccurrence)));
                            if (comparissonResult > 0) {
                                outgoingOccurrences = incomingOccurrence;
                            }
                        } else {
                            outgoingOccurrences = "IF(" + outgoingOccurrences + ">" + incomingOccurrence + "," + incomingOccurrence + "," + outgoingOccurrences + ")";
                        }
                    }
                    if (debug) {
                        System.out.println("\t\t\t ... smallest occurrence rate = " + outgoingOccurrences);
                    }

                    //and then go down outgoing CE
                    traverse(nextElement.getProperty(E3value.de_down_ce).getResource(), outgoingOccurrences);
                    return;
                } //## AND fork ##
                else {

                    if (debug) {
                        System.out.println("\t\t\t ...it is an AND  fork with " + outgoingNodeList.size() + " outgoing ports.");
                    }
                    //Go down each path using occurence = OCCURENCE*fraction
                    for (Statement node : outgoingNodeList) {
                        double ratio = node.getResource().getProperty(E3value.up_fraction).getFloat();
                        //Therefore, if the occurrence rate is a number
                        if (occurrences.matches("\\d*\\.?\\d*")) {
                            //Compute it now to keep it a number
                            occurrences = Double.toString(Double.valueOf(occurrences) * ratio);
                        } //if it is an expression
                        else {
                            //Keep it as an expression
                            occurrences = "(" + occurrences + ")*" + ratio;
                        }
                        traverse(node.getResource(), occurrences);
                    }
                    return;
                }

                //###  connection_element  ###
            } else if (nextElement.hasProperty(RDF.type, E3value.connection_element)) {//if it's a ConnectionElement
                if (debug) {
                    System.out.println("\t\t\t... it is a Connection Element");
                }
                //if the next element happens to be a join (i.e. have multiple incoming CEs)
                StmtIterator incomingNodes = nextElement.getProperty(E3value.ce_with_down_de).getResource().listProperties(E3value.de_up_ce);
                List<Statement> incomingNodeList = incomingNodes.toList();
                if (incomingNodeList.size() > 1) {
                    //mark this incoming path as visited (to be used by the join to compute the occurrences of the outgoing path)
                    nextElement.addProperty(E3value.e3_has_formula, "VISITED=1");
                    nextElement.getProperty(E3value.down_fraction).changeObject(occurrences);
                }
                nextElement = nextElement.getProperty(E3value.ce_with_down_de).getResource();//choose the next element (Value Interface or AND/OR node)

                //###  value_interface  with incoming CE###                
            } else if (nextElement.hasProperty(RDF.type, E3value.value_interface) && nextElement.hasProperty(E3value.de_up_ce)) { //if it's a ValueInterface with an incoming ConnectionElement (meaning we need to go down the ValueExchange(s))
                if (debug) {
                    System.out.println("\t\t\t... it is a ValueInterface with an incoming ConnectionElement");
                }
                //add occurrences to it (before taking count into consideration)
                updateValueInterfaceOccurrences(nextElement, occurrences);

                //if the Value Interface was part of a MarketSegment, multiply the occurence by the count of this MarketSegment
                if (nextElement.hasProperty(E3value.vi_assigned_to_ms)) {
                    Resource marketSegment = nextElement.getPropertyResourceValue(E3value.vi_assigned_to_ms);
                    occurrences = multiplyByCount(marketSegment, occurrences);
                }

                //find all other interfaces that are connected to this one 
                HashSet<Resource> connectedValueInterfaces = getConnectedInterfaces(nextElement, true, true);

                //and continue the traversal through each one
                for (Resource connectedValueInterface : connectedValueInterfaces) {
                    traverse(connectedValueInterface, occurrences);
                }
                return;

                //###  value_interface with outgoing CE  ###
            } else if (nextElement.hasProperty(RDF.type, E3value.value_interface) && nextElement.hasProperty(E3value.de_down_ce)) { //if it's a ValueInterface with an outgoing ConnectionElement (meaning we need to go down this ConnectionElement)
                if (debug) {
                    System.out.println("\t\t\t... it is a ValueInterface with an outgoing ConnectionElement");
                }

                //If the Value Interface is part of a MarketSegment, divide the occurence by the count of this MarketSegment
                if (nextElement.hasProperty(E3value.vi_assigned_to_ms)) {
                    Resource marketSegment = nextElement.getPropertyResourceValue(E3value.vi_assigned_to_ms);
                    occurrences = divideByCount(marketSegment, occurrences);
                }
                //Then, add occurrences to it 
                updateValueInterfaceOccurrences(nextElement, occurrences);

                nextElement = nextElement.getProperty(E3value.de_down_ce).getResource();//choose the next Connection Element                

                //###  value_interface with no connected CE (exchanges on either sides)  ###
            } else if (nextElement.hasProperty(RDF.type, E3value.value_interface)) {
                if (debug) {
                    System.out.println("\t\t\t... it is a ValueInterface with no connected CE (exchanges on both sides)");
                }
                //First, find out which is (are) the next value interface(s)                
                HashSet<Resource> connectedValueInterfaces = getConnectedInterfaces(nextElement, true, true);
                System.out.println("connected value interfaces: " + connectedValueInterfaces.toString());
                HashSet<Resource> outgoingConnectedValueInterfaces = new HashSet<>();
                //by looking through all connected interfaces
                for (Resource connectedValueInterface : connectedValueInterfaces) {
                    //and selecting the ones which have a outgoing CE
                    if (connectedValueInterface.hasProperty(E3value.de_down_ce)) {
                        outgoingConnectedValueInterfaces.add(connectedValueInterface);
                    } //or which are in turn connected to another interface  which has an outgoing CE
                    else if (!connectedValueInterface.hasProperty(E3value.de_down_ce)) {
                        HashSet<Resource> secondConnectedValueInterfaces = getConnectedInterfaces(connectedValueInterface, true, true);
                        for (Resource secondConnectedValueInterface : secondConnectedValueInterfaces) {
                            if (secondConnectedValueInterface.hasProperty(E3value.de_down_ce)) {
                                outgoingConnectedValueInterfaces.add(connectedValueInterface);
                            }
                        }
                    }
                }

                System.out.println("outgoing connected value interfaces: " + outgoingConnectedValueInterfaces.toString());

                //Second, find out if we are entering or exiting a MarketSegment
                //and divide or multiple the occurence by its count as needed
                if (nextElement.hasProperty(E3value.vi_assigned_to_ms)) {
                    Resource marketSegment = nextElement.getPropertyResourceValue(E3value.vi_assigned_to_ms);
                    HashSet<Resource> innerConnectedValueInterfaces = getConnectedInterfaces(nextElement, true, false);
                    if (innerConnectedValueInterfaces.containsAll(connectedValueInterfaces)) {
                        occurrences = divideByCount(marketSegment, occurrences);
                    } else {
                        occurrences = multiplyByCount(marketSegment, occurrences);
                    }
                }

                //Then, add occurrences to it 
                updateValueInterfaceOccurrences(nextElement, occurrences);

                //and continue the traversal through each one
                for (Resource outgoingConnectedValueInterface : outgoingConnectedValueInterfaces) {
                    traverse(outgoingConnectedValueInterface, occurrences);
                }
                return;
            }
        }
        if (debug) {
            System.out.println("\t...reached end stimulus!");
        }
    }

// Methods for computing values (totals, averages, series)    
    /**
     * Computes and returns the value of an elements' attribute element
     *
     * @param element
     * @param attribute
     * @return numerical value (or 0 if attribute was not found)
     */
    public double valueOf(Resource element, String attribute) {
        String uuid = element.getProperty(E3value.e3_has_uid).getString();
        Optional<Double> val = this.evaluatedModel.valueOf("#" + uuid + "." + attribute);
        if (val.isPresent()) {
            return val.get();
        }
        return 0;
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
     * expected case (taking into consideration only solid and nonOccurring
     * lines ) or for the real case (taking into consideration only solid and
     * hidden lines). True means expected. False means real.
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
            // if next element is a value interface
            if (nextElement.hasProperty(RDF.type, E3value.value_interface)) {
                //just add it
                selectedValueInterfaces.add(nextElement);
            }
            // Only look at value interfaces (not AND/OR nodes)
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
            // Only look at value interfaces (not AND/OR nodes)
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
        if (!actor.hasProperty(RDF.type, E3value.elementary_actor) && !actor.hasProperty(RDF.type, E3value.market_segment)) {
            System.err.println("Not an actor!");
            return 0;
        }

        //Second, take into account Investment, Expenses and Interest:
        if (actor.hasProperty(E3value.e3_has_formula)) {
            //get list of formulas (INVESTMENT, EXPENSES, INTEREST)
            StmtIterator actorFormulas = actor.listProperties(E3value.e3_has_formula);
            while (actorFormulas.hasNext()) {
                Statement formula = actorFormulas.next();
                String formulaString = formula.getString();
                String attribute = formulaString.split("=", 2)[0];
                if (attribute.equals("INVESTMENT") || attribute.equals("EXPENSES") || attribute.equals("INTEREST")) {
                    double value = valueOf(actor, attribute);
                    result -= value;
                }
            }
        }

        //Third, calculate the income/loss per Value Interface:
        //if actor/ms has a Value Interfaces
        if (actor.hasProperty(E3value.ac_has_vi) || actor.hasProperty(E3value.ms_has_vi)) {
            StmtIterator actorValueInterfaces;
            //get list of ValueInterfaces
            if (actor.hasProperty(E3value.ms_has_vi)) {
                actorValueInterfaces = actor.listProperties(E3value.ms_has_vi);
            } else {
                actorValueInterfaces = actor.listProperties(E3value.ac_has_vi);
            }
            //for each VAlueInterface,
            while (actorValueInterfaces.hasNext()) {
                Statement valueInterface = actorValueInterfaces.next();
                //if it is part of the desired ones 
                if (selectedValueInterfaces.contains(valueInterface.getResource())) {
                    //get ValueOfferings (groups of equally directed ports)
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
                                String formulaString = formula.getString();
                                String attribute = formulaString.split("=", 2)[0];
                                if (attribute.equals("VALUATION") || attribute.equals("EXPENSES")) {
                                    double value = valueOf(valuePort.getResource(), attribute);
                                    valuePortValuation += value;
                                }
                            }

                            //multiplying it with the CARDINALITY of the Port's associated Value Exchange and nullifying if necessary
                            if (valuePort.getResource().hasProperty(E3value.vp_out_connects_ve)) {//if it's an (outgoing) ValuePort
                                if (ideal == true) {//If we want the expected case
                                    if (isHidden(valuePort.getResource().getProperty(E3value.vp_out_connects_ve).getResource())) {//and it's respective (outgoing) Value Exchange is hidden
                                        valuePortValuation = 0; //nullify it
                                    }
                                } else//If we want the real case
                                 if (isNonOccurring(valuePort.getResource().getProperty(E3value.vp_out_connects_ve).getResource())) {//and it's respective (outgoing) Value Exchange is nonOccurring
                                        valuePortValuation = 0; //nullify it
                                    }
                                valuePortValuation *= getCardinality(valuePort.getResource().getProperty(E3value.vp_out_connects_ve).getResource());// then multiply with cardinality of respective (outgoing) ve                             
                            } else if (valuePort.getResource().hasProperty(E3value.vp_in_connects_ve)) {//if it's an (incoming) ValuePort
                                if (ideal == true) {//If we want the expected case
                                    if (isHidden(valuePort.getResource().getProperty(E3value.vp_in_connects_ve).getResource())) {//and it's respective (incoming) Value Exchange is hidden
                                        valuePortValuation = 0; //nullify it
                                    }
                                } else//If we want the real case
                                 if (isNonOccurring(valuePort.getResource().getProperty(E3value.vp_in_connects_ve).getResource())) {//and it's respective (incoming) Value Exchange is nonOccurring
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
     * expected case (taking into consideration only solid and nonOccurring
     * lines ) or for the real case (taking into consideration only solid and
     * hidden lines). True means expected. False means real.
     * @return the profit for that actor in the selected case
     */
    public double getTotalForActor(Resource actor, boolean ideal) {
        double result = 0;
        actor = model.getResource(actor.getURI());

        //First, check if input is really an actor:
        if (!actor.hasProperty(RDF.type, E3value.elementary_actor) && !actor.hasProperty(RDF.type, E3value.market_segment)) {
            System.err.println(actor.getProperty(E3value.e3_has_name) + "(UID "+actor.getProperty(E3value.e3_has_uid)+") is not an actor!");
            throw new IndexOutOfBoundsException("fuuck");
            //return 0;
        }

        //Second, take into account Investment, Expenses and Interest:
        if (actor.hasProperty(E3value.e3_has_formula)) {
            //get list of formulas (INVESTMENT, EXPENSES, INTEREST)
            StmtIterator actorFormulas = actor.listProperties(E3value.e3_has_formula);
            while (actorFormulas.hasNext()) {
                Statement formula = actorFormulas.next();
                String formulaString = formula.getString();
                String attribute = formulaString.split("=", 2)[0];
                if (attribute.equals("INVESTMENT") || attribute.equals("EXPENSES") || attribute.equals("INTEREST")) {
                    double value = valueOf(actor, attribute);
                    result -= value;
                }
            }
        }

        //Third, calculate the income/loss per Value Interface:
        //if actor has a Value Interfaces
        if (actor.hasProperty(E3value.ac_has_vi) || actor.hasProperty(E3value.ms_has_vi)) {
            StmtIterator actorValueInterfaces;
            //get list of ValueInterfaces
            if (actor.hasProperty(E3value.ms_has_vi)) {
                actorValueInterfaces = actor.listProperties(E3value.ms_has_vi);
            } else {
                actorValueInterfaces = actor.listProperties(E3value.ac_has_vi);
            }
            //for each VAlueInterface,
            while (actorValueInterfaces.hasNext()) {
                Statement valueInterface = actorValueInterfaces.next();
                double occurences = 0;
                //get number of occurences
                StmtIterator valueInterfaceFormulas = valueInterface.getResource().listProperties(E3value.e3_has_formula);
                while (valueInterfaceFormulas.hasNext()) {
                    Statement formula = valueInterfaceFormulas.next();
                    String attribute = formula.getString().split("=", 2)[0];
                    String value = formula.getString().split("=", 2)[1];
                    if (attribute.equals("OCCURRENCES")) {
                        // if it is a number
                        if (value.matches("\\d*\\.?\\d*")) {
                            //we must use it directly (since the evaluatedModel will not be up-to-date)
                            occurences = Double.valueOf(value);
                        } //if it is an expression
                        else {
                            //we query the evaluatedModel
                            occurences = valueOf(valueInterface.getResource(), attribute);
                        }
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
                        double valuePortExpenses = 0;
                        StmtIterator valuePortFormulas = valuePort.getResource().listProperties(E3value.e3_has_formula);
                        //adding them up
                        while (valuePortFormulas.hasNext()) {
                            Statement formula = valuePortFormulas.next();
                            String formulaString = formula.getString();
                            String attribute = formulaString.split("=", 2)[0];
                            if (attribute.equals("VALUATION")) {
                                double value = valueOf(valuePort.getResource(), attribute);
                                valuePortValuation += value;
                            } else if (attribute.equals("EXPENSES")) {
                                double value = valueOf(valuePort.getResource(), attribute);
                                valuePortExpenses += value;
                            }
                        }

                        //multiplying it with the CARDINALITY of the Port's associated Value Exchange and nullifying if necessary
                        if (valuePort.getResource().hasProperty(E3value.vp_out_connects_ve)) {//if it's an (outgoing) ValuePort
                            //add the expenses to the valuation
                            valuePortValuation += valuePortExpenses;
                            if (ideal == true) {//If we want the expected case
                                if (isHidden(valuePort.getResource().getProperty(E3value.vp_out_connects_ve).getResource())) {//and it's respective (outgoing) Value Exchange is hidden
                                    valuePortValuation = 0; //nullify it
                                }
                            } else//If we want the real case
                             if (isNonOccurring(valuePort.getResource().getProperty(E3value.vp_out_connects_ve).getResource())) {//and it's respective (outgoing) Value Exchange is nonOccurring
                                    valuePortValuation = 0; //nullify it
                                }
                            valuePortValuation *= getCardinality(valuePort.getResource().getProperty(E3value.vp_out_connects_ve).getResource());// then multiply with cardinality of respective (outgoing) ve                             
                        } else if (valuePort.getResource().hasProperty(E3value.vp_in_connects_ve)) {//if it's an (incoming) ValuePort
                            //deduct expenses from valuation
                            valuePortValuation -= valuePortExpenses;
                            if (ideal == true) {//If we want the expected case
                                if (isHidden(valuePort.getResource().getProperty(E3value.vp_in_connects_ve).getResource())) {//and it's respective (incoming) Value Exchange is hidden
                                    valuePortValuation = 0; //nullify it
                                }
                            } else//If we want the real case
                             if (isNonOccurring(valuePort.getResource().getProperty(E3value.vp_in_connects_ve).getResource())) {//and it's respective (incoming) Value Exchange is nonOccurring
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
     * Computes and returns series for all actors across an interval of either
     * occurrence rates (if the given resource is a need) or counts (if given
     * resource is a Market segment)
     *
     * @param needOrMarketSegment the need or market segment to be used on the
     * X-axis
     * @param startValue minimum occurrence rate of need
     * @param endValue maximum occurrence rate of need
     * @param ideal ideal or sub-ideal case
     * @return a map of <Actor,XY series>, where XYSeries represents the
     * sensitivity chart of each actor. The X-axis shows the parameter range
     *  and the Y-axis shows the financial result.
     */
    public Map<Resource, XYSeries> getSeries(Resource needOrMarketSegment, int startValue, int endValue, boolean ideal) {
        //make sure the resources are from this model
        needOrMarketSegment = model.getResource(needOrMarketSegment.getURI());
        Map<Resource, XYSeries> actorSeriesMap = new HashMap();
        Set<Resource> actors = this.getActorsAndMarketSegments();
        for (Resource actor : actors) {
            XYSeries actorSeries = new XYSeries(actor.getProperty(E3value.e3_has_name).getString());
            actorSeriesMap.put(actor, actorSeries);
        }

        double initialValue = 0;

        if (needOrMarketSegment.hasProperty(RDF.type, (E3value.market_segment))) {
            initialValue = Double.valueOf(this.getMarketSegmentCount(needOrMarketSegment));
        } else if (needOrMarketSegment.hasProperty(RDF.type, (E3value.start_stimulus))) {
            initialValue = Double.valueOf(this.getNeedOccurrence(needOrMarketSegment));
        }

        //we only need 50 values so divide interval to 50
        if (startValue < endValue) {
            double step = ((float) endValue - (float) startValue) / 50;
            //calculate profit for each (occurence) value:
            for (double i = startValue; i <= endValue; i += step) {
                if (needOrMarketSegment.hasProperty(RDF.type, (E3value.market_segment))) {
                    this.updateCount(needOrMarketSegment, i);
                } else if (needOrMarketSegment.hasProperty(RDF.type, (E3value.start_stimulus))) {
                    this.updateNeedOccurrence(needOrMarketSegment, i);
                }
                //For each actor
                for (Resource actor : actors) {
                    //add it's profit to the relevant series
                    actorSeriesMap.get(actor).add(i, this.getTotalForActor(actor, ideal));
                }
            }
        } else if (startValue == endValue) {
            double i = startValue;
            if (needOrMarketSegment.hasProperty(RDF.type, (E3value.market_segment))) {
                this.updateCount(needOrMarketSegment, i);
            } else if (needOrMarketSegment.hasProperty(RDF.type, (E3value.start_stimulus))) {
                this.updateNeedOccurrence(needOrMarketSegment, i);
            }

            //For each actor
            for (Resource actor : actors) {
                //add it's profit to the relevant series
                actorSeriesMap.get(actor).add(i, this.getTotalForActor(actor, ideal));
            }
        } else {
            PopUps.infoBox("Start value must be lower than end value!", "Error");
        }

        if (needOrMarketSegment.hasProperty(RDF.type, (E3value.market_segment))) {
            this.updateCount(needOrMarketSegment, initialValue);
        } else if (needOrMarketSegment.hasProperty(RDF.type, (E3value.start_stimulus))) {

            this.updateNeedOccurrence(needOrMarketSegment, initialValue);
        }

        return actorSeriesMap;
    }

    /**
     * Computes averages for each of the series and attaches them to the chart
     *
     * @param seriesMap containing the series to calculate averages on and the
     * resources they belong to
     * @return the same seriesMap, but with the series keys containing Averages
     */
    public Map<Resource, XYSeries> appendAverages(Map<Resource, XYSeries> seriesMap ) {
        for (Resource actor : seriesMap.keySet()) {
            double sum = 0;
            for (Object dataItemObject : seriesMap.get(actor).getItems()) {
                XYDataItem dataItem = (XYDataItem) dataItemObject;
                sum += dataItem.getY().doubleValue();
            }
            double mean = sum / seriesMap.get(actor).getItemCount();
            DecimalFormat df = new DecimalFormat("#.##");
            seriesMap.get(actor).setKey(seriesMap.get(actor).getKey() + "\n [Avg.\t = \t" + df.format(mean) + "]");;
        }
        return seriesMap;
    }

}
