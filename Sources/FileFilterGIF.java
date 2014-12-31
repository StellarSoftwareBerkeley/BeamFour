package com.stellarsoftware.beam;

import java.io.File; 
import javax.swing.filechooser.FileFilter;


class FileFilterGIF extends FileFilter
{
   public boolean accept(File f)
   {
      if (f.isDirectory())
        return true; 
      return f.getName().toLowerCase().endsWith(".gif");
   }

   public String getDescription()
   {
      return "GIF image";
   }
}
