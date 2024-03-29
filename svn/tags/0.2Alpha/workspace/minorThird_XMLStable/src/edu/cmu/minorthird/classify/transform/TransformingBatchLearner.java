/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.transform;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.multi.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.gui.*;

import java.util.*;

/**
 * Learns to first transforming data with an InstanceTransform, then
 * classify it.
 *
 * @author William Cohen
 */

public class TransformingBatchLearner extends BatchClassifierLearner
{
	private InstanceTransformLearner transformLearner;
	private BatchClassifierLearner classifierLearner;

	public TransformingBatchLearner()
	{
		this(new FrequencyBasedTransformLearner(3), new MaxEntLearner());
	}

	public void setTransformLearner(InstanceTransformLearner learner) { transformLearner=learner; }
	public InstanceTransformLearner getTransformLearner() { return transformLearner; }

	public void setClassifierLearner(BatchClassifierLearner learner) { classifierLearner=learner; }
	public BatchClassifierLearner getClassifierLearner() { return classifierLearner; }

	public void setSchema(ExampleSchema schema)
	{
		classifierLearner.setSchema(schema);
	}

	public TransformingBatchLearner(
		InstanceTransformLearner transformLearner,
		BatchClassifierLearner classifierLearner)
	{
		this.transformLearner = transformLearner;
		this.classifierLearner = classifierLearner;
	}

	public Classifier batchTrain(Dataset dataset)
	{
		final InstanceTransform transformer = transformLearner.batchTrain(dataset);
		final Classifier classifier = classifierLearner.batchTrain(transformer.transform(dataset));
		//ViewerFrame f1 = new ViewerFrame("classifier", new SmartVanillaViewer(classifier));
		//ViewerFrame f2 = new ViewerFrame("dataset", new SmartVanillaViewer(transformer.transform(dataset)));

		return new TransformingClassifier(classifier,transformer);
	}
}
