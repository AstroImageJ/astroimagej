Preferences can be propagated forward by copying the hidden file
HOME/.image/IJ_Prefs.txt to HOME/.astroimage/AIJ_Prefs.txt 

To propagate memory and Java virtual machine settings forward, copy the
ImageJ.cfg file from your old installation directory to AstroImageJ.cfg in your
new installation directory.

The AstroImageJ.cfg file is where Java arguments may be specified to control JVM behavior.

AstroImageJ is distributed with a 64-bit version of Java.

If 32-bit operation is desired, a valid 32-bit Java 17+ installation in needed.
You can find such distributions available here: https://www.azul.com/downloads/?architecture=x86-32-bit&package=jre
The following options are needed:
  - Windows:
    Either:
      - Replace the jre folder with a valid 32-bit JRE, being careful to match the directory structure of the original, or
      - Open launcher.ini as a text file and change the path of jvm_path to point to your Java installation.
  - Linux:
    Either:
      - Replace the jre folder with a valid 32-bit JRE, being careful to match the directory structure of the original, or
      - Open AstroImageJ as a text file/shell script and change the path of "JAVA" to point to your Java installation.
  - Mac:
    32-bit operation is not supported at this time due to lack of 32-bit Java versions for MacOs
