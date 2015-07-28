/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.spelementex;

import edu.stanford.nlp.ling.CoreLabel;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import main.java.parsers.StanfordParser;
import main.java.spelementex.ling.CrfFeature;
import main.java.spelementex.markup.Doc;
import main.java.spelementex.markup.SpatialElement;
import main.java.spelementex.markup.SpatialRelation;
import static main.java.spelementex.markup.SpatialRelation.ROLE_VALIDTYPES_MAP;
import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;

/**
 *
 * @author Jenny D'Souza
 */
public class Annotator {
    
    /**
     * Map to associate entity types with their corresponding ID prefix.
     */
    public static final Map<String, String> TYPE_PREFIX_MAP;
    static {
        Map<String, String> aMap = new HashMap<>();
        aMap.put("PLACE", "pl");
        aMap.put("PATH", "p");
        aMap.put("SPATIAL_ENTITY", "se");
        aMap.put("NONMOTION_EVENT", "e");
        aMap.put("MOTION", "m");
        aMap.put("SPATIAL_SIGNAL", "s");
        aMap.put("MOTION_SIGNAL", "ms");
        TYPE_PREFIX_MAP = Collections.unmodifiableMap(aMap);        
    }        
    
    private final Collection<File> testFiles;        
    public Annotator(Collection<File> testFiles) {
        this.testFiles = testFiles;
    }    
       
    public void writeCrfRolesData(Map<Integer, Map<Integer, CoreLabel>> sentenceStartOffsetParserLabel, 
            Map<Integer, SpatialElement> startOffsetSpatialElement, FileOutputStream output) throws IOException {  
        SpatialElement se = null;
        int[] roleOffsets;
        String featureStr = "";
        
        for (int sentence : sentenceStartOffsetParserLabel.keySet()) {
            Map<Integer, CoreLabel> startOffsetParserLabel = sentenceStartOffsetParserLabel.get(sentence);
            roleOffsets = new int[2];
            
            for (int startOffset : startOffsetParserLabel.keySet()) {
                CoreLabel token = startOffsetParserLabel.get(startOffset);
                // this is the text of the token
                String word = token.word();

                //generate crf feature
                CrfFeature crfFeat = new CrfFeature(word, token.lemma(), token.tag(), token.ner());
                crfFeat.setUniFV();

                se = startOffsetSpatialElement.containsKey(startOffset) ? startOffsetSpatialElement.get(startOffset) : 
                        startOffset >= roleOffsets[1] ? SpatialElement.getSE(word, startOffset, startOffsetSpatialElement) : se;
                boolean isMotionSignal = se == null ? false : se.isMotionSignal();
                boolean isMotionSignalOnly = se == null ? false : se.isMotionSignalOnly();                        

                if (se == null)
                    crfFeat.setType();
                else
                    crfFeat.setType(se.getNonMotionSignalType(), isMotionSignal, isMotionSignalOnly);
                featureStr += crfFeat.toString()+"\n";                
            }
            output.write((featureStr+"\n").getBytes());
            featureStr = "";            
        }
    }
    
    /**
     * Writes a single test data file to use as input for CRF++ toolkit.
     * 
     * @param sentenceStartOffsetParserLabel
     * @param output
     * @throws IOException 
     */
    public void writeCrfData(Map<Integer, Map<Integer, CoreLabel>> sentenceStartOffsetParserLabel, FileOutputStream output) throws IOException {
        String featureStr = "";
        for (int sentence : sentenceStartOffsetParserLabel.keySet()) {
            Map<Integer, CoreLabel> startOffsetParserLabel = sentenceStartOffsetParserLabel.get(sentence);

            for (int startOffset : startOffsetParserLabel.keySet()) {
                CoreLabel token = startOffsetParserLabel.get(startOffset);
                //generate crf feature
                CrfFeature crfFeat = new CrfFeature(token.word(), token.lemma(), token.tag(), token.ner());
                crfFeat.setUniFV();
                featureStr += crfFeat.toString()+"\n";
            }
            
            output.write((featureStr+"\n").getBytes());
            featureStr = "";
        }
    }    
     
    public void annotateDocument(Doc document, Map<Integer, Map<Integer, Integer>> sentenceTokenStartOffsetInfo,
            Map<Integer, Map<Integer, CoreLabel>> sentenceStartOffsetParserLabel, String dataDir) throws IOException {
        
        Map<String, Integer> roleIndexMap = new HashMap<>();
        Map<String, Integer> otherRoleIndexMap = new HashMap<>();
        
        for (int sentence : sentenceTokenStartOffsetInfo.keySet()) {
            Map<Integer, Integer> tokenStartOffsetInfo = sentenceTokenStartOffsetInfo.get(sentence);
            Map<Integer, CoreLabel> startOffsetParserLabel = sentenceStartOffsetParserLabel.get(sentence);
            
            for (String role : Evaluator.ROLE_C_MAP.keySet()) {
                String[] result = FileUtils.readFileToString(new File(dataDir+"\\"+role+"Result.txt")).split("\\n");
                roleIndexMap.put(role, document.setDocumentSpatialRoleAnnotations(tokenStartOffsetInfo, startOffsetParserLabel, 
                        role, !roleIndexMap.containsKey(role) ? 0 : roleIndexMap.get(role), result.length, result));
            }
            for (String otherRole : Evaluator.OTHERROLE_C_MAP.keySet()) {
                String[] result = FileUtils.readFileToString(new File(dataDir+"\\"+otherRole+"ResultOther.txt")).split("\\n");
                otherRoleIndexMap.put(otherRole, document.setDocumentSpatialRoleAnnotations(tokenStartOffsetInfo, startOffsetParserLabel, 
                        "", !otherRoleIndexMap.containsKey(otherRole) ? 0 : otherRoleIndexMap.get(otherRole), result.length, result));                
            }
            
        }
    }
    
    /**
     * Iterates over test files, applies the CRF model on each of them,
     * and produces new output files marked up with their spatial roles.
     * For e.g., given file "1.xml", 
     * this function will apply the role-based and non-role-based CRF models on its text, 
     * obtain predictions and write them into "1.xml" inn the output directory.
     * 
     * @param dataDir
     * @param modelDir
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException 
     */
    public void markUpSpatialElementRoles(String dataDir, String modelDir) throws SAXException, ParserConfigurationException, IOException {
        //all annotated files will be written to output directory
        File outputDir = new File("output");
        if (!outputDir.exists())
            outputDir.mkdirs();
        
        for (File file : testFiles) {
            Doc document = new Doc();
            //parse input xml file for its text and spatial elements
            Main.dp.parseXmlAnnotationsFile(file.toString(), document, true, false);
            //map which stores all spatial elements
            Map<Integer, SpatialElement> startOffsetSpatialElement = new HashMap<>(document.getStartOffsetSpatialElement()); 
            //map to store the sentence-wise nlp info of the text document
            //obtained from the Stanford CoreNLP parser            
            Map<Integer, Map<Integer, CoreLabel>> sentenceStartOffsetParserLabel = StanfordParser.parse(document);        
            Map<Integer, Map<Integer, Integer>> sentenceTokenStartOffsetInfo = StanfordParser.getSentenceTokenStartOffsetInfo(sentenceStartOffsetParserLabel);
            
            //prepare CRF test data file
            System.out.println("annotating "+file.getPath());
            writeCrfRolesData(sentenceStartOffsetParserLabel, startOffsetSpatialElement, new FileOutputStream(dataDir+"\\test.txt"));
            
            //generate result
            for (String r : Evaluator.ROLE_NBEST_MAP.keySet()) 
                Evaluator.applyModel(Evaluator.ROLE_NBEST_MAP.get(r), modelDir+"\\"+r+"Model.txt", dataDir+"\\test.txt", dataDir+"\\"+r+"Result.txt");
            for (String r : Evaluator.OTHERROLE_NBEST_MAP.keySet()) 
                Evaluator.applyModel(Evaluator.OTHERROLE_NBEST_MAP.get(r), modelDir+"\\"+r+"ModelOther.txt", dataDir+"\\test.txt", dataDir+"\\"+r+"ResultOther.txt");
            
            //gathers all role annotations for spatial elements in this file
            annotateDocument(document, sentenceTokenStartOffsetInfo, sentenceStartOffsetParserLabel, dataDir);     
            
            //writes the annotations to output
            String outputFileName = outputDir.toString()+"\\"+file.getName();
            document.writeRoleAnnotations(outputFileName);
        }
    }    
    
    /**
     * Annotates document with predicted spatial entities and motion signals.
     * 
     * @param document
     * @param sentenceTokenStartOffsetInfo
     * @param sentenceStartOffsetParserLabel
     * @param seResult
     * @param msResult
     */
    public void annotateDocument(Doc document, Map<Integer, Map<Integer, Integer>> sentenceTokenStartOffsetInfo, 
            Map<Integer, Map<Integer, CoreLabel>> sentenceStartOffsetParserLabel, String[] seResult, String[] msResult) {
        
        int seIndex = 0;
        int seLength = seResult.length;
        int msIndex = 0;
        int msLength = msResult.length;
        
        for (int sentence : sentenceTokenStartOffsetInfo.keySet()) {
            Map<Integer, Integer> tokenStartOffsetInfo = sentenceTokenStartOffsetInfo.get(sentence);
            Map<Integer, CoreLabel> startOffsetParserLabel = sentenceStartOffsetParserLabel.get(sentence);
            
            seIndex = document.setDocumentSpatialElementAnnotations(tokenStartOffsetInfo, startOffsetParserLabel, seIndex, seLength, seResult);
            msIndex = document.setDocumentSpatialElementAnnotations(tokenStartOffsetInfo, startOffsetParserLabel, msIndex, msLength, msResult);
        }        
    }
        
    /**
     * Iterates over test files, applies the CRF model on each of them,
     * and produces new output files marked up with their spatial entities.
     * For e.g., given file "1.xml", this function will apply the CRF model
     * on its text, obtain spatial entity predictions, 
     * and write the predictions into a new file "1.xml" in the output directory.
     * 
     * @param seModelFileName
     * @param msModelFileName
     * @param crfTestFileName
     * @param seCrfResultFileName
     * @param msCrfResultFileName
     * @throws FileNotFoundException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException 
     */
    public void markUpSpatialElementTypes(String seModelFileName, String msModelFileName, 
            String crfTestFileName, String seCrfResultFileName, String msCrfResultFileName) throws FileNotFoundException, SAXException, ParserConfigurationException, IOException {
        FileOutputStream crfOutputFile;
        //all annotated files will be written to output directory
        File outputDir = new File("output");
        if (!outputDir.exists())
            outputDir.mkdirs();
        
        for (File file : testFiles) {            
            Doc document = new Doc();     
            //parses input xml file for its text
            Main.dp.parseXmlAnnotationsFile(file.toString(), document, false, false);
            //map to store the sentence-wise nlp info of the text document
            //obtained from the Stanford CoreNLP parser            
            Map<Integer, Map<Integer, CoreLabel>> sentenceStartOffsetParserLabel = StanfordParser.parse(document);        
            Map<Integer, Map<Integer, Integer>> sentenceTokenStartOffsetInfo = StanfordParser.getSentenceTokenStartOffsetInfo(sentenceStartOffsetParserLabel);
            
            System.out.println("annotating "+file.getPath());
            crfOutputFile = new FileOutputStream(crfTestFileName);
            writeCrfData(sentenceStartOffsetParserLabel, crfOutputFile);
                       
            //applies crf SE model on test data
            Evaluator.applyModel(Evaluator.NBEST_SE, seModelFileName, crfTestFileName, seCrfResultFileName);
            //applies crf MS model on test data
            Evaluator.applyModel(Evaluator.NBEST_MS, msModelFileName, crfTestFileName, msCrfResultFileName);
            
            //annotates the document with CRF predictions
            annotateDocument(document, sentenceTokenStartOffsetInfo, sentenceStartOffsetParserLabel, 
                    FileUtils.readFileToString(new File(seCrfResultFileName)).split("\\n"), 
                    FileUtils.readFileToString(new File(msCrfResultFileName)).split("\\n"));
                        
            String outputFileName = outputDir.toString()+"\\"+file.getName(); //file.toString().substring(0, file.toString().lastIndexOf("."))+"_predict.xml";
            document.writeAnnotations(outputFileName);
        }
    }
        
    public static String getPredictedRole(String roleTag, String role) {
        String predictedRole = getPredictedTagType(roleTag);
        return !role.equals("") && !predictedRole.equals(role) ? "" : predictedRole;
    }
    
    /**
     * Gets type (e.g., MOTION_SIGNAL) given CRF tag (e.g., B-MOTION_SIGNAL).
     * As another example, gets SPATIAL_SIGNAL-TOPOLOGICAL given B-SPATIAL_SIGNAL-TOPOLOGICAL.
     * 
     * @param predictedTag
     * @return string spatial entity type given the CRF B-tag with type.
     */
    public static String getPredictedTagType(String predictedTag) {
        return predictedTag.substring(predictedTag.indexOf("-")+1);
    }    
    
    /**
     * Following B/I/O tag conventions, gets all the tokens spanning a given entity
     * starting at B and continuing through all the following I tags.
     * 
     * @param result
     * @param i
     * @param tokenNum
     * @return 
     */
    public static int[] getTokenOffset(String[] result, int i, int tokenNum) {
        int[] offsets = new int[2];
        for (int j = i; j < result.length; j++) {
            String line = result[j].trim();
            if (line.equals(""))
                break;
            
            if (i == j) {
                offsets[0] = tokenNum;
                continue;
            }
            
            String[] lineTokens = line.split("\\s+");
            if (lineTokens[lineTokens.length-1].equals("O") || lineTokens[lineTokens.length-1].matches("B\\-.*"))
                break;
            
            tokenNum++;
        }
        offsets[1] = tokenNum;        
        return offsets;
    }    
    
}
