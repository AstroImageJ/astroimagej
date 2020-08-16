/*
 * BiSliderInterpolationModeEditor.java
 *
 * Created on June 16, 2006, 8:51 PM
 *
 */
package bislider.com.visutools.nav.bislider;

import java.beans.*;

/**
 * describe the possibles values for the InterpolationMode property of the BiSlider for IDEs
 * @author vernier
 */
public class BiSliderInterpolationModeEditor  extends PropertyEditorSupport {
 /**
  * @return the possibles values for the InterpolationMode property of the BiSlider for IDEs
  */
  public String[] getTags() {
    String interpolationModes[] = { BiSlider.HSB, BiSlider.RGB, BiSlider.CENTRAL};
    return interpolationModes;
  }  

  
  /**
   * @return the text used by IDE to be inserted in generated code 
   */
  public String getJavaInitializationString() {  
    String s = "com.visutools.nav.bislider.BiSlider."+(String)getValue();
    return s;
  }
  
  
  /*
  public void setAsText(String text) throws IllegalArgumentException  {
    if (!(getSource() instanceof BiSlider))
      throw (new IllegalArgumentException("Source is not a BiSlider in this PropertyEditorSupport !"));

    if (text.equals(BiSlider.HSB))
      ((BiSlider)getSource()).setInterpolationMode(BiSlider.HSB);
    else if (text.equals(BiSlider.RGB))
      ((BiSlider)getSource()).setInterpolationMode(BiSlider.RGB);
    else if (text.equals(BiSlider.CENTRAL_BLACK))
      ((BiSlider)getSource()).setInterpolationMode(BiSlider.CENTRAL_BLACK);
    else throw (new IllegalArgumentException("Must be one of: HSB, RGB, CENTRAL_BLACK"));
  }*/
}

