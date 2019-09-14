package com.stellarsoftware.beam;

import java.awt.*;           // Action
import java.awt.event.*;     // Event
import javax.swing.*;

/** Random.java
  *
  *  Called only by DMF when user clicks Run:Random.
  *  Requires that Layout, Plot, P1D, or P2D be in front.
  *  Maybe MTF is also OK. 
  *  Menu graying/enabling is done within DMF. 
  *  So, the error of having null gFront or targetPanel Should Never Happen.
  *
  *  Started by instantiating the class. 
  *  Two states:  running shows "Stop", ended shows "Done"
  *  Ended by user clicking "Stop" or by getting enough rays.
  *
  *  Possible activities for Random....
  *    + add random rays to a layout
  *    + add random rays to a plot
  *    + add random rays to a 1D
  *    + add random rays to a 2D
  *    + add random rays to a MTF
  *
  * 
  * 
  *  For each random ray, this timer task requests that the
  *  owner "targetPanel.doRandomRay()" and return True or False. 
  *
  *  The targetPanel gets results by requesting RT13.bRunRandomRay(),
  *  and then uses RT13.dGetRay(0, hsurf, hattr) to gather each point. 
  *  Only the kickoff management and scorekeeping is done here. 
  *
  *
  *  @author: M.Lampton (c) 2003 STELLAR SOFTWARE all rights reserved.
  */
class Random implements B4constants
{
    private GPanel targetPanel = null; 
    private javax.swing.Timer myTimer; 
    private JLabel jlTop, jlMid, jlBot; 
    private JButton jbDone; 
    private JDialog jd = null; 
    private int goodcount = 0, totalcount=0; 
    private int maxtries, maxgood, nBunch; 
    private int nsurfs, nrays, nfields; 
    private boolean bRunning = true; 
    private int iEdits = 0;         // shuts down if editors change.

    public Random() // constructor
    {
        if (DMF.giFlags[STATUS] != GPARSEOK)
          return; 
        iEdits = DMF.nEdits; 
        setupLocals();
        buildDialog(); 
        startBunches();
    } 


    private void setupLocals()
    {
        nsurfs = DMF.giFlags[ONSURFS]; 
        nrays = DMF.giFlags[RNRAYS]; 
        nfields = DMF.giFlags[RNFIELDS]; 
        nBunch = U.suckInt(DMF.reg.getuo(UO_RAND, 0)); 
        nBunch = Math.max(1, Math.min(MAXBUNCH, nBunch)); 
        maxtries = U.suckInt(DMF.reg.getuo(UO_RAND, 1)); 
        maxtries = Math.max(1, maxtries); 
        maxgood = U.suckInt(DMF.reg.getuo(UO_RAND, 2)); 
        maxgood = Math.max(1, maxgood); 
        GJIF gFront = DMF.getFrontGJIF(); // null SNH.
        targetPanel = gFront.getGPanel(); // null SNH.
    }


    private void buildDialog()
    {
        jd = new JDialog(DMF.dmf, "Random", false); 
        jd.addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(WindowEvent we)
            {
               shutdown(); 
            }
        }); 
        Container cp = jd.getContentPane(); 
        cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS)); 
        jlTop = new JLabel("  "); 
        jlTop.setAlignmentX(Component.CENTER_ALIGNMENT); 
        jlMid = new JLabel("  "); 
        jlMid.setAlignmentX(Component.CENTER_ALIGNMENT); 
        jlBot = new JLabel("  "); 
        jlBot.setAlignmentX(Component.CENTER_ALIGNMENT); 
        jbDone = new JButton("Stop");
        jbDone.setAlignmentX(Component.CENTER_ALIGNMENT); 
        jbDone.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent aa)
            {
               shutdown(); 
            }
        }); 
        cp.add(jlTop); 
        cp.add(jlMid); 
        cp.add(jlBot); 
        cp.add(Box.createRigidArea(new Dimension(175,15))); 
        cp.add(jbDone); 
        jd.pack(); 
        jd.setVisible(true); 
        jd.toFront(); 
    }


    private void startBunches()
    {
        goodcount = 0; 
        totalcount = 0; 
        myTimer = new javax.swing.Timer(50, doBunch); 
        bRunning = true; 
        myTimer.start(); 
    }


    ActionListener doBunch = new ActionListener()
    {
        public void actionPerformed(ActionEvent ae)
        {
            if (bRunning)
            {
                for (int i=0; i<nBunch; i++)
                {
                    if (targetPanel.doRandomRay())
                      goodcount++;  
                    totalcount++; 
                    if ((totalcount>=maxtries) || (goodcount>=maxgood) || (DMF.nEdits!=iEdits))
                    {
                        bRunning = false; 
                        break; 
                    }
                }
                String s = "Ray Starts = " + Integer.toString(totalcount); 
                jlTop.setText(s); 
                String g = "Ray Finishes = " + Integer.toString(goodcount); 
                jlMid.setText(g);
                String r = " "; 
                if (totalcount > 0)
                {
                    double percent = (goodcount*100.0)/totalcount; 
                    r = "Percent = " + U.fwd(percent, 5, 1); 
                }
                jlBot.setText(r);  
                targetPanel.redo();  // myBatchList -> g2Tech, and blit.
            }
            else
            {
                myTimer.stop(); 
                jbDone.setText("Done"); 
            }
        } 
    };


    private void shutdown()
    {
        myTimer.stop(); 
        myTimer = null; 
        jd.setVisible(false);  
        jd = null; 
        totalcount = 0;
        goodcount = 0; 
    }
}

