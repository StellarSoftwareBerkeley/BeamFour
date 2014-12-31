package com.stellarsoftware.beam;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;        // layout managers
import javax.swing.border.*; // borders
import javax.swing.event.*;  // JSpinner ChangeEvent
import java.io.*;            // all i/o
import java.util.*;          // StringTokenizer

@SuppressWarnings("serial")

/**
  *  Options is the user interface to the Registry.
  *  
  *  Options extends JMenu, provides an options system using strings.
  *
  *  ALL OPTION STRINGS ARE DEFINED IN CONSTANTS.JAVA not here. 
  *
  *  ActionListeners implement the dialogs to help separate GUI from code.
  *  Both Registry and Options implement Constants.
  *
  *  The Registry constructor is responsible for options in/out.
  *  Caution: never use an equals sign in a uoName.
  *  Equals signs are used by the parser to parse the options data.
  *
  *  To add an option:
  *    1. In B4Constants, name and insert new macro UO_XXX = nnn;
  *    2. In B4Constants, write and insert its display string "sss"
  *    3. In B4Constants, write and insert its default factory string.
  *    4. In Options, dialogXX, create the new data object
  *    5. In Options, dialogXX, add the object to array in ShowDialog().
  *    6. In Options, dialogXX, OK_OPTION, putuo() the result.
  *
  *  To use an optioned parameter:
  *    1. Avoid "supply push" where Options alters a client parameter
  *       which will be used passively by past and future clients.
  *       Reason: no way to initialize this parm at startup. 
  *       Instead, implement "demand pull."  The client's constructor should
  *       get its string  from DMF.reg.getuo(UO_XXX) then set and 
  *       limit its parameters.  Demand pull works from startup, not just 
  *       from the first Option visit.
  *    2. For active options e.g. ray table construction, the parms are 
  *       used only once then discarded, so "supply push" is OK.
  *
  */
class Options extends JMenu implements B4constants
{  
    // public static final long serialVersionUID = 42L;

    final int NCHARS = 6; // width of data entry fields

    JFrame owner = null; 

    public Options(String menuname, JFrame gjf)  /// the constructor
    // Called once at startup to build the options main menu. 
    {
        super(menuname);  
        owner = gjf; 

        JMenuItem inoutItem = new JMenuItem("InOut"); 
        inoutItem.addActionListener(new
          ActionListener()
          {
             public void actionPerformed(ActionEvent ae)
             {
                doInOutDialog(owner); 
             }
          });
        this.add(inoutItem); 

        JMenuItem layoutItem = new JMenuItem("Layout"); 
        layoutItem.addActionListener(new
          ActionListener()
          {
             public void actionPerformed(ActionEvent ae)
             {
                doLayoutDialog(owner); 
             }
          });
        this.add(layoutItem); 

        JMenuItem autoItem = new JMenuItem("AutoAdjust"); 
        autoItem.addActionListener(new
          ActionListener()
          {
             public void actionPerformed(ActionEvent ae)
             {
                doAutoAdjustDialog(owner); 
             }
          });
        this.add(autoItem); 

        JMenuItem plot2Item = new JMenuItem("Plot2Dim"); 
        plot2Item.addActionListener(new
          ActionListener()
          {
             public void actionPerformed(ActionEvent ae)
             {
                doPlot2Dialog(owner); 
             }
          });
        this.add(plot2Item); 

        JMenuItem multiPlotItem = new JMenuItem("MultiPlot"); 
        multiPlotItem.addActionListener( new
          ActionListener()
          {
             public void actionPerformed(ActionEvent ae)
             {
                doMultiPlotDialog(owner); 
             }
          }); 
        this.add(multiPlotItem); 

        JMenuItem mapItem = new JMenuItem("Map"); 
        mapItem.addActionListener( new
          ActionListener()
          {
             public void actionPerformed(ActionEvent ae)
             {
                doMapDialog(owner); 
             }
          }); 
        this.add(mapItem); 

        JMenuItem plot3Item = new JMenuItem("Plot3Dim"); 
        plot3Item.addActionListener(new
          ActionListener()
          {
             public void actionPerformed(ActionEvent ae)
             {
                doPlot3Dialog(owner); 
             }
          });
        this.add(plot3Item); 

        JMenuItem do1dItem = new JMenuItem("Histo1Dim"); 
        // so simple we use an anonymous inner class..
        do1dItem.addActionListener(new
          ActionListener()
          {
             public void actionPerformed(ActionEvent ae)
             {
                doH1Ddialog(owner); 
             }
          });
        this.add(do1dItem); 

        JMenuItem do2dItem = new JMenuItem("Histo2Dim"); 
        do2dItem.addActionListener(new
          ActionListener()
          {
             public void actionPerformed(ActionEvent ae)
             {
                doH2Ddialog(owner); 
             }
          });
        this.add(do2dItem); 

        JMenuItem randomItem = new JMenuItem("Random"); 
        randomItem.addActionListener(new
          ActionListener()
          {
             public void actionPerformed(ActionEvent ae)
             {
                doRandomDialog(owner); 
             }
          });
        this.add(randomItem); 

        JMenuItem cadItem = new JMenuItem("CAD"); 
        cadItem.addActionListener(new
          ActionListener()
          {
             public void actionPerformed(ActionEvent ae)
             {
                doCadDialog(owner); 
             }
          });
        this.add(cadItem); 

        JMenuItem startItem = new JMenuItem("Startup Files"); 
        startItem.addActionListener(new
          ActionListener()
          {
             public void actionPerformed(ActionEvent ae)
             {
                doStartDialog(owner); 
             }
          });
        this.add(startItem); 

        JMenuItem factoryItem = new JMenuItem("Factory Options"); 
        factoryItem.addActionListener(new
          ActionListener()
          {
             public void actionPerformed(ActionEvent ae)
             {
                doFactoryDialog(owner); 
             }
          });
        this.add(factoryItem); 

        JMenuItem editItem = new JMenuItem("Editors"); 
        editItem.addActionListener(new
          ActionListener()
          {
             public void actionPerformed(ActionEvent ae)
             {
                doEditorDialog(owner); 
             }
          });
        this.add(editItem); 


        JMenuItem graphicsItem = new JMenuItem("Graphics"); 
        graphicsItem.addActionListener(new
          ActionListener()
          {
             public void actionPerformed(ActionEvent ae)
             {
                doGraphicsDialog(owner); 
             }
          });
        this.add(graphicsItem); 


        JMenuItem defRayItem = new JMenuItem("Default Rays"); 
        defRayItem.addActionListener(new
          ActionListener()
          {
             public void actionPerformed(ActionEvent ae)
             {
                doDefaultRaysDialog(owner); 
             }
          });
        this.add(defRayItem); 


        //-------ray generator has four children-------------

        JMenu rayMenu = new JMenu("Ray Generators"); 

        JMenuItem ray1dItem = new JMenuItem("1D Uniform Rays");
        ray1dItem.addActionListener(new
          ActionListener()
          {
              public void actionPerformed(ActionEvent ae)
              {
                 if (DMF.bRayGenOK)
                   doRay1dDialog(owner); 
                 else 
                   JOptionPane.showMessageDialog(owner, "Ray Table Required"); 
              }
          });
        rayMenu.add(ray1dItem); 

        JMenuItem rayRectItem = new JMenuItem("2D Rectangular Uniform Rays");
        rayRectItem.addActionListener(new
          ActionListener()
          {
              public void actionPerformed(ActionEvent ae)
              {
                 if (DMF.bRayGenOK)
                   doRayRectDialog(owner); 
                 else 
                   JOptionPane.showMessageDialog(owner, "Ray Table Required"); 
              }
          });
        rayMenu.add(rayRectItem); 

        JMenuItem rayCircItem = new JMenuItem("2D Circular Uniform Rays");
        rayCircItem.addActionListener(new
          ActionListener()
          {
              public void actionPerformed(ActionEvent ae)
              {
                 if (DMF.bRayGenOK)
                   doRayCircDialog(owner); 
                 else 
                   JOptionPane.showMessageDialog(owner, "Ray Table Required");  
              }
          });
        rayMenu.add(rayCircItem); 

        JMenuItem rayGausItem = new JMenuItem("2D Circular Gaussian Rays");
        rayGausItem.addActionListener(new
          ActionListener()
          {
              public void actionPerformed(ActionEvent ae)
              {
                 if (DMF.bRayGenOK)
                   doRayGausDialog(owner); 
                 else 
                   JOptionPane.showMessageDialog(owner, "Ray Table Required");  
              }
          });
        rayMenu.add(rayGausItem); 

        this.add(rayMenu); 


        JMenu lookMenu = new JMenu("Look & Feel"); // LJ 527
        final UIManager.LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();
        for (int i=0; i<info.length; i++)
        {
            final String className = info[i].getClassName();
            lookMenu.add(new AbstractAction(info[i].getName())
            {
                public static final long serialVersionUID = 42L;
                public void actionPerformed(ActionEvent ae)
                {
                    try { UIManager.setLookAndFeel(className); }
                    catch (Exception e2) {}
                    SwingUtilities.updateComponentTreeUI(owner); 
                }
            }); 
        }
        this.add(lookMenu); 
    }



    //------private dialog methods; each uses JOptionPane--------------

    void doInOutDialog(JFrame frame)
    {
        LabelBitBox rms = new LabelBitBox(UO_IO, 0); 

        int result = JOptionPane.showOptionDialog(frame,
           new Object[] {rms}, 
           "InOut Options", 
           JOptionPane.OK_CANCEL_OPTION, 
           JOptionPane.PLAIN_MESSAGE,
           null, null, null); 

        if (result == JOptionPane.OK_OPTION)
        {
           DMF.reg.putuo(UO_IO, 0, rms.isSelected() ? "T" : "F"); 
        }
    }




    void doLayoutDialog(JFrame frame)
    // standard dialogs use JOptionPane; always modal.
    // custom dialogs need JDialog.
    {
        LabelDataBox el = new LabelDataBox(UO_LAYOUT, 0, NCHARS); 
        LabelDataBox az = new LabelDataBox(UO_LAYOUT, 1, NCHARS); 
        LabelDataBox ar = new LabelDataBox(UO_LAYOUT, 3, NCHARS); 
        LabelBitBox  sticky = new LabelBitBox(UO_LAYOUT, 2); 
        
        String title1 = "Which axis is to point upward?";
        BorHorizRadioBox vert = new BorHorizRadioBox(title1, 
                                  UO_LAYOUT, 4, 6); 
        String title2 = "Axes to be drawn?"; 
        BorderedCheckBoxRow ax = new BorderedCheckBoxRow(title2, 
                                  UO_LAYOUT, 10, 5, frame); 
        String title3 = "Arcs to be drawn?";
        BorderedCheckBoxRow rb = new BorderedCheckBoxRow(title3, 
                                  UO_LAYOUT, 19, 8, frame); 
        LabelBitBox shading  = new LabelBitBox(UO_LAYOUT, 27); 
        LabelBitBox connect  = new LabelBitBox(UO_LAYOUT, 37); 
        LabelBitBox retrovis = new LabelBitBox(UO_LAYOUT, 38); 
        
        String title4 = "Line weights?"; 
        BorderedDataBoxRow db = new BorderedDataBoxRow(title4, 
                                  UO_LAYOUT, 28, 3, frame); 
        BorHorizStereoBox bhsb = new BorHorizStereoBox("Format", 
                                  UO_LAYOUT, 15, frame); 
                                  
        int result = JOptionPane.showOptionDialog(frame,
           new Object[] {el, az, ar, sticky, shading, connect, 
                         retrovis, vert, ax, rb, db,  bhsb}, 
           "Layout Options", 
           JOptionPane.OK_CANCEL_OPTION, 
           JOptionPane.PLAIN_MESSAGE,
           null, null, null); 

        if (result == JOptionPane.OK_OPTION)
        {
            DMF.reg.putuo(UO_LAYOUT, 0, el.getText()); 
            DMF.reg.putuo(UO_LAYOUT, 1, az.getText()); 
            DMF.reg.putuo(UO_LAYOUT, 2, sticky.isSelected() ? "T" : "F"); 
            DMF.reg.putuo(UO_LAYOUT, 3, ar.getText()); 
            for (int i=0; i<6; i++)
              DMF.reg.putuo(UO_LAYOUT, 4+i, vert.isSelected(i) ? "T" : "F"); 
            for (int i=0; i<5; i++)
              DMF.reg.putuo(UO_LAYOUT, 10+i, ax.isSelected(i) ? "T" : "F");
            for (int i=0; i<3; i++)
              DMF.reg.putuo(UO_LAYOUT, 15+i, bhsb.isSelected(i) ? "T" : "F"); 
            DMF.reg.putuo(UO_LAYOUT, 18, bhsb.getText()); 
            for (int i=0; i<8; i++)
              DMF.reg.putuo(UO_LAYOUT, 19+i, rb.isSelected(i) ? "T" : "F"); 
            DMF.reg.putuo(UO_LAYOUT, 27, shading.isSelected() ? "T" : "F"); 
            DMF.reg.putuo(UO_LAYOUT, 37, connect.isSelected() ? "T" : "F"); 
            DMF.reg.putuo(UO_LAYOUT, 38, retrovis.isSelected() ? "T" : "F"); 
            for (int i=0; i<3; i++)
              DMF.reg.putuo(UO_LAYOUT, 28+i, db.getText(i)); 

            updateAllInstances("Layout"); 
        }
    }





    void doAutoAdjustDialog(JFrame frame)
    {
        LabelDataBox step = new LabelDataBox(UO_AUTO, 0, NCHARS); 
        LabelDataBox maxit = new LabelDataBox(UO_AUTO, 1, NCHARS); 
        LabelDataBox tol = new LabelDataBox(UO_AUTO, 2, NCHARS); 

        int result = JOptionPane.showOptionDialog(frame,
           new Object[] {step, maxit, tol}, 
           "AutoAdjust Options", 
           JOptionPane.OK_CANCEL_OPTION, 
           JOptionPane.PLAIN_MESSAGE,
           null, null, null); 

        if (result == JOptionPane.OK_OPTION)
        {
            DMF.reg.putuo(UO_AUTO, 0, step.getText()); 
            DMF.reg.putuo(UO_AUTO, 1, maxit.getText()); 
            DMF.reg.putuo(UO_AUTO, 2, tol.getText()); 
        }
    }




    void doPlot2Dialog(JFrame frame)
    {
        LabelDataBox hvar = new LabelDataBox(UO_PLOT2, 0, NCHARS); 
        LabelDataBox hran = new LabelDataBox(UO_PLOT2, 1, NCHARS); 
        LabelBox hlabel = new LabelBox("HorSpan=blank or 0 for auto scaling"); 
        JLabel hblank = new JLabel(" "); 
        LabelDataBox vvar = new LabelDataBox(UO_PLOT2, 2, NCHARS); 
        LabelDataBox vran = new LabelDataBox(UO_PLOT2, 3, NCHARS); 
        LabelBox vlabel = new LabelBox("VertSpan=blank or 0 for auto scaling"); 
        JLabel vblank = new JLabel(" "); 
        LabelDataBox wave = new LabelDataBox(UO_PLOT2, 4, NCHARS); 
        BorVertRadioBox spot = new BorVertRadioBox("Dot style", UO_PLOT2, 5, 4); 
        JLabel blank = new JLabel(" "); 
        BorVertRadioBox rays = new BorVertRadioBox("Which rays", UO_PLOT2, 9, 2); 
        LabelDataBox other = new LabelDataBox(UO_PLOT2, 11, 3); 
        LabelBitBox black = new LabelBitBox(UO_PLOT2, 12); 

        int result = JOptionPane.showOptionDialog(frame,
           new Object[] {hvar, hran, hlabel, hblank, 
                         vvar, vran, vlabel, vblank, 
                         wave, spot, blank, rays, other, black}, 
           "Plot2Dim Options", 
           JOptionPane.OK_CANCEL_OPTION, 
           JOptionPane.PLAIN_MESSAGE,
           null, null, null); 

        if (result == JOptionPane.OK_OPTION)
        {
            DMF.reg.putuo(UO_PLOT2, 0, hvar.getText()); 
            DMF.reg.putuo(UO_PLOT2, 1, hran.getText()); 
            DMF.reg.putuo(UO_PLOT2, 2, vvar.getText()); 
            DMF.reg.putuo(UO_PLOT2, 3, vran.getText()); 
            DMF.reg.putuo(UO_PLOT2, 4, wave.getText()); 

            for (int i=0; i<4; i++)
              DMF.reg.putuo(UO_PLOT2, 5+i, spot.isSelected(i) ? "T" : "F"); 

            for (int i=0; i<2; i++)
              DMF.reg.putuo(UO_PLOT2, 9+i, rays.isSelected(i) ? "T" : "F"); 

            DMF.reg.putuo(UO_PLOT2, 11, other.getText()); 
            DMF.reg.putuo(UO_PLOT2, 12, black.isSelected() ? "T" : "F"); 

            updateAllInstances("Plot2Dim"); 
        }
    }




    void doMultiPlotDialog(JFrame frame)  // rev A150, Feb 2013; NCHARS=6.
    {
        Component vs = Box.createVerticalStrut(10); 
        BorderedMultiPlotBox bmpH = new BorderedMultiPlotBox(
           "Plots horizontally--Step Values run left to right", 
            UO_MPLOT, 0, MAXMP, frame); 
        BorderedMultiPlotBox bmpV = new BorderedMultiPlotBox(
           "Plots vertically--Step Values run top to bottom",
            UO_MPLOT, 7, MAXMP, frame); 

        LabelDataBox hvar = new LabelDataBox(UO_MPLOT, 14, NCHARS); 
        LabelDataBox hspan = new LabelDataBox(UO_MPLOT, 15, NCHARS); 
        LabelDataBox vvar = new LabelDataBox(UO_MPLOT, 16, NCHARS); 
        LabelDataBox vspan = new LabelDataBox(UO_MPLOT, 17, NCHARS); 
        LabelDataBox boxfrac = new LabelDataBox(UO_MPLOT, 18, NCHARS); 

        String title3 = "Statistics for each box";
        BorderedCheckBoxRow bcbr = new BorderedCheckBoxRow(title3, 
                                  UO_MPLOT, 19, 10, frame); 
        LabelBitBox black    = new LabelBitBox(UO_MPLOT, 29); 
        LabelBitBox round    = new LabelBitBox(UO_MPLOT, 30); 
        LabelBitBox skip     = new LabelBitBox(UO_MPLOT, 31); 
        LabelBitBox restrict = new LabelBitBox(UO_MPLOT, 32); 

        int result = JOptionPane.showOptionDialog(frame,
           new Object[] { bmpH, vs, bmpV, vs, hvar, hspan, vvar, vspan,
                          boxfrac, bcbr, black, round, skip, restrict}, 
           "MultiPlot Options", 
           JOptionPane.OK_CANCEL_OPTION, 
           JOptionPane.PLAIN_MESSAGE,
           null, null, null); 

        if (result == JOptionPane.OK_OPTION)
        {
            DMF.reg.putuo(UO_MPLOT, 0,  bmpH.getN()); 
            DMF.reg.putuo(UO_MPLOT, 1,  bmpH.getVar1()); 
            DMF.reg.putuo(UO_MPLOT, 2,  bmpH.getStr1()); 
            DMF.reg.putuo(UO_MPLOT, 3,  bmpH.getVar2()); 
            DMF.reg.putuo(UO_MPLOT, 4,  bmpH.getStr2()); 
            DMF.reg.putuo(UO_MPLOT, 5,  bmpH.getVar3()); 
            DMF.reg.putuo(UO_MPLOT, 6,  bmpH.getStr3()); 

            DMF.reg.putuo(UO_MPLOT, 7,  bmpV.getN()); 
            DMF.reg.putuo(UO_MPLOT, 8,  bmpV.getVar1()); 
            DMF.reg.putuo(UO_MPLOT, 9,  bmpV.getStr1()); 
            DMF.reg.putuo(UO_MPLOT, 10, bmpV.getVar2()); 
            DMF.reg.putuo(UO_MPLOT, 11, bmpV.getStr2()); 
            DMF.reg.putuo(UO_MPLOT, 12, bmpV.getVar3()); 
            DMF.reg.putuo(UO_MPLOT, 13, bmpV.getStr3()); 

            DMF.reg.putuo(UO_MPLOT, 14, hvar.getText()); 
            DMF.reg.putuo(UO_MPLOT, 15, hspan.getText()); 
            DMF.reg.putuo(UO_MPLOT, 16, vvar.getText()); 
            DMF.reg.putuo(UO_MPLOT, 17, vspan.getText()); 
            DMF.reg.putuo(UO_MPLOT, 18, boxfrac.getText()); 

            for (int i=0; i<10; i++)
              DMF.reg.putuo(UO_MPLOT, 19+i, bcbr.isSelected(i) ? "T" : "F"); 

            DMF.reg.putuo(UO_MPLOT, 29, black.isSelected()    ? "T" : "F"); 
            DMF.reg.putuo(UO_MPLOT, 30, round.isSelected()    ? "T" : "F"); 
            DMF.reg.putuo(UO_MPLOT, 31, skip.isSelected()     ? "T" : "F"); 
            DMF.reg.putuo(UO_MPLOT, 32, restrict.isSelected() ? "T" : "F"); 
            
            updateAllInstances("MultiPlot"); 
        }
    }



    void doMapDialog(JFrame frame)
    {
        BorHorizRadioBox bhrb = new BorHorizRadioBox(
           "What to map?", UO_MAP, 0, 4);   // four buttons
        BorderedMapBox bmbH = new BorderedMapBox(
           "Horizontal Axis", UO_MAP, 4, frame); 
        BorderedMapBox bmbV = new BorderedMapBox(
           "Vertical Axis", UO_MAP, 11, frame); 

        LabelDataBox percent = new LabelDataBox(UO_MAP, 18, NCHARS); 
        LabelDataBox aspect = new LabelDataBox(UO_MAP, 19, NCHARS); 
        LabelBitBox black = new LabelBitBox(UO_MAP, 20); 
        LabelDataBox outbox = new LabelDataBox(UO_MAP, 21, 20); 

        int result = JOptionPane.showOptionDialog(frame,
           new Object[] {bhrb, bmbH, bmbV, percent, aspect, black, outbox},
           "Map Options", 
           JOptionPane.OK_CANCEL_OPTION, 
           JOptionPane.PLAIN_MESSAGE,
           null, null, null); 

        if (result == JOptionPane.OK_OPTION)
        {
            for (int i=0; i<4; i++)   // buttons 0, 1, 2
              DMF.reg.putuo(UO_MAP, i, bhrb.isSelected(i) ? "T" : "F"); 

            DMF.reg.putuo(UO_MAP,  4, bmbH.getVar()); 
            DMF.reg.putuo(UO_MAP,  5, bmbH.getStep()); 
            DMF.reg.putuo(UO_MAP,  6, bmbH.getNpts()); 
            // DMF.reg.putuo(UO_MAP,  7, bmbH.getNpix()); 
            DMF.reg.putuo(UO_MAP,  8, bmbH.getCen()); 
            DMF.reg.putuo(UO_MAP,  9, bmbH.getPvar()); 
            DMF.reg.putuo(UO_MAP, 10, bmbH.getPstep()); 

            DMF.reg.putuo(UO_MAP, 11, bmbV.getVar()); 
            DMF.reg.putuo(UO_MAP, 12, bmbV.getStep()); 
            DMF.reg.putuo(UO_MAP, 13, bmbV.getNpts()); 
            // DMF.reg.putuo(UO_MAP, 14, bmbV.getNpix()); 
            DMF.reg.putuo(UO_MAP, 15, bmbV.getCen()); 
            DMF.reg.putuo(UO_MAP, 16, bmbV.getPvar()); 
            DMF.reg.putuo(UO_MAP, 17, bmbV.getPstep()); 

            DMF.reg.putuo(UO_MAP, 18, percent.getText()); 
            DMF.reg.putuo(UO_MAP, 19, aspect.getText()); 
            DMF.reg.putuo(UO_MAP, 20, black.isSelected() ? "T" : "F"); 
            DMF.reg.putuo(UO_MAP, 21, outbox.getText());  

            updateAllInstances("Map"); 
        }
    } //----end of doMapDialog------






    void doPlot3Dialog(JFrame frame)
    {
        LabelDataBox avar = new LabelDataBox(UO_PLOT3, 0, NCHARS); 
        LabelDataBox aran = new LabelDataBox(UO_PLOT3, 1, NCHARS); 
        LabelDataBox bvar = new LabelDataBox(UO_PLOT3, 2, NCHARS); 
        LabelDataBox bran = new LabelDataBox(UO_PLOT3, 3, NCHARS); 
        LabelDataBox cvar = new LabelDataBox(UO_PLOT3, 4, NCHARS); 
        LabelDataBox cran = new LabelDataBox(UO_PLOT3, 5, NCHARS); 
        LabelBox clabel = new LabelBox("Set any span=0.0 for auto scaling"); 
        LabelDataBox elev = new LabelDataBox(UO_PLOT3, 6, NCHARS); 
        LabelDataBox azim = new LabelDataBox(UO_PLOT3, 7, NCHARS); 
        LabelDataBox wave = new LabelDataBox(UO_PLOT3, 8, NCHARS); 
        BorHorizRadioBox spot = new BorHorizRadioBox("Dot style", UO_PLOT3, 9, 4); 
        BorVertRadioBox rays = new BorVertRadioBox("Which rays", UO_PLOT3, 13, 2);  
        BorHorizStereoBox bhsb = new BorHorizStereoBox("Format", UO_PLOT3, 15, frame); 

        int result = JOptionPane.showOptionDialog(frame,
           new Object[] {avar, aran,
                         bvar, bran,
                         cvar, cran, clabel,
                         elev, azim, 
                         wave, spot, rays, bhsb}, 
           "Plot3Dim Options", 
           JOptionPane.OK_CANCEL_OPTION, 
           JOptionPane.PLAIN_MESSAGE,
           null, null, null); 

        if (result == JOptionPane.OK_OPTION)
        {
            DMF.reg.putuo(UO_PLOT3, 0, avar.getText()); 
            DMF.reg.putuo(UO_PLOT3, 1, aran.getText()); 
            DMF.reg.putuo(UO_PLOT3, 2, bvar.getText()); 
            DMF.reg.putuo(UO_PLOT3, 3, bran.getText()); 
            DMF.reg.putuo(UO_PLOT3, 4, cvar.getText()); 
            DMF.reg.putuo(UO_PLOT3, 5, cran.getText()); 
            DMF.reg.putuo(UO_PLOT3, 6, elev.getText()); 
            DMF.reg.putuo(UO_PLOT3, 7, azim.getText()); 
            DMF.reg.putuo(UO_PLOT3, 8, wave.getText()); 

            for (int i=0; i<4; i++)
              DMF.reg.putuo(UO_PLOT3, 9+i, spot.isSelected(i) ? "T" : "F"); 

            for (int i=0; i<2; i++)
              DMF.reg.putuo(UO_PLOT3, 13+i, rays.isSelected(i) ? "T" : "F"); 

            for (int i=0; i<3; i++)
              DMF.reg.putuo(UO_PLOT3, 15+i, bhsb.isSelected(i) ? "T" : "F"); 
            DMF.reg.putuo(UO_PLOT3, 18, bhsb.getText()); 

            updateAllInstances("Plot3Dim"); 
        }
    }




    void doH1Ddialog(JFrame frame)
    {
        LabelDataBox var = new LabelDataBox(UO_1D, 0, NCHARS); 
        LabelDataBox nbins = new LabelDataBox(UO_1D, 1, NCHARS); 
        BorVertRadioBox bounds = new BorVertRadioBox("", UO_1D, 2, 3); 
        LabelDataBox hmin = new LabelDataBox(UO_1D, 5, NCHARS); 
        LabelDataBox hmax = new LabelDataBox(UO_1D, 6, NCHARS); 

        int result = JOptionPane.showOptionDialog(frame,
           new Object[] {var, nbins, bounds, hmin, hmax}, 
           "Histogram 1Dim Options", 
           JOptionPane.OK_CANCEL_OPTION, 
           JOptionPane.PLAIN_MESSAGE,
           null, null, null); 

        if (result == JOptionPane.OK_OPTION)
        {
            DMF.reg.putuo(UO_1D, 0, var.getText()); 
            DMF.reg.putuo(UO_1D, 1, nbins.getText()); 

            for (int i=0; i<3; i++)
              DMF.reg.putuo(UO_1D, 2+i, bounds.isSelected(i) ? "T" : "F"); 

            DMF.reg.putuo(UO_1D, 5, hmin.getText()); 
            DMF.reg.putuo(UO_1D, 6, hmax.getText()); 

            updateAllInstances("Histo1Dim"); 
        }
    }




    void doH2Ddialog(JFrame frame)
    {
        BorVertRadioBox display = new BorVertRadioBox("View", UO_2D, 0, 2); 
        LabelDataBox hvar     = new LabelDataBox(UO_2D, 2, NCHARS); 
        LabelDataBox vvar     = new LabelDataBox(UO_2D, 3, NCHARS); 
        LabelDataBox hbins    = new LabelDataBox(UO_2D, 4, NCHARS); 
        LabelDataBox vbins    = new LabelDataBox(UO_2D, 5, NCHARS); 
        LabelDataBox elev     = new LabelDataBox(UO_2D, 6, NCHARS); 
        LabelDataBox azim     = new LabelDataBox(UO_2D, 7, NCHARS); 

        BorVertRadioBox bounds = new BorVertRadioBox("Bounds", UO_2D, 8, 3); 
        LabelDataBox hmin    = new LabelDataBox(UO_2D, 11, NCHARS);
        LabelDataBox hmax    = new LabelDataBox(UO_2D, 12, NCHARS); 
        LabelDataBox vmin    = new LabelDataBox(UO_2D, 13, NCHARS);
        LabelDataBox vmax    = new LabelDataBox(UO_2D, 14, NCHARS); 
        BorHorizStereoBox bhsb = new BorHorizStereoBox("Format", UO_2D, 15, frame); 

        int result = JOptionPane.showOptionDialog(frame,
           new Object[] {display, hvar, vvar, hbins, vbins,
              elev, azim, bounds, hmin, hmax, vmin, vmax, bhsb}, 
           "Histogram 2Dim Options", 
           JOptionPane.OK_CANCEL_OPTION, 
           JOptionPane.PLAIN_MESSAGE,
           null, null, null); 

        if (result == JOptionPane.OK_OPTION)
        {
            for (int i=0; i<2; i++)
              DMF.reg.putuo(UO_2D, i, display.isSelected(i) ? "T" : "F"); 

            DMF.reg.putuo(UO_2D, 2, hvar.getText()); 
            DMF.reg.putuo(UO_2D, 3, vvar.getText()); 
            DMF.reg.putuo(UO_2D, 4, hbins.getText());
            DMF.reg.putuo(UO_2D, 5, vbins.getText()); 
            DMF.reg.putuo(UO_2D, 6,  elev.getText()); 
            DMF.reg.putuo(UO_2D, 7,  azim.getText());  

            for (int i=0; i<3; i++)
              DMF.reg.putuo(UO_2D, 8+i, bounds.isSelected(i) ? "T" : "F"); 

            DMF.reg.putuo(UO_2D, 11, hmin.getText()); 
            DMF.reg.putuo(UO_2D, 12, hmax.getText()); 
            DMF.reg.putuo(UO_2D, 13, vmin.getText()); 
            DMF.reg.putuo(UO_2D, 14, vmax.getText()); 

            for (int i=0; i<3; i++)
              DMF.reg.putuo(UO_2D, 15+i, bhsb.isSelected(i) ? "T" : "F"); 
            DMF.reg.putuo(UO_2D, 18, bhsb.getText()); 

            updateAllInstances("Histo2Dim"); 
        }
    }



/*************
    void doRandomDialog(JFrame frame)
    {
        // create three LabelDataBoxes, each 6 columns wide...
        LabelDataBox refresh = new LabelDataBox(UO_RAND, 0, NCHARS); 
        LabelDataBox tries = new LabelDataBox(UO_RAND, 1, NCHARS); 
        LabelDataBox succ = new LabelDataBox(UO_RAND, 2, NCHARS); 
        BorVertRadioBox xyz = new BorVertRadioBox("X0 Y0 Z0", UO_RAND, 3, 2); 
        BorVertRadioBox uvw = new BorVertRadioBox("U0 V0 W0", UO_RAND, 5, 2); 

        int result = JOptionPane.showOptionDialog(frame,
           new Object[] {refresh, tries, succ, xyz, uvw}, 
           "Random Ray Options", 
           JOptionPane.OK_CANCEL_OPTION, 
           JOptionPane.PLAIN_MESSAGE,
           null, null, null); 

        if (result == JOptionPane.OK_OPTION)
        {
            DMF.reg.putuo(UO_RAND, 0, refresh.getText()); 
            DMF.reg.putuo(UO_RAND, 1, tries.getText()); 
            DMF.reg.putuo(UO_RAND, 2, succ.getText()); 
            for (int i=0; i<2; i++)
              DMF.reg.putuo(UO_RAND, 3+i, xyz.isSelected(i) ? "T" : "F"); 
            for (int i=0; i<2; i++)
              DMF.reg.putuo(UO_RAND, 5+i, uvw.isSelected(i) ? "T" : "F");               
        }
    }
*****************/


    void doRandomDialog(JFrame frame)
    // this version from A154 with distributions
    {
        // create three LabelDataBoxes, each NCHARS columns wide...
        LabelDataBox refresh = new LabelDataBox(UO_RAND, 0, NCHARS); 
        LabelDataBox tries = new LabelDataBox(UO_RAND, 1, NCHARS); 
        LabelDataBox succ = new LabelDataBox(UO_RAND, 2, NCHARS); 
        
        // create three bordered boxes containing radio buttons, etc.
        BorVertRadioBox xyz = new BorVertRadioBox("X0 Y0 Z0", UO_RAND, 3, 2);         // two buttons
        BorVertRadioBox uvw = new BorVertRadioBox("U0 V0 W0", UO_RAND, 5, 2);         // two buttons
        BorVertRadioField brf = new BorVertRadioField("Distribution", UO_RAND,7,5);

        int result = JOptionPane.showOptionDialog(frame,
           new Object[] {refresh, tries, succ, xyz, uvw, brf}, 
           "Random Ray Options", 
           JOptionPane.OK_CANCEL_OPTION, 
           JOptionPane.PLAIN_MESSAGE,
           null, null, null); 

        if (result == JOptionPane.OK_OPTION)
        {
            DMF.reg.putuo(UO_RAND, 0, refresh.getText()); 
            DMF.reg.putuo(UO_RAND, 1, tries.getText()); 
            DMF.reg.putuo(UO_RAND, 2, succ.getText()); 
            for (int i=0; i<2; i++)   // two buttons
              DMF.reg.putuo(UO_RAND, 3+i, xyz.isSelected(i) ? "T" : "F"); 
            for (int i=0; i<2; i++)   // two buttons
              DMF.reg.putuo(UO_RAND, 5+i, uvw.isSelected(i) ? "T" : "F");               
            for (int i=0; i<5; i++)   // five buttons
              DMF.reg.putuo(UO_RAND, 7+i, brf.isSelected(i) ? "T" : "F"); 
            DMF.reg.putuo(UO_RAND, 12, brf.getText()); 
        }
    }
    
    


    void doCadDialog(JFrame frame)
    // All bitmaps have moved into BJIF and WriteImage.
    {
        int n = 9; // 8=NoQuads, 9=WithQuads.
        
        BorVertRadioBox format = new BorVertRadioBox("Format", UO_CAD, 0, n); 
        JLabel blank    = new JLabel(" "); 
        BorVertRadioBox orient = new BorVertRadioBox("Orientation", UO_CAD, 9, 2); 

        int result = JOptionPane.showOptionDialog(frame,
           new Object[] {format, blank, orient}, 
           "CAD Output Options", 
           JOptionPane.OK_CANCEL_OPTION, 
           JOptionPane.PLAIN_MESSAGE,
           null, null, null); 

        if (result == JOptionPane.OK_OPTION)
        {
           for (int i=0; i<n; i++)
             DMF.reg.putuo(UO_CAD, i, format.isSelected(i) ? "T" : "F"); 

           for (int i=0; i<2; i++)
             DMF.reg.putuo(UO_CAD, 12+i, orient.isSelected(i) ? "T" : "F"); 
        }
    }




    void doStartDialog(JFrame frame)
    // Includes option to automatically use most recent file set, A112 onward. 
    {
        LabelDataBox optBox  = new LabelDataBox(UO_START, 0, 55); 
        LabelDataBox rayBox  = new LabelDataBox(UO_START, 1, 55); 
        LabelDataBox medBox  = new LabelDataBox(UO_START, 2, 55); 
        LabelBitBox  autoBox = new LabelBitBox(UO_START, 3);
        Object[] buttonNames = {"                         OK                       ", 
                                "                       Cancel                     "}; 

        int result = JOptionPane.showOptionDialog(frame,
           new Object[] {optBox, rayBox, medBox, autoBox}, 
           "Startup File Options", 
           JOptionPane.OK_CANCEL_OPTION, 
           // plain because QUESTION_MESSAGE is an exclamation on the Mac.
           JOptionPane.PLAIN_MESSAGE,
           null,
           buttonNames, 
           buttonNames[1]); 

        String  sOpt  = "";
        String  sRay  = ""; 
        String  sMed  = ""; 
        boolean bAuto = autoBox.isSelected(); 
            
        if (bAuto)  // use current editor filenames
        {
            if (DMF.oejif != null)
              sOpt = DMF.oejif.getFpath(); 
            else
              sOpt = ""; 

            if (DMF.rejif != null)
              sRay = DMF.rejif.getFpath(); 
            else
              sRay = ""; 

            if (DMF.mejif != null)
              sMed = DMF.mejif.getFpath(); 
            else
              sMed = ""; 
        }

        else  // use LabelDataBox names
        {
            sOpt = optBox.getText(); 
            sRay = rayBox.getText(); 
            sMed = medBox.getText(); 
        }

        if (result == 0)  // clean and store them 
        {
            if ((sOpt.length()>0) && !sOpt.toUpperCase().endsWith(".OPT"))
              sOpt = sOpt + ".OPT"; 
            DMF.reg.putuo(UO_START, 0, sOpt); 

            if ((sRay.length()>0) && !sRay.toUpperCase().endsWith(".RAY"))
              sRay = sRay + ".RAY"; 
            DMF.reg.putuo(UO_START, 1, sRay); 

            if ((sMed.length()>0) && !sMed.toUpperCase().endsWith(".MED"))
              sMed = sMed + ".MED"; 
            DMF.reg.putuo(UO_START, 2, sMed); 
            
            DMF.reg.putuo(UO_START, 3,  autoBox.isSelected() ? "T" : "F"); 
        }
    }





    void doFactoryDialog(JFrame frame)
    {
        JLabel jlabel = new JLabel("Reset all options to initial factory values?"); 

        int result = JOptionPane.showOptionDialog(frame,
           new Object[] {jlabel}, 
           "Factory Options", 
           JOptionPane.OK_CANCEL_OPTION, 
           JOptionPane.PLAIN_MESSAGE,
           null, null, null); 

        if (result == JOptionPane.OK_OPTION)
        {
            DMF.reg.imposeFactory(); 
        }
    }



    void doEditorDialog(JFrame frame)
    {
        int NCHARS = 4; 
        JPanel aPanel = new JPanel(); 
        int count = 5;   // five strings in aPanel
        aPanel.setPreferredSize(new Dimension(300, 24+20*count)); 
        // aPanel.setMaximumSize(getPreferredSize()); 
        aPanel.setLayout(new BoxLayout(aPanel, BoxLayout.Y_AXIS));
        aPanel.add(new JLabel("  F7 or Ctl/Cmd Left:  narrow field")); 
        aPanel.add(new JLabel("  F8 or Ctl/Cmd Right:  widen field")); 
        aPanel.add(new JLabel("  F9 or Alt Right:         split field"));
        aPanel.add(new JLabel("  F10 or Alt Down:      copy down")); 
        aPanel.add(new JLabel("  Ctl/Cmd Z:           undo & redo")); 
        aPanel.setBorder(BorderFactory.createTitledBorder("Special Editor Keys"));

        JPanel bPanel = new JPanel(); 
        count = 3;   // three strings in bPanel
        bPanel.setPreferredSize(new Dimension(190, 24+20*count));
        bPanel.setMaximumSize(getPreferredSize()); 
        bPanel.setLayout(new BoxLayout(bPanel, BoxLayout.Y_AXIS)); 
        bPanel.add(new JLabel("  1. Mark lines with the mouse")); 
        bPanel.add(new JLabel("  2. then Edit:Copy  or  Ctl/Cmd C")); 
        bPanel.add(new JLabel("  3. then Edit:Paste  or  Ctl/Cmd V")); 
        bPanel.setBorder(BorderFactory.createTitledBorder("To insert lines")); 
        
        BorVertRadioBox text = new BorVertRadioBox("Editor mode", UO_EDIT, 9, 2); 
        LabelBitBox paths  = new LabelBitBox(UO_EDIT, 0); 
        LabelBitBox rowcol = new LabelBitBox(UO_EDIT, 1); 
        LabelBitBox commas = new LabelBitBox(UO_EDIT, 2); 
        LabelDataBox fwidth = new LabelDataBox(UO_EDIT, 3, NCHARS); 
        
        BorVertRadioBox colons = new BorVertRadioBox("Clipboard output field separators", UO_EDIT, 4, 2); 
        LabelDataBox fontsize = new LabelDataBox(UO_EDIT, 6, NCHARS); 
        LabelBitBox bold = new LabelBitBox(UO_EDIT, 7); 
        LabelBitBox smooth  = new LabelBitBox(UO_EDIT, 8); 

        int result = JOptionPane.showOptionDialog(frame,
           new Object[] {aPanel, bPanel, text, paths, rowcol, commas, 
                         fwidth, colons, fontsize, bold, smooth}, 
                         "Editor Options", 
                         JOptionPane.OK_CANCEL_OPTION, 
                         JOptionPane.PLAIN_MESSAGE,
                         null, null, null); 

        if (result == JOptionPane.OK_OPTION)
        {
            DMF.reg.putuo(UO_EDIT, 0, paths.isSelected() ? "T" : "F"); 
            DMF.reg.putuo(UO_EDIT, 1, rowcol.isSelected() ? "T" : "F"); 
            DMF.reg.putuo(UO_EDIT, 2, commas.isSelected() ? "T" : "F"); 
            DMF.reg.putuo(UO_EDIT, 3, fwidth.getText()); 
            for (int i=0; i<2; i++)
              DMF.reg.putuo(UO_EDIT, 4+i, colons.isSelected(i) ? "T" : "F"); 
            DMF.reg.putuo(UO_EDIT, 6, fontsize.getText()); 
            DMF.reg.putuo(UO_EDIT, 7, bold.isSelected() ? "T" : "F"); 
            DMF.reg.putuo(UO_EDIT, 8, smooth.isSelected() ? "T" : "F"); 
            for (int i=0; i<2; i++)
              DMF.reg.putuo(UO_EDIT, 9+i, text.isSelected(i) ? "T" : "F"); 
              
            // now, repaint all editors.
            if (DMF.mejif != null)
              DMF.mejif.repaint(); 
            if (DMF.rejif != null)
              DMF.rejif.repaint(); 
            if (DMF.oejif != null)
              DMF.oejif.repaint(); 
        }
    }



    void doGraphicsDialog(JFrame frame)
    {
        int NCHARS = 4; 
        JPanel cPanel = new JPanel(); 
        int count = 5;   // five strings in cPanel
        cPanel.setPreferredSize(new Dimension(300, 24+20*count)); 
        // cPanel.setMaximumSize(getPreferredSize()); 
        cPanel.setLayout(new BoxLayout(cPanel, BoxLayout.Y_AXIS));
        cPanel.add(new JLabel("     Left Button Drag:          pan scene")); 
        cPanel.add(new JLabel("     Left Button Click:           set caret")); 
        cPanel.add(new JLabel("     Wheel or F7,F8:            zoom in/out"));
        cPanel.add(new JLabel("     Shift+Wheel or F5,F6:     vert zoom")); 
        cPanel.add(new JLabel("     Right Button Drag:              twirl")); 
        cPanel.setBorder(BorderFactory.createTitledBorder("Special Graphics Keys"));

        BorVertRadioBox wheel = new BorVertRadioBox("Mouse Wheel", UO_GRAPH, 0, 2); 
        LabelDataBox fsize = new LabelDataBox(UO_GRAPH, 2, NCHARS); 
        LabelBitBox fbold = new LabelBitBox(UO_GRAPH, 3); 
        LabelDataBox asize = new LabelDataBox(UO_GRAPH, 4, NCHARS); 
        LabelBitBox abold = new LabelBitBox(UO_GRAPH, 5); 
        LabelDataBox gsize = new LabelDataBox(UO_GRAPH, 6, NCHARS); 
        LabelBitBox smooth = new LabelBitBox(UO_GRAPH, 7); 

        int result = JOptionPane.showOptionDialog(frame,
           new Object[] {cPanel, wheel, fsize, fbold, asize, abold, 
                         gsize, smooth}, 
           "Graphic Options", 
           JOptionPane.OK_CANCEL_OPTION, 
           JOptionPane.PLAIN_MESSAGE,
           null, null, null); 

        if (result == JOptionPane.OK_OPTION)
        {
            for (int i=0; i<2; i++)
              DMF.reg.putuo(UO_GRAPH, i, wheel.isSelected(i) ? "T" : "F");

            DMF.reg.putuo(UO_GRAPH, 2, fsize.getText()); 
            DMF.reg.putuo(UO_GRAPH, 3, fbold.isSelected()? "T" : "F"); 
            DMF.reg.putuo(UO_GRAPH, 4, asize.getText()); 
            DMF.reg.putuo(UO_GRAPH, 5, abold.isSelected()? "T" : "F"); 
            DMF.reg.putuo(UO_GRAPH, 6, gsize.getText()); 
            DMF.reg.putuo(UO_GRAPH, 7, smooth.isSelected()? "T" : "F"); 
            
            //----repaint all GJIFs------------
            updateAllInstances("GJIF"); 
        }
    } // end of doGraphicsDialog()




    void doDefaultRaysDialog(JFrame frame)
    {
        int NCHARS = 4; 
        String s1 = "Sign of makeup U0, V0, or W0"; 
        BorVertRadioBox msign = new BorVertRadioBox(s1, UO_DEF, 3, 2);

        JPanel midPanel = new JPanel(); 
        int count = 4;   // four items in midPanel
        midPanel.setPreferredSize(new Dimension(190, 24+20*count)); 
        midPanel.setMaximumSize(getPreferredSize()); 
        midPanel.setLayout(new BoxLayout(midPanel, BoxLayout.Y_AXIS));
        midPanel.add(new JLabel("         Random ray start positions are")); 
        midPanel.add(new JLabel("         randomly interpolated between the")); 
        midPanel.add(new JLabel("         most negative and most positive"));
        midPanel.add(new JLabel("         table ray starts for X, Y, and Z.")); 
        String sMid = "Random ray positions"; 
        midPanel.setBorder(BorderFactory.createTitledBorder(sMid));

        String s3 = "Random ray direction generator";
        BorVertRadioBox rrand = new BorVertRadioBox(s3, UO_DEF, 5, 4); 

        LabelDataBox radius = new LabelDataBox(UO_DEF, 9, NCHARS); 

        int result = JOptionPane.showOptionDialog(frame,
           new Object[] {msign, midPanel, rrand, radius}, 
           "Default Ray Options", 
           JOptionPane.OK_CANCEL_OPTION, 
           JOptionPane.PLAIN_MESSAGE,
           null, null, null); 

        if (result == JOptionPane.OK_OPTION)
        {
            for (int i=0; i<2; i++)
              DMF.reg.putuo(UO_DEF, 3+i, msign.isSelected(i) ? "T" : "F"); 
            for (int i=0; i<4; i++)
              DMF.reg.putuo(UO_DEF, 5+i, rrand.isSelected(i) ? "T" : "F");
            DMF.reg.putuo(UO_DEF,  9, radius.getText()); 

            //----now parse REJIF------------
            if (DMF.rejif != null)
              DMF.rejif.parse(); 
        }
    } // end of doDefaultRaysDialog()





    void doRay1dDialog(JFrame frame) // Ray Generator, 1D
    {
        int iwhich=0;
        double dCenter=0.0;
        double dSpan=0.0; 
        int nCount=0;
        int istartrow=1;
        int igoal=0;    // 0=start; 1=goal

        String s1 = "1D Coordinate"; 
        BorHorizRadioBox coord = new BorHorizRadioBox(s1, UO_1DRAY, 0, 6); // x,y,z,u,v,w

        LabelDataBox center = new LabelDataBox(UO_1DRAY, 6, NCHARS); 
        LabelDataBox span = new LabelDataBox(UO_1DRAY, 7, NCHARS); 
        LabelDataBox count = new LabelDataBox(UO_1DRAY, 8, NCHARS); 
        JLabel blank = new JLabel(" "); 
        LabelDataBox start = new LabelDataBox(UO_1DRAY, 9, NCHARS); 

        BorVertRadioBox rays = new BorVertRadioBox("Output to", UO_1DRAY, 10, 2); // starts or goals

        int result = JOptionPane.showOptionDialog(frame,
           new Object[] {rays, coord, center, span, count, blank, start}, 
           "1D Ray Pattern Options", 
           JOptionPane.OK_CANCEL_OPTION, 
           JOptionPane.PLAIN_MESSAGE,
           null, null, null); 

        if (result == JOptionPane.OK_OPTION)
        {
            for (int i=0; i<6; i++)
            {
                DMF.reg.putuo(UO_1DRAY, i, coord.isSelected(i) ? "T" : "F"); 
                if (coord.isSelected(i))
                  iwhich = i; 
            }   

            DMF.reg.putuo(UO_1DRAY, 6, center.getText()); 
            dCenter = U.suckDouble(center.getText()); 

            DMF.reg.putuo(UO_1DRAY, 7, span.getText()); 
            dSpan = U.suckDouble(span.getText()); 

            DMF.reg.putuo(UO_1DRAY, 8, count.getText()); 
            nCount = (int) U.suckDouble(count.getText()); 

            DMF.reg.putuo(UO_1DRAY, 9, start.getText()); 
            istartrow = (int) U.suckDouble(start.getText()); 

            for (int i=0; i<2; i++)
            {
                DMF.reg.putuo(UO_1DRAY, i+10, rays.isSelected(i) ? "T" : "F"); 
                if (rays.isSelected(i))
                  igoal = i;             // 0=start; 1=goal
            }   


            //// now try putting the coordinates into place

            int errcode = iPut1Drays(igoal, iwhich, dCenter, dSpan, nCount, istartrow); 
            if (errcode != 0)
            {
                String w[] = {"X", "Y", "Z", "U", "V", "W"}; 
                char c = (igoal==0) ? '0' : 'g'; 
                JOptionPane.showMessageDialog(frame, "Ray table lacks "+w[iwhich]+c); 
            }

            //----Now focus and parse REJIF------------
            //--Focussing REJIF forces all GJIFs to background---
            //---so they retain previous art until summoned-----
            
            if (DMF.rejif != null)
            {
                try { DMF.rejif.setSelected(true); }
                catch (java.beans.PropertyVetoException pve) {}
                DMF.rejif.parse(); 
            }
        }
    } // end of doRay1dDialog()




    void doRayRectDialog(JFrame frame) // Ray generator 2D rect
    {
        int iwhich=0;
        double dCenter1=0.0;
        double dSpan1=0.0; 
        int nCount1=0;
        double dCenter2=0.0;
        double dSpan2=0.0; 
        int nCount2=0;
        int istartrow=1; 
        int igoal=0;    // start or goal

        String s1 = "2D Coordinates"; 
        BorHorizRadioBox coord = new BorHorizRadioBox(s1, UO_2DRRAY, 0, 6);

        LabelDataBox center1 = new LabelDataBox(UO_2DRRAY, 6, NCHARS); 
        LabelDataBox span1 = new LabelDataBox(UO_2DRRAY, 7, NCHARS); 
        LabelDataBox count1 = new LabelDataBox(UO_2DRRAY, 8, NCHARS); 
        LabelDataBox center2 = new LabelDataBox(UO_2DRRAY, 9, NCHARS); 
        LabelDataBox span2 = new LabelDataBox(UO_2DRRAY, 10, NCHARS); 
        LabelDataBox count2 = new LabelDataBox(UO_2DRRAY, 11, NCHARS); 
        JLabel blank = new JLabel(" "); 
        LabelDataBox start = new LabelDataBox(UO_2DRRAY, 12, NCHARS); 

        BorVertRadioBox rays = new BorVertRadioBox("Output to", UO_2DRRAY, 13, 2); 

        int result = JOptionPane.showOptionDialog(frame,
           new Object[] {rays, coord, center1, span1, count1, center2, 
                   span2, count2, blank, start}, 
           "2D Rectangular Pattern Options", 
           JOptionPane.OK_CANCEL_OPTION, 
           JOptionPane.PLAIN_MESSAGE,
           null, null, null); 

        if (result == JOptionPane.OK_OPTION)
        {
            for (int i=0; i<6; i++)
            {
                DMF.reg.putuo(UO_2DRRAY, i, coord.isSelected(i) ? "T" : "F"); 
                if (coord.isSelected(i))
                  iwhich = i; 
            }   

            DMF.reg.putuo(UO_2DRRAY, 6, center1.getText()); 
            dCenter1 = U.suckDouble(center1.getText()); 

            DMF.reg.putuo(UO_2DRRAY, 7, span1.getText()); 
            dSpan1 = U.suckDouble(span1.getText()); 

            DMF.reg.putuo(UO_2DRRAY, 8, count1.getText()); 
            nCount1 = (int) U.suckDouble(count1.getText()); 

            DMF.reg.putuo(UO_2DRRAY, 9, center2.getText()); 
            dCenter2 = U.suckDouble(center2.getText()); 

            DMF.reg.putuo(UO_2DRRAY, 10, span2.getText()); 
            dSpan2 = U.suckDouble(span2.getText()); 

            DMF.reg.putuo(UO_2DRRAY, 11, count2.getText()); 
            nCount2 = (int) U.suckDouble(count2.getText()); 

            DMF.reg.putuo(UO_2DRRAY, 12, start.getText()); 
            istartrow = (int) U.suckDouble(start.getText()); 

            for (int i=0; i<2; i++)
            {
                DMF.reg.putuo(UO_2DRRAY, i+13, rays.isSelected(i) ? "T" : "F"); 
                if (rays.isSelected(i))
                  igoal = i; 
            }   


            //// now try putting the coordinates into place
            
            int q = iPutRectRays(igoal, iwhich, dCenter1, dSpan1, nCount1, 
                      dCenter2, dSpan2, nCount2, istartrow); 
            if (q != 0)
            {
                String w1[] = {"X",   "X",  "Y",  "U",   "U",   "V"}; 
                String w2[] = {",Y", ",Z",  ",Z", ",V",  ",W",  ",W"}; 
                char c = (igoal==0) ? '0' : 'g'; 
                JOptionPane.showMessageDialog(frame, "Ray table lacks "+w1[iwhich]+c+w2[iwhich]+c); 
            }
            
            //----Now focus and parse REJIF------------
            //--Focussing REJIF forces all GJIFs to background---
            //---so they retain previous art until summoned-----
            
            if (DMF.rejif != null)
            {
                // DMF.rejif.toFront(); 
                try { DMF.rejif.setSelected(true); }
                catch (java.beans.PropertyVetoException pve) {}
                DMF.rejif.parse(); 
            }
        }
    } // end of doRayRectDialog()




    void doRayCircDialog(JFrame frame) // circular pattern ray builder
    {
        int iwhich=0;
        int ncircles=0;
        double dOff1=0.0;
        double dOff2=0.0; 
        double dRadius=0.0;
        int istartrow=1; 
        int igoal=0;    // start or goal

        String s1 = "2D Coordinate Pair"; 
        BorHorizRadioBox pair = new BorHorizRadioBox(s1, UO_2DCRAY, 0, 6);

        LabelDataBox off1 = new LabelDataBox(UO_2DCRAY, 6, NCHARS); 
        LabelDataBox off2 = new LabelDataBox(UO_2DCRAY, 7, NCHARS); 
        LabelDataBox radius = new LabelDataBox(UO_2DCRAY, 8, NCHARS); 
        String s3 = "How many concentric circles?";
        BorderedHexBox bhb = new BorderedHexBox(s3, UO_2DCRAY, 9, 17, 1, frame); 
        JLabel blank = new JLabel(" "); 
        LabelDataBox start = new LabelDataBox(UO_2DCRAY, 10, NCHARS);

        BorVertRadioBox rays = new BorVertRadioBox("Output to", UO_2DCRAY, 11, 2); 

        int result = JOptionPane.showOptionDialog(frame,
           new Object[] {rays, pair, off1, off2, radius, bhb, blank, start}, 
           "2D Circular Uniform Rays", 
           JOptionPane.OK_CANCEL_OPTION, 
           JOptionPane.PLAIN_MESSAGE,
           null, null, null); 

        if (result == JOptionPane.OK_OPTION)
        {
            for (int i=0; i<6; i++)
            {
                DMF.reg.putuo(UO_2DCRAY, i, pair.isSelected(i) ? "T" : "F"); 
                if (pair.isSelected(i))
                  iwhich = i; 
            }   

            DMF.reg.putuo(UO_2DCRAY, 6, off1.getText()); 
            dOff1 = U.suckDouble(off1.getText()); 
            
            DMF.reg.putuo(UO_2DCRAY, 7, off2.getText()); 
            dOff2 = U.suckDouble(off2.getText()); 
            
            DMF.reg.putuo(UO_2DCRAY, 8, radius.getText()); 
            dRadius = U.suckDouble(radius.getText()); 

            DMF.reg.putuo(UO_2DCRAY, 9, bhb.getText()); 
            ncircles = bhb.getNcircles(); 

            DMF.reg.putuo(UO_2DCRAY, 10, start.getText()); 
            istartrow = (int) U.suckDouble(start.getText()); 

            for (int i=0; i<2; i++)
            {
                DMF.reg.putuo(UO_2DCRAY, i+11, rays.isSelected(i) ? "T" : "F"); 
                if (rays.isSelected(i))
                  igoal = i; 
            }   


            //// now try putting the coordinates into place
            
            int q = iPutCircles(igoal, iwhich, ncircles, dOff1, dOff2, dRadius, istartrow); 
            if (q != 0)
            {
                String w1[] = {"X",   "X",  "Y",  "U",   "U",   "V"}; 
                String w2[] = {",Y", ",Z",  ",Z", ",V",  ",W",  ",W"}; 
                char c = (igoal==0) ? '0' : 'g'; 
                JOptionPane.showMessageDialog(frame, "Ray table lacks "+w1[iwhich]+c+w2[iwhich]+c); 
            }
            
            //----Now focus and parse REJIF------------
            //--Focussing REJIF forces all GJIFs to background---
            //---so they retain previous art until summoned-----
            
            if (DMF.rejif != null)
            {
                try { DMF.rejif.setSelected(true); }
                catch (java.beans.PropertyVetoException pve) {}
                DMF.rejif.parse(); 
            }
        }
    } // end of doRayCircDialog()



    void doRayGausDialog(JFrame frame) // circular Gaussian ray builder
    {
        int iwhich=0;
        int ncircles=0;
        double dOff1=0.0;
        double dOff2=0.0; 
        double dRadius=0.0;
        int istartrow=1; 
        int igoal=0;    // 0=start or 1=goal

        String s1 = "2D Coordinate Pair"; 
        BorHorizRadioBox pair = new BorHorizRadioBox(s1, UO_2DCGAUS, 0, 6);

        LabelDataBox off1 = new LabelDataBox(UO_2DCGAUS, 6, NCHARS); 
        LabelDataBox off2 = new LabelDataBox(UO_2DCGAUS, 7, NCHARS); 
        LabelDataBox radius = new LabelDataBox(UO_2DCGAUS, 8, NCHARS); 

        String s3 = "How many concentric circles?";
        BorderedHexBox bhb = new BorderedHexBox(s3, UO_2DCGAUS, 9, 17, 0, frame); 
        JLabel blank = new JLabel(" "); 
        LabelDataBox start = new LabelDataBox(UO_2DCGAUS, 10, NCHARS); 

        BorVertRadioBox rays = new BorVertRadioBox("Output to", UO_2DCGAUS, 11, 2); 

        int result = JOptionPane.showOptionDialog(frame,
           new Object[] {rays, pair, off1, off2, radius, bhb, blank, start}, 
           "2D Circular Gaussian Rays", 
           JOptionPane.OK_CANCEL_OPTION, 
           JOptionPane.PLAIN_MESSAGE,
           null, null, null); 

        if (result == JOptionPane.OK_OPTION)
        {
            for (int i=0; i<6; i++)
            {
                DMF.reg.putuo(UO_2DCRAY, i, pair.isSelected(i) ? "T" : "F"); 
                if (pair.isSelected(i))
                  iwhich = i; 
            }   

            DMF.reg.putuo(UO_2DCGAUS, 6, off1.getText()); 
            dOff1 = U.suckDouble(off1.getText()); 
            
            DMF.reg.putuo(UO_2DCGAUS, 7, off2.getText()); 
            dOff2 = U.suckDouble(off2.getText()); 
            
            DMF.reg.putuo(UO_2DCGAUS, 8, radius.getText()); 
            dRadius = U.suckDouble(radius.getText()); 

            DMF.reg.putuo(UO_2DCGAUS, 9, bhb.getText()); 
            ncircles = bhb.getNcircles(); 
            
            DMF.reg.putuo(UO_2DCGAUS, 10, start.getText()); 
            istartrow = (int) U.suckDouble(start.getText()); 

            for (int i=0; i<2; i++)
            {
                DMF.reg.putuo(UO_2DCGAUS, i+11, rays.isSelected(i) ? "T" : "F"); 
                if (rays.isSelected(i))
                  igoal = i; 
            }   


            //// now try putting the coordinates into place

            int q = iPutGaussian(igoal, iwhich, ncircles, dOff1, dOff2, dRadius, istartrow); 
            if (q != 0)
            {
                String w1[] = {"X",   "X",  "Y",  "U",   "U",   "V"}; 
                String w2[] = {",Y", ",Z",  ",Z", ",V",  ",W",  ",W"}; 
                char c = (igoal==0) ? '0' : 'g'; 
                JOptionPane.showMessageDialog(frame, "Ray table lacks "+w1[iwhich]+c+w2[iwhich]+c); 
            }
            
            //----Now focus and parse REJIF------------
            //--Focussing REJIF forces all GJIFs to background---
            //---so they retain previous art until summoned-----
            
            if (DMF.rejif != null)
            {
                try { DMF.rejif.setSelected(true); }
                catch (java.beans.PropertyVetoException pve) {}
                DMF.rejif.parse(); 
            }
        }
    } // end of doRayGausDialog()




/*******************
    void doDebugDialog(JFrame frame)
    {
        int NCHARS = 4; 
        int prevLevel = DMF.idebug; 
        DebugBox dbox = new DebugBox(prevLevel, NCHARS); 
        int result = JOptionPane.showOptionDialog(frame,
           new Object[] {dbox}, 
           "Debug Options",   // dialog title 
           JOptionPane.OK_CANCEL_OPTION, 
           JOptionPane.PLAIN_MESSAGE,
           null, null, null); 

        if (result == JOptionPane.OK_OPTION)
        {
            String s = dbox.getText().trim(); 
            int i = U.parseInt(s); 
            if (DEBUGGING && (i>=0) && (i<10))
              DMF.idebug = i; 
            else
              DMF.idebug = 0; 
        }
    }
***********************/


    //------------Ray Generator Functions start here-----------------------------

    private int iPut1Drays(int g, int w, double dC, double dS, int nrays, int istart)
    // Used by doRay1dDialog() to make a linear ray pattern. 
    // g=1 means goals;  g=0 means raystarts
    // w chooses which coordinate: x,y,z,u,v,w = 0...5
    {
        int icoord[] = {RX, RY, RZ, RU, RV, RW}; 
        if ((w < 0) || (w > 5))
          return 1; 
        int i1 = icoord[w]; 
        int f1 = -1;                // initially, absentee field
        int nf = DMF.giFlags[RNFIELDS]; 
        REJIF redit = DMF.rejif;
        if (redit == null)
          return 2; 

        if (dS<=0.0)
          return 3; 

        if (g==0)                    // ray start
        {
            f1 = REJIF.rI2F[i1];     // ok for starts; Xlint 8 Oct 2014
        }
        else                         // ray goal
        {
            i1 += RGOAL;             // lab frame goal
            int i2 = i1 + RTXL;      // or surface frame goal 
            for (int f=0; f<nf; f++)
              if ((REJIF.rF2I[f]==i1) || (REJIF.rF2I[f]==i2))  // Xlint again
                f1 = f; 
        }   
          
        if (f1<0)                    // absentee field? 
          return 4; 

        double dx = (nrays>1) ? dS/(nrays-1.0) : dS;
        double ox = (nrays>1) ? dC-0.5*dS  : dC; 

        int row = 2 + U.minmax(istart, 1, MAXRAYS-1); 

        for (int k=0; k<nrays; k++)
        {
            double x = ox + k*dx;  
            redit.forceFieldDouble(f1, row, x); 
            row++;
        }
        redit.refreshSizes(); 
        redit.putField(0, 0, U.fwi(row-3,6)+" rays "); 
        redit.repaint(); 
        return 0; 
    }


    private int iPutRectRays(int g, int w, double dC1, double dS1, int n1,
                           double dC2, double dS2, int n2, int istart)
    // Used by doRayRectDialog() to make a rectangular ray pattern. 
    // g=1: goals;  g=0: raystarts
    {
        int icoord1[] = {RX, RX, RY, RU, RU, RV}; 
        int icoord2[] = {RY, RZ, RZ, RV, RW, RW}; 
        if ((w < 0) || (w > 5))
          return 1; 
        int i1 = icoord1[w]; 
        int i2 = icoord2[w]; 
        int f1=-1, f2=-1;        // absentee fields
        int nf = DMF.giFlags[RNFIELDS]; 
        REJIF redit = DMF.rejif;
        if (redit == null)
          return 2; 

        int nrays = n1*n2; 
        if ((n1<1) || (n2<1) || (nrays>999))
          return 3; 
        if ((dS1<=0.0) || (dS2<=0.0))
          return 4; 
       
        if (g == 0)                  // ray start
        {
            f1 = REJIF.rI2F[i1];    // Xlint
            f2 = REJIF.rI2F[i2];    // Xlint 
        }
        else                         // ray goal
        {
            i1 += RGOAL;             // lab frame goal
            i2 += RGOAL; 
            int i3 = i1 + RTXL;      // or surface frame goal 
            int i4 = i2 + RTXL; 
            for (int f=0; f<nf; f++)
            {
                if ((REJIF.rF2I[f]==i1) || (REJIF.rF2I[f]==i3))
                  f1 = f; 
                if ((REJIF.rF2I[f]==i2) || (REJIF.rF2I[f]==i2))
                  f2 = f; 
            }
        }

        if ((f1<0) || (f2<0) || (f1==f2))  // absentee or duplicate fields?
          return 5; 

        double dx1 = (n1>1) ? dS1/(n1-1.0) : dS1;
        double ox1 = (n1>1) ? dC1-0.5*dS1  : dC1; 
        double dx2 = (n2>1) ? dS2/(n2-1.0) : dS2;
        double ox2 = (n2>1) ? dC2-0.5*dS2  : dC2; 

        // Now for the math....

        int row = 2 + U.minmax(istart, 1, MAXRAYS-1); 
        
        for (int ii=0; ii<n1; ii++)
        {
            double x = ox1 + ii*dx1;  
            for (int jj=0; jj<n2; jj++)
            {
                double y = ox2 + jj*dx2; 
                redit.forceFieldDouble(f1, row, x); 
                redit.forceFieldDouble(f2, row, y); 
                row++;
            }
        }
        redit.refreshSizes(); 
        redit.putField(0, 0, U.fwi(row-3,6)+" rays "); 
        redit.repaint(); 
        return 0; 
    }


    private int iPutCircles(int g, int w, int ncircles, double d1, 
                               double d2, double R, int istart)
    // Used by doRayCircDialog() to make circular ray patterns. 
    // g=1: goals;  g=0: raystarts
    {
        int icoord1[] = {RX, RX, RY, RU, RU, RV}; 
        int icoord2[] = {RY, RZ, RZ, RV, RW, RW}; 
        if ((w < 0) || (w > 5))
          return 1; 
        int i1 = icoord1[w]; 
        int i2 = icoord2[w]; 
        int f1=-1, f2=-1;        // absentee fields
        int nf = DMF.giFlags[RNFIELDS]; 
        REJIF redit = DMF.rejif;
        if (redit == null)
          return 2; 

        if ((ncircles<1) || (ncircles>17))
          return 3; 
        if (R<=0.0)
          return 4; 
       
        if (g == 0)                  // ray start
        {
            f1 = REJIF.rI2F[i1]; 
            f2 = REJIF.rI2F[i2]; 

        }
        else                         // ray goal
        {
            i1 += RGOAL;             // lab frame goal
            i2 += RGOAL; 
            int i3 = i1 + RTXL;      // or surface frame goal 
            int i4 = i2 + RTXL; 
            for (int f=0; f<nf; f++)
            {
                if ((REJIF.rF2I[f]==i1) || (REJIF.rF2I[f]==i3))
                  f1 = f; 
                if ((REJIF.rF2I[f]==i2) || (REJIF.rF2I[f]==i2))
                  f2 = f; 
            }
        }

        if ((f1<0) || (f2<0) || (f1==f2))  // absentee or duplicate fields?
          return 5; 

        // now for the math....

        int row = 2 + U.minmax(istart, 1, MAXRAYS-1); 
        
        //---now install the center ray----------
        redit.forceFieldDouble(f1, row, d1);  
        redit.forceFieldDouble(f2, row, d2); 
        row++; 
        
        //----next install the rings-------
        for (int icirc=1; icirc<=ncircles; icirc++)
        {
            double daz = 60.0 / icirc;
            double offset = (icirc%2 == 0) ? 0.0 : 0.5*daz; 
            double r = icirc * R / ncircles;  
            for (int jaz = 0; jaz<6*icirc; jaz++)
            {
                double a = offset + jaz*daz; 
                double x = d1 + r*U.cosd(a); 
                double y = d2 + r*U.sind(a); 
                redit.forceFieldDouble(f1, row, x); 
                redit.forceFieldDouble(f2, row, y); 
                row++; 
            }
        }
        redit.refreshSizes(); 
        redit.putField(0, 0, U.fwi(row-3,6)+" rays "); 
        redit.repaint(); 
        return 0;
    }
    
    
    private int iPutGaussian(int g, int w, int ncircles, double d1, 
                               double d2, double R, int istart)
    // Used by doRayGausDialog() to make circular Gaussian ray patterns. 
    // g=1: goals;  g=0: raystarts
    {
        int icoord1[] = {RX, RX, RY, RU, RU, RV}; 
        int icoord2[] = {RY, RZ, RZ, RV, RW, RW}; 
        if ((w < 0) || (w > 5))
          return 1; 
        int i1 = icoord1[w]; 
        int i2 = icoord2[w]; 
        int f1=-1, f2=-1;        // absentee fields
        int nf = DMF.giFlags[RNFIELDS]; 
        REJIF redit = DMF.rejif;
        if (redit == null)
          return 2; 

        if ((ncircles<1) || (ncircles>17))
          return 3; 
        if ((R <= 0.0))
          return 4; 
       
        if (g == 0)                  // ray start
        {
            f1 = REJIF.rI2F[i1]; 
            f2 = REJIF.rI2F[i2]; 

        }
        else                         // ray goal
        {
            i1 += RGOAL;             // lab frame goal
            i2 += RGOAL; 
            int i3 = i1 + RTXL;      // or surface frame goal 
            int i4 = i2 + RTXL; 
            for (int f=0; f<nf; f++)
            {
                if ((REJIF.rF2I[f]==i1) || (REJIF.rF2I[f]==i3))
                  f1 = f; 
                if ((REJIF.rF2I[f]==i2) || (REJIF.rF2I[f]==i2))
                  f2 = f; 
            }
        }

        if ((f1<0) || (f2<0) || (f1==f2))  // absentee or duplicate fields?
          return 5; 

        //---install the rings & rays; no central ray----------

        int row = 2 + U.minmax(istart, 1, MAXRAYS-1); 
        
        double sigma = R/Math.sqrt(2.0*Math.log(1+ncircles)); 

        for (int icirc=1; icirc<=ncircles; icirc++)
        {
            double daz = 60.0 / icirc;
            double offset = (icirc%2 == 0) ? 0.0 : 0.5*daz; 
            double p = sqr(icirc)/(ncircles + sqr(ncircles)); 
            double r = sigma*Math.sqrt(2.0*Math.log(1/(1-p)));
  
            for (int jaz = 0; jaz<6*icirc; jaz++)
            {
                double a = offset + jaz*daz; 
                double x = d1 + r*U.cosd(a); 
                double y = d2 + r*U.sind(a); 
                redit.forceFieldDouble(f1, row, x); 
                redit.forceFieldDouble(f2, row, y); 
                row++; 
            }
        }
        redit.refreshSizes(); 
        redit.putField(0, 0, U.fwi(row-3,6)+" rays "); 
        redit.repaint(); 
        return 0;
    }
    
    private double sqr(double arg)
    // Important: returns a double for int argument.
    {
        return arg*arg;
    }
    
    private void updateAllInstances(String gname)
    {
        JInternalFrame[] jifs = DMF.getJIFs(); 
        for (int i=0; i<jifs.length; i++)
          if (jifs[i] instanceof GJIF)
          {
              boolean bTypeMatch = gname.equals("GJIF"); 
              boolean bNameMatch = ((GJIF) jifs[i]).getName().equals(gname);
              if (bTypeMatch || bNameMatch)
                ((GJIF) jifs[i]).doUpdateUO(); 
          }
    }  

} //----------end of class Options------------------------------












//---------------Helper classes start here-----------------------



class LabelBox extends JPanel implements B4constants 
// user specifies label
{   
    public static final long serialVersionUID = 42L;

    public LabelBox(String label)
    {
        super(); 
        setPreferredSize(new Dimension(180,25)); 
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS)); 
        JLabel xLabel = new JLabel(label); 
        add(Box.createGlue());   // left spring
        add(xLabel); 
        add(Box.createHorizontalStrut(15)); 
    }
}


class PlainDataBox extends JPanel implements B4constants 
// user specifies how many columns width
{   
    public static final long serialVersionUID = 42L;

    private JTextField xField; 

    public PlainDataBox(int ig, int im, int columns)
    // ig=groupNumber; im=fieldNumber, columns=charWidth.
    {
        super(); 
        setPreferredSize(new Dimension(100,20)); 
        xField = new JTextField(DMF.reg.getuo(ig, im), columns); 
        int fwidth = 15 * columns; 
        xField.setMaximumSize(new Dimension(fwidth,20)); 
        add(xField); 
    }

    public String getText()
    {
        return xField.getText(); 
    }
}


class LabelDataBox extends JPanel implements B4constants 
// user specifies how many columns width
{   
    public static final long serialVersionUID = 42L;

    private JTextField xField; 

    public LabelDataBox(int ig, int im, int columns)
    // ig=groupNumber; im=fieldNumber, columns=charWidth.
    {
        super(); 
        setPreferredSize(new Dimension(100,20));
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS)); 
        JLabel xLabel = new JLabel(UO[ig][im][0]); 
        xField = new JTextField(DMF.reg.getuo(ig, im), columns); 
        int fwidth = 15 * columns; 
        xField.setMaximumSize(new Dimension(fwidth,20)); 
        add(Box.createGlue());   // left spring fails. Why????
        add(xLabel); 
        add(Box.createHorizontalStrut(5)); 
        add(xField); 
    }

    public String getText()
    {
        return xField.getText(); 
    }
}


class LabelNarrowTextBox extends JPanel implements B4constants 
// width is fixed and narrow
{   
    public static final long serialVersionUID = 42L;
    private int columns = 6; 
    private JTextField xField; 

    public LabelNarrowTextBox(int ig, int im)
    // ig=groupNumber; im=fieldNumber, columns=charWidth.
    {
        super(); 
        setPreferredSize(new Dimension(100,20));
        setMaximumSize(getPreferredSize()); 
        setMinimumSize(getPreferredSize()); 
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS)); 
        JLabel xLabel = new JLabel(UO[ig][im][0]); 
        xField = new JTextField(DMF.reg.getuo(ig, im), columns); 
        int fwidth = 60;  
        xField.setMaximumSize(new Dimension(fwidth,20)); 
        xField.setMinimumSize(getMaximumSize()); 
        add(xLabel); 
        add(Box.createHorizontalStrut(5)); 
        add(xField); 
    }

    public String getText()
    {
        return xField.getText(); 
    }
}


class LabelWideTextBox extends JPanel implements B4constants 
// user specifies how many columns width
{   
    public static final long serialVersionUID = 42L;

    private JTextField xField; 

    public LabelWideTextBox(int ig, int im, int columns)
    // ig=groupNumber; im=fieldNumber, columns=charWidth.
    {
        super(); 
        setPreferredSize(new Dimension(300,20));
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS)); 
        JLabel xLabel = new JLabel(UO[ig][im][0]); 
        xField = new JTextField(DMF.reg.getuo(ig, im), columns); 
        int fwidth = 15 * columns; 
        // xField.setMaximumSize(new Dimension(fwidth,20)); 
        // add(Box.createGlue());   // left spring fails. Why????
        add(xLabel); 
        add(Box.createHorizontalStrut(5)); 
        add(xField); 
    }

    public String getText()
    {
        return xField.getText(); 
    }
}

class LabelBitBox extends JPanel implements B4constants 
// helper for checkboxes
{
    public static final long serialVersionUID = 42L;

    private JCheckBox cBox; 

    public LabelBitBox(int ig, int im)
    {
        super();     // initialize the JPanel
        setPreferredSize(new Dimension(180, 25)); 
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS)); 
        cBox = new JCheckBox(UO[ig][im][0], "T".equals(DMF.reg.getuo(ig,im)));
        cBox.setHorizontalTextPosition(AbstractButton.LEFT); 
        add(Box.createGlue());   // left spring
        add(cBox); 
    }

    public boolean isSelected()
    {
        return cBox.isSelected(); 
    }
}


class DebugBox extends JPanel implements B4constants 
// Does not use UserOptions.   For Debug level only.
// user specifies prevLevel and how many columns width.
{   
    public static final long serialVersionUID = 42L;

    private JTextField xField; 

    public DebugBox(int ilevel,  int columns)
    // ilevel is the current debug level.
    {
        super(); 
        setPreferredSize(new Dimension(100,20)); 
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS)); 
        JLabel xLabel = new JLabel("Debug level"); 
        String sCurrent = U.fwi(ilevel,2); 
        xField = new JTextField(sCurrent, columns); 
        int fwidth = 15 * columns; 
        xField.setMaximumSize(new Dimension(fwidth,20)); 
        add(Box.createGlue());   // left spring fails. Why????
        add(xLabel); 
        add(Box.createHorizontalStrut(5)); 
        add(xField); 
    }

    public String getText()
    {
        return xField.getText(); 
    }
}




class BorVertRadioBox extends JPanel implements B4constants 
// helper for radio button groups
{
    public static final long serialVersionUID = 42L;

    private ButtonGroup bg; 
    int igg, imm; 
    int nbuttons; 

    public BorVertRadioBox(String title, int ig, int im, int gbuttons)
    {
        super(); 
        nbuttons = gbuttons; 
        igg = ig;
        imm = im; 
        RadioRepair.fixemup(ig, im, nbuttons); 

        int extra = title.length() > 0 ? 25 : 15; 
        setPreferredSize(new Dimension(190, 24*nbuttons+extra)); 
        setMaximumSize(getPreferredSize()); 
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        bg = new ButtonGroup(); 
        for (int i=0; i<nbuttons; i++)
        {
            JPanel jp = new JPanel(); 
            jp.setPreferredSize(new Dimension(180, 24)); 
            jp.setMaximumSize(jp.getPreferredSize()); 
            jp.setLayout(new BoxLayout(jp, BoxLayout.X_AXIS)); 
            jp.add(Box.createGlue()); // horizontal spring
            boolean chosen = "T".equals(DMF.reg.getuo(igg,imm+i)); 
            JRadioButton jrb = new JRadioButton(UO[igg][imm+i][0], chosen);
            jrb.setActionCommand(UO[igg][imm+i][0]); 
            jrb.setHorizontalTextPosition(AbstractButton.LEFT); 
            jp.add(jrb); 
            this.add(jp); 
            bg.add(jrb); 
        }
        this.setBorder(BorderFactory.createTitledBorder(title));
    }

    public boolean isSelected(int i)  // i=0, 1, ...
    {
        String s = bg.getSelection().getActionCommand(); 
        return UO[igg][imm+i][0].equals(s); 
    }
}







class BorVertRadioField extends JPanel implements B4constants 
// helper for radio button group with a text field. 
// see Options::Random::Distributions.
{
    public static final long serialVersionUID = 42L;

    private ButtonGroup bg; 
    private JTextField jt; 
    int igg, imm; 
    int nbuttons; 

    public BorVertRadioField(String title, int ig, int im, int gbuttons)
    {
        super(); 
        nbuttons = gbuttons; 
        igg = ig;
        imm = im; 
        RadioRepair.fixemup(ig, im, nbuttons); 

        int extra = title.length() > 0 ? 45 : 35; 
        setPreferredSize(new Dimension(190, 24*nbuttons+extra)); 
        setMaximumSize(getPreferredSize()); 
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        bg = new ButtonGroup(); 
        for (int i=0; i<nbuttons; i++)
        {
            JPanel jp = new JPanel(); 
            jp.setPreferredSize(new Dimension(180, 24)); 
            jp.setMaximumSize(jp.getPreferredSize()); 
            jp.setLayout(new BoxLayout(jp, BoxLayout.X_AXIS)); 
            jp.add(Box.createGlue()); // horizontal spring
            boolean chosen = "T".equals(DMF.reg.getuo(igg,imm+i)); 
            JRadioButton jrb = new JRadioButton(UO[igg][imm+i][0], chosen);
            jrb.setActionCommand(UO[igg][imm+i][0]); 
            jrb.setHorizontalTextPosition(AbstractButton.LEFT); 
            jp.add(jrb); 
            this.add(jp); 
            bg.add(jrb); 
        }
        
        //----now add the labelled text field--------
        JPanel jp = new JPanel(); 
        jp.setLayout(new BoxLayout(jp, BoxLayout.X_AXIS)); 
        jp.setPreferredSize(new Dimension(180, 25));
        jp.setMaximumSize(jp.getPreferredSize()); 
        
        JLabel jl = new JLabel("   "+UO[ig][imm+nbuttons][0]); 
        jl.setPreferredSize(new Dimension(240, 24)); 
        jl.setMaximumSize(jl.getPreferredSize()); 

        // jl.setBorder(BorderFactory.createCompoundBorder(           // debugging
        //            BorderFactory.createLineBorder(Color.red),
        //            jl.getBorder()));

        jt = new JTextField(DMF.reg.getuo(igg, imm+nbuttons), 8);
        jt.setPreferredSize(new Dimension(20, 24)); 
        jt.setMaximumSize(jt.getPreferredSize());  
        jt.setActionCommand(UO[ig][imm+nbuttons][0]);

        // jt.setBorder(BorderFactory.createCompoundBorder(           // debugging
        //            BorderFactory.createLineBorder(Color.red),
        //            jt.getBorder()));

        jp.add(jl); 
        // jp.add(Box.createRigidArea(new Dimension(5,0))); // bit of space
        jp.add(jt); 
        this.add(jp); 
        
        this.setBorder(BorderFactory.createTitledBorder(title));
    }

    public boolean isSelected(int i)  // i=0, 1, ...
    {
        String s = bg.getSelection().getActionCommand(); 
        return UO[igg][imm+i][0].equals(s); 
    }
    
    public String getText()  // pulls the text out of JTextArea.
    {
        return (jt==null) ? "" : jt.getText(); 
    }
}







class BorHorizRadioBox extends JPanel implements B4constants 
// helper for radio button groups; gets its button labels from UO array.
{
    public static final long serialVersionUID = 42L;

    private ButtonGroup bg; 
    int igg, imm;  
    int nbuttons; 

    public BorHorizRadioBox(String title, int ig, int im, int gbuttons)
    {
        super();  
        igg = ig; 
        imm = im;
        nbuttons = gbuttons; 

        RadioRepair.fixemup(ig, im, nbuttons); 

        // trying to get the fourth button to appear.  Works!
        setPreferredSize(new Dimension(80*nbuttons, 75));  // was 180,75
        // setPreferredSize(new Dimension(50*nbuttons, 75));  // was 180,75
        setMinimumSize(getPreferredSize()); 
        // retain this JPanel's default flow layout for even spacing
        bg = new ButtonGroup(); 
        for (int i=0; i<nbuttons; i++)
        {
            boolean chosen = "T".equals(DMF.reg.getuo(igg, imm+i)); 
            JRadioButton jrb = new JRadioButton(UO[igg][imm+i][0], chosen);
            jrb.setActionCommand(UO[igg][imm+i][0]); 
            // jrb.setVerticalTextPosition(AbstractButton.BOTTOM); // looks bad
            jrb.setVerticalTextPosition(AbstractButton.TOP);       // looks good
            jrb.setHorizontalTextPosition(AbstractButton.CENTER); 
            this.add(jrb); 
            bg.add(jrb); 
        }
        this.setBorder(BorderFactory.createTitledBorder(title));
    }

    public boolean isSelected(int i)  // i=0, 1, ...
    {
        String s = bg.getSelection().getActionCommand(); 
        return UO[igg][imm+i][0].equals(s); 
    }
}



class BorHorizStereoBox extends JPanel implements B4constants 
// helper for stereo settings
{
    public static final long serialVersionUID = 42L;

    private ButtonGroup bg; 
    private JTextField jt; 
    int igg, imm; 

    public BorHorizStereoBox(String title, int ig, int im, JFrame frame)
    {
        super(); 
        igg = ig;
        imm = im;  
        final int NCHARS = 6; 
        int nbuttons = 3;  // whiteBkg, blackBkg, stereo 

        RadioRepair.fixemup(ig, im, nbuttons); 

        setPreferredSize(new Dimension(300, 90));  // plenty wide enough for 3 elements
        setMinimumSize(getPreferredSize());        // never clip these elements
        // retain this JPanel's default flow layout for even spacing

        //------first, do the three buttons-----------------
        bg = new ButtonGroup(); 
        for (int i=0; i<nbuttons; i++)
        {
            JPanel jp = new JPanel(); 
            jp.setPreferredSize(new Dimension(40, 50)); 
            jp.setMaximumSize(jp.getPreferredSize()); 
            boolean chosen = "T".equals(DMF.reg.getuo(igg, imm+i)); 
            JRadioButton jrb = new JRadioButton(UO[igg][imm+i][0], chosen);
            jrb.setActionCommand(UO[igg][imm+i][0]); 
            jrb.setVerticalTextPosition(AbstractButton.TOP); 
            jrb.setHorizontalTextPosition(AbstractButton.CENTER); 
            jp.add(jrb); 
            this.add(jp); 
            bg.add(jrb); 
        }

        //------then set up the text field----------------
        JPanel jp = new JPanel(); 
        jp.setLayout(new GridLayout(2,1));           // 2 rows one column
        jp.setPreferredSize(new Dimension(80, 35));
        // jp.setMaximumSize(jp.getPreferredSize()); 

        jt = new JTextField(DMF.reg.getuo(igg, imm+3), NCHARS);
        jt.setMaximumSize(new Dimension(60, 15));  
        jt.setActionCommand(UO[ig][imm+3][0]);
        JLabel jl = new JLabel("   "+UO[ig][imm+3][0]); 
        jl.setMaximumSize(new Dimension(60, 15)); 

        jp.add(jl); 
        jp.add(jt); 
        this.add(jp); 

        this.setBorder(BorderFactory.createTitledBorder(title));
    }

    public boolean isSelected(int i)
    {
        if (bg != null)
        {
           String s = bg.getSelection().getActionCommand(); 
           return UO[igg][imm+i][0].equals(s); 
        }
        else
          return true; 
    }

    public String getText()
    {
        if (jt != null)
          return jt.getText(); 
        else
          return ""; 
    }
}



class BorderedCheckBoxRow extends JPanel implements B4constants 
// Helper for n checkboxes in a row; n<=12.
{
    public static final long serialVersionUID = 42L;

    int igg, imm; 
    JCheckBox cBox[] = new JCheckBox[12]; 

    public BorderedCheckBoxRow(String title, int ig, int im, int n, JFrame frame)
    {
        super();     // initialize this JPanel
        igg = ig; 
        imm = im;  
        setPreferredSize(new Dimension(40*n, 75)); /// was 30*n
        setMinimumSize(getPreferredSize());        /// avoid clipping!
        // retain this JPanel's default flow layout for even spacing
        for (int i=0; i<n; i++)
        { 
            cBox[i] = new JCheckBox(UO[igg][imm+i][0], 
              "T".equals(DMF.reg.getuo(igg, imm+i)));
            cBox[i].setHorizontalTextPosition(AbstractButton.CENTER); 
            cBox[i].setVerticalTextPosition(AbstractButton.TOP);  
            this.add(cBox[i]); 
        }
        this.setBorder(BorderFactory.createTitledBorder(title));
    }

    public boolean isSelected(int i)  // i=0...n-1
    {
        return cBox[i].isSelected();
    }
}


class BorderedDataBoxRow extends JPanel implements B4constants 
// Helper for n databoxes in a row; n<=3.  Used only by LayoutOptions.
{
    public static final long serialVersionUID = 42L;

    int igg, imm; 
    JTextField jt[] = new JTextField[3];
    JPanel jp[] = new JPanel[3];
    JLabel jl[] = new JLabel[3]; 
    int NCHARS = 5; 
    
    public BorderedDataBoxRow(String title, int ig, int im, int n, JFrame frame)
    {
        super();     // initialize this JPanel
        igg = ig; 
        imm = im;  
        setPreferredSize(new Dimension(30*n, 70)); 
        setMinimumSize(getPreferredSize()); 
        // retain this JPanel's default flow layout for even spacing
        for (int i=0; i<n; i++)
        { 
            jp[i] = new JPanel(); 
            jp[i].setLayout(new GridLayout(2,1));  // 2 rows, 1 column
            jp[i].setPreferredSize(new Dimension(60, 35));
            
            jl[i] = new JLabel(UO[ig][imm+i][0]); 
            jl[i].setMaximumSize(new Dimension(60,15)); 
            
            jt[i] = new JTextField(DMF.reg.getuo(igg, imm+i), 2); // 2 chars width
            jt[i].setMaximumSize(new Dimension(20,15)); 
            jt[i].setActionCommand(UO[ig][imm+i][0]); 
            
            jp[i].add(jl[i]); 
            jp[i].add(jt[i]); 
            this.add(jp[i]); 
        }
        this.setBorder(BorderFactory.createTitledBorder(title));
    }

    public String getText(int i)  // i=0...n-1
    {
        return jt[i].getText();
    }
}



class BorderedHexBox extends JPanel
// JPanel helper for hexagonal numbers
{
    public static final long serialVersionUID = 42L;

    int ncirc=1, nrays=1; 
    String srays; 
    JSpinner jspin; 
    JLabel jrings, jrays; 

    public BorderedHexBox(String title, int ig, int im, int nmax, int dn, JFrame frame)
    // dn is a ray count correction term: zero for Gaussian, +1 for Uniform.
    {
        super();     // initialize this JPanel
        final int mydn = dn; 
        setPreferredSize(new Dimension(200, 75)); 
        setMinimumSize(getPreferredSize()); 
        // retain this JPanel's default flow layout for even horiz spacing
        String ss = DMF.reg.getuo(ig, im);
        ncirc = U.suckInt(ss); 
        ncirc = Math.max(1, Math.min(nmax, ncirc)); 
        nrays = mydn + 3*ncirc + 3*ncirc*ncirc; 
        jrings = new JLabel("Nrings:"); 
        this.add(jrings); 
        SpinnerNumberModel model = new SpinnerNumberModel(ncirc, 1, nmax, 1); 
        jspin = new JSpinner(model); 
        jspin.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                ncirc = ((Integer) jspin.getValue()).intValue(); 
                nrays = mydn + 3*ncirc + 3*ncirc*ncirc; 
                srays = "    Nrays = "+U.fwi(nrays,6).trim(); 
                jrays.setText(srays); 
            }
        });
        this.add(jspin); 
        jrays = new JLabel("    Nrays = "+U.fwi(nrays,6).trim()); 
        this.add(jrays); 

        this.setBorder(BorderFactory.createTitledBorder(title));
    }

    public int getNcircles()
    {
        return ncirc;
    }

    public String getText()
    {
        return U.fwi(ncirc, 6).trim(); 
    }
}



class BorderedMultiPlotBox extends JPanel implements B4constants 
// JPanel helper for MultiPlot options.
{
    public static final long serialVersionUID = 42L;

    final int NCHARS = 6; 
    final int NWIDE = 40;  // chars for wide edit field
    int nplots = 1;
    JSpinner jsNums;
    JLabel jlHow; 
    JPanel jpSpin; 
    LabelNarrowTextBox myLNT1, myLNT2, myLNT3; 
    LabelWideTextBox myLWT1, myLWT2, myLWT3; 

    public BorderedMultiPlotBox(String title, int ig, int im, int maxspin, JFrame frame)
    // ig = group= 4; im=index into group; maxspin=maxplots=9; 
    {
        super(); 
        setPreferredSize(new Dimension(200, 130)); 
        // setMinimumSize(getPreferredSize()); 
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); 

        String ss = DMF.reg.getuo(ig,im);
        int nplots = U.suckInt(ss); 
        nplots = Math.max(1, Math.min(maxspin, nplots)); 

        // first, get its user preference from  DMF.reg.getuo(ig,im)
        String sUser = DMF.reg.getuo(ig,im).trim(); 
        nplots = U.minmax(U.suckInt(sUser), 1, maxspin); 
        SpinnerNumberModel numModel = new SpinnerNumberModel(1,1,maxspin,1); 
        try
        {
            numModel.setValue(nplots); 
        }
        catch (IllegalArgumentException iae)
        {
            numModel.setValue(1); 
        }
        jsNums = new JSpinner(numModel); 
        jpSpin = new JPanel(); 
        jlHow = new JLabel("How many plots along this axis?"); 
        jpSpin.add(jlHow); 
        jpSpin.add(jsNums); 
        this.add(jpSpin); 

        Box box1 = Box.createHorizontalBox(); 
        myLNT1 = new LabelNarrowTextBox(ig,im+1); 
        myLWT1 = new LabelWideTextBox(ig,im+2,NWIDE); 
        box1.add(myLNT1);
        box1.add(Box.createHorizontalStrut(10)); 
        box1.add(myLWT1); 

        Box box2 = Box.createHorizontalBox(); 
        myLNT2 = new LabelNarrowTextBox(ig,im+3); 
        myLWT2 = new LabelWideTextBox(ig,im+4,NWIDE); 
        box2.add(myLNT2);
        box2.add(Box.createHorizontalStrut(10)); 
        box2.add(myLWT2); 

        Box box3 = Box.createHorizontalBox(); 
        myLNT3 = new LabelNarrowTextBox(ig,im+5); 
        myLWT3 = new LabelWideTextBox(ig,im+6,NWIDE); 
        box3.add(myLNT3);
        box3.add(Box.createHorizontalStrut(10)); 
        box3.add(myLWT3); 

        this.add(box1); 
        this.add(box2); 
        this.add(box3); 
        this.setBorder(BorderFactory.createTitledBorder(title)); 
    }

    public String getN()
    {
        return jsNums.getValue().toString().trim(); 
    }

    public String getVar1()
    {
        return myLNT1.getText(); 
    }

    public String getStr1()
    {
        return myLWT1.getText();
    }

    public String getVar2()
    {
        return myLNT2.getText(); 
    }

    public String getStr2()
    {
        return myLWT2.getText(); 
    }

    public String getVar3()
    {
        return myLNT3.getText(); 
    } 

    public String getStr3()
    {
        return myLWT3.getText(); 
    }
} //---end of BorderedMultiPlot-----




class BorderedMapBox extends JPanel
// JPanel helper for map options
{
    public static final long serialVersionUID = 42L;

    final int NCHARS = 6; 
    LabelDataBox dbVar, dbStep, dbNpts, dbNpix, dbCen, dbPvar, dbPstep; 

    public BorderedMapBox(String title, int ig, int im, JFrame frame)
    {
        super();  
        setPreferredSize(new Dimension(200, 170)); 
        setMinimumSize(getPreferredSize()); 
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); 

        dbVar = new LabelDataBox(ig, im, NCHARS);      // which var
        this.add(dbVar); 
        dbStep = new LabelDataBox(ig, im+1, NCHARS);   // step size
        this.add(dbStep); 
        dbNpts = new LabelDataBox(ig, im+2, NCHARS);   // how many points
        this.add(dbNpts); 
        // dbNpix = new LabelDataBox(ig, im+3, NCHARS);   // how many pixels
        // this.add(dbNpix); 
        dbCen = new LabelDataBox(ig, im+4, NCHARS);    // center value
        this.add(dbCen); 
        dbPvar = new LabelDataBox(ig, im+5, NCHARS);   // parallax var
        this.add(dbPvar); 
        dbPstep = new LabelDataBox(ig, im+6, NCHARS);  // parallax step
        this.add(dbPstep); 
        this.setBorder(BorderFactory.createTitledBorder(title));
    }

    public String getVar()
    {
        return dbVar.getText(); 
    }

    public String getStep()
    {
        return dbStep.getText();
    }
    
    public String getNpts()
    {
        return dbNpts.getText(); 
    }
    
    public String getNpix()
    {
        return dbNpix.getText();
    }

    public String getCen()
    {
        return dbCen.getText(); 
    }

    public String getPvar()
    {
        return dbPvar.getText(); 
    }

    public String getPstep()
    {
        return dbPstep.getText(); 
    } 
} //---end of BorderedMapBox----





class RadioRepair
// If all radio buttons are false, RadioButton crashes.
// This fixemup() makes sure that exactly one button is set. 
{
    public static final long serialVersionUID = 42L;

    static void fixemup(int ig, int im, int nbuttons)
    {
        int count = 0; 
        for (int i=0; i<nbuttons; i++)
          if ("T".equals(DMF.reg.getuo(ig, im+i)))
            count++; 
        if (count == 1)
          return; 
        for (int i=0; i<nbuttons; i++)
          DMF.reg.putuo(ig, im+i, (i==0) ? "T" : "F"); 
    }
}       
