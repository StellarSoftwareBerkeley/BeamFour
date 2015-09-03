package com.stellarsoftware.beam;

import java.util.*;              // Timer
import java.awt.*;               // Color, etc
import java.awt.event.*;         // MouseEvent
import java.awt.print.*;         // printing
import javax.print.attribute.*;  // printing attributes
import java.io.*;                // files;QuickPNG.
import java.awt.image.*;         // QuickPNG; 
import javax.imageio.*;          // QuickPNG; 
import javax.swing.*;
import javax.swing.event.*;      // MenuEvents, InternalFrameAdapter

@SuppressWarnings("serial")

/** GJIF.java
  * A Graphic JInternalFrame class.
  *
  * Each instance holds a graphic annotation panel GPanel
  * that performs graphics art rendering, annotation, CAD, etc.
  * Action requests from DMF come here for adjudication,
  * and are usually forwarded to the appropriate AnnoPanel to execute.
  *
  * The caret blinking is managed in BJIF. 
  *
  * @author: M.Lampton (c) 2004 STELLAR SOFTWARE all rights reserved.
  */
public class GJIF extends BJIF implements B4constants
{
    // public static final long serialVersionUID = 42L;

    protected int myType;          // remember what type of panel I contain!
    private   int iCountdown;      // manage temporary title
    private   String myName;       // permanent; used in Options ID and titlebar.
    private   String myWarn;       // blank, else warning overrides myName
    private   JMenuItem myjmi;     // remember the JMenuItem for ungraying
    private   GPanel myGPanel;       // The panel that is contained herein. 

    boolean bExitOK(Component c)
    {
        return true;
    }

    public GJIF(int gtype, String gname, JMenuItem gjmi)
    // This is called by DMF with the above three data. 
    {
        super(gname);            // set up BJIF & post title.
        myType = gtype;          // type of panel requested
        myName = gname;          // save my name
        myWarn = "";             // no warnings yet
        myjmi = gjmi;            // save menuItem for enable-on-close

        int size = getUOpixels(); 
        super.setSize(size, size); 
        setTitle(gname);         // for generic identification, Feb 2011
        myjmi.setEnabled(false); // initially gray the menuItem 

        switch (myType) /// cases RM_INOUT, RM_RANDOM, RM_AUTO have no panel
        {
            case RM_LAYOUT: myGPanel = new LayoutPanel(this); break; 
            case RM_PLOT2:  myGPanel = new Plot2Panel(this);  break; 
            case RM_MPLOT:  myGPanel = new MPlotPanel(this);  break; 
            case RM_MAP:    myGPanel = new MapPanel(this);    break; 
            case RM_PLOT3:  myGPanel = new Plot3Panel(this);  break; 
            case RM_H1D:    myGPanel = new H1DPanel(this); break;
            case RM_H2D:    myGPanel = new H2DPanel(this); break; 
            case RM_MTF:    myGPanel = new MTFPanel(this); break; 
            case RM_DEMO:   myGPanel = new DemoPanel(this); break; 
            default:        U.beep(); return; 
        } 
        setContentPane(myGPanel); 
        addInternalFrameListener(new InternalFrameAdapter() 
        {
            public void internalFrameClosed(InternalFrameEvent ife)
            {
                myjmi.setEnabled(true);
            }

            public void internalFrameActivated(InternalFrameEvent ife)
            {
               if (myGPanel != null)
                 myGPanel.doQualifiedRedraw();  // depends on nEdits.
            }
        });
        setVisible(true); 
        toFront(); 
    } //-------------end of GJIF constructor---------------




    //--------titlebar management tools-----------------

    public void postWarning(String warn)
    // Client panel's getUOWarning() should call this method.
    // Calling with a null string will clear the warning. 
    // GJIF's title is variable; myName is permanent. 
    {
        myWarn = warn; 
        setTitle( (myWarn.length() > 0) ? myWarn : myName); 
    } 
    
    public void postCoords(String coords)
    // Client's doCursor() delivers its string here for display.
    {
        if (myWarn.length() < 1)
          setTitle(coords);
    }

    public void cleanupTitle()
    // Client GPanel doCursor() should call this when cursor exits.
    {
        setTitle( (myWarn.length() > 0) ? myWarn : myName); 
    }
    
    public void cleanupTitle(String s)
    // Allows client doCursor to have several alternative names
    // depending on its state: e.g. Layout (normal) or Layout* (sticky).
    {
        setTitle((myWarn.length() > 0) ? myWarn : s); 
    }

    //--------generic tools serving GPanel client needs--------------    
    
    public String getName()  // needed for Options identification
    {
        return myName; 
    }
    
    public int getType()
    {
        return myType; 
    }


    public GPanel getGPanel()
    // allows AutoAdjust to request LayoutPanel:requestNewArtwork()
    {
        return myGPanel; 
    }

    public void doCAD()  // DMF request pass thru to myGPanel
    {
        myGPanel.doCAD(); 
    }

    public void doUpdateUO()  // Options change => fresh art. 
    {
        myGPanel.doUpdateUO(); 
    }

    public void doSaveData()      // Options button => data file.
    {
        myGPanel.doSaveData();
    }

    public void doWriteHisto()    // DMF same deal. 
    {
        myGPanel.doSaveData(); 
    }

    public void tryPrint()
    // http://www.javacommerce.com/displaypage.jsp?name=printcode.sql&id=18252
    {
        if (myGPanel == null)
          return; 
        PrinterJob job = PrinterJob.getPrinterJob(); 
        job.setPrintable(myGPanel); 
        if (job.printDialog()) 
        {
            try { job.print(); }
            catch (PrinterException pe)
            {
              JOptionPane.showMessageDialog(null, "Failed to print.");
            }
        }
    }


    //----------------private methods--------------------


    private int getUOpixels()
    // Returns User Option window size in pixels.
    {
        int i = U.parseInt(DMF.reg.getuo(UO_GRAPH, 6)); 
        if (i<=100)
          i = 500; 
        return Math.min(3000, Math.max(100, i)); 
    }
    
} //-----------------end of GJIF---------------------
