package com.stellarsoftware.beam;

import javax.swing.*;      // Graphics2D features
import java.io.*;          // Save as file 

@SuppressWarnings("serial")

/**
  * H1DPanel draws a 1D binned ray histogram.
  *
  * Custom artwork class furnishes TechList to GPanel.
  * Rendering is done within GPanel. 
  *
  * LIKE other art, creates new histo when UO=OK
  *  or when nsurfs, ngroups, nrays change.  
  *
  * Unit square coordinates, like P2D unit cube coordinates:
  * no intermediate scale factors but instead scales integers.
  *
  * Has Auto, Diameter, and Manual spans.
  * Uses precomputed tick values. 
  *
  * Artwork:  GPanel calls this.doTechList(). 
  *
  * Repaint: handled within GPanel using blit.
  *
  * Zoom/Pan: GPanel manages zoom/pan.....
  *     modify zoom coefficients; 
  *     gTech.dispose();
  *     repaint();
  * ...which forces a call to this.doTechList().
  *
  * Random ray:  calls this.addRandomRay()....
  *   ... which augments histo[] without updating artwork.
  *   
  * Random ray burst repaint is done by GPanel::redo()....
  *     gTech.dispose(); 
  *     repaint(); 
  * ...which forces a call to this.doTechList() thereby
  *  showing the current contents of the histogram. 
  *
  *  A169 March 2015: added average display of histogrammed data;
  *  also improved the typeface scaling and locations.
  * 
  * @author M.Lampton (c) STELLAR SOFTWARE 2004, 2015 all rights reserved.
  */
public class H1DPanel extends GPanel
{
    // public static final long serialVersionUID = 42L;
    
    private int ngroups, nsurfs, npSurfs, npRays, nrays, ngood;
    private int hsurf, hattr; 
    private int prevGroups[] = new int[MAXSURFS+1]; // detect new groups
    
    final double EXTRAROOM = 1.5;  // windowsize / plotbox
    final double EXTRASPAN = 1.5;  // plotwidth / datarange
    final double MINSPAN = 1E-8; 
    final int MAXBINS = 1025; 

    private double histopeak = 1.0; 
    private double histotop = 1.0; 
    private String shmin, shmax;      // used for labelling axis
    private double hmin=0, hmax=0;    // used for digitizing each ray 
    private double hmid=0, hspan=1;   // used for drawing scales
    private int nbins = 0;            // used for digitizing each ray 

    private int CADstyle=0;
    private int histo[] = new int[MAXBINS]; 
    private int vnticks, vndigits; 
    private double vticks[] = new double[10];
    private String hst; 
     
    private double sum = 0.0; 
    private int    count = 0; 
    private boolean bShowAverage = true; 


    public H1DPanel(GJIF gj) // the constructor
    // Called by GJIF to begin a new P1D panel.
    // Sets up parameters, runs table rays, builds initial histogram.
    // No artwork here.
    // Later, GJIF will display its GPanel, hence doTechList() below.
    // paintComponent(), annotate() etc are done in GPanel.
    {
        // implicitly calls super() with no arguments
        
        myGJIF = gj;           // protected; used here & GPanel
        bClobber = true;       // protected; random redo() needs new artwork
        bPleaseParseUO = true; // protected; assures initial parse.  
    }



    //-----------protected methods----------------


    protected void doTechList(boolean bFullArt) // replaces abstract method
    // Called by GPanel for artwork: new, pan, zoom, & random ray group.
    {
        nsurfs = DMF.giFlags[ONSURFS];                 // always needed.
        ngroups = DMF.giFlags[ONGROUPS];               // always needed.
        nrays = DMF.giFlags[RNRAYS];                   // always needed.
        ngood = RT13.iBuildRays(true);
        
        
        String warn = getUOwarning();   // never crashes.
        myGJIF.postWarning(warn); 
        if (warn.length() > 0)
          return;
          
          
        //---see if group assignments have changed----
        boolean bChanged = false; 
        for (int j=0; j<MAXSURFS; j++)
          if (prevGroups[j] != RT13.group[j])
          {
              prevGroups[j] = RT13.group[j]; 
              bChanged = true; 
          }
        
        if ((npSurfs != nsurfs) || (npRays != nrays) || bPleaseParseUO || bChanged)
        {
            doParse();     
            npSurfs = nsurfs; 
            npRays = nrays; 
        }

        doArt();
    }
    
    protected void doRotate(int i, int j) // replaces abstract "do" method
    {
        return; 
    }


    protected boolean doRandomRay() // replaces abstract "do" method
    {
        if (RT13.bRunRandomRay())
        {
           addRayToHisto(0); 
           return true; 
        }
        return false; 
    } 
    
    protected void doFinishArt()     // replaces abstract "do" method
    {
        return; 
    }


    protected void doCursor(int ix, int iy)  // replaces abstract method
    // delivers current cursor coordinates
    {
        return; 
    }

    protected double getStereo()    // replaces abstract "get" method
    {
        return 0.0; 
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
            for (int i=0; i<nbins; i++)
              pw.println(histo[i]); 
            fw.close();
        }
        catch (Exception e)
        {}
    } 


    protected void doPlainSaveData()  // stupider version of the above
    {
        File file = new File("H1D.TXT");   // import java.io.*;
        FileWriter fw = null;              // import java.io.*;
        PrintWriter pw = null;             // import java.io.*;
        try
        {
            fw = new FileWriter(file);
            pw = new PrintWriter(fw);
            for (int i=0; i<nbins; i++)
              pw.println(histo[i]); 
            fw.close();
        }
        catch (Exception e)
        {}
    } 




    //---------private methods---------------

    private String getUOwarning()
    // Evaluates User Option "UO" fields.
    // Local variables shadow H1D fields. 
    // First line of defense, must never crash. 
    {
        int ngroups = DMF.giFlags[ONGROUPS];
        String hst = DMF.reg.getuo(UO_1D, 0); 
        int op = REJIF.getCombinedRayFieldOp(hst); 
        int hsurf = RT13.getGroupNum(op); 
        int hattr = RT13.getAttrNum(op); 
        if ((hsurf<0) || (hattr<0) || (hattr>RNATTRIBS))
          return "Unknown variable:  "+hst; 
        return "";
    }

    private void doParse()
    // Parses UO fields, performs sizing; trace was already run. 
    {
        bPleaseParseUO = false; // flag in GPanel

        shmin = new String(""); // value label
        shmax = new String(""); // value label

        hst = DMF.reg.getuo(UO_1D, 0); 
        int op = REJIF.getCombinedRayFieldOp(hst); 
        hsurf = RT13.getGroupNum(op); 
        hattr = RT13.getAttrNum(op); 
        nbins = U.parseInt(DMF.reg.getuo(UO_1D, 1));  
        nbins = Math.max(2, Math.min(MAXBINS, nbins)); 

        CADstyle = 0;  

        //-----Automatic haxis scaling from good rays--------
        //  Goal here is to determine hmin, hmax.

        int ngood=0; 
        RT13.iBuildRays(true); 
        for (int kray=1; kray<=nrays; kray++)
        {
            if (RT13.bGoodRay[kray])
            {
                double h = RT13.dGetRay(kray, hsurf, hattr); 
                if (ngood==0)
                {
                   hmin = hmax = h;  // startup
                }
                if (h>hmax) hmax=h; 
                if (h<hmin) hmin=h; 
                ngood++; 
            }
        }
        
        //------enlarge (hmin,hmax) symmetrically if necessary------

        hspan = Math.max(MINSPAN, EXTRASPAN*(hmax-hmin)); 
        hmid = 0.5*(hmin+hmax); 
        hmin = hmid - 0.5*hspan; 
        hmax = hmid + 0.5*hspan; 

        //-------rulerize auto (hmin, hmax) and post results----------

        int results[] = new int[2]; 
        double hticks[] = new double[10]; 
        U.ruler(hmin, hmax, true, hticks, results); 
        int hnticks = results[0]; 
        int hndigits = results[1]; 
        hmin = hticks[0];     
        hmax = hticks[hnticks-1];
        hmid = 0.5*(hmin+hmax); 
        hspan = hmax-hmin; 
        shmin = U.fwd(hmin, 16, hndigits).trim();
        shmax = U.fwd(hmax, 16, hndigits).trim(); 

        //----apply diameter limits if requested & local------------

        boolean bLocal = ((hattr==RTXL) || (hattr==RTYL));
        if (bLocal && "T".equals(DMF.reg.getuo(UO_1D, 3)))
        {
            double dDiam = (hattr==RTXL) 
                             ?  RT13.surfs[hsurf][OODIAX] 
                             :  RT13.surfs[hsurf][OODIAY]; 
            if (dDiam > MINSPAN)
            {
                hmin = -0.5*dDiam; 
                hmax = +0.5*dDiam; 
                hmid = 0.5*(hmin+hmax); 
                hspan = hmax-hmin; 
                shmin = U.tidy(hmin); 
                shmax = U.tidy(hmax); 
            }
        } 

        //-------apply manual span if requested----------------------
        //--But shouldn't the manual span always come first,---------
        //--so as to suppress the automatic span which might fail?---
        if ("T".equals(DMF.reg.getuo(UO_1D, 4)))
        {
            String sHmin = DMF.reg.getuo(UO_1D, 5); 
            double dHmin = U.suckDouble(sHmin); 
            String sHmax = DMF.reg.getuo(UO_1D, 6); 
            double dHmax = U.suckDouble(sHmax); 
            double dSpan = Math.abs(dHmax - dHmin); 
            if (!U.isNegZero(dHmin) && !U.isNegZero(dHmax) && (dSpan > MINSPAN))
            {
                hmax = dHmax; 
                hmin = dHmin;
                hmid = 0.5*(hmin+hmax); 
                hspan = hmax-hmin; 
                shmin = U.tidy(hmin); 
                shmax = U.tidy(hmax); 
            }
        } 

        //---all done scaling data to histogram---------------

        bShowAverage = "T".equals(DMF.reg.getuo(UO_1D, 7)); 
        
        //----set up GPanel affines for scaledItem()----
        //----never in doArt() or pan zoom will fail---
        
        uxcenter = 0.0; 
        uxspan = EXTRAROOM; // around the plotbox
        uycenter = 0.0; 
        uyspan = EXTRAROOM; // around the plotbox

        //---finally compute the table-ray histogram------

        for (int i=0; i<MAXBINS; i++)
          histo[i] = 0; 

        for (int kray=1; kray<=nrays; kray++)
          if (RT13.bGoodRay[kray])
            addRayToHisto(kray); 
            
    } //---end doParse().


    //---------ARTWORK BEGINS HERE----------------
    //---------ARTWORK BEGINS HERE----------------
    //---------ARTWORK BEGINS HERE----------------

    private void doArt()
    {
        setVertScale(); 

        int iFontcode = getUOGraphicsFontCode();  
        int iHpoints = iFontcode / 10000;     
        int iWpoints = 1 + iHpoints / 2;   
        double scaledW = iWpoints * uxspan / dUOpixels; 
        double xtick = 0.5 * iWpoints * uxspan / dUOpixels; 
        double ytick = 0.5 * iWpoints * uyspan / dUOpixels;  
        double scaledH = iHpoints * uyspan / dUOpixels; 
        double yruler = -0.5;                       // unit plot box
        double ytop = 0.5;                          // unit plot box
        double hyoffset = 0.7*scaledH;              // Char CenterOrigin
        double vyoffset = 0.0;
        double vrhgap = -0.2;                       // CenterOrigin

        //-----start the drawing---------

        clearXYZO();                    // always clear quadlist
        addXYZO(SETWHITEBKG);
        addXYZO(SETCOLOR + BLACK); 
        addXYZO(1.0, SETSOLIDLINE);     // for entire artwork
        addXYZO(COMMENTRULER);          // advertise the H ruler

        //-----draw a unit box------

        addScaledItem(-0.5, -0.5, MOVETO); 
        addScaledItem(+0.5, -0.5, PATHTO); 
        addScaledItem(+0.5, +0.5, PATHTO); 
        addScaledItem(-0.5, +0.5, PATHTO); 
        addScaledItem(-0.5, -0.5, STROKE); 

        double x=0; 

        //----------left tick value------------
        x = -0.5 -0.5*scaledW*(shmin.length()-1); 
        for (int k=0; k<shmin.length(); k++)
        {
            int ic = (int) shmin.charAt(k) + iFontcode; 
            addScaledItem(x, yruler-hyoffset, 0.0, ic); 
            x += scaledW; 
        }

        //----------right tick value------------
        x = +0.5 -0.5*scaledW*(shmax.length()-1); 
        for (int k=0; k<shmax.length(); k++)
        {
            int ic = (int) shmax.charAt(k) + iFontcode; 
            addScaledItem(x, yruler-hyoffset, 0.0, ic); 
            x += scaledW; 
        }

        //---------title for horizontal axis-----------

        String hst = DMF.reg.getuo(UO_1D, 0).trim(); 
        int nchars = hst.length(); 
        for (int k=0; k<nchars; k++)
        {
            int ic = (int) hst.charAt(k) + iFontcode; 
            x = scaledW*(k-nchars/2); 
            addScaledItem(x, yruler-hyoffset, ic); 
        }

        //-------v ruler at left and right-----------

        addXYZO(COMMENTRULER);

        for (int i=0; i<vnticks; i++)
        {
            double yy = -0.5 + vticks[i]/histotop;
            addScaledItem(-0.5, yy, MOVETO); 
            addScaledItem(-0.5+xtick, yy, STROKE); 
            addScaledItem(+0.5, yy, MOVETO); 
            addScaledItem(+0.5-xtick, yy, STROKE); 
        }

        //------vertical axis tick labels loop-----------

        for (int i=0; i<vnticks; i++)
        {
            String s = U.fwd(vticks[i], 16, vndigits).trim();
            nchars = s.length(); 
            for (int k=0; k<nchars; k++)
            {
                int ic = (int) s.charAt(k) + iFontcode;     
                double xvert = -0.5 + scaledW*(k-nchars-vrhgap); 
                double yvert = -0.5 + vticks[i]/histotop + vyoffset; 
                addScaledItem(xvert, yvert, ic);          
            }
        } 

        //------title for vertical axis-------------

        String vst = "Nhits";
        nchars = vst.length(); 

        for (int k=0; k<nchars; k++)
        {
            int ic = (int) vst.charAt(k) + iFontcode; 
            double xvert = -0.5 + (k-nchars-3)*scaledW;
            addScaledItem(xvert, 0.1, ic);
        }

        //---------now draw the histogram-----------

        double dx = 1.0/nbins; 
        double y1 = -0.5 + histo[1]/histotop; 
        addScaledItem(-0.5, y1, MOVETO); 
        for (int i=1; i<nbins; i++)
        {
            double xhisto = -0.5 + i*dx;     
            double yhisto = -0.5 + histo[i]/histotop;  
            addScaledItem(xhisto, yhisto, PATHTO); 
            int op = (i < nbins-1) ? PATHTO : STROKE; 
            addScaledItem(xhisto+dx, yhisto, op); 
        }

        if (bShowAverage && (count > 0))
        {
            double average = sum/count; 
            String sAverage = "avg=" +U.fwd(average,12,3).trim()+" n="+count; 
            int nchar = sAverage.length(); 
            for (int k=0; k<nchar; k++)
            {
                int ic = (int) sAverage.charAt(k) + iFontcode; 
                x = scaledW*(k-nchar/2); 
                addScaledItem(x, ytop+hyoffset, ic); 
            }
        }
            
    } // end doTechList().



    public int getNbins()         // for MTF
    {
        return nbins;
    }

    public double getHistoSpan()  // for MTF
    {
        return hspan; 
    }

    public int getHisto(int i)    // for MTF
    {
        return ((i>=0) && (i<nbins)) ? histo[i] : 0;
    }


    private void addRayToHisto(int kray)
    {
        double h = RT13.dGetRay(kray, hsurf, hattr); 
        int ih = (int) Math.floor(nbins*(h-hmin)/(hmax-hmin)); 
        if ((ih>=0) && (ih<nbins))
          histo[ih]++; 
        sum += h; 
        count++; 
    }


    private void setVertScale()
    // Frequent rescalings as histo builds up.
    {
        histopeak = 10.0; // never zero!
        for (int i=0; i<nbins; i++)
          if (histopeak < histo[i])
            histopeak = histo[i];
        int results[] = new int[2]; 
        U.ruler(0, histopeak, true, vticks, results); 
        vnticks = results[0];
        vndigits = results[1]; 
        histotop = vticks[vnticks-1]; 
    }
}
