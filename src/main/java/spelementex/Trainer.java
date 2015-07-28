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
import org.xml.sax.SAXException;

/**
 *
 * @author Jenny D'Souza
 */
public class Trainer {
    
    private final Collection<File> trainFiles;
        
    public Trainer(Collection<File> trainFiles) {
        this.trainFiles = trainFiles;
    }
    
    /**
     * Writes labelled training data to use as input for CRF++ toolkit.
     * Writes to two separate training data files: 1) only with labels for
     * motion-signal spatial entity tokens; and 2) with labels for all
     * spatial entity tokens except motion-signals.
     * 
     * @param seOutput
     * @param msOutput
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException 
     */
    public void writeTypeLabelledCrfData(FileOutputStream seOutput, FileOutputStream msOutput) throws SAXException, ParserConfigurationException, IOException {
        int[] seOffsets;
        int[] msOffsets;

        for (File file : trainFiles) {            
            System.out.println("writing labelled CRF data for "+file.getPath());
            
            //parses the spatial entity annotations from input xml files
            Doc document = new Doc();
            Main.dp.parseXmlAnnotationsFile(file.toString(), document, true, false);
            //map which stores all spatial elements
            Map<Integer, SpatialElement> startOffsetSpatialElement = new HashMap<>(document.getStartOffsetSpatialElement()); 
            //map to store the sentence-wise nlp info of the text document
            //obtained from the Stanford CoreNLP parser
            Map<Integer, Map<Integer, CoreLabel>> sentenceStartOffsetParserLabel = StanfordParser.parse(document);
            
            SpatialElement se = null;
            
            String seFeatureStr = "";            
            String msFeatureStr = "";
                        
            for (int sentence : sentenceStartOffsetParserLabel.keySet()) {
                Map<Integer, CoreLabel> startOffsetParserLabel = sentenceStartOffsetParserLabel.get(sentence);
                seOffsets = new int[2];
                msOffsets = new int[2];
                
                for (int startOffset : startOffsetParserLabel.keySet()) {
                    
                    CoreLabel token = startOffsetParserLabel.get(startOffset);
                    // this is the text of the token
                    String word = token.word();
                    
                    //generate crf feature
                    CrfFeature crfFeat = new CrfFeature(word, token.lemma(), token.tag(), token.ner());
                    crfFeat.setUniFV();
                    String featureStr = crfFeat.toString();
                    
                    seFeatureStr += featureStr;
                    
                    se = startOffsetSpatialElement.containsKey(startOffset) ? startOffsetSpatialElement.get(startOffset) : 
                            startOffset >= seOffsets[1] ? SpatialElement.getSE(word, startOffset, startOffsetSpatialElement) : se;
                    boolean isMotionSignal = se == null ? false : se.isMotionSignal();
                    boolean isMotionSignalOnly = se == null ? false : se.isMotionSignalOnly();
                    
                    if (se == null || isMotionSignalOnly) 
                        seFeatureStr += " O\n";
                    else if (seOffsets[1] != se.getEnd()) {
                        seOffsets[0] = startOffset;
                        int difference = startOffset - se.getStart();
                        seOffsets[1] = se.getEnd() + difference;
                    }
                    
                    if (se != null && !isMotionSignalOnly) {
                        if (startOffset == seOffsets[0])
                            seFeatureStr += " B-"+se.getNonMotionSignalType()+"\n";
                        else if (startOffset < seOffsets[1])
                            seFeatureStr += " I-"+se.getNonMotionSignalType()+"\n";
                    }
                    
                    msFeatureStr += featureStr;
                                        
                    if (se == null || !isMotionSignal) 
                        msFeatureStr += " O\n";
                    else if (msOffsets[1] != se.getEnd()) {
                        msOffsets[0] = startOffset;
                        int difference = startOffset - se.getStart();
                        msOffsets[1] = se.getEnd() + difference;
                    }
                    
                    if (se != null && isMotionSignal) {
                        if (startOffset == msOffsets[0])
                            msFeatureStr += " B-MOTION_SIGNAL\n";
                        else if (startOffset < msOffsets[1])
                            msFeatureStr += " I-MOTION_SIGNAL\n";
                    }                    
                }
                seOutput.write((seFeatureStr+"\n").getBytes());
                seFeatureStr = "";
                msOutput.write((msFeatureStr+"\n").getBytes());
                msFeatureStr = "";
            }
        }
    }
    
    /**
     * Creates empty training files for CRF role extraction.
     * 
     * @param dataDir
     * @throws FileNotFoundException 
     */
    public void clearCrfRoleDataFiles(String dataDir, boolean train) throws FileNotFoundException {
        String ext = train ? "Train" : "Test";
        for (String role : SpatialRelation.ROLE_OVERLAP_ROLES_MAP.keySet()) {
            new FileOutputStream(dataDir+"\\"+role+ext+".txt");
            if (!role.equals("trigger_link"))
                new FileOutputStream(dataDir+"\\"+role+ext+"Other.txt");        
        }
    }
    
    /**
     * Writes labeled training data to use as input for CRF++ toolkit.
     * For each role, writes to two separate training data files: 
     * 1) with labels for the role and other roles it doesn't overlap with;
     * 2) with labels for the roles and it does and doesn't overlap with except itself.
     * 
     * @param dataDir
     * @param train
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException 
     */
    public void writeRoleLabelledCrfData(String dataDir, boolean train) throws SAXException, ParserConfigurationException, IOException {
        int[] roleOffsets;
        int[] otherOffsets;
        String ext = train ? "Train" : "Test";
        
        Set<String> allRolesSet = new HashSet<>(SpatialRelation.ROLE_OVERLAP_ROLES_MAP.keySet());        
        for (File file : trainFiles) {            
            System.out.println("writing labelled CRF data for "+file.getPath());
            
            //parses the spatial entity annotations from input xml files
            Doc document = new Doc();
            Main.dp.parseXmlAnnotationsFile(file.toString(), document, true, true);
            //map which stores all spatial elements
            Map<Integer, SpatialElement> startOffsetSpatialElement = new HashMap<>(document.getStartOffsetSpatialElement()); 
            //map to store the sentence-wise nlp info of the text document
            //obtained from the Stanford CoreNLP parser
            Map<Integer, Map<Integer, CoreLabel>> sentenceStartOffsetParserLabel = StanfordParser.parse(document);

            for (String role : SpatialRelation.ROLE_OVERLAP_ROLES_MAP.keySet()) {
                
                Set<String> overlapRoles = new HashSet<>(SpatialRelation.ROLE_OVERLAP_ROLES_MAP.get(role));
                Set<String> nonOverlapRoles = new HashSet<>(allRolesSet);
                nonOverlapRoles.removeAll(overlapRoles);
                nonOverlapRoles.remove(role);
                                
                FileOutputStream output = new FileOutputStream(dataDir+"\\"+role+ext+".txt", true);
                FileOutputStream outputOther = overlapRoles.contains("null") ? null : new FileOutputStream(dataDir+"\\"+role+ext+"Other.txt", true);
                
                SpatialElement se = null;

                String roleFeatureStr = "";            
                String otherFeatureStr = "";
                
                List<Integer> nonOverlapRolesStartOffsets = document.getElementRolesStartOffsets(nonOverlapRoles); 
                List<Integer> roleStartOffsets = !document.getElementRoleStartOffsets().containsKey(role) ? 
                        new ArrayList<>() : document.getElementRoleStartOffsets().get(role);
                roleStartOffsets.addAll(nonOverlapRolesStartOffsets);
                List<Integer> overlapRolesStartOffsets = outputOther != null ? document.getElementRolesStartOffsets(overlapRoles) : null;
                if (overlapRolesStartOffsets != null)
                    overlapRolesStartOffsets.addAll(nonOverlapRolesStartOffsets);
                
                for (int sentence : sentenceStartOffsetParserLabel.keySet()) {
                    Map<Integer, CoreLabel> startOffsetParserLabel = sentenceStartOffsetParserLabel.get(sentence);
                    roleOffsets = new int[2];
                    otherOffsets = new int[2];

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
                        String featureStr = crfFeat.toString();

                        roleFeatureStr += featureStr;                        
                        
                        String roleLabel = se == null ? "O" : se.getRole(role, nonOverlapRoles);
                                                
                        if (se == null || roleLabel.equals("O")) 
                            roleFeatureStr += " O\n";
                        else if (roleOffsets[1] != se.getEnd()) {
                            roleOffsets[0] = startOffset;
                            int difference = startOffset - se.getStart();
                            roleOffsets[1] = se.getEnd() + difference;
                        }

                        if (!roleLabel.equals("O")) {
                            if (startOffset == roleOffsets[0])
                                roleFeatureStr += " B-"+roleLabel+"\n";
                            else if (startOffset < roleOffsets[1])
                                roleFeatureStr += " I-"+roleLabel+"\n";
                        }

                        if (outputOther == null)
                            continue;
                        
                        otherFeatureStr += featureStr;
                        roleLabel = se == null ? "O" : se.getRole(overlapRoles, nonOverlapRoles);
                        
                        if (se == null || roleLabel.equals("O")) 
                            otherFeatureStr += " O\n";
                        else if (otherOffsets[1] != se.getEnd()) {
                            otherOffsets[0] = startOffset;
                            int difference = startOffset - se.getStart();
                            otherOffsets[1] = se.getEnd() + difference;
                        }

                        if (!roleLabel.equals("O")) {
                            if (startOffset == otherOffsets[0])
                                otherFeatureStr += " B-"+roleLabel+"\n";
                            else if (startOffset < otherOffsets[1])
                                otherFeatureStr += " I-"+roleLabel+"\n";
                        }                    
                    }
                    output.write((roleFeatureStr+"\n").getBytes());
                    roleFeatureStr = "";
                    if (outputOther != null) {
                        outputOther.write((otherFeatureStr+"\n").getBytes());
                        otherFeatureStr = "";
                    }
                }
            }
        }
    }    
    
}
