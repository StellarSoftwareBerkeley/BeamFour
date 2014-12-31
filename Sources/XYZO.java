package com.stellarsoftware.beam;

/** XYZO objects are elements of a QuadList graphic.
  *
  * Native quadlist system is CenterOriginPoints, +X=right, +Y=up, +Z=out.
  * Artwork quads have -250.0<x,y<250.0;  z is unlimited but usually zero.
  * Pixels are the same, except origin = ULCorner and +Y=down. 
  * Conversion to pixels uses local getIXPIX(x), getIYPIX(y).
  *
  * A better name for this class would be HVDO because these coordinates
  * are not in user XYZ space, but rather in display HVD screen space.
  *
  * To get user coords from the artwork, the quadlist includes
  * userconsts x,y,z = offsets for that quadlist view;
  * userslopes x,y,z = magnifications for that quadlist view;
  * Therefore...
  * UserValue x,y,z = userConst + userSlope * quadListValue.
  * Careful! this back-conversion does not undo viewing angles!
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
***/
public class XYZO
{
    private double px, py, pz;  // drawing point coordinates
    private int op;             // drawing opcode or char

    public XYZO(double gx, double gy, double gz, int gi)
      { px=gx; py=gy; pz=gz; op=gi; }

    public XYZO(int gx, int gy, int gi)
      { px=gx; py=gy; pz=0.0; op = gi; }

    public XYZO(int gx, int gy, char gc)
      { px=gx; py=gy; pz=0.0; op=(int)gc; }

    public XYZO(int gi)
      { px=0.0; py=0.0; pz=0.0; op=gi; }

    public double getX()
      { return px; }

    public double getY()
      { return py; }

    public double getZ()
      { return pz; }

    public int getI()
      { return (int) px; }

    public int getJ()
      { return (int) py; }

    public int getO()
      { return op; }

    public char getC()
      { return (char) op; }
}
