package ezgraph;

import es.yrbcn.graph.weighted.*;
import it.unimi.dsi.webgraph.*;
import it.unimi.dsi.webgraph.labelling.*;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.*;
import java.util.*;
import java.io.*;
import java.lang.reflect.*;

public class PageRank {

	private Int2DoubleMap scores;

	private Graph graph;

 	public PageRank ( Graph graph ) { this(graph, 0.0000000001, 1000); }

 	public PageRank ( Graph graph, double threshold, int maxIter ) {
		org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("es.yrbcn.graph.weighted.WeightedPageRankPowerMethod");
		logger.setLevel(org.apache.log4j.Level.FATAL);
		this.graph = graph;
		scores = new Int2DoubleOpenHashMap(graph.numNodes());
		try {
			WeightedPageRankPowerMethod pr = new WeightedPageRankPowerMethod(graph.graph);
			if ( threshold > 0 && maxIter > 0 ) pr.stepUntil(WeightedPageRank.or( new WeightedPageRank.NormDeltaStoppingCriterion(threshold), new WeightedPageRankPowerMethod.IterationNumberStoppingCriterion(maxIter)));
			else if ( threshold > 0 && maxIter < 0 ) pr.stepUntil(new WeightedPageRank.NormDeltaStoppingCriterion(threshold));
			else if ( threshold < 0 && maxIter > 0 ) pr.stepUntil(new WeightedPageRankPowerMethod.IterationNumberStoppingCriterion(maxIter));
			else pr.stepUntil(WeightedPageRank.or( new WeightedPageRank.NormDeltaStoppingCriterion(0.0000000001), new WeightedPageRankPowerMethod.IterationNumberStoppingCriterion(1000)));
			int pos = 0;
			for ( double rank : pr.rank ) scores.put(pos++, rank);
		} catch ( IOException ex ) { throw new Error(ex); }
	}


	public double getPageRank ( int node ) { return scores.get(node); }

	public double getPageRank ( String node ) { int id = graph.node(node); return scores.get(id); }

	public List<String> getSortedNodes (  ) { 
		List<String> list = new ArrayList<String>();
  		Iterator<Integer> iterator = scores.keySet().iterator();
  		while (iterator.hasNext()) list.add(graph.node(iterator.next()));
		java.util.Collections.sort(list, new Comparator<String>(){
            		public int compare(String entry, String entry1) {
		                int i1 = graph.node(entry);
				int i2 = graph.node(entry1);
				return scores.get(i1) == scores.get(i2) ? 0 : scores.get(i1) < scores.get(i2) ? 1 : -1;
            		}
        	});
		return list;
	}

}