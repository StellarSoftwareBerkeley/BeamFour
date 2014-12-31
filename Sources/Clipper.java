package com.stellarsoftware.beam;

/**
  *  Performs double precision clipping of line segments
  *  (c) 2005 M.Lampton STELLAR SOFTWARE
  *
  *  Has no UI, imports nothing. 
  */
class Clipper
{
    private double xLeft, yBot, xRight, yTop; 

    public Clipper(double x1, double y1, double x2, double y2)
    // This constructor sets up edges of clipping rectangle.
    {
        xLeft = Math.min(x1, x2); 
        xRight = Math.max(x1, x2); 
        yBot = Math.min(y1, y2); 
        yTop = Math.max(y1, y2); 
    }

    public boolean clip(double vec[]) 
    // Clips a line segment "vec" with the preset rectangle.
    // vec is {x1,y1,x2,y2}
    // returns true if visible, else false. 
    {
        int tt1 = tictactoe(vec[0], vec[1]); 
        int tt2 = tictactoe(vec[2], vec[3]); 

        if ((tt1==4) && (tt2==4))      // case "within"
          return true;  

        int row1 = tt1 / 3; 
        int row2 = tt2 / 3; 
        if ((row1==0) && (row2==0))    // case "outside"
          return false; 
        if ((row1==2) && (row2==2))    // case "outside" 
          return false;

        int col1 = tt1 % 3; 
        int col2 = tt2 % 3; 
        if ((col1==0) && (col2==0))    // case "outside"
          return false; 
        if ((col1==2) && (col2==2))    // case "outside"
          return false; 

        // the remaining cases involve mathematical clipping...
        return mathclip(vec); 
    }

    private boolean mathclip(double v[])
    // Called by clip() which eliminates the easy cases first. 
    // WARNING requires yBot<yTop, xLeft<xRight.
    // Four edge crossing tests: hitLeft, hitRight, hitTop, hitBot.
    // Performs all clips indicated, returns visibility.
    {
        double u[] = new double[4]; 
        double x=0.0, y=0.0; 
        boolean bVisible = false; 

        // test the left border...
        u[0] = u[2] = xLeft;  u[1] = yBot; u[3] = yTop; 
        if( straddle(u,v))
        {
            y = v[1] + (xLeft-v[0])*(v[3]-v[1])/(v[2]-v[0]);
            if (v[0]<xLeft)
            {
                v[0] = xLeft; 
                v[1] = y; 
            }
            else 
            {
                v[2] = xLeft; 
                v[3] = y; 
            }
            bVisible = true;
        }
            
        // test right border...
        u[0] = u[2] = xRight;   
        if ( straddle(u,v))
        {
            y = v[1] + (xRight-v[0])*(v[3]-v[1])/(v[2]-v[0]);
            if (v[0]>xRight)
            {
                v[0] = xRight; 
                v[1] = y; 
            }
            else 
            {
                v[2] = xRight; 
                v[3] = y; 
            }
            bVisible = true;
        }

        // test bottom border...
        u[0] = xLeft; u[2] = xRight; u[1] = u[3] = yBot; 
        if ( straddle(u,v))
        {
            x = v[0] + (yBot-v[1])*(v[2]-v[0])/(v[3]-v[1]);
            if (v[1]<yBot)
            {
                v[0] = x; 
                v[1] = yBot; 
            }
            else 
            {
                v[2] = x; 
                v[3] = yBot; 
            }
            bVisible = true;
        }

        // test top border...
        u[1] = u[3] = yTop;                            
        if ( straddle(u,v))
        {
            x = v[0] + (yTop-v[1])*(v[2]-v[0])/(v[3]-v[1]);
            if (v[1]>yTop)
            {
                v[0] = x; 
                v[1] = yTop; 
            }
            else 
            {
                v[2] = x; 
                v[3] = yTop; 
            }
            bVisible = true;
        }
        return bVisible;
    }

    private boolean straddle(double u[], double v[]) // serves mathclip()
    {
        double dxu = u[2] - u[0]; 
        double dyu = u[3] - u[1]; 
        double dxv = v[2] - v[0]; 
        double dyv = v[3] - v[1]; 
        double dx1 = v[0] - u[0]; 
        double dy1 = v[1] - u[1]; 
        double dx2 = v[2] - u[2]; 
        double dy2 = v[3] - u[3]; 
        return ((dxu*dy1 - dyu*dx1)*(dxu*dy2 - dyu*dx2) < 0.0)
            && ((dxv*dy1 - dyv*dx1)*(dxv*dy2 - dyv*dx2) < 0.0);
    }

    private int tictactoe(double x, double y) // serves clip()
    // returns 0..8; 4=center.  Requires yBot<yTop.
    {
        int i = (x<xLeft) ? 0 : (x<xRight) ? 1 : 2; 
        int j = (y<yBot) ? 0 : (y<yTop) ? 3 : 6; 
        return i+j; 
    }
}