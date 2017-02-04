package design;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;

import design.info.Base;
import design.info.ValueExchange;
import design.info.ValueTransaction;
import net.miginfocom.swing.MigLayout;

/**
 * To-do list to correctly implement value transactions:
 * - vt dialog
 * - E3Properties needs to be able to handle a value transaction
 * - (de-)serialization & saving
 * - do/undo?
 * @author bobe
 * 
 * TODO: @Incomplete There's no do/undo mechanism in place for value transactions.
 *
 */
public class ValueTransactionDialog extends JDialog {
	public static final String HIGHLIGHT_COLOR = "#00FF00";
	
	private ChangeListener changeListener;
	private Main main;
	private E3Graph graph;
	private E3GraphComponent component;
	public static boolean isOpened = false;
	private mxIEventListener modelListener;
	private MouseAdapter selectionListener;
	private JTextField fractionField;
	private JTextField nameField;
	private JList<ValueTransaction> vtList;
	private DefaultListModel<ValueTransaction> listModel;
	
	private boolean doNotUpdateUI = false;
	
	ValueTransaction selectedValueTransaction = null;

	public ValueTransactionDialog(Main main) {
		this.main = main;
		
		isOpened = true;
		
		setTitle("ValueTransaction Editor");
		
		//////////////////////
		// Build the dialog //
		//////////////////////

		JPanel mainPanel = new JPanel();
		mainPanel.setBorder(new EmptyBorder(4, 4, 4, 2));
		getContentPane().add(mainPanel, BorderLayout.CENTER);
		mainPanel.setLayout(new BorderLayout(0, 0));
		
		JPanel bottomPanel = new JPanel();
		mainPanel.add(bottomPanel, BorderLayout.SOUTH);
		bottomPanel.setLayout(new BorderLayout(0, 0));
		
		JPanel dataPanel = new JPanel();
		bottomPanel.add(dataPanel, BorderLayout.CENTER);
		dataPanel.setLayout(new MigLayout("", "[20%][80%]", "[20px][20px]"));
		
		JLabel nameLabel = new JLabel("Name");
		dataPanel.add(nameLabel, "cell 0 0,alignx left,growy");
		
		nameField = new JTextField();
		dataPanel.add(nameField, "cell 1 0,grow");
		nameField.setColumns(10);
		
		nameField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void changedUpdate(DocumentEvent arg0) {
				exec(arg0);
			}

			@Override
			public void insertUpdate(DocumentEvent arg0) {
				exec(arg0);
			}

			@Override
			public void removeUpdate(DocumentEvent arg0) {
				exec(arg0);
			}
			
			public void exec(DocumentEvent arg0) {
				if (selectedValueTransaction != null) {
					selectedValueTransaction.name = nameField.getText();
					if (vtList != null && !doNotUpdateUI) {
						vtList.updateUI();
					}
				}
			}
		});
		
		JLabel fractionLabel = new JLabel("Fraction");
		dataPanel.add(fractionLabel, "cell 0 1,grow");
		
		fractionField = new JTextField();
		dataPanel.add(fractionField, "cell 1 1,grow");
		fractionField.setColumns(10);
		
		fractionField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				exec(e);
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				exec(e);
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				exec(e);
			}
			
			public void exec(DocumentEvent e) {
				if (selectedValueTransaction != null) {
					selectedValueTransaction.formulas.put("FRACTION", fractionField.getText());
					if (vtList != null && !doNotUpdateUI) {
						vtList.updateUI();
					}
				}
			}
		});
		
		JPanel closePanel = new JPanel();
		bottomPanel.add(closePanel, BorderLayout.SOUTH);
		
		JButton closeButton = new JButton("Close");
		closePanel.add(closeButton);
		
		closeButton.addActionListener(e -> {
			// Simple dialog close event
			ValueTransactionDialog.this.setVisible(false);
			ValueTransactionDialog.this.dispatchEvent(new WindowEvent(ValueTransactionDialog.this, WindowEvent.WINDOW_CLOSING));
		});
		
		JPanel listPanel = new JPanel();
		mainPanel.add(listPanel, BorderLayout.CENTER);
		listPanel.setLayout(new BorderLayout(0, 0));

		JPanel buttonPanel = new JPanel();
		listPanel.add(buttonPanel, BorderLayout.NORTH);
		
		JButton newButton = new JButton("New");
		buttonPanel.add(newButton);
		
		newButton.addActionListener(e -> {
			long SUID = Utils.getUnusedID(graph);
			ValueTransaction vtInfo = new ValueTransaction(SUID);

			listModel.addElement(vtInfo);
			graph.valueTransactions.add(vtInfo);
		});
		
		JButton deleteButton = new JButton("Delete");
		buttonPanel.add(deleteButton);
		
		deleteButton.addActionListener(e -> {
			if (listModel.contains(selectedValueTransaction)) {
				listModel.remove(listModel.indexOf(selectedValueTransaction));
				graph.valueTransactions.remove(selectedValueTransaction);
				updateListAndGraph();
			}
		});
		
		listModel = new DefaultListModel<>();
		vtList = new JList<>(listModel);
		
		vtList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) return; // We only do something if the event is final, i.e. if the event is the last one
				
				int selectedIndex = vtList.getSelectedIndex();
				if (selectedIndex == -1) return;

				ValueTransaction vt = listModel.getElementAt(selectedIndex);
				
				selectedValueTransaction = vt;
				removeHighlight();
				highlight(vt);

				// @Hack this is so ugly
				doNotUpdateUI = true;
				nameField.setText(vt.name);
				fractionField.setText(vt.formulas.getOrDefault("FRACTION", "1"));
				doNotUpdateUI = false;
			}
		});
		
		JScrollPane scrollPane = new JScrollPane(vtList);
		listPanel.add(scrollPane, BorderLayout.CENTER);
		
		setSize(400, 300);
		
		//////////////////////////////
		// Done building the dialog //
		//////////////////////////////

		// This should refresh the window everytime the user switches to a new tab
		changeListener = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				setFocusToCurrentGraph();
			}
		};
		main.views.addChangeListener(changeListener);

		// To listen when the dialog closes.
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				cleanupGraph();
				main.views.removeChangeListener(changeListener);
				
				isOpened = false;
			}
		});

		// Don't think we need this
		modelListener = new mxIEventListener() {
			@Override
			public void invoke(Object sender, mxEventObject evt) {
				rebuildList();
			}
		};
		
		selectionListener = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// Get the currently selected value object.
				// If nothing is selected, we don't have to do anything.
				int index = vtList.getSelectedIndex();
				if (index == -1) return;
				ValueTransaction vtInfo = vtList.getSelectedValue();

				// Get the info object of the cell below the click position
				Object cell = main.getCurrentGraphComponent().getCellAt(e.getX(), e.getY());
				// Get the info object and copy it implicitly
				Base value = Utils.base(graph, cell);

				// If it is a value exchange...
				if (value instanceof ValueExchange) {
					ValueExchange veInfo = (ValueExchange) value;
					
					if (!vtInfo.exchanges.contains(veInfo.SUID)) {
						vtInfo.exchanges.add(veInfo.SUID);
					} else {
						vtInfo.exchanges.remove((Long) veInfo.SUID);
					}
					
					// @Incomplete Ideally there'd be an undo event here.
					// But I'm doing that only if the rest turns out to be easy

					System.out.println("Added VE #" + veInfo.SUID + " to VT " + vtInfo.SUID);
					updateListAndGraph();
				}
			}
		};
		
		setFocusToCurrentGraph();
	}
	
	public void cleanupGraph() {
		graph.valueTransactions.clear();
		for (int i = 0; i < listModel.size(); i++) {
			graph.valueTransactions.add(listModel.getElementAt(i));
		}
		
		removeHighlight();
		
		graph.repaint();

		graph.getModel().removeListener(modelListener);
		component.getGraphControl().removeMouseListener(selectionListener);
		
		selectedValueTransaction = null;
		nameField.setText("");
		fractionField.setText("");
	}
	
	public void setFocusToCurrentGraph() {
		if (graph != null) {
			cleanupGraph();
		}
		
		// Get and save the new current graph & component
		graph = main.getCurrentGraph();
		component = main.getCurrentGraphComponent();

		// Add the listeners
		graph.getModel().addListener("change", modelListener);
		component.getGraphControl().addMouseListener(selectionListener);
		
		// Add all value transactions from current graph
		listModel.removeAllElements();
		for (ValueTransaction vt : graph.valueTransactions) {
			listModel.addElement(vt);
		}
	}
	
	public void updateListAndGraph() {
		vtList.updateUI();
		removeHighlight();
		if (vtList.getSelectedIndex() != -1) {
			highlight(vtList.getSelectedValue());
		}
	}
	
	public void highlight(ValueTransaction vt) {
		Utils.getAllCells(graph).stream()
			.filter(cell -> {
				Base info = (Base) graph.getModel().getValue(cell);
				if (info == null) return false;

				return vt.exchanges.contains(info.SUID);
			})
			.forEach(ve -> {
				Utils.setCellStateProperty(graph, ve, mxConstants.STYLE_STROKECOLOR, HIGHLIGHT_COLOR);
			});
		
		graph.repaint();
	}
	
	public void removeHighlight() {
		Utils.getAllCells(graph).stream()
			.filter(cell -> graph.getModel().getValue(cell) instanceof ValueExchange)
			.forEach(ve -> {
				Utils.resetCellStateProperty(graph, ve, mxConstants.STYLE_STROKECOLOR);
			});

		graph.repaint();
	}
	
	public void rebuildList() {
		System.out.println("rebuildList called!");
	}

}
