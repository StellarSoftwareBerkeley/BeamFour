package com.stellarsoftware.beam;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;    // all i/o
import java.util.*;  // StringTokenizer


/**
  *  A registry for strings, filed in B4OPTIONS.TXT
  *
  *  This is the internal engine for registering user options.
  *  The user interface methods are in Options.java
  *  Constants is the interface class for user option definitions:
  *    macros UO_XXX and constant factory strings UO[][][]
  *
  *  The internal list of various user preferences is sUser[group][item].
  *  Never use "|" in an option: it is the parser field delimiter. 
  *
  * @author M.Lampton (c) STELLAR SOFTWARE 2004 all rights reserved.
  * Revised 2008 for 3-dimensional UO definitions.
  *
  * A179 August 2015: revised to permit some empty user option fields
  *  -- although empty descriptors is still not valid.
  */
class Registry implements B4constants
{
    private static String sUser[][]; 
    private File fReg; 
    private String fname = "B4OPTIONS.TXT"; 
    private int maxmembers, nTotal; 


    public Registry(String sInit)  // constructor called once by DMF
    {
        //---first, create sUser[][]------------
        nTotal = 0; 
        maxmembers = 0;
        for (int g=0; g<NUOGROUPS; g++)
        {
            nTotal += UO[g].length; 
            if (maxmembers < UO[g].length)
              maxmembers = UO[g].length;
        }

        //----load it with names and blanks-----
        sUser = new String[NUOGROUPS][maxmembers]; 
        for (int g=0; g<NUOGROUPS; g++)
          for (int j=0; j<maxmembers; j++)
            sUser[g][j] = new String(""); 

        //---load the stored preferences from disk-----
        fReg = new File(sInit, fname);
        int n = readOptions(); 
        if (n != nTotal)
        {
           imposeFactory(); 
           saveOptions(); 
        }
    }


    public void imposeFactory()
    // imposes constant fields in UO onto sUser preferences.
    {
        for (int g=0; g<NUOGROUPS; g++)
          for (int j=0; j<UO[g].length; j++)
            sUser[g][j] = UO[g][j][1]; 
    }


    public String getuo(int g, int j)
    // fetches a desired preference from sUser[][].
    {
       if ((g>=0) && (g<NUOGROUPS))
         if ((j>=0) && (j<UO[g].length))
           return sUser[g][j]; 
       return ""; 
    }


    public void putuo(int g, int j, String s)
    // stashes a new preference, and saves everything to disk.
    {
       if ((g>=0) && (g<NUOGROUPS))
         if ((j>=0) && (j<UO[g].length))
         {
             sUser[g][j] = s; 
             saveOptions(); 
         }
    }


    public void saveOptions()
    {
        try
        {
            PrintWriter out = new PrintWriter(new
              FileWriter(fReg), true); // autoflush
            for (int g=0; g<NUOGROUPS; g++)
              for (int j=0; j<UO[g].length; j++)
                out.println(UO[g][j][0] + "|" + sUser[g][j]); 
        }
        catch (IOException ioe)
        {
        }
    } 


    public int readOptions()
    // Called by constructor to bring the disk-saved options into memory.
    // CAUTION: "|" is this parser's token delimiter.
    // So do not use "|" anywhere within a uoName or a UOpreference.
    {
        for (int g=0; g<NUOGROUPS; g++)
          for (int j=0; j<UO[g].length; j++)
            sUser[g][j] = "";  
        try
        {
            BufferedReader br = new BufferedReader(new FileReader(fReg)); 
            int nGoodLines=0; 
            for (int g=0; g<NUOGROUPS; g++)
              for (int j=0; j<UO[g].length; j++)
              {
                  String s = br.readLine(); 
                  if ((s == null) || (s.length() < 1))
                  {
                      return nGoodLines;          // length failure; bail out.
                  }
                  String t = getField(0, s);    // read the item name.
                  if (!t.equals(UO[g][j][0]))   // test it against UO.
                  {
                      return nGoodLines;          // name disagreement; bail out.
                  }
                  sUser[g][j] = getField(1, s); // read & install preference
                  nGoodLines++;                 // field OK; increment and proceed.
              }
            return nGoodLines; 
        }
        catch (IOException ioe)
        {
            return 0; 
        }
    }

    private static String getField(int n, String given)
    // Returns the n-th field of a given "|" delimited string.
    // Requires import java.util.* for StringTokenizer. 
    // Called by readOptions(), above. 
    {
        StringTokenizer t = new StringTokenizer(given, "|"); 
        if (n >= t.countTokens())
          return "";
        for (int i=0; i<n; i++)  // skip unwanted tokens
          t.nextToken(); 
        return t.nextToken(); 
    }
}
