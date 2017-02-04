package design;

import static javax.swing.JOptionPane.INFORMATION_MESSAGE;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.TransferHandler;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;

import com.hp.hpl.jena.rdf.model.Model;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxConstants;

import design.checker.E3CheckDialog;
import design.checker.WalkEntireModel;
import design.checker.checks.FlowChecker;
import design.export.JSONExport;
import design.export.RDFExport;
import design.export.RDFExport.VTMode;
import design.info.Base;
import design.info.EndSignal;
import design.info.MarketSegment;
import design.info.StartSignal;
import design.info.ValueExchange;
import design.style.E3StyleEditor;
import e3fraud.gui.FraudWindow;
import e3fraud.gui.ProfitabilityAnalyser;
import e3fraud.model.E3Model;

@SuppressWarnings(value = {"serial"})
public class EditorActions {

    private static String conversionMessage[] = {"Converting a fraud model to a value model will "
        + "cause fraud annotations such as colluded actors, hidden"
        + "transfers, and non-occurring transfers "
        + "to be lost."
        + "\nContinue? (a duplicate will be created "
        + "before conversion)"};

    private static String invalidModelMessage[] = {"The current model contains unconnected "
        + "ports. This might cause unexpected results.",
        "Do you wish to continue?"};

    public static abstract class BaseAction extends AbstractAction {

        public Main main;

        public BaseAction(String caption, Main main) {
            super(caption);
            this.main = main;
        }
        
        public BaseAction(String caption, Icon icon, Main main) {
            super(caption, icon);
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
                    + ")", isFraud ? getIcon("page_red") : getIcon("page_green"),main);
            this.isFraud = isFraud;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            main.addNewTabAndSwitch(isFraud);
        }
    }

    public static class OpenFile extends BaseAction {

        public OpenFile(Main main) {
            super("Open...", getIcon("folder"), main);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fc = Utils.getE3FileChooser();

            Utils.openFile(main.mainFrame, fc).ifPresent(graph -> {
                main.addNewTabAndSwitch(graph);
            });
        }
    }
    
    public static class OpenRecent extends JMenu {
    	private Main main;

		public OpenRecent(Main main) {
			super("Open recent");
    		this.main = main;
    		
    		addMenuListener(new MenuListener() {
				@Override
				public void menuCanceled(MenuEvent arg0) { }

				@Override
				public void menuDeselected(MenuEvent arg0) {
					removeAll();
				}

				@Override
				public void menuSelected(MenuEvent arg0) {
					List<String> recentFiles = Utils.getRecentlyOpenedFiles();

					if (recentFiles.size() == 0) {
						JMenuItem mi = new JMenuItem("No recent files available");
    					mi.setEnabled(false);
    					add(mi);
					} else {
						recentFiles.stream().forEach(recentFile -> {
							File file = new File(recentFile);
							add(new AbstractAction(file.getName()) {
								@Override
								public void actionPerformed(ActionEvent e) {
									Optional<E3Graph> result = GraphIO.loadGraph(file.getAbsolutePath());

									if (!result.isPresent()) {
										JOptionPane.showMessageDialog(
												Main.mainFrame,
												"Error during file loading. Please make sure the file destination is accesible.",
												"Loading error",
												JOptionPane.ERROR_MESSAGE);
										
									} else {
										main.addNewTabAndSwitch(result.get());
										Utils.addRecentlyOpened(file);
									}
								}
							});
						});
					}
				}
    		});
    	}
    }

    public static class Save extends BaseAction {

        public Save(Main main) {
            super("Save (ctrl+s)", getIcon("disk"), main);
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
            RDFExport rdfExport = new RDFExport(main.getCurrentGraph(), false, VTMode.DERIVE_ORPHANED, false);
            Optional<String> result = rdfExport.getResult();

            // Do not export to rdf if there was an error
            if (!result.isPresent()) {
                Optional<String> error = rdfExport.getError();

                String errorString = "An error occurred while exporting to RDF. Please ensure that the model has no errors.";

                if (error.isPresent()) {
                    errorString += " The error:\n" + error.get();
                }

                JOptionPane.showMessageDialog(
                        Main.mainFrame,
                        errorString,
                        "Invalid model",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            String resultRDF = result.get();

            JFileChooser fc = new JFileChooser();

            // Add supported file formats as filter
            fc.addChoosableFileFilter(new FileNameExtensionFilter("RDF", "rdf"));

            // Make sure "All files" filter is available as well
            fc.setAcceptAllFileFilterUsed(true);
            // Only be able to select files
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

            // Return if the user does not accept a file
            if (fc.showSaveDialog(main.mainFrame) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            // Get the selected file
            File targetFile = fc.getSelectedFile();

            // Check if the file already exists, abort at user request
            if (targetFile.exists()) {
                if (JOptionPane.showConfirmDialog(
                        Main.mainFrame,
                        "The selected location already exists. Would you like to overwrite it?",
                        "Location already exists",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.INFORMATION_MESSAGE) == JOptionPane.CANCEL_OPTION) {
                    return;
                };
            }

            // If an extension filter was selected, append the extension to the file path if it's not there
            if (fc.getFileFilter() instanceof FileNameExtensionFilter) {
                // We know the RDF filter was selected here

                // Append .rdf if it's not there
                if (!targetFile.getName().toLowerCase().endsWith(".rdf")) {
                    targetFile = new File(targetFile.getAbsolutePath() + ".rdf");
                }
            }

            // Try to open an output stream (which is automatically closed
            try (PrintWriter out = new PrintWriter(targetFile)) {
                out.write(resultRDF);
            } catch (FileNotFoundException e1) {
                // Show a message that the location is unaccessible
                JOptionPane.showMessageDialog(
                        Main.mainFrame,
                        "Selected location \"" + targetFile.getAbsolutePath() + "\" does not exist. Please try again.",
                        "Invalid location",
                        JOptionPane.ERROR_MESSAGE
                );

                // Retry by launching the same action
                new ExportRDF(main).actionPerformed(e);

                return;
            } catch (IOException e1) {
                // Show a message with debug information if this happens
                JOptionPane.showMessageDialog(
                        Main.mainFrame,
                        "An unexpected error occurred. Please contact the developers with the following information:\n" + e1.getMessage() + "\n" + e1.getStackTrace(),
                        "Unexpected error",
                        JOptionPane.ERROR_MESSAGE
                );

                e1.printStackTrace();

                return;
            }
            
            JOptionPane.showMessageDialog(Main.mainFrame, "RDF exported to: " + targetFile);

            // SVG test
            // TODO: Use this sometime for SVG export when JGraphX supports stencils.
//			Document doc = mxCellRenderer.createSvgDocument(main.getCurrentGraph(), null, 1, Color.WHITE, null);
//			
//			try {
//				mxUtils.writeFile(mxXmlUtils.getXml(doc), "C:\\Users\\Bobe\\Desktop\\test.svg");
//			} catch (IOException e1) {
//				e1.printStackTrace();
//			}
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
            JFileChooser fc = new JFileChooser();

            // Add supported file formats as filter
            fc.addChoosableFileFilter(new FileNameExtensionFilter("PNG image", "png"));
            fc.addChoosableFileFilter(new FileNameExtensionFilter("JPEG image", "jpeg"));
            fc.addChoosableFileFilter(new FileNameExtensionFilter("BMP image", "gif"));
            fc.addChoosableFileFilter(new FileNameExtensionFilter("GIF image", "bmp"));

            // Make sure "ALl files" filter is available as well
            fc.setAcceptAllFileFilterUsed(true);
            // Only be able to select files
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

            // Return if the user does not accept a file
            if (fc.showSaveDialog(main.mainFrame) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            // Get the selected file
            File targetFile = fc.getSelectedFile();

            // If the file exists get confirmation from the user
            if (targetFile.exists()) {
                int result = JOptionPane.showConfirmDialog(
                        main.mainFrame,
                        targetFile + " already exists. Would you like to overwrite it?",
                        "File already exists",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                if (result != JOptionPane.OK_OPTION) {
                    return;
                }
            }

            // If an extension filter was selected, append the extension to the file path if it's not there
            if (fc.getFileFilter() instanceof FileNameExtensionFilter) {
                FileNameExtensionFilter ff = (FileNameExtensionFilter) fc.getFileFilter();
                String format = ff.getExtensions()[0];

                if (!targetFile.getName().toLowerCase().replace("jpg", "jpeg").endsWith(format)) {
                    targetFile = new File(targetFile.getAbsolutePath() + "." + format);
                }
            }

            // Make sure the user has selected a proper format
            String format = null;
            if (targetFile.getName().toLowerCase().endsWith("png")) {
                format = "PNG";
            } else if (targetFile.getName().toLowerCase().endsWith("bmp")) {
                format = "BMP";
            } else if (targetFile.getName().toLowerCase().replace("jpg", "jpeg").endsWith("jpeg")) {
                format = "JPEG";
            } else if (targetFile.getName().toLowerCase().endsWith("gif")) {
                format = "GIF";
            }

            // If not, show an error
            if (format == null) {
                JOptionPane.showConfirmDialog(
                        main.mainFrame,
                        "Requested file format is not supported.",
                        "Unsupported file format",
                        JOptionPane.OK_OPTION,
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Create the image
            BufferedImage image = mxCellRenderer.createBufferedImage(main.getCurrentGraph(), null, 1, Color.WHITE, true, null);

            // Null will be returned if there are no cells to display. Show an error and return
            if (image == null) {
                JOptionPane.showConfirmDialog(
                        main.mainFrame,
                        "Cannot save an empty model",
                        "File saving error",
                        JOptionPane.OK_OPTION,
                        JOptionPane.ERROR_MESSAGE);

                return;
            }

            // Try to write the file
            try {
                ImageIO.write(image, format, targetFile);
            } catch (IOException e1) {
                e1.printStackTrace();

                JOptionPane.showConfirmDialog(
                        main.mainFrame,
                        "Error saving file. Make sure the chosen location is accesible.",
                        "File saving error",
                        JOptionPane.OK_OPTION,
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
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
            JOptionPane.showMessageDialog(
                    Main.mainFrame,
                    "This feature is not yet implemented",
                    "Feature not implemented",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static class Cut extends BaseAction {

        public Cut(Main main) {
            super("Cut (ctrl+x)", getIcon("cut"), main);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            TransferHandler.getCutAction().actionPerformed(new ActionEvent(main.getCurrentGraphComponent(), -1, null));
        }
    }

    public static class Copy extends BaseAction {

        public Copy(Main main) {
            super("Copy (ctrl+c)", getIcon("page_white_copy"), main);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            TransferHandler.getCopyAction().actionPerformed(new ActionEvent(main.getCurrentGraphComponent(), -1, null));
        }
    }

    public static class Paste extends BaseAction {

        public Paste(Main main) {
            super("Paste (ctrl+v)", getIcon("paste_plain"), main);
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
            E3Graph graph = main.getCurrentGraph();
            List<Object> allCells = Utils.getAllCells(graph)
                    .stream()
                    .filter(obj -> graph.getModel().getParent(obj) == graph.getDefaultParent())
                    .collect(Collectors.toList());
            graph.getSelectionModel().setCells(allCells.toArray());
            //System.out.println("Size: " + allCells.size());
        }
    }

    public static class Undo extends BaseAction {

        public Undo(Main main) {
            super("Undo (crtrl+z)", getIcon("arrow_rotate_anticlockwise"), main);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            main.getCurrentGraphComponent().undoManager.undo();
        }
    }

    public static class Redo extends BaseAction {

        public Redo(Main main) {
            super("Redo (ctrl+y)", getIcon("arrow_rotate_clockwise"), main);
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
            super("Zoom in (ctrl+numpad plus)", getIcon("magnifier_zoom_in"), main);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            main.getCurrentGraphComponent().zoomIn();
        }
    }

    public static class ZoomOut extends BaseAction {

        public ZoomOut(Main main) {
            super("Zoom out (ctrl+numpad minus)", getIcon("magnifier_zoom_out"), main);
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
        	mxGraphComponent graphComponent = main.getCurrentGraphComponent();
        	graphComponent.setGridVisible(!graphComponent.isGridVisible());
        	main.getCurrentGraph().refresh();
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
            E3Graph graph = main.getCurrentGraph();

            graph.getModel().beginUpdate();
            try {
                for (Object cell : Utils.getAllCells(graph)) {
                    Base value = (Base) Utils.base(graph, cell);
                    Base oldValue = value;

                    if (value instanceof StartSignal) {
                        StartSignal startSignal = (StartSignal) value.getCopy();
                        startSignal.showLabel = makeVisible;
                        value = startSignal;
                    } else if (value instanceof EndSignal) {
                        EndSignal endSignal = (EndSignal) value.getCopy();
                        endSignal.showLabel = makeVisible;
                        value = endSignal;
                    } else if (value instanceof ValueExchange) {
                        ValueExchange valueExchange = (ValueExchange) value.getCopy();
                        valueExchange.labelHidden = !makeVisible;
                        value = valueExchange;
                    }

                    if (value != oldValue) {
                        graph.getModel().setValue(cell, value);
                    }
                }

            } finally {
                graph.getModel().endUpdate();
            }
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
            E3Graph graph = main.getCurrentGraph();

            graph.getModel().beginUpdate();
            try {
                for (Object cell : Utils.getAllCells(graph)) {
                    Base value = (Base) Utils.base(graph, cell);

                    if (value instanceof ValueExchange) {
                        ValueExchange valueExchange = (ValueExchange) value.getCopy();
                        valueExchange.valueObjectHidden = !makeVisible;
                        graph.getModel().setValue(cell, valueExchange);
                    }
                }
            } finally {
                graph.getModel().endUpdate();
            }
        }
    }

    public static class DuplicateModel extends BaseAction {

        public DuplicateModel(Main main) {
            super("Create duplicate", getIcon("page_copy"), main);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            main.addNewTabAndSwitch(new E3Graph(main.getCurrentGraph(), true));
        }
    }

    public static class ChangeModelType extends BaseAction {

        public ChangeModelType(Main main) {
            super("Change type", getIcon("page_refresh"), main);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (main.getCurrentGraph().isFraud) {
                int response = JOptionPane.showConfirmDialog(
                        Main.mainFrame,
                        conversionMessage,
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

            if (newTitle != null) {
                main.getCurrentGraph().title = newTitle;
                main.setCurrentTabTitle(newTitle);
            }
        }
    }

    public static class ShowValueObjectsPanel extends BaseAction {

        public ShowValueObjectsPanel(Main main) {
            super("Edit Value Objects... ", getIcon("old/vo"), main);
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

            if (ValueObjectDialog.isOpened) {
                JOptionPane.showMessageDialog(
                        Main.mainFrame,
                        "A value object dialog is already open.",
                        "Dialog already open",
                        JOptionPane.ERROR_MESSAGE);

                ValueObjectDialog.dialogInstance.requestFocus();
                return;
            }

            new ValueObjectDialog(main).show();
        }
    }
    
    public static class ShowValueTransactionsPanel extends BaseAction {

        public ShowValueTransactionsPanel(Main main) {
            super("Edit Value Transactions...", main);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO: Maybe prefer greyed out menu item?
            if (main.views.getTabCount() == 0) {
                JOptionPane.showMessageDialog(
                        Main.mainFrame,
                        "A model must be opened to display its ValueTransactions. Click File ➡ New model to open a new model.",
                        "No model available",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (ValueTransactionDialog.isOpened) {
                JOptionPane.showMessageDialog(
                        Main.mainFrame,
                        "A value transaction dialog is already open.",
                        "Dialog already open",
                        JOptionPane.ERROR_MESSAGE);

//                ValueTransactionDialog.dialogInstance.requestFocus();
                return;
            }

            new ValueTransactionDialog(main).setVisible(true);
        }
    }
    
    public static class AnalyzeTransactions extends BaseAction {

        public AnalyzeTransactions(Main main) {
            super("Analyze transactions (coming soon)", main);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO: Implement this
            JOptionPane.showMessageDialog(
                    Main.mainFrame,
                    "This feature is not yet implemented",
                    "Feature not implemented",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static class FraudGeneration extends BaseAction {

        public FraudGeneration(Main main) {
            super("Fraud generation...", getIcon("old/e3fraud"), main);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
        	System.out.println("TEST!");
        	
            if (main.views.getTabCount() == 0) {
                JOptionPane.showMessageDialog(
                        Main.mainFrame,
                        "A model must be opened to analyze. Click File ➡ New model to start building one.",
                        "No model available",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

//            if (!main.getCurrentGraph().isValid()) {
//                int choice = JOptionPane.showConfirmDialog(
//                        Main.mainFrame,
//                        invalidModelMessage,
//                        "Model is not well formed.",
//                        JOptionPane.YES_NO_OPTION);
//
//                if (choice == JOptionPane.NO_OPTION) {
//                    return;
//                }
//            }
            
            boolean castMarketSegments = false;
            if (main.getCurrentGraph().countE3ObjectsOfType(MarketSegment.class) > 0) {
            	String oneMSMessage = 
                        "<html>"
                        + "This model contains a market segment. "
                        + "Would you like to convert it to <b>one</b> actor "
                        + "to allow for the element to participate in collusion?"
                        + "</html>";
                        
                String moreThanOneMessage =
                		"<html>"
                		+ "This model contains multiple market segments. "
                		+ "Would you like to convert each market segment to <b>one</b> actor "
                		+ "to allow for the elements to participate in collusions? "
                		+ "</html>";
                
                String message;
                if (main.getCurrentGraph().countE3ObjectsOfType(MarketSegment.class) == 1) {
                	message = oneMSMessage;
                } else {
                	message = moreThanOneMessage;
                }
            	
                int choice = JOptionPane.showConfirmDialog(
                        Main.mainFrame,
                        message,
                        "Market Segment conversion",
                        JOptionPane.YES_NO_OPTION
                        );
                
                castMarketSegments = choice == JOptionPane.YES_OPTION;
            }

            long totalActors = (long) main.getCurrentGraph().countActors();
            if (castMarketSegments) {
            	totalActors += main.getCurrentGraph().countE3ObjectsOfType(MarketSegment.class);
            }
            if (totalActors < 2) {
                JOptionPane.showMessageDialog(
                        Main.mainFrame,
                        "Fraud generation requires at least two actors. Please add more actors to the model",
                        "Not enough actors.",
                        JOptionPane.ERROR_MESSAGE);

                return;
            }

            E3Graph targetGraph = main.getCurrentGraph();
            
            // Check if the model checker fails or not
            boolean cont = Utils.doModelCheck(targetGraph, main);
            
            if (!cont) {
            	return;
            }

            if (main.getCurrentGraph().isFraud) {
                int choice = JOptionPane.showConfirmDialog(
                        Main.mainFrame,
                        "Fraud generation currently only works on value models. Do you want to"
                        + "convert this fraud model to a value model?",
                        "Unsupported model type",
                        JOptionPane.YES_NO_OPTION);

                if (choice == JOptionPane.NO_OPTION) {
                    return;
                }

                int confirmation = JOptionPane.showConfirmDialog(
                        Main.mainFrame,
                        conversionMessage,
                        "Conversion confirmation",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (confirmation == JOptionPane.CANCEL_OPTION) {
                    return;
                }

                targetGraph = targetGraph.toValue();
                main.addNewTabAndSwitch(targetGraph);
            }
            
            RDFExport rdfExporter = new RDFExport(targetGraph, true, VTMode.DERIVE_ORPHANED, castMarketSegments);
            if (!rdfExporter.getModel().isPresent()) {
                Optional<String> error = rdfExporter.getError();

                String errorString = "An error occurred while converting to an internal format. Please make sure the model contains no errors.";
                if (error.isPresent()) {
                    errorString += " The error: \n" + error.get();
                }

                JOptionPane.showMessageDialog(
                        Main.mainFrame,
                        errorString,
                        "Invalid model",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }
            Model model = rdfExporter.getModel().get();

            JFrame frame = new JFrame("Fraud analysis of \"" + main.getCurrentGraphTitle() + "\"");
            FraudWindow fraudWindowInstance = new FraudWindow(new E3Graph(targetGraph, false), new E3Model(model), main, frame);

            frame.add(fraudWindowInstance);
            frame.pack();
            frame.setLocationRelativeTo(Main.mainFrame);
            frame.setVisible(true);
        }
    }

    public static class ProfitabilityChart extends BaseAction {

        public ProfitabilityChart(Main main) {
            super("Profitability chart...", getIcon("chart_curve"), main);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (main.views.getTabCount() == 0) {
                JOptionPane.showMessageDialog(
                        Main.mainFrame,
                        "A model must be opened to analyze. Click File ➡ New model to start building one.",
                        "No model available",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (main.getCurrentGraph().countActors() < 1) {
                JOptionPane.showMessageDialog(
                        Main.mainFrame,
                        "Fraud generation requires at least one actor. Please add more actors to the model",
                        "Not enough actors.",
                        JOptionPane.ERROR_MESSAGE);

                return;
            }
            
            boolean cont = Utils.doModelCheck(main.getCurrentGraph(), main);
            
            if (!cont) {
            	return;
            }

            RDFExport rdfExporter = new RDFExport(main.getCurrentGraph(), false, VTMode.DERIVE_ORPHANED, false);

            if (!rdfExporter.getModel().isPresent()) {
                Optional<String> error = rdfExporter.getError();

                String errorString = "An error occurred while converting to an internal format. Please make sure the model contains no errors.";
                if (error.isPresent()) {
                    errorString += " The error: \n" + error.get();
                }

                JOptionPane.showMessageDialog(
                        Main.mainFrame,
                        errorString,
                        "Invalid model",
                        JOptionPane.ERROR_MESSAGE
                );

                return;
            }

            Model model = rdfExporter.getModel().get();

            JFreeChart chart = ProfitabilityAnalyser.getProfitabilityAnalysis(new E3Model(model), !main.getCurrentGraph().isFraud);
            if (chart != null) {
                ChartFrame chartframe1 = new ChartFrame("Profitability of \"" + main.getCurrentGraphTitle() + "\"", chart);
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
        
        public OpenSite(Main main, Icon icon, String caption, String url) {
            super(caption, icon, main);
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
            super(main, getIcon("help"), "Help Wiki (F1)", "https://github.com/danionita/e3tools/wiki");
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
            JOptionPane.showMessageDialog(null, "The e3tool integrates the e3value value modelling and e-business analysis methodology developed by Jaap Gordjin with the e3fraud fraud assessment methodology developed by Dan Ionita. \n This tool was developed at the University of Twente by Dan Ionita and Bob Rubbens. \n Icons from the famfamfam Silk icon pack (http://www.famfamfam.com/lab/icons/silk/) owned by Mark James.", "About e3tool", INFORMATION_MESSAGE);
        }
    }

    public static class ToggleValuationLabels extends BaseAction {

        public ToggleValuationLabels(Main main) {
            super("Toggle valuation labels", main);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (main.getCurrentGraphComponent().valuationLabelsVisible){
            main.getCurrentGraphComponent().toggleValuationLabels(false);
            }
            else{
                main.getCurrentGraphComponent().toggleValuationLabels(true);
            }
        }
    }

    public static class SelectTheme extends BaseAction {
		public SelectTheme(Main main) {
			super("Select theme...", main);
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			List<String> choicesList = E3Style.getAvailableThemes();
			String[] choices = new String[choicesList.size()];
			choicesList.toArray(choices);
			
			String result = (String) JOptionPane.showInputDialog(
					Main.mainFrame,
					"Select a theme to use with the current model",
					"Select a theme", 
					JOptionPane.QUESTION_MESSAGE,
					null,
					choices,
					choices[0]
					);
			
			if (result == null) return;
			
			Optional <E3Style> newStyle = Optional.empty();
			if (choicesList.contains(result)) {
				newStyle = E3Style.load(result);
			}
			
			if (!newStyle.isPresent()) {
				JOptionPane.showMessageDialog(
						Main.mainFrame,
						"Could not load theme \"" + result + "\"",
						"Error loading theme",
						JOptionPane.ERROR_MESSAGE);
				
				return;
			}
			
			E3Style style = newStyle.get();
			E3Graph graph = main.getCurrentGraph();
			
			ThemeChange themeChange = new ThemeChange(
					main.getCurrentGraphComponent(),
					main.getCurrentToolComponent(),
					style,
					false);

			Utils.update(graph, () -> {
				((mxGraphModel) graph.getModel()).execute(themeChange);
			});
		}
    }
    
    public static class EditTheme extends BaseAction {
		public EditTheme(Main main) {
			super("Edit current theme...", main);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			E3Graph graph = main.getCurrentGraph();
			E3StyleEditor editor = new E3StyleEditor(graph);
			editor.setModal(true);
			editor.addListener(e3ThemeStyleEvent -> {
				E3Style newStyle = new E3Style(graph.style);
				boolean anythingChanged = newStyle.applyStyleDelta(e3ThemeStyleEvent);
				
				if (anythingChanged) {
					ThemeChange themeChange = new ThemeChange(
							main.getCurrentGraphComponent(),
							main.getCurrentToolComponent(),
							newStyle,
							false);

					Utils.update(graph, () -> {
						((mxGraphModel) graph.getModel()).execute(themeChange);
					});
				}
			});
			
			editor.setVisible(true);
		}
    }


    public static class ModelCheck extends BaseAction {

        public ModelCheck(Main main) {
            super("Check model...", main);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
        	actionPerformed(main.getCurrentGraph());
        }
        
        public void actionPerformed(E3Graph graph) {
//            E3Graph currentGraph = main.getCurrentGraph();
//            System.out.println("Doing WEM");
//            WalkEntireModel wem = new WalkEntireModel(currentGraph);
//            System.out.println("Doing FC");
////            FlowChecker fc = new FlowChecker(currentGraph);
//            System.out.println("Done checking");
//            if (fc.getConflictingDots().size() > 0) {
//            	//System.out.println("Conflicting dots detected! Suspects:");
//            	for (Object obj : fc.getConflictingDots()) {
//            		Base info = (Base) currentGraph.getModel().getValue(obj);
//            		System.out.println(info.name + " (" + info.SUID + ")");
//            		
//					currentGraph.getView().getState(obj).getStyle().put(
//							mxConstants.STYLE_STROKECOLOR,
//							"#00FF00"
//							);
//            	}
//            	currentGraph.repaint();
//            }
        	
//        	E3Graph graph = main.getCurrentGraph();
        	
//        	Utils.getAllCells(graph).stream()
//        		.forEach(cell -> {
//        			System.out.println(graph.getModel().getStyle(cell));
//        			Map<String, Object> style = graph.getCellStyle(cell);
//        			
//        			Base val = (Base) graph.getModel().getValue(cell);
//        			System.out.println("Object #" + val.SUID);
//
//        			style.entrySet().stream().forEach(e -> {
////        				System.out.println("\tKey: " + e.getKey() + " Value: " + e.getValue());
//					});
//        				
//        		});
        	
        	E3CheckDialog e3cd = new E3CheckDialog(main);
        	
        	e3cd.setVisible(true);
        }
    }

    public static class NCF extends BaseAction {

        public NCF(Main main) {
            super("Net Value Flow analysis", getIcon("old/nvf"), main);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
        	boolean cont = Utils.doModelCheck(main.getCurrentGraph(), main);
        	
        	if (!cont) {
        		return;
        	}

            NCFDialog myDialog = new NCFDialog(main.getCurrentGraph());
        }
    }
    
     private static Icon getIcon(String iconString){
        if (iconString.contains("old/")) {
            return IconStore.getOldIcon(iconString);
        } else {
            return IconStore.getIcon(iconString);
        }
    }
}
