package com.stellarsoftware.beam;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


/** AutoAdj.java
  *
  * class LMadj is at the bottom of this file. 
  * 
  * @author: M.Lampton (c) 2003..2006 STELLAR SOFTWARE all rights reserved.
  */
class AutoAdj
{
    AdjHost myHost = new AdjHost(); // does everything.
}


class AdjHost implements B4constants
{
    //----------parallel with InOut-------------
    private OEJIF optEditor = null; 
    private REJIF rayEditor = null; 
    private int nsurfs=0, ngroups=0, nrays=0, onfields=0, rnfields=0;
    private int npts, nadj=0, onadj=0, rnadj=0, ngood=0, autongoals=0; 
    private boolean bWFE=false; 

    //-------------unique to Auto---------------
    private LMadj myLM = null; 
    private javax.swing.Timer myTimer; 
    private int nLabels = 8; 
    private JLabel jL[] = new JLabel[nLabels]; 
    private JButton jbDone; 
    private JDialog jd = null; 
    private int hostiter=0;
    private int maxiter=100;          // see UO_AUTO_MAXIT below
    private double tol = 1E-12;       // see UO_AUTO_TOL below
    private double dUserStep = 1E-6;  // see UO_AUTO_STEP below
    private boolean bComplete = false; 
    private double dDelta[] = new double[MAXADJ]; 
    private int istatus; 
    private double sos, rms;          // from Comparo.resid[]
    private double jac[][];           // [npts][nadj]; dense




    public AdjHost() // constructor; performs the entire task. 
    {
        optEditor = DMF.oejif;
        rayEditor = DMF.rejif;
        if ((optEditor==null) || (rayEditor==null))
          return;          // SNH thanks to graying.
        DMF.sAutoErr = "";    // no errors yet

        optEditor.doStashForUndo(); 
        rayEditor.doStashForUndo(); 
        
        nsurfs = DMF.giFlags[ONSURFS]; 
        ngroups = DMF.giFlags[ONGROUPS]; 
        onfields = DMF.giFlags[ONFIELDS];  // fields per optic.
        nrays = DMF.giFlags[RNRAYS]; 
        rnfields = DMF.giFlags[RNFIELDS];  // fields per ray.
        if ((ngroups<1) || (onfields<1) || (nrays<1) || (rnfields<1))
          return;          // SNH graying. 
          
        bWFE = DMF.giFlags[RWFEFIELD] > RABSENT; 
        autongoals = DMF.giFlags[RNGOALS] + (bWFE ? 1 : 0); 
        
        DMF.bringEJIFtoFront(rayEditor);

        //-----autongoals includes 1 for WFE if present-----------
        if (autongoals < 1)
        {
            JOptionPane.showMessageDialog(optEditor, "Auto: no ray goals or WFE");
            return; 
        }
        onadj = DMF.giFlags[ONADJ];  // set by OEJIF during parse().
        rnadj = DMF.giFlags[RNADJ];  // set by REJIF during parse().
        nadj = onadj + rnadj;        // total adjustables. 
        if (nadj < 1)
        {
            JOptionPane.showMessageDialog(optEditor, "Auto: no adjustables"); 
            return; 
        }

        dUserStep = U.suckDouble(DMF.reg.getuo(UO_AUTO, 0)); 
        dUserStep = Math.max(1E-12, Math.min(dUserStep, 1.0));
        hostiter = 0; 
        maxiter = U.suckInt(DMF.reg.getuo(UO_AUTO, 1)); 
        maxiter = Math.max(1, Math.min(maxiter, 1000)); 
        tol = U.suckDouble(DMF.reg.getuo(UO_AUTO, 2)); 
        tol = Math.max(1E-20, Math.min(tol, 1.0)); 

        //------count the initially good rays----------

        ngood = RT13.iBuildRays(true);  // try all rays, initially.
        if (ngood < 1) 
        {
            JOptionPane.showMessageDialog(optEditor, "Auto: no good rays");
            return; 
        } 
        npts = ngood * autongoals; // includes WFE at line 68.
        if (npts < 1)
          return;  // SNH
        vUpdateOpticsTable(); 

        //----set up the deltas for both optics & ray adjustables----

        for (int iadj=0; iadj<nadj; iadj++)
        {
            dDelta[iadj] = getDelta(iadj); 
        }


        //----turn off blinker parsing of surfs[] raystarts[] etc---------

        DMF.bAutoBusy = true; 

        //-----begin the comparison process-------

        Comparo.doResiduals(); // npts is restricted to initial good rays in RT13. 

        int nptest = Comparo.iGetNPTS();
        if (nptest != npts)  // SNH. 
        {
            JOptionPane.showMessageDialog(optEditor, "Auto: re-run InOut.");
            return; 
        } 
        sos = Comparo.dGetSOS(); 
        rms = Comparo.dGetRMS(); 

        //----set up for iterative improvement---------

        jac = new double[npts][nadj];
        vPostEmptyDialog(); 
        myLM = new LMadj(this, tol, nadj, npts); 
        vStartTimer();         // performs the adjustment iterations
    }


    private double getDelta(int iadj)
    // For each adjustable's attribute to get a derivative step.
    // Assumes predetermined editors and dUserStep.
    // Be sure RT13.iBuildRays() has run first!
    {
        if ((optEditor==null) || (rayEditor==null) || (iadj>=nadj))
        {
            return 1E-6;  // SNH graying.
        }
        if (ngood < 1)
        {
            return 1E-6;  // SNH pretesting. 
        }

        double dOsize = optEditor.getOsize(); 

        if (iadj < onadj)  // optics adjustment
        {  
            int iatt = optEditor.getAdjAttrib(iadj); 
            if (iatt >= OZ00)  // Zernike polynomial
              return 0.01*dUserStep; 

            double r = 0.0; 
            int j = optEditor.getAdjSurf(iadj);
            if ((j>0) && (j<=nsurfs))
            {
                int g = RT13.group[j]; 
                if ((g>0) && (g<=ngroups))
                {
                    for (int kray=1; kray<=nrays; kray++)
                      if (RT13.bGoodRay[kray])
                      {
                          double rx = RT13.dGetRay(kray, g, RTXL); 
                          double ry = RT13.dGetRay(kray, g, RTYL); 
                          r = Math.max(r, Math.sqrt(rx*rx+ry*ry)); 
                      }
                }
            }
            if (r < 1E-6)
              r = 0.5*dOsize; 

            switch (iatt)
            {
               case OX:
               case OY:
               case OZ:    return dUserStep * dOsize;
               case OTILT:
               case OPITCH:
               case OROLL:  return 60 * dUserStep;
               case OCURVE:
               case OCURVX:  return dUserStep / r; 
               case OA2:     return dUserStep / (2*r);
               case OA3:     return dUserStep / (3*Math.pow(r,2));
               case OA4:     return dUserStep / (4*Math.pow(r,3));
               case OA5:     return dUserStep / (5*Math.pow(r,4));
               case OA6:     return dUserStep / (6*Math.pow(r,5)); 
               case OA7:     return dUserStep / (7*Math.pow(r,6));
               case OA8:     return dUserStep / (8*Math.pow(r,7)); 
               case OA9:     return dUserStep / (9*Math.pow(r,8));
               case OA10:    return dUserStep / (10*Math.pow(r,9));
               case OA11:    return dUserStep / (11*Math.pow(r,10)); 
               case OA12:    return dUserStep / (12*Math.pow(r,11));
               case OA13:    return dUserStep / (13*Math.pow(r,12));
               case OA14:    return dUserStep / (14*Math.pow(r,13));
               default: return dUserStep;
            }
        }
        else   // ray adjustment
        {
            int iatt = rayEditor.getAdjAttrib(iadj-onadj);
            switch (iatt)
            {
               case RX:
               case RY:
               case RZ:  return dUserStep * dOsize;
               case RU:
               case RV:
               case RW:  return dUserStep;
               default:  return dUserStep;
            }
        }
    }




    ///////////////// GUI interfaces //////////////////

    private void vPostEmptyDialog()
    {
        JFrame jf = DMF.getJFrame(); 
        jd = new JDialog(jf, "AutoAdjust", false); 
        jd.addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(WindowEvent we)
            {
               shutdown(); 
            }
        }); 
        Container cp = jd.getContentPane(); 
        cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS)); 

        for (int i=0; i<nLabels; i++)
        {
            jL[i] = new JLabel(); 
            jL[i].setAlignmentX(Component.CENTER_ALIGNMENT); 
        }

        jbDone = new JButton("Stop");
        jbDone.setAlignmentX(Component.CENTER_ALIGNMENT); 
        jbDone.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent aa)
            {
                shutdown(); 
            }
        }); 

        for (int i=0; i<nLabels; i++)
          cp.add(jL[i]); 

        //------empty space beneath the JLabels-----
        cp.add(Box.createRigidArea(new Dimension(195,10))); 
        cp.add(jbDone); 
        jd.setVisible(true); 
        jd.toFront(); 
        vUpdateDialog(); 
    }


    private void vUpdateDialog()
    {
        jL[0].setText("Iteration = " + hostiter); 
        jL[1].setText("RMS Average =  "+U.fwe(rms)); 
        jL[2].setText(""); 
        if (autongoals > 1)
        {
             if (bWFE)
               jL[2].setText("Caution: WFE + other goals"); 
             else if (autongoals==2)
               jL[2].setText("RSS Radius = "+U.fwe(ROOT2*rms)); 
        }
        jL[3].setText("Nrays = "+U.fwi(ngood,8).trim());
        jL[4].setText("Ngoals = "+U.fwi(autongoals, 1));
        jL[5].setText("Nterms = "+U.fwi(npts,8).trim()); 
        jL[6].setText("Nadj = "+U.fwi(nadj,8).trim()); 
        if (istatus==DOWNITER)
          jL[7].setText("Running..."); 
        else 
        if (istatus==BADITER)
        {
            if (DMF.sAutoErr.equals(""))   // no previous message
              jL[7].setText("Ray risk. Stopping.");
            else                           // retain previous message
              jL[7].setText(DMF.sAutoErr); 
        }
        else
          jL[7].setText(""); 

        //----finally set appropriate size------
        jd.pack(); 
    }


    private void vStartTimer()
    {
        DMF.bAutoBusy = true;  
        myTimer = new javax.swing.Timer(500, doTimerTask); 
        hostiter = 0; 
        myTimer.start(); 
        bComplete = false; 
    }


    private void vUpdateOpticsTable()
    // Places current RT13 data into .OPT and displays it.
    // dNudge() keeps OSHAPE and OASPHER synchronized, also ORAD and OCURVE.
    {
        for (int iadj=0; iadj<onadj; iadj++)
        {
            int surf = optEditor.getAdjSurf(iadj); 
            int row = 2+surf; 
            int attr = optEditor.getAdjAttrib(iadj); 
            int field = optEditor.getAdjField(iadj); 
            if ((surf>0) && (attr>=0))
              optEditor.putFieldDouble(field, row, RT13.surfs[surf][attr]); 
        }
        optEditor.repaint(); 
        optEditor.toFront(); 
    }


    private void vUpdateRayTable()
    // Assumed globals: nrays, nfields, ngoals, rayEditor
    // Be sure to run iSetupGoals() first, before running this.
    // This updates trace outputs, but not nudges to raystarts[][].
    {
        // DMF.bringEJIFtoFront(rayEditor);
        // ^^unnecessary but no known harm. 
        for (int kray=1; kray<=nrays; kray++) // ray loop
        {
            int row = kray+2; 
            for (int f=0; f<rnfields; f++) // field loop
            {
                // int op = rayEditor.rF2I[f];  ?? warning from Xlint
                int op = REJIF.rF2I[f];         // this works: 8 Oct 2014
                if (op == RNOTE)  // ray note message here....
                {
                    String s = sResults[RT13.getStatus(kray)] 
                       + U.fwi(RT13.getHowfarLoop(kray),2);
                    rayEditor.putField(f, row, s); 
                }

                if (op == RDEBUG) // debugger message here....
                  rayEditor.putField(f, row, RT13.bGoodRay[kray] ? "OK" : "NG"); 


                if (op >= RGOAL) // Comparo updates floating goals, not Auto.
                  continue; 

                if (op >= 100)   // output table data are wanted here...
                {
                    int jsurf = RT13.getGroupNum(op); // handles "final"
                    int iattr = RT13.getAttrNum(op); 
                    if ((iattr>=0) && (iattr<RNATTRIBS) && (jsurf>0))
                    {
                        if (RT13.bGoodRay[kray])
                        {
                            double d = RT13.dGetRay(kray, jsurf, iattr); 
                            rayEditor.putFieldDouble(f, row, d);  
                        }
                        else
                          rayEditor.putBlank(f, row); 
                    }
                }
            } // done with writing all fields for this ray. 
        } // done with all rays. 

        rayEditor.repaint(); 
        rayEditor.toFront(); 

    } //-----end of vUpdateRayTable()----------




    private void vUpdateLayout()
    {
        GJIF g = DMF.getLayoutGJIF(); 
        if (g == null)
          return; 
        g.getGPanel().requestNewArtwork(); 
        g.toFront(); 
        g.repaint(); 
    }




    ActionListener doTimerTask = new ActionListener()
    // Responds to timer tick with one LM iteration & update. 
    {
        public void actionPerformed(ActionEvent ae)
        {
            if (!bComplete)
            {
                hostiter++; 
                if (hostiter >=maxiter)
                  istatus = MAXITER;
                else
                  istatus = myLM.iLMiter(); 
                bComplete = (istatus==BADITER) 
                         || (istatus==LEVELITER) 
                         || (istatus==MAXITER); 
           
                vUpdateDialog();
                vUpdateRayTable(); 
                vUpdateOpticsTable();   
                vUpdateLayout();
            }
            if (bComplete)
            {
                myTimer.stop(); 
                jbDone.setText("Done"); 
                DMF.bAutoBusy = false;  // allow future parsings. 
            }
        } 
    };

    private void shutdown()
    {
        if (myTimer != null)
        {
            myTimer.stop(); 
            myTimer = null;
        } 
        if (jd != null)
        {
            jd.dispose();      // does it all
            // jd.setVisible(false);  
            // jd = null;
        } 
        //---with dialog gone, can resync front & selected----
        DMF.bringEJIFtoFront(optEditor);
        DMF.bAutoBusy = false;  // allow future parsings. 
    } 


    //----These five LM callbacks must be furnished by any LMhost-------


    double dPerformResid()
    // Combines trace, goal adjustment and resid[] construction.
    // Employed by LM and by dBuildJacobian() via dNudge(). 
    // Returns sum-of-squares. 
    {
        int nrays = RT13.iBuildRays(false);   // run only good rays
        if (nrays < ngood)
        {
            return BIGVAL; // special error code
        }
        Comparo.doResiduals(); 
        sos = Comparo.dGetSOS(); 
        rms = Comparo.dGetRMS(); 
        return sos;
    }


    double dNudge(double dp[])  
    // Called by LM to modify parms. 
    // Dimension of dp[] is total nadj.
    // Splits total adjustable vector into Optics and Ray portions. 
    // This cannot fail, but passes through failures in dPerformResid() 
    {
        double dpo[] = new double[onadj]; 
        double dpr[] = new double[rnadj]; 
        int i; 
        for (i=0; i<onadj; i++)
          dpo[i] = dp[i]; 
        nudgeOpt(dpo); 
        for (i=0; i<rnadj; i++)
          dpr[i] = dp[onadj + i]; 
        nudgeRay(dpr); 
        return dPerformResid();
    }


    void nudgeOpt(double dp[])
    {
        boolean bAngle = false; 
        for (int iadj=0; iadj<onadj; iadj++)
        {
            int surf = optEditor.getAdjSurf(iadj); 
            int attr = optEditor.getAdjAttrib(iadj); 
            if ((attr==OTILT) ||(attr==OPITCH) || (attr==OROLL))
              bAngle = true; 
            int field = optEditor.getAdjField(iadj); 

            //---modify the master adjustable----------------

            RT13.surfs[surf][attr] += dp[iadj]; 

            optEditor.putFieldDouble(field, surf+2, RT13.surfs[surf][attr]); 

            //-----modify its slaves and antislaves----------

            int nSlaves = optEditor.getSlaves(iadj).size(); 
            for (int i=0; i<nSlaves; i++)
            {
                Integer jSlave = optEditor.getSlaves(iadj).get(i);
                int j = jSlave.intValue(); 
                double dSign = (j > 0) ? +1.0 : -1.0; 
                j = Math.abs(j); 
                RT13.surfs[j][attr] += dSign * dp[iadj]; 
                optEditor.putFieldDouble(field, j+2, RT13.surfs[j][attr]); 
            }
            if (attr==OSHAPE)
              RT13.surfs[surf][OASPHER] += dp[iadj];
        }
        if (bAngle)
          RT13.setEulers();
        optEditor.repaint(); 
    }


    void nudgeRay(double dp[])
    {
        for (int iadj=0; iadj<rnadj; iadj++)
        {
            int kray = rayEditor.getAdjRay(iadj); 
            int attr = rayEditor.getAdjAttrib(iadj); 
            int field = rayEditor.getAdjField(iadj); 

            //---nudge the master adjustable ray----------------
            RT13.raystarts[kray][attr] += dp[iadj]; 
            rayEditor.putFieldDouble(field, kray+2, RT13.raystarts[kray][attr]); 

            //-----nudge any slave rays--------------------------

            int nSlaves = rayEditor.getSlaves(iadj).size(); 
            for (int i=0; i<nSlaves; i++)
            {
                Integer jSlave = rayEditor.getSlaves(iadj).get(i);
                int k = jSlave.intValue(); 
                double dSign = (k > 0) ? +1.0 : -1.0; 
                k = Math.abs(k); 
                RT13.raystarts[k][attr] += dSign * dp[iadj]; 
                rayEditor.putFieldDouble(field, k+2, RT13.raystarts[k][attr]); 
            }
        }
        rayEditor.repaint(); 
    }


    boolean bBuildJacobian()
    // Uses current vector parms[].
    // If current parms[] is bad, returns false.  
    // False should trigger an explanation & shutdown. 
    // Preliminary test of jac.length: SNH but see A148.
    // Called by LM.bLMiter().
    // All failures return false and are triggered within dNudge.
    {
        double delta[] = new double[nadj];
        double d=0; 

        if (jac.length != npts)  // SNH!
        {
            return false; 
        }

        for (int j=0; j<nadj; j++)
        {
            for (int k=0; k<nadj; k++)  // one component at a time.
              delta[k] = (k==j) ? dDelta[j] : 0.0;

            d = dNudge(delta); // resid at pplus
            if (d==BIGVAL)
            {
                return false;  
            }
            for (int i=0; i<npts; i++)
              jac[i][j] = dFetchResid(i);

            for (int k=0; k<nadj; k++)
              delta[k] = (k==j) ? -2.0*dDelta[j] : 0.0;

            d = dNudge(delta); // resid at pminus
            if (d==BIGVAL)
            {
                return false;  
            }

            for (int i=0; i<npts; i++)
              jac[i][j] -= dFetchResid(i);

            for (int i=0; i<npts; i++)
              jac[i][j] /= (2.0*dDelta[j]);

            for (int k=0; k<nadj; k++)
              delta[k] = (k==j) ? dDelta[j] : 0.0;

            d = dNudge(delta);  // return this parm to starting value.
            if (d==BIGVAL)
            {
                return false;  
            }
        }
        return true; 
    }


    double dFetchJac(int i, int j)
    // Returns one element of the Jacobian matrix.
    // i=datapoint, j=whichparm.
    {
        return jac[i][j]; 
    }

    double dFetchResid(int i)
    // Returns one element of the array resid[].
    {
        return Comparo.resid[i]; 
    }
}  //------------end of class AdjHost---------------------------







/**
  *  class LMadj   Levenberg Marquardt w/ Lampton improvements
  *  M.Lampton, 1997 Computers In Physics v.11 #10 110-115.
  *
  *  Constructor is used to set up all parms including host for callback.
  *  Sole public method is bLMiter() performs one iteration.
  *  Arrays parms[], resid[], jac[][] are unknown here.
  *  Callback method uses CallerID to access five host methods:
  *
  *    double dPerformResid();    Returns sos, or BIGVAL if parms failed.
  *    double dNudge(dp);         Moves parms, builds resid[], returns sos or BIGVAL
  *    boolean bBuildJacobian();  false if parms failed.
  *    double dFetchJac(i,j);     cannot fail.
  *    double dFetchResid(i);     cannot fail. 
  *
  *  Exit leaves host with parms[] optimized through its sequence of nudges. 
  *
  *  @author: M.Lampton (c) 2005 Stellar Software
  */
class LMadj implements B4constants  
{
    private final double LMBOOST    =  2.0;     // damping multiplier per bad step
    private final double LMSHRINK   = 0.10;     // damping multiplier per good step
    private final double LAMBDAZERO = 100.0;    // initial damping
    private final double LAMBDAMAX  = 1E6;      // max damping

    private int ninner=0, nouter=0;             // loop counters
    private double sos, sosinit, lambda;        // local

    private AdjHost myH = null;      // overwritten by constructor
    private double  lmtol = 1E-6;    // overwritten by constructor
    private int     lmiter = 100;    // overwritten by constructor
    private int     nparms = 0;      // overwritten by constructor
    private int     npts = 0;        // overwritten by constructor

    private double[] delta;          // local
    private double[] beta;           // local
    private double[][] alpha;        // local
    private double[][] amatrix;      // local 

    private OEJIF optEditor = null;  // DEBUGAUTO only

    public LMadj(AdjHost gH, double gtol, int gnparms, int gnpts)
    // Constructor sets up private fields, including host for callbacks.
    {
        myH = gH;
        lmtol = gtol; 
        nparms = gnparms;
        npts = gnpts;  
        ninner = nouter = 0; 
        delta = new double[nparms];
        beta = new double[nparms];
        alpha = new double[nparms][nparms]; 
        amatrix = new double[nparms][nparms];
        lambda = LAMBDAZERO; 
        optEditor = DMF.oejif; // DEBUGAUTO only
    }

    int iLMiter( )
    // Called repeatedly by LMhost's doTimerTask() to perform each major LM iteration. 
    // Returns BADITER to shut down ray failed;
    // Returns DOWNITER if iteration went OK, more needed;
    // Returns LEVELITER if iteration went OK, all done. 
    // Globals: npts, nparms, myH. 
    // Ref: M.Lampton, Computers in Physics v.11 pp.110-115 1997.
    {
        nouter++; 
        ninner=0; 
        sosinit = myH.dPerformResid();
        if (sosinit==BIGVAL)
        {
            return BADITER; // done; no further progress via iterations.  
        }

        if (!myH.bBuildJacobian())
        {
            return BADITER;
        }

        for (int k=0; k<nparms; k++)      // get downhill gradient beta
        {
            beta[k] = 0.0;
            for (int i=0; i<npts; i++)
              beta[k] -= myH.dFetchResid(i)*myH.dFetchJac(i,k);
        }
        for (int k=0; k<nparms; k++)      // get curvature matrix alpha
          for (int j=0; j<nparms; j++)
          {
              alpha[j][k] = 0.0;
              for (int i=0; i<npts; i++)
                alpha[j][k] += myH.dFetchJac(i,j)*myH.dFetchJac(i,k);
          }

        double rise = 0; 
        do  /// inner damping loop searches for one downhill step
        {
            ninner++;                        // local diagnostic only. Any max??
            for (int k=0; k<nparms; k++)     // copy and damp it
              for (int j=0; j<nparms; j++)
                amatrix[j][k] = alpha[j][k] + ((j==k) ? lambda : 0.0);
            gaussj(amatrix, nparms);         // invert
            for (int k=0; k<nparms; k++)     // compute delta[]
            {
                delta[k] = 0.0; 
                for (int j=0; j<nparms; j++)
                  delta[k] += amatrix[j][k]*beta[j];
            }

            sos = myH.dNudge(delta);         // try it out.
            rise = (sos-sosinit)/(1+sosinit);

            //---four possibilities and three exits---------

            if (rise <= -lmtol)              // good step!
            {
               lambda *= LMSHRINK;           // shrink lambda
               return DOWNITER;                // leave inner loop
            }

            if (rise <= 0.0)                 // good step but level.
            {
               lambda *= LMSHRINK;           // shrink lambda
               return LEVELITER;             // leave inner loop
            }

            for (int k=0; k<nparms; k++)     // reverse course!
               delta[k] *= -1.0;
            myH.dNudge(delta);               // sosprev is still OK

            if (rise < lmtol)                // finished but keep prev parms
            {
               return LEVELITER;             // leave inner loop
            }
            lambda *= LMBOOST;               // else apply more damping.
        } while (lambda<LAMBDAMAX);

        return BADITER; 
    }



    private double gaussj( double[][] a, int N )
    // inverts the double array a[N][N] by Gauss-Jordan method
    {
        double det = 1.0, big, save;
        int i,j,k,L;
        int[] ik = new int[100];
        int[] jk = new int[100];
        for (k=0; k<N; k++)
        {
            big = 0.0;
            for (i=k; i<N; i++)
              for (j=k; j<N; j++)          // find biggest element
                if (Math.abs(big) <= Math.abs(a[i][j]))
                {
                    big = a[i][j];
                    ik[k] = i;
                    jk[k] = j;
                }
            if (big == 0.0) return 0.0;
            i = ik[k];
            if (i>k)
              for (j=0; j<N; j++)          // exchange rows
              {
                  save = a[k][j];
                  a[k][j] = a[i][j];
                  a[i][j] = -save;
              }
            j = jk[k];
            if (j>k)
              for (i=0; i<N; i++)
              {
                  save = a[i][k];
                  a[i][k] = a[i][j];
                  a[i][j] = -save;
              }
            for (i=0; i<N; i++)            // build the inverse
              if (i != k)
                a[i][k] = -a[i][k]/big;
            for (i=0; i<N; i++)
              for (j=0; j<N; j++)
                if ((i != k) && (j != k))
                  a[i][j] += a[i][k]*a[k][j];
            for (j=0; j<N; j++)
              if (j != k)
                a[k][j] /= big;
            a[k][k] = 1.0/big;
            det *= big;                    // bomb point
        }                                  // end k loop
        for (L=0; L<N; L++)
        {
            k = N-L-1;
            j = ik[k];
            if (j>k)
              for (i=0; i<N; i++)
              {
                  save = a[i][k];
                  a[i][k] = -a[i][j];
                  a[i][j] = save;
              }
            i = jk[k];
            if (i>k)
              for (j=0; j<N; j++)
              {
                  save = a[k][j];
                  a[k][j] = -a[i][j];
                  a[i][j] = save;
              }
        }
        return det;
    }
}
