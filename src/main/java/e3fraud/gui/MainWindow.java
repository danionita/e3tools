/*
 * Copyright (C) 2015 Dan Ionita 
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
package e3fraud.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultCaret;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import org.jfree.chart.ChartFrame;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import com.hp.hpl.jena.rdf.model.Resource;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.view.mxGraphView;

import design.main.E3Graph;
import design.main.E3GraphComponent;
import design.main.Info.Actor;
import design.main.Info.MarketSegment;
import design.main.Info.ValueActivity;
import design.main.Main;
import design.main.Utils;
import e3fraud.model.E3Model;
import e3fraud.parser.FileParser;
import e3fraud.tools.currentTime;

/*
 * FileChooserDemo.java uses these files:
 *   images/Open16.gif
 *   images/Save16.gif
 */
public class MainWindow extends JPanel
        implements ActionListener {

    int COMPONENT_WIDTH = 50;
    int CHART_WIDTH = 500;
    int CHART_HEIGHT = 400;

    String lastSelectedActorString;
    String lastSelectedNeedString;

    static private final String newline = "\n";
    public JButton openButton, generateButton, expandButton, collapseButton, refreshButton, idealGraphButton;
    public JTextArea log;
    JLabel generationSettingLabel, collusionsLabel, rankingSettingLabel, groupingSettingLabel;
    ButtonGroup generationGroup, rankingGroup;
    JSpinner collusionsButton;
    public JRadioButton lossButton, gainButton, lossGainButton, gainLossButton;
    JCheckBox groupingButton;

    JTree tree;
    DefaultMutableTreeNode root;
    DefaultTreeModel treeModel;

    JFileChooser fc;
    JFileChooser sfc;
    Frame resultFrame;

    JScrollPane resultScrollPane, logScrollPane;
    JPanel collusionSettingsPanel, settingsPanel, chartPane;
    JFreeChart graph1 = null;
    JFreeChart graph2 = null;
    public E3Model baseModel = null;
    Resource selectedNeed;
    Resource selectedActor;
    ChartPanel chartPanel;
    int startValue = 0, endValue = 0;
    boolean extended;

    private JProgressBar progressBar;
    
    public MainWindow() {
    	this((E3Model) null, null);
    }
    
    public MainWindow(E3Model baseModel, String name) {
        super(new BorderLayout(5, 5));

        this.baseModel = baseModel;
        
        extended = false;

        //Create the log first, because the action listeners
        //need to refer to it.
        log = new JTextArea(10, 50);
        log.setMargin(new Insets(5, 5, 5, 5));
        log.setEditable(false);
        logScrollPane = new JScrollPane(log);
        DefaultCaret caret = (DefaultCaret) log.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        //        create the progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(progressBar.getMinimum());
        progressBar.setVisible(false);
        progressBar.setStringPainted(true);
        //Create the settings pane
        generationSettingLabel = new JLabel("Generate:");
        SpinnerModel collusionsSpinnerModel = new SpinnerNumberModel(1, 0, 3, 1);
        collusionSettingsPanel = new JPanel(new FlowLayout()) {
            @Override
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }
        };
        // collusionSettingsPanel.setLayout(new BoxLayout(collusionSettingsPanel, BoxLayout.X_AXIS));
        collusionsButton = new JSpinner(collusionsSpinnerModel);
        collusionsLabel = new JLabel("collusion(s)");
        collusionSettingsPanel.add(collusionsButton);
        collusionSettingsPanel.add(collusionsLabel);

        rankingSettingLabel = new JLabel("Rank by:");
        lossButton = new JRadioButton("loss");
        lossButton.setToolTipText("Sort sub-ideal models based on loss for Target of Assessment (high -> low)");
        gainButton = new JRadioButton("gain");
        gainButton.setToolTipText("Sort sub-ideal models based on gain of any actor except Target of Assessment (high -> low)");
        lossGainButton = new JRadioButton("loss + gain");
        lossGainButton.setToolTipText("Sort sub-ideal models based on loss for Target of Assessment and, if equal, on gain of any actor except Target of Assessment");
        //gainLossButton = new JRadioButton("gain + loss");
        lossGainButton.setSelected(true);
        rankingGroup = new ButtonGroup();
        rankingGroup.add(lossButton);
        rankingGroup.add(gainButton);
        rankingGroup.add(lossGainButton);
        //rankingGroup.add(gainLossButton);
        groupingSettingLabel = new JLabel("Group by:");
        groupingButton = new JCheckBox("collusion");
        groupingButton.setToolTipText("Groups sub-ideal models based on the pair of actors colluding before ranking them");
        groupingButton.setSelected(false);
        refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(this);

        settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.PAGE_AXIS));
        settingsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        settingsPanel.add(generationSettingLabel);
        collusionSettingsPanel.setAlignmentX(LEFT_ALIGNMENT);
        settingsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        settingsPanel.add(collusionSettingsPanel);
        settingsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        rankingSettingLabel.setAlignmentY(TOP_ALIGNMENT);
        settingsPanel.add(rankingSettingLabel);
        settingsPanel.add(lossButton);
        settingsPanel.add(gainButton);
        settingsPanel.add(lossGainButton);
        //settingsPanel.add(gainLossButton);
        settingsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        settingsPanel.add(groupingSettingLabel);
        settingsPanel.add(groupingButton);
        settingsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        settingsPanel.add(refreshButton);
        settingsPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        progressBar.setPreferredSize(new Dimension(settingsPanel.getSize().width, progressBar.getPreferredSize().height));
        settingsPanel.add(progressBar);
        
        //Create the result tree
        root = new DefaultMutableTreeNode("No models to display");
        treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel);
        //tree.setUI(new CustomTreeUI());
        tree.setCellRenderer(new CustomTreeCellRenderer(tree));
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setLargeModel(true);
        resultScrollPane = new JScrollPane(tree);
        tree.addTreeSelectionListener(treeSelectionListener);

        //tree.setShowsRootHandles(true);
        //Create a file chooser for saving
        sfc = new JFileChooser();
        FileFilter jpegFilter = new FileNameExtensionFilter("JPEG image", new String[]{"jpg", "jpeg"});
        sfc.addChoosableFileFilter(jpegFilter);
        sfc.setFileFilter(jpegFilter);

        //Create a file chooser for loading
        fc = new JFileChooser();
        FileFilter rdfFilter = new FileNameExtensionFilter("RDF file", "RDF");
        fc.addChoosableFileFilter(rdfFilter);
        fc.setFileFilter(rdfFilter);

        //Create the open button.  
        openButton = new JButton("Load model",
                createImageIcon("images/Open24.png"));
        openButton.addActionListener(this);
        openButton.setToolTipText("Load a e3value model");

        //Create the ideal graph button. 
        idealGraphButton = new JButton("Show ideal graph",
                createImageIcon("images/Plot.png"));
        idealGraphButton.setToolTipText("Display ideal profitability graph");
        idealGraphButton.addActionListener(this);

        Dimension thinButtonDimension = new Dimension(15, 420);

        //Create the expand subideal graph button. 
        expandButton = new JButton(">");
        expandButton.setPreferredSize(thinButtonDimension);
        expandButton.setMargin(new Insets(0, 0, 0, 0));
        expandButton.setToolTipText("Expand to show non-ideal profitability graph for selected model");
        expandButton.addActionListener(this);

        //Create the collapse sub-ideal graph button. 
        collapseButton = new JButton("<");
        collapseButton.setPreferredSize(thinButtonDimension);
        collapseButton.setMargin(new Insets(0, 0, 0, 0));
        collapseButton.setToolTipText("Collapse non-ideal profitability graph for selected model");
        collapseButton.addActionListener(this);

        //Create the generation button. 
        generateButton = new JButton("Generate sub-ideal models",
                createImageIcon("images/generate.png"));
        generateButton.addActionListener(this);
        generateButton.setToolTipText("Generate sub-ideal models for the e3value model currently loaded");

        //Create the chart panel
        chartPane = new JPanel();
        chartPane.setLayout(new FlowLayout(FlowLayout.LEFT));
        chartPane.add(expandButton);

        //For layout purposes, put the buttons in a separate panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(openButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        buttonPanel.add(generateButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPanel.add(idealGraphButton);

        //Add the buttons, the ranking options, the result list and the log to this panel.
        add(buttonPanel, BorderLayout.PAGE_START);
        add(settingsPanel, BorderLayout.LINE_START);
        add(resultScrollPane, BorderLayout.CENTER);

        add(logScrollPane, BorderLayout.PAGE_END);
        add(chartPane, BorderLayout.LINE_END);
        //and make a nice border around it
        setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        
        if (name != null) {
			log.append(e3fraud.tools.currentTime.currentTime() + " Loaded graph \"" + name + "\"\n");
        }
    }
    
    public void showGraph(E3Graph graph) {
    	analyzedGraph = Utils.cloneGraph(graph);
    	
    	graphComponent = new E3GraphComponent(analyzedGraph);
    	graphComponent.setEnabled(false);
    	graphComponent.setPreferredSize(new Dimension(0, 0));

    	settingsPanel.add(graphComponent);
    	
//    	SwingUtilities.invokeLater(new Runnable() {
//			@Override
//			public void run() {
//				mxGraphView view = graphComponent.getGraph().getView();
//				
//				System.out.println(graphComponent.getVisibleRect().getWidth());
//				System.out.println(view.getGraphBounds().getWidth());
//
//				double scale = view.getGraphBounds().getWidth() / graphComponent.getVisibleRect().getWidth(); 
//
//				System.out.println("Scale: " + scale);
//
//				view.setScale(scale);
//				
//				graphComponent.refresh();
//			}
//    	});
    }
    
    public void fit() {
		Main.mainFrame.pack();

		mxGraphView view = graphComponent.getGraph().getView();
		
		double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
		
		for (Object obj : analyzedGraph.getChildCells(analyzedGraph.getDefaultParent())) {
			// Only look at the positions from top-level elements
			if (!(analyzedGraph.getModel().getValue(obj) instanceof ValueActivity
					|| analyzedGraph.getModel().getValue(obj) instanceof MarketSegment
					|| analyzedGraph.getModel().getValue(obj) instanceof Actor)) continue;
			
			mxGeometry gm = analyzedGraph.getCellGeometry(obj);
			minX = Math.min(minX, gm.getX());
			minY = Math.min(minY, gm.getY());
			
		}

		double scale = graphComponent.getVisibleRect().getWidth() / view.getGraphBounds().getWidth();

		view.scaleAndTranslate(scale, -minX, -minY);
		
		graphComponent.refresh();
    }

    //configure listeners
    public void actionPerformed(ActionEvent e) {

        //Handle open button action.
        if (e.getSource() == openButton) {
            int returnVal = fc.showOpenDialog(MainWindow.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                //parse file
                this.baseModel = FileParser.parseFile(file);
                log.append(currentTime.currentTime() + " Opened: " + file.getName() + "." + newline);
            } else {
                log.append(currentTime.currentTime() + " Open command cancelled by user." + newline);
            }
            log.setCaretPosition(log.getDocument().getLength());

            //handle Generate button
        } else if (e.getSource() == generateButton) {
            if (this.baseModel != null) {
                //have the user indicate the ToA via pop-up
                JFrame frame1 = new JFrame("Select Target of Assessment");
                Map<String, Resource> actorsMap = this.baseModel.getActorsMap();
                String selectedActorString = (String) JOptionPane.showInputDialog(frame1,
                        "Which actor's perspective are you taking?",
                        "Choose main actor",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        actorsMap.keySet().toArray(),
                        actorsMap.keySet().toArray()[0]);
                if (selectedActorString == null) {
                    log.append(currentTime.currentTime() + " Attack generation cancelled!" + newline);
                } else {
                    lastSelectedActorString = selectedActorString;
                    //have the user select a need via pop-up
                    JFrame frame2 = new JFrame("Select graph parameter");
                    Map<String, Resource> needsMap = this.baseModel.getNeedsMap();
                    String selectedNeedString = (String) JOptionPane.showInputDialog(frame2,
                            "What do you want to use as parameter?",
                            "Choose need to parametrize",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            needsMap.keySet().toArray(),
                            needsMap.keySet().toArray()[0]);
                    if (selectedNeedString == null) {
                        log.append("Attack generation cancelled!" + newline);
                    } else {
                        lastSelectedNeedString = selectedNeedString;
                        //have the user select occurence interval via pop-up
                        JTextField xField = new JTextField("1", 4);
                        JTextField yField = new JTextField("500", 4);
                        JPanel myPanel = new JPanel();
                        myPanel.add(new JLabel("Mininum occurences:"));
                        myPanel.add(xField);
                        myPanel.add(Box.createHorizontalStrut(15)); // a spacer
                        myPanel.add(new JLabel("Maximum occurences:"));
                        myPanel.add(yField);
                        int result = JOptionPane.showConfirmDialog(null, myPanel,
                                "Please Enter occurence rate interval", JOptionPane.OK_CANCEL_OPTION);

                        if (result == JOptionPane.CANCEL_OPTION) {
                            log.append("Attack generation cancelled!" + newline);
                        } else if (result == JOptionPane.OK_OPTION) {
                            startValue = Integer.parseInt(xField.getText());
                            endValue = Integer.parseInt(yField.getText());

                            selectedNeed = needsMap.get(selectedNeedString);
                            selectedActor = actorsMap.get(selectedActorString);

                            //Have a Worker thread to the time-consuming generation and raking (to not freeze the GUI)
                            GenerationWorker generationWorker = new GenerationWorker(baseModel, selectedActorString, selectedActor, selectedNeed, selectedNeedString, startValue, endValue, log, lossButton, gainButton, lossGainButton, gainLossButton, groupingButton, collusionsButton) {
                                //make it so that when Worker is done
                                @Override
                                protected void done() {
                                    try {
                                        progressBar.setVisible(false);
                                        System.err.println("I made it invisible");
                                        //the Worker's result is retrieved
                                        treeModel.setRoot(get());
                                        tree.setModel(treeModel);

                                        tree.updateUI();
                                        tree.collapseRow(1);
                                        //tree.expandRow(0);
                                        tree.setRootVisible(false);
                                        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                                    } catch (InterruptedException | ExecutionException ex) {
                                        Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
                                        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                                        log.append("Out of memory; please increase heap size of JVM");
                                        PopUps.infoBox("Encountered an error. Most likely out of memory; try increasing the heap size of JVM", "Error");
                                    }
                                }
                            };
                            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                            progressBar.setVisible(true);
                            progressBar.setIndeterminate(true);
                            progressBar.setString("generating...");
                            generationWorker.addPropertyChangeListener(
                                    new PropertyChangeListener() {
                                public void propertyChange(PropertyChangeEvent evt) {
                                    if ("phase".equals(evt.getPropertyName())) {
                                        progressBar.setMaximum(100);
                                        progressBar.setIndeterminate(false);
                                        progressBar.setString("ranking...");
                                    } else if ("progress".equals(evt.getPropertyName())) {
                                        progressBar.setValue((Integer) evt.getNewValue());
                                    }
                                }
                            });
                            generationWorker.execute();
                        }
                    }
                }
            } else {
                log.append("Load a model file first!" + newline);
            }
        } //handle the refresh button
        else if (e.getSource() == refreshButton) {
            if (lastSelectedNeedString != null && lastSelectedActorString != null) {
                Map<String, Resource> actorsMap = this.baseModel.getActorsMap();
                Map<String, Resource> needsMap = this.baseModel.getNeedsMap();
                selectedNeed = needsMap.get(lastSelectedNeedString);
                selectedActor = actorsMap.get(lastSelectedActorString);

                //Have a Worker thread to the time-consuming generation and raking (to not freeze the GUI)
                GenerationWorker generationWorker = new GenerationWorker(baseModel, lastSelectedActorString, selectedActor, selectedNeed, lastSelectedNeedString, startValue, endValue, log, lossButton, gainButton, lossGainButton, gainLossButton, groupingButton, collusionsButton) {
                    //make it so that when Worker is done
                    @Override
                    protected void done() {
                        try {
                            progressBar.setVisible(false);
                            //the Worker's result is retrieved
                            treeModel.setRoot(get());
                            tree.setModel(treeModel);
                            tree.updateUI();
                            tree.collapseRow(1);
                            //tree.expandRow(0);
                            tree.setRootVisible(false);
                            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        } catch (InterruptedException | ExecutionException ex) {
                            Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
                            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                            log.append("Most likely out of memory; please increase heap size of JVM");
                            PopUps.infoBox("Encountered an error. Most likely out of memory; try increasing the heap size of JVM", "Error");
                        }
                    }
                };
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                progressBar.setVisible(true);
                progressBar.setIndeterminate(true);
                progressBar.setString("generating...");
                generationWorker.addPropertyChangeListener(
                        new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        if ("phase".equals(evt.getPropertyName())) {
                            progressBar.setMaximum(100);
                            progressBar.setIndeterminate(false);
                            progressBar.setString("ranking...");
                        } else if ("progress".equals(evt.getPropertyName())) {
                            progressBar.setValue((Integer) evt.getNewValue());
                        }
                    }
                });
                generationWorker.execute();

            } else {
                log.append(currentTime.currentTime() + " Nothing to refresh. Generate models first" + newline);
            }

        } //handle show ideal graph button
        else if (e.getSource() == idealGraphButton) {
            if (this.baseModel != null) {
                graph1 = GraphingTool.generateGraph(baseModel, selectedNeed, startValue, endValue, true);//expected graph 
                ChartFrame chartframe1 = new ChartFrame("Ideal results", graph1);
                chartframe1.setPreferredSize(new Dimension(CHART_WIDTH, CHART_HEIGHT));
                chartframe1.pack();
                chartframe1.setLocationByPlatform(true);
                chartframe1.setVisible(true);
            } else {
                log.append(currentTime.currentTime() + " Load a model file first!" + newline);
            }
        } //Handle the graph extend button//Handle the graph extend button
        else if (e.getSource() == expandButton) {
            //make sure there is a graph to show
            if (graph2 == null) {
                log.append(currentTime.currentTime() + " No graph to display. Select one first." + newline);
            } else {
                //this makes sure both graphs have the same y axis:
//            double lowerBound = min(graph1.getXYPlot().getRangeAxis().getRange().getLowerBound(), graph2.getXYPlot().getRangeAxis().getRange().getLowerBound());
//            double upperBound = max(graph1.getXYPlot().getRangeAxis().getRange().getUpperBound(), graph2.getXYPlot().getRangeAxis().getRange().getUpperBound());
//            graph1.getXYPlot().getRangeAxis().setRange(lowerBound, upperBound);
//            graph2.getXYPlot().getRangeAxis().setRange(lowerBound, upperBound);
                chartPane.removeAll();
                chartPanel = new ChartPanel(graph2);
                chartPanel.setPreferredSize(new Dimension(CHART_WIDTH, CHART_HEIGHT));
                chartPane.add(chartPanel);
                chartPane.add(collapseButton);
                extended = true;
                this.setPreferredSize(new Dimension(this.getWidth() + CHART_WIDTH, this.getHeight()));
                JFrame frame = (JFrame) getRootPane().getParent();
                frame.pack();
            }
        } //Handle the graph collapse button//Handle the graph collapse button
        else if (e.getSource() == collapseButton) {
            System.out.println("resizing by -" + CHART_WIDTH);
            chartPane.removeAll();
            chartPane.add(expandButton);
            this.setPreferredSize(new Dimension(this.getWidth() - CHART_WIDTH, this.getHeight()));
            chartPane.repaint();
            chartPane.revalidate();
            extended = false;
            JFrame frame = (JFrame) getRootPane().getParent();
            frame.pack();
        }
    }

    //handle list selection changes
    TreeSelectionListener treeSelectionListener = new TreeSelectionListener() {
        @Override
        public void valueChanged(TreeSelectionEvent treeSelectionEvent) {
            JTree tree = (JTree) treeSelectionEvent.getSource();
            //on selection
            if (!tree.isSelectionEmpty()) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                if (node.getUserObject() instanceof E3Model) {
                    //update current sub-ideal graph
                    graph2 = GraphingTool.generateGraph((E3Model) node.getUserObject(), selectedNeed, startValue, endValue, false);//real graph 
                    // and if the chartPanel is expanded, update that too
                    if (chartPanel != null) {
                        chartPanel.setChart(graph2);
                    }
                }
            }
        }
    };

    /**
     * Returns an ImageIcon, or null if the path was invalid.
     */
    protected static ImageIcon
            createImageIcon(String path) {
        java.net.URL imgURL = MainWindow.class
                .getResource(path);
        if (imgURL
                != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    /**
     * Create the GUI and show it. For thread safety, this method should be
     * invoked from the event dispatch thread.
     */
    public static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("e3fraud");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Add content to the window.
        mainWindowInstance = new MainWindow();
        frame.setContentPane(mainWindowInstance);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }
    
    public static MainWindow mainWindowInstance;
	private E3Graph analyzedGraph;
	private E3GraphComponent graphComponent;
}
