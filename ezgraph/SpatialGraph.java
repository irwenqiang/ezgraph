package ezgraph;

import es.yrbcn.graph.weighted.*;
import it.unimi.dsi.webgraph.*;
import it.unimi.dsi.webgraph.labelling.*;
import java.util.*;
import java.util.zip.*;
import java.io.*;
import java.lang.reflect.*;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.*;
import it.unimi.dsi.logging.ProgressLogger;
import jdbm.*;

public class SpatialGraph extends Graph {

  protected PrimaryHashMap<Integer,Float> latitude;

  protected PrimaryHashMap<Integer,Float> longitude;

  public SpatialGraph ( ) { super(); }

  public SpatialGraph ( String file, String file2 ) throws IOException {
	super(file);
	iterator = nodeIterator();
	BufferedReader br;
	try { 
		br = new BufferedReader(new InputStreamReader( new GZIPInputStream( new FileInputStream(file2) ))); 
	} catch ( Exception ex ) { br = new BufferedReader(new FileReader(file)); }
	String aux;
	while ( ( aux = br.readLine() ) != null ) try {
		if ( commit++ % COMMIT_SIZE == 0 ) { commit(); }
		String parts[] = aux.split("\t");
		String name = new String(parts[0]);
		Float lat = new Float(parts[1]);
		Float lon = new Float(parts[2]);
		setLatitude(name,lat);
		setLongitude(name,lon);
	} catch ( Exception ex ) { throw new Error(ex); }
	commit();
  }

  public static SpatialGraph merge ( SpatialGraph g1 , SpatialGraph g2 ) {
	int commit = 0;
	Graph g = Graph.merge(g1,g2);
	SpatialGraph sg = new SpatialGraph();
	sg.nodes = g.nodes;
	sg.nodesReverse = g.nodesReverse;
	sg.graph = g.graph;
  	sg.reverse = g.reverse;
	sg.numArcs = g.numArcs;
	sg.iterator = new SpatialNodeIterator(sg);
	for ( String n : sg.nodesReverse.keySet() ) {
		if ( commit++ % COMMIT_SIZE == 0 ) { sg.commit(); }
		sg.setLatitude( n , g1.latitude(n) );
		sg.setLongitude( n , g1.longitude(n) );
	}
	sg.commit();
	return sg;
  }

  public SpatialGraph copy() {
	Graph g = super.copy();
	SpatialGraph sg = new SpatialGraph();
	sg.nodes = g.nodes;
	sg.nodesReverse = g.nodesReverse;
	sg.graph = g.graph;
  	sg.reverse = g.reverse;
	sg.numArcs = g.numArcs;
	sg.iterator = new SpatialNodeIterator(sg);
	for ( String n : sg.nodesReverse.keySet() ) {
		if ( commit++ % COMMIT_SIZE == 0 ) { sg.commit(); }
		sg.setLatitude( n , latitude(n) );
		sg.setLongitude( n , longitude(n) );
	}
	sg.commit();
	return sg;
  }

  private SpatialNodeIterator advanceIterator ( int x ) {
	if ( x >= graph.numNodes() ) throw new Error("Problem with the id for the node.");
	if ( !iterator.hasNext() || iterator.nextInt() >= x ) iterator = nodeIterator();
	Integer aux = null;
	while ( (aux = iterator.nextInt()) != x ) {  }
	return (SpatialNodeIterator)(iterator);
  }

  public float latitude ( int nodeNum ) { return this.latitude.get(nodeNum); }

  public float latitude ( String node ) { return this.latitude.get(nodesReverse.get(node)); }

  public void setLatitude ( int nodeNum , float latitude ) { this.latitude.put(nodeNum,latitude); }

  public void setLatitude ( String node , float latitude ) { this.latitude.put(nodesReverse.get(node),latitude); }

  public float longitude ( int nodeNum ) { return this.longitude.get(nodeNum); }

  public float longitude ( String node ) { return this.longitude.get(nodesReverse.get(node)); }

  public void setLongitude ( int nodeNum , float longitude ) { this.longitude.put(nodeNum,longitude); }

  public void setLongitude ( String node , float longitude ) { this.longitude.put(nodesReverse.get(node),longitude); }

  public SpatialGraph neighbourhoodGraph ( int node , int hops ) { return neighbourhoodGraph ( new int[]{ node } , hops ); }

  public SpatialGraph neighbourhoodGraph ( String node , int hops ) { return neighbourhoodGraph ( new String[]{ node } , hops ); }

  public SpatialGraph neighbourhoodGraph ( String nodes[] , int hops ) {
	int nnodes[] = new int[nodes.length];
	for ( int i = 0; i < nodes.length; i++ ) nnodes[i] = nodesReverse.get(nodes[i]);
	return neighbourhoodGraph(nnodes, hops);
  }

  public SpatialGraph neighbourhoodGraph ( int nnodes[] , int hops ) {
	Graph g = super.neighbourhoodGraph(nnodes,hops);
	SpatialGraph sg = new SpatialGraph();
	sg.nodes = g.nodes;
	sg.nodesReverse = g.nodesReverse;
	sg.graph = g.graph;
  	sg.reverse = g.reverse;
	sg.numArcs = g.numArcs;
	sg.iterator = new SpatialNodeIterator(sg);
	for ( String n : sg.nodesReverse.keySet() ) {
		if ( commit++ % COMMIT_SIZE == 0 ) { sg.commit(); }
		sg.setLatitude( n , latitude(n) );
		sg.setLongitude( n , longitude(n) );
	}
	sg.commit();
	return sg;
  } 

  public void commit () { 
	super.commit();
	try { latitude.getRecordManager().commit(); } catch ( IOException e ) { throw new Error(e); }
	try { longitude.getRecordManager().commit(); } catch ( IOException e ) { throw new Error(e); }
  };

  protected void finalize () throws Throwable {
	super.finalize();
	latitude.clear();
	latitude.getRecordManager().commit();
	latitude.getRecordManager().close();
	longitude.clear();
	longitude.getRecordManager().commit();
	longitude.getRecordManager().close();
  }

}
