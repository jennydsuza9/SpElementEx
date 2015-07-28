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
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import main.java.parsers.DomParser;
import main.java.parsers.StanfordParser;
import org.xml.sax.SAXException;

/**
 * SpEntEx: Spatial Entity Extractor for extracting spatial entities in text.
 * 
 * To use only our features, provide your training, development, and test data.
 * Usage: java Main -train <train-directory> -dev <dev-directory> -test <test-directory>
 * 
 * To use our features and our trained model, provide your test data to annotate.
 * Usage: java Main -test <test-directory>
 * 
 * @author Jenny D'Souza
 */
public class Main {
        
    public static int type = 0;
    public static int role = 0;
    
    public static final String TYPE_TEMPLATE_FILE = "main\\resources\\templates\\type-template.txt";
    public static final String SE_MODEL_FILE = "main\\resources\\models\\types\\seModel.txt";
    public static final String MS_MODEL_FILE = "main\\resources\\models\\types\\msModel.txt";
    public static final String ROLE_TEMPLATE_FILE = "main\\resources\\templates\\role-template.txt";
    public static final String CRF_DIR = "main\\resources\\crfpp";
            
    public static File trainDir;
    public static File devDir;
    public static File testDir;
      
    public static DomParser dp;
    
    public static FileOutputStream log;        
    
    public Main(String[] args) throws FileNotFoundException {      
        testDir = new File(args[args.length-1]);
        if (args[1].contains("type"))
            type = 1;
        if (args[1].contains("role"))
            role = 1;
        if (args.length == 8) {
            trainDir = new File(args[3]);
            devDir = new File(args[5]);
        }
        dp = new DomParser();        
        StanfordParser.initializeStanfordParser();
        log = new FileOutputStream("log.txt");        
    }
     
    /**
     * Marks up spatial elements in test data with relation roles using pre-trained models from SpaceEval-2015 training data.
     * 
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException 
     */
    public void markUpRawXMLWithRoles() throws SAXException, ParserConfigurationException, IOException {        
        Annotator annotator = new Annotator(type == 1 && role == 1 ? FilesProcessor.getXMLFiles(new File("output")) : FilesProcessor.getXMLFiles(testDir));
        annotator.markUpSpatialElementRoles("main\\data", "main\\resources\\models\\roles");        
    }
    
    /**
     * Marks up test data with spatial elements using pre-trained models from SpaceEval-2015 training data.
     * 
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException 
     */
    public void markUpRawXMLWithTypes() throws SAXException, ParserConfigurationException, IOException {                
        Annotator annotator = new Annotator(FilesProcessor.getXMLFiles(testDir));        
        String test = "main\\data\\test.txt";
        String seResult = "main\\data\\seResult.txt";
        String msResult = "main\\data\\msResult.txt";
        annotator.markUpSpatialElementTypes(Main.SE_MODEL_FILE, Main.MS_MODEL_FILE, test, seResult, msResult);        
    }
    
    /**
     * Trains role and non-role models from combined train, dev, and test data.
     * The output model files are written to the resources folder.
     * 
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException 
     */
    public void trainRolesModel() throws SAXException, ParserConfigurationException, IOException {
        Collection<File> xmlFiles = FilesProcessor.getXMLFiles(trainDir);
        xmlFiles.addAll(FilesProcessor.getXMLFiles(devDir));
        xmlFiles.addAll(FilesProcessor.getXMLFiles(testDir));
        
        Trainer trainer = new Trainer(xmlFiles);
        trainer.writeRoleLabelledCrfData("main\\data", true);
        
        for (String r : Evaluator.ROLE_C_MAP.keySet()) 
            Evaluator.train(Evaluator.ROLE_C_MAP.get(r), ROLE_TEMPLATE_FILE, "main\\data\\"+r+"Train.txt", "main\\resources\\models\\roles\\"+r+"Model.txt");
        for (String r : Evaluator.OTHERROLE_C_MAP.keySet()) 
            Evaluator.train(Evaluator.OTHERROLE_C_MAP.get(r), ROLE_TEMPLATE_FILE, "main\\data\\"+r+"TrainOther.txt", "main\\resources\\models\\roles\\"+r+"ModelOther.txt");
    }    
    
    /**
     * Trains se and ms models from combined train, dev, and test data.
     * The output model files are written to the resources folder.
     * 
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException 
     */
    public void trainModel() throws SAXException, ParserConfigurationException, IOException {
        //combine all space-eval data to train a model
        Collection<File> xmlFiles = FilesProcessor.getXMLFiles(trainDir);
        xmlFiles.addAll(FilesProcessor.getXMLFiles(devDir));
        xmlFiles.addAll(FilesProcessor.getXMLFiles(testDir));
        
        Trainer trainer = new Trainer(xmlFiles);
        String seTrain = "main\\data\\seTrain.txt";        
        String msTrain = "main\\data\\msTrain.txt";        
        trainer.writeTypeLabelledCrfData(new FileOutputStream(seTrain), new FileOutputStream(msTrain));   
        
        Evaluator.train(Evaluator.C_SE, TYPE_TEMPLATE_FILE, seTrain, "main\\resources\\seNewModel.txt");
        Evaluator.train(Evaluator.C_MS, TYPE_TEMPLATE_FILE, msTrain, "main\\resources\\msNewModel.txt");        
    }
    
    /**
     * Trains models for extracting roles based on optimal C and N-best parameters.
     * Then applies these models to extract roles from input test data.
     * 
     * @throws org.xml.sax.SAXException
     * @throws javax.xml.parsers.ParserConfigurationException
     * @throws java.io.IOException
     */
    public void trainAndMarkUpRawXMLWithRoles() throws SAXException, ParserConfigurationException, IOException {
        //writes training data
        Collection<File> xmlFiles = FilesProcessor.getXMLFiles(trainDir);
        xmlFiles.addAll(FilesProcessor.getXMLFiles(devDir));
        
        Trainer trainer = new Trainer(xmlFiles);
        trainer.clearCrfRoleDataFiles("main\\data", true);
        trainer.writeRoleLabelledCrfData("main\\data", true);
        
        for (String r : Evaluator.ROLE_C_MAP.keySet()) 
            Evaluator.train(Evaluator.ROLE_C_MAP.get(r), ROLE_TEMPLATE_FILE, "main\\data\\"+r+"Train.txt", "main\\data\\"+r+"Model.txt");
        for (String r : Evaluator.OTHERROLE_C_MAP.keySet()) 
            Evaluator.train(Evaluator.OTHERROLE_C_MAP.get(r), ROLE_TEMPLATE_FILE, "main\\data\\"+r+"TrainOther.txt", "main\\data\\"+r+"ModelOther.txt");
                
        Annotator annotator = new Annotator(type == 1 && role == 1 ? FilesProcessor.getXMLFiles(new File("output")) : FilesProcessor.getXMLFiles(testDir));
        annotator.markUpSpatialElementRoles("main\\data", "main\\data");
    }
    
    /**
     * Trains two models for extracting SE and MS, respectively, based on optimal
     * C and N-best parameters. Then applies these models to extract SEs and MSs
     * from input test data.
     * 
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException 
     */
    public void trainAndMarkUpRawXMLWithTypes() throws SAXException, ParserConfigurationException, IOException {
        //combine training and dev data to train a model on more instances
        Collection<File> xmlFiles = FilesProcessor.getXMLFiles(trainDir);
        xmlFiles.addAll(FilesProcessor.getXMLFiles(devDir));
        
        //writes training data
        Trainer trainer = new Trainer(xmlFiles);        
        String seTrain = "main\\data\\seTrain.txt";        
        String msTrain = "main\\data\\msTrain.txt";        
        trainer.writeTypeLabelledCrfData(new FileOutputStream(seTrain), new FileOutputStream(msTrain));
                
        //trains models for ms and se, respectively.
        String seModel = "main\\data\\seModel.txt";        
        String msModel = "main\\data\\msModel.txt";        
        Evaluator.train(Evaluator.C_SE, TYPE_TEMPLATE_FILE, seTrain, seModel);
        Evaluator.train(Evaluator.C_MS, TYPE_TEMPLATE_FILE, msTrain, msModel);
        
        //annotator marks up raw xml
        Annotator annotator = new Annotator(FilesProcessor.getXMLFiles(testDir));        
        String test = "main\\data\\test.txt";
        String seResult = "main\\data\\seResult.txt";
        String msResult = "main\\data\\msResult.txt";
        annotator.markUpSpatialElementTypes(seModel, msModel, test, seResult, msResult);        
    }
    
    /**
     * Uses training and development data to develop an optimal model for predicting
     * spatial element roles in relations.
     * 
     * @throws org.xml.sax.SAXException
     * @throws javax.xml.parsers.ParserConfigurationException
     * @throws java.io.IOException
     */
    public void developRoleExtractionModel() throws SAXException, ParserConfigurationException, IOException {
        //writes training data
        Collection<File> xmlFiles = FilesProcessor.getXMLFiles(trainDir);  
        Trainer trainer = new Trainer(xmlFiles);
        trainer.clearCrfRoleDataFiles("main\\data", true);
        trainer.writeRoleLabelledCrfData("main\\data", true);
                
        //write development data
        xmlFiles = FilesProcessor.getXMLFiles(devDir);
        trainer = new Trainer(xmlFiles);
        trainer.clearCrfRoleDataFiles("main\\data", false);
        trainer.writeRoleLabelledCrfData("main\\data", false);
        
        //develop role model
        //in other words, optimizes C and N-best parameters for role extraction
        Evaluator.develop("main\\data");
    }
    
    /**
     * Uses training and development data to develop an optimal model for predicting
     * motion signals and other spatial elements.
     * 
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException 
     */
    public void developTypeExtractionModel() throws SAXException, ParserConfigurationException, IOException {          
        //writes training data
        Collection<File> xmlFiles = FilesProcessor.getXMLFiles(trainDir);         
        Trainer trainer = new Trainer(xmlFiles);        
        String seTrain = "main\\data\\seTrain.txt";
        String msTrain = "main\\data\\msTrain.txt";
        trainer.writeTypeLabelledCrfData(new FileOutputStream(seTrain), new FileOutputStream(msTrain));
        
        //write development data
        xmlFiles = FilesProcessor.getXMLFiles(devDir);        
        trainer = new Trainer(xmlFiles);        
        String seDevelop = "main\\data\\seTest.txt";
        String msDevelop = "main\\data\\msTest.txt";
        trainer.writeTypeLabelledCrfData(new FileOutputStream(seDevelop), new FileOutputStream(msDevelop));
        
        //develops SE model
        //in other words, optimizes C and N-best parameters for SE extraction
        String seModel = "main\\data\\seModel.txt";
        String seResult = "main\\data\\seResult.txt";
        Evaluator.develop(true, seTrain, seModel, seDevelop, seResult);
        
        //develops MS model
        //in other words, optimizes C and N-best parameters for MS extraction
        String msModel = "main\\data\\msModel.txt";
        String msResult = "main\\data\\msResult.txt";
        Evaluator.develop(false, msTrain, msModel, msDevelop, msResult);
    }
    
    public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
        Main main;                   
        
        //to develop a model by optmizing model parameters on development data, 
        //retrain new model with optimal C and N-best parameters, and
        //using retrained models, predict spatial entities and motion signals from test data.
        if (args.length == 8 && args[2].equals("-train") && (new File(args[3])).exists() &&
                args[4].equals("-dev") && (new File(args[5])).exists() && 
                args[6].equals("-test") && (new File(args[7])).exists()) {
            main = new Main(args);
            if (type == 1) {
                //optimizes model parameters on development set
                main.developTypeExtractionModel();
                //trains a new model on training and dev data using optimized model parameters from above
                //annotated test data with the model
                main.trainAndMarkUpRawXMLWithTypes();
                //NOTE: uncomment line below only to generate model file on entire dataset scattered in train, dev, and test directories
                //main.trainModel(); 
            }
            if (role == 1) {
                main.developRoleExtractionModel();
                main.trainAndMarkUpRawXMLWithRoles();
                //NOTE: uncomment line below only to generate model file on entire dataset scattered in train, dev, and test directories
                //main.trainRolesModel();
            }
        }
        //mark up test data using pre-trained model on SpaceEval-2015 training data
        else if (args.length == 4 && args[2].equals("-test") && (new File(args[3])).exists()) {
            main = new Main(args);
            if (type == 1)
                main.markUpRawXMLWithTypes();
            if (role == 1)
                main.markUpRawXMLWithRoles();
        }
        else {       
            System.out.println("=======================================");
            System.out.println("To use only our features, provide your training, development, and test data.");
            System.out.println("Usage: java main.java.spentex.Main "
                    + "-annotators type,role "
                    + "-train <YOUR TRAIN DIRECTORY> "
                    + "-dev <YOUR DEVELOPMENT DIRECTORY> "
                    + "-test <YOUR TEST DIRECTORY>");
            System.out.println(" ");
            System.out.println("To use our features and our trained model, provide your test data to annotate.");
            System.out.println("Usage: java main.java.spentex.Main "
                    + "-annotators type,role "
                    + "-test <YOUR TEST DIRECTORY>");
            System.out.println("=======================================");
            
            System.exit(1);
        }
    }
    
}
