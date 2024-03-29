/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.multi;

import edu.cmu.minorthird.classify.*;

import java.util.*;
import java.io.*;

/**
 * An inverted index, mapping features to examples which contain the
 * features.
 *
 * @author Cameron Williams
 */

public class MultiDatasetIndex implements Serializable
{
	static private final long serialVersionUID = 1;
	private final int CURRENT_VERSION_NUMBER = 1;

	private TreeMap indexByFeature;
	private TreeMap indexByClass;
	private int sumFeatureValues;
	private int exampleCount;

	public MultiDatasetIndex()
	{
		indexByFeature = new TreeMap();
		indexByClass = new TreeMap();
		sumFeatureValues = 0;
	}

	/** Construct an index of a dataset. */
	public MultiDatasetIndex(MultiDataset data)
	{
		this();
		for (MultiExample.Looper i=data.multiIterator(); i.hasNext(); ) {
			addMultiExample(i.nextMultiExample());
		}
	}


	/** Add a single example to the index. */
	public void addMultiExample(MultiExample e)
	{
	    classIndex(e.getMultiLabel().bestClassName().toString()).add(e);
		for (Feature.Looper j=e.featureIterator(); j.hasNext(); ) {
			Feature f = j.nextFeature();
			featureIndex(f).add(e);
			sumFeatureValues++;
		}
		exampleCount++;
	}

	/** Iterate over all features indexed. */
	public Feature.Looper featureIterator()
	{
		return new Feature.Looper(indexByFeature.keySet());
	}

	/** Number of examples containing non-zero values for feature f. */
	public int size(Feature f)
	{
		return featureIndex(f).size();
	}

	/** Number of examples with the given class label. */
	public int size(String label)
	{
		return classIndex(label).size();
	}

	/** Get i-th example containing feature f. */
	public MultiExample getMultiExample(Feature f,int i)
	{
		return (MultiExample) featureIndex(f).get(i);
	}

	/** Get i-th example with given class label. */
	public MultiExample getMultiExample(String label,int i)
	{
		return (MultiExample) classIndex(label).get(i);
	}

	/** Get all examples with a feature in common with the given instance. */
	public MultiExample.Looper getNeighbors(Instance instance)
	{
		HashSet set = new HashSet();
		for (Feature.Looper i=instance.featureIterator(); i.hasNext(); ) {
			Feature f = i.nextFeature();
			for (Iterator j=featureIndex(f).iterator(); j.hasNext();  ) {
				MultiExample e = (MultiExample)j.next();
				set.add( e );
			}
		}
		return new MultiExample.Looper(set);
	}

	//
	// statistics about the dataset
	//

	/** Number of features indexed. */
	public int numberOfFeatures() { return indexByFeature.keySet().size(); }

	/** Average number of non-zero feature values in examples. */
	public double averageFeaturesPerExample() { return sumFeatureValues/((double)exampleCount); }

	//
	// subroutines
	//

	protected List featureIndex(Feature f)
	{
		List result = (List) indexByFeature.get(f);
		if (result==null) indexByFeature.put( f, (result=new ArrayList()) );
		return result;
	}

	protected List classIndex(String lab)
	{
		List result = (List) indexByClass.get(lab);
		if (result==null) indexByClass.put( lab, (result=new ArrayList()) );
		return result;
	}

	public String toString()
	{
		StringBuffer buf = new StringBuffer("[index");
		for (Feature.Looper i=featureIterator(); i.hasNext(); ) {
			Feature f = i.nextFeature();
			buf.append("\n"+f+":");
			for (int j=0; j<size(f); j++) {
				buf.append("\n\t"+getMultiExample(f,j).toString());
			}
		}
		for (Iterator i=indexByClass.keySet().iterator(); i.hasNext(); ) {
			String label = (String)i.next();
			buf.append("\n"+label+":");
			for (int j=0; j<size(label); j++) {
				buf.append("\n\t"+getMultiExample(label,j).toString());
			}
		}
		buf.append("\nindex]");
		return buf.toString();
	}

	//
	// main
	//

	static public void main(String[] args)
	{
	    System.out.println("MultiDatasetIndex");
	}
}
