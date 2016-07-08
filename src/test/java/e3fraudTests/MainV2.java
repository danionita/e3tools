package e3fraudTests;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import javax.swing.SwingUtilities;
import e3fraud.gui.MainWindowV2;
import e3fraud.parser.FileParser;
import java.io.File;
import javax.swing.UIManager;

/**
 *
 * @author IonitaD
 */
public class MainV2 {

    public static void main(String[] args) {
        //Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                //Turn off metal's use of bold fonts
                UIManager.put("swing.boldMetal", Boolean.FALSE);
            	
//		try {
//			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//		}
//		catch(Exception e){
//			System.out.println("Couldn't set Look and Feel to system");
//		}
            	
                MainWindowV2.createAndShowGUI(FileParser.parseFile(new File("C:\\Users\\Dan\\Documents\\Scenario 1.rdf")));
            }
        });
    }
}
