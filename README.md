# AstroImageJ

Merged project of Dr. Karen Collins' AstroImageJ.

## Developer Instructions

### General Use

Clone the repo locally, then run `gradlew packageAij[For<Your-OS-Here>]` (leave off what is in square brackets to
generate all of them) in the repo's folder. The built zip(s) will appear in your-project-root/build/distributions. To
build only ij.jar or astronomy_.jar, run `gradlew :ij:build` or `gradlew :Astronomy_:build`, respectively.

To run AIJ, use `gradlew aijRun`. The generated directory, `AIJ-Run`, will contain the generated version of AIJ.

#### Special Options
The tasks `copyBuiltJars` and `runAij` are configured through text files in the project root, `jarLocation.txt` and
`devLaunchOptions.txt`, respectively. Both files are ignored by git and will not be commited to the repo. 
***Spelling and case sensitive.***

`jarLocation.txt` contains the absolute path to an AIJ distribution, so that development builds can be tested on a 
medium-term basis without using Gradle or and IDE to update it each time. When developing in Intellij Idea, if this file
is present, the `copyBuiltJars` task will be run automatically when the hammer icon is clicked.

`devLaunchOptions.txt` contains the Java launch arguments, eg. `-Xmx5000m -Xms256m`, the same as the config file `AstroImageJ.cfg` does for 
normal installations of AIJ. If present, these options are used in place of the defaults for `runAij`.

### Use in Intellij Idea:

In Idea, go to File > New > Project From Version Control (If it has not opened to a project, select 'Get from Version
Control' on the Welcome window.)
Enter https://github.com/keastrid/astroimagej for the URL, and direct the path to where you want the project, then
click 'clone'. If you had a project open, it will ask you which window to open AIJ in, choose your preference.

Once open, it may notify you that it doesn't have a JDK - if that is so, click the bar and point AIJ to your JDK, or you
can have it install one for you.

On the right-hand side, you will see a button labelled 'Gradle' with a little elephant on top. Clicking it wil open the
Gradle sidebar. Navigate to AIJ-Merged > Tasks > build > astroimagej development. Double-click on `runAij` to build and
run the project. To build the project, double-click on any of the `packageAij` tasks. The built zip(s) will appear in
your-project-root/build/distributions. To build only ij.jar or astronomy_.jar, navigate to the build commands under
their respective modules (instead of Tasks > ..., it will be ij > ... or Astronomy_ > ...). The built jar will be in
your-project-root/<ij or Astronomy_>/build/libs.

# Code Signing Policy
## Windows
Free code signing provided by [SignPath.io](https://signpath.io/), certificate by [SignPath Foundation](https://signpath.org/).

# Privacy Policy
This program will not transfer any information to other networked systems unless specifically requested by the user or the person installing or operating it.

# Credits
## Icons
### AIJ icon
The AIJ icon is a modified version of [this image](https://esahubble.org/images/heic0504d/) from ESA/Hubble,
[Creative Commons Attribution 4.0 International](https://creativecommons.org/licenses/by/4.0/)

Credit:
[NASA](http://www.nasa.gov/), [ESA](http://www.esa.int/), J. Clarke (Boston University, USA), and Z. Levay ([STScI](http://www.stsci.edu/))

The image was modified to remove the planet shadow.

### Other icons
Other icons are separately licensed. Please see the license files in the icon folders for more information.

## Included libraries
### ImageJ
A modified version of [ImageJ](https://github.com/imagej/ImageJ), which is in the 
[public domain](https://github.com/imagej/ImageJ?tab=License-1-ov-file).

### [Nom.tam.fits](https://github.com/nom-tam-fits/nom-tam-fits)
Licensed in the [public domain](https://github.com/nom-tam-fits/nom-tam-fits?tab=Unlicense-1-ov-file).

### [Nashorn](https://github.com/openjdk/nashorn)
Licensed under [GPL-2.0 with classpath exception](https://github.com/openjdk/nashorn/blob/main/LICENSE).

### [Pdfbox-graphics2d](https://github.com/rototor/pdfbox-graphics2d)
Licensed under [Apache 2.0](https://github.com/rototor/pdfbox-graphics2d?tab=readme-ov-file#licence).

### [JNA](https://github.com/java-native-access/jna)
Licensed under [Apache 2.0](https://github.com/java-native-access/jna?tab=License-1-ov-file#readme).

### [Sigstore-java](https://github.com/sigstore/sigstore-java/tree/main)
Licenced under [Apache 2.0](https://github.com/sigstore/sigstore-java/tree/main?tab=Apache-2.0-1-ov-file#readme).

### [FastDoubleParser](https://github.com/wrandelshofer/FastDoubleParser)
Licensed under [MIT license](https://github.com/wrandelshofer/FastDoubleParser?tab=MIT-1-ov-file#readme).

### [Hipparchus](https://github.com/Hipparchus-Math/hipparchus)
Licensed under [Apache 2.0](https://github.com/Hipparchus-Math/hipparchus?tab=Apache-2.0-1-ov-file).

### [JAMA](https://math.nist.gov/javanumerics/jama/)
Licensed in the [public domain](https://math.nist.gov/javanumerics/jama/).

### [BiSlider](https://perso.limsi.fr/vernier/BiSlider/)
All Rights Reserved, with permission to use, copy, modify and distribute this software and its documentation 
for educational, research and non-profit purposes. See the [full license here](https://perso.limsi.fr/vernier/BiSlider/).

For AstroImageJ, additional permission under GNU GPL version 3 section 7 is granted:
If you modify this Program, or any covered work, by linking or combining it with BiSlider (or a modified version of that library),
containing parts covered by the terms of BiSlider's license, the licensors of this Program (AstroImageJ) grant you 
additional permission to convey the resulting work.

See the [GPL FAQ for details on use of GPL incompatible libraries in a GPL work](https://www.gnu.org/licenses/gpl-faq.en.html#GPLIncompatibleLibs).

### [Json Simple](https://github.com/fangyidong/json-simple)
Licensed under [Apache 2.0](https://github.com/fangyidong/json-simple?tab=Apache-2.0-1-ov-file).