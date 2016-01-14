package com.stellarsoftware.beam;

/**  Revised 7 Oct 2015 for improved accuracy
  *  Replaced arccos(dotProduct) with arctan2(crossProduct, dotProduct); 
  */

class Triple
{
    private double x, y, z;
    
    public Triple(double gx, double gy, double gz)
      { x=gx; y=gy; z=gz; }
      
    public Triple()
      { x=0; y=0; z=0; }
      
    public double getX()
      { return x; }
      
    public double getY()
      { return y; }
      
    public double getZ()
      { return z; }
      
    public void putX(double gx)
      { x = gx; }
      
    public void putY(double gy)
      { y = gy; }
      
    public void putZ(double gz)
      { z = gz; }
    
    public boolean isZero()
    {
        return (getX()==0.) && (getY()==0.) && (getZ()==0.); 
    }

    public double getLength()
    {
        return Math.sqrt(x*x + y*y + z*z); 
    }
      
    static Triple getUniformCube()
    // throughout a cube of unit radius
    {
        return new Triple(2.*Math.random()-1., 2*Math.random()-1., 2*Math.random()-1.); 
    }
    
    static Triple getSphereSurface()
    // on the surface of a unit sphere
    {
        double a = 2. * Math.PI*Math.random(); 
        double z = 2. * Math.random() - 1.; 
        double r = Math.sqrt(1 - z*z); 
        return new Triple(r*Math.cos(a), r*Math.sin(a), z); 
    }
    
    static Triple getCross(Triple A, Triple B)
    {
        double x = A.getY()*B.getZ() - A.getZ()*B.getY(); 
        double y = A.getZ()*B.getX() - A.getX()*B.getZ(); 
        double z = A.getX()*B.getY() - A.getY()*B.getX(); 
        return new Triple(x, y, z); 
    }
    
    static double getDot (Triple A, Triple B)
    {
        return A.getX()*B.getX() + A.getY()*B.getY() + A.getZ()*B.getZ();
    }
    
    static Triple getSum(Triple A, Triple B)
    {
        return new Triple(A.getX()+B.getX(), A.getY()+B.getY(), A.getZ()+B.getZ());
    }
    
    static Triple getDif(Triple A, Triple B)
    {
        return new Triple(A.getX()-B.getX(), A.getY()-B.getY(), A.getZ()-B.getZ());
    }

    static Triple getProduct(Triple A, double d)
    {
        return new Triple(d*A.getX(), d*A.getY(), d*A.getZ()); 
    }
    
    static Triple getParallel(Triple V, Triple U)
    // obtains the parallel part of V on the unit vector U
    // Dot-Product method:  U(V.U)
    {
        return getProduct(U, getDot(V, U)); 
    }
    
    static Triple getPerp(Triple V, Triple U)
    // obtains the perp part of V on the unit vector U
    // Dot-Product method:  V-U(U.V)
    {
        return getDif(V, getParallel(V, U));
    }
    
    static Triple getPerpCross(Triple V, Triple U)
    // obtains the perp part of V on the unit vector U
    // Cross-Product method: Ux(VxU) but prefer V-(U.V)U
    {
        return getCross(U, getCross(V, U)); 
    }
    
    static double getAngle(Triple U, Triple V)
    // Evaluates the angle in radians between two general Triples.
    // Any lengths except zero; perp and para are both linear in U and V.
    // Use of atan2() avoids arccos and arcsin singularities!
    {   
        if (U.isZero() || V.isZero())
          return -0.0;
        double para = getDot(U, V); 
        double perp = getCross(U, V).getLength(); 
        double angle = Math.atan2(perp, para); 
        if (angle < 0.)
          angle += Math.PI;   
        return angle; 
    }
}
  
