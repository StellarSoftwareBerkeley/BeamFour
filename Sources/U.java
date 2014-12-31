package com.stellarsoftware.beam;

import java.awt.*; 
import java.text.NumberFormat; 
import java.text.DecimalFormat;
import java.util.*;   // Locale, ArrayList

///
/// To Do: unify suckDouble -- parseDouble
///  also: use private NumberFormat(Locale=US) on input
///  private NumberFormat & DecimalFormat are available.
///  also: filter through nationalize() before parsing inputs
///


/**  U.java   static utilities class  
  *  contains a "main" driver to test formatter fmt()
  *
  *
  * @author M.Lampton (c) STELLAR SOFTWARE 2004 all rights reserved.
  */
class U implements B4constants  
{
    public static void main(String[] cmd) // testing
    {
        double nums[] = {1.234, -5.678, +0.432e-4, -0.23456e-7};

        double huge = 1234567.89; 
        for (int j=0; j<4; j++)
          for (int i=0; i<16; i++)  // check e-format lengths
          {
            int seeklength = i; 
            int seekdec = 4; 
            
            String s = fmtc(nums[j], seeklength, seekdec, 'e'); 
            int obslen = s.length(); 
          }
    }



    //-------public static methods-----------

    static int getTwoDigitCode(String s)
    // Ignores the first character and returns the two digit code:  "X34" => 34
    // Returns zero if there is no such code
    {
        if (s.length() < 2)
          return 0; 
        char a = getCharAt(s,1);  // always safe
        if ((a<'0') || (a>'9'))
          return 0; 
        int i = a-48; 
        char b = getCharAt(s,2);  // always safe
        if ((b<'0') || (b>'9'))
          return i; 
        return i*10 + b-48; 
    }


    static int getColorCode(char c)
    // allows artwork to convert a tag char into a display color
    {
        int icolor = BLACK; // zero; others are 1...9
        switch (c)
        {
            case 'R':
            case 'r': icolor = RED; break; 
            case 'G':
            case 'g': icolor = GREEN; break;
            case 'Y':
            case 'y': icolor = YELLOW; break;
            case 'B':
            case 'b': icolor = BLUE; break;
            case 'M':
            case 'm': icolor = MAGENTA; break;
            case 'C':
            case 'c': icolor = CYAN;  break; 
            default:  icolor = BLACK; 
        }
        return icolor; 
    }

    static int getColorCode(String s)
    {
        char c = U.getCharAt(s,0); // safe even for empty strings
        return getColorCode(c); 
    }


    static int getInt(double x)
    {
        return (int) Math.round(x); 
    }

    static double grand()
    // Returns a zero-mean unit-variance Gaussian random number
    {
        double sum = -6.0; 
        for (int i=0; i<12; i++)
          sum += Math.random(); 
        return sum;
    }
    
    static double put360(double x)
    // Puts x into range 0<=x<360
    { 
        double y = x/360 - 0.5;
        return 360*(y-Math.round(y)+0.5); 
    }
    
    static double put180(double x)
    // Puts x into range -180<x<+180
    {
        double y = x/360; 
        return 360*(y-Math.round(y));
    }
  
    static int minmax(int i, int bot, int top)
    {
        return Math.max(bot, Math.min(top, i)); 
    }

    static double minmax(double d, double bot, double top)
    {
        return Math.max(bot, Math.min(top, d));
    }

    static double trapezoid(double x)
    // height=1; center is x=0; shoulders x=+-0.25; base x=+-0.5
    {
        return minmax(2.0-4.0*Math.abs(x), 0.0, 1.0); 
    }

    static double getBlue(double x)
    // convert 0<x<1 to RGB thermometer: blue peaks at 0.0
    {
        return trapezoid(x); 
    }

    static double getGreen(double x)
    // convert 0<x<1 to RGB thermometer: green peaks at 0.5
    {
        return trapezoid(x-0.5); 
    }
 
    static double getRed(double x)
    // convert 0<x<1 to RGB thermometer: red peaks at 1.0
    {
        return trapezoid(x-1); 
    }


    static void beep() // sounds more like a bonk.
    {
          Toolkit.getDefaultToolkit().beep();
    }

    static String getExtensionToLower(String fname) 
    {
        String ext = null;
        int i = fname.lastIndexOf('.');
        if (i > 0 &&  i < fname.length() - 1)
          ext = fname.substring(i+1).toLowerCase();
        return ext;
    }


    static double pm1(double x)
    {
        return Math.min(Math.max(x, -1.0), +1.0); 
    }

    static boolean isNegZero(double x)
    {
        return 1.0/x == Double.NEGATIVE_INFINITY;
    }

    static double sqr(double x) 
    {
        return x*x;
    }

    static double sawtooth(double x, double p, boolean bOdd)
    {
        if (p<TOL)
          return 0.0; 
        if (!bOdd)
          x += 0.5*p; 
        return x - p*Math.round(x/p); 
    }

    static double cosd(double x)
    {
        return Math.cos(Math.toRadians(x)); 
    }

    static double sind(double x)
    {
        return Math.sin(Math.toRadians(x)); 
    }

    static double tand(double x)
    {
        return Math.tan(Math.toRadians(x)); 
    }

    static int imax3(int a, int b, int c)
    {
        return Math.max(a, Math.max(b, c)); 
    }

    static int imax5(int a, int b, int c, int d, int e)
    {
        return Math.max(a, Math.max(b, Math.max(c, Math.max(d, e))));
    }

    static boolean isOdd(int i)
    {
        return i % 2 != 0;
    }

    static boolean isBit(int word, int whichbit)
    {
        return isOdd(word >>> whichbit);
    }

    static int setBit(int word, int whichbit)
    // forces a given bit to be "1"
    {
        if (isBit(word, whichbit))
          return word;  // already set
        return word + (1 << whichbit);
    }

    static int clearBit(int word, int whichbit)
    // forces a given bit to be "0"
    {
        if (isBit(word, whichbit))
          word -= (1 << whichbit);
        return word;
    }

    static int manageBit(int word, int whichbit, boolean b)
    // forces a given bit to be "b"
    {
        return b ? setBit(word, whichbit) : clearBit(word, whichbit);
    }


    static int SAFEINT(double x)
    {
        return (int) Math.max(-10000.0, Math.min(10000.0, x)); 
    }

    static String fwi(int n, int w)
    // converts an int to a string with given width.
    {
        StringBuffer sb = new StringBuffer(); 
        sb.append(Integer.toString(n)); 
        while (sb.length() < w)
           sb.insert(0, " ");
        return sb.toString();
    }

    static String fn6(int dat[], int ndat)
    {
        StringBuffer sb = new StringBuffer(100); 
        for (int i=0; i<ndat; i++)
          sb.append(fwi(dat[i], 6)); 
        return sb.toString();
    } 

    static String fwd(double x, int w, int d)
    // converts a double to a string with given width and decimals.
    {
        NumberFormat nf = NumberFormat.getInstance(Locale.US); 
        DecimalFormat df = (DecimalFormat) nf;
        df.setMaximumFractionDigits(d); 
        df.setMinimumFractionDigits(d);
        df.setGroupingUsed(false);   
        String s = df.format(x); 
        while (s.length() < w)
          s = " " + s;  
        if (s.length() > w)
        {
            s = "";
            for (int i=0; i<w; i++)
              s = s + "-";
        }  
        return s; 
    }


    static String tidy(double x)
    // Converts double to a tidy string;
    // trims trailing zeros, leading & trailing blanks
    // but fails to tidy 0.99999999999
    {
        StringBuffer sb = new StringBuffer(fwd(x, 18, 9)); 
        int iend = sb.length()-1; 
        int i; 
        for (i=iend; i>4; i--)
        {
            if ('0' != sb.charAt(i))
              break; 
        }
        sb.setLength(i+1); 
        String s = sb.toString(); 
        return s.trim(); 
    }



    static String gd(double x)
    // Converts a double into a nicely formatted string.
    // Sacrifices precision to gain shortness.
    // Uses D notation if reasonable size
    // else uses E notation. Three to 7 chars.
    {
        String s = "0.00"; 
        if (x == 0.0)
          return s;
        int mag = (int) log10(Math.abs(x)); 
        if ((mag<-3) || (mag>3))
          return fweshort(x).trim(); 

        boolean bNeg = x < 0.0; 
        int ndec = 5 - Math.max(0, mag);
        if (bNeg)
          ndec--; 
        StringBuffer sb = new StringBuffer(fwd(x, 8, ndec)); 
        while ((sb.length() > 2) && (sb.charAt(sb.length()-1)=='0'))
          sb.setLength(sb.length()-1); 
        String t = new String(sb); 
        return t.trim();
    }



    static String fwe(double x)
    // Converts a positive double into a 9-char Enotation string
    // But what about negative exponents?
    // Ugly property: shows E-03 not E-3
    {
        NumberFormat nf = NumberFormat.getInstance(Locale.US); 
        DecimalFormat ef = (DecimalFormat) nf;
        ef.applyPattern("0.00E00"); 
        String s = ef.format(x); 
        if (s.length()==7)   // when zero
          return "  "+s;
        if (s.length()==8)   // when positive
          return " "+s;
        return s;            // else negative
    }
    
    static String fweshort(double x)
    // a shorter version of fwe()
    {
        NumberFormat nf = NumberFormat.getInstance(Locale.US); 
        DecimalFormat ef = (DecimalFormat) nf;
        ef.applyPattern("0.00E0"); 
        String s = ef.format(x); 
        return s;            // else negative
    }

    static String twoint(int i, int j)
    // converts two small ints to a string of width 8
    {
       return fwi(i,4) + fwi(j,4); 
    }


    static String nd(int n, double[] v)
    // converts a vector of n doubles into a fixed point string
    {
       StringBuffer sb = new StringBuffer(100);
       for (int i=0; i<n; i++)
          sb.append(fwd(v[i],11,6)); 
       String s = sb.toString();
       return s; 
    }


    public static String fmtc(double x, int fw, int dp, char c)
    /// formats to either fwidth+enotation or fwidth+decplaces.
    /// char c must be 'e', 'E', ',' or '.'
    /// Called by EPanel:putFieldDouble()
    {
        if (fw < 1)
          return ""; 
        if ((c=='E') || (c=='e'))
         return efmt(x, fw, c);
       else
         return ffmt(x, fw, dp, c);
    }


    public static String efmt(double x, int fw, char c)
    // char c will be 'e' or 'E'
    {
        //---------first design the format---------
        if (fw < 7)
        {
           String s = "------"; 
           String t = s.substring(0, fw); 
           return t; 
        }
        char sign = '+'; 
        if (x < 0)
        {
           sign = '-'; 
           x = Math.abs(x); 
        }
        if (x < 1E-99)
          x = 0.0; 
        if (x > 9E99)
          x = 9E99; 
        if (fw > 40)
          fw = 40; 
        int eminus = 0; // extra char for expon minus sign
        if (x < 1.0)
          eminus = 1;  
        StringBuffer sb = new StringBuffer(50); 

        sb.append("0.");  
        for (int i=3; i<fw-3-eminus; i++)
          sb.append("0"); 

        sb.append("E00"); // never get plus signs in exponentials

        //---------now construct the formatter----------

        NumberFormat nf = NumberFormat.getInstance(Locale.US); 
        DecimalFormat df = (DecimalFormat) nf;
        df.applyPattern(sb.toString()); 

        //----re-use sb---------------

        sb.setLength(0); 
        sb.append(sign); 
        sb.append(df.format(x));
 
       //-----if decimal point is absent, correct it-----
 
        if (getCharAt(sb, 2) == 'E') // safe
          sb.insert(2, '.'); 
        if (c=='e')
          for (int i=0; i<sb.length(); i++)
          {
              if (sb.charAt(i) == 'E')
                sb.setCharAt(i, 'e'); 
          }
        return sb.toString(); 
    }
 
    public static String ffmt(double x, int w, int d, char c)
    {
        if (d<0)  // digits
          d = 0; 
        if (w<1)  // width
          w = 1; 

        NumberFormat nf = NumberFormat.getInstance(Locale.US); 
        DecimalFormat df = (DecimalFormat) nf;
        df.setMaximumFractionDigits(d); 
        df.setMinimumFractionDigits(d);
        df.setGroupingUsed(false);   

        StringBuffer sb = new StringBuffer(50); 
        sb.append(df.format(x)); 

        while (sb.length() < w)
          sb.insert(0, ' ');  // prepend sb

        if (sb.length() > w)
        {
            int frontsize = sb.length() - 1 - d; 
            if (frontsize < w)
              sb.setLength(w);   // meat ax.  
            else
            {
                sb.setLength(0);  // clears sb
                for (int i=0; i<w; i++)
                  sb.append('-');
            }
        }  
        if (c==',')
          for (int i=0; i<sb.length(); i++)
          {
              if (sb.charAt(i)=='.')
                sb.setCharAt(i, ','); 
          }
        return sb.toString(); 
    }

    public static int parseInt(String s)
    // exception safe parser; handles "?" strings ok
    {
        try
        {
            int i = Integer.parseInt(s); 
            return i; 
        }
        catch(NumberFormatException nfe)
        {
            return -0; 
        }
    }


/********************
    public static double parseDouble(String s)
    // exception safe parser; handles "?" strings ok
    {
        try
        {
            double d = Double.parseDouble(s); 
            return d;
        }
        catch(NumberFormatException nfe)
        {
            return -0.0;
        }
    }
*********************/


    public static int suckInt(String s) 
    // Pulls the first int from a string.
    // The int may be preceded or followed by blanks, text, etc. 
    // Failure: returns zero. 
    {
        int istart=0, iend=0; 
        int ilen = s.length(); 
        if (ilen < 1)
          return 0; 
        int i=0; 
        while ((i<ilen) && (getCharAt(s,i)<'0') || (getCharAt(s,i)>'9'))
          istart = iend = ++i; 
        while ((i<ilen) && ('0'<=getCharAt(s,i)) && (getCharAt(s,i)<='9'))
          iend = ++i; 
        if (iend <= istart)
          return 0; 
        String t = s.substring(istart, iend).trim(); 
        return Integer.parseInt(t); 
    }

    public static double suckDouble(String s) 
    // pulls a double out of a string, trimmed or not. 
    // String may have preceding and trailing blanks. 
    // String may NOT have preceding and trailing chars. Tests OK.
    // if there is a final colon it is trimmed off.  Tests OK.
    // Malformed? returns NaN. 
    // EMPTY?  returns -0.0.  
    // Contains -0.0?  returns +0.0.
    {
        s = nationalize(s); // trim, remove groups, convert comma to a period.
        int len = s.length(); 
        if (len < 1)
          return -0.0; 
        if (getCharAt(s, len-1) == ':')
          s = s.substring(0, len-1); // drop the tag char.
        s = s.trim(); 
        len = s.length(); 
        if (len < 1)
          return -0.0; 
        double d = Double.NaN; 
        try 
        {
             d = Double.parseDouble(s);
        }
        catch (NumberFormatException nfe)
        {
            if (nfe.getMessage().equals("EMPTY String"))
              return -0.0;  
            return Double.NaN; 
        } 
        if (isNegZero(d))
          d = +0.0; 
        return d; 
    }

    public static char suckChar(String s)
    {
        return getCharAt(s, 0); 
    }

    public static char getCharAt(String s, int n)
    // Safe version of String.charAt()
    // error condition returns ' '
    {
       if ((n < 0) || (s.length() <= n))
         return ' ';
       char c = ' '; 
       try {c = s.charAt(n);}
       catch (IndexOutOfBoundsException iobe) {c = ' ';}
       return c; 
    }

    public static char getCharAt(StringBuffer sb, int n)
    {
        String s = new String(sb); 
        return getCharAt(s, n); 
    }


    public static String s12(String s)
    // makes any string at least 12 characters long
    {
         while (s.length() < 10)
            s += ' ';
         return s; 
    }

    static double log10(double arg)
    {
        if (arg>0.0)
          return Math.log(arg)/LN10;
        return -999.9;
    }

    static double dex(double arg)
    {
        if (arg > 300.0)
           arg = 300.0; 
        if (arg < -300.0)
           arg = -300.0; 
        return Math.pow(10.0, arg); 
    }


    static int tokenize(String input, String delimiters, ArrayList<String> tList)
    {
        tList.clear(); 
        StringTokenizer st = new StringTokenizer(input, delimiters); 
        int tcount = 0; 
        while (st.hasMoreTokens())
        {
           tList.add(st.nextToken().trim()); 
           tcount++;
        }
        return tcount;
    }


    static void ruler(double a, double b, // the given interval
                      boolean bBeyond,    // overspan? else underspan
                      double[] values,    // list of tick values
                      int[] output)       // [0]=nticks,[1]=fracdigits.
    // Paul Heckbert in GRAPHICS GEMS 1990; Lampton 2005.
    {
        if (a==b) return; 
        if (a>b)  {double t=a; a=b; b=t;}
        // following choices give nTicks=2 to 5, beyond or not.
        int nomticks = bBeyond ? 3 : 5; 
        double range = nicenum(b-a, false); 
        double step = nicenum(range/(nomticks-1), true); 
        if (bBeyond)
        {
            a = Math.floor(a/step)*step; 
            b = Math.ceil(b/step)*step; 
        }
        else
        {
            a = Math.ceil(a/step)*step; 
            b = Math.floor(b/step)*step;
        }
        int iDig = -(int) Math.floor(log10(step)); 
        int nFracDigits = Math.max(iDig, 0); 
        int nTicks = (int) (1.5 + (b-a)/step); 
        nTicks = Math.max(1, Math.min(MAXTICKS-1, nTicks)); 
        for (int i=0; i<nTicks; i++)
          values[i] = a+i*step; 
        // in Constants.java: NTICKS=0;  NFRACDIGITS=1, MAXTICKS=10
        output[NTICKS] = nTicks; 
        output[NFRACDIGITS] = nFracDigits; 
    }

    static double nicenum (double x, boolean bRound)
    // finds a nice number approx equal to |x|.
    // rounds downward if bRound=true, else nice>=|x|.
    // Paul Heckbert in GRAPHICS GEMS 1990.
    {
        double exp;    // exponent of x in e-notation
        double f;      // fractional part of x
        double nf;     // nice fraction
        x = Math.abs(x); 
        if (x==0.0)
          return 0.0; 

        exp =  Math.floor(log10(x)); 
        f = x / dex(exp); 
        if (bRound)
          if (f<1.5) nf=1.0;
          else if (f<3.0) nf=2.0;
          else if (f<7.0) nf=5.0;
          else nf=10.0; 
        else
          if (f<=1.0) nf=1.0; 
          else if (f<=2.0) nf=2.0;
          else if (f<=5.0) nf=5.0; 
          else nf=10.0; 
        return nf * dex(exp);
    }


/*********************
    static void ruler(double a, double b, // the given interval
                      boolean beyond,     // overspan? else underspan
                      double[] values,    // list of output tick values
                      int[] output)       // 0=NTICKS; 1=NFRACDIGITS
    // Lampton's ruler algorithm, span within or beyond (a,b).
    // Gives 2 to 4 ticks with the coefficients shown. 
    // Subtle bug though: sometimes intervals are unequal!
    {
        double coefbeyond=1.0, coefwithin=0.333;  //0.5; 
        output[NTICKS] = output[NFRACDIGITS] = 0; 
        if (a==b)
          return;
        if (a>b)
          {double t=a; a=b; b=t;}
        double f = (b-a) * (beyond ? coefbeyond : coefwithin); 
        double p = Math.floor(log10(f)); 
        double step = dex(p); 

        // enlarge step=N*10^M to fit the span

        f /= step;  
        if (f>8.0)
          step *= 8.0;           
        else if (f>5.0)
          step *= 5.0;
        else if (f>4.0)
          step *= 4.0;
        else if (f>3.0)
          step *= 3.0; 
        else if (f>2.0)
          step *= 2.0; 
        else if (f>1.5)
          step *= 1.5;  

        int i = (int) (beyond ? Math.floor(a/step) : Math.ceil(a/step)); 
        int j = (int) (beyond ? Math.ceil(b/step) : Math.floor(b/step)); 
        
        // array overflow safety limiter:
        if (j>i+6)
          j = i+6; 
        for (int k=0; k<=j-i; k++)
          values[k] = (k+i)*step; 
        output[NTICKS] = 1+j-i; 
        output[NFRACDIGITS] = (int) (p<0 ? -p : 0);

        // A test to learn about the ruler bug:
        // Sometimes, segments are unequal: -2, 0, 2, 3.
        // Are the ticks equally spaced?
        // Hmmm, can't get bug to reappear. 
        double delta = values[1] - values[0];
        int nticks = output[NTICKS]; 
        for (int n=2; n<nticks; n++)
        {
            double d = values[n] - values[n-1]; 
            double diff = d - delta; 
        }
    }
********************/

    public static boolean areSimilar(int i, int j) // same type?
    // returns true if ray attributes i & j are both spacelike 
    // or both angular, else false.
    {
        // return (i/3)%2 == (j/3)%2;
        boolean biXYZ = ((i==RX) || (i==RY) || (i==RZ) 
            || (i==RTXL) || (i==RTYL) || (i==RTZL));
        boolean biUVW = ((i==RU) || (i==RV) || (i==RW) 
            || (i==RTUL) || (i==RTVL) || (i==RTWL));
        boolean bjXYZ = ((j==RX) || (j==RY) || (j==RZ) 
            || (j==RTXL) || (j==RTYL) || (j==RTZL));
        boolean bjUVW = ((j==RU) || (j==RV) || (j==RW) 
            || (j==RTUL) || (j==RTVL) || (j==RTWL));
        return (biXYZ && bjXYZ) || (biUVW && bjUVW); 
    }

    public static boolean isAngle(int i)
    // Returns true if surface var is tilt, pitch, or roll
    {
        return ((i==OTILT) || (i==OPITCH) || (i==OROLL));
    }


    public static boolean getBit(int arg, int whichbit)
    {
        arg >>= whichbit;
        return (arg & 1) == 1; 
    }

    public static double dGetDefaultValue(String s)
    // Used within RT13 to evaluate default ray strings
    {
        s = s.trim(); 
        if (s.equals("-makeup"))
          return MMAKEUP; // -1.000001
        if (s.equals("+makeup"))
          return PMAKEUP; // +1.000001
        try
        {
            double d = Double.parseDouble(s); 
            return d;
        }
        catch(NumberFormatException nfe)
        {
            return -0.0;
        }
    }


    /** N-dimensional fast Fourier transform using zero-based arrays.
      * Cooley-Tukey FFT algorithm; see Press et al Numerical Recipes 1986.
      * data[0,1,2...] = {real0, imag0, real1, imag1, ....} in and out.
      * ndim is how many dimensions, = 1 for 1-dimensional transform.
      * nn[idim] is how many complex points in each dimension.
      * Each nn must be an exact power of two.
      *
      * @author M.Lampton Java edition (c) 2002 STELLAR SOFTWARE
      */
    static public void fourn(double data[], int nn[], int ndim, int isign)
    {
        int idim;
        int i1, i2, i3, i2rev, i3rev, ip1, ip2, ip3, ifp1, ifp2;
        int ibit, k1, k2, n, nprev, nrem, ntot;
        double tempi, tempr;
        double theta, wi, wpi, wpr, wr, wtemp;

        for (ntot=1, idim=0; idim<ndim; idim++)
          ntot *= nn[idim]; 
        nprev=1;
        for (idim=ndim-1; idim>=0; idim--)
        {
            n = nn[idim]; 
            nrem = ntot/(n*nprev);
            ip1 = nprev*2;  
            ip2 = ip1*n;  
            ip3 = ip2*nrem;
            i2rev = 0;   
            for (i2=0; i2<ip2; i2+=ip1)
            {
                if (i2 < i2rev) 
                {
                    for (i1=i2; i1<=i2+ip1-2; i1+=2)
                    {
                        for (i3=i1; i3<ip3; i3+=ip2)
                        {
                            i3rev = i2rev+i3-i2;  
                            double t = data[i3];    
                            data[i3] = data[i3rev]; 
                            data[i3rev] = t;      
                            t = data[i3+1];   
                            data[i3+1] = data[i3rev+1]; 
                            data[i3rev+1] = t;         
                        }
                    }
                }
                ibit = ip2/2;  
                while (ibit>=ip1 && i2rev>=ibit)    
                {
                    i2rev -= ibit;
                    ibit /= 2; 
                }
                i2rev += ibit;
            }
            ifp1=ip1;
            while (ifp1 < ip2)
            {
                ifp2 = ifp1*2; 
                theta = isign*2*3.14159265358979324/(ifp2/ip1);
                wtemp = Math.sin(0.5*theta);
                wpr = -2.0*wtemp*wtemp;
                wpi = Math.sin(theta);
                wr = 1.0;
                wi=0.0;
                for (i3=0; i3<ifp1; i3+=ip1)
                {
                    for (i1=i3; i1<=i3+ip1-2; i1+=2) 
                    {
                        for (i2=i1; i2<ip3; i2+=ifp2) 
                        {
                            k1 = i2;             
                            k2 = k1+ifp1;
                            tempr = (float)wr*data[k2]-(float)wi*data[k2+1];
                            tempi = (float)wr*data[k2+1]+(float)wi*data[k2]; 
                            data[k2] = data[k1]-tempr;     
                            data[k2+1] = data[k1+1]-tempi; 
                            data[k1] += tempr; 
                            data[k1+1] += tempi;
                        }
                    }
                    wr = (wtemp=wr)*wpr-wi*wpi+wr;
                    wi = wi*wpr+wtemp*wpi+wi;
                }
                ifp1 = ifp2;
            }
            nprev *= n;
        }
        if (isign < 0)
          for (i1=0; i1<2*ntot; i1++)
            data[i1] /= ntot;       
    }



    
    //----private static method for nationalization------------

    static String nationalize(String s)
    // Nationalizes a numerical string for worldwide input.
    // Eliminates groupings, enforces period as decimal point
    {
         StringBuffer sbIn = new StringBuffer(s.trim()); 
         int len = sbIn.length(); 

         //---convert to periods and locate rightmost--------
         int dpcount = 0;    //  0 = not found
         int jdp = -1;       // -1 = not found
         for (int i=0; i<len; i++)
         {
             char c = sbIn.charAt(i); 
             if ((c=='.') || (c==','))
             {
                 dpcount++; 
                 jdp = i; 
                 sbIn.setCharAt(i, '.'); 
             }
         }
         if (dpcount < 2)
           return sbIn.toString(); 

         //---copy, skipping group chars, but keep jdp-----

         StringBuffer sbOut = new StringBuffer(); 
         for (int i=0; i<len; i++)
         {
             char c = sbIn.charAt(i); 
             if ((c != '.') || (i==jdp))
               sbOut.append(c); 
         }
         return sbOut.toString();
    }



    public static int getFields(String ruler, String record, 
                     ArrayList<String> fList,  ArrayList<Character> cList)
    // Reads ruler; extract fields from table record into fList.
    // NOTE initial colon is not a field, and is skipped.
    // Returns the number of fields found including zero length fields.
    // (This should equal ncolons+1.)
    // Also returns the list of tag characters. 
    // Unused in BEAM FOUR, but elegant. 
    // StringBuilder is absent in Java 1.4;  my ProGuard uses 1.4;  
    // use safer/slower StringBuffer instead for ProGuard compatibility. 
    {
        fList.clear();  
        cList.clear(); 
        int max = Math.max(ruler.length(), record.length()); 
        // StringBuilder sb = new StringBuilder(); 
        StringBuffer sb = new StringBuffer(); 
        boolean bInitialColon = (U.getCharAt(ruler, 0) == ':');
        int istart = bInitialColon ? 1 : 0; 
        for (int i=istart; i<max; i++)
        {
            if (U.getCharAt(ruler, i) == ':')
            {
                fList.add(sb.toString().trim()); 
                cList.add(U.getCharAt(record, i)); 
                sb.setLength(0);  
            }
            else
              sb.append(U.getCharAt(record, i)); 
        }
        fList.add(sb.toString().trim()); 
        cList.add(' ');  
        return fList.size(); 
    }



}
