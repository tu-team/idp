/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import edu.cmu.minorthird.classify.algorithms.trees.*;
import edu.cmu.minorthird.classify.algorithms.svm.*;
import edu.cmu.minorthird.classify.algorithms.knn.*;
import edu.cmu.minorthird.classify.transform.*;
import edu.cmu.minorthird.classify.sequential.*;
import edu.cmu.minorthird.classify.multi.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;

import java.util.*;
import java.io.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import org.apache.log4j.*;

/**
 * Main UI program for the 'classify' package. 
 *
 * @author Cameron Williams
 */
public class ClassifyCommandLineUtil extends BasicCommandLineProcessor
{
    private static final Set LEGAL_OPS = new HashSet(Arrays.asList(new String[]{"simple","sequential", "multi"}));

    private static Dataset safeToDataset(String s, boolean seq, int multi) {
	boolean sequential = seq;
	try {
	    if (s.startsWith("sample:")) return Expt.toDataset(s);
	    else if (sequential) return DatasetLoader.loadSequence(new File(s));
	    else if (multi > 0) return DatasetLoader.loadMulti(new File(s), multi);
	    else return DatasetLoader.loadFile(new File(s));
	} catch (IOException ex) {
	    throw new IllegalArgumentException("Error loading '"+s+"': "+ex);
	} catch (NumberFormatException ex) {
	    throw new IllegalArgumentException("Error loading '"+s+"': "+ex);
	}
    }

    private static SequenceClassifierLearner toSeqLearner(String s)
    {
	try {
	    bsh.Interpreter interp = new bsh.Interpreter();
	    interp.eval("import edu.cmu.minorthird.classify.*;");
	    interp.eval("import edu.cmu.minorthird.classify.algorithms.linear.*;");
	    interp.eval("import edu.cmu.minorthird.classify.algorithms.trees.*;");
	    interp.eval("import edu.cmu.minorthird.classify.algorithms.knn.*;");
	    interp.eval("import edu.cmu.minorthird.classify.algorithms.svm.*;");
	    interp.eval("import edu.cmu.minorthird.classify.transform.*;");
	    interp.eval("import edu.cmu.minorthird.classify.semisupervised.*;");
	    interp.eval("import edu.cmu.minorthird.classify.sequential.*;");
	    return (SequenceClassifierLearner)interp.eval(s);
	} catch (bsh.EvalError e) {
	    throw new IllegalArgumentException("error parsing learnerName '"+s+"':\n"+e);
	}
    }

    /** Generalized class for Leaner... contains classifierLearner and sequentialLearner */
    public static class Learner extends BasicCommandLineProcessor{
	public static class SequentialLnr extends ClassifyCommandLineUtil.Learner {
	    public SequenceClassifierLearner seqLearner=new GenericCollinsLearner();
	    
	    public SequenceClassifierLearner getLearnerInSequentialMode() { return seqLearner; }
	    public void setLearnerInSequentialMode(SequenceClassifierLearner c) { seqLearner=c; }
	}
	public static class ClassifierLnr extends ClassifyCommandLineUtil.Learner {
	    public ClassifierLearner clsLearner = new NaiveBayes();
	    
	    public ClassifierLearner getLearner() { return clsLearner; }
	    public void setLearner(ClassifierLearner c) { clsLearner=c; }
	}
    }

    /** Parameters used for all experiments */
    public static class BaseParams extends BasicCommandLineProcessor {
	public boolean showData=false;
	public boolean showTestDetails;
	public File saveAs;
	public String saveAsFilename;
	public Object resultToShow=null, resultToSave=null;
	public boolean showResult = false;
	
	public void saveAs(String s) { saveAs = new File(s); saveAsFilename=s; }
	public void showData() { 
	    showData=true; 
	}
	public void showResult() { showResult=true; }
	public void showTestDetails() { showTestDetails=true; }
	public void other(String s) {
	    Object o = this;
	    RefUtils.modify(o,s);
	}		

	//for gui	
	public String getSaveAsFilename() { return saveAsFilename; }
	public void setSaveAsFilename(String s) { saveAsFilename=s; saveAs=new File(s);}
	public boolean getShowTestDetails() { return showTestDetails; }
	public void setShowTestDetails(boolean flag) { showTestDetails=flag; }
	public boolean getShowData() { return showData;}
	public void setShowData(boolean show) {showData = show;}
	//help buttons
	public String getSaveAsFilenameHelp() { return "The name of the file where the result will be sotred"; }
	public String getShowTestDetailsHelp() { return "Visualize Details, such as the classifier, with results"; }
	public String getShowDataHelp() { return "Open a window with the Dataset"; }
    }

    /** Parameters for training.  Common options defined and different options defined in type classes */
    public static class TrainParams extends BaseParams {	
	public String op="train";
	public Dataset trainData=null;
	public String trainDataFilename = null;			
	public ClassifyCommandLineUtil.Learner.SequentialLnr seqLnr = new ClassifyCommandLineUtil.Learner.SequentialLnr();
	public ClassifyCommandLineUtil.Learner.ClassifierLnr clsLnr = new ClassifyCommandLineUtil.Learner.ClassifierLnr();
	public ClassifyCommandLineUtil.Learner lnr = clsLnr;  
	public String typeString = "simple";
	public static TrainParams type = new SimpleTrainParams();
	// For Multi label experiment - to create the correct type of dataset in train and test
	public int multi=-1;
	public boolean crossDim = false;
	
	public TrainParams() { 
            super(); 
        }

	public void type(String s) { 
	    typeString = s;
	    if(s.equalsIgnoreCase("simple")) 
		type = new SimpleTrainParams();
	    else if (s.equalsIgnoreCase("seq")) 
		type = new SeqTrainParams();
	    else if (s.equalsIgnoreCase("multi"))
		type = new MultiTrainParams();
	    else {
		System.out.println("WARN: type " + s + " is not a valid option, will use simple");
		typeString = "simple";
	    }
	}
	
	public void multi(String dim) {
	    if (typeString.equalsIgnoreCase("multi")) {
		type.multi(dim);
		this.multi = type.multi;
	    }else System.out.println("WARN: attempting to define mutli on a " + typeString + 
				    " experiment \n      Must define -type multi to use this option");
	}
	public void cross() {
	    if (typeString.equalsIgnoreCase("multi")) {
		type.cross();
		this.crossDim = type.crossDim;
	    }else System.out.println("WARN: attempting to define cross on a " + typeString + 
				    " experiment \n      Must define -type mutlit to use this option");
	}

	public void data(String s) { 
	    type.data(s); 
	    this.trainData = type.trainData;
	    this.trainDataFilename = type.trainDataFilename;
	}
	public void learner(String s) {
	    type.learner(s); 
	    this.clsLnr.clsLearner = type.clsLnr.clsLearner;
	    this.seqLnr.seqLearner = type.seqLnr.seqLearner;
	    this.lnr = type.lnr;
	}
		
	public void saveAs(String s) { 
	    saveAs = new File(s); 
	    saveAsFilename=s; 
	    type.saveAs = this.saveAs;
	    type.saveAsFilename = this.saveAsFilename;
	}
	public void showData() { 
	    showData=true; 
	    type.showData = this.showData;
	}
	public void showResult() { 
	    showResult=true; 
	    type.showResult = this.showResult;
	}
	public void showTestDetails() { 
	    showTestDetails=true; 
	    type.showTestDetails = this.showTestDetails;
	}
	

    }

    /** Paramaters for training with a simple dataset */
    public static class SimpleTrainParams extends TrainParams {
	public SimpleTrainParams() {
	    typeString = "simple";
	}
	public void data(String s) { 
	    trainData = safeToDataset(s, false, -1);  //creates a simple dataset
	    trainDataFilename = s; 
	}
	public void learner(String s) {
	    clsLnr.clsLearner = Expt.toLearner(s); 
	    lnr = clsLnr;
	}
	//for gui
	public String getDatasetFilename() { return trainDataFilename; }
	public void setDatasetFilename(String s) { 
	    trainData = safeToDataset(s, false, -1); 
	    trainDataFilename=s; 
	}
	public ClassifyCommandLineUtil.Learner.ClassifierLnr getLearnerParameters() { return clsLnr; }
	public void setLearnerParameters(ClassifyCommandLineUtil.Learner.ClassifierLnr learn) { 
	    clsLnr = learn;
	    lnr = clsLnr; 
	}
	//help buttons
	public String getDatasetFilenameHelp() { return "The relative path and name of the file that contains the dataset"; }
	public String getLearnerParametersHelp() { return "Defines the learner to use on the trainingData.\n  This is a simple experiment, so a Classifier Learner will be used."; }
    }

    /** Paramaters for training with a simple dataset */
    public static class MultiTrainParams extends TrainParams {
	public MultiTrainParams() {
	    typeString = "multi";
	}
	public void multi(String dim) {
	    multi = new Integer(dim).intValue();
	}
	
	public void cross() {
	    if(multi <0)
		System.out.println("Warning: Cannot use crossdimensional classification without multiLabels!");
	    crossDim = true;
	}

	public void data(String s) { 
	    trainData = safeToDataset(s, false, multi);  //creates a multi dataset
	    trainDataFilename = s; 
	}
	
	public void learner(String s) {
	    clsLnr.clsLearner = Expt.toLearner(s); 
	    lnr = clsLnr;
	}

	//for gui
	public int getMulti() { return multi;}
	public void setMulti(int multi) {this.multi = multi;}
	public boolean getCross() {return crossDim;}
	public void setCross(boolean cross) {this.crossDim = cross;}
	public String getDatasetFilename() { return trainDataFilename; }
	public void setDatasetFilename(String s) { 
	    trainData = safeToDataset(s, false, multi); 
	    trainDataFilename=s; 
	}
	public ClassifyCommandLineUtil.Learner.ClassifierLnr getLearnerParameters() { return clsLnr; }
	public void setLearnerParameters(ClassifyCommandLineUtil.Learner.ClassifierLnr learn) { 
	    clsLnr = learn;
	    lnr = clsLnr; 
	}
	//help buttons
	public String getMultiHelp() { return "The number of labels per example, or dimensions, in the dataset"; }
	public String getCrosshelp() { return "Adds the predictions for each label type to the data as extra features"; }
	public String getDatasetFilenameHelp() { return "The relative path and name of the file that contains the dataset"; }
	public String getLearnerParametersHelp() { return "Defines the learner to use on the trainingData.\n  This is a multi experiment, so a Classifier Learner will be used."; }

    }

        /** Paramaters for training with a simple dataset */
    public static class SeqTrainParams extends TrainParams {
	public SeqTrainParams() {
	    typeString = "seq";
	}
	public void data(String s) { 
	    trainData = safeToDataset(s, true, -1);  //creates a simple dataset
	    trainDataFilename = s; 
	}
	
	public void learner(String s) {
	    seqLnr.seqLearner = toSeqLearner(s);
	    lnr = seqLnr;
	}

	//for gui
	public String getDatasetFilename() { return trainDataFilename; }
	public void setDatasetFilename(String s) { 
	    trainData = safeToDataset(s, true, -1); 
	    trainDataFilename=s; 
	}
	public ClassifyCommandLineUtil.Learner.SequentialLnr getLearnerParameters() { return seqLnr; }
	public void setLearnerParameters(ClassifyCommandLineUtil.Learner.SequentialLnr learn) { 
	    seqLnr = learn;
	    lnr = seqLnr; 
	}
	//help buttons
	public String getDatasetFilenameHelp() { return "The relative path and name of the file that contains the dataset"; }
	public String getLearnerParametersHelp() { return "Defines the learner to use on the trainingData.\n  This is a sequential experiment, so a Sequential Learner will be used."; }
    }

        /** Paramters for Test Classifier */ 
    public static class TestParams extends BaseParams {
	public Dataset testData=null;
	public String testDataFilename=null;
	public File loadFrom=null;
	public String loadFromFilename=null;
	public Object resultToShow=null, resultToSave=null;
	public ClassifyCommandLineUtil.BaseParams base;
	public Splitter splitter = new RandomSplitter(.7);
	// For Multi label experiment - to create the correct type of dataset in train and test
	public int multi=-1;
	public boolean crossDim = false;
	public String typeString = "simple";
	public static TestParams type = new SimpleTestParams();
	
	
	public TestParams() { super(); }

	public void type(String s) { 
	    typeString = s;
	    if(s.equalsIgnoreCase("simple")) 
		type = new SimpleTestParams();
	    else if (s.equalsIgnoreCase("seq")) 
		type = new SeqTestParams();
	    else if (s.equalsIgnoreCase("multi"))
		type = new MultiTestParams();
	    else {
		System.out.println("WARN: type " + s + " is not a valid option, will use simple");
		typeString = "simple";
	    }
	}

	public void multi(String dim) {
	    if (typeString.equalsIgnoreCase("multi")) {
		type.multi(dim);
		this.multi = type.multi;
	    }else System.out.println("WARN: attempting to define mutli on a " + typeString + 
				    " experiment \n      Must define -type multi to use this option");
	}
	public void cross() {
	    if (typeString.equalsIgnoreCase("multi")) {
		type.cross();
		this.crossDim = type.crossDim;
	    }else System.out.println("WARN: attempting to define cross on a " + typeString + 
				    " experiment \n      Must define -type mutlit to use this option");
	}

	public void classifierFile(String s) { 
	    loadFrom = new File(s); 
	    loadFromFilename=s; 
	    type.loadFrom = this.loadFrom;
	    type.loadFromFilename = this.loadFromFilename;
	}	
	public void loadFrom(String s) { 
	    loadFrom = new File(s); 
	    loadFromFilename=s; 
	    type.loadFrom = this.loadFrom;
	    type.loadFromFilename = this.loadFromFilename;
	}
	public void test(String s) {  
	    type.test(s);
	    this.testData = type.testData;
	    this.testDataFilename = type.testDataFilename;
	}
	public void splitter(String s) { 
	    splitter = Expt.toSplitter(s); 
	    type.splitter = this.splitter;
	}
	public void saveAs(String s) { 
	    saveAs = new File(s); 
	    saveAsFilename=s; 
	    type.saveAs = this.saveAs;
	    type.saveAsFilename = this.saveAsFilename;
	}
	public void showData() { 
	    showData=true; 
	    type.showData = this.showData;
	}
	public void showResult() { 
	    showResult=true; 
	    type.showResult = this.showResult;
	}
	public void showTestDetails() { 
	    showTestDetails=true; 
	    type.showTestDetails = this.showTestDetails;
	}

	//for gui
	public Splitter getSplitter() { return splitter; }
	public void setSplitter(Splitter s) { splitter=s; type.splitter=this.splitter; }
	public String getClassifierFilename() { return loadFromFilename; }
	public void setClassifierFilename(String s) { 
	    loadFromFilename=s; loadFrom=new File(s);
	    type.loadFromFilename = this.loadFromFilename;  type.loadFrom = this.loadFrom;  
	}

	//help buttons
	public String getMultiHelp() { return "The number of labels per example, or dimensions, in the dataset"; }
	public String getCrosshelp() { return "Adds the predictions for each label type to the data as extra features"; }
    }

    public static class SimpleTestParams extends TestParams {
	public SimpleTestParams() {
	    typeString = "simple";
	}
	public void test(String s) {  
	    testData = safeToDataset(s, false, -1);  //creates a simple dataset
	    testDataFilename = s;
	    Iterator it; 
	    it = testData.iterator();
	    splitter = new FixedTestSetSplitter(it);
	}

	//for gui
	public String getTestsetFilename() { return testDataFilename; }
	public void setTestsetFilename(String s) { testData = safeToDataset(s, false, -1); testDataFilename=s; }
    }

    public static class MultiTestParams extends TestParams {
	public MultiTestParams() {
	    typeString = "multi";
	}
	public void test(String s) {  
	    testData = safeToDataset(s, false, multi);  //creates a multi dataset
	    testDataFilename = s;
	    Iterator it; 
	    it = ((MultiDataset)testData).multiIterator();
	    splitter = new FixedTestSetSplitter(it);
	}
	public void multi(String dim) {
	    multi = new Integer(dim).intValue();
	}
	
	public void cross() {
	    if(multi <0)
		System.out.println("Warning: Cannot use crossdimensional classification without multiLabels!");
	    crossDim = true;
	}

	//for gui
	public int getMulti() { return multi;}
	public void setMulti(int multi) {this.multi = multi;}
	public boolean getCross() {return crossDim;}
	public void setCross(boolean cross) {this.crossDim = cross;}
	public String getTestsetFilename() { return testDataFilename; }
	public void setTestsetFilename(String s) { testData = safeToDataset(s, false, multi); testDataFilename=s; }
    }

    public static class SeqTestParams extends TestParams {
	public SeqTestParams() {
	    typeString = "seq";
	}
	public void test(String s) {  
	    //testData = safeToDataset(s, false, -1);  creates a simple dataset
            testData = safeToDataset(s, true, -1);  //creates a sequential dataset
	    testDataFilename = s;
	    Iterator it; 
	    it = ((SequenceDataset)testData).sequenceIterator();
	    splitter = new FixedTestSetSplitter(it);
	}

	//for gui
	public String getTestsetFilename() { return testDataFilename; }
	public void setTestsetFilename(String s) { testData = safeToDataset(s, true, -1); testDataFilename=s; }
    }

    /** Paramters for TrainTest Classifier.
     *  These parameters are only used for the Command Line.  They interact with Simple, Multi, and SeqTrainTestParams 
     *  once type is defined.  NOTE: type must be defined first in order for command line to work properly.  Default type
     *  is simple.  There are no GUI parameters because this is never used for the gui.
     */ 
    public static class TrainTestParams extends TrainParams {
	public Dataset testData;
	public String testDataFilename;
	public Object resultToShow=null, resultToSave=null;
	public ClassifyCommandLineUtil.BaseParams base;
	public Splitter splitter = new RandomSplitter(.7);
	public static TrainTestParams type=new SimpleTrainTestParams();
	public ClassifyCommandLineUtil.Learner.SequentialLnr seqLnr = new ClassifyCommandLineUtil.Learner.SequentialLnr();
	public ClassifyCommandLineUtil.Learner.ClassifierLnr clsLnr = new ClassifyCommandLineUtil.Learner.ClassifierLnr();
	public ClassifyCommandLineUtil.Learner lnr = clsLnr;  
	public String typeString = "simple";
	
	public TrainTestParams() { super(); }

	public void type(String s) { 
	    typeString = s;
	    System.out.println("Defining Type: " + s);
	    if(s.equalsIgnoreCase("simple")) 
		type = new SimpleTrainTestParams();
	    else if (s.equalsIgnoreCase("seq")) {
		type = new SeqTrainTestParams();
	    } else if (s.equalsIgnoreCase("multi"))
		type = new MultiTrainTestParams();
	    else {
		System.out.println("WARN: type " + s + " is not a valid option, will use simple");
		typeString = "simple";
	    }
	}

	public Dataset getTrainData() { return trainData; }

	public void data(String s) { 
	    type.data(s); 
	    this.trainData = type.trainData;
	    this.trainDataFilename = type.trainDataFilename;
	}
	public void test(String s) {  
	    type.test(s);
	    this.testData = type.testData;
	    this.testDataFilename = type.testDataFilename;
	    Iterator it; 
	    it = testData.iterator();
	    splitter = new FixedTestSetSplitter(it);
	}
	public void splitter(String s) { 
	    splitter = Expt.toSplitter(s); 
	    type.splitter = this.splitter;
	}
	public void learner(String s) {
	    type.learner(s); 
	    this.clsLnr.clsLearner = type.clsLnr.clsLearner;
	    this.seqLnr.seqLearner = type.seqLnr.seqLearner;
	    this.lnr = type.lnr;
	}
	//For multi
	public void multi(String dim) {
	    if (typeString.equalsIgnoreCase("multi")) {
		type.multi(dim);
		this.multi = type.multi;
	    }else System.out.println("WARN: attempting to define mutli on a " + typeString + 
				    " experiment \n      Must define -type multi to use this option");
	}
	public void cross() {
	    if (typeString.equalsIgnoreCase("multi")) {
		type.cross();
		this.crossDim = type.crossDim;
	    }else System.out.println("WARN: attempting to define cross on a " + typeString + 
				    " experiment \n      Must define -type mutlit to use this option");
	}
	public void saveAs(String s) { 
	    saveAs = new File(s); 
	    saveAsFilename=s; 
	    type.saveAs = this.saveAs;
	    type.saveAsFilename = this.saveAsFilename;
	}
	public void showData() { 
	    showData=true; 
	    type.showData = this.showData;
	}
	public void showResult() { 
	    showResult=true; 
	    type.showResult = this.showResult;
	}
	public void showTestDetails() { 
	    showTestDetails=true; 
	    type.showTestDetails = this.showTestDetails;
	}
	
    }

    /**  Specific TrainTestParameters for Simple/Standard mode.  Called from TrainTestParams to specify specific data, learner, etc.
     *   Used for GUI.
     */
    public static class SimpleTrainTestParams extends TrainTestParams {
	public SimpleTrainTestParams() {
	    typeString = "simple";
	}
	public void data(String s) { 
	    trainData = safeToDataset(s, false, -1);  //creates a simple dataset
	    trainDataFilename = s; 
	}

	public void test(String s) {  
	    testData = safeToDataset(s, false, -1);  //creates a simple dataset
	    testDataFilename = s;
	    Iterator it; 
	    it = testData.iterator();
	    splitter = new FixedTestSetSplitter(it);
	}		

	public void learner(String s) {
	    clsLnr.clsLearner = Expt.toLearner(s); 
	    lnr = clsLnr;
	}

	//for gui
	public String getDatasetFilename() { return trainDataFilename; }
	public void setDatasetFilename(String s) { 
	    trainData = safeToDataset(s, false, -1); 
	    trainDataFilename=s; 
	}
	public String getTestsetFilename() { return testDataFilename; }
	public void setTestsetFilename(String s) { testData = safeToDataset(s, false, -1); testDataFilename=s; }

	public ClassifyCommandLineUtil.Learner.ClassifierLnr getLearnerParameters() { return clsLnr; }
	public void setLearnerParameters(ClassifyCommandLineUtil.Learner.ClassifierLnr learn) { 
	    clsLnr = learn;
	    lnr = clsLnr; 
	}
	public Splitter getSplitter() { return splitter; }
	public void setSplitter(Splitter s) { splitter=s; }
	//Help Buttons
	public String getDatasetFilenameHelp() { return "The relative path and name of the file that contains the dataset"; }
	public String getTestsetFilenameHelp() { return "The realative path and name of the test dataset.\nSplitter will not be used if this option is defined"; }
	public String getLearnerParametersHelp() { return "Defines the learner to use on the trainingData.\n  This is a simple experiment, so a Classifier Learner will be used."; }
	public String getSplitterHelp() { return "The splitter that defines how the training and test set will be split."; }
    }

    /**  Specific TrainTestParameters for Multi mode.  Called from TrainTestParams to specify specific data, learner, etc.
     *   Used for GUI.
     */
    public static class MultiTrainTestParams extends TrainTestParams {
	public MultiTrainTestParams() {
	    typeString = "multi";
	}
	public void data(String s) { 
	    trainData = safeToDataset(s, false, multi);  //creates a multi dataset
	    trainDataFilename = s; 
	}

	public void test(String s) {  
	    testData = safeToDataset(s, false, multi);  //creates a multi dataset
	    testDataFilename = s;
	    Iterator it; 
	    it = ((MultiDataset)testData).multiIterator();
	    splitter = new FixedTestSetSplitter(it);
	}

	public void learner(String s) {
	    clsLnr.clsLearner = Expt.toLearner(s); 
	    lnr = clsLnr;
	}
	public void multi(String dim) {
	    multi = new Integer(dim).intValue();
	}
	
	public void cross() {
	    if(multi <0)
		System.out.println("Warning: Cannot use crossdimensional classification without multiLabels!");
	    crossDim = true;
	}

	//for gui
	public String getDatasetFilename() { return trainDataFilename; }
	public void setDatasetFilename(String s) { 
	    trainData = safeToDataset(s, false, multi); 
	    trainDataFilename=s; 
	}
	public String getTestsetFilename() { return testDataFilename; }
	public void setTestsetFilename(String s) { testData = safeToDataset(s, false, multi); testDataFilename=s; }
	public void setLearnerParameters(ClassifyCommandLineUtil.Learner.ClassifierLnr learn) { 
	    clsLnr = learn;
	    lnr = clsLnr; 
	}
	public int getMulti() { return multi;}
	public void setMulti(int multi) {this.multi = multi;}
	public boolean getCross() {return crossDim;}
	public void setCross(boolean cross) {this.crossDim = cross;}
	public Splitter getSplitter() { return splitter; }
	public void setSplitter(Splitter s) { splitter=s; }

	// Help Buttons
	public String getMultiHelp() { return "The number of labels per example, or dimensions, in the dataset"; }
	public String getCrosshelp() { return "Adds the predictions for each label type to the data as extra features"; }
	public String getDatasetFilenameHelp() { return "The relative path and name of the file that contains the dataset"; }
	public String getTestsetFilenameHelp() { return "The realative path and name of the test dataset.\nSplitter will not be used if this option is defined"; }
	public String getLearnerParametersHelp() { return "Defines the learner to use on the trainingData.\n  This is a multi experiment, so a Classifier Learner will be used."; }
	public String getSplitterHelp() { return "The splitter that defines how the training and test set will be split."; }
    }

    /**  Specific TrainTestParameters for Sequential mode.  Called from TrainTestParams to specify specific data, learner, etc.
     *   Used for GUI.
     */
    public static class SeqTrainTestParams extends TrainTestParams {
	public SeqTrainTestParams() {
	    typeString = "seq";
	}

	public void data(String s) { 
	    trainData = safeToDataset(s, true, -1);  //creates a seq dataset
	    trainDataFilename = s; 
	}

	public void test(String s) {  
	    testData = safeToDataset(s, true, -1);  //creates a seq dataset
	    testDataFilename = s;
	    Iterator it; 
	    it = ((SequenceDataset)testData).sequenceIterator();
	    splitter = new FixedTestSetSplitter(it);
	}
	
	public void learner(String s) {
	    seqLnr.seqLearner = toSeqLearner(s);
	    lnr = seqLnr;
	}

	//for gui
	public String getDatasetFilename() { return trainDataFilename; }
	public void setDatasetFilename(String s) { 
	    trainData = safeToDataset(s, true, -1); 
	    trainDataFilename=s; 
	}
	public String getTestsetFilename() { return testDataFilename; }
	public void setTestsetFilename(String s) { testData = safeToDataset(s, true, -1); testDataFilename=s; }
	public ClassifyCommandLineUtil.Learner.SequentialLnr getLearnerParameters() { return seqLnr; }
	public void setLearnerParameters(ClassifyCommandLineUtil.Learner.SequentialLnr learn) { 
	    seqLnr = learn;
	    lnr = seqLnr; 
	}
	public Splitter getSplitter() { return splitter; }
	public void setSplitter(Splitter s) { splitter=s; }

	//Help Buttons
	public String getDatasetFilenameHelp() { return "The relative path and name of the file that contains the dataset"; }
	public String getTestsetFilenameHelp() { return "The realative path and name of the test dataset." + 
		"\nSplitter will not be used if this option is defined"; }
	public String getLearnerParametersHelp() { return "Defines the learner to use on the trainingData.\n  This is a sequential " +
		"experiment, so a Sequential Learner will be used."; }
	public String getSplitterHelp() { return "The splitter that defines how the training and test set will be split."; }
    }

}
