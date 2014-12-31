package com.stellarsoftware.beam;

import java.awt.*;         // frame, BasicStroke, Color, Font
import java.awt.event.*;   // KeyEvent MouseEvent etc
import java.awt.image.*;   // BufferedImage; transparent Blue color
import java.awt.font.*;    // font metric
import java.awt.geom.*;    // Rectangle2D
import java.awt.print.*;   // printing
import java.util.*;        // ArrayList
import javax.swing.*;      // Graphics2D features

import java.io.*;          // File 
import javax.imageio.*;    // PNG, JPEG

@SuppressWarnings("serial")

/**
  *
  *
  * Abstract class GPanel handles all artwork and annotation.
  * Has listeners for keystrokes, mouse, wheel, scrollers.
  * No sizeListener(); instead uses getSize() at each repaint.
  *
  * Seven abstract methods must be supplied by every extension:
  *
  *     protected void doTechList()  to create a new artwork tech list
  *     protected void doRotate()
  *     protected boolean doRandomRay()
  *     protected void doFinishArt()
  *     protected void doCursor(i,j) to manage the cursor in user space
  *     protected double getStereo()
  *     protected void doSaveData()  to a file, for histograms
  * 
  * Extensions needing mouse pan zoom must use AddScaledItem(xyz, opcode)
  * to add a vertex to a drawing and MUST FIRST SET THE AFFINES
  *    uxcenter, uxspan, uycenter, uyspan, uzcenter, uzspan
  * and must not modify these affines again.   
  * The mouse pan zoom feature will modify these affines as needed. 
  *
  * Here the manageXXXzoom() methods coordinate panels and zoom state.
  * For LAYOUT, also the zoom scalefactors can be displayed on the titlebar. 
  * For Plot2D the var and surf error message is displayed on the titlebar. 
  * Implements Zoom (wheel or F7) and Pan (left button drag).
  * Implements Rotate (right button drag) by calling new artwork.
  * Would be nice to center rotation on visible feature: line 614
  *
  * This abstract class is concreted by clientPanels doing artwork:
  *   Layout; Plot2D; MPlot, Plot3D, H1D; H2D; MTF; Test.
  *
  * Native system is CenterOriginPoints, +X=right, +Y=up, +Z=out.
  * so artwork quads have -250.0 < x,y,z < +250.0
  * Pixels are the same, except origin = ULCorner and +Y=down. 
  * Conversion to pixels uses local getIXPIX(x), getIYPIX(y).
  *
  * To get user coords from the artwork, the quadlist includes affines:
  *    userconsts x,y,z = offsets for that quadlist view;
  *    userslopes x,y,z = magnifications for that quadlist view;
  *    UserValue x,y,z = userConst + userSlope * quadListValue.
  * These affine userconsts & userslopes should be evaluated once,
  * at startup; then when doing artwork the affines should be added by the 
  * client at the beginning of its quadlist, by calling local addAffines().
  * The client must then NOT MODIFY the affines futher since the mouse
  * pan zoom actions will have taken over. 
  *
  *    protected void addAffines()
  *    {
  *       addXYZO(uxcenter, uycenter, uzcenter, USERCONSTS); 
  *       double d = (dUOpixels > 0) ? dUOpixels : 500.0; 
  *       addXYZO(uxspan/d, uyspan/d, uzspan/d, USERSLOPES);
  *    }
  *
  * Q: might it be better to not divide by d? Show just the spans?
  * That way they would be USERSPANS.  The renderer does not care since
  * the quadlist is already in render units.  However the user of the
  * affines (only CAD::DXF, I think) would have to know the "d" value to
  * convert quads to real space coords:
  *    UserValue = userConst + (userSpan / d) * quadListValue.
  *
  * A: No, leave it as is.  Works OK, simplifies CAD. 
  *
  * The BJIF caret is used to operate the blinking caret overlay.
  * Focus is driven by GJIF's internalFrameListener. 
  * 
  * Includes redo() which responds to Random.
  *
  * DRAMATIS PERSONAE
  *    myTechList    is an ArrayList of XYZO "quads" from artwork generator.
  *    biTech        is a bitmap that is screen compatible.
  *    g2Tech        is a private Graphics2D context from biTech.  
  *    doTechList()  (abstract) is how we request new artwork from client. 
  *    renderList()  is the local method that draws any ArrayList onto a bitmap. 
  *
  * General artwork: myTechList  -> g2Tech or g2CAD or g2Print.
  * Random batch:    myBatchList -> g2Tech and blit to screen.
  * Random accum:    myRandList  -> g2CAD or g2Print
  * Annotation art:  myAnnoList  -> g2Local or g2CAD or g2Print.
  *
  * Caret blinking is handled by a BJIF timer that alternates caret=true, false, true...
  * and calls OS repaint() which calls local paintComponent(), hence drawPage(), 
  * which for each state does three things:
  *     1.  Blits biTech onto the current display; (quick!)
  *     2.  Uses renderList(myAnnoList) to refresh annotation;
  *     3.  Draws, or not, the caret block using setXORMode(). 
  * This is always blindingly fast. 
  *
  * renderList() manages its own graphic alias smoothing.
  * Seems to me cleaner to have doCAD and paintComponent()
  * impose smoothing on each's g2D.           << DONE
  *
  * Bug: annotation fails to deliver its color to .PS files
  * although it works fine on screen. << FIXED.
  *
  * To do: build annoColor into the annoList.   <<DONE
  * Bug: BasicStroke has spiky JOIN_BEVELs.     <<fixed: JOIN_ROUNDs 
  *
  *
  * (c) 2004 Stellar Software all rights reserved. 
  */
abstract class GPanel extends JPanel implements B4constants, Printable
{
    // public static final long serialVersionUID = 42L;

    //-----Each extension must supply values for the following---------

    protected GJIF  myGJIF;                  // set by descendant panel
    protected boolean bClobber;              // random = destroy prev art
    protected boolean bPleaseParseUO;        // set by Options; reset by extension.
    protected double uxcenter = 0.0;         // set by extension setLocals() horiz
    protected double uycenter = 0.0;         // set by extension setLocals() vert
    protected double uzcenter = 0.0;         // set by extension setLocals() depth
    protected double uxspan = 1.0;           // set by extension setLocals() horiz
    protected double uyspan = 1.0;           // set by extension setLocals() vert
    protected double uzspan = 1.0;           // set by extension setLocals() depth
    protected double uxanchor = 0.0;         // set by mouse
    protected double uyanchor = 0.0;         // set by mouse
    protected double uzanchor = 0.0;         // unused.
    protected double dUOpixels = 500.0;
    protected int    iEdits;                 // to compare with DMF.nEdits
    
    //---Abstract "do" methods; each extension must implement these-----
    //-------protected scope is safest---------------------
    
    abstract void    doTechList(boolean bArtStatus); 
    abstract void    doRotate(int i, int j);
    abstract boolean doRandomRay(); 
    abstract void    doFinishArt(); 
    abstract void    doCursor(int i, int j); 
    abstract double  getStereo(); 
    abstract void    doSaveData(); 
    
    //-----------------Constructor------------------

    public GPanel()  // pixels are init by extensions
    {
        myTechList = new ArrayList<XYZO>(); 
        myAnnoList = new ArrayList<XYZO>(); 
        myBatchList = new ArrayList<XYZO>(); 
        myRandList = new ArrayList<XYZO>(); 

        this.setFocusable(true);                   
        this.addKeyListener(new MyKeyHandler());
        this.addMouseListener(new MyMouseHandler());
        this.addMouseMotionListener(new MyMouseMotionHandler());
        this.addMouseWheelListener(new MyMouseZoomer()); 
        iEdits = DMF.nEdits; 
    }


    public void requestNewArtwork()  
    // Allows each AutoAdjust() iteration to request fresh Layout artwork. 
    // Don't re-parse UO or sizes when this is called.
    // Just let drawPage() regenerate its new g2Tech and render it. 
    {
        bArtStatus = true; // local stash for when OS paints.
        myAnnoList.clear();
        if (g2Tech != null)
          g2Tech.dispose(); 
        g2Tech = null;   
        repaint();         // call OS, which calls paintComponent() below.
    }

    public void paintComponent(Graphics g)
    // Gets called when OS requests repaint() each caret blink.
    // GJIF offers myGJIF.setTitle(), myGJIF.getTitle().
    // This uses the stashed page, does not recalculate graphic.
    {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;  
        drawPage(g2); 
    }


    public void redo()
    // Called by Random after random rays have augmented myBatchList.
    // For Layout and Plot, rays atop earlier artwork, bClobber=false:
    //   First it appends myBatchList to myRandList, retain for CAD/printing. 
    //   Then we render myBatchList onto biTech using g2Tech.
    //   Then clears myBatchList making room for more random rays. 
    //   Then requests a repaint() to blit biTech onto the screen. 
    // For H1D, H2D, MTF, Map, a total redraw is necessary, bClobber=true:
    //   Accomplished by nulling g2Tech while retaining biTech:
    //   can repaint biTech many times but not augment it. 
    {
        if (g2Tech == null)  // SNH.
          return; 
        
        int lRand = myRandList.size();
        int lBatch = myBatchList.size(); 

        boolean bTooBig = (lRand + lBatch > MAXRANDQUADS); 
        if (!bTooBig)
          for (int i=0; i<lBatch; i++)
            myRandList.add(myBatchList.get(i)); 
        double dStereo = getStereo(); 
        
        if (bClobber)
          g2Tech = null;  // let drawPage() create fresh artwork
        else
        {
            renderList(myBatchList, g2Tech, dStereo, true);
            if (dStereo != 0.0)
              renderList(myBatchList, g2Tech, -dStereo, false); 
        }
        myBatchList.clear(); // done with myBatchList.

        repaint(); 
    }


    public void doFinish()
    // Called at completion of random ray run.
    // Allows overlay at completion of random ray run.
    // Employed only by Layout's foreground monoscopic artwork.
    // All other clients ignore doFinishArt() requests. 
    {
        if (g2Tech == null)  // SNH.
          return; 
        double dStereo = getStereo(); 
        if (dStereo != 0.0)
          return;            // cannot overwrite in stereo. 
        myTechList.clear();  // prepare for cleanup artwork
        doFinishArt();       // get cleanup artwork from client panel. 
        renderList(myTechList, g2Tech, 0.0, true);
        repaint();
    }


    public int print(Graphics g, PageFormat pf, int page) throws PrinterException
    {
        if (page >= 1)
          return Printable.NO_SUCH_PAGE;
        Graphics2D g2 = (Graphics2D) g; 
        g2.translate(pf.getImageableX(), pf.getImageableY()); 
        drawPage(g2); 
        return Printable.PAGE_EXISTS;
    }



    public void doCAD()  // called by DMF >> GJIF >> here
    // This routine supplies both a buffered screen image, and
    // the three quad lists, to an outboard CAD writer. 
    // Some CAD formats need the image, others need the lists. 
    // Rendering is done here because renderList() needs local affines.
    {
        int style = -1; 
        for (int i=0; i<9; i++)
          if ("T".equals(DMF.reg.getuo(UO_CAD, i)))
          {
             style = i;
             break; 
          } 
        if (style < 0)
        {
           U.beep(); 
           return; 
        }
        boolean bPortrait = "T".equals(DMF.reg.getuo(UO_CAD, 10)); 
        CAD.doCAD(style, bPortrait, myTechList, myRandList, myAnnoList); 
    }
    

    public void doUpdateUO()  
    // Options calls this via GJIF when options change
    {
        bPleaseParseUO = true; // flag allows client doParse().
        bArtStatus = bFULLART; // stash for when OS paints
        myAnnoList.clear();    // discard old artwork
        if (g2Tech != null)    // discard old artwork
          g2Tech.dispose();    // discard old artwork
        g2Tech = null;         // discard old artwork
        repaint();             // request OS repaint.
    }


    public void doQualifiedRedraw()  
    // GJIF calls this when coming forward: check for table edits.
    // How to implement bSticky, retain previous magnification?
    {
        if (DMF.nEdits == iEdits)
          return;              // no redraw needed. 
        iEdits = DMF.nEdits;   // update local edits count
        // bPleaseParseUO = true; // flag allows client doParse().
        bArtStatus = bFULLART; // stash for when OS paints
        myAnnoList.clear();    // discard old artwork
        if (g2Tech != null)    // discard old artwork
          g2Tech.dispose();    // discard old artwork
        g2Tech = null;         // discard old artwork
        repaint();             // request OS repaint.
    }

    //----------------protected methods---------------------

    protected int getTechListSize()
    {
        if (myTechList == null)
          return 0; 
        else
          return myTechList.size();
    }



    //--------------private & client support area-----------
    //--------------private & client support area-----------
    //--------------private & client support area-----------


    //------------graphics and blitting-----------------

    private ArrayList<XYZO> myTechList;    // vector art for Tech drawing
    private ArrayList<XYZO> myAnnoList;    // vector art for annotation
    private ArrayList<XYZO> myBatchList;   // vector art for random batch
    private ArrayList<XYZO> myRandList;    // vector art for accum randoms
    private Graphics2D g2Tech;       // Graphics2D for Tech drawing
    private BufferedImage biTech;    // unannotated bitmap
    private Dimension dim;           // current panel size  
    private int prevwidth=0;         // display size
    private int prevheight=0;        // display size
    private double dStereo=0.0;      // display stereo convergence

    



    private void drawPage(Graphics2D g2)
    // This routine does all the blitting: blinker, anno, biTech.
    //
    // Annotation worksaver: technical image is saved in biTech,
    // and blitted to screen before anno character list is painted.
    // This blit & annoPaint is done for each anno keystroke.
    // However zoom, pan, rotate require totally new artwork.
    //
    // Here renderList() handles all font work because fontsize
    // and boldness is built into each char.
    //
    // Includes caret blink via host BJIF and paintComponent().
    {
        dim = getSize(); 
        dUOpixels = getUOpixels(); 
        imid = dim.width / 2; 
        jmid = dim.height / 2; 

        if ((prevwidth != dim.width) || (prevheight != dim.height)
        || (biTech==null) || (g2Tech==null))
        {
            if (g2Tech != null)
              g2Tech.dispose(); 

            myTechList.clear();  
            myRandList.clear(); 
            myAnnoList.clear(); 
            myBatchList.clear(); 

            prevwidth = dim.width; 
            prevheight = dim.height; 
            biTech = new BufferedImage(dim.width, dim.height,
                       BufferedImage.TYPE_INT_RGB); 
            g2Tech = (Graphics2D) biTech.getGraphics();
            setGraphicSmoothing(g2Tech);

            doTechList(bArtStatus); // locally stashed bArtStatus 
            
            double dStereo = getStereo(); 
            
            if (dStereo == 0.0)
              renderList(myTechList, g2Tech, 0.0, true);
            else
              renderListTwice(myTechList, biTech, dStereo, true);  
        }

        setGraphicSmoothing(g2);                    // prep screen
        g2.drawImage(biTech, 0, 0, null);           // blit biTech
        if (myAnnoList.size() > 0)
          renderList(myAnnoList, g2, 0.0, false);   // annotate

        if ((myGJIF != null) && myGJIF.getCaretStatus())    
        {
            int f = getUOAnnoFont();                // fontsize points
            int i = icaret - f/4;  
            int j = jcaret - f/3; 
            int w = f/2;
            int h = (2*f)/3; 
            g2.setXORMode(Color.YELLOW);
            g2.fillRect(i, j, w, h); 
        }
    }




    //----locally stashed fields---------------------


    // bArtStatus is stashed locally because it is set during
    // each mouse action yet must be made available at an unknown
    // future time when the OS repaints the artwork. Its value
    // specifies whether a skeleton or a fullart is wanted. 
    
    private boolean bArtStatus = bFULLART; 
    
    //------caret wheel zoom support for clients-----------
        
    static java.util.Timer wheelTimer; 
    private int wheelTimerCount=0;
    private int wheelTimerMax=2; 

    private int icaret=250, jcaret=250;      // pixels
    private int imid=250, jmid=250;          // pixels
    private int imouse=0, jmouse=0;          // pixels
    
    
    private String sScaleFactors()
    //  pixels per user unit scalefactors();
    //  Can be used to substitute myGJIF.setTitle();
    {
        if (Math.abs(uxspan) < TOL) 
          return ""; 
        if (Math.abs(uyspan) < TOL)
          return ""; 
        double hscale = dUOpixels / uxspan; 
        double vscale = dUOpixels / uyspan; 
        double ratio = uxspan / uyspan; 
        return "Hor="+U.fwd(hscale,8,2).trim()
            +"  Vert="+U.fwd(vscale,8,2).trim()
            +"  Ratio="+U.fwd(ratio,6,2).trim(); 
    }


    //-----------mouse action support------------

    private boolean bLeftButton=false;            
    private boolean bRightButton=false; 
    private boolean bDragged=false; 


    private void setGraphicSmoothing(Graphics2D g2)
    {
        if ("T".equals(DMF.reg.getuo(UO_GRAPH, 7)))
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


   //----------helpers for this AnnoPanel doing annotation----------

    private int getUOAnnoFont()
    {
        int i = U.parseInt(DMF.reg.getuo(UO_GRAPH, 4)); 
        return Math.max(3, Math.min(100, i)); 
    }

    private int getUOAnnoBold()
    // as of Nov 2005, Font.BOLD=1, Font.PLAIN=0
    {
       return "T".equals(DMF.reg.getuo(UO_GRAPH, 5)) ? Font.BOLD : Font.PLAIN;
    }

    private int getUOAnnoFontCode()  // adds to ASCII
    {
        return 10000*getUOAnnoFont() + 1000*getUOAnnoBold();
    }

    private int getFixedAnnoFontCode()  // adds to ASCII, no UO
    {
        return 120000;   // 12 point plain
    }



    //------helpers for client assembling its techList-----------

    protected int getUOGraphicsFont()
    {
        return U.parseInt(DMF.reg.getuo(UO_GRAPH, 2)); 
    }


    protected int getUOGraphicsBold()
    // as of Nov 2005, Font.BOLD=1, Font.PLAIN=0
    {
       return "T".equals(DMF.reg.getuo(UO_GRAPH, 3)) ? Font.BOLD : Font.PLAIN;
    }


    protected int getUOGraphicsFontCode()  // adds to ASCII
    {
        return 10000*getUOGraphicsFont() + 1000*getUOGraphicsBold(); 
    }
     
  
    protected void addXYZO(double x, double y, double z, int k)  
    // For use by any extension.
    {
        myTechList.add(new XYZO(x, y, z, k)); 
    }


    protected void addXYZO(int i, int j, int op)  
    // For use by each extension; i & j are in pixels. 
    {
        myTechList.add(new XYZO(i, j, op)); 
    }


    protected void addXYZO(int i, int j, char c)
    {
        myTechList.add(new XYZO(i, j, c));
    }


    protected void addXYZO(int op)
    {
        myTechList.add(new XYZO(op)); 
    }
    
    protected void addXYZO(double x, int op)
    // For use in Layout to implement custom line widths.
    // x is the user-specified line width in points or pixels. 
    {
        myTechList.add(new XYZO(x, 0.0, 0.0, op));
    }


    protected void clearXYZO()
    // For use by extension for each fresh start.
    // But myTechList is not destroyed: still there for receiving artwork.
    // Also biTech remains painted, and g2Tech interface is available.
    {
        myTechList.clear(); 
    }


    protected void addAffines()
    // stash user affine consts & slopes for DXF: pix->UserUnits
    {
        addXYZO(uxcenter, uycenter, uzcenter, USERCONSTS); 
        double d = (dUOpixels > 0) ? dUOpixels : 500.0; 
        addXYZO(uxspan/d, uyspan/d, uzspan/d, USERSLOPES);
    }


    protected void addScaledItem(double ux, double uy, int op)
    // scales user (x,y) and appends to myTechList.
    // allows pan zoom twirl thanks to getXX() calls
    // Be sure to addAffines() before using this!
    {
        addXYZO(getax(ux), getay(uy), 0.0, op);
    }


    protected void addScaledItem(double ux, double uy, double z, int op)
    {
        addXYZO(getax(ux), getay(uy), z, op);
    }


    protected void addScaledItem(double[] xyz, int op)
    // allows pan zoom twirl thanks to getXX() calls
    {
        addXYZO(getax(xyz[0]), getay(xyz[1]), getaz(xyz[2]), op); 
    }


    //----the following helpers are for random ray batch artwork--------

    protected void buildXYZO(int opcode, boolean bRand)
    {
        if (bRand)
          myBatchList.add(new XYZO(opcode)); 
        else
          myTechList.add(new XYZO(opcode));
    }
    
    
    protected void buildXYZO(double x, int opcode, boolean bRand)
    {
        if (bRand)
          myBatchList.add(new XYZO(x, 0.0, 0.0, opcode)); 
        else
          myTechList.add(new XYZO(x, 0.0, 0.0, opcode));
    }


    protected void buildScaledItem(double[] xyz, int k, boolean bRand)
    {
        double x = getax(xyz[0]); 
        double y = getay(xyz[1]); 
        double z = getaz(xyz[2]); 
        if (bRand)
          myBatchList.add(new XYZO(x, y, z, k)); 
        else
          myTechList.add(new XYZO(x, y, z, k)); 
    }


    protected void buildScaledItem(double x, double y, double z, int k, boolean bRand)
    {
        x = getax(x); 
        y = getay(y); 
        z = getaz(z); 
        if (bRand)
          myBatchList.add(new XYZO(x, y, z, k)); 
        else
          myTechList.add(new XYZO(x, y, z, k)); 
    }


    protected void buildScaledItem(double x, double y, int k, boolean bRand)
    {
        buildScaledItem(x, y, 0.0, k, bRand);
    }



    //---------these helpers are general purpose--------------

    protected double getax(double ux)
    // converts userX to annoX
    {
        return dUOpixels*(ux-uxcenter)/uxspan; 
    }


    protected double getay(double uy)
    // converts userY to annoY
    {
        return dUOpixels*(uy-uycenter)/uyspan; 
    }


    protected double getaz(double uz)
    // zero offset, and shares scale factor with yaxis.
    {
        return dUOpixels*uz/uyspan; 
    }


    protected double getux(double ax)
    // converts annoX to userX
    {
        return uxcenter + ax*uxspan/dUOpixels; 
    }


    protected double getuy(double ay)
    // converts annoY to userY
    {
        return uycenter + ay*uyspan/dUOpixels;
    }


    //----------helpers for pixel rendering---------

    public double getuxPixel(int ipix)
    // Converts raw pixel coord into user coord ux.
    // Used by clients to show cursor coords in user space.
    {
        return uxcenter + (ipix-imid)*uxspan/dUOpixels; 
    }


    public double getuyPixel(int jpix)
    // Converts raw pixel coord into user coord uy.
    // Used by clients to show cursor coords in user space.
    {
        return uycenter - (jpix-jmid)*uyspan/dUOpixels;
    }


    private double getAXPIX( int ipix) // pixel->annoPoints
    {
        return (double)(ipix - imid); 
    }


    private double getAYPIX( int jpix) // pixel->annoPoints
    {
        return (double)(jmid - jpix); 
    }


    private int getIXPIX(double x)  // annoPoints->pixel
    {
        return (int)(imid + x); 
    }


    private int getIYPIX(double y)  // annoPoints->pixel
    {
        return (int)(jmid - y); 
    }


    private int getUOpixels()
    // Returns User Option window size in pixels.
    {
        int i = U.parseInt(DMF.reg.getuo(UO_GRAPH, 6)); 
        if (i<=10)
          i = 500; 
        return Math.min(3000, Math.max(100, i)); 
    }

   


    //----------annotation charlist management-------------

    char getCurrentChar()
    {
        int len = myAnnoList.size(); 
        if (len > 0)
        {
            int k = myAnnoList.get(len-1).getO(); 
            if (k<=127)
              return (char) k; 
        }
        return (char) 0;
    }


    void setNextCaretCoords(int igiven)
    // Can I eliminate this entirely??
    // and then eliminate getIYPIX().. etc?  Nope.
    {
        int len = myAnnoList.size(); 
        if (len > 0)
        {
            icaret = igiven + getIXPIX(myAnnoList.get(len-1).getI()); 
            jcaret = getIYPIX(myAnnoList.get(len-1).getJ()); 
            return; 
        }

        // treat case of empty myAnnoList length....
        icaret = imouse; 
        jcaret = jmouse; 
    }


    void addAnno(double x, double y, char c)
    {
        int i = (int) c + getUOAnnoFontCode(); 
        myAnnoList.add(new XYZO(x, y, 0.0, i));
    }


    void deleteLastAnno()  // for backspace.
    {
        int i = myAnnoList.size(); 
        if (i>0)
          myAnnoList.remove(myAnnoList.get(i-1)); 
    }


    private double getUserSlope()  // ZoomIn limiter
    {
        if (myTechList==null)
          return 1.0; 
        int reach = Math.min(myTechList.size(), 5); 
        for (int i=0; i<reach; i++)
        {
            XYZO myXYZO = myTechList.get(i); 
            if (myXYZO.getO() == USERSLOPES)
              return myXYZO.getX(); 
        }
        return 1.0; 
    }




    //----------support for mouse pan zoom twirl-------------------

    protected void getNewMouseArtwork(boolean bFinal)  
    // protected allowing thread update.
    // Don't re-parse UO or sizes when this is called!
    // Called by Zoom, mouseDragTranslate, rotate.
    // Not called for simple mousePressed/mouseReleased.
    // bFinal=false when mouse is still down and skeleton is wanted.
    // bFinal=true when mouse is released and final artwork is wanted.
    {
        bArtStatus = bFinal; // local stash for when OS paints.
        myAnnoList.clear();
        if (g2Tech != null)
          g2Tech.dispose(); 
        g2Tech = null;   
        repaint();           // call OS
    }


    private void manageZoomIn()  // called by F7 and Wheel
    // ZoomIn centered on the current caret location. 
    // New artwork will use the new centers & spans.
    {
        double dzoom = 1.0 - ZOOMOUT; // here ZOOMOUT=0.7071..
        uxcenter += dzoom * uxspan*(icaret-imid)/dUOpixels; 
        uycenter += dzoom * uyspan*(jmid-jcaret)/dUOpixels; 
        if (getUserSlope() > 1E-14)
        {
            uxspan *= ZOOMOUT;
            uyspan *= ZOOMOUT; 
            uzspan *= ZOOMOUT; 
        }
        startWheelTimer(); 
        getNewMouseArtwork(bSKELETON); 
        
        if (RM_LAYOUT == myGJIF.myType)  // Layout zoom scale factor display
          if ("T".equals(DMF.reg.getuo(UO_LAYOUT,3)))
            myGJIF.setTitle(sScaleFactors());
    }

    private void manageVertZoomIn()  // called by F5 and WheelShift
    // ZoomIn centered on the current caret location. 
    // New artwork will use the new centers & spans.
    {
        double dzoom = 1.0 - ZOOMOUT; 
        uycenter += dzoom * uyspan*(jmid-jcaret)/dUOpixels; 
        if (getUserSlope() > 1E-14)
        {
            uyspan *= ZOOMOUT; 
        }
        startWheelTimer(); 
        getNewMouseArtwork(bSKELETON); 
        
        if (RM_LAYOUT == myGJIF.myType)  // Layout zoom scale factor display
          if ("T".equals(DMF.reg.getuo(UO_LAYOUT,3)))
            myGJIF.setTitle(sScaleFactors());
    }


    private void manageZoomOut()  // called by F8 and Wheel
    {
        double dzoom = (1.0/ZOOMOUT)-1.0; 
        uxcenter -= dzoom * uxspan*(icaret-imid)/dUOpixels; 
        uycenter -= dzoom * uyspan*(jmid-jcaret)/dUOpixels; 
        uxspan /= ZOOMOUT; 
        uyspan /= ZOOMOUT; 
        uzspan /= ZOOMOUT; 
        startWheelTimer(); 
        getNewMouseArtwork(bSKELETON); 
        
        if (RM_LAYOUT == myGJIF.myType)  // Layout zoom scale factor display
          if ("T".equals(DMF.reg.getuo(UO_LAYOUT,3)))
            myGJIF.setTitle(sScaleFactors());
    }

    private void manageVertZoomOut()  // called by F6 and WheelShift
    {
        double dzoom = (1.0/ZOOMOUT)-1.0; 
        uycenter -= dzoom * uyspan*(jmid-jcaret)/dUOpixels; 
        uyspan /= ZOOMOUT; 
        startWheelTimer(); 
        getNewMouseArtwork(bSKELETON); 
        
        if (RM_LAYOUT == myGJIF.myType)  // Layout zoom scale factor display
          if ("T".equals(DMF.reg.getuo(UO_LAYOUT,3)))
            myGJIF.setTitle(sScaleFactors());
    }

    private void manageDragTranslate(int di, int dj) 
    // Called by drag.
    // New artwork will use the modified centers. 
    {
        uxcenter -= uxspan*(di)/dUOpixels;
        uycenter += uyspan*(dj)/dUOpixels; 
        getNewMouseArtwork(bSKELETON); 
    }


    private void manageDragRotate(int i, int j) 
    // Called by drag.
    // Relies upon client's doRotate() to create rotated artwork. 
    // Uses average outOf zzz of target objects to translate image. 
    // This translation is done here, without client help.
    {
        double daz = (i/3)/57.3;  // radians azimuth change
        double del = (j/3)/57.3;  // radians elevation change
        double zzz=0;             // sum & average zvertex
        double xs=1, ys=1;        // slopes: userUnits/point
        int ncount = 0; 
        if (myTechList == null)
          return; 
        int npts = Math.min(1000, myTechList.size()); 
        if (npts < 1)
          return; 

        for (int k=0; k<npts; k++)
        {
            XYZO m = myTechList.get(k); 
            int op = m.getO(); 
            double x = m.getX();
            double y = m.getY(); 
            double z = m.getZ(); 
            if (op==USERSLOPES) // has defined scale factors
            {
                xs = m.getX(); 
                ys = m.getY(); 
                continue; 
            }
            if ((op==MOVETO) || (op==PATHTO) || (op==STROKE))
            {
                if ((x>-200) && (x<200) && (y>-200) && (y<200))
                {
                   zzz += z;
                   ncount++; 
                }
            }
            if (op==COMMENTRULER) // exclude furniture from average
              break;  
        }
        if (ncount > 1)
        {
            zzz /= ncount;
            uxcenter += xs * zzz * daz; 
            uycenter -= ys * zzz * del; 
        } 
        doRotate(i, j);                 // Have client modify sinel & cosel.
        getNewMouseArtwork(bSKELETON);  // Have client do temporary artwork.
    }



    //-------Artwork & WheelTimer implementation-------------

    private void startWheelTimer()
    // Discards previous wheelTimer and creates a new one. 
    // Multiple starts OK; extendable delay.
    {
        wheelTimerCount = 0; 
        if (wheelTimer != null)
          wheelTimer.cancel(); 
        wheelTimer = null; 
        wheelTimer = new java.util.Timer(); 
        wheelTimer.schedule(new Incrementor(), 0, 50); 
    }


    private class Incrementor extends TimerTask
    // internal class eases communication with local variables
    // Delay avoids excessive wheel driven recomputations. 
    {
        public void run()
        {
            wheelTimerCount++; 
            if (wheelTimerCount > wheelTimerMax)
            {
               wheelTimer.cancel(); 
               wheelTimer = null; 
               getNewMouseArtwork(bFULLART); 
            }
        }
    }


    //------------Here are the event listeners----------

    private class MyKeyHandler extends KeyAdapter
    {
        int charwidth=0; 

        public void keyPressed(KeyEvent ke)
        {
            int fontcode = getUOAnnoFontCode();  
            int charH = fontcode / 10000;  
            int charW = 1 + fontcode / 20000;   

            int ic = ke.getKeyCode();  

            if ((ic==KeyEvent.VK_DELETE) || (ic==KeyEvent.VK_BACK_SPACE))
            {
                deleteLastAnno(); 
                int step = (getCurrentChar()=='\n') ? 0 : charW;
                setNextCaretCoords(step); 
                repaint();    
            }

            if (ic==KeyEvent.VK_ENTER)
            {
                icaret = imouse;  
                jcaret += charH; 
                addAnno(getAXPIX(icaret), getAYPIX(jcaret), '\n'); 
                repaint();    
            }

            if (ic==KeyEvent.VK_UP)
            {
                jcaret -= ke.isControlDown() ? 1 : charH;
                repaint(); // update caret
            }

            if (ic==KeyEvent.VK_DOWN)
            {
                jcaret += ke.isControlDown() ? 1 : charH; 
                repaint(); // update caret
            }

            if (ic==KeyEvent.VK_LEFT)
            {
                icaret -= ke.isControlDown() ? 1 : charW; 
                repaint(); // update caret
            }

            if (ic==KeyEvent.VK_RIGHT)
            {
                icaret += ke.isControlDown() ? 1 : charW; 
                repaint(); // update caret
            }

            if (ic==KeyEvent.VK_F5)
            {
                manageVertZoomIn();
            }

            if (ic==KeyEvent.VK_F6)
            {
                manageVertZoomOut(); 
            }

            if (ic==KeyEvent.VK_F7)
            {
                manageZoomIn();
            }

            if (ic==KeyEvent.VK_F8)
            {
                manageZoomOut(); 
            }
        }

        public void keyTyped(KeyEvent ke)
        {
            int fontcode = getUOAnnoFontCode();  
            int charH = fontcode / 10000;  
            int charW = 1 + fontcode / 20000;   

            if (myAnnoList.size() < 1)      // startup.
            {
               int foreground = SETCOLOR + BLACK; 
               if (g2Tech != null)
                 if (g2Tech.getBackground() == Color.BLACK)
                   foreground = SETCOLOR + WHITE; 
               addAnno(0.0, 0.0, (char) foreground);
            }

            char c = ke.getKeyChar(); 
            if ((c>=' ') && (c<='~') && (icaret>0))
            {
                addAnno(getAXPIX(icaret), getAYPIX(jcaret), c); 
                setNextCaretCoords(charW); 
                repaint(); 
            }
        }
    }


    private class MyMouseHandler extends MouseAdapter
    {
        public void mouseEntered(MouseEvent event)
        {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        }

        public void mouseExited(MouseEvent event)
        {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)); 
            doCursor(-1, -1); // doCursor() is abstract; see client GPanel
        }

        public void mousePressed(MouseEvent event)  // mouse down
        {
            icaret = imouse = event.getX(); 
            jcaret = jmouse = event.getY(); 
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); 
            if (event.getButton()==MouseEvent.BUTTON1)
              bLeftButton= true; 
            if (event.getButton()==MouseEvent.BUTTON3)
              bRightButton= true; 

            uxanchor = getux(getAXPIX(icaret));
            uyanchor = getuy(getAYPIX(jcaret)); 
        }

        public void mouseClicked(MouseEvent event) // mouse up
        {
        }

        public void mouseReleased(MouseEvent event)
        {
            if (bDragged && bLeftButton)
            {
                icaret = event.getX(); 
                jcaret = event.getY();
                manageDragTranslate(icaret-imouse, jcaret-jmouse); 
                imouse = icaret; 
                jmouse = jcaret; 
            }
            if (bDragged && bRightButton)
            {
                icaret = event.getX(); 
                jcaret = event.getY();
                manageDragRotate(icaret-imouse, jcaret-jmouse); 
                imouse = icaret; 
                jmouse = jcaret; 
            }
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            bLeftButton = false; 
            bRightButton = false; 
            if (bDragged)
              getNewMouseArtwork(bFULLART); 
            bDragged = false; 
        }
    }


    private class MyMouseMotionHandler implements MouseMotionListener
    {
        public void mouseMoved(MouseEvent event)
        {
            if (DMF.getFrontGJIF() == myGJIF)
              doCursor(event.getX(), event.getY()); // abstract
        }

        public void mouseDragged(MouseEvent event)
        {
            bDragged = true; 
            doCursor(event.getX(), event.getY());  // abstract
            
            //--------perform dynamic dragging----------
            if (bDragged && bLeftButton)
            {
                icaret = event.getX(); 
                jcaret = event.getY();
                manageDragTranslate(icaret-imouse, jcaret-jmouse); 
                imouse = icaret; 
                jmouse = jcaret; 
            }
            if (bDragged && bRightButton)
            {
                icaret = event.getX(); 
                jcaret = event.getY();
                manageDragRotate(icaret-imouse, jcaret-jmouse); 
                imouse = icaret; 
                jmouse = jcaret; 
            }
        }
    }


    private class MyMouseZoomer implements MouseWheelListener
    {
        public void mouseWheelMoved(MouseWheelEvent mwe)
        {
            int i = mwe.getWheelRotation();
            int j = "T".equals(DMF.reg.getuo(UO_GRAPH, 0)) ? 1 : -1; 
            boolean bShift = mwe.isShiftDown(); 
            if (i*j>0)
              if (bShift)
                manageVertZoomIn();
              else
                manageZoomIn();
            if (i*j<0)
              if (bShift)
                manageVertZoomOut();
              else
                manageZoomOut(); 
        }
    }


    //---------Output routines---------------


    private double getRadius(ArrayList<XYZO> xxList)
    // evaluates the max (x,y) radius of XYZOs in xxList
    {
        double r = 0.0; 
        for (int i=0; i<xxList.size(); i++)
        {
            XYZO myXYZO = xxList.get(i); 
            double x = myXYZO.getX(); 
            double y = myXYZO.getY(); 
            r = Math.max(r, Math.max(Math.abs(x), Math.abs(y))); 
        }
        return r; 
    }


    private void localclip(ArrayList<XYZO> xxList)
    // Clips artwork to a box, double precision.
    // Uses Clipper to do the dirty work. 
    // Also unpacks polylines into separate line segments.
    // Discards all invisible parts.
    // Interprets fills as boundary line segments. 
    // Clipped segments lose their third dimension, sorry.
    {
        ArrayList<XYZO> sList = new ArrayList<XYZO>(); 

        // copy the given xxList over to become our source...
        for (int i=0; i<xxList.size(); i++)
        {
            sList.add(xxList.get(i)); 
        }

        // now empty the given aList...
        xxList.clear(); 

        // now set up a clipper...
        Clipper myClip = new Clipper(-1000, -1000, 1000, 1000); 
        double vec[] = new double[4]; 
        XYZO myXYZO; 
        for (int t=0; t<sList.size(); t++)
        {
            myXYZO = sList.get(t); // copy preexisting object
            double x = myXYZO.getX(); 
            double y = myXYZO.getY(); 
            int op = myXYZO.getO(); 
            int opcode = op % 1000; 

            switch(opcode)
            {
                case MOVETO:  // start decomposing this polyline...
                  vec[2] = x; 
                  vec[3] = y; 
                  break; 

                case PATHTO:  // these are the same now
                case STROKE:  // these are the same now
                case FILL:    // added, eliminating skipto
                  vec[0] = vec[2]; 
                  vec[1] = vec[3]; 
                  vec[2] = x; 
                  vec[3] = y; 
                  if (myClip.clip(vec))
                  {
                     XYZO tempXYZO = new XYZO(vec[0], vec[1], 0.0, MOVETO); 
                     xxList.add(tempXYZO); 
                     tempXYZO = new XYZO(vec[2], vec[3], 0.0, STROKE); 
                     xxList.add(tempXYZO); 
                     // xxList.add(new XYZO(vec[0], vec[1], 0.0, MOVETO)); 
                     // xxList.add(new XYZO(vec[2], vec[3], 0.0, STROKE)); 
                  }
                  break; 

                default:   // deal with singletons here...
                  if( Math.max(Math.abs(x), Math.abs(y)) < 1000) 
                    xxList.add(new XYZO(x, y, 0.0, op));  
                  break; 
            } 
        }
    }



    private void renderList(ArrayList<XYZO> aList, Graphics2D gX, 
         double dStereo, boolean bPreClear)
    // Renders a given List onto a given Graphics2D.
    // Called by redo(), doFinish(), doCAD(), and drawPage(). 
    // Renders g2Tech and gAnno.  (Also caret blinks: see line 70). 
    // So use an EXPLICIT clearRect() at start of g2Tech.
    // A clearRect() here would have gAnno obliterate g2Tech,
    //   if gAnno contained a SETXXXBKG as its initial element. 
    // CenterOrigin for character locations. 
    // adopted BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND
    {
        final double DECISIONRADIUS = 100000.0;

        if ((aList==null) || (gX==null))
          return; 

        int fontcodeprev=0;

        gX.setPaintMode();

        gX.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, 
                                      BasicStroke.JOIN_ROUND));   
        final int MAXPOLY = 2020; 
        int polycount = 0; 
        int ipoly[] = new int[MAXPOLY+1];  // pixels
        int jpoly[] = new int[MAXPOLY+1];  // pixels
        float fdash[] = {3.0f, 2.0f};      // pixels
        int pxoffset=0, pyoffset=0;        // pixels, for centering chars

        if (getRadius(aList) > DECISIONRADIUS)
          localclip(aList); 

        for (int t=0; t<aList.size(); t++)
        {
            // convert list into screen pixels for use by gX....
            XYZO myXYZO = aList.get(t); 
            
            double x = myXYZO.getX();  // rightward
            double y = myXYZO.getY();  // upward
            double z = myXYZO.getZ();  // out of screen 
            z -= uzcenter;             // for stereo balance
            float  fline = 0.0f;       // for line widths

            int ipx = getIXPIX( (int) (x - STEREO*dStereo*z)); 
            int ipy = getIYPIX( (int) y); 

            int opint = aList.get(t).getO(); 
            int opcode = opint % 1000;  
            int fontcode = opint / 1000;         // = ibold + 10*fontsize

            switch (opcode)
            {
               case SETWHITEBKG:  // can only be set as zeroth element
                 if ((t==0) && bPreClear)
                 {
                     if (dStereo==0.0)
                     {
                         gX.setBackground(Color.WHITE);
                         gX.clearRect(0, 0, dim.width, dim.height); 
                         gX.setColor(Color.BLACK); 
                     }
                     else
                     {
                         gX.setBackground(Color.BLACK);
                         gX.clearRect(0, 0, dim.width, dim.height); 
                         if (dStereo > 0.0)
                         {
                             gX.setColor(Color.BLUE); 
                         }
                         if (dStereo < 0.0)
                         {
                             gX.setColor(DRED); 
                         }
                     }
                 }
                 break; 

               case SETBLACKBKG:  // can only be set as zeroth element
                 if ((t==0) && bPreClear)
                 {
                     gX.setBackground(Color.BLACK);
                     gX.clearRect(0, 0, dim.width, dim.height); 
                     gX.setColor(Color.WHITE); 
                     if (dStereo > 0.0)
                     {
                         gX.setColor(Color.BLUE); 
                     }
                     if (dStereo < 0.0)
                     {
                         gX.setColor(DRED); 
                     }
                 }
                 break; 

               case SETSOLIDLINE: 
                  x = Math.max(0.0, Math.min(5.0, x)); 
                  fline = (x==0.0) ? 1.0f : (float) x; 
                  gX.setStroke(new BasicStroke(fline, 
                     BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));   
                  break; 

               case SETDOTTEDLINE:
                  x = U.minmax(x, 0.0, 5.0);; 
                  fline = (x==0.0) ? 1.0f : (float) x;                               
                  gX.setStroke(new BasicStroke(fline, BasicStroke.CAP_BUTT,
                     BasicStroke.JOIN_ROUND, fline+2.0f, fdash, 0.0f));
                  break; 
                  
               case SETRGB:
                  int r = (int) Math.round(255.0 * U.minmax(x, 0, 1)); 
                  int g = (int) Math.round(255.0 * U.minmax(y, 0, 1)); 
                  int b = (int) Math.round(255.0 * U.minmax(z, 0, 1)); 
                  gX.setPaint(new Color(r,g,b)); 
                  break; 
                  
               case MOVETO:
                  polycount=0; 
                  ipoly[polycount] = ipx; 
                  jpoly[polycount] = ipy; 
                  break; 

               case PATHTO:
                  polycount++; 
                  if (polycount >= MAXPOLY)
                    polycount = MAXPOLY-100; 
                  ipoly[polycount] = ipx; 
                  jpoly[polycount] = ipy; 
                  break; 

               case STROKE:
                  polycount++; 
                  if (polycount >= MAXPOLY)
                    polycount = MAXPOLY-100; 
                  ipoly[polycount] = ipx; 
                  jpoly[polycount] = ipy; 
                  polycount++;
                  gX.drawPolyline(ipoly, jpoly, polycount); 
                  polycount = 0; 
                  break; 

               case FILL:
                  // use existing color setting
                  polycount++; 
                  if (polycount >= MAXPOLY)
                    polycount = MAXPOLY-100; 
                  ipoly[polycount] = ipx; 
                  jpoly[polycount] = ipy; 
                  polycount++; 
                  gX.fillPolygon(ipoly, jpoly, polycount); 
                  polycount = 0; 
                  break; 

               default:
                  if ((opcode>31) && (opcode<127))  // ASCII characters
                  {
                     if (fontcode != fontcodeprev)  // new fontcode.
                     {
                        int fpoints = fontcode / 10;
                        int ibold = fontcode % 2; 
                        Font f = new Font("Monospaced", ibold, fpoints); 
                        gX.setFont(f); 
                        // FontRenderContext frc = gX.getFontRenderContext();
                        // charwidth = (int) f.getStringBounds("a", frc).getWidth();
                        pxoffset = 1 + fpoints/4;
                        pyoffset = fpoints/3; 
                        fontcodeprev = fontcode; 
                     }
                     gX.drawString(""+(char)opcode, ipx-pxoffset, ipy+pyoffset); 
                  }
                  else if ((opcode>=130) && (opcode<=179)) // colored symbols
                  {
                      int colorcode = opcode % 10; // native colorcodes are 0....9

                      if (dStereo > 0.0)
                        colorcode = BLUE;
                      if (dStereo < 0.0)
                        colorcode = 10;    // is now DRED; 

                      switch (colorcode)
                      { 
                          case BLACK:   gX.setPaint(Color.BLACK); break; 
                          case RED:     gX.setPaint(Color.RED); break; 
                          case GREEN:   gX.setPaint(Color.GREEN); break; 
                          case YELLOW:  gX.setPaint(Color.YELLOW); break; 
                          case BLUE:    gX.setPaint(Color.BLUE); break; 
                          case MAGENTA: gX.setPaint(Color.MAGENTA); break;                       
                          case CYAN:    gX.setPaint(Color.CYAN); break; 
                          case WHITE:   gX.setPaint(Color.WHITE); break; 
                          case LTGRAY:  gX.setPaint(LGRAY); break;                    
                          case DKGRAY:  gX.setPaint(DGRAY); break; 
                          case 10:      gX.setPaint(DRED); break; 
                      }
                      int dotcode = opcode - (opcode % 10); 
                      switch (dotcode)
                      {
                        case DOT: 
                          gX.drawLine(ipx, ipy-1, ipx+1, ipy-1);
                          gX.drawLine(ipx, ipy,   ipx+1, ipy); 
                          break; 

                        case PLUS:
                          gX.drawLine(ipx-2, ipy, ipx+2, ipy); 
                          gX.drawLine(ipx, ipy-2, ipx, ipy+2); 
                          break; 

                        case SQUARE:
                          gX.drawLine(ipx-1, ipy-1, ipx+1, ipy-1); 
                          gX.drawLine(ipx-1, ipy,   ipx+1, ipy); 
                          gX.drawLine(ipx-1, ipy+1, ipx+1, ipy+1); 
                          break; 

                        case DIAMOND:
                          gX.drawLine(ipx,   ipy-2, ipx,   ipy-2); 
                          gX.drawLine(ipx-1, ipy-1, ipx+1, ipy-1); 
                          gX.drawLine(ipx-2, ipy,   ipx+2, ipy); 
                          gX.drawLine(ipx-1, ipy+1, ipx+1, ipy+1); 
                          gX.drawLine(ipx,   ipy+2, ipx,   ipy+2); 
                          break; 

                        case SETCOLOR:  break; // handled above.
                      }
                  }
                  break; 
            } // end switch(opcode)
        } // end for()
    } // end renderList()



    private void renderListTwice(ArrayList<XYZO> aList, BufferedImage bi, 
         double dStereo, boolean bPreClear)
    // Calls renderList() twice to create a stereo pair. 
    // Triple OR avoids clobbering underlying artwork.
    // Makes cleanup doFinish() unnecessary. 
    {
       BufferedImage biR = new BufferedImage(dim.width, dim.height, 
                 BufferedImage.TYPE_INT_RGB); 
       Graphics2D g2R = (Graphics2D) biR.getGraphics(); 
       renderList(aList, g2R, -dStereo, bPreClear);  // -dS gives red image

       BufferedImage biB = new BufferedImage(dim.width, dim.height, 
                 BufferedImage.TYPE_INT_RGB); 
       Graphics2D g2B = (Graphics2D) biB.getGraphics(); 
       renderList(aList, g2B, dStereo, bPreClear);   // +dS gives blue image

       for (int i=0; i<dim.width; i++)
         for (int j=0; j<dim.height; j++)
           bi.setRGB(i,j, bi.getRGB(i,j) | biR.getRGB(i,j) | biB.getRGB(i,j)); 
    }



    static int getFontWidth(Graphics2D gX, Font fX)
    // Given a size:   int size = 32; 
    // Define a font:  Font font = new Font("Monospaced", Font.BOLD, size);
    // Set the font:   gX.setFont(font);
    // Then, call this:
    {
        FontRenderContext frc = gX.getFontRenderContext();
        return (int) fX.getStringBounds("a", frc).getWidth(); 
    }

} //------end GPanel--------
