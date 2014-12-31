package com.stellarsoftware.beam;

import java.util.*;         // ArrayList

/**
  *
  * Class Adjustment is a data structure combining four elements
  *     iattrib, irow, ifield, slaveList. 
  * It defines each element of an ArrayList of adjustable parameters.
  * It is used as a stash in OEJIF when parsing adjustables.
  * Thereafter, OEJIF is able to answer questions about its adjustables.
  * Later, Auto asks OEJIF questions about its adjustables.
  * Since .RAY data are adjustable, REJIF also needs Adjustment
  * and therefore this class is free standing not captive in OEJIF.
  * Moreover, Auto knows about
  *    DMF.giFlags[RNADJ]
  * in addition to
  *    DMF.giFlags[ONADJ].
  *
  * Improvement 14 July 2007 version A35: eliminate all doubles,
  *   carry no internal value, instead carry only pointers into 
  *   RT13.surfs[jusrf][iattr] and into RT13.raystarts[kray][iattr].
  * The four fields of Adjustment are {iattr, item, field, slaveList}. 
  *   where item = kray or jsurf depending on owner's usage.
  *
  * Remember when going to/from table, row=item+2. 
  *
  * Radius of Curvature (A149): iattrib = ORAD, ORADX, or ORADY
  *  On input or nudge, set OCURVE = 0 or 1/OROC
  *  Then on output, retrieve OROC = 9E99 or 1/OCURVE.
  *  Analogy is to shape vs asphericity.  
  *
  *  @author M.Lampton (c) 2004 STELLAR SOFTWARE all rights reserved.
  */
class Adjustment     // attrib, rec, field, slaveList
{
    private int iatt, rr, ff;
    private ArrayList<Integer> slaveList; 

    public Adjustment(int ga, int gr, int gf, ArrayList<Integer> gList)
      { iatt=ga; rr=gr; ff=gf; slaveList=gList; }

    public int getAttrib()
    {
        return iatt;
    }

    public int getRecord() // this is jsurf or kray, 1...nItems.
    {
        return rr;
    }

    public int getField()
    {
        return ff;
    }

    public ArrayList<Integer> getList()
    {
        return slaveList;
    }
}
