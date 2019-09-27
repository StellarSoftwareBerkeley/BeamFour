package com.stellarsoftware.beam;

import javax.swing.*;      // Graphics2D features

@SuppressWarnings("serial")

/**
  * A207.11: eliminated groups; using bimodals; 
  *    dropping "additional surface" option;
  *    uses howfarOK[] and skips negative zero data points.
  *
  * Custom artwork class extends GPanel, supplies artwork.
  * Generates local font information from GetUOGraphicsFontCode().
  * Properly responds to changes in nsurfs & nrays  (A112). 
  * Random implements doRandomRay: adds 1 ray artwork to myRandList.
  *
  * Implements CenterOrigin for character locations. 
  * Implements additional surface "jOtherSurface" 
  * Not yet implemented: optical path. 
  * Not yet implemented: manual scaling; diam scaling. 
  * Does caret shut down when focus is lost?
  *
  * Adopting explicit QBASE methods.
  *
  * @author M.Lampton (c) STELLAR SOFTWARE 2004-2015 all rights reserved.
  */
public class Plot2Panel extends GPanel
{
    // public static final long serialVersionUID = 42L;

    //---non static to permit many instances------

    final double    EXTRAROOM = 2.0;  
    final double    MINSPAN = 1E-6; 
    private int     CADstyle = 0;
    private int     iSymbol = 0; 

    private int     nsurfs, nprev, nrays, ngood;
    private int     hsurf, hattr, vsurf, vattr;  

    private String  hst, vst; 
    
    private double  wavel = 0.0; 
    private boolean blackbkg = false; // option number 11 see below
    private boolean badUO = false;  


    //----------------public methods------------------------

    public Plot2Panel(GJIF gj)
    {
        // implicitly calls super() with no arguments
        myGJIF = gj;            // protected; used here & GPanel
        bClobber =false;        // protected; random redo() keeps old artwork
        bPleaseParseUO = true;  // protected; in GPanel.
        nprev = 0;              // private; triggers parse. 
        CADstyle = 0;  
    }



    //--------------protected methods, called by GPanel---------------

    protected void doTechList(boolean bFullArt) // replaces abstract method
    // Called by GPanel when fresh Plot2Panel artwork is needed.
    {
        nsurfs = DMF.giFlags[ONSURFS];    // always needed.
        nrays = DMF.giFlags[RNRAYS];      // always needed.
        ngood = RT13.iBuildRays(true);  
        
        String warn = getUOwarning();     // never crashes.
        myGJIF.postWarning(warn); 
        
        if (warn.length() > 0)
          badUO = true;   // use this in setting display scale
        
        if ((nprev != nsurfs) || bPleaseParseUO)
        {
            nprev = nsurfs; 
            doParseSizes();   // regenerates sizes & scale factors  
        }
        doArt();              // finally... do the artwork.
    }

    protected void doRotate(int i, int j) // replaces abstract "do" method
    {
        return; 
    }

    protected boolean doRandomRay() // replaces abstract "do" method
    {
        return drawOneRay(0); 
    }

    protected void doCursor(int ix, int iy)  // replaces abstract method
    // Given mouse cursor coordinates in pixels, 
    // delivers current cursor user coordinates
    // or if outside window it refreshes GJIF title/warning.
    {
        if (ix<0)
          myGJIF.cleanupTitle(); // retains any warnings
        else
          myGJIF.postCoords("Hor=" + U.fwd(getuxPixel(ix),18,6).trim() 
                       + "  Vert=" + U.fwd(getuyPixel(iy),18,6).trim());
    }

    protected double getStereo()    // replaces abstract "get" method
    {
        return 0.0; 
    }

    protected void doSaveData()     // replaces abstract "do" method
    {
        return; 
    } 




    //-----------------private methods--------------------------


    private String getUOwarning()
    // This assumes have already set ngood = RT13.iBuildRays().
    // Always call this early in doTechList() as an error detector.
    // Place warning into titlebar using myGJIF.postWarning().
    // Remove doParse() sizing and pan+zoom into separate doSizing().
    // Here, four situations are tested and a warning is generated. 
    // First line of defense, must never crash. 
    // 
    // If no errors, returns the empty string. 
    // new idea (Rod Andrew): if no rays, continue with empty plot
    {
        String hst = DMF.reg.getuo(UO_PLOT2, 0); 
        int op = REJIF.getCombinedRayFieldOp(hst); 
        int hsurf = RT13.getSurfNum(op); 
        int hattr = RT13.getAttrNum(op); 

        String vst = DMF.reg.getuo(UO_PLOT2, 2); 
        op = REJIF.getCombinedRayFieldOp(vst); 
        int vsurf = RT13.getSurfNum(op); 
        int vattr = RT13.getAttrNum(op); 

        String s = ""; 
        if (ngood < 1)         // yikes what about "good" vs "all rays"?
          s += "No good rays ";
          
        String word = "Nsurfaces="+nsurfs;
          
        if ((hattr<0) || (hattr>RNATTRIBS) || (hsurf < 0))
        {
            s += "Bad '" + hst + "'  "; 
            if (hsurf < 0)
              s += word; 
        }
        if ((vattr<0) || (vattr>RNATTRIBS) || (vsurf < 0))
        {
            s += "Bad '" + vst + "'  "; 
            if (vsurf < 0)
              s += word; 
        }
        return s;
    }



    private void doParseSizes()
    // Analyzes UO fields including "final",  and performs sizing. 
    // Call this to regenerate sizes: new UO or new nsurfs.
    // BE SURE TO CALL getUOWarning() first to verify strings OK.
    // RT13.iBuildRays() was already called in doTechList(). 
    {
        bPleaseParseUO = false;     // flag in GPanel

        hst = DMF.reg.getuo(UO_PLOT2, 0); 
        int op = REJIF.getCombinedRayFieldOp(hst); 
        hsurf = RT13.getSurfNum(op); 
        hattr = RT13.getAttrNum(op); 

        vst = DMF.reg.getuo(UO_PLOT2, 2); 
        op = REJIF.getCombinedRayFieldOp(vst); 
        vsurf = RT13.getSurfNum(op); 
        vattr = RT13.getAttrNum(op); 

        double h=0, hmin=0, hmax=0, v=0, vmin=0, vmax=0; 
        int ngood=0; 

        for (int kray=1; kray<=nrays; kray++)
        {
            if (RT13.isRayOK[kray])
            {
                h = RT13.dGetRay(kray, hsurf, hattr); 
                v = RT13.dGetRay(kray, vsurf, vattr); 
                if (ngood==0)  // startup
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

        uxcenter = 0.5 * (hmax + hmin); 
        uycenter = 0.5 * (vmax + vmin); 
        uxspan = EXTRAROOM * Math.max(hmax - hmin, MINSPAN); 
        uyspan = EXTRAROOM * Math.max(vmax - vmin, MINSPAN); 
        if (U.areSimilar(hattr, vattr))  // same type
          uxspan = uyspan = Math.max(uxspan, uyspan); 

        /// end of automatic scaling
        /// now impose manual span if present
        /// be sure to use U.suckDouble() for safety!

        double gxspan = U.suckDouble(DMF.reg.getuo(UO_PLOT2, 1)); 
        if (gxspan > 0.0)
          uxspan = EXTRAROOM*gxspan; 
        double gyspan = U.suckDouble(DMF.reg.getuo(UO_PLOT2, 3)); 
        if (gyspan > 0.0)
          uyspan = EXTRAROOM*gyspan; 
        wavel = U.suckDouble(DMF.reg.getuo(UO_PLOT2, 4)); 
        
        //---now fix up spans for case of badUO, no rays, no scalefactors----
        if ((badUO) && (gxspan==0) && (gyspan==0))
        {
            uxspan = 1; 
            uyspan = 1; 
        }
    } //---------end of doParseSizes()-----------







    //----------ARTWORK------------------------
    //----------ARTWORK------------------------
    //----------ARTWORK------------------------
    

    private void doArt()  
    {
        blackbkg = "T".equals(DMF.reg.getuo(UO_PLOT2, 11));
        iSymbol = DOT; 
        if ("T".equals(DMF.reg.getuo(UO_PLOT2, 6)))
          iSymbol = PLUS; 
        if ("T".equals(DMF.reg.getuo(UO_PLOT2, 7)))
          iSymbol = SQUARE; 
        if ("T".equals(DMF.reg.getuo(UO_PLOT2, 8)))
          iSymbol = DIAMOND; 
        
        //---start the drawing, explicit new way----------
        
        clearList(QBASE);  
        addRaw(0., 0., 0., blackbkg ? SETBLACKBKG : SETWHITEBKG, QBASE);
        addRaw(0., 0., 0., SETCOLOR+(blackbkg ? WHITE : BLACK), QBASE); 
        addRaw(1., 0., 0., SETSOLIDLINE, QBASE); 
        
        // Need ternary decision: auto,diam,manual.
        // Here is auto mode....
        // NEED THOROUGH PROTECTION AGAINST ZERO DENOMINATORS HERE...

        double xruler = uxcenter - 0.33*uxspan;
        double yruler = uycenter - 0.33*uyspan;  

        int iFontcode = getUOGraphicsFontCode();  
        int iHpoints = iFontcode / 10000;     
        int iWpoints = 1 + iHpoints / 2;   
        
        // scaledW = charwidth = constant on screen in spite of zoom
        
        double scaledW = iWpoints * uxspan / dUOpixels; 
        double xtick = 0.5 * iWpoints * uxspan / dUOpixels; 
        double ytick = 0.5 * iWpoints * uyspan / dUOpixels;  
        double scaledH = iHpoints * uyspan / dUOpixels; 
        double hyoffset = -0.7*scaledH;             // Centered
        double vyoffset = 0.0;                      // Centered
        double vrhgap = -0.2;                       // Centered

        double ticks[] = new double[10]; 
        int results[] = new int[2]; 


        ///////////// the X ruler ////////////////////////

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

        // Hruler title = hst....

        String t = hst.trim(); 
        int nchars = t.length(); 
        for (int k=0; k<nchars; k++)
        {
            int ic = (int) t.charAt(k) + iFontcode; 
            double x = uxcenter + scaledW*(k-nchars/2); 
            addScaled(x, yruler-2.5*scaledH, 0., ic, QBASE);
        }


        /////////// the Y ruler ///////////////////////////
        // Bug with small window:
        // Too many digits can overflow leftward off screen!
        // Fix: do Y axis first, shove xruler rightward if necessary.
        // Then do X axis using possibly shoved xruler. 

        addRaw(0., 0., 0., COMMENTRULER, QBASE);  // the V ruler
        U.ruler(yruler, yruler+0.67*uyspan, false, ticks, results); 
        nticks = results[NTICKS]; 
        nfracdigits = results[NFRACDIGITS]; 
        addScaled(xruler, ticks[0], 0., MOVETO, QBASE); 
        addScaled(xruler+xtick, ticks[0], 0., PATHTO, QBASE); 
        addScaled(xruler, ticks[0], 0., PATHTO, QBASE); 
        for (int i=1; i<nticks; i++)
        {
            addScaled(xruler, ticks[i], 0., PATHTO, QBASE); 
            addScaled(xruler+xtick, ticks[i], 0., PATHTO, QBASE); 
            int op = (i < results[NTICKS]-1) ? PATHTO : STROKE;  
            addScaled(xruler, ticks[i], 0., op, QBASE); 
        }

        // labelling...

        for (int i=0; i<nticks; i++)
        {
            String s = U.fwd(ticks[i], 16, nfracdigits).trim();
            nchars = s.length(); 
            for (int k=0; k<nchars; k++)
            {
                int ic = (int) s.charAt(k) + iFontcode; 
                double x = xruler + scaledW*(k-nchars-vrhgap); 
                addScaled(x, ticks[i]+vyoffset, 0., ic, QBASE);
            }
        }

        // title = vst...

        double tspan = ticks[nticks-1] - ticks[0]; 
        double numer = U.isOdd(nticks) ? nticks-2.0 : nticks-1.0; 
        double denom = 2.0*nticks - 2.0; 
        double y = ticks[0] + tspan * numer/denom;   
        String v = vst.trim(); 
        nchars = v.length(); 
        for (int k=0; k<nchars; k++)
        {
            int ic = (int) v.charAt(k) + iFontcode; 
            double x = xruler + (k-nchars-1)*scaledW; 
            addScaled(x, y, 0., ic, QBASE); 
        }

        /// finally... draw the table ray hits. 

        for (int k=1; k<=nrays; k++)
          drawOneRay(k); // not random ray

    }  //------------------end of doArt()----------
    
    
    
    

    boolean drawOneRay(int kray)
    // Relies upon iSymbol, hsurf, ..setup as part of doArt().
    // Handles random rays k=0 also table rays 1<=kray<=nrays. 
    // A207.11 Dec 2018 with bimodals; no groups; no OtherSurface.
    // Plot all that reach these two surfaces, or only OK rays
    // These are UO_PLOT2D option 9 "good" vs option 10 "all"
    // Table rays are plotted as part of QBASE
    // Random rays are plotted as part of QBATCH.
    {
        // First get a ray: new random, or from existing table
        boolean isGood; 
        int base; 
        if (kray==0)
        {
           isGood = RT13.bRunRandomRay();
           base = QBATCH;
        }
        else
        {
           isGood = RT13.isRayOK[kray]; 
           base = QBASE;
        }
    
        // Now test the ray
        
        boolean bHasEnough = RT13.getHowfarOK(kray) >= Math.max(hsurf, vsurf);        
        boolean bHasComplete = RT13.getHowfarOK(kray) == nsurfs; 
        boolean bWantEnough  = DMF.reg.getuo(UO_PLOT2, 10).equals("T"); // option 10 is "Sufficient"
        boolean bWantComplete = DMF.reg.getuo(UO_PLOT2, 9).equals("T");  // option 9 is "Complete"

        int iColor;
        if (kray==0)
            iColor = (int) RT13.raystarts[RT13.getGuideRay()][RSCOLOR];
        else
            iColor = (int) RT13.raystarts[kray][RSCOLOR]; 
        if (blackbkg && (iColor==BLACK))
            iColor = WHITE;  
        if (DEBUG)
        {
           System.out.println("Plot2Panel.drawOneRay has iSymbol, iColor = " + iSymbol + "  " + iColor); 
           System.out.println("Plot2panel.drawOneRay()  bHasComplete, bHasEnough, bWantComplete, bWantEnough = "
               +bHasComplete+bHasEnough+bWantComplete+bWantEnough);   
        }
        if ((bWantEnough && bHasEnough) || (bWantComplete && bHasComplete))
        {
            double xxx = RT13.dGetRay(kray, hsurf, hattr); 
            double yyy = RT13.dGetRay(kray, vsurf, vattr); 
            if (U.isNotNegZero(xxx) && U.isNotNegZero(yyy))
            {
                if (DEBUG)
                   System.out.printf("Plot2Panel is adding one point at xxx, yyy = %8.4f %8.4f \n", xxx, yyy); 
                addScaled(xxx, yyy, 0., iSymbol+iColor, base);    
                return true; 
            }
        }
        return false; 
    } 
}  //----------end of public class------------------    

