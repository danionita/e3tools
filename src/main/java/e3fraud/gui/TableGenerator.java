/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package e3fraud.gui;

import com.hp.hpl.jena.rdf.model.Resource;
import e3fraud.model.E3Model;
import e3fraud.vocabulary.E3value;
import java.util.List;
import javax.swing.JTable;

/**
 *
 * @author Dan
 */
public class TableGenerator {
    public static JTable generateTable(E3Model fraudModel, E3Model valueModel){
    Object columnNames[] = { "Actor", "Result", "Expected result", "Loss/Gain"};       
    Object rowData[][] = new Object[fraudModel.getActors().size()][4];
    
    int row=0;
    for (Resource actor : fraudModel.getActors()){
        rowData[row][0] = actor.getProperty(E3value.e3_has_name).getLiteral().toString();
        
        double result = fraudModel.getTotalForActor(actor, false);
        rowData[row][1] = result;                
       
        String actorUID = actor.getProperty(E3value.e3_has_uid).getString();
        List<Long> colludedActors = fraudModel.getFraudChanges().colludedActors;
        double expectedResult = 0;
        if (colludedActors.contains(Long.parseLong(actorUID))) {
            for (long colludedActorUID : colludedActors) {
                Resource colludedActor = valueModel.getJenaModel().listResourcesWithProperty(E3value.e3_has_uid,String.valueOf(colludedActorUID)).next();
                    expectedResult += valueModel.getTotalForActor(colludedActor, true);
                }
        }
        else{
            expectedResult = valueModel.getTotalForActor(actor, true);
        }
        
        rowData[row][2] = expectedResult;
        
        rowData[row][3] = result - expectedResult;
        
        row++;
    }    
        
    
    
    JTable table = new JTable(rowData, columnNames);
    return table;
    }
    
   public static JTable generateTable(E3Model valueModel){
    Object columnNames[] = { "Actor", "Result" };       
    Object rowData[][] = new Object[valueModel.getActors().size()][2];
    
    int row=0;
    for (Resource actor : valueModel.getActors()){
        rowData[row][0] = actor.getProperty(E3value.e3_has_name).getLiteral().toString();
        
        double result = valueModel.getTotalForActor(actor, false);
        rowData[row][1] = result;       
        row++;
    }    
    
    JTable table = new JTable(rowData, columnNames);
    return table;
    }
    
    
}
