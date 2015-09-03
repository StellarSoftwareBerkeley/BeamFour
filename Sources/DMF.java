
package com.stellarsoftware.beam;

import java.awt.*;              // all Abstract Windows Toolkit methods
import java.awt.event.*;        // mouse, keystroke etc
import java.awt.geom.*;         // for printing
import java.awt.print.*;        // for printing
import java.beans.*;            // vetoableChangeListenerFMI li
import java.io.*;               // all files and i/o methods
import java.util.*;             // Iterator, line 1282.
import javax.swing.*;           // all UI methods
import javax.swing.event.*;     // for MenuEvents and InternalFrameAdapter
import javax.swing.text.*;      // for BadLocationException
import java.awt.datatransfer.*; // for Drag-n-Drop
import java.awt.dnd.*;          // for Drag-n-Drop
import javax.swing.filechooser.FileNameExtensionFilter; 

@SuppressWarnings("serial")

/**
  * DMF: a Desktop with a Menu system in a JFrame.
  * 
  *
  * Basic idea is: Horstmann & Cornell vol.2. p.539
  *   1. use a plain JFrame for the application
  *   2. set its contentpane to a JDesktop pane "jdp"
  *   3. build JInternalFrame window elements.
  *   4. The jdp.add(myJIF)s are line 280, 291, 302, .. 1140.
  *   5. setSelected() can be vetoed! need to catch
  *     and process the PropertyVetoException...
  *
  * This edition uses distributed carets; each JIFrame has its own.
  * Caret support here is the public static boolean bHostActive flag.
  *
  * The caret blink timer is in BJIF our "Blinky JIF" class. 
  * QuickPNG screen capture is in BJIF to avoid capturing the caret. 
  * (only BJIF knows when the caret is off.)
  *
  * All user options are stashed in B4OPTIONS.TXT
  * Includes Drag-n-Drop for .OPT, .RAY, .MED filetypes (H&C vol.2 p.740).
  *
  * Includes public giFlags[] for i/o of all parsers & status. 
  *
  * Includes focus enforcer in EJIF and GJIF. 
  * This invokeLater() has a runnable() that
  * calls requestFocusInWindow(): EJIF line 160, GJIF line 63. 
  *
  *
  * A124: found that two menus use one accelerator, Control-A.
  * This is unwise.  I eliminated Control-A from SaveAs, now unaccelerated.
  * Microsoft, and TextEdit apps, do not accelerate SaveAs. 
  *   Accelerators are of the form KeyEvent.VK_A, etc:
  *   File accelerators are... O, R, M, S
  *   Edit accelerators are... X, C, V, D, A
  * A125: created a refresher of these nine JMenuItems to run when
  * the main window regains focus or is activated. 
  *  ^^ PROBLEM INTRODUCED HERE: A127 menu item graying is now messed up.
  *  ^^ Removing the reinstallAccelerator() feature, regaining graying. 
  *
  * to do: need to force scroll if drag-marking above/below screen. 
  *
  * A144: fixing intermittent File::Open
  * A157: added About...Java Version
  * A164: added About...compiler version
  * A175: eliminating all mnemonics and simplifying accelerators
  * A176: exploring RecentFile loads. One problem is that FileMenuListener
  *  attemps to handle all FileMenuItems, and it is difficult to disentangle the
  *  MainMenuItems from the SubMenuItems.  Plan on eliminating FMIListener,
  *  replacing with local calls to loadOPT(), loadRAY(), loadMED(),
  *  and perhaps newOPT(), newRAY(), and newMED().
  *  Doing the Recent submenus in graying() which is dynamic.
  *  EJIF need not do ungraying and does not need gjmi1, gjmi2.
  *
  * A176: removed LimitingDesktopMgr() from JDesktopPane; it causes a
  *  crash when Mac OS window is iconified.  Line 198.
  *
  * A179: simplified Startup Files options: none or most recent.
  *  Adopting single project folder for all File:Open chooser starts.
  *  Three choices for project folder: UserHome, MostRecent, or Specified.
  *  One text-edit box that always shows MostRecent unless Specified is true.
  *  Accomplish this via a "setProject" "getProject" method pair;
  *  setProject fills in sProject field & dataBox unless Specified is True;
  *  getProject produces either UserHome, or sProject, or Specified, depending on button.
  *
  * A180, A181: reinstalling a BoundingDesktopMgr() into JDesktopPane;
  *  it eliminates underscooting at the MenuBar, and does not crash MacOS "minimize"
  *  because BJIF constructor with MacOS L&F forbids minimize on new windows,
  *  and Options L&F inflates & forbids "minimize" going to MacOS L&F,
  *  and Options L&F allows "minimize" going to any other L&F. 
  *
  * A183: improved three-button exit logic.
  *
  *  @author M.Lampton (c) 2004-2015 STELLAR SOFTWARE all rights reserved.
  */
public class DMF extends JFrame implements B4constants
{
    public static int iXY = 10;  

    public static JFrame dmf;           // allows messages, e.g. setTitle()
    public static JDesktopPane jdp;     // for WhichEditorInFront...
              
    public static Registry reg;         // permanent *visible* home for reg

    public static OEJIF oejif;          // allows messages, e.g. repaint()
    public static REJIF rejif;
    public static MEJIF mejif; 
    public static int nEdits;           // increments for each edit.
    
    public static int giFlags[] = new int[NFLAGS];
    public static String sAutoErr = ""; // RT13 reports to AutoAdj, if necessary

    public static boolean bHostActive = true;  
    public static boolean bRequestWriteImage = false; // see BJIF
    public static boolean bRayGenOK = true; 
    public static boolean bAutoBusy = false;    // forbid parsing when AutoAdj is running

    private static int iWindowOffset = 20;      // pixels
    private static int iWindowOffsetMax = 200;  // pixels

    static final String enames[] = {"Optics", "Rays", "Media"};
    static final String extensions[] = {"OPT", "RAY", "MED"}; 


    //-----file menu setup---------------

    static final int NEWOPT=0, NEWRAY=1, NEWMED=2,  
                     OPENOPT=3, OPENRAY=4, OPENMED=5, 
                     SAVE=6, SAVEAS=7, 
                     QUICKPNG=8, WRITECAD=9, WRITEHISTO=10, 
                     PRINT=11, QUIT=12, NFITEMS=13; 

    static final String fileItemStr[] = 
       {"NewOptics", "NewRays", "NewMedia",  
       "OpenOptics", "OpenRays", "OpenMedia", 
       "SaveTable", "SaveTableAs", 
       "QuickPNG", "WriteCAD", "WriteHisto", 
       "Print/PDF", "Quit"}; 

    static JMenuItem fileMenuItem[] = new JMenuItem[NFITEMS]; 
    
    static JMenu jmRO, jmRR, jmRM;  // public for graying at line 855

    //----run menu setup---------------------

    public static GJIF gjifTypes[] = new GJIF[RM_NITEMS]; // GJIFrame types; #0=unused

    //---runItemStr[] become menu names (line 331) **and** JIF names (line 826). 
    //---these are numbered via the RM_XXX definitions in Constants.java
    
    static JMenuItem runMenuItem[] = new JMenuItem[RM_NITEMS];  

    //-----the loose non-arrayed menuItems for controlling graying---

    static JMenuItem cutMenuItem;
    static JMenuItem copyMenuItem;
    static JMenuItem pasteMenuItem;
    static JMenuItem deleteMenuItem;
    static JMenuItem selectAllMenuItem; 
    static JMenuItem showErrorMenuItem; 
    static JMenuItem specialKeysMenuItem; 
    static JMenuItem aboutMenuItem; 
    
    //------menu listeners, needed for creation and refreshing----------
    
    FMIlistener fmiListener = null;
    EMIListener emiListener = null;
    RMIlistener rmiListener = null;

    static JMenuBar menubar; 

    static private String sInitialDir;
    static public  String sCurrentDir; 
    static private String sWorkingTitle; 
    
    static public  String sUserHome = "";    // for defaults
    
    //------about the Java RunTime environment--------------
    
    static public int    iJRT;   // typically 6, 7, 8...
    static public String sJRT;   // typically "JRT6", JRT7", ...
    static public String sLong;  // typically "1.6.0_65"
    

    public DMF() //----constructor--------
    {
        super();                          // does nothing without args
        dmf = this;                       // save a reference to this frame
                
        sLong = java.lang.System.getProperty("java.version"); 
        char cJRT = U.getCharAt(sLong, 2);  
        iJRT = java.lang.Character.getNumericValue(cJRT); 
        sJRT = "JRT" + cJRT;  
                
        sInitialDir = System.getProperty("user.dir");  
        sCurrentDir = sInitialDir;        
        reg = new Registry(sInitialDir);  // create and load registry 
        sUserHome = System.getProperty("user.home"); 
        
        //---fix up an absentee project path----
        if (reg.getuo(UO_START, 8).length() < 2)
          reg.putuo(UO_START, 8, sUserHome); 
        
        sWorkingTitle = PRODUCT;   // defined as BEAM FOUR in B4constants.java
        setTitle(sWorkingTitle);
        setBackground(LBLUE);      // briefly, then jdp takes over.
        nEdits = 0; 
        
        //----------set initial AutoBusy flag-------------------

        bAutoBusy = false; // not busy allows blinker parsing 

        //-----------build the menus and menubar----------------
        
        vBuildMenus(); 

        //----prepare desktop: Horstmann & Cornell Vol.2. p.539--------
        //----and http://www.java2s.com/Code/Java/Swing-JFC------------
        
        jdp = new JDesktopPane(); 
        // jdp.setDesktopManager(new LimitingDesktopMgr());  // eliminated in A176.
        // needed to eliminate underscoot even in JDK6 + JRT6.
        jdp.setDesktopManager(new HeadroomDesktopManager()); // installed A180, 184.
        
        jdp.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE); 
        jdp.setBackground(LBLUE);

        this.setContentPane(jdp);
        this.setJMenuBar(menubar); 
        this.setSize(INITIALFRAMEWIDTH, INITIALFRAMEHEIGHT);

        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); 
        this.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent we)
            {
               vMasterExit(); 
            }
            public void windowActivated(WindowEvent we)
            {
                bHostActive = true; 
            }
            public void windowDeactivated(WindowEvent we)
            {
                bHostActive = false; 
            }
            public void windowGainedFocus(WindowEvent we)
            {
                bHostActive = true; 
            }
            public void windowLostFocus(WindowEvent we)
            {
                bHostActive = false; 
            }
        });

        //---attach a callback drop file listener--------
        new DropTarget(this, new DMFDropTargetListener(this)); 

        //-----load any specified startup files--------
        doAutoloadStartupFiles(); 

    } //---end of the constructor---------------- 





    //--------other methods-------------------
    
    public static void inflateIcons()
    // public static, for Options L&F menu line 320 of A180.
    // Called when going to MacOS L&F. 
    // (For new JIFs, see BJIF.)
    // Resuscitates all iconized JIFs.  
    // Use this prior to L&F switching to MacOS, because a bug
    // in MacOS L&F makes all foreign icons vanish.
    // Also (A182) iconization permission is a UserOption. 
    {   
        boolean bMacIconOK = "T".equals(reg.getuo(UO_START, 9)); 
        JInternalFrame[] jifs = jdp.getAllFrames(); 
        for (int i=0; i<jifs.length; i++)
        {
            if (jifs[i].isIcon())
            {
                try {jifs[i].setIcon(false);}
                catch (Exception x) {}
            }
            jifs[i].setIconifiable(bMacIconOK);  
        } 
    }

    public static void permitIcons()
    // public static, for Options L&F menu line 332 of A181.
    // Re-enables all JIF iconize buttons.
    // Use this after switching out of MacOS L&F, or selecting Icons=OK.
    {   
        JInternalFrame[] jifs = jdp.getAllFrames(); 
        for (int i=0; i<jifs.length; i++)
        {
            jifs[i].setIconifiable(true);  
        } 
    }
    
    
    private static void setProject(String path)
    {
        reg.putuo(UO_START, 8, path); 
    }
    
    private static String getFileOpenPath()
    // returns appropriate path for File:Open
    {
        if ("T".equals(reg.getuo(UO_START, 7)))  // 7 = most recent path button
          return reg.getuo(UO_START, 8);         // 8 = invisible recent path stash
        return sUserHome; 
    }
    
    
    public static JFrame getJFrame()
    {
        return dmf; 
    }
   
    public static double getOsize()
    // called by RT13 for numerical solver hint.
    // called by RT13 for vExtend() method.
    // called by Layout for drawing scale.
    {
        if (oejif != null)
        {
            double d = oejif.getOsize();
            return (d<TOL) ? 1.0 : d;
        }
        return 1.0;
    }


    public void vTryLoadDropFile(String fPathNameExt)
    //----callback from DropTargetListener-------
    {
        if (fPathNameExt.length() < MINFILENAME)
        {
            return;
        }
        if (!bFileNameReadCheck(fPathNameExt))
        {
            return;
        }
        String ext = U.getExtensionToLower(fPathNameExt); 
                             
        if (ext.equals("opt") && (oejif == null))
        {
            setProject(U.getOnlyPath(fPathNameExt)); 
            oejif = new OEJIF(iXY, fPathNameExt); 
            iXY = (iXY + iWindowOffset) % iWindowOffsetMax; 
            jdp.add(oejif); 
            oejif.setVisible(true);
            oejif.toFront(); 
            addRecent(0, fPathNameExt); 
        }
        else if (ext.equals("ray") && (rejif == null))
        {
            setProject(U.getOnlyPath(fPathNameExt)); 
            rejif = new REJIF(iXY, fPathNameExt); 
            iXY = (iXY + iWindowOffset) % iWindowOffsetMax; 
            jdp.add(rejif); 
            rejif.setVisible(true);
            rejif.toFront(); 
            addRecent(1, fPathNameExt); 
        }
        else if (ext.equals("med") && (mejif == null))
        {
            setProject(U.getOnlyPath(fPathNameExt)); 
            mejif = new MEJIF(iXY, fPathNameExt);             
            iXY = (iXY + iWindowOffset) % iWindowOffsetMax; 
            jdp.add(mejif); 
            mejif.setVisible(true);
            mejif.toFront(); 
            addRecent(2, fPathNameExt); 
        }
    }




    //-------------private methods--------------------
    
    private boolean bFileNameReadCheck(String fname)
    // pre-tests a filename before creating an EJIF instance
    {
        if (fname.length() < MINFILENAME)
          return false; 
        File f = new File(fname); 
        return bVerifyFile(f); 
    }
        

    public static boolean bVerifyFile(File f)
    {
        if ((f == null) || !f.exists() || !f.canRead())
        {
            return false; 
        }

        int nlines=0, nchars=0; 
        try
        {
            FileReader fr = new FileReader(f);
            BufferedReader br = new BufferedReader(fr); 
            String record; 
            while ((record = br.readLine()) != null)
            {
                nlines++; 
                nchars += record.length(); 
            }
        }
        catch (Exception e)
        {
            return false; 
        }
        if ((nlines < 2) || (nchars < 2))
        {
            return false; 
        }
        return true;
    }
    
    
    private void vBuildMenus()
    // assumes that sTenRecent[][] has been loaded.
    // Called only at startup.
    {
        fmiListener = new FMIlistener(); 
        emiListener = new EMIListener();
        rmiListener = new RMIlistener(); 

        //------------build the file menu---------------------

        JMenu fileMenu = new JMenu("File");
        
        fileMenu.setMnemonic('F');
        fileMenu.add(fileMenuItem[NEWOPT] = 
           makeItem(fileItemStr[NEWOPT], fmiListener, NULLCHAR));  // NULLCHAR means no accelerator
        fileMenu.add(fileMenuItem[NEWRAY] = 
           makeItem(fileItemStr[NEWRAY], fmiListener, NULLCHAR)); 
        fileMenu.add(fileMenuItem[NEWMED] = 
           makeItem(fileItemStr[NEWMED], fmiListener, NULLCHAR));

        fileMenu.addSeparator(); 

        fileMenu.add(fileMenuItem[OPENOPT] = 
           makeItem(fileItemStr[OPENOPT], fmiListener, NULLCHAR));
        fileMenu.add(fileMenuItem[OPENRAY] = 
           makeItem(fileItemStr[OPENRAY], fmiListener, NULLCHAR)); 
        fileMenu.add(fileMenuItem[OPENMED] = 
           makeItem(fileItemStr[OPENMED], fmiListener, NULLCHAR));

        fileMenu.addSeparator(); 
        
        jmRO = new JMenu("Recent Optics Files");  // public declaration for dynamic graying
        fileMenu.add(jmRO); 
        jmRR = new JMenu("Recent Ray Files");     // public declaration for dynamic graying  
        fileMenu.add(jmRR); 
        jmRM = new JMenu("Recent Media Files");   // public declaration for dynamic graying
        fileMenu.add(jmRM); 
        
        fileMenu.addSeparator(); 

        fileMenu.add(fileMenuItem[SAVE] = 
           makeItem(fileItemStr[SAVE], fmiListener, 'S'));  // allows Cmd S to save
        fileMenu.add(fileMenuItem[SAVEAS] = 
           makeItem(fileItemStr[SAVEAS], fmiListener, NULLCHAR));

        fileMenu.addSeparator(); 
        
        fileMenu.add(fileMenuItem[QUICKPNG] = 
           makeItem(fileItemStr[QUICKPNG], fmiListener, NULLCHAR)); 
        fileMenu.add(fileMenuItem[WRITECAD] = 
           makeItem(fileItemStr[WRITECAD], fmiListener, NULLCHAR)); 
        fileMenu.add(fileMenuItem[WRITEHISTO] = 
           makeItem(fileItemStr[WRITEHISTO], fmiListener, NULLCHAR)); 
        fileMenu.add(fileMenuItem[PRINT] = 
           makeItem(fileItemStr[PRINT], fmiListener, NULLCHAR)); 

        fileMenu.addSeparator(); 

        fileMenu.add(fileMenuItem[QUIT] = 
           makeItem(fileItemStr[QUIT], fmiListener, NULLCHAR));
        fileMenu.addMenuListener(fileGrayingListener);    // see below...
        fileMenu.addMenuListener(blinkerBlocker);         // see below...

        //----------build the Edit menu---------------------

        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic('E');
        editMenu.add(cutMenuItem  = makeItem("Cut", emiListener, 'X'));
        editMenu.add(copyMenuItem = makeItem("Copy", emiListener, 'C'));
        editMenu.add(pasteMenuItem = makeItem("Paste", emiListener, 'V'));
        editMenu.add(deleteMenuItem = makeItem("Delete", emiListener, 'D')); 

        editMenu.addSeparator(); 

        editMenu.add(selectAllMenuItem = makeItem("SelectAll", emiListener, 'A'));
        editMenu.addMenuListener(editGrayingListener);  // see below... 
        editMenu.addMenuListener(blinkerBlocker);       // see below...

        //---------build the Run menu----------------------

        JMenu runMenu = new JMenu("Run"); 
        runMenu.setMnemonic('R'); 
        for (int i=0; i<RM_NITEMS; i++)
          runMenu.add(runMenuItem[i] = makeItem(runItemStr[i], rmiListener, NULLCHAR)); 
        runMenu.addMenuListener(runGrayingListener); // see below; drives parser
        runMenu.addMenuListener(blinkerBlocker);     // see below...

        //-------------build the Options menu--------------

        Options optionMenu = new Options("Options", this); 
        optionMenu.addMenuListener(optionGrayingListener); // used in Options...
        optionMenu.addMenuListener(blinkerBlocker);  // see below...

        //-------------build the Help menu-----------------

        JMenu helpMenu = new JMenu("Help"); 
        showErrorMenuItem = new JMenuItem("Show table error"); 
        showErrorMenuItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                vMasterParse(true); // ferret out the problem
                int ifield=0, iline=0; 
                switch (giFlags[STATUS])
                {
                   case GOSYNTAXERR:
                     iline  = giFlags[OSYNTAXERR]/100;
                     ifield = giFlags[OSYNTAXERR]%100;
                     if (bringEJIFtoFront(oejif))
                       oejif.setCaretXY(ifield, iline); 
                     break; 
                   case GRSYNTAXERR:
                     iline  = giFlags[RSYNTAXERR]/100;
                     ifield = giFlags[RSYNTAXERR]%100;
                     if (bringEJIFtoFront(rejif))
                       rejif.setCaretXY(ifield, iline); 
                     break; 
                   case GMSYNTAXERR:
                     iline  = giFlags[MSYNTAXERR]/100;
                     ifield = giFlags[MSYNTAXERR]%100;
                     if (bringEJIFtoFront(mejif))
                       mejif.setCaretXY(ifield, iline); 
                     break; 
                }
            }
        });
        specialKeysMenuItem = new JMenuItem("Special Keys"); 
        specialKeysMenuItem.addActionListener(new ActionListener()
        {
            int panelWidth = 230; 
            public void actionPerformed(ActionEvent ae)
            {
                JPanel aPanel = new JPanel(); 
                int count = 6;   // six strings in aPanel
                aPanel.setPreferredSize(new Dimension(panelWidth, (1+count)*18)); 
                aPanel.setMaximumSize(getPreferredSize()); 
                aPanel.setLayout(new BoxLayout(aPanel, BoxLayout.Y_AXIS));
                aPanel.add(new JLabel("Ctrl Left:         narrow field")); 
                aPanel.add(new JLabel("Ctrl Right:        widen field")); 
                aPanel.add(new JLabel("Alt Right:           split field"));
                aPanel.add(new JLabel("Alt Down:           copy down")); 
                aPanel.add(new JLabel("Ctrl Alt Down:  copy to bottom"));
                aPanel.add(new JLabel("Ctrl Z:              undo & redo")); 
                aPanel.setBorder(BorderFactory.createTitledBorder("Table Mode Keys"));

                JPanel bPanel = new JPanel(); 
                count = 3;   // three strings in bPanel
                bPanel.setPreferredSize(new Dimension(panelWidth, (1+count)*18));
                bPanel.setMaximumSize(getPreferredSize()); 
                bPanel.setLayout(new BoxLayout(bPanel, BoxLayout.Y_AXIS)); 
                bPanel.add(new JLabel("1. Mark N lines with your mouse")); 
                bPanel.add(new JLabel("2. then Edit:Copy  or Ctl/Cmd/C")); 
                bPanel.add(new JLabel("3. then Edit:Paste  or  Ctl/Cmd V")); 
                bPanel.setBorder(BorderFactory.createTitledBorder("To insert N lines in Table Mode")); 

                JPanel cPanel = new JPanel(); 
                count = 5;   // five strings in cPanel
                cPanel.setPreferredSize(new Dimension(panelWidth, (1+count)*18));
                cPanel.setMaximumSize(getPreferredSize()); 
                cPanel.setLayout(new BoxLayout(cPanel, BoxLayout.Y_AXIS)); 
                //--same JLabels as in Options::Graphics--------------------------
                cPanel.add(new JLabel("Left Button Drag: pan scene")); 
                cPanel.add(new JLabel("Left Button Click: set caret")); 
                cPanel.add(new JLabel("Wheel or F7,F8:  zoom in/out"));
                cPanel.add(new JLabel("Shift+Wheel or F5,F6: vert zoom")); 
                cPanel.add(new JLabel("Right Button Drag:  twirl")); 
                cPanel.setBorder(BorderFactory.createTitledBorder("Mouse Graphics")); 
                
                JPanel dPanel = new JPanel(); 
                count = 6;  // six strings in dPanel
                dPanel.setPreferredSize(new Dimension(panelWidth, (1+count)*18));
                dPanel.setMaximumSize(getPreferredSize()); 
                dPanel.setLayout(new BoxLayout(dPanel, BoxLayout.Y_AXIS)); 
                dPanel.add(new JLabel("    red           r or R"));
                dPanel.add(new JLabel("    green       g or G")); 
                dPanel.add(new JLabel("    blue         b or B")); 
                dPanel.add(new JLabel("    yellow      y or Y"));
                dPanel.add(new JLabel("    magenta   m or M")); 
                dPanel.add(new JLabel("    cyan         c or C")); 
                dPanel.setBorder(BorderFactory.createTitledBorder("Ray Color @wave Tags")); 

                JOptionPane optionPane = new JOptionPane(new Object[] {aPanel, bPanel, cPanel, dPanel}); 
                JDialog myDialog = optionPane.createDialog(dmf, "Special Keys");
                myDialog.setModal(false);  
                myDialog.setVisible(true);   
            }
        });

        aboutMenuItem = new JMenuItem("About..."); 
        aboutMenuItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                //---moved into DMF constructor------------
                // String sVersion = java.lang.System.getProperty("java.version"); 
                // char cJRT = U.getCharAt(sVersion, 2);  
                // int iJRT = java.lang.Character.getNumericValue(cJRT); 
                // String sJRT = "JRT" + cJRT;  
                
                String sAbout = RELEASE+'\n'+COPYRIGHT+'\n'+COMPILER;
                sAbout += "\nJava Runtime is: "+sLong; 
                sAbout += "\nalso known as: "+sJRT; 
                String sTitle = "About "+sWorkingTitle; 
                JOptionPane.showMessageDialog(dmf, sAbout, sTitle, JOptionPane.PLAIN_MESSAGE); 
            }
        });
        helpMenu.add(showErrorMenuItem); 
        helpMenu.addSeparator(); 
        helpMenu.add(specialKeysMenuItem); 
        helpMenu.addSeparator(); 
        helpMenu.add(aboutMenuItem); 

        helpMenu.addMenuListener(blinkerBlocker);      // see below....
        helpMenu.addMenuListener(helpGrayingListener); // see below...

        //-------------build the main menubar-------------

        menubar = new JMenuBar();
        menubar.add(fileMenu);
        menubar.add(editMenu);
        menubar.add(runMenu); 
        menubar.add(optionMenu); 
        menubar.add(helpMenu); 
    }

    //--------unused helpers for Java version-------------

    String getNumber(String arg)
    {
        int length = arg.length(); 
        StringBuffer sb = new StringBuffer(); 
        for (int i=0; i<length; i++)
        {
            char c = arg.charAt(i); 
            if (isNumeric(c))
              sb.append(c); 
        }
        return sb.toString();
    } 
            
    boolean isNumeric(char c)
    {
        return ((c>='0') && (c<='9')) || (c=='.') || (c=='_');
    }
    
    //----helpers for file loading----------------

    private void doAutoloadStartupFiles()
    {
        boolean bAutoLoad = false; // nothing yet to parse

        if ("T".equals(reg.getuo(UO_START, 1)) && (oejif == null))
        {
            String s = reg.getuo(UO_RECENTO, 0);  // most recent optics file
            if ((s.length()>MINFILENAME) && bFileNameReadCheck(s))
            {
                setProject(U.getOnlyPath(s));                 
                oejif = new OEJIF(iXY, s); 
                iXY = (iXY + iWindowOffset) % iWindowOffsetMax; 
                jdp.add(oejif); 
                oejif.setVisible(true);
                oejif.toFront(); 
                repaint();  // unnecessary?
                bAutoLoad = true;   // now something to parse?
            }
        }
        if ("T".equals(reg.getuo(UO_START, 3)) && (rejif == null))
        {
            String s = reg.getuo(UO_RECENTR, 0);  // most recent ray file  
            if ((s.length()>MINFILENAME) && bFileNameReadCheck(s))
            {
                setProject(U.getOnlyPath(s)); 
                rejif = new REJIF(iXY, s); 
                iXY = (iXY + iWindowOffset) % iWindowOffsetMax; 
                jdp.add(rejif); 
                rejif.setVisible(true);
                rejif.toFront(); 
                repaint();  // unnecessary?
                bAutoLoad = true; 
            }
        }
        if ("T".equals(reg.getuo(UO_START, 5)) && (mejif == null))
        {
            String s = reg.getuo(UO_RECENTM, 0);  // most recent media file
            if ((s.length()>MINFILENAME) && bFileNameReadCheck(s))
            {
                setProject(U.getOnlyPath(s)); 
                mejif = new MEJIF(iXY, s); 
                iXY = (iXY + iWindowOffset) % iWindowOffsetMax; 
                jdp.add(mejif); 
                mejif.setVisible(true);
                mejif.toFront(); 
                repaint();  // unnecessary?
                bAutoLoad = true; 
            }
        }
    }

    //--------------manage client JIFs--------------------------

    public static int whichEditorIsFront()
    // clumsy because I don't have an array of editors.  Yet.
    {
        JInternalFrame jif = jdp.getSelectedFrame(); 
        if (jif == null)
          return ABSENT; 
        if (jif == oejif)
          return 0; 
        if (jif == rejif)
          return 1; 
        if (jif == mejif)
          return 2; 
        return ABSENT;
    }

    public static EJIF getFrontEJIF()
    // clumsy because I don't have an array of editors.  Yet.
    {
        JInternalFrame jif = jdp.getSelectedFrame(); 
        if (jif == null)
          return (EJIF) null; 
        if (jif == (JInternalFrame) oejif)
          return (EJIF) oejif;
        if (jif == (JInternalFrame) rejif)
          return (EJIF) rejif; 
        if (jif == (JInternalFrame) mejif)  
          return (EJIF) mejif; 
        return (EJIF) null;
    }

    public static boolean bringEJIFtoFront(EJIF e)
    // Brings any desired editor to the front for postings or errors
    // Returns true if successful.
    {
        if (e == null)
          return false; 
        e.setVisible(true); 
        e.toFront();
        try {e.setSelected(true); }   // Lights Up
        catch (java.beans.PropertyVetoException pve) {}
        e.focusPanel(); 
        return true; 
    }

    public static GJIF getFrontGJIF() 
    {
        JInternalFrame jif = jdp.getSelectedFrame(); 
        if ((jif != null) && (jif instanceof GJIF))
          return (GJIF) jif;
        return (GJIF) null; 
    }

    public static GJIF getLayoutGJIF()
    {
        JInternalFrame jif[] = jdp.getAllFrames(); 
        for (int i=0; i<jif.length; i++)
          if (jif[i] instanceof GJIF)
          {
              GJIF g = (GJIF) jif[i]; 
              if (g.getType() == RM_LAYOUT)
                return g;
          }
        return null; // no such type is present.
    }

    public static int getFrontGJIFType()
    {
        GJIF g = getFrontGJIF(); 
        if (g == null)
          return -1; 
        return g.getType(); 
    }

    public static JInternalFrame[] getJIFs()
    // Produces a list of all current JIFs.
    // Every JIF has a title:  JIF.getTitle()
    // Titles are assigned from runItemStr[] above. 
    // So, compare each element's title with a desired title. 
    {
        return jdp.getAllFrames(); 
    }
    
    
    //--------------the menu listeners----------------------

    MenuListener blinkerBlocker = new MenuListener()
    // This blinker blocker blocks the blinker.
    {
        public void menuSelected(MenuEvent me)
        {
            bHostActive = false; 
        }
        public void menuDeselected(MenuEvent me) 
        {
            bHostActive = true; 
        }  
        public void menuCanceled(MenuEvent me) 
        {
            bHostActive = true; 
        }  
    };

    //-------------file menu---------------------------------

    public static class FMIlistener implements ActionListener 
    /// an action listener switchyard for FileMenuItems.
    /// Graying will have been applied by fileGrayingListener(), below.
    // Need to generalize this to handle New, Open, Recent menu items. 
    {
        // default constructor ok, no static fields to initialize

        public void actionPerformed(ActionEvent ae) 
        {
            JMenuItem item = (JMenuItem) ae.getSource();
            String cmd = item.getActionCommand();

            int index = ABSENT;   // clumsy way to discover who called it.
            for (int i=0; i<NFITEMS; i++)
              if (cmd == fileItemStr[i])
                index = i; 

            JInternalFrame jiFront = jdp.getSelectedFrame(); 
            EJIF efront = getFrontEJIF(); 
            GJIF gfront = getFrontGJIF();  

            boolean toOpen = (index > 2); // decoder
            int whicheditor = index % 3;  // decoder
            switch (index)
            {
                case NEWOPT:
                    if (oejif == null)
                    {
                        String fname = getFileOpenPath() + "unnamed.OPT"; 
                        writeSkeleton(fname); 
                        oejif = new OEJIF(iXY, fname); 
                        iXY = (iXY + iWindowOffset) % iWindowOffsetMax; 
                        jdp.add(oejif); 
                        oejif.setVisible(true);
                        oejif.toFront(); 
                        try {oejif.setSelected(true);}
                        catch(java.beans.PropertyVetoException pve) {} 
                    }
                    break; 
                    
                case NEWRAY:
                    if (rejif == null)
                    {
                        String fname = getFileOpenPath() + "unnamed.RAY"; 
                        writeSkeleton(fname); 
                        rejif = new REJIF(iXY, fname); 
                        iXY = (iXY + iWindowOffset) % iWindowOffsetMax; 
                        jdp.add(rejif); 
                        rejif.setVisible(true);
                        rejif.toFront(); 
                        try {rejif.setSelected(true);}
                        catch(java.beans.PropertyVetoException pve) {} 
                    }
                    break; 
                    
                case NEWMED:
                    if (mejif == null)
                    {
                        String fname = getFileOpenPath() + "unnamed.MED"; 
                        writeSkeleton(fname); 
                        mejif = new MEJIF(iXY, fname); 
                        iXY = (iXY + iWindowOffset) % iWindowOffsetMax; 
                        jdp.add(mejif); 
                        mejif.setVisible(true);
                        mejif.toFront(); 
                        try {mejif.setSelected(true);}
                        catch(java.beans.PropertyVetoException pve) {} 
                    }
                    break; 
   
                case OPENOPT:  
                    if (oejif == null)
                    {
                        JFileChooser fc = new JFileChooser(); 
                        fc.setDialogTitle("Open Optics"); 
                        File fDir = new File(getFileOpenPath()); 
                        if (fDir.isDirectory())
                          fc.setCurrentDirectory(fDir); 
                        FileNameExtensionFilter fnef = new FileNameExtensionFilter("Optics", "OPT"); 
                        fc.setAcceptAllFileFilterUsed(false); 
                        fc.addChoosableFileFilter(fnef); 
                        int result = fc.showOpenDialog(dmf); 
                        if (result != JFileChooser.APPROVE_OPTION)
                          break; 
                        File file = fc.getSelectedFile(); 
                        if (!bVerifyFile(file))
                        {
                            break; 
                        }
                        DMF.sCurrentDir = file.getParent(); 
                        String fname = file.getAbsolutePath(); 
                        setProject(U.getOnlyPath(fname));                         
                        oejif = new OEJIF(iXY, fname); 
                        iXY = (iXY + iWindowOffset) % iWindowOffsetMax; 
                        jdp.add(oejif); 
                        oejif.setVisible(true);
                        oejif.toFront(); 
                        try {oejif.setSelected(true);}
                        catch(java.beans.PropertyVetoException pve) {}
                        addRecent(0, fname);  // oejif is not yet ready to interrogate 
                    }
                    break; 
                    
                case OPENRAY: 
                    if (rejif == null)
                    {
                        JFileChooser fc = new JFileChooser(); 
                        fc.setDialogTitle("Open Rays"); 
                        File fDir = new File(getFileOpenPath()); 
                        if (fDir.isDirectory())
                          fc.setCurrentDirectory(fDir); 
                        FileNameExtensionFilter fnef = new FileNameExtensionFilter("Rays", "RAY"); 
                        fc.setAcceptAllFileFilterUsed(false); 
                        fc.addChoosableFileFilter(fnef); 
                        int result = fc.showOpenDialog(dmf); 
                        if (result != JFileChooser.APPROVE_OPTION)
                          break; 
                        File file = fc.getSelectedFile(); 
                        if (!bVerifyFile(file))
                        {
                            break; 
                        }
                        DMF.sCurrentDir = file.getParent(); 
                        String fname = file.getAbsolutePath(); 
                        setProject(U.getOnlyPath(fname)); 
                        rejif = new REJIF(iXY, fname); 
                        iXY = (iXY + iWindowOffset) % iWindowOffsetMax; 
                        jdp.add(rejif); 
                        rejif.setVisible(true);
                        rejif.toFront(); 
                        try {rejif.setSelected(true);}
                        catch(java.beans.PropertyVetoException pve) {}
                        addRecent(1, fname);  // rejif is not yet ready to interrogate 
                    }
                    break; 
                    
                case OPENMED:  
                    if (mejif == null)
                    {
                        JFileChooser fc = new JFileChooser(); 
                        fc.setDialogTitle("Open Media"); 
                        File fDir = new File(getFileOpenPath()); 
                        if (fDir.isDirectory())
                          fc.setCurrentDirectory(fDir); 
                        FileNameExtensionFilter fnef = new FileNameExtensionFilter("Media", "MED"); 
                        fc.setAcceptAllFileFilterUsed(false); 
                        fc.addChoosableFileFilter(fnef); 
                        int result = fc.showOpenDialog(dmf); 
                        if (result != JFileChooser.APPROVE_OPTION)
                          break; 
                        File file = fc.getSelectedFile(); 
                        if (!bVerifyFile(file))
                        {
                            break; 
                        }
                        DMF.sCurrentDir = file.getParent(); 
                        String fname = file.getAbsolutePath(); 
                        
                        setProject(U.getOnlyPath(fname)); 
                        mejif = new MEJIF(iXY, fname); 
                        iXY = (iXY + iWindowOffset) % iWindowOffsetMax; 
                        jdp.add(mejif); 
                        mejif.setVisible(true);
                        mejif.toFront(); 
                        try {mejif.setSelected(true);}
                        catch(java.beans.PropertyVetoException pve) {}
                        addRecent(2, fname);  // mejif is not yet ready to interrogate 
                    }
                    break; 
                    
                case SAVE:
                    if (efront != null)
                      efront.pleaseSave(); 
                    break; 

                case SAVEAS:
                    if (efront != null)
                      efront.pleaseSaveAs(); 
                    break; 

                case QUICKPNG:
                    if (jiFront != null)
                      bRequestWriteImage = true; // BJIF will process at next blink.
                    break; 

                case WRITECAD:
                    if (gfront != null)
                      gfront.doCAD(); 
                    break; 

                case WRITEHISTO:
                    if (gfront != null)
                      gfront.doWriteHisto();
                    break; 

                case PRINT:
                    if (efront != null)
                      efront.tryPrint(); 
                    else if (gfront != null)
                      gfront.tryPrint(); 
                    break; 

                case QUIT:
                    vMasterExit();
                    break; 
            }
        }
    }

    MenuListener fileGrayingListener = new MenuListener()
    {
        public void menuSelected(MenuEvent me)
        {
            fileMenuItem[NEWOPT].setEnabled(oejif == null); 
            fileMenuItem[NEWRAY].setEnabled(rejif == null); 
            fileMenuItem[NEWMED].setEnabled(mejif == null); 
            fileMenuItem[OPENOPT].setEnabled(oejif == null); 
            fileMenuItem[OPENRAY].setEnabled(rejif == null); 
            fileMenuItem[OPENMED].setEnabled(mejif == null); 
            
            jmRO.setEnabled(oejif == null); // menu for recent optics
            jmRR.setEnabled(rejif == null); // menu for recent rays
            jmRM.setEnabled(mejif == null); // menu for recent media
            
            //---Build the recent files list: first removeAll() menu items----
            jmRO.removeAll(); 
            jmRR.removeAll(); 
            jmRM.removeAll(); 
            
            //---Gather the recent Opt files-----
            int nOpt = 0;               // count the recent file names
            for (int i=0; i<10; i++)    // count the recent file names
            {
                String s = reg.getuo(UO_RECENTO, i);
                if (s.length() >= MINFILENAME)
                  nOpt++; 
            }
            if (nOpt < 1)
            {
                JMenuItem ro = new JMenuItem("Sorry no recent OPT files"); 
                jmRO.add(ro); 
            }
            else
            {
                JMenuItem ro[] = new JMenuItem[nOpt];
                for (int i=0; i<nOpt; i++)
                {
                    ro[i] = new JMenuItem(reg.getuo(UO_RECENTO, i)); 
                    ro[i].addActionListener(new RFListener()); 
                    jmRO.add(ro[i]); 
                }
            }
            //---and the ray JMenuItems----
            int nRay = 0;               // count the recent file names
            for (int i=0; i<10; i++)    // count the recent file names
            {
                String s = reg.getuo(UO_RECENTR, i);
                if (s.length() >= MINFILENAME)
                  nRay++; 
            }
            if (nRay < 1)
            {
                JMenuItem rr = new JMenuItem("Sorry no recent RAY files"); 
                jmRR.add(rr); 
            }
            else
            {
                JMenuItem rr[] = new JMenuItem[nRay];
                for (int i=0; i<nRay; i++)
                {
                    rr[i] = new JMenuItem(reg.getuo(UO_RECENTR, i)); 
                    rr[i].addActionListener(new RFListener()); 
                    jmRR.add(rr[i]); 
                }
            }
            //---and the media JMenuItems.
            int nMed = 0;               // count the recent file names
            for (int i=0; i<10; i++)    // count the recent file names
            {
                String s = reg.getuo(UO_RECENTM, i);
                if (s.length() >= MINFILENAME)
                  nMed++; 
            }
            if (nMed < 1)
            {
                JMenuItem rm = new JMenuItem("Sorry no recent MED files"); 
                jmRM.add(rm); 
            }
            else
            {
                JMenuItem rm[] = new JMenuItem[nMed];
                for (int i=0; i<nMed; i++)
                {
                    rm[i] = new JMenuItem(reg.getuo(UO_RECENTM, i)); 
                    rm[i].addActionListener(new RFListener()); 
                    jmRM.add(rm[i]); 
                }
            }
            

            //---Only the selected frame can be printed or saved....
            JInternalFrame jif = jdp.getSelectedFrame(); 
            boolean anyselected = jif != null; 
            boolean oselected = (jif != null) && (jif == (JInternalFrame) oejif); 
            boolean rselected = (jif != null) && (jif == (JInternalFrame) rejif); 
            boolean mselected = (jif != null) && (jif == (JInternalFrame) mejif); 
            boolean eselected = oselected || rselected || mselected;
            boolean gselected = (null != getFrontGJIF()); 
            int ii = getFrontGJIFType(); 
            boolean hselected = ((ii==RM_H1D) || (ii==RM_MTF) || (ii==RM_H2D)); 

            fileMenuItem[SAVE].setEnabled(eselected); 
            fileMenuItem[SAVEAS].setEnabled(eselected); 
            fileMenuItem[QUICKPNG].setEnabled(anyselected); 
            fileMenuItem[WRITECAD].setEnabled(gselected);
            fileMenuItem[WRITEHISTO].setEnabled(hselected); 
            fileMenuItem[PRINT].setEnabled(anyselected); 
            fileMenuItem[QUIT].setEnabled(true); 
        }

        public void menuDeselected(MenuEvent me) {}

        public void menuCanceled(MenuEvent me) {}
    };


    //----RecentFile listener------------------
    
    public static class RFListener implements ActionListener
    {
        // default constructor is OK, no static fields to initialize
        
        public void actionPerformed(ActionEvent e)
        {
             JMenuItem jmi = (JMenuItem) e.getSource(); 
             String fname = jmi.getActionCommand(); 
             String ext = U.getExtension(fname).toUpperCase(); 
             int index = -1;
             if (ext.equals(".OPT")) index=0;  // or EJIF.myFlavor
             if (ext.equals(".RAY")) index=1; 
             if (ext.equals(".MED")) index=2; 
             switch (index)
             {
                 case 0: 
                    oejif = new OEJIF(iXY, fname); 
                    iXY = (iXY + iWindowOffset) % iWindowOffsetMax; 
                    jdp.add(oejif); 
                    oejif.setVisible(true);
                    oejif.toFront(); 
                    try {oejif.setSelected(true);}
                    catch(java.beans.PropertyVetoException pve) {}
                    addRecent(0, fname);  // oejif is not yet ready to interrogate 
                    break; 
                case 1:
                    rejif = new REJIF(iXY, fname); 
                    iXY = (iXY + iWindowOffset) % iWindowOffsetMax; 
                    jdp.add(rejif); 
                    rejif.setVisible(true);
                    rejif.toFront(); 
                    try {rejif.setSelected(true);}
                    catch(java.beans.PropertyVetoException pve) {}
                    addRecent(1, fname);  // rejif is not yet ready to interrogate 
                    break;  
                case 2:
                    mejif = new MEJIF(iXY, fname); 
                    iXY = (iXY + iWindowOffset) % iWindowOffsetMax; 
                    jdp.add(mejif); 
                    mejif.setVisible(true);
                    mejif.toFront(); 
                    try {mejif.setSelected(true);}
                    catch(java.beans.PropertyVetoException pve) {}
                    addRecent(2, fname);  // mejif is not yet ready to interrogate 
                    break; 
            }      
        }
    }
    

    //------------edit menu------------------------

    public static class EMIListener implements ActionListener 
    {
        // default constructor is ok, has no static fields to initialize

        public void actionPerformed(ActionEvent e) 
        {
            JMenuItem item = (JMenuItem) e.getSource(); 
            String cmd = item.getActionCommand();
            EJIF frontEJIF = getFrontEJIF(); 
            if (frontEJIF == null)
            {
                return; 
            }
            if (cmd == null)
            {
                return; 
            }
            if (cmd.equals("Cut"))
            {
                frontEJIF.doCut(); 
                return; 
            }
            if (cmd.equals("Copy"))
            {
                frontEJIF.doCopy(); 
                return; 
            }
            if (cmd.equals("Paste"))
            {
                frontEJIF.doPasteInto(); 
                return; 
            }
            if (cmd.equals("Delete"))
            {
                frontEJIF.doDelete(); 
                return; 
            }
            if (cmd.equals("SelectAll"))
            {
                frontEJIF.doSelectAll(); 
                return; 
            }
        }
    }

    MenuListener editGrayingListener = new MenuListener()
    /// Clumsy because I don't have an array of editors.  Yet.
    {
        public void menuSelected(MenuEvent me)
        {
            fileMenuItem[NEWOPT].setEnabled(oejif == null); 
            fileMenuItem[NEWRAY].setEnabled(rejif == null); 
            fileMenuItem[NEWMED].setEnabled(mejif == null); 
            fileMenuItem[OPENOPT].setEnabled(oejif == null); 
            fileMenuItem[OPENRAY].setEnabled(rejif == null); 
            fileMenuItem[OPENMED].setEnabled(mejif == null); 

            JInternalFrame jif = jdp.getSelectedFrame(); 
            boolean oselected = (jif != null) && (jif == (JInternalFrame) oejif); 
            boolean rselected = (jif != null) && (jif == (JInternalFrame) rejif); 
            boolean mselected = (jif != null) && (jif == (JInternalFrame) mejif); 
            boolean eselected = oselected || rselected || mselected;

            boolean marked = false; 
            if (oselected && oejif.areYouMarked())
              marked = true; 
            if (rselected && rejif.areYouMarked())
              marked = true;
            if (mselected && mejif.areYouMarked())
              marked = true; 

            cutMenuItem.setEnabled(marked); 
            copyMenuItem.setEnabled(marked); 
            pasteMenuItem.setEnabled(eselected); 
            deleteMenuItem.setEnabled(marked); 
            selectAllMenuItem.setEnabled(eselected); 
        }

        public void menuDeselected(MenuEvent me) {}

        public void menuCanceled(MenuEvent me) {}
    };


    //---------run menu----------------------------

    MenuListener runGrayingListener = new MenuListener()
    /// Graying for the run items...
    /// a Listener can be added to any menu whose graying status 
    /// needs to be updated immediately before displaying itself. 
    /// This one checks Gparse status for Run menu items. 
    {
        public void menuSelected(MenuEvent me)
        {
            vMasterParse(true); 
            boolean ok = (giFlags[STATUS] == GPARSEOK); 

            // general case: enable/disable all RunItems depending on "ok"
            for (int i=0; i<RM_NITEMS; i++)
              runMenuItem[i].setEnabled(ok); 

            // special case: leave test graphics ungrayed...
            runMenuItem[RM_DEMO].setEnabled(true); 

            // special case: layout without rays....
            if (giFlags[STATUS] == GLAYOUTONLY)
              runMenuItem[1].setEnabled(true); 

            if (ok)
            {
                int gn = getFrontGJIFType(); 

                // special diagnostic case for MTF...
                boolean bHaveH1D = (gn == RM_H1D); 
                runMenuItem[RM_MTF].setEnabled(bHaveH1D);

                // special diagnostic case for Random...
                boolean bRandomOK = ((gn==RM_LAYOUT) || (gn==RM_PLOT2)
                || (gn==RM_PLOT3) || (gn==RM_H1D) || (gn==RM_H2D) || (gn==RM_MTF)); 
                // Disabled: Map and MPlot: (gn==RM_MAP), (gn == RM_MPLOT));
                
                runMenuItem[RM_RANDOM].setEnabled(bRandomOK); 

                boolean bGoals = (giFlags[RNGOALS]>0) || (giFlags[RWFEFIELD]>RABSENT);
                boolean bAutoAdj = giFlags[ONADJ] + giFlags[RNADJ] > 0; 
                bAutoAdj = bGoals && bAutoAdj; 
                runMenuItem[RM_AUTOADJ].setEnabled(bAutoAdj); 

                int a0 = giFlags[RAYADJ0]; 
                boolean bAutoRay = (a0>=0 && a0<=RW) || (a0>=RTXL && a0<RTWL); 
                int a1 = giFlags[RAYADJ1]; 
                if (a1>=0)
                  bAutoRay = bAutoRay && (a1<RW) || (a1>RTXL && a1<RTWL); 
                bAutoRay = bAutoRay && giFlags[RNGOALS]>0; 
                runMenuItem[RM_AUTORAY].setEnabled(bAutoRay); 
            }
        }

        public void menuDeselected(MenuEvent me) {}

        public void menuCanceled(MenuEvent me) {}

    };

    public static class RMIlistener implements ActionListener 
    /// The action listener switchyard for all RunMenuItems. 
    /// Here we either run the service (InOut, Auto,...)
    /// ..or we create a general GJIF frame for graphics.
    /// That GJIF is told which panel functionality to install. 
    /// Those special panels are all extensions of AnnoPanel.
    /// Some run items need to know which GJIF is in front. 
    /// Each can work it out itself, using the available
    /// public static DMF.getFrontGJIF().
    {
        // default constructor ok, no static fields to initialize

        public void actionPerformed(ActionEvent e) 
        {
            JMenuItem item = (JMenuItem) e.getSource();
            String cmd = item.getActionCommand();

            // get the index to the command....
            int index = ABSENT; 
            for (int i=0; i<RM_NITEMS; i++)
              if (cmd == runItemStr[i])
                index = i; 

            if (index == RM_INOUT)
            {
                InOut myIO = new InOut();
            }
            else if (index == RM_AUTOADJ)
            {
                AutoAdj myAutoAdj = new AutoAdj();
            }
            else if (index == RM_AUTORAY)
            {
                AutoRay myAutoRay = new AutoRay();
            }
            else if (index == RM_RANDOM) 
            {
                Random myRandom = new Random();
            }
            else  // graphics cases Layout,P1D,MPlot,Map,MTF,P2D,P3D,Demo. 
            {
                gjifTypes[index] = new GJIF(index, cmd, item); 
                gjifTypes[index].setLocation(iXY, iXY); 
                iXY = (iXY + iWindowOffset) % iWindowOffsetMax; 
                jdp.add(gjifTypes[index]); 
                gjifTypes[index].setVisible(true);
                gjifTypes[index].toFront();
                try {gjifTypes[index].setSelected(true);} 
                catch(java.beans.PropertyVetoException pve) {}
            }
        }
    }

    //---------------option menu graying---------------------

    MenuListener optionGrayingListener = new MenuListener()
    // refreshes the flag bRayGenOK for use within Options:RayGenerators.
    {
        public void menuSelected(MenuEvent me)
        {
            vMasterParse(true); 
            int ierr = giFlags[STATUS]; 
            boolean bRay = rejif != null; 
            bRayGenOK = bRay;  //  && !((ierr==GRABSENT) || (ierr==GREMPTY));
        }

        public void menuDeselected(MenuEvent me) {}

        public void menuCanceled(MenuEvent me) {}
    };


    //---------------help menu graying---------------------

    MenuListener helpGrayingListener = new MenuListener()
    {
        public void menuSelected(MenuEvent me)
        {
            vMasterParse(true); 
            int i = giFlags[STATUS]; 
            boolean bShow = (i==GOSYNTAXERR) 
                         || (i==GRSYNTAXERR) 
                         || (i==GMSYNTAXERR); 
            showErrorMenuItem.setEnabled(bShow); 
            specialKeysMenuItem.setEnabled(true); 
            aboutMenuItem.setEnabled(true); 
        }

        public void menuDeselected(MenuEvent me) {}

        public void menuCanceled(MenuEvent me) {}
    };

    //--------------menu helper methods---------------------

    public static JMenuItem makeItem(String label, ActionListener listener, char accel) 
    // Convenience: assembles a JMI from strings, listener, etc
    // Creates complete accelerated JMenuItems for FileMenu and EditMenu. 
    // A175: eliminated all mnemonics.  
    {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(listener);
        item.setActionCommand(label);
        if (accel != NULLCHAR) 
        {
            int iMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
            item.setAccelerator(KeyStroke.getKeyStroke(accel, iMask)); 
        }
        return item;
    }




    public static void vMasterParse(boolean bActive)
    // Called by DMF_RunGrayingListener, editors, showError. 
    // Called also by Options menu constructor for graying. 
    // Also called by blinker if bNeedsParse, to freshen titlebar status. 
    // Called upon opening or closing any table. 
    // Posts each new DMF status title. 
    // Set bActive=false to delay posting any judgment outputs
    // Set bActive=true to allow full parse action. 
    // Potential danger: must not mess up AutoAdjust's surfs[] work.
    {
        if (bAutoBusy)
          return; 

        if (!bActive)
        {
            giFlags[STATUS] = GUNKNOWN; 
            postTitle(""); 
            return; 
        }

        if ((oejif==null) && (rejif==null) && (mejif==null))
        {
            giFlags[STATUS] = GNOFILES; 
            postTitle(""); // was "No files loaded"
            return; 
        }
           
        if (oejif == null)
          giFlags[OPRESENT] = 0; 
        else
          oejif.parse();

        if (rejif == null)
          giFlags[RPRESENT] = 0; 
        else
          rejif.parse();

        if (mejif == null)
          giFlags[MPRESENT] = 0; 
        else
          mejif.parse();

        //---------now compute and post giFlags[STATUS]---------

        if (giFlags[OPRESENT] == 0)
        {
            giFlags[STATUS] = GOABSENT; 
            postTitle(""); 
            return; 
        }
        if ((giFlags[ONSURFS] < 1) || (giFlags[ONFIELDS] < 1))
        {
            giFlags[STATUS] = GOEMPTY; 
            postTitle(""); 
            return; 
        }
        if (giFlags[OSYNTAXERR] > 0)
        {
            giFlags[STATUS] = GOSYNTAXERR; 
            postTitle(""); 
            return; 
        } 

        boolean rayOK = (giFlags[RPRESENT] > 0) 
                     && (giFlags[RNRAYS] > 0) 
                     && (giFlags[RNFIELDS] > 0);

        // expand rayAbsence to all its metrics for Layout:AllDiams.
        // I hope this does not confuse the ray parser!
        if (!rayOK)
        {
            giFlags[RNRAYS] = 0; 
            giFlags[RNFIELDS] = 0; 
        }

        if ((!rayOK) && (giFlags[OALLDIAMSPRESENT] > 0))
        {
           giFlags[STATUS] = GLAYOUTONLY; 
           postTitle(""); 
           return; 
        }
        if (giFlags[RPRESENT] == 0)
        {
            giFlags[STATUS] = GRABSENT; 
            postTitle(""); 
            return; 
        }
        if ((giFlags[RNRAYS] < 1) || (giFlags[RNFIELDS] < 1))
        {
            giFlags[STATUS] = GREMPTY; 
            postTitle(""); 
            return; 
        }
        if (giFlags[RSYNTAXERR] > 0)
        {
            giFlags[STATUS] = GRSYNTAXERR; 
            postTitle(""); 
            return; 
        }

        //-------past this point we have a good .OPT and .RAY---------

        if ((giFlags[OGRATINGPRESENT]==1) && (giFlags[RALLWAVESNUMERIC]==0))
        {
            giFlags[STATUS] = GRNEEDNUMERWAVES; 
            postTitle(""); 
            return; 
        }

        if (giFlags[OMEDIANEEDED] == FALSE)
        {
            // so refraction=OK, grating=OK
            giFlags[STATUS] = GPARSEOK; 
            postTitle(""); 
            return; 
        }
        else if (giFlags[RALLWAVESPRESENT] == FALSE)
        {
            // making media LUT fail
            giFlags[STATUS] = GRLACKWAVE; 
            postTitle(""); 
            return; 
        } 

        //-------from here on, media are required------------------
        //  Two things to verify:
        //  1.  Every glass named in .OPT appears among the listed glasses
        //  2.  Every wavel named in .RAY appears amont the listed wavels.
        //

        if (giFlags[MPRESENT] == FALSE)
        {
            giFlags[STATUS] = GMABSENT; 
            postTitle(""); 
            return; 
        }
        if ((giFlags[MNGLASSES] < 1) || (giFlags[MNWAVES] < 1))
        {
            giFlags[STATUS] = GMEMPTY; 
            postTitle(""); 
            return; 
        }
        if (giFlags[MSYNTAXERR] > 0)
        {
            giFlags[STATUS] = GMSYNTAXERR; 
            postTitle(""); 
            return; 
        }

        //----from here on, media table is internally OK----------
        //------ but does it have what we need?-------------------
        //----------------set up RT13 LUTs----------------------

        for (int j=1; j<=MAXSURFS; j++)
          RT13.gO2M[j] = ABSENT;

        for (int k=1; k<=MAXRAYS; k++)
          RT13.gR2M[k] = ABSENT; 

        //--------search the glass names in use----------------
        //------But! skip any glass names that are numeric-----

        int unkglassrec = ABSENT; 
        String unkglassname = "";
        boolean trouble = false; 
        for (int jsurf=1; jsurf<=giFlags[ONSURFS]; jsurf++)
        {
            // if (OEJIF.oglasses[jsurf].length() > 0)
            double refr = RT13.surfs[jsurf][OREFRACT]; 
            if (Double.isNaN(refr))            // invalid numeric
            {
                for (int mrec=1; mrec <= giFlags[MNGLASSES]; mrec++)
                  if (OEJIF.oglasses[jsurf].equals(MEJIF.mglasses[mrec]))
                  {
                      RT13.gO2M[jsurf] = mrec; // found it!
                      break;                   // abandon search. 
                  }
                if (RT13.gO2M[jsurf] == ABSENT)
                {
                    unkglassrec = jsurf; 
                    unkglassname = OEJIF.oglasses[jsurf]; 
                    trouble = true; 
                    break; // break out of required glass loop
                }
            }
        }
        if (trouble)
        {
            giFlags[STATUS] = GOGLASSABSENT;
            postTitle(unkglassname); 
            return; 
        }

        //-----if we get this far, we have all our glasses------
        //------next: do we have all our wavelengths?-----------

        int unkwaverec = ABSENT; 
        String unkwavename = ""; 
        trouble = false; 
        for (int irec=1; irec <= giFlags[RNRAYS]; irec++)
        {
            if (REJIF.wavenames[irec].length() > 0)  // empty is trouble here.
              for (int f=1; f <= giFlags[MNWAVES]; f++)
                if (REJIF.wavenames[irec].equals(MEJIF.mwaves[f]))
                {
                    RT13.gR2M[irec] = f; // found it.
                    break;               // abandon search.
                }
            if (RT13.gR2M[irec] == ABSENT)
            {
                unkwaverec = irec; 
                unkwavename = REJIF.wavenames[irec]; 
                trouble = true; 
                break; 
            }
        }
        if (trouble)
        {
            giFlags[STATUS] = GRWAVEABSENT;
            postTitle(unkwavename); 
            return; 
        }
        giFlags[STATUS] = GPARSEOK; 
        postTitle(""); 
    }


    private static void postTitle(String s)
    // Normally, argument is empty string
    // This posts the appropriate title with diagnostic. 
    // Called by DMF::vMasterParse() to show status.
    // And if DEBUG > 0, called by caret engine with diagnostic.
    {
        int i = giFlags[STATUS]; 
        // String s = ""; 
        if ((i>0) && (i<NEXPLANATIONS))
          s = sExplanations[i] + " " + s;
        dmf.setTitle(sWorkingTitle + s); 
    }


    public static void vMasterExit()
    // Called by WindowListener and by FileMenuExit. 
    // Polls each JIF to see if exit is OK.
    // Each JIF will manage its own save where needed.
    // Any cancels? Don't exit.
    {
        JInternalFrame[] jifs = jdp.getAllFrames(); 
        for (int i=0; i<jifs.length; i++)
        {
            BJIF bjif = (BJIF) jifs[i]; 
            if (!bjif.bExitOK(dmf))
              return;    // bail out at first cancel.
        }
        System.exit(0);
    }
    



    //----Recent Files support---------
    
    public static void addRecent(int k, String s)  // k=0:opt; k=1:ray; k=2:med.
    {
        if (s.length() < MINFILENAME)        // reject tiny string.
          return; 
        int g = k+UO_RECENTO;                // UO group number
        for (int i=0; i<10; i++)             // eliminate dupes.
          if (s.equals(reg.getuo(g,i)))
            for (int j=i; j<9; j++)          // pull one up to eliminate dupe.
              reg.putuo(g,j,reg.getuo(g,j+1));    
        for (int i=9; i>0; i--)              // push all down one, making room.
          reg.putuo(g,i,reg.getuo(g,i-1));
        reg.putuo(g, 0, s);                  // install new string at top.
    }    
    


    public static boolean writeSkeleton(String fname)
    {
        File f = new File(fname); 
        try
        {
            FileWriter fw = new FileWriter(f); 
            PrintWriter pw = new PrintWriter(fw, true); 
            pw.println("                                              "); 
            pw.println("                                              "); 
            pw.println("----------:----------:----------:----------:--"); 
            pw.println("          :          :          :          :  "); 
            pw.println("          :          :          :          :  "); 
            pw.println("          :          :          :          :  "); 
            pw.println("          :          :          :          :  "); 
            pw.println("          :          :          :          :  "); 
            pw.println("          :          :          :          :  "); 
            pw.println("          :          :          :          :  "); 
            pw.println("          :          :          :          :  "); 
            pw.flush(); 
            pw.close(); 
        }
        catch (IOException e)  {return false; }
        return true;
    }    
        
} //----------end of DMF-----------------------







/**
   This listener prequalifies each dropped file, 
   then passes it on to DMF's vTryLoadDropFile(). 
*/
class DMFDropTargetListener implements DropTargetListener
{  
    public DMFDropTargetListener(DMF gDMF)
    {  
        myDMF = gDMF;  // permits callback.
    }

    public void dragEnter(DropTargetDragEvent event)
    {  
        if (!isDragAcceptable(event))
          event.rejectDrag();
    }

    public void dragExit(DropTargetEvent event)
    {
        // no cleanup needed here. 
    }

    public void dragOver(DropTargetDragEvent event)
    {  
        // default visual feedback looks OK to me. 
    }

    public void dropActionChanged(DropTargetDragEvent event)
    {  
        if (!isDragAcceptable(event))
          event.rejectDrag();
    }

    public void drop(DropTargetDropEvent event)
    {  
        if (!isDropAcceptable(event))
        {
            event.rejectDrop();
            return;
        }
        event.acceptDrop(DnDConstants.ACTION_COPY);
        vAnalyzeDroppedData(event); 
        event.dropComplete(true);
    }

    public boolean isDragAcceptable(DropTargetDragEvent event)
    //--accept everything; transferable applies only to Drop not Drag.----
    {  
        return (event.getDropAction()
                & DnDConstants.ACTION_COPY_OR_MOVE) != 0;
    }

    public boolean isDropAcceptable(DropTargetDropEvent event)
    //--------checks for action and flavor-------------
    {  
        boolean bActionOK = (event.getDropAction()
                & DnDConstants.ACTION_COPY_OR_MOVE) != 0;
        boolean bFlavorOK = bAnalyzeDropFlavor(event); 
        return bActionOK && bFlavorOK; 
    }


    private boolean bAnalyzeDropFlavor(DropTargetDropEvent event)
    {
        Transferable transferable = event.getTransferable();
        DataFlavor[] flavors = transferable.getTransferDataFlavors();
        for (int i=0; i<flavors.length; i++)
          if (flavors[i].equals(DataFlavor.javaFileListFlavor))
            return true;
        return false;
    } 


    @SuppressWarnings("unchecked")  // for the necessary cast to List<File>
    private void vAnalyzeDroppedData(DropTargetDropEvent event)
    //----getTransferData() applies only to dropped events; no peeking!-----
    {
        String myExt = ""; 
        File f = null; 
        Transferable transferable = event.getTransferable();
        DataFlavor[] flavors = transferable.getTransferDataFlavors();  // ?
        DataFlavor d = DataFlavor.javaFileListFlavor;  // pre-tested.  ?? Oct 8 2014
        try
        {  
           java.util.List<File> fileList = (java.util.List) transferable.getTransferData(d);
           int nfiles = fileList.size(); 
           if (nfiles == 1)
           {
               f = fileList.get(0); 
               if (f.isFile() && f.canRead())
                 myDMF.vTryLoadDropFile(f.getPath()); 
           }
        }
        catch(Exception e)
        {  
        }
    }
 
    private DMF myDMF;
}


@SuppressWarnings("serial")

class HeadroomDesktopManager extends DefaultDesktopManager 
// http://stackoverflow.com/questions/8136944/preventing-underreach
// but much simplified to eliminate underscoot. 
{
    @Override
    public void beginDraggingFrame(JComponent f) 
    {
        // Don't do anything. Needed to prevent the DefaultDesktopManager 
        // setting the dragMode.
    }

    @Override
    public void beginResizingFrame(JComponent f, int direction) 
    {
        // Don't do anything. Needed to prevent the DefaultDesktopManager 
        // setting the dragMode
    }
    
    @Override
    public void setBoundsForFrame(JComponent f, int x, int y, int w, int h) 
    {
        y = Math.max(y, 1); 
        f.setBounds(x, y, w, h); 
    }      
}

