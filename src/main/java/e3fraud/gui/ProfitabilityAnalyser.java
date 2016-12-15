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

import com.hp.hpl.jena.rdf.model.Resource;
import e3fraud.model.E3Model;
import e3fraud.tools.currentTime;
import java.awt.Dimension;
import java.util.Map;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;

/**
 *
 * @author Dan
 */
public class ProfitabilityAnalyser {

    static boolean debug = false;
    
    private static int needStartValue = 0, needEndValue = 0;
    private static Resource selectedNeed;
    private static JFreeChart chart;
    private static String selectedActorString;

    public static JFreeChart getProfitabilityAnalysis(E3Model model, boolean ideal) {
        if(debug) System.out.println(currentTime.currentTime() + " Starting profitability analysis...");
        Map<String, Resource> actorsMap = model.getActorsMap();

        //have the user indicate the ToA via pop-up
        if (!ideal) {
            JFrame frame1 = new JFrame("Select Target of Assessment");
            selectedActorString = (String) JOptionPane.showInputDialog(frame1,
                    "Which actor's perspective are you taking?",
                    "Choose main actor",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    actorsMap.keySet().toArray(),
                    actorsMap.keySet().toArray()[0]);
        } else {
            selectedActorString = (String) actorsMap.keySet().toArray()[0];
        }
        
        if (selectedActorString == null) {
            if(debug) System.out.println(currentTime.currentTime() + " Profitability analysis cancelled by user!");
        } else {
            //have the user select a need via pop-up
            JFrame frame2 = new JFrame("Select graph parameter");
            Map<String, Resource> needsMap = model.getNeedsMap();
            String selectedNeedString = (String) JOptionPane.showInputDialog(frame2,
                    "Which need (start stimulus) would you like to use on the X-Axis?",
                    "Choose need to parametrize",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    needsMap.keySet().toArray(),
                    needsMap.keySet().toArray()[0]);
            if (selectedNeedString == null) {
                if(debug) System.out.println(currentTime.currentTime() + "Profitability analysis cancelled by user!");
            } else {
                //have the user select occurence interval via pop-up
                JTextField xField = new JTextField("1", 4);
                JTextField yField = new JTextField("500", 4);
                JPanel myPanel = new JPanel();
                myPanel.add(new JLabel("Mininum occurences of "+selectedNeedString+":"));
                myPanel.add(xField);
                myPanel.add(Box.createHorizontalStrut(15)); // a spacer
                myPanel.add(new JLabel("Maximum occurences of "+selectedNeedString+":"));
                myPanel.add(yField);
                int result = JOptionPane.showConfirmDialog(null, myPanel,
                        "Please enter X-axis range", JOptionPane.OK_CANCEL_OPTION);
                
                if (result == JOptionPane.CANCEL_OPTION) {
                    if(debug) System.out.println(currentTime.currentTime() + "Profitability analysis cancelled by user!");
                } else if (result == JOptionPane.OK_OPTION) {
                    needStartValue = Integer.parseInt(xField.getText());
                    needEndValue = Integer.parseInt(yField.getText());
                    selectedNeed = needsMap.get(selectedNeedString);
                    model.getAveragesForActors(selectedNeed, needStartValue, needEndValue, ideal);
                    
                    try {
                        chart = ChartGenerator.generateChart(model, selectedNeed, needStartValue, needEndValue, ideal);//expected graph 
                        return chart;
                    } catch (java.lang.IllegalArgumentException e) {
                        PopUps.infoBox("Duplicate actors are not supported. Please make sure all actors have unique names", "Error");
                    }
                }

            }

        }
        return null;
    }
}
