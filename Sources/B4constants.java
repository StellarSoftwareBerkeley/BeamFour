package com.stellarsoftware.beam;

import java.awt.*;   // Color

/**
  * B4constants.java  -- an interface carrying static global constants
  *  A207: attempting logic cleanup for bimodal optics
  *  A206: Converting all mirrors into bimodal mirrors, never killing rays
  *  A205: Autoadjust ray failure now announces surface & failure classification
  *  A204: fix the spider layout artwork (spider FUNCTIONS okay). 
  *  A203: allow "asphericity" as well as previous "asph"; see OEJIF line 800.
  *  A202 wider editor fields. 
  *  A201  Sept 2017 has user selected skeleton parameters for new files. 
  *  All fields here are public static final. (H&C p.206)
  *  Added RTANGLE between ray and normal any surface, March 2015, Oct 2015.
  *  Added RTNORMX, RTNORMY, RTNORMZ as new ray attributes. 
  *  Added lower case i, j, k to read out the surface normal vector. 
  *  Added OSGAUSS for 2D circular Gaussian shaped surface profile
  *  Added user selectable new file skeleton options  A201
  *  @author M.Lampton STELLAR SOFTWARE (c) 2004-2016 all rights reserved.
  */
interface B4constants
{    
    static final String  PRODUCT    = "BEAM FOUR  "; 
    static final String  RELEASE    = "Release 208,  15 Jan 2019";
    static final String  COMPILER   = "Compiler was: Javac 1.8.0_20";
    static final String  COPYRIGHT  = "(c) 2018 Stellar Software"; 
    static final boolean DEBUG      = false; 
    
    static final char    NULLCHAR   = (char) 0;
    
    static final int BLINKMILLISEC      = 300; // millisec
    static final int INITIALFRAMEWIDTH  = 600; // pixels
    static final int INITIALFRAMEHEIGHT = 500; // pixels
    static final int INITIALEDITWIDTH   = 400; // pixels
    static final int INITIALEDITHEIGHT  = 300; // pixels
    static final int DIAGONAL           = 10;  // diagonal spacing of windows
    static final int MINFILENAME        = 5;   // shortest legal filename
    
    // To evaluate INITIALGRAPHICSIZE see getUOpixels() below...

    static final int MAXPOLYARG     = 999948; // bigger crashes drawPolyline()
    static final int MAXRANDQUADS   = 10000;  // print+CAD limit
    // note: no MAXQUADS since quadLists are self-sizing arrayLists. 
    static final int MAXSORT        = 10000;  // sorted layout items
    static final int MAXHOLES       = 1000;   // array iris ceiling
    static final int MAXBUNCH       = 10000;  // max random rays per refresh

    static final int ABSENT         = -1;     // but yikes see below...
    static final int TRUE           = 1;
    static final int FALSE          = 0;
    static final int NOSYNTAXERR    = 0; 
    static final int PREERASE       = 1;
    static final int NOERASE        = 0;
    static final int THINLINE       = 0;
    static final int FATLINE        = 1;

    static final int IMAX           = 512;       // Chars EPanel charTable
    static final int JMAX           = 3600;      // Rows EPanel charTable
    static final int MAXFIELDS      = 100;
    static final int MAXSURFS       = 100;       // < JMAX
    // static final int MAXGROUPS      = MAXSURFS;  
    static final int MAXRAYS        = JMAX-3;    // < JMAX
    // note: 17 circles = 919 rays; 34 rings = 3571 rays.
    static final int MAXRINGS       = (int) (-0.5+Math.sqrt(12*MAXRAYS-3.)/6.);
    static final int MAXMEDIA       = 200;       // < JMAX
    static final int MAXGOALS       = 7;         // AutoAdjust
    // static final int MAXWFEGROUPS   = 100; 
    static final int MAXADJ         = 100; 
    static final int MAXMAP         = 100;  
    static final int MAXMP          = 9;      // multiplots per axis
    static final int EDITOPT        = 0; 
    static final int EDITRAY        = 1; 
    static final int EDITMED        = 2; 

/*-----------------char constants--------------------*/

    static final byte EOF        = -1;  
    static final char TAB        = '\t';  // 9
    static final char LF         = '\n';  // 10 for Unix and Java
    static final char CR         = '\r';  // 13 for Mac
    static final char SPACE      = ' ';   // 32
    static final char QUOTE      = (char) 34;
    static final char APOSTROPHE = (char) 39;
    static final char COMMA      = (char) 44;
    static final char COLON      = (char) 58;
    static final char SEMICOLON  = (char) 59; 
    static final char BACKSLASH  = (char) 92;
    static final char TILDE      = (char) 126; 
    static final char DEL        = (char) 127; 

/*--------------math constants--------------------------*/

    static final double TINY        = 1E-199;
    static final double TOL         = 5e-14;
    static final double EPS         = TOL; 
    static final double ZOOMOUT     = Math.sqrt(0.5); 
    static final double INF         = 9e+99;
    static final double BIGVAL      = 9.876543e+99; 
    static final double DELTA       = 1E-6;  // used in RT13 for f.d.
    static final double NEGZERO     = -0.0; 
    static final double TWOPI       = 2.0*Math.PI;
    static final double ROOT2       = Math.sqrt(2.0); 
    static final double LN10        = Math.log(10.0);
    static final double BADVAL      = -9.876543E-210; 
    static final double PMAKEUP     = +2.00001;
    static final double MMAKEUP     = -2.00001;
    static final double STEREO      = 0.001;

/*--------------------editor iTextInfo[] macros---------------------*/

    static final int TITLE    = 0; 
    static final int HEADER   = 1; 
    static final int RULER    = 2; 


/*--------------------graphics constants---------------------------*/

    static final int NTICKS       = 0;   // index ruler results[]
    static final int NFRACDIGITS  = 1;   // index ruler results[]
    static final int MAXTICKS     = 10;  // maximum number allowed ticks
    static final int TPP          = 16;  // twips per pixel
    static final int WCHAR        = 7;   // X pixels
    static final int HCHAR        = 12;  // Y pixels
    static final int WCARET       = 4;   // blinking caret width
    static final int HCARET       = 10;  // blinking caret height
    static final int CHARDXPIX    = -2;  // pixels; + moves rightward
    static final int CHARDYPIX    = +4;  // pixels; + moves downward
    static final int MINTICKS     = 2;   // minimum ticks per ruler
    static final int MAXSEGS      = 100; // for layout; 2<nsegs<MAXSEGS.

    static final double MINSIZE      = 1.5e-6; // plot & layout sanity?
    static final double MAXSIZE      = 1.5e+6; // plot & layout sanity?
    static final double DENSE        = 1.60;   // heavy if refraction>DENSE
    static final double PLOTTICKFRAC = 0.015;  // plot iristick/opticspan
    // static final double DOTTEDFRAC   = 0.04;   // extension/opticspan
    static final double TICKHEIGHT   = 0.015;  // rulerticks/unitsquare

/*---------------LMITER constants-------------------------*/


    static final int DOWNITER  = 0;    // iteration ok, want more
    static final int LEVELITER = 1;    // iteration ok, all done.  
    static final int MAXITER   = 2;    // did enough iterations. 
    static final int BADITER   = 3;    // ray killed bail out

    static final String sIter[] = {"ok iter", "level iter", "bad iter"}; 
    
/*--------------mouse graphics request constants------------*/

    static final boolean bSKELETON = false; 
    static final boolean bFULLART  = true; 

/*----table designator strings do not fully replace getAttribute()---*/

    static final String SURFCHARS = "XYZTPRASC";        // for MapPanel 
    static final String RAYSTARTCHARS = "XYZUVWxyzuvw"; // for MapPanel


/*-------------------RunRay Tstring[] error codes---------------*/
/*-----these could be simplified going to a 4x4 SURFxRAY plan---*/
/*---have them pure ray errors, not mingled with surface IDs----*/

    static final int RROK  =  0;    // general no-failure error code
    
    static final int RRMIS =  1;    // ray missed surface.
    static final int RRBAK =  2;    // ray intercepted but behind start.
    static final int RRBRA =  3;    // bracket intercept fail
    
    static final int RRDIA =  4;    // Diameter: obj or iris
    
    static final int RRdia =  5;    // inner diameter: obj or iris
    static final int RRSPI =  6;    // spider leg    
    
    static final int RRORD =  7;    // redirection fail diffraction order
    static final int RRTIR =  8;    // redirection fail total internal reflection
    static final int RRARR =  9;    // array setup invalid
    static final int RRBXO = 10;    // bimodal crossover failure
    static final int RRTER = 11;    // terminate the trace at redirect() stage
    static final int RRUNK = 12;    // unknown or blf/blm, or not implemented    
    
    // sResults[] are used by InOut to explain result[]
    // Needs a special code for NonNumericalWavelength.
    static final String[] sResults = {   
    "OK",        // 0
    "mis",       // 1
    "bak",       // 2
    "bra",       // 3
    "Dia",       // 4
    "dia",       // 5
    "Spi",       // 6
    "Ord",       // 7
    "TIR",       // 8
    "Arr",       // 9
    "BXO",       // 10
    "Ter",       // 11
    "Unk"};      // 12
   
    // the 22 codex values are listed here...
    static final int UNIOK = 0; 
    static final int UNIIF = 1;
    static final int UNIPO = 2; // Pupil Outside fail
    static final int UNIPI = 3; // Pupil Inside fail
    static final int UNIRF = 4; // Redirection Failure
    
    static final int BMOK  = 5;
    static final int BMIF  = 6;
    static final int BMPO  = 7; 
    static final int BMPI  = 8; 
    static final int BMRF  = 9; 
    
    static final int BLFOK = 10;
    static final int BLFIF = 11; 
    static final int BLFPO = 12; 
    static final int BLFPI = 13; 
    static final int BLFRF = 14;
    
    static final int BLBOK = 15;
    static final int BLBIF = 16; 
    static final int BLBPO = 17;
    static final int BLBPI = 18;
    static final int BLBRF = 19; 
    
    static final int BTOK  = 20; // bimodal terminator ray hit & kill 
    static final int BTIF  = 21; //
    static final int BTPO  = 22;
    static final int BTPI  = 23;
    static final int BTRF  = 24; 
    
    static final int BLBXO = 25;  // bimpdal crossover failure 
    
    static final int CODEXMULT = 5; // since 5 categories of ray failures

    static final String[] sCodex = {
    "UNIOK",    // 0
    "UNIIF",    // 1
    "UNIPO",    // 2
    "UNIPI",    // 3
    "UNIRF",    // 4
    
    "BMOK",     // 5
    "BMIF",     // 6
    "BMPO",     // 7
    "BMPI",     // 8
    "BMRF",     // 9
    
    "BLFOK",    // 10
    "BLFIF",    // 11
    "BLFPO",    // 12
    "BLFPI",    // 13
    "BLFRF",    // 14
    
    "BLBOK",    // 15
    "BLBIF",    // 16
    "BLBPO",    // 17
    "BLBPI",    // 18
    "BLBRF",    // 19
    
    "BTOK",     // 20  bimodal terminator ray hit & kill
    "BTIF",     // 21  bimodal terminator ray miss & bypass
    "BTPO",     // 22  bimodal terminator ray OD bypass
    "BTPI",     // 23  bimodal terminator ray ID bypass
    "BTRF",     // 24  bimodal terminator ray redirect fail: SNH
    
    "BLBXO"};   // 25 bimodal crossover failure


/*------------ generic editor status[] index macro defs ------------*/

    static final int GPRESENT         = 0; // 0=absent=FALSE, 1=present=TRUE
    static final int GNLINES          = 1; 
    static final int GNRECORDS        = 2; // = MIN(guideNumber, nlines-3)
    static final int GNFIELDS         = 3; 
    static final int NGENERIC         = 4;  

/*------------ giFlags[] parser index macro defs ---------------------*/

    static final int OPRESENT         = 0; // 0=absent=FALSE, 1=present=TRUE
    static final int ONLINES          = 1; // generic
    static final int ONSURFS          = 2; // generic
    // static final int ONGROUPS         = 3; // new; testing...
    static final int ONFIELDS         = 4; // generic
    static final int OMEDIANEEDED     = 5; // 0=FALSE=okWithout, 1=TRUE=needsMedia.
    static final int OGRATINGPRESENT  = 6; // 1=present 
    static final int ONADJ            = 7; // 0=no adjustables present. 
    static final int OALLDIAMSPRESENT = 8; // 0=FALSE; 1=TRUE
    static final int OSYNTAXERR       = 9; // 0=OK, else badField+100*badLine

    static final int RPRESENT         = 10; // 0=absent=FALSE, 1=present=TRUE
    static final int RNLINES          = 11;
    static final int RNRAYS           = 12; 
    static final int RNFIELDS         = 13; 
    static final int RNGOALS          = 14; // how many goals + WFE if present
    static final int RWFEFIELD        = 15; // -1=absent, else present.
    static final int RNWFEGROUPS      = 16; // parse result = nWFE groups; goals=0
    static final int RNADJ            = 17; // 0=no adjustables present.
    static final int RWAVEFIELD       = 18; // ABSENT=-1 else field num
    static final int RALLWAVESPRESENT = 19; // 0=absent=false; 1=present=true.
    static final int RALLWAVESNUMERIC = 20; // 1=allNumeric, else 0=mixed
    static final int RUVWCODE         = 21; 
    static final int RNRAYADJ         = 22; // how many adjustables in ray #1
    static final int RAYADJ0          = 23; // raystart attrib of adj 0
    static final int RAYADJ1          = 24; // raystart attrib of adj 1
    static final int RAYGOALATT0      = 25; // raygoal attrib
    static final int RAYGOALATT1      = 26; // raygoal attrib
    static final int RAYGOALFIELD0    = 27; // raygoal field 
    static final int RAYGOALFIELD1    = 28; // raygoal field 
    static final int RSYNTAXERR       = 29; // 0=OK, else badField+100*badLine
    static final int MPRESENT         = 30; // 0=absent=FALSE, 1=present=TRUE
    static final int MNLINES          = 31;
    static final int MNGLASSES        = 32; // how many glasses
    static final int MNFIELDS         = 33; // how many fields
    static final int MNWAVES          = 34; // how many wavelengths
    static final int MSYNTAXERR       = 35; // 0=OK, else badField+100*badLine
    static final int STATUS           = 36; // overall result from Gparse, see below.
    static final int NFLAGS           = 37; // this will grow

/*----------------Error ID Values in giFlags[GERROR]---------------------*/

    static final int GUNKNOWN          = 0; // undermined state
    static final int GNOFILES          = 1; // no files present
    static final int GOABSENT          = 2; // optics table is absent
    static final int GOEMPTY           = 3; // optics table is empty
    static final int GLAYOUTONLY       = 4; // optics has all diams
    static final int GOSYNTAXERR       = 5; // 

    static final int GRABSENT          = 6; // ray table is absent
    static final int GREMPTY           = 7; // ray table is empty
    static final int GRLACKWAVE        = 8; // lacks needed @wavel
    static final int GRSYNTAXERR       = 9; //

    static final int GMABSENT          = 10; // media table is absent
    static final int GMEMPTY           = 11; // media table is empty
    static final int GMSYNTAXERR       = 12; //

    // Some special cases where each table is OK, yet group is inconsistent...

    static final int GOGLASSABSENT     = 13; //
    static final int GRWAVEABSENT      = 14;
    static final int GRNEEDNUMERWAVES  = 15; // true=1, false=0.
    static final int GPARSEOK          = 16; // overall parse = OK
    static final int NEXPLANATIONS     = 17;

    static final String[] sExplanations = {
    "",                                    // 0 = unknown status
    "No files",                            // 1
    "Optics file is absent",               // 2
    "Optics file is empty",                // 3
    "No rays, but Layout is allowed",      // 4
    "Optics syntax error",                 // 5
    "Ray table is absent",                 // 6
    "Ray table is empty",                  // 7
    // "Ray table lacks @wavelength",      // 8
    "Unknown refractive index",            // 8
    "Ray syntax error",                    // 9
    "Media table is absent",               // 10
    "Media table is empty",                // 11
    "Media syntax error",                  // 12
    "Media lacks glass name: ",            // 13
    "Media lacks ray wavelength: ",        // 14
    "Grating needs numerical wavelengths", // 15
    "OK"};                                 // 16


/*----------------------optics table column attribute fields-------------*/

    static final int OABSENT   = -1; 
    static final int OREFRACT  =  1; // refractive index approaching surface j
    static final int OX        =  2;
    static final int OY        =  3;
    static final int OZ        =  4;
    static final int OTILT     =  5;
    static final int OPITCH    =  6;
    static final int OROLL     =  7;

    static final int OCURVE    =  9; // axisymmetric curvature
    static final int OCURVX    = 10; // curvature in x: cyl, toric, biconic
    static final int OCURVY    = 11; // curvature in y; cyl, toric, biconic   
    static final int ORAD      = 12; // radius of curvature; converted in OEJIF.
    static final int ORADX     = 13; // radius of curvature; converted in OEJIF.
    static final int ORADY     = 14; // radius of curvature; converted in OEJIF

    static final int OASPHER   = 15; // axisymmetric asphericity; RT13
    static final int OASPHX    = 16; // biconic only
    static final int OASPHY    = 17; // biconic only
    static final int OSHAPE    = 18; // unknown in RT13. Converted in OEJIF.

    static final int OGROUP    = 19; // group identifier column

    static final int OA1       = 20; // polynomial coefficients
    static final int OA2       = 21;
    static final int OA3       = 22;
    static final int OA4       = 23;
    static final int OA5       = 24;
    static final int OA6       = 25;
    static final int OA7       = 26;
    static final int OA8       = 27;
    static final int OA9       = 28;
    static final int OA10      = 29;
    static final int OA11      = 30;
    static final int OA12      = 31;
    static final int OA13      = 32;
    static final int OA14      = 33;



    
/* =======HETTRICK  22 MARCH 2016==========
   
   So, in summary, I (personally) would be happy as a clam with the 
   following (minimized) number of inputs:
  
   Gy, VY01, VY02, VY03, Gx, VX10, VX20, VX30,
   VY10, VY20, VY30, VY11, VY12, VY21

   Then BEAM4 internally uses the following formula to determine the 
   corresponding 6 constants needed for computing dN/dx:

   VX(i,j) = [(i+1)/j] * VY(i+1,j-1)  for  all  j .ne. 0

   resulting in the following:

   VX01 = VY10
   VX02 = 1/2 VY11
   VX03 = 1/3 VY12
   VX11 = 2 VY20
   VX12 = VY21
   VX21 = 3 VY30    
   
   ==== added 4-5 June 2016: fourth power ====
    Know what?
    Numbers are cheap.
    I'll stick in the whole group going to power-sum = 4:
    VX40, VY40, VY04, VY13, VY22, VY31 user specifiable via .OPT
    plus internally calculated  VX13, VX22, VX31, VX04.
*/    

    static final int OORDER    = 34;  // any of this group triggers OGROOVY; see OEJIF.
    static final int OGX       = 35;  // explicit; user synonyms "GX" or "VX00"
    static final int OGY       = 36;  // explicit; user synonyms "GY" or "VY00"

    static final int OVX01     = 37;  // implicit; see RT13::setEulers().
    static final int OVX02     = 38;  // implicit
    static final int OVX03     = 39;  // implicit
    static final int OVX04     = 40;  // implicit
    static final int OVX10     = 41;  // "VX10"; straight groove
    static final int OVX11     = 42;  // implicit
    static final int OVX12     = 43;  // implicit
    static final int OVX13     = 44;  // implicit
    static final int OVX20     = 45;  // "VX20"; straight groove
    static final int OVX21     = 46;  // implicit
    static final int OVX22     = 47;  // implicit
    static final int OVX30     = 48;  // "VX30"; straight groove
    static final int OVX31     = 49;  // implicit
    static final int OVX40     = 50;  // "VX40"; straight groove
    
    static final int OVY01     = 51;  // "VY01"; straight groove
    static final int OVY02     = 52;  // "VY02"; straight groove
    static final int OVY03     = 53;  // "VY03"; straight groove
    static final int OVY04     = 54;  // "VY04"; straight groove
    static final int OVY10     = 55;  // "VY10"
    static final int OVY11     = 56;  // "VY11"
    static final int OVY12     = 57;  // "VY12"
    static final int OVY13     = 58;  // "VY13"
    static final int OVY20     = 59;  // "VY20"
    static final int OVY21     = 60;  // "VY21"
    static final int OVY22     = 61;  // "VY22"
    static final int OVY30     = 62;  // "VY30"
    static final int OVY31     = 63;  // "VY31"
    static final int OVY40     = 64;  // "VY40"
    
    static final int ORGAUSS   = 65;  // rms radius of circular Gaussian
    static final int OHGAUSS   = 66;  // peak height of circular Gaussian
    
    static final int OHOEX1    = 71;
    static final int OHOEY1    = 72;
    static final int OHOEZ1    = 73;
    static final int OHOEX2    = 74;
    static final int OHOEY2    = 75;
    static final int OHOEZ2    = 76;
    static final int OHOELAM   = 77;
    static final int OGROOVY   = 78; // OGX to OHOELAM nonzero set this groovy flag.
    
    static final int OZ00      = 80; // Zernike coefficient 0: piston
    static final int OZ01      = 81;
    static final int OZ02      = 82; 
    static final int OZ03      = 83; 
    static final int OZ04      = 84; 
    static final int OZ05      = 85; 
    static final int OZ06      = 86; 
    static final int OZ07      = 87; 
    static final int OZ08      = 88; 
    static final int OZ09      = 89; 
    static final int OZ10      = 90; 
    static final int OZ11      = 91; 
    static final int OZ12      = 92; 
    static final int OZ13      = 93; 
    static final int OZ14      = 94; 
    static final int OZ15      = 95; 
    static final int OZ16      = 96; 
    static final int OZ17      = 97; 
    static final int OZ18      = 98; 
    static final int OZ19      = 99; 
    static final int OZ20      = 100; 
    static final int OZ21      = 101; 
    static final int OZ22      = 102; 
    static final int OZ23      = 103; 
    static final int OZ24      = 104; 
    static final int OZ25      = 105; 
    static final int OZ26      = 106; 
    static final int OZ27      = 107; 
    static final int OZ28      = 108; 
    static final int OZ29      = 109; 
    static final int OZ30      = 110; 
    static final int OZ31      = 111; 
    static final int OZ32      = 112; 
    static final int OZ33      = 113; 
    static final int OZ34      = 114; 
    static final int OZ35      = 115; 
    static final int ODIOP    = 116; //focal length of thin lens
    static final int OFINALADJ = 117; // final autoadjustable parameter

    static final int OTIRINDEX = 121;
    static final int OODIAOBS  = 122; // observed from trace
    static final int OIDIAM    = 123; // used by parser only
    static final int OIDIAX    = 124; // used by clients
    static final int OIDIAY    = 125; // used by clients
    static final int OODIAM    = 126; // used by parser only
    static final int OODIAX    = 127; // used by clients
    static final int OODIAY    = 128; // used by clients
    static final int OZMIN     = 129;
    static final int OZMAX     = 130;
    static final int OFFOX     = 131; // offset from vertex
    static final int OFFOY     = 132; // offset from vertex
    static final int OFFIX     = 133; // offset from vertex
    static final int OFFIY     = 134; // offset from vertex
    static final int OSCATTER  = 135; // scatter angle field degrees, Aug 2011 A128;  also A195.  
    static final int ONSPIDER  = 137; // number of spider legs
    static final int OWSPIDER  = 138; // width of spider leg
    static final int ONARRAYX  = 139;
    static final int ONARRAYY  = 140; 

    static final int OE11      = 141; // Eulers computed by OEJIF parser
    static final int OE12      = 142; // These convert lab to local.
    static final int OE13      = 143; // For local to lab, use transpose. 
    static final int OE21      = 144;
    static final int OE22      = 145;
    static final int OE23      = 146;
    static final int OE31      = 147;
    static final int OE32      = 148;
    static final int OE33      = 149;
    static final int OTYPE     = 150; // 0=lens, 1=mirror, 2=iris...
    static final int OFORM     = 151; // 0=ellip, 1=rect...
    static final int OPROFILE  = 152; // 0=plane, 1=conic...
    static final int ONPARMS   = 153; // array size.

/*-------------OTABLE strings for OEJIF diagnostic-----------*/

    static final String[] sOlabels = {
    " oRefract",    // 0 
    "       oX",    // 1
    "       oY",    // 2
    "       oZ",    // 3
    "    oTilt",    // 4
    "   oPitch",    // 5
    "    oRoll",    // 6
    " oAsphere",    // 7
    "   oShape",    // 8
    "   oCurve",    // 9
    "   oCurvX"};   // 10

/*----------------------OFORM attributes---------------------*/

    static final int OFELLIP     = 0;  // OFORM value from parser
    static final int OFIRECT     = 1;  // inner = rectangular
    static final int OFORECT     = 2;  // outer = rectangular
    static final int OFBRECT     = 3;  // both = rectangular

/*----------OTYPE attributes for OEJIF parser line 113-------*/

    static final int OTLENS      = 0;  // OTYPE value; default.
    static final int OTMIRROR    = 1;
    static final int OTRETRO     = 2;
    static final int OTIRIS      = 3;
    static final int OTDISTORT   = 4;  // OTYPE for WFE distorter
    static final int OTLENSARRAY = 5; 
    static final int OTMIRRARRAY = 6; 
    static final int OTIRISARRAY = 7; 
 // static final int OTSCATTER   = 8; 
    static final int OTGSCATTER  = 8;  // A195  Gaussian scatter type
    static final int OTUSCATTER  = 9;  // A195  Uniform scatter type
    static final int OTCBIN      = 10; // coordinate break input
    static final int OTCBOUT     = 11; // coordinate break output
    static final int OTBMIRROR   = 12; // bimodal mirror
    static final int OTBLFRONT   = 13; // bimodal lens front
    static final int OTBLBACK    = 14; // bimodal lens back
    static final int OTTERMINATE = 15; 
    static final int OTTHIN      = 16; // perfect thin lens
    static final int OTUNK       = 17; // unknown type; SNH.

    // phantom is merely an OTLENS with equal indices.
    // transmission grating is merely a groovy lens
    // reflection grating is merely a groovy mirror

    static final String sTypes[] = {
    "   lens",   // 0
    " mirror",   // 1
    "  retro",   // 2
    "   iris",   // 3
    "distort",   // 4
    "lensArr",   // 5  /// should be lens + OSOLVER=OSARRAY???
    "mirrArr",   // 6  /// nope, see RT13.dIntercept().
    "irisArr",   // 7  /// This is correct. 
    "g scat ",   // 8
    "u scat ",   // 9
    "cbInput",   // 10
    "cbOutput",  // 11
    "bimirror",  // 12
    "bilens_f",  // 13  bimodal lens front surface
    "bilens_r",  // 14  bimodal lens rear surface
    "terminate", // 15  bimodal terminator
    "thin",      // 16  perfect thin lens
    "unknown"};  // 17  SNH

/*-----------OPROFILE attributes and strings------------------*/

    static final int OSPLANO   = 0; 
    static final int OSCONIC   = 1; 
    static final int OSCONICLT = 2; 
    static final int OSCONICGT = 3; 
    static final int OSXCYL    = 4; 
    static final int OSXCYLLT  = 5; 
    static final int OSXCYLGT  = 6; 
    static final int OSYCYL    = 7; 
    static final int OSYCYLLT  = 8; 
    static final int OSYCYLGT  = 9; 
    static final int OSTORIC   = 10; 
    static final int OSPOLYREV = 11; 
    static final int OSZERNREV = 12;
    static final int OSZERNTOR = 13; 
    static final int OSBICONIC = 14;
    static final int OSARRAY   = 15;
    static final int OSGAUSS   = 16;
    static final int OSNFLAGS  = 17;   

    static final String[] sProfiles = {
    "Plano",     // 0 
    "Conic",     // 1
    "Conic<",    // 2
    "Conic>",    // 3
    "Xcyl",      // 4
    "Xcyl<",     // 5
    "Xcyl>",     // 6
    "Ycyl",      // 7
    "Ycyl<",     // 8
    "Ycyl>",     // 9
    "Toric",     // 10
    "PolyRev",   // 11
    "ZernRev",   // 12
    "ZernTor",   // 13
    "Biconic",   // 14
    "Array",     // 15
    "****Gauss****"};    // 16 



/*------ general ray index attribute macros; input only--------*/

    static final int RABSENT        =    -1; // absentee return code
    static final int RX             =     0; // labframe ray
    static final int RY             =     1; // labframe ray
    static final int RZ             =     2; // labframe ray
    static final int RU             =     3; // labframe ray
    static final int RV             =     4; // labframe ray
    static final int RW             =     5; // labframe ray
    static final int RPATH          =     6; // all frames

/*-------------- ray start (input) index attribute macrodefs-----------*/

    static final int RSWAVEL        =     7; // raystarts[][] only
    static final int RSCOLOR        =     8; // raystarts[][] only
    static final int RSORDER        =     9; // raystarts[][] only
    static final int RNSTARTS       =    10; // raystart[] attributes.

/*-------------- ray output index local attribute macrodefs--------------*/

    static final int RTXL           =     7; // dGetRay() local ray
    static final int RTYL           =     8; // dGetRay() local ray
    static final int RTZL           =     9; // dGetRay() local ray
    static final int RTUL           =    10; // dGetRay() local ray
    static final int RTVL           =    11; // dGetRay() local ray
    static final int RTWL           =    12; // dGetRay() local ray
    static final int RTANGLE        =    13; // dGetRay() global ray
    static final int RTNORMX        =    14; // dGetRay()
    static final int RTNORMY        =    15;  
    static final int RTNORMZ        =    16;  
    static final int RTWFE          =    17; // dGetRay() special.
    static final int RNATTRIBS      =    18; // how many ray output attribs

/*-----------------ray table special calculation codes--------------------*/

    static final int RNOTE          =   133; // text output only
    static final int RDEBUG         =   134; // text output only
    static final int RFINAL         = 10000; // placeholder for 100*nsurfs term
    static final int RGOAL          = 10100; // input/output goal term


/*---------------- quadlist opcode definitions--------------------------*/
/*-------------- codes 32 to 127 are ordinary ASCII chars --------------*/

    static final int NULL              = 0; // does nothing
    static final int BLACK             = 0; // add to DOT ... SETCOLOR
    static final int RED               = 1;
    static final int GREEN             = 2; 
    static final int YELLOW            = 3; 
    static final int BLUE              = 4; 
    static final int MAGENTA           = 5; 
    static final int CYAN              = 6; 
    static final int WHITE             = 7; 
    static final int LTGRAY            = 8;
    static final int DKGRAY            = 9;

    static final int SETWHITEBKG       = 10; // initialization only.
    static final int SETBLACKBKG       = 11; // initialization only.
    static final int SETSOLIDLINE      = 12; // x sets width: 0<x<5 points
    static final int SETDOTTEDLINE     = 13; // x sets width: 0<x<5 points
    static final int SETRGB            = 14; // xyz set RGB:  0<x<1 etc

    static final int MOVETO            = 20; // construct path using xx, yy
    static final int PATHTO            = 21; // construct path using xx, yy
    static final int STROKE            = 22; // draw current path and zero it
    static final int FILL              = 23; // shade current path and zero it
    static final int SETFONT           = 24; // x=size,points; y=0 or 1: bold
    static final int USERCONSTS        = 25; // x,y,z affine consts
    static final int USERSLOPES        = 26; // x,y,z affine slopes

    static final int DOT               = 130; // must add color 0..9
    static final int PLUS              = 140; // must add color 0..9
    static final int SQUARE            = 150; // must add color 0..9
    static final int DIAMOND           = 160; // must add color 0..9
    static final int SETCOLOR          = 170; // must add color 0..9, no xycode

    static final int COMMENTSHADE      = 222; // not drawn
    static final int COMMENTSURF       = 223;
    static final int COMMENTRAY        = 224;
    static final int COMMENTAXIS       = 225;
    static final int COMMENTRULER      = 226;
    static final int COMMENTDATA       = 227;
    static final int COMMENTANNO       = 228;
    static final int COMMENTFINISH     = 229; 


    //------------------ quadlist index macrodefs -----------------
    static final int QBASE   = 0; 
    static final int QBATCH  = 1; 
    static final int QRAND   = 2; 
    static final int QFINISH = 3; 
    static final int QANNO   = 4; 
    
    //------------ triplist definitions---------------------
    static final int MAXTRIPS = 10000; // max triplet, units=twips.


    //----------------special colors-------------------------
    static final Color LGRAY = new Color(192, 192, 192); 
    static final Color DGRAY = new Color(155, 155, 155); 
    static final Color LBLUE = new Color(192, 192, 255); 
    static final Color DRED  = new Color(155, 0, 0);

    //------run menu item macros and strings for GJIF & DMF----------------

    static final int RM_INOUT  = 0; 
    static final int RM_LAYOUT = 1;  // has graphic
    static final int RM_PLOT2  = 2;  // has graphic
    static final int RM_MPLOT  = 3;  // has graphic
    static final int RM_MAP    = 4;  // has graphic
    static final int RM_PLOT3  = 5;  // has graphic
    static final int RM_H1D    = 6;  // has graphic
    static final int RM_MTF    = 7;  // has graphic
    static final int RM_H2D    = 8;  // has graphic
    static final int RM_AUTOADJ= 9; 
    static final int RM_AUTORAY= 10; 
    static final int RM_RANDOM = 11; 
    static final int RM_DEMO   = 12;  // has demo
    static final int RM_NITEMS = 13; 

    static final String runItemStr[] = {
         "InOut", 
         "Layout", 
         "Plot2Dim", 
         "MultiPlot", 
         "Map", 
         "Plot3Dim", 
         "Histo1Dim", 
         "MTF", 
         "Histo2Dim", 
         "AutoAdjust", 
         "AutoRay", 
         "Random", 
         "Demo"}; 
    






//-------User Option Group Macros--------------
   
   static final int UO_NEWFILE = 0;  // skeleton options
   static final int UO_IO      = 1; 
   static final int UO_LAYOUT  = 2; 
   static final int UO_AUTO    = 3;
   static final int UO_PLOT2   = 4; 
   static final int UO_MPLOT   = 5;
   static final int UO_MAP     = 6; 
   static final int UO_PLOT3   = 7; 
   static final int UO_1D      = 8; 
   static final int UO_2D      = 9; 
   static final int UO_RAND    = 10; 
   static final int UO_CAD     = 11; 
   static final int UO_START   = 12; 
   static final int UO_EDIT    = 13;
   static final int UO_GRAPH   = 14;
   static final int UO_DEF     = 15; 
   static final int UO_1DRAY   = 16; 
   static final int UO_2DRRAY  = 17; 
   static final int UO_2DCRAY  = 18; 
   static final int UO_2DCGAUS = 19; 
   static final int UO_RECENTO = 20; 
   static final int UO_RECENTR = 21; 
   static final int UO_RECENTM = 22; 
   static final int NUOGROUPS  = 23; 

//--------UO strings: avoid "|" used in parsing-----------
//----UO strings is a ragged right array, 
//----with various lengths of groups in each heading.

   static final String UO[][][] = 
   {
       {  // group 0 = UO_NEWFILE
          {"Nrecords",             "15"},   // 0; nrows = Nrecords + 3
          {"Nfields",              "10"},   // 1
          {"FieldWidth",           "18"}    // 2
       },
         
       {  // group 1 = UO_IO
          {"Show RMS when goals exist", "T"}
       },

       {  // group 1 = UO_LAYOUT
          {"View Elevation",       "33.0"},  // 0
          {"View Azimuth",         "45.0"},  // 1
          {"Sticky pan/zoom?",        "F"},  // 2
          {"Arc Segments",           "10"},  // 3
          {"+X",                      "T"},  // 4 = vaxis Array! hard coded as 4!
          {"-X",                      "F"},  // 5
          {"+Y",                      "F"},  // 6
          {"-Y",                      "F"},  // 7
          {"+Z",                      "F"},  // 8
          {"-Z",                      "F"},  // 9
          {"Hruler",                  "T"},  // 10 axes show or not
          {"Vruler",                  "T"},  // 11
          {"Xaxis",                   "T"},  // 12
          {"Yaxis",                   "T"},  // 13
          {"Zaxis",                   "T"},  // 14
          {"White",                   "T"},  // 15
          {"Black",                   "F"},  // 16 formats
          {"Stereo",                  "F"},  // 17
          {"Parallax",                "5"},  // 18
          {"N",                       "T"},  // 19 arcs
          {"E",                       "T"},  // 20
          {"S",                       "T"},  // 21
          {"W",                       "T"},  // 22
          {"NE",                      "T"},  // 23
          {"SE",                      "T"},  // 24
          {"SW",                      "T"},  // 26
          {"NW",                      "T"},  // 26
          {"Refractor shading?",      "T"},  // 27
          {"Rays",                  "0.5"},  // 28 ray line width pixels
          {"Surfs",                 "1.5"},  // 29 surf line width pixels
          {"Axes",                  "0.5"},  // 30 axis line width pixels
          {"Sticky uxcenter",         "0"},  // 31
          {"Sticky uycenter",         "0"},  // 32
          {"Sticky uzcenter",         "0"},  // 33
          {"Sticky uxspan",           "0"},  // 34
          {"Sticky uyspan",           "0"},  // 35
          {"Sticky uzspan",           "0"},  // 36
          {"Refractor connectors?",   "F"},  // 37
          {"Retro visible?",          "T"},  // 38
          {"Dotted extension %",      "4"}   // 39
          
       },

       {  // group 2 = UO_AUTO
          {"Step size",            "1E-6"},    // 0
          {"Max Iter",              "100"},    // 1
          {"Tolerance",           "1E-12"},    // 2
          {"Weight Xgoal",          "1.0"},    // 3
          {"Weight Ygoal",          "1.0"},    // 4
          {"Weight Zgoal",          "1.0"},    // 5
          {"Weight Ugoal",          "1.0"},    // 6
          {"Weight Vgoal",          "1.0"},    // 7
          {"Weight Wgoal",          "1.0"}     // 8
       },

       {  // group 3 = UO_PLOT2
          {"Horiz Var",                  "yfinal"},  // 0
          {"Horiz Span",                       ""},  // 1
          {"Vert Var",                   "xfinal"},  // 2
          {"Vert Span",                        ""},  // 3
          {"Encoding Wavel",                   ""},  // 4
          {"Dot",                             "T"},  // 5
          {"Plus",                            "F"},  // 6
          {"Square",                          "F"},  // 7
          {"Diamond",                         "F"},  // 8
          {"Complete rays",                   "T"},  // 9  RROK
          {"Sufficient rays",                 "F"},  // 10 enough surfs
          // {"Additional surface, none if zero", ""},  // elim A207
          {"Black Background",                "F"}   // 11
       },

       {  // group 4 = UO_MPLOT  MultiPlot options

          {"Nplots horizontally",           "1"},  // 0  nH
          {"Var 1",                        "V0"},  // 1
          {"Values",                      "0.0"},  // 2
          {"Var 2",                          ""},  // 3
          {"Values",                         ""},  // 4
          {"Var 3",                          ""},  // 5
          {"Values",                         ""},  // 6

          {"Nplots vertically",             "1"},  // 7  nV
          {"Var 1",                        "U0"},  // 8
          {"Values",                      "0.0"},  // 9
          {"Var 2",                          ""},  // 10
          {"Values",                         ""},  // 11
          {"Var 3",                          ""},  // 12
          {"Values",                         ""},  // 13

          {"horiz var in each box",    "yfinal"},  // 14
          {"hor span, or blank=auto",        ""},  // 15
          {"vert var in each box",     "xfinal"},  // 16
          {"vert span, or blank=auto",       ""},  // 17
          {"BoxFraction of screen grid",  "0.7"},  // 18

          {"H",                             "F"},  // 19
          {"V",                             "F"},  // 20
          {"n",                             "F"},  // 21
          {"h",                             "F"},  // 22
          {"v",                             "F"},  // 23
          {"hh",                            "F"},  // 24
          {"v v",                           "F"},  // 25
          {"hv",                            "F"},  // 26
          {"s",                             "F"},  // 27
          {"m",                             "F"},  // 28

          {"Black Bkg",                     "F"},  // 29
          {"Round PlotBox",                 "F"},  // 30
          {"Skip any empty boxes",          "F"},  // 31
          {"Restrict plotted rays to box",  "F"}   // 32
       },


       {  // group 5 = UO_MAP
          {"rmsWFE",                "F"},  // 0
          {"pvWFE",                 "F"},  // 1
          {"rmsPSF",                "T"},  // 2
          {"rssPSF",                "F"},  // 3
          {"Variable",             "V0"},  // 4: horiz axis
          {"Step size",         "0.001"},  // 5
          {"How many points?",     "10"},  // 6
          {"Pixels per point?",     "5"},  // 7; no longer used
          {"Center: blank=auto",     ""},  // 8
          {"Parallax variable",      ""},  // 9
          {"Parallax step",          ""},  // 10
          {"Variable",             "U0"},  // 11, vert axis
          {"Step size",         "0.001"},  // 12
          {"How many points?",     "10"},  // 13
          {"Pixels per point?",     "5"},  // 14; no longer used
          {"Center: blank=auto",     ""},  // 15
          {"Parallax variable",      ""},  // 16
          {"Parallax step",          ""},  // 17
          {"Max % vignetting",     "33"},  // 18
          {"Width/Height",        "1.0"},  // 19
          {"Text output filename",   ""}   // 20
       },

       {  // group 6 = UO_PLOT3
          {"A Var",     "xfinal"},   // 0
          {"A Span",          ""},   // 1
          {"B Var",     "yfinal"},   // 2
          {"B Span",          ""},   // 3
          {"C Var",     "zfinal"},   // 4
          {"C Span",          ""},   // 5
          {"View Elev",     "33"},   // 6
          {"View Azim",     "45"},   // 7
          {"Encoding Wavel",  ""},   // 8
          {"Dot",            "T"},   // 9
          {"Plus",           "F"},   // 10
          {"Sqr",            "F"},   // 11
          {"Diam",           "F"},   // 12
          {"Complete rays",  "T"},   // 13
          {"Sufficient rays","F"},   // 14
          {"White",          "T"},   // 15
          {"Black",          "F"},   // 16
          {"Stereo",         "F"},   // 17
          {"Parallax",       "5"}    // 18
       },

       {  // group 7 = UO_1D
          {"Var",         "xfinal"},  // 0
          {"Nbins",          "256"},  // 1
          {"Automatic Bounds", "T"},  // 2
          {"Diameter Bounds",  "F"},  // 3
          {"Manual Bounds",    "F"},  // 4
          {"Manual Min",        ""},  // 5
          {"Manual Max",        ""},  // 6
          {"Show average",     "T"}   // 7
       },

       {  // group 8 = UO_2D
          {"Orchard Diagram",   "F"},  // 0
          {"Manhattan Diagram", "T"},  // 1
          {"Hor Var",      "yfinal"},  // 2
          {"Vert Var",     "xfinal"},  // 3
          {"H bins",           "32"},  // 4
          {"V bins",           "32"},  // 5
          {"View elev",        "22"},  // 6
          {"View Azim",        "22"},  // 7
          {"Automatic Bounds",  "T"},  // 8
          {"Diameter Bounds",   "F"},  // 9
          {"Manual Bounds",     "F"},  // 10
          {"Manual Hor Min",     ""},  // 11
          {"Manual Hor Max",     ""},  // 12
          {"Manual Vert Min",    ""},  // 13
          {"Manual Vert Max",    ""},  // 14
          {"White",             "T"},  // 15
          {"Black",             "F"},  // 16
          {"Stereo",            "F"},  // 17
          {"Parallax",          "5"}   // 18
       },
      

       {  // group 9 = UO_RAND
          {"Random rays per refresh", "100"},  // 0
          {"Stop at ray starts",    "99999"},  // 1
          {"or at ray finishes",    "99999"},  // 2
          {"Continuous?",               "T"},  // 3, xyz
          {"or Discrete?",              "F"},  // 4, xyz
          {"Continuous?",               "T"},  // 5, uvw
          {"or Discrete?",              "F"},  // 6, uvw
          {"Uniform?",                  "T"},  // 7
          {"Cosine?",                   "F"},  // 8
          {"Quartic Bell?",             "F"},  // 9
          {"Gaussian?*",                "F"},  // 10
          {"Lorentzian?*",              "F"},  // 11
          {"*Concentration=",           "4"}   // 12
       },  

       {  // group 10 = UO_CAD
          {"Postscript,  .EPS",   "T"},  // 0
          {"Plot size A,  .PLT",  "F"},  // 1
          {"Plot size B,  .PLT",  "F"},  // 2
          {"Plot size C,  .PLT",  "F"},  // 3
          {"Plot size D,  .PLT",  "F"},  // 4
          {"Plot size E,  .PLT",  "F"},  // 5
          {"2D DXF file,  .DXF",  "F"},  // 6
          {"3D DXF file,  .DXF",  "F"},  // 7
          {"Quads file,  .TXT",   "F"},  // 8
          {"Landscape",           "T"},  // 9
          {"Portrait",            "F"}   // 10
       },

       {  // group 11 = UO_START
          {"No AutoLoad",              "F"}, // 0, radio button
          {"AutoLoad most recent",     "T"}, // 1, radio button
          {"No AutoLoad",              "F"}, // 2, radio button
          {"AutoLoad most recent",     "T"}, // 3, radio button
          {"No AutoLoad",              "F"}, // 4, radio button
          {"AutoLoad most recent",     "T"}, // 5, radio button
          {"Start at User Home",       "F"}, // 6, radio button for File:Open
          {"Or most recent folder",    "T"}, // 7, radio button for File:Open
          {"RecentFolder",             "/"}, // 8, invisible field for File:Open
          {"Allow MacOS icons",        "F"}, // 9, radio button
          {"Forbid MacOS icons",       "T"}  // 10, radio button
          // Caution: an empty data field wreaks havoc with the registry.
          // Hence, I am using a solidus as a placeholder here.
          // DMF will replace it with default User.Home at startup,
          // and each subsequent file load will update to that folder. 
       },

       {  // group 12 = UO_EDIT
          {"Show file paths?",            "F"},  // 0
          {"Show row & col?",             "T"},  // 1
          {"Expand commas when reading?", "T"},  // 2
          {"Default fieldwidth, chars",  "10"},  // 3
          {"Colons: for text?",           "T"},  // 4
          {"Tabs: spreadsheets?",         "F"},  // 5
          {"Editor font size, points",   "14"},  // 6
          {"Editor bold font?",           "T"},  // 7 
          {"Editor pixel smoothing?",     "T"},  // 8
          {"Table mode?",                 "T"},  // 9
          {"or text mode?",               "F"}   // 10
       },

       {  // group 13 = UO_GRAPH
          {"To zoom in: pull wheel?",         "F"},  // 0
          {"or push wheel?",                  "T"},  // 1
          {"Graphic font size, pixels",      "12"},  // 2
          {"Graphic bold font?",              "T"},  // 3
          {"Annotation font size, pixels",   "18"},  // 4
          {"Annotation bold font?",           "T"},  // 5
          {"Initial window size, pixels",   "400"},  // 6
          {"Graphics pixel smoothing?",       "T"}   // 7
       },

       {  // group 14 = UO_DEF
          {"U",                        "F"},  // 0
          {"V",                        "F"},  // 1
          {"W",                        "T"},  // 2
          {"Positive",                 "T"},  // 3
          {"Negative",                 "F"},  // 4
          {"Volume",                   "T"},  // 5
          {"Isotropic U0",             "F"},  // 6
          {"Isotropic V0",             "F"},  // 7
          {"Isotropic W0",             "F"},  // 8
          {"Isotropic radius, deg", "10.0"}   // 9
       },

       {  // group 15 = UO_1DRAY
          {"X",             "T"},  // 0
          {"Y",             "F"},  // 1
          {"Z",             "F"},  // 2
          {"U",             "F"},  // 3
          {"V",             "F"},  // 4
          {"W",             "F"},  // 5
          {"1D Center",   "0.0"},  // 6
          {"1D Span",     "1.0"},  // 7
          {"1D Ncount",    "10"},  // 8
          {"Start at row",  "1"},  // 9
          {"ray starts?",   "T"},  // 10
          {"or ray goals?", "F"}   // 11
       },
 
       {  // group 16 = UO_2DRRAY
          {"X,Y",           "T"},  // 0
          {"X,Z",           "F"},  // 1
          {"Y,Z",           "F"},  // 2
          {"U,V",           "F"},  // 3
          {"U,W",           "F"},  // 4
          {"V,W",           "F"},  // 5
          {"2D Center1",   "0.0"},  // 6
          {"2D Span1",     "1.0"},  // 7
          {"2D Num1",       "10"},  // 8
          {"2D Center2",   "0.0"},  // 9
          {"2D Span2",     "1.0"},  // 10
          {"2D Num2",       "10"},  // 11
          {"Start at row",   "1"},  // 12
          {"ray starts?",    "T"},  // 13
          {"or ray goals?",  "F"}   // 14
       },

       {  // group 17 = UO_2DCRAY
          {"X,Y",                    "T"},  // 0
          {"X,Z",                    "F"},  // 1
          {"Y,Z",                    "F"},  // 2
          {"U,V",                    "F"},  // 3
          {"U,W",                    "F"},  // 4
          {"V,W",                    "F"},  // 5
          {"1st coord offset",     "0.0"},  // 6
          {"2nd coord offset",     "0.0"},  // 7
          {"Outer circle radius",  "1.0"},  // 8
          {"Number of circles",      "1"},  // 9
          {"Start at row",           "1"},  // 10
          {"ray starts?",            "T"},  // 11
          {"or ray goals?",          "F"}   // 12
       },

       {  // group 18 = UO_2DCGAUS
          {"X,Y",                    "T"},  // 0
          {"X,Z",                    "F"},  // 1
          {"Y,Z",                    "F"},  // 2
          {"U,V",                    "F"},  // 3
          {"U,W",                    "F"},  // 4
          {"V,W",                    "F"},  // 5
          {"1st coord offset",     "0.0"},  // 6
          {"2nd coord offset",     "0.0"},  // 7
          {"Outer circle radius",  "1.0"},  // 8
          {"Number of circles",      "1"},  // 9
          {"Start at row",           "1"},  // 10
          {"ray starts?",            "T"},  // 11
          {"or ray goals?",          "F"}   // 12
       },
       
       {  //  group 19 = UO_RECENTO
          {"RO0", ""},
          {"RO1", ""},
          {"RO2", ""},
          {"RO3", ""},
          {"RO4", ""},
          {"RO5", ""},
          {"RO6", ""},
          {"RO7", ""},
          {"RO8", ""},
          {"RO9", ""}
       },
       
       {  // group 20 = UO_RECENTR
          {"RR0", ""},
          {"RR1", ""},
          {"RR2", ""},
          {"RR3", ""},
          {"RR4", ""},
          {"RR5", ""},
          {"RR6", ""},
          {"RR7", ""},
          {"RR8", ""},
          {"RR9", ""}
       },

       {  // group 21 = UO_RECENTM
          {"RM0", ""},
          {"RM1", ""},
          {"RM2", ""},
          {"RM3", ""},
          {"RM4", ""},
          {"RM5", ""},
          {"RM6", ""},
          {"RM7", ""},
          {"RM8", ""},
          {"RM9", ""}
       }   
    };    
} //--------end of B4onstants.java---------------------

