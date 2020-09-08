/*
 * adapter of the BiSlider Listener
 *
 * Created on September 7, 2006, 10:16 AM
 *
 */

package bislider.com.visutools.nav.bislider;

/**
 *
 * @author vernier
 */
public class BiSliderAdapter implements BiSliderListener{
  
  /** Creates a new instance of BiSliderAdapter */
  public BiSliderAdapter() {
  }
  
  /** something changed that modified the color gradient between min and max */
  public void newColors(BiSliderEvent BiSliderEvent_Arg) {}
  /**  min or max values changed  */  
  public void newValues(BiSliderEvent BiSliderEvent_Arg) {}
  /**  min selected value changed  */
  public void newMinValue(BiSliderEvent BiSliderEvent_Arg) {}
  /**  max selected value changed  */
  public void newMaxValue(BiSliderEvent BiSliderEvent_Arg) {}
  /**  selected segments changed  */
  public void newSegments(BiSliderEvent BiSliderEvent_Arg) {}
  
}
