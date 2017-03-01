/*
 * Copyright (C) 2016 Dan
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

import e3fraud.tools.SettingsObjects.GenerationSettings;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 *
 * @author Dan
 */
public class GenerationSettingsDialog extends javax.swing.JDialog {

    private GenerationSettings settings;
    private  Map<String, javax.swing.JCheckBox> valueObjectsMap = new HashMap<>();
    

    public GenerationSettings getSettings() {
        return settings;
    }

    /**
     * Creates new form AdvancedGenerationSettingsDialog
     *
     * @param parent
     * @param modal
     * @param settings
     * @param valueObjects
     */
    public GenerationSettingsDialog(java.awt.Frame parent, boolean modal, GenerationSettings settings, Set<String> valueObjects) {
        super(parent, modal);

        for (String valueObjectName : valueObjects) {
            valueObjectsMap.put(valueObjectName, new JCheckBox(valueObjectName));
        }

        initComponents();

        generateHiddenTransfersCheckbox.setSelected(settings.isGenerateCollusion());
        generateNonOccurringTransfersCheckbox.setSelected(settings.isGenerateNonOccurring());
        generateCollusionCheckbox.setSelected(settings.isGenerateCollusion());
        hiddenTransfersComboBox.setSelectedIndex(settings.getNumberOfHiddenTransfersPerExchange() - 1);//-1 because ComboBox selection is 0-based
        for (String valueObject : valueObjectsMap.keySet()) {
            valueObjectsMap.get(valueObject).setSelected(settings.getTypesOfNonOccurringTransfers().contains(valueObject));
        }
        collusionSpinner.setValue(settings.getColludingActors());

        // Close the dialog when Esc is pressed
        String cancelName = "cancel";
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), cancelName);
        ActionMap actionMap = getRootPane().getActionMap();
        actionMap.put(cancelName, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                doClose(null);
            }
        });
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSpinner1 = new javax.swing.JSpinner();
        generateHiddenTransfersCheckbox = new javax.swing.JCheckBox();
        generateNonOccurringTransfersCheckbox = new javax.swing.JCheckBox();
        generateCollusionCheckbox = new javax.swing.JCheckBox();
        advancedGenerationSettingsLabel = new javax.swing.JLabel();
        collusionSpinner = new javax.swing.JSpinner();
        actorsLabel = new javax.swing.JLabel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        hiddenTransfersComboBox = new javax.swing.JComboBox<>();
        valueObjectCheckboxContainer = new javax.swing.JPanel();
        for (JCheckBox valueObjectCheckbox : valueObjectsMap.values()){
            valueObjectCheckboxContainer.add(valueObjectCheckbox);
        }
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        generateHiddenTransfersCheckbox.setSelected(true);
        generateHiddenTransfersCheckbox.setText("Hidden transfers valuated at");

        generateNonOccurringTransfersCheckbox.setSelected(true);
        generateNonOccurringTransfersCheckbox.setText("Non-occurring transfers of ");

        generateCollusionCheckbox.setSelected(true);
        generateCollusionCheckbox.setText("Collusion of up to");

        advancedGenerationSettingsLabel.setText("Generate fraud scenarios containing:");

        collusionSpinner.setModel(new javax.swing.SpinnerNumberModel(2, 2, null, 1));
        collusionSpinner.setEnabled(generateCollusionCheckbox.isSelected());

        actorsLabel.setText("actors");

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        hiddenTransfersComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "50% of revenue", "33% and 66% of revenue", "25%, 50% and 75% of revenue" }));

        valueObjectCheckboxContainer.setLayout(new javax.swing.BoxLayout(valueObjectCheckboxContainer, javax.swing.BoxLayout.LINE_AXIS));

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/information.png"))); // NOI18N
        jLabel2.setToolTipText("Hidden transfers are transfers which were not present in the value model. Hidden transfers are generated by identifying pairs of untrusted actors, computing their expected profit and adding hidden transfers originating from whichever actor has a positive financial result. The value of these hidden transfers is computed as a fraction of the actors' financial result.");

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/information.png"))); // NOI18N
        jLabel3.setToolTipText("Non-occurring transfers are transfers which were present in the value model but do not take place. Non-occurring transfers are created by invalidating individual transfers. ");

        jLabel4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/information.png"))); // NOI18N
        jLabel4.setToolTipText("Collusion takes place when two or more actors are acting as one: they pool their budgets and collectively bear all expenses and profit. Only untrusted can collude");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(cancelButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(generateHiddenTransfersCheckbox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(hiddenTransfersComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(advancedGenerationSettingsLabel)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(generateCollusionCheckbox)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(collusionSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(actorsLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(generateNonOccurringTransfersCheckbox)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(valueObjectCheckboxContainer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, okButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(advancedGenerationSettingsLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(generateHiddenTransfersCheckbox, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(hiddenTransfersComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(generateNonOccurringTransfersCheckbox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(valueObjectCheckboxContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(generateCollusionCheckbox)
                    .addComponent(collusionSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(actorsLabel)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton)
                    .addComponent(okButton))
                .addContainerGap())
        );

        getRootPane().setDefaultButton(okButton);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        boolean genearateHiddenTransfers = generateHiddenTransfersCheckbox.isSelected();
        boolean generateNonOccurringTransfers = generateNonOccurringTransfersCheckbox.isSelected();
        boolean generateCollusion = generateCollusionCheckbox.isSelected();
        int numberOfColludingActors = (Integer) collusionSpinner.getValue();
        int hiddenTransfersToGenerate = (Integer) hiddenTransfersComboBox.getSelectedIndex() + 1; //+1 because ComboBox selection is 0-based
        List<String> typesOfNonOccurringTransfers = new ArrayList<>();
        for (String valueObjectName : valueObjectsMap.keySet()) {
            if (valueObjectsMap.get(valueObjectName).isSelected()) {
                typesOfNonOccurringTransfers.add(valueObjectName);
            }
        }

        settings = new GenerationSettings(genearateHiddenTransfers, generateNonOccurringTransfers, generateCollusion, numberOfColludingActors, hiddenTransfersToGenerate, typesOfNonOccurringTransfers);
        doClose(settings);
    }//GEN-LAST:event_okButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        doClose(null);
    }//GEN-LAST:event_cancelButtonActionPerformed

    /**
     * Closes the dialog
     */
    private void closeDialog(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_closeDialog
        doClose(null);
    }//GEN-LAST:event_closeDialog

    private void doClose(GenerationSettings settings) {
        this.settings = settings;
        setVisible(false);
        dispose();
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel actorsLabel;
    private javax.swing.JLabel advancedGenerationSettingsLabel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JSpinner collusionSpinner;
    private javax.swing.JCheckBox generateCollusionCheckbox;
    private javax.swing.JCheckBox generateHiddenTransfersCheckbox;
    private javax.swing.JCheckBox generateNonOccurringTransfersCheckbox;
    private javax.swing.JComboBox<String> hiddenTransfersComboBox;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JSpinner jSpinner1;
    private javax.swing.JButton okButton;
    private javax.swing.JPanel valueObjectCheckboxContainer;
    // End of variables declaration//GEN-END:variables

}