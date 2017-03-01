/*
 * Copyright (C) 2015, 2016 Dan Ionita 
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
import e3fraud.vocabulary.E3value;
import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeriesCollection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;

/**
 *
 * @author Dan
 */
public class ChartGenerator {

    public static JFreeChart generateChart(Map<Resource, XYSeries> actorSeriesMap, String parameter) {
        //Get list of actors
        Set<Resource> actors = actorSeriesMap.keySet();

        //prepare chart
        XYSeriesCollection line_chart_dataset = new XYSeriesCollection();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setBaseShapesFilled(false);
        

        //Then, for each actor
        int i = 0;
        for (Resource actor : actors) {
            //add it's series to the chart
            XYSeries series = actorSeriesMap.get(actor);
            line_chart_dataset.addSeries(series);

            //select a color based on the actor's name (to maintain actor-color tuples)
            String actorName = actor.getProperty(E3value.e3_has_name).getLiteral().toString();
            renderer.setSeriesPaint(i, stringToColor(actorName));
            renderer.setSeriesStroke(i, new BasicStroke(6.0f));
            i++;
        }

        //Create the chart
        JFreeChart lineChartObject = null;
        lineChartObject = ChartFactory.createXYLineChart("", parameter, "Revenue", line_chart_dataset);
        lineChartObject.getXYPlot().setRenderer(renderer);
        return lineChartObject;
    }

    public static void saveToFile(File file, JFreeChart lineChartObject) throws IOException {
        int width = 1024;
        /* Width of the image */

        int height = 720;
        /* Height of the image */

        ChartUtilities.saveChartAsPNG(file, lineChartObject, width, height);
    }

    
    private static Color stringToColor(String str){
                    byte[] bytesOfMessage = null;
                bytesOfMessage = str.getBytes();
            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(ChartGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
            byte[] thedigest = md.digest(bytesOfMessage);
            
            return new Color(thedigest[0]+128,thedigest[1]+128,thedigest[2]+128);
    }
}
