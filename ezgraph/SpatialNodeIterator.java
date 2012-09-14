package ezgraph;

import es.yrbcn.graph.weighted.*;
import it.unimi.dsi.webgraph.*;
import it.unimi.dsi.webgraph.labelling.*;
import java.util.*;
import java.io.*;
import java.lang.reflect.*;

public class SpatialNodeIterator extends NodeIterator {

	SpatialGraph graph;

	public SpatialNodeIterator ( SpatialGraph graph ) {
		super(graph);
		this.graph = graph;
	}

	public SpatialNodeIterator ( SpatialGraph graph , int n ) {
		super(graph,n);
		this.graph = graph;
	}

	public float spatialStrength( float latMin, float latMax, float lonMin, float lonMax ) { 
		return spatialOutStrength(latMin,latMax,lonMin,lonMax) + spatialInStrength(latMin,latMax,lonMin,lonMax); 
	}

	public float spatialOutStrength( float latMin, float latMax, float lonMin, float lonMax ) {
		float count = 0;
		int[] n = successorArray();
		Label[] l = successorLabelArray();
		for ( int i = 0; i < n.length; i++ ) if( contained(n[i],latMin,latMax,lonMin,lonMax) ) count += l[i].getFloat();
		return count; 
	}

	public float spatialInStrength( float latMin, float latMax, float lonMin, float lonMax ) { 
		float count = 0;
		int[] n = ancestorArray();
		Label[] l = ancestorLabelArray();
		for ( int i = 0; i < n.length; i++ ) if( contained(n[i],latMin,latMax,lonMin,lonMax) ) count += l[i].getFloat();
		return count; 
	}

	public double spatialStrengthRatio( float latMin, float latMax, float lonMin, float lonMax ) { 
		return (double)spatialStrength(latMin,latMax,lonMin,lonMax) / (double)strength();
	}

	public double spatialOutStrengthRatio( float latMin, float latMax, float lonMin, float lonMax ) {
		return (double)spatialOutStrength(latMin,latMax,lonMin,lonMax) / (double)outstrength();
	}

	public double spatialInStrengthRatio( float latMin, float latMax, float lonMin, float lonMax ) { 
		return (double)spatialInStrength(latMin,latMax,lonMin,lonMax) / (double)instrength();
	}

	public int spatialDegree( float latMin, float latMax, float lonMin, float lonMax ) { 
		return spatialOutdegree(latMin,latMax,lonMin,lonMax) + spatialIndegree(latMin,latMax,lonMin,lonMax); 
	}

	public int spatialOutdegree( float latMin, float latMax, float lonMin, float lonMax ) {
		int count = 0; 
		for ( int n : successorArray() ) if( contained(n,latMin,latMax,lonMin,lonMax) ) count++;
		return count; 
	}

	public int spatialIndegree( float latMin, float latMax, float lonMin, float lonMax ) { 
		int count = 0; 
		for ( int n : ancestorArray() ) if( contained(n,latMin,latMax,lonMin,lonMax) ) count++;
		return count;
	}

	public double spatialDegreeRatio( float latMin, float latMax, float lonMin, float lonMax ) { 
		return (double)spatialDegree(latMin,latMax,lonMin,lonMax) / (double)degree();
	}

	public double spatialOutdegreeRatio( float latMin, float latMax, float lonMin, float lonMax ) {
		return (double)spatialOutdegree(latMin,latMax,lonMin,lonMax) / (double)outdegree();
	}

	public double spatialIndegreeRatio( float latMin, float latMax, float lonMin, float lonMax ) { 
		return (double)spatialIndegree(latMin,latMax,lonMin,lonMax) / (double)indegree();
	}

	public double spatialCloseness( ) { 
		double count = 0; 
		for ( int n : successorArray() ) count += distance(current,n);
		for ( int n : ancestorArray() ) count += distance(current,n);
		return count / (double)degree(); 
	}

	public double spatialOutCloseness( ) {
		double count = 0; 
		for ( int n : successorArray() ) count += distance(current,n);
		return count / (double)indegree(); 
	}

	public double spatialInCloseness( ) { 
		double count = 0; 
		for ( int n : ancestorArray() ) count += distance(current,n);
		return count / (double)outdegree();
	}

	private double distance ( int n1, int n2 ) {
		return 0; // TODO: call spatial distance
	}

	private boolean contained ( int n , float latMin, float latMax, float lonMin, float lonMax ) {
		if ( graph.latitude(n) >= latMin &&  graph.latitude(n) <= latMax && 
		     graph.longitude(n) >= lonMin &&  graph.longitude(n) <= lonMin ) return true;
		return false;
	}

}
