Preferences can be propagated forward by copying the hidden file
HOME/.image/IJ_Prefs.txt to HOME/.astroimage/AIJ_Prefs.txt 

To propagate memory and java virtual machine settings forward, copy the
ImageJ.cfg file from your old installation directory to AstroImageJ.l4j.ini in your
new installation directory.


AstroImageJ is distributed with a 64-bit version of Java, and its launcher is also 64-bit.
32-bit compatibility is not guaranteed at this time.

If 32-bit operation is desired, the value of jvm_path in the launcher.ini file
must point to a 32-bit Java installation.