/*******************************************************************************
 * Copyright (C) 2016 Jaap Gordijn
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
 *******************************************************************************/

package design;

import com.e3value.eval.ncf.E3ParseException;
import com.e3value.eval.ncf.ProfGenerator;
import com.e3value.eval.ncf.ontology.model;
import design.export.RDFExport;
import design.export.RDFExport.VTMode;
import e3fraud.tools.SettingsObjects.NCFSettings;
import javax.swing.*;



import java.awt.*;
import java.awt.Dialog.ModalityType;
import java.awt.event.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import net.miginfocom.swing.MigLayout;

/**
 * Created by IntelliJ IDEA.
 * User: gordijn
 * Date: Dec 30, 2005
 * Time: 4:35:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class NCFDialog extends JDialog {
    // These fields should be saved in this class; in the superclass does not work
	
    private JTextField fileName;
    private JTextField directoryName;
    private JTextField statusField;
    private JCheckBox GenValueActivity;
    private JCheckBox GenActor;
    private JCheckBox GenPerConstruct;
    private JCheckBox vpDirection;
    private JCheckBox vpName;
    private JCheckBox vpValueObject;
    private JCheckBox viValueObjects;
    private JCheckBox viName;
    private JCheckBox vtName;
    private JCheckBox vtValueObject;
    private JCheckBox createTransactions;
    private NCFSettings ncfSettings;
    
    // NVF   form
    JApplet a;
    E3Graph diagram;
 
    public NCFDialog(E3Graph d)
    {
    	super();
    	
        this.diagram = d;
        if(d.ncfSettings!=null){
        this.ncfSettings = d.ncfSettings;}
        else{
            this.ncfSettings = new NCFSettings();
        }
        
        setModalityType(ModalityType.APPLICATION_MODAL);
   	
    	setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    	
    	setTitle("XLS generation");
    	Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation((int)screen.getWidth()/2-this.getWidth()/2,(int)screen.getHeight()/2-this.getHeight()/2);
        
        JPanel contentPane= new JPanel();
        getContentPane().add(contentPane);
        contentPane.setLayout(new MigLayout("", "[77px][9px][89px][5px][289px][4px][77px]", "[20px][20px][219px][23px][20px]"));

        JLabel lblSvgFile = new JLabel("XLS file:");
        contentPane.add(lblSvgFile, "cell 0 0,growx,aligny top");
        
        fileName = new JTextField();
        contentPane.add(fileName, "cell 2 0 3 1,growx,aligny top");
        fileName.setColumns(10);
        if (d.title!= null && d.title.length()>0) {
        	fileName.setText(d.title);
        }
        else {
        	fileName.setText("Untitled");
        }
        
        JLabel lblSvgDirectory = new JLabel("XLS directory:");
        contentPane.add(lblSvgDirectory, "cell 0 1,growx,aligny top");
        
        directoryName = new JTextField();
        directoryName.setColumns(10);
        contentPane.add(directoryName, "cell 2 1 3 1,growx,aligny top");
        
        File defaultDirAsFile = javax.swing.filechooser.FileSystemView.getFileSystemView().getDefaultDirectory();
        String defaultDir = defaultDirAsFile.getPath();
        if (d.file!= null && d.file.length()>0) {
            File f = d.file;
            if (f.getParent() != null) {
            	directoryName.setText(f.getParent());
            }
            else {
            	directoryName.setText(defaultDir);
            }
        }
        else {
        	directoryName.setText(defaultDir);
        }
        
        JButton btnBrowse = new JButton("Browse");
        btnBrowse.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		onBrowse();
        	}
        });
        contentPane.add(btnBrowse, "cell 6 1,grow");
        
        JButton btnGenerate = new JButton("Generate");
        btnGenerate.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		onGenerate();
        	}
        });
        contentPane.add(btnGenerate, "cell 6 3,growx");
//        
//        JButton btnOK = new JButton("OK");
//        btnOK.addActionListener(new ActionListener() {
//        	public void actionPerformed(ActionEvent e) {
//        		onOK();
//        	}
//        });
//        
//        JButton btnCancel = new JButton("Cancel");
//        btnCancel.addActionListener(new ActionListener() {
//        	public void actionPerformed(ActionEvent e) {
//        		onCancel();
//        	}
//        });
//        contentPane.add(btnCancel, "cell 6 4,grow");
//        
//                btnCancel.addActionListener(new ActionListener() {
//                    public void actionPerformed(ActionEvent e) {
//                        onCancel();
//                    }
//                });
//        contentPane.add(btnOK, "cell 6 5,grow");
//        getRootPane().setDefaultButton(btnOK);
//        
//                btnOK.addActionListener(new ActionListener() {
//                    public void actionPerformed(ActionEvent e) {
//                        onOK();
//                    }
//                });
        
        statusField = new JTextField();
        statusField.setEditable(false);
        contentPane.add(statusField, "cell 0 6 7 1,growx,aligny top");
        statusField.setColumns(10);
        
        JPanel Sheets = new JPanel();
        contentPane.add(Sheets, "cell 0 2 3 1,grow");
        Sheets.setLayout(new MigLayout("", "[161px]", "[23px][18px][23px]"));
        
        GenActor = new JCheckBox("Actor sheet");
        GenActor.setSelected(true);
        Sheets.add(GenActor, "cell 0 0,alignx left,aligny top");     
        
        GenValueActivity = new JCheckBox("Value activity sheet");
        Sheets.add(GenValueActivity, "cell 0 1,alignx left,aligny center");
        
        GenPerConstruct = new JCheckBox("Sheet per e3value construct");
        Sheets.add(GenPerConstruct, "cell 0 2,alignx left,aligny top");
        
        JPanel GenProps = new JPanel();
        contentPane.add(GenProps, "cell 4 2 3 1,grow");
        GenProps.setLayout(new MigLayout("", "[150px][34px][166px]", "[14px][23px][20px][23px][14px][][42px][23px]"));
        
        JLabel nVpLabel = new JLabel("Name value ports using their:");
        GenProps.add(nVpLabel, "cell 0 0,growx,aligny top");
        
        JLabel nViLabel = new JLabel("Name value interfaces using their:");
        GenProps.add(nViLabel, "cell 2 0,alignx right,aligny top");
        
        vpName = new JCheckBox("name");
        GenProps.add(vpName, "cell 0 2,alignx left,aligny center");
        vpName.setSelected(ncfSettings.vpName);
        
        viName = new JCheckBox("name");
        viName.setSelected(true);
        GenProps.add(viName, "cell 2 2,alignx left,aligny center");
        viName.setSelected(ncfSettings.viName);
        
        JLabel nVtLabel = new JLabel("Name value transfers using their:");
        GenProps.add(nVtLabel, "cell 0 4,alignx left,aligny top");
        
        vpDirection = new JCheckBox("direction");
        vpDirection.setSelected(true);
        GenProps.add(vpDirection, "cell 0 1,alignx left,aligny top");
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });
        
         
        vpValueObject = new JCheckBox("value object");
        GenProps.add(vpValueObject, "cell 0 3,alignx left,aligny top");
        
        viValueObjects = new JCheckBox("value objects");
        viValueObjects.setSelected(true);
        GenProps.add(viValueObjects, "cell 2 1,alignx left,aligny top");
        
        vtValueObject = new JCheckBox("value objects");
        vtValueObject.setSelected(true);
        GenProps.add(vtValueObject, "cell 0 5,alignx left,aligny bottom");
        vtValueObject.setSelected(ncfSettings.vtValueObject);
        
        vtName = new JCheckBox("name");
        vtName.setSelected(true);
        GenProps.add(vtName, "cell 0 6,alignx left,aligny top");
        
        createTransactions = new JCheckBox("auto-create value transactions");
        createTransactions.setSelected(true);
        GenProps.add(createTransactions, "cell 0 7,alignx left,aligny top");

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }
        , KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
       
        
        vpValueObject.setSelected(ncfSettings.vpValueObject);
        vpDirection.setSelected(ncfSettings.vpDirection);
        GenActor.setSelected(ncfSettings.GenActor);
        GenValueActivity.setSelected(ncfSettings.GenValueActivity);
        GenPerConstruct.setSelected(ncfSettings.GenPerConstruct);
        vtName.setSelected(ncfSettings.vtName);
        viValueObjects.setSelected(ncfSettings.viValueObjects);
        createTransactions.setSelected(ncfSettings.createTransactions);

        pack();
    	setVisible(true);
 
    	btnGenerate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onGenerate();
            }
        });

        /*
        Excel.addActionListener(new ActionListener() {
        
            public void actionPerformed(ActionEvent e) {
                onExcel();
            }
        });
		*/
        
        btnBrowse.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              onBrowse();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }
        , KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        }

    /** Generate XLS form  and open it
    *
    * @return true: succes; false: error
    */

    private boolean onExcel()
    {
        if (onGenerate() == false) {
            return false;
        }

        String fn = "\"" + directoryName.getText();
        if (!fileName.getText().endsWith(".xls")) {
            fn += "\\" + fileName.getText() + ".xls";
        }
        else {
            fn += "\\" + fileName.getText();
        }
        fn += "\"";

        savePreferences();
        dispose();

        return true;
    }

    /**
     * Generate XLS form
     *
     * @return true: succes; false: error
     */
    private boolean onGenerate() {
        String fn;
        String pathName;

        fn = fileName.getText();
        if (!fn.endsWith(".xls")) {
            fn += ".xls";
        }

        pathName = directoryName.getText() + System.getProperty("file.separator") + fn;

        // Check overwrite
        if (!fileExists(pathName)) {
            statusField.setText(pathName + " already exists");
            int res = JOptionPane
                    .showConfirmDialog(null,
                            "Do you want to overwrite " + pathName,
                            "OVERWRITE ?",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
            if (res == JOptionPane.NO_OPTION) {
                return false;
            } else if (res == JOptionPane.CANCEL_OPTION) {
                return false;
            }
        }

     try{
            RDFExport export = new RDFExport(diagram, false, VTMode.DERIVE_ORPHANED, false);
            String result = export.getResult().get();

            InputStream stream = new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8));

            ProfGenerator p = new ProfGenerator();
            p.loadRDFStream(stream);

        
   
            Iterator i = p.getMapObjects().values().iterator();
            int found_models = 0;

            while (i.hasNext()) {
                Object o = i.next();
                if (o instanceof model) {
                    found_models++;
                    if (found_models > 1) {
                        throw new E3ParseException("RDF file should contain exactly one 'model'");
                    }
                    p.setMymodel((model) o);
                }
            }
            p.storeXLS(pathName,
                    viValueObjects.isSelected(),
                    viName.isSelected(),
                    vpValueObject.isSelected(),
                    vpDirection.isSelected(),
                    vpName.isSelected(),
                    vtName.isSelected(),
                    vtValueObject.isSelected(),
                    GenActor.isSelected(),
                    GenValueActivity.isSelected(),
                    GenPerConstruct.isSelected(),
                    createTransactions.isSelected());

            statusField.setText("XLS file generated");
            return true;
        } catch (Exception x) {
            statusField.setText("XLS file could not be generated. Pleae check that the model is correct and the destination file is write-able.");
            x.printStackTrace();
            return false;
        }
    }

    /**
    * Check if file needs to overwriten
    * @param pathName Name of the file to be checked
    * @return true: overwrite; false: do not overwrite
    */
    private boolean fileExists(String pathName)
    {
        File f = new File(pathName);
        if (f.exists()) return false;

        return true;
    }

    /** Show a Browse Window, so that a directory for the export can be selected
     *
    */
    private void onBrowse() {
        // open a filebrowser to select export dir
        final JFileChooser fc = new JFileChooser(directoryName.getText());
        fc.setDialogTitle("Select XLS directory");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int returnVal = fc.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            directoryName.setText(file.getPath());
        }
    }

    private void savePreferences()
    {
    ncfSettings.vpValueObject = vpValueObject.isSelected();
    ncfSettings.vpDirection = vpDirection.isSelected();
    ncfSettings.vpName = vpName.isSelected();
    ncfSettings.vtName = vtName.isSelected();
    ncfSettings.vtValueObject = vtValueObject.isSelected();
    ncfSettings.GenActor = GenActor.isSelected();
    ncfSettings.GenValueActivity = GenValueActivity.isSelected();
    ncfSettings.GenPerConstruct = GenPerConstruct.isSelected();
    ncfSettings.viValueObjects = viValueObjects.isSelected();
    ncfSettings.viName = viName.isSelected();
    ncfSettings.createTransactions = createTransactions.isSelected(); 
    diagram.ncfSettings = ncfSettings;
    }

    private void onOK() {

        savePreferences();

        if (onGenerate())
            dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }
}
