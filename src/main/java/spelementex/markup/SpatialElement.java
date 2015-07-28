/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.spelementex.markup;

import edu.stanford.nlp.ling.CoreLabel;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static main.java.spelementex.markup.SpatialRelation.ROLE_VALIDTYPES_MAP;
import org.w3c.dom.Element;

/**
 *
 * @author Jenny D'Souza
 */
public class SpatialElement {
    
    public final static List<String> TYPES = Arrays.asList("PLACE", "PATH", "SPATIAL_ENTITY", 
            "NONMOTION_EVENT", "MOTION", "SPATIAL_SIGNAL", "MOTION_SIGNAL");
    
    Element spatialEntityEl;
    List<String> type = new ArrayList<>();   
    String semantic_type;
    
    String id;
    int start;
    int end;
    String text;
    
    List<String> role = new ArrayList<>();
    
    public SpatialElement(String id, String type, String semantic_type, int start, int end, String text) {
        this.id = id;
        if (!this.type.contains(type))
            this.type.add(type);
        this.semantic_type = semantic_type;
        this.start = start;
        this.end = end;
        this.text = text;
    }
    
    public SpatialElement(Element spatialEntityEl, String type) {
        this.spatialEntityEl = spatialEntityEl;
        if (!this.type.contains(type))
            this.type.add(type);
    }
        
    public void processSpatialElement() throws UnsupportedEncodingException {
        this.start = Integer.parseInt(spatialEntityEl.getAttribute("start"));
        this.end = Integer.parseInt(spatialEntityEl.getAttribute("end"));
        this.id = spatialEntityEl.getAttribute("id");
        
        this.text = new String(spatialEntityEl.getAttribute("text").getBytes("UTF-8"), "UTF-8");        
                
        if (this.type.contains("SPATIAL_SIGNAL")) {
            this.type.remove("SPATIAL_SIGNAL");
            this.type.add("SPATIAL_SIGNAL-"+spatialEntityEl.getAttribute("semantic_type"));
        }
    }
    
    public List<String> getType() {
        return this.type;
    }
    
    public String getNonMotionSignalType() {
        for (String t : this.type) {
            if (!t.equals("MOTION_SIGNAL"))
                return t;
        }        
        return "";
    }
    
    public boolean isMotionSignal() {
        return this.type.contains("MOTION_SIGNAL");
    }
    
    public boolean isMotionSignalOnly() {
        return this.type.size() == 1 && this.type.contains("MOTION_SIGNAL");
    }
    
    public String getId() {
        return id;
    }    
    
    public int getStart() {
        return start;
    }
    
    public int getEnd() {
        return end;
    }        
    
    public String getText() {        
        return text;
    }
     
    public void setRole(String role) {
        if (!this.role.contains(role))
            this.role.add(role);
    }    
    
    public String getRole(String role, Set<String> moreRoles) {
        if (this.role.contains(role))
            return role;
        List<String> commonRoles = new ArrayList<>(moreRoles);
        commonRoles.retainAll(this.role);
        return commonRoles.isEmpty() ? "O" : commonRoles.get(0);
    }
    
    public String getRole(Set<String> roles, Set<String> moreRoles) {
        List<String> commonRoles = new ArrayList<>(roles);
        commonRoles.retainAll(this.role);
        if (!commonRoles.isEmpty())
            return commonRoles.get(0);
        commonRoles = new ArrayList<>(moreRoles);
        commonRoles.retainAll(this.role);
        return commonRoles.isEmpty() ? "O" : commonRoles.get(0);        
    }
    
    public static String getSEId(String role, int index, String[] result, int tokenNum, 
            Map<Integer, Integer> tokenNumStartOffset, Map<Integer, SpatialElement> startOffsetSpatialElement) {
        int startOffset = tokenNumStartOffset.get(tokenNum);
        
        if (startOffsetSpatialElement.containsKey(startOffset)) {
            SpatialElement se = startOffsetSpatialElement.get(startOffset);
            List<String> validTypes = new ArrayList<>(se.type);
            validTypes.retainAll(ROLE_VALIDTYPES_MAP.get(role));
            return validTypes.isEmpty() ? "" : se.id;
        }
        
        String[] resultTokens = result[index].trim().split("\\s+");
        String type = resultTokens[resultTokens.length-2];
        if (type.equals("O") || index-1 < 0)
            return "";

        index--;    
        tokenNum--;
        if (index == 0)
            return "";
        resultTokens = result[index-1].trim().split("\\s+");
        if (resultTokens.length <= 3 || !resultTokens[resultTokens.length-2].equals(type))
            return "";
        
        while (index >= 0) {
            resultTokens = result[index].split("\\s+");
            if (resultTokens.length <= 3 || !resultTokens[resultTokens.length-2].equals(type))
                break;
            index--;
            tokenNum--;
        }
        
        tokenNum++;
        startOffset = tokenNumStartOffset.get(tokenNum);

        if (startOffsetSpatialElement.containsKey(startOffset)) {
            SpatialElement se = startOffsetSpatialElement.get(startOffset);
            List<String> validTypes = new ArrayList<>(se.type);
            validTypes.retainAll(ROLE_VALIDTYPES_MAP.get(role));
            return validTypes.isEmpty() ? "" : se.id;
        }      
        
        return "";
    }
    
    public static SpatialElement getSE(String word, int startOffset, Map<Integer, SpatialElement> startOffsetSpatialElement) {
        int tempParserOffset = startOffset-1;
        while (tempParserOffset > 0 && tempParserOffset > startOffset-5) {
            if (startOffsetSpatialElement.containsKey(tempParserOffset)) {
                
                SpatialElement se = startOffsetSpatialElement.get(tempParserOffset);
                String seWord = se.text.split("\\s+")[0];
                
                if ((word.length() > 1 && seWord.matches("[^a-zA-Z0-9]{0,2}"+word+"[^a-zA-Z0-9]{0,2}")) || 
                        word.matches("[^a-zA-Z0-9]{0,2}"+seWord+"[^a-zA-Z0-9]{0,2}") || word.equals(seWord)) {                    
                    return se;                
                }
            }
            tempParserOffset--;
        }
        tempParserOffset = startOffset+1;
        while (tempParserOffset < startOffset+5) {
            if (startOffsetSpatialElement.containsKey(tempParserOffset)) {

                SpatialElement se = startOffsetSpatialElement.get(tempParserOffset);
                String seWord = se.text.split("\\s+")[0];
                
                if ((word.length() > 1 && seWord.matches("[^a-zA-Z0-9]{0,2}"+word+"[^a-zA-Z0-9]{0,2}")) || 
                        word.matches("[^a-zA-Z0-9]{0,2}"+seWord+"[^a-zA-Z0-9]{0,2}") || word.equals(seWord)) {                    
                    return se;
                }
                
            }
            tempParserOffset++;
        }
        return null;
    }
        
    public static SpatialElement setSE(String id, String predictedType,
            int[] tokenOffsets, Map<Integer, Integer> tokenStartOffsetInfo, Map<Integer, CoreLabel> startOffsetParseLabel,
            String full_text) {
        int start = tokenStartOffsetInfo.get(tokenOffsets[0]);
        int end = startOffsetParseLabel.get(tokenStartOffsetInfo.get(tokenOffsets[1])).endPosition();
        
        String[] predictedTypeTokens = predictedType.split("\\-");
        String type = predictedTypeTokens[0];
        String semantic_type = predictedTypeTokens.length == 2 ? predictedTypeTokens[1] : "";
        
        return new SpatialElement(id, type, semantic_type, start, end, full_text.substring(start, end));
    }
    
}
