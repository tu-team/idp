/* Copyright 2006, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.ranking;

import java.util.*;
import java.io.*;
import java.text.DecimalFormat;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.*;

import javax.swing.*;
import java.awt.*;


/** Evaluate a classifier as a ranker
 */

public class RankingEvaluation implements Visible, Saveable
{
    private final static int GRAPHS_PER_PAGE = 10;
    private final static int NUM_TOP_TO_SHOW = 50;
    private TreeMap rankedListMap = new TreeMap();
    private TreeMap unrankedMap = new TreeMap();
    private TreeMap scoreMap = new TreeMap();
    private TreeMap numPosExamples = new TreeMap();
    private boolean guiFlag = false;
    private String loadedFile = null;

    public void extend(String rankingId, ArrayList ranking, BinaryClassifier classifier)
    {
	extend(rankingId,ranking,classifier,Collections.EMPTY_SET);
    }

    public void extend(String rankingId, ArrayList ranking, BinaryClassifier classifier, Set unrankedPos)
    {
	BatchRankingLearner.sortByScore( classifier, ranking );
	rankedListMap.put( rankingId, ranking );
	double[] scores = new double[ranking.size()];
	int k=0;
	for (Iterator i=ranking.iterator(); i.hasNext(); ) {
	    Example ex = (Example)i.next();
	    if (ex.getLabel().isPositive()) increment(numPosExamples,rankingId,1);
	    scores[k++] = classifier.score(ex);
	}
	scoreMap.put(rankingId,scores);
	unrankedMap.put(rankingId,unrankedPos);
	increment(numPosExamples,rankingId,unrankedPos.size());
    }

    private void increment(TreeMap map,String key,int delta)
    {
	Integer i = (Integer)map.get(key);
	if (i==null) map.put(key,new Integer(delta));
	else map.put(key,new Integer(i.intValue()+delta));
    }

    //
    // accessors
    //

    private ArrayList getRanking(String rankingId) { return (ArrayList)rankedListMap.get(rankingId); }
    private double getScore(String rankingId,int rank) { return ((double[])scoreMap.get(rankingId))[rank-1]; }
    private Iterator getRankingIterator() { return rankedListMap.keySet().iterator(); }
    private int numPosExamples(String rankingId) {return ((Integer)numPosExamples.get(rankingId)).intValue();}
    private boolean isPositive(String rankingId,Example ex) { return ex.getLabel().isPositive(); }
    private int numRankings() { return rankedListMap.keySet().size(); }
    private Set getUnrankedPositives(String rankingId) { return Collections.EMPTY_SET; }

    //
    // split the examples into groups of K
    //
    private String[][] exampleGroups(int groupSize)
    {
	int remainder = numRankings() % groupSize;
	int numRemainderGroups = remainder>0 ? 1 : 0;
	String[][] group = new String[(numRankings()/groupSize) + numRemainderGroups][];
	for (int i=0; i<group.length-numRemainderGroups; i++) {
	    group[i] = new String[groupSize];
	}
	if (numRemainderGroups>0) {
	    group[group.length-1] = new String[ remainder ];
	}
	int j=0, k=0;
	for (Iterator i=getRankingIterator(); i.hasNext(); ) {
	    String name = (String)i.next();
	    group[j][k++] = name;
	    if (k>=group[j].length) { 
		j++;
		k=0;
	    }
	}
	return group;
    }

    //
    // useful subroutine - returns an array a such that a[0] is recall
    // at each rank, and a[1] is precision at each rank.
    //
    private double[][] recallAndPrecisionForEachK(String rankingId)
    {
	ArrayList ranking = getRanking(rankingId);
	int totalPos = numPosExamples(rankingId);
	double[] recall = new double[ranking.size()+1];
	double[] precision = new double[ranking.size()+1];
	int rank=0; 
	double numPosAboveRank=0;
	for (Iterator j=ranking.iterator(); j.hasNext(); ) {
	    Example ex = (Example)j.next();
	    rank++;
	    if (isPositive(rankingId,ex))  numPosAboveRank++;
	    if (totalPos>0) {
		recall[rank] = numPosAboveRank/totalPos; 
		precision[rank] = numPosAboveRank/rank;
	    } else {
		recall[rank] = precision[rank] = 1.0;
	    }
	}
	double[][] result = new double[2][];
	result[0] = recall;
	result[1] = precision;
	return result;
    }


    //
    // public functions
    //

    /** Non-interpolated average precision.
     */
    public double averagePrecision(String rankingId)
    {
	if (numPosExamples(rankingId)==0) return 1.0;
	double rank = 0, numPosAboveRank = 0, totPrec = 0;
	ArrayList ranking = getRanking(rankingId);
	for (Iterator i=ranking.iterator(); i.hasNext(); ) {
	    Example ex = (Example)i.next();
	    rank++;
	    if (isPositive(rankingId,ex)) {
		numPosAboveRank++;
		totPrec += numPosAboveRank/rank;
	    }
	}
	return totPrec/numPosExamples(rankingId);
    }

    /** Max value of F1 over all possible thresholds.
     */
    public double maxF1(String rankingId)
    {
	if (numPosExamples(rankingId)==0) return 1.0;
	double rank=0, numPosAboveRank=0, maxF1=0;
	ArrayList ranking = getRanking(rankingId);
	for (Iterator i=ranking.iterator(); i.hasNext(); ) {
	    Example id = (Example)i.next();
	    rank++;
	    if (isPositive(rankingId,id)) {
		numPosAboveRank++;
	    }
	    double precision = numPosAboveRank/rank;
	    double recall = numPosAboveRank/numPosExamples(rankingId);
	    if (precision+recall>0) {
		double f1 = 2*precision*recall/(precision+recall);
		maxF1 = Math.max( maxF1, f1 );
	    }
	}
	return maxF1;
    }

    public double maxRecall(String rankingId)
    {
	if (numPosExamples(rankingId)==0) return 1.0;
	double numRanked = 0;
	ArrayList ranking = getRanking(rankingId);
	for (Iterator i=ranking.iterator(); i.hasNext(); ) {
	    Example ex = (Example)i.next();
	    if (ex.getLabel().isPositive()) numRanked++;
	}
	return numRanked/numPosExamples(rankingId);
    }

    /** Interpolated precision at eleven recall levels, averaged over all examples. */
    public double[] averageElevenPointPrecision()
    {
	double[] averagePrecision = new double[11];
	for (Iterator i=getRankingIterator(); i.hasNext(); ) {
	    String name = (String)i.next();
	    double[] precision = elevenPointPrecision(name);
	    for (int j=0; j<=10; j++) {
		averagePrecision[j] += precision[j];
	    }
	}
	for (int j=0; j<=10; j++) {
	    averagePrecision[j] /= numRankings();
	}
	return averagePrecision;
    }
    

    /** Interpolated precision at eleven recall levels: 0.0, ... ,1.0 */
    public double[] elevenPointPrecision(String rankingId)
    {
	double[][] a = recallAndPrecisionForEachK(rankingId);
	double[] recall = a[0];
	double[] precision = a[1];
	double[] interpolatedPrecision = new double[11];
	for (int k=1; k<recall.length; k++) {
	    double r = recall[k];
	    double p = precision[k];
	    for (int j=0; j<=10; j++) {
		if (r >= j/10.0) {
		    interpolatedPrecision[j] = Math.max( interpolatedPrecision[j], p );
		}
	    }
	}
	return interpolatedPrecision;
    }

    /** A summary table. Columns are: avgpr, the 
     * non-interpolated average precision of the ranking (the average
     * of this is thus mean average precision); maxF1, the maximum F1
     * value for the ranking; maxRec, the maximum recall achieved
     * (i.e., the fraction of relevant nodes appearing in the
     * ranking); and #pos, the number of positive/relevant nodes.
     */
    public String toTable()
    {
	if (rankedListMap.keySet().size()==0) {
	    return "no examples?\n";
	}
	StringBuffer buf = new StringBuffer();
	DecimalFormat fmt = new DecimalFormat("0.000");
	DecimalFormat fmt2 = new DecimalFormat("0.0");
	buf.append("avgPr\tmaxF1\tmaxRec\t#pos\n");
	double totMaxF1=0, totAvgPrec=0, totPos=0, totMaxRec=0;
	for (Iterator i=getRankingIterator(); i.hasNext(); ) {
	    String name = (String)i.next();
	    double ap = averagePrecision(name);
	    double maxf = maxF1(name);
	    double maxr = maxRecall(name);
	    int np = numPosExamples(name);
	    buf.append(fmt.format(ap) + "\t");
	    buf.append(fmt.format(maxf) + "\t");
	    buf.append(fmt.format(maxr) + "\t");
	    buf.append(np + "\t");
	    buf.append(name + "\n");
	    totAvgPrec += ap;
	    totMaxF1 += maxf;
	    totMaxRec += maxr;
	    totPos += np;
	}
	buf.append("\n");
	buf.append(fmt.format(totAvgPrec/numRankings())+"\t");	
	buf.append(fmt.format(totMaxF1/numRankings())+"\t");
	buf.append(fmt.format(totMaxRec/numRankings())+"\t");
	buf.append(fmt2.format(totPos/numRankings())+"\t");
	buf.append("average" + "\n");
	return buf.toString();
    }

    private double[] averageRecallAtEachK()
    {
	int longestRankedList = 0;
	for (Iterator i=getRankingIterator(); i.hasNext(); ) {
	    String name = (String)i.next();
	    longestRankedList = Math.max( getRanking(name).size(), longestRankedList );
	}
	// first have recall[k] be total recall over all examples at rank k
	double[] recall = new double[longestRankedList+1];
	for (Iterator i=getRankingIterator(); i.hasNext(); ) {
	    String name = (String)i.next();
	    ArrayList ranking = getRanking(name);
	    int rank=0; 
	    double numPosAboveRank=0;
	    for (Iterator j=ranking.iterator(); j.hasNext(); ) {
		Example id = (Example)j.next();
		rank++;
		if (isPositive(name,id))  numPosAboveRank++;
		if (numPosExamples(name)>0) {
		    recall[rank] += numPosAboveRank/numPosExamples(name);
		} else {
		    recall[rank] = 1.0;
		}
	    }
	    // extend the last recorded recall level to the end of this list
	    for (int k=rank+1; k<recall.length; k++) {
		recall[k] = recall[rank];
	    }
	}
	// now scale recall to average recall at K
	for (int k=1; k<recall.length; k++) {
	    recall[k] /= numRankings();
	}
	recall[0] = -1; // convenient
	return recall;
    }

    /** Recall as function of K, averaged over all examples. */
    public String averageRecallAsFunctionOfK()
    {
	DecimalFormat fmt = new DecimalFormat("0.000");
	StringBuffer buf = new StringBuffer("");
	buf.append("K\tAvgRecall\n");
	double[] recall = averageRecallAtEachK();
	for (int k=1; k<recall.length; k++) {
	    if (recall[k]!=recall[k-1]) {
		buf.append(k+"\t"+fmt.format(recall[k])+"\n");
	    }
	}
	return buf.toString();
    }


    public String toTable(String name,int numToShowAllEntries)
    {
	ArrayList ranking = getRanking(name);
	StringBuffer buf = new StringBuffer();
	DecimalFormat fmt = new DecimalFormat("0.000");
	int rank = 0;
	for (Iterator i=ranking.iterator(); i.hasNext(); ) {
	    Example id = (Example)i.next();
	    ++rank;
	    double score = getScore(name,rank);
	    String tag = isPositive(name,id) ? "+" : "-";
	    // print the entry if it's positive, or if it's near the top
	    if (rank<numToShowAllEntries || tag.startsWith("+")) {
		buf.append(rank+"\t"+fmt.format(score)+"\t"+tag+"\t"+id+"\n");
	    }
	}
	// now print the false negatives - ie the unranked positives
	for (Iterator i=getUnrankedPositives(name).iterator(); i.hasNext(); ) {
	    Example id = (Example)i.next();
	    String tag = "+";
	    buf.append(">"+rank+"\t0\t"+tag+"\t"+id+"\n");
	}
	return buf.toString();
	
    }

    public Viewer toGUI()
    {
	ParallelViewer v = new ParallelViewer();
	v.addSubView( "Summary Table", new ComponentViewer() {
		public JComponent componentFor(Object o) {
		    RankingEvaluation gsEval = (RankingEvaluation)o; 
		    return new VanillaViewer( gsEval.toTable() );
		}
	    });
	ParallelViewer v2 = new ParallelViewer();
	v.addSubView( "11-Pt Precision", v2 );
	v2.addSubView( "Averaged", new ComponentViewer() {
		public JComponent componentFor(Object o) {
		    RankingEvaluation gsEval = (RankingEvaluation)o; 
		    double[] avgPrec = gsEval.averageElevenPointPrecision();
		    LineCharter lc = new LineCharter();
		    lc.startCurve("11-Pt Avg Prec");
		    for (int j=0; j<=10; j++) {
			lc.addPoint( j/10.0, avgPrec[j] );
		    }
		    return lc.getPanel("11-Pt Average Interpolated Precision", "Recall", "Precision");
		}
	    });
	String[][] groups = exampleGroups(GRAPHS_PER_PAGE);
	for (int i=0; i<groups.length; i++) {
	    final String tag = groups.length==1 ? "Details" : ("Details: Group "+(i+1));
	    final String[] group = groups[i]; 
	    v2.addSubView( tag, new ComponentViewer() {
		    public JComponent componentFor(Object o) {
			RankingEvaluation gsEval = (RankingEvaluation)o; 
			LineCharter lc = new LineCharter();
			for (int i=0; i<group.length; i++) {
			    String name = group[i];
			    double[] avgPrec = gsEval.elevenPointPrecision(name);
			    lc.startCurve(name);
			    for (int j=0; j<=10; j++) {
				lc.addPoint( j/10.0, avgPrec[j] );
			    }
			}
			return lc.getPanel("11-Pt Interpolated Precision", "Recall", "Precision");
		    }
		});

	}
	v.addSubView( "AvgRecall vs Rank", new ComponentViewer() {
		public JComponent componentFor(Object o) {
		    RankingEvaluation gsEval = (RankingEvaluation)o; 
		    double[] avgRec = averageRecallAtEachK();
		    LineCharter lc = new LineCharter();
		    lc.startCurve("Recall vs Rank");
		    for (int i=1; i<avgRec.length; i++) {
			lc.addPoint( i, avgRec[i] );
		    }
		    return lc.getPanel("AvgRecall vs Rank", "Rank", "AvgRecall");
		}
	    });
	ParallelViewer v3 = new ParallelViewer();
	v3.putTabsOnLeft();
	v.addSubView( "Details", v3 );
	for (Iterator i=getRankingIterator(); i.hasNext(); ) {
	    final String name = (String)i.next();
	    v3.addSubView( name, new ComponentViewer() {
		    public JComponent componentFor(Object o) {
			return new VanillaViewer( toTable(name,NUM_TOP_TO_SHOW) );
		    }
		});
	}
	v.setContent(this);
	return v;
    }

    //
    // implement Saveable
    //
    final static private String EVAL_FORMAT_NAME = "Graph Searcher Evaluation";
    final static private String EVAL_EXT = ".gsev";
    public String[] getFormatNames() { return new String[]{EVAL_FORMAT_NAME}; }
    public String getExtensionFor(String format) { return EVAL_EXT; }
    public void saveAs(File file,String formatName) throws IOException { save(file);	}
    public Object restore(File file) throws IOException	{ return load(file); }

    final static private StringEncoder encoder = new StringEncoder('%',"/\\:;$ \t\n");
    final static private String evalExt = Evaluation.EVAL_EXT;
	private static final String defaultEncoding = "UTF8";

    private void save(File file) throws IOException 
    {
    	// save in default encoding utf8
	//PrintStream out = new PrintStream(new FileOutputStream(file));
    PrintStream out = new PrintStream(new FileOutputStream(file), false, defaultEncoding);
	for (Iterator i=getRankingIterator(); i.hasNext(); ) {
	    String name = (String)i.next();
	    ArrayList ranking = getRanking(name);
	    int rank = 0;
	    for (Iterator j=ranking.iterator(); j.hasNext(); ) {
		Example id = (Example)j.next();
		rank++;
		double weight = getScore(name,rank);
		out.println(name +"\t"+ id.getSource() +"\t"+ rank +"\t" + weight);
	    }
	    for (Iterator j=ranking.iterator(); j.hasNext(); ) {
		Example id = (Example)j.next();
		if (isPositive(name,id)) {
		    out.println(name +"\t" + id.getSource());
		}
	    }
	    Set pos = getUnrankedPositives(name);
	    for (Iterator j=pos.iterator(); j.hasNext(); ) {
		Example id = (Example)j.next();
		out.println(name +"\t" + id.getSource());
	    }
	}
	out.close();
    }

    static public RankingEvaluation load(File file) throws IOException
    {
	RankingEvaluation eval = new RankingEvaluation();
	eval.loadFromFile(file);
	return eval;
    }

    private void loadFromFile(File file) throws IOException 
    { 
	LineNumberReader in = new LineNumberReader(new InputStreamReader(new FileInputStream(file)));
	String line = null;
	while ((line = in.readLine())!=null) {
	    String[] parts = line.split("\t");
	    if (parts.length==2) {
		// rankingId positiveExample
		Set pos = (Set)unrankedMap.get(parts[0]);
		if (pos==null) unrankedMap.put(parts[0], (pos = new TreeSet()));
		pos.add( parts[1] );
		increment(numPosExamples,parts[0],1);
	    } else if (parts.length==4) {
		// rankingId graphId rank weight
		ArrayList ranking = (ArrayList)rankedListMap.get(parts[0]);
		if (ranking==null) rankedListMap.put(parts[0], (ranking = new ArrayList()));
		ArrayList scores = (ArrayList)scoreMap.get(parts[0]);
		if (scores==null) scoreMap.put( parts[0], (scores = new ArrayList()));
		scores.add( new Double(StringUtil.atof(parts[3])) );
		ranking.add( parts[1] );
	    } else {
		throw new IllegalArgumentException(file+" line "+in.getLineNumber()+": illegal format");
	    }
	}
	for (Iterator i=getRankingIterator(); i.hasNext(); ) {
	    String rankingId = (String)i.next();
	    //System.out.println("unbuffering: "+rankingId);
	    Set pos = (Set)unrankedMap.get( rankingId );
	    if (pos==null) pos = Collections.EMPTY_SET;
	    ArrayList scores = (ArrayList)scoreMap.get( rankingId );
	    ArrayList ranking = (ArrayList)rankedListMap.get( rankingId );
	    double[] newScores = new double[scores.size()];
	    for (int j=0; j<ranking.size(); j++) {
		String exId = (String)ranking.get(j);
		if (pos.contains(exId)) {
		    ranking.set(j, new Example( new MutableInstance(exId), ClassLabel.binaryLabel(+1) ));
		    pos.remove( exId );
		} else {
		    ranking.set(j, new Example( new MutableInstance(exId), ClassLabel.binaryLabel(-1) ));
		}
		newScores[j] = ((Double)scores.get(j)).doubleValue();
	    }
	    scoreMap.put( rankingId, newScores );
	}
    }

    //
    // test
    //

    public class MyCLP extends BasicCommandLineProcessor {
	//public void graph(String s) { graph=new TextGraph(s,'r'); }
	public void gui() { guiFlag = true; }
	public void loadFrom(String s) { 
	    loadedFile=s; 
	    try { loadFromFile(new File(s)); } catch (IOException ex) { ex.printStackTrace(); }
	}
    }
    public void processArguments(String[] args) { new MyCLP().processArguments(args); }

    static public void main(String[] args) throws IOException
    {
	RankingEvaluation eval = new RankingEvaluation();
	eval.processArguments(args);
	if (eval.guiFlag) new ViewerFrame(eval.loadedFile, eval.toGUI());
	else System.out.println(eval.toTable());
    }
}
