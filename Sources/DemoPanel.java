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
  * Adopting explicit baseList artwork. 
  *
  * @author M.Lampton (c) STELLAR SOFTWARE 2004-2015 all rights reserved.
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
        uxcenter = 0.0;       // optional for GPanel's addAffines()
        uxspan = EXTRAROOM;   // optional for GPanel's addAffines()
        uycenter = 0.0;       // optional for GPanel's addAffines()
        uyspan = EXTRAROOM;   // optional for GPanel's addAffines()

        az = +22; 
        cosaz = U.cosd(az); 
        sinaz = U.sind(az);
        el = +33;
        cosel = U.cosd(el); 
        sinel = U.sind(el);
    }


                    
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







    //-----ARTWORK METHODS---------------
    //-----ARTWORK METHODS---------------
    
    
    
    void addViewBase(double x, double y, double z, int op)
    {
        double xyz[] = {x, y, z}; 
        addViewBase(xyz, op); 
    }
    
    void addViewBase(double[] xyz, int op)
    {
        viewelaz(xyz);                // local rotation operator
        addScaled(xyz, op, QBASE);    // GPanel service
    }
    
    void addRawBase(double x, double y, double z, int op)
    {
        addRaw(x, y, z, op, QBASE);   // GPanel service
    }

	//-----------------------done with test------------------------

    private void doArt()  
    {
        double xyz[] = new double[3]; 
        clearList(QBASE); 
        addRaw(0., 0., 0., SETWHITEBKG, QBASE); 
        addRaw(0., 0., 0., SETCOLOR+BLACK, QBASE); 
        addRaw(1., 0., 0., SETSOLIDLINE, QBASE); 

        for (int i=0; i<6; i++)
          drawJ(jjj[i]); 
    }


    void drawJ(int j)
    {
        double xyz[] = new double[3]; 
        switch(j)
        {
          case 0:  //-----draw the blue floor z=-0.5
            addRawBase(  0.0,  0.0,  0.0, SETCOLOR+BLUE);
            addViewBase(-0.5, -0.5, -0.5, MOVETO); 
            addViewBase(+0.5, -0.5, -0.5, PATHTO); 
            addViewBase(+0.5, +0.5, -0.5, PATHTO); 
            addViewBase(-0.5, +0.5, -0.5, PATHTO); 
            addViewBase(-0.5, -0.5, -0.5, FILL); 
            addString(   0.0,  0.0, -0.5, "BLUE"); 
            break; 

          case 1:  //-----draw the red frontwall y=-0.5
            addRawBase(  0.0,  0.0,  0.0, SETCOLOR+RED);
            addViewBase(-0.5, -0.5, -0.5, MOVETO); 
            addViewBase(+0.5, -0.5, -0.5, PATHTO); 
            addViewBase(+0.5, -0.5, +0.5, PATHTO); 
            addViewBase(-0.5, -0.5, +0.5, PATHTO); 
            addViewBase(-0.5, -0.5, -0.5, FILL); 
            addString(   0.0, -0.5,  0.0, "RED"); 
            break; 
          
          case 2:  //-----draw the green leftwall x=-0.5
            addRawBase(  0.0,  0.0,  0.0, SETCOLOR+GREEN);
            addViewBase(-0.5, -0.5, -0.5, MOVETO); 
            addViewBase(-0.5, +0.5, -0.5, PATHTO); 
            addViewBase(-0.5, +0.5, +0.5, PATHTO); 
            addViewBase(-0.5, -0.5, +0.5, PATHTO); 
            addViewBase(-0.5, -0.5, -0.5, FILL); 
            addString(  -0.5,  0.0,  0.0, "GREEN"); 
            break; 

          case 3:  //-----draw the magenta topwall z=+0.5
            addRawBase(  0.0,  0.0,  0.0, SETCOLOR+MAGENTA);
            addViewBase(-0.5, -0.5, +0.5, MOVETO); 
            addViewBase(+0.5, -0.5, +0.5, PATHTO); 
            addViewBase(+0.5, +0.5, +0.5, PATHTO); 
            addViewBase(-0.5, +0.5, +0.5, PATHTO); 
            addViewBase(-0.5, -0.5, +0.5, FILL); 
            addString(   0.0,  0.0, +0.5, "MAGENTA"); 
            break;           
          
          case 4: //----draw the cyan backwall y=+0.5
            addRawBase(  0.0,  0.0,  0.0, SETCOLOR+CYAN);
            addViewBase(-0.5, +0.5, -0.5, MOVETO); 
            addViewBase(+0.5, +0.5, -0.5, PATHTO); 
            addViewBase(+0.5, +0.5, +0.5, PATHTO); 
            addViewBase(-0.5, +0.5, +0.5, PATHTO); 
            addViewBase(-0.5, +0.5, -0.5, FILL); 
            addString(   0.0, +0.5,  0.0, "CYAN"); 
            break;           
          
          case 5:  //-----draw the yellow rightwall x=+0.5
            addRawBase(  0.0,  0.0,  0.0, SETCOLOR+YELLOW);
            addViewBase(+0.5, -0.5, -0.5, MOVETO); 
            addViewBase(+0.5, +0.5, -0.5, PATHTO); 
            addViewBase(+0.5, +0.5, +0.5, PATHTO); 
            addViewBase(+0.5, -0.5, +0.5, PATHTO); 
            addViewBase(+0.5, -0.5, -0.5, FILL); 
            addString(  +0.5,  0.0,  0.0, "YELLOW"); 
            break;           
          
          default: break; 
       }
    }  // end of drawJ()



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

        addRaw(0., 0., 0., SETCOLOR+BLACK, QBASE); 
        int nchars = s.length(); 
        xyz[0] -= 0.5 * scaledW * nchars;
        for (int k=0; k<nchars; k++)
        {
             int ic = (int) s.charAt(k) + iFontcode; 
             xyz[0] += scaledW; 
             addScaled(xyz, ic, QBASE); 
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


