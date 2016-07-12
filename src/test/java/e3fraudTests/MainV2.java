/*******************************************************************************
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
 *******************************************************************************/
package e3fraudTests;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import javax.swing.SwingUtilities;
import e3fraud.gui.FraudWindow;
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
            	
                FraudWindow.createAndShowGUI(FileParser.parseFile(new File("C:\\Users\\Dan\\Documents\\Scenario 1.rdf")));
            }
        });
    }
}
