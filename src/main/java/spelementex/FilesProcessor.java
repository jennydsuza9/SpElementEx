/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.spelementex;

import java.io.File;
import java.util.Collection;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author Jenny D'Souza
 */
public class FilesProcessor {
    
    public static final String[] XML_EXTENSION = new String[]{"xml"};    
    
    public static Collection<File> getXMLFiles(File dir) {
        return FileUtils.listFiles(dir, XML_EXTENSION, true);
    }    
    
    
}
