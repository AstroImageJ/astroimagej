package bislider.com.visutools.nav.bislider;

import java.awt.*;

/**
 * The event a BiSlider can generate. <br>
 * <br>
 *
 * <table border=1 width = "90%">
 *   <tr>
 *     <td>
 *       Copyright 1997-2006 Frederic Vernier. All Rights Reserved.<br>
 *       <br>
 *       Permission to use, copy, modify and distribute this software and its documentation for educational, research and
 *       non-profit purposes, without fee, and without a written agreement is hereby granted, provided that the above copyright
 *       notice and the following three paragraphs appear in all copies.<br>
 *       <br>
 *       To request Permission to incorporate this software into commercial products contact Frederic Vernier, 19 butte aux
 *       cailles street, Paris, 75013, France. Tel: (+33) 871 747 387. eMail: Frederic.Vernier@laposte.net / Web site: http://vernier.frederic.free.fr
 *       <br>
 *       IN NO EVENT SHALL FREDERIC VERNIER BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL
 *       DAMAGES, INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF FREDERIC
 *       VERNIER HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.<br>
 *       <br>
 *       FREDERIC VERNIER SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *       MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE PROVIDED HERE UNDER IS ON AN "AS IS" BASIS, AND
 *       FREDERIC VERNIER HAS NO OBLIGATIONS TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.<br>
 *     </td>
 *   </tr>
 * </table>
 * <br>
 * <b>Project related :</b> FiCell, FieldExplorer<br>
 * <br>
 * <b>Dates:</b> <br>
 * <li> Creation : 15/07/2005<br>
 * <li> Format : 15/07/2005<br>
 * <li> Last Modif : 15/07/2005 <br>
 * <br>
 * <b>Bugs:</b> <br>
 * <li> <br>
 * <br>
 * <b>To Do:</b> <br>
 * <br>
 *
 * @author    Frederic Vernier, Frederic.Vernier@laposte.net
 * @version   1.4.1
 */

public class ContentPainterEvent
   extends AWTEvent
   implements Cloneable {

  //---------- MODIFIERS|-------------------- Type|----------------------------------------------- Name = Init value
  protected final static  javax.swing.text.html.parser.ParserDelegator  MAXIMUM_VARIABLE_SIZE_FOR_NAME  = null;

  // the data
  protected               Graphics                                      Graphics1;
  protected               double                                        Min1;
  protected               double                                        Max1;
  protected               Color                                         Color1;
  protected               int                                           SegmentIndex;
  protected               Rectangle                                     Rectangle1;
  protected               Rectangle                                     BoundingRectangle1;
  final static            long                                          serialVersionUID                = 6868457033542496338L;


  /**
   * duplicate the object
   *
   * @return                                Description of the Return Value
   * @exception CloneNotSupportedException  Description of the Exception
   */
  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }


  /**
   * Constructor
   *
   * @param Source_Arg     Source of the event. Should be the BiSlider instance
   * @param Graphics_Arg   The graphics object to draw with
   * @param Min_Arg        The minimum value of this piece of data
   * @param Max_Arg        The maximum value of this piece of data
   * @param Index_Arg      The index of the segment
   * @param Color_Arg      The color normally associated with these values
   * @param Rectangle_Arg  The rectangle to fill with painting
   * @param BoundingRectangle_Arg  The rectangle in which the other rectangle is
   */
  public ContentPainterEvent(
    Object Source_Arg,
    Graphics Graphics_Arg,
    double Min_Arg,
    double Max_Arg,
    int Index_Arg,
    Color Color_Arg,
    Rectangle Rectangle_Arg,
    Rectangle BoundingRectangle_Arg) {

    super(Source_Arg, 0);
    
    SegmentIndex       = Index_Arg;
    Graphics1          = Graphics_Arg;
    Min1               = Min_Arg;
    Max1               = Max_Arg;
    Color1             = Color_Arg;
    Rectangle1         = Rectangle_Arg;
    BoundingRectangle1 = BoundingRectangle_Arg;
  } // Constructor


  /**
   * @return   the Graphics to draw the rectangle with
   */
  public Graphics getGraphics() {
    return Graphics1;
  } // getGraphics()
  
  /**
   * @return the Index of the segment we are drawing
   */
  public int getSegmentIndex() {
    return SegmentIndex;
  } // getSegmentIndex()  


  /**
   * @return   the minimum value to paint
   */
  public double getMinimum() {
    return Min1;
  } // getMinimum()


  /**
   * @return   the maximum value to paint
   */
  public double getMaximum() {
    return Max1;
  } // getMaximum()


  /**
   * @return   the default color to paint the rectangle
   */
  public Color getColor() {
    return Color1;
  } // getColor()


  /**
   * @return   the Rectangle, bounding box to paint something in
   */
  public Rectangle getRectangle() {
    return Rectangle1;
  } // getRectangle()


  /**
   * @return   the Bounding Rectangle, the segment in witch the rectangle is painted
   */
  public Rectangle getBoundingRectangle() {
    return BoundingRectangle1;
  } // getRectangle()


  /**
   * display the content of the color table
   *
   * @return   Description of the Return Value
   */
  public String toString() {
    return "ContentPainterEvent@" + hashCode() + " to paint " + Rectangle1 + " with " + Color1 + " to represent " + Min1 + "-" + Max1;
  } // toString()
} // ContentPainterEvent.java

