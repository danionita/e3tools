/*
 * Copyright (C) 2015 Dan Ionita 
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
package e3fraud.parser;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import e3fraud.model.E3Model;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 *
 * @author IonitaD
 */
public class FileParser extends JFrame {

    FileReader fr;

    /**
     * @param
     */
    public static E3Model parseFile(File file) {
        //Load file
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FileParser.class.getName()).log(Level.SEVERE, "File Not Found!", ex);
        }

        //First, replace undeline (_) with dashes(-)
        //This is because e3valuetoolkit does a bad job at exporting RDF and outputs _ instead of -
        SearchAndReplaceInputStream fixedInputStream = new SearchAndReplaceInputStream(inputStream, "_", "-");

        //creating THE JENA MODEL
        Model model = ModelFactory.createDefaultModel();
        model.read(fixedInputStream, null);
        //convert it to a E3Model
        e3fraud.model.E3Model e3model = new e3fraud.model.E3Model(model);

        return e3model;
    }

    public static boolean writeFile(String path, E3Model model) {
        File file = new File(path);
        FileOutputStream out = null;

        try {
            out =  new FileOutputStream(file);
        } catch (FileNotFoundException ex) {
           
        }
        model.getJenaModel().setNsPrefix("a", "http://www.cs.vu.nl/~gordijn/e3value#");
        model.getJenaModel().write(out,"RDF/XML");
        try {
            //RDFDataMgr.write(out, model.getJenaModel(), Lang.RDFXML);
            out.close();
        } catch (IOException ex) {
            Logger.getLogger(FileParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }
}
