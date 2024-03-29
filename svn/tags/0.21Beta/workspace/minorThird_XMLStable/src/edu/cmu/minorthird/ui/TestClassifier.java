package edu.cmu.minorthird.ui;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.util.*;

import org.apache.log4j.Logger;
import java.util.*;
import java.io.*;


/**
 * Test an existing text classifier.
 *
 * @author William Cohen
 */

public class TestClassifier extends UIMain
{
  private static Logger log = Logger.getLogger(TestClassifier.class);

	// private data needed to test a classifier

	private CommandLineUtil.SaveParams save = new CommandLineUtil.SaveParams();
	private CommandLineUtil.ClassificationSignalParams signal = new CommandLineUtil.ClassificationSignalParams(base);
	private CommandLineUtil.TestClassifierParams test = new CommandLineUtil.TestClassifierParams();
	private Object result = null;

	// for gui
	public CommandLineUtil.SaveParams getSaveParameters() { return save; }
	public void setSaveParameters(CommandLineUtil.SaveParams p) { save=p; }
	public CommandLineUtil.ClassificationSignalParams getSignalParameters() { return signal; } 
	public void setSignalParameters(CommandLineUtil.ClassificationSignalParams p) { signal=p; } 
	public CommandLineUtil.TestClassifierParams getAdditionalParameters() { return test; } 
	public void setAdditionalParameters(CommandLineUtil.TestClassifierParams p) { test=p; } 

    public String getTestClassifierHelp() {
	return "<A HREF=\"http://minorthird.sourceforge.net/tutorials/TestClassifier%20Tutorial.htm\">TestClassifier Tutorial</A></html>";
    }

	public CommandLineProcessor getCLP()
	{
		return new JointCommandLineProcessor(new CommandLineProcessor[]{gui,base,save,signal,test});
	}

	//
	// load and test a classifier
	// 

	public void doMain()
	{
		// check that inputs are valid
		if (test.loadFrom==null) throw new IllegalArgumentException("-loadFrom must be specified");

		// load the classifier
		ClassifierAnnotator ann = null;
		try {
			ann = (ClassifierAnnotator)IOUtil.loadSerialized(test.loadFrom);
		} catch (IOException ex) {
			throw new IllegalArgumentException("can't load annotator from "+test.loadFrom+": "+ex);
		}

		// do the testing and show the result
		Dataset d = 
			CommandLineUtil.toDataset(base.labels,ann.getFE(),signal.spanProp,signal.spanType,signal.candidateType);
		Evaluation evaluation = null;
		if (test.showData) new ViewerFrame("Dataset", d.toGUI());
		if (test.showClassifier) new ViewerFrame("Classifier", new SmartVanillaViewer(ann.getClassifier()));
		evaluation = new Evaluation(d.getSchema());
		evaluation.extend( ann.getClassifier(), d, 0 );
		if (test.showTestDetails) {
			result = new ClassifiedDataset(ann.getClassifier(), d);
		} else {
			result = evaluation;
		}

		if (base.showResult) {
			new ViewerFrame("Result", new SmartVanillaViewer(result));
		}

		if (save.saveAs!=null) {
			try {
				IOUtil.saveSerialized((Serializable)evaluation,save.saveAs);
			} catch (IOException e) {
				throw new IllegalArgumentException("can't save to "+save.saveAs+": "+e);
			}
		}
		evaluation.summarize();
	}

	public Object getMainResult() { return result; }

	public static void main(String args[])
	{
		 new TestClassifier().callMain(args);
	}
}
