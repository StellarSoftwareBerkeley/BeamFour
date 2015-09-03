package com.stellarsoftware.beam;

/**
  *  Performs evaluation of residual vector = observed - goal
  *  or, for WFE, residual vector = WFE since goalValues=0.0
  *  Also evaluates SOS for this residual.
  *  If/when it modifies floating goals, it posts them onto REJIF.
  *
  *  All static methods; no constructor. 
  *
  *  Principal public method is nGetResiduals(), returns npts;
  *  also  dGetSOS()
  *  also  dGetRMS()
  *  also  resid[]  with npts data points.
  *
  *  Has no UI except via OEJIF, REJIF, AutoAdj, AutoRay.
  *  Imports nothing. 
  *  Assumes that RT13.iBuildRays() has already been run.
  *  Assumes RT13.dRays[k][j][i], RT13.bGoodRay[k] are current.
  *  (c) 2007 M.Lampton STELLAR SOFTWARE
  */
class Comparo implements B4constants
{
    public  static double resid[];        // [npts]; densely packed, public for Auto.
    public  static int goalAttrib[];      // attribute for each goal; 13=RTWFE.
    public  static int goalField[];       // field Number for each goal 
    
    private static OEJIF optEditor = null; 
    private static REJIF rayEditor = null; 
    private static int ngroups=0, nrays=0, onfields=0, rnfields=0;
    private static int ngood=0, ngoals=0, nadj=0; 
    private static int npts=0; 
    private static double sos=0.0;
    private static double goalValue[][];  // [ngoals][nrays]; sparse in rays
    private static boolean bDone[];
    private static boolean bHasWFE;

    //--------------sole public method--------------------

    public static void doResiduals()
    // Assumes trace has been run already: RT13.iBuildRays(true).
    // Counts up npts from RT13.bGoodRay[]. 
    // Places residuals into public resid[].
    // Places sos, rms, npts into gettable private storage.
    {
        if (!bSetup())
        {
            npts = 0; 
            return; 
        }

        if (ngoals > 0)  // explicit goals to manage
        {
            vSetupGoals();
            vGangGoals();
        }    

        sos = -0.0; 
        npts = 0; 

        if (ngoals > 0)  // explicit goals case
        {
            for (int kray=1; kray<=nrays; kray++)
            {
                if (RT13.bGoodRay[kray])
                  for (int igoal=0; igoal<ngoals; igoal++)
                  {
                      double t1 = getRay(kray, goalAttrib[igoal]); 
                      double t2 = goalValue[igoal][kray]; 
                      resid[npts] = t1-t2;
                      sos += resid[npts]*resid[npts]; 
                      npts++; 
                  }
            }
        }

        if (bHasWFE)  // implicit goal case
        {
            for (int kray=1; kray<=nrays; kray++)
              if (RT13.bGoodRay[kray])
              {
                  resid[npts] = getRay(kray, RTWFE);
                  sos += resid[npts]*resid[npts]; 
                  npts++;
              }
        }
    }

    public static int iGetNPTS()
    {
        return npts;
    }

    public static double dGetSOS()
    {
        return sos;
    }

    public static double dGetRMS()
    {
        return (npts > 0) ? Math.sqrt(sos/npts) : -0.0; 
    }


    
    //--------------private methods---------------------------

    private static boolean bSetup()
    {
        optEditor = DMF.oejif;
        rayEditor = DMF.rejif;
        ngroups   = DMF.giFlags[ONGROUPS]; 
        onfields  = DMF.giFlags[ONFIELDS];       // fields per optic.
        nrays     = DMF.giFlags[RNRAYS]; 
        rnfields  = DMF.giFlags[RNFIELDS];       // fields per ray.
        ngoals    = DMF.giFlags[RNGOALS];        // verified above. 
        bHasWFE   = DMF.giFlags[RWFEFIELD] > RABSENT; 

        if ((optEditor==null) || (rayEditor==null))
          return false; // SNH graying.

        if ((ngroups<1) || (onfields<1) || (nrays<1) || (rnfields<1))
          return false; // SNH graying. 

        if ((ngoals < 1) && !bHasWFE)
          return false; // SNH by logic in InOut or Auto. 

        // bDone = new boolean[nrays+1]; 
        goalAttrib = new int[MAXGOALS]; 
        goalField = new int[MAXGOALS]; 
        goalValue = new double[MAXGOALS][nrays+1]; // dense in goals, sparse in rays
        resid  = new double[MAXGOALS*nrays];       // dense array of residuals 
        return true; 
    } // end of bSetup()



    private static void vSetupGoals()
    // The only place to get goal data is from the rayEditor table. 
    // Store goal values locally so that averaging can be performed.
    // Don't forget to update the table, if averaging is done. 
    {
        int igoal = 0; 
        for (int f=0; f<rnfields; f++)
        {
            int op = REJIF.rF2I[f]; 
            if (op >= RGOAL)                       // allows WFE
            {
                if (igoal >=MAXGOALS)
                  igoal = MAXGOALS-1;              // graceful overflow

                goalAttrib[igoal] = op % 100;      // WFE has no goals.
                goalField[igoal] = f; 
                for (int kray=1; kray<=nrays; kray++)
                {
                   int row = kray + 2; 
                   goalValue[igoal][kray] = rayEditor.getFieldDouble(f, row);
                }
                igoal++; 
            }
        }
        return; 
    } // end of vSetupGoals(). 



    private static double getRay(int kray, int iattrib)
    {
        if ((iattrib>=RX) && (iattrib<RNATTRIBS))
        {
            double d = RT13.dGetRay(kray, ngroups, iattrib);
            return d; 
        }
        return -0.0;
    }



    private static void vGangGoals()
    // Discovers and averages ray data among ganged goals.
    // Assumes nrays, ngoals, goalValue[], goalField[] initialized.
    // Assumes RT13.dRays[][][] has been evaluated.
    // Assumes ngoals>0, ngood>0, npts>0. 
    {
        boolean bLookedAt[] = new boolean[nrays+1]; 
        for (int igoal=0; igoal<ngoals; igoal++)
        {
            int f = goalField[igoal]; 
            for (int kray=1; kray<=nrays; kray++)
              bLookedAt[kray] = !RT13.bGoodRay[kray];  

            for (int ktop=1; ktop<=nrays; ktop++) // search for top ray this tag
            {
                if (bLookedAt[ktop])              // skip. 
                  continue; 
                char tag = rayEditor.getTag(f, ktop+2); 
                if ((tag<'A') || (tag>'z'))       // not tagged or averaged
                {
                    bLookedAt[ktop] = true;       // skip
                    continue; 
                }
 
                // Found a good ray with a valid goal group tag!
                // So, search for all cohorts here and below, to average.
                
                int nCohorts = 0;   // count will include ktop itself. 
                double value = 0.0; // average will include ktop itself. 
                for (int k=ktop; k<=nrays; k++)
                {
                    if (RT13.bGoodRay[k] 
                    && !bLookedAt[k] 
                    && (tag==rayEditor.getTag(f,k+2)))
                    {
                        nCohorts++; 
                        double term = getRay(k, goalAttrib[igoal]); 
                        value += term; 
                        bLookedAt[k] = true;  
                    }
                }
                if (nCohorts>0)  // should always be true
                {
                    value = value/nCohorts; // average ray value
                    for (int k=1; k<=nrays; k++)  
                      if (tag == rayEditor.getTag(f,k+2))
                      {
                          rayEditor.putFieldDouble(f, k+2, value);
                          goalValue[igoal][k] = value; 
                          bLookedAt[k] = true;
                      }
                }
            } // each group's ktop
        } // each goal
    } // end of vGangGoals().

    
} //--------end of Comparo----------------