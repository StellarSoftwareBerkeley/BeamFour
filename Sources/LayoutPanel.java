package com.stellarsoftware.beam;

import javax.swing.*;      // obtain Graphics2D features
import java.util.Arrays;   // to list an int array jhit[] below

@SuppressWarnings("serial")

/**
  * Custom artwork class furnishes layout artwork to GPanel.
  * 26 Dec 2018 A207 line 1123 eliminated negative surfaces
  * 30 Dec 2018 A207.11 skipping negative zero Z ray points -- line 1140; 
  *   ^^ this allows bimodals to skip surfaces leaving -0.0 there.
  *
  * Fontcode accompanies each char, and use CenterOrigin for fontXY.
  * Internal coord frame is user dimensions cube, center origin, +y=up.
  * Output coord frame is dUOpixels cube, center origin, +y=up.
  *
  *
  * Affines: uxcenter, uxspan, uycenter, uyspan, uzcenter, uzspan
  * are protected fields of Gpanel.
  * They are initially computed here by setScaleFactors().
  * Therefter, they are managed by Gpanel in its pan/zoom/twirl behavior. 
  *
  * Random calls doRandomRay: adds 1 ray to myRandList.
  * Draws NRI media shaded.
  *
  * June 2009: added user selectable linewidths for tidier layouts.
  * Rendered on-screen and in bitmaps by GPanel, limited to screen resolution.
  * Also available in PostScript (see CAD)  as of August 2009 for hi-rez.
  *
  * May 2010 A112 added groups. nsurfs for drawing surfs, ngroups for drawing rays. 
  * 
  * Nov 2010 A118 added Sticky view option: az, el, pan, zoom.
  * Layout now has an asterisk when it is Sticky. 
  *
  * May 2011 A126 added Connectors for refractive surface pairs. 
  *
  * July 2011 A127 added bRetroVisible controlling full artwork,
  *  but leaving Retro surfaces always visible in skeleton artwork.
  *
  * 5 Sept 2011 A129 improved separation of Layouts with differing display
  * factors, independent randoms: simultaneous views of one optic.
  *
  * Regeneration policy: no need to regenerate if nsurfs changes;
  *   therefore doParse() only at startup and UOptions change.
  *   This policy retains current pan & zoom.
  *
  * 3 Aug 2012 A136: zero width lines now disappear. 
  *
  * 18 Aug 2012 A136custom: trying {shading,arcs,rays,finish} in doFullArt() line 962
  *  ooops won't work: doFullArt is sorted, i.e. near shading must block distant arcs.
  *
  * 10 Oct 2012 A137: coordinate break introduced, line 1185
  *
  * M.Lampton STELLAR SOFTWARE (c) 2004-2012 all rights reserved.
  */
public class LayoutPanel extends GPanel   // implements B4constants via GPanel
{
    // public static final long serialVersionUID = 42L;

    final private static double EXTRAROOM = 2.0;
    final private static double MINSPAN = 1E-8;   // ???!!
    final private static double IRISTICKFRAC = 0.10;
    final private static double EXTRADIAM = 1.01;

    //-----identifiers for sorted component index iii[]----------------
    final private int RADSHA  = 0; 
    final private int OUTSHA  = 1; 
    final private int INNSHA  = 2; 
    final private int RADARC  = 3;
    final private int OUTARC  = 4; 
    final private int INNARC  = 5; 
    final private int SPIHOLE = 6; 
    final private int ARRHOLE = 7; 
    final private int ARRRECT = 8; 
    final private int CONNECT = 9; 
 
    //-----------doSizing parameters--------------------
    private int nsurfs, ngroups, nrays, ngood, nsegs, nspaces;  
    private double span=1.0; 
    private double az=0.0, cosaz=1.0, sinaz=0.0;
    private double el=0.0, cosel=1.0, sinel=0.0; 
    private double xmin=0, xmax=0, ymin=0, ymax=0, zmin=0, zmax=0; // rulers
    private double widthRays=1.0, widthSurfs=2.0, widthAxes=1.0;    // line widths from UO

    //---------setLocalArrays() parameters-----------------
    private boolean bArcs[] = new boolean[8];           // 0-3=radial; 4-7=peripheral.
    private boolean bShad[] = new boolean[8];           // 0-3=radial; 4-7=peripheral.
    private boolean bConn[] = new boolean[8];           // 0-3=radial; 4-7=peripheral.
    private boolean bPosInnerR[] = new boolean[MAXSURFS+1];
    private boolean bGivenOuterRadius[] = new boolean[MAXSURFS+1];
    private boolean whitebkg = true;
    private boolean bStickyUO = false; 
    private boolean bStickyOK = false; 
    private boolean bRetroVis = true; 
    
    private boolean bPleaseInitScaleFactors = true; 
    
    private int nSpiderLegs[] = new int[MAXSURFS+1];  
    private int nArrayElements[] = new int[MAXSURFS+1];  
    private int nArrayX[] = new int[MAXSURFS+1];
    private int nArrayY[] = new int[MAXSURFS+1]; 
    private char cArrayType[] = new char[MAXSURFS+1]; // 'I', 'L', 'M', else not an array.


    //---------the surface radii----see line 528------------------
    private double rox[];  // = new double[MAXSURFS+1];      // working radius; see line 528
    private double roy[];  // = new double[MAXSURFS+1];      // working radius
    private double gox[];  // = new double[MAXSURFS+1];      // given outer radius, x
    private double goy[];  // = new double[MAXSURFS+1];      // given outer radius, y
    private double gix[];  // = new double[MAXSURFS+1];      // given inner radius, x
    private double giy[];  // = new double[MAXSURFS+1];      // given inner radius, y
    private double radox[][];  // = new double[MAXSURFS+1][4];  // radial arc outer endpoints 
    private double radoy[][];  // = new double[MAXSURFS+1][4];  // radial arc outer endpoints
    private double radix[][];  // = new double[MAXSURFS+1][4];  // radial arc inner endpoints 
    private double radiy[][];  // = new double[MAXSURFS+1][4];  // radial arc inner endpoints

    double sinq[]; // circular one quadrant sine
    double cosq[]; // circular one quadrant cosine
    double ssq[];  // square one quadrant sine
    double csq[];  // square one quadrant cosine

    double c4[]   = {+1.0, 0.0, -1.0, 0.0}; 
    double s4[]   = {+0.0, +1.0, +0.0, -1.0};
    double c45[]  = {0.7071, -0.7071, -0.7071, 0.7071};
    double s45[]  = {0.7071, 0.7071, -0.7071, -0.7071};  

    // Arcs: i= 0..3=N,E,S,W=radial; 4..7=NE..NW=periphery.
    double arcx[] = {+1.0, -1.0, -1.0, +1.0}; // arcs
    double arcy[] = {+1.0, +1.0, -1.0, -1.0}; // arcs


    public LayoutPanel(GJIF gj)
    {
        myGJIF = gj;            // protected; used here & GPanel
        bClobber = false;       // protected; random() keeps old artwork
        bPleaseParseUO = true;  // for initial startup
    }

    //---protected methods mandated by GPanel--------
    
    protected void doTechList(boolean bArtStatus) // replaces abstract "do"
    // Called by GPanel when fresh artwork is needed: new pan, zoom, rotate.  
    // But this is not called for annotations or caret blinks.
    // For annotation, host's bitmap of TechList is blitted instead,
    // then annotation and caret are written atop the bitmap. 
    // Copyright 2006 STELLAR SOFTWARE all rights reserved
    {
        nsurfs = DMF.giFlags[ONSURFS]; 
        nrays = DMF.giFlags[RNRAYS];  
        ngood = RT13.iBuildRays(true);
        int istatus = DMF.giFlags[STATUS];
        boolean bOK = (GPARSEOK==istatus) || (GLAYOUTONLY==istatus); 
        if (!bOK)
        {
            doBlank(); 
            return; 
        }
        
        if (bPleaseParseUO)     // set at startup and by Options;
          doParseUO();          // else parse would clobber twirl!
        bPleaseParseUO = false;
        
        if (bPleaseInitScaleFactors)
           setScaleFactors();    // only at startup.
        bPleaseInitScaleFactors = false; 
          
        doArrays(); 
        
        if (bArtStatus == bFULLART)
        {
            doFullArt();   // happily ignorant of stereo
            saveCurrentAffines(); 
        }
        else
          doSkeleton();    // happily ignorant of stereo
    }


    protected void doRotate(int i, int j) // replaces abstract "do" method
    // Called by base class GPanel for mouse rotations.
    // Sets up private sinel, cosel for next artwork run.
    // Next artwork run will be requested by GPanel. 
    // Copyright 2006 STELLAR SOFTWARE all rights reserved
    {
        i = i/3; // integer divide to ignore small mouse motions
        az += i; 
        cosaz = U.cosd(az); 
        sinaz = U.sind(az);
        j = j/3; // integer divide to ignore small mouse motions
        el += j; 
        cosel = U.cosd(el); 
        sinel = U.sind(el); 
    }


    protected boolean doRandomRay() // replaces abstract "do" method
    {
        return drawOneRay(0, QBATCH);     // local version
    } 
    

    protected void doCursor(int ix, int iy)  // replaces abstract method
    // Called by cursor motion; 
    // Delivers current cursor coordinates to title bar.
    // NOTE: SCALEFACTOR DISPLAY DWELLS IN GPANEL NOT HERE. 
    {
        String title = bStickyUO ? "Layout (sticky)" : "Layout"; 
        if (ix<0)
          myGJIF.cleanupTitle(title); // retains any warnings
        else
          myGJIF.postCoords(title + "Hor=" + U.fwd(getuxPixel(ix),18,6).trim() 
                               + "  Vert=" + U.fwd(getuyPixel(iy),18,6).trim());
    }

    protected double getStereo()    // replaces abstract "get" method
    // Not used locally because local methods are pure monoscopic.
    // Allows GPanel base class to get user's stereo preference. 
    {
        if ("T".equals(DMF.reg.getuo(UO_LAYOUT, 17)))
        {
            String ss = DMF.reg.getuo(UO_LAYOUT, 18); 
            double d = U.suckDouble(ss); 
            if (d == Double.NaN)
              d = 0.0; 
            return d; 
        }
        return 0.0; 
    }

    protected void doSaveData()     // replaces abstract "do" method
    {
        return; 
    } 



    //---------------private area----------------------
    
   
    private void doParseUO()
    // Called as part of doTechList() for new UOs. 
    // No UO fields to validate; no failure modes. 
    // Trace was already run as part of doTechList().
    {
        bPleaseParseUO = false; // mandatory to allow pan/zoom/twirl
        el = U.suckDouble(DMF.reg.getuo(UO_LAYOUT, 0)); 
        az = U.suckDouble(DMF.reg.getuo(UO_LAYOUT, 1)); 
        cosel = U.cosd(el); 
        sinel = U.sind(el); 
        cosaz = U.cosd(az); 
        sinaz = U.sind(az); 
        bStickyUO = "T".equals(DMF.reg.getuo(UO_LAYOUT, 2)); 
        myGJIF.setTitle(bStickyUO ? "Layout (sticky)" : "Layout"); 
        bRetroVis = "T".equals(DMF.reg.getuo(UO_LAYOUT, 38)); 
        
        //-------set up arcs and segments------------------

        nsegs = U.suckInt(DMF.reg.getuo(UO_LAYOUT, 3));
        nsegs = Math.max(2, Math.min(100, nsegs)); 
        boolean bHasArray = false; 
        for (int j=1; j<=MAXSURFS; j++)
        {
           int t = (int) RT13.surfs[j][OTYPE]; 
           if (t==OTLENSARRAY)
             bHasArray = true; 
           if (t==OTMIRRARRAY)
             bHasArray = true; 
        }
        if (bHasArray)
          nsegs *= 5; 

        nspaces = U.isOdd(nsegs) ? nsegs+1 : nsegs; 

        //--------set up numerical segment constants---------------

        sinq = new double[nsegs+1]; 
        cosq = new double[nsegs+1]; 
        ssq  = new double[nsegs+1]; 
        csq  = new double[nsegs+1]; 

        for (int i=0; i<=nsegs; i++)
        {
            sinq[i] = U.sind(i*90.0/nsegs); 
            cosq[i] = U.cosd(i*90.0/nsegs); 
            ssq[i] = Math.min(i*2.0/nsegs, 1.0); 
            csq[i] = Math.min(1.0, (nsegs-i)*2.0/nsegs);
        }

        //---------arcs & shadings-----------------

        boolean bShading = "T".equals(DMF.reg.getuo(UO_LAYOUT, 27)); 
        boolean bConnect = "T".equals(DMF.reg.getuo(UO_LAYOUT, 37)); 
        for (int i=0; i<8; i++)       // 8 curves + 8 shadings, refresh OK
        { 
             boolean bThisArc = "T".equals(DMF.reg.getuo(UO_LAYOUT, 19+i)); 
             bArcs[i] = bThisArc; 
             bShad[i] = bThisArc && bShading;
             bConn[i] = bThisArc && bConnect; 
        } 

        // modify the shadecodes to skip buried shade panels; refresh OK
        if (bShad[7] && bShad[4])  // NE & NW kill N
          bShad[0] = false; 
        if (bShad[4] && bShad[5])  // NE & SE kill E
          bShad[1] = false; 
        if (bShad[5] && bShad[6])  // SE & SW kill S
          bShad[2] = false; 
        if (bShad[6] && bShad[7])  // SW & NW kill W
          bShad[3] = false; 
          
        //-----line widths, pixels------------------
        
        widthRays  = U.minmax(U.suckDouble(DMF.reg.getuo(UO_LAYOUT, 28)),0,5);
        widthSurfs = U.minmax(U.suckDouble(DMF.reg.getuo(UO_LAYOUT, 29)),0,5);
        widthAxes  = U.minmax(U.suckDouble(DMF.reg.getuo(UO_LAYOUT, 30)),0,5);
        
    }  // end of doParseUO()



    private void setScaleFactors()
    // Sets sets affines for host GPanel;
    // also sets global extrema xmin...zmax for local drawRulers().
    // How to get these extrema, in sticky mode?
    {
          
        //----find out if we can use previous sticky parameters----
       
        double uo[] = new double[6]; 
        bStickyOK = bStickyUO; 
        if (bStickyUO)
        {
            for (int i=0; i<6; i++) // xc,yc,zc,xspan,yspan,zspan
              uo[i] = U.suckDouble(DMF.reg.getuo(UO_LAYOUT, 31+i)); 
            for (int i=3; i<6; i++)
              if (uo[i] == 0.0)
                bStickyOK = false; 
        }
            
        if (bStickyOK)
        {
            el = U.suckDouble(DMF.reg.getuo(UO_LAYOUT, 0)); 
            az = U.suckDouble(DMF.reg.getuo(UO_LAYOUT, 1)); 
            cosel = U.cosd(el); 
            sinel = U.sind(el); 
            cosaz = U.cosd(az); 
            sinaz = U.sind(az); 
            uxcenter = uo[0]; 
            uycenter = uo[1]; 
            uzcenter = uo[2];
            uxspan = uo[3];
            uyspan = uo[4];
            uzspan = uo[5];             
                
            // now set up the ruler limits:
                
            xmin = uxcenter - uxspan; 
            xmax = uxcenter + uxspan;
            ymin = uycenter - uyspan;
            ymax = uycenter + uyspan;
            zmin = uzcenter - uzspan;
            zmax = uzcenter + uzspan; 
        }
        else
          getNewAffines(); 
    }
    
    
    
    private void getNewAffines()
    {   
        xmin=0; xmax=0; ymin=0; ymax=0; zmin=0; zmax=0;  // globals
        
        double hmin=0.0, hmax=0.0, vmin=0.0, vmax=0.0, omin=0.0, omax=0.0; 
        double rxmin=0, rxmax=0, rymin=0, rymax=0, rzmin=0, rzmax=0, rdiam=0; 
        double diam=0;
        double xyz[] = new double[3]; 
        for (int j=1; j<=nsurfs; j++)
        {
            if (j==1)   // lab frame extrema
            {
                xmin = xmax = RT13.surfs[j][OX]; 
                ymin = ymax = RT13.surfs[j][OY]; 
                zmin = zmax = RT13.surfs[j][OZ];
            }
            xmin = Math.min(xmin, RT13.surfs[j][OX]); 
            xmax = Math.max(xmax, RT13.surfs[j][OX]); 
            ymin = Math.min(ymin, RT13.surfs[j][OY]);
            ymax = Math.max(ymax, RT13.surfs[j][OY]); 
            zmin = Math.min(zmin, RT13.surfs[j][OZ]); 
            zmax = Math.max(zmax, RT13.surfs[j][OZ]);

            //----set up for viewframe------

            xyz[0] = RT13.surfs[j][OX];
            xyz[1] = RT13.surfs[j][OY];
            xyz[2] = RT13.surfs[j][OZ];

            viewelaz(xyz); // move into viewframe

            if (j==1)   // initialize viewframe extrema
            {
                hmin = hmax = xyz[0]; 
                vmin = vmax = xyz[1]; 
                omin = omax = xyz[2]; 
            }
            
            // include the declared Diameter here
            diam = Math.max(RT13.surfs[j][OODIAX], RT13.surfs[j][OODIAY]); 
            hmin = Math.min(hmin, xyz[0]-diam);
            hmax = Math.max(hmax, xyz[0]+diam); 
            vmin = Math.min(vmin, xyz[1]-diam); 
            vmax = Math.max(vmax, xyz[1]+diam); 
            omin = Math.min(omin, xyz[2]-diam); 
            omax = Math.max(omax, xyz[2]+diam); 
        }

        //---include raystarts, both extent and locations--------

        rxmin = rxmax = RT13.raystarts[1][RX]; 
        rymin = rymax = RT13.raystarts[1][RY]; 
        rzmin = rzmax = RT13.raystarts[1][RZ]; 
        
        for (int k=1; k<=nrays; k++)
        {
            rxmin = Math.min(rxmin, RT13.raystarts[k][RX]); 
            rxmax = Math.max(rxmax, RT13.raystarts[k][RX]); 
            rymin = Math.min(rymin, RT13.raystarts[k][RY]); 
            rymax = Math.max(rymax, RT13.raystarts[k][RY]); 
            rzmin = Math.min(rzmin, RT13.raystarts[k][RZ]); 
            rzmax = Math.max(rxmax, RT13.raystarts[k][RZ]); 
            
            xmin  = Math.min(xmin, RT13.raystarts[k][RX]); 
            xmax  = Math.max(xmax, RT13.raystarts[k][RX]); 
            ymin  = Math.min(ymin, RT13.raystarts[k][RY]); 
            ymax  = Math.max(ymax, RT13.raystarts[k][RY]); 
            zmin  = Math.min(zmin, RT13.raystarts[k][RZ]); 
            zmax  = Math.max(zmax, RT13.raystarts[k][RZ]);

            xyz[0] = RT13.raystarts[k][RX];   // labframe
            xyz[1] = RT13.raystarts[k][RY];   // labframe
            xyz[2] = RT13.raystarts[k][RZ];   // labframe
            
            viewelaz(xyz);                    // go to viewframe
            
            hmax = Math.max(hmax, xyz[0]);
            hmin = Math.min(hmin, xyz[0]); 
            vmax = Math.max(vmax, xyz[1]);
            vmin = Math.min(vmin, xyz[1]); 
            omax = Math.max(omax, xyz[2]); 
            omin = Math.min(omin, xyz[2]); 
        }
        
        //--evaluate the raystart sphere-----
    
        rdiam = rxmax - rxmin;
        rdiam = Math.max(rdiam, rymax - rymin); 
        rdiam = Math.max(rdiam, rzmax - rzmin); 
        
        //---apply the raystart sphere------
        
        hmax = Math.max(hmax,  rdiam); 
        hmin = Math.min(hmin, -rdiam); 
        vmax = Math.max(vmax,  rdiam); 
        vmin = Math.min(vmin, -rdiam); 
        omax = Math.max(omax,  rdiam); 
        omin = Math.min(omin, -rdiam); 
        
        // All done sizing the optic. Set up GPanel scalefactors.
        // Tidy up xmin...zmax to make equal axes

        double xcenter = 0.5*(xmin+xmax); 
        double xradius = 0.5*(xmax-xmin);
        double ycenter = 0.5*(ymin+ymax); 
        double yradius = 0.5*(ymax-ymin);
        double zcenter = 0.5*(zmin+zmax); 
        double zradius = 0.5*(zmax-zmin);

        double radius  = Math.max(Math.max(xradius, yradius), zradius); 

        xmin = xcenter - radius;
        xmax = xcenter + radius;
        ymin = ycenter - radius;
        ymax = ycenter + radius;
        zmin = zcenter - radius;
        zmax = zcenter + radius; 

        //----set the affines in GPanel----
        //---and in doFullArt() be sure to addAffines() before drawing---
        
        uxcenter = 0.5 * (hmax + hmin); // protected field of GPanel
        uycenter = 0.5 * (vmax + vmin); // protected field of GPanel
        uzcenter = 0.5 * (omax + omin); // protected field of GPanel
        
        radius = Math.max(MINSPAN, radius); 
        uxspan = uyspan = uzspan = span = 2*EXTRAROOM*radius; 
        saveCurrentAffines(); 
    }
    
    
    
    private void saveCurrentAffines()
    // Called by getNewAffines() and by doTechList()
    {   
        el = U.put180(el); 
        az = U.put180(az); 
        DMF.reg.putuo(UO_LAYOUT,  0, U.fwd(el,6,1).trim()); 
        DMF.reg.putuo(UO_LAYOUT,  1, U.fwd(az,6,1).trim()); 
        DMF.reg.putuo(UO_LAYOUT, 31, U.fwd(uxcenter,16,8).trim()); 
        DMF.reg.putuo(UO_LAYOUT, 32, U.fwd(uycenter,16,8).trim()); 
        DMF.reg.putuo(UO_LAYOUT, 33, U.fwd(uzcenter,16,8).trim()); 
        DMF.reg.putuo(UO_LAYOUT, 34, U.fwd(uxspan,16,8).trim()); 
        DMF.reg.putuo(UO_LAYOUT, 35, U.fwd(uyspan,16,8).trim()); 
        DMF.reg.putuo(UO_LAYOUT, 36, U.fwd(uzspan,16,8).trim()); 
    }



    private void doArrays()
    // Sets up local arrays with current .OPT information
    {

        for (int j=0; j<=MAXSURFS; j++)  
          bPosInnerR[j] = bGivenOuterRadius[j] = false; 

        //-----get radii: rxx=measured, gxx=given; see line 72---------

        rox = new double[nsurfs+1];
        roy = new double[nsurfs+1]; 
        gox = new double[nsurfs+1]; 
        goy = new double[nsurfs+1]; 
        gix = new double[nsurfs+1]; 
        giy = new double[nsurfs+1]; 

        radox = new double[nsurfs+1][4]; 
        radoy = new double[nsurfs+1][4];
        radix = new double[nsurfs+1][4]; 
        radiy = new double[nsurfs+1][4];

        for (int j=0; j<=nsurfs; j++)
        {
            rox[j] = 0.0; 
            roy[j] = 0.0; 
            goy[j] = 0.5*RT13.surfs[j][OODIAY]; 
            gox[j] = 0.5*RT13.surfs[j][OODIAX]; 
            giy[j] = 0.5*RT13.surfs[j][OIDIAY]; 
            gix[j] = 0.5*RT13.surfs[j][OIDIAX]; 
        }

        //------given inner & outer radii------------
        for (int j=0; j<=nsurfs; j++)
        {
            bPosInnerR[j] = (gix[j]>0.0) || (giy[j]>0.0);
            bGivenOuterRadius[j] = (gox[j]>0.0) || (goy[j]>0.0); 
        }

        for (int j=1; j<=nsurfs; j++)
        {
            rox[j] = gox[j]; 
            roy[j] = goy[j]; 
        }

        //---evaluate any missing radii------------------

        boolean bMissingRadii = false; 
        for (int j=1; j<=nsurfs; j++)
          if (!bGivenOuterRadius[j])
            bMissingRadii = true; 

        if (bMissingRadii) // overwrite absentees with actuals
        {
            for (int k=1; k<=nrays; k++)
            {
                int jmax = RT13.getHowfarOK(k); 
 
                for (int j=1; j<=jmax; j++)
                  if (!bGivenOuterRadius[j])  // absentee found
                  {
                      double xx = RT13.dGetRay(k,j,RTXL) - RT13.surfs[j][OFFOX];
                      double rx = Math.abs(xx); 
                      double yy = RT13.dGetRay(k,j,RTYL) - RT13.surfs[j][OFFOY];
                      double ry = Math.abs(yy); 
                      double rr = 0.0; 
                      if (isOuterRect(j))
                        rr = EXTRADIAM * Math.max(rx, ry);
                      else
                        rr = EXTRADIAM * Math.sqrt(rx*rx + ry*ry);  
                      rox[j] = roy[j] = Math.max(rox[j], rr); 
                  }            
            }
        }

        //------------set up the spiders------------

        for (int j=1; j<=nsurfs; j++)
        {
            nSpiderLegs[j] = 0; 
            if (OTIRIS == RT13.surfs[j][OTYPE])
            {
                int i = (int) RT13.surfs[j][ONSPIDER];
                i = Math.min(10, Math.max(0, i)); 
                nSpiderLegs[j] = i; 
            }
        }

        //-----------set up Arrays------------------

        for (int j=1; j<=nsurfs; j++)
        {
            nArrayX[j] = (int) RT13.surfs[j][ONARRAYX]; 
            nArrayY[j] = (int) RT13.surfs[j][ONARRAYY]; 
            nArrayElements[j] = nArrayX[j] * nArrayY[j]; 
            cArrayType[j] = ' '; 
            
            switch((int) RT13.surfs[j][OTYPE])
            {
                case OTIRISARRAY: cArrayType[j] = 'I'; break; 
                case OTLENSARRAY: cArrayType[j] = 'L'; break;
                case OTMIRRARRAY: cArrayType[j] = 'M'; break;
                default:          cArrayType[j] = ' '; 
            }
        }

        //--------find the endpoints of the Layout radii--------
        //--------serves radial arcs and radial shadings-------
        //--------coord frame is the OuterBoundary frame--------

        for (int j=1; j<=nsurfs; j++)
        {
            int iform = (int) RT13.surfs[j][OFORM];
            boolean bIRect = ((OFIRECT==iform) || (OFBRECT==iform));
            // boolean bORect = ((OFORECT==iform) || (OFBRECT==iform));

            for (int q=0; q<=3; q++)
            {
                radox[j][q] = c4[q] * rox[j];
                radoy[j][q] = s4[q] * roy[j];
                radix[j][q] = 0.0;
                radiy[j][q] = 0.0;  
                if (!bPosInnerR[j])
                  continue; 
                double dx = RT13.surfs[j][OFFIX] - RT13.surfs[j][OFFOX];
                double dy = RT13.surfs[j][OFFIY] - RT13.surfs[j][OFFOY];
                double rx = gix[j]; 
                double ry = giy[j];  
                double r2=0.0, sqrt=0.0; 
                boolean bOdd = U.isOdd(q); 

                r2 = bOdd ? dx*dx/(rx*rx) : dy*dy/(ry*ry); 
                if (r2 < 1.0)
                {
                    sqrt = bIRect ? 1.0 : Math.sqrt(1-r2); 
                    if (bOdd)
                      radiy[j][q] = dy + s4[q]*ry*sqrt; 
                    else
                      radix[j][q] = dx + c4[q]*rx*sqrt;  
                } 
            }
        }
    } // end of doArrays().





    //-----------local math stuff---------------------------

    double cc(boolean bSquare, int i)
    {
        return bSquare ? csq[i] : cosq[i]; 
    }

    double ss(boolean bSquare, int i)
    {
        return bSquare ? ssq[i] : sinq[i];
    } 

    boolean isOuterRect(int j)
    {
        int iform = (int) RT13.surfs[j][OFORM];
        return ((OFORECT==iform) || (OFBRECT==iform));
    }






    ////////// global definitions for sorting ///////////
    ///
    ///   iii = 0 = radial shade = RADSHA
    ///         1 = outer shade  = OUTSHA 
    ///         2 = inner shade  = INNSHA 
    ///         3 = radial arc   = RADARC
    ///         4 = outer arc    = OUTARC 
    ///         5 = inner arc    = INNARC 
    ///         6 = spiderhole   = SPIHOLE
    ///         7 = arrayhole    = ARRHOLE
    ///         8 = arrayrect    = ARRRECT
    ///         9 = connector    = CONNECT
    ///
    /// Better design: use ArrayList whichbase is extendable?
    /// An array can be sorted; don't know about an ArrayList. 

    boolean bbb[] = new boolean[MAXSORT];    // sorted foreground identifier
    int iii[]     = new int[MAXSORT];        // sorted item identifier
    int jjj[]     = new int[MAXSORT];        // sorted surface identifier
    int kkk[]     = new int[MAXSORT];        // sorted quadrant or hole number
    double zzz[]  = new double[MAXSORT];     // sort keys;
    int ncount = 0; 


    private void doZsort()
    /// sets the zsorted drawing list for arcs and shades
    {
        double xyz[] = new double[3]; 
        ncount = 0; 
        for (int j=1; j<=nsurfs; j++)
        {
            if (nSpiderLegs[j] > 0)          // spider!
            {
                int nSpiderHoles = nSpiderLegs[j]; 
                for (int k=0; k<nSpiderHoles; k++)
                {
                    iii[ncount] = SPIHOLE;
                    jjj[ncount] = j; 
                    kkk[ncount] = k;  
                    zzz[ncount] = getZspiderHole(j, k);
                    bbb[ncount] = zzz[ncount] > getZvertex(j); 
                    ncount++;
                }
            }
            else if ('I' == cArrayType[j])  // IrisArray
            {
                for (int k=0; k<4; k++)
                {
                    if (bArcs[4+k])        // outer periph arc
                    {
                       iii[ncount] = OUTARC; 
                       jjj[ncount] = j; 
                       kkk[ncount] = k; 
                       zzz[ncount] = getZouterArc(j, k); 
                       bbb[ncount] = zzz[ncount] > getZvertex(j); 
                       ncount++; 
                    }
                }
                for (int k=0; k<nArrayElements[j]; k++)
                {
                    iii[ncount] = ARRHOLE; 
                    jjj[ncount] = j;
                    kkk[ncount] = k; 
                    zzz[ncount] = getZarrayElement(j, k); 
                    bbb[ncount] = zzz[ncount] > getZvertex(j); 
                    ncount++; 
                }
            }
            else if ('L' == cArrayType[j])  // LensArray
            {
                for (int k=0; k<4; k++)
                {
                    if (bArcs[4+k])        // outer periph arc
                    {
                       iii[ncount] = OUTARC; 
                       jjj[ncount] = j; 
                       kkk[ncount] = k; 
                       zzz[ncount] = getZouterArc(j, k); 
                       bbb[ncount] = zzz[ncount] > getZvertex(j); 
                       ncount++; 
                    }
                }
                for (int k=0; k<nArrayElements[j]; k++)
                {
                    iii[ncount] = ARRRECT; 
                    jjj[ncount] = j;
                    kkk[ncount] = k; 
                    zzz[ncount] = getZarrayElement(j, k); 
                    bbb[ncount] = zzz[ncount] > getZvertex(j); 
                    ncount++; 
                }
            }            
            else if ('M' == cArrayType[j])  // MirrArray
            {
                for (int k=0; k<4; k++)
                {
                    if (bArcs[4+k])        // outer periph arc
                    {
                       iii[ncount] = OUTARC; 
                       jjj[ncount] = j; 
                       kkk[ncount] = k; 
                       zzz[ncount] = getZouterArc(j, k); 
                       bbb[ncount] = zzz[ncount] > getZvertex(j); 
                       ncount++; 
                    }
                }
                for (int k=0; k<nArrayElements[j]; k++)
                {
                    iii[ncount] = ARRRECT; 
                    jjj[ncount] = j;
                    kkk[ncount] = k; 
                    zzz[ncount] = getZarrayElement(j, k); 
                    bbb[ncount] = zzz[ncount] > getZvertex(j); 
                    ncount++; 
                }
            }            
            else if ((OTRETRO == (int) RT13.surfs[j][OTYPE]) && !bRetroVis)
                   continue; 
            
            else  // regular non-array surface...
            {
                double ref = RT13.refractLayoutShading[j]; 
                boolean bRefract = ((ref>1.0) || (ref<0.0));
                for (int k=0; k<4; k++)         // quadrant
                {
                    if (bRefract && bShad[k])   // radial shading
                    {
                       iii[ncount] = RADSHA; 
                       jjj[ncount] = j;
                       kkk[ncount] = k; 
                       zzz[ncount] = getZradialShade(j, k); 
                       bbb[ncount] = zzz[ncount] > getZvertex(j); 
                       ncount++; 
                    }
                    if (bRefract && bShad[k+4]) // outer periph shading
                    {
                       iii[ncount] = OUTSHA; 
                       jjj[ncount] = j;
                       kkk[ncount] = k; 
                       zzz[ncount] = getZouterShade(j, k); 
                       bbb[ncount] = zzz[ncount] > getZvertex(j); 
                       ncount++; 
                    }
                    if (bRefract && bConn[k])   // connectors
                    {
                       iii[ncount] = CONNECT; 
                       jjj[ncount] = j;
                       kkk[ncount] = k; 
                       zzz[ncount] = getZconnect(j, k); 
                       bbb[ncount] = zzz[ncount] > getZvertex(j); 
                       ncount++; 
                    }                                        
                    if (bRefract && bShad[k+4] && bPosInnerR[j]) // inner shading
                    {
                       iii[ncount] = INNSHA; 
                       jjj[ncount] = j; 
                       kkk[ncount] = k; 
                       zzz[ncount] = getZinnerShade(j, k); 
                       bbb[ncount] = zzz[ncount] > getZvertex(j); 
                       ncount++; 
                    }
                    if (bArcs[k])         // radial arc
                    {
                       iii[ncount] = RADARC; 
                       jjj[ncount] = j; 
                       kkk[ncount] = k; 
                       zzz[ncount] = getZradialArc(j, k); 
                       bbb[ncount] = zzz[ncount] > getZvertex(j); 
                       ncount++; 
                    }
                    if (bArcs[k+4])        // outer periph arc
                    {
                       iii[ncount] = OUTARC; 
                       jjj[ncount] = j;
                       kkk[ncount] = k;  
                       zzz[ncount] = getZouterArc(j, k); 
                       bbb[ncount] = zzz[ncount] > getZvertex(j); 
                       ncount++; 
                    }
                    if (bArcs[k+4] && bPosInnerR[j])  // inner arc
                    {
                       iii[ncount] = INNARC; 
                       jjj[ncount] = j;
                       kkk[ncount] = k;  
                       zzz[ncount] = getZinnerArc(j, k); 
                       bbb[ncount] = zzz[ncount] > getZvertex(j); 
                       ncount++;
                    }
                }
            }
            if (ncount > MAXSORT-20)
              break;  // safety net
        }
        ssort(zzz, bbb, iii, jjj, kkk, ncount); 
    }













    //--------------ARTWORK-------------------
    //--------------ARTWORK-------------------
    //--------------ARTWORK-------------------
    
/*********    

    private void add2D(double x, double y, int op, int whichbase)
    // For 2D artwork scaled with current affines, for example rulers.
    // Do not call viewelaz(). 
    {
        addScaled(x, y, 0.0, op, whichbase);   // GPanel service
    } 
        
    private void addRawLocal(double x, int op, int whichbase)
    // for unscaled 2D artwork in raw pixel units, for example linewidths.
    {
        addRaw(x, 0.0, 0.0, op, whichbase);    // GPanel service
    }
        
***********/





    void doFullArt()  
    // Prepares artwork from a z-sorted list of "ncount" elements.
    // Be sure each art generator performs its SETCOLOR.
    // Copyright 2006 STELLAR SOFTWARE all rights reserved
    {
        for (int j=1; j<=nsurfs; j++)
          RT13.refractLayoutShading[j] = RT13.getRefraction(j, 1); 

        whitebkg = "T".equals(DMF.reg.getuo(UO_LAYOUT, 15));
        // QBASE is a global constant, base of artwork; then QBATCH, QRAND, QFINISH, QANNO.
        clearList(QBASE); 
        addRaw(0., 0., 0., whitebkg ? SETWHITEBKG : SETBLACKBKG, QBASE); 
        addRaw(0., 0., 0., SETCOLOR + (whitebkg ? BLACK : WHITE), QBASE); 
        addRaw(1., 0., 0., SETSOLIDLINE, QBASE); 
        addRaw(0., 0., 0., COMMENTRULER, QBASE); 
        

        
        // colors may be countermanded by Stereo.

        addAffines();      // see GPanel: loads uxcenter....uyspan

        doZsort();         // sorts artwork elements

        doSortedArt(true); // draws sorted artwork elements. 

        // Draw all the table rays if width > zero...
        if (widthRays > 0)
        {
            addRaw(widthRays, 0., 0., SETSOLIDLINE, QBASE);
            for (int kray=1; kray<=nrays; kray++)
              drawOneRay(kray, QBASE); 
        }

        doFurniture();   // draws rulers and axes

        doFinishArt();   // overlays additional nearside artwork. 

    } // end doFullArt()


    void doSkeleton()  
    // This is a simplified doFullArt(), no zsort, no rays, no shading.
    // Writes its quads to baseList. 
    // It is displayed only during mouse-down drag/zoom/twirl motion. 
    // Assumes we've already set rox[], roy[], nSpiderLegs[] etc.
    // Copyright 2006 STELLAR SOFTWARE all rights reserved.
    // Unexplained: doFurniture must precede optics lest the stereo
    //    red-blue color coding become red-red. 
    {   
        whitebkg = "T".equals(DMF.reg.getuo(UO_LAYOUT, 15));
        
        clearList(QBASE); 
        addRaw(0., 0., 0., whitebkg ? SETWHITEBKG : SETBLACKBKG, QBASE); 
        addRaw(0., 0., 0., SETCOLOR + (whitebkg ? BLACK : WHITE), QBASE); 
        addRaw(1., 0., 0., SETSOLIDLINE, QBASE); 


        addAffines();  // see GPanel: uxcenter....uyspan

        for (int j=1; j<=nsurfs; j++)
        {
            if (nSpiderLegs[j]>0)                //-------spider-----
            {
               for (int k=0; k<nSpiderLegs[j]; k++)
                 putSpiderHole(j, k, true,QBASE); //---TRUE?? FALSE??-------
            }
            else if('I' == cArrayType[j])       //-------IrisArray--
            {
                for (int k=0; k<4; k++)         // k=quadrant number
                  if (bArcs[k+4])
                    putOuterArc(j, k,QBASE); 
                for (int k=0; k<nArrayElements[j]; k++)
                  putIrisArrayHole(j, k,QBASE); 
            }
            else if('L' == cArrayType[j])       //-------LensArray--
            {
                for (int k=0; k<4; k++)         // k=quadrant number
                  if (bArcs[k+4])
                    putOuterArc(j, k,QBASE); 
                for (int k=0; k<nArrayElements[j]; k++)
                  putRectArrayElement(j, k,QBASE); 
            }
            else if('M' == cArrayType[j])       //-------MirrArray--
            {
                for (int k=0; k<4; k++)         // k=quadrant number
                  if (bArcs[k+4])
                    putOuterArc(j, k,QBASE); 
                for (int k=0; k<nArrayElements[j]; k++)
                  putRectArrayElement(j, k,QBASE); 
            }
            else       //---Other; no shading no connectors-----
            {
                for (int k=0; k<4; k++)
                  if (bArcs[k])
                    putRadialArc(j, k,QBASE); 
                for (int k=0; k<4; k++)
                  if (bArcs[k+4])
                    putOuterArc(j, k,QBASE); 
                for (int k=0; k<4; k++)
                  if (bArcs[k+4] && bPosInnerR[j])
                    putInnerArc(j, k,QBASE); 
            }
        }

        doFurniture(); 

    } // end doSkeleton()



    private void doSortedArt(boolean bShading)
    // Called by doFullArt() and doSkeleton().
    // Draws all optical surface elements, but in sorted order.  No rays.
    {
        addRaw(0., 0., 0., COMMENTSURF, QBASE); 
        addRaw(widthSurfs, 0., 0., SETSOLIDLINE, QBASE); 
        
        for (int ns=0; ns<ncount; ns++)  // draw z-sorted sequence...
        {
              int i = iii[ns];    // i = identifier
              int j = jjj[ns];    // j = surface number
              int k = kkk[ns];    // k = quadrant or hole number, 0...n-1

              if (((i==RADSHA) || (i==OUTSHA) || (i==INNSHA)) && bShading)
              {
                  addRaw(0., 0., 0., COMMENTSHADE, QBASE); 
                  double rls = RT13.refractLayoutShading[j];  
                  int grayness = (rls > 1.6) ? DKGRAY : LTGRAY;
                  addRaw(0., 0., 0., SETCOLOR + grayness, QBASE); 
              }
              else
                  addRaw(0., 0., 0., SETCOLOR + (whitebkg ? BLACK : WHITE), QBASE); 
              switch(i)
              {
                case RADSHA:  // if (bShading) 
                                putRadialShade(j, k, QBASE);  
                              break; 
                case OUTSHA:  // if (bShading)
                                putOuterShade(j, k, QBASE); 
                              break; 
                case INNSHA:  // if (bShading)
                                putInnerShade(j, k, QBASE); 
                              break;
                case CONNECT: putConnector(j, k, QBASE);  break; 
                case RADARC:  putRadialArc(j, k, QBASE); break; 
                case OUTARC:  putOuterArc(j, k, QBASE); break; 
                case INNARC:  putInnerArc(j, k, QBASE); break; 
                case SPIHOLE: putSpiderHole(j, k, true, QBASE); break; 
                case ARRHOLE: putIrisArrayHole(j, k, QBASE); break; 
                case ARRRECT: putRectArrayElement(j, k, QBASE); break; 
                default: break; 
              }
        }
    }


    private void doFurniture()
    // Draws rulers and axes.
    {
        addRaw(0., 0., 0.,  COMMENTAXIS, QBASE); 
        addRaw(widthAxes, 0., 0., SETSOLIDLINE, QBASE); 
        addRaw(0., 0., 0., SETCOLOR + (whitebkg ? BLACK : WHITE), QBASE); 
        
        if ("T".equals(DMF.reg.getuo(UO_LAYOUT, 10)))
          drawHruler(); 
        if ("T".equals(DMF.reg.getuo(UO_LAYOUT, 11)))
          drawVruler(); 
        if ("T".equals(DMF.reg.getuo(UO_LAYOUT, 12)))
          drawXaxis(); 
        if ("T".equals(DMF.reg.getuo(UO_LAYOUT, 13)))
          drawYaxis(); 
        if ("T".equals(DMF.reg.getuo(UO_LAYOUT, 14)))
          drawZaxis(); 
    }


    private void doBlank()
    // A simplified doFullArt() that creates a blank field.
    // Used when artwork is requested but cannot be generated
    {
        clearList(QBASE); 
        whitebkg = "T".equals(DMF.reg.getuo(UO_LAYOUT, 15));
        addRaw(0., 0., 0., whitebkg ? SETWHITEBKG : SETBLACKBKG, QBASE);
    }


    private boolean drawOneRay(int kray, int whichbase)
    // called by doFullArt() and by doRandomRay().
    // Puts a ray onto any specified quadList. 
    // Returns boolean since doRandomRay() counts status. 
    // Copyright 2006 STELLAR SOFTWARE all rights reserved
    // howfar = 0 to nsurfs;
    // can skip points having Z=-0.0 for bimodals.
    // Table rays are plotted into the memory area QBASE
    // Random rays are batch mode and go into QBATCH.
    {
        // System.out.println("-----Layout has started with kray = "+kray);
        
        boolean isGood; 
        if (kray == 0)
          isGood = RT13.bRunRandomRay();
        else
          isGood = RT13.isRayOK[kray];  
        int rayerr = RT13.getStatus(kray);  
        boolean rrok = (rayerr == RROK); 
        int howfarOK = RT13.getHowfarOK(kray); 
        
        // System.out.println("Layout starting kray = "+kray+", isGood = "+isGood+", howfarOK = "+howfarOK); 
        // double u0 = RT13.dGetRay(kray,0,RU); 
        // System.out.println("Layout starting kray has u0 = "+u0); 
        
        boolean bExtend = RT13.getExtend(kray);  
        int kGuide = (kray==0) ? RT13.getGuideRay() : kray; 

        int icolor = (int) RT13.raystarts[kGuide][RSCOLOR]; 
        if ((icolor==WHITE) && whitebkg)
          icolor=BLACK; 
        if ((icolor==BLACK) && !whitebkg)
          icolor=WHITE; 

        if (widthRays == 0)
          return isGood; 
          
        //------boil the surfaces down to the good (not-skipped) surfaces----

        int jhit[] = new int[nsurfs+1]; 
        jhit[0] = 0;           // surface zero is always one hit.
        int nBeyondZero = 0;   // how many hits beyond zero
        for (int j=1; j<=howfarOK; j++)
        {
            double Z = RT13.dGetRay(kray, j, RZ);
            if (U.isNegZero(Z))  // skip absentee intercepts
               continue; 
            else
               nBeyondZero++;
               jhit[nBeyondZero] = j;  // gather good intercepts for artwork
        }
        int jsolid = jhit[nBeyondZero]; 
        if (DEBUG)
           System.out.println("-----Layout jhit[] = " + Arrays.toString(jhit)); 
        
        // DON'T EXIT THERE MIGHT BE JUST ONE HIT (NO SOLID) BUT COULD STILL HAVE AN EXTEND
        // nSystem.out.println("LayoutPanel finds jsolid = "+jsolid); 

               
        //------now draw this ray------------

        addRaw(0., 0., 0., COMMENTRAY, whichbase); 
        addRaw(widthRays, 0., 0., SETSOLIDLINE, whichbase); 
        addRaw(0., 0., 0., SETCOLOR+icolor, whichbase); 
        double xyz[] = {0., 0., 0.}; 
        if (nBeyondZero > 0)  // solid part, propagation OK
        {
            if (DEBUG)
               System.out.println("-----Layout is starting kray = "+kray);
            for (int h=0; h<=nBeyondZero; h++)
            {

               int j = jhit[h]; 
               xyz[0] = RT13.dGetRay(kray, j, RX);
               xyz[1] = RT13.dGetRay(kray, j, RY); 
               xyz[2] = RT13.dGetRay(kray, j, RZ); 
               if (DEBUG)
                  System.out.printf("-----Layout is adding hit, jsurf, x = %6d %6d %12.6f \n", h, j, xyz[0]); 
                   
               viewelaz(xyz);
               int itype = (int) RT13.dGetSurfParm(OTYPE,j); 
               boolean bCBIN  = (OTCBIN  == itype); 
               boolean bCBOUT = (OTCBOUT == itype); 
               int op = ((j==0 || bCBOUT) ? MOVETO : (j==jsolid || bCBIN) ? STROKE : PATHTO);
               addScaled(xyz, op, whichbase);
            }
        }   
        if (bExtend)
        {
            // Although the ray trace ends at howfar, RT13.bRunOneRay() used vExtendLabs() to
            // gain another surface at howfar+1 that has the extension information. 
            // This extension is hidden by InOut so no one will be the wiser.  Heh heh. 
            // The extension numbers are handled by RT13
            addRaw(widthRays, 0., 0., SETDOTTEDLINE, whichbase); 
            
            if (DEBUG)
               System.out.println("Layout extending from jsolid = "+jsolid);
            xyz[0] = RT13.dGetRay(kray, jsolid, RX);
            xyz[1] = RT13.dGetRay(kray, jsolid, RY); 
            xyz[2] = RT13.dGetRay(kray, jsolid, RZ);
            viewelaz(xyz);  
            addScaled(xyz, MOVETO, whichbase);
            
            if (DEBUG)
               System.out.println("Layout extending to jsolid+1 = "+(jsolid+1)); 
            xyz[0] = RT13.dGetRay(kray, jsolid+1, RX);
            xyz[1] = RT13.dGetRay(kray, jsolid+1, RY); 
            xyz[2] = RT13.dGetRay(kray, jsolid+1, RZ);
            viewelaz(xyz);  
            addScaled(xyz, STROKE, whichbase);
        }
        return isGood;          
    }


    private void doFinishArt() 
    // Draw only the foreground elements onto finishList.
    // This artwork goes into finishList. 
    // The random rays are stored elsewhere: batchList then randList. 
    // DO NOT try to set the background or scale factors. 
    // In stereo, need to call this twice: first to make solid color
    // using setPaint(), then rotated using setXORMode(Color.BLACK). 
    //
    // For stereo: need two stereo calls to
    // doFinishArt() **after** all other stereo calls are complete**
    //
    // add these quads onto finishList not baseList.
    // 
    {        
        addRaw(0., 0., 0., COMMENTFINISH, QFINISH); 
        addRaw(widthSurfs, 0., 0., SETSOLIDLINE, QFINISH); 
        addRaw(0., 0., 0., SETCOLOR + (whitebkg ? BLACK : WHITE), QFINISH); 
        
        boolean bShading = true; 
        for (int ns=0; ns<ncount; ns++)  // draw z-sorted sequence...
          if (bbb[ns])
          {
              int i = iii[ns];    // i = identifier
              int j = jjj[ns];    // j = surface number
              int k = kkk[ns];    // k = quadrant or hole number, 0...n-1

              if (i==OUTSHA)
              {
                  addRaw(0., 0., 0., COMMENTSHADE, QFINISH); 
                  double rls = RT13.refractLayoutShading[j];  
                  int grayness = (rls > 1.6) ? DKGRAY : LTGRAY;
                  addRaw(0., 0., 0., SETCOLOR + grayness, QFINISH); 
              }
              else
                 addRaw(0., 0., 0., SETCOLOR + (whitebkg ? BLACK : WHITE), QFINISH); 
                 
              switch(i)
              {
                case OUTSHA:  if (bShading)
                                putOuterShade(j, k, QFINISH); 
                              break; 
                case OUTARC:  putOuterArc(j, k, QFINISH); break; 
                case INNARC:  putInnerArc(j, k, QFINISH); break; 
                case RADARC:  putRadialArc(j, k, QFINISH); break; 
                case SPIHOLE: putSpiderHole(j, k, false, QFINISH); break; 
                case ARRHOLE: putIrisArrayHole(j, k, QFINISH);  break; 
                case ARRRECT: putRectArrayElement(j, k, QFINISH); break;
                default: break; 
              }
          }
    }


    //------------getZfunctions() are for Zsorting---------------------
    //-----these assume we've already set rox[], roy[], nSpiderLegs[] etc----

    double getZvertex(int j)
    // For Z sorting only.
    {
        double xyz[] = {0.0, 0.0, 0.0};  // the vertex.
        vx2lab(xyz, RT13.surfs[j]); 
        viewelaz(xyz); 
        return xyz[2];
    }


    double getZspiderHole(int j, int ihole)
    // for Z sorting only.
    {
        double r = 0.5*(rox[j] + roy[j]); 
        int nholes = nSpiderLegs[j]; 
        if (nholes < 1)
          return -0.0; 
        double ahole = (0.5 + ihole) * TWOPI/nholes; 
        double xyz[] = new double[3];
        xyz[0] = RT13.surfs[j][OFFOX] + r*Math.cos(ahole); 
        xyz[1] = RT13.surfs[j][OFFOY] + r*Math.sin(ahole); 
        xyz[2] = 0.0; 
        vx2lab(xyz, RT13.surfs[j]); 
        viewelaz(xyz); 
        return xyz[2];
    }


    double getZarrayElement(int j, int nh)  // surface=j, elem nh=0...n-1.
    // For Z sorting centers, otherwise similar to putArrayElement().
    // Even classier would be to do this by quadrants = quarter elements!
    {
        double diax = RT13.surfs[j][OODIAX]; 
        int nx = (int) RT13.surfs[j][ONARRAYX]; 
        double diay = RT13.surfs[j][OODIAY]; 
        int ny = (int) RT13.surfs[j][ONARRAYY]; 
        
        if ((nx<1) || (ny<1))
          return 0.0; 

        double px = diax/nx;
        double rx = px/2; 
        double py = diay/ny;
        double ry = py/2; 

        int ix = nh % nx;
        int jy = nh / nx; 

        double xyz[] = new double[3]; 
        xyz[0] = px*(-0.5*nx + 0.5 + ix); 
        xyz[1] = py*(-0.5*ny + 0.5 + jy); 
        xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j]); 
        vx2lab(xyz, RT13.surfs[j]); 
        viewelaz(xyz); 
        return xyz[2];
    }


    double getZradialShade(int j, int t)
    // for sorting only; involves both j, j-1
    {
        double xyz[] = new double[3]; 
        double xoff = 0.5*(RT13.surfs[j-1][OFFOX] + RT13.surfs[j][OFFOX]); 
        double yoff = 0.5*(RT13.surfs[j-1][OFFOY] + RT13.surfs[j][OFFOY]);
        xyz[0] = 0.25*(rox[j-1]+rox[j])*c4[t%4] + xoff; 
        xyz[1] = 0.25*(roy[j-1]+roy[j])*s4[t%4] + yoff; 
        xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j]); 
        vx2lab(xyz, RT13.surfs[j]); 
        viewelaz(xyz); 
        return xyz[2]; 
    }

    double getZconnect(int j, int t)
    // for sorting only; uses surfaces j and j-1; quadrant = t
    {
        double xyz[] = new double[3]; 
        double xoff = 0.5*(RT13.surfs[j-1][OFFOX] + RT13.surfs[j][OFFOX]); 
        double yoff = 0.5*(RT13.surfs[j-1][OFFOY] + RT13.surfs[j][OFFOY]);
        xyz[0] = 0.5*(rox[j-1]+rox[j])*c4[t%4] + xoff; 
        xyz[1] = 0.5*(roy[j-1]+roy[j])*s4[t%4] + yoff; 
        xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j]); 
        vx2lab(xyz, RT13.surfs[j]); 
        viewelaz(xyz); 
        return xyz[2]; 
    }
    
    double getZouterShade(int j, int t)
    // for sorting only; involves both j, j-1
    {
        double xyz[] = new double[3]; 
        double xoff = 0.5*(RT13.surfs[j-1][OFFOX] + RT13.surfs[j][OFFOX]); 
        double yoff = 0.5*(RT13.surfs[j-1][OFFOY] + RT13.surfs[j][OFFOY]);
        xyz[0] = 0.5*(rox[j-1]+rox[j])*c45[t%4] + xoff; 
        xyz[1] = 0.5*(roy[j-1]+roy[j])*s45[t%4] + yoff; 
        xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j]); 
        vx2lab(xyz, RT13.surfs[j]); 
        viewelaz(xyz); 
        return xyz[2]; 
    }


    double getZinnerShade(int j, int t)
    // for sorting only; should involve both j, j-1
    {
        double xyz[] = new double[3]; 
        double xoff = 0.5*(RT13.surfs[j-1][OFFIX] + RT13.surfs[j][OFFIX]); 
        double yoff = 0.5*(RT13.surfs[j-1][OFFIY] + RT13.surfs[j][OFFIY]);
        xyz[0] = 0.5*(gix[j-1]+gix[j])*c45[t%4] + xoff; 
        xyz[1] = 0.5*(giy[j-1]+giy[j])*s45[t%4] + yoff; 
        xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j]); 
        vx2lab(xyz, RT13.surfs[j]); 
        viewelaz(xyz); 
        return xyz[2]; 
    }


    double getZradialArc(int j, int t)
    // for sorting only. 
    {
        double xyz[] = new double[3]; 
        xyz[0] = rox[j]*0.5*c4[t%4] + RT13.surfs[j][OFFOX];
        xyz[1] = roy[j]*0.5*s4[t%4] + RT13.surfs[j][OFFOY];
        xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j]); 
        vx2lab(xyz, RT13.surfs[j]); 
        viewelaz(xyz); 
        return xyz[2]; 
    }


    double getZouterArc(int j, int t)
    // for sorting only. 
    {
        double xyz[] = new double[3]; 
        xyz[0] = rox[j]*c45[t%4] + RT13.surfs[j][OFFOX]; 
        xyz[1] = roy[j]*s45[t%4] + RT13.surfs[j][OFFOY]; 
        xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j]); 
        vx2lab(xyz, RT13.surfs[j]); 
        viewelaz(xyz); 
        return xyz[2]; 
    }


    double getZinnerArc(int j, int t)
    // for sorting only. 
    {
        double xyz[] = new double[3]; 
        xyz[0] = gix[j]*c45[t%4] + RT13.surfs[j][OFFIX]; 
        xyz[1] = giy[j]*s45[t%4] + RT13.surfs[j][OFFIY]; 
        xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j]); 
        vx2lab(xyz, RT13.surfs[j]); 
        viewelaz(xyz); 
        return xyz[2]; 
    }


    //--------putFunctions are for drawing individual parts------------
    //-----these assume we've already set rox[], roy[], nSpiderLegs[] etc----

    void putSpiderHole(int j, int ihole, boolean bWholeHole, int whichbase)
    // j = surface number, 1....nsurfs
    // ihole = hole number 0...nholes-1
    // one four-arc hole is drawn per spider leg. 
    // Leg zero is along the X axis: see RT13. 
    // Drawn in local offset frame, but outputs into vertex frame.
    {

        if ((nSpiderLegs[j]<1) || (ihole<0) || (ihole>=nSpiderLegs[j]) || (nsegs<1))
          return; 

        if ((rox[j] < U.TOL) || (roy[j] < U.TOL))
          return; 

        int snsegs = nsegs;
        if (nSpiderLegs[j]<3)
          snsegs *= 3; 

        double halfLeg = 0.5 * RT13.surfs[j][OWSPIDER] + U.TOL; 
        double aStep = TWOPI/nSpiderLegs[j]; 

        // Enlarge inner radius if necessary...
        double ri = giy[j]; 
        ri = Math.max(ri, (nSpiderLegs[j]==1) ? halfLeg : halfLeg/Math.sin(0.5*aStep)); 
            
        double aThisLeg = ihole * aStep;     // Leg zero = +X.
        double aOuter = Math.asin(halfLeg/rox[j]); 
        double aInner = Math.asin(halfLeg/ri); 
        double dOuter = (aStep - 2*aOuter)/snsegs; 
        if (dOuter < U.TOL)
          return; 

        double dInner = (aStep - 2*aInner)/snsegs; // pos or zero
        double xyz[] = new double[3]; 
        double dx=0, dy=0, xsave=0, ysave=0, a=0.0; 

        for (int i=0; i<=snsegs; i++)     // outer periphery
        {
            a = aThisLeg + aOuter + i*dOuter; 
            xsave = rox[j] * Math.cos(a); 
            ysave = roy[j] * Math.sin(a); 
            xyz[0] = xsave - RT13.surfs[j][OFFOX];  // vxframe
            xyz[1] = ysave - RT13.surfs[j][OFFOY];  // vxframe
            xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j]); 
            vx2lab(xyz, RT13.surfs[j]); 
            viewelaz(xyz); 
            if (bWholeHole)
              addScaled(xyz, (i==0) ? MOVETO : PATHTO, whichbase);
            else
              addScaled(xyz, (i==0) ? MOVETO : (i<snsegs) ? PATHTO : STROKE, whichbase); 
        }

        if (bWholeHole)
        {
            a = aThisLeg + aStep - aInner;      // incoming leg side
            dx = (ri*Math.cos(a) - xsave)/nsegs; 
            dy = (ri*Math.sin(a) - ysave)/nsegs; 

            for (int i=1; i<=nsegs; i++)
            {
                xyz[0] = xsave + i*dx - RT13.surfs[j][OFFOX]; // vxframe
                xyz[1] = ysave + i*dy - RT13.surfs[j][OFFOY]; // vxframe
                xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j]); 
                vx2lab(xyz, RT13.surfs[j]); 
                viewelaz(xyz); 
                addScaled(xyz, PATHTO, whichbase); 
            }

            if (dInner > U.TOL)               // inner periphery
              for (int i=1; i<=snsegs; i++) 
              {
                  a = aThisLeg + aStep - aInner - i*dInner; 
                  xsave = ri * Math.cos(a); 
                  ysave = ri * Math.sin(a); 
                  xyz[0] = xsave - RT13.surfs[j][OFFOX];
                  xyz[1] = ysave - RT13.surfs[j][OFFOY];
                  xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j]); 
                  vx2lab(xyz, RT13.surfs[j]); 
                  viewelaz(xyz); 
                  addScaled(xyz, PATHTO, whichbase);
              }

            a = aThisLeg + aOuter;           // outgoing leg side
            dx = (rox[j]*Math.cos(a) - xsave)/nsegs;
            dy = (roy[j]*Math.sin(a) - ysave)/nsegs;
            for (int i=1; i<=nsegs; i++)
            {
                xyz[0] = xsave + i*dx - RT13.surfs[j][OFFOX];
                xyz[1] = ysave + i*dy - RT13.surfs[j][OFFOY];
                xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j]); 
                vx2lab(xyz, RT13.surfs[j]); 
                viewelaz(xyz); 
                addScaled(xyz, (i<nsegs) ? PATHTO : STROKE, whichbase ); 
            }
        }
    } //------------end of putSpiderHole()---------------




    void putIrisArrayHole(int j, int nh, int whichbase)  // nh=hole number
    {
        double diax = RT13.surfs[j][OODIAX]; 
        int nx = (int) RT13.surfs[j][ONARRAYX]; 
        double diay = RT13.surfs[j][OODIAY]; 
        int ny = (int) RT13.surfs[j][ONARRAYY]; 
        
        if ((nx<1) || (ny<1))
          return; 

        double px = diax/nx;
        double rx = px/2; 
        double py = diay/ny;
        double ry = py/2; 

        int ix = nh % nx;
        int jy = nh / nx; 
    
        double xcenter = px*(-0.5*nx + 0.5 + ix); 
        double ycenter = py*(-0.5*ny + 0.5 + jy); 
        
        // Enforce outer form = rectangular?
        int iform = (int) RT13.surfs[j][OFORM]; 

        boolean bIRect = ((OFIRECT==iform) || (OFBRECT==iform));
        for (int iq=0; iq<4; iq++)  // iq=quadrant=0...3
        {
            double xyz[] = new double[3]; 
            for (int i=0; i<=nsegs; i++)
            {
                xyz[0] = gix[j] * cc(bIRect, i) * arcx[iq]; 
                xyz[1] = giy[j] * ss(bIRect, i) * arcy[iq];
                xyz[0] += xcenter;
                xyz[1] += ycenter;
                xyz[2] = 0.0; 
                vx2lab(xyz, RT13.surfs[j]); 
                viewelaz(xyz); 
                int op = (i==0) ? MOVETO : (i==nsegs) ? STROKE : PATHTO;
                addScaled(xyz, op, whichbase);
            }
        }
    } //--------------end of putIrisArrayHole()------------------



    void putRectArrayElement(int j, int nh, int whichbase)  // j=surface, nh=element 0..n-1
    {
        double diax = RT13.surfs[j][OODIAX]; 
        int nx = (int) RT13.surfs[j][ONARRAYX]; 
        double diay = RT13.surfs[j][OODIAY]; 
        int ny = (int) RT13.surfs[j][ONARRAYY]; 
        
        if ((nx<1) || (ny<1))
          return; 

        double px = diax/nx;
        double rx = px/2; 
        double py = diay/ny;
        double ry = py/2; 

        int ix = nh % nx;
        int jy = nh / nx; 
    
        double xcenter = px*(-0.5*nx + 0.5 + ix); 
        double ycenter = py*(-0.5*ny + 0.5 + jy); 
        
        // Make both inner & outer forms rectangular:
        int iform = OFBRECT; // (int) RT13.surfs[j][OFORM]; 

        boolean bIRect = true; 
        for (int iq=0; iq<4; iq++)  // iq=quadrant=0...3
        {
            double xyz[] = new double[3]; 
            for (int i=0; i<=nsegs; i++)
            {
                xyz[0] = rx * cc(bIRect, i) * arcx[iq]; 
                xyz[1] = ry * ss(bIRect, i) * arcy[iq];
                xyz[0] += xcenter;
                xyz[1] += ycenter;
                xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j]); 
                vx2lab(xyz, RT13.surfs[j]); 
                viewelaz(xyz); 
                int op = (i==0) ? MOVETO : (i==nsegs) ? STROKE : PATHTO;
                addScaled(xyz, op, whichbase);
            }
        }
    } //--------------end of putRectArrayElement()------------------




    //----Artwork for three arcs: radial, outer, inner------

    void putRadialArc(int j, int t, int whichbase)
    {
        double xyz[] = new double[3]; 
        int iq = t%4; 
        boolean bIris = (RT13.surfs[j][OTYPE] == OTIRIS); 
        double gx = Math.abs(RT13.surfs[j][OGX]); 
        double gy = Math.abs(RT13.surfs[j][OGY]); 
        boolean givenDiam = (gox[j]>0.0) && (goy[j]>0.0); 
        boolean bGrooved = givenDiam && ((gx>0.0) || (gy>0.0));  
        boolean bGroovy = gy > gx; 

        if ((bArcs[iq] || bIris) && !bGrooved)  // 0,1,2,3: draw a radius
        {
            if (bIris)
            {
                double dr = IRISTICKFRAC * Math.max(gox[j],goy[j]); 
                double drx = Math.min(dr, gix[j]); 
                double dry = Math.min(dr, giy[j]); 
                if (bPosInnerR[j]) // inner iris beauty marks
                {
                    for (int i=0; i<=1; i++)
                    {
                        xyz[0] = c4[iq] * (gix[j] - drx * i); 
                        xyz[1] = s4[iq] * (giy[j] - dry * i); 
                        xyz[0] += RT13.surfs[j][OFFIX];
                        xyz[1] += RT13.surfs[j][OFFIY];
                        xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j]); 
                        vx2lab(xyz, RT13.surfs[j]); 
                        viewelaz(xyz); 
                        int op = (i==0) ? MOVETO : STROKE;    
                        addScaled(xyz, op, whichbase);
                    }
                }
                if (bGivenOuterRadius[j]) // outer iris beauty marks
                {
                    for (int i=0; i<=1; i++) 
                    {
                        xyz[0] = c4[iq] * (rox[j] + dr * i); 
                        xyz[1] = s4[iq] * (roy[j] + dr * i); 
                        xyz[0] += RT13.surfs[j][OFFOX];
                        xyz[1] += RT13.surfs[j][OFFOY];
                        xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j]); 
                        vx2lab(xyz, RT13.surfs[j]); 
                        viewelaz(xyz); 
                        int op = (i==0) ? MOVETO : STROKE;      
                        addScaled(xyz, op, whichbase);
                    }
                }
            }
            else  // not an iris so draw the full radius
            for (int i=0; i<=nsegs; i++)
            {
                //-------use precomputed endpoints------------
                xyz[0] = radix[j][iq] + i*(radox[j][iq]-radix[j][iq])/nsegs; 
                xyz[1] = radiy[j][iq] + i*(radoy[j][iq]-radiy[j][iq])/nsegs; 
                xyz[0] += RT13.surfs[j][OFFOX];
                xyz[1] += RT13.surfs[j][OFFOY];
                xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j]); 
                vx2lab(xyz, RT13.surfs[j]); 
                viewelaz(xyz); 
                int op = (i==0) ? MOVETO : i==nsegs ? STROKE : PATHTO; 
                addScaled(xyz, op, whichbase);
            }
        }
    }
 

    void putOuterArc(int j, int t, int whichbase)
    {
        double xyz[] = new double[3]; 
        int iq = t%4; 
        int iform = (int) RT13.surfs[j][OFORM];
        boolean bORect = ((OFORECT==iform) || (OFBRECT==iform));
        boolean bIris = (RT13.surfs[j][OTYPE] == OTIRIS); 
        if (bIris && !bGivenOuterRadius[j])
          return; 

        for (int i=0; i<=nsegs; i++)  // outer arc
        {
            xyz[0] = rox[j] * cc(bORect, i) * arcx[iq]; 
            xyz[1] = roy[j] * ss(bORect, i) * arcy[iq];
            xyz[0] += RT13.surfs[j][OFFOX];
            xyz[1] += RT13.surfs[j][OFFOY];
            xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j]); 
            vx2lab(xyz, RT13.surfs[j]); 
            viewelaz(xyz); 
            int op = i==0 ? MOVETO : i==nsegs ? STROKE : PATHTO;
            addScaled(xyz, op, whichbase);
        }
    }


    void putInnerArc(int j, int t, int whichbase)
    {
        double xyz[] = new double[3]; 
        int iq = t%4; 
        int iform = (int) RT13.surfs[j][OFORM];
        boolean bIRect = ((OFIRECT==iform) || (OFBRECT==iform));
        boolean bIris = (RT13.surfs[j][OTYPE] == OTIRIS); 
        if (bIris && !bPosInnerR[j])
          return; 

        for (int i=0; i<=nsegs; i++)
        {
            xyz[0] = gix[j] * cc(bIRect, i) * arcx[iq]; 
            xyz[1] = giy[j] * ss(bIRect, i) * arcy[iq];
            xyz[0] += RT13.surfs[j][OFFIX];
            xyz[1] += RT13.surfs[j][OFFIY];
            xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j]); 
            vx2lab(xyz, RT13.surfs[j]); 
            viewelaz(xyz); 
            int op = i==0 ? MOVETO : i==nsegs ? STROKE : PATHTO;
            addScaled(xyz, op, whichbase);
        }
    }


    //---------three shadings: radial, outer, inner---------

    void putRadialShade(int j, int t, int whichbase)
    {
        double xyz[] = new double[3]; 
        int iq = t%4;
        for (int i=0; i<nsegs; i++)
        {
            // first corner.
            xyz[0] = radix[j][iq] + i*(radox[j][iq]-radix[j][iq])/nsegs; 
            xyz[1] = radiy[j][iq] + i*(radoy[j][iq]-radiy[j][iq])/nsegs; 
            xyz[0] += RT13.surfs[j][OFFOX];  // since we are in outer coord sys.
            xyz[1] += RT13.surfs[j][OFFOY];  // since we are in outer coord sys.
            xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j]); 
            vx2lab(xyz, RT13.surfs[j]); 
            viewelaz(xyz); 
            addScaled(xyz, MOVETO, whichbase); 

            // second corner...
            xyz[0] = radix[j-1][iq] + i*(radox[j-1][iq]-radix[j-1][iq])/nsegs; 
            xyz[1] = radiy[j-1][iq] + i*(radoy[j-1][iq]-radiy[j-1][iq])/nsegs; 
            xyz[0] += RT13.surfs[j-1][OFFOX];
            xyz[1] += RT13.surfs[j-1][OFFOY];
            xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j-1]); 
            vx2lab(xyz, RT13.surfs[j-1]); 
            viewelaz(xyz); 
            addScaled(xyz, PATHTO, whichbase); 

            // third corner...
            xyz[0] = radix[j-1][iq] + (i+1)*(radox[j-1][iq]-radix[j-1][iq])/nsegs; 
            xyz[1] = radiy[j-1][iq] + (i+1)*(radoy[j-1][iq]-radiy[j-1][iq])/nsegs; 
            xyz[0] += RT13.surfs[j-1][OFFOX];
            xyz[1] += RT13.surfs[j-1][OFFOY];
            xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j-1]); 
            vx2lab(xyz, RT13.surfs[j-1]); 
            viewelaz(xyz); 
            addScaled(xyz, PATHTO, whichbase); 

            // fourth corner...
            xyz[0] = radix[j][iq] + (i+1)*(radox[j][iq]-radix[j][iq])/nsegs; 
            xyz[1] = radiy[j][iq] + (i+1)*(radoy[j][iq]-radiy[j][iq])/nsegs; 
            xyz[0] += RT13.surfs[j][OFFOX];
            xyz[1] += RT13.surfs[j][OFFOY];
            xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j]); 
            vx2lab(xyz, RT13.surfs[j]); 
            viewelaz(xyz); 
            addScaled(xyz, FILL, whichbase); 
        } 
    }

    void putConnector(int j, int t, int whichbase)
    {
        double xyz[] = new double[3]; 
        int iq = t%4;

        // at surface j...
        xyz[0] = radox[j][iq]; 
        xyz[1] = radoy[j][iq]; 
        xyz[0] += RT13.surfs[j][OFFOX];  // since we are in outer coord sys.
        xyz[1] += RT13.surfs[j][OFFOY];  // since we are in outer coord sys.
        xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j]); 
        vx2lab(xyz, RT13.surfs[j]); 
        viewelaz(xyz); 
        addScaled(xyz, MOVETO, whichbase); 

        // at surface j-1 ...
        xyz[0] = radox[j-1][iq];
        xyz[1] = radoy[j-1][iq];
        xyz[0] += RT13.surfs[j-1][OFFOX];
        xyz[1] += RT13.surfs[j-1][OFFOY];
        xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j-1]); 
        vx2lab(xyz, RT13.surfs[j-1]); 
        viewelaz(xyz); 
        addScaled(xyz, STROKE, whichbase); 
    }




    void putOuterShade(int j, int t, int whichbase)
    {
        double xyz[] = new double[3]; 
        int iform = (int) RT13.surfs[j][OFORM];
        boolean bORect = ((OFORECT==iform) || (OFBRECT==iform));
        int iprev = (int) RT13.surfs[j-1][OFORM]; 
        boolean bPRect = ((OFORECT==iprev) || (OFBRECT==iprev)); 
        int iq = t%4;
        for (int i=0; i<nsegs; i++)
        {
            // first corner...
            xyz[0] = rox[j] * cc(bORect, i) * arcx[iq]; 
            xyz[1] = roy[j] * ss(bORect, i) * arcy[iq];
            xyz[0] += RT13.surfs[j][OFFOX];
            xyz[1] += RT13.surfs[j][OFFOY];
            xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j]); 
            vx2lab(xyz, RT13.surfs[j]); 
            viewelaz(xyz); 
            addScaled(xyz, MOVETO, whichbase); 

            // second corner...
            xyz[0] = rox[j-1] * cc(bPRect, i) * arcx[iq]; 
            xyz[1] = roy[j-1] * ss(bPRect, i) * arcy[iq];
            xyz[0] += RT13.surfs[j-1][OFFOX];
            xyz[1] += RT13.surfs[j-1][OFFOY];
            xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j-1]); 
            vx2lab(xyz, RT13.surfs[j-1]); 
            viewelaz(xyz); 
            addScaled(xyz, PATHTO, whichbase); 

            // third corner...
            xyz[0] = rox[j-1] * cc(bPRect, i+1) * arcx[iq]; 
            xyz[1] = roy[j-1] * ss(bPRect, i+1) * arcy[iq];
            xyz[0] += RT13.surfs[j-1][OFFOX];
            xyz[1] += RT13.surfs[j-1][OFFOY];
            xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j-1]); 
            vx2lab(xyz, RT13.surfs[j-1]); 
            viewelaz(xyz); 
            addScaled(xyz, PATHTO, whichbase); 

            // fourth corner...
            xyz[0] = rox[j] * cc(bORect, i+1) * arcx[iq]; 
            xyz[1] = roy[j] * ss(bORect, i+1) * arcy[iq];
            xyz[0] += RT13.surfs[j][OFFOX];
            xyz[1] += RT13.surfs[j][OFFOY];
            xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j]); 
            vx2lab(xyz, RT13.surfs[j]); 
            viewelaz(xyz); 
            addScaled(xyz, FILL, whichbase);
        }
    }


    void putInnerShade(int j, int t, int whichbase)
    {
        double xyz[] = new double[3]; 
        int iform = (int) RT13.surfs[j][OFORM];
        boolean bIRect = ((OFIRECT==iform) || (OFBRECT==iform));
        int iprev = (int) RT13.surfs[j-1][OFORM]; 
        boolean bPRect = ((OFORECT==iprev) || (OFBRECT==iprev)); 
        int iq = t%4; 
        for (int i=0; i<nsegs; i++)
        {
            // first corner...
            xyz[0] = gix[j] * cc(bIRect, i) * arcx[iq]; 
            xyz[1] = giy[j] * ss(bIRect, i) * arcy[iq];
            xyz[0] += RT13.surfs[j][OFFIX];
            xyz[1] += RT13.surfs[j][OFFIY];
            xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j]); 
            vx2lab(xyz, RT13.surfs[j]); 
            viewelaz(xyz); 
            addScaled(xyz, MOVETO, whichbase); 

            // second corner...
            xyz[0] = gix[j-1] * cc(bPRect, i) * arcx[iq]; 
            xyz[1] = giy[j-1] * ss(bPRect, i) * arcy[iq];
            xyz[0] += RT13.surfs[j-1][OFFIX];
            xyz[1] += RT13.surfs[j-1][OFFIY];
            xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j-1]); 
            vx2lab(xyz, RT13.surfs[j-1]); 
            viewelaz(xyz); 
            addScaled(xyz, PATHTO, whichbase); 

            // third corner...
            xyz[0] = gix[j-1] * cc(bPRect, i+1) * arcx[iq]; 
            xyz[1] = giy[j-1] * ss(bPRect, i+1) * arcy[iq];
            xyz[0] += RT13.surfs[j-1][OFFIX];
            xyz[1] += RT13.surfs[j-1][OFFIY];
            xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j-1]); 
            vx2lab(xyz, RT13.surfs[j-1]); 
            viewelaz(xyz); 
            addScaled(xyz, PATHTO, whichbase); 

            // fourth corner...
            xyz[0] = gix[j] * cc(bIRect, i+1) * arcx[iq]; 
            xyz[1] = giy[j] * ss(bIRect, i+1) * arcy[iq];
            xyz[0] += RT13.surfs[j][OFFIX];
            xyz[1] += RT13.surfs[j][OFFIY];
            xyz[2] = Z.dGetZsurf(xyz[0], xyz[1], RT13.surfs[j]); 
            vx2lab(xyz, RT13.surfs[j]); 
            viewelaz(xyz); 
            addScaled(xyz, FILL, whichbase);
        }
    }



    void drawHruler()  // always drawn on baseList using add2D() for scaling
    {
        double xruler = uxcenter - 0.33*uxspan;
        double yruler = uycenter - 0.33*uyspan;  

        int iFontcode = getUOGraphicsFontCode();  
        int iHpoints = iFontcode / 10000;     
        int iWpoints = 1 + iHpoints / 2;   
        double scaledW = iWpoints * uxspan / dUOpixels; 
        double xtick = 0.5 * iWpoints * uxspan / dUOpixels; 
        double ytick = 0.5 * iWpoints * uyspan / dUOpixels;  
        double scaledH = iHpoints * uyspan / dUOpixels; 
        double hyoffset = -0.7*scaledH;    // -scaledH; 

        double ticks[] = new double[10]; 
        int results[] = new int[2]; 
        boolean whitebkg = "T".equals(DMF.reg.getuo(UO_LAYOUT, 15));
        int color = SETCOLOR + (whitebkg ? BLACK : WHITE);         
        addRaw(0., 0., 0.,  COMMENTRULER, QBASE);            // raw not scaled
        addRaw(widthAxes, 0., 0., SETSOLIDLINE, QBASE);      // raw not scaled
        addRaw(0., 0., 0., color, QBASE);                    // raw not scaled
        
        U.ruler(xruler, xruler+0.67*uxspan, false, ticks, results); 
        int nticks = results[NTICKS];
        int nfracdigits = results[NFRACDIGITS]; 
        addScaled(ticks[0], yruler, 0., MOVETO, QBASE); 
        addScaled(ticks[0], yruler+ytick, 0., PATHTO, QBASE); 
        addScaled(ticks[0], yruler, 0., PATHTO, QBASE); 
        
        for (int i=1; i<nticks; i++)
        {
            addScaled(ticks[i], yruler, 0., PATHTO, QBASE); 
            addScaled(ticks[i], yruler+ytick, 0., PATHTO, QBASE); 
            int op = (i < nticks-1) ? PATHTO : STROKE;  
            addScaled(ticks[i], yruler, 0., op, QBASE); 
        }

        // Label the ticks and don't let the labels overlap.
        // Labels have highly disparate lengths!
        // Test each individual label, see if it fits. 

        double xPrevRightmost = 0.0; 
        for (int i=0; i<results[NTICKS]; i++)
        {
            String s = U.fwd(ticks[i], 16, nfracdigits).trim();
            double x = ticks[i] - 0.5*scaledW*(s.length()-1); 
            if ((i==0) || (xPrevRightmost < x))
            {
                for (int k=0; k<s.length(); k++)
                {
                    int ic = (int) s.charAt(k) + iFontcode; 
                    addScaled(x, yruler+hyoffset, 0., ic, QBASE); 
                    x += scaledW;
                }
            }
            xPrevRightmost = x + scaledW; 
        }      

        ///// Hruler title....

        String t = getAxisLabel(0).trim(); 
        for (int k=0; k<t.length(); k++)
        {
            int ic = (int) t.charAt(k) + iFontcode; 
            double x = uxcenter + scaledW*(k-t.length()/2); 
            addScaled(x, yruler-2.5*scaledH, 0., ic, QBASE);  
        }
    } // end drawHruler()


    void drawVruler()  // always drawn on baseList
    {
        double xruler = uxcenter - 0.33*uxspan;
        double yruler = uycenter - 0.33*uyspan;  

        int iFontcode = getUOGraphicsFontCode();  
        int iHpoints = iFontcode / 10000;     
        int iWpoints = 1 + iHpoints / 2;   
        double scaledW = iWpoints * uxspan / dUOpixels; 
        double xtick = 0.5 * iWpoints * uxspan / dUOpixels; 
        double ytick = 0.5 * iWpoints * uyspan / dUOpixels;  
        double scaledH = iHpoints * uyspan / dUOpixels; 
        double vyoffset = 0.0;          // -0.4 * scaledH; Centered  
        double vrhgap = -0.2;           // 0.2; Centered                 

        double ticks[] = new double[MAXTICKS]; 
        int results[] = new int[2]; 
        boolean whitebkg = "T".equals(DMF.reg.getuo(UO_LAYOUT, 15));
        int color = SETCOLOR + (whitebkg ? BLACK : WHITE); 
        addRaw(0., 0., 0.,  COMMENTRULER, QBASE); 
        addRaw(widthAxes, 0., 0., SETSOLIDLINE, QBASE);  
        addRaw(0., 0., 0., color, QBASE); 
        
        U.ruler(yruler, yruler+0.67*uyspan, false, ticks, results); 

        // in Constants.java: NTICKS=0;  NFRACDIGITS=1.
        int nticks = results[NTICKS]; 
        int nfracdigits = results[NFRACDIGITS]; 
        addScaled(xruler, ticks[0], 0., MOVETO, QBASE); 
        addScaled(xruler+xtick, ticks[0], 0., PATHTO, QBASE); 
        addScaled(xruler, ticks[0], 0., PATHTO, QBASE); 
        
        for (int i=1; i<nticks; i++)
        {
            addScaled(xruler, ticks[i], 0., PATHTO, QBASE); 
            addScaled(xruler+xtick, ticks[i], 0., PATHTO, QBASE); 
            int op = (i < nticks-1) ? PATHTO : STROKE;  
            addScaled(xruler, ticks[i], 0., op, QBASE); 
        }

        // label the ticks...

        for (int i=0; i<nticks; i++)
        {
            String s = U.fwd(ticks[i], 16, nfracdigits).trim();
            int nchars = s.length(); 
            for (int k=0; k<nchars; k++)
            {
                int ic = (int) s.charAt(k) + iFontcode; 
                double x = xruler + scaledW*(k-nchars-vrhgap); 
                addScaled(x, ticks[i]+vyoffset, 0., ic, QBASE); 
            }
        }

        // put Vruler title...
        String t = getAxisLabel(1); 
        int nchars = t.length(); 

        if (nticks > 1)
        {
            double tspan = ticks[nticks-1] - ticks[0]; 
            double numer = U.isOdd(nticks) ? nticks-2.0 : nticks-1.0; 
            double denom = 2.0*nticks - 2.0; 
            double y = ticks[0] + tspan * numer/denom;   
            for (int k=0; k<nchars; k++)
            {
                int ic = (int) t.charAt(k) + iFontcode; 
                double x = xruler + (k-nchars-1)*scaledW; 
                addScaled(x, y, 0., ic, QBASE); 
            }
        }
    } // end drawVruler()


    void drawXaxis() // uses global xmin, xmax
    {
        boolean whitebkg = "T".equals(DMF.reg.getuo(UO_LAYOUT, 15));
        addRaw(0., 0., 0.,  COMMENTAXIS, QBASE); 
        addRaw(widthAxes, 0., 0., SETDOTTEDLINE, QBASE);
        addRaw(0., 0., 0.,  SETCOLOR + (whitebkg ? BLACK : WHITE), QBASE);
                
        double xyz[] = new double[3]; 
        xyz[0] = xmin;
        xyz[1] = 0.0; 
        xyz[2] = 0.0; 
        viewelaz(xyz);  
        addScaled(xyz, MOVETO, QBASE); 
        xyz[0] = xmax;
        xyz[1] = 0.0; 
        xyz[2] = 0.0; 
        viewelaz(xyz);  
        addScaled(xyz, STROKE, QBASE); 
    } 


    void drawYaxis()  // uses global ymin, ymax
    {
        boolean whitebkg = "T".equals(DMF.reg.getuo(UO_LAYOUT, 15));
        addRaw(0., 0., 0.,  COMMENTAXIS, QBASE); 
        addRaw(widthAxes, 0., 0., SETDOTTEDLINE, QBASE);
        addRaw(0., 0., 0.,  SETCOLOR + (whitebkg ? BLACK : WHITE), QBASE);

        double xyz[] = new double[3]; 
        xyz[0] = 0.0;
        xyz[1] = ymin; 
        xyz[2] = 0.0; 
        viewelaz(xyz);  
        addScaled(xyz, MOVETO, QBASE); 
        xyz[0] = 0.0;
        xyz[1] = ymax; 
        xyz[2] = 0.0; 
        viewelaz(xyz);  
        addScaled(xyz, STROKE, QBASE); 
    }


    void drawZaxis()  // uses global zmin, zmax
    {
        boolean whitebkg = "T".equals(DMF.reg.getuo(UO_LAYOUT, 15));
        addRaw(0., 0., 0.,  COMMENTAXIS, QBASE); 
        addRaw(widthAxes, 0., 0., SETDOTTEDLINE, QBASE);
        addRaw(0., 0., 0.,  SETCOLOR + (whitebkg ? BLACK : WHITE), QBASE);

        double xyz[] = new double[3]; 
        xyz[0] = 0.0;
        xyz[1] = 0.0; 
        xyz[2] = zmin; 
        viewelaz(xyz);  
        addScaled(xyz, MOVETO, QBASE); 
        xyz[0] = 0.0;
        xyz[1] = 0.0; 
        xyz[2] = zmax; 
        viewelaz(xyz);  
        addScaled(xyz, STROKE, QBASE); 
    } // end drawZaxis()




    ///////////// support functions begin here //////////////

    void viewelaz(double[] xyz)
    // puts a labframe point xyz into user's el, az viewframe
    {
        double horiz, vert, outof;
        int vaxis = getLayoutVaxis(); // 0...5
        switch (vaxis)
        {
            case 0: 
              horiz = xyz[1]*sinaz + xyz[2]*cosaz;
              vert =  xyz[0]*cosel - xyz[1]*sinel*cosaz + xyz[2]*sinel*sinaz;
              outof = xyz[0]*sinel + xyz[1]*cosaz*cosel - xyz[2]*cosel*sinaz;
              break;

            case 1:
              horiz = -xyz[1]*sinaz + xyz[2]*cosaz;
              vert =  -xyz[0]*cosel + xyz[1]*sinel*cosaz + xyz[2]*sinel*sinaz;
              outof = -xyz[0]*sinel - xyz[1]*cosel*cosaz - xyz[2]*cosel*sinaz;
              break;

            case 2: 
              horiz = -xyz[0]*sinaz + xyz[2]*cosaz;
              vert =   xyz[0]*sinel*cosaz + xyz[1]*cosel + xyz[2]*sinel*sinaz;
              outof = -xyz[0]*cosel*cosaz + xyz[1]*sinel - xyz[2]*cosel*sinaz;
              break;

            case 3:
              horiz = xyz[0]*sinaz + xyz[2]*cosaz;
              vert = -xyz[0]*sinel*cosaz - xyz[1]*cosel + xyz[2]*sinel*sinaz;
              outof = xyz[0]*cosel*cosaz - xyz[1]*sinel - xyz[2]*cosel*sinaz;
              break;

            case 4:
              horiz = xyz[0]*cosaz - xyz[1]*sinaz;
              vert =  xyz[0]*sinel*sinaz + xyz[1]*sinel*cosaz + xyz[2]*cosel;
              outof = -xyz[0]*cosel*sinaz - xyz[1]*cosel*cosaz + xyz[2]*sinel;
              break;

            case 5:
              horiz = xyz[0]*cosaz + xyz[1]*sinaz;
              vert =  xyz[0]*sinel*sinaz - xyz[1]*sinel*cosaz - xyz[2]*cosel;
              outof = -xyz[0]*cosel*sinaz + xyz[1]*cosel*cosaz - xyz[2]*sinel;
              break;

            default:
              return;   // no change in xyz[]
        };
        xyz[0] = horiz;
        xyz[1] = vert;
        xyz[2] = outof;
    }


    int getLayoutVaxis()
    // Returns 0....5 meaning +X....-Z
    {
        int VAX=4; // array base index in Constants
        for (int i=0; i<6; i++)
          if ("T".equals(DMF.reg.getuo(UO_LAYOUT, i+VAX)))
            return i;
        return 0;
    }

    String getAxisLabel(int whichbaseAxis)
    /// use whichbaseAxis=0 for hor axis label
    /// use whichbaseAxis=1 for vert axis label
    {
        double labx[] = {1, 0, 0};
        viewelaz(labx); 
        if (labx[whichbaseAxis] > 0.998)
          return "+X";  
        if (labx[whichbaseAxis] < -0.998)
          return "-X"; 
        double laby[] = {0, 1, 0}; 
        viewelaz(laby); 
        if (laby[whichbaseAxis] > 0.998)
          return "+Y"; 
        if (laby[whichbaseAxis] < -0.998)
          return "-Y"; 
        double labz[] = {0, 0, 1}; 
        viewelaz(labz); 
        if (labz[whichbaseAxis] > 0.998)
          return "+Z"; 
        if (labz[whichbaseAxis] < -0.998)
          return "-Z"; 
        return "  "; 
    }


    void vx2lab(double xyz[], double surf[])
    // converts a vxframe xyz[] to labframe xyz[].
    {
        double x = xyz[0]; 
        double y = xyz[1]; 
        double z = xyz[2]; 
        xyz[0] = surf[OE11]*x + surf[OE12]*y + surf[OE13]*z + surf[OX]; 
        xyz[1] = surf[OE21]*x + surf[OE22]*y + surf[OE23]*z + surf[OY]; 
        xyz[2] = surf[OE31]*x + surf[OE32]*y + surf[OE33]*z + surf[OZ]; 
    }


    static void ssort(double[] dkeys, boolean bb[], int ii[], 
                      int jj[], int kk[], int num)
    // selection sort, Sedgewick p.95;
    // puts keys into ascending order with their luggage. 
    {
        double dtemp; 
        int itemp, jtemp, ktemp;
        boolean btemp; 
        for (int i=0; i<num-1; i++)
        {
            for (int j=i+1; j<num; j++)
              if (dkeys[j] < dkeys[i])
              {
                  itemp = ii[j]; 
                  jtemp = jj[j]; 
                  ktemp = kk[j]; 
                  dtemp = dkeys[j]; 
                  btemp = bb[j]; 

                  ii[j] = ii[i];
                  jj[j] = jj[i]; 
                  kk[j] = kk[i]; 
                  dkeys[j] = dkeys[i]; 
                  bb[j] = bb[i]; 

                  ii[i] = itemp; 
                  jj[i] = jtemp;
                  kk[i] = ktemp; 
                  dkeys[i] = dtemp;
                  bb[i] = btemp; 
              }
         }
    }
}
