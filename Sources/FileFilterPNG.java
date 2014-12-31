package com.stellarsoftware.beam;

import java.io.File; 
import javax.swing.filechooser.FileFilter;


class FileFilterPNG extends FileFilter
{
   public boolean accept(File f)
   {
      if (f.isDirectory())
        return true; 
      return f.getName().toLowerCase().endsWith(".png");
   }

   public String getDescription()
   {
      return "PNG image";
   }
}
