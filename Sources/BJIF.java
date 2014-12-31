package com.stellarsoftware.beam;

import java.util.*;           // Timer; sort()
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;       // Rect
import javax.swing.*;         // JIFrames
import javax.swing.event.*;   // JIFrame events
import java.io.*;             // output files
import java.awt.image.*;      // Buffer 
import javax.imageio.*;       // PNG, GIF, JPG

@SuppressWarnings("serial")

/**
  * BJIF.java  --- Abstract class from JInternalFrame.
  * Supplies a caret blinker and WriteImage functions. 
  * It is further extended to EJIF (editing) and GJIF (graphics).
  *
  * BJIF includes:
  *   bug-free keystroke focus manager for its ContentPane;
  *   caret timer & manager;
  *   active/passive sensor to help with caret;
  *   public boolean getCaret() for clients to call for caret state;
  *   WriteImage processor triggered by DMF.bRequestWriteImage; 
  *   general call to repaint() self (and client) for caret on/off.
  *
  *   A140: calls DMF.vMasterParse() for each blink=ON task. 
  *   This feature gives better user feedback about syntax errors.
  *   YES IT DOES. BUT NOW VERTICAL SCROLL MARKING IS INTERMITTENT.
  *   DIAGNOSING: DELETING THIS NEW FEATURE 30 OCT 2012
  *   Reinstalling it A142 1 Nov 2012 with a permission flag bNeedsParse.
  *  
  *   Possible associated bug: AutoAdj keeps getting whammed by
  *   something that disrupts its surfs[], spoiling convergence.
  *   This is not a problem in A135 -- that one runs smoothly. 
  *   Could DMF.vMasterParse barge in on AutoAdj, spoiling its convergence?
  *
  * BJIF excludes:
  *   any knowledge of decendant client panels;
  *   any knowledge of keystrokes.
  *
  * Requirements on DMF: 
  *   use WindowListener to maintain DMF.bHostActive;
  *   use MenuListener to maintain DMF.bQuickPNGrequest.
  *
  * Requirements on EJIF, GJIF:
  *    communicate self pointer to client JPanel
  *
  * Requirements on JPanels:
  *    call myBJIF.getCaret() within paintComponent() caret permission.
  *
  * New doQuickPNG installed Nov 2014  A166.
  *
  *  @author M.Lampton (c) 2007 STELLAR SOFTWARE all rights reserved.
  */
abstract class BJIF extends JInternalFrame implements B4constants
{
    // public static final long serialVersionUID = 42L; // Xlint 8 Oct 2014 ??
    protected boolean bNeedsParse=false;    // BJIF timer calls vMasterParse

    private java.util.Timer caretTimer;  
    private boolean bActive = false;
    private boolean bCaret = false; 
    private Container myC = null; 


    public BJIF(String s) // constructor
    {
        super(s, true, true, true, true);   // set up JIF
        myC = getContentPane();             // default keyTarget

        this.addInternalFrameListener(new InternalFrameAdapter()
        {
            public void internalFrameActivated(InternalFrameEvent e)
            {
                bActive = true; 
                final Container cFinal = myC; 
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        cFinal.requestFocusInWindow();
                    }
                }); 
            }

            public void internalFrameDeactivated(InternalFrameEvent e)
            {
                bActive = false; 
            }

            public void internalFrameClosed(InternalFrameEvent e)
            // update status when a window closes
            {
                DMF.vMasterParse(true); 
            }
        });

        //-----------set up the caret engine-----------------
        caretTimer = new java.util.Timer(); 
        caretTimer.schedule(new BlinkTask(this), 0, BLINKMILLISEC); // trying "this"
    }


    public void setKeyPanel(Container c)
    // allows EJIF to override ContentPane with its ePanel.
    {
       myC = c; 
    }


    public boolean getCaretStatus() // called by descendant's client JPanel
    {
        return bCaret;
    }



    //----------------private methods-------------------------

    private class BlinkTask extends TimerTask
    // private classes can share host variables. 
    {
        private boolean bPrev=true; // worksaver
        private BJIF myBJIF = null; // trying to save host instance
        
        public BlinkTask(BJIF givenBJIF)  // constructor
        {
            myBJIF = givenBJIF;  // save host instance
        }

        public void run()
        {
            if (bActive && DMF.bHostActive)
            {
                if (!bCaret && DMF.bRequestWriteImage)
                  doQuickPNG(myBJIF);  // trying BJIF itself
                bCaret = !bCaret; 

                /// Must not modify working data asynchronously.
                /// Must never disrupt AutoAdj work in surfs[].

                if (bCaret && bNeedsParse)
                { 
                    DMF.vMasterParse(true); // parses OEJIF,REJIF,MEJIF when Auto is not busy.
                    bNeedsParse = false;
                }
            }
            else
              bCaret = false; 

            if (bPrev != bCaret)
              repaint(); // repaints descendant's client panel too!
            bPrev = bCaret;
        }
    }



    static private void doQuickPNG(BJIF myBJIF)
    // static private void doQuickPNG(Container myC)
    // Called from BlinkTask (above) when DMF.bRequestWriteImage==true.
    // Waiting for the caret to resume avoids capturing the FileMenu.
    // Triggering only when caret=off eliminates caret-in-picture.
    // Trying static to eliminate flaky showSaveDialog here,
    // since WriteCAD uses a fully static method CAD::doCAD.  No joy.
    // Trying eliminating setFileFilter(), which doCAD lacks.  No joy.
    // Eliminating showSaveDialog entirely.  Works. 
    {
        DMF.bRequestWriteImage = false;
        JFileChooser jfc = new JFileChooser(); 
        String sDir = DMF.sCurrentDir;              // OK this is current.
        if (sDir != null)
        {
            File fDir = new File(sDir); 
            if (fDir != null)
              if (fDir.isDirectory())
                jfc.setCurrentDirectory(fDir);
        } 
        

        File cwd = jfc.getCurrentDirectory(); 
        // System.out.println("doQuickPNG CWD = "+cwd); 
        
        File[] files = cwd.listFiles(); 
        ArrayList<Integer> aList = new ArrayList<Integer>(); 
        for (int i=0; i<files.length; i++)
          if (files[i].isFile())
          {
              String fname = files[i].getName().toUpperCase(); 
              if (fname.startsWith("QUICK") && fname.endsWith(".PNG"))
              {  
                  int j = U.suckInt(fname); 
                  // System.out.println(fname + "    "+j); 
                  aList.add(j); 
              }
          }
          
        //---find the lowest available QuickPNG number------
        
        Collections.sort(aList); 
        int firstAvail = 0;
        boolean bOK; 
        do
        {
            firstAvail++;
            bOK = true; 
            for (int i=0; i<aList.size(); i++)
              if (aList.get(i) == firstAvail)
                bOK = false;  
        } while (bOK == false); 
        // System.out.println("First avail = "+firstAvail); 
        
        //----now build the complete file name----
        String fname = "Quick" + firstAvail + ".png"; 
        String cfname = cwd + File.separator + fname; 
        // System.out.println(cfname); 
        
        //---now create the output file----
        File outfile = new File(cfname); 
        
        //---try to get a window rectangle----
        //---here I use a JPanel cast to be more definite ----
        
        JPanel contentPanel = (JPanel) myBJIF.getContentPane();
        Rectangle r = contentPanel.getBounds(); 
        Point pxy = contentPanel.getLocationOnScreen(); 
        r.x = pxy.x; 
        r.y = pxy.y; 
        // System.out.println(" x="+r.x+"   y="+r.y+"   width="+r.width+"   height="+r.height); 
                        
        try
        {
            Robot robot = new Robot();
            BufferedImage image = robot.createScreenCapture(r);
            ImageIO.write(image, "png", outfile);
            myBJIF.setTitle("saved "+fname);
        }
        catch (Exception e) {}
    }
} //-----end of BJIF-----------
