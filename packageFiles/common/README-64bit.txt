Preferences can be propagated forward by copying the hidden file
HOME/.image/IJ_Prefs.txt to HOME/.astroimage/AIJ_Prefs.txt 

To propagate memory and java virtual machine settings forward, copy the
ImageJ.cfg file from your old installation directory to AstroImageJ.cfg in your
new installation directory.

As with previous versions of AstroImageJ and ImageJ running under windows, if
64-bit operation is desired, the AstroImageJ.exe launcher will always configure
AstroImageJ with 32-bit java rather than 64-bit, if 32-bit java is installed on
the system. The first time it is started, AstroImageJ.exe creates a file named
AstroImageJ.cfg in the installation directory that contains these settings. To
enable 64-bit operation when both 32-bit and 64-bit Java are installed, edit
the AstroImageJ.cfg file that has been created to point to the 64-bit java
installation. As an example, if 32-bit java is installed at C:\Program Files
(x86)\Java\jre7\bin\javaw.exe and 64-bit java is installed at C:\Program
Files\Java\jre7\bin\javaw.exe, modify the AstroImageJ.cfg file as shown below
(in this typical configuartion, the only change needed  is to remove the
characters " (x86)" from the second line):