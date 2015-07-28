/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.spelementex.markup;

import edu.stanford.nlp.ling.CoreLabel;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import main.java.parsers.DomParser;
import main.java.spelementex.Annotator;

/**
 *
 * @author Jenny D'Souza
 */
public class Doc {
    
    Map<Integer, SpatialElement> startOffsetSpatialElement;
    Map<String, Integer> elementIdStartOffset;
    
    Map<String, List<Integer>> elementRoleStartOffsets;
    
    String text;
    
    Map<String, SpatialElement> spatialElementAnnotations = new HashMap<>();
    Map<String, Integer> idTypePrefixMaxInt = new HashMap<>();
    
    Map<String, Set<String>> spatialRoleAnnotations = new HashMap<>();
    
    public Doc() {
        startOffsetSpatialElement = new HashMap<>();
        elementIdStartOffset = new HashMap<>();
        elementRoleStartOffsets = new HashMap<>();
    }
    
    public void addDocSpatialElement(SpatialElement se) {
        startOffsetSpatialElement.put(se.start, se);
        elementIdStartOffset.put(se.id, se.start);
    }
    
    public void addDocSpatialElementRole(String role, int startOffset) {
        List<Integer> startOffsets = elementRoleStartOffsets.get(role);
        if (startOffsets == null)
            elementRoleStartOffsets.put(role, startOffsets = new ArrayList<>());
        if (!startOffsets.contains(startOffset))
            startOffsets.add(startOffset);
    }
    
    public void setText(String text) throws UnsupportedEncodingException {        
        this.text = text;
    }

    public String getText() {
        return text;
    }    
    
    public Map<Integer, SpatialElement> getStartOffsetSpatialElement() {
        return startOffsetSpatialElement;
    }
          
    public Map<String, Integer> getElementIdStartOffsetMap() {
        return elementIdStartOffset;
    }
    
    public Map<String, List<Integer>> getElementRoleStartOffsets() {
        return elementRoleStartOffsets;
    }
    
    public List<Integer> getElementRolesStartOffsets(Set<String> roles) {
        List<Integer> startOffsets = new ArrayList<>();
        for (String role : roles) {
            if (!elementRoleStartOffsets.containsKey(role))
                continue;
            startOffsets.addAll(elementRoleStartOffsets.get(role));
        }
        return startOffsets;
    }
    
    /**
     * from predictions, extract spatial roles
     * 
     * @param tokenStartOffsetInfo
     * @param startOffsetParserLabel
     * @param role
     * @param beginIndex
     * @param resultFileLength
     * @param result
     * @return 
     */
    public int setDocumentSpatialRoleAnnotations(Map<Integer, Integer> tokenStartOffsetInfo, 
            Map<Integer, CoreLabel> startOffsetParserLabel, String role,
            int beginIndex, int resultFileLength, String[] result) {
        Set<Integer> predictedTokens = new HashSet<>();
        boolean first = true;
        int tokenNum = 1;
        
        for (int i = beginIndex; i < resultFileLength; i++) {
            String line = result[i].trim();
            if (line.equals(""))
                continue;

            String[] lineTokens = line.split("\\s+");

            if (lineTokens.length == 3) {
                tokenNum = 1;
                //# 0 0.076469
                //check to move to next sentence
                if (lineTokens[1].equals("0") && !first) {
                    beginIndex = i;
                    break;
                }
                first = false;
                continue;
            }            

            //get predicted role
            String predictedRole = lineTokens[lineTokens.length-1].matches("B\\-.*") ? Annotator.getPredictedRole(lineTokens[lineTokens.length-1], role) : "";
            if (predictedRole.equals("")) {
                tokenNum++;
                continue;
            }            
            
            //if the predicted role is for a special character
            if (lineTokens[0].matches("[^a-zA-Z0-9]+") || lineTokens[0].equals("-lrb-") || lineTokens[0].equals("-rrb-")) {
                if (i+1 == resultFileLength) {
                    tokenNum++; 
                    continue;
                }
                line = result[i+1].trim();
                lineTokens = line.split("\\s+");                    
                if (line.equals("") || lineTokens.length == 3 || lineTokens[lineTokens.length-1].equals("O") || 
                        lineTokens[lineTokens.length-1].matches("B\\-.*")) {
                    tokenNum++; 
                    continue;
                }
            }                

            int[] tokenOffsets = Annotator.getTokenOffset(result, i, tokenNum);

            Set<Integer> tempPredictedTokens = new HashSet<>();
            for (int j = tokenOffsets[0]; j <= tokenOffsets[1]; j++) 
                tempPredictedTokens.add(j);

            int before = tempPredictedTokens.size();
            tempPredictedTokens.removeAll(predictedTokens);
            int after = tempPredictedTokens.size();
            if (before == after) {
                String id = SpatialElement.getSEId(predictedRole, i, result, tokenNum, tokenStartOffsetInfo, this.startOffsetSpatialElement);                
                if (!id.equals("")) {
                    Set<String> annotatedIDs = spatialRoleAnnotations.get(predictedRole);
                    if (annotatedIDs == null)
                        spatialRoleAnnotations.put(predictedRole, annotatedIDs = new HashSet<>());
                    annotatedIDs.add(id);
                    predictedTokens.addAll(tempPredictedTokens);
                }
            }

            tokenNum++;
        }      
        return beginIndex;
    }    
    
    /**
     * from predictions, extract spatial entities
     * 
     * @param tokenStartOffsetInfo
     * @param startOffsetParserLabel
     * @param beginIndex
     * @param resultFileLength
     * @param result
     * @return 
     */
    public int setDocumentSpatialElementAnnotations(Map<Integer, Integer> tokenStartOffsetInfo, 
            Map<Integer, CoreLabel> startOffsetParserLabel, 
            int beginIndex, int resultFileLength, String[] result) {
        Set<Integer> predictedTokens = new HashSet<>();
        boolean first = true;
        int tokenNum = 1;
        
        for (int i = beginIndex; i < resultFileLength; i++) {
            String line = result[i].trim();
            if (line.equals(""))
                continue;

            String[] lineTokens = line.split("\\s+");

            if (lineTokens.length == 3) {
                tokenNum = 1;
                //# 0 0.076469
                //check to move to next sentence
                if (lineTokens[1].equals("0") && !first) {
                    beginIndex = i;
                    break;
                }
                first = false;
                continue;
            }            

            String predictedTag = lineTokens[lineTokens.length-1];
            if (predictedTag.matches("B\\-.*") && !predictedTokens.contains(tokenNum)) {
                if (lineTokens[0].matches("[^a-zA-Z0-9]+") || lineTokens[0].equals("-lrb-") || lineTokens[0].equals("-rrb-")) {
                    if (i+1 == resultFileLength) {
                        tokenNum++; 
                        continue;
                    }
                    line = result[i+1].trim();
                    lineTokens = line.split("\\s+");                    
                    if (line.equals("") || lineTokens.length == 3 || lineTokens[lineTokens.length-1].equals("O") || 
                            lineTokens[lineTokens.length-1].matches("B\\-.*")) {
                        tokenNum++; 
                        continue;
                    }
                }                
                String predictedType = Annotator.getPredictedTagType(predictedTag);
                int[] tokenOffsets = Annotator.getTokenOffset(result, i, tokenNum);
                
                Set<Integer> tempPredictedTokens = new HashSet<>();
                for (int j = tokenOffsets[0]; j <= tokenOffsets[1]; j++) 
                    tempPredictedTokens.add(j);

                int before = tempPredictedTokens.size();
                tempPredictedTokens.removeAll(predictedTokens);
                int after = tempPredictedTokens.size();
                if (before == after) {
                    String idPrefix = predictedType.contains("SPATIAL_SIGNAL") ? 
                            Annotator.TYPE_PREFIX_MAP.get("SPATIAL_SIGNAL") : 
                            Annotator.TYPE_PREFIX_MAP.get(predictedType);
                    int id = idTypePrefixMaxInt.containsKey(idPrefix) ? idTypePrefixMaxInt.get(idPrefix) : 1;


                    predictedTokens.addAll(tempPredictedTokens);
                    spatialElementAnnotations.put(idPrefix+id, SpatialElement.setSE(idPrefix+id, predictedType, tokenOffsets, 
                            tokenStartOffsetInfo, startOffsetParserLabel, text));                        

                    //update to next id value
                    id++;
                    idTypePrefixMaxInt.put(idPrefix, id);
                }
            }

            tokenNum++;
        }      
        return beginIndex;
    }
    
    public void writeAnnotations(String outputFileName) throws IOException {
                
        String outputStr = "<SpaceEvalTaskv1.2>\n";
        outputStr += "<TEXT>";
        outputStr += text;
        outputStr += "</TEXT>\n<TAGS>\n";
        for (String id : spatialElementAnnotations.keySet()) {
            SpatialElement se = spatialElementAnnotations.get(id);
            for (String type : se.type) {
                outputStr += "<"+type+" id=\""+se.id+"\" text=\""+se.text+"\" "
                        + "start=\""+se.start+"\" end=\""+se.end+"\" "+
                        (!type.equals("SPATIAL_SIGNAL") ? "/>" :
                        (!se.semantic_type.equals("") ? "semantic_type=\""+se.semantic_type+"\" />" : "semantic_type=\"TOPOLOGICAL\" />"));
                outputStr += "\n";
            }
        }
        outputStr += "</TAGS>\n";
        outputStr += "</SpaceEvalTaskv1.2>";
        FileOutputStream output = new FileOutputStream(outputFileName);
        output.write(DomParser.prettyFormat(outputStr).getBytes());
    }
    
    public Map<String, SpatialElement> getSpatialElementAnnotations() {
        return spatialElementAnnotations;
    }
    
    public void writeRoleAnnotations(String outputFileName) throws IOException {
        String outputStr = "<SpaceEvalTaskv1.2>\n";
        outputStr += "<TEXT>";
        outputStr += text;
        outputStr += "</TEXT>\n<TAGS>\n";    
        for (int startOffset : startOffsetSpatialElement.keySet()) {
            SpatialElement se = startOffsetSpatialElement.get(startOffset);
            for (String type : se.type) {
                String semantic_type = !type.contains("SPATIAL_SIGNAL") ? "" : type.split("\\-").length == 2 ? "semantic_type=\""+type.split("\\-")[1]+"\" " : "semantic_type=\"TOPOLOGICAL\" ";
                type = type.contains("SPATIAL_SIGNAL") ? "SPATIAL_SIGNAL" : type;                
                outputStr += "<"+type+" id=\""+se.id+"\" text=\""+se.text+"\" "
                        + "start=\""+se.start+"\" end=\""+se.end+"\" "+semantic_type+"/>\n";       
            }
        }
        for (String role : spatialRoleAnnotations.keySet()) {
            String annotationTag = SpatialRelation.ROLE_ANNOTATION_MAP.get(role);
            Set<String> ids = spatialRoleAnnotations.get(role);
            for (String id : ids)
                outputStr += annotationTag+id+"\" />\n";
        }
        outputStr += "</TAGS>\n";
        outputStr += "</SpaceEvalTaskv1.2>";
        
        FileOutputStream output = new FileOutputStream(outputFileName);        
        output.write(DomParser.prettyFormat(outputStr).getBytes());        
    }
    
}
