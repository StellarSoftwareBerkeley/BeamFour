package com.stellarsoftware.beam;

import java.util.*;        // ArrayList
import javax.swing.*;      // JOptionPane dialog

/** InOut.java
  *  A207 eliminates groups: Dec 2018
  *  A207 blanks fields that are negative zero: see EJIF line 680
  *  Rev A34 uses RT13.dGetRay() for entire ray trace:
  *  Rev 112 accommodates groups of surfaces.
  *  Runs a complete ray trace and posts results to the ray table. 
  *  If ngoals>0, or WFEcolumn, uses Comparo to get residuals & rms.
  *  If there are floating goals, Comparo (not InOut) updates ray table. 
  *
  *  Test giFlags[RWFEFIELD] >= 0 to see if WFEcalc is to be RMS'd.
  *  Added RDOT output field March 2015 MLL.
  *
  *  @author: M.Lampton (c) 2003 - 2105 STELLAR SOFTWARE all rights reserved.
  */
class InOut implements B4constants
{
    private OEJIF optEditor = null; 
    private REJIF rayEditor = null; 
    private int nsurfs=0, nfields=0, nrays=0, onfields=0, rnfields=0;
    private int ngood=0, ngoals=0;


    public InOut()  // constructor; performs the entire task. 
    {
        if (!bInitialSetup())
          return; 
        DMF.bringEJIFtoFront(rayEditor);

        //------setup is complete. Trace & display results----------

        ngood = RT13.iBuildRays(true);
        vUpdateRayTable(); 

        boolean bWFE = DMF.giFlags[RWFEFIELD] > RABSENT;
        ngoals = DMF.giFlags[RNGOALS] + (bWFE ? 1 : 0); 
        if (ngoals < 1)
          return;  // return without complaint. 

        if (ngood<1)
        {
            JOptionPane.showMessageDialog(rayEditor,
                "No good rays", 
                "InOut status", 
                JOptionPane.INFORMATION_MESSAGE); // shows black exclamation
            return; 
        }

        Comparo.doResiduals(); 
        int npts = Comparo.iGetNPTS(); 
        double rms = Comparo.dGetRMS(); 
        String ss = "RMS 1D Average = "+U.fwe(rms)     + '\n';
        if (ngoals > 1)
        {
             if (bWFE)
               ss += "Caution: WFE + other goals"    + '\n'; 
             else if (ngoals==2)
               ss += "RSS 2D Radius = "+U.fwe(ROOT2*rms) + '\n'; 
        }
        ss += "      Nrays = "+U.fwi(ngood,9).trim() + '\n'  
             +"      Ngoals = "+U.fwi(ngoals,1)      + '\n'
             +"      Nterms = "+U.fwi(npts,9).trim(); 
        JOptionPane.showMessageDialog(rayEditor, ss, 
          "RMS from goals", JOptionPane.PLAIN_MESSAGE); // shows no icon
    }


    private boolean bInitialSetup()
    {
        optEditor = DMF.oejif;
        rayEditor = DMF.rejif;
        nsurfs = DMF.giFlags[ONSURFS]; 
        onfields = DMF.giFlags[ONFIELDS];  // fields per optic.
        nrays = DMF.giFlags[RNRAYS]; 
        rnfields = DMF.giFlags[RNFIELDS];  // fields per ray.
        ngoals = DMF.giFlags[RNGOALS]; 
        if ((optEditor==null) || (rayEditor==null))
          return false; // SNH graying.
        if ((onfields<1) || (nrays<1) || (rnfields<1))
          return false; // SNH graying. 
        return true; 
    }


    private void vUpdateRayTable()
    // Grabs selected data from RT13.dGetRay() and fills in RayTable.
    // Assumed globals: nrays, nfields, ngoals, rayEditor.
    // Run iSetup(), then RT13.iBuildRays(), prior to calling this. 
    {
        for (int kray=1; kray<=nrays; kray++) // ray loop
        { 
            if (DEBUG)
               System.out.println("InOut.vUpdateRayTable starting with kray = "+kray);
            int row = kray+2; 
            int howfarOK = RT13.getHowfarOK(kray);     // unneeded here.
            int howfarLoop = RT13.getHowfarLoop(kray);  // does everything: OK and Fail
            if (DEBUG)
               System.out.println("InOut.vUpdateRayTable has howfarOK, howfarLoop = "+howfarOK+"  "+howfarLoop);  
            for (int f=0; f<rnfields; f++)    // field loop
            {
                int op = REJIF.rF2I[f]; 
                if (op == RNOTE)  // ray note message here....
                {
                    int stat = RT13.getStatus(kray); 
                    String s = " "+sResults[stat] + U.fwi(howfarLoop,2);
                    if (DEBUG)
                       System.out.println("InOut.vUpdateRayTable RNOTE: stat, str = "+stat+"  "+s);         
                    rayEditor.putField(f, row, s); 
                }
                else
                   if (op == RDEBUG) // debugger message here....
                     rayEditor.putField(f, row, RT13.isRayOK[kray] ? "OK" : "NG"); 
                else
                   if (op >= RGOAL) // Comparo handles floating goals update
                     continue; 
                else
                  if (op >= 100)   // output table results are wanted here...
                  {
                      rayEditor.putBlank(f, row); 
                      int j = RT13.getSurfNum(op); // handles "final"
                      int ia = RT13.getAttrNum(op); 

                      if ((ia>=0) && (ia<RNATTRIBS) && (j>0) && (j<=howfarOK))
                      {
                        double x = RT13.dGetRay(kray,j,ia); 
                        if (U.isNotNegZero(x))
                           rayEditor.putFieldDouble(f, row, RT13.dGetRay(kray,j,ia));  // in EJIF
                      }
                  }
            } // done with writing all fields for this ray. 
        } // done with all rays. 

        rayEditor.repaint(); 

    } // end of vUpdateRayTable(). 


} //---------end of InOut----------------
