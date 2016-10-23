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
 * *****************************************************************************
 */
package design;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.mxgraph.util.mxPoint;

public class Main {

    public static final JFrame mainFrame = new JFrame("e3tools editor");
    public static Object contextTarget = null;
    public static mxPoint contextPos = new mxPoint(-1, -1);
    public static ToolComponent globalTools;
    public static final boolean mirrorMirrorOnTheWallWhoIsTheFairestOfThemAll = true;
    public static final boolean DEBUG = true;

    public static final int DEFAULT_CHART_WIDTH = 500;
    public static final int DEFAULT_CHART_HEIGHT = 400;

	public static final File e3toolDir = new File(
        	Utils.makePath(
					System.getProperty("user.home")
					, "e3tool"));
    public static final File e3styleDir = new File(
    		Utils.makePath(
    				e3toolDir.getPath()
    				, "styles"));

    public JTabbedPane views;
    private JToolBar toolbar;

    public E3GraphComponent getCurrentGraphComponent() {
        JSplitPane pane = (JSplitPane) views.getComponentAt(views.getSelectedIndex());
        return (E3GraphComponent) pane.getRightComponent();
    }

    public E3Graph getCurrentGraph() {
        try {
            return (E3Graph) getCurrentGraphComponent().getGraph();
        } catch (java.lang.ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }
    
    public ToolComponent getCurrentToolComponent() {
        JSplitPane pane = (JSplitPane) views.getComponentAt(views.getSelectedIndex());
        return (ToolComponent) pane.getLeftComponent();
    }

    public void addNewTabAndSwitch(boolean isFraud) {
        addNewTabAndSwitch(new E3Graph(E3Style.loadInternal("E3Style").get(), isFraud));
    }

    public void addNewTabAndSwitch(E3Graph graph) {
        E3GraphComponent graphComponent = new E3GraphComponent(graph);

        graph.getModel().beginUpdate();
        try {
            // Playground for custom shapes
        } finally {
            graph.getModel().endUpdate();
        }

        graphComponent.refresh();

        // Create split view
        JSplitPane mainpane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new ToolComponent(graph.style), graphComponent);
        mainpane.setResizeWeight(0.025);

        if (graph.isFraud) {
            ModelTab.addClosableTab(views, mainpane, IconStore.getImage("/e3f.png", 25, 25));
        } else {
            ModelTab.addClosableTab(views, mainpane, IconStore.getImage("/e3v.png", 25, 25));
        }

        views.setSelectedIndex(views.getTabCount() - 1);
    }

    public void setCurrentTabTitle(String title) {
        ((ModelTab) views.getTabComponentAt(views.getSelectedIndex()))
                .setTitle(title);
    }

    public String getCurrentGraphTitle() {
        return getCurrentGraph().title;
    }

    private void addToolbarButton(String icon, AbstractAction action) {
        JButton button = new JButton(action);
        button.setText("");
        button.setFocusPainted(false);
        if (icon.contains("old/")) {
            button.setIcon(IconStore.getOldIcon(icon));
        } else {
            button.setIcon(IconStore.getIcon(icon));
        }
        button.setToolTipText((String) action.getValue(Action.NAME));

        toolbar.add(button);
    }

    private void addGlobalShortcut(String keys, AbstractAction action) {
        views
                .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(keys), action.getValue(Action.NAME));

        views.getActionMap().put(action.getValue(Action.NAME), action);
    }

    public Main() {
        // Silly log4j
    	// Use Level.OFF to turn it off
        Logger.getRootLogger().setLevel(Level.DEBUG);

        if (mirrorMirrorOnTheWallWhoIsTheFairestOfThemAll) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.out.println("Couldn't set Look and Feel to system");
            }
        }

        views = new JTabbedPane();

        // Add menubar
        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(new EditorActions.NewTab(this, false));
        fileMenu.add(new EditorActions.NewTab(this, true));

        fileMenu.addSeparator();

        fileMenu.add(new EditorActions.OpenFile(this));
        fileMenu.add(new EditorActions.Save(this));
        fileMenu.add(new EditorActions.SaveAs(this));

        fileMenu.addSeparator();

        JMenu exportMenu = new JMenu("Export...");

        exportMenu.add(new EditorActions.ExportRDF(this));
        exportMenu.add(new EditorActions.ExportJSON(this));
        exportMenu.add(new EditorActions.ExportImage(this));

        fileMenu.add(exportMenu);

        JMenu importMenu = new JMenu("Import...");

        importMenu.add(new EditorActions.ImportRDF(this));
        importMenu.add(new EditorActions.ImportXSVG(this));

        fileMenu.add(importMenu);
        fileMenu.add(new EditorActions.Print(this));

        menuBar.add(fileMenu);

        JMenu editMenu = new JMenu("Edit");

        editMenu.add(new EditorActions.Cut(this));
        editMenu.add(new EditorActions.Copy(this));
        editMenu.add(new EditorActions.Paste(this));
        editMenu.add(new EditorActions.Delete(this));
        editMenu.add(new EditorActions.SelectAll(this));

        editMenu.addSeparator();

        editMenu.add(new EditorActions.Undo(this));
        editMenu.add(new EditorActions.Redo(this));

        editMenu.addSeparator();

        editMenu.add(new EditorActions.Find(this));

        menuBar.add(editMenu);

        JMenu viewMenu = new JMenu("View");

        viewMenu.add(new EditorActions.ZoomIn(this));
        viewMenu.add(new EditorActions.ZoomOut(this));

        viewMenu.addSeparator();

        viewMenu.add(new EditorActions.ToggleGrid(this));

        viewMenu.addSeparator();

        viewMenu.add(new EditorActions.ToggleLabels(this, true));
        viewMenu.add(new EditorActions.ToggleLabels(this, false));
        viewMenu.add(new EditorActions.ToggleValueObjects(this, true));
        viewMenu.add(new EditorActions.ToggleValueObjects(this, false));
        viewMenu.add(new EditorActions.ToggleValuationLabels(this, true));
        viewMenu.add(new EditorActions.ToggleValuationLabels(this, false));

        menuBar.add(viewMenu);

        JMenu modelMenu = new JMenu("Model");

        modelMenu.add(new EditorActions.DuplicateModel(this));
        JMenuItem changeType = new JMenuItem(new EditorActions.ChangeModelType(this));
        modelMenu.add(changeType);
        modelMenu.add(new EditorActions.ChangeModelTitle(this));
        modelMenu.add(new EditorActions.ChangeTheme(this));

        modelMenu.addSeparator();

        modelMenu.add(new EditorActions.ShowValueObjectsPanel(this));
        modelMenu.add(new EditorActions.ShowValueTransactionsPanel(this));
        modelMenu.add(new EditorActions.ModelCheck(this));

        modelMenu.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                if (getCurrentGraph() == null) {
                    changeType.setText("Convert");
                    changeType.setEnabled(false);
                } else {
                    if (getCurrentGraph().isFraud) {
                        changeType.setText("Convert to e3value model");
                    } else {
                        changeType.setText("Convert to e3fraud model");
                    }
                    changeType.setEnabled(true);
                }
            }

            @Override
            public void menuDeselected(MenuEvent e) {
            }

            @Override
            public void menuCanceled(MenuEvent e) {
            }
        });

        menuBar.add(modelMenu);

        JMenu toolMenu = new JMenu("Tools");

        toolMenu.add(new EditorActions.ShowNetValueFlow(this)).setEnabled(false);
        toolMenu.add(new EditorActions.AnalyzeTransactions(this)).setEnabled(false);
        toolMenu.add(new EditorActions.NCF(this));

        toolMenu.addSeparator();

        toolMenu.add(new EditorActions.FraudGeneration(this));
        toolMenu.add(new EditorActions.ProfitabilityChart(this));

        menuBar.add(toolMenu);

        JMenu exampleMenu = new JMenu("Examples");

        exampleMenu.add(new ExampleModels.SingleTransaction(this));
        exampleMenu.add(new ExampleModels.FlatRateTelephony(this));
        exampleMenu.add(new ExampleModels.LogicGate(this));

        menuBar.add(exampleMenu);

        JMenu helpMenu = new JMenu("Help");

        // TODO: Add shortcuts
        helpMenu.add(new EditorActions.OpenHelpWiki(this));
        helpMenu.addSeparator();
        helpMenu.add(new EditorActions.OpenE3ValueWebsite(this));
        helpMenu.addSeparator();
        helpMenu.add(new EditorActions.ShowAbout(this));

        menuBar.add(helpMenu);

        mainFrame.setJMenuBar(menuBar);

        toolbar = new JToolBar();
        toolbar.setFloatable(false);

        // Toolbar buttons
        addToolbarButton("page_green", new EditorActions.NewTab(this, false));
        addToolbarButton("page_red", new EditorActions.NewTab(this, true));
        addToolbarButton("folder", new EditorActions.OpenFile(this));
        addToolbarButton("disk", new EditorActions.Save(this));

        toolbar.addSeparator();

        addToolbarButton("cut", new EditorActions.Cut(this));
        addToolbarButton("page_white_copy", new EditorActions.Copy(this));
        addToolbarButton("paste_plain", new EditorActions.Paste(this));

        toolbar.addSeparator();

        addToolbarButton("magnifier_zoom_in", new EditorActions.ZoomIn(this));
        addToolbarButton("magnifier_zoom_out", new EditorActions.ZoomOut(this));

        toolbar.addSeparator();

        addToolbarButton("page_copy", new EditorActions.DuplicateModel(this));
        addToolbarButton("page_refresh", new EditorActions.ChangeModelType(this));
        addToolbarButton("old/vo", new EditorActions.ShowValueObjectsPanel(this));
        //addToolbarButton("old/vt", new EditorActions.ShowValueTransactionsPanel(this));

        toolbar.addSeparator();

        //addToolbarButton("old/nvf", new EditorActions.ShowNetValueFlow(this));
        addToolbarButton("old/e3fraud", new EditorActions.FraudGeneration(this));
        addToolbarButton("chart_curve", new EditorActions.ProfitabilityChart(this));

        toolbar.addSeparator();

        addToolbarButton("help", new EditorActions.OpenHelpWiki(this));

        // Shortcuts
        addGlobalShortcut("ctrl N", new EditorActions.NewTab(this, false));
        addGlobalShortcut("ctrl M", new EditorActions.NewTab(this, true));
        addGlobalShortcut("ctrl S", new EditorActions.Save(this));
        addGlobalShortcut("ctrl A", new EditorActions.SelectAll(this));
        addGlobalShortcut("ctrl F", new EditorActions.ShowNetValueFlow(this));
        addGlobalShortcut("ctrl H", new EditorActions.AnalyzeTransactions(this));
        addGlobalShortcut("F1", new EditorActions.OpenHelpWiki(this));

        mainFrame.getContentPane().add(toolbar, BorderLayout.PAGE_START);

        globalTools = new ToolComponent(E3Style.loadInternal("E3Mono").get());

        addNewTabAndSwitch(false);

        mainFrame.getContentPane().add(views, BorderLayout.CENTER);
        
        // Make sure e3toolDirs exists
        e3toolDir.mkdirs();
        e3styleDir.mkdirs();
        
        // To make sure we give the user a chance to save their work on exit
        mainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        mainFrame.addWindowListener(new WindowAdapter() {
        	public void windowClosing(WindowEvent e) {
        		// Cycle through all the open graphs
        		while (getCurrentGraph() != null) {
        			// If it possibly contains unsaved work
        			if (getCurrentGraph().saveBeforeExit) {
        				// Ask the user what to do
        				int result = JOptionPane.showConfirmDialog(
        						mainFrame,
        						"This model contains unsaved changes. Would you like to save these changes?",
        						"Possible unsaved changes detected",
        						JOptionPane.YES_NO_CANCEL_OPTION,
        						JOptionPane.QUESTION_MESSAGE
        						);
        				
        				// Then either cancel, save, or discard.
        				if (result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION) {
        					return;
        				} else if (result == JOptionPane.YES_OPTION) {
        					if (!Utils.saveAs(mainFrame, getCurrentGraph())) {
        						// If the users clicks cancel in the dialog
        						// or an error occurs
        						// we just stop closing
        						return;
        					}
        				} else if (result == JOptionPane.NO_OPTION) {
        					// Carry on!
        				}
        			}
        			
        			// Remove the view if everything went ok
        			// And move on to the next
        			views.remove(views.getSelectedIndex());
        		}
        		
        		// Get rid of the frame and the application on exit
        		mainFrame.dispose();
        		System.exit(0);
        	}
        });
        
        // Show main screen
        mainFrame.setSize(1024, 800);
        // Centers it
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }

    public static void main(String[] args) {
        Main t = new Main();
    }
}
