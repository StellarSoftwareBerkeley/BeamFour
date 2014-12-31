package com.stellarsoftware.beam;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;          // Rectangle2D
import java.awt.print.*;         // printing
import java.awt.font.*;          // font metric
import java.awt.datatransfer.*;  // clipboard
import java.beans.*;             // vetoableChangeListener
import java.io.*;                // files
import javax.swing.*;            // everything else
import javax.swing.event.*;      // for MenuEvents and InternalFrameAdapter

@SuppressWarnings("serial")

/**
  * A164: installing CopyFieldBottom()
  *   key listener: line 1027
  *   implementor:  line 1221
  *
  *
  * A158:  testing permanent scrollbars, to possibly eliminate the
  *  self-scrolling on Mac platform triggered by resize drag.
  *
  * A152:  TextMode needs line-break pushdown and line-yank pullup.
  *
  *
  * EPanel.java  --- an editor panel for the EJIF frame. 
  *  This class contains a private full size charTable...
  *     private char charTable[][] = new char[JMAX+1][IMAX+1];
  *  but clients may have tighter limitations on table size.
  *
  *  No need to mention: AdjustmentListener, KeyListener, MouseListener, 
  *    MouseMotionListener, FocusListener, accelerator...
  *
  *  2D edit technique: char charTable[][] contains the text.   
  *
  *  This class replaces JTextArea and adds many new features. 
  *  
  *  A129: has DMF.nEdits, helping graphics to coordinate repaints. 
  *
  *  Caret on/off is driven by BJIF and EJIF, not locally. 
  *  Caret location is managed here.
  *
  *  Focus is driven by EJIF's internalFrameListener. 
  *  JScrollBars are managed here. 
  *
  *  A118 implements a simple undo with a single String buffer. 
  *  stashUndo is called at the start of methods...
  *     doDelete()
  *     vLoadString()
  *     clearTable()
  *     putTypedChar()
  *     widenTable()
  *     narrowTable()
  *     copyFieldDown(). 
  *
  *  stashForUndo() is not called in myEJIF.setDirty(true) because that method
  *  of myGJIF is called very frequently: each microchange in the table. 
  *  doUndo() is called only within MyKeyHandler::keyPressed for VK_Z.
  *
  *  Seems to me that stashForUndo() ought to be called at start of AutoAdjust
  *  to permit backing out of .OPT and .RAY adjustments.   To accomplish this
  *  it will need to be public, not private. DONE; and conveyed through EJIF.
  *
  *
  * (c) 2006 Stellar Software
  **/
class EPanel extends JPanel implements B4constants, MouseWheelListener
{ 
    // public static final long serialVersionUID = 42L;  // Xlint 8 Oct 2014

    public EPanel(EJIF ejif) // constructor
    { 
        // no need to initialize base class JPanel "super()"
        myEJIF = ejif;   
        vsbReference = null; 
        clearTable(); 

        //--------set up fieldArray helpers-----------------

        nfields = 0; 

        for (int f=0; f<MAXFIELDS; f++)
        {
            iFieldStartCol[f] = 0;
            iFieldWidth[f] = 0; 
            iFieldTagCol[f] = 0;
            iFieldDecimalPlaces[f] = 0;
            cFieldFormat[f] = '-'; 
        }

        //---initialize sizes; also refresh these each repaint().
        //---repaint() gets called for every resizing. 

        px = getSize().width;     // pixels
        py = getSize().height;    // pixels
        iWidth = px/charwidth;    // number of chars wide
        jHeight = py/charheight;  // number of chars tall

        //---focusTraversal must be disabled for VK_TAB key to work
        //---Caret management does not use focus: mere epiphenomenon. 
        //---so we don't need a focusListener.  
        //---Focus influences keystrokes only. 
        //---But wait! how can LossOfFocus force a caretfree repaint?

        this.setFocusTraversalKeysEnabled(false); 
        this.setFocusable(true);    
        this.addKeyListener(new MyKeyHandler()); 

        this.addMouseListener(new MyMouseHandler()); 
        this.addMouseMotionListener(new MyMouseMotionHandler()); 

        // possible simplification here:
        // Swing MouseInputAdapter = MouseListener + MouseMotionListener().
        // but it does not include any wheel listening.

        this.addMouseWheelListener(this); 

        // Now find out editor contents to manage scroll bars:
        // No no install permanent scrollbars: A158, A163

        hsbReference = myEJIF.createHSB();
        iOff = 0; 
        vsbReference = myEJIF.createVSB(); 
        jOff = 0; 




        getAllLineLengths(); 
        myEJIF.setDirty(false);
    }  //--------end constructor---------------

    public void setCaretFlag(boolean b)
    // Required support for abstract class EJIF
    {
        bCaret = b;
    } 

    public void refreshSizes()
    // After major edits, reestablishes table size & line lengths
    // These are needed internally for further editing. 
    {
        getAllLineLengths();
    }

    public void paintComponent(Graphics g)
    // Paints the visible JPanel and the caret each blink.
    // The frame title is repainted by EJIF. 
    {  
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        fontsize = U.parseInt(DMF.reg.getuo(UO_EDIT, 6)); 
        fontsize = Math.max(3, Math.min(100, fontsize)); 
        boolean bBold = "T".equals(DMF.reg.getuo(UO_EDIT, 7)); 
        int iBold = bBold ? Font.BOLD : Font.PLAIN; 
        Font myFont = new Font("Monospaced", iBold, fontsize); 
        g2.setFont(myFont); 
        charheight = fontsize; 
        charwidth = getFontWidth(g2, myFont); 

        g2.setPaint(Color.BLACK); 

        setEditSmoothing(g2); 

        px = getSize().width;          // pixels showing
        iWidth = px/charwidth;         // characters showing
        py = getSize().height;         // pixels showing
        jHeight = py/charheight;       // characters showing

        manageVSB();  // scroll bar management when size changes
        manageHSB(); 

        int jmin = jOff;  
        int jmax = jOff + jHeight;

        for (int j=jmin; j<jmax; j++)  // j = row of table
        {
            int jwin = j-jmin;         // jwin = row within window
            if (j < JMAX-1)            // within the table
            {
                g2.setPaint(isRowMarked(j) ? BLUEGRAY : Color.WHITE); 
                g2.fillRect(0, jwin*charheight+JPOFF, px, charheight); 
                g2.setPaint(Color.BLACK);  
                linelen[j] = getOneLineLength(j); 
                int count = Math.max(0, linelen[j] - iOff); 
                String s = new String(charTable[j], iOff, count); 
                g2.drawString(s, 0, (jwin+1)*charheight);
                // alternative: g2.drawChars(....)
            }
            else                      // beyond the table
            {
                g2.setPaint(Color.GRAY); 
                g2.fillRect(0, jwin*charheight+JPOFF, px, charheight); 
            }
        } 

        if (myEJIF.getCaretStatus())
        {
            int i = (iCaret-iOff)*charwidth + IOC; 
            int j = (jCaret-jOff)*charheight + JOC; 
            int caretwidth = charwidth; 
            if("T".equals(DMF.reg.getuo(UO_EDIT, 10)))  // text mode
              caretwidth = charwidth/4; 
            g2.setXORMode(Color.YELLOW);
            g2.fillRect(i, j, caretwidth, charheight); 
        }
        // EJIF manages its own title, has own paintComponent(). 

    } //----end paintComponent()

    public void setCaretXY(int field, int row)
    {
        int nfields = getFieldInfo(); 
        if ((field>=0) && (field<nfields))
          iCaret = iFieldStartCol[field]; 
        if ((row>=0) && (row<JMAX-2))
          jCaret = row; 
    }

    public int getCaretY()  // added A106 for AutoRayGen
    {
        return jCaret;
    }
    

    //------------ public administrative methods---------------

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

    public void setVerticalPosition(int rowNumber)
    // Called by Frame when scrollbar produces a message
    {
        jOff = rowNumber; 
        repaint();   // OS eventually calls paintComponent() below
    }

    public void setHorizontalPosition(int colNumber)
    // Called by Frame when scrollbar produces a message
    {
        iOff = colNumber; 
        repaint(); 
    }

    public int getFieldInfo()
    // field start is the transition (start or colon)->noncolon
    // field end is transition noncolon->(colon or end)
    // iFieldWidth[] **excludes** the tag char.
    // returns the number of fields found. 
    {
        /// start by zeroing the field globals...
        nfields = 0; 

        for (int f=0; f<MAXFIELDS; f++)
        {
            iFieldStartCol[f] = 0;
            iFieldWidth[f] = 0; 
            iFieldTagCol[f] = 0;
            iFieldDecimalPlaces[f] = 0;
            cFieldFormat[f] = '-'; 
        }

        int rlen = getOneLineLength(RULER); 

        if (rlen < 5)
          return 0;  // no fields!

        /// search the ruler for its colons...
        char colon = ':';
        char tc = colon; 
        char pc = colon; 
        int point = 0; 
        boolean bstart, btag; 
        for (int i=0; i<rlen; i++)
        {
            tc = (i < rlen-1) ? charTable[RULER][i] : colon; 
            bstart = (pc == colon) && (tc != colon); 
            btag = (pc != colon) && (tc == colon); 
            if (bstart)
              iFieldStartCol[nfields] = i; 
            if ((tc == '.') || (tc == ','))
            {
                point = i;
                cFieldFormat[nfields] = tc; 
            } 
            if ((tc=='E') || (tc=='e'))
              cFieldFormat[nfields] = tc;
            if (btag)
            {
                iFieldTagCol[nfields] = i; 
                iFieldWidth[nfields] = i - iFieldStartCol[nfields];
                if (point > iFieldStartCol[nfields])
                  iFieldDecimalPlaces[nfields] = i-point-1; 
                else
                  iFieldDecimalPlaces[nfields] = iFieldWidth[nfields]/2; 

                if (nfields < MAXFIELDS-1)
                  nfields++;
            }
            pc = tc; 
        }
        return nfields; 
    }
    
    public String getFieldFull(int f, int jrow)
    // String excludes tag char but includes leading & trailing blanks
    // For clean string contents, user should apply .trim()
    {
        if ((f<0) || (f >= nfields))
          return new String(""); 
        if ((jrow<0) || (jrow>=nlines))
          return new String(""); 
        int iLeft = iFieldStartCol[f]; 
        int width = iFieldWidth[f];
        return new String(charTable[jrow], iLeft, width); 
    }

    public String getFieldTrim(int ifield, int jrow)
    // Returns the trimmed field, no leading or trailing blanks
    {
        return getFieldFull(ifield, jrow).trim(); 
    }

    public double getFieldDouble(int ifield, int jrow)
    // empty returns -0.0; badnum returns Double.NaN
    // U.suckDouble() includes trimming and -0 for empty.
    {
        return U.suckDouble(getFieldFull(ifield, jrow)); 
    }

    public int getFieldWidth(int f)
    {
        return iFieldWidth[f];
    }

    public void putFieldString(int f, int jrow, String s)
    // Puts a field into an existing table with room. 
    {
        if ((f<0) || (f>=nfields) || (jrow<0) || (jrow>nlines))
          return; 
        int ileft = iFieldStartCol[f]; 
        int len = (jrow==0) ? s.length() : iFieldWidth[f];  // no tag.
        for (int i=0; i<len; i++)
          charTable[jrow][ileft + i] = U.getCharAt(s, i); 
        myEJIF.setDirty(true); 
        // note: U.getCharAt() returns blanks as needed.
    }

    public void forceFieldString(int f, int jrow, String s)
    // Enlarges table if necessary to accommodate jrow
    {
        if ((f<0) || (f>=nfields) || (jrow<0) || (jrow>JMAX-5))
          return; 
        if (jrow >= nlines)
        {
            putLineWithColons(jrow);
            nlines = jrow+1; 
        }
        int ileft = iFieldStartCol[f]; 
        int len = iFieldWidth[f];  // excludes tag.
        for (int i=0; i<len; i++)
          charTable[jrow][ileft + i] = U.getCharAt(s, i); 
        myEJIF.setDirty(true); 

        // note: U.getCharAt() returns blanks as needed.
    }

    public void putFieldDouble(int f, int jrow, double d)
    {
        String s =  U.fmtc(d, 
                           iFieldWidth[f], 
                           iFieldDecimalPlaces[f], 
                           cFieldFormat[f]);
        putFieldString(f, jrow, s);
        myEJIF.setDirty(true); 
    }

    public void forceFieldDouble(int f, int jrow, double d)
    {
        String s =  U.fmtc(d, 
                           iFieldWidth[f], 
                           iFieldDecimalPlaces[f], 
                           cFieldFormat[f]);
        forceFieldString(f, jrow, s);
        myEJIF.setDirty(true); 
    }

    public char getTagChar(int f, int jrow)
    {
        return charTable[jrow][iFieldTagCol[f]]; 
    }

    public void putTagChar(int f, int jrow, char c)
    {
        charTable[jrow][iFieldTagCol[f]] = c; 
        myEJIF.setDirty(true); 
    }

    public boolean hasContent()
    {
        return (getAllLineLengths() > 0); 
    }

    public int getLineCount()
    {
        return getAllLineLengths(); 
    } 

    public int getLineLength(int jrow)
    {
        return linelen[jrow]; 
    }

    public int getICaret()
    {
        return iCaret;
    }
   
    public int getJCaret()
    {
        return jCaret;
    }

    public int getCaretFieldNum()
    {
        return getWhichField(iCaret); 
    }

    public int getNumFields()
    {
        return getFieldInfo(); 
    }
   
    public int getGuideNumber()
    // Returns the intended number of user records in the table. 
    {
        String s = new String(charTable[0], 0, 20); 
        return U.suckInt(s); 
    }

    public boolean isMarked()  // public for edit graying
    {
        return jDrag >= 0; 
    }

    public void doUnmarkRepaint()
    {
        jDrag = -1;
        repaint(); 
    }

    public void doSelectAll()   // for edit menu
    {
        getAllLineLengths(); 
        jDown = 0; 
        jDrag = nlines-1; 
        repaint(); 
    }


    //------------- public i/o methods-------------------------

    public String getLine(int j)
    // assumes that getAllLineLengths() has been called first
    // to initialize nlines and individual line lengths. 
    {
        if ((j<0) || (j>=nlines))
          return ""; 
        return new String(charTable[j], 0, linelen[j]);
    }

    public void vLoadSkeleton()
    {
        DMF.nEdits++; 
        clearTable(); 
        for (int i=0; i<60; i++)
          charTable[2][i] = '-'; 
        for (int j=2; j<15; j++)
          for (int i=10; i<60; i+=10)
            charTable[j][i] = ':';
        iMouse = jMouse = jDown = iCaret = jCaret = iOff = jOff = 0; 
        jDrag = -1; 
        myEJIF.setDirty(false);
        getAllLineLengths();
        getFieldInfo(); 
    }    

    public boolean save(File f)
    // Uses println() to generate local platform EOLs.
    {
        getAllLineLengths();   // sets up linelengths & nlines. 
        try
        { 
           PrintWriter pw = new PrintWriter(new FileWriter(f), true);
           for (int j=0; j<nlines; j++)
           {
              String s = new String(charTable[j], 0, linelen[j]); 
              pw.println(s); 
           }
           pw.close(); 
           myEJIF.setDirty(false); 
           return true; 
        }
        catch (IOException ioe) 
        { return false; }
    }

    public String getMarkedText()
    // Reads text in a table. 
    // This does not upset jCaret or existing markings,
    // so that "cut" can subsequently doDelete().
    // Performs tab delimiter substitutions when option=TABS.
    {
        getAllLineLengths(); 
        if (!isMarked())
          return ""; 
        StringBuffer sb = new StringBuffer(10000); 
        int j0 = Math.min(jDown, jDrag); 
        int j1 = Math.max(jDown, jDrag); 
        for (int j=j0; j<=j1; j++)
        {
            sb.append(charTable[j], 0, linelen[j]); 
            sb.append('\n'); 
        }

        //------perform tab substitutions-------------
        //-------use charTable[2] as ruler------------
        //------don't clobber any EOLs!---------------

        if (DMF.reg.getuo(UO_EDIT, 5).equals("T"))
        {
            int iX=0, j=j0; 
            for (int i=0; i<sb.length(); i++)
            {
                if (j>0)
                  if ((charTable[2][iX]==COLON) && (sb.charAt(i)!='\n'))
                    sb.setCharAt(i, TAB); 

                iX++; 
                if (sb.charAt(i) == '\n')
                {
                    iX=0;
                    j++;
                } 
            }
        }
        return new String(sb); 
    }

    public void setCaretToMark()
    {
        int jMark = Math.min(jDown, jDrag); 
        if (jMark >= 0)
          jCaret = jMark; 
    }

    public void doDelete()
    // deletes lines that are in the marked zone
    // this might be faster using System.arrayCopy()
    {
        DMF.nEdits++; 
        getAllLineLengths(); 
        stashForUndo(); 
        if (!isMarked())
          return; 
        int j0 = Math.min(jDown, jDrag); 
        int j1 = Math.max(jDown, jDrag); 
        int nmarked = 1 + j1 - j0; 
        for (int j=j0; j<JMAX-nmarked; j++)
          for (int i=0; i<IMAX; i++)
            charTable[j][i] = charTable[j+nmarked][i]; 
        for (int j=JMAX-nmarked; j<JMAX; j++)
          clearLine(j); 
        // any need to rearrange jCaret? 
        getAllLineLengths(); 
        myEJIF.setDirty(true);
        doUnmarkRepaint(); 
    }

    public void vLoadString(String s, boolean preclear)
    // Handles PC, Mac, Unix EOL format strings from file or clipboard.
    // Always inserts, never overwrites. 
    // Can receive entire table: decides colons vs CSV/Tab;
    // or just the data portion of a table, uses existing ruler. 
    // Called by EJIF.pasteInto() or by EJIF.loadFile(). 
    // For pastes, want caret left at bottom of each.
    // For fresh loads, prefer jCaret = 0. 
    // What about complete pastes where jStart == 0? Bottom. 
    // So, use preclear to set jCaret = 0. 
    {
        DMF.nEdits++; 
        stashForUndo();
        if (s.length() < 1)
          return; 
        if (preclear)
          clearTable();  // also zeroes caret, scrollers

        getAllLineLengths(); 
        int i=0;         
        char c, cprev=' '; 

        boolean bComplete = (jCaret == 0); 
        boolean bForeign = (getDelimiterStatus(s, jCaret) == 2); 

        period = 1 + U.parseInt(DMF.reg.getuo(UO_EDIT, 3)); 
        period = Math.max(4, Math.min(20, period));

        // start loading at line = jCaret...

        for (int k=0; k<s.length(); k++)
        {
            if (i==0)                  // start a new line?
              pushDownOneLine(jCaret); // insertion; clears jCaret line too. 

            c = U.getCharAt(s, k); 
            switch (c)
            {
               // case COMMA:
               // case SEMICOLON:
               case TAB: 
                  if (jCaret < 3)     // build new ruler
                    i = formulaTagPos(i, period);
                  else                // use existing ruler
                    i = rulerTagPos(i, period); 
                  if ((i<IMAX) && (jCaret<JMAX) && (jCaret>1))
                    charTable[jCaret][i] = COLON;
                  if (i<IMAX-2)
                    i++;              // prepare for next character
                  break; 
               case LF: 
                  if (cprev==CR) break; 
                  i=0; jCaret++; break; 
               case CR:
                  i=0; jCaret++; break; 
               default:
                  if ((c>=SPACE) && (c<='~') && (i<IMAX-2))
                  {
                      charTable[jCaret][i] = c; 
                      i++;
                  }
            }
            if (jCaret>=JMAX-2)
              break;
            cprev = c; 
        } 
        getAllLineLengths();
        iCaret = 0;
        if (preclear)
          jCaret = 0; 
        getFieldInfo(); 
        repaint(); 
    }


    public void stashForUndo()
    // called locally for significant changes to the charTable, 
    // or via EJIF at start of AutoAdjust (hence public).
    {
        sUndo = getTableString(); 
        nstash++; 
    }

    //-----------------end of public methods-------------------------





    //-----------------begin private area----------------------------
    //-----------------begin private area----------------------------
    //-----------------begin private area----------------------------
    //-----------------begin private area----------------------------
    //--------move all these constants into Constants.java??----------

    private static final Color BLUEGRAY = new Color(188, 188, 255); 
    private static final int JPOFF = 3;   // vert paint offset
    private static final int IOC = 0;     // horiz caret offset
    private static final int JOC = 2;     // vert caret offset
    private static final int TABJUMP = 8;  
    private static final int VERTJUMP = 8; 

    //-------------here is the char table-----------------

    private char charTable[][] = new char[JMAX+1][IMAX+1];

    //-------------other private fields---------------

    private String sUndo = new String(""); 
    private int nstash = 0;       // diagnostics only
    
    private int linelen[] = new int[JMAX]; 
    private int nlines = 0;
    private int maxlinelen = 0;  
    private int period = 10;      // fieldwidth+1
    private int fontsize = 16; 
    private int charwidth = 8;    // horiz char spacing
    private int charheight = 16;  // vert char spacing

    private boolean bCaret = false; 
    private int iCaret, jCaret;   // caret column & row 
    private int iOff, jOff;       // scroll offset column & row
    private int iMouse, jMouse;   // unused
    private int jDown=0;          // start of drag, for graying
    private int jDrag=-1;         // -1=noMark, else end of drag 

    private int px, py;           // client window w,h in pixels
    private int iWidth, jHeight;  // client window w,h in chars

    private JScrollBar vsbReference = null;
    private JScrollBar hsbReference = null; 
    private EJIF myEJIF             = null; 

    //----------fieldArray helpers: obsolescent??------------

    private int nfields = 0;  
    private int iFieldStartCol[]      = new int[MAXFIELDS];
    private int iFieldWidth[]         = new int[MAXFIELDS]; 
    private int iFieldTagCol[]        = new int[MAXFIELDS];
    private int iFieldDecimalPlaces[] = new int[MAXFIELDS]; 
    private char cFieldFormat[]       = new char[MAXFIELDS]; 
    private char cColons[]            = new char[IMAX]; 


    //--------------private methods-------------------

    private String getTableString()
    // Multipurpose string sucker.
    // Called by private swapUndo() and by public stashForUndo().
    {
        StringBuffer sb = new StringBuffer(1000); 
        getAllLineLengths(); 
        for (int j=0; j<nlines; j++)
        {
            sb.append(charTable[j], 0, linelen[j]); 
            sb.append('\n'); 
        }
        return sb.toString(); 
    }
    
    private void putTableString(String sGiven)
    // Clears the charTable and installs a given String.
    // Also tidies up the diagnostics, and redisplays. 
    // Assumes EOL is '\n' and so is not multiplatform.
    {
        DMF.nEdits++; 
        int i=0, j=0, k=0; 
        for (j=0; j<JMAX; j++)    // clear the table
          for (i=0; i<IMAX; i++)
            charTable[j][i] = ' '; 
        i=0;
        j=0; 
        for (k=0; k<sGiven.length(); k++)  // char loop
        {
            char c = U.getCharAt(sGiven, k); 
            if (c == '\n')
            {
                j++; 
                i=0; 
            }
            else
            {
                charTable[j][i] = c; 
                i++;
            }
        } 
        getAllLineLengths();
        getFieldInfo(); 
        repaint();
    }

    private void swapUndo()
    // called by local myKeyHandler() method for Ctl-Z.
    {
        DMF.nEdits++; 
        if (sUndo.length() < 1)
          return; 
        String sTemp = getTableString();        
        putTableString(sUndo); 
        sUndo = sTemp; 
    }

    private int formulaTagPos(int i, int p)
    // Used by vLoadString to generate tags when ruler=formula.
    // if p=10: 0...9->9; 10...19->19 etc
    {
        return ((i+p)/p)*p - 1;  // new formula, equal field widths
    }

    private int rulerTagPos(int i,  int p)
    // Used by vLoadString to generate tags from a given ruler.
    // Used by myKeyHandler to implement TAB function. 
    // If "i" lies beyond final ruler colon, it reverts to the formula.
    // Least possible result is i, that is no skipping; nondecreasing.
    // Assumes charTable[2][i] has been properly set up!
    {
        for (int k=i; k<IMAX-2; k++)
          if (charTable[2][k] == COLON)
            return k; 
        return formulaTagPos(i, p); 
    }

    int doBackTab(int i) 
    // Used by myKeyHandler to implement BackTab.
    // Avoids use of field organizers. 
    {
        for (int k=i-2; k>0; k--)
          if (charTable[2][k] == COLON)
            return k+1; 
        return 0; 
    }

    private int getWhichField(int icol)
    // Each field must have a ruler colon tag.
    {
        if (nfields < 1)
          return ABSENT; 
        if (icol < iFieldStartCol[0])
          return ABSENT; 
        if (icol > iFieldTagCol[nfields-1])
          return ABSENT;  // no such field
        for (int f=0; f<MAXFIELDS; f++)
          if (iFieldTagCol[f] >= icol)
            return f; 
        return 0; 
    }

    private void clearLine(int j)
    // When exactly is this called?
    {
        DMF.nEdits++; 
        for (int i=0; i<IMAX; i++)
          charTable[j][i] = ' '; 
        myEJIF.setDirty(true);  
    }

    private void pushDownOneLine(int j)
    // Used by vLoadString() and TextMode ENTER key
    // Inserts one blank line into the table at "j". 
    // For multiple line calls, we want only the initial preview saved.
    // so here, no stashForUndo().    
    {
        DMF.nEdits++; 
        j = Math.max(0, j); 
        for (int t=JMAX; t>j; t--)  
          System.arraycopy(charTable[t-1], 0, charTable[t], 0, IMAX);           
        clearLine(j); 
        myEJIF.setDirty(true); 
    }
    
    private void pullUpOneLine(int j)
    // line j will vanish, receiving text of j+1, etc.
    // used by TextMode backspace at jCaret=0
    {
        DMF.nEdits++; 
        for (int t=j; t<JMAX; t++)
          System.arraycopy(charTable[t+1], 0, charTable[t], 0, IMAX); 
    }

    private void clearTable()
    {
        DMF.nEdits++; 
        stashForUndo(); 
        for (int j=0; j<JMAX; j++)
          clearLine(j); 
        iMouse = jMouse = jDown = iCaret = jCaret = iOff = jOff = 0; 
        jDrag = -1; 
        myEJIF.setDirty(false); 
        getFieldInfo(); 
    }    

    private int getDelimiterStatus(String s, int jcaret)
    // Examines a prospective data string for its delimiters. 
    // if jcaret<3, tests rulerline, else tests lineZero. 
    // Returns 0=unknown, 1=colons=native, 2=foreign=CSV/Tab
    // Needed if a string is to be inserted at jCaret=0,
    // because native->UseColonPattern; foreign->Use UO_EDIT_FWIDTH.
    {
        int j=0, ftype=0; 
        int jtest = Math.max(0, 2-jcaret); 
        for (int i=0; i<s.length(); i++)
        {
            char c = s.charAt(i); 
            if (c=='\n')
              j++; 
            if (j==jtest)  // test line
            {
                switch (c)
                {
                   case COLON: ftype=1; break; 
                   // case COMMA:
                   // case SEMICOLON: 
                   case TAB:  ftype=2; break;
                }
                if (ftype != 0)
                  break;
            } 
            if (j>jtest)
              break; 
        }
        return ftype; 
    }

    private boolean isRowMarked(int j)
    {
        if (jDrag<0)
          return false; 
        return (j-jDown)*(j-jDrag) <= 0; 
    }

    private int getAllLineLengths()
    // Sets local nlines and linelen[] and maxlinelen.
    // Individual linelen[] can be as big as IMAX-1
    {
        nlines = 0; 
        maxlinelen = 0; 
        for (int j=JMAX-1; j>=0; j--)
        {
            linelen[j] = getOneLineLength(j); 
            if ((nlines==0) && (linelen[j]>0))
              nlines = j+1;
            if (linelen[j] > maxlinelen)
              maxlinelen = linelen[j]; 
        }  
        for (int i=0; i<IMAX; i++)
          cColons[i] = (charTable[RULER][i] == ':') ? ':' : ' '; 
        manageVSB(); 
        return nlines; 
    }

    private void putLineWithColons(int j)
    // use this only after having run getAllLineLengths()
    {
        DMF.nEdits++; 
        if (j > RULER)
          for (int i=0; i<IMAX; i++)
            charTable[j][i] = cColons[i]; 
        myEJIF.setDirty(true); 
    }
    
    private int getOneLineLength(int j)
    {
        charTable[j][IMAX-1] = SPACE; // enforce terminal SP
        int len = 0; 
        for (int i=IMAX-1; i>=0; i--)
          if (charTable[j][i] != SPACE)
          {
              len = i+1; 
              break; 
          }
        return len;
    }

    StringBuffer fieldToStringBuffer()   
    // converts field or marked segment into a stringBuffer
    {
        getAllLineLengths();
        int jmin = isMarked() ? Math.min(jDown, jDrag) : 0;
        int jmax = isMarked() ? Math.max(jDown, jDrag) : nlines-1; 
        StringBuffer sb = new StringBuffer((jmax-jmin+2)*IMAX);
        for (int j=jmin; j<=jmax; j++)
        {
            sb.append(charTable[j], 0, linelen[j]); 
            sb.append(LF); 
        }
        return sb; 
    }

    private class MyKeyHandler implements KeyListener
    {  
        public void keyPressed(KeyEvent event)
        {  
            switch (event.getKeyCode())
            {
               case KeyEvent.VK_HOME:
                   iCaret = jCaret = 0; 
                   break; 

               case KeyEvent.VK_PAGE_UP:
                   jCaret = Math.max(0, jCaret-VERTJUMP); 
                   break; 

               case KeyEvent.VK_PAGE_DOWN:
                   jCaret = Math.min(JMAX-2, jCaret+VERTJUMP); 
                   break; 

               case KeyEvent.VK_Z: 
                   if (event.isControlDown() || event.isMetaDown())
                     swapUndo();
                   break; 
                   
               case KeyEvent.VK_LEFT:
                   if (event.isControlDown() || event.isMetaDown())
                     narrowTable(iCaret);       // narrow
                   else
                     iCaret = Math.max(0, iCaret-1); 
                   break; 

               case KeyEvent.VK_RIGHT:
                   if (event.isControlDown() || event.isMetaDown())
                     widenTable(iCaret, false); // widen, no colons 
                   else if (event.isAltDown())  // Alt Right 
                     widenTable(iCaret, true);  // widen, insert colons
                   else
                     iCaret = Math.min(IMAX-2, iCaret+1); 
                   break; 

               case KeyEvent.VK_UP:
                   jCaret = Math.max(0, jCaret-1); 
                   break; 

               case KeyEvent.VK_DOWN:
                   if (event.isAltDown())       // Alt + Down
                   {
                       if (event.isControlDown())
                         CopyFieldBottom(); 
                       else
                         CopyFieldDown(); 
                   }
                   else
                     jCaret = Math.min(JMAX-2, jCaret+1); 
                   break; 

               case KeyEvent.VK_ENTER:
                   if ("T".equals(DMF.reg.getuo(UO_EDIT, 10)))   // text mode
                   {
                       if (jCaret < JMAX)
                       {
                           stashForUndo(); 
                           pushDownOneLine(jCaret+1);        // clear line below
                           int ncopy = IMAX - iCaret;        // nchars to copy 
                           System.arraycopy(charTable[jCaret], iCaret, charTable[jCaret+1], 0, ncopy); 
                           
                           for (int i=iCaret; i<IMAX; i++)   // blank source chars
                             charTable[jCaret][i] = SPACE; 
                           jCaret++;
                           iCaret = 0; 
                       }   
                   }
                   else             // table mode, no table changes.
                   {
                       iCaret = 0; 
                       jCaret = Math.min(JMAX-2, jCaret+1); 
                   }
                   break; 

               case KeyEvent.VK_BACK_SPACE:
               case KeyEvent.VK_DELETE:
                   stashForUndo(); 
                   if ("T".equals(DMF.reg.getuo(UO_EDIT, 10)))   // text mode
                   {
                       if ((iCaret==0) && (jCaret>0))            // pull up OK
                       {
                           int istart = getOneLineLength(jCaret-1);
                           int iavail = IMAX - istart;   
                           for (int k=0; k<iavail; k++)          // append to above
                             charTable[jCaret-1][k+istart] = charTable[jCaret][k]; 
                           pullUpOneLine(jCaret);                // raise lines below
                           jCaret--; 
                           iCaret = istart; 
                       }
                       else if (iCaret>0)      // pull chars leftward
                       {
                           iCaret--; 
                           for (int k=iCaret; k<IMAX-2; k++)
                             charTable[jCaret][k] = charTable[jCaret][k+1]; 
                       }
                   }
                   else if (iCaret > 0)   // table mode
                   {
                       iCaret--; 
                       charTable[jCaret][iCaret] = ' ';   
                   }
                   myEJIF.setDirty(true); 
                   if (jCaret == RULER)
                     getFieldInfo(); 
                   break; 

               case KeyEvent.VK_TAB: 
                   if (event.isShiftDown()) 
                     iCaret = doBackTab(iCaret);
                   else
                     iCaret = 1+rulerTagPos(iCaret, period); 
                   break;    

               case KeyEvent.VK_F7:
                   narrowTable(iCaret); 
                   break; 

               case KeyEvent.VK_F8:
                   widenTable(iCaret, false); 
                   break; 

               case KeyEvent.VK_F9:
                   widenTable(iCaret, true); 
                   break; 

               case KeyEvent.VK_F10: 
                   CopyFieldDown(); 
                   event.consume(); 
                   break; 
            }

            if (iCaret < iOff)
              iOff = iCaret; 
            if (iCaret >= iOff + iWidth-1)
              iOff = Math.max(0, iCaret-iWidth+1); 

            if (jCaret < jOff)
              jOff = jCaret; 
            if (jCaret >= jOff + jHeight-1)
              jOff = Math.max(0, jCaret-jHeight+1); 

            if (vsbReference != null)
              vsbReference.setValue(jOff); 
            if (hsbReference != null)
              hsbReference.setValue(iOff); 
            repaint();
        }

        public void keyReleased(KeyEvent event) 
        { 
        }

        public void keyTyped(KeyEvent event)
        // Process typed chars only, lower & upper case. 
        // Accelerator keys are handled elsewhere.
        {  
            DMF.nEdits++; 
            char c = event.getKeyChar(); 
            int mod = event.getModifiers(); 
            if ((c>=' ') && (c<='~'))             // avoids BKSP and DEL
              if ((mod==0) || (mod==java.awt.Event.SHIFT_MASK))
              {
                  stashForUndo(); 
                  iCaret = Math.max(0, Math.min(IMAX-2, iCaret)); 
                  jCaret = Math.max(0, Math.min(JMAX-1, jCaret)); 
                  if("T".equals(DMF.reg.getuo(UO_EDIT, 10)))  // text mode shove right
                    for (int k=IMAX-1; k>iCaret; k--)
                      charTable[jCaret][k] = charTable[jCaret][k-1];
                  charTable[jCaret][iCaret] = c; 
                  iCaret = Math.min(IMAX-2, iCaret+1);   // increment iCaret
                  if ((c>' ') && (jCaret+1>nlines))
                    nlines = jCaret+1;                   // helps Vscrolling
                  if ((c>' ') && (iCaret>maxlinelen))
                    maxlinelen = iCaret;                 // helps Hscrolling
                  myEJIF.setDirty(true);               
              }
            if (jCaret == RULER)
              getFieldInfo(); 
        }

    } //---end private class MyKeyHandler------------
    

    void widenTable(int i, boolean bColons)
    {
        DMF.nEdits++; 
        getAllLineLengths();
        stashForUndo(); 
        for (int j=1; j<nlines; j++)
        {
            System.arraycopy(charTable[j], i, charTable[j], i+1, IMAX-i-1); 
            charTable[j][i] = SPACE; 
        }
        if (bColons)
          for (int j=RULER; j<nlines; j++)
            charTable[j][i] = ':'; 
        else
          charTable[RULER][i] = '-'; 
        myEJIF.setDirty(true); 
        getAllLineLengths();
        getFieldInfo(); 
    }

    void narrowTable(int i)
    {
        DMF.nEdits++; 
        if ((i<0) || (i>IMAX-2))
          return; 
        stashForUndo(); 
        getAllLineLengths();
        for (int j=1; j<nlines; j++)
          System.arraycopy(charTable[j], i+1, charTable[j], i, IMAX-i-1); 
        getAllLineLengths();
        getFieldInfo(); 
        myEJIF.setDirty(true); 
    }

    void CopyFieldDown()
    /// copies the data field and its tag char.
    {
        DMF.nEdits++; 
        if ((jCaret>RULER) && (jCaret<nlines-1))
        {
            // stashForUndo(); // yikes! ruins the function. 
            int field = getWhichField(iCaret); 
            String s = getFieldFull(field, jCaret); 
            char c = getTagChar(field, jCaret); 
            jCaret++; 
            putFieldString(field, jCaret, s); 
            putTagChar(field, jCaret, c);
            myEJIF.setDirty(true); 
        }
    }
    
    void CopyFieldBottom()
    /// copies field and tag all the way to the bottom
    {
        DMF.nEdits++; 
        if ((jCaret>RULER) && (jCaret<nlines-1))
        {
            // stashForUndo(); // yikes! ruins the function. 
            int field = getWhichField(iCaret); 
            String s = getFieldFull(field, jCaret); 
            char c = getTagChar(field, jCaret); 
            for (int j=jCaret+1; j<nlines; j++)
            {
                putFieldString(field, j, s); 
                putTagChar(field, j, c);
            }
            myEJIF.setDirty(true); 
        }
    }
    

    //------------------- mouse stuff --------------------        

    private class MyMouseHandler extends MouseAdapter
    {
        public void mousePressed(MouseEvent event)
        {
            iCaret = ((int) event.getPoint().getX())/charwidth + iOff; 
            iCaret = Math.max(0, Math.min(IMAX-2, iCaret)); 
            jCaret = ((int) event.getPoint().getY())/charheight + jOff; 
            jCaret = Math.max(0, Math.min(JMAX-1, jCaret));
            jDown = jCaret; 
            jDrag = -1; 
            repaint();
        }
    }


    private class MyMouseMotionHandler implements MouseMotionListener
    {
        public void mouseMoved(MouseEvent event)
        {
        }

        public void mouseDragged(MouseEvent event)
        // Horstmann & Cornell v.1 p.308: drag beyond borders OK.
        // Uses beyond-borders drag numbers to force scrolling. 
        // Remember to reposition the caret. 
        // Remember to keep caret within display area. 
        // Remember to update the vertical scrollbar.
        {
            int jPix = event.getY(); 
            if ((jPix < 0) && (jOff > 0))
              jOff--; 
            if ((jPix > py) && (jOff < JMAX-10))
              jOff++; 
            jPix = Math.max(0, Math.min(py, jPix)); // anti escape
            jDrag = jPix/charheight + jOff; 
            jDrag = Math.max(0, Math.min(JMAX-1, jDrag)); 
            jCaret = jDrag; 
            if (vsbReference != null)
              vsbReference.setValue(jOff); // update vertical scrollbar
            repaint(); 
        }
    }


    public void mouseWheelMoved(MouseWheelEvent e) 
    {
        int notches = e.getWheelRotation(); 
        if (notches == 0)
          return; 
        jOff += 4*notches; 
        jOff = Math.max(0, Math.min(JMAX-10, jOff)); 

        // now move the host frame's scroll button
        if (vsbReference != null)
          vsbReference.setValue(jOff); 
        repaint();  
    }
    

    //---------------scrollbar management----------------

    private void manageVSB()  // vertical scrollbar
    {
        boolean bNeed = (nlines>=jHeight) || (jOff>0); 
        if ((myEJIF!=null) && (vsbReference==null) && bNeed)
          vsbReference = myEJIF.createVSB(); 

        /**********no no do not destroy*************
        if ((myEJIF!=null) && (vsbReference!=null) && !bNeed)
          vsbReference = myEJIF.destroyVSB(); 
        *******************************************/

        /// added Mar 2012, A135 making VSB track actual nlines
        if ((myEJIF!=null) && (vsbReference!=null) && bNeed)
          vsbReference.setMaximum(nlines); 
    }
    

    private void manageHSB()  // horizontal scrollbar
    {
        boolean bNeed = (maxlinelen>=iWidth) || (iOff>0); 
        if ((myEJIF!=null) && (hsbReference==null) && bNeed)
          hsbReference = myEJIF.createHSB(); 
       
        /*************no no do not destroy************************
        if ((myEJIF!=null) && (hsbReference!=null) && !bNeed)
          hsbReference = myEJIF.destroyHSB(); 
        *******************************************************/
    }


    //-------------- static utilities --------------

    static void beep()
    {
       Toolkit.getDefaultToolkit().beep(); 
    }


    static int getFontWidth(Graphics2D g2, Font f)
    // Given a size:   int size = 32; 
    // Define a font:  Font font = new Font("Monospaced", Font.BOLD, size);
    // Set the font:   g2.setFont(font);
    // Then, call this:
    {
        FontRenderContext frc = g2.getFontRenderContext();
        return (int) f.getStringBounds("a", frc).getWidth(); 
    }


} //-----------end EPanel class------------------------------
