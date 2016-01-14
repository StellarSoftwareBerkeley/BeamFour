package com.stellarsoftware.beam;


/** class Z is entirely static methods, never instantiated.
  * Provides surface function vGetZsurf() for Layout, all surface types.
  * Also supplies vGetSurf() for RT13 for higher surface types.
  *
  *    PHANTOM HYPERBOLOIDS ARE FORBIDDEN
  *    FARSIDE ELLIPSOIDS ARE FORBIDDEN
  *
  * Also supplies all gradients via vGetZnorm().
  * 
  * Includes Zernike(0....35) 0=piston. 
  * These require defined Diameter to operate.
  *
  *********** TWO CURVATURES *****************
  *
  *    OCURVE is a general curvature in x and y; shape, poly.
  *    OCURVX is a special circular curvature in x only.
  *    If present, OCURVX imposes toric behavior,
  *    y continues with shape & poly.
  *
  *********** INTRODUCING OCURVY ******************
  *
  *    This third curvature may help users distinguish toric & biconic
  *    surfaces, which need X and Y curvatures defined, from general
  *    axisymmetric curved surfaces.
  *
  * Perp vector methods pXXXX() are unused as of Aug 2006.
  *   N = normalize( df/dx, df/dy, -1.0); reversed Oct 7, 2015, A189; line 99.
  *
  *---eliminating |y|: lines 225, 527, 546, 548, 552, 563;  A190 Dec 2015
  *
  *
  *
  * @author M.Lampton (c) STELLAR SOFTWARE 2006 all rights reserved.
  */
class Z implements B4constants
{
    static public double dGetZsurf(double x, double y, double surf[])
    // Uses x,y and returns z for general surface.
    // Called by Layout graphics with xyz[3] all local.
    // Called by RT13 for all surface types. 
    // Copyright 2006 STELLAR SOFTWARE all rights reserved.
    {
        x = dSawtoothX(x, surf); 
        y = dSawtoothY(y, surf); 
        switch ((int) surf[OPROFILE])
        {
           case OSPLANO:   return 0.0; 
           case OSCONIC:
           case OSCONICLT:
           case OSCONICGT: return zConic(x, y, surf);
           case OSXCYL:
           case OSXCYLLT:
           case OSXCYLGT:  return zXcyl(x, y, surf);
           case OSYCYL:
           case OSYCYLLT:
           case OSYCYLGT:  return zYcyl(x, y, surf);
           case OSTORIC:   return zToric(x, y, surf);
           case OSPOLYREV: return zPolyRevCombiner(x, y, surf);
           case OSZERNREV: return zZernRevCombiner(x, y, surf);
           case OSZERNTOR: return zZernTorCombiner(x, y, surf);
           case OSBICONIC: return zBiconic(x, y, surf); 
           default:        return 0.0; 
        }
    }


    static public void vGetZnorm(double x, double y, double surf[], double norm[])
    // x, y, and surf[] are input; result is norm[3].
    // Each method called gives its gradient in norm[0], norm[1];
    // Converted to normalized normal at end of this method.
    // A189: changed signs so that norm[2] is towards +z. 
    {
        x = dSawtoothX(x, surf); 
        y = dSawtoothY(y, surf); 
        switch ((int) surf[OPROFILE])
        {
           case OSPLANO:   vGradPlane(norm); break;
           case OSCONIC:
           case OSCONICLT:
           case OSCONICGT: vGradConic(x, y, surf, norm); break; 
           case OSXCYL:
           case OSXCYLLT:
           case OSXCYLGT:  vGradXcyl(x, y, surf, norm); break; 
           case OSYCYL:
           case OSYCYLLT:
           case OSYCYLGT:  vGradYcyl(x, y, surf, norm); break;
           case OSTORIC:   vGradToric(x, y, surf, norm); break;
           case OSPOLYREV: vGradPolyRevCombiner(x, y, surf, norm); break;
           case OSZERNREV: vGradZernRevCombiner(x, y, surf, norm); break;
           case OSZERNTOR: vGradZernTorCombiner(x, y, surf, norm); break;
           case OSBICONIC: vGradBiconic(x, y, surf, norm); break; 
           default:        vGradPlane(norm); break; 
        }
        norm[0] *= -1.0;    // 2015 Oct 7, A189
        norm[1] *= -1.0;    // 2015 Oct 7, A189
        norm[2] = +1.0;     // 2015 Oct 7, A189
        normalize(norm); 
    }

    static public double dSawtoothX(double x, double surf[])
    {
        int t = (int) surf[OTYPE];
        boolean bArray = ((t==OTLENSARRAY) || (t==OTMIRRARRAY) || (t==OTIRISARRAY)); 
        double diax = surf[OODIAX]; 
        int nx = (int) surf[ONARRAYX]; 
        if (!bArray || (diax<=TOL) || (nx<1))
          return x; 
        double px = diax/nx;
        return U.sawtooth(x, px, U.isOdd(nx)); 
    }

    static public double dSawtoothY(double y, double surf[])
    {
        int t = (int) surf[OTYPE];
        boolean bArray = ((t==OTLENSARRAY) || (t==OTMIRRARRAY) || (t==OTIRISARRAY)); 
        double diay = surf[OODIAY]; 
        int ny = (int) surf[ONARRAYY]; 
        if (!bArray || (diay<=TOL) || (ny<1))
          return y; 
        double py = diay/ny;
        return U.sawtooth(y, py, U.isOdd(ny));
    }


    /*------------private "z" helper functions---------------------*/
    /*------------private "z" helper functions---------------------*/
    /*------------private "z" helper functions---------------------*/


    static private double zPolyRevCombiner(double x, double y, double surf[])
    // Copyright 2007 M.Lampton STELLAR SOFTWARE all rights reserved.
    {
        return zConic(x, y, surf) + zPoly2D(x, y, surf); 
    }


    static private double zZernRevCombiner(double x, double y, double surf[])
    // Copyright 2007 M.Lampton STELLAR SOFTWARE all rights reserved.
    {
        return zConic(x, y, surf) + zPoly2D(x, y, surf) + zZern(x, y, surf); 
    }


    static private double zZernTorCombiner(double x, double y, double surf[])
    // Copyright 2007 M.Lampton STELLAR SOFTWARE all rights reserved.
    {
        return zToric(x, y, surf) + zZern(x, y, surf); 
    }


    static private double zConic(double x, double y, double surf[])
    // Evaluates z for a conic of revolution. 
    // Ignores GT and LT hints, has only principal hemi + flange.
    // Copyright 2006 STELLAR SOFTWARE all rights reserved.
    {
        double r2 = x*x + y*y; 
        double s = surf[OASPHER] + 1; 
        double c = surf[OCURVE]; 
        double arg = 1.0 - s*c*c*r2; 
        double z = 0.0; 
        if (arg <= 0.0)
          z = 1.0/(s*c); // flange
        else
          z = c*r2/(1.0 + Math.sqrt(arg)); 
        return z; 
    }


    static private double zXcyl(double x, double y, double surf[])
    // Circular cylinder, axis = y, varies in x; y is unused. 
    // Special case of conic; worksaver. 
    // Copyright 2006 STELLAR SOFTWARE all rights reserved.
    {
        double cx = surf[OCURVX]; 
        double argx = 1.0 - cx*cx*x*x; 
        double z = 0.0; 
        if (argx <= 0.0)
          z = 1.0/cx;   // flange
        else
          z = cx*x*x/(1.0 + Math.sqrt(argx));  
        return z; 
    }


    static private double zYcyl(double x, double y, double surf[])
    // Conic cylinder, axis = x, varies in y; x is unused. 
    // Special case of conic; worksaver. 
    // Copyright 2006 STELLAR SOFTWARE all rights reserved.
    {
        double cy = surf[OCURVE]; 
        double sy = surf[OASPHER] + 1; 
        double argy = 1.0 - sy*cy*cy*y*y; 
        double z = 0.0; 
        if (argy <= 0.0)
          z =  1/(sy*cy);  // flange
        else
          z = cy*y*y/(1.0 + Math.sqrt(argy));
        return z; 
    }


    static private double zToric(double x, double y, double surf[])
    // A general toric polynomial surface is generated.
    // Circular in x,z; general in y,z.
    // Copyright 2006 STELLAR SOFTWARE all rights reserved.
    {
        double sy = surf[OASPHER] + 1.0; 
        double cx = surf[OCURVX]; 
        double cy = surf[OCURVE]; 

        double arg1 = 1.0 - sy*cy*cy*y*y; 
        if (arg1 <= 0.0)
        {
            return 0.0;
        } 

        arg1 = Math.sqrt(arg1); 
        double fy = cy*y*y/(1+arg1); 

        //---------add polynomial terms-----------

        // y = Math.abs(y); // exploring this change A190 Dec 2015
        double sum = 0.0; 
        for (int i=14; i>=1; i--)
          sum = (sum + surf[i+OA1-1])*y;
        fy += sum;
        
        //--------revolve fy around displaced y axis------

        double ratio = 1-cx*fy;   // minorRadius/majorRadius
        if (ratio <= 0.0)
        {
            return 0.0;
        } 
        double arg2 = ratio*ratio - cx*cx*x*x; 
        if (arg2 <= 0.0)
        {
            return 0.0;
        } 
        arg2 = Math.sqrt(arg2); 
        double z = fy + cx*x*x/(ratio + arg2); 
        return z; 
    }

    static private double zBiconic(double x, double y, double surf[])
    // Evaluates z for a biconic surface.
    // Copyright 2010 STELLAR SOFTWARE all rights reserved. 
    {
        double s = surf[OASPHX] + 1; 
        double t = surf[OASPHY] + 1; 
        double cx = surf[OCURVX]; 
        double cy = surf[OCURVY]; 
        double arg = 1 - s*cx*cx*x*x - t*cy*cy*y*y; 
        if (arg < 0)
          return -0.0; 
        double root = Math.sqrt(arg); 
        return (cx*x*x + cy*y*y)/(1 + root);      
    }


    static private double zPoly2D(double x, double y, double surf[])
    // Polynomial of revolution, uses both x and y.
    // Copyright 2006 STELLAR SOFTWARE all rights reserved.
    {
        double r2 = x*x + y*y; 
        double r = Math.sqrt(r2); 
        double sum = 0.0; 
        for (int i=14; i>=1; i--)
          sum = (sum + surf[i+OA1-1])*r;
        return sum;
    }


    static private double zZern(double x, double y, double surf[])
    // Always fully 2-dimensional. 
    // Copyright 2006 STELLAR SOFTWARE all rights reserved.
    {
        double D = Math.max(surf[OODIAX], surf[OODIAY]); 
        if (D <= 0.0)
        {
            return 0.0;
        } 
        double r = Math.sqrt(x*x + y*y); 
        double rnorm = 2.0*r/D; 
        if (rnorm > 1.0+EPS)
        {
            return 0.0;
        } 
        double theta = Math.atan2(y, x); 
        double sum = 0.0; 
        for (int index=0; index<=35; index++)
        {
           int iatt = index + OZ00; 
           if (surf[iatt]!=0.0)
             sum += surf[iatt]*zernTerm(index, rnorm, theta); 
        }
        return sum; 
    }



    //----------------Zernike support---------------------

    static private double zernTerm(int index, double rho, double theta)
    // rho = normalized radius, 0 to 1.0; theta in radians
    // Copyright 2006 STELLAR SOFTWARE all rights reserved
    {
        return radialZern(index, rho) * azimuthalZern(index, theta); 
    } 


    static private double azimuthalZern(int index, double theta)
    // Azimuthal part of a Zernike function.
    // index runs 0...35 avoiding Wyant index 36; theta in radians
    // Copyright 2006 STELLAR SOFTWARE all rights reserved
    {
        if (index <= 0)     // piston
          return 1.0;       
        int halfsum = (int) Math.sqrt(index); 
        int idif = index - halfsum*halfsum; 
        int halfdif = idif/2; 
        int n = halfsum + halfdif; 
        int m = halfsum - halfdif; 
        return ((idif%2)==0) ? Math.cos(m*theta) : Math.sin(m*theta); 
    }


    static private double radialZern(int index, double rho)
    // radial Zernike polynomials: Born & Wolf p.523 section 9.2.1 eq.5:
    // n = radial index;      0 <= n; can be even or odd
    // m = azimuthal index;   0 <= m <= n; oddness(m)=oddness(n)
    // rho = normalized radius; 0 <= rho <= 1.
    // Copyright 2006 STELLAR SOFTWARE all rights reserved
    {
        if ((rho<0.0) || (rho>1.0+EPS))
          return 0.0; 
        int halfsum = (int) Math.sqrt(index); 
        int idif = index - halfsum*halfsum; 
        int halfdif = idif/2; 
        int n = halfsum + halfdif; 
        int m = halfsum - halfdif; 

        double result = 0.0;
        for (int s=0; s<=halfdif; s++)
        {
           double expo = n-2*s; 
           double sign = ((s%2)==0) ? 1.0 : -1.0;
           double numer = sign * fac(n-s); 
           double denom = fac(s) * fac(halfsum-s) * fac(halfdif-s);
           double coef = numer/denom; 
           double term = coef * Math.pow(rho, expo);
           result += term; 
        }
        return result;  
    }








    /*-------------------Part Two: Derivatives---------------------*/
    /*-------------------Part Two: Derivatives---------------------*/
    /*-------------------Part Two: Derivatives---------------------*/


    /*-------------------Derivative Combiners----------------------*/


    static private void vGradPolyRevCombiner(double x, double y,
                        double surf[], double grad[])
    // Combines Conic and Poly2D; gives unnormalized gradient. 
    // Copyright 2006 STELLAR SOFTWARE all rights reserved
    {
        double tempC[] = new double[3];
        double tempP[] = new double[3];
        vGradConic(x, y, surf, tempC); 
        vGradPolyRev(x, y, surf, tempP); 
        grad[0] = tempC[0] + tempP[0];
        grad[1] = tempC[1] + tempP[1]; 
    }


    static private void vGradZernRevCombiner(double x, double y,
                        double surf[], double grad[])
    // Combines conic + polyRev + Zern, gives  net 2D gradient.
    {
        double tempC[] = new double[3];
        double tempP[] = new double[3];
        double tempZ[] = new double[3];
        vGradConic(x, y, surf, tempC); 
        vGradPolyRev(x, y, surf, tempP); 
        vGradZern(x, y, surf, tempZ);
        grad[0] = tempC[0] + tempP[0] + tempZ[0];
        grad[1] = tempC[1] + tempP[1] + tempZ[1]; 
    }


    static private void vGradZernTorCombiner(double x, double y,
                        double surf[], double grad[])
    // Combines toric + Zern, gives total gradient. 
    {
        double tempT[] = new double[3];
        double tempZ[] = new double[3];
        vGradToric(x, y, surf, tempT); 
        vGradZern(x, y, surf, tempZ);
        grad[0] = tempT[0] + tempZ[0];
        grad[1] = tempT[1] + tempZ[1]; 
    }

    /*----------------------basic gradients------------------------*/

    static private void vGradPlane(double grad[])
    {
        grad[0] = 0.0; 
        grad[1] = 0.0; 
    }


    static private void vGradConic(double x, double y, double surf[], double grad[])
    // Unnormalized gradient Dx and Dy for the conic function alone.
    // Unnormalized so other gradients can be added; then normalized.
    // Serves only the prime hemiellipsoid, never farside. 
    // Copyright 2006 STELLAR SOFTWARE all rights reserved
    {
        grad[0] = 0.0; 
        grad[1] = 0.0; 
        
        double r2 = x*x + y*y;
        double s = surf[OASPHER] + 1; 
        double c = surf[OCURVE]; 
        double arg = 1.0 - s*c*c*r2; 
        if (arg <= 0.0)  // flange
          return; 
        double coef = c/Math.sqrt(arg); 
        grad[0] = x*coef; 
        grad[1] = y*coef; 
    }

    static private void vGradBiconic(double x, double y, double surf[], double grad[])
    // Unnormalized gradient of the biconic function
    // Unnormalized, so other gradients can be added; then normalized.
    // Copyright 2010 STELLAR SOFTWARE all rights reserved
    {
        grad[0] = 0.0; 
        grad[1] = 0.0; 

        double s = surf[OASPHX] + 1; 
        double t = surf[OASPHY] + 1; 
        double cx = surf[OCURVX]; 
        double cy = surf[OCURVY]; 
        double arg = 1 - s*cx*cx*x*x - t*cy*cy*y*y; 
        if (arg <= 0)
          return;
        double root = Math.sqrt(arg); 
        double opr = 1 + root; 
        double factor = (cx*x*x + cy*y*y)/(root*opr*opr); 
        grad[0] = cx*cx*s*x*factor + 2*cx*x/opr; 
        grad[1] = cy*cy*t*y*factor + 2*cy*y/opr; 
        return; 
    }


    static private void vGradXcyl(double x, double y, double surf[], double grad[])
    // x unnormalized gradient terms for conic surface alone; for addition.
    // Copyright 2006 STELLAR SOFTWARE all rights reserved
    {
        double cx = surf[OCURVX]; 
        double arg = 1.0 - cx*cx*x*x; 
        grad[0] = 0.0; 
        grad[1] = 0.0; 
        if (arg <= 0.0)  
          return; 
        double coef = cx/Math.sqrt(arg); 
        grad[0] = x*coef; 
    }


    static private void vGradYcyl(double x, double y, double surf[], double grad[])
    // y unnormalized gradient terms for conic surface alone; for addition.
    // Copyright 2006 STELLAR SOFTWARE all rights reserved
    {
        double cy = surf[OCURVE]; 
        double sy = surf[OASPHER] + 1.0; 
        double arg = 1.0 - sy*cy*cy*y*y; 
        grad[0] = 0.0; 
        grad[1] = 0.0; 
        if (arg <= 0.0)  
          return; 
        double coef = cy/Math.sqrt(arg); 
        grad[1] = y*coef; 
    }



    static private void vGradPolyRev(double x, double y, double surf[], double grad[])
    // Poly2D alone; gives unnormalized gradient. 
    // Copyright 2006 STELLAR SOFTWARE all rights reserved
    {
        // System.out.println("class Z, vGradPolyRev has been called."); 
        double r2 = x*x + y*y; 
        double r = Math.sqrt(r2); 
        grad[0] = 0.0; 
        grad[1] = 0.0; 
        if (r > 0.0)
        {
            double dfdr = 0.0; 
            double sum = 0.0; 
            for (int i=14; i>=1; i--)
              sum = (sum + i*surf[i+OA1-1])*r;
            dfdr += sum/r; 
            grad[0] += x*dfdr/r; 
            grad[1] += y*dfdr/r; 
        }
    }


    static private void vGradToric(double x, double y, double surf[], double grad[])
    // Gives unnormalized gradient terms for toric alone.
    // Polynomial terms OS1..OA14 are included.
    // Copyright 2007 STELLAR SOFTWARE all rights reserved.
    {
        // System.out.println("class Z, vGradToric has been called."); 
        // double absy = Math.abs(y); 
        double sy = surf[OASPHER] + 1.0; 
        double cx = surf[OCURVX]; 
        double cy = surf[OCURVE]; 
        grad[0] = 0.0; 
        grad[1] = 0.0; 

        //----------conicValue(y)----------
        double arg1 = 1-sy*cy*cy*y*y; 
        if (arg1 <= 0.0)
          return; 
        arg1 = Math.sqrt(arg1); 
        double conicValue = cy*y*y/(1+arg1); 

        //--------conicDeriv(y)---------
        double conicDeriv = cy*y/arg1; 

        //--------polyValue(y)----------
        double polyValue = 0.0; 
        // if (absy>0.0)
          for (int i=14; i>=1; i--)
            polyValue = (polyValue + surf[i+OA1-1])*y;   // y not absy;

        //--------polyDeriv(y)--------
        double polyDeriv = 0.0; 
        if (Math.abs(y)>0.0)   // away from y=0 use this formula
        {
            double sum = 0.0; 
            for (int i=14; i>=1; i--)
              sum = (sum + i*surf[i+OA1-1])*y;    
            polyDeriv = sum/y; 
        }
        else
           polyDeriv = surf[OA1]; 

        double fy = conicValue + polyValue; 
        double dfdy = conicDeriv + polyDeriv; 

        //-------set up the x function----------
        double ratio = 1 - cx*fy;  // = minorRadius/majorRadius
        if (ratio <= 0.0)
          return; 
        double arg2 = ratio*ratio - cx*cx*x*x; 
        if (arg2 <= 0.0)
          return; 
        arg2 = Math.sqrt(arg2); 
        grad[0] = cx * x / arg2; 
        grad[1] = ratio * dfdy / arg2; 
    }




    //-------------Zernike gradient-------------------------------

    static private void vGradZern(double x, double y,
                        double surf[], double grad[])
    // x and y unnormalized gradient terms for pure Zernike surface.
    // Copyright 2006 STELLAR SOFTWARE all rights reserved
    {
        double gradterm[] = new double[2]; 
        grad[0] = grad[1] = gradterm[0] = gradterm[1] = 0.0; 
        double Radius = 0.5*Math.max(surf[OODIAX], surf[OODIAY]); 
        if (Radius <= 0.0)
          return; 
        double rxy = Math.sqrt(x*x + y*y); 
        double rnorm = rxy/Radius; 
        if (rnorm > 1.0+EPS)
          return; 
        if (rnorm < 2E-17)  // central patch
        {
            grad[0] = surf[OZ01]/Radius;
            grad[1] = surf[OZ02]/Radius;
            return; 
        }
        double xoverr = x/rxy;
        double yoverr = y/rxy; 
        double theta = Math.atan2(y, x);

        for (int iatt=OZ00; iatt<=OZ35; iatt++)
          if (surf[iatt]!=0.0)
          {
              int index = iatt-OZ00; 
              gradterm[0] = gradterm[1] = -0.0; 
              vGradZernTerm(index, rnorm, theta, xoverr, yoverr, gradterm); 
              grad[0] += surf[iatt]*gradterm[0];
              grad[1] += surf[iatt]*gradterm[1];
          }
          grad[0] /= Radius;
          grad[1] /= Radius; 
    }


    static private void vGradZernTerm(int index, double rnorm, 
       double theta, double xnorm, double ynorm, double grad[])
    // index is 0...35 avoiding Wyant numbering discontinuity.
    // r = normalized radius, 0 to 1.0,  t = theta, radians
    // Copyright 2006 STELLAR SOFTWARE all rights reserved
    {
        grad[0] = grad[1] = 0.0;
        if ((index < 0) || (index > 35))
          return; 
        if (index == 0)   // piston; no gradient
          return;       
        int halfsum = (int) Math.sqrt(index); 
        int idif = index - halfsum*halfsum; 
        int halfdif = idif/2; 
        int n = halfsum + halfdif; 
        int m = halfsum - halfdif; 

        double rad = radialZern(index, rnorm);
        double azi = azimuthalZern(index, theta); 
        double dzdr = dZern_dR(index, rnorm); 
        double dzdt = dZern_dT(index, theta); 
        grad[0] = azi*dzdr*xnorm - rad*dzdt*ynorm/rnorm;
        grad[1] = azi*dzdr*ynorm + rad*dzdt*xnorm/rnorm;
        return; 
    } 


    static private double dZern_dR(int index, double rho)
    // Here, "rho" = radius/SemiDiameter = rnorm.
    // Returns dZ/drho = radial Zernike polynomial derivative.
    // radius 0<r<1
    // Copyright 2006 STELLAR SOFTWARE all rights reserved
    {
        if ((rho<0.0) || (rho>1.0+EPS))
          return 0.0;  
        int halfsum = (int) Math.sqrt(index); 
        int idif = index - halfsum*halfsum; 
        int halfdif = idif/2; 
        int n = halfsum + halfdif; 
        int m = halfsum - halfdif; 

        double result = 0.0;
        for (int s=0; s<=halfdif; s++)
        {
           double expo = n-2*s; 
           double sign = ((s%2)==0) ? 1.0 : -1.0;
           double numer = sign * fac(n-s); 
           double denom = fac(s) * fac(halfsum-s) * fac(halfdif-s);
           double coef = numer/denom; 
           result += coef * expo * Math.pow(rho, expo-1);;
        }
        return result;
    }

    static private double dZern_dT(int index, double t)
    // Returns dZ/dTheta = azimuthal Zernike polynomial derivative.
    // Copyright 2006 STELLAR SOFTWARE all rights reserved
    {
        int halfsum = (int) Math.sqrt(index); 
        int idif = index - halfsum*halfsum; 
        int halfdif = idif/2; 
        int n = halfsum + halfdif;  // radial part
        int m = halfsum - halfdif;  // azimuthal part
        if (m==0)
          return 0.0; 
        return (idif%2==0) ? -m*Math.sin(m*t) : m*Math.cos(m*t); 
    }





//-----------------math utilities--------------------------------



    static private void normalize(double xyz[])
    {
        double r2 = xyz[0]*xyz[0] + xyz[1]*xyz[1] + xyz[2]*xyz[2];
        if (r2 <= 0.0)
        {
            xyz[0] = 0.0; 
            xyz[1] = 0.0; 
            xyz[2] = 1.0;
            return; 
        }
        double r = Math.sqrt(r2); 
        xyz[0] /= r; 
        xyz[1] /= r; 
        xyz[2] /= r; 
    }

    static private double fac(int n)
    // factorial; do not exceed n=166.
    {
       if (n<2)
         return 1.0;
       if (n>166)
         return 1E300;
       double result = 1.0;
       while (n>1)
       {
          result *= n;
          n--;
       }
       return result;
    }
}
