package ezgraph;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;

public class DelaunayGraph extends Graph {

  private Point_dt firstP;

﻿  private Point_dt lastP;

  private boolean allCollinear;

  private Triangle_dt firstT,lastT, currT;

  private Triangle_dt startTriangle;

  private Triangle_dt startTriangleHull;

  private int nPoints=0;

  private Set<Point_dt> _vertices;

  private Vector<Triangle_dt> _triangles;

  private int _modCount=0, _modCount2=0;
  
  // the Bounding Box, {{x0,y0,z0} ,  {x1,y1,z1}}
  private Point_dt _bb_min, _bb_max; 

  public DelaunayGraph(Point_dt[] ps) {
﻿   _modCount = 0; _modCount2=0;
﻿   _bb_min = null; _bb_max = null; 
﻿   this._vertices = new TreeSet<Point_dt>(Point_dt.getComparator());
﻿   _triangles = new Vector<Triangle_dt>();
    allCollinear = true;
    for(int i=0;ps!=null && i<ps.length && ps[i]!=null; i++) this.insertPoint(ps[i]);
  }
  
 private int getModeCounter() { return this._modCount;}

 private void insertPoint( Point_dt p ) {  
﻿   if(this._vertices.contains(p)) return;
  ﻿ _modCount++;
  ﻿ updateBB(p);
﻿   this._vertices.add(p);
   Triangle_dt t = insertPointSimple(p);
   if ( t == null ) return;
   Triangle_dt tt = t;
   currT = t;
   do {
      flip(tt, _modCount);
      tt = tt.canext;
   } while ( tt != t && !tt.halfplane );
 }

  private Iterator<Triangle_dt> getLastUpdatedTriangles() {
﻿    Vector<Triangle_dt> tmp = new Vector<Triangle_dt>();
﻿    if(this.trianglesSize()>1) {
﻿  ﻿    Triangle_dt t = currT;
﻿  ﻿    allTriangles(t,tmp,this._modCount);
﻿    }
﻿    return tmp.iterator();
  }

  private void allTriangles(Triangle_dt curr, Vector<Triangle_dt> front,int mc) {
﻿    if(curr!= null && curr._mc==mc && !front.contains(curr)) {
﻿  ﻿  front.add(curr);  
﻿  ﻿  allTriangles(curr.abnext,front,mc);
﻿  ﻿  allTriangles(curr.bcnext,front,mc);
﻿  ﻿  allTriangles(curr.canext,front,mc);
﻿    }
  }

  private Triangle_dt insertPointSimple( Point_dt p ) {
    nPoints++;
    if ( !allCollinear ) {
      Triangle_dt t = find(startTriangle, p);
      if (t.halfplane)
        startTriangle = extendOutside(t,p);
      else
        startTriangle = extendInside(t,p);
      return startTriangle;
    }
    if ( nPoints == 1 ) {
      firstP = p;
      return null;
    }
    if ( nPoints == 2 ) {
      startTriangulation( firstP,p );
      return null;
    }
    switch ( p.pointLineTest(firstP,lastP) ) {
      case Point_dt.LEFT:
        startTriangle = extendOutside( firstT.abnext,p );
        allCollinear = false;
        break;
      case Point_dt.RIGHT:
        startTriangle = extendOutside( firstT,p );
        allCollinear = false;
        break;
      case Point_dt.ONSEGMENT:
        insertCollinear( p, Point_dt.ONSEGMENT );
        break;
      case Point_dt.INFRONTOFA:
        insertCollinear( p, Point_dt.INFRONTOFA );
        break;
      case Point_dt.BEHINDB:
        insertCollinear( p, Point_dt.BEHINDB );
        break;
    }
    return null;
  }

  private void insertCollinear( Point_dt p, int res ) {
    Triangle_dt t,tp,u;
    switch ( res ) {
      case Point_dt.INFRONTOFA:
        t  = new Triangle_dt(firstP,p);
        tp = new Triangle_dt(p,firstP);
        t.abnext = tp;
        tp.abnext = t;
        t.bcnext = tp;
        tp.canext = t;
        t.canext = firstT;
        firstT.bcnext = t;
        tp.bcnext = firstT.abnext;
        firstT.abnext.canext = tp;
        firstT = t;
        firstP = p;
        break;
      case Point_dt.BEHINDB:
        t  = new Triangle_dt( p,lastP );
        tp = new Triangle_dt( lastP,p );
        t.abnext = tp;
        tp.abnext = t;
        t.bcnext = lastT;
        lastT.canext = t;
        t.canext = tp;
        tp.bcnext = t;
        tp.canext = lastT.abnext;
        lastT.abnext.bcnext = tp;
        lastT = t;
        lastP = p;
        break;
      case Point_dt.ONSEGMENT:
        u = firstT;
        while ( p.isGreater(u.a) ) u = u.canext;
        t  = new Triangle_dt(p,u.b);
        tp = new Triangle_dt(u.b,p);
        u.b = p;
        u.abnext.a = p;
        t.abnext = tp;
        tp.abnext = t;
        t.bcnext = u.bcnext;
        u.bcnext.canext = t;
        t.canext = u;
        u.bcnext = t;
        tp.canext = u.abnext.canext;
        u.abnext.canext.bcnext = tp;
        tp.bcnext = u.abnext;
        u.abnext.canext = tp;
        if ( firstT == u ) firstT = t;
        break;
    }
  }

  private void startTriangulation( Point_dt p1, Point_dt p2 ) {
    Point_dt ps,pb;
    if ( p1.isLess(p2) ) {
      ps = p1;
      pb = p2;
    } else {
      ps = p2;
      pb = p1;
    }
    firstT = new Triangle_dt( pb,ps );
    lastT = firstT;
    Triangle_dt t = new Triangle_dt( ps,pb );
    firstT.abnext = t;
    t.abnext = firstT;
    firstT.bcnext = t;
    t.canext = firstT;
    firstT.canext = t;
    t.bcnext = firstT;
    firstP = firstT.b;
    lastP = lastT.a;
    startTriangleHull = firstT;
  }

  private Triangle_dt extendInside( Triangle_dt t, Point_dt p ) {
    Triangle_dt h1,h2;
    h1 = treatDegeneracyInside( t,p );
    if ( h1 != null ) return h1;
    h1 = new Triangle_dt( t.c,t.a,p );
    h2 = new Triangle_dt( t.b,t.c,p );
    t.c = p;
    t.circumcircle();
    h1.abnext = t.canext;
    h1.bcnext = t;
    h1.canext = h2;
    h2.abnext = t.bcnext;
    h2.bcnext = h1;
    h2.canext = t;
    h1.abnext.switchneighbors(t,h1);
    h2.abnext.switchneighbors(t,h2);
    t.bcnext = h2;
    t.canext = h1;
    return t;
  }

  private Triangle_dt treatDegeneracyInside( Triangle_dt t, Point_dt p ) {
    if ( t.abnext.halfplane && p.pointLineTest(t.b,t.a) == Point_dt.ONSEGMENT )
      return extendOutside( t.abnext,p );
    if ( t.bcnext.halfplane && p.pointLineTest(t.c,t.b) == Point_dt.ONSEGMENT )
      return extendOutside( t.bcnext,p );
    if ( t.canext.halfplane && p.pointLineTest(t.a,t.c) == Point_dt.ONSEGMENT )
      return extendOutside( t.canext,p );
    return null;
  }

  private Triangle_dt extendOutside( Triangle_dt t, Point_dt p ) {
    if ( p.pointLineTest(t.a,t.b) == Point_dt.ONSEGMENT ) {
      Triangle_dt dg = new Triangle_dt( t.a,t.b,p );
      Triangle_dt hp = new Triangle_dt( p,t.b );
      t.b = p;
      dg.abnext = t.abnext;
      dg.abnext.switchneighbors(t,dg);
      dg.bcnext = hp;
      hp.abnext = dg;
      dg.canext = t;
      t.abnext = dg;
      hp.bcnext = t.bcnext;
      hp.bcnext.canext = hp;
      hp.canext = t;
      t.bcnext = hp;
      return dg;
    }
    Triangle_dt ccT = extendcounterclock( t, p );
    Triangle_dt cT = extendclock( t, p );
    ccT.bcnext = cT;
    cT.canext = ccT;
    startTriangleHull = cT;
    return cT.abnext;
  }

  private Triangle_dt extendcounterclock( Triangle_dt t, Point_dt p ) {
    t.halfplane = false;
    t.c = p;
    t.circumcircle();
    Triangle_dt tca = t.canext;
    if ( p.pointLineTest(tca.a,tca.b) >= Point_dt.RIGHT ) {
      Triangle_dt nT = new Triangle_dt( t.a,p );
      nT.abnext = t;
      t.canext = nT;
      nT.canext = tca;
      tca.bcnext = nT;
      return nT;
    }
    return extendcounterclock(tca,p);
  }

  private Triangle_dt extendclock( Triangle_dt t, Point_dt p ) {
    t.halfplane = false;
    t.c = p;
    t.circumcircle();
    Triangle_dt tbc = t.bcnext;
    if ( p.pointLineTest(tbc.a,tbc.b) >= Point_dt.RIGHT ) {
      Triangle_dt nT=new Triangle_dt(p,t.b);
      nT.abnext=t;
      t.bcnext=nT;
      nT.bcnext=tbc;
      tbc.canext=nT;
      return nT;
    }
    return extendclock(tbc,p);
  }

  private void flip( Triangle_dt t, int mc ) {

    Triangle_dt u=t.abnext, v;
    t._mc = mc;
    if ( u.halfplane || !u.circumcircle_contains(t.c) ) // kein flip nötig
      return;

    if ( t.a == u.a ) {
      v = new Triangle_dt( u.b,t.b,t.c );
      v.abnext = u.bcnext;
      t.abnext = u.abnext;
    } 
    else if ( t.a == u.b ) {
      v = new Triangle_dt( u.c,t.b,t.c );
      v.abnext = u.canext;
      t.abnext = u.bcnext;
    }
    else if ( t.a == u.c ) {
      v = new Triangle_dt( u.a,t.b,t.c );
      v.abnext = u.abnext;
      t.abnext = u.canext;
    } 
    else {
      System.out.println( "Error in flip." );
      return;
    }

    v._mc = mc;
    v.bcnext=t.bcnext;
    v.abnext.switchneighbors(u,v);
    v.bcnext.switchneighbors(t,v);
    t.bcnext=v;
    v.canext=t;
    t.b = v.a;
    t.abnext.switchneighbors(u,t);
    t.circumcircle();

    currT = v;
    flip(t,mc); 
    flip(v,mc); 
  } 

  
  /************************ new staff 6/8/2005  *********************
   * this part include some additional functionality needed...*/

  /** 
   * write all the vertices of this triangulation to a text file of the following format <br>
   * #vertices (n)﻿  <br>
   * x1 y1 z1﻿  ﻿  ﻿  <br>
   * ...﻿  ﻿  ﻿  ﻿  <br>
   * xn yn zn ﻿  ﻿  ﻿  <br>
   */
  public void write_tsin(String tsinFile) throws Exception{
﻿      FileWriter fw = new FileWriter(tsinFile);
﻿       PrintWriter os = new PrintWriter(fw);
﻿  ﻿  // prints the tsin file header:
﻿       int len = this._vertices.size(); 
﻿  ﻿  os.println(len);
﻿  ﻿  Iterator<Point_dt> it = this._vertices.iterator();
﻿  ﻿  while(it.hasNext()){
﻿  ﻿  ﻿  os.println(it.next().toFile());
﻿  ﻿  }
﻿  ﻿  os.close();
﻿  ﻿  fw.close();
  }
  /**
   * this method write the triangulation as an SMF file (OFF like format)
   * 
   * 
   * @param smfFile - file name
   * @throws Exception
   */
  public void write_smf(String smfFile) throws Exception{
﻿    ﻿  int len = this._vertices.size();
﻿    ﻿  Point_dt[] ans = new Point_dt[len];
﻿    ﻿  Iterator<Point_dt> it = this._vertices.iterator();
﻿    ﻿  Comparator<Point_dt> comp = Point_dt.getComparator();
﻿  ﻿  for(int i=0;i<len;i++){ans[i] = it.next();}
﻿  ﻿  Arrays.sort(ans,comp);
﻿  ﻿  
﻿      FileWriter fw = new FileWriter(smfFile);
﻿      PrintWriter os = new PrintWriter(fw);
﻿  ﻿  // prints the tsin file header: 
﻿  ﻿  os.println("begin");
﻿  ﻿  
﻿  ﻿  for(int i=0;i<len;i++) {
﻿  ﻿  ﻿  os.println("v "+ans[i].toFile());
﻿  ﻿  }
﻿  ﻿  int t=0, i1=-1, i2=-1, i3=-1;
﻿  ﻿  for(Iterator<Triangle_dt> dt=this.trianglesIterator();dt.hasNext();) {
﻿  ﻿  ﻿  Triangle_dt curr = dt.next();
﻿  ﻿  ﻿  t++;
﻿  ﻿  ﻿  if(!curr.halfplane) {
﻿  ﻿  ﻿  ﻿  i1 = Arrays.binarySearch(ans, curr.a, comp);
﻿  ﻿  ﻿  ﻿  i2 = Arrays.binarySearch(ans, curr.b, comp);
﻿  ﻿  ﻿  ﻿  i3 = Arrays.binarySearch(ans, curr.c, comp);
﻿  ﻿  ﻿  ﻿  if(i1<0 | i2<0|i3<0) throw new RuntimeException("** ERR: wrong triangulation inner bug - cant write as an SMF file! **");
﻿  ﻿  ﻿  ﻿  os.println("f "+(i1+1)+" "+(i2+1)+" "+(i3+1));
﻿  ﻿  ﻿  }
﻿  ﻿  }
﻿  ﻿  os.println("end");
﻿  ﻿  os.close();
﻿  ﻿  fw.close();
} 

 private int CH_size() {
﻿   int ans = 0;
﻿   Iterator<Point_dt> it = this.CH_vertices_Iterator();
﻿   while(it.hasNext()){
﻿  ﻿   ans++;
﻿  ﻿   it.next();
﻿   }
﻿   return ans;
 }

 private Triangle_dt find(Point_dt p){
  ﻿  Triangle_dt T = find(this.startTriangle,p); 
﻿    return T;
 }

 private Triangle_dt find(Point_dt p, Triangle_dt start){
﻿  if(start == null) start = this.startTriangle;
  ﻿  Triangle_dt T = find(start,p); 
﻿  return T;
 }

 private static Triangle_dt find(Triangle_dt curr, Point_dt p ) {
﻿  ﻿  if(p==null) return null; // throw new RuntimeException(" ERR: got null for find! ");
﻿  ﻿  Triangle_dt next_t;
﻿  ﻿  if(curr.halfplane) {
﻿  ﻿  ﻿  next_t = findnext2(p,curr);
﻿  ﻿  ﻿  if(next_t==null || next_t.halfplane) return curr;
﻿  ﻿  ﻿  curr = next_t;
﻿  ﻿  }
﻿  ﻿  while (true) {
﻿  ﻿  ﻿  next_t = findnext1(p, curr);
﻿  ﻿  ﻿  if (next_t==null) return curr;
﻿  ﻿  ﻿  if ( next_t.halfplane ) return next_t;
﻿  ﻿  ﻿  curr = next_t;
﻿  ﻿  }
﻿  }

﻿  private static Triangle_dt findnext1( Point_dt p, Triangle_dt v ) {
﻿  ﻿      if(p.pointLineTest(v.a,v.b)==Point_dt.RIGHT && !v.abnext.halfplane) return v.abnext;
﻿  ﻿      if(p.pointLineTest(v.b,v.c)==Point_dt.RIGHT && !v.bcnext.halfplane) return v.bcnext;
﻿  ﻿      if(p.pointLineTest(v.c,v.a)==Point_dt.RIGHT && !v.canext.halfplane) return v.canext;
﻿  ﻿      if(p.pointLineTest(v.a,v.b)==Point_dt.RIGHT ) return v.abnext;
﻿  ﻿      if(p.pointLineTest(v.b,v.c)==Point_dt.RIGHT ) return v.bcnext;
﻿  ﻿      if(p.pointLineTest(v.c,v.a)==Point_dt.RIGHT ) return v.canext;
﻿  ﻿      return null;
﻿  }
﻿  
  private static Triangle_dt findnext2( Point_dt p, Triangle_dt v ) {
﻿  ﻿      if(v.abnext !=null && !v.abnext.halfplane) return v.abnext;
﻿  ﻿      if(v.bcnext !=null && !v.bcnext.halfplane) return v.bcnext;
﻿  ﻿      if(v.canext !=null && !v.canext.halfplane) return v.canext;
﻿  ﻿      return null;
﻿  }  
  
  private boolean contains(Point_dt p) {
  ﻿  Triangle_dt tt = find(p);
  ﻿  return !tt.halfplane;
  }

  public boolean contains(double x, double y) {
  ﻿  return contains(new Point_dt(x,y));
  }

  public Point_dt z(Point_dt q) {
 ﻿  Triangle_dt t = find(q);
 ﻿  return t.z(q);
  }

  Triangle_dt safeFind(Point_dt q) {
  ﻿  Triangle_dt curr = null;
  ﻿  Iterator<Triangle_dt> it = this.trianglesIterator();
  ﻿  while(it.hasNext()) {
  ﻿  ﻿  curr = it.next();
  ﻿  ﻿  if(curr.contains(q)) return curr;
  ﻿  }
  ﻿  System.out.println("@@@@@ERR: point "+q+" was NOT found! :");
  ﻿  return null;
  }

  public double z(double x, double y) {
  ﻿  Point_dt q = new Point_dt(x,y);
 ﻿  Triangle_dt t = find(q);
 ﻿  return t.z_value(q);
  } 
  
  private void updateBB(Point_dt p) {
﻿    double x=p.x, y=p.y, z=p.z;
﻿    if(_bb_min == null) {
﻿  ﻿    _bb_min = new Point_dt(p);
﻿  ﻿    _bb_max = new Point_dt(p);
﻿    }
﻿    else {
﻿  ﻿  if(x<_bb_min.x) _bb_min.x=x; else if(x>_bb_max.x) _bb_max.x=x;
﻿  ﻿  if(y<_bb_min.y) _bb_min.y=y; else if(y>_bb_max.y) _bb_max.y=y;
﻿  ﻿  if(z<_bb_min.z) _bb_min.z=z; else if(z>_bb_max.z) _bb_max.z=z;
﻿    }
  }
  /** return the min point of the bounding box of this triangulation {{x0,y0,z0}} */
﻿  public Point_dt bb_min() {return _bb_min;}
﻿  /** return the max point of the bounding box of this triangulation {{x1,y1,z1}} */
﻿  public Point_dt bb_max() {return _bb_max;}﻿  
﻿  /** 
﻿   * computes main statistics of this triangulation: ver_size, tri_size, bounding box, modCounted.
﻿   * @return a String with the main statistics of this Triangulation.
﻿   */
﻿   String info() {
﻿  ﻿  String ans = ""+this.getClass().getCanonicalName()+"  # vertices:"+size()+"  # triangles:"+trianglesSize()+"  modCountr:"+_modCount+"\n";
﻿  ﻿  ans += "min BB:"+this.bb_min()+"  max BB:"+bb_max();
﻿  ﻿  return ans;
﻿  }
  //////////////////////////// Iterators: Vertices & Triangles ////////////////////////
﻿  /**
﻿   * computes the current set (vector) of all triangles and return an iterator to them.
﻿   */
﻿  public Iterator<Triangle_dt> trianglesIterator() {
﻿  ﻿  if(this.size()<=2) _triangles = new Vector<Triangle_dt>();
﻿  ﻿  initTriangles(); 
﻿  ﻿  return _triangles.iterator();
﻿  }
﻿  /**
﻿   * @return iterator to the set of all the points on the XY-convex hull.
﻿   */
﻿  public Iterator<Point_dt> CH_vertices_Iterator() {
﻿  ﻿  Vector<Point_dt> ans = new Vector<Point_dt>();
﻿  ﻿  Triangle_dt curr = this.startTriangleHull;
﻿  ﻿  boolean cont = true;
﻿  ﻿  double x0 = _bb_min.x(), x1 = _bb_max.x();
﻿  ﻿  double y0 = _bb_min.y(), y1 = _bb_max.y();
﻿  ﻿  boolean sx,sy;
﻿  ﻿  while(cont) {
﻿  ﻿  ﻿  sx = curr.p1().x()==x0 || curr.p1().x()==x1;
﻿  ﻿  ﻿  sy = curr.p1().y()==y0 || curr.p1().y()==y1;
﻿  ﻿  ﻿  if((sx & sy) | (!sx & !sy)) {  
﻿  ﻿  ﻿  ﻿  ans.add(curr.p1());
﻿  ﻿  ﻿  ﻿  System.out.println(curr.p1());
﻿  ﻿  ﻿  }
﻿  ﻿  ﻿  if(curr.bcnext != null && curr.bcnext.halfplane) curr = curr.bcnext;
﻿  ﻿  ﻿  if(curr == this.startTriangleHull) cont = false;
﻿  ﻿  }
﻿  ﻿  return ans.iterator();
﻿  }
﻿  /**
﻿   * @return iterator to the set of points compusing this triangulation.
﻿   */
﻿  public Iterator<Point_dt> verticesIterator() {
﻿  ﻿  return this._vertices.iterator();
﻿  }
﻿  private void initTriangles() {
﻿  ﻿  if(_modCount == _modCount2) return;
﻿  ﻿  if(this.size()>2) { 
﻿  ﻿  ﻿  _modCount2 = _modCount;
﻿  ﻿  ﻿  Vector<Triangle_dt> front = new Vector<Triangle_dt>();
﻿  ﻿  ﻿  _triangles = new Vector<Triangle_dt>();
﻿  ﻿  ﻿  front.add(this.startTriangle);
﻿  ﻿  ﻿  while(front.size()>0) {
﻿  ﻿  ﻿  ﻿  Triangle_dt t = front.remove(0);
﻿  ﻿  ﻿  ﻿  if(t._mark==false) {
﻿  ﻿  ﻿  ﻿  ﻿  t._mark = true;
﻿  ﻿  ﻿  ﻿  ﻿  _triangles.add(t);
﻿  ﻿  ﻿  ﻿  ﻿  if(t.abnext!=null && !t.abnext._mark) {front.add(t.abnext);}
﻿  ﻿  ﻿  ﻿  ﻿  if(t.bcnext!=null && !t.bcnext._mark) {front.add(t.bcnext);}
﻿  ﻿  ﻿  ﻿  ﻿  if(t.canext!=null && !t.canext._mark) {front.add(t.canext);}
﻿  ﻿  ﻿  ﻿  }
﻿  ﻿  ﻿  }
﻿  ﻿  ﻿  //_triNum = _triangles.size();
﻿  ﻿  ﻿  for(int i=0;i<_triangles.size();i++) {
﻿  ﻿  ﻿  ﻿  _triangles.elementAt(i)._mark = false;
﻿  ﻿  ﻿  } 
﻿  ﻿  }
﻿  }
}

class Point_dt {
﻿  double x,y,z;
﻿  public Point_dt(){}
﻿  /** constructs a 3D point */
﻿  public Point_dt(double x, double y, double z){
﻿  ﻿  this.x=x;   this.y=y;  this.z = z;
﻿  }

/** constructs a 3D point with a z value of 0.*/
﻿  public Point_dt(double x,double y){this(x,y,0);}
/** simple copy constructor */
﻿  public Point_dt( Point_dt p ) {
﻿  ﻿  x = p.x;
﻿  ﻿  y = p.y;
﻿  ﻿  z = p.z;
﻿  }
/** returns the x-coordinate of this point. */﻿  
public double x() {return x;};﻿  
/** returns the y-coordinate of this point. */﻿  
public double y() {return y;};﻿  
/** returns the z-coordinate of this point. */﻿  
public double z() {return z;};﻿  
﻿  double distance2(Point_dt p){
﻿  ﻿  return (p.x-x)*(p.x-x)+(p.y-y)*(p.y-y);
﻿  }
﻿  double distance2(double px, double py){
﻿  ﻿  return (px-x)*(px-x)+(py-y)*(py-y);
﻿  }

﻿  boolean isLess( Point_dt p ) {
﻿    return ( x<p.x ) || ( (x==p.x) && (y<p.y) );
﻿  }

﻿  boolean isGreater( Point_dt p ) {
﻿    return (x>p.x) || ( (x==p.x) && (y>p.y) );
﻿  }
/** return true iff this point [x,y] coordinates are the same as p [x,y] coordinates. (the z value is ignored).*/
﻿  public boolean equals(Point_dt p ) {
﻿    return (x==p.x) && (y==p.y);
﻿  }
/** return a String in the [x,y,z] format */ 
﻿  public String toString() {
﻿  ﻿  return(new String(" Pt[" + x + "," + y + "," +z +"]"));
﻿  }
﻿
     public double distance (Point_dt p)
﻿     {
﻿        double temp = Math.pow (p.x() - x, 2) + Math.pow (p.y() - y, 2);
﻿        return Math.sqrt (temp);
﻿     }
﻿
     public double distance3D (Point_dt p)
﻿     {
﻿        double temp = Math.pow (p.x() - x, 2) + Math.pow (p.y() - y, 2) + Math.pow (p.z() - z, 2);
﻿        return Math.sqrt (temp);
﻿     }
﻿
  public final static int ONSEGMENT = 0;﻿  
﻿  
  public final static int LEFT = 1;

﻿  /**
﻿   * ﻿   a---------b <br>
﻿   * ﻿  ﻿  ﻿  ﻿  +﻿  ﻿  ﻿  ﻿  ﻿  ﻿  ﻿  
﻿   * */
﻿  public final static int RIGHT = 2;
/** +a---------b */
﻿  public final static int INFRONTOFA = 3;﻿  
﻿  /** a---------b+ */
﻿  public final static int BEHINDB = 4;
﻿  public final static int ERROR = 5;

/**
 *  tests the relation between this point (as a 2D [x,y] point) and
 *  a 2D segment a,b (the Z values are ignored), returns one of the following:
 *  LEFT, RIGHT, INFRONTOFA, BEHINDB, ONSEGMENT
 * 
 * @param a the first point of the segment.
 * @param b the second point of the segment.
 * @return the value (flag) of the relation between this point and the a,b line-segment.
 */
﻿  public int pointLineTest(Point_dt a, Point_dt b) {

﻿  ﻿  double dx = b.x-a.x;
﻿  ﻿  double dy = b.y-a.y;
﻿  ﻿  double res = dy*(x-a.x)-dx*(y-a.y);

﻿  ﻿  if (res < 0) return LEFT;
﻿  ﻿  if (res > 0) return RIGHT;
﻿  
﻿  ﻿  if (dx > 0) {
﻿  ﻿  ﻿  if (x < a.x) return INFRONTOFA;
﻿  ﻿  ﻿  if (b.x < x) return BEHINDB;
﻿  ﻿  ﻿  return ONSEGMENT;
﻿  ﻿  }
﻿  ﻿  if (dx < 0) {
﻿  ﻿  ﻿  if (x > a.x) return INFRONTOFA;
﻿  ﻿  ﻿  if (b.x > x) return BEHINDB;
﻿  ﻿  ﻿  return ONSEGMENT;
﻿  ﻿  }
﻿  ﻿  if (dy > 0) {
﻿  ﻿  ﻿  if (y < a.y) return INFRONTOFA;
﻿  ﻿  ﻿  if (b.y < y) return BEHINDB;
﻿  ﻿  ﻿  return ONSEGMENT;
﻿  ﻿  }
﻿  ﻿  if (dy < 0) {
﻿  ﻿  ﻿  if (y > a.y) return INFRONTOFA;
﻿  ﻿  ﻿  if (b.y > y) return BEHINDB;
﻿  ﻿  ﻿  return ONSEGMENT;
﻿  ﻿  }
﻿  ﻿  System.out.println("Error, pointLineTest with a=b");
﻿  ﻿  return ERROR;
﻿  }


   boolean areCollinear(Point_dt a, Point_dt b) {
    double dx = b.x-a.x;
    double dy = b.y-a.y;
    double res = dy*(x-a.x)-dx*(y-a.y);
    return res==0;
  }

 /* public ajSegment Bisector( ajPoint b) {
    double sx = (x+b.x)/2;
    double sy = (y+b.y)/2;
    double dx = b.x-x;
    double dy = b.y-y;
    ajPoint p1 = new ajPoint(sx-dy,sy+dx);
    ajPoint p2 = new ajPoint(sx+dy,sy-dx);
    return new ajSegment( p1,p2 );
  }*/

  Point_dt circumcenter( Point_dt a, Point_dt b) {

    double u = ((a.x-b.x)*(a.x+b.x) + (a.y-b.y)*(a.y+b.y)) / 2.0f;
    double v = ((b.x-x)*(b.x+x) + (b.y-y)*(b.y+y)) / 2.0f;
    double den = (a.x-b.x)*(b.y-y) - (b.x-x)*(a.y-b.y);
    if ( den==0 ) // oops
      System.out.println( "circumcenter, degenerate case" );
    return new Point_dt((u*(b.y-y)   - v*(a.y-b.y)) / den,
                       (v*(a.x-b.x) - u*(b.x-x)) / den);
  }
  public static Comparator<Point_dt> getComparator(int flag) {return new Compare(flag);}
  public static Comparator<Point_dt> getComparator() {return new Compare(0);}
}

   class Compare implements Comparator {
      private int _flag;
      public Compare(int i) { _flag = i;}
      
      /**compare between two points. */
  public int compare(Object o1,Object o2) {﻿  
  ﻿  int ans =0;
  ﻿  if (o1!=null && o2!=null && o1 instanceof Point_dt && o2 instanceof Point_dt) {﻿  
  ﻿  ﻿  Point_dt d1 = (Point_dt)o1;
  ﻿  ﻿  Point_dt d2 = (Point_dt)o2;
  ﻿      if(_flag == 0) {
  ﻿  ﻿  if(d1.x  > d2.x ) return 1;
  ﻿  ﻿  if(d1.x  < d2.x ) return -1;
  ﻿          // x1 == x2
  ﻿  ﻿  if(d1.y  > d2.y ) return 1;
  ﻿  ﻿  if(d1.y  < d2.y ) return -1;
  ﻿      }
  ﻿      else ﻿  if(_flag == 1) {
  ﻿  ﻿  if(d1.x  > d2.x ) return -1;
  ﻿  ﻿  if(d1.x  < d2.x ) return 1;
  ﻿          // x1 == x2
  ﻿  ﻿  if(d1.y  > d2.y ) return -1;
  ﻿  ﻿  if(d1.y  < d2.y ) return 1;
  ﻿      }
  ﻿      else ﻿  if(_flag == 2) {
  ﻿  ﻿  if(d1.y  > d2.y ) return 1;
  ﻿  ﻿  if(d1.y  < d2.y ) return -1;
  ﻿          // y1 == y2
  ﻿  ﻿  if(d1.x  > d2.x ) return 1;
  ﻿  ﻿  if(d1.x  < d2.x ) return -1;

  ﻿      }
  ﻿      else ﻿  if(_flag == 3) {
  ﻿  ﻿  if(d1.y  > d2.y ) return -1;
  ﻿  ﻿  if(d1.y  < d2.y ) return 1;
  ﻿          // y1 == y2
  ﻿  ﻿  if(d1.x  > d2.x ) return -1;
  ﻿  ﻿  if(d1.x  < d2.x ) return 1;
  ﻿      }
  ﻿  }
  ﻿  else 
  ﻿      {
  ﻿  ﻿  if(o1==null && o2 ==null) return 0;﻿  ﻿  ﻿  
  ﻿  ﻿  if(o1==null && o2 !=null) return 1;﻿  
  ﻿  ﻿  if(o1!=null && o2 ==null) return -1;
  ﻿      }
  ﻿  return ans;
      }
      
      public boolean equals(Object ob) {return false;}
}

class Triangle_dt {
﻿
  Point_dt a,b,c;
﻿
  Triangle_dt abnext,bcnext,canext;
﻿
  Circle_dt circum;
﻿
  int _mc = 0; // modcounter for triangulation fast update.

﻿  boolean halfplane=false; // true iff it is an infinite face.
//﻿  public boolean visitflag;
﻿  boolean _mark = false;   // tag - for bfs algorithms
//﻿  private static boolean visitValue=false;
﻿  public static int _counter = 0, _c2=0;
﻿  //public int _id;
/** constructs a triangle form 3 point - store it in counterclockwised order.*/
﻿  public Triangle_dt( Point_dt A, Point_dt B, Point_dt C ) {
//﻿  ﻿  visitflag=visitValue;
﻿  ﻿  a=A;
﻿  ﻿  int res = C.pointLineTest(A,B);
﻿  ﻿  if ( (res <= Point_dt.LEFT) ||
﻿  ﻿       (res == Point_dt.INFRONTOFA) ||
﻿  ﻿       (res == Point_dt.BEHINDB) ) {
﻿  ﻿  ﻿  b=B;
﻿  ﻿  ﻿  c=C;
﻿  ﻿  }
﻿  ﻿  else {  // RIGHT
﻿  ﻿  ﻿  System.out.println("Warning, ajTriangle(A,B,C) "+
﻿  ﻿  ﻿  "expects points in counterclockwise order.");
﻿  ﻿  ﻿  System.out.println(""+A+B+C);
﻿  ﻿  ﻿  b=C;
﻿  ﻿  ﻿  c=B;
﻿  ﻿  }
﻿  ﻿  circumcircle();
﻿  ﻿  //_id = _counter++;
﻿  ﻿  //_counter++;_c2++;
﻿  ﻿  //if(_counter%10000 ==0) System.out.println("Triangle: "+_counter);
﻿  }

/**
 * creates a half plane using the segment (A,B).
 * @param A
 * @param B
 */
﻿  public Triangle_dt( Point_dt A, Point_dt B ) {
//﻿  ﻿  visitflag=visitValue;
﻿  ﻿  a=A;
﻿  ﻿  b=B;
﻿  ﻿  halfplane=true;
//﻿  ﻿  _id = _counter++;
﻿  }
/*﻿  protected void finalize() throws Throwable{
﻿  ﻿  super.finalize();
﻿  ﻿  _counter--;
﻿  } */
﻿  
﻿  /** 
﻿   * remove all pointers (for debug)
﻿   */
﻿  //public void clear() {
﻿  //﻿  this.abnext = null; this.bcnext=null; this.canext=null;}
﻿  
﻿  /**
﻿   * returns true iff this triangle is actually a half plane.
﻿   */
﻿  public boolean isHalfplane() {return this.halfplane;}
﻿  /**
﻿   * returns the first vertex of this triangle.
﻿   */
﻿  public Point_dt p1() {return a;}
﻿  /**
﻿   * returns the second vertex of this triangle.
﻿   */
﻿  public Point_dt p2() {return b;}
﻿  /**
﻿   * returns the 3th vertex of this triangle.
﻿   */
﻿  public Point_dt p3() {return c;}
﻿  /**
﻿   * returns the consecutive triangle which shares this triangle p1,p2 edge. 
﻿   */
﻿  public Triangle_dt next_12() {return this.abnext;} 
﻿  /**
﻿   * returns the consecutive triangle which shares this triangle p2,p3 edge. 
﻿   */
﻿  public Triangle_dt next_23() {return this.bcnext;} 
﻿  /**
﻿   * returns the consecutive triangle which shares this triangle p3,p1 edge. 
﻿   */
﻿  public Triangle_dt next_31() {return this.canext;} 
﻿  

  void switchneighbors( Triangle_dt Old,Triangle_dt New ) {
    if ( abnext==Old ) abnext=New;
    else if ( bcnext==Old ) bcnext=New;
    else if ( canext==Old ) canext=New;
    else System.out.println( "Error, switchneighbors can't find Old." );
  }

  Triangle_dt neighbor( Point_dt p ) {
    if ( a==p ) return canext;
    if ( b==p ) return abnext;
    if ( c==p ) return bcnext;
    System.out.println( "Error, neighbors can't find p: "+p );
    return null;
  }

  Circle_dt circumcircle() {

    double u = ((a.x-b.x)*(a.x+b.x) + (a.y-b.y)*(a.y+b.y)) / 2.0f;
    double v = ((b.x-c.x)*(b.x+c.x) + (b.y-c.y)*(b.y+c.y)) / 2.0f;
    double den = (a.x-b.x)*(b.y-c.y) - (b.x-c.x)*(a.y-b.y);
    if ( den==0 ) // oops, degenerate case
      circum = new Circle_dt( a,Double.POSITIVE_INFINITY );
    else {
      Point_dt cen =  new Point_dt((u*(b.y-c.y) - v*(a.y-b.y)) / den,
                                 (v*(a.x-b.x) - u*(b.x-c.x)) / den);
      circum = new Circle_dt( cen,cen.distance2(a) );
    }
    return circum;
  }

  boolean circumcircle_contains( Point_dt p ) {

﻿  ﻿  return circum.r > circum.c.distance2(p);
﻿  }

/*
  private void visitAndPrint() {

    visitValue = !visitValue;
    visitMorePrint();
  }

  private void visitMorePrint() {

    visitflag = visitValue;
    if ( !halfplane ) {
﻿  ﻿  System.out.println(circum);
    }
    if ( abnext.visitflag != visitValue )
      abnext.visitMorePrint();
    if ( bcnext.visitflag != visitValue )
      bcnext.visitMorePrint();
    if ( canext.visitflag != visitValue )
      canext.visitMorePrint();    
  }
*/

 /* public ajSegment dualEdge( ajTriangle t ) {

    if ( t.halfplane )
      if ( halfplane ) {
        System.out.println("Warning, no dual edge between two halfplanes.");
        System.out.println(""+this+t);
        return null;
      }
      else
        return new ajSegment(circum.c,
          new ajPoint( circum.c.x-(t.b.y-t.a.y),
                       circum.c.y+(t.b.x-t.a.x)
          )
        );
    else if ( halfplane )
      return new ajSegment(t.circum.c,
        new ajPoint( t.circum.c.x-(b.y-a.y),
                     t.circum.c.y+(b.x-a.x)
        )
      );
    else
      return new ajSegment(circum.c,t.circum.c);
  }
*/

  public String toString() {
    String res =""; //+_id+") ";
    res += a.toString()+b.toString();
    if ( !halfplane )
       res +=c.toString() ;
    ﻿  // res +=c.toString() +"   | "+abnext._id+" "+bcnext._id+" "+canext._id;
    return res;
  }

  /**
   * computes if this triangle contains the point p.
   * @param p the query point
   * @return true iff p is not null and is inside this triangle (Note: on boundary is considered inside!!).
   */
  public boolean contains(Point_dt p) {
﻿  boolean ans = false;
﻿  if(this.halfplane | p== null) return false;
﻿  
    if((p.x==a.x& p.y==a.y) | (p.x==b.x& p.y==b.y)| (p.x==c.x& p.y==c.y)) return true;
    int a12 = p.pointLineTest(a,b);
    int a23 = p.pointLineTest(b,c);
    int a31 = p.pointLineTest(c,a);
    
    if ((a12 == Point_dt.LEFT && a23 == Point_dt.LEFT && a31 == Point_dt.LEFT ) ||
﻿  (a12 == Point_dt.RIGHT && a23 == Point_dt.RIGHT && a31 == Point_dt.RIGHT ) ||﻿  
﻿  (a12 == Point_dt.ONSEGMENT ||a23 == Point_dt.ONSEGMENT ||  a31 == Point_dt.ONSEGMENT))
﻿  ans = true;

﻿  return ans;
    }
  /**
   * compute the Z value for the X,Y values of q.
   * assume this triangle represent a plane --> q does NOT need to be contained
   * in this triangle.
   * 
   * @param q query point (its Z value is ignored).
   * @return the Z value of this plane implies by this triangle 3 points.
   * 
   * NOTE:
   * 1. this code was written for hi efficiency - no function call! - very ugly!
   * 2. the code was not yet provven bug free (suspect non genertal position inputs).
   */
  public double z_value(Point_dt q) {
  ﻿  if(q==null || this.halfplane) throw new RuntimeException("*** ERR wrong parameters, can't approximate the z value ..***: "+q);
  ﻿  /** incase the query point is on one of the points */
  ﻿  if(q.x==a.x & q.y==a.y) return a.z;
  ﻿  if(q.x==b.x & q.y==b.y) return b.z;
  ﻿  if(q.x==c.x & q.y==c.y) return c.z;
  ﻿  
     /** //////////////////////////////////////////////////////////////
  ﻿   *  plane: aX + bY + c = Z:
  ﻿   *  2D line: y= mX + k
  ﻿   *  
  ﻿   */
  ﻿  double X=0,x0 = q.x, x1 = a.x, x2=b.x, x3=c.x;
  ﻿  double Y=0,y0 = q.y, y1 = a.y, y2=b.y, y3=c.y;
  ﻿  double Z=0, m01=0,k01=0,m23=0,k23=0;
  ﻿  // 0 - regular, 1-horisintal , 2-vertical.
  ﻿  int flag01 = 0;
  ﻿  if(x0!=x1) {
  ﻿  ﻿  m01 = (y0-y1)/(x0-x1);
  ﻿  ﻿  k01 = y0 - m01*x0;
  ﻿  ﻿  if(m01 ==0) flag01 = 1;
  ﻿  }
  ﻿  else { // 2-vertical.
  ﻿  ﻿  flag01 = 2;//x01 = x0
  ﻿  }
  ﻿  int flag23 = 0;
  ﻿  if(x2!=x3) {
  ﻿  ﻿  m23 = (y2-y3)/(x2-x3);
  ﻿  ﻿  k23 = y2 - m23*x2;
  ﻿  ﻿  if(m23 ==0) flag23 = 1;
  ﻿  }
  ﻿  else { // 2-vertical.
  ﻿  ﻿  flag23 = 2;//x01 = x0
  ﻿  }
  ﻿  
  ﻿  if(flag01 ==2 ) {
  ﻿  ﻿  X = x0;
  ﻿  ﻿  Y = m23*X + k23;
  ﻿  }
  ﻿  else {
  ﻿  ﻿  if(flag23==2) {
  ﻿  ﻿  ﻿  X = x2;
  ﻿    ﻿  ﻿  Y = m01*X + k01;
  ﻿  ﻿  }
  ﻿  ﻿  else {  // regular case 
  ﻿  ﻿  ﻿  X=(k23-k01)/(m01-m23);
  ﻿  ﻿  ﻿  Y = m01*X+k01;
  ﻿  ﻿  ﻿  
  ﻿  ﻿  }
  ﻿  }
  ﻿  double r = 0;
  ﻿  if(flag23==2) {r=(y2-Y)/(y2-y3);} else {r=(x2-X)/(x2-x3);}
  ﻿  Z = b.z + (c.z-b.z)*r;
  ﻿  if(flag01==2) {r=(y1-y0)/(y1-Y);} else {r=(x1-x0)/(x1-X);}
  ﻿  double qZ = a.z + (Z-a.z)*r;
  ﻿  return qZ;
  }
  /**
   * compute the Z value for the X,Y values of q.
   * assume this triangle represent a plane --> q does NOT need to be contained
   * in this triangle.
   *   
   * @param x  x-coordinate of the query point.
   * @param y  y-coordinate of the query point.
   * @return z (height) value approximation given by the triangle it falls in.
   * 
   */
  public double z(double x, double y) {
  ﻿  return  z_value(new Point_dt(x,y));
  }
  /**
   * compute the Z value for the X,Y values of q.
   * assume this triangle represent a plane --> q does NOT need to be contained
   * in this triangle.
   *   
   * @param q query point (its Z value is ignored).
   * @return q with updated Z value.
   * 
   */
  public Point_dt z(Point_dt q) {
  ﻿  double z = z_value(q);
  ﻿  return new Point_dt(q.x,q.y, z);
  }
}


class Circle_dt  {

  Point_dt c;

  double r;

  public Circle_dt(){ }

  public Circle_dt( Point_dt c, double r ) {
    this.c = c;
    this.r = r;
  }

  public Circle_dt( Circle_dt circ) {
    this.c = circ.c;
    this.r = circ.r;
  }

}

