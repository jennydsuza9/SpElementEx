/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.parsers;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import main.java.spelementex.markup.Doc;

/**
 *
 * @author Jenny D'Souza
 */
public class StanfordParser {
    
    static StanfordCoreNLP pipeline;
    
    /**
     * Initializes the Stanford CoreNLP parser with input annotator properties
     * as a string of the desired annotator flags. 
     * For e.g., "tokenize, ssplit, pos, lemma, ner". 
     */
    public static void initializeStanfordParser() {
        // Create a CoreNLP pipeline.
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma, ner");
        pipeline = new StanfordCoreNLP(props);        
    }    
    
    /** 
     * Parses a given input text document using the Stanford CoreNLP parser.
     * 
     * @param document
     * @return Map which links sentences to their token annotation through the token's begin character offset
     * @throws java.io.UnsupportedEncodingException 
     */
    public static Map<Integer, Map<Integer, CoreLabel>> parse(Doc document) throws UnsupportedEncodingException {

        // Initialize an Annotation with some text to be annotated. The text is the argument to the constructor.
        Annotation annotation = new Annotation(new String(document.getText().getBytes("UTF-8"), "UTF-8"));
        //Annotation annotation = new Annotation(new String(document.getText().getBytes("US-ASCII"), "US-ASCII"));
        // run all the selected Annotators on this text
        pipeline.annotate(annotation);

        // An Annotation is a Map and you can get and use the various analyses individually.
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        
        //returns if the annotation is empty.
        if (sentences == null || sentences.isEmpty())
            return null;
                
        //map to return linking sentences to their tokens annotation through the tokens' begin character offset.
        Map<Integer, Map<Integer, CoreLabel>> sentenceStartOffsetParserToken = new TreeMap<>();
        
        int sentenceNumber = 1;
        for (CoreMap sentence : sentences) {

            Map<Integer, CoreLabel> startOffsetParserToken = sentenceStartOffsetParserToken.get(sentenceNumber);
            if (startOffsetParserToken == null)
                sentenceStartOffsetParserToken.put(sentenceNumber, startOffsetParserToken = new TreeMap<>());
            sentenceNumber++;                
            
            //extracting tokenized information from the stanford parser output.
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) 
                startOffsetParserToken.put(token.beginPosition(), token);
        }
        return sentenceStartOffsetParserToken;
    }    
    
    /**
     * Function to link sentence->token number->token offset from sentence->token offset->token.
     * 
     * @param sentenceStartOffsetParserToken
     * @return Map which links sentence->token number->token offset
     */
    public static Map<Integer, Map<Integer, Integer>> getSentenceTokenStartOffsetInfo(Map<Integer, Map<Integer, CoreLabel>> sentenceStartOffsetParserToken) {
        Map<Integer, Map<Integer, Integer>> sentenceTokenStartOffsetInfo = new TreeMap<>();
        sentenceStartOffsetParserToken.keySet().stream().forEach((sentence) -> {
            Map<Integer, CoreLabel> startOffsetParserToken = sentenceStartOffsetParserToken.get(sentence);
            
            Map<Integer, Integer> tokenStartOffsetInfo = sentenceTokenStartOffsetInfo.get(sentence);
            if (tokenStartOffsetInfo == null)
                sentenceTokenStartOffsetInfo.put(sentence, tokenStartOffsetInfo = new TreeMap<>());
            int tokenNum = 1;
            
            for (int startOffset : startOffsetParserToken.keySet()) {
                tokenStartOffsetInfo.put(tokenNum, startOffset);
                tokenNum++;
            }
        });
        return sentenceTokenStartOffsetInfo;
    }
    
}
