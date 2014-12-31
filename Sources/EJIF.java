package com.stellarsoftware.beam;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;          // printing
import java.awt.print.*;         // printing
import javax.print.attribute.*;  // printing attributes
import java.awt.font.*;          // font metric
import java.io.*;                // files;QuickPNG.

import java.awt.datatransfer.*;  // clipboard
import java.beans.*;             // vetoableChangeListener

import javax.swing.*;            // everything else
import javax.swing.event.*;      // for MenuEvents and InternalFrameAdapter

@SuppressWarnings("serial")

/**
  * EJIF.java  --- Abstract class,  EPanel in a JInternalFrame.
  * It extends BJIF thereby obtaining a blinker for caret management. 
  * It manages the JScrollBars. 
  *
  * In A163, building the horizontal scrollbar into a JPanel
  * leftward of a fixed block, installed in cPanel.South,
  * so its left scrollbutton never intrudes into the resize button.
  * The LR button now looks like Notepad's LR button: resize only. 
  *
  * In A158, trying to eliminate scrollbar create/destroy cycle,
  * in effort to stop the Mac self-scrolling/resize confusion.
  * Construct, install, do not destroy them. 
  *
  * It is extended to OEJIF, REJIF, MEJIF. 
  * It manages its own posted title, including a temporary title. 
  * Uses paintComponent() to update its title. 
  * 
  * This file also contains one private class..
  *   PrintText
  *
  * DMF.nEdits increments with each change; for graphics refreshing. 
  *
  * static clipboard introduced, A65, allows demo editions
  *
  * A142: introducing bNeedsParse flag; triggered by BJIF timer. 
  * This removes all calls to DMF.vMasterParse(): called only by BJIF.
  *
  * Task-specific parser is added by each extension.
  * Each concrete class OEJIF REJIF MEJIF should call EJIF constructor,
  * and of course supply its own parse() method. 
  *
  * Uses: utilities class U is used only for debugging
  *
  *------------------- EJIF public methods--------------------------
  *
  *  abstract void parse();             // each extension supplies this.
  *
  *  public String getFieldTrim(f,r)    // all extensions
  *  public double getFieldDouble(f,r)  // all extensions
  *  public char   getTag(f,r)          // all extensions
  *
  *  public void putField(f,r,s)        // inout
  *  public void putFieldDouble(f,r,d)  // inout, auto
  *  public void putBlank(f,r)          // inout
  *  public void forceFieldDouble(f,r,d)// ray generators
  *  public void refreshSizes()         // upon major edits
  *
  *  public EJIF()                      // DMF
  *  public void focusPanel()           // DMF ????
  *  public void setCaretXY(i,j)        // DMF
  *  public void pleaseSave()           // DMF
  *  public void pleaseSaveAs()         // DMF
  *  public boolean bExitOK()           // DMF
  *  public boolean areYouEmpty()       // DMF
  *  public boolean areYouDirty()       // DMF
  *  public void setClean()             // DMF
  *  public boolean doYouNeedParsing()  // DMF
  *  public boolean areYouMarked()      // DMF
  *  public void doPrint()              // DMF
  *  public void runTest(which)         // early testing only
  *  public static void setOverwrite()  // bimodal only
  *  public void doCut()                // DMF only
  *  public void doCopy()               // DMF only
  *  public void doPasteInto()          // DMF only
  *  public void doDelete()             // DMF only
  *  public void doSelectAll()          // DMF only
  *  public void doStashUndo()          // AutoAdjust
  *
  *  ---------------------protected EJIF methods------------------
  *  protected boolean bHaveBuffer()
  *  protected int getNumRecords()    
  *  protected void updateAndShowInfo()
  *
  *  ------------------------private EJIF-------------------------
  *
  *  private String getFieldFull(int field, int row)
  *  private void putTag(int field, int row, char c)
  *
  *
  * Special keys in KeyHandler: F10=copyDown, F11=narrow, F12=widen.
  * Alt Down performs CopyFieldDown.
  * There is no Alt Up to perform CopyFieldUp.
  *
  * Uses special handling of BackSpace for nondisruptive table edits.
  * Uses event.consume() Esc key which would otherwise disrupt table.
  * Uses event.consume() F10 key, avoid FileMenu popup. 
  *
  * -------------STARTUP management--------------
  * Three possibilities: New, Chooser, AutoStartup
  *        toOpen:        0     1        1
  *        fname:         -     -      FILENAME.EXT
  *
  * Autostartup default is local directory
  * Chooser default is "MyDocuments"  -- make this specifiable?
  * To adopt the current working directory H&C p.474 suggest
  *   chooser.setCurrentWorkingDirectory(new File("."));
  * filename converts to a File: LJ p.303
  *
  *  @author M.Lampton (c) 2004 STELLAR SOFTWARE all rights reserved.
  */
abstract class EJIF extends BJIF implements B4constants, AdjustmentListener
{
    // public static final long serialVersionUID = 42L; // Xlint 8 Oct 2014
    
    //--------protected fields shared with extensions-----

    protected String myFpath;
    protected String myExt;               // remember my extension
    protected String myExtType;           // remember my extension and type; 
    protected int    myExtNumber = 0;     // 1, 2, 3 mean Opt, Ray, Med. 

    //-------private EJIF fields---------------------------

    private JMenuItem jmi1, jmi2;         // menuItems for ungraying.

    private int myStackNumber;            // remember my stacking number
    private int maxrecords;               // depends on editor type
    private boolean bDirty=false;         // avoid exit if unsaved changes
    // private boolean bNeedsParse=false; // inherit from host BJIF
    private int iCountdown = 0;           // manage temporary titles
    private File myFile=null; 
    private EPanel ePanel=null; 
    JScrollBar vsb=null, hsb=null; 
    private Container cPane=null;         // will contain ePanel.
    static private Clipboard clipb=null;  // local or system. 

    //---------------public methods and fields-------------------

    abstract void parse();   // supplied by extensions. 
 
    public EJIF(int which, int iLoc, String gExt, JMenuItem gjmi1, 
     JMenuItem gjmi2, boolean toOpen, String gFname, int gmaxrec)
    // (c) 2004 M.Lampton STELLAR SOFTWARE
    {
        super(gExt);          // set up BJIF 
        myExt = gExt;         // with initial period 
        super.setSize(INITIALEDITWIDTH, INITIALEDITHEIGHT); 
        super.setLocation(iLoc, iLoc); 

        myFpath = gFname; 
        maxrecords = gmaxrec; 

        //---Set up the JMenuItems for ungraying upon closing-------               
        jmi1 = gjmi1;             // save this within object for enable-on-close
        jmi2 = gjmi2;             // ditto
        myStackNumber = which;    // ditto
        myExt = gExt; 
        char c = U.getCharAt(myExt, 1); // skip initial period
        switch (c)
        {
           case 'o': 
           case 'O': myExtNumber = 1; break; 
           case 'r': 
           case 'R': myExtNumber = 2; break; 
           case 'm': 
           case 'M': myExtNumber = 3; break; 
           default:  myExtNumber = 0; // should never happen!
        }
        
        jmi1.setEnabled(false);                               
        jmi2.setEnabled(false);          

        // create and install the EPanel into cPane
        // but don't replace cPane! need it for sliders. 
        ePanel = new EPanel(this); 
        cPane = getContentPane();
        cPane.add(ePanel); 

        super.setKeyPanel(ePanel);

        // setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addVetoableChangeListener(new VetoableChangeListener()
        // add listener to EJIF, ok frame closing: H&C vol 2 p.545
        {
            public void vetoableChange(PropertyChangeEvent event)
            throws PropertyVetoException
            {  
               String name = event.getPropertyName();
               Object value = event.getNewValue();

               // Yikes! don't we want "closing" not "closed"?
               // Nope. H&C V2 p.545 say vetoable state is "closed".
               // For notification only, use "closing" H&C V2 p.547.

               if (name.equals("closed") && value.equals(Boolean.TRUE) && bDirty)
               {  
                  if (!bExitOK())
                    throw new PropertyVetoException("Cancelled", event);
               }
            }           
        });

        addInternalFrameListener(new InternalFrameAdapter()  
        {
            public void internalFrameActivated(InternalFrameEvent ife) 
            {
                // DMF.vMasterParse(true);  
            }

            public void internalFrameDeactivated(InternalFrameEvent ife) 
            {
                // DMF.vMasterParse(true); 
            }

            public void internalFrameClosed(InternalFrameEvent ife)
            {
                switch (myStackNumber) // cleanup: destroy this editor
                {
                    case 0: 
                       DMF.oejif = null; 
                       DMF.giFlags[OPRESENT] = 0; 
                       break; 
                    case 1: 
                       DMF.rejif = null; 
                       DMF.giFlags[RPRESENT] = 0; 
                       break; 
                    case 2: 
                       DMF.mejif = null; 
                       DMF.giFlags[MPRESENT] = 0; 
                       break; 
                }
                ePanel = null; 
                jmi1.setEnabled(true);
                jmi2.setEnabled(true);
                // DMF.vMasterParse(true); 
            }
        });
       
        // Prepare the startup file.
        // Three possibilities: New, Chooser, AutoStartup
        //        toOpen:        f     t        t
        //        fname:         -     -      FILENAME.EXT

        if ((ePanel != null) && toOpen)
        {
            if (myFpath.length() < 1)  // unspecified startup file
            {
                JFileChooser fc = new JFileChooser( );
                String sDir = DMF.sCurrentDir; 
                if (sDir != null)
                {
                   File fDir = new File(sDir); 
                   if (fDir.isDirectory())
                     fc.setCurrentDirectory(fDir);
                }
                fc.setFileFilter(new ExtFilter(myExt));     // H&C p.475
                int result = fc.showOpenDialog(this);
                if (result != JFileChooser.APPROVE_OPTION) 
                {
                    ePanel.vLoadSkeleton(); 
                    myFpath = "blank" + myExt; 
                    postEJIFtitle();
                    return;
                }
                myFile = fc.getSelectedFile();  // no exceptions thrown here
                DMF.sCurrentDir = myFile.getParent(); 
                if (bLoadFile(myFile))          // includes file safety checks
                {
                    myFpath = myFile.getPath(); // not getName().
                    postEJIFtitle();
                    DMF.nEdits++; 
                }
                else
                {
                    JOptionPane.showMessageDialog(this, "Could not load file");
                    ePanel.vLoadSkeleton(); 
                    myFpath = "blank" + myExt; 
                    postEJIFtitle(); 
                }
            }
            else  /////////////// specified autoload "myFname"
            {
                myFile = new File(myFpath);  // no exceptions thrown here
                if (bLoadFile(myFile))       // includes safety checks
                {
                    postEJIFtitle();
                    DMF.nEdits++; 
                }
                else
                {
                    JOptionPane.showMessageDialog(this, "Could not load file");
                    ePanel.vLoadSkeleton(); 
                    myFpath = "blank" + myExt; 
                    postEJIFtitle();
                }
            }
        }
        else  // startup with new skeleton...
        {
            ePanel.vLoadSkeleton(); 
            myFpath = "";
            postEJIFtitle(); 
            DMF.nEdits++; 
        }         
        // DMF.vMasterParse(true);
        bDirty = false; 
        bNeedsParse = true; 
        
    } /////end of constructor


    public String getFpath()
    // Essential to query myFile.getPath() for fresh information!
    // However, on opening a new table, myFile is null. 
    {
        if (myFile != null)
          myFpath = myFile.getPath(); 
        else
          myFpath = "";
        return myFpath;
    } 

    public void paintComponent(Graphics g)
    // Repaints its own temporary/permanent title each caret blink
    {
        postEJIFtitle(); 
    }



    public boolean bLoadFile(File f)  
    // Extracts string from file; calls ePanel.vLoadString(). 
    // No internal smarts about EOL or CSV/Tab.
    // Analogous to doPasteInto(). 
    // Needed improvements (A107): 
    //   1. Limit load to size acceptable to extension.
    //   2. Post a message if that limit is exceeded by file. 
    {
        try 
        {
           BufferedReader br = new BufferedReader(new FileReader(f)); 
           String text = null; 
           StringBuffer sb = new StringBuffer(); 
           while((text = br.readLine()) != null) 
           { 
              sb.append(text);
              sb.append("\n"); 
           } 
           br.close(); 
          
           if (sb.length() < 2)
             return false; 

           String s = new String(sb); 
           ePanel.vLoadString(s, true);    // preclear=true.
           if (getNumLines() > maxrecords+2)
             iCountdown = -10;              // warning.  
           return true; 
        } 
        catch (IOException e) 
        { return false; } 
    }


    class ExtFilter extends javax.swing.filechooser.FileFilter
    {
        String ext; 

        public ExtFilter(String gext)     // mandatory constructor
        {
            ext = gext.toLowerCase();     // example: ".med"
        }

        public boolean accept (File f)    // mandatory method
        {
            return f.getName().toLowerCase().endsWith(ext)
                  || f.isDirectory();
        }
     
        public String getDescription()    // mandatory method
        {
            return sGetType() + " file "; 
        }
    }


    //-----------public methods, continued-----------

    public void focusPanel()
    // Sets focus onto this frame's ePanel; called by DMF
    // Fails to receive focus if JFrame has got focus first.
    // Use this and do not try to focus the JFrame container.  
    // Caret is handled separately, not by frame/panel focus.
    // Focus is for keystrokes only. 
    {
        if (ePanel == null)
          return; 

        // focus enforcer: workAround focus bug ThreadID=604540
        SwingUtilities.invokeLater(new Runnable()
        {
           public void run()
           {
               ePanel.requestFocusInWindow();
           }
        });
    }

    public String sGetType()
    {
        switch (myExtNumber)
        {
            case 0:  return ""; 
            case 1:  return "OPT"; 
            case 2:  return "RAY";
            case 3:  return "MED";
            default: return ""; 
        }
    }
    
    public void vSetMostRecent()
    {
        switch (myExtNumber)
        {
           case 1: DMF.sMostRecentFilePathOpt = myFpath; break; 
           case 2: DMF.sMostRecentFilePathRay = myFpath; break; 
           case 3: DMF.sMostRecentFilePathMed = myFpath; break; 
        }
    }

    public void setCaretXY(int field, int row)
    // Set caret location for error display
    {
        if (ePanel != null)
          ePanel.setCaretXY(field, row); 
    }

    public int getCaretY()  // added A106 for AutoRayGen
    {
        return ePanel.getCaretY();
    }

    public void adjustmentValueChanged(AdjustmentEvent ae)
    // Public because EJIF is an adjustment listener
    {
        if (ae.getAdjustable().equals(vsb))
          ePanel.setVerticalPosition(vsb.getValue()); 
        else if (ae.getAdjustable().equals(hsb))
          ePanel.setHorizontalPosition(hsb.getValue());
    }

    public boolean bExitOK()
    {
        if (bDirty)
        {
            Object[] options = {"Don't close", "Abandon changes"}; 
            int result = JOptionPane.showOptionDialog(
                null,   // no parent frame
                "Unsaved changes in "+myExt,
                "Warning", 
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, 
                null,  // no custom icon
                options, 
                options[0]);  

            switch (result)
            {
                case JOptionPane.YES_OPTION: return false; 
                default: return true; // abandon OK.
            }
        }
        return true; // always exit=OK if clean. 
    }


    public boolean bExitOKold()
    {
        if (bDirty)
        {
            String s = "Abandon unsaved work in " + myExt + " ?"; 
            switch (JOptionPane.showConfirmDialog(null, s))
            {
                case JOptionPane.YES_OPTION: return true; 
                case JOptionPane.NO_OPTION:
                case JOptionPane.CANCEL_OPTION:
                default: return false; 
            }
        }
        return true; 
    }

    public boolean areYouDirty()
    {
        return bDirty; 
    }

    public void setDirty(boolean state)
    // Called "true" by EPanel when user inputs modify the table.
    // Called "false by EPanel when Epanel saves the file. 
    {
       bDirty = state;
       if (state)
         bNeedsParse = true; 
    } 


    public void pleaseSaveAs()
    {
        if (ePanel == null)  // should never happen
          return; 
        JFileChooser fc = new JFileChooser( );
        String sDir = DMF.sCurrentDir; 
        if (sDir != null)
        {
            File fDir = new File(sDir); 
            if (fDir.isDirectory())
              fc.setCurrentDirectory(fDir);
        }
        fc.setFileFilter(new ExtFilter(myExt));     // H&C p.475
        int result = fc.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION)
          return; 
        myFile = fc.getSelectedFile(); 
        if (myFile.exists() && !myFile.isFile())
          return;   // probably a directory.

        // we have a definite selection...
        DMF.sCurrentDir = myFile.getParent(); 

        // If no name or extension, supply one...
        String fp = myFile.getPath(); 
        if (fp.length() < 1)           // no name
          fp = "noname"; 

        // paths may legally contain periods. Do not use period test to find extension.

        String fu = fp.toUpperCase();
        boolean hasExt = fu.endsWith(".OPT") || fu.endsWith(".RAY") || fu.endsWith(".MED");
        if (!hasExt)                   // no legal extension?  
        {
            fp = fp + myExt;           // myExt includes the period.
            myFile = new File(fp); 
        }
        if (myFile.exists() && myFile.isFile())
        {
            int iq = JOptionPane.showConfirmDialog(null, "Overwrite existing file?");
            if (iq != JOptionPane.YES_OPTION)
              return; 
        }
        if (ePanel.save(myFile))
        {
            bDirty = false; 
            myFpath = myFile.getPath();
            vSetMostRecent();             
            iCountdown = 5;  // posts "Saved" message
            return; 
        }
        JOptionPane.showMessageDialog(null, "Could not save file"); 
    }


    public void pleaseSave()
    {
        if (ePanel == null) 
          return; 
        if (myFile == null)
          pleaseSaveAs(); 
        if (ePanel.save(myFile))
        {
            bDirty = false; 
            myFpath = myFile.getPath(); 
            vSetMostRecent(); 
            iCountdown = 5;  // posts "Saved" message
            return; 
        }
        JOptionPane.showMessageDialog(null, "Could not save file"); 
    }


    public void tryPrint()
    // http://www.javacommerce.com/displaypage.jsp?name=printcode.sql&id=18252
    {
        if (ePanel == null)
          return; 
        PrinterJob job = PrinterJob.getPrinterJob(); 
        job.setPrintable(new PrintText(ePanel)); 
        if (job.printDialog()) 
        {
            try { job.print(); }
            catch (PrinterException pe)
            {
               JOptionPane.showMessageDialog(null, "Failed to print.");
            }
        }
    }


    //-------public editing calls from DMF------

    public void doCut()
    {
        DMF.nEdits++; 
        clipb = Toolkit.getDefaultToolkit().getSystemClipboard();
        if (clipb == null)
          clipb = new Clipboard("local"); 

        String s = ePanel.getMarkedText(); 
        StringSelection ss = new StringSelection(s); 
        clipb.setContents(ss, null); 
        ePanel.doDelete(); 
        bDirty = true; 
        bNeedsParse = true; 
    }

    public void doCopy()
    // Copies marked table text onto the clipboard
    {
        DMF.nEdits++; 
        clipb = Toolkit.getDefaultToolkit().getSystemClipboard();
        if (clipb == null)
          clipb = new Clipboard("local"); 

        String s = ePanel.getMarkedText(); 
        StringSelection ss = new StringSelection(s); 
        clipb.setContents(ss, null); 
    }


    public void doPasteInto()
    // Pastes clipboard string into current table.
    // Does not paste over. 
    // Analogous to bLoadFile(). 
    {
        DMF.nEdits++; 
        clipb = Toolkit.getDefaultToolkit().getSystemClipboard();
        if (clipb == null)
          clipb = new Clipboard("local"); 

        ePanel.setCaretToMark(); 
        Transferable contents = clipb.getContents(null); 
        if (contents == null)
        {
            return;
        } 
        DataFlavor sf = DataFlavor.stringFlavor; 

        if (contents.isDataFlavorSupported(sf))
        {
           try
           {
              String s = (String) (contents.getTransferData(sf)); 
              ePanel.vLoadString(s, false);     // preclear=false
              if (getNumLines() > maxrecords+2)
                iCountdown = -10;                // warning.  
              bDirty = true; 
              bNeedsParse = true; 
           }
           catch(UnsupportedFlavorException ufe) {}
           catch (IOException ioe) {}
        }
    }

    public void doDelete()
    {
        DMF.nEdits++; 
        ePanel.doDelete(); 
        bDirty = true; 
        bNeedsParse = true; 
    }

    public void doSelectAll()
    {
        ePanel.doSelectAll(); 
    }

    public void doStashForUndo()
    {
        ePanel.stashForUndo();
    }
    
    //-----------protected internal methods for derived classes-----------

    protected void vPreParse(int results[])
    // Called by extended class to pre-gather table information.
    {
        for (int i=0; i<NGENERIC; i++)
          results[i] = 0; 
        if (ePanel == null)
          return; 
        results[GPRESENT] = 1; 
        results[GNLINES] = ePanel.getLineCount();
        int nguide = ePanel.getGuideNumber(); 
        results[GNRECORDS] = Math.min(nguide, results[GNLINES]-3); 
        results[GNFIELDS] = ePanel.getFieldInfo(); 
    }

    protected int getNumLines()     // extensions need this
    {
        if (ePanel == null)
          return 0; 
        return ePanel.getLineCount(); 
    } 

    protected int getNumFields()    // extensions use this.
    {
        if (ePanel == null)
          return 0; 
        return ePanel.getNumFields(); 
    }

    protected int getCaretFieldNum()
    {
        if (ePanel == null)
          return 0; 
        return ePanel.getCaretFieldNum(); 
    }

    protected boolean areYouMarked()
    {
        if (ePanel == null)
          return false; 
        return ePanel.isMarked(); 
    }

    protected int getGuideNumber()
    {
        if (ePanel == null)
          return 0; 
        return ePanel.getGuideNumber(); 
    }

    protected int getFieldWidth(int f)
    {
        if (ePanel == null)
          return 0; 
        return ePanel.getFieldWidth(f);
    }

    protected String getFieldTrim(int f, int r)
    {
        if (ePanel == null)
          return ""; 
        return ePanel.getFieldTrim(f, r); 
    }

    protected double getFieldDouble(int f, int r)
    {
        if (ePanel == null)
          return -0.0; 
        return ePanel.getFieldDouble(f, r); 
    }

    protected char getTag(int f, int r)
    {
        if (ePanel == null)
          return ' '; 
        return ePanel.getTagChar(f, r); 
    }

    protected void putField(int f, int r, String s)
    {
        if (ePanel == null)
          return; 
        ePanel.putFieldString(f, r, s); 
        bDirty = true; 
        // bNeedsParse = true; // not here this is calculation driven 
    }

    protected void putBlank(int f, int r)
    {
        if (ePanel == null)
          return; 
        ePanel.putFieldString(f, r, ""); 
        bDirty = true; 
        bNeedsParse = true; 
    }

    protected void putFieldDouble(int f, int r, double d)
    {
        if (ePanel == null)
          return; 
        ePanel.putFieldDouble(f, r, d); 
        bDirty = true; 
        // bNeedsParse = true; // not here this is calculation driven
    }

    protected void forceFieldDouble(int f, int r, double d)
    {
        if (ePanel == null)
          return;
        ePanel.forceFieldDouble(f, r, d); 
        bDirty = true; 
    }

    protected void refreshSizes()
    {
        if (ePanel == null)
          return; 
        ePanel.refreshSizes();
    }

    protected JScrollBar createHSB()  // create this one first
    {
        if ((cPane != null) && (hsb == null))
        {
            hsb = new JScrollBar(JScrollBar.HORIZONTAL, 0, 0, 0, IMAX-30); 
            hsb.addAdjustmentListener(this); // hsb messages to EdFrame
            JPanel southPane = new JPanel(); 
            southPane.setLayout(new BoxLayout(southPane, BoxLayout.X_AXIS));
            southPane.add(hsb); 
            southPane.add(Box.createRigidArea(new Dimension(16,10))); // resizer
            cPane.add(southPane, BorderLayout.SOUTH); 
            cPane.validate();  // reconfigure container
        }
        return hsb; 
    }
 
    protected JScrollBar destroyHSB()
    {
        /****in A158 do nothing****in A163 blank it?*****************
        if ((cPane != null) && (hsb != null))
        {
            hsb.removeAdjustmentListener(this); 
            cPane.remove(hsb);
            cPane.validate(); // reconfigure container
        } 
        hsb = null; 
        *****************************************************/
        return hsb; 
    }


    protected JScrollBar createVSB()  // create this one last. 
    {
        if ((cPane != null) && (vsb == null))
        {
            vsb = new JScrollBar(JScrollBar.VERTICAL, 0, 0, 0, JMAX-15); 
            vsb.addAdjustmentListener(this); // vsb messages to EdFrame
            cPane.add(vsb, BorderLayout.EAST); 
            cPane.validate();  // reconfigure container
        }
        return vsb; 
    }
 
    protected JScrollBar destroyVSB()
    {
        /****in A158 do nothing*************************
        if ((cPane != null) && (vsb != null))
        {
            vsb.removeAdjustmentListener(this); 
            cPane.remove(vsb);
            cPane.validate(); // reconfigure container
        } 
        vsb = null; 
        *************************************************/
        return vsb; 
    }


    //-------------private methods----------------

    private void postEJIFtitle()
    // Posts the appropriate title. 
    // Public so that its JPanel can request fresh title with caret location.
    // countdown > 0: post "Saving" status.
    // countdown < 0: post "Overflow" status.
    // countdown == 0: post filename. 
    // Uses the caret blink clock in BJIF's repaint() to call this countdown counter. 
    {
        String s = ""; 
        if (myFile != null)
          if (myFile.isFile())
          {
              if ("T".equals(DMF.reg.getuo(UO_EDIT, 0)))
                s += myFile.getPath();
              else
                s += myFile.getName();   // includes extension
          }
          
        if (iCountdown > 0)
        {
            setTitle("Saving...");
            iCountdown--; 
            return; 
        }
        if (iCountdown < 0) // changing the title forces an immediate repaint!
        {
            setTitle("Overflow");
            iCountdown++;
            return;
        }    
        if ((ePanel != null) && ("T".equals(DMF.reg.getuo(UO_EDIT, 1))))
        {
            String ss = "";
            switch (ePanel.getJCaret())
            {
                case 0:  ss="T"; break; 
                case 1:  ss="H"; break; 
                case 2:  ss="R"; break; 
                default: ss=Integer.toString(ePanel.getJCaret()-2); 
            }
            s +="  row=" + ss + "  col=" + (ePanel.getICaret()+1); 
        }
        
        s += "       " + sGetType() + "editor"; 
        setTitle(s); 
    }


} //-----------------ends EJIF class-----------------
























class PrintText implements Printable, B4constants
// may need revision for landscape/portrait.
{
    static private boolean bLandscape; 
    static private EPanel ePanel; 
    static private int nlines; 
    static private int npages; 
    static private int linesperpage; 
    static private int fontsize = 10; 

    public PrintText(EPanel gPanel)
    {
        bLandscape = false; 
        ePanel = gPanel;
        nlines = ePanel.getLineCount(); 
        fontsize = U.parseInt(DMF.reg.getuo(UO_EDIT, 6)); 
        fontsize = Math.max(5, Math.min(50, fontsize)); 
    }

    public int print(Graphics g, PageFormat f, int pageIndex)
    /// this method gets called once per page. 
    {
        Graphics2D g2 = (Graphics2D) g; 
        int xpts = (int) f.getImageableX();      // typically 72 points = 1 inch
        int ypts = (int) f.getImageableY();      // typically 72 points = 1 inch
        int wpts = (int) f.getImageableWidth();  // typically 468 points = 6.5 inches
        int hpts = (int) f.getImageableHeight(); // typically 648 points = 9 inches

        linesperpage = bLandscape ? wpts/fontsize-2 : hpts/fontsize-2;
        npages = 1 + nlines/linesperpage; 
        boolean bBold = "T".equals(DMF.reg.getuo(UO_EDIT, 7)); 
        int iBold = bBold ? Font.BOLD : Font.PLAIN; 
        g2.setColor(Color.BLACK); 
        g2.setFont(new Font("Courier New", iBold, fontsize)); 
        g2.setPaint(Color.BLACK); 
        setEditSmoothing(g2); 

        /// Now respond to any given pageIndex....

        if ((pageIndex < 0) || (pageIndex >= npages))
          return NO_SUCH_PAGE; 

        g2.translate(xpts, ypts);  // move to U.L.corner image area
        if (bLandscape)
        {
            g2.translate(0, hpts); 
            g2.rotate(-0.5*Math.PI); // minus=counterclockwise
        }
        int topline = pageIndex * linesperpage; 
        int botline = Math.min(nlines, topline+linesperpage-1); 
        for(int i=topline; i<=botline; i++)
          g2.drawString(ePanel.getLine(i), 0, fontsize*(i-topline+1)); 
        return PAGE_EXISTS; 
    }


    private void setEditSmoothing(Graphics2D g2)
    {
        if ("T".equals(DMF.reg.getuo(UO_EDIT, 8)))
        {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        else
        {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        }
    }
}  //-------------- end PrintText class------------------------
