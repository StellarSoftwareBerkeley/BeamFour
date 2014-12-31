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
  *    macros UO_XXX and strings UO[][][]
  *
  *  Never use "|" in a UO: it is the parser field delimiter. 
  *
  * @author M.Lampton (c) STELLAR SOFTWARE 2004 all rights reserved.
  * Revised 2008 for 3-dimensional UO definitions.
  */
class Registry implements B4constants
{
    private static String sUser[][]; 
    private File fReg; 
    private String fname = "B4OPTIONS.TXT"; 
    private int maxmembers=0, nTotal=0; 


    public Registry(String sInit)  // constructor called once by DMF
    {
        //---first, create sUser[][]------------
        for (int i=0; i<NUOGROUPS; i++)
        {
            nTotal += UO[i].length; 
            if (maxmembers < UO[i].length)
              maxmembers = UO[i].length;
        }

        sUser = new String[NUOGROUPS][maxmembers]; 
        for (int i=0; i<NUOGROUPS; i++)
          for (int j=0; j<maxmembers; j++)
            sUser[i][j] = new String(""); 

        fReg = new File(sInit, fname);
        int n = readOptions(); 
        if (n != nTotal)
        {
           imposeFactory(); 
           saveOptions(); 
        }
    }


    public void imposeFactory()
    {
        for (int i=0; i<NUOGROUPS; i++)
          for (int j=0; j<UO[i].length; j++)
            sUser[i][j] = UO[i][j][1]; 
    }


    public String getuo(int i, int j)
    {
       if ((i>=0) && (i<NUOGROUPS))
         if ((j>=0) && (j<UO[i].length))
           return sUser[i][j]; 
       return ""; 
    }


    public void putuo(int i, int j, String s)
    {
       if ((i>=0) && (i<NUOGROUPS))
         if ((j>=0) && (j<UO[i].length))
         {
             sUser[i][j] = s; 
             saveOptions(); 
         }
    }


    public void saveOptions()
    {
        try
        {
            PrintWriter out = new PrintWriter(new
              FileWriter(fReg), true); // autoflush
            for (int i=0; i<NUOGROUPS; i++)
              for (int j=0; j<UO[i].length; j++)
                out.println(UO[i][j][0] + "|" + sUser[i][j]); 
        }
        catch (IOException ioe)
        {
        }
    } 


    public int readOptions()
    // Called by constructor to set up the saved options.
    // CAUTION: "|" is this parser's token delimiter.
    // So do not use it anywhere within a uoName.
    {
        for (int i=0; i<NUOGROUPS; i++)
          for (int j=0; j<UO[i].length; j++)
            sUser[i][j] = "";  
        try
        {
            BufferedReader br = new BufferedReader(new FileReader(fReg)); 
            int nGoodLines=0; 
            for (int i=0; i<NUOGROUPS; i++)
              for (int j=0; j<UO[i].length; j++)
              {
                  String s = br.readLine(); 
                  if ((s == null) || (s.length() < 1))
                    return nGoodLines; 

                  String t = getField(0, s); 
                  if (!t.equals(UO[i][j][0]))
                    return nGoodLines; 

                  sUser[i][j] = getField(1, s); 
                  nGoodLines++; 
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
    {
        StringTokenizer t = new StringTokenizer(given, "|"); 
        if (n >= t.countTokens())
          return "";
        for (int i=0; i<n; i++)  // skip unwanted tokens
          t.nextToken(); 
        return t.nextToken(); 
    }
}
