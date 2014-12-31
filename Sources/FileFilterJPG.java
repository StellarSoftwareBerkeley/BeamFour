package com.stellarsoftware.beam;

import java.io.File; 
import javax.swing.filechooser.FileFilter;


class FileFilterJPG extends FileFilter
{
   public boolean accept(File f)
   {
      if (f.isDirectory())
        return true; 
      return f.getName().toLowerCase().endsWith(".jpg");
   }

   public String getDescription()
   {
      return "JPG image";
   }
}
