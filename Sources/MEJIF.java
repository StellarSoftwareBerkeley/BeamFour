package com.stellarsoftware.beam;

import javax.swing.*; 

@SuppressWarnings("serial")

/**
  * MEJIF is a media editor class concreting the abstract class EJIF.
  * It accesses B4Constants via EJIF which implements B4Constants. 
  * It supplies EJIF's abstract method parse(). 
  *
  * Uses from EJIF: getFieldTrim(f,r).
  *
  * Fills in two public local namelists:
  *    mglasses[1...] is record=1...MNRECORDS;
  *    mwaves[1...] is headers[1, 2, ... MNFIELDS-1]
  * Also fills in double RT13.media[iglass=1...][jwave=1...].
  * Checker of names should trim before comparing. 
  *
  * DMF compares OEJIF.oglasses[] against MEJIF.mglasses[]. 
  *
  * DMF comparse REJIF.wavenames[] against MEJIF.mwaves[]. 
  *
  * MPlotPanel will need to verify its wavenames against mwaves[].
  *
  * @author M.Lampton (c) 2004 STELLAR SOFTWARE all rights reserved. 
  */
class MEJIF extends EJIF
{
    // public static final long serialVersionUID = 42L;

    ///////////////// private MEJIF items/////////////

    private static String  text = new String();

    // these next two are public to allow DMF validity checks.
    public  static String  mwaves[] = new String[MAXFIELDS];      
    public  static String  mglasses[] = new String[MAXMEDIA+1];  


    public MEJIF(int iXY, String gfname)
    // constructor creates a media editor using EJIF
    {
        super(2, iXY, ".MED", gfname, MAXMEDIA); 
        myFpath = gfname; 
    }

    void parse()  // replaces the abstract parse() in EJIF
    {
        int nglasses=0, nfields=0; 

        /////  first, communicate EJIF arrays to DMF.giFlags[]
        
        int status[] = new int[NGENERIC]; 
        vPreParse(status);            // in EJIF; puts generic data into status[]
        DMF.giFlags[MPRESENT] = status[GPRESENT]; 
        DMF.giFlags[MNLINES] = status[GNLINES]; 
        nglasses = status[GNRECORDS]; 
        if (nglasses > MAXMEDIA+1)    // safeguard against overflow
          nglasses = MAXMEDIA; 
        DMF.giFlags[MNGLASSES] = nglasses; 
        DMF.giFlags[MNFIELDS] = nfields = status[GNFIELDS]; 
        if (nglasses < 1)
          return; 

        /////////////// initialize the output data //////////////

        DMF.giFlags[MNWAVES] = 0;
        DMF.giFlags[MSYNTAXERR] = 0; 

        for (int irec=0; irec<=MAXMEDIA; irec++)
          for (int f=0; f<MAXFIELDS; f++)
            RT13.media[irec][f] = 1.0; 

        for (int irec=1; irec<=MAXMEDIA; irec++)
          mglasses[irec] = ""; 
 
        for (int f=0; f<MAXFIELDS; f++)
          mwaves[f] = "";

        //////////// mwaves[1...] begin at field=1, line=1 //////

        for (int f=1; f<nfields; f++)  // skip field zero it is not a wavelength field
        {
             mwaves[f] = getFieldTrim(f, 1); 
             // System.out.println("MEJIF finds wave name = "+mwaves[f]);
        }
        DMF.giFlags[MNWAVES] = nfields-1; 


        //////// mglasses[1...]  begin at record=1, line=3 /////

        for (int irec=1; irec<=DMF.giFlags[MNGLASSES]; irec++)
        {
            mglasses[irec] = getFieldTrim(0, irec+2); 
            // System.out.println("MEJIF finds glass name = "+mglasses[irec]);
        } 

        //////// parse the data records into RT13.media[][] //////

        int badline=0, badfield=0, msyntaxerr=0; 
        for (int f=1; f<=DMF.giFlags[MNWAVES]; f++)
        {
            for (int irec=1; irec<=DMF.giFlags[MNGLASSES]; irec++)
            {
                double n = RT13.media[irec][f] = getFieldDouble(f, 2+irec); 
                // System.out.printf("MEJIF refraction at f,g= %3d %3d %8.5f \n", f, irec, n);
                if (Double.isNaN(RT13.media[irec][f])) 
                {
                    badline = irec+2; 
                    badfield = f; 
                    msyntaxerr = badfield + 100*badline; 
                    // leave NaN in place here to alert Gparse!
                    break; 
                }
                if (RT13.media[irec][f] == 0.0)  // make blank or zero = 1.0
                  RT13.media[irec][f] = 1.0;  
            }
            if (msyntaxerr > 0)
              break; 
        }
        DMF.giFlags[MSYNTAXERR] = msyntaxerr; 
        // System.out.println("MEJIF finds nwaves = " + DMF.giFlags[MNWAVES]);
        // System.out.println("MEJIF finds nglasses = " + DMF.giFlags[MNGLASSES]); 
    } // end of parse(). 
}

