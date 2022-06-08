AstroImageJ

User settings are stored in <userhome>/.astroimagej/AIJ_Prefs.txt, unless a file named AIJ_Prefs.txt is present in
AstroImageJ's directory (the same folder as ij.jar. For Mac, this will be in AstroImageJ.app/resources. For Windows and
Linux this is the same directory as the application).

The AstroImageJ.cfg file is where Java arguments may be specified to control JVM behavior, such as maximum memory.
Some JVM arguments of note:
 - Max memory usage (consider using the GUI to set this option!)
   Specifies the maximum size (in bytes) of the heap. This value must be a multiple of 1024 and greater than 2 MB.
   Append the letter k or K to indicate kilobytes, m or M to indicate megabytes, or g or G to indicate gigabytes.
   The default value is chosen at runtime based on system configuration, the default for AIJ 75% of physical memory.
   The value in AstroImageJ.cfg takes precedent over the default value.
   For server deployments, -Xms and -Xmx are often set to the same value.
   The following examples show how to set the maximum allowed size of allocated memory to 80 MB using various units:
   -Xmx83886080
   -Xmx81920k
   -Xmx80m
   The -Xmx option is equivalent to -XX:MaxHeapSize.
- Change the display size of AIJ
  -Dsun.java2d.uiScale=1.0
  This option differs from the GUI version of display scaling in that it effects all of AIJ's windows.
- You may find a more complete list available here: https://docs.oracle.com/en/java/javase/17/docs/specs/man/java.html#overview-of-java-options

AstroImageJ is distributed with a 64-bit version of Java.

Running with a system installation of Java:
  - Windows
    In launcher.ini, delete the line that starts with "jvm_path."
  - Linux
    Open AstroImageJ as a text file/shell script, and change "JAVA" to be "$JAVA_HOME."

Running with a specific Java installation:
  Java 17+ is required.
  You can obtain such distributions from here: https://www.azul.com/downloads/?package=jre
  The following options are needed:
    - Windows:
      Either:
        - Replace the jre folder with a valid JRE, being careful to match the directory structure of the original, or
        - Open launcher.ini as a text file and change the path of jvm_path to point to your Java installation.
    - Linux:
      Either:
        - Replace the jre folder with a valid JRE, being careful to match the directory structure of the original, or
        - Open AstroImageJ as a text file/shell script and change the path of "JAVA" to point to your Java installation.
    - Mac:
      - Replace the jre folder with a valid JRE, being careful to match the directory structure of the original

If 32-bit operation is desired, a valid 32-bit Java 17+ installation in needed and the instructions above can be followed.
32-bit operation is highly discouraged and will have no support.
You can find such distributions available here: https://www.azul.com/downloads/?architecture=x86-32-bit&package=jre

32-bit operation on MacOs is not available at this time due to lack of 32-bit Java versions for MacOs.
