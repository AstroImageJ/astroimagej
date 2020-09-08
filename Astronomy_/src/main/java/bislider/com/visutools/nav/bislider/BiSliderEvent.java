package bislider.com.visutools.nav.bislider;

import java.awt.*;

/**
 * The event a BiSlider can generate. <br>
 * <br>
 *
 * <table border=1 width = "90%">
 *
 *   <tr>
 *
 *     <td>
 *       Copyright 1997-2006 Frederic Vernier. All Rights Reserved.<br>
 *       <br>
 *       Permission to use, copy, modify and distribute this software and its documentation for educational, research and
 *       non-profit purposes, without fee, and without a written agreement is hereby granted, provided that the above copyright
 *       notice and the following three paragraphs appear in all copies.<br>
 *       <br>
 *       To request Permission to incorporate this software into commercial products contact Frederic Vernier, 19 butte aux
 *       cailles street, Paris, 75013, France. Tel: (+33) 871 747 387. eMail: Frederic.Vernier@laposte.net / Web site: http://www.limsi.fr/vernier/Individu/vernier
 *       <br>
 *       IN NO EVENT SHALL FREDERIC VERNIER BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL
 *       DAMAGES, INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF FREDERIC
 *       VERNIER HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.<br>
 *       <br>
 *       FREDERIC VERNIER SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *       MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE PROVIDED HERE UNDER IS ON AN "AS IS" BASIS, AND
 *       FREDERIC VERNIER HAS NO OBLIGATIONS TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.<br>
 *
 *     </td>
 *
 *   </tr>
 *
 * </table>
 * <br>
 * <b>Project related :</b> FiCell, FieldExplorer<br>
 * <br>
 * <b>Dates:</b> <br>
 *
 * <li> Creation : XXX<br>
 *
 * <li> Format : 01/11/2001<br>
 *
 * <li> Last Modif : 11/02/2003 <br>
 * <br>
 * <b>Bugs:</b> <br>
 *
 * <li> <br>
 * <br>
 * <b>To Do:</b> <br>
 *
 * <li> make a function returning a tokenizer with the segments.<br>
 * <br>
 *
 *
 * @author    Frederic Vernier, Frederic.Vernier@laposte.net
 * @version   1.4.1
 */

public class BiSliderEvent extends AWTEvent implements Colorizer, Cloneable {
  // the data
  private  double[][]  ColorArray  = null;

  static final long serialVersionUID = 6868457033542496338L;

  /**
  * duplicate the object
  **/
  public Object clone() throws CloneNotSupportedException{
    return super.clone();
  }

  /**
   * Constructor
   *
   * @param Source_Arg      Description of the Parameter
   * @param ColorArray_Arg  Description of the Parameter
   */
  public BiSliderEvent(
    BiSlider Source_Arg,
    double[][] ColorArray_Arg) {
    super(Source_Arg, 0);
    ColorArray = ColorArray_Arg;
  } // Constructor



  /**
   * @return the raw format of the color table
   */
  public double[][] getColorArray() {
    return ColorArray;
  } // getColorArray()



  /**
   * @return   the minimum value to colorize
   */
  public double getMinimum() {
    return ColorArray[0][0];
  } // getMinimum()



  /**
   * @return the maximum value to colorize
   */
  public double getMaximum() {
    return ColorArray[ColorArray.length - 1][1];
  } // getMaximum()



  /**
   * @param Value_Arg  Description of the Parameter
   * @return           the color associated with a value by this table of colors
   */
  public Color getColorForValue(
    double Value_Arg) {

    if (Value_Arg >= ColorArray[0][0] && Value_Arg <= ColorArray[ColorArray.length - 1][1]) {
      for (int k = 0; k < ColorArray.length; k++) {
        if (Value_Arg >= ColorArray[k][0] && Value_Arg <= ColorArray[k][1]) {
          return new Color((int)ColorArray[k][2]);
        }
      }
    }

    return null;
  } // getColorForValue()



  /**
   * @param Value_Arg  Description of the Parameter
   * @return           if the value is in the range of the colorization
   */
  public boolean isColorizable(
    double Value_Arg) {
    return (Value_Arg >= ColorArray[0][0] && Value_Arg <= ColorArray[ColorArray.length - 1][1]);
  } // isColorizable()



  /**
   * display the content of the color table
   *
   * @return   Description of the Return Value
   */
  public String toString() {
    StringBuffer  Text1  = new StringBuffer("\n");

    for (int k = 0; k < ColorArray.length; k++) {
      Color  Color1  = new Color((int)ColorArray[k][2]);
      Text1.append(ColorArray[k][0] + "<->" + ColorArray[k][1] + " with R=" + Color1.getRed() + ", G=" + Color1.getGreen() + ", B=" + Color1.getBlue() + "\n");
    }

    return Text1.toString();
  } // toString()


}
/*
 * ColorisationEvent.java
 */

