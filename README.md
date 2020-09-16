# astroimagej
Merged project of Dr. Karen Collins' AstroImageJ.

# General Instructions
Clone the repo locally, then run `gradlew build` in the repo's folder. The built zip will appear in your-project-root/build/distributions.
To build only ij.jar or astronomy_.jar, run `gradlew :ij:build` or `gradlew :Astronomy_:build`, respectively.

# Use in Intellij Idea:
In Idea, go to File > New > Project From Version Control (If it has not opened to a project, select 'Get from Version Control' on the Welcome window.)
Enter https://github.com/keastrid/astroimagej for the URL, and direct the path to where you want the project, then click 'clone'.
If you had a project open, it will ask you which window to open AIJ in, choose your preference.

Once open, it may notify you that it doesn't have a JDK - if that is so, click the bar and point AIJ to your JDK, or you can have it install one for you.

On the right hand side, you will see a button labelled 'Gradle' with a little elephant on top. Clicking it wil open the Gradle sidebar. Navigate to AIJ-Merged > Tasks > build > build.
Double click on 'build' to build the project. The built zip will appear in your-project-root/build/distributions.
To build only ij.jar or astronomy_.jar, navigate to the build commands under their respective modules (instead of Tasks > ..., it will be ij > ... or Astronomy_ > ...).
The built jar will be in  your-project-root/<ij or Astronomy_>/build/libs.
