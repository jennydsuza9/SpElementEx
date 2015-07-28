/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.parsers;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import main.java.spelementex.markup.Doc;
import main.java.spelementex.markup.SpatialElement;
import main.java.spelementex.markup.SpatialRelation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jenny D'Souza
 */
public class DomParser {
     
    Document dom;

    private void parseSpatialRelationElements(NodeList nl, String type, Doc document) {
        if (nl == null || nl.getLength() <= 0)
            return;
        
        for (int i = 0; i < nl.getLength(); i++) {
            
            Element el = (Element)nl.item(i);
            
            SpatialRelation sr = new SpatialRelation(el, type);
            sr.processSpatialRelation(document);            
        }
    }
    
    private void parseSpatialElements(NodeList nl, String type, Doc document) throws IOException {
        if (nl == null || nl.getLength() <= 0)
            return;
        
        for (int i = 0; i < nl.getLength(); i++) {
            
            Element el = (Element)nl.item(i);
            
            SpatialElement se = new SpatialElement(el, type);
            se.processSpatialElement();
            
            if (se.getStart() == -1 && se.getEnd() == -1)
                continue;
            
            document.addDocSpatialElement(se);
        }
    }    
    
    /**
     * Parses a document for its text, its spatial element types if type flag is set,
     * and spatial relation participant roles if roles flag is set.
     * 
     */
    private void parseDocument(Doc document, boolean typeFlag, boolean roleFlag) throws IOException{
        //get the root element
        Element docEle = (Element) dom.getDocumentElement();

        if (typeFlag) {
            for (String type : SpatialElement.TYPES) {
                //gets nodelist of all spatial elements of type
                NodeList nl = docEle.getElementsByTagName(type);
                parseSpatialElements(nl, type, document);
            }
        }
        if (roleFlag) {
            for (String type : SpatialRelation.TYPES) {
                //gets nodelist of all spatial relations of type
                NodeList nl = docEle.getElementsByTagName(type);
                parseSpatialRelationElements(nl, type, document);
            }
        }
        
        document.setText(docEle.getElementsByTagName("TEXT").item(0).getTextContent());
    }	    
    
    public void parseXmlAnnotationsFile(String xmlFileName, Doc document, boolean typeFlag, boolean roleFlag) throws org.xml.sax.SAXException, ParserConfigurationException, IOException{
        //get the factory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        //Using factory get an instance of document builder
        DocumentBuilder db = dbf.newDocumentBuilder();

        //parse using builder to get DOM representation of the XML file
        dom = db.parse(xmlFileName);
        parseDocument(document, typeFlag, roleFlag);
    }      
    
    public static String prettyFormat(String input, int indent) throws IOException {
        try {
            Source xmlInput = new StreamSource(new StringReader(input));
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", indent);
            Transformer transformer = transformerFactory.newTransformer(); 
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "US-ASCII");
            transformer.transform(xmlInput, xmlOutput);
            return xmlOutput.getWriter().toString();
        } catch (IllegalArgumentException | TransformerException e) {            
            throw new RuntimeException(e); // simple exception handling, please review it
        }
    }

    public static String prettyFormat(String input) throws IOException {
        return prettyFormat(input, 2);
    } 
        
}
