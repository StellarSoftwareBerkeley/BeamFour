package com.stellarsoftware.beam;

import javax.swing.*;      // Graphics2D features

@SuppressWarnings("serial")

/**
  * Plot 3D graphic artwork class extends GPanel.
  *
  * Ray values are scaled to a unit cube for display.
  * Unit Cube: uxcenter=0, uxspan=1, etc.
  *
  * Uses CenterOrigin for character locations. 
  * A207: needs -zero detection for absent data, see line 223
  *
  * DeImplemented: additional surface "jOther"
  * Not yet implemented: optical path. 
  *
  * Auto scaling is implemented. 
  * Manual scaling is implemented.
  * Diameter scaling is not yet implemented.  
  *
  * @author M.Lampton (c) STELLAR SOFTWARE 2006 all rights reserved.
  */
public class Plot3Panel extends GPanel
{
    // public static final long serialVersionUID = 42L;

    //---non static to permit many instances----
    
    final double EXTRAROOMA = 2.0;
    final double EXTRAROOMB = 2.0;
    final double EXTRAROOMC = 4.0;  // fails.
    final double MINSPAN = 1E-8; 
    final double NCHARSOFFSET = 1.5; 
    private int  CADstyle = 0;
    private int iSymbol = 0; 
    private double wavel = 0.0; 
    private boolean blackbkg = false;  // option 16 in B4Constants.java

    private int nsurfs, nprev, nrays, ngood, asurf, aattr, bsurf, battr, csurf, cattr;  
    private String ast, bst, cst; 
    private int prevGroups[] = new int[MAXSURFS+1]; // detect new groups


    private double aspan = 1.0;   // adjusted below
    private double bspan = 1.0;   // adjusted below
    private double cspan = 1.0;   // adjusted below
    private double amid = 0.0;    // adjusted below
    private double bmid = 0.0;    // adjusted below
    private double cmid = 0.0;    // adjusted below
    private double az=0, cosaz=1, sinaz=0; 
    private double el=0, cosel=1, sinel=0; 


    public Plot3Panel(GJIF gj)
    {
        myGJIF = gj;           // protected; used here & GPanel
        bClobber = false;      // protected; random redo() keeps old artwork
        bPleaseParseUO = true; // protected; from GPanel. 
        nprev = 0;             // private, triggers parse
        CADstyle = 0;  
        uxcenter = 0.0;        // Unit cube can be installed by addAffines()
        uxspan = EXTRAROOMA;   // Unit cube
        uycenter = 0.0;        // Unit cube
        uyspan = EXTRAROOMB;   // Unit cube
        uzcenter = 0.0;        // Unit cube
        uzspan = EXTRAROOMC;   // Unit cube
    }




    //----------protected and methods-------------------


    protected void doTechList(boolean bFullArt) // replaces abstract method
    // Called by GPanel when fresh artwork is needed:
    // new, pan, zoom, rotate.  
    // But this is not called for annotation mods.
    // For annotation, host's bitmap is blitted instead.
    {
        nsurfs = DMF.giFlags[ONSURFS];                  // always needed.
        nrays = DMF.giFlags[RNRAYS];                    // always needed.
        ngood = RT13.iBuildRays(true);
        
        String warn = getUOwarning();  // never crashes.
        myGJIF.postWarning(warn); 
        if (warn.length() > 0) 
          return; 
 
        if ((nprev != nsurfs) || bPleaseParseUO)
        {
            nprev = nsurfs;
            doParseSizes(); 
        }
        doArt();
    }


    protected void doRotate(int i, int j) // replaces abstract "do" method
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

    protected boolean doRandomRay() // replaces abstract "do" method
    {
        return drawOneRay(0); 
    }
    
    
    protected void doCursor(int ix, int iy)
    // delivers current cursor coordinates
    {
        return; 
    }

    protected double getStereo()  // replaces abstract "get" method
    {
        double d = 0.0; 
        boolean bS = "T".equals(DMF.reg.getuo(UO_PLOT3, 17));
        if (bS)
        {
            String ss = DMF.reg.getuo(UO_PLOT3, 18); 
            d = U.suckDouble(ss); 
            if (d == Double.NaN)
              d = 0.0; 
        }
        return d; 
    }

    protected void doSaveData()   // replaces abstract "do" method
    {
        return; 
    } 


    //------------private methods-------------------
    
    private String getUOwarning()
    // Local variables shadow fields of Plot3D. 
    // First line of defense, must never crash. 
    {
        String word = "Nsurfaces="+nsurfs;
          
        String ast = DMF.reg.getuo(UO_PLOT3, 0); 
        int op = REJIF.getCombinedRayFieldOp(ast); 
        int asurf = RT13.getSurfNum(op); 
        int aattr = RT13.getAttrNum(op); 
        if ((aattr<0) || (aattr>RNATTRIBS))
          return "Unknown:  '"+ast+"'"; 
        if (asurf<0)
          return "Bad '"+ast+"'  "+word;
          
        String bst = DMF.reg.getuo(UO_PLOT3, 2); 
        op = REJIF.getCombinedRayFieldOp(bst); 
        int bsurf = RT13.getSurfNum(op); 
        int battr = RT13.getAttrNum(op); 
        if ((battr<0) || (battr>RNATTRIBS))
          return "Unknown:  '"+bst+"'"; 
        if (bsurf<0)
          return "Bad '"+bst+"'  "+word; 
          
        String cst = DMF.reg.getuo(UO_PLOT3, 4); 
        op = REJIF.getCombinedRayFieldOp(cst); 
        int csurf = RT13.getSurfNum(op); 
        int cattr = RT13.getAttrNum(op); 
        if ((cattr<0) || (cattr>RNATTRIBS))
          return "Unknown:  '"+cst+"'"; 
        if (csurf<0)
          return "Bad '"+cst+"'  "+word; 

        return ""; 
    }


    private void doParseSizes()
    // Evaluates UO fields. Failure should never happen.
    {
        bPleaseParseUO = false; 

        ast = DMF.reg.getuo(UO_PLOT3, 0); 
        int op = REJIF.getCombinedRayFieldOp(ast); 
        asurf = RT13.getSurfNum(op); 
        aattr = RT13.getAttrNum(op); 

        bst = DMF.reg.getuo(UO_PLOT3, 2); 
        op = REJIF.getCombinedRayFieldOp(bst); 
        bsurf = RT13.getSurfNum(op); 
        battr = RT13.getAttrNum(op); 

        cst = DMF.reg.getuo(UO_PLOT3, 4); 
        op = REJIF.getCombinedRayFieldOp(cst); 
        csurf = RT13.getSurfNum(op); 
        cattr = RT13.getAttrNum(op); 

        //--------------view angles--------------
        
        el = U.suckDouble(DMF.reg.getuo(UO_PLOT3, 6));
        cosel = U.cosd(el); 
        sinel = U.sind(el); 
        az = U.suckDouble(DMF.reg.getuo(UO_PLOT3, 7));  
        cosaz = U.cosd(az); 
        sinaz = U.sind(az); 

        //---------automatic scaling---------------

        double a=0, amin=0, amax=0, b=0, bmin=0, bmax=0, c=0, cmin=0, cmax=0; 
        int ngood=0; 
        for (int kray=1; kray<=nrays; kray++)
        {
            if (RT13.isRayOK[kray])
            {
                a = RT13.dGetRay(kray, asurf, aattr); 
                b = RT13.dGetRay(kray, bsurf, battr); 
                c = RT13.dGetRay(kray, csurf, cattr);

                if (ngood==0)  // startup
                {
                   amin = amax = a;
                   bmin = bmax = b; 
                   cmin = cmax = c; 
                }
                if (a>amax) amax=a; 
                if (a<amin) amin=a; 
                if (b>bmax) bmax=b; 
                if (b<bmin) bmin=b; 
                if (c>cmax) cmax=c; 
                if (c<cmin) cmin=c;  
                ngood++; 
            }
        }
        amid = 0.5 * (amax + amin); 
        bmid = 0.5 * (bmax + bmin); 
        cmid = 0.5 * (cmax + cmin); 
        aspan = Math.max(MINSPAN, amax-amin); 
        bspan = Math.max(MINSPAN, bmax-bmin); 
        cspan = Math.max(MINSPAN, cmax-cmin); 
        if (U.areSimilar(aattr, battr))
          aspan = bspan = Math.max(aspan, bspan); 
        if (U.areSimilar(battr, cattr))
          bspan = cspan = Math.max(bspan, cspan); 
        if (U.areSimilar(cattr, aattr))
          cspan = aspan = Math.max(cspan, aspan); 

        //---------manual scaling---------------

        double gaspan = U.suckDouble(DMF.reg.getuo(UO_PLOT3, 1)); 
        if (gaspan > 0.0)
          aspan = EXTRAROOMA * aspan; 
        double gbspan = U.suckDouble(DMF.reg.getuo(UO_PLOT3, 3)); 
        if (gbspan > 0.0)
          bspan = EXTRAROOMB * gbspan; 
        double gcspan = U.suckDouble(DMF.reg.getuo(UO_PLOT3, 5));
        if (gcspan > 0.0)
          cspan = EXTRAROOMC * gcspan;
        wavel = U.suckDouble(DMF.reg.getuo(UO_PLOT3, 8)); 

    }  // end of doParse()









    //-------ARTWORK---------------
    //-------ARTWORK---------------
    //-------ARTWORK---------------
    

    private void addView(double x, double y, double z, int op, int whichbase)
    {
        double xyz[] = {x, y, z};     
        addView(xyz, op, whichbase); 
    }
    
    private void addView(double xyz[], int op, int whichbase)
    {
        viewelaz(xyz); 
        addScaled(xyz, op, whichbase);
    }
    

    
    private void doArt() 
    // Called only by doTechList(). 
    {
        iSymbol = DOT; 
        if ("T".equals(DMF.reg.getuo(UO_PLOT3, 10)))
          iSymbol = PLUS;
        if ("T".equals(DMF.reg.getuo(UO_PLOT3, 11)))
          iSymbol = SQUARE; 
        if ("T".equals(DMF.reg.getuo(UO_PLOT3, 12)))
          iSymbol = DIAMOND; 
        blackbkg = "T".equals(DMF.reg.getuo(UO_PLOT3, 16));

        clearList(QBASE); 
        addRaw(0., 0., 0., blackbkg ? SETBLACKBKG : SETWHITEBKG, QBASE);
        addRaw(0., 0., 0., SETCOLOR+(blackbkg ? WHITE : BLACK), QBASE);         
        addRaw(1., 0., 0., SETSOLIDLINE, QBASE); 
        addRaw(0., 0., 0., COMMENTRULER, QBASE); 
        
        drawAruler(); 
        drawBruler(); 
        drawCruler();

        // finally.... draw the ray hits. 
        addRaw(1., 0., 0., SETSOLIDLINE, QBASE); 
        addRaw(0., 0., 0., COMMENTRAY, QBASE); 
        for (int k=1; k<=nrays; k++)
          drawOneRay(k); 
    }

    /*
    private boolean oldDrawOneRay(int kray, int whichbase)
    // Called both by RandomRay and TableRay.
    // Relies upon iSymbol, asurf, bsurf...set up in doArt().
    {
        int jmin = ngroups;
        int icolor = 0; 

        if(DMF.reg.getuo(UO_PLOT3, 14).equals("T")) 
          jmin = U.imax5(asurf, bsurf, csurf, jOther, 0); 

        // jmin now encapsulates our complete success criterion

        boolean bStatus;
        if (kray==0)
        {
            RT13.bRunRandomRay();
            bStatus = (RT13.getHowfarRay(0) >= jmin);
        }
        else
          bStatus = (RT13.getHowfarOK(kray) >= jmin); 

        if (bStatus)
        {
            int kGuide = RT13.getGuideRay(); 
            int kkk = (kray == 0) ? kGuide : kray; 
            int raycolor = (int) RT13.raystarts[kkk][RSCOLOR]; 
            icolor = raycolor; 
            if (whitebkg && (raycolor==WHITE))
              icolor = BLACK; 
            if (!whitebkg && (raycolor==BLACK))
              icolor = WHITE; 
            double xyz[] = new double[3]; 
            double aa = RT13.dGetRay(kray, asurf, aattr); 
            double bb = RT13.dGetRay(kray, bsurf, battr); 
            double cc = RT13.dGetRay(kray, csurf, cattr); 
            xyz[0] = (aa-amid)/aspan;  // unit cube
            xyz[1] = (bb-bmid)/bspan;  // unit cube
            xyz[2] = (cc-cmid)/cspan;  // unit cube
            addView(xyz, iSymbol+icolor, whichbase); 
            if ((jOther > 0) && (asurf == bsurf) && (asurf == csurf))
            {
                aa = RT13.dGetRay(kray, jOther, aattr); 
                bb = RT13.dGetRay(kray, jOther, battr); 
                cc = RT13.dGetRay(kray, jOther, cattr); 
                xyz[0] = (aa-amid)/aspan;  // unit cube
                xyz[1] = (bb-bmid)/bspan;  // unit cube
                xyz[2] = (cc-cmid)/cspan;  // unit cube
                addView(xyz, iSymbol+icolor, whichbase); 
            }
            return true; 
        }
        return false; 
    } 
    */
    
    
    
    boolean drawOneRay(int kray)
    // Relies upon iSymbol, hsurf, ..setup as part of doArt().
    // Handles random rays k=0 also table rays 1<=kray<=nrays. 
    // A207.11 Dec 2018 with bimodals; no groups; no OtherSurface.
    // Plot all that reach these two surfaces, or only OK rays?
    // These are UO_PLOT3D option 13 "good" vs option 14 "all"
    {
        // First get a ray
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
        boolean bHasEnough = RT13.getHowfarOK(kray) >= U.imax3(asurf, bsurf, csurf);
        boolean bHasComplete = RT13.getHowfarOK(kray) == nsurfs; 
        boolean bWantEnough  = DMF.reg.getuo(UO_PLOT3, 14).equals("T");  // option 14 is "Sufficient"
        boolean bWantComplete = DMF.reg.getuo(UO_PLOT3, 13).equals("T");  // option 13 is "Complete"
 
        int icolor;
        if (kray==0)
            icolor = (int) RT13.raystarts[RT13.getGuideRay()][RSCOLOR];
        else
            icolor = (int) RT13.raystarts[kray][RSCOLOR]; 
        if (blackbkg && (icolor==BLACK))
            icolor = WHITE; 
            
        if ((bWantEnough && bHasEnough) || (bWantComplete && bHasComplete))
        {
            double xyz[] = new double[3];
            double a = RT13.dGetRay(kray, asurf, aattr); 
            double b = RT13.dGetRay(kray, bsurf, battr); 
            double c = RT13.dGetRay(kray, csurf, cattr); 
            if (U.isNotNegZero(a) && U.isNotNegZero(b) && U.isNotNegZero(c))
            {
                if (DEBUG)
                   System.out.println("Plot3D is loading one triplet.");
                xyz[0] = (a-amid)/aspan;    // unit cube
                xyz[1] = (b-bmid)/bspan;    // unit cube
                xyz[2] = (c-cmid)/cspan;    // unit cube
                addView(xyz, iSymbol+icolor, base);    
                return true; 
            }
        }
        return false; 
    } 


    private void drawAruler()
    {
        addView(-0.5, -0.5, -0.5, MOVETO, QBASE); 
        addView(+0.5, -0.5, -0.5, STROKE, QBASE); 

        double ticks[] = new double[10]; 
        int results[] = new int[2]; 
        U.ruler(amid-0.5*aspan, amid+0.5*aspan, false, ticks, results); 
        int nticks = results[NTICKS]; 
        int nfracdigits = results[NFRACDIGITS]; // unused with U.gd()
        double dperp = NCHARSOFFSET * dGetCharWidth();
        
        for (int i=0; i<nticks; i++)
        {
            double fract = (ticks[i]-amid)/aspan; 
            addView(fract, -0.5, -0.5, MOVETO, QBASE); 
            addView(fract, -0.5+dperp, -0.5, STROKE, QBASE); 
            String s = U.gd(ticks[i]); 
            addViewedString(fract, -0.5-2*dperp, -0.5, s); 
        }
        addViewedString(0.0, -0.5-5.0*dperp, -0.5, ast.trim()); // legend
    }


    private void drawBruler()
    {
        addView(-0.5, -0.5, -0.5, MOVETO, QBASE); 
        addView(-0.5, +0.5, -0.5, STROKE, QBASE); 

        double ticks[] = new double[10]; 
        int results[] = new int[2]; 
        U.ruler(bmid-0.5*bspan, bmid+0.5*bspan, false, ticks, results); 
        int nticks = results[NTICKS]; 
        int nfracdigits = results[NFRACDIGITS]; // unused with U.gd()
        double dperp = NCHARSOFFSET * dGetCharWidth();

        for (int i=0; i<nticks; i++)
        {
            double fract = (ticks[i]-bmid)/bspan; 
            addView(-0.5, fract, -0.5, MOVETO, QBASE); 
            addView(-0.5+dperp, fract, -0.5, STROKE, QBASE); 
            String s = U.gd(ticks[i]);
            addViewedString(-0.5-2*dperp, fract, -0.5, s); 
        }
        addViewedString(-0.5-5*dperp, 0.0, -0.5, bst.trim()); // legend
    }


    private void drawCruler()
    {
        addView(-0.5, -0.5, -0.5, MOVETO, QBASE); 
        addView(-0.5, -0.5, +0.5, STROKE, QBASE); 

        double ticks[] = new double[10]; 
        int results[] = new int[2]; 
        U.ruler(cmid-0.5*cspan, cmid+0.5*cspan, false, ticks, results); 
        int nticks = results[NTICKS]; 
        int nfracdigits = results[NFRACDIGITS]; // unused with U.gd()
        double dperp = NCHARSOFFSET * dGetCharWidth();

        for (int i=0; i<nticks; i++)
        {
            double fract = (ticks[i]-cmid)/cspan; 
            addView(-0.5, -0.5, fract, MOVETO, QBASE); 
            addView(-0.5, -0.5+dperp, fract, STROKE, QBASE); 
            String s = U.gd(ticks[i]);
            addViewedString(-0.5, -0.5-2*dperp, fract, s); 
        }
        addViewedString(-0.5-3*dperp, -0.5-3*dperp, 0.0, cst.trim()); // legend
    }



    //--------3D artwork low level methods--------------------------


    void viewelaz(double[] xyz)
    // Puts a labframe point xyz into user's el, az viewframe
    // Assumes that globals sinaz...cosel have been preset. 
    // Formulas are for +Z=vertical axis?? under study!
    {

        double horiz = xyz[0]*cosaz - xyz[1]*sinaz;
        double vert =  xyz[0]*sinel*sinaz + xyz[1]*sinel*cosaz + xyz[2]*cosel;
        double outof = -xyz[0]*cosel*sinaz - xyz[1]*cosel*cosaz + xyz[2]*sinel;
        xyz[0] = horiz;
        xyz[1] = vert;
        xyz[2] = outof;
    }
 
    void addViewedString(double x, double y, double z, String s)
    // Places a string so its center is at [xyz].
    // charwidth etc are generated here.
    // Char coordinates are LowerLeftOrigin. 
    {
        double xyz[] = {x, y, z}; 
        viewelaz(xyz);            // xyz[] is now in screen frame
        s = s.trim(); 
        int nchars = s.length(); 
        double dmid = 0.5*(nchars+2);   // +2 or +3; dispute. 

        double scaledWidth = dGetCharWidth(); 
        double xtick = 0.5 * scaledWidth; 
        double ytick = 0.5 * scaledWidth; 
        double yoffset = -1 * scaledWidth;
        xyz[0] -= scaledWidth * dmid;
        // xyz[1] += yoffset;        
        for (int k=0; k<nchars; k++)
        {
             int ic = (int) s.charAt(k) + getUOGraphicsFontCode(); 
             xyz[0] += scaledWidth; 
             addScaled(xyz, ic, QBASE); 
        }
    }

    void addStringLeftward(double x, double y, double z, String s)
    // Places a string so its right end is at [xyz].
    // Strimg appears leftward of the given position. 
    // charwidth etc are generated locally.
    {
        double xyz[] = {x, y, z}; 
        viewelaz(xyz);            // xyz[] is now in screen frame

        s = s.trim(); 
        int nchars = s.length();  

        double scaledWidth = dGetCharWidth(); 
        double xtick = 0.5 * scaledWidth; 
        double ytick = 0.5 * scaledWidth; 
        double yoffset = -1 * scaledWidth;

        xyz[0] -= scaledWidth * (nchars+1);
        // xyz[1] += yoffset; 
        for (int k=0; k<nchars; k++)
        {
             int ic = (int) s.charAt(k) + getUOGraphicsFontCode(); 
             xyz[0] += scaledWidth; 
             addScaled(xyz, ic, QBASE); 
        }
    }

    double dGetCharWidth()
    // Gives width of current char in Unit Cube linear units. 
    {
        int iFontcode = getUOGraphicsFontCode();  
        int iHpoints = iFontcode / 10000;     
        int iWpoints = 1 + iHpoints / 2;  
        return iWpoints * uxspan / dUOpixels; 
    }
}
