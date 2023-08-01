package com.stellarsoftware.beam;

import javax.swing.*;      // Graphics2D; JFileChooser
import java.io.*;          // Save Data

@SuppressWarnings("serial")

/**
  * H2DPanel extends GPanel, draws 2D binned ray histogram.
  * Random ray responder is installed.
  * A207: eliminates groups
  * 
  * Because the x, y, and z scaling factors are different,
  * integer bins & counts are scaled to a unit cube for display.
  *
  * Automatic scaling only, so far:  
  * Manual & Diameter scaling is not yet installed. 
  * 
  * Converted to QBASE, March 2015, line 655. 
  *
  * @author M.Lampton (c) STELLAR SOFTWARE 2004-2015 all rights reserved.
  */
public class H2DPanel extends GPanel
{
    // public static final long serialVersionUID = 42L;

    //---non static to permit many instances----
    
    final double EXTRAROOM = 2.0;  
    final double EXTRASPAN = 1.2; 
    final double MINSPAN = 1E-8; 
    final int    MAX2DBINS = 100; 

    private double hmin=0, hmax=0, vmin=0, vmax=0;
    private double hmid=0, hspan=1, vmid=0, vspan=1; 
    private String shmin, shmax, svmin, svmax; 

    private double zmax = 10.0; 
    private double histopeak = 1.0; 
    private double histomax = 1.0; 

    private int    CADstyle=0;
    private double az, cosaz=1, sinaz=0; 
    private double el, cosel=1, sinel=0; 

    private int histo[][]; 
    private int nhbins, nvbins; 
    private double dhisto[][]; 
    private int nhticks, nhdigits;
    private int nvticks, nvdigits; 
    private int nzticks, nzdigits; 
    private int results[] = new int[2]; 
    private double hticks[] = new double[10]; 
    private double vticks[] = new double[10];
    private double zticks[] = new double[10];

    private int nsurfs, npSurfs, ngroups, nrays, npRays, ngood;
    private int hsurf, hattr, vsurf, vattr;  
    private int prevGroups[] = new int[MAXSURFS+1]; // detect new groups
    private String hst, vst; 
    private boolean whitebkg, bOrch, bStereo; 



    public H2DPanel(GJIF gj)
    {
        // implicitly calls super() with no arguments
        myGJIF = gj;            // protected; used here & GPanel
        bClobber = true;        // protected; random redo() forces new artwork
        bPleaseParseUO = true;  // protected; allows initial parse. 
    }



    //---------------protected and private methods-------------------


    protected void doTechList(boolean bFullArt) // replaces abstract method
    // Called by GPanel for artwork: new, pan, zoom, & random ray group.
    {
        nsurfs = DMF.giFlags[ONSURFS];                 // always needed.
        nrays = DMF.giFlags[RNRAYS];                   // always needed.
        ngood = RT13.iBuildRays(true);
                

        String warn = getUOwarning(); // private & local
        myGJIF.postWarning(warn); 
        if (warn.length() > 0)
          return; 

        if ((npSurfs != nsurfs) || (npRays != nrays) || bPleaseParseUO)
        {
            doParse();     
            npSurfs = nsurfs; 
            npRays = nrays; 
        } 

        doArt();             // allow random
    }

    void doRotate(int i, int j) // replaces abstract "doXX" method
    {
        double daz = i/3; 
        az += daz; 
        cosaz = U.cosd(az); 
        sinaz = U.sind(az);
        double del = j/3;
        el += del;
        cosel = U.cosd(el); 
        sinel = U.sind(el); 
    }

    boolean doRandomRay() // replaces abstract "doXX" method
    {
        if (RT13.bRunRandomRay())
        {
           addRayToHisto(0); 
           return true; 
        }
        return false; 
    } 

    protected void doCursor(int ix, int iy)  // replaces abstract method
    // delivers current cursor coordinates
    {
        return; 
    }

    protected double getStereo()    // replaces abstract "get" method
    {
        double d = 0.0; 
        boolean bS = "T".equals(DMF.reg.getuo(UO_2D, 17));
        if (bS)
        {
            String ss = DMF.reg.getuo(UO_2D, 18); 
            d = U.suckDouble(ss); 
            if (d == Double.NaN)
              d = 0.0; 
        }
        return d; 
    }

    protected void doSaveData()     // replaces abstract "do" method
    {
        JFileChooser fc = new JFileChooser(); // import javax.swing.*; 
        String sDir = DMF.sCurrentDir; 
        if (sDir != null)
        {
            File fDir = new File(sDir); 
            if (fDir != null)
              if (fDir.isDirectory())
                fc.setCurrentDirectory(fDir);
        } 
        int q = fc.showSaveDialog(null); 
        if (q == JFileChooser.CANCEL_OPTION)
          return; 
        File file = fc.getSelectedFile(); 
        if (file == null)
          return; 

        FileWriter fw = null;              // import java.io.*;
        PrintWriter pw = null;             // import java.io.*;
        try
        {
            fw = new FileWriter(file);
            pw = new PrintWriter(fw);
            for (int j=0; j<nvbins; j++)
            {
                for (int i=0; i<nhbins-1; i++)
                  pw.print(histo[i][j] + ", ");
                pw.println(histo[nhbins-1][j]); 
            }
            fw.close();
        }
        catch (Exception e)
        {}
    } 




    //-----------private methods---------------

    private String getUOwarning()
    // Accessible to Options.
    // Local variables shadow H2D fields.
    // First line of defense, must never crash. 
    {
        String hst = DMF.reg.getuo(UO_2D, 2); 
        int hop = REJIF.getCombinedRayFieldOp(hst); 
        int hsurf = RT13.getSurfNum(hop); 
        int hattr = RT13.getAttrNum(hop); 
        if ((hsurf<0) || (hattr<0) || (hattr>RNATTRIBS))
          return "H var unknown:  "+hst; 

        String vst = DMF.reg.getuo(UO_2D, 3); 
        int vop = REJIF.getCombinedRayFieldOp(vst); 
        int vsurf = RT13.getSurfNum(vop); 
         int vattr = RT13.getAttrNum(vop); 
        if ((vsurf<0) || (vattr<0) || (vattr>RNATTRIBS))
          return "V var unknown:  "+vst; 
        return ""; 
    }
    
    
    private void doParse()
    // Parses UO fields, performs sizing; trace was already run. 
    {
        bPleaseParseUO = false; 

        shmin = new String(""); 
        shmax = new String(""); 
        svmin = new String(""); 
        svmax = new String(""); 

        hst = DMF.reg.getuo(UO_2D, 2); 
        int hop = REJIF.getCombinedRayFieldOp(hst); 
        hsurf = RT13.getSurfNum(hop); 
        hattr = RT13.getAttrNum(hop); 

        vst = DMF.reg.getuo(UO_2D, 3); 
        int vop = REJIF.getCombinedRayFieldOp(vst); 
        vsurf = RT13.getSurfNum(vop); 
        vattr = RT13.getAttrNum(vop); 

        nhbins = U.parseInt(DMF.reg.getuo(UO_2D, 4));  
        nhbins = Math.max(2, Math.min(MAX2DBINS, nhbins)); 
        nvbins = U.parseInt(DMF.reg.getuo(UO_2D, 5)); 
        nvbins = Math.max(2, Math.min(MAX2DBINS, nvbins));

        CADstyle = 0;  
        histo = new int[nhbins][nvbins]; 
        dhisto = new double[nhbins][nvbins]; 

        uxcenter = 0.0;     // Unit cube
        uxspan = EXTRAROOM; // Unit cube
        uycenter = 0.0;     // Unit cube
        uyspan = EXTRAROOM; // Unit cube

        el = U.suckDouble(DMF.reg.getuo(UO_2D, 6));
        cosel = U.cosd(el); 
        sinel = U.sind(el); 
        az = U.suckDouble(DMF.reg.getuo(UO_2D, 7));  
        cosaz = U.cosd(az); 
        sinaz = U.sind(az); 

        //----Automatic scaling from good rays-------

        int ngood=0; 
        RT13.iBuildRays(true); 
        for (int kray=1; kray<=nrays; kray++)
        {
            if (RT13.isRayOK[kray])
            {
                double h = RT13.dGetRay(kray, hsurf, hattr); 
                double v = RT13.dGetRay(kray, vsurf, vattr); 

                if (ngood==0)
                {
                   hmin = hmax = h;
                   vmin = vmax = v; 
                }
                if (h>hmax) hmax=h; 
                if (h<hmin) hmin=h; 
                if (v>vmax) vmax=v; 
                if (v<vmin) vmin=v; 
                ngood++; 
            }
        }

        //----enlarge (min,max) symmetrically if necessary----

        hspan = Math.max(MINSPAN, EXTRASPAN*(hmax-hmin)); 
        vspan = Math.max(MINSPAN, EXTRASPAN*(vmax-vmin)); 
        if (U.areSimilar(hattr, vattr))
          hspan = vspan = Math.max(hspan, vspan); 
        hmid = 0.5*(hmax+hmin); 
        hmin = hmid - 0.5*hspan; 
        hmax = hmid + 0.5*hspan; 
        vmid = 0.5*(vmax+vmin); 
        vmin = vmid - 0.5*vspan; 
        vmax = vmid + 0.5*vspan;

        //----rulerize auto (min,max) and get labels-----------------

        int results[] = new int[2]; 
        U.ruler(hmin, hmax, true, hticks, results); 
        int hnticks = results[0]; 
        int hndigits = results[1]; 
        hmin = hticks[0];     
        hmax = hticks[hnticks-1]; 
        hmid = 0.5*(hmin+hmax); 
        hspan = hmax-hmin; 
        shmin = U.fwd(hmin, 16, hndigits).trim();
        shmax = U.fwd(hmax, 16, hndigits).trim(); 

        U.ruler(vmin, vmax, true, vticks, results); 
        int vnticks = results[0]; 
        int vndigits = results[1]; 
        vmin = vticks[0];     
        vmax = vticks[vnticks-1]; 
        vmid = 0.5*(vmin+vmax); 
        vspan = vmax-vmin; 
        svmin = U.fwd(vmin, 16, vndigits).trim(); 
        svmax = U.fwd(vmax, 16, vndigits).trim(); 

        //----apply diameter limits if requested & local------------

        boolean bHattr = ((hattr==RTXL) || (hattr==RTYL));
        boolean bVattr = ((vattr==RTXL) || (vattr==RTYL));
        boolean bLocal = bHattr && bVattr; 
        if (bLocal && "T".equals(DMF.reg.getuo(UO_2D, 9)))
        {
            double dHDiam = (hattr == RTXL) 
                              ? RT13.surfs[hsurf][OODIAX]
                              : RT13.surfs[hsurf][OODIAY]; 
            double dVDiam = (vattr == RTXL)
                              ? RT13.surfs[vsurf][OODIAX]
                              : RT13.surfs[vsurf][OODIAY];  
            if ((dHDiam > MINSPAN) && (dVDiam > MINSPAN))
            {
                hmin = -0.5*dHDiam; 
                hmax = +0.5*dHDiam; 
                hmid = 0.0;
                hspan = dHDiam; 
                shmin = U.tidy(hmin); 
                shmax = U.tidy(hmax); 
                vmin = -0.5*dVDiam;
                vmax = +0.5*dVDiam;
                vmid = 0.0; 
                vspan = dVDiam; 
                svmin = U.tidy(vmin); 
                svmax = U.tidy(vmax);  
            }
        } 

        //-------apply manual span if requested-----------

        if ("T".equals(DMF.reg.getuo(UO_2D, 10)))
        {
            String tHmin = DMF.reg.getuo(UO_2D, 11); 
            double dHmin = U.suckDouble(tHmin); 
            String tHmax = DMF.reg.getuo(UO_2D, 12); 
            double dHmax = U.suckDouble(tHmax); 
            double dHspan = Math.abs(dHmax - dHmin); 

            String tVmin = DMF.reg.getuo(UO_2D, 13); 
            double dVmin = U.suckDouble(tVmin); 
            String tVmax = DMF.reg.getuo(UO_2D, 14); 
            double dVmax = U.suckDouble(tVmax); 
            double dVspan = Math.abs(dVmax - dVmin); 

            boolean bH = !U.isNegZero(dHmin) && !U.isNegZero(dHmax); 
            boolean bV = !U.isNegZero(dVmin) && !U.isNegZero(dVmax); 
            boolean bSpan = ((dHspan>MINSPAN) && (dVspan>MINSPAN)); 
            if (bH && bV && bSpan)  
            {
                hmax = dHmax; 
                hmin = dHmin;
                hmid = 0.5*(hmax+hmin); 
                hspan = hmax-hmin; 
                shmin = tHmin; 
                shmax = tHmax; 
                vmin = dVmin;
                vmax = dVmax;               
                vmid = 0.5*(vmin+vmax); 
                vspan = vmax-vmin;
                svmin = tVmin;
                svmax = tVmax; 
            }
        } 

        //---all done scaling data to histogram---------------
        //---Finally compute the table-ray histogram--------

        for (int i=0; i<nhbins; i++)
          for (int j=0; j<nvbins; j++)
            histo[i][j] = 0; 
        for (int kray=1; kray<=nrays; kray++)
          if (RT13.isRayOK[kray])
            addRayToHisto(kray); 

        //  ...and get histopeak and dhisto[][]

        fitUnitHeight(); 
    }  // end of doParse().








    //------ARTWORK-----------------
    //------ARTWORK-----------------
    
    void add3Dbase(double xyz[], int op)  // local shorthand; does scaling
    {
        addScaled(xyz[0], xyz[1], xyz[2], op, QBASE);  // GPanel service
    }
    
    void add3Dview(double x, double y, double z, int op)
    {
        double xyz[] = {x, y, z}; 
        viewelaz(xyz);                                    // does rotation
        addScaled(xyz[0], xyz[1], xyz[2], op, QBASE);  // GPanel service
    }

    void viewelaz(double[] xyz)
    // Puts a labframe point xyz into user's el, az viewframe
    // Assumes that globals sinaz...cosel have been preset. 
    // Formulas are for +Z=vertical axis
    {
        double horiz = xyz[0]*cosaz - xyz[1]*sinaz;
        double vert =  xyz[0]*sinel*sinaz + xyz[1]*sinel*cosaz + xyz[2]*cosel;
        double outof = -xyz[0]*cosel*sinaz - xyz[1]*cosel*cosaz + xyz[2]*sinel;
        xyz[0] = horiz;
        xyz[1] = vert;
        xyz[2] = outof;
    }




    private void doArt()
    {
        nsurfs = DMF.giFlags[ONSURFS];
        nrays = DMF.giFlags[RNRAYS];
        double xyz[] = new double[3]; 

        int iFontcode = getUOGraphicsFontCode();  
        int iHpoints = iFontcode / 10000;     
        int iWpoints = 1 + iHpoints / 2;   
        double scaledW = iWpoints * uxspan / dUOpixels; 
        double xtick = 0.5 * iWpoints * uxspan / dUOpixels; 
        double ytick = 0.5 * iWpoints * uyspan / dUOpixels;  

        //------------setup artwork------------
        
        whitebkg = "T".equals(DMF.reg.getuo(UO_2D, 15));
        clearList(QBASE);  
        addRaw(0., 0., 0., (whitebkg ? SETWHITEBKG : SETBLACKBKG), QBASE);
        addRaw(0., 0., 0., SETCOLOR+(whitebkg ? BLACK : WHITE), QBASE); 
        addRaw(1., 0., 0., SETSOLIDLINE, QBASE); 
        addRaw(0., 0., 0., COMMENTRULER, QBASE); 
        addAffines();   // doesn't seem to help fit the graphic into the window

        //----------------draw the floor-------------

        add3Dview(-0.5, -0.5, -0.5, MOVETO);
        add3Dview(+0.5, -0.5, -0.5, PATHTO);
        add3Dview(+0.5, +0.5, -0.5, PATHTO);
        add3Dview(-0.5, +0.5, -0.5, PATHTO);
        add3Dview(-0.5, -0.5, -0.5, STROKE);

        //------------add corner labels--------------

        int extra = 4; 

        double yy = -0.5 - extra*ytick; 
        addStringCenter(-0.5, yy, -0.5, shmin); 

        addStringCenter(+0.5, yy, -0.5, shmax); 

        int nchars = svmin.length();
        double xx = -0.5 - (nchars+1)*xtick; 
        addStringCenter(xx, -0.5, -0.5, svmin); 
 
        addStringCenter(xx, +0.5, -0.5, svmax); 

        if ((nhbins < 2) || (nvbins < 2) || (histopeak < 1))
          return; 

        double dx = 1.0/nhbins; 
        double dy = 1.0/nvbins; 

        boolean bHincr = U.sind(az)   < 0.0; 
        boolean bVincr = U.cosd(az)   < 0.0; 
        boolean bDown = U.sind(el)   >= 0.0; 
        boolean bFront = U.sind(az-45) > 0.0;  // ruler in front of mountain
        double xruler = -0.7;
        double yruler = +0.7;

        //------------plot the vertical ruler in back-------------

        if (!bFront)
        {
            addRaw(0., 0., 0., SETCOLOR+(whitebkg ? BLACK : WHITE), QBASE); 
            add3Dview(xruler, yruler, -0.5, MOVETO); 
            add3Dview(xruler, yruler, +0.5, STROKE); 
            for (int i=0; i<nzticks; i++)
            {
                double zz = -0.5 + zticks[i]/zmax; 
                add3Dview(xruler, yruler, zz, MOVETO); 
                add3Dview(xruler+xtick, yruler, zz, STROKE);
                String s = U.fwd(zticks[i], 16, nzdigits).trim(); 
                addStringRight(xruler, yruler, zz, s); 
            }
        }
        
        //---------plot histo within a unit cube------------

        bOrch = "T".equals(DMF.reg.getuo(UO_2D, 0));
        bStereo = "T".equals(DMF.reg.getuo(UO_2D, 17));
        if (bOrch || bStereo)
        {
            addRaw(1., 0., 0., SETSOLIDLINE, QBASE); 
            for (int ia=0; ia<nhbins; ia++)
              for (int ja=0; ja<nvbins; ja++)
              {
                  int i = bHincr ? ia : nhbins-ia-1;
                  int j = bVincr ? ja : nvbins-ja-1;
                  double x = dx*i - 0.5 + dx/2;
                  double y = dy*j - 0.5 + dx/2;
                  double h = dhisto[i][j]; 
                  add3Dview(x, y, -0.5, MOVETO);
                  add3Dview(x, y,  h, STROKE);
              }
        }
        else // manhattan format
        {
            for (int ia=0; ia<nhbins; ia++)
              for (int ja=0; ja<nvbins; ja++)
              {
                  int i = bHincr ? ia : nhbins-ia-1;
                  int j = bVincr ? ja : nvbins-ja-1;

                  double x = dx*i - 0.5;
                  double y = dy*j - 0.5;
                  double h = dhisto[i][j]; 
                  if (!bDown)
                    addLid(x, y, dx, dy, h); 

                  if (!bHincr && !bVincr)  // viewquadrant=zero
                  {
                      addFace(x+dx, y,    dy, 1, h, BLUE); 
                      addFace(x,    y+dy, dx, 0, h, BLACK); 
                      addFace(x,    y,    dy, 1, h, DKGRAY); 
                      addFace(x,    y,    dx, 0, h, RED); 
                  }
                  else if (!bHincr && bVincr) // one
                  {
                      addFace(x+dx, y,    dy, 1, h, BLUE); 
                      addFace(x,    y,    dx, 0, h, RED); 
                      addFace(x,    y,    dy, 1, h, DKGRAY); 
                      addFace(x,    y+dy, dx, 0, h, BLACK); 
                      addLid(x, y, dx, dy, h); 
                  }
                  else if (bHincr && bVincr)  // two
                  {
                      addFace(x,    y,    dy, 1, h, DKGRAY); 
                      addFace(x,    y,    dx, 0, h, RED); 
                      addFace(x+dx, y,    dy, 1, h, BLUE); 
                      addFace(x,    y+dy,  dx, 0, h, BLACK); 
                  }
                  else  // viewquadrant three
                  {
                      addFace(x,    y,    dy, 1, h, DKGRAY); 
                      addFace(x,    y+dy, dx, 0, h, BLACK); 
                      addFace(x+dx, y,    dy, 1, h, BLUE); 
                      addFace(x,    y,    dx, 0, h, RED); 
                  }
                  if (bDown)
                    addLid(x, y, dx, dy, h); 
              }
        }

        //------------plot the vertical ruler in front-------------

        if (bFront)
        {
            addRaw(0., 0., 0., SETCOLOR+(whitebkg ? BLACK : WHITE), QBASE); 
            add3Dview(xruler, yruler, -0.5, MOVETO); 
            add3Dview(xruler, yruler, +0.5, STROKE); 
            for (int i=0; i<nzticks; i++)
            {
                double zz = -0.5 + zticks[i]/zmax; 
                add3Dview(xruler, yruler, zz, MOVETO); 
                add3Dview(xruler+xtick, yruler, zz, STROKE);
                String s = U.fwd(zticks[i], 16, nzdigits).trim(); 
                addStringRight(xruler, yruler, zz, s); 
            }
        }
    }  // end of doArt()



    private void addRayToHisto(int kray)
    {
       double h = RT13.dGetRay(kray, hsurf, hattr); 
       h = (h-hmin)/(hmax-hmin); 
       if ((h<0) || (h>0.99999999))
         return; 
       double v = RT13.dGetRay(kray, vsurf, vattr); 
       v = (v-vmin)/(vmax-vmin); 
       if ((v<0) || (v>0.99999999))
         return; 
       int ih = (int) (nhbins*h); 
       int iv = (int) (nvbins*v); 
       if ((ih>=0) && (ih<nhbins) && (iv>=0) && (iv<nvbins))
       {
          histo[ih][iv]++; 
          fitUnitHeight(); 
       } 
    }


    private void fitUnitHeight()
    // Frequent rescales with random rays.
    // Determine histopeak, and then
    // Normalize dhisto[] to unit height...
    {
        histopeak = 0.0; 
        for (int i=0; i<nhbins; i++)
          for (int j=0; j<nvbins; j++)
            if (histopeak < histo[i][j])
              histopeak = histo[i][j];

        U.ruler(0.0, histopeak, false, zticks, results); 

        // make this an integer ruler..
        nzticks = results[NTICKS]; 
        nzdigits = results[NFRACDIGITS]; 
        if (zticks[nzticks-1] < 1.0)
        {
            nzdigits=0;
            nzticks=2; 
            zticks[0] = 0.0;
            zticks[1] = 1.0; 
        }

        zmax = zticks[nzticks-1]; 

        for (int i=0; i<nhbins; i++)
          for (int j=0; j<nvbins; j++)
            dhisto[i][j] = -0.5 + histo[i][j]/zmax; 
    }


    //--------3D cube-facet methods--------------------------

    void addFace(double x, double y, double d, int u, double h, int color)
    // u=0: panel parallel to x axis; d=dx
    // u=1: panel parallel to y axis; d=dy
    {
        addRaw(0., 0., 0., SETCOLOR+color, QBASE); 
        int v = 1-u; 
        add3Dview(x,     y,     -0.5,   MOVETO);
        add3Dview(x+v*d, y+u*d, -0.5,   PATHTO); 
        add3Dview(x+v*d, y+u*d,   h,    PATHTO); 
        add3Dview(x,     y,       h,    PATHTO); 
        add3Dview(x,     y,     -0.5,   FILL);
    }


    void addLid(double x, double y, double dx, double dy, double h)
    {
        if (h>-0.5)
          addRaw(0., 0., 0., SETCOLOR+DKGRAY, QBASE); 
        else
          addRaw(0., 0., 0., SETCOLOR+LTGRAY, QBASE); 
        add3Dview(x,    y,    h, MOVETO); 
        add3Dview(x+dx, y,    h, PATHTO); 
        add3Dview(x+dx, y+dy, h, PATHTO); 
        add3Dview(x,    y+dy, h, PATHTO); 
        add3Dview(x,    y,    h, FILL); 
    }




    //--------2D and 3D string methods--------------------------
    
    void addStringCenter(double x, double y, double z, String s)
    // Places a string so its center is at [xyz] user coordinates
    // but the string generator writes in scaled screen base coordinates.
    // charwidth etc are generated locally.
    // Char coordinates are LowerLeftOrigin. 
    {
        double xyz[] = {x, y, z}; 
        viewelaz(xyz);            // xyz[] is now in screen frame
        s = s.trim(); 
        int nchars = s.length(); 
        double dmid = 0.5*(nchars+2);   // +2 or +3; dispute. 

        int iFontcode = getUOGraphicsFontCode();  
        int iHpoints = iFontcode / 10000;     
        int iWpoints = 1 + iHpoints / 2;  
        double scaledW = iWpoints * uxspan / dUOpixels; 
        double xtick = 0.5 * iWpoints * uxspan / dUOpixels; 
        double ytick = 0.5 * iWpoints * uyspan / dUOpixels;  
        double yoffset = -0.5 * iHpoints * uyspan / dUOpixels;

        xyz[0] -= scaledW * dmid;
        // xyz[1] += yoffset;        
        for (int k=0; k<nchars; k++)
        {
             int ic = (int) s.charAt(k) + iFontcode; 
             xyz[0] += scaledW; 
             add3Dbase(xyz, ic); 
        }
    }


    void addStringRight(double x, double y, double z, String s)
    // Places a string so its right end is at [xyz].
    // charwidth etc are generated locally.
    {
        double xyz[] = {x, y, z}; 
        viewelaz(xyz);            // xyz[] is now in screen frame

        s = s.trim(); 
        int nchars = s.length();  

        int iFontcode = getUOGraphicsFontCode();  
        int iHpoints = iFontcode / 10000;     
        int iWpoints = 1 + iHpoints / 2;  
        double scaledW = iWpoints * uxspan / dUOpixels; 
        double xtick = 0.5 * iWpoints * uxspan / dUOpixels; 
        double ytick = 0.5 * iWpoints * uyspan / dUOpixels;  
        double yoffset = -0.5 * iHpoints * uyspan / dUOpixels;

        xyz[0] -= scaledW * (nchars+1);
        // xyz[1] += yoffset; 
        for (int k=0; k<nchars; k++)
        {
             int ic = (int) s.charAt(k) + iFontcode; 
             xyz[0] += scaledW; 
             add3Dbase(xyz, ic); 
        }
    }
}
