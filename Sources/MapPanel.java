package com.stellarsoftware.beam;

import javax.swing.*;      // Graphics2D features
import java.io.*;          // Save as file 
import java.util.*;        // ArrayList
import java.awt.*;         // Action
import java.awt.event.*;   // Events

@SuppressWarnings("serial")

/** March 2015: adopted explicit QBASE for artwork quads. 
  * March 2015: improved output text file, including .CSV format.
  * 2012: Scales added for HorVar and VertVar
  * 2011: With this plan every options starts a fresh new run.
  * But... changes in the optics or ray tables do not start a fresh run.
  * In MultiPlot, they do. 
  * Would be nice if ray or optics changes trigger a fresh run.
  *
  * Here we offer only WFE and PSF (x,y) not PSF(u,v). 
  *
  *========SCHEMATIC FLOW DRIVEN BY TIMER TICKS=====
  *
  *    MapPanel() constructor
  *    {
  *        startMap(); 
  *    }
  *
  *    startMap()  ---<<--from constructor or doTechList()
  *    {
  *       dList = new ArrayList<Double>();  
  *       doParseUO();
  *       startBunches();
  *    }
  *    
  *    startBunches()
  *    {
  *       create myTimer(assign task doTick());
  *    }
  *    
  *    doTick()
  *    {
  *       if (running)
  *       {
  *          doBunchRays();
  *          GPanel.redo(); --->>>-GPanel.redo(): calls repaint()
  *       }                    then OS calls GPanel.paintComponent()
  *    }                       which then calls GPanel.drawPage(g2)
  *                            which then calls doTechList() here.
  *    doBunchRays()
  *    // called as TimerTask doTick().
  *    {
  *       save & modify parms;
  *       run rays;
  *       calc result and add to list;
  *       restore parms.
  *    }
  *    
  *    doTechList()  ----<<<---called by GPanel.drawPage().
  *    {
  *       if (bPleaseParseUO) 
  *         startMap();
  *       else
  *         doArt();
  *    }
  *    
  *=====SCHEMATIC FLOW DRIVEN BY OPTIONS:MAP=====
  *    
  *    Options.doMapDialog()
  *    {
  *       ...
  *       if (RM_MAP == DMF.getFrontGJFType())
  *       {    
  *           GJIF g= DMF.getFrontGJIF();
  *           g.doParseUOandPlot();
  *       }
  *    }
  *    
  *    GPanel.doParseUOandPlot()
  *    {
  *        bPleaseParseUO = true; // just briefly!
  *        getNewArtwork(true); 
  *    }
  *    
  *    GPanel.getNewArtwork()
  *    {
  *        g2Tech = null;  // discards old artwork
  *        repaint();    ----->>----Calls OS
  *    }                      which calls GPanel.paintComponent()
  *                           which calls GPanel.drawPage(g2)
  *                           which calls doTechList(g2) here.
  *    
  *  Scale factors:  -1<x<+1; -1<y<+1. 
  *  But the thermometer lies outside this square. 
  *
  * @author M.Lampton (c) STELLAR SOFTWARE 2011 all rights reserved.
  */
public class MapPanel extends GPanel // implements Runnable
{
    // public static final long serialVersionUID = 42L;

    private ArrayList<Double> dList;   // blur values for artwork
    private ArrayList<String> sList;   // string data for outfile

    private int nsurfs, nprev, ngroups, nrays;
    private int prevGroups[] = new int[MAXSURFS+1];
    
    //------plot box definition-------
    
    final double EXTRAROOM = 1.5;  // window/plotbox
    final double BADCELL   = -1.0; // inadequate ray count
    
    //------data from user options------
    
    private int     mapType=-1; 
    private String  sType[] = {"rmsWFE", "pvWFE", "rmsPSF", "rssPSF"}; 
    private String  sHvar,  sVvar;
    private double  dHstep, dVstep;
    private int     nH,     nV;
    private int     maxBunches; 
    private int     goodBunches; 
    private double  dHcen,  dVcen; 
    private double  dHmin, dHmax, dVmin, dVmax; // ticks: July 2012
    private String  sHpar,  sVpar;  // parallax
    private double  dHpar,  dVpar;  // parallax
    private int     maxVig;         // percent
    private int     minGood;        // = nRays*(1-0.01*maxVig) or 2
    private double  aspect;
    private boolean bCSV; 
    private String  sOutfile; 
    private boolean bOutfile; 
    
    //-----derived AttribSurf codes from user options---
    
    private int asH[] =  {-1, -1};
    private int asV[] =  {-1, -1};
    private int asHp[] = {-1, -1};
    private int asVp[] = {-1, -1}; 

    //---the center of the map--------------

    private double dHmapCenter, dVmapCenter;
    private double maxdatum=0, mindatum=0; 
    
    //---optics and ray saves------------
    
    private double hRaysave[] = new double[MAXRAYS]; 
    private double hOptsave;
    private double vRaysave[] = new double[MAXRAYS];
    private double vOptsave; 
    private double hRaysaveP[] = new double[MAXRAYS]; 
    private double vRaysaveP[] = new double[MAXRAYS]; 

    //---nominal typeface but doArt() can modify these---------

    int iFontcode = 120000;
    int iHpoints = 12; 
    int iWpoints = 7; 
    double EXTRA = 2.0; 

    
    public MapPanel(GJIF gj) 
    {
        myGJIF = gj;  
        bClobber = true; 
        uxcenter = 0.0;         // unit square; for GPanel's addAffines.
        uxspan   = EXTRA;       // unit square
        uycenter = 0.0;         // unit square
        uyspan   = EXTRA;       // unit square
        bPleaseParseUO = true;  // allows total restart.
    }
    
    //-----------protected methods----------------

    protected void doTechList(boolean bFullArt) // replaces abstract method
    // Called by GPanel for artwork: new, pan, zoom, & random ray group.
    // THIS RESPONDS TO THE OS CALL TO GPanel::paintComponent()
    // It gets called NxM times to make a complete Map.
    // Do not request repaint() here: this is a provider not a requestor. 
    {
        nsurfs = DMF.giFlags[ONSURFS]; 
        ngroups = DMF.giFlags[ONGROUPS];
        nrays = DMF.giFlags[RNRAYS];  
        
        if (bPleaseParseUO)
          startMap();
        else
          doArt(); 
    }

    
    protected void doRotate(int i, int j) // replaces abstract "do" method
    {
        return; 
    }

    protected boolean doRandomRay() // replaces abstract "do" method
    {
        return true; 
    } 
    
    protected void doCursor(int ix, int iy)  // replaces abstract method
    {
        return; 
    }

    protected double getStereo()    // replaces abstract "get" method
    {
        return 0.0; 
    }

    protected void doSaveData()     // replaces abstract "do" method
    {
        return; 
    } 



//----------private methods-----------------

    private void startMap()
    // Called only by doTechList().
    {
        String warn = doParseUO();         
        displayMessage(warn);
        if (warn.length() > 0)
        {
            myGJIF.postWarning(warn); 
            repaint(); // need to kick off warning artwork?
            return;
        }
        bPleaseParseUO = false; 
        goodBunches = 0; 
        dList = new ArrayList<Double>(); // zero data.        
        startBunches(); // starts the timing loop
    }
    
    
    private String doParseUO() 
    // This is the User Options parser. 
    // Call this whenever user options change; gives warning or "".
    // First line of defense! must never crash. 
    {
        nsurfs = DMF.giFlags[ONSURFS]; 
        ngroups = DMF.giFlags[ONGROUPS]; 
        nrays = DMF.giFlags[RNRAYS]; 
        if (nsurfs<1) 
          return "No surfaces are defined"; 
        if (nrays<2)
          return "Fewer than two rays are defined"; 
          
        //---find out which mapType radio button is true-------

        mapType = -1; 
        for (int i=0; i<4; i++)  // buttons 0, 1, 2, 3
          if ("T".equals(DMF.reg.getuo(UO_MAP, i)))
            mapType = i; 
        if (mapType < 0)
          return "No map type selected"; 
          

        sHvar  = DMF.reg.getuo(UO_MAP, 4).trim(); 
        dHstep = U.suckDouble(DMF.reg.getuo(UO_MAP,5)); 
        nH     = U.minmax(U.suckInt(DMF.reg.getuo(UO_MAP,6)), 2, MAXMAP); 
        dHcen  = U.suckDouble(DMF.reg.getuo(UO_MAP,8));
        sHpar  = DMF.reg.getuo(UO_MAP,9).trim(); 
        dHpar  = U.suckDouble(DMF.reg.getuo(UO_MAP,10));  // parallax
        
        sVvar  = DMF.reg.getuo(UO_MAP, 11).trim(); 
        dVstep = U.suckDouble(DMF.reg.getuo(UO_MAP,12)); 
        nV     = U.minmax(U.suckInt(DMF.reg.getuo(UO_MAP,13)), 2, MAXMAP); 
        dVcen  = U.suckDouble(DMF.reg.getuo(UO_MAP,15));
        sVpar  = DMF.reg.getuo(UO_MAP,16).trim(); 
        dVpar  = U.suckDouble(DMF.reg.getuo(UO_MAP,17));  // parallax
        maxVig = U.suckInt(DMF.reg.getuo(UO_MAP,18)); 
        maxVig = U.minmax(maxVig, 0, 99); 
        minGood = (int) Math.round(nrays*(1-0.01*maxVig));
        minGood = U.minmax(minGood, 2, nrays); 
        aspect = U.suckDouble(DMF.reg.getuo(UO_MAP,19)); // usually 1.0
        aspect = Math.min(4.0, Math.max(0.5, aspect));   // usually 1.0

        maxBunches = nH*nV; 
        sOutfile = DMF.reg.getuo(UO_MAP, 20).trim(); 
        bOutfile = sOutfile.length() > 0; 
        if (bOutfile)
        {
            String temp = sOutfile.toUpperCase(); 
            bCSV = temp.endsWith(".CSV");
        }


        //----test the map parameters for validity----
        
        boolean ok = parseOneStepVar(sHvar, asH, ngroups); 
        if (!ok)
          return "Hvar unknown"; 
        ok = parseOneStepVar(sVvar, asV, ngroups); 
        if (!ok)
          return "Vvar unknown"; 
        
        //----set up the min & max user parms----------
        
        dHmin = dHcen - 0.5*(nH-1)*dHstep; 
        dHmax = dHcen + 0.5*(nH-1)*dHstep; 
        dVmin = dVcen - 0.5*(nV-1)*dVstep; 
        dVmax = dVcen + 0.5*(nV-1)*dVstep;

        //---test for possible parallax-------
        
        if (sHpar.length() > 0)
        {
            ok = parseOneStepVar(sHpar, asHp, ngroups); 
            if (!ok)
              return "Hparallax is unknown";
        }
        if (sVpar.length() > 0)
        {
            ok = parseOneStepVar(sVpar, asVp, ngroups); 
            if (!ok)
              return "Vparallax is unknown"; 
        }
        
        //---------good parse!--------------
        if (bOutfile)
        {
            sList = new ArrayList<String>(); 
            String s = "  "+sVvar+",   "+sHvar+",  Ngood,   Xf,   Yf,   Zf,  "+sType[mapType]; 
            // sVvar, sHvar reversed 16 Feb 2015
            sList.add(s); 
        }

        return ""; 
            
    } // end of doParseUO()


    private static boolean parseOneStepVar(String s, int ij[], int ngroups)
    // Parses one candidate StepVar specification.
    // Returns ij[0] = iattribute and ij[1] = jsurf;
    //
    // jsurf<0 means fail.
    // jsurf=0 means rayStart.
    // jsurf>0 means Optics table surface parameter. 
    //
    // Return value is true if parse is OK, else false. 
    //
    // Valid string must have two or more chars. 
    // Method: if 2nd char is zero, definitely RayStart;
    //  else definitely optics. 
    // Call at run time only because this routine uses ngroups. 
    {
        String zeros = "0oO";                    // equivalents to zero
        String nums = "123456789F";              // valid numeric keys

        s = s.toUpperCase().trim();              // always OK for StepVars.
        if (s.length() < 2)
        {
            ij[1] = -1; 
            return false;
        }
        char ch0 = U.getCharAt(s, 0); 
        char ch1 = U.getCharAt(s, 1); 
        if (zeros.indexOf(ch1) >= 0)             // raystart
        {
            ij[1] = 0;   
            ij[0] = REJIF.getCombinedRayFieldOp(s) % 100; 
 
            if ((ij[0]<RX) || (ij[0]>RW))  
              ij[1] = -1;                        // declare failure. 
            if (RAYSTARTCHARS.indexOf(ch0) < 0)  // another validity check.
              ij[1] = -1;                        // declare failure. 
            return (ij[1] == 0);                 // return status: 0=good.
        }
        if (nums.indexOf(ch1) >= 0)              // optics?
        {
            ij[0] = OEJIF.getOptFieldAttrib(s); 
 
            if (ch1 == 'F')
              ij[1] = ngroups;                   // ngroups is valid here. 
            else
              ij[1] = U.suckInt(s);              // surface number
            if (SURFCHARS.indexOf(ch0) < 0)      // unsupported attribute key
              ij[1] = -1;                        // declare failure. 
            if ((ij[1]<1) || (ij[1]>99))         // invalid surface
              ij[1] = -1;                        // declare failure.
            return (ij[1] > 0);                  // return status. 
        }
        ij[1] = -1;                              // declare failure.
        return false;                            // return status. 
    }


    private void getMapCenters()
    // Called by doArt().
    // Evaluates dHmapCenter and dVmapCenter
    {
        dHmapCenter = dHcen;              // parsed UO value
        if (U.isNegZero(dHmapCenter))     // absent?
          if (asH[1] == 0)                // use ray start
            dHmapCenter = getRaystartMidValue(asH[0]); 
          else                            // use optics
            dHmapCenter = getOpticsNominalValue(asH[0], asH[1]); 
            
        dVmapCenter = dVcen;              // parsed UO value
        if (U.isNegZero(dVmapCenter))     // absent?
          if (asV[1] == 0)                // use ray start
            dVmapCenter = getRaystartMidValue(asV[0]); 
          else                            // use optics
            dVmapCenter = getOpticsNominalValue(asV[0], asV[1]); 
    }


    private double getRaystartMidValue(int iatt)
    // Called by getMapCenters()
    // Returns midrange of given raystart coordinate. 
    {
        if (nrays < 1)
          return -0.0; 
        // tabulated raystarts begin at 1 not zero:
        double valmin = RT13.raystarts[1][iatt];  
        double valmax = RT13.raystarts[1][iatt]; 
        for (int k=2; k<=nrays; k++)
        {
            if (RT13.raystarts[k][iatt] < valmin)
              valmin = RT13.raystarts[k][iatt]; 
            if (RT13.raystarts[k][iatt] > valmax)
              valmax = RT13.raystarts[k][iatt]; 
        }
        return 0.5 * (valmin + valmax); 
    }
    
    
    private double getOpticsNominalValue(int iatt, int jsurf)
    // Called by getMapCenters().
    // Returns nominal value of a given surface parameter
    {
        if (jsurf > nsurfs)
          return -0.0; 
        return RT13.dGetSurf(iatt, jsurf); 
    }



    public void doBunchRays(int gBunch)
    // This is the central workhorse of MapPanel. 
    // The timing driver calls this to request each new bunch of rays.
    // Evaluates Ngood and one new data point.
    // Updates dList with ray trace result for ih,iv.
    // RAYS ONLY; DOES NOT CALL ANY DISPLAY SOFTWARE. 
    // The timing driver will call GPanel.redo() after each pass here. 
    {   
        //-----initialize the optical relocation deltas---
        
        int sofar = dList.size(); // the next cell to evaluate
        int ih = sofar % nH; 
        int iv = sofar / nH; 
        
        double dH1 = (-0.5*(nH-1) + ih) * dHstep;  // param value
        double dH2 = (-0.5*(nH-1) + ih) * dHpar;   // parallax
        double dV1 = (-0.5*(nV-1) + iv) * dVstep;  // param value
        double dV2 = (-0.5*(nV-1) + iv) * dVpar;   // parallax

        //------save original values of stepped parms--------
        //------no parallax for optics, only for rays--------
        
        if (asH[1] == 0)       // if stepping raystarts: save.
          for (int k=1; k<=nrays; k++)
            hRaysave[k] = RT13.raystarts[k][asH[0]];
        if (asHp[1] == 0)
          for (int k=1; k<=nrays; k++)
            hRaysaveP[k] = RT13.raystarts[k][asHp[0]];

        if (asH[1] > 0)        // if stepping optics; save.
          hOptsave = RT13.surfs[asH[1]][asH[0]]; 

        if (asV[1] == 0)       // if stepping raystarts; save.
          for (int k=1; k<=nrays; k++)
            vRaysave[k] = RT13.raystarts[k][asV[0]]; 
        if (asVp[1] == 0)
          for (int k=1; k<=nrays; k++)
            vRaysaveP[k] = RT13.raystarts[k][asVp[0]];  
       
        if (asV[1] > 0)        // if stepping optics; save.
          vOptsave = RT13.surfs[asV[1]][asV[0]]; 

        //----------------step the parameters-----------------------
        //--but respect the optional user-specified center values---

        if (asH[1] == 0)       // modify rays
          for (int k=1; k<=nrays; k++)
            RT13.raystarts[k][asH[0]] = hRaysave[k] + dH1; 
        if (asHp[1] == 0)
          for (int k=1; k<=nrays; k++)
            RT13.raystarts[k][asHp[0]] = hRaysaveP[k] + dH2; 

        if (asH[1] > 0)        // else modify optics
          RT13.surfs[asH[1]][asH[0]] = hOptsave + dH1; 

        if (asV[1] == 0)       // modify rays
          for (int k=1; k<=nrays; k++)
            RT13.raystarts[k][asV[0]] = vRaysave[k] + dV1; 
        if (asVp[1] == 0) 
          for (int k=1; k<=nrays; k++)
            RT13.raystarts[k][asVp[0]] = vRaysaveP[k] + dV2;

        if (asV[1] > 0)        // else modify optics
          RT13.surfs[asV[1]][asV[0]] = vOptsave + dV1; 
          
        RT13.setEulers();       // meat ax approach
        
        //---trace the rays and gather the datum for this box--------
        //---Note: REJIF line 88 sets each ray WFE group index=0
        //---prior to running a trace for WFE evaluation. 
            
        for (int kray=0; kray<=nrays; kray++)
          RT13.iWFEgroup[kray] = 0;   // all rays are in WFEgroup zero
        
        double d = -0.0; 
        int ngood = RT13.iBuildRays(true);  // builds all rays
        if (ngood < minGood)
          d = BADCELL; 
        else 
        {
            goodBunches++; 
            switch(mapType)
            {
               case 0:   d = getRmsWFE(); break; 
               case 1:   d = getPvWFE(); break; 
               case 2:   d = 0.707107*getRssPSF(); break; 
               case 3:   d = getRssPSF(); break; 
               default:  d = BADCELL; 
            }
            if (bOutfile)
            {
                char c = bCSV ? ',' : ' ';
                String s = U.fwd(dV1,16,9)+c     // reversed 16 Feb 2015
                  +U.fwd(dH1,16,9)+c             // reversed 16 Feb 2015
                  +U.fwi(ngood,9)+c
                  +U.fwd(getAverage(RX),16,6)+c  // centroid Xfinal
                  +U.fwd(getAverage(RY),16,6)+c  // centroid Yfinal
                  +U.fwd(getAverage(RZ),16,6)+c  // centroid Zfinal
                  +U.fwd(d,16,9);                // selected metric
                sList.add(s); 
            }
        }
        dList.add(new Double(d)); ////// this is it!
        
        //----finally, undo the parameter step-------

        if (asH[1] == 0)        // restore rays
          for (int k=1; k<=nrays; k++)
            RT13.raystarts[k][asH[0]] = hRaysave[k];
        if (asHp[1] == 0)    
          for (int k=1; k<=nrays; k++)
            RT13.raystarts[k][asHp[0]] = hRaysaveP[k]; //????
        if (asH[1] > 0)         // else restore optics
          RT13.surfs[asH[1]][asH[0]] = hOptsave; 

        if (asV[1] == 0)        // restore rays
          for (int k=1; k<=nrays; k++)
            RT13.raystarts[k][asV[0]] = vRaysave[k]; //????
        if (asVp[1] == 0)    
          for (int k=1; k<=nrays; k++)
            RT13.raystarts[k][asVp[0]] = vRaysaveP[k]; 

        if (asV[1] > 0)         // else restore optics
          RT13.surfs[asV[1]][asV[0]] = vOptsave; 
          
        RT13.setEulers();       // meat ax approach
    } 

    
    //------Quantitative methods serving doBunchRays()-----

    private double getAverage(int iatt)
    {
        double sum=0; 
        int ngood=0; 
        for (int k=1; k<=nrays; k++)
          if (RROK == RT13.getStatus(k))
          {
              sum += RT13.dGetRayFinal(k,iatt); 
              ngood++; 
          }
        if (ngood > 0)
          return sum/ngood;
        return 0;
    }
  
  
    private double getRmsWFE()
    {
        double sum=0.0, sum2=0.0; 
        int n=0; 
        for (int k=1; k<=nrays; k++)
          if (RROK == RT13.getStatus(k))
          {
              double wfe = RT13.dGetRay(k, 0, RTWFE); 
              sum += wfe;
              sum2 += wfe*wfe; 
              n++; 
          }
        if (n < minGood)
          return BADCELL; 
        double var = sum2/(n-1) - sum*sum/(n*(n-1)); 
        double rms = Math.sqrt(var); 
        return rms;
    }

    private double getPvWFE()
    {
        double peak=0, valley=0; 
        int n=0; 
        for (int k=1; k<=nrays; k++)
          if (RROK == RT13.getStatus(k))
          {
              double wfe = RT13.dGetRay(k, 0, RTWFE); // group zero
              if (n == 0)
                peak = valley = wfe;
              else if (wfe > peak)
                peak = wfe;
              else if (wfe < valley)
                valley = wfe; 
              n++; 
          }
        if (n < minGood)
          return BADCELL; 
        return peak - valley; 
    }
              
    private double getRssPSF()
    // This one is 2D, always uses xfinal and yfinal.
    // But alternative 1D X and Y options exist too.
    // Also for some purposes could want MaxRadius, PVX, and PVY. 
    // That's six flavors right there!
    // Moreover: PSF size vs PSF error from goal location?
    {
        double xsum=0.0, xsum2=0.0, ysum=0.0, ysum2=0.0;  
        // RFINAL in B4Constants = 10000; placeholder for 100*nsurfs term.
        int opxf  = RFINAL + RTXL;  // opcode for xfinal
        int opyf  = RFINAL + RTYL;  // opcode for yfinal
        int xsurf = RT13.getGroupNum(opxf); // handles "final" surface 
        int ysurf = RT13.getGroupNum(opyf); // handles "final" surface
        if ((xsurf < 1) || (ysurf < 1))
          return BADCELL; 
        int n = 0; 
        for (int k=1; k<=nrays; k++)
          if (RROK == RT13.getStatus(k))
          {
             double x = RT13.dGetRay(k, xsurf, RTXL); // ignore goals
             double y = RT13.dGetRay(k, ysurf, RTYL); // ignore goals
             xsum += x; 
             ysum += y; 
             xsum2 += x*x;
             ysum2 += y*y;
             n++; 
          }
        if (n < minGood)
          return BADCELL; 
        double varx = xsum2/(n-1) - xsum*xsum/(n*(n-1)); 
        double vary = ysum2/(n-1) - ysum*ysum/(n*(n-1)); 
        double rms = Math.sqrt(varx + vary); 
        return rms;
    }


    private void gatherMinMax()
    // Call this after all ray traces, before artwork.
    // Many or all dList members might be BADCELL.
    {
        int sofar = dList.size(); 
        int ngood = 0; 
        for (int k=0; k<sofar; k++)
        {
            double d = dList.get(k).doubleValue(); 
            if (d > 0) 
            {  
                if (ngood==0)
                   maxdatum = mindatum = d;
                maxdatum = Math.max(maxdatum, d); 
                mindatum = Math.min(mindatum, d); 
                ngood++; 
            }
        }

        if (mindatum == maxdatum)
        {
            mindatum=-0.0; 
            maxdatum=+1.0;
        }
    }

    private void doFinishFile()
    {
        if (bOutfile && (sList != null) && (sList.size() > 0))
        {
            File file = new File(sOutfile); 
            try
            {
                FileWriter fw = new FileWriter(file); 
                PrintWriter pw = new PrintWriter(fw, true);
                for (int i=0; i<sList.size(); i++)
                  pw.println(sList.get(i)); 
                pw.flush();
                pw.close(); 
            }
            catch (IOException e)
            { }
        }
    }






    //---------ARTWORK BEGINS HERE----------------
    //---------ARTWORK BEGINS HERE----------------
    //---------ARTWORK BEGINS HERE----------------
    
    
    private void add2D(double x, double y, int op)  // local shorthand
    {
        addScaled(x, y, 0.0, op, QBASE);  // GPanel service
    }
    
    

    private void doArt()
    // This gets called many times, showing intermediate maps..... 
    // doTechList calls this plot whenever a PARTIAL MAP is available
    {
        iFontcode = getUOGraphicsFontCode();  
        iHpoints = iFontcode / 10000;     
        iWpoints = 1 + iHpoints / 2;   
        
        // scaledW is charWidth in user units, for spacing. 
        double scaledW = iWpoints * uxspan / dUOpixels; 
        
        getMapCenters(); 
        gatherMinMax(); 
        
        //---start the drawing, explicit new way----------
        
        clearList(QBASE); 
        addRaw(0., 0., 0., SETWHITEBKG, QBASE);      // unscaled
        addRaw(0., 0., 0., SETCOLOR+BLACK, QBASE);   // unscaled
        addRaw(1., 0., 0., SETSOLIDLINE, QBASE);     // unscaled
        addRaw(0., 0., 0., COMMENTRULER, QBASE);     // unscaled
                
        //-----draw the currently completed color boxes-------------

        double dx     = aspect/nH; 
        double dy     = 1.0/nV; 
        double dxe    = 1.01*dx;        // eliminate white gaps
        double dye    = 1.01*dy;        // eliminate white gaps
        double xbase  = +0.5 - aspect;  // typically -0.5; 
        double ybase  = -0.5; 
        int sofar = dList.size(); 
        for (int k=0; k<sofar; k++)
        {
            int i = k % nH; 
            int j = k / nH; 
            double x = xbase + i*dx; 
            double y = ybase + j*dy;

            double d = dList.get(k).doubleValue(); 
            if (maxdatum > mindatum)
              d = (d - mindatum)/(maxdatum - mindatum); 
            else
              d = BADCELL;  // out of range forces gray.

            //---draw and fill each rectangle------
                            
            if ((d >= 0.0) && (d <= 1.0))  
              addRaw(U.getRed(d), U.getGreen(d), U.getBlue(d), SETRGB, QBASE); 
            else                         
              addRaw(0., 0., 0., SETCOLOR+LTGRAY, QBASE);   
              
            add2D(x, y, MOVETO); 
            add2D(x+dxe, y, PATHTO); 
            add2D(x+dxe, y+dye, PATHTO); 
            add2D(x, y+dye, PATHTO); 
            add2D(x, y, FILL); 
        }
  
        //-----display the stepped parameter names------
        
        addRaw(0., 0., 0., SETCOLOR+BLACK, QBASE);   // unscaled
        for (int k=0; k<sHvar.length(); k++)
        {
            int ic = (int) sHvar.charAt(k) + iFontcode; 
            double x = 0.5 - 0.5*aspect + scaledW * k; 
            double y = ybase - 0.08; 
            add2D(x, y, ic); 
        }
        
        for (int k=0; k<sVvar.length(); k++)
        {
            int ic = (int) sVvar.charAt(k) + iFontcode; 
            double x = xbase -0.08 + scaledW*(k - sVvar.length()); 
            double y = 0.0; 
            add2D(x, y, ic); 
        }

        //--draw the tick marks & user variable labels------------

        double xLeft  = xbase + 0.5*dx; 
        double xRight = xbase + (nH-0.5)*dx; 
        double yBot   = ybase + 0.5*dy; 
        double yTop   = ybase + (nV-0.5)*dy; 

        add2D(xLeft,  ybase-0.01, MOVETO); 
        add2D(xLeft,  ybase-0.05, STROKE); 
        add2D(xRight, ybase-0.01, MOVETO); 
        add2D(xRight, ybase-0.05, STROKE); 
        add2D(xbase-0.01, yBot, MOVETO); 
        add2D(xbase-0.05, yBot, STROKE); 
        add2D(xbase-0.01, yTop, MOVETO); 
        add2D(xbase-0.05, yTop, STROKE); 

        String sHmin = U.gd(dHmin);  // U.fwe(dHmin); 
        int    nHmin = sHmin.length(); 
        String sHmax = U.gd(dHmax);  // U.fwe(dHmax); 
        int    nHmax = sHmax.length(); 
        String sVmin = U.gd(dVmin);  // U.fwe(dVmin); 
        int    nVmin = sVmin.length(); 
        String sVmax = U.gd(dVmax);  // U.fwe(dVmax) ;  
        int    nVmax = sVmax.length();    
        for (int k=0; k<nHmin; k++)  // lower left centered
        {
            int ic = (int) sHmin.charAt(k) + iFontcode; 
            double x = xLeft + scaledW * (k-nHmin/2); 
            double y = ybase - 0.08; 
            add2D(x, y, ic); 
        }
        for (int k=0; k<nHmax; k++)  // lower right centered
        {
            int ic = (int) sHmax.charAt(k) + iFontcode; 
            double x = xRight + scaledW * (k-nHmax/2); 
            double y = ybase - 0.08; 
            add2D(x, y, ic); 
        }
        for (int k=0; k<nVmin; k++)  // lower left right justified
        {
            int ic = (int) sVmin.charAt(k) + iFontcode; 
            double x = xbase -0.05 + scaledW * (k-nVmin+1); 
            double y = yBot; 
            add2D(x, y, ic); 
        }
        for (int k=0; k<nVmax; k++)  // upper left right justified
        {
            int ic = (int) sVmax.charAt(k) + iFontcode; 
            double x = xbase -0.08 + scaledW * (k-nVmax+1); 
            double y = yTop; 
            add2D(x, y, ic); 
        }


        //---FINALLY draw the thermometer if data & span are ok--------
        //----DANGER--- here we redefine dx, dy-------------------
 
        if ((goodBunches<2) || (U.isNegZero(mindatum)) || (maxdatum <= mindatum))
        {
            String warn = "Ngood < 2: vignetting?"; 
            myGJIF.postWarning(warn); 
            return;
        }
        else
        {   
            myGJIF.postWarning(""); 
        }
          
        addRaw(0., 0., 0., COMMENTRULER, QBASE);       
        int nthermo = 100; 
        double xright = +0.6; 
        dy = 1.0/nthermo; 
        dye = 1.01*dy;    // eliminate white gaps
        dx = 0.06; 

        for (int i=0; i<nthermo; i++)
        {
            double z = i*dy; 
            double y = ybase + z; 
            addRaw(U.getRed(z), U.getGreen(z), U.getBlue(z), SETRGB, QBASE);
            add2D(xright,     y,     MOVETO); 
            add2D(xright+dx,  y,     PATHTO); 
            add2D(xright+dx,  y+dye, PATHTO); 
            add2D(xright,     y+dye, PATHTO); 
            add2D(xright,     y,     FILL); 
        }

        //----add labels to the thermometer----


        addRaw(0., 0., 0., SETCOLOR+BLACK, QBASE); 

        String s[] = new String[3]; 
        s[0] = U.fwe(mindatum); 
        s[2] = U.fwe(maxdatum); 
        s[1] = U.fwe(0.5*(mindatum + maxdatum)); 
        for (int i=0; i<=2; i++)
          for (int k=0; k<s[i].length(); k++)   // left justified
          {
              int ic = (int) s[i].charAt(k) + iFontcode; 
              double x = xright + dx + scaledW*k;    
              double y = ybase + 0.5*i; 
              add2D(x, y, ic); 
          }
    } //-----------end of doArt()-------------------------





    private void displayMessage(String s)
    // Posts a message on the MapPanel screen
    {
        iFontcode = getUOGraphicsFontCode();  
        iHpoints = iFontcode / 10000;     
        iWpoints = 1 + iHpoints / 2;   
        
        // scaledW is charWidth in user units, for spacing. 
        double scaledW = iWpoints * uxspan / dUOpixels; 
        
        //----the drawing-----------

        clearList(QBASE);     // always clear initial quadlist
        addRaw(0., 0., 0., SETWHITEBKG, QBASE);
        addRaw(0., 0., 0., SETCOLOR+BLACK, QBASE);         
        for (int k=0; k<s.length(); k++)
        {
            int ic = (int) s.charAt(k) + iFontcode; 
            double x = scaledW*k; 
            double y = 0.0; 
            add2D(x, y, ic); 
        }
    }




    //---TIMING LOOP CODE STARTS HERE------------------
    //     Interfaces are.....
    //     doBunchRays() above.
    //     redo()      in GPanel; with bClobber=true, calls doArt() here.
    //     doFinishFile()  is called at end of timer run here. 

    private javax.swing.Timer myTimer; 
    private int goodcount=0;
    private int iBunch=0; 
    private boolean bRunning = true; 

    private void startBunches()
    {
        nrays = DMF.giFlags[RNRAYS]; 
        goodcount = 0; 
        iBunch = 0; 
        myTimer = new javax.swing.Timer(20, doTick); 
        bRunning = true; 
        myTimer.start(); 
    }

    ActionListener doTick = new ActionListener()
    {
        public void actionPerformed(ActionEvent ae)
        {
            if (bRunning)
            {
                doBunchRays(iBunch);
                iBunch++; 
                if (iBunch >= maxBunches)
                  bRunning = false; 
                redo();  // GPanel: myBatchList -> g2Tech, and blit.
            }
            else
            {
                myTimer.stop(); 
                doFinishFile(); 
            }
        } 
    };

}  //----------end of MapPanel--------------------
