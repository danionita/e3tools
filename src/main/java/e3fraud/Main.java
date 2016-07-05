/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package e3fraud
;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import e3fraud.gui.MainWindow;

/**
 *
 * @author IonitaD
 */
public class Main {

    public static void main(String[] args) {
        //Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                //Turn off metal's use of bold fonts
//                UIManager.put("swing.boldMetal", Boolean.FALSE);
            	
//		try {
//			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//		}
//		catch(Exception e){
//			System.out.println("Couldn't set Look and Feel to system");
//		}
            	
                MainWindow.createAndShowGUI();
            }
        });
    }
}
