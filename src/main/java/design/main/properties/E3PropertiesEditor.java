package design.main.properties;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import design.main.Info;
import design.main.Main;
import design.main.Info.Base;

public class E3PropertiesEditor {
	private JLabel idLabel;
	private JTextField nameField;
	private JPanel topPanel;
	private JTable formulaTable;
	private JTextArea editArea;
	
	private int editingRow = -1;
	private int editingCol = -1;
	private boolean changingTextArea = false;
	private boolean changingCell = false;
	private JDialog dialog;
	
	private Info.Base object;
	
	public final List<E3PropertiesEventListener> listeners = new ArrayList<>();
	
	public void addE3PropertiesListener(E3PropertiesEventListener listener) {
		listeners.add(listener);
	}
	
	public void fireEvent(E3PropertiesEvent event) {
		for (E3PropertiesEventListener listener : listeners) {
			listener.invoke(event);
		}
	}
	
	private void setEditingField(int row, int col) {
		editingRow = row;
		editingCol = col;
		
		if (editingRow == -1 || editingCol == -1) return;
		
		changingTextArea = true;
		editArea.setText((String) formulaTable.getModel().getValueAt(row, col));
		changingTextArea = false;
	}

	public E3PropertiesEditor(JFrame owner, Info.Base object_) {
		object = object_.getCopy();
		
		topPanel = new JPanel();
		topPanel.setLayout(new GridBagLayout());
		
		idLabel = new JLabel(""+object.SUID);
		nameField = new JTextField(object.name);

		Object[][] data = new Object[object.formulas.size()][2];
		{
			int i = 0; // To limit the scope of i (I'm also using it 40+ lines down)
			for (String key : object.formulas.keySet()) {
				data[i][0] =  key;
				data[i][1] = object.formulas.get(key);
				
				i++;
			}
		}

		formulaTable = new JTable(new DefaultTableModel(data, new Object[]{"Name", "Formula"}));
		formulaTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		formulaTable.setCellSelectionEnabled(true);
		formulaTable.getTableHeader().setReorderingAllowed(false);
		formulaTable.getTableHeader().addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {
				if (formulaTable.getCellEditor() != null) {
					formulaTable.getCellEditor().stopCellEditing();
				}
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				if (formulaTable.getCellEditor() != null) {
					formulaTable.getCellEditor().stopCellEditing();
				}
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				if (formulaTable.getCellEditor() != null) {
					formulaTable.getCellEditor().stopCellEditing();
				}
			}
		});
		formulaTable.putClientProperty("terminateEditOnFocusLost", true);

		formulaTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				setEditingField(formulaTable.getSelectedRow(), formulaTable.getSelectedColumn());
			}
		});

		formulaTable.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
			public void columnSelectionChanged(ListSelectionEvent e) {
				setEditingField(formulaTable.getSelectedRow(), formulaTable.getSelectedColumn());
			}

			@Override
			public void columnAdded(TableColumnModelEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void columnMarginChanged(ChangeEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void columnMoved(TableColumnModelEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void columnRemoved(TableColumnModelEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		
		formulaTable.getModel().addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				if (!changingCell) {
					changingTextArea = true;
					if (e.getFirstRow() != -1 && e.getColumn() != -1) {
						editArea.setText((String) formulaTable.getModel().getValueAt(e.getFirstRow(), e.getColumn()));
					}
					changingTextArea = false;
				}
			}
		});

		JScrollPane formulaPane = new JScrollPane(formulaTable);
		formulaPane.setPreferredSize(new Dimension(1, 1));
		
		JPanel buttonPane = new JPanel();
		buttonPane.add(new JButton(new AbstractAction("New row") {
			@Override
			public void actionPerformed(ActionEvent e) {
				((DefaultTableModel) formulaTable.getModel()).addRow(new String[]{"", ""});
			}
		}));
		buttonPane.add(new JButton(new AbstractAction("Delete row") {
			@Override
			public void actionPerformed(ActionEvent e) {
				int row = formulaTable.getSelectedRow();
				int col = formulaTable.getSelectedColumn();

				if (row == -1 || col == -1) return;
				
				String name = (String) formulaTable.getModel().getValueAt(row, 0);
				String formula = (String) formulaTable.getModel().getValueAt(row, 1);
				
				if ((name.trim() + name.trim()).length() > 0) {
					int response = JOptionPane.showConfirmDialog(
							Main.mainFrame,
							"You are about to delete the formula \""
									+ name 
									+ "\". Would you like to proceed?",
							"Deletion confirmation",
							JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.WARNING_MESSAGE
							);
					
					if (response != JOptionPane.OK_OPTION) {
						return;
					}
				}
				
				((DefaultTableModel) formulaTable.getModel()).removeRow(row);
			}
		}));

		List<String> labels = new ArrayList<>(Arrays.asList("ID:", "Name:", "Formulas:", ""));
		List<Component> labelComponents = new ArrayList<>();
		for (String label : labels) {
			labelComponents.add(new JLabel(label));
		}
		
		List<Component> components = new ArrayList<>(Arrays.asList(idLabel, nameField, formulaPane, buttonPane));
		
		for (int i = 0; i < labelComponents.size(); i++) {
			Component label = labelComponents.get(i);
			Component comp = components.get(i);
			
			GridBagConstraints c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = i;
			c.anchor = GridBagConstraints.FIRST_LINE_START;
			c.weightx = 0;
			c.insets = new Insets(5, 5, 5, 5);
			topPanel.add(label, c);
			
			c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 1;
			c.gridy = i;
			c.anchor = GridBagConstraints.FIRST_LINE_START;
			c.weightx = 1;
			c.insets = new Insets(5, 5, 5, 5);
			
			if (i == 2) {
				c.weighty = 1;
				c.fill = GridBagConstraints.BOTH;
			}
			
			topPanel.add(comp, c);
		}
		
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new GridBagLayout());
		
		JLabel editLabel = new JLabel("Edit:");
		editArea = new JTextArea();
		editArea.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				if (!changingTextArea) {
					changingCell = true;
					formulaTable.getModel().setValueAt(editArea.getText(), editingRow, editingCol);
					changingCell = false;
				}
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				if (!changingTextArea) {
					changingCell = true;
					formulaTable.getModel().setValueAt(editArea.getText(), editingRow, editingCol);
					changingCell = false;
				}
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
			}
		});
		JScrollPane editPane = new JScrollPane(editArea);
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.weightx = 0;
		c.insets = new Insets(5, 5, 5, 5);
		bottomPanel.add(editLabel, c);

		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 1;
		c.gridy = 0;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.weightx = 1;
		c.weighty = 1;
		c.insets = new Insets(5, 5, 5, 5);
		bottomPanel.add(editPane, c);

		dialog = new JDialog(owner, "Edit object");
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, bottomPanel);
		splitPane.setResizeWeight(0.8);
		
		dialog.add(splitPane);
		
		dialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (formulaTable.getCellEditor() != null) {
					formulaTable.getCellEditor().stopCellEditing();
				}
				
				object.name = nameField.getText();
				object.formulas.clear();
				for (int i = 0; i < formulaTable.getModel().getRowCount(); i++) {
					String name = (String) formulaTable.getModel().getValueAt(i, 0);
					String formula = (String) formulaTable.getModel().getValueAt(i, 1);
					object.formulas.put(name, formula);
				}
				
				fireEvent(new E3PropertiesEvent(this, object));
			}
		});
	}
	
	public void show() {
		dialog.pack();
		dialog.setSize(640, 480);
		dialog.setVisible(true);
	}
}