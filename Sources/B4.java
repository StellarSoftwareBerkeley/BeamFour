package com.stellarsoftware.beam;

import javax.swing.*;

/** B4.java  --- supplies main() for DeskMenuFrame
  *
  *
  * @author M.Lampton (c) STELLAR SOFTWARE 2004 all rights reserved.
  */
public class B4
{
    public static void main(String[] args) 
    {
        SwingUtilities.invokeLater(new Runnable() 
        {
            public void run() 
            {
                DMF dmf = new DMF(); 
                dmf.setVisible(true);
                dmf.toFront(); 
            }
        });
    }
}