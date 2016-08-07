package design;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.TransferHandler;

import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;

import design.export.JSONExport;
import design.export.RDFExport;
import e3fraud.gui.FraudWindow;
import e3fraud.gui.ProfitabilityAnalyser;
import e3fraud.model.E3Model;

@SuppressWarnings(value = { "serial" })
public class EditorActions {
	public static abstract class BaseAction extends AbstractAction {
		public Main main;

		public BaseAction(String caption, Main main) {
			super(caption);
			this.main = main;
		}
	}
	
	public static class NewTab extends BaseAction {
		private boolean isFraud;

		public NewTab(Main main, boolean isFraud) {
			super("New e3 "
					+ (isFraud ? "fraud" : "value")
					+ " value model (ctrl+"
					+ (isFraud ? "n" : "m") 
					+ ")", main);
			this.isFraud = isFraud;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			main.addNewTabAndSwitch(isFraud);
		}
	}
	
	public static class OpenFile extends BaseAction {
		public OpenFile(Main main) {
			super("Open...", main);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser fc = Utils.getE3FileChooser();

			Optional<E3Graph> graph = Utils.openFile(main.mainFrame, fc);
			if (graph.isPresent()) {
				main.addNewTabAndSwitch(graph.get());
			}
		}
	}
	
	public static class Save extends BaseAction {
		public Save(Main main) {
			super("Save... (ctrl+s)", main);
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (main.getCurrentGraph().file == null) {
				Utils.saveAs(main.mainFrame, main.getCurrentGraph());
			} else {
				Utils.saveToFile(main.mainFrame, main.getCurrentGraph(), main.getCurrentGraph().file);
			}
		}
	}
	
	public static class SaveAs extends BaseAction {
		public SaveAs(Main main) {
			super("Save as...", main);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Utils.saveAs(main.mainFrame, main.getCurrentGraph());
		}
	}
	
	public static class ExportRDF extends BaseAction {
		public ExportRDF(Main main) {
			super("RDF", main);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			System.out.println(new RDFExport(main.getCurrentGraph(), true).toString());
		}
	}
	
	public static class ExportJSON extends BaseAction {
		public ExportJSON(Main main) {
			super("JSON", main);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			new JSONExport(main.getCurrentGraph(), main.getCurrentGraphTitle()).generateJSON();
		}
	}
	
	public static class ExportImage extends BaseAction {
		public ExportImage(Main main) {
			super("Image", main);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO: Implement this (see jgraphx manual, it contains a few bits about images)
		}
	}
	
	public static class ImportRDF extends BaseAction {
		public ImportRDF(Main main) {
			super("RDF", main);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			
		}
	}
	
	public static class ImportXSVG extends BaseAction {
		public ImportXSVG(Main main) {
			super("XSVG", main);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			
		}
	}
	
	public static class Print extends BaseAction {
		public Print(Main main) {
			super("Print...", main);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO: Implement this
		}
	}
	
	public static class Cut extends BaseAction {
		public Cut(Main main) {
			super("Cut (ctrl+x)", main);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
            TransferHandler.getCutAction().actionPerformed(new ActionEvent(main.getCurrentGraphComponent(), -1, null));
		}
	}
	
	public static class Copy extends BaseAction {
		public Copy(Main main) {
			super("Copy (ctrl+c)", main);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
            TransferHandler.getCopyAction().actionPerformed(new ActionEvent(main.getCurrentGraphComponent(), -1, null));
		}
	}
	
	public static class Paste extends BaseAction {
		public Paste(Main main) {
			super("Paste (ctrl+v)", main);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
            TransferHandler.getPasteAction().actionPerformed(new ActionEvent(main.getCurrentGraphComponent(), -1, null));
		}
	}
	
	public static class Delete extends BaseAction {
		public Delete(Main main) {
			super("Delete (delete)", main);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			E3Graph graph = main.getCurrentGraph();
			graph.getModel().beginUpdate();
			try {
				graph.removeCells(graph.getSelectionCells());
			} finally {
				graph.getModel().endUpdate();
			}
		}
	}
	
	public static class SelectAll extends BaseAction {
		public SelectAll(Main main) {
			super("Select all (ctrl+a)", main);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO: Implement support for multiselect
		}
	}
	
	public static class Undo extends BaseAction {
		public Undo(Main main) {
			super("Undo (crtrl+z)", main);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			main.getCurrentGraphComponent().undoManager.undo();
		}
	}
	
	public static class Redo extends BaseAction{
		public Redo(Main main) {
			super("Redo (ctrl+y)", main);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			main.getCurrentGraphComponent().undoManager.redo();
		}
	}
	
	public static class Find extends BaseAction {
		public Find(Main main) {
			super("Find", main);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			
		}
	}

	public static class ZoomIn extends BaseAction {
		public ZoomIn(Main main) {
			super("Zoom in (ctrl+numpad plus)", main);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			main.getCurrentGraphComponent().zoomIn();
		}
	}
	
	
	public static class ZoomOut extends BaseAction {
		public ZoomOut(Main main) {
			super("Zoom out (ctrl+numpad minus)", main);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			main.getCurrentGraphComponent().zoomOut();
		}
	}
	
	
	public static class ToggleGrid extends BaseAction {
		public ToggleGrid(Main main) {
			super("Toggle grid", main);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO: Implement this
		}
	}
	
	public static class ToggleLabels extends BaseAction {
		private boolean makeVisible;

		public ToggleLabels(Main main, boolean makeVisible) {
			super((makeVisible ? "Show" : "Hide") + " all labels", main);
			this.makeVisible = makeVisible;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO: Implement this
		}
	}
	
	public static class ToggleValueObjects extends BaseAction {
		private boolean makeVisible;

		public ToggleValueObjects(Main main, boolean makeVisible) {
			super((makeVisible ? "Show" : "Hide") + " all ValueObjects", main);
			this.makeVisible = makeVisible;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO: Implement this
		}
	}
	
	
	public static class DuplicateModel extends BaseAction {
		public DuplicateModel(Main main) {
			super("Create duplicate", main);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			main.addNewTabAndSwitch(new E3Graph(main.getCurrentGraph()));
		}
	}
	
	
	public static class ChangeModelType extends BaseAction {
		public ChangeModelType(Main main) {
			super("Change type", main);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if (main.getCurrentGraph().isFraud) {
				int response = JOptionPane.showConfirmDialog(
						Main.mainFrame,
						"Converting a fraud model to a value model will "
						+ "cause information about colluded actors, hidden "
						+ "transactions, and non-occurring transactions "
						+ "to be lost. Continue?",
						"Conversion confirmation",
						JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE
				);

				if (response == JOptionPane.OK_OPTION) {
					main.addNewTabAndSwitch(main.getCurrentGraph().toValue());
				}
			} else {
				main.addNewTabAndSwitch(main.getCurrentGraph().toFraud());
			}
		}
	}
	
	public static class ChangeModelTitle extends BaseAction {
		public ChangeModelTitle(Main main) {
			super("Change title", main);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			String newTitle = JOptionPane.showInputDialog(
					main.mainFrame,
					"Enter new model title",
					"Rename \"" + main.getCurrentGraphTitle() + "\"",
					JOptionPane.QUESTION_MESSAGE);

			main.getCurrentGraph().title = newTitle;
			main.setCurrentTabTitle(newTitle);
		}
	}
	
	public static class ShowValueObjectsPanel extends BaseAction {
		public ShowValueObjectsPanel(Main main) {
			super("ValueObjects...", main);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO: Maybe prefer greyed out menu item?
			if (main.views.getTabCount() == 0) {
				JOptionPane.showMessageDialog(
						Main.mainFrame,
						"A model must be opened to display its ValueObjects. Click File ➡ New model to open a new model.",
						"No model available",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			new ValueObjectDialog(main.getCurrentGraph()).show();;
		}
	}
	
	public static class ShowValueTransactionsPanel extends BaseAction {
		public ShowValueTransactionsPanel(Main main) {
			super("ValueTransactions...", main);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO: Implement this
		}
	}
	
	
	public static class ShowNetValueFlow extends BaseAction {
		public ShowNetValueFlow(Main main) {
			super("Net value flow... (ctrl+f)", main);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO: Implement this
		}
	}
	
	public static class AnalyzeTransactions extends BaseAction {
		public AnalyzeTransactions(Main main) {
			super("Analyze transactions (ctrl+h)", main);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO: Implement this
		}
	}
	
	public static class FraudGeneration extends BaseAction {
		public FraudGeneration(Main main) {
			super("Fraud generation...", main);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO: Convert this option to something greyed out, just like in the file menu?
			if (main.views.getTabCount() == 0) {
				JOptionPane.showMessageDialog(
						Main.mainFrame,
						"A model must be opened to analyze. Click File ➡ New model to open a new model.",
						"No model available",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			JFrame frame = new JFrame("Fraud analysis of \"" + main.getCurrentGraphTitle() + "\"");
			RDFExport rdfExporter = new RDFExport(main.getCurrentGraph(), true);
			FraudWindow fraudWindowInstance = new FraudWindow(new E3Graph(main.getCurrentGraph()), new E3Model(rdfExporter.model), main, frame); //, getCurrentGraphName());
			// TODO: Maybe add icons for fraud analysis as well?
			frame.add(fraudWindowInstance);
			frame.pack();
			frame.setVisible(true);
		}
	}
	
	
	public static class ProfitabilityChart extends BaseAction {
		public ProfitabilityChart(Main main) {
			super("Profitability chart...", main);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			RDFExport rdfExporter = new RDFExport(main.getCurrentGraph(), true);
			JFreeChart chart = ProfitabilityAnalyser.getProfitabilityAnalysis(new E3Model(rdfExporter.model), !main.getCurrentGraph().isFraud);
			if (chart != null) {
				ChartFrame chartframe1 = new ChartFrame("Profitability analysis of \"" + main.getCurrentGraphTitle() + "\"", chart);
				chartframe1.setPreferredSize(new Dimension(Main.DEFAULT_CHART_WIDTH, Main.DEFAULT_CHART_HEIGHT));
				chartframe1.pack();
				chartframe1.setLocationByPlatform(true);
				chartframe1.setVisible(true);
			}
		}
	}
	
	public static class OpenSite extends BaseAction {
		private String url;

		public OpenSite(Main main, String caption, String url) {
			super(caption, main);
			this.url = url;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				Utils.openWebpage(new URL(url));
			} catch (MalformedURLException ex) {
				java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
			}
		}
	}
	
	public static class OpenHelpWiki extends OpenSite {
		public OpenHelpWiki(Main main) {
			super(main, "Help Wiki (F1)", "https://github.com/danionita/e3tools/wiki");
		}
	}
	
	public static class OpenE3ValueWebsite extends OpenSite {
		public OpenE3ValueWebsite(Main main) {
			super(main, "e3value website", "http://e3value.few.vu.nl");
		}
	}
	
	public static class ShowAbout extends BaseAction {
		public ShowAbout(Main main) {
			super("About...", main);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			JOptionPane.showMessageDialog(null, "The e3tool integrates the e3value value modelling and e-business analysis methodology developed by Jaap Gordjin with the e3fraud fraud assessment methodology developed by Dan Ionita. \n This tool was developed at the University of Twente by Dan Ionita and Bob Rubbens. ");
		}
	}
}
