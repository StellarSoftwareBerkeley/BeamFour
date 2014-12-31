package com.stellarsoftware.beam;

import javax.swing.*;      // Graphics2D features

@SuppressWarnings("serial")

/**
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
  * @author M.Lampton (c) STELLAR SOFTWARE 2004 all rights reserved.
  */
public class Plot2Panel extends GPanel
{
    // public static final long serialVersionUID = 42L;

    //---non static to permit many instances------

    final double    EXTRAROOM = 2.0;  
    final double    MINSPAN = 1E-6; 
    private int     CADstyle = 0;
    private int     iSymbol = 0; 

    private int     nsurfs, nprev, ngroups, nrays, ngood;
    private int     hsurf, hattr, vsurf, vattr;  
    private int     prevGroups[] = new int[MAXSURFS+1]; // detect new groups

    private int     jOtherSurface = 0; 
    private String  hst, vst; 
    
    private double  wavel = 0.0; 
    private boolean blackbkg = false; 
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
        ngroups = DMF.giFlags[ONGROUPS];  // always needed.
        nrays = DMF.giFlags[RNRAYS];      // always needed.
        ngood = RT13.iBuildRays(true);  
        
        String warn = getUOwarning();     // never crashes.
        myGJIF.postWarning(warn); 
        
        
        if (warn.length() > 0)
          badUO = true;   // use this in setting display scale
        
        //---see if group assignments have changed----
        boolean bChanged = false; 
        for (int j=0; j<MAXSURFS; j++)
          if (prevGroups[j] != RT13.group[j])
          {
              prevGroups[j] = RT13.group[j]; 
              bChanged = true; 
          }
        
        if ((nprev != nsurfs) || bPleaseParseUO || bChanged)
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
        return drawOneRandomRay(); 
    }


    protected void doFinishArt() // replaces abstract "do" method
    {
        return; 
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
    // Ungrouped: ngroups = nsurfs
    // Grouped:   ngroups < nsurfs
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
        int hsurf = RT13.getGroupNum(op); 
        int hattr = RT13.getAttrNum(op); 

        String vst = DMF.reg.getuo(UO_PLOT2, 2); 
        op = REJIF.getCombinedRayFieldOp(vst); 
        int vsurf = RT13.getGroupNum(op); 
        int vattr = RT13.getAttrNum(op); 

        String s = ""; 
        if (ngood < 1)         // yikes what about "good" vs "all rays"?
          s += "No good rays ";
          
        String word = (ngroups<nsurfs) ? "Ngroups="+ngroups : "Nsurfaces="+nsurfs;
          
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
    // Call this to regenerate sizes: new UO or new nsurfs, ngroups.
    // BE SURE TO CALL getUOWarning() first to verify strings OK.
    // RT13.iBuildRays() was already called in doTechList(). 
    {
        bPleaseParseUO = false;     // flag in GPanel

        hst = DMF.reg.getuo(UO_PLOT2, 0); 
        int op = REJIF.getCombinedRayFieldOp(hst); 
        hsurf = RT13.getGroupNum(op); 
        hattr = RT13.getAttrNum(op); 

        vst = DMF.reg.getuo(UO_PLOT2, 2); 
        op = REJIF.getCombinedRayFieldOp(vst); 
        vsurf = RT13.getGroupNum(op); 
        vattr = RT13.getAttrNum(op); 

        double h=0, hmin=0, hmax=0, v=0, vmin=0, vmax=0; 
        int ngood=0; 

        jOtherSurface = getOther(ngroups); 

        for (int kray=1; kray<=nrays; kray++)
        {
            if (RT13.bGoodRay[kray])
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




    private void doArt()  
    {
        blackbkg = "T".equals(DMF.reg.getuo(UO_PLOT2, 12));
        iSymbol = DOT; 
        if ("T".equals(DMF.reg.getuo(UO_PLOT2, 6)))
          iSymbol = PLUS; 
        if ("T".equals(DMF.reg.getuo(UO_PLOT2, 7)))
          iSymbol = SQUARE; 
        if ("T".equals(DMF.reg.getuo(UO_PLOT2, 8)))
          iSymbol = DIAMOND; 

        clearXYZO();       
        addXYZO(blackbkg ? SETBLACKBKG : SETWHITEBKG);
        addXYZO(SETCOLOR + (blackbkg ? WHITE : BLACK)); 

        // Need ternary decision: auto,diam,manual.
        // Here is auto mode....
        // NEED THOROUGH PROTECTION AGAINST ZERO DENOMINATORS HERE...


        ///////////// draw the furniture.  //////////

        addXYZO(1.0, SETSOLIDLINE);     // for rulers
        addXYZO(COMMENTRULER);          // advertise the H ruler
        addXYZO(SETCOLOR + (blackbkg ? WHITE : BLACK)); 

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
        addScaledItem(ticks[0], yruler, MOVETO); 
        addScaledItem(ticks[0], yruler+ytick, PATHTO); 
        addScaledItem(ticks[0], yruler, PATHTO); 
        for (int i=1; i<nticks; i++)
        {
            addScaledItem(ticks[i], yruler, PATHTO); 
            addScaledItem(ticks[i], yruler+ytick, PATHTO); 
            int op = (i < nticks-1) ? PATHTO : STROKE;  
            addScaledItem(ticks[i], yruler, op); 
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
                    addScaledItem(x, yruler+hyoffset, 0.0, ic);
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
            addScaledItem(x, yruler-2.5*scaledH, 0.0, ic);
        }


        /////////// the Y ruler ///////////////////////////
        // Bug with small window:
        // Too many digits can overflow leftward off screen!
        // Fix: do Y axis first, shove xruler rightward if necessary.
        // Then do X axis using possibly shoved xruler. 

        addXYZO(COMMENTRULER);  // the V ruler
        U.ruler(yruler, yruler+0.67*uyspan, false, ticks, results); 
        nticks = results[NTICKS]; 
        nfracdigits = results[NFRACDIGITS]; 
        addScaledItem(xruler, ticks[0], MOVETO); 
        addScaledItem(xruler+xtick, ticks[0], PATHTO); 
        addScaledItem(xruler, ticks[0], PATHTO); 
        for (int i=1; i<nticks; i++)
        {
            addScaledItem(xruler, ticks[i],PATHTO); 
            addScaledItem(xruler+xtick, ticks[i], PATHTO); 
            int op = (i < results[NTICKS]-1) ? PATHTO : STROKE;  
            addScaledItem(xruler, ticks[i], op); 
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
                addScaledItem(x, ticks[i]+vyoffset, 0.0, ic);
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
            addScaledItem(x, y, 0.0, ic); 
        }

        jOtherSurface = getOther(nrays); 

        /// finally... draw the table ray hits. 

        for (int k=1; k<=nrays; k++)
          drawOneTableRay(k); // not random ray

    }  //------------------end of doArt()
    
    
    
    

    boolean drawOneTableRay(int kray)
    // Relies upon iSymbol, jOther, hsurf, ..setup as part of doArt().
    // Not for random rays; table rays have 1<=kray<=nrays. 
    {
        int jmin = ngroups;
        if(DMF.reg.getuo(UO_PLOT2, 10).equals("T")) 
          jmin = U.imax3(hsurf, vsurf, jOtherSurface); 

        // jmin now encapsulates our complete success criterion

        boolean bStatus = (RT13.getHowfarRay(kray) >= jmin); 
        if (bStatus)
        {
            int icolor = (int) RT13.raystarts[kray][RSCOLOR]; 
            if (blackbkg && (icolor==BLACK))
              icolor = WHITE; 

            double xxx = RT13.dGetRay(kray, hsurf, hattr); 
            double yyy = RT13.dGetRay(kray, vsurf, vattr); 
            buildScaledItem(xxx, yyy, iSymbol+icolor, false); // false=Table
            if ((jOtherSurface > 0) && (hsurf == vsurf))
            {
               xxx = RT13.dGetRay(kray, jOtherSurface, hattr); 
               yyy = RT13.dGetRay(kray, jOtherSurface, vattr); 
               icolor = blackbkg ? WHITE : BLACK;
               buildScaledItem(xxx, yyy, iSymbol+icolor, false); // false=Table
            }
            return true; 
        }
        return false; 
    } 
    
    
    boolean drawOneRandomRay()
    // Relies upon iSymbol, jOtherSurface, hsurf, ..setup as part of doArt().
    // Success criterion depends on: do we want to require jFinal for all rays?
    // or just plot rays that get as far as our plot surface?
    {
        int jmin = ngroups;
        if(DMF.reg.getuo(UO_PLOT2, 10).equals("T")) // plot all rays?
          jmin = U.imax3(hsurf, vsurf, jOtherSurface); 

        // jmin now encapsulates our complete success criterion

        boolean bStatus;

        RT13.bRunRandomRay();   // run one random ray.
        bStatus = (RT13.getHowfarRay(0) >= jmin);

        if (bStatus)
        {
            int kGuide = RT13.getGuideRay(); 
            int kkk = kGuide;
            int icolor = (int) RT13.raystarts[kkk][RSCOLOR]; 
            if (blackbkg && (icolor==BLACK))
              icolor = WHITE; 

            double xxx = RT13.dGetRay(0, hsurf, hattr); 
            double yyy = RT13.dGetRay(0, vsurf, vattr); 
            buildScaledItem(xxx, yyy, iSymbol+icolor, true); // true=random
            if ((jOtherSurface > 0) && (hsurf == vsurf))
            {
               xxx = RT13.dGetRay(0, jOtherSurface, hattr); 
               yyy = RT13.dGetRay(0, jOtherSurface, vattr); 
               icolor = blackbkg ? WHITE : BLACK;
               buildScaledItem(xxx, yyy, iSymbol+icolor, true); // true=random
            }
            return true; 
        }
        return false; 
    } 


    private int getOther(int ngroups) 
    {
        int myOther = 0; 
        String sOther = DMF.reg.getuo(UO_PLOT2, 11);
        char cOther = U.getCharAt(sOther, 0); 
        if ((cOther == 'f') || (cOther == 'F'))  // final surface
          myOther = ngroups;
        else
          myOther = U.suckInt(sOther);
        if ((myOther < 1) || (myOther > ngroups))
          myOther = 0; 
        return myOther;
    }
}
