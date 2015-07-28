/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.spelementex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static main.java.spelementex.Annotator.getTokenOffset;
import main.java.spelementex.markup.SpatialRelation;
import main.java.spelementex.util.ExternalCommand;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author Jenny D'Souza
 */
public class Evaluator {
    
    /**
     * list of c values for development experiments.
     */
    public static final List<String> C_VALUES = Arrays.asList("0.1", "0.5", "1.0", 
            "5.0", "10.0", "50.0", "100.0", "500.0", "1000.0", "5000.0", "10000.0");
    
    /**
     * default model parameters.
     */
    public static String C_SE = "1.0";
    public static int NBEST_SE = 49;
    public static String C_MS = "10.0";
    public static int NBEST_MS = 19;           
    
    public static Map<String, String> OTHERROLE_C_MAP;
    static {
        Map<String, String> aMap = new HashMap<>();
        aMap.put("trajector", "50.0");
        aMap.put("landmark_link", "5.0");
        aMap.put("trigger_movelink", "1.0");
        aMap.put("mover", "0.5");
        aMap.put("source", "1000.0");
        aMap.put("midPoint", "50.0");
        aMap.put("goal", "1.0");
        aMap.put("landmark_movelink", "1.0");
        aMap.put("path", "0.5");
        aMap.put("motion_signal", "100.0");
        OTHERROLE_C_MAP = aMap;
    } 
    public static Map<String, Integer> OTHERROLE_NBEST_MAP;
    static {
        Map<String, Integer> aMap = new HashMap<>();
        aMap.put("trajector", 16);
        aMap.put("landmark_link", 11);
        aMap.put("trigger_movelink", 16);
        aMap.put("mover", 6);
        aMap.put("source", 11);
        aMap.put("midPoint", 6);
        aMap.put("goal", 6);
        aMap.put("landmark_movelink", 11);
        aMap.put("path", 6);
        aMap.put("motion_signal", 6);
        OTHERROLE_NBEST_MAP = aMap;        
    }       
    public static Map<String, String> ROLE_C_MAP;
    static {
        Map<String, String> aMap = new HashMap<>();
        aMap.put("trajector", "5000.0");
        aMap.put("landmark_link", "1.0");
        aMap.put("trigger_link", "500.0");        
        aMap.put("trigger_movelink", "5.0");
        aMap.put("mover", "1.0");
        aMap.put("source", "500.0");
        aMap.put("midPoint", "0.1");
        aMap.put("goal", "1000.0");
        aMap.put("landmark_movelink", "0.1");
        aMap.put("path", "0.1");
        aMap.put("motion_signal", "1.0");
        ROLE_C_MAP = aMap;        
    }     
    public static Map<String, Integer> ROLE_NBEST_MAP;
    static {
        Map<String, Integer> aMap = new HashMap<>();
        aMap.put("trajector", 1);
        aMap.put("landmark_link", 1);
        aMap.put("trigger_link", 1);        
        aMap.put("trigger_movelink", 1);
        aMap.put("mover", 1);
        aMap.put("source", 1);
        aMap.put("midPoint", 6);
        aMap.put("goal", 1);
        aMap.put("landmark_movelink", 16);
        aMap.put("path", 6);
        aMap.put("motion_signal", 1);
        ROLE_NBEST_MAP = aMap;        
    }    
    
    /**
     * prints C and N-best values
     */
    public static void printModelParamValues() {
        System.out.println("For spatial entities");
        System.out.println("C: "+C_SE+"; N-best: "+NBEST_SE);
        System.out.println("For motion signals");
        System.out.println("C: "+C_MS+"; N-best: "+NBEST_MS);
    }
    
    /**
     * gets f-score by standard formula.
     * 
     * @param recall
     * @param precision
     * @return 
     */
    public static double getFscore(double recall, double precision) {
        return (precision == 0 || recall == 0) ? 0.0 : (2*precision*recall)/(precision+recall);
    }
    
    /**
     * gets precision by standard formula.
     * 
     * @param tp
     * @param fp
     * @return 
     */
    public static double getPrecision(double tp, double fp) {
        return tp == 0.0 ? 0.0 : tp/(tp+fp);
    }
    
    /**
     * gets recall by standard formula.
     * 
     * @param tp
     * @param total
     * @return 
     */
    public static double getRecall(double tp, double total) {
        return tp == 0.0 ? 0.0 : tp/total;
    }
        
    /**
     * given result file, gets the true and false positive entity counts.
     * please note: this function is mainly intended for development purposes; 
     * it will not work if the result file does not have gold annotation labels 
     * in the second to the last column.
     * 
     * @param result
     * @return double[] of size 2 with true positive count at index 0 and
     * false positive count at index 1.
     */
    public static double[] getTpFp(String[] result) {
        double[] tpFp = new double[2];
        
        Set<String> goldAnnotations = new HashSet<>();
        Set<Integer> visitedTokens = new HashSet<>();
        Set<String> predictedAnnotations = new HashSet<>();
        Set<Integer> predictedTokens = new HashSet<>();
        int tokenNum = 0;
        
        int length = result.length;
        for (int i = 0; i < length; i++) {
            String line = result[i].trim();
            if (line.equals(""))
                continue;
            
            String[] lineTokens = line.split("\\s+");
            
            if (lineTokens.length == 3) {
                tokenNum = 0;
                //# 0 0.076469
                if (lineTokens[1].equals("0") && !goldAnnotations.isEmpty()) {
                    goldAnnotations.retainAll(predictedAnnotations);
                    tpFp[0] += goldAnnotations.size();
                    predictedAnnotations.removeAll(goldAnnotations);
                    tpFp[1] += predictedAnnotations.size();
                    
                    goldAnnotations = new HashSet<>();
                    visitedTokens = new HashSet<>();
                    predictedAnnotations = new HashSet<>();
                    predictedTokens = new HashSet<>();
                }
                continue;
            }
            
            String goldTag = lineTokens[lineTokens.length-2];
            if (goldTag.matches("B\\-.*") && !visitedTokens.contains(tokenNum)) {
                int[] tokenOffsets = getTokenOffset(result, i, tokenNum);
                goldAnnotations.add(tokenOffsets[0]+"-"+tokenOffsets[1]+"-"+goldTag);
                visitedTokens.add(tokenNum);
            }
            
            String predictedTag = lineTokens[lineTokens.length-1];
            if (predictedTag.matches("B\\-.*") && !predictedTokens.contains(tokenNum)) {
                if (lineTokens[0].matches("[^a-zA-Z0-9]+") || lineTokens[0].equals("-lrb-") || lineTokens[0].equals("-rrb-")) {
                    if (i+1 == result.length) {
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
                int[] tokenOffsets = getTokenOffset(result, i, tokenNum);

                Set<Integer> tempPredictedTokens = new HashSet<>();
                for (int j = tokenOffsets[0]; j <= tokenOffsets[1]; j++) 
                    tempPredictedTokens.add(j);
                
                int before = tempPredictedTokens.size();
                tempPredictedTokens.removeAll(predictedTokens);
                int after = tempPredictedTokens.size();
                if (before == after) {
                    predictedTokens.addAll(tempPredictedTokens);
                    predictedAnnotations.add(tokenOffsets[0]+"-"+tokenOffsets[1]+"-"+predictedTag);
                }
            }
            
            tokenNum++;
        }
        
        goldAnnotations.retainAll(predictedAnnotations);
        tpFp[0] += goldAnnotations.size();
        predictedAnnotations.removeAll(goldAnnotations);
        tpFp[1] += predictedAnnotations.size();        
        
        return tpFp;
    }
    
    /**
     * gets the total number of entities in data.
     * 
     * @param data
     * @return double the total number of entities in data
     */
    public static double getTotal(String[] data) {
        double total = 0.0;
        for (String line : data) {
            line = line.trim();
            String[] lineTokens = line.split("\\s+");
            String lastToken = lineTokens[lineTokens.length-1];
            if (lastToken.matches("B\\-.*"))
                total++;
        }
        return total;
    }
        
    /**
     * Develops the optimal model based on user-provided training and development data.
     * Tries all combinations of C_VALUES and N-best to find the combination that maximizes recall.
     * 
     * @param se
     * @param train
     * @param model
     * @param test
     * @param result
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public static void develop(boolean se, String train, String model, String test, String result) throws FileNotFoundException, IOException {
        double bestFscore = 0.0;    
        double r = 0.0;
        double p = 0.0;
        //stores the total entities annotated in development data
        double total = getTotal(FileUtils.readFileToString(new File(test)).split("\\n"));
        
        for (String c_value : C_VALUES) {
            //train a model
            String crf_command = Main.CRF_DIR+"\\crf_learn.exe -c "+c_value+" "+Main.TYPE_TEMPLATE_FILE+" "+train+" "+model;
            ExternalCommand.run(crf_command, null);
            
            for (int n = 1; n <= 50; n = n+2) {
                //test the trained model with different n-best values
                crf_command = Main.CRF_DIR+"\\crf_test.exe -n "+n+" -m "+model+" "+test;
                ExternalCommand.run(crf_command, new FileOutputStream(result));

                //compute tp, fp, recall, precision and f-score of result from test
                double[] tpFp = getTpFp(FileUtils.readFileToString(new File(result)).split("\\n"));
                double tempRecall = getRecall(tpFp[0], total);
                double tempPrecision = getPrecision(tpFp[0], tpFp[1]);
                double tempFscore = getFscore(tempRecall, tempPrecision);
                                
                //if new score is better than previous best score, then
                //update previous best score to new score and 
                //set c and nBest to current model parameter combination. 
                if (tempRecall > r || //if new recall is better than previous recall
                        (tempRecall == r && tempFscore > bestFscore)) { //if new recall is equal to previous recall and new fscore is better than previous fscore
                    bestFscore = tempFscore;
                    r = tempRecall; p = tempPrecision;
                    if (se) {
                        C_SE = c_value;
                        NBEST_SE = n;
                    }
                    else {
                        C_MS = c_value;
                        NBEST_MS = n;
                    }
                }
            }
        }        
        Main.log.write(("\nfor "+(se ? "other spatial elements" : "motion-signals")+" on dev data\n").getBytes());
        Main.log.write(("best c-value: "+(se ? C_SE : C_MS)+"; best n: "+(se ? NBEST_SE : NBEST_MS)+"\n").getBytes());
        Main.log.write(("r: "+r+"; p: "+p+"; f: "+bestFscore+"\n\n").getBytes());
    }
    
    /**
     * given result file, gets the true and false positive entity counts.
     * please note: this function is mainly intended for development purposes; 
     * it will not work if the result file does not have gold annotation labels 
     * in the second to the last column.
     * 
     * @param result
     * @param tagType
     * @return double[] of size 2 with true positive count at index 0 and
     * false positive count at index 1.
     */
    public static double[] getTpFp(String[] result, String tagType) {
        double[] tpFp = new double[2];
        
        Set<String> goldAnnotations = new HashSet<>();
        Set<Integer> visitedTokens = new HashSet<>();
        Set<String> predictedAnnotations = new HashSet<>();
        Set<Integer> predictedTokens = new HashSet<>();
        int tokenNum = 0;
        
        int length = result.length;
        for (int i = 0; i < length; i++) {
            String line = result[i].trim();
            if (line.equals(""))
                continue;
            
            String[] lineTokens = line.split("\\s+");
            
            if (lineTokens.length == 3) {
                tokenNum = 0;
                //# 0 0.076469
                if (lineTokens[1].equals("0") && !goldAnnotations.isEmpty()) {
                    goldAnnotations.retainAll(predictedAnnotations);
                    tpFp[0] += goldAnnotations.size();
                    predictedAnnotations.removeAll(goldAnnotations);
                    tpFp[1] += predictedAnnotations.size();
                    
                    goldAnnotations = new HashSet<>();
                    visitedTokens = new HashSet<>();
                    predictedAnnotations = new HashSet<>();
                    predictedTokens = new HashSet<>();
                }
                continue;
            }
            
            String goldTag = lineTokens[lineTokens.length-2];
            if (goldTag.matches("B\\-"+tagType) && !visitedTokens.contains(tokenNum)) {
                int[] tokenOffsets = getTokenOffset(result, i, tokenNum);
                goldAnnotations.add(tokenOffsets[0]+"-"+tokenOffsets[1]+"-"+goldTag);
                visitedTokens.add(tokenNum);
            }
            
            String predictedTag = lineTokens[lineTokens.length-1];
            if (predictedTag.matches("B\\-"+tagType) && !predictedTokens.contains(tokenNum)) {
                if (lineTokens[0].matches("[^a-zA-Z0-9]+") || lineTokens[0].equals("-lrb-") || lineTokens[0].equals("-rrb-")) {
                    if (i+1 == result.length) {
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
                int[] tokenOffsets = getTokenOffset(result, i, tokenNum);

                Set<Integer> tempPredictedTokens = new HashSet<>();
                for (int j = tokenOffsets[0]; j <= tokenOffsets[1]; j++) 
                    tempPredictedTokens.add(j);
                
                int before = tempPredictedTokens.size();
                tempPredictedTokens.removeAll(predictedTokens);
                int after = tempPredictedTokens.size();
                if (before == after) {
                    predictedTokens.addAll(tempPredictedTokens);
                    predictedAnnotations.add(tokenOffsets[0]+"-"+tokenOffsets[1]+"-"+predictedTag);
                }
            }
            
            tokenNum++;
        }
        
        goldAnnotations.retainAll(predictedAnnotations);
        tpFp[0] += goldAnnotations.size();
        predictedAnnotations.removeAll(goldAnnotations);
        tpFp[1] += predictedAnnotations.size();        
        
        return tpFp;
    }    
    
    /**
     * gets the total number of entities of a tag type in data.
     * 
     * @param data
     * @param tagType
     * @return 
     */
    public static double getTotal(String[] data, String tagType) {
        double total = 0.0;
        for (String line : data) {
            line = line.trim();
            String[] lineTokens = line.split("\\s+");
            String lastToken = lineTokens[lineTokens.length-1];
            if (lastToken.matches("B\\-"+tagType))
                total++;
        }
        return total;        
    }    
    
    /**
     * Develops the optimal model based on user-provided training and development data.
     * Tries all combinations of C_VALUES and N-best to find the combination that maximizes recall.
     * 
     * @param role
     * @param train
     * @param model
     * @param test
     * @param result
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public static void develop(String role, String train, String model, String test, String result) throws FileNotFoundException, IOException {
        double bestFscore = 0.0;    
        double r = 0.0;
        double p = 0.0;
        //stores the total entities annotated in development data
        double total = getTotal(FileUtils.readFileToString(new File(test)).split("\\n"));
        
        for (String c_value : C_VALUES) {
            //train a model
            String crf_command = Main.CRF_DIR+"\\crf_learn.exe -c "+c_value+" "+Main.ROLE_TEMPLATE_FILE+" "+train+" "+model;
            ExternalCommand.run(crf_command, null);
            
            for (int n = 1; n <= 51; n = n+5) {
                //test the trained model with different n-best values
                crf_command = Main.CRF_DIR+"\\crf_test.exe -n "+n+" -m "+model+" "+test;
                ExternalCommand.run(crf_command, new FileOutputStream(result));

                //compute tp, fp, recall, precision and f-score of result from test
                double[] tpFp = getTpFp(FileUtils.readFileToString(new File(result)).split("\\n"));
                double tempRecall = getRecall(tpFp[0], total);
                double tempPrecision = getPrecision(tpFp[0], tpFp[1]);
                double tempFscore = getFscore(tempRecall, tempPrecision);
                                
                //if new score is better than previous best score, then
                //update previous best score to new score and 
                //set c and nBest to current model parameter combination. 
                if (tempRecall > r || //if new recall is better than previous recall
                        (tempRecall == r && tempFscore > bestFscore)) { //if new recall is equal to previous recall and new fscore is better than previous fscore
                    bestFscore = tempFscore;
                    r = tempRecall; p = tempPrecision;
                    OTHERROLE_C_MAP.put(role, c_value);
                    OTHERROLE_NBEST_MAP.put(role, n);
                }
            }
        }        
        Main.log.write(("for non-overlapping roles to "+role+" on dev data\n").getBytes());
        Main.log.write(("best c-value: "+OTHERROLE_C_MAP.get(role)+"; best n: "+OTHERROLE_NBEST_MAP.get(role)+"\n").getBytes());
        Main.log.write(("r: "+r+"; p: "+p+"; f: "+bestFscore+"\n\n").getBytes());
    }    
    
    /**
     * Develops the optimal model based on user-provided training and development data.
     * Tries all combinations of C_VALUES and N-best to find the combination that maximizes recall.
     * 
     * @param se
     * @param outputDir
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public static void develop(String outputDir) throws FileNotFoundException, IOException {
        for (String role : SpatialRelation.ROLE_OVERLAP_ROLES_MAP.keySet()) {
            
            if (!role.equals("trigger_link")) {
                develop(role, outputDir+"\\"+role+"TrainOther.txt", outputDir+"\\"+role+"ModelOther.txt", 
                        outputDir+"\\"+role+"TestOther.txt", outputDir+"\\"+role+"ResultOther.txt");
            }
            
            String train = outputDir+"\\"+role+"Train.txt";
            String model = outputDir+"\\"+role+"Model.txt";
            String test = outputDir+"\\"+role+"Test.txt";
            String result = outputDir+"\\"+role+"Result.txt";
            
            double bestFscore = 0.0;    
            double r = 0.0;
            double p = 0.0;
            //stores the total entities annotated in development data
            double total = getTotal(FileUtils.readFileToString(new File(test)).split("\\n"), role);

            for (String c_value : C_VALUES) {
                //train a model
                String crf_command = Main.CRF_DIR+"\\crf_learn.exe -c "+c_value+" "+Main.ROLE_TEMPLATE_FILE+" "+train+" "+model;
                ExternalCommand.run(crf_command, null);

                for (int n = 1; n <= 51; n = n+5) {
                    //test the trained model with different n-best values
                    crf_command = Main.CRF_DIR+"\\crf_test.exe -n "+n+" -m "+model+" "+test;
                    ExternalCommand.run(crf_command, new FileOutputStream(result));

                    //compute tp, fp, recall, precision and f-score of result from test
                    double[] tpFp = getTpFp(FileUtils.readFileToString(new File(result)).split("\\n"), role);
                    double tempRecall = getRecall(tpFp[0], total);
                    double tempPrecision = getPrecision(tpFp[0], tpFp[1]);
                    double tempFscore = getFscore(tempRecall, tempPrecision);

                    //if new score is better than previous best score, then
                    //update previous best score to new score and 
                    //set c and nBest to current model parameter combination. 
                    if (tempFscore > bestFscore) {
                    /*if (tempRecall > r || //if new recall is better than previous recall
                            (tempRecall == r && tempFscore > bestFscore)) { //if new recall is equal to previous recall and new fscore is better than previous fscore*/
                        bestFscore = tempFscore;
                        r = tempRecall; p = tempPrecision;
                        
                        ROLE_C_MAP.put(role, c_value);
                        ROLE_NBEST_MAP.put(role, n);
                    }
                }
            } 
            Main.log.write(("for "+role+" on dev data\n").getBytes());
            Main.log.write(("best c-value: "+ROLE_C_MAP.get(role)+"; best n: "+ROLE_NBEST_MAP.get(role)+"\n").getBytes());
            Main.log.write(("r: "+r+"; p: "+p+"; f: "+bestFscore+"\n\n").getBytes());
        }
    }    
    
    public static void train(String c_value, String template, String train, String model) {
        String crf_command = Main.CRF_DIR+"\\crf_learn.exe -c "+c_value+" "+template+" "+train+" "+model;
        ExternalCommand.run(crf_command, null);
    }    
    
    public static void applyModel(int n_value, String model, String test, String result) throws FileNotFoundException {
        String crf_command = Main.CRF_DIR+"\\crf_test.exe -n "+n_value+" -m "+model+" "+test;
        ExternalCommand.run(crf_command, new FileOutputStream(result));        
    }
    
    public static void evaluate(String role, String test, String result) throws FileNotFoundException, IOException {        
        double total = role.equals("") ? getTotal(FileUtils.readFileToString(new File(test)).split("\\n")) : getTotal(FileUtils.readFileToString(new File(test)).split("\\n"), role);
        double[] tpFp = role.equals("") ? getTpFp(FileUtils.readFileToString(new File(result)).split("\\n")) : getTpFp(FileUtils.readFileToString(new File(result)).split("\\n"), role);
        double recall = getRecall(tpFp[0], total);
        double precision = getPrecision(tpFp[0], tpFp[1]);
        double f_score = getFscore(recall, precision);
        
        Main.log.write(("recall: "+recall+" precision: "+precision+" f-score: "+f_score+"\n").getBytes());
    }    
    
    public static void evaluate(String outputDir) throws FileNotFoundException, IOException {
        for (String role : SpatialRelation.ROLE_OVERLAP_ROLES_MAP.keySet()) {
            
            if (!role.equals("trigger_link")) {
                Main.log.write(("for non-overlapping roles with "+role+"\n").getBytes());
                
                String model = outputDir+"\\"+role+"ModelOther.txt";
                String test = outputDir+"\\"+role+"TestOther.txt";
                String result = outputDir+"\\"+role+"ResultOther.txt";                
                
                applyModel(OTHERROLE_NBEST_MAP.get(role), model, test, result);
                evaluate("", test, result);
                Main.log.write("\n".getBytes());
            }
            
            Main.log.write(("for role: "+role+"\n").getBytes());
            
            String model = outputDir+"\\"+role+"Model.txt";
            String test = outputDir+"\\"+role+"Test.txt";
            String result = outputDir+"\\"+role+"Result.txt";

            applyModel(ROLE_NBEST_MAP.get(role), model, test, result);
            evaluate(role, test, result);            
            Main.log.write("\n".getBytes());
        }
    }    
        
}
