package edu.cmu.minorthird.ui;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.classify.sequential.*;

/** 
 * Defines the list of classes that can be selected by an instance of UIMain. 
 */

/*package*/ class InLineSelectableTypes
{			
    public static final String[] CLASSES = new String[]
	{
	    //
	    // bunches of parameters
	    //
	    "baseParameters", "onlineBaseParameters", "saveParameters", 
	    /*"classificationSignalParameters",*/ "trainClassifierParameters", 
	    "testClassifierParameters", "testExtractorParameters", 
	    "loadAnnotatorParameters", "splitterParameters",
	    /*"extractionSignalParameters",*/ "trainExtractorParameters",
	    "testClassifierParameters", "trainTaggerParameters",
	    "taggerSignalParameters", "mixupParameters",
	    "onlineSignalParameters", "multiClassificationSignalParameters",	  
	    /*"signalParameters",*/ "trainingParameters",
	    "additionalParameters", "editParameters", "annotatorOutputParams",
	    "_LabeledDataParameters", "textLearnerParameters"
	  
	};
}
