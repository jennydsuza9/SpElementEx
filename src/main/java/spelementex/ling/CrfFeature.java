/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.spelementex.ling;

import java.util.List;

/**
 *
 * @author Jenny D'Souza
 */
public class CrfFeature {
    
    public static final String NV = "__nil__";    
    
    private final String word;
    private final String lemma;
    private final String pos;
    private final String ner;
    
    private String prefix1;
    private String prefix2;
    private String prefix3; 
    private String suffix1;
    private String suffix2;
    private String suffix3; 
    private String wordShape;
    private String wordShapeShort;     
    private int allCaps;
    private int initCaps;
    private int initCapsAlpha;
    private int capsMix;
    private int hasDigit;
    private int singleDigit;
    private int doubleDigit;
    private int naturalNumber;
    private int realNumber;
    private int hasDash;
    private int initDash;
    private int endDash;
    private int alphaNumeric1;
    private int alphaNumeric2;      
    
    private String type = "";
    
    public CrfFeature (String word, String lemma, String pos, String ner) {
        this.word = word;
        this.lemma = lemma;
        this.pos = pos;
        this.ner = ner;
    }      
    
    public void setUniFV() {
        prefix1 = setPrefix(word, 1);
        prefix2 = setPrefix(word, 2);
        prefix3 = setPrefix(word, 3);
        suffix1 = setSuffix(word, 1);
        suffix2 = setSuffix(word, 2);
        suffix3 = setSuffix(word, 3);   
        wordShape = setWordShape(word);
        wordShapeShort = setWordShapeShort(word);   
        allCaps = setAllCaps(word);
        initCaps = setInitCaps(word);
        initCapsAlpha = setInitCapsAlpha(word);
        capsMix = setCapsMix(word);
        hasDigit = setHasDigit(word);
        singleDigit = setSingleDigit(word);
        doubleDigit = setDoubleDigit(word);
        naturalNumber = setNaturalNumber(word);
        realNumber = setRealNumber(word);
        hasDash = setHasDash(word);
        initDash = setInitDash(word);
        endDash = setEndDash(word);
        alphaNumeric1 = setAlphaNumeric1(word);
        alphaNumeric2 = setAlphaNumeric2(word);            
    }    
    
    public void setType() {
        type = "O";
    }
    
    public void setType(String nonMotionSignalType, boolean isMotionSignal, boolean isMotionSignalOnly) {
        if (!isMotionSignal)
            type = nonMotionSignalType;
        else if (isMotionSignalOnly)
            type = "MOTION_SIGNAL";
        else 
            type = nonMotionSignalType+"-MOTION_SIGNAL";
        
    }
    
    public static String setPrefix(String str, int len) {
        if (str.length() < len) {
            return NV;
        }
        return str.substring(0, len);
    }    
    
    public static String setSuffix(String str, int len) {
        if (str.length() < len) {
            return NV;
        }
        return str.substring(str.length() - len);
    }      
    
   public static String setWordShape (String wc) {
        wc = wc.replaceAll("[A-Z]", "A");
 	wc = wc.replaceAll("[a-z]", "a");
 	wc = wc.replaceAll("[0-9]", "0");
 	wc = wc.replaceAll("[^A-Za-z0-9]", "x");
        return wc;
    }

    public static String setWordShapeShort (String bwc) {
        bwc = bwc.replaceAll("[A-Z]+", "A");
 	bwc = bwc.replaceAll("[a-z]+", "a");
 	bwc = bwc.replaceAll("[0-9]+", "0");
 	bwc = bwc.replaceAll("[^A-Za-z0-9]+", "x");
        return bwc;
    }       
    
    public static int setInitCaps(String str) {    
        if (str.matches("[A-Z].*")) return 1;
        return 0;
    }

    public static int setInitCapsAlpha(String str) {
        if (str.matches("[A-Z][a-z].*")) return 1;
        return 0;
    }    
    
    public static int setAllCaps(String str) {
        if (str.matches("[A-Z]+")) return 1;
        return 0;
    }

    public static int setCapsMix(String str) {
        if (str.matches("[A-Za-z]+")) return 1;
        return 0;
    }

    public static int setHasDigit(String str) {
        if (str.matches(".*[0-9].*")) return 1;
        return 0;
    }

    public static int setSingleDigit(String str) {
        if (str.matches("[0-9]")) return 1;
        return 0;
    }

    public static int setDoubleDigit(String str) {
        if (str.matches("[0-9][0-9]")) return 1;
        return 0;
    }

    public static int setNaturalNumber(String str) {
        if (str.matches("[0-9]+")) return 1;
        return 0;
    }

    public static int setRealNumber(String str) {
        if (str.matches("[-0-9]+[.,]+[0-9.,]+")) return 1;
        return 0;
    }

    public static int setHasDash(String str) {
        if (str.matches(".*-.*")) return 1;
        return 0;
    }

    public static int setInitDash(String str) {
        if (str.matches("-.*")) return 1;
        return 0;
    }

    public static int setEndDash(String str) {
        if (str.matches(".*-")) return 1;
        return 0;
    }

    public static int setAlphaNumeric1(String str) {
        if (str.matches(".*[A-Za-z].*[0-9].*")) return 1;
        return 0;
    }

    public static int setAlphaNumeric2(String str) {
        if (str.matches(".*[0-9].*[A-Za-z].*")) return 1;
        return 0;
    }     
        
    @Override
    public String toString() {
        return
                lemma + " " + 
                pos + " " +   
                prefix1 + " " +
                prefix2 + " " +
                prefix3 + " " +
                suffix1 + " " +
                suffix2 + " " +
                suffix3 + " " +
                wordShape + " " + 
                wordShapeShort + " " +
                allCaps + " " +
                initCaps + " " + 
                initCapsAlpha + " " +
                capsMix + " " +
                hasDigit + " " + 
                singleDigit + " " +
                doubleDigit + " " +
                naturalNumber + " " +
                realNumber + " " +
                hasDash + " " +
                initDash + " " +   
                endDash + " " +
                alphaNumeric1 + " " +
                alphaNumeric2 +" " +
                ner +
                (type.equals("") ? "" : " "+type);
    }      
    
}
