package com.stellarsoftware.beam;

// import java.awt.image.*;     // BufferedImage
import java.util.*;             // ArrayList
import java.io.*;               // File 
import javax.swing.*;           // JFileChooser
// import javax.imageio.*;      // PNG, JPEG

/**  CAD.java
  *  Static output methods, called by AnnoPanel
  *  when an AnnoPanel extension receives a call
  *  doCAD() from DMF. 
  *
  *  Caution: Registry is unknown here.
  *
  *  Bitmap images were eliminated in A121: vector graphics only.
  *
  * Native quadlist system is CenterOriginPoints, +X=right, +Y=up, +Z=out.
  * Artwork quads have -250.0<x,y<250.0;  z is unlimited but usually zero.
  * Pixels are the same, except origin = ULCorner and +Y=down. 
  * Conversion to pixels uses local getIXPIX(x), getIYPIX(y).
  *
  * To get user coords from the artwork, the quadlist includes
  * userconsts x,y,z = offsets for that quadlist view;
  * userslopes x,y,z = magnifications for that quadlist view;
  * Therefore...
  * UserValue x,y,z = userConst + userSlope * quadListValue.
  *
  * Each char comes with its own fontcode = sum of terms:
  *     ASCIIchar = opcode % 1000 = bottom three digits
  *     fontcode = opcode / 1000  = higher digits
  *     fontcode = 10*fontsizePoints + (bBold ? 1 : 0)
  *     fontsizePoints = fontcode / 10;
  *     bBold = (1 == fontcode % 2); 
  * Must generate a new makeFont instruction if fontcode changes.
  * Initially zero (no font) so initial char creates makeFont.
  *
  * August 2009: has user selectable PostScript linewidths; see lines 230-250
  * Jan 2011:  moved bitmap formats into BJIF WriteImage group. 
  *
  *  DXF methods need to backconvert from points to UserDims.  <<done.
  *
  *  @author M.Lampton (c) 2004 STELLAR SOFTWARE all rights reserved.
  */
class CAD implements B4constants  
{

    private static int prevFontCode=0;  // serves all CAD flavors


    public static boolean doCAD(int style, boolean bPort, 
                    ArrayList<XYZO> a, ArrayList<XYZO> b, ArrayList<XYZO> c)
    // Called via DMF >> GJIF >> GPanel line 245>> here, like this: 
    // CAD.doCAD(style, bPortrait, myTechList, myRandList, myAnnoList);
    // No bitmap options here; instead see BJIF:doQuickPng().
    {
        if (a == null)
          return false; 
        JFileChooser jfc = new JFileChooser(); 
        String sDir = DMF.sCurrentDir; 
        if (sDir != null)
        {
            File fDir = new File(sDir); 
            if (fDir != null)
              if (fDir.isDirectory())
                jfc.setCurrentDirectory(fDir);
        } 
    
        // System.out.println("doCAD CWD = "+jfc.getCurrentDirectory()); 
        
        int q = jfc.showSaveDialog(null); 
        if (q == JFileChooser.CANCEL_OPTION)
          return false; 
        File file = jfc.getSelectedFile(); 
        if (file == null)
          return false; 

        DMF.sCurrentDir = file.getParent();

        prevFontCode = 0; 
        boolean ok=false; 
        switch(style)
        {
           case 0:   ok = listPS(bPort, a, b, c, file); break; 
           case 1:   ok = listPLOT(bPort, 0, a, b, c, file); break; 
           case 2:   ok = listPLOT(bPort, 1, a, b, c, file); break; 
           case 3:   ok = listPLOT(bPort, 2, a, b, c, file); break; 
           case 4:   ok = listPLOT(bPort, 3, a, b, c, file); break; 
           case 5:   ok = listPLOT(bPort, 4, a, b, c, file); break; 
           case 6:   ok = listDXF(2, a, b, c, file); break; 
           case 7:   ok = listDXF(3, a, b, c, file); break;
           case 8:  ok = listQuads(a, b, c, file); break; 
        }
        return ok; 
    }



   //////////////// PostScript graphics //////////////////////

    private static boolean listPS(boolean bPortrait, 
    ArrayList<XYZO> a, ArrayList<XYZO> b, ArrayList<XYZO> c, File f)
    {
        if ((a==null) || (f==null))
          return false; 
        prevFontCode = 0; 
        PrintWriter pw; 
        try 
          { pw = new PrintWriter(new FileWriter(f), true);}
        catch (IOException ioe) 
          { return false; }
        boolean bbkg = a.get(0).getO() == SETBLACKBKG; 

        initPS(pw, bPortrait, bbkg);
        writePSList(a, pw); 
        writePSList(b, pw);
        writePSList(c, pw); 
        pw.println(" showpage"); 
        pw.close(); 
        return true; 
    }

    private static void writePSList(ArrayList<XYZO> gAL, PrintWriter gPW)
    {
        int prevfontcode = 0; 
        if ((gAL == null) || (gPW == null))
          return; 
        for (int i=0; i<gAL.size(); i++)
        {
            Object o = gAL.get(i); 
            double x = ((XYZO) o).getX(); 
            double y = ((XYZO) o).getY(); 
            double z = ((XYZO) o).getZ(); 
            int op   = ((XYZO) o).getO();   // contains font info  
            writePSquad(gPW, x, y, z, op); 
        }
    }

    private static void initPS(PrintWriter pw, boolean bPort, boolean bBkg)
    {
        String sXL = " 0.0"; 
        String sYB = " 0.0";    //  100.0? 
        String sXR = " 800.0";  //  600.0?
        String sYT = " 999.0";  // 700.0?

        pw.println("%!PS-Adobe-2.0"); 
        pw.println("%% BoundingBox: "+sXL+sYB+sXR+sYT);
        pw.println("%% Pages: 1"); 
        pw.println("%% Creator: BEAM FOUR Java Edition (c) Stellar Software 2010"); 
        pw.println("/Courier findfont 10 scalefont setfont");  // note slash.
        pw.println("/diam {2} def"); 
        pw.println("/rad { 0.5 diam mul } def"); 
        pw.println("/mdiam { -1 diam mul } def"); 
        pw.println("/mrad { -0.5 diam mul } def"); 
        pw.println("/dot { moveto 0 1 rlineto stroke } def"); 
        pw.println("/plus { moveto 0 rad rmoveto"); 
        pw.println("  0 mdiam rlineto mrad rad rmoveto");
        pw.println("  diam 0 rlineto stroke } def");
        pw.println("/box { moveto mrad mrad rmoveto");
        pw.println("  diam 0 rlineto 0 diam rlineto");
        pw.println("  mdiam 0 rlineto closepath fill } def"); 
        pw.println("/diamond { moveto mrad 0 rmoveto");
        pw.println("  rad rad rlineto rad mrad rlineto");
        pw.println("  mrad mrad rlineto closepath fill } def"); 
        pw.println("/blk { 0.0 0.0 0.0 setrgbcolor } def"); 
        pw.println("/red { 1.0 0.0 0.0 setrgbcolor } def"); 
        pw.println("/grn { 0.0 1.0 0.0 setrgbcolor } def"); 
        pw.println("/yel { 0.6 0.6 0.0 setrgbcolor } def"); 
        pw.println("/blu { 0.0 0.0 1.0 setrgbcolor } def"); 
        pw.println("/mag { 0.8 0.0 0.8 setrgbcolor } def"); 
        pw.println("/cya { 0.0 0.6 0.6 setrgbcolor } def"); 
        pw.println("/wht { 1.0 1.0 1.0 setrgbcolor } def"); 
        pw.println("/ltg { 0.8 setgray } def"); 
        pw.println("/dkg { 0.6 setgray } def");
        pw.println("/dotted { [4] 0 setdash } def"); 
        pw.println("/solid { [] 0 setdash } def"); 
        pw.println("/cf { closepath fill } def"); 
        pw.println("/cfls { closepath gsave 0.8 setgray fill");
        pw.println("  grestore 0 setgray stroke } def"); 
        pw.println("/chfs { closepath gsave 0.6 setgray fill"); 
        pw.println("  grestore 0 setgray stroke } def"); 
        pw.println(" 1.0 setlinewidth"); 
        pw.println("0 setgray"); 
        pw.println("blk"); 
        pw.println("solid"); 
        pw.println("1 setlinecap");   // round cap
        pw.println("1 setlinejoin");  // round join

        // do black background, if wanted
        if (bBkg)
        {
            pw.println("0 0 0 setrgbcolor"); 
            pw.println(sXL+sYB+" moveto"); 
            pw.println(sXL+sYT+" lineto"); 
            pw.println(sXR+sYT+" lineto"); 
            pw.println(sXR+sYB+" lineto"); 
            pw.println("closepath fill"); 
            pw.println("1 1 1 setrgbcolor"); 
        }

        // center the figure on the page:
        pw.println("320 400 translate"); 

        // scale the figure from pixels to points:
        pw.println("1.0 1.0 scale"); 

        // set Portrait vs Landscape:
        if (bPort)
          pw.println("0 rotate"); 
        else
          pw.println("90 rotate"); 
    }

    private static void writePSquad(PrintWriter pw, 
    double x, double y, double z, int opfull)
    {
        if ((pw==null) || (opfull<1))
          return; 

        String sx = U.fwd(x, 9, 2)+" "; 
        String sy = U.fwd(y, 9, 2)+" "; 
        String sz = U.fwd(z, 9, 2)+" "; 

        int op = opfull % 1000; 
        int fc = opfull / 1000; 

        switch (op)
        {
            case SETSOLIDLINE: 
              pw.println(" solid"); 
              x = Math.max(0.0, Math.min(5.0, x));
              pw.println(sx + " setlinewidth"); 
              break;

            case SETDOTTEDLINE:   
              pw.println(" dotted");
              x = Math.max(0.0, Math.min(5.0, x)); 
              pw.println(sx + " setlinewidth"); 
              break;               

            case SETRGB:
              pw.println(sx + sy + sz + " setrgbcolor"); // range 0...1
              break; 
              
            case MOVETO:       pw.println(sx + sy + " moveto"); break;
            case PATHTO:       pw.println(sx + sy + " lineto"); break;
            case STROKE:       pw.println(sx + sy + " lineto stroke"); break; 
            case FILL:         pw.println(sx + sy + " lineto closepath fill"); break; 
            case COMMENTSHADE: pw.println("% surface shading"); break;
            case COMMENTSURF:  pw.println("% surface arcs"); break;
            case COMMENTRAY:   pw.println("% ray"); break;
            case COMMENTAXIS:  pw.println("% axis"); break;
            case COMMENTRULER: pw.println("% ruler"); break;
            case COMMENTDATA:  pw.println("% data"); break;
            case COMMENTANNO:  pw.println("% annotation"); break;

            default:
              if ((op>31) && (op<127))
              {
                  if (fc != prevFontCode)  // new font
                  {
                      if (fc % 2 == 1)
                        pw.print("/Courier-Bold findfont "); // note slash.
                      else
                        pw.print("/Courier findfont ");      // note slash.
                      int fs = fc/10; 
                      pw.println(fs+" scalefont setfont");
                      prevFontCode = fc; 
                  }
                  int dx = -prevFontCode/30;
                  int dy = -prevFontCode/40 -1; 
                  String tx = U.fwd(x+dx, 9, 2); 
                  String ty = U.fwd(y+dy, 9, 2); 
                  pw.println(tx + ty + " moveto (" + (char)op + ") show"); 
              }
              else if ((op>=DOT) && (op<=SETCOLOR+DKGRAY))
              {
                  int colorcode = op % 10; 
                  switch (colorcode)
                  {
                      case BLACK:   pw.println(" blk"); break; 
                      case RED:     pw.println(" red"); break; 
                      case GREEN:   pw.println(" grn"); break; 
                      case YELLOW:  pw.println(" yel"); break; 
                      case BLUE:    pw.println(" blu"); break;
                      case MAGENTA: pw.println(" mag"); break; 
                      case CYAN:    pw.println(" cya"); break; 
                      case WHITE:   pw.println(" wht"); break; 
                      case LTGRAY:  pw.println(" ltg"); break; 
                      case DKGRAY:  pw.println(" dkg"); break; 
                  }
                  int dotcode = op - colorcode; 
                  switch (dotcode)
                  {
                      case DOT:     pw.println(sx + sy + " dot"); break; 
                      case PLUS:    pw.println(sx + sy + " plus"); break; 
                      case SQUARE:  pw.println(sx + sy + " box"); break; 
                      case DIAMOND: pw.println(sx + sy + " diamond"); break; 
                      // case SETCOLOR has been taken care above. 
                  }
              }
              break; 
        }
    }


    ///////////////// HPGL plotter graphics ////////////////////////////////

    private static double plotsizeinches = 6.0;
    private static double xoff=0.0, yoff=0.0; 

    private static boolean listPLOT(boolean bPortrait, int plotsize, 
       ArrayList<XYZO> a, ArrayList<XYZO> b, ArrayList<XYZO> c, File f)
    {
        if ((a==null) || (f==null) || (plotsize<0) || (plotsize>4))
          return false; 

        PrintWriter pw; 
        try 
          { pw = new PrintWriter(new FileWriter(f), true);}
        catch (IOException ioe) 
          { return false; }
        
        plotsizeinches = getInches(plotsize);

        pw.println(sInitPlot[2*plotsize + (bPortrait ? 1 : 0)]);

        writePlotList(a, pw); 
        writePlotList(b, pw);
        writePlotList(c, pw); 
        pw.println("PU;"); 
        pw.close(); 
        return true; 
    }


    private static void writePlotList(ArrayList<XYZO> gAL, PrintWriter gPW)
    {
        if ((gAL == null) || (gPW == null))
          return; 
        for (int i=0; i<gAL.size(); i++)
        {
            XYZO o = gAL.get(i); 
            double xraw = o.getX(); 
            double yraw = o.getY(); 
            int   opraw = o.getO();  
            writePlotQuad(gPW, xraw, yraw, opraw); 
        }
    }


    private static double getInches(int i) 
    // artwork size A=0, B=1, C=2, D=3, E=4
    { 
        switch (i)
        {
            case 0: return 6.0; 
            case 1: return 8.0; 
            case 2: return 14.0; 
            case 3: return 18.0; 
            case 4: return 28.0;
        }
        return 6.0;
    }


    private static String sInitPlot[] =
    // IN; means initialize plotter.
    // SI0.35,0.5; means char size = 0.35cm wide, 0.5cm height.
    // RO90 means portrait; RO0 means landscape, origin always lower left.
    // IW; with no parms sets input window = paper size.
    // IPax,ay,bx,by; sets the diagonal of the scaling points.
    // SCxmin,xmax,ymin,ymax; sets bounds of user space, e.g. -250 to +250
    // SC terminator sets input units identical to plotter units.
    // PU; = pen up;  PD; = pen down; PAx,y;= position absolute; PRx,y;=relative
    {
      "IN;SI0.25,0.4;SP1;RO90;IW;IP1000,2000,7000,8000;SC-250,250,-250,250;",  //APORT
      "IN;SI0.25,0.4;SP1;RO0;IW;IP2000,1000,8000,7000;SC-250,250,-250,250;",   //ALAND
      "IN;SI0.35,0.6;SP1;RO90;IW;IP1000,4000,9000,12000;SC-250,250,-250,250;", //BPORT
      "IN;SI0.35,0.6;SP1;RO0;IW;IP4000,1000,12000,9000;SC-250,250,-250,250;",  //BLAND
      "IN;SI0.7,0.8;SP1;RO90;IW;IP1000,3000,15000,17000;SC-250,250,-250,250;", //CPORT
      "IN;SI0.7,0.8;SP1;RO0;IW;IP3000,1000,17000,15000;SC-250,250,-250,250;",  //CLAND
      "IN;SI1,1.2;SP1;RO90;IW;IP1000,7000,19000,25000;SC-250,250,-250,250;",   //DPORT
      "IN;SI1,1.2;SP1;RO0;IW;IP7000,1000,25000,19000;SC-250,250,-250,250;",    //DLAND
      "IN;SI1.2,2;SP1;RO90;IW;IP2000,6000,30000,34000;SC-250,250,-250,250;",   //EPORT
      "IN;SI1.2,2;SP1;RO0;IW;IP6000,2000,34000,30000;SC-250,250,-250,250;"     //ELAND
    };

    private static void writePlotQuad(PrintWriter pw, double xraw, 
    double yraw, int opcode)
    {
        final char ETX = (char)3;  // Plotter string terminator

        if ((pw==null) || (opcode<1))
          return; 
        int op = opcode % 1000;

        double x = xraw;  // use native CenterOriginPoints
        double y = yraw;  // use native CenterOriginPoints

        String sx = U.fwd(x,12,2).trim() + ','; 
        String sy = U.fwd(y,12,2).trim() + ';';

        switch (op)
        {
            case SETSOLIDLINE:  pw.println("LT;"); break; 
            case SETDOTTEDLINE: pw.println("LT5.0,1.0;"); break; 
            case MOVETO:        pw.println("PUPA" + sx + sy); break;
            case PATHTO:        pw.println("PDPA" + sx + sy); break;
            case STROKE:        pw.println("PDPA" + sx + sy + "PU;"); break; 
            case FILL:          pw.println("PDPA" + sx + sy + "PU;"); break; 
            case COMMENTSHADE:  break;
            case COMMENTSURF:   break;
            case COMMENTRAY:    break;
            case COMMENTAXIS:   break;
            case COMMENTRULER:  break;
            case COMMENTDATA:   break;
            case COMMENTANNO:   break;

            default:
              if ((op>31) && (op<127))
              {
                  int fc = opcode / 1000;      
                  if (fc != prevFontCode)
                  {
                      prevFontCode = fc;
                      int fontsize = fc/10;   // points
                      double winsize= 500.0;  // points

                      // set character size and offsets...

                      double height_cm = 2.54*plotsizeinches*fontsize/winsize;
                      double width_cm = 0.6 * height_cm;
                      String sw = U.fwd(width_cm,8,2).trim() + ',';
                      String sh = U.fwd(height_cm,8,2).trim() + "; ";
                      pw.println("SI" + sw + sh); 
                      xoff = -0.3*fontsize;   // points
                      yoff = -0.3*fontsize;   // points 
                  }
                  String tx = U.fwd(x+xoff,9,2).trim() + ','; 
                  String ty = U.fwd(y+yoff,9,2).trim() + ';';
                  pw.println("PUPA" + tx + ty + "LB" + (char)op + ETX); 
              }
              else if ((op>=DOT) && (op<=SETCOLOR+DKGRAY))
              {
                  int colorcode = op % 10; 
                  switch (colorcode)
                  {
                      case RED:     pw.println("SP3;"); break; 
                      case GREEN:   pw.println("SP4;"); break; 
                      case YELLOW:  pw.println("SP6;"); break; 
                      case BLUE:    pw.println("SP2;"); break;
                      case MAGENTA: pw.println("SP5;"); break; 
                      case CYAN:    pw.println("SP7;"); break; 
                      default:      pw.println("SP1;"); break;
                  }
                  int dotcode = op - colorcode; 
                  switch (dotcode)
                  {
                      case DOT:     
                         pw.println("PUPA" + sx + sy + "PD;PR0,1;PU;"); 
                         break; 
                      case PLUS:    
                         pw.println("PUPA" + sx + sy
                           + "PR0,1;PD0,-2,0,1,-1,0,2,0;PU;"); 
                         break; 
                      case SQUARE:  
                         pw.println("PUPA" + sx + sy 
                           + "PR-1,-1;PD0,2,2,0,0,-2,-2,0;PU;"); 
                         break; 
                      case DIAMOND: 
                         pw.println("PUPA" + sx + sy
                           + "PR 0,-1;PD-1,1,1,1,1,-1,-1,-1;PU;"); 
                         break; 
                      // case SETCOLOR has been taken care above. 
                  }
              }
              break; // end default case
        }  // end switch()
    } // end writePlotQuad()




    ////// DXF GRAPHICS //// scaling: inches or user units ///////
    ////// chars scaling mystery; so are not shown ///////////////

    private static double dxfxc, dxfxs, dxfyc, dxfys, dxfzc, dxfzs;

    private static boolean listDXF(int n, ArrayList<XYZO> a, 
       ArrayList<XYZO> b,  ArrayList<XYZO> c, File f)
    {
        if ((a==null) || (f==null))
          return false; 
        PrintWriter pw; 
        try 
          { pw = new PrintWriter(new FileWriter(f), true);}
        catch (IOException ioe) 
          { return false; }

        boolean b3D = (n==3); 
        final int fontsize = 10; 

        // default: change CenterOriginPixels to CornerOriginInches
        // however these defaults are replaced by UserAffine if the
        // USERCONST and USERSLOPE appear early in the quadlist. 

        dxfxc = dxfyc = +4.00;  // inches from corner
        dxfzc = 0.0; 
        dxfxs = dxfys = dxfzs = 1.0/72.0; // points to inches
 
        initDXF(pw); 
        writeDXF(a, b3D, fontsize, pw); 
        writeDXF(b, b3D, fontsize, pw);
        writeDXF(c, b3D, fontsize, pw); 
        endDXF(pw); 

        pw.close(); 
        return true; 
    } // end listDXF()


    private static void writeDXF(ArrayList<XYZO> gAL, boolean b3D,
    int fontsize, PrintWriter pw)
    {
        final int SURFLAYER = 1; 
        final int RAYLAYER = 2; 
        final int RULERLAYER = 3; 
        final int xoffset = -fontsize; 
        final int yoffset = -fontsize/2; 

        if ((gAL == null) || (pw == null))
          return; 

        int iLayer = SURFLAYER; // to start with...

        for (int i=0; i<gAL.size(); i++)
        {
            XYZO o = gAL.get(i); 
            double xraw = o.getX(); 
            double yraw = o.getY(); 
            double zraw = o.getZ(); 
            int   opraw = o.getO();  

            double x = dxfxc + xraw * dxfxs; 
            double y = dxfyc + yraw * dxfys;
            double z = dxfzc + zraw * dxfzs;
            int op = opraw % 1000; // ignore char modifiers

            switch(op)
            {
                case USERCONSTS:  // allows output in user units
                  dxfxc = xraw; 
                  dxfyc = yraw; 
                  dxfzc = zraw; 
                  break; 

                case USERSLOPES:  // allows output in user units
                  dxfxs = xraw; 
                  dxfys = yraw; 
                  dxfzs = zraw; 
                  break; 
                case SETSOLIDLINE:   break; 
                case SETDOTTEDLINE:  break; 
                case MOVETO:    
                  pw.println("0");
                  pw.println("POLYLINE"); 
                  pw.println("8"); 
                  pw.println(""+iLayer); 
                  pw.println("66"); 
                  pw.println("1"); 
                  if (b3D)  // 3DDXF
                  {
                      pw.println("70"); 
                      pw.println("8"); 
                  }
                  pw.println("0"); 
                  pw.println("VERTEX"); 
                  pw.println("8"); 
                  pw.println(""+iLayer); 
                  pw.println("10"); 
                  pw.println(dxfnum(x)); 
                  pw.println("20"); 
                  pw.println(dxfnum(y)); 
                  if (b3D)
                  {
                      pw.println("30"); 
                      pw.println(dxfnum(z)); 
                      pw.println("70"); 
                      pw.println("32"); 
                  }
                  break;

                case PATHTO:
                case STROKE:
                case FILL: 
                  pw.println("0"); 
                  pw.println("VERTEX"); 
                  pw.println("8"); 
                  pw.println(""+iLayer); 
                  pw.println("10"); 
                  pw.println(dxfnum(x)); 
                  pw.println("20"); 
                  pw.println(dxfnum(y)); 
                  if (b3D)
                  {
                      pw.println("30"); 
                      pw.println(dxfnum(z)); 
                      pw.println("70"); 
                      pw.println("32"); 
                  }
                  if (op != PATHTO)  // done
                  {
                      pw.println("0"); 
                      pw.println("SEQEND"); 
                  }
                  break;

                case COMMENTSHADE: 
                case COMMENTSURF:  iLayer = SURFLAYER; break;
                case COMMENTRAY:   iLayer = RAYLAYER; break;
                case COMMENTAXIS:  
                case COMMENTRULER: iLayer = RULERLAYER; break;
                case COMMENTDATA:  break;
                case COMMENTANNO:  break;

                default:
                  if ((op>31) && (op<127))
                  {
                      /*****char scaling & size issue ********
                      iLayer = RULERLAYER;
                      pw.println("0"); 
                      pw.println("TEXT"); 
                      pw.println("8"); 
                      pw.println(""+iLayer); 
                      pw.println("10"); 
                      pw.println(dxfnum(x+xoffset)); 
                      pw.println("20"); 
                      pw.println(dxfnum(y+yoffset)); 
                      if (b3D)
                      {
                          pw.println("30"); 
                          pw.println("0.0"); // z=0
                          pw.println("70"); 
                          pw.println("32"); 
                      }
                      pw.println("40"); 
                      pw.println(dxfnum(xoffset)); 
                      pw.println("1"); 
                      pw.println(""+(char)op); 
                      *************************************/
                  }
                  else if ((op>=DOT) && (op<=SETCOLOR+DKGRAY))
                  {
                      int colorcode = op % 10; 
                      switch (colorcode)
                      {
                          case BLACK:   break; 
                          case RED:     break; 
                          case GREEN:   break; 
                          case YELLOW:  break; 
                          case BLUE:    break;
                          case MAGENTA: break; 
                          case CYAN:    break; 
                          case WHITE:   break; 
                          case LTGRAY:  break; 
                          case DKGRAY:  break; 
                      }
                      int dotcode = op - colorcode; 
                      switch (dotcode)
                      {
                        case DOT:     
                        case PLUS:    
                        case SQUARE:  
                        case DIAMOND: 
                          pw.println("0"); 
                          pw.println("POINT"); 
                          pw.println("8"); 
                          pw.println(""+iLayer); 
                          pw.println("10"); 
                          pw.println(dxfnum(x)); 
                          pw.println("20"); 
                          pw.println(dxfnum(y)); 
                          if (b3D)
                          {
                              pw.println("30"); 
                              pw.println("0.0"); //z=0
                              pw.println("70"); 
                              pw.println("32"); 
                          }
                      }
                  }
                  break; 
            } // end switch()
        } // end of ArrayList
    } // end of writeDXF()

    private static void initDXF(PrintWriter gPW)
    {
        gPW.println("999");
        gPW.println("ENTITIES FROM BEAM FOUR JAVA EDITION (C) 2004 STELLAR SOFTWARE");  
        gPW.println("0");
        gPW.println("SECTION"); 
        gPW.println("2"); 
        gPW.println("ENTITIES"); 
    }

    private static void endDXF(PrintWriter gPW)
    {
        gPW.println("0"); 
        gPW.println("ENDSEC"); 
        gPW.println("0"); 
        gPW.println("EOF"); 
    }

    private static String dxfnum(double x)
    {
        return U.fwd(x, 20, 9).trim();
    }




    ///////////////// Quad list /////////////////////////

    private static boolean listQuads(ArrayList<XYZO> a, ArrayList<XYZO> b, 
        ArrayList<XYZO> c, File f)
    // lists the XYZO quads
    {
        if ((a==null) || (f==null))
          return false; 
        PrintWriter pw; 
        try 
          { pw = new PrintWriter(new FileWriter(f), true);}
        catch (IOException ioe) 
          { return false; }
        writeQuadList(a, pw); 
        writeQuadList(b, pw); 
        writeQuadList(c, pw); 
        pw.close(); 
        return true; 
    }

    private static void writeQuadList(ArrayList<XYZO> gAL, PrintWriter gPW)
    {
        if ((gAL == null) || (gPW == null))
          return; 
        for (int i=0; i<gAL.size(); i++)
        {
            XYZO o = gAL.get(i); 
            double x = o.getX(); 
            double y = o.getY(); 
            double z = o.getZ();
            int op   = o.getO();  
            String s = U.fwi(i,6) 
                     + U.fwd(x,20,8) 
                     + U.fwd(y,20,8) 
                     + U.fwd(z,20,8) 
                     + U.fwi(op, 8);  
            gPW.println(s + interpretQuad(op % 1000)); 
        }
    }

    static String interpretQuad(int k)
    {
        String t="", u=""; 
        if ((k>=32) && (k<127))
          t = " "+(char) k; 
        else if ((k>=DOT) && (k<=SETCOLOR+DKGRAY))
        {
            int icolor = k % 10; 
            int imark = k - icolor; 
            switch (imark)
            {
                case DOT:      t = " DOT"; break; 
                case PLUS:     t = " PLUS"; break; 
                case SQUARE:   t = " SQUARE"; break; 
                case DIAMOND:  t = " DIAMOND"; break; 
                case SETCOLOR: t = " SETCOLOR"; break; 
            }
            switch (icolor)
            {
                case BLACK:    u = " BLACK"; break; 
                case RED:      u = " RED"; break; 
                case GREEN:    u = " GREEN"; break; 
                case YELLOW:   u = " YELLOW"; break; 
                case BLUE:     u = " BLUE"; break; 
                case MAGENTA:  u = " MAGENTA"; break; 
                case CYAN:     u = " CYAN"; break; 
                case WHITE:    u = " WHITE"; break; 
                case LTGRAY:   u = " LTGRAY"; break; 
                case DKGRAY:   u = " DKGRAY"; break; 
            }
        }
        else switch (k)
        {
           case NULL:          t = " NULL"; break;  
           case SETWHITEBKG:   t = " SETWHITEBKG"; break;
           case SETBLACKBKG:   t = " SETBLACKBKG"; break; 
           case SETSOLIDLINE:  t = " SETSOLIDLINE"; break; 
           case SETDOTTEDLINE: t = " SETDOTTEDLINE"; break; 
           case SETRGB:        t = " SETRGB"; break; 
           case MOVETO:        t = " MOVETO"; break; 
           case PATHTO:        t = " PATHTO"; break; 
           case STROKE:        t = " STROKE"; break; 
           case FILL:          t = " FILL"; break; 
           case SETFONT:       t = " SETFONT"; break; 
           case USERCONSTS:    t = " USERCONSTS"; break; 
           case USERSLOPES:    t = " USERSLOPES"; break; 
           case COMMENTSHADE:  t = " COMMENTSHADE"; break; 
           case COMMENTSURF:   t = " COMMENTSURF"; break; 
           case COMMENTRAY:    t = " COMMENTRAY"; break; 
           case COMMENTAXIS:   t = " COMMENTAXIS"; break; 
           case COMMENTRULER:  t = " COMMENTRULER"; break; 
           case COMMENTDATA:   t = " COMMENTDATA"; break; 
           case COMMENTANNO:   t = " COMMENTANNO"; break; 
        }
        return t + u; 
    }
}
