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
import com.hp.hpl.jena.vocabulary.RDF;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeriesCollection;
import e3fraud.model.E3Model;
import e3fraud.vocabulary.E3value;
import java.text.DecimalFormat;
import java.util.Map;
import org.jfree.data.xy.XYSeries;

/**
 *
 * @author Dan
 */
public class ChartGenerator {

    public static JFreeChart generateChart(Map<Resource, XYSeries> actorSeriesMap, String parameter) {
        //Get list of actors
        Set<Resource> actors = actorSeriesMap.keySet();

        XYSeriesCollection line_chart_dataset = new XYSeriesCollection();   

        //Then, for each actor
        for (Resource actor : actors) {
            //add it's series to the chart
            XYSeries series = actorSeriesMap.get(actor);
            line_chart_dataset.addSeries(series);
        }

        /* Step -2:Define the JFreeChart object to create line chart */
        JFreeChart lineChartObject = null;
        lineChartObject = ChartFactory.createScatterPlot("", parameter, "Revenue", line_chart_dataset, PlotOrientation.VERTICAL, true, true, false);

        return lineChartObject;
    }

    public static void saveToFile(File file, JFreeChart lineChartObject) throws IOException {
        int width = 1024;
        /* Width of the image */

        int height = 720;
        /* Height of the image */

        ChartUtilities.saveChartAsPNG(file, lineChartObject, width, height);
    }
}
