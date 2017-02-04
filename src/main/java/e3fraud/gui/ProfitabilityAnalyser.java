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
import java.util.HashMap;
import java.util.Map;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.jfree.chart.JFreeChart;

/**
 *
 * @author Dan
 */
public class ProfitabilityAnalyser {

    static boolean debug = false;
    
    private static int needStartValue = 0, needEndValue = 0;
    private static Resource selectedNeedOrMarketSegment;
    private static JFreeChart chart;
    private static String selectedActorString;

    public static JFreeChart getProfitabilityAnalysis(E3Model model, boolean ideal) {
        if(debug) System.out.println(currentTime.currentTime() + " Starting profitability analysis...");
        Map<String, Resource> msMap = model.getMSMap();        
        Map<String, Resource> needsMap = model.getNeedsMap();
        
        //populate list of possible parameters
        Map<String, Resource> parameters = new HashMap<>();
        for (String marketSegmentName : msMap.keySet()){
            parameters.put(marketSegmentName + " COUNT",msMap.get(marketSegmentName));
        }
        for (String needName : needsMap.keySet()){
            parameters.put("OCCURRENCES of " +needName, needsMap.get(needName));
        }

            //have the user select a need via pop-up
            JFrame frame2 = new JFrame("Select chart parameter");
            String selectedParameter = (String) JOptionPane.showInputDialog(frame2,
                    "Which parameter would you like to use on the X-axis?",
                    "Choose parameter",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    parameters.keySet().toArray(),
                    parameters.keySet().toArray()[0]);
            if (selectedParameter == null) {
                if(debug) System.out.println(currentTime.currentTime() + "Profitability analysis cancelled by user!");
            } else {
                //have the user select occurence interval via pop-up
                JTextField xField = new JTextField("1", 4);
                JTextField yField = new JTextField("500", 4);
                JPanel myPanel = new JPanel();
                myPanel.add(new JLabel("Start value :"));
                myPanel.add(xField);
                myPanel.add(Box.createHorizontalStrut(15)); // a spacer
                myPanel.add(new JLabel("End value:"));
                myPanel.add(yField);
                int result = JOptionPane.showConfirmDialog(null, myPanel,
                        "Please enter X-axis range", JOptionPane.OK_CANCEL_OPTION);
                
                if (result == JOptionPane.CANCEL_OPTION) {
                    if(debug) System.out.println(currentTime.currentTime() + "Profitability analysis cancelled by user!");
                } else if (result == JOptionPane.OK_OPTION) {
                    needStartValue = Integer.parseInt(xField.getText());
                    needEndValue = Integer.parseInt(yField.getText());
                    selectedNeedOrMarketSegment = parameters.get(selectedParameter);
                    model.generateSeriesAndComputeAverages(selectedNeedOrMarketSegment, needStartValue, needEndValue, ideal);
                    
                    try {
                        chart = ChartGenerator.generateChart(model, selectedNeedOrMarketSegment, needStartValue, needEndValue, ideal);
                        return chart;
                    } catch (java.lang.IllegalArgumentException e) {
                        PopUps.infoBox("Duplicate actors are not supported. Please make sure all actors have unique names", "Error");
                    }
                }

            }
        return null;
    }
}
