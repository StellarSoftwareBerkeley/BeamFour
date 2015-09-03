package com.stellarsoftware.beam;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


/** AutoRay.java
  *
  *  A174: improved results dialog. 
  *
  * Adjusts one or two ray starts to deliver a distant pupil
  * Does ray tracing by calls to RT13.bRunOneRay().
  * Avoids use of Comparo.
  *
  * This version uses no timer, runs full speed, tight loop.
  * No i/o during the loop, only at the end. 
  *
  * Work is organized by RayHost.  After setup, it steps through each ray:
  *   tests ray initially; 
  *   initializes resid[] and sos; 
  *   starts new LMray instance; 
  *   OUTER LOOP iteratively calls LMray.iLMiter() as long as downward sos trend,
  *      but will not exceed MAXITER. 
  * When all rays are done, calls for InOut to display the new .RAY table. 
  *
  * LMray performs its initialization, then for each iLMiter() call:
  *   gets current resid[] and sos; 
  *   gets current Jacobian;
  *   evaluates current undamped curvature matrix.  
  *   if BIGVAL ray failure happens, requests an exit. 
  *   Local INNER LOOP then...
  *      applies some damping, tries a step;
  *      if downhill, retains step, reduces damping, exits to OUTER LOOP;
  *      if uphill, reverses step, raises damping, and tries again; 
  *      if level, exits to host OUTER LOOP; 
  *      if lambda>LAMBDAMAX, exits to host OUTER LOOP.
  * 
  *
  *  RayHost supplies bBuildJacobian() which calls dNudge(), which calls dPerformResid().
  *
  *
  * 
  * @author: M.Lampton (c) 2012 STELLAR SOFTWARE all rights reserved.
  */
class AutoRay
{
    RayHost myhost = new RayHost();       // does everything.
}


class RayHost implements B4constants
{
    //----------parallel with InOut-------------
    private OEJIF optEditor = null; 
    private REJIF rayEditor = null; 


    //-------------unique to Auto---------------
    private LMray  myLM     = null; 
    private int    MAXITER  = 10; 
    private double LMTOL    = 1E-12;   
    private int gkray;                    // classwide so LM callbacks can use it.

    private int iLMstatus;  
    private int nsurfs      = 0;
    private int nrays       = 0;
    private int nadj        = 0;
    private int ngoals      = 0; 
    private int navail      = 0;
    private int nfinishes   = 0;
    private int nwentbad    = 0;
    
    private double drays[]  = {0, 0};     // ray values to compare
    private double dgoals[] = {0, 0};     // goal values, differ for each ray    
    private int    igoals[] = {-1,-1};    // goal attribs, same for all rays
    private int    fgoals[] = {-1,-1};    // goal fields, same for all rays

    private double jac[][]  = new double[2][2]; 
    private double resid[]  = {0, 0}; 
    private double dDelta[] = {1E-6, 1E-6}; 
    private boolean badray  = false;      // flag when a ray goes sour
    private JDialog jd      = null;       // to post results when done. 
    
    

    public RayHost()                      // constructor does everything.
    {
        optEditor = DMF.oejif;
        rayEditor = DMF.rejif;
        if ((optEditor==null) || (rayEditor==null))
          return;                         // SNH thanks to graying.
          
        rayEditor.doStashForUndo(); 
        
        nsurfs   = DMF.giFlags[ONSURFS]; 
        nrays    = DMF.giFlags[RNRAYS]; 
        nadj     = DMF.giFlags[RNRAYADJ]; // for AutoAdj: set by REJIF during parse().
        // nadj     = Math.min(nadj, 2);  // unnecessary with new REJIF.
        if (nadj < 1)
        {
            JOptionPane.showMessageDialog(rayEditor, "AutoRay: no adjustables"); 
            return; 
        }
        if (nadj > 2)
        {
            JOptionPane.showMessageDialog(rayEditor, "AutoRay: > 2 adjustables"); 
            return; 
        }
        ngoals = DMF.giFlags[RNGOALS]; 
        if (ngoals < 1)
        {
            JOptionPane.showMessageDialog(rayEditor, "AutoRay: no ray goals");
            return; 
        }
        igoals[0] = DMF.giFlags[RAYGOALATT0] % 100;
        igoals[1] = DMF.giFlags[RAYGOALATT1] % 100;   // -1 means absent
        fgoals[0] = DMF.giFlags[RAYGOALFIELD0]; 
        fgoals[1] = DMF.giFlags[RAYGOALFIELD1]; 

        //------Verify starting rays----------

        navail = RT13.iBuildRays(true);
        if (navail < 1) 
        {
            JOptionPane.showMessageDialog(optEditor, "AutoRay: no good rays");
            return; 
        } 

        //-----do each ray individually--------------

        int ngoodstarts = 0;    // local count; should equal navail. 
        nfinishes = 0; 
        nwentbad = 0;  
        
        for (gkray=1; gkray<=nrays; gkray++)
        {
            if(!RT13.bRunOneRay(gkray))
              continue;
            ngoodstarts++; 
            badray = false;    // so far, so good.
            
            // initialize resid[] and sos; 
            double sos = 0; 
            for (int i=0; i<ngoals; i++)
            {
                dgoals[i] = rayEditor.getFieldDouble(fgoals[i], gkray+2); 
                drays[i] = RT13.dGetRay(gkray, nsurfs, igoals[i]); 
                resid[i] = drays[i] - dgoals[i]; 
                sos += U.sqr(resid[i]); 
            }

            // now kick off LM process
            myLM = new LMray(this, LMTOL, nadj, ngoals); 
            boolean bDone = false; 
            int iter = 0; 
            while (!bDone)
            {
                 iter++; 
                 int iStatus = myLM.iLMiter(); 
                 badray = badray || (iStatus==BADITER); 
                 bDone = badray || (iStatus==LEVELITER) || (iter>MAXITER); 
            }
            if (badray)
               nwentbad++; 
            else
              nfinishes++; 
        }
        vUpdateRayStarts();        // post ray start adjustments to .RAY table
        InOut myIO = new InOut();  // post ray trace results to .RAY table
        vPostSummary();            // show local AutoRay result summary. 
    }
    
    
    private void vPostSummary()
    {
        //----prepare the results-------------
        
        Comparo.doResiduals();   
        int nptest = Comparo.iGetNPTS(); 
        double sos = Comparo.dGetSOS(); 
        double rms = Comparo.dGetRMS(); 
        
        //-----prepare the dialog-------------
        
        JFrame jf = DMF.getJFrame(); 
        if (jd == null)
          jd = new JDialog(jf, "AutoRay", false); 
        jd.addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(WindowEvent we)
            {
                jd.dispose();  
            }
        }); 
        
        Container cp = jd.getContentPane(); 
        cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS)); 
        int nLabels = 7; 
        JLabel jL[] = new JLabel[nLabels]; 
        for (int i=0; i<nLabels; i++)
        {
            jL[i] = new JLabel(); 
            jL[i].setAlignmentX(Component.CENTER_ALIGNMENT); 
        }        
        
        jL[0].setText("RMS 1D average = "+U.fwe(rms));  
        jL[1].setText("Nrays = "+nrays); 
        jL[2].setText("Ngoals = "+ngoals); 
        jL[4].setText("Nstarts = "+navail); 
        jL[5].setText("Nadjusted = "+nfinishes); 
        jL[6].setText("Nfailed = "+nwentbad); 
        
        for (int i=0; i<nLabels; i++)
          cp.add(jL[i]); 
        cp.add(Box.createRigidArea(new Dimension(205, 5))); // pleasant width
        
        JButton jbDone = new JButton("Done"); 
        jbDone.setAlignmentX(Component.CENTER_ALIGNMENT); 
        jbDone.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent aa)
            {
                jd.dispose(); 
            }
        });
        cp.add(jbDone); 
        jd.pack(); 
        jd.setVisible(true); 
        jd.toFront(); 
    }
    

 

    private void vUpdateRayStarts()
    {
        int op0 = DMF.giFlags[RAYADJ0]; 
        int op1 = DMF.giFlags[RAYADJ1]; 
        for (int kray=1; kray<=nrays; kray++)
        {
            for (int op=0; op<=RTWL; op++)  // tabulated ray starts only
            {
                if (op==op0 || op==op1)
                {
                    double d = RT13.raystarts[kray][op]; 
                    int f = REJIF.rI2F[op]; 
                    rayEditor.putFieldDouble(f, kray+2, d);  
                }
            }
        }
        rayEditor.repaint(); 
    } //-----end of vUpdateRayTable()----------


    //----These five LM callbacks must be furnished by any LMhost-------

    double dPerformResid()
    // Ray traces one ray, then evaluates resid[].
    // Employed by LMray and by bBuildJacobian() via dNudge(). 
    // fills in values of resid[]. 
    // Returns sum-of-squares.  One ray is global gkray.
    {
        if (RT13.bRunOneRay(gkray))  // includes fixUVW()
        {   
            double sos = 0; 
            for (int i=0; i<ngoals; i++)
            {
                // re-goaling here is unnecessary: unchanged from setup.
                // dgoals[i] = rayEditor.getFieldDouble(fgoals[i], gkray+2); 
                drays[i] = RT13.dGetRay(gkray, nsurfs, igoals[i]); 
                resid[i] = drays[i] - dgoals[i]; 
                sos += U.sqr(resid[i]); 
            }
            return sos;
        }
        else
        {
            badray = true; 
            return BIGVAL; // special error code
        }
    }


    double dNudge(double dp[])  
    // Called by LMray to modify parms. 
    // Dimension of dp[] is total nadj.
    // This cannot fail, but dPerformResid() can fail rays. 
    {
        for (int iadj=0; iadj<nadj; iadj++)
        {
            int attr = rayEditor.getAdjAttrib(iadj); 
            int field = rayEditor.getAdjField(iadj); 
            RT13.raystarts[gkray][attr] += dp[iadj]; 
        }
        return dPerformResid();
    }


    boolean bBuildJacobian()
    // Uses current vector parms[].
    // If current parms[] is bad, returns false.  
    // False should trigger an explanation. 
    // Called by LMray.iLMiter().
    {
        double delta[] = new double[nadj];
        double d=0; 
        for (int j=0; j<nadj; j++)
        {
            for (int k=0; k<nadj; k++)
              delta[k] = (k==j) ? dDelta[j] : 0.0;

            d = dNudge(delta); // resid at pplus
            if (d==BIGVAL)
            {
                badray = true; 
                return false;  
            }
            for (int i=0; i<ngoals; i++)
              jac[i][j] = dFetchResid(i);

            for (int k=0; k<nadj; k++)
              delta[k] = (k==j) ? -2.0*dDelta[j] : 0.0;

            d = dNudge(delta); // resid at pminus
            if (d==BIGVAL)
            {
                badray = true; 
                return false;  
            }

            for (int i=0; i<ngoals; i++)
              jac[i][j] -= dFetchResid(i); 

            for (int i=0; i<ngoals; i++)
              jac[i][j] /= (2.0*dDelta[j]);

            for (int k=0; k<nadj; k++)
              delta[k] = (k==j) ? dDelta[j] : 0.0;

            d = dNudge(delta);  // back to starting value.

            if (d==BIGVAL)
            {
                badray = true; 
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
        return resid[i]; 
    }
}  //------------end of class RayHost---------------------------






/**
  *  class LMray   Levenberg Marquardt Lampton
  *  M.Lampton, 1997 Computers In Physics v.11 #10 110-115.
  *
  *  Constructor is used to set up all parms including host for callback.
  *  Sole public method is iLMiter() performs one iteration.
  *  Arrays parms[], resid[], jac[][] are unknown here.
  *  Instead, the host must fake these, and provide results....
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
class LMray implements B4constants  
{
    private final double LMBOOST    =  2.0;     // damping increase per bad step
    private final double LMSHRINK   = 0.10;     // damping decrease per good step
    private final double LAMBDAZERO = 0.001;    // initial damping
    private final double LAMBDAMAX  =  1E3;     // max damping

    private int niter = 0;                      // local diagnostic only
    private double sos, sosinit, lambda;        // local diagnostic only

    private RayHost myH = null;    // overwritten by constructor
    private double  lmtol = 1E-6;  // overwritten by constructor
    private int     lmiter = 100;  // overwritten by constructor
    private int     nparms = 0;    // overwritten by constructor
    private int     npts = 0;      // overwritten by constructor

    private double[] delta;        // local
    private double[] beta;         // local
    private double[][] alpha;      // local
    private double[][] amatrix;    // local 

    public LMray(RayHost gH, double gtol, int gnparms, int gnpts)
    // Constructor sets up private fields, including host for callbacks.
    {
        myH = gH;
        lmtol = gtol; 
        nparms = gnparms;
        npts = gnpts;  
        niter = 0; 
        delta = new double[nparms];
        beta = new double[nparms];
        alpha = new double[nparms][nparms]; 
        amatrix = new double[nparms][nparms];
        lambda = LAMBDAZERO; 
    }

    int iLMiter( )
    // Called repeatedly by LMhost to perform each LM iteration. 
    // Returns BADITER to shut down ray failed;
    // Returns DOWNITER if iteration went OK, more needed;
    // Returns LEVELITER if iteration went OK, all done. 
    // Globals: npts, nparms, myH. 
    // Ref: M.Lampton, Computers in Physics v.11 pp.110-115 1997.
    {
        sosinit = myH.dPerformResid();
        if (sosinit==BIGVAL)              // failed ray?
          return BADITER;                 // cannot proceed, request host OUTER LOOP exit.  

        if (!myH.bBuildJacobian())        // ask host for new Jacobian.
          return BADITER;                 // cannot proceed, request host OUTER LOOP exit.

        for (int k=0; k<nparms; k++)      // get downhill gradient beta
        {
            beta[k] = 0.0;
            for (int i=0; i<npts; i++)
              beta[k] -= myH.dFetchResid(i)*myH.dFetchJac(i,k);
        }
        for (int k=0; k<nparms; k++)      // get undamped curvature matrix alpha
          for (int j=0; j<nparms; j++)
          {
              alpha[j][k] = 0.0;
              for (int i=0; i<npts; i++)
                alpha[j][k] += myH.dFetchJac(i,j)*myH.dFetchJac(i,k);
          }

        double rise = 0; 
        do  /// LMinner damping loop searches for one downhill step
        {
            niter++;                         // local diagnostic only
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

            if (rise <= -lmtol)              // good downhill step!
            {
               lambda *= LMSHRINK;           // shrink lambda
               return DOWNITER;              // return to host: request another OUTER LOOP iteration.
            }

            if (rise <= 0.0)                 // good step but level; all done. 
            {
               lambda *= LMSHRINK;           // no need to shrink lambda?
               return LEVELITER;             // return to host: OUTER LOOP exit.
            }

            for (int k=0; k<nparms; k++)     // reverse course!
               delta[k] *= -1.0;
            myH.dNudge(delta);               // sosprev is still OK

            if (rise < lmtol)                // finished but keep prev parms
            {
               return LEVELITER;             // return to host: OUTER LOOP exit.
            }

            lambda *= LMBOOST;               // UPITER:  apply more damping.
        } while (lambda<LAMBDAMAX);          // and stay in this INNER LOOP.

        return BADITER; // exceeded LAMBDAMAX, so request host OUTER LOOP exit.
                        // usual cause is ray damage during minimization. 
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
