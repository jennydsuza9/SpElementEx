/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.spelementex.markup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Element;

/**
 *
 * @author Jenny D'Souza
 */
public class SpatialRelation {
    
    public final static List<String> TYPES = Arrays.asList("QSLINK", "OLINK", "MOVELINK");
        
    public static final Map<String, List<String>> ROLE_VALIDTYPES_MAP;
    static {
        Map<String, List<String>> aMap = new HashMap<>();
        aMap.put("trajector", Arrays.asList("PLACE", "PATH", "SPATIAL_ENTITY", "NONMOTION_EVENT", "MOTION"));
        aMap.put("landmark_link", Arrays.asList("PLACE", "PATH", "SPATIAL_ENTITY", "NONMOTION_EVENT", "MOTION"));
        aMap.put("trigger_link", Arrays.asList("SPATIAL_SIGNAL-TOPOLOGICAL", "SPATIAL_SIGNAL-DIRECTIONAL", "SPATIAL_SIGNAL-DIR_TOP"));
        aMap.put("trigger_movelink", Arrays.asList("MOTION"));
        aMap.put("mover", Arrays.asList("PLACE", "PATH", "SPATIAL_ENTITY", "NONMOTION_EVENT"));
        aMap.put("source", Arrays.asList("PLACE", "PATH", "SPATIAL_ENTITY", "NONMOTION_EVENT"));
        aMap.put("midPoint", Arrays.asList("PLACE", "PATH", "SPATIAL_ENTITY"));
        aMap.put("goal", Arrays.asList("PLACE", "PATH", "SPATIAL_ENTITY", "NONMOTION_EVENT"));
        aMap.put("landmark_movelink", Arrays.asList("PLACE", "PATH", "SPATIAL_ENTITY", "NONMOTION_EVENT"));
        aMap.put("path", Arrays.asList("PATH"));
        aMap.put("motion_signal", Arrays.asList("MOTION_SIGNAL"));        
        ROLE_VALIDTYPES_MAP = Collections.unmodifiableMap(aMap);
    }
    
    public static final Map<String, List<String>> ROLE_OVERLAP_ROLES_MAP;
    static {
        Map<String, List<String>> aMap = new HashMap<>();
        aMap.put("trajector", Arrays.asList("path","landmark_movelink","goal","mover","midPoint","landmark_link","source","trigger_movelink"));                
        aMap.put("landmark_link", Arrays.asList("path","landmark_movelink","goal","mover","trajector","midPoint","source","trigger_movelink"));        
        aMap.put("trigger_link", Arrays.asList("null"));        
        aMap.put("trigger_movelink", Arrays.asList("mover","trajector","landmark_link"));        
        aMap.put("mover", Arrays.asList("path","landmark_movelink","motion_signal","goal","trajector","midPoint","landmark_link","trigger_movelink"));        
        aMap.put("source", Arrays.asList("path","goal","trajector","midPoint","landmark_link"));
        aMap.put("midPoint", Arrays.asList("mover","trajector","landmark_link","source"));
        aMap.put("goal", Arrays.asList("landmark_movelink","mover","trajector","landmark_link","source"));    
        aMap.put("landmark_movelink", Arrays.asList("goal","mover","trajector","landmark_link"));        
        aMap.put("path", Arrays.asList("mover","trajector","landmark_link","source"));
        aMap.put("motion_signal", Arrays.asList("mover"));
        ROLE_OVERLAP_ROLES_MAP = Collections.unmodifiableMap(aMap);        
    }    
 
    public static final Map<String, String> ROLE_ANNOTATION_MAP;
    static {
        Map<String, String> aMap = new HashMap<>();
        aMap.put("trajector", "<QSLINK trajector=\"");
        aMap.put("landmark_link", "<QSLINK landmark=\"");
        aMap.put("trigger_link", "<QSLINK trigger=\"");
        aMap.put("trigger_movelink", "<MOVELINK trigger=\"");
        aMap.put("mover", "<MOVELINK mover=\"");
        aMap.put("source", "<MOVELINK source=\"");
        aMap.put("midPoint", "<MOVELINK midPoint=\"");
        aMap.put("goal", "<MOVELINK goal=\"");
        aMap.put("landmark_movelink", "<MOVELINK landmark=\"");
        aMap.put("path", "<MOVELINK pathID=\"");
        aMap.put("motion_signal", "<MOVELINK motion_signalID=\"");
        ROLE_ANNOTATION_MAP = Collections.unmodifiableMap(aMap);
    }    
    
    Element spatialRelationEl;
    String type;
    String trigger;
    String landmark;
    String trajector;
    
    //trigger
    String mover;
    String source;
    List<String> midPoint;
    String goal;
    //landmark
    List<String> path;
    List<String> motion_signal;
    
    public SpatialRelation(Element spatialRelationEl, String type) {
        this.spatialRelationEl = spatialRelationEl;
        this.type = type;
    }
    
    public void processLink(Doc document) {
        this.trigger = spatialRelationEl.getAttribute("trigger");
        this.landmark = spatialRelationEl.getAttribute("landmark");
        this.trajector = spatialRelationEl.getAttribute("trajector");
        
        SpatialElement triggerSE = !document.elementIdStartOffset.containsKey(this.trigger) ? null :
                document.startOffsetSpatialElement.get(document.elementIdStartOffset.get(this.trigger));
        SpatialElement landmarkSE = !document.elementIdStartOffset.containsKey(this.landmark) ? null :
                document.startOffsetSpatialElement.get(document.elementIdStartOffset.get(this.landmark));
        SpatialElement trajectorSE = !document.elementIdStartOffset.containsKey(this.trajector) ? null :
                document.startOffsetSpatialElement.get(document.elementIdStartOffset.get(this.trajector));
        
        if ((triggerSE != null && triggerSE.start == -1) || (landmarkSE != null && landmarkSE.start == -1) ||
                (trajectorSE != null && trajectorSE.start == -1))
            return;
        
        if (triggerSE != null) {
            triggerSE.setRole("trigger_link");
            document.addDocSpatialElementRole("trigger_link", triggerSE.start);
        }
        if (landmarkSE != null) {
            landmarkSE.setRole("landmark_link");
            document.addDocSpatialElementRole("landmark_link", landmarkSE.start);
        }
        if (trajectorSE != null) {
            trajectorSE.setRole("trajector");
            document.addDocSpatialElementRole("trajector", trajectorSE.start);
        }
    }
        
    public List<String> setList(String attributeStr, List<String> attributeValuesList) {
        attributeValuesList = new ArrayList<>();
        if (!attributeStr.equals("")) {
            String[] attributeValues = attributeStr.replaceAll(", ", ",").split(",");
            for (String attributeValue : attributeValues)
                attributeValuesList.add(attributeValue);
        }
        return attributeValuesList;
    }
    
    public List<SpatialElement> getSpatialElements(Doc document, List<String> roleIDs) {
        List<SpatialElement> seList = null;
        for (String roleID : roleIDs) {
            if (!document.elementIdStartOffset.containsKey(roleID)) 
                continue;
            SpatialElement se = document.startOffsetSpatialElement.get(document.elementIdStartOffset.get(roleID));
            if (se.start == -1)
                continue;
            if (seList == null)
                seList = new ArrayList<>();
            seList.add(se);                
        }
        
        return seList;
    }
    
    public void processMovelink(Doc document) {
        this.trigger = spatialRelationEl.getAttribute("trigger");
        this.mover = spatialRelationEl.getAttribute("mover");
        this.source = spatialRelationEl.getAttribute("source");
        this.midPoint = setList(spatialRelationEl.getAttribute("midPoint"), this.midPoint);
        this.goal = spatialRelationEl.getAttribute("goal");
        this.landmark = spatialRelationEl.getAttribute("landmark");
        this.path = setList(spatialRelationEl.getAttribute("pathID"), this.path);
        this.motion_signal = setList(spatialRelationEl.getAttribute("motion_signalID"), this.motion_signal);
        
        if (this.motion_signal.contains(",")) {
            System.out.println(this.motion_signal);
            System.exit(1);
        }
        
        SpatialElement triggerSE = !document.elementIdStartOffset.containsKey(this.trigger) ? null :
                document.startOffsetSpatialElement.get(document.elementIdStartOffset.get(this.trigger));
        SpatialElement moverSE = !document.elementIdStartOffset.containsKey(this.mover) ? null :
                document.startOffsetSpatialElement.get(document.elementIdStartOffset.get(this.mover));
        SpatialElement sourceSE = !document.elementIdStartOffset.containsKey(this.source) ? null :
                document.startOffsetSpatialElement.get(document.elementIdStartOffset.get(this.source));
        List<SpatialElement> midPointSE = getSpatialElements(document, this.midPoint);        
        SpatialElement goalSE = !document.elementIdStartOffset.containsKey(this.goal) ? null :
                document.startOffsetSpatialElement.get(document.elementIdStartOffset.get(this.goal));        
        SpatialElement landmarkSE = !document.elementIdStartOffset.containsKey(this.landmark) ? null :
                document.startOffsetSpatialElement.get(document.elementIdStartOffset.get(this.landmark));
        List<SpatialElement> pathSE = getSpatialElements(document, this.path);
        List<SpatialElement> motion_signalSE = getSpatialElements(document, this.motion_signal);
        
        if ((triggerSE != null && triggerSE.start == -1) || (moverSE != null && moverSE.start == -1) ||
                (sourceSE != null && sourceSE.start == -1) || (goalSE != null && goalSE.start == -1) || 
                (landmarkSE != null && landmarkSE.start == -1))
            return;
        
        if (triggerSE != null) {
            triggerSE.setRole("trigger_movelink");
            document.addDocSpatialElementRole("trigger_movelink", triggerSE.start);
        }
        if (moverSE != null) {
            moverSE.setRole("mover");
            document.addDocSpatialElementRole("mover", moverSE.start);
        }
        if (sourceSE != null) {
            sourceSE.setRole("source");
            document.addDocSpatialElementRole("source", sourceSE.start);
        }
        if (midPointSE != null) {
            for (SpatialElement se : midPointSE) {
                se.setRole("midPoint");
                document.addDocSpatialElementRole("midPoint", se.start);
            }
        }
        if (goalSE != null) {
            goalSE.setRole("goal");
            document.addDocSpatialElementRole("goal", goalSE.start);
        }
        if (landmarkSE != null) {
            landmarkSE.setRole("landmark_movelink");
            document.addDocSpatialElementRole("landmark_movelink", landmarkSE.start);
        }
        if (pathSE != null) {
            for (SpatialElement se : pathSE) {
                se.setRole("path");
                document.addDocSpatialElementRole("path", se.start);
            }
        }
        if (motion_signalSE != null) {
            for (SpatialElement se : motion_signalSE) {
                se.setRole("motion_signal");
                document.addDocSpatialElementRole("motion_signal", se.start);
            }
        }
    }
    
    public void processSpatialRelation(Doc document) {
        if (type.equals("QSLINK") || type.equals("OLINK"))
            processLink(document);
        else
            processMovelink(document);
    }
    
}
