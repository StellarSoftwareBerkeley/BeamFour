package com.stellarsoftware.beam;

import java.util.*;         // ArrayList
import javax.swing.*; 
import javax.swing.text.*;  // BadLocationException

@SuppressWarnings("serial")

/**
  *  REJIF is a concrete ray editor class extending EJIF.
  *  It supplies EJIF's abstract method parse(). 
  *  It implements Consts via EJIF. 
  * 
  *  A156: allows "wa" to substitute for "@wave"
  *
  *  Uses U for suckInt(), suckDouble, getCharAt(), and for debugging.
  *
  *  Uses EJIF for getTag(f,r) and getFieldTrim(f,r).
  *
  * Writes to external RT13.raystarts[][][]
  * also to RT13.smins[], RT13.spans[] for random <<RORDER??
  * Writes/reads external DMF.giFlags[] etc
  *
  * Caution: irec=1...RNRAYS; record zero is a special ray.
  * Lookup table rF2I[] is public for raystarts[][], InOut, Auto.
  * Lookup table rI2F[] exists for raystart attribs 0...8 = RX...RORDER. 
  * Lookup table rI2F[] does not exist for all possible args
  * because >10000 ray attributes and they are few and sparse.  
  * Better to just search the rF2I[] list.
  * Table returns RABSENT for unrecognized field. 
  * But remember to reinterpret RFINAL and RGOAL for output usage.
  *  * To support AutoAdjust, need an output list for all goals,
  * and field numbers for those goals, so that each ray can
  * have its discrepancy computed. DCRFs are defined in OEJIF. 
  *
  * @author M.Lampton (c) STELLAR SOFTWARE 2004 all rights reserved.
  */
class REJIF extends EJIF 
{
    // ArrayList goals;
   
    public static String wavenames[] = new String[JMAX];
    public static int rF2I[] = new int[MAXFIELDS]; 
    public static int rI2F[] = new int[RNSTARTS];  // raystarts ONLY not general opcodes.
    public static int wavefield; 
    // public static final long serialVersionUID = 42L;


    public REJIF(int x, JMenuItem gjmi1, JMenuItem gjmi2, 
    boolean toOpen, String gfname, int gmaxrec)
    {
        super(1, x, ".RAY", gjmi1, gjmi2, toOpen, gfname, gmaxrec); 
        myFpath = gfname; 
    }



    private static String  headers[] = new String[MAXFIELDS];    
    private static int     nrays, nfields, fwfe; 
    private ArrayList<Adjustment> adjustables; 



    void parse() // replaces the abstract parse() in EJIF
    {    
        adjustables = new ArrayList<Adjustment>(); 

        //---First set DMF.giFlags[] defaults--------------

        int status[] = new int[NGENERIC]; 
        vPreParse(status);                          // EJIF generic parser
        DMF.giFlags[RPRESENT]     = status[GPRESENT]; 
        DMF.giFlags[RNLINES]      = status[GNLINES]; 
        DMF.giFlags[RNRAYS]       = nrays = status[GNRECORDS]; 
        DMF.giFlags[RNFIELDS]     = nfields = status[GNFIELDS]; 
        DMF.giFlags[RWFEFIELD]    = fwfe = RABSENT; // -1
        DMF.giFlags[RNWFEGROUPS]  = 1;              // one group
        DMF.giFlags[RALLWAVESPRESENT] = 0;          // false

        DMF.giFlags[RNRAYADJ]     = 0;        // Nadj in raystart #1; can be up to 6!
        DMF.giFlags[RAYADJ0]      = RABSENT;  // no autoray attributes yet
        DMF.giFlags[RAYADJ1]      = RABSENT;
        DMF.giFlags[RAYGOALATT0]  = RABSENT;  // no autoray goals yet either
        DMF.giFlags[RAYGOALATT1]  = RABSENT;
        DMF.giFlags[RAYGOALFIELD0]= RABSENT;
        DMF.giFlags[RAYGOALFIELD1]= RABSENT;
        DMF.giFlags[RNGOALS]      = 0;        // no ray table goals yet

        // Blunder here keeps RayGenerator from finding its headers. 
        // The detailed field IDs are needed even with nrays = 0.
        //
        // if (nrays < 1)
        //   return; 

        //----Then zero the output arrays------------------

        for (int kray=0; kray<=MAXRAYS; kray++)
        {
            wavenames[kray] = "";
            for (int ia=0; ia<RNSTARTS; ia++)
              RT13.raystarts[kray][ia] = -0.0; // -0.0 means absentee data
            RT13.iWFEgroup[kray] = 0;          // default is group #zero
        }

        for (int f=0; f<MAXFIELDS; f++)
          headers[f] = "";

        wavefield = RABSENT; 

        //---------set headers and lookup table--------------

        for (int f=0; f<MAXFIELDS; f++) 
        {
            rF2I[f] = RABSENT;
            headers[f] = ""; 
        }  

        for (int i=0; i<RNSTARTS; i++)
          rI2F[i] = RABSENT; 

        wavefield = DMF.giFlags[RWAVEFIELD] = RABSENT; 
        int ntries=0, nunrecognized=0, ngoals=0;
        int iU=0, iV=0, iW=0; 
        for (int field=0; field<nfields; field++)
        {
            ntries++; 
            headers[field] = getFieldTrim(field, 1); 
            int op = getCombinedRayFieldOp(headers[field]); // below
            if (op <= RABSENT)
              nunrecognized++; 
            rF2I[field] = op; 
            if ((op >= RX) && (op < RNSTARTS))  // allows inputs only.
              rI2F[op] = field;                 // array size = inputs only.
            if (op == RU)
              iU=1;
            if (op == RV)
              iV=1;
            if (op == RW)
              iW=1; 
            if ((op == RSWAVEL) && (wavefield == ABSENT))
            {
                wavefield = DMF.giFlags[RWAVEFIELD] = field; 
                DMF.giFlags[RALLWAVESPRESENT] = 1;  // true; 
            } 
            if ((op>=RGOAL) && (op<RGOAL+13))
              ngoals++; 
            if (op % 100 == RTWFE)
              DMF.giFlags[RWFEFIELD] = fwfe = field; 
        }
        DMF.giFlags[RNGOALS] = ngoals;  // + ((fwfe>=0) ? 1 : 0); 

        DMF.giFlags[RUVWCODE] = iU + 2*iV + 4*iW; 

        //--------------get data records, by field----------

        boolean allWavesNumeric = true; 
        int badline=0, badfield=0, rsyntaxerr=0; 
        double t=0.0; 

        for (int field=0; field<DMF.giFlags[RNFIELDS]; field++)
        {
            int op = rF2I[field]; 

            // get raystarts:  RX,RY,RZ,RU,RV,RW,RPATH,RSWAVEL,RSCOLOR,RSORDER
            // these get stored in RT13.raystarts[][] overwriting -0.0

            if ((op>=RX) && (op<RNSTARTS)) // subset for input Raystarts.
            {   
                //------autoray adjustables found in initial ray record--------
                //--count all the question marks but save only the first two---

                if (getTag(field, 3) == '?') 
                {
                    if (DMF.giFlags[RNRAYADJ] == 0)  
                      DMF.giFlags[RAYADJ0] = op;
                    else if (DMF.giFlags[RNRAYADJ] == 1)
                      DMF.giFlags[RAYADJ1] = op;
                    DMF.giFlags[RNRAYADJ]++;
                }

                //------now get all get raystarts----------------------

                for (int kray=1; kray<=nrays; kray++)
                {
                    t = RT13.raystarts[kray][op] = getFieldDouble(field, 2+kray);
                    if (Double.isNaN(t))
                    {
                        if (op == RSWAVEL)
                          allWavesNumeric = false; 
                        else
                        {
                            badline = kray+2; 
                            badfield = field; 
                            rsyntaxerr = badfield + 100*badline; 
                            break; 
                        }
                    } 
                    if (op == RSWAVEL)  // special case, no syntax check
                    {
                        wavenames[kray] = getFieldTrim(field, 2+kray); 
                        if (wavenames[kray].length() < 1)
                          DMF.giFlags[RALLWAVESPRESENT] = 0;  // false
                        RT13.raystarts[kray][RSCOLOR] = U.getColorCode(getTag(field, 2+kray)); 
                    }
                }
            }
            if (rsyntaxerr > 0)
              break; // break out of field loop to preserve location. 

            // Now syntax-test any goal values that may be present,
            // but don't store them anywhere.  
            // InOut and Auto will store them internally as needed.

            if ((op >= RGOAL) && (op <= RGOAL+RTWL))  // greater than 10100 !!
            {   
                if (DMF.giFlags[RAYGOALATT0] > RABSENT)
                {
                    DMF.giFlags[RAYGOALFIELD1] = field; 
                    DMF.giFlags[RAYGOALATT1] = op;
                }
                else
                {
                    DMF.giFlags[RAYGOALFIELD0] = field; 
                    DMF.giFlags[RAYGOALATT0] = op; 
                }
                for (int kray=1; kray<=nrays; kray++)
                {
                    if (Double.isNaN(getFieldDouble(field, 2+kray)))
                    {
                        badline = kray+2; 
                        badfield = field; 
                        rsyntaxerr = badfield + 100*badline; 
                        break; 
                    } 
                }
            }
            if (rsyntaxerr > 0)
              break; // break out of field loop to preserve location. 
        } //-------end of field loop-------------

        DMF.giFlags[RSYNTAXERR] = rsyntaxerr; 
        DMF.giFlags[RALLWAVESNUMERIC] = allWavesNumeric ? 1 : 0; 
        DMF.giFlags[RNADJ] = iParseAdjustables(nrays); 
        DMF.giFlags[RNWFEGROUPS] = iParseWFEgroups(nrays); 

        setSminsSpans();                 // needs WFEgroups, above.

    }  //---------end of parse()---------------



    //--------public methods for this and other parsers to use---------------

    public static int getCombinedRayFieldOp(String s)
    /**  (c) 1993, 2004 M.Lampton STELLAR SOFTWARE
     *  Computes a combined field op code = RayAttrib + 100 * RaySurface.
     *  Runs within ray parser, when nsurfs is likely unknown. 
     *  Returns RABSENT=-1 when input string is unrecognized.
     *  Returns 0..5, 6=RPATH, 7=RWAVEL, 8=RCOLOR, 9=RORDER; 10=RNSTARTS.
     *  Returns 100..up for output data fields
     *  Special cases for outputs RNOTE=133, RDEBUG=134, RFRONT=135.
     *  Careful: RFINAL=10000.
     *  Careful: RGOAL=10100 is both an input and output field.
     *  MLL Aug1998: added 'o', 'O' as synonyms for zero '0'
     *  MLL Oct2013: added "wa" as synonym for "@" wavelength
     *  input data have surfcode==0
     *  The interpretation of RFINAL cannot be done here since nsurfs may change.
     *  It has to be done within the output routine, not here. 
     *  The methods RT13.getSurf(op) and RT13.getAttr(op) do this.
     */
    {
        int len = s.length(); 
        if (len < 1)
          return RABSENT; 
        char c0=' ', c0up=' ', c1up=' ', c2up=' '; 
        c0 = s.charAt(0); 
        s = s.toUpperCase(); 
        c0up = s.charAt(0);
        if (len>1)
          c1up = s.charAt(1); 
        if (len>2)
          c2up = s.charAt(2);  

        if (c0up == 'N')            // raynotes output field
          return RNOTE;
        if (c0up == 'D')            // debugnotes output field
          return RDEBUG;
        if (c0up == 'O')            // diffraction order input field
          return RSORDER;
        if (c0up == '@')            // wavelength input field.
          return RSWAVEL;
        if ((c0up=='W') && (c1up=='A'))
          return RSWAVEL;           // also a wavelength input field.

        if ((c0up=='W') && (c1up=='F') && (c2up=='E'))
          return RTWFE+RFINAL;

        /// calculate the surfcode:
        int surfcode = 0; 
        switch (c1up)
        {
            case ' ':
            case 'F': surfcode = RFINAL; break;   // 10000
            case 'G': surfcode = RGOAL; break;    // 10100
            case 'O': surfcode = 0;  break;       // synonym for zero
            default:  surfcode = 100*U.getTwoDigitCode(s); 
        }
        if (surfcode < 0)
          return RABSENT;  

        boolean bNonzero = surfcode > 99; 
        
        /// now affix the attribute code:

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
            case 'x':  return bNonzero ? RTXL+surfcode : RX;
            case 'y':  return bNonzero ? RTYL+surfcode : RY;
            case 'z':  return bNonzero ? RTZL+surfcode : RZ;
            case 'u':  return bNonzero ? RTUL+surfcode : RU;
            case 'v':  return bNonzero ? RTVL+surfcode : RV;
            case 'w':  return bNonzero ? RTWL+surfcode : RW;
            default:   return RABSENT;
        }
    }





    //--------- public methods for autoadjust inquiries-----------------

    public double getAdjValue(int i)
    // Fetch appropriate value from RT13.raystarts[][]
    {
       if ((adjustables!=null) && (i>=0) && (i<adjustables.size()))
       {
           int kray = adjustables.get(i).getRecord();
           int iattr = adjustables.get(i).getAttrib(); 
           if ((kray>0) && (kray<=nrays) && (iattr>=0) && (iattr<OFINALADJ))
             return RT13.raystarts[kray][iattr]; 
       }
       return 0.0; 
    }

    public int getAdjAttrib(int i)
    {
       if ((adjustables != null) && (i>=0) && (i < adjustables.size()))
         return adjustables.get(i).getAttrib();
       else
         return -1; 
    }

    public int getAdjRay(int i)
    {
       if ((adjustables != null) && (i>=0) && (i < adjustables.size()))
         return adjustables.get(i).getRecord();
       else
         return -1; 
    }

    public int getAdjField(int i)
    {
       if ((adjustables != null) && (i>=0) && (i < adjustables.size()))
         return adjustables.get(i).getField();
       else
         return -1; 
    }

    public ArrayList<Integer> getSlaves(int i)
    {
       if ((adjustables != null) && (i>=0) && (i < adjustables.size()))
         return adjustables.get(i).getList();
       else
         return null; 
    }





    //-------------private methods-----------------------

    private int iParseAdjustables(int nrays)
    // fills in private ArrayList of adjustables, with slaves & antislaves.
    // Returns how many groups were found based on rayStart tags.
    {
        boolean bLookedAt[] = new boolean[nrays+1]; 
        adjustables.clear(); 
        for (int field=0; field<nfields; field++)
        {
            int op = rF2I[field]; 
            if ((op<RX) || (op>RW))  // or other validity test
              continue; 

            for (int record=1; record<=nrays; record++)
              bLookedAt[record] = false; 

            for (int record=1; record<=nrays; record++)
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
                    for (int k=record+1; k<=nrays; k++)
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
                adjustables.add(new Adjustment(op, record, field, slaves)); 
            } // done with all groups in this field
        }// done with all fields
        return adjustables.size(); 
    }

    private boolean isAdjustableTag(char c)
    {
        return (c=='?') || Character.isLetter(c);
    }


    private int iParseWFEgroups(int nrays)
    // Fills in RT13.iWFEgroup[kray].
    // Groups are numbered 0, 1, ...ngroups-1. 
    // Returns how many groups were found based on WFE tags.
    {
        for (int k=1; k<=nrays; k++)    // First, assign everything to 
          RT13.iWFEgroup[k] = 0;        // group number zero. 

        if (fwfe < 0)                   // No WFE field present?
          return 1;                     // Then just one huge group.

        int ngroups = 0;                // Initialize search engine. 
        int nraysfound = 0; 
        boolean bLookedAt[] = new boolean[nrays+1]; 
        for (int kray=1; kray<=nrays; kray++)
          bLookedAt[kray] = false;  

        for (int ktop=1; ktop<=nrays; ktop++) // search for next top ray
        {
            if (bLookedAt[ktop])              // skip; already catalogued.
              continue; 
            char tag = getTag(fwfe, ktop+2); 

            // Search for all cohorts here and below
            int nrayspergroup = 0;
            for (int k=ktop; k<=nrays; k++)
            {
                if (!bLookedAt[k] && (tag==getTag(fwfe, k+2)))
                {
                    RT13.iWFEgroup[k] = ngroups; 
                    bLookedAt[k] = true;  
                    nrayspergroup++;     // diagnostic
                    nraysfound++;        // diagnostic
                }
            }
            ngroups++; 
        } 
        return ngroups; 
    }



    private void setSminsSpans()
    // Examines tabulated raystarts for extreme values;  
    // Sets smins[][], spans[][] for use by randomizer in RT13.
    // Called only locally by REJIF.parse().
    // Special case for absentee data: let smin[]=span[]=-0.0
    // Uses RT13.iWFEgroup[] -- must run iParseWFEgroups() first!!
    {
        int nrays = DMF.giFlags[RNRAYS]; 
        int ngroups = DMF.giFlags[RNWFEGROUPS]; 

        double a=0.0, b=0.0, x; 
        for (int ig=0; ig<ngroups; ig++)
        {
            for (int iatt=RX; iatt<=RW; iatt++)
            {
                int raycount = 0; 
                a = b = 0.0; 
                boolean bPresent = false; 
                boolean bAngles = (iatt >= RU); 
                for (int k=1; k<=nrays; k++)
                {
                    if (ig == RT13.iWFEgroup[k])  //---gather a, b-----
                    {
                        raycount++; 

                        x = RT13.raystarts[k][iatt]; 
                        if (!U.isNegZero(x))
                          bPresent = true; 

                        if (raycount == 1)
                        {
                            a = b = RT13.raystarts[k][iatt]; 
                            bPresent = !U.isNegZero(a); 
                        }
                        else if (raycount > 1)
                        {
                            a = Math.min(a, x); 
                            a = bAngles ? U.pm1(a) : a; 
                            b = Math.max(b, x); 
                            b = bAngles ? U.pm1(b) : b; 
                        }
                    }
                } //-----------done searching all rays-----------


                RT13.smins[ig][iatt] = bPresent ? a : -0.0; 
                RT13.spans[ig][iatt] = bPresent ? b-a : -0.0;

            } //-------------done with all attributes--------
        } //---------------done with all groups---------------------
    } //-----------------done setting spans-----------------------
}
