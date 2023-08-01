package com.stellarsoftware.beam;

import javax.swing.*;      // Graphics2D features
import java.util.*;        // ArrayList

@SuppressWarnings("serial")

/** MultiPlot Artwork Generator
  * A207: eliminating groups
  * A173: Adopting five GPanel quadLists and GPanel helper methods.
  *
  * A150: uses new Options dialog with explicit lists of variable values;
  *   allows wavelength & color to be linked to U0 or V0.
  *   Uses a new feature: RT13.gwave permits commandeering wavelength.
  *   Color is local, so no commandeering needed on that score. 
  *
  *
  * Upgraded to kcolor[][][] for both box coords and ray number, April 2013.
  * kcolor[][][] is now initialized to ABSENT=-1
  *    here... line 355,408: kcolor[] is taken from Options color parser. 
  *    now.... line 645.... if (kcolor==ABSENT) then use RayStart color. 
  *    
  *
  * A150 planned improvement: set up a separate timer thread and
  *   display 1box, 2boxes, 3boxes... at each step modifying scales,
  *   until the whole pattern is done, then kill the timer.   
  *
  *
  * A145: Show boxes only out to radius _____.
  *
  * A136custom: reduced the title to <rms> only, line 735, for 1 column display
  * also the decoration numbers are MICRONS not user mm.
  *
  * A134: Random Ray feature is under construction but its menu is grayed.
  *
  * Previous version did not stash all result points
  *   and so could not show dynamic update with scaling;
  *   it had to do all boxes first with a black screen and
  *   then suddenly display the finished product.
  *
  * An improved scheme would be to stash all the result points
  *   and recompute the display scale, box by box, and show
  *   the partial computations: keep the user in the loop. 
  * 
  * Nice feature would be auto regenerate for new options and
  *   for changes to .OPT or .RAY.  
  *   Problem: how to detect if .OPT or .RAY has changed.
  *   Presently is spotty: OPT changes are detected (how?)
  *   and RAY changes in X0, Y0, @wave are detected (how?)
  *   yet RAY changes in U0, V0 are ignored (why?)
  *   Fix is to move the initialization out of the constructor
  *   and put it into doTechList. Works for XYZ; fails for P,T 22 Feb 2011.
  *   (That was due to a lack of Euler matrix updating.)
  *
  *   When fixed, please extend this feature to MapPanel.  DONE. 
  *
  * MultiPlot artwork class extends GPanel, supplies artwork.
  * Makes multiple plots of good table rays. 
  * No random ray capability.  Yet.
  * Can step through raystart params or optics params.
  * Suffix zero: raystart.  suffix 1...ngroups: optics.
  *
  *  A106: remote pupil feature. 
  *  A106: enlarged group of displayable box data.
  *
  *  A106: plot2, mplot, plot3 all need a way to 
  * exit gracefully from case of failed Parse() with clear
  * error dialog and no plot.  Have a flag bParseOK.
  *
  * @author M.Lampton (c) STELLAR SOFTWARE 2007-2015 all rights reserved.
  */
public class MPlotPanel extends GPanel
{
    // public static final long serialVersionUID = 42L;

    private int iSymbol = DOT; 
    final static double EXTRAROOM = 1.2;   // around the plot
    final static double EXTRASPAN = 1.1;   // within each plotbox when automatic

    private int nsurfs, nprev, ngroups, nrays;
    private int prevGroups[] = new int[MAXSURFS+1];
    
    private double hspan=0, vspan=0;
    private double gridstep=1, halfbox=1;
    
    //---------parsed information about the plot grid inputs-----------

    private int nHsteps, nVsteps;            // parsed from options
    
    private String sParmH[] = new String[3]; // unnecessary local copy stepped parms
    private String sParmV[] = new String[3]; // unnecessary local copy stepped parms
    private String sDataH[] = new String[3]; // unnecessary local copy data value
    private String sDataV[] = new String[3]; // unnecessary local copy data value

    private static int[][] ijH = new int[2][3]; // 0=attrib & 1=surface each of 3 parms
    private static int[][] ijV = new int[2][3]; // 0=attrib & 1=surface each of 3 parms

    private String sFragH[][] = new String[3][MAXMP]; // fragments of sFragH.
    private double dFragH[][] = new double[3][MAXMP]; // corresponding doubles.
    private String sFragV[][] = new String[3][MAXMP]; // fragments of sFragV. 
    private double dFragV[][] = new double[3][MAXMP]; // corresponding doubles.

    private int nfragsH[] = new int[3]; 
    private int nfragsV[] = new int[3]; 

    private int nwaves    = 0;                        // how many user specified wavels. 

    private int waveField[] = new int[MAXMP];         // column in RT13.media[][]; like gR2M[]
    private boolean bAllWavesNumeric = false;         // allows ops without MEDIA.

    //-----parsed information about the plot grid outputs------------

    private String sRayH, sRayV;    // output ray coordinates

    private double uoHspan=0;
    private double uoVspan=0; 
    private double uoBoxfrac=0.8; 

    private int opH, opV;           // raycodes for output H and V
    private int hsurf, hattr;       // output variable
    private int vsurf, vattr;       // output variable

    private int kcolor[][][];       // i, j, kray; init ABSENT; set by doParse() or RayStarts.
    private double steps[][][];     // i, j, coord; for annotator.


    private double results[][][][]; // i, j, coord, kray.
    private double centroids[][][]; // i, j, coord. 
    private double rhh[][];         // i, j
    private double rvv[][];         // i, j
    private double rhv[][];         // i, j
    private double rss[][];         // i, j
    private double rms[][];         // i, j
    private double hspans[][];      // i, j
    private double vspans[][];      // i, j

    private int ngood[][];          // i, j




    public MPlotPanel(GJIF gj)
    {
        myGJIF = gj;           // protected; used here & GPanel
        uxspan = 1.0;          // parent GPanel setup
        uxcenter = 0.0;        // parent GPanel setup
        uyspan = 1.0;          // parent GPanel setup
        uycenter = 0.0;        // parent GPanel setup
        bPleaseParseUO = true; // unnecessary here but usual
    }



    protected void doTechList(boolean bFullArt) // replaces abstract method
    // bFullArt flag is ignored here; ALWAYS does full art. 
    // Called by GPanel when fresh artwork is needed.
    // But this is not called simply for annotation mods;
    // for annotation, host's bitmap is blitted instead.
    {    
        nsurfs    = DMF.giFlags[ONSURFS];    // input stuff
        nrays     = DMF.giFlags[RNRAYS]; 
        kcolor    = new int[MAXMP][MAXMP][MAXRAYS];  
        steps     = new double[MAXMP][MAXMP][2]; 

        ngood     = new int[MAXMP][MAXMP];   // output stuff
        results   = new double[MAXMP][MAXMP][2][nrays+1];
        centroids = new double[MAXMP][MAXMP][2]; 
        rhh       = new double[MAXMP][MAXMP];   
        rvv       = new double[MAXMP][MAXMP]; 
        rhv       = new double[MAXMP][MAXMP];  
        rss       = new double[MAXMP][MAXMP];
        rms       = new double[MAXMP][MAXMP]; 
        hspans    = new double[MAXMP][MAXMP]; 
        vspans    = new double[MAXMP][MAXMP]; 

        //----code moved here from constructor----------
        bClobber =false;        // protected; random redo() keeps old artwork
        bPleaseParseUO = true;  // protected; allows initial parse. ???

        doParseUO(); 

        if (bPleaseParseUO) // actually unnecessary to safeguard twirl here
        {
            String warn = doParseUO(); 
            myGJIF.postWarning(warn); 
            if (warn.length() > 0)
              return; 
        }
        bPleaseParseUO = false; 

        doStash();  // line 556 below
     
        for (int ix=0; ix<nHsteps; ix++) 
          for (int iy=0; iy<nVsteps; iy++)
            calcOneBox(ix, iy);  // parse & trace only; no artwork; allows uniform size
       
        doRestore();  // line 567 below

        doArt(); 
    }

    protected void doRotate(int i, int j) // replaces abstract "do" method
    {
        return; 
    }

    protected boolean doRandomRay() // replaces abstract "do" method
    {
        // addRandomRays(); 
        return false;  //true; 
    }

    protected void doCursor(int ix, int iy)  // replaces abstract method
    // posts current cursor.
    {
        myGJIF.setTitle("MultiPlot"); 
    }
    
    protected double getStereo()    // replaces abstract "get" method
    {
        return 0.0; 
    }

    protected void doSaveData()     // replaces abstract "do" method
    {
        return; 
    } 


    //--------private methods---------

    private String doParseUO()
    // Call this whenever user options change; gives warning or "".
    // Static to allow calling from Options without instantiation. 
    // Internal variables here shadow MPLOT's dynamic fields. 
    // However, cannot use ngroups or other dynamic vars here.
    // So this routine tests only initial-letter attributes not numbers. 
    // Empty entries are OK; flag only bad syntax, not empty fields.
    // First line of defense, must never crash. 
    //
    // RayStarts are these:   RX,RY,RZ,RU,RV,RW,RPATH,RSWAVEL,RSCOLOR,RSORDER.
    // Test most raystarts:    (c0up in "XYZUVW") && (c1up=='0') zero or oh
    //     Raystart data are:   short strings that all parse numerically
    // 
    // Test for type = @wavel:  c0up == '@'
    // @wavel data may need to be numerically OK, if grating is present:
    //   if (1==DMF.giFlags[OGRATINGPRESENT])...
    // @wavel data may need to be found among media columns, if .MED is required
    //   if (1==DMF.giFlags[OMEDIANEEDED])...
    //   and we must find the media column number for each @wave value.
    //   
    //    REJIF captures wavenames[] and their existence in .MED is verified in DMF.
    //    MEJIF assembles its refraction-doubles-LUT into RT13.media[irec][mfield].
    //    ...this LUT covers all possible glasses and all possible wavels. 
    //    "Gratings only" without media lookup: require numericals. 
    //    REJIF does this with getFieldDouble() then if (Double.isNaN(t))....
    //    Count up to nHsteps or nVsteps, checking syntax just that far. 
    // Test for type = Color:   (c0up=='C') && (c1up=='O')  oh not zero
    //        Color data are:   short strings, use REJIF.getColorCode(String)
    //        those that fail automatically become black.
    //        Integer color codes are stashed in RT13.raystarts[k][RSCOLOR].
    //
    // Test optical types:      (c0up in "ACIPRTXYZ") && (c1up in "123456789")
    //     Optical data are:    short strings that all parse numerically
    //
    {
        nsurfs = DMF.giFlags[ONSURFS];  
        nrays = DMF.giFlags[RNRAYS]; 

        for (int ix=0; ix<MAXMP; ix++)
          for (int iy=0; iy<MAXMP; iy++)
            for (int k=0; k<MAXRAYS; k++)
              kcolor[ix][iy][k] = ABSENT;    

        for (int i=0; i<=2; i++)
        {
            ijH[0][i] = -1;  /// no attrib yet
            ijH[1][i] = -1;  /// no surface yet
            ijV[0][i] = -1;  /// no attrib yet
            ijV[1][i] = -1;  /// no surface yet
        }

        nHsteps = U.suckInt(DMF.reg.getuo(UO_MPLOT, 0)); 
        nVsteps = U.suckInt(DMF.reg.getuo(UO_MPLOT, 7)); 

        for (int i=0; i<=2; i++)  // unnecessary local copies of major strings
        {
            sParmH[i] = DMF.reg.getuo(UO_MPLOT, 1+2*i).trim(); 
            sDataH[i] = DMF.reg.getuo(UO_MPLOT, 2+2*i).trim(); 
            sParmV[i] = DMF.reg.getuo(UO_MPLOT, 8+2*i).trim(); 
            sDataV[i] = DMF.reg.getuo(UO_MPLOT, 9+2*i).trim(); 
        }

        for (int i=0; i<=2; i++)  // extract the string fragments
        {
            for (int k=0; k<MAXMP; k++)
            {
                sFragH[i][k] = ""; 
                sFragV[i][k] = ""; 
            } 
            ArrayList<String> fraglist = new ArrayList<String>(); 
            nfragsH[i] = U.tokenize(sDataH[i], " ,;", fraglist); 
            for (int k=0; k<nfragsH[i]; k++)
              sFragH[i][k] = fraglist.get(k).trim();

            for (int k=0; k<nfragsH[i]; k++) // interpret them as numeric or NaN.
              dFragH[i][k] = U.suckDouble(sFragH[i][k]); 

            fraglist.clear();  
            nfragsV[i] = U.tokenize(sDataV[i], " ,;", fraglist); 
            for (int k=0; k<nfragsV[i]; k++)
              sFragV[i][k] = fraglist.get(k).trim(); 

            for (int k=0; k<nfragsV[i]; k++) // interpret them as numeric or NaN.
              dFragV[i][k] = U.suckDouble(sFragV[i][k]); 
        }

        //---parse horizontal step inputs------------ 

        for (int i=0; i<=2; i++)    //  first, second, third step items:
        {
            char c0 = U.getCharAt(sParmH[i],0);
            char c1 = U.getCharAt(sParmH[i],1);  
            int ratt = getRayStartAttribute(c0);     // below
            int oatt = getOptAttribute(c0);          // below
            int surf = U.getTwoDigitCode(sParmH[i]); 

            if ((ratt > RABSENT) && (surf < 1))  // valid raystart spec
            {
                ijH[0][i] = ratt;
                ijH[1][i] = 0; // surface=0 will imply raystart

                for (int k=0; k<nfragsH[i]; k++)
                {
                    if (ratt == RSWAVEL)
                    {
                        if (1==DMF.giFlags[OMEDIANEEDED])
                        {
                            String complaint = findMediaWavelength(sFragH[i][k], k); 
                            if (complaint.length() > 0)
                              return complaint; 
                        }
                        if (1==DMF.giFlags[GRNEEDNUMERWAVES])
                          if (Double.isNaN(dFragH[i][k]))
                            return sFragH[i][k] + " is not numerical."; 
                    }
                    else if (ratt == RSCOLOR)  // no failure messages here
                    {
                        int icolor = U.getColorCode(sFragH[i][k]); 
                        for (int m=0; m<nVsteps; m++) 
                          for (int r=0; r<MAXRAYS; r++)  // color all rays in box
                            kcolor[k][m][r] = U.getColorCode(sFragH[i][k]); 
                    }
                    else if (Double.isNaN(dFragH[i][k]))  // numerical datum
                           return sFragH[i][k] + " is not numerical."; 
                } 
            }
            else if ((oatt > 0) && (surf > 0))  // optical surface spec          
            {
                ijH[0][i] = oatt;
                ijH[1][i] = surf;  // surf > 0 will imply surface number
                for (int k=0; k<nfragsH[i]; k++)
                  if (Double.isNaN(dFragH[i][k]))
                    return sFragH[i][k] + " is not numerical."; 
            }
        }

        for (int i=0; i<nHsteps; i++)
          for (int j=0; j<nVsteps; j++) 
            steps[i][j][0] = dFragH[0][i]; // H step values for annotation


        //----parse vertical step inputs-------------

        for (int i=0; i<=2; i++)    //  first, second, third step items:
        {
            char c0 = U.getCharAt(sParmV[i],0);
            char c1 = U.getCharAt(sParmV[i],1);  
            int ratt = getRayStartAttribute(c0);  
            int oatt = getOptAttribute(c0); 
            int surf = U.getTwoDigitCode(sParmV[i]); 

            if ((ratt > RABSENT) && (surf < 1))  // valid raystart spec
            {
                ijV[0][i] = ratt;
                ijV[1][i] = 0; // surface=0 will imply raystart
                for (int k=0; k<nfragsV[i]; k++)
                {
                    if (ratt == RSWAVEL)
                    {
                        if (1==DMF.giFlags[OMEDIANEEDED])
                        {
                            String complaint = findMediaWavelength(sFragV[i][k], k); 
                            if (complaint.length() > 0)
                              return complaint; 
                        }
                        if (1==DMF.giFlags[GRNEEDNUMERWAVES])
                          if (Double.isNaN(dFragV[i][k]))
                            return sFragV[i][k] + " is not numerical."; 
                    }
                    else if (ratt == RSCOLOR)  // no failure messages here
                    {
                        for (int m=0; m<nHsteps; m++) 
                          for (int r=0; r<MAXRAYS; r++)  // color all rays in box
                            kcolor[m][k][r] = U.getColorCode(sFragV[i][k]); 
                    }
                    else if (Double.isNaN(dFragV[i][k]))  // numerical datum
                           return sFragV[i][k] + " is not numerical."; 
                } 
            }
            else if ((oatt > 0) && (surf > 0))  // valid optical surface spec          
            {
                ijV[0][i] = oatt;
                ijV[1][i] = surf;  // surf > 0 will imply surface number
                for (int k=0; k<nfragsV[i]; k++)
                  if (Double.isNaN(dFragV[i][k]))
                    return sFragV[i][k] + " is not numerical."; 
            }
        }

        for (int i=0; i<nHsteps; i++)
          for (int j=0; j<nVsteps; j++) 
            steps[i][j][1] = dFragV[0][j]; // V step values for annotation

        //----- test horizontal output opcode; includes "final"

        sRayH = DMF.reg.getuo(UO_MPLOT, 14).trim(); 
        if (sRayH.length() < 1)
          return "H plotvar is absent."; 
        opH = getRayCombinedOutputOp(sRayH); 
        if (opH == RABSENT)
          return "H plotvar "+sRayH+" is unknown."; 
        hattr = opH%100; 
        hsurf = opH/100; 
        uoHspan = U.suckDouble(DMF.reg.getuo(UO_MPLOT, 15)); 

        //------ test vertical output opcode; includes "final"

        sRayV = DMF.reg.getuo(UO_MPLOT, 16).trim(); 
        if (sRayV.length() < 1)
          return "V plotvar is absent."; 
        opV = getRayCombinedOutputOp(sRayV); 
        if (opV == RABSENT)
          return "V plotvar "+sRayV+" is unknown."; 
        vattr = opV%100; 
        vsurf = opV/100; 
        uoVspan = U.suckDouble(DMF.reg.getuo(UO_MPLOT, 17)); 

        return "";   // no complaint: good parse!

    } //--------end of doParseUO()----------




    //-----------parser helper methods-----------------------

    private String findMediaWavelength(String frag, int k)
    // Fills in waveField[k] column if it can find it in Media.
    // Returns its complaint, or "" if OK.
    {
        if (frag.length()<1)                 
          return "Zero wave length";           // SNH  
        if ((0==DMF.giFlags[MPRESENT]) || (null==DMF.mejif))
          return "Media table is required";    // SNH
        for (int f=1; f<MAXFIELDS; f++)
        {
            if (frag.equals(MEJIF.mwaves[f]))  // Xlint 8 Oct 2014
            {
                waveField[k] = f;   // found it.
                return "";  
            }
        }
        return "Wavelength "+frag+" not found";
    }

    private int getRayStartAttribute(char c)
    //  limited function specific for Multiplot ray starts
    { 
        switch (c = Character.toUpperCase(c))
        {
            case 'X': return RX; 
            case 'Y': return RY; 
            case 'Z': return RZ;
            case 'U': return RU; 
            case 'V': return RV; 
            case 'W': return RW; 
            case '@': return RSWAVEL; 
            case 'C': return RSCOLOR;
        }
        return RABSENT; 
    }

    private int getOptAttribute(char c)
    // limited function specific for MultiPlot surface control
    {
        switch (c = Character.toUpperCase(c))
        {
            case 'X': return OX;
            case 'Y': return OY;
            case 'Z': return OZ;
            case 'T': return OTILT;
            case 'P': return OPITCH;
            case 'R': return OROLL; 
            case 'A': return OASPHER; 
            case 'C': return OCURVE; 
        }
        return OABSENT; 
    } 

    private int getRayCombinedOutputOp(String s)
    // Simplified version of REJIF's, output fields only
    // Returns attrib + 100*jsurface.
    {
        if (s.length() < 1)
          return RABSENT; 
        char c0 = U.getCharAt(s, 0); 
        char c1 = Character.toUpperCase(U.getCharAt(s,1)); 
        int surfcode = 0; 
        if (c1 == 'F')
          surfcode = 100*DMF.giFlags[ONSURFS]; // "final" 
        else
          surfcode = 100*U.getTwoDigitCode(s); 
        if (surfcode < 1)
          return RABSENT; 
        switch (c0)
        {
            case 'X':  return RX+surfcode;
            case 'Y':  return RY+surfcode;
            case 'Z':  return RZ+surfcode;
            case 'U':  return RU+surfcode;
            case 'V':  return RV+surfcode;
            case 'W':  return RW+surfcode;
            case 'P':
            case 'p':  return RPATH+surfcode;
            case 'x':  return RTXL+surfcode;
            case 'y':  return RTYL+surfcode;
            case 'z':  return RTZL+surfcode;
            case 'u':  return RTUL+surfcode;
            case 'v':  return RTVL+surfcode;
            case 'w':  return RTWL+surfcode;
            default:   return RABSENT;
        }
    }


    //-------output methods start here----------------


    private double stashR[][] = null; // copy of RT13.raystarts[][]
    private double stashS[][] = null; // copy of RT13.surfs[][]

    private void doStash()
    // stashes RT13's raystarts & optics 
    {
        stashR = new double[MAXRAYS+1][RNSTARTS];
        for (int j=0; j<MAXRAYS+1; j++)
          System.arraycopy(RT13.raystarts[j], 0, stashR[j], 0, RNSTARTS);  
        stashS = new double[MAXSURFS+1][ONPARMS]; 
        for (int j=0; j<MAXSURFS+1; j++)
          System.arraycopy(RT13.surfs[j], 0, stashS[j], 0, ONPARMS); 
    }

    private void doRestore()
    // copies the local stash back into RT13.
    {
        for (int j=0; j<MAXRAYS+1; j++)
          System.arraycopy(stashR[j], 0, RT13.raystarts[j], 0, RNSTARTS); 
        for (int j=0; j<MAXSURFS+1; j++)
          System.arraycopy(stashS[j], 0, RT13.surfs[j], 0, ONPARMS); 
        RT13.setEulers();  // should be unnecessary
        RT13.gwave = 0;    // was modified by calcOneBox().
    }

    private void setTempParms(int ix, int iy)
    // called by each plotbox to modify its raystarts & optics
    // Wavelengths are commandeered via RT13.gwave = desired media column.
    // Caution: surfs[jsurf][iattrib] is reverse of ijX[0]=attrib, ijX[1]=surf.
    {
        for (int s=0; s<3; s++)        // three specifications per axis
        {
            if (ijH[1][s] == 0)        // surf=0: hence raystart
              if (ijH[0][s] == RSWAVEL)
                RT13.gwave = waveField[ix];
              else
                for (int kray=1; kray<=nrays; kray++)
                  RT13.raystarts[kray][ijH[0][s]] = dFragH[s][ix];

            if (ijH[1][s] > 0)         // surf=1,2,3... hence optical
                RT13.surfs[ijH[1][s]][ijH[0][s]] = dFragH[s][ix];

            if (ijV[1][s] == 0)        // surf=0: hence raystart
              if (ijV[0][s] == RSWAVEL)
                RT13.gwave = waveField[iy]; 
              else
                for (int kray=1; kray<=nrays; kray++)
                  RT13.raystarts[kray][ijV[0][s]] = dFragV[s][iy];

            if (ijV[1][s] > 0)         // surf=1,2,3... hence optical
                RT13.surfs[ijV[1][s]][ijV[0][s]] = dFragV[s][iy];
        }
        RT13.setEulers();  // meat ax
    }

    private void calcOneBox(int ix, int iy)
    // Gathers spot diagram info for box ix,iy.
    // ix=0 is the leftmost plotbox.  iy=0 is the topmost. 
    // This runs only after parsing; ngroups is known to be OK. 
    // No drawing here: this runs BEFORE doArt() does its artwork. 
    // Be sure to doStash() optics & raystarts before deviating.
    // Be sure to doRestore() optics & raystarts after deviating.
    //
    // Warning: this routine sets RT13.gwave to commandeer wavelengths.
    // When done be sure to reset RT13.gwave=0 to restore kray control. 
    {
        nrays = DMF.giFlags[RNRAYS]; 
        
        setTempParms(ix, iy);

        //----trace these table rays--------  

        ngood[ix][iy] = RT13.iBuildRays(true);   // handles UVW normalization
    
        //---now zero and gather the local results----

        hspans[ix][iy] = 0.0; 
        vspans[ix][iy] = 0.0; 
        for (int m=0; m<2; m++)
        {
            centroids[ix][iy][m] = 0.0; 
            for (int k=1; k<=nrays; k++)
              results[ix][iy][m][k] = 0.0; 
        }
        
        // Now pack results[] with good rays ONLY...

        int kcount = 0; 
        for (int k=1; k<=nrays; k++)
          if (RROK == RT13.getStatus(k))
          {
              results[ix][iy][0][kcount] = RT13.dGetRay(k, hsurf, hattr); 
              results[ix][iy][1][kcount] = RT13.dGetRay(k, vsurf, vattr); 
              if (kcolor[ix][iy][k] == ABSENT)                              // ABSENT = -1: not Optioned 
                kcolor[ix][iy][kcount] = (int) RT13.raystarts[k][RSCOLOR];  // kcount not k
              kcount++; 
          }

        //---now get their centers-------------
        
        for (int k=0; k<kcount; k++)
        {
            centroids[ix][iy][0] += results[ix][iy][0][k]; 
            centroids[ix][iy][1] += results[ix][iy][1][k]; 
        }
        if (kcount > 0)
        {
            centroids[ix][iy][0] /= kcount; 
            centroids[ix][iy][1] /= kcount; 
        }

        //---next evaluate the spans and the moments-------
        
        double sumShh = 0.0; 
        double sumSvv = 0.0;
        double sumShv = 0.0; 
        hspans[ix][iy] = 0.0;
        vspans[ix][iy] = 0.0;  
        for (int k=0; k<kcount; k++)
        {
            double dev0 = results[ix][iy][0][k] - centroids[ix][iy][0]; 
            double dev1 = results[ix][iy][1][k] - centroids[ix][iy][1]; 
            sumShh += dev0*dev0; 
            sumSvv += dev1*dev1; 
            sumShv += dev0*dev1; 
            hspans[ix][iy] = Math.max(hspans[ix][iy], 2.0*Math.abs(dev0)); 
            vspans[ix][iy] = Math.max(vspans[ix][iy], 2.0*Math.abs(dev1)); 
        }
        if (kcount > 0)
        {
            rhh[ix][iy] = Math.sqrt(sumShh/kcount); 
            rvv[ix][iy] = Math.sqrt(sumSvv/kcount); 
            if (sumShv > 0.0)
              rhv[ix][iy] = Math.sqrt(sumShv/kcount); 
            else
              rhv[ix][iy] = -Math.sqrt(-sumShv/kcount); 
            rss[ix][iy] = Math.sqrt((sumShh + sumSvv)/kcount); 
            rms[ix][iy] = Math.sqrt((sumShh + sumSvv)/(2*kcount)); 
        }

        //---doRestore() only after all boxes are calculated. 
    } 


    void setHspan()
    // call this only when a population of boxes has been built
    {
        hspan = TOL; 
        if (uoHspan > TOL)
          hspan = uoHspan; 
        else
          for (int i=0; i<nHsteps; i++)
            for (int j=0; j<nVsteps; j++)
              if (hspans[i][j] > hspan)
                hspan = EXTRASPAN*hspans[i][j]; 
    }

    void setVspan()
    {
        vspan = TOL; 
        if (uoVspan > TOL)
          vspan = uoVspan; 
        else
          for (int i=0; i<nHsteps; i++)
            for (int j=0; j<nVsteps; j++)
              if (vspans[i][j] > vspan)
                vspan = EXTRASPAN*vspans[i][j]; 
    }

    void linkSpans()
    { 
        if (U.areSimilar(hattr, vattr))              // same type
          if ((uoHspan < TOL) && (uoVspan < TOL))    // automatic scaling
            hspan = vspan = Math.max(hspan, vspan); 
    } 







    //----------ARTWORK-------------------------------------------
    //----------ARTWORK-------------------------------------------
    //----------ARTWORK-------------------------------------------
    //-------artwork is done after the boxes are computed---------
    
    private void add2D(double x, double y, int op)  // local shorthand
    {
        addScaled(x, y, 0.0, op, QBASE);  // GPanel service
    }
    
        
        
    private void doArt()  
    {
        setHspan();
        setVspan(); 
        linkSpans();

        uoBoxfrac = U.suckDouble(DMF.reg.getuo(UO_MPLOT, 18)); 
        uoBoxfrac = U.minmax(uoBoxfrac, 0.1, 0.99); 

        boolean blackbkg  = "T".equals(DMF.reg.getuo(UO_MPLOT, 29));
        boolean bRound    = "T".equals(DMF.reg.getuo(UO_MPLOT, 30)); 
        boolean bSkip     = "T".equals(DMF.reg.getuo(UO_MPLOT, 31)); 
        boolean bRestrict = "T".equals(DMF.reg.getuo(UO_MPLOT, 32)); 

        clearList(QBASE); 
        int background = blackbkg ? SETBLACKBKG : SETWHITEBKG; 
        int foreground = blackbkg ? WHITE : BLACK; 
        addRaw(0., 0., 0., background, QBASE);            // unscaled
        addRaw(0., 0., 0., SETCOLOR+foreground, QBASE);   // unscaled
        addRaw(1., 0., 0., SETSOLIDLINE, QBASE);          // unscaled
        addRaw(0., 0., 0., COMMENTRULER, QBASE);          // unscaled

        ///----set up the drawing grid centers------------

        gridstep = (Math.min(1.0/nHsteps, 1.0/nVsteps))/EXTRAROOM; 
        halfbox = 0.5*uoBoxfrac*gridstep;
        double ytop = 0.5*gridstep*nVsteps;  // top edge of top row
        double ybottom = -ytop;              // bottom edge of bottom row 

        //--------prepare the display font---------------

        int iFontcode = getUOGraphicsFontCode();  
        int iHpoints = iFontcode / 10000;     
        int iWpoints = 1 + iHpoints / 2;   
        double scaledW = iWpoints * uxspan / dUOpixels; 

        //-------draw the boxes and ray hit results---------
        
        iSymbol = DOT; 

        for (int i=0; i<nHsteps; i++)
          for (int j=0; j<nVsteps; j++)
          {
              //---first check to see if this box has any rays---

              double bx = steps[i][j][0]; 
              double by = steps[i][j][1]; 
              double br = Math.sqrt(bx*bx + by*by); 
              if (bSkip && (ngood[i][j] < 1))
                continue; 

              //---next locate the box center---

              double xcbox = gridstep*(i - 0.5*nHsteps + 0.5); // left to right
              double ycbox = gridstep*(0.5*nVsteps - 0.5 - j); // top to bottom  
     
              //---draw the plotbox---

              addRaw(0., 0., 0., SETCOLOR+foreground, QBASE);   // unscaled
              if (bRound)
                addScaledCircle(xcbox, ycbox, halfbox);
              else
                addScaledSquare(xcbox, ycbox, halfbox); 
                           
              String sss = getPlotBoxLabel(i, j); 
              int nchars = sss.length(); 
              if (nchars > 0)
              {
                  for (int k=0; k<nchars; k++)
                  {
                     int ic = (int) sss.charAt(k) + iFontcode; 
                     double x = xcbox + scaledW*(k-0.5*nchars+0.5); 
                     double y = ycbox + halfbox + scaledW; 
                     add2D(x, y, ic); 
                  }
              } 
              for (int k=0; k<ngood[i][j]; k++) // draw the saved ray symbols
              {
                  int icolor = kcolor[i][j][k]; 
                  if (blackbkg && (icolor==BLACK))
                    icolor = WHITE; 

                  // This assumes that results[] is sorted with first bunch=OK, rest=NG.  True??

                  double x = results[i][j][0][k] - centroids[i][j][0]; // user units
                  double y = results[i][j][1][k] - centroids[i][j][1]; // user units

                  //----normalize, test, and rescale to diagram coordinates-----

                  x *= 2.0/hspan; // -1 to +1
                  y *= 2.0/vspan; // -1 to +1

                  if (!isInBox(bRound,x,y) && bRestrict)
                    continue; 

                  x = xcbox + x*halfbox; 
                  y = ycbox + y*halfbox;
                  add2D(x, y, iSymbol+icolor);
              }
          }

          //----show the display box size in user units-----

          addRaw(0., 0., 0., SETCOLOR + (blackbkg ? WHITE : BLACK), QBASE); 
          String ss = "Hscale="+U.gd(hspan)+"  Vscale="+U.gd(vspan); 
          int nchars = ss.length(); 
          for (int k=0; k<nchars; k++)
          {
              int ic = (int) ss.charAt(k) + iFontcode; 
              double x = scaledW*(k-0.5*nchars+0.5); 
              double y = ytop + 3*scaledW; 
              add2D(x, y, ic);
          }
    }  //-----end of doArt()-------


    //----------helper methods----------------------------

    private boolean isInBox(boolean bRound, double x, double y)
    { 
        return  (bRound ? x*x+y*y : Math.max(x*x,y*y)) < 1.0; 
    }

    
    private String um(double x)   // unused
    // converts a string like 0.00300 mm into 3 microns
    {
        return U.fwd(1000*x, 12, 1).trim(); 
    }

    private String mm(double x)   // unused
    // shows a reasonable precision raw dimension
    {
        return U.fwd(x,12,6).trim(); 
    }
    
    
    private void addScaledSquare(double xc, double yc, double radius)
    // draws a square at specified location and radius
    {
        add2D(xc-radius, yc-radius, MOVETO); 
        add2D(xc+radius, yc-radius, PATHTO); 
        add2D(xc+radius, yc+radius, PATHTO); 
        add2D(xc-radius, yc+radius, PATHTO); 
        add2D(xc-radius, yc-radius, STROKE); 
    }
    
    private void addScaledCircle(double xc, double yc, double radius)
    {
        double xprev = xc + radius;
        double yprev = yc;
        add2D(xprev, yprev, MOVETO); 
        for (int i=1; i<=36; i++)
        {
            double x = xc + radius*U.cosd(10*i);
            double y = yc + radius*U.sind(10*i); 
            add2D(x, y, PATHTO); 
            xprev = x; 
            yprev = y; 
        }
        add2D(xprev, yprev, STROKE);     
    }
    
    private String getPlotBoxLabel(int i, int j)
    {
        boolean bHS   = "T".equals(DMF.reg.getuo(UO_MPLOT, 19));  // "H"
        boolean bVS   = "T".equals(DMF.reg.getuo(UO_MPLOT, 20));  // "V" 
        boolean bNum  = "T".equals(DMF.reg.getuo(UO_MPLOT, 21));  // "n"
        boolean bHbar = "T".equals(DMF.reg.getuo(UO_MPLOT, 22));  // "h"
        boolean bVbar = "T".equals(DMF.reg.getuo(UO_MPLOT, 23));  // "v"
        boolean bHH   = "T".equals(DMF.reg.getuo(UO_MPLOT, 24));  // "hh"
        boolean bVV   = "T".equals(DMF.reg.getuo(UO_MPLOT, 25));  // "vv"
        boolean bHV   = "T".equals(DMF.reg.getuo(UO_MPLOT, 26));  // "hv"
        boolean bRSS  = "T".equals(DMF.reg.getuo(UO_MPLOT, 27));  // "s"
        boolean bRMS  = "T".equals(DMF.reg.getuo(UO_MPLOT, 28));  // "m"

        String sss = ""; 
        String space = ","; 
        if (bHS)
        {
            if (sss.length() > 0)
              sss += space; 
            sss += U.gd(steps[i][j][0]); 
        }
        if (bVS)
        {
            if (sss.length() > 0)
              sss += space; 
            sss += U.gd(steps[i][j][1]); 
        }
        if (bNum)
        {
            if (sss.length() > 0)
              sss += space; 
            sss += U.fwi(ngood[i][j],9).trim(); 
        }
        if (bHbar)
        {
            if (sss.length() > 0)
              sss += space; 
            sss += U.gd(centroids[i][j][0]);
        }
        if (bVbar)
        {
            if (sss.length() > 0)
              sss += space; 
            sss += U.gd(centroids[i][j][1]);
        }
        if (bHH)
        {
            if (sss.length() > 0)
              sss += space; 
            sss += U.gd(rhh[i][j]);
        }
        if (bVV)
        {
           if (sss.length() > 0)
             sss += space; 
           sss += U.gd(rvv[i][j]);
        }
        if (bHV)
        {
           if (sss.length() > 0)
             sss += space; 
           sss += U.gd(rhv[i][j]);
        }
        if (bRSS)
        {
           if (sss.length() > 0)
              sss += space; 
            sss += U.gd(rss[i][j]);
        } 
        if (bRMS)
        {
           if (sss.length() > 0)
             sss += space; 
           sss += U.gd(rms[i][j]);
        }
        return sss; 
    }
}
