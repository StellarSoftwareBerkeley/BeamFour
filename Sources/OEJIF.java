package com.stellarsoftware.beam;

import java.util.*;         // ArrayList
import javax.swing.*;       // JMenuItem 
import javax.swing.text.*;  // BadLocationException

@SuppressWarnings("serial")

/**
  *  This file has just one class: OEJIF that extends EJIF,
  *  supplying EJIF's abstract method parse(). 
  *  The function of parse() is to set values into DMF.giFlags[] and RT13.surfs[][]. 
  *
  *  It implements Constants via EJIF. 
  *
  *  parse() has no dirty bit worksavers; it always parses. 
  *
  *  @author M.Lampton (c) 2004-2012 STELLAR SOFTWARE all rights reserved.
  */
class OEJIF extends EJIF 
{
    // public static final long serialVersionUID = 42L;

    public static int oF2I[] = new int[MAXFIELDS];    
    public static int oI2F[] = new int[ONPARMS];     
    public static String oglasses[] = new String[JMAX+1];  
    // public, so that DMF can check validity against MEJIF.mglasses[].


    public OEJIF(int iXY, String gfname) 
    {
        super(0, iXY, ".OPT", gfname, MAXSURFS); // call EJIF
        myFpath = gfname;                        // field of EJIF.
    }


    void parse()  
    // replaces the abstract parse() in EJIF. 
    // This is NOT PRIVATE; DMF:vMasterParse calls it, triggered by blinker, etc. 
    {
        adjustables = new ArrayList<Adjustment>(); 

        // First, communicate EJIF results to DMF.giFlags[]
        // vPreParse() takes care of parsing title line. 

        int status[] = new int[NGENERIC]; 
        vPreParse(status); 
        DMF.giFlags[OPRESENT] = status[GPRESENT]; 
        DMF.giFlags[ONLINES]  = status[GNLINES]; 
        DMF.giFlags[ONSURFS]  = nsurfs = status[GNRECORDS]; 
        DMF.giFlags[ONGROUPS] = nsurfs; 
        DMF.giFlags[ONFIELDS] = nfields = status[GNFIELDS]; 
        if (nsurfs < 1)
          return; 

        //-------------set up default surface data--------------

        DMF.giFlags[OMEDIANEEDED] = FALSE; // ok=notNeeded; TRUE=needed

        for (int j=1; j<=MAXSURFS; j++)
        {
            oglasses[j] = "";
            for (int ia=0; ia<ONPARMS; ia++)
              RT13.surfs[j][ia] = -0.0; // minus zero means blank entry.
              
            RT13.surfs[j][OREFRACT] = 1.0; 
            RT13.jstart[j] = j;  // groups
            RT13.jstop[j]  = j;  // groups
            RT13.group[j]  = j;  // groups
        }
 
        for (int f=0; f<MAXFIELDS; f++)
          headers[f] = "";

        for (int r=0; r<JMAX; r++)
        {
            typetag[r] = ':'; 
            for (int f=0; f<MAXFIELDS; f++)
              cTags[r][f] = ':';
        }

        //----get headers using getFieldTrim()---------------

        for (int f=0; f<nfields; f++)
          headers[f] = getFieldTrim(f, 1); 

        //--build the two one-way lookup tables for field IDs-------

        for (int i=0; i<ONPARMS; i++)   // ABSENT = -1
          oI2F[i] = ABSENT;  

        for (int f=0; f<MAXFIELDS; f++) // ABSENT = -1
          oF2I[f] = ABSENT;  

        int ntries=0, nunrecognized=0; 
        for (int f=0; f<nfields; f++)
        {
            ntries++; 
            int iatt = getOptFieldAttrib(headers[f]); // bottom of this file...
            oF2I[f] = iatt; 
            if ((iatt > ABSENT) && (iatt < ONPARMS))
              oI2F[iatt] = f; 
            else
              nunrecognized++;  // unused.
        }

        //----gather individual surface types: nonnumerical data-------
        //----a grating is not a type: it's a groovy mirror or lens----

        for (int jsurf=1; jsurf<=nsurfs; jsurf++)
        {
            RT13.surfs[jsurf][OTYPE] = OTLENS;  // default
            RT13.jstart[jsurf] = jsurf;         // default
            RT13.jstop[jsurf]  = jsurf;         // default
            RT13.group[jsurf]  = jsurf;         // default
        }
          
        //-----first parse the optics type column---------
        
        int ifield = oI2F[OTYPE]; 
        if (ifield > ABSENT)
          for (int jsurf=1; jsurf<=nsurfs; jsurf++)
          {
              String s = getFieldTrim(ifield, 2+jsurf); 
              char c0 = U.getCharAt(s, 0); 
              char c2 = U.getCharAt(s.toUpperCase(), 2); 
              char c4 = U.getCharAt(s.toUpperCase(), 4); 
              switch(c0)
              {
                 case 'b': // bimodal lens front="bif" else back "bix".
                 case 'B': RT13.surfs[jsurf][OTYPE]
                            = (c2=='F') ? OTBLFRONT : OTBLBACK; break; 
                            
                 case 'i': // iris
                 case 'I': RT13.surfs[jsurf][OTYPE] 
                             = (c4=='A') ? OTIRISARRAY : OTIRIS; break; 

                 case 'G': RT13.surfs[jsurf][OTYPE] = OTMIRROR; break;

                 case 'l':
                 case 'L': RT13.surfs[jsurf][OTYPE] 
                             = (c4=='A') ? OTLENSARRAY : OTLENS; break;

                 case 'm':
                 case 'M': RT13.surfs[jsurf][OTYPE] 
                             = (c4=='A') ? OTMIRRARRAY : OTMIRROR; break; 

                 // phantom is just a refracting surface with equal indices.

                 case 'r':
                 case 'R': RT13.surfs[jsurf][OTYPE] = OTRETRO; break; 

                 // case 's':  // spider: replaced by iris with legs. 

                 case 's':
                 case 'S':  RT13.surfs[jsurf][OTYPE] = OTSCATTER; break; 
 
                 case 'd':     // optical path distorters
                 case 'D':
                 case 'w':
                 case 'W': RT13.surfs[jsurf][OTYPE] = OTDISTORT; break; 
             
                 case 'c':     // coordinate breaks
                 case 'C': if (c2 == 'I')
                           {
                               RT13.surfs[jsurf][OTYPE] = OTCBIN; 
                               break;
                           }
                           if (c2 == 'O')
                           {
                               RT13.surfs[jsurf][OTYPE] = OTCBOUT; 
                               break; 
                           }
                           break;
                 case 't':
                 case 'T': RT13.surfs[jsurf][OTYPE] = OTTHIN; break;
                 default: RT13.surfs[jsurf][OTYPE] = OTLENS; break; 
               }
               typetag[jsurf] = getTag(ifield, 2+jsurf); 
          };
        
        //--------parse the optics forms column----------

        for (int jsurf=1; jsurf<=nsurfs; jsurf++)
          RT13.surfs[jsurf][OFORM] = OFELLIP;  // default

        ifield = oI2F[OFORM]; 
        if (ifield > ABSENT)
          for (int jsurf=1; jsurf<=nsurfs; jsurf++)
          {
              //---enforce idea of all arrays rectangular-------
              int i = (int) RT13.surfs[jsurf][OTYPE]; 
              boolean bArray = ((i==OTLENSARRAY) || (i==OTMIRRARRAY) || (i==OTIRISARRAY));

              String s = getFieldTrim(ifield, 2+jsurf); 
              char c0 = U.getCharAt(s, 0); 
              char c1 = U.getCharAt(s, 1); 
              RT13.surfs[jsurf][OFORM] = OFELLIP;  
              if ((c0=='s') || (c1=='s'))
                RT13.surfs[jsurf][OFORM] += OFIRECT; 
              if ((c0=='S') || (c1=='S') || bArray)
                RT13.surfs[jsurf][OFORM] += OFORECT; 
          } 
          
          
        //-----parse the groups if group column is present-----
        
        ifield = oI2F[OGROUP];  
        char cGroup[] = new char[MAXSURFS+1]; 
        for (int j=1; j<=MAXSURFS; j++)
          cGroup[j] = ' ';
        if (ifield > ABSENT)   // groups column present?
        {
            for (int j=1; j<=nsurfs; j++)
              cGroup[j] = U.getCharAt(getFieldTrim(ifield, 2+j), 0); 
            RT13.group[1]  = 1; 
            RT13.jstart[1] = 1;
            RT13.jstop[1]  = 1; 

            for ( int j=1; j<=MAXSURFS; j++)
            {
                boolean bNew = (cGroup[j] == ' ') 
                            || (cGroup[j-1] == ' ')
                            || (cGroup[j] != cGroup[j-1]); 
                int g = bNew ? RT13.group[j-1]+1 : RT13.group[j-1];           
                RT13.group[j]  = g; 
                if (bNew)         
                  RT13.jstart[g] = j;
                RT13.jstop[g]  = j;    
            }
            for (int j=nsurfs+1; j<=MAXSURFS; j++)
               RT13.group[j] = RT13.group[nsurfs]; 

            DMF.giFlags[ONGROUPS] = RT13.group[nsurfs]; 
        }
              
      
        //----------refraction: sometimes numerical data--------------
        //---if refraction LUT is needed, OREFRACT will be NaN.----

        for (int jsurf=1; jsurf<=nsurfs; jsurf++)
          oglasses[jsurf] = new String("");       // default: all numeric

        boolean bAllRefractNumeric = true;       // default: true; 

        ifield = oI2F[OREFRACT];
        if (ifield > ABSENT)
          for (int jsurf=1; jsurf<=nsurfs; jsurf++)
          {
              oglasses[jsurf] = getFieldTrim(ifield, 2+jsurf); 
              RT13.surfs[jsurf][OREFRACT] = U.suckDouble(oglasses[jsurf]); 
              if (Double.isNaN(RT13.surfs[jsurf][OREFRACT]))
                bAllRefractNumeric = false; 
              if (0.0 == RT13.surfs[jsurf][OREFRACT])
                RT13.surfs[jsurf][OREFRACT] = 1.0; 
          }
        DMF.giFlags[OMEDIANEEDED] = bAllRefractNumeric ? FALSE : TRUE;  


        //-------Now get numerical data records-------------------
        //---except ABSENT, OFORM, OTYPE, OREFRACT, OGROUP-----

        boolean bAllOtherNumeric = true; 
        int badline=0, badfield=0, osyntaxerr=0; 
        double d; 
        for (int jsurf=1; jsurf<=nsurfs; jsurf++)
        {
            for (int f=0; f<nfields; f++)
            {
                int ia = oF2I[f];   // attribute of this surface
                if (ia == OTYPE)    // types were analyzed above...
                  continue; 
                if (ia == OFORM)    // forms were analyzed above...
                  continue; 
                if (ia == OREFRACT) // refraction analyzed above...
                  continue; 
                if (ia == OGROUP)   // group analyzed above...
                  continue; 
                if (ia > ABSENT)    // all numerical fields can overwrite negZero here. 
                {
                    // first, fill in the datum....
                    d = RT13.surfs[jsurf][ia] = getFieldDouble(f, 2+jsurf);

                    // then check for trouble and correct it....
                    if (U.isNegZero(d) && isAdjustable(jsurf, ia))
                      RT13.surfs[jsurf][ia] = +0.0;         // active

                    // d = value, or NaN, or -0.0=unused, or +0.0=in use. 
                    // now, U.NegZero(d) indicates no use whatsoever.

                    if (Double.isNaN(d))
                    {
                        bAllOtherNumeric = false;
                        badline = jsurf+2; 
                        badfield = f; 
                        osyntaxerr = badfield + 100*badline; 
                        break; 
                    } 

                    // allow defined shape to determine asphericity...
                    if ((ia == OSHAPE) && !U.isNegZero(d))
                      RT13.surfs[jsurf][OASPHER] = d - 1.0; 
                      
                    // allow defined radii of curvature to determine curvature...  
                    if ((ia == ORAD) && (d != 0.0))
                      RT13.surfs[jsurf][OCURVE] = 1.0/d; 
                    if ((ia == ORADX) && (d != 0.0))
                      RT13.surfs[jsurf][OCURVX] = 1.0/d; 
                    if ((ia == ORADY) && (d != 0.0))
                      RT13.surfs[jsurf][OCURVY] = 1.0/d; 
                }
            }
            if (osyntaxerr > 0)
              break; 
        }

        DMF.giFlags[OSYNTAXERR] = osyntaxerr; 
        if (osyntaxerr > 0)
          return; 

        //----data are now cleansed stashed & indexed------------
        //-------Perform all the post-parse cleanup here------------        

        DMF.giFlags[ONADJ] = iParseAdjustables(nsurfs);
        
        //----force all CoordBreaks to be planar? or not? rev 168----
        // for (int j=1; j<nsurfs; j++)
        //   if ((RT13.surfs[j][OTYPE]==OTCBIN) || (RT13.surfs[j][OTYPE]==OTCBOUT))
        //   {
        //       RT13.surfs[j][OPROFILE] = OSPLANO; 
        //       for (int iatt=OCURVE; iatt<=OZ35; iatt++)
        //         RT13.surfs[j][iatt] = 0.0; 
        //   }
          
          
        //-------evaluate diameters DIAX, DIAY-------------------
        for (int j=1; j<=nsurfs; j++)
        {
            boolean bM = RT13.surfs[j][OIDIAM] > 0.0; 
            boolean bX = RT13.surfs[j][OIDIAX] > 0.0; 
            boolean bY = RT13.surfs[j][OIDIAY] > 0.0; 
            if (!bX)
            {
                if (bM)
                  RT13.surfs[j][OIDIAX] = RT13.surfs[j][OIDIAM]; 
                else if (bY)
                  RT13.surfs[j][OIDIAX] = RT13.surfs[j][OIDIAY]; 
            }
            if (!bY)
            {
                if (bM)
                  RT13.surfs[j][OIDIAY] = RT13.surfs[j][OIDIAM]; 
                else if (bX)
                  RT13.surfs[j][OIDIAY] = RT13.surfs[j][OIDIAX]; 
            }
            bM = RT13.surfs[j][OODIAM] > 0.0; 
            bX = RT13.surfs[j][OODIAX] > 0.0; 
            bY = RT13.surfs[j][OODIAY] > 0.0; 
            if (!bX)
            {
                if (bM)
                  RT13.surfs[j][OODIAX] = RT13.surfs[j][OODIAM]; 
                else if (bY)
                  RT13.surfs[j][OODIAX] = RT13.surfs[j][OODIAY]; 
            }
            if (!bY)
            {
                if (bM)
                  RT13.surfs[j][OODIAY] = RT13.surfs[j][OODIAM]; 
                else if (bX)
                  RT13.surfs[j][OODIAY] = RT13.surfs[j][OODIAX]; 
            }
        }
        boolean bAllDiamsPresent = true; 
        for (int j=1; j<=nsurfs; j++)
        {
            boolean bX = RT13.surfs[j][OODIAX] > 0.0; 
            boolean bY = RT13.surfs[j][OODIAY] > 0.0; 
            if (!bX || !bY)
              bAllDiamsPresent = false;   
        }
        DMF.giFlags[OALLDIAMSPRESENT] = bAllDiamsPresent ? TRUE : FALSE; // ints! 

        //------------set the Euler angle matrix------------------

        RT13.setEulers(); 

        //------------Test each surface for groovyness----------------
        for (int j=1; j<=nsurfs; j++)
        {
            boolean bGroovy = false; 
            for (int kg=OGX; kg<=OHOELAM; kg++)
              if (Math.abs(RT13.surfs[j][kg])>0.0)
                bGroovy = true;
            RT13.surfs[j][OGROOVY] = bGroovy ? 1.0 : 0.0; 
        }

        //---------verify that array diams are within cells------------
        for (int j=1; j<nsurfs; j++)
        {
            int i = (int) RT13.surfs[j][OTYPE]; 
            boolean bArray = ((i==OTLENSARRAY) || (i==OTMIRRARRAY) || (i==OTIRISARRAY));
            if (bArray)
            {
                double diax = RT13.surfs[j][OODIAX]; 
                if (U.isNegZero(diax))
                  diax = RT13.surfs[j][OODIAM];  
                int nx = (int) RT13.surfs[j][ONARRAYX]; 
                if ((diax<=TOL) || (nx<1))
                  continue; // continue, not return!
                double px = diax/nx;
                if(RT13.surfs[j][OIDIAX] > diax/nx)
                  RT13.surfs[j][OIDIAX] = diax/nx; 

                double diay = RT13.surfs[j][OODIAM]; 
                int ny = (int) RT13.surfs[j][ONARRAYY]; 
                if ((diay<=TOL) || (ny<1))
                  continue; // continue, not return!  
                double py = diay/ny;
                if (RT13.surfs[j][OIDIAM] > diay/ny)
                  RT13.surfs[j][OIDIAM] = diay/ny;

                if (nx*ny > MAXHOLES)
                  RT13.surfs[j][ONARRAYY] = (MAXHOLES)/nx; 
            }
        }

        //---------classify each surface profile for solvers------------
        //---CX cyl & torics: ternary logic. See MNOTES May 25 2007-----
        //
        //              C=blank,   zero,  nonzero
        //             ---------  ------  -------
        //   CX=blank:   PLANO     PLANO   CONIC
        //    CX=zero:   PLANO     PLANO   CYCYL
        // CX=nonzero:   CXCYL     CXCYL   TORIC
        //
        // Adjustability:  see below. 
        // Special case added in A119 Dec 2010:
        //    CX=nonblank and CY=nonblank: OSBICONIC
        //
        //  TERNARY LOGIC: see lines 453-463.

        boolean badZern = false;     // flag for single warning message at end
        
        for (int j=1; j<=nsurfs; j++)
        {            
            double  ce = RT13.surfs[j][OCURVE];
            double  cx = RT13.surfs[j][OCURVX]; 
            double  cy = RT13.surfs[j][OCURVY];
            double  ae = RT13.surfs[j][OASPHER]; 
            double  ax = RT13.surfs[j][OASPHX];
            double  ay = RT13.surfs[j][OASPHY]; 
            
            //---TERNARY LOGIC EVALUATOR starts here-----
            //---three states: empty field, entry=0, entry is nonzero------
            //---Determined by Curv and Cx; Cy has no influence---------  
            
            boolean bCEactive = (ce!=0.0) || isAdjustable(j, OCURVE);
            boolean bCXactive = (cx!=0.0) || isAdjustable(j, OCURVX); 
            int tce = bCEactive ? 2 : U.isNegZero(ce) ? 0 : 1;   // 0, 1, or 2.
            int tcx = bCXactive ? 2 : U.isNegZero(cx) ? 0 : 1;   // 0, 1, or 2.
            int tg[] = { OSPLANO, OSPLANO, OSCONIC, OSPLANO, OSPLANO, OSYCYL, OSXCYL,  OSXCYL,  OSTORIC}; 
            int arg = tce + 3*tcx; 
            int iProfile = tg[arg]; 
            
            // String osnames[] = {"OSPLANO", "OSPLANO", "OSCONIC", "OSPLANO", "OSPLANO", "OSYCYL", "OSXCYL", "OSXCYL", "OSTORIC"}; 
            // if (j==1)
            //   System.out.println("OEJIF ternary logic result: iProfile = "+iProfile+"  "+osnames[arg]);
            
            // Rules, A190:
            // PolyCyl: requires axis=x, Cx=0 (uncurved in XZ plane), Curve=Nonzero, poly in y: OSYCYL.
            // CircCyl: can have axis=x, Cx=0 (uncurved in XZ plane), Curve=Nonzero, no poly;  OSYCYL.
            // CircCyl: or have  axis=y, Cx=nonzero, Curve=blank or zero, no poly terms:  OSXCYL
            // Toric:   requires Curve=nonzero, Cx=nonzero. 
            //
            //----ternary logic evaluator ends here-----
            
            //----test for biconic---------------------
            //--this can overwrite the ternary logic----

            boolean bBCXactive = !U.isNegZero(cx) || isAdjustable(j,OCURVX); 
            boolean bBCYactive = !U.isNegZero(cy) || isAdjustable(j,OCURVY); 
            boolean bBAXactive = !U.isNegZero(ax) || isAdjustable(j,OASPHX); 
            boolean bBAYactive = !U.isNegZero(ay) || isAdjustable(j,OASPHY); 
            if (bBCXactive && bBCYactive && bBAXactive && bBAYactive)
              iProfile = OSBICONIC; 
              
            //-------polynomial----------

            boolean bPoly = false; 
            for (int i=OA1; i<=OA14; i++)
              if ((0 != RT13.surfs[j][i]) || isAdjustable(j,i))
                bPoly = true;

            //----Zernike flag and diameter test  -----------------
            //---CoordinateBreaks set zernikes to zero, not -0  -----
            //---here it is important to accept zeros---------------
            
            boolean bZern = false; 
            for (int i=OZ00; i<=OZ35; i++)
              if ((0 != RT13.surfs[j][i]) || isAdjustable(j,i))
                bZern = true;
                
            if (bZern && (RT13.surfs[j][OODIAM] == 0.0)) 
              badZern = true; // gives warning "Zernikes require Diameters"

            //-------upgrade if poly or zern is present--------

            if (bPoly)
              switch (iProfile)
              {
                 case OSPLANO:  iProfile = OSPOLYREV; break; 
                 case OSCONIC:  iProfile = OSPOLYREV; break; 
                 case OSXCYL:   iProfile = OSTORIC; break;
                 case OSYCYL:   iProfile = OSTORIC; break;
                 case OSTORIC:  iProfile = OSTORIC; break; 
              }

            if (bZern)
              iProfile = (iProfile==OSTORIC) ? OSZERNTOR : OSZERNREV;

            //---------apply hints to conic or cyl, not higher----------

            switch (iProfile)
            {
               case OSCONIC: 
                  if ('<' == typetag[j])  iProfile = OSCONICLT;
                  if ('>' == typetag[j])  iProfile = OSCONICGT;
                  break; 
               case OSXCYL: 
                  if ('<' == typetag[j])  iProfile = OSXCYLLT;
                  if ('>' == typetag[j])  iProfile = OSXCYLGT;
                  break; 
               case OSYCYL: 
                  if ('<' == typetag[j])  iProfile = OSYCYLLT;
                  if ('>' == typetag[j])  iProfile = OSYCYLGT;
                  break; 
            }
            RT13.surfs[j][OPROFILE] = iProfile; 
        }

        //---------feel out dOsize for getDelta()----------

        dOsize = 0.0; 
        for (int j=1; j<=nsurfs; j++)
        {
           dOsize += Math.abs(RT13.surfs[j][OX]); 
           dOsize += Math.abs(RT13.surfs[j][OY]); 
           dOsize += Math.abs(RT13.surfs[j][OZ]); 
           dOsize += Math.abs(RT13.surfs[j][OODIAM]); 
           dOsize += Math.abs(RT13.surfs[j][OODIAX]); 
        }
        if (nsurfs > 1)
          dOsize /= nsurfs; 
        if (dOsize < TOL)
          dOsize = 1.0;
          
        if (badZern)  
          JOptionPane.showMessageDialog(this, "Zernikes without Diameter are ignored.");

    }  //------end of parse()-----------------






    //-----------public functions for AutoAdjust------------
    //-----Now that Adjustment is a public class,
    //-----cannot Auto get its own data?----------------
    //-----Nope. ArrayList adjustments is private.----------
    //
    //---Yikes, sometimes at startup adjustables is all -1 even with good adjustables.
    //-----What should initialize adjustables?? 


    public double getOsize()
    // called ONLY by DMF, in support of its static method.
    {
        return dOsize; 
    }

    public double getAdjValue(int i)
    // Fetch appropriate value from RT13.surfs[][].
    // Adjustables was parsed back in line 318.
    {
       if ((adjustables!=null) && (i>=0) && (i<adjustables.size()))
       {
           int jsurf = adjustables.get(i).getRecord();
           int iattr = adjustables.get(i).getAttrib(); 
           if ((jsurf>0) && (jsurf<=nsurfs) && (iattr>=0) && (iattr<OFINALADJ))
             return RT13.surfs[jsurf][iattr]; 
       }
       return 0.0; 
    }

    public int getAdjAttrib(int i)
    // Adjustables was parsed back in line 318.
    {
       if ((adjustables != null) && (i>=0) && (i < adjustables.size()))
         return adjustables.get(i).getAttrib();
       else
         return -1; 
    }

    public int getAdjSurf(int i)
    // Adjustables was parsed back in line 318.
    {
       if ((adjustables != null) && (i>=0) && (i < adjustables.size()))
         return adjustables.get(i).getRecord();
       else
         return -1; 
    }

    public int getAdjField(int i)
    // Adjustables was parsed back in line 318.
    {
       if ((adjustables != null) && (i>=0) && (i < adjustables.size()))
         return adjustables.get(i).getField();
       else
         return -1; 
    }

    public ArrayList<Integer> getSlaves(int i)
    // Adjustables was parsed back in line 318.
    {
       if ((adjustables != null) && (i>=0) && (i < adjustables.size()))
         return adjustables.get(i).getList();
       else
         return null; 
    }







    //-------------private stuff----------------

    private static char    cTags[][] = new char[JMAX][MAXFIELDS]; 
    private static char    typetag[] = new char[JMAX]; 
    private static String  headers[] = new String[MAXFIELDS];    
    private ArrayList<Adjustment>  adjustables; 
    private static int nsurfs=0, nfields=0; 
    private static double dOsize = 0.0; 


    private int iParseAdjustables(int nsurfs)
    // fills in private ArrayList of adjustables, with slaves.
    // Returns how many groups were found based on tags.
    {
        boolean bLookedAt[] = new boolean[nsurfs+1]; 
        adjustables.clear(); 
        for (int field=0; field<nfields; field++)
        {
            int attrib = oF2I[field]; 
            if ((attrib<0) || (attrib>OFINALADJ))  // or other validity test
              continue; 

            for (int record=1; record<=nsurfs; record++)
              bLookedAt[record] = false; 

            for (int record=1; record<=nsurfs; record++)
            {
                char tag0 = getTag(field, record+2);
                boolean bAdj = isAdjustableTag(tag0); 
                if (!bAdj || bLookedAt[record])
                { 
                    bLookedAt[record] = true; 
                    continue;
                } 

                //---New adjustable parameter found------------
                bLookedAt[record] = true; 
                ArrayList<Integer> slaves = new ArrayList<Integer>(); 
                
                if (Character.isLetter(tag0))
                {
                    boolean bUpper0 = Character.isUpperCase(tag0); 
                    char tag0up = Character.toUpperCase(tag0); 
                    for (int k=record+1; k<=nsurfs; k++)
                    {
                        if (!bLookedAt[k])  // find slaves & antislaves
                        {
                            char tagk = getTag(field, k+2); 
                            boolean bUpperk = Character.isUpperCase(tagk); 
                            char tagkup = Character.toUpperCase(tagk); 
                            boolean bSameGroup = (tag0up == tagkup); 
                            if (bSameGroup)
                            {
                                int iSlave = (bUpper0 == bUpperk) ? k : -k; 
                                slaves.add(new Integer(iSlave)); 
                                bLookedAt[k] = true;  
                            }
                        }
                    }
                }
                adjustables.add(new Adjustment(attrib, record, field, slaves)); 
            } // done with all groups in this field
        }// done with all fields
        return adjustables.size(); 
    }



    boolean isAdjustable(int jsurf, int iatt)
    // Tests for range of adjustable attributes & tag chars.
    // Assumes that oI2F[] and cTags[][] are properly set. 
    // HOWEVER THIS IS DEAF TO THE NEW GANGED PARADIGM. 
    {
       if ((iatt < 0) || (iatt > OFINALADJ))
         return false; 
       int field = oI2F[iatt]; 
       if ((field < 0) || (field >= nfields))
         return false; 
       char c = getTag(field, jsurf+2);  // cTags[jsurf][field]; 
       return isAdjustableTag(c); 
    }


    boolean isAdjustableTag(char c)
    {
        return (c=='?') || Character.isLetter(c);
    }



    public static int getOptFieldAttrib(String s)
    // Given an optics table column header field, this routine returns a
    // number 0..122 for identified optics table fields, or else returns ABSENT.
    // This function is called by ParseOpt to fill in its fieldop[] array.
    // Free standing allows return from any depth! unlike break.
    // Table data should be numerical, except ABSENT, OFORM, OTYPE, OREFRACT. 
    // Radius of curvature is written "RxCxxxx" i.e. c2up='C'.
    {
        char c0=' ', c1up=' ', c2up=' ', c3up=' ', c4up=' ';
        s = s.trim(); 
        int len = s.length(); 
        if (len < 1)
          return ABSENT;
        c0 = s.charAt(0); 
        s = s.toUpperCase(); 
        if (len > 1)
          c1up = s.charAt(1); 
        if (len > 2)
          c2up = s.charAt(2); 
        if (len > 3)
          c3up = s.charAt(3);
        if (len > 4)
          c4up = s.charAt(4);
        switch (c0)
        {
          case 'A':
          case 'a': switch (c1up)  // asphericity coefficients
                    {
                       case '1': switch(c2up)
                       {
                          case '0': return OA10;
                          case '1': return OA11;
                          case '2': return OA12;
                          case '3': return OA13;
                          case '4': return OA14;
                          case '5':
                          case '6':
                          case '7':
                          case '8':
                          case '9': return ABSENT;
                          default: return OA1;
                       }
                       case '2': return OA2;
                       case '3': return OA3;
                       case '4': return OA4;
                       case '5': return OA5;
                       case '6': return OA6;
                       case '7': return OA7;
                       case '8': return OA8;
                       case '9': return OA9;
                       case 'C': return OTYPE; // "ACTION"
                    }
                    if (s.contains("X"))
                      return OASPHX;
                    if (s.contains("Y"))
                      return OASPHY;
                    return OASPHER;

          case 'C':
          case 'c': if (s.contains("X"))  // curvatures
                      return OCURVX;
                    if (s.contains("Y"))
                      return OCURVY;
                    return OCURVE; 

          case 'D': if (s.contains("X"))
                      return OODIAX;
                    if (s.contains("Y"))
                      return OODIAY;
                    return OODIAM;
                    
          case 'd': if (s.contains("X"))
                      return OIDIAX;
                    if (s.contains("Y"))
                      return OIDIAY; 
                    return OIDIAM;       
          
          case 'F':
          case 'f': switch(c2up)
                    {
                      case 'c': return OFOCAL; // focal length for thin perfect lenses
                      default: return OFORM;  // "form" = nonnumerical
                    }

          case 'G':
          case 'g': switch(c1up)   // Group or Grating groove density
                    {
                       case 'R': return OGROUP;
                       case 'X': return OGX;
                       case 'Y': return OGY;
                       case '1': return OVLS1;
                       case '2': return OVLS2;
                       case '3': return OVLS3;
                       case '4': return OVLS4;
                    }
                    return ABSENT;

          case 'H':
          case 'h': if (len < 4)     // HOE entries
                  return ABSENT;
                switch(c3up)
                {
                    case 'L':
                    case 'l': return OHOELAM;
                    case 'X':
                    case 'x': if (c4up == '1')
                                return OHOEX1;
                              if (c4up == '2')
                                return OHOEX2;
                              return ABSENT;
                    case 'Y':
                    case 'y': if (c4up == '1')
                                return OHOEY1;
                              if (c4up == '2')
                                return OHOEY2;
                              return ABSENT;
                    case 'Z':
                    case 'z': if (c4up == '1')
                                return OHOEZ1;
                              if (c4up == '2')
                                return OHOEZ2;
                              return ABSENT;
                }
                return ABSENT;

          case 'I':
          case 'i': return OREFRACT;  // refractive index or glass name

          case 'L':
          case 'l':
          case 'M':
          case 'm':  return OTYPE;  // "Lens" "mirror" etc

          case 'N':
          case 'n':  if (c1up=='S') 
                       return ONSPIDER;
                     if (c1up=='X')
                       return ONARRAYX;
                     if (c1up=='Y')
                       return ONARRAYY;
                     return ABSENT; 

          case 'O':  // OffIX, OffIY, OffOX, OffOY
          case 'o':  if ((c3up=='O') && (c4up=='X')) return OFFOX;
                     if ((c3up=='O') && (c4up=='Y')) return OFFOY;
                     if ((c3up=='I') && (c4up=='X')) return OFFIX;
                     if ((c3up=='I') && (c4up=='Y')) return OFFIY;
                     return OORDER;

          case 'P':
          case 'p':  return OPITCH;

          case 'R':
          case 'r':  if ((c2up=='C') && (c3up=='X')) return ORADX;
                     if ((c2up=='C') && (c3up=='Y')) return ORADY;
                     if (c2up=='C') return ORAD;  
                     return OROLL;

          case 'S':
          case 's':  if (c1up=='C')  return OSCATTER; 
                     return OSHAPE; 

          case 'T':
          case 't':  if (c1up == 'Y')  return OTYPE; 
                     return OTILT;     // tilt. 

          case 'V':
          case 'v':  switch (c3up)
                     {
                        case '1': return OVLS1;
                        case '2': return OVLS2;
                        case '3': return OVLS3;
                        case '4': return OVLS4;
                     }
                     return ABSENT;

          case 'W':
          case 'w':  if (c1up=='S')  
                       return OWSPIDER;
                     else return ABSENT; 

          case 'X':
          case 'x':  return OX;

          case 'Y':
          case 'y':  return OY;

          case 'Z':
          case 'z':  if (c1up=='E')  // e.g.Zern6
                     {
                        int i = U.suckInt(s); 
                        return ((i>=0) && (i<36)) ? OZ00+i : ABSENT;
                     }
                     else return OZ;
             
          default:   return ABSENT; 
        }
    } //---end of getOptFieldAttrib----------
    

    public static int getSimplifiedOptFieldAttrib(String s)
    // Given a one character hint at an optics table column header field, 
    // this routine returns the number 0..99 for identified optics table fields, 
    // or else returns -1 indicating ABSENT.
    //
    // OSHAPE is absent here since it needs conversion to OASPHER in OEJIF. 
    //
    // Called by MPlotPanel and MapPanel for their simple optics parameters.
    {
        int result = ABSENT; 
        s = s.toUpperCase(); 
        if (s.length() < 1)
          return result; 
        char c = s.charAt(0); 
        switch (c)
        {
            case 'A':  result = OASPHER; break;
            case 'C':  result = OCURVE; break;
            case 'P':  result = OPITCH; break; 
            case 'R':  result = OROLL; break; 
            case 'T':  result = OTILT; break; 
            case 'X':  result = OX; break; 
            case 'Y':  result = OY; break; 
            case 'Z':  result = OZ; break;      
            default:   result = ABSENT; 
        }
        return result; 
    }
    
}
