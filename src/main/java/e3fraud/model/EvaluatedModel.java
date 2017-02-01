/**
 * *****************************************************************************
 * Copyright (C) 2016 Bob Rubbens
 *
 *
 * This file is part of e3tool.
 *
 * e3tool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * e3tool is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with e3tool.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************
 */
package e3fraud.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.formula.FormulaParseException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

import e3fraud.parser.FileParser;
import e3fraud.vocabulary.E3value;

public class EvaluatedModel {

    public static final String alphaNumericID = "[a-zA-Z][a-zA-Z0-9]*";
    public static final String alphaNumericWithSpacesID = "[a-zA-Z][a-zA-Z0-9 ]*";
    public static final String zeroOrNumberUID = "(?:[1-9][0-9]*?|0)";

    private final boolean DEBUG = false;

    private Model model;
    private Map<String, Integer> rowMap = new HashMap<>();
    private Map<String, String> uidMap = new HashMap<>();
    private Workbook wb;
    private Sheet sheet;
    private int nextRow;
    private Optional<String> lastUID;
    private Optional<String> lastFormulaName;

    /**
     * Matches strings like: e3{'Subscription fee'.VALUATION}
     */
    private static final Pattern newNamePat = Pattern.compile(
            "e3\\{'(" + alphaNumericWithSpacesID + ")'\\.(" + alphaNumericID + ")\\}"
    );
    /**
     * Matches strings like: e3{ValuePort('Subscription fee').VALUATION}
     */
    private static final Pattern oldNamePat = Pattern.compile(
            "e3\\{" + alphaNumericID + "\\('(" + alphaNumericWithSpacesID + ")'\\)\\.(" + alphaNumericID + ")\\}"
    );
    /**
     * Matches strings like: #123.VALUATION
     */
    private static final Pattern refPat = Pattern.compile(
            "(#" + zeroOrNumberUID + ").(" + alphaNumericID + ")"
    );
    
        /**
     * Matches strings like: 'Subscription fee'.VALUATION
     */
    private static final Pattern namePat = Pattern.compile(
            "'(" + alphaNumericWithSpacesID + ")'.(" + alphaNumericID + ")"
    );

    /**
     * Matches strings like: e3{VALUATION}
     */
    private static final Pattern formulaPat = Pattern.compile(
            "e3\\{(" + alphaNumericID + ")\\}"
    );

    /**
     * Matches strings like: e3{#123.VALUATION}
     */
    private static final Pattern uidPat = Pattern.compile(
            "e3\\{(#" + zeroOrNumberUID + "\\." + alphaNumericID + ")\\}"
    );

    private EvaluatedModel(Model model) {
        this.model = model;
    }

    /**
     * Returns an EvaluatedModel wrapped in an optional if everything goes
     * right. If the model is inconsistent (i.e. a formula refers to some entity
     * or formula that does not exist), empty is returned.
     */
    public static Optional<EvaluatedModel> evaluateModel(Model model) {
        try {
            Optional<EvaluatedModel> em = Optional.of(new EvaluatedModel(model));
            em.get().evaluateAll();
            return em;
        } catch (FormulaParseException e) {
            System.out.println("Could not parse formula!");
            e.printStackTrace();
            return Optional.empty();
        }
    }
    
    public static class ModelOrError {
    	ModelOrError() {
    		optionalModel = Optional.empty();
    		badUID = Optional.empty();
    		badFormulaName = Optional.empty();
    	}
    	
    	public Optional<EvaluatedModel> optionalModel;
    	public Optional<String> badUID;
    	public Optional<String> badFormulaName;
    }
    
    public static ModelOrError evaluateModelOrError(Model model) {
		EvaluatedModel em = new EvaluatedModel(model);

        try {
            em.evaluateAll();

        	ModelOrError moe = new ModelOrError();
        	moe.optionalModel = Optional.of(em);

        	return moe;
        } catch (FormulaParseException e) {
            System.out.println("Could not parse formula!");
            e.printStackTrace();

        	ModelOrError moe = new ModelOrError();
        	moe.badUID = em.lastUID;
        	moe.badFormulaName = em.lastFormulaName;

            return moe;
        }
    }

    /**
     * This does a bulk update and puts the EvaluatedModel in its initial state.
     * If an error occurs, this function throws an exception. Once called this
     * function should not be called again. Instead just create a new
     * EvaluatedModel.
     */
    private void evaluateAll() {
        // To keep track of our progress in the excel sheet
        nextRow = 0;

        // For each thing with a UID in the model...
        ResIterator it = model.listResourcesWithProperty(E3value.e3_has_uid);
        while (it.hasNext()) {
            Resource res = it.next();
            
            // Ignore value offerings
            if (res.getProperty(RDF.type) == E3value.value_offering) {
            	continue;
            }
            
            String name;
            // Save the mapping from name to uid
            if(res.hasProperty(E3value.e3_has_name)){
				name = res.getProperty(E3value.e3_has_name).getString();
            } else {
            	System.err.println("Element has no name! Reverting to default \"temp\", but this is a bug!");
            	name="temp";
			}

            String uid = res.getProperty(E3value.e3_has_uid).getString();

            uidMap.put(name, uid);

            // For each formula attached to this entity...
            StmtIterator stmtIt = res.listProperties(E3value.e3_has_formula);

            while (stmtIt.hasNext()) {
                String formula = stmtIt.next().getString();
//				System.out.println("Considering \"" + formula + "\" of " + name + "(#" + uid + ")");
                String formulaName = formula.split("=")[0];
                // Give it its own row
                rowMap.put("#" + uid + "." + formulaName, nextRow++);

//				System.out.println("Putting: " + "#" + uid + "." + formulaName + " at " + (nextRow - 1));
            }
        }

        // Now we're going to populate the excel sheet
        wb = new XSSFWorkbook();
        sheet = wb.createSheet("this");

        for (int i = 0; i < nextRow; i++) {
            sheet.createRow(i);
        }

        // Again, for each UID...
        it = model.listResourcesWithProperty(E3value.e3_has_uid);
        while (it.hasNext()) {
            Resource res = it.next();

            String uid = res.getProperty(E3value.e3_has_uid).getString();

            // And for each formula of this UID...
            StmtIterator stmtIt = res.listProperties(E3value.e3_has_formula);
            while (stmtIt.hasNext()) {
                // Get the formula name and value
                String formulaEntry = stmtIt.next().getString();
                String formulaName = formulaEntry.split("=")[0];
                // Remove all e3references
                String formula = e3ExpressionToExcel(uid, formulaEntry.split("=")[1]);
                
                // Save the debug information in case of an exception
                lastUID = Optional.of(uid);
                lastFormulaName = Optional.of(formulaName);
                // Create the cell containing the UID and formulaname and fill in the formula
                // TODO: This is actually a superfluous column, since we save the mapping
                // from reference (i.e. #123.VALUATION) to row. Therefore, this should be 
                // removed to improve the memory footprint, or should be toggleable, because
                // it's nice to have for debugging.
                Cell nameCell = sheet.getRow(rowMap.get("#" + uid + "." + formulaName)).createCell(0);
                nameCell.setCellValue("#" + uid + "." + formulaName);
                Cell cell = sheet.getRow(rowMap.get("#" + uid + "." + formulaName)).createCell(1);
                cell.setCellFormula(formula);
            }
        }

        XSSFFormulaEvaluator.evaluateAllFormulaCells(wb);

        // If debugging write the xlsx to disk in the current working directory
        if (DEBUG) {
            try {
                FileOutputStream fileOut = new FileOutputStream("test.xlsx");
                wb.write(fileOut);
                fileOut.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets the value of an e3reference. The e3reference should have the form of
     * either: - 'Subscription fee'.VALUATION - #123.VALUATION
     *
     * If a different format is used the function will return empty. If the
     * value cannot be found, empty will be returned as well. If the value is
     * not numeric, empty will be returned as well.
     */
    public Optional<Double> valueOf(String e3ref) {
        // Check if the e3ref is in the name format
        // (i.e. 'Subscription fee'.VALUATION
        Matcher match = namePat.matcher(e3ref);

        // If so, convert it to UID format.
        if (match.matches()) {
            String name = match.group(1);
            String attr = match.group(2);

            if (!uidMap.containsKey(name)) {
                System.out.println("Name \"" + name + "\" is unknown");
                return Optional.empty();
            }

            e3ref = "#" + uidMap.get(name) + "." + attr;
        }
        

        // Return empty if the reference does not exist
        if (!rowMap.containsKey(e3ref)) {
            System.out.println("Could not find: " + e3ref);
            new Exception().printStackTrace(System.out);
            return Optional.empty();
        }

        int row = rowMap.get(e3ref);

        // Return the appropriate value.
        Cell cell = sheet.getRow(row).getCell(1);
        // As soon as we start using their new API we'll
        // use the appropriate function call.
        CellType cellType = cell.getCellTypeEnum();
                System.out.println("valueOf("+e3ref+")="+cell.getNumericCellValue());
        switch (cellType) {
            case NUMERIC:
                return Optional.of(cell.getNumericCellValue());
            case FORMULA:
                return Optional.of(cell.getNumericCellValue());
            default:
                System.out.println(e3ref + " is not numeric but " + cellType);
                return Optional.empty();
        }

    }

    /**
     * Replaces all locals in an expression (e.g. <code>e3{VALUATION}</code>
     * with actual references to an entity (e.g.
     * <code>e3{#123.VALUATION}</code>).
     *
     * @param uid The uid of the entity that contains the formula
     * @param formula The formula in question.
     * @return The formula with local references removed.
     */
    public String replaceLocals(String uid, String formula) {
        if (uid == null) {
            return formula;
        }

        String newF = formulaPat.matcher(formula).replaceAll("e3{#" + uid + ".$1}");

        if (DEBUG && !formula.equals(newF)) {
            System.out.println("Old: " + formula);
            System.out.println("New: " + newF);
        }

        return newF;
    }

    /**
     * Replaces name references in the formula (i.e. 'Subscription
     * fee'.VALUATION)
     */
    public String replaceNames(String formula) {
        // Replace all occurrences like e3{'Subscription fee'.VALUATION}
        // With e3{#123.VALUATION}
        Matcher match = newNamePat.matcher(formula);

        String oldFormula = formula;

        while (match.find()) {
            String name = match.group(1);
            String attr = match.group(2);
            int start = match.start();
            int end = match.end();

            formula = formula.substring(0, start)
                    + "e3{#" + uidMap.get(name) + "." + attr + "}"
                    + formula.substring(end);
            match = newNamePat.matcher(formula);
        }

        // Replace all occurrences like e3{SomeValuePort('Subscription fee').VALUATION}
        // With e3{#123.VALUATION}
        match = oldNamePat.matcher(formula);

        while (match.find()) {
            String name = match.group(1);
            String attr = match.group(2);
            int start = match.start();
            int end = match.end();

            formula = formula.substring(0, start)
                    + "e3{#" + uidMap.get(name) + "." + attr + "}"
                    + formula.substring(end);

            match = oldNamePat.matcher(formula);
        }

        if (DEBUG && oldFormula != formula) {
            System.out.println("Old: " + oldFormula);
            System.out.println("New: " + formula);
        }

        return formula;
    }

    /**
     * Replaces e3 UID references (i.e. e3{#123.VALUATION}) with their
     * respective cell reference (i.e. B3) in the internal excel spreadsheet.
     */
    public String replaceUIDs(String formula) {
        Matcher match = uidPat.matcher(formula);

        String oldFormula = formula;

        while (match.find()) {
            String arg = match.group(1);

//			System.out.println("Arg:" + arg);
            if (rowMap.containsKey(arg)) {
                int start = match.start();
                int end = match.end();

//				System.out.println("Match: " + match.group(0));
                formula = formula.substring(0, start)
                        // Excel is 0 indexed. Hence, if rowMap.get() actually returns 0,
                        // it means the "first" row in excel, which is actually 1. Hence,
                        // we increment it by 1.
                        // And we use column B because column A holds all the uid + formula names.
                        // This can be removed/made optional/triggerable with a flag in the future
                        // if the current approach is too slow.
                        + "B" + (rowMap.get(arg) + 1)
                        + formula.substring(end);

                match = uidPat.matcher(formula);
            } else {
                System.out.println("Not found!");
            }
        }

        if (DEBUG && oldFormula != formula) {
            System.out.println("Old: " + oldFormula);
            System.out.println("New: " + formula);
        }

        return formula;
    }

    /**
     * Shorthand for turning an e3 expression into an excel expression
     */
    public String e3ExpressionToExcel(String uidScope, String e3expr) {
        return Arrays.asList(e3expr).stream()
                .map(e -> replaceLocals(uidScope, e3expr))
                .map(this::replaceNames)
                .map(this::replaceUIDs)
                .findFirst()
                .get();
    }

    /**
     * Argument reference can only be of form <code>#123.VALUATION</code>.
     * uidScope contains the UID of the enity that owns the formula. This is
     * such that local references (e.g. e3{VALUATION}) can be resolved. If null
     * is passed, these references will simply not be resolved if present.
     */
     public void changeExistingFormula(String reference, String uidScope, double value) {
        // If the row does not exists, abort
        if (!rowMap.containsKey(reference)) {
            System.out.println("Reference \"" + reference + "\" refers to non-existing formula.");
            return;
        }
        // Get the row, convert the formula, change the expression, update the sheet
        int row = rowMap.get(reference);

        Cell cell = sheet.getRow(row).getCell(1);
        cell.setCellValue(value);
        cell.setCellType(CellType.NUMERIC);
    }
    /**
     * Argument reference can only be of form <code>#123.VALUATION</code>.
     * uidScope contains the UID of the enity that owns the formula. This is
     * such that local references (e.g. e3{VALUATION}) can be resolved. If null
     * is passed, these references will simply not be resolved if present.
     */
     public void changeExistingFormula(String reference, String uidScope, String formula) {
        // If the row does not exists, abort
        if (!rowMap.containsKey(reference)) {
            System.out.println("Reference \"" + reference + "\" refers to non-existing formula.");
            return;
        }
        // Get the row, convert the formula, change the expression, update the sheet
        int row = rowMap.get(reference);

        formula = e3ExpressionToExcel(uidScope, formula);

        Cell cell = sheet.getRow(row).getCell(1);
        cell.setCellFormula(formula);             
    }
     
     public void reEvaluate(){
                 XSSFFormulaEvaluator.evaluateAllFormulaCells(wb);
     }

    /**
     * Argument reference can only be of form <code>#123.VALUATION</code>.
     * uidScope contains the UID of the enity that owns the formula. This is
     * such that local references (e.g. e3{VALUATION}) can be resolved. If null
     * is passed, these references will simply not be resolved if present.
     */
    public void addNewFormula(String reference, String uidScope, String formula) {
        // If the formula already exists, or is not in good form, abort.
        if (!refPat.matcher(reference).matches()) {
            System.out.println("\"" + reference + "\" is not a valid e3value UID reference.");
            return;
        }

        if (rowMap.containsKey(reference)) {
            System.out.println("Reference \"" + reference + "\" already exists.");
            return;
        }
        // Convert the formula, get a new row, create the row and cells,
        // and update the sheet
        formula = e3ExpressionToExcel(uidScope, formula);    
        int newRow = nextRow++;
        rowMap.put(reference, newRow);
         //System.out.println("ADding "+formula+" to "+reference + "(row "+newRow+")");

        Row row = sheet.createRow(newRow);
        row.createCell(0).setCellValue(reference);
        row.createCell(1).setCellFormula(formula);

        //XSSFFormulaEvaluator.evaluateAllFormulaCells(wb);
    }

    /**
     * Argument reference can only be of form <code>#123.VALUATION</code>.
     * uidScope contains the UID of the enity that owns the formula. This is
     * such that local references (e.g. e3{VALUATION}) can be resolved. If null
     * is passed, these references will simply not be resolved if present and
     * will cause an exception.
     *
     * A row in the internel excel spreadsheet will be created if the reference
     * does not exist yet. If it does, it will be changed. In both cases the
     * entire sheet will be updated.
     */
    public void addOrChangeFormula(String reference, String uidScope, String formula) {
        if (rowMap.containsKey(reference)) {
            changeExistingFormula(reference, uidScope, formula);
        } else {
            addNewFormula(reference, uidScope, formula);            
        }
        XSSFFormulaEvaluator.evaluateAllFormulaCells(wb);
    }

    /**
     * Example program of how EvaluatedModel is supposed to be used.
     */
    public static void main(String[] args) {
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        E3Model model = FileParser.parseFile(new File("src/test/resources/evaluation_rdf_test2.rdf"));

        for (int i = 0; i < 100; i++) {
            System.out.println(i);
            EvaluatedModel eModel = new EvaluatedModel(model.getJenaModel());
        }

//		Optional<Double> val = eModel.valueOf("#7.VALUATION");
//		System.out.println(val.get());
//		{
//			Optional<Double> val = eModel.valueOf("#0.VALUATION");
//			Optional<Double> val2 = eModel.valueOf("'SubscriptionFee'.VALUATION");
//			
//			System.out.println(val.isPresent());
//			// Only call get if it is present!
//			System.out.println(val.get());
//			System.out.println(val2.isPresent());
//			System.out.println(val2.get());
//		}
//		
//		eModel.changeExistingFormula("#70.MYFUNKYID", null, 20+"");
//
//		{
//			Optional<Double> val = eModel.valueOf("#0.VALUATION");
//			Optional<Double> val2 = eModel.valueOf("'SubscriptionFee'.VALUATION");
//			
//			System.out.println(val.isPresent());
//			// Only call get if it is present!
//			System.out.println(val.get());
//			System.out.println(val2.isPresent());
//			System.out.println(val2.get());
//		}
        System.out.println("Done");
    }
}
