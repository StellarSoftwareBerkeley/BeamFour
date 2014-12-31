package com.stellarsoftware.beam;

import javax.swing.*;      // Graphics2D features

@SuppressWarnings("serial")

/**
  * Custom artwork class furnishes artwork to GPanel.
  * Test article demonstrates Zsorting.
  * Fontcode accompanies each char, and use CenterOrigin for fontXY.
  * Internal coord frame is unit cube, center origin, +y=up.
  * Output coord frame is dUOpixels cube, center origin, +y=up.
  *
  * Implements Thread to force slow rotations.
  * But: the rotations fail to show without stirring. 
  *
  * @author M.Lampton (c) STELLAR SOFTWARE 2004 all rights reserved.
*/
public class DemoPanel extends GPanel // implements Runnable
{
    // public static final long serialVersionUID = 42L;

    final double EXTRAROOM = 3.0;  
    private double az=0, cosaz=1, sinaz=0; 
    private double el=0, cosel=1, sinel=0; 
    int jjj[] = new int[6]; 
    double zzz[] = new double[6]; 

    public DemoPanel(GJIF gj) // constructor
    {
        myGJIF = gj;          // protected; used here & GPanel
        uxcenter = 0.0;       // needed for GPanel's addAffines()
        uxspan = EXTRAROOM;   // needed for GPanel's addAffines()
        uycenter = 0.0;       // needed for GPanel's addAffines()
        uyspan = EXTRAROOM;   // needed for GPanel's addAffines()
        // uzcenter = 0.0; 
        // uzspan = EXTRAROOM; 

        az = +22; 
        cosaz = U.cosd(az); 
        sinaz = U.sind(az);
        el = +33;
        cosel = U.cosd(el); 
        sinel = U.sind(el);
        
        /*******Thread*****
        count = 0;                  
        myThread = new Thread(this); 
        myThread.start();  
        *******************/
    }

    /*******Thread*******
    
    public void doMathAndRepaint()
    {
        if (count < MAXTHREAD)
        {
            count += 1;
            az += 1; 
            if (count < MAXTHREAD)
              myGJIF.setTitle("Thread: "+count); 
            else
              myGJIF.setTitle("Demo Panel"); 
        }
        else
           myThread = null; 
        repaint(); 
    }
    
    private Thread myThread = null;
    private int MAXTHREAD = 100; 
    private int count = 0; 
    
    public void run()
    {
        try
        {
            while (count < MAXTHREAD)
            {
                doMathAndRepaint(); 
                Thread.sleep(100); 
            }
        }
        catch (InterruptedException e)
        {
        }
    }
    *********************/        
            
            
            
    //-----protected methods-----------
    
    protected void doTechList(boolean bFullArt)  // replaces abstract method
    // Called by GPanel when fresh artwork is needed:
    // new, pan, zoom, rotate.  
    // But this is never called simply for annotation mods.
    // For annotation, host's bitmap is blitted instead.
    {
        doZsort();  
        doArt();
    }

    protected void doRotate(int i, int j) // replaces abstract "do" method
    {
        double daz = i/3; 
        az += daz; 
        cosaz = U.cosd(az); 
        sinaz = U.sind(az);
        double del = j/3;
        el += del;
        cosel = U.cosd(el); 
        sinel = U.sind(el); 
    }

    protected boolean doRandomRay() // replaces abstract "do" method
    {
        return false;
    } 


    protected void doFinishArt() // replaces abstract "do" method
    {
        return; 
    }


    protected void doCursor(int ix, int iy)  // replaces abstract method
    // delivers current cursor coordinates
    {
        return; 
    }
    
    protected double getStereo()    // replaces abstract "get" method
    {
        return 0.0; 
    }

       
    protected void doSaveData()     // replaces abstract "do" method
    {
        return; 
    } 


//------------private methods--------

    private void doZsort()
    {
        double xyz[] = new double[3]; 

        jjj[0] = 0; // blue
        xyz[0] = 0.0; xyz[1] = 0.0; xyz[2] = -0.5; 
        viewelaz(xyz); 
        zzz[0] = xyz[2]; 

        jjj[1] = 1; // red
        xyz[0] = 0.0; xyz[1] = -0.5; xyz[2] = 0.0; 
        viewelaz(xyz); 
        zzz[1] = xyz[2]; 

        jjj[2] = 2; // green
        xyz[0] = -0.5; xyz[1] = 0.0; xyz[2] = 0.0; 
        viewelaz(xyz); 
        zzz[2] = xyz[2]; 

        jjj[3] = 3; // magenta
        xyz[0] = 0.0; xyz[1] = 0.0; xyz[2] = +0.5; 
        viewelaz(xyz); 
        zzz[3] = xyz[2]; 

        jjj[4] = 4; // cyan
        xyz[0] = 0.0; xyz[1] = +0.5; xyz[2] = 0.0; 
        viewelaz(xyz); 
        zzz[4] = xyz[2]; 

        jjj[5] = 5; // yellow
        xyz[0] = +0.5; xyz[1] = 0.0; xyz[2] = 0.0; 
        viewelaz(xyz); 
        zzz[5] = xyz[2]; 

        ssort(zzz, jjj, 6); 
    }


    private void doArt()  
    {
        double xyz[] = new double[3]; 
        clearXYZO();       
        addXYZO(SETWHITEBKG);
        addXYZO(SETCOLOR + BLACK); 
        addAffines(); // a service of host GPanel
        for (int i=0; i<6; i++)
          drawJ(jjj[i]); 
    }


    void drawJ(int j)
    {
        double xyz[] = new double[3]; 
        switch(j)
        {
          case 0:  ///////// draw the floor z=-0.5 ////////
            addXYZO(SETCOLOR + BLUE); 
            xyz[0] = -0.5; xyz[1] = -0.5; xyz[2] = -0.5; 
            addViewedItem(xyz, MOVETO);
            xyz[0] = +0.5; xyz[1] = -0.5; xyz[2] = -0.5; 
            addViewedItem(xyz, PATHTO);
            xyz[0] = +0.5; xyz[1] = +0.5; xyz[2] = -0.5; 
            addViewedItem(xyz, PATHTO);
            xyz[0] = -0.5; xyz[1] = +0.5; xyz[2] = -0.5; 
            addViewedItem(xyz, PATHTO);
            xyz[0] = -0.5; xyz[1] = -0.5; xyz[2] = -0.5; 
            addViewedItem(xyz, FILL);
            addString(0, 0, -0.5, "BLUE"); 
            break; 

          case 1:  ///////// draw the frontwall y=-0.5 ////
            addXYZO(SETCOLOR + RED); 
            xyz[0] = -0.5; xyz[1] = -0.5; xyz[2] = -0.5; 
            addViewedItem(xyz, MOVETO);
            xyz[0] = -0.5; xyz[1] = -0.5; xyz[2] = +0.5; 
            addViewedItem(xyz, PATHTO);
            xyz[0] = +0.5; xyz[1] = -0.5; xyz[2] = +0.5; 
            addViewedItem(xyz, PATHTO);
            xyz[0] = +0.5; xyz[1] = -0.5; xyz[2] = -0.5; 
            addViewedItem(xyz, PATHTO);
            xyz[0] = -0.5; xyz[1] = -0.5; xyz[2] = -0.5; 
            addViewedItem(xyz, FILL);
            addString(0, -0.5, 0, "RED"); 
            break; 

          case 2:  ///////// draw the leftwall x=-0.5 ////
            addXYZO(SETCOLOR + GREEN); 
            xyz[0] = -0.5; xyz[1] = -0.5; xyz[2] = -0.5; 
            addViewedItem(xyz, MOVETO);
            xyz[0] = -0.5; xyz[1] = -0.5; xyz[2] = +0.5; 
            addViewedItem(xyz, PATHTO);
            xyz[0] = -0.5; xyz[1] = +0.5; xyz[2] = +0.5; 
            addViewedItem(xyz, PATHTO);
            xyz[0] = -0.5; xyz[1] = +0.5; xyz[2] = -0.5; 
            addViewedItem(xyz, PATHTO);
            xyz[0] = -0.5; xyz[1] = -0.5; xyz[2] = -0.5; 
            addViewedItem(xyz, FILL);
            addString(-0.5, 0, 0, "GREEN"); 
            break; 

          case 3:  ///////// draw the topwall z=+0.5 ///
            addXYZO(SETCOLOR + MAGENTA); 
            xyz[0] = -0.5; xyz[1] = -0.5; xyz[2] = +0.5; 
            addViewedItem(xyz, MOVETO);
            xyz[0] = -0.5; xyz[1] = +0.5; xyz[2] = +0.5; 
            addViewedItem(xyz, PATHTO);
            xyz[0] = +0.5; xyz[1] = +0.5; xyz[2] = +0.5; 
            addViewedItem(xyz, PATHTO);
            xyz[0] = +0.5; xyz[1] = -0.5; xyz[2] = +0.5; 
            addViewedItem(xyz, PATHTO);
            xyz[0] = -0.5; xyz[1] = -0.5; xyz[2] = +0.5; 
            addViewedItem(xyz, FILL);
            addString(0, 0, +0.5, "MAGENTA"); 
            break; 

          case 4: ///////// draw the backwall y=+0.5 /////
            addXYZO(SETCOLOR + CYAN); 
            xyz[0] = -0.5; xyz[1] = +0.5; xyz[2] = -0.5; 
            addViewedItem(xyz, MOVETO);
            xyz[0] = -0.5; xyz[1] = +0.5; xyz[2] = +0.5; 
            addViewedItem(xyz, PATHTO);
            xyz[0] = +0.5; xyz[1] = +0.5; xyz[2] = +0.5; 
            addViewedItem(xyz, PATHTO);
            xyz[0] = +0.5; xyz[1] = +0.5; xyz[2] = -0.5; 
            addViewedItem(xyz, PATHTO);
            xyz[0] = -0.5; xyz[1] = +0.5; xyz[2] = -0.5; 
            addViewedItem(xyz, FILL);
            addString(0, +0.5, 0, "CYAN"); 
            break; 

          case 5:  ///////// draw the rightwall x=+0.5 ///
            addXYZO(SETCOLOR + YELLOW); 
            xyz[0] = +0.5; xyz[1] = -0.5; xyz[2] = -0.5; 
            addViewedItem(xyz, MOVETO);
            xyz[0] = +0.5; xyz[1] = -0.5; xyz[2] = +0.5; 
            addViewedItem(xyz, PATHTO);
            xyz[0] = +0.5; xyz[1] = +0.5; xyz[2] = +0.5; 
            addViewedItem(xyz, PATHTO);
            xyz[0] = +0.5; xyz[1] = +0.5; xyz[2] = -0.5; 
            addViewedItem(xyz, PATHTO);
            xyz[0] = +0.5; xyz[1] = -0.5; xyz[2] = -0.5; 
            addViewedItem(xyz, FILL);
            addString(+0.5, 0, 0, "YELLOW"); 
            break; 

          default: break; 
       }
    }  // end of drawJ()


    void addViewedItem(double[] xyz, int op)
    {
        viewelaz(xyz); 
        addScaledItem(xyz, op); 
    }

    void addString(double x, double y, double z, String s)
    // Places a string at a desired center location [xyz].
    // CenterOrigin for chars. 
    {
        double xyz[] = {x, y, z}; 
        viewelaz(xyz); // converts xyz[] to screen frame.

        int iFontcode = getUOGraphicsFontCode();  
        int iHpoints = iFontcode / 10000;     
        int iWpoints = 1 + iHpoints / 2;   
        double scaledW = iWpoints * uxspan / dUOpixels; 
        double scaledH = iHpoints * uyspan / dUOpixels; 
        double xtick = 0.5 * iWpoints * uxspan / dUOpixels; 
        double ytick = 0.5 * iWpoints * uyspan / dUOpixels;  

        addXYZO(SETCOLOR + BLACK); 
        int nchars = s.length(); 
        xyz[0] -= 0.5 * scaledW * nchars;
        for (int k=0; k<nchars; k++)
        {
             int ic = (int) s.charAt(k) + iFontcode; 
             xyz[0] += scaledW; 
             addScaledItem(xyz, ic); 
        }
    }


    void viewelaz(double[] xyz)
    // Puts a labframe point xyz into user's el, az viewframe
    // Assumes that globals sinaz...cosel have been preset. 
    // Formulas are for +Z=vertical axis
    {
        double horiz = xyz[0]*cosaz - xyz[1]*sinaz;
        double vert =  xyz[0]*sinel*sinaz + xyz[1]*sinel*cosaz + xyz[2]*cosel;
        double outof = -xyz[0]*cosel*sinaz - xyz[1]*cosel*cosaz + xyz[2]*sinel;
        xyz[0] = horiz;
        xyz[1] = vert;
        xyz[2] = outof;
    }


    static void ssort(double[] dkeys, int[] idata, int num)
    // selection sort, Sedgewick p.95, puts keys into ascending order
    {
        double dtemp; 
        int itemp;
        for (int i=0; i<num-1; i++)
        {
            for (int j=i+1; j<num; j++)
              if (dkeys[j] < dkeys[i])
              {
                  itemp = idata[j]; 
                  dtemp = dkeys[j]; 
                  idata[j] = idata[i];
                  dkeys[j] = dkeys[i]; 
                  idata[i] = itemp;
                  dkeys[i] = dtemp;
              }
         }
    }
}


