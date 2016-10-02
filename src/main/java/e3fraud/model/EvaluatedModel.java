package e3fraud.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import e3fraud.parser.FileParser;
import e3fraud.vocabulary.E3value;

public class EvaluatedModel {

	public static final String alphaNumericID = "[a-zA-Z][a-zA-Z0-9]*";
	public static final String zeroOrMoreUID = "(?:[1-9][0-9]*?|0)";
	
	private final boolean DEBUG = false;

	private Model model;
	private Map<String, Integer> rowMap = new HashMap<>();
	private Map<String, String> uidMap = new HashMap<>();
	private Workbook wb;
	private Sheet sheet;
	
	public EvaluatedModel(Model model) {
		this.model = model;
		
		evaluateAll();
	}
	
	public void evaluateAll() {
		int nextRow = 0;

		ResIterator it = model.listResourcesWithProperty(E3value.e3_has_uid);
		while (it.hasNext()) {
			Resource res = it.next();
			
			String name = res.getProperty(E3value.e3_has_name).getString();
			String uid = res.getProperty(E3value.e3_has_uid).getString();
			
			uidMap.put(name, uid);
			
			StmtIterator stmtIt = res.listProperties(E3value.e3_has_formula);
			while (stmtIt.hasNext()) {
				String formula = stmtIt.next().getString();
				String formulaName = formula.split("=")[0];
				rowMap.put("#" + uid + "." + formulaName, nextRow++);
			}
		}
		
		wb = new XSSFWorkbook();
		sheet = wb.createSheet("this");
		
		for (int i = 0; i < nextRow; i++) {
			sheet.createRow(i);
		}
		
		it = model.listResourcesWithProperty(E3value.e3_has_uid);
		while (it.hasNext()) {
			Resource res = it.next();
			
			String name = res.getProperty(E3value.e3_has_name).getString();
			String uid = res.getProperty(E3value.e3_has_uid).getString();
			
			StmtIterator stmtIt = res.listProperties(E3value.e3_has_formula);
			while (stmtIt.hasNext()) {
				String formulaEntry = stmtIt.next().getString();
				String formulaName = formulaEntry.split("=")[0];
				String formula = formulaEntry.split("=")[1];
				
				String formula1 = replaceLocals(uid, formula);
				String formula2 = replaceNames(formula1);
				String formula3 = replaceUIDs(formula2);
				
				Cell nameCell = sheet.getRow(rowMap.get("#" + uid + "." + formulaName)).createCell(0);
				nameCell.setCellValue("#" + uid + "." + formulaName);
				Cell cell = sheet.getRow(rowMap.get("#" + uid + "." + formulaName)).createCell(1);
				cell.setCellFormula(formula3);
			}
		}
		
		XSSFFormulaEvaluator.evaluateAllFormulaCells(wb);
		
		if (DEBUG) {
			try {
				FileOutputStream fileOut = new FileOutputStream("test.xlsx");
				wb.write(fileOut);
				fileOut.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public Optional<Double> valueOf(String e3ref) {
		Pattern nameMatch = Pattern.compile("'(" + alphaNumericID + ")'.(" + alphaNumericID + ")");
		Matcher match = nameMatch.matcher(e3ref);

		if (match.matches()) {
			String name = match.group(1);
			String attr = match.group(2);
			
			if (!uidMap.containsKey(name)) {
				System.out.println("Name \"" + name + "\" is unknown");
				return Optional.empty();
			}

			e3ref = "#" + uidMap.get(name) + "." + attr; 
		}
		
		if (!rowMap.containsKey(e3ref)) {
			System.out.println("Not found: " + e3ref);
			return Optional.empty();
		}

		int row = rowMap.get(e3ref);
		
		Cell cell = sheet.getRow(row).getCell(1);
		CellType cellType = cell.getCellTypeEnum();
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
	
	public String replaceLocals(String uid, String formula) {
		String old = formula;
		String newF = formula.replaceAll("e3\\{(" + alphaNumericID + ")\\}", "e3{#" + uid + ".$1}");
		if (DEBUG && !old.equals(newF)) {
			System.out.println("Old: " + old);
			System.out.println("New: " + newF);
		}
		
		return newF;
	}
	
	public String replaceNames(String formula) {
		Pattern newNamePat = Pattern.compile(
				"e3\\{'(" + alphaNumericID + ")'\\.(" + alphaNumericID + ")\\}"
				);		
		Pattern oldNamePat = Pattern.compile(
				"e3\\{" + alphaNumericID + "\\('(" + alphaNumericID + ")'\\)\\.(" + alphaNumericID + ")\\}"
				);

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
	
	public String replaceUIDs(String formula) {
		Pattern uidPat = Pattern.compile(
				"e3\\{(#" + zeroOrMoreUID + "\\." + alphaNumericID + ")\\}"
				);		

		Matcher match = uidPat.matcher(formula);
		
		String oldFormula = formula;
		
		while (match.find()) {
			String arg = match.group(1);

			if(rowMap.containsKey(arg)) {
				int start = match.start();
				int end = match.end();

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
	 * Example program of how EvaluatedModel is supposed to be used.
	 */
	public static void main(String[] args) {
		E3Model model = FileParser.parseFile(new File("src/test/resources/evaluation_rdf_test.rdf"));
		EvaluatedModel eModel = new EvaluatedModel(model.getJenaModel());

		Optional<Double> val = eModel.valueOf("#0.VALUATION");
		Optional<Double> val2 = eModel.valueOf("'SubscriptionFee'.VALUATION");
		
		System.out.println(val.isPresent());
		// Only call get if it is present!
		System.out.println(val.get());
		System.out.println(val2.isPresent());
		System.out.println(val2.get());
		
		System.out.println("Done");
	}
}
