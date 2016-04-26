package design.main;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.layout.mxOrganicLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxICell;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxGraph;

import design.main.Info.Actor;
import design.main.Info.Base;
import design.main.Info.MarketSegment;
import design.main.Info.ValueActivity;
import design.main.Info.ValueExchange;
import design.main.Info.ValuePort;
import design.vocabulary.E3value;

public class Main { 
	
	public static final JFrame mainFrame = new JFrame("E3fraud editor");
	public static mxGraph graph = null;
	public static E3GraphComponent graphComponent = null;
	public static Object contextTarget = null;
	public static mxPoint contextPos = new mxPoint(-1, -1);
	public static ToolComponent tools = null;
	
	public static final ArrayList<String> valueObjects = new ArrayList<>(
			Arrays.asList("MONEY", "SERVICE")
			);
	
	public Main() {
		// Silly log4j
		Logger.getRootLogger().setLevel(Level.OFF);
		
		// Set LaF to system
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception e){
			System.out.println("Couldn't set Look and Feel to system");
		}

		// Add menubar
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(new JMenu("File"));
		menuBar.add(new JMenu("Graph"));
		
		JMenu valueObjectsMenu = new JMenu("ValueObjects");
		valueObjectsMenu.addMenuListener(new MenuListener() {
			@Override
			public void menuSelected(MenuEvent e) {
				JDialog dialog = new JDialog(mainFrame, "ValueObjects", Dialog.ModalityType.DOCUMENT_MODAL);
				dialog.setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));
				
				JPanel buttonPanel = new JPanel();
				buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
				JButton newButton = new JButton();
				buttonPanel.add(newButton);
				JButton deleteButton = new JButton();
				buttonPanel.add(deleteButton);
				dialog.add(buttonPanel);

				Map<String, Integer> count = new HashMap<>();
				for (Object cell : Utils.getAllCells(graph)) {
					Object val = graph.getModel().getValue(cell);
					if (val instanceof ValueExchange) {
						ValueExchange ve = (ValueExchange) val;
						if (ve.valueObject != null) {
							count.put(ve.valueObject, count.getOrDefault(ve.valueObject, 0) + 1);
						}
					}
				}
				
				DefaultListModel<String> listModel = new DefaultListModel<>();
				for (String valueObject : valueObjects) {
					listModel.addElement(valueObject + " (" + count.getOrDefault(valueObject, 0) + "x)");
				}
				
				JList valueObjectsList = new JList(listModel);
				valueObjectsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				valueObjectsList.addListSelectionListener(new ListSelectionListener() {
					@Override
					public void valueChanged(ListSelectionEvent e) {
						if (e.getValueIsAdjusting()) return; // We only do something if the event is final, i.e. if the event is the last one

						// TODO: If cell recognition is no longer done with styles, refactor this
						// such that only the explicit style of a cell is changed.
						// I don't like messing with this state thing
						// (Altough it worked almost immediately. Maybe this is the right way?)
						// A benefit of this method is that it does not affect undo history (but is that actually true?).
						String valueObject = valueObjects.get(valueObjectsList.getSelectedIndex());
						for (Object obj : Utils.getAllCells(graph)) {
							Base val = Utils.base(graph, obj);
							if (val instanceof ValueExchange) {
								ValueExchange ve = (ValueExchange) val;
								if (ve.valueObject.equals(valueObject)) {
									graph.getView().getState(obj).getStyle().put(mxConstants.STYLE_STROKECOLOR, "#00FF00");
								} else {
									graph.getView().getState(obj).getStyle().put(mxConstants.STYLE_STROKECOLOR, "#0000FF");
								}
							}
						}
						
						graph.repaint();
					}
				});

				JScrollPane listScroller = new JScrollPane(valueObjectsList);
				dialog.add(listScroller);
				
				newButton.setAction(new AbstractAction("New") {
					@Override
					public void actionPerformed(ActionEvent e) {
						String newName = JOptionPane.showInputDialog(
								mainFrame,
								"Enter the name of the new ValueObject",
								"New ValueObject",
								JOptionPane.QUESTION_MESSAGE);
						if (newName == null || newName.trim().length() == 0) return;
						if (valueObjects.indexOf(newName) != -1) return;

						listModel.addElement(newName + " (0x)");
						valueObjects.add(newName);
					}
				});
				
				deleteButton.setAction(new AbstractAction("Delete") {
					@Override
					public void actionPerformed(ActionEvent e) {
						int index = valueObjectsList.getSelectedIndex();
						if (index == -1) return;
						String valueObjectName = valueObjects.get(index);

						List<Object> usingCells = new ArrayList<>();
						for (Object cell : Utils.getAllCells(graph)) {
							Object val = graph.getModel().getValue(cell);
							if (val instanceof ValueExchange) {
								ValueExchange ve = (ValueExchange) val;
								if (ve.valueObject != null && ve.valueObject.equals(valueObjectName)) {
									usingCells.add(cell);
								}
							}
						}
						
						int response = JOptionPane.showConfirmDialog(
								mainFrame,
								"You are about to delete the ValueObject \""
										+ valueObjectName 
										+ "\". This ValueObject is used "
										+ usingCells.size()
										+ " times in this model. Would you like to proceed?",
								"Deletion confirmation",
								JOptionPane.OK_CANCEL_OPTION,
								JOptionPane.WARNING_MESSAGE
								);
						
						if (response == JOptionPane.OK_OPTION) {
							graph.getModel().beginUpdate();
							try {
								for (Object cell : usingCells) {
									ValueExchange ve = Utils.getValueExchange(cell);
									ve.valueObject = null;
									graph.getModel().setValue(cell, ve);
								}
							} finally {
								graph.getModel().endUpdate();
							}

							listModel.remove(valueObjectsList.getSelectedIndex());
							valueObjects.remove(index);
						}
					}
				});
				
				valueObjectsMenu.setSelected(false);
				
				// Makes all edges in the graph blue again in case they've been highlighted
				dialog.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosing(WindowEvent e) {
						for (Object obj : Utils.getAllCells(graph)) {
							Base val = Utils.base(graph, obj);
							if (val instanceof ValueExchange) {
								ValueExchange ve = (ValueExchange) val;
								graph.getView().getState(obj).getStyle().put(mxConstants.STYLE_STROKECOLOR, "#0000FF");
							}
						}
						
						graph.repaint();
					}
				});
				
				dialog.setSize(300, 320);
				dialog.setVisible(true);
			}
			
			@Override
			public void menuDeselected(MenuEvent e) { }
			
			@Override
			public void menuCanceled(MenuEvent e) { }
		});
		menuBar.add(valueObjectsMenu);
		
		mainFrame.setJMenuBar(menuBar);
		
		graph = new E3Graph();
		Object root = graph.getDefaultParent();
		graphComponent = new E3GraphComponent(graph);
		
		graph.getModel().beginUpdate();
		try {
			// Playground for custom shapes
		} finally {
			graph.getModel().endUpdate();
		}

		tools = new ToolComponent();
		
		JTabbedPane valueProperties = new JTabbedPane();	
		
		// Create split view
		JSplitPane mainPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tools, graphComponent);
		mainPane.setResizeWeight(0.025);
		mainFrame.getContentPane().add(mainPane);
		
		// Show main screen
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setSize(1024, 768);
		mainFrame.setVisible(true);
	}
	
	public static void main(String[] args) {
		Main t = new Main();
		
		t.graphComponent.setEnabled(false);
		
		// Import test
        //Load file
        InputStream inputStream = null;
        String file = "All.rdf";
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException ex) {
        	System.out.println("Whoops, file " + file + "  not found");
        	return;
        }

        //First, replace undeline (_) with dashes(-)
        //This is because e3valuetoolkit does a bad job at exporting RDF and outputs _ instead of -
        SearchAndReplaceInputStream fixedInputStream = new SearchAndReplaceInputStream(inputStream, "_", "-");

        //creating THE JENA MODEL
        Model model = ModelFactory.createDefaultModel();
        model.read(fixedInputStream, null);

        t.graph.getModel().beginUpdate();
        try {
			// Import ValueObjects
			{
				Main.valueObjects.clear();
				
				ResIterator it = model.listSubjectsWithProperty(RDF.type, E3value.value_object);
				while (it.hasNext()) {
					Resource res = it.next();
					String valueObject = res.getProperty(E3value.e3_has_name).getString();
					if (!Main.valueObjects.contains(valueObject)) Main.valueObjects.add(valueObject);
				}
			}
			
			Map<Resource, Object> resourceToCell = new HashMap<>();
			
			// Import Actors
			{
				ResIterator it = model.listSubjectsWithProperty(RDF.type, E3value.elementary_actor);
				while (it.hasNext()) {
					Resource res = it.next();
					String name = res.getProperty(E3value.e3_has_name).getString();
					int UID = res.getProperty(E3value.e3_has_uid).getInt();
					StmtIterator formulas = res.listProperties(E3value.e3_has_formula);
					
					mxCell actor = (mxCell) Main.tools.clone(Main.tools.actor);
					Actor actorValue = (Actor) actor.getValue();
					actorValue.formulas.clear();

					actorValue.setSUID(UID);
					actorValue.name = name;

					while(formulas.hasNext()) {
						String formula = formulas.next().getString();
						String formulaName = formula.split("=")[0];
						String value = Utils.concatTail(formula.split("="));

						actorValue.formulas.put(formulaName, value);
					}
					
					t.graph.addCell(actor);
					
					resourceToCell.put(res, actor);
				}
			}
			
			// Import MarketSegments
			{
				ResIterator it = model.listSubjectsWithProperty(RDF.type, E3value.market_segment);
				while (it.hasNext()) {
					Resource res = it.next();
					String name = res.getProperty(E3value.e3_has_name).getString();
					int UID = res.getProperty(E3value.e3_has_uid).getInt();
					StmtIterator formulas = res.listProperties(E3value.e3_has_formula);
					
					mxCell marketSegment = (mxCell) Main.tools.clone(Main.tools.marketSegment);
					MarketSegment marketSegmentValue = (MarketSegment) marketSegment.getValue();
					marketSegmentValue.formulas.clear();

					marketSegmentValue.setSUID(UID);
					marketSegmentValue.name = name;

					while(formulas.hasNext()) {
						String formula = formulas.next().getString();
						String formulaName = formula.split("=")[0];
						String value = Utils.concatTail(formula.split("="));

						marketSegmentValue.formulas.put(formulaName, value);
					}
					
					// Some weird extra property that only sometimes appears in the resource
					// Jaap?
					if (res.hasProperty(E3value.e3_has_formula)) {
						String formula = res.getProperty(E3value.e3_has_formula).getString();
						String formulaName = formula.split("=")[0];
						String value = Utils.concatTail(formula.split("="));

						marketSegmentValue.formulas.put(formulaName, value);
					}
					
					t.graph.addCell(marketSegment);

					resourceToCell.put(res, marketSegment);
				}
			}
			
			// Import ValueActivities
			{
				ResIterator it = model.listSubjectsWithProperty(RDF.type, E3value.value_activity);
				while (it.hasNext()) {
					Resource res = it.next();
					String name = res.getProperty(E3value.e3_has_name).getString();
					int UID = res.getProperty(E3value.e3_has_uid).getInt();
					StmtIterator formulas = res.listProperties(E3value.e3_has_formula);
					
					mxCell valueActivity = (mxCell) Main.tools.clone(Main.tools.valueActivity);
					ValueActivity valueActivityValue = (ValueActivity) valueActivity.getValue();
					valueActivityValue.formulas.clear();

					valueActivityValue.setSUID(UID);
					valueActivityValue.name = name;

					while(formulas.hasNext()) {
						String formula = formulas.next().getString();
						String formulaName = formula.split("=")[0];
						String value = Utils.concatTail(formula.split("="));

						valueActivityValue.formulas.put(formulaName, value);
					}
					
					t.graph.addCell(valueActivity);
					
					resourceToCell.put(res, valueActivity);
				}
			}
			
			// Import ValueInterfaces
			{
				ResIterator it = model.listSubjectsWithProperty(RDF.type, E3value.value_interface);
				while (it.hasNext()) {
					Resource res = it.next();

					Consumer<Statement> printCellName = p -> {
						Object parent = resourceToCell.get(p.getResource());
						mxCell valueInterface = (mxCell) t.tools.clone(t.tools.valueInterface);
						
						String name = res.getProperty(E3value.e3_has_name).getString();
						int UID = res.getProperty(E3value.e3_has_uid).getInt();
						
						Base value = (Base) valueInterface.getValue();
						value.name = name;
						value.setSUID(UID);
						
						valueInterface.getGeometry().setX(0);
						valueInterface.getGeometry().setY(0);
						
						t.graph.addCell(valueInterface, parent);
						
						List<Object> toRemove = new ArrayList<Object>();
						for (int i = 0; i < valueInterface.getChildCount(); i++) {
							Object child = valueInterface.getChildAt(i);
							Base childValue = (Base) t.graph.getModel().getValue(child);
							if (childValue instanceof ValuePort) toRemove.add(child);
						}
						toRemove.forEach(o -> t.graph.getModel().remove(o));
						
						E3Graph.straightenValueInterface(graph, valueInterface);
						
						resourceToCell.put(res, valueInterface);
					};

					if (res.hasProperty(E3value.vi_assigned_to_ac)) {
						printCellName.accept(res.getProperty(E3value.vi_assigned_to_ac));
					}
					if (res.hasProperty(E3value.vi_assigned_to_ms)) {
						printCellName.accept(res.getProperty(E3value.vi_assigned_to_ms));
					}
					if (res.hasProperty(E3value.vi_assigned_to_va)) {
						printCellName.accept(res.getProperty(E3value.vi_assigned_to_va));
					}
				}
			}
			
			// Import valueOfferings (wrapper objects of valuePorts, because the "name" property was already taken
			// by something else so they needed a wrapper?)
			{
				ResIterator it = model.listSubjectsWithProperty(RDF.type, E3value.value_offering);
				while (it.hasNext()) {
					Resource res = it.next();
					
					Object parent = resourceToCell.get(res.getProperty(E3value.vo_in_vi).getResource());
					boolean incoming = res.getProperty(E3value.e3_has_name).getString().equals("in");
					
					mxCell valuePort = (mxCell) E3Graph.addValuePort(t.graph, (mxICell) parent, incoming);
					
					Resource valuePortRes = res.getProperty(E3value.vo_consists_of_vp).getResource();
					String name = valuePortRes.getProperty(E3value.e3_has_name).getString();
					int UID = valuePortRes.getProperty(E3value.e3_has_uid).getInt();
					
					Base valuePortValue = Utils.base(t.graph, valuePort);
					valuePortValue.name = name;
					valuePortValue.setSUID(UID);
					t.graph.getModel().setValue(valuePort, valuePortValue);
					
					resourceToCell.put(res, valuePort);
					resourceToCell.put(valuePortRes, valuePort);
				}
			}
			
			// Import valueExchanges
			{
				ResIterator it = model.listResourcesWithProperty(RDF.type, E3value.value_exchange);
				
				while (it.hasNext()) {
					Resource res = it.next();
		
					String name = res.getProperty(E3value.e3_has_name).getString();
					int UID = res.getProperty(E3value.e3_has_uid).getInt();
					
					System.out.println(name + UID);
					
					ValueExchange veValue = new ValueExchange();
					veValue.formulas.clear();
					veValue.name = name;
					veValue.setSUID(UID);
					
					StmtIterator formulas = res.listProperties(E3value.e3_has_formula);
					
					while(formulas.hasNext()) {
						String formula = formulas.next().getString();
						String formulaName = formula.split("=")[0];
						String value = Utils.concatTail(formula.split("="));

						veValue.formulas.put(formulaName, value);
					}
					
					if (res.hasProperty(E3value.e3_has_formula)) {
						String formula = res.getProperty(E3value.e3_has_formula).getString();
						String formulaName = formula.split("=")[0];
						String value = Utils.concatTail(formula.split("="));

						veValue.formulas.put(formulaName, value);
					}

					Object left = resourceToCell.get(res.getProperty(E3value.ve_has_in_po).getResource());
					Object right = resourceToCell.get(res.getProperty(E3value.ve_has_out_po).getResource());
					
					Object valueExchange = t.graph.insertEdge(t.graph.getDefaultParent(), null, null, left, right);
					// Have to set value here manually, the value parameter o the insertEdge method doesn't seem to work
					graph.getModel().setValue(valueExchange, veValue);
					
					resourceToCell.put(res, valueExchange);
				}
			}
			
			// This was to enable editing again, so the state can't be altered while
			// a model is loaded. I'm not sure if it works though
			t.graphComponent.setEnabled(true);

			// Layout the graph nicely
			mxIGraphLayout layout = new mxOrganicLayout(t.graph);
//			mxIGraphLayout layout = new mxHierarchicalLayout(t.graph);
//			mxIGraphLayout layout = new mxOrthogonalLayout(t.graph);
//			mxIGraphLayout layout = new mxPartitionLayout(t.graph);
//			mxIGraphLayout layout = new mxStackLayout(t.graph);
//			mxIGraphLayout layout = new mxCompactTreeLayout(t.graph);
			
			layout.execute(graph.getDefaultParent());
			
			// TODO: Recursively layout all actors/marketsegments/valueactivities
        } finally {
        	t.graph.getModel().endUpdate();
        }
	}
}
