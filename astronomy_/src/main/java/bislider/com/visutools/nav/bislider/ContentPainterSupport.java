package bislider.com.visutools.nav.bislider;

import java.util.Vector;
import java.awt.*;

/**
 * The support object to store registered listeners and fire events. <br>
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
 * <li> Last Modif : 15/07/2005<br>
 * <br>
 * <b>Bugs:</b> <br>
 *
 * <li> <br>
 * <br>
 * <b>To Do:</b> <br>
 * <br>
 *
 *
 * @author    Frederic Vernier, Frederic.Vernier@laposte.net
 * @version   1.4.1
 */

public class ContentPainterSupport implements java.io.Serializable {
  //---------- MODIFIERS|-------------------- Type|----------------------------------------------- Name = Init value
  protected final static  javax.swing.text.html.parser.ParserDelegator  MAXIMUM_VARIABLE_SIZE_FOR_NAME  = null;

  protected               Vector<ContentPainterListener>                ContentPainterListeners         = new Vector<ContentPainterListener>();

  final static            long                                          serialVersionUID                = -1497023291489642695L;


  /**
   * Adds a feature to the ContentPainterListener attribute of the ContentPainterSupport object
   *
   * @param l  The feature to be added to the ContentPainterListener attribute
   */
  public synchronized void addContentPainterListener(ContentPainterListener l) {
    // add a listener if it is not already registered
    if (!ContentPainterListeners.contains(l)) {
      ContentPainterListeners.addElement(l);
    }
  } // addContentPainterListener()


  /**
   * Description of the Method
   *
   * @param l  Description of the Parameter
   */
  public synchronized void removeContentPainterListener(ContentPainterListener l) {
    // remove it if it is registered
    if (ContentPainterListeners.contains(l)) {
      ContentPainterListeners.removeElement(l);
    }
  } // removeContentPainterListener()


  /**
   * return the number of listeners
   */
  public synchronized int getPainterListenerNumber() {
    return ContentPainterListeners.size();
  } // removeContentPainterListener()


  /**
   * fire the event asynchronously
   *
   * @param Source_Arg             Source of the event. Should be the BiSlider instance
   * @param Graphics_Arg           The graphics object to draw with
   * @param Min_Arg                The minimum value of this piece of data
   * @param Max_Arg                The maximum value of this piece of data
   * @param Index_Arg              The index of the segment
   * @param Color_Arg              The color normally associated with these values
   * @param Rectangle_Arg          The rectangle to fill with painting
   * @param BoundingRectangle_Arg  The rectangle in which the other rectangle is
   */
  public void fireAsyncPaint(
    final Object Source_Arg,
    final Graphics Graphics_Arg,
    final double Min_Arg,
    final double Max_Arg,
    final int Index_Arg,        
    final Color Color_Arg,
    final Rectangle Rectangle_Arg,
    final Rectangle BoundingRectangle_Arg) {
    Thread  Thread1  =
      new Thread() {
        public void run() {
          internFireAsyncPaint(Source_Arg, Graphics_Arg, Min_Arg, Max_Arg, Index_Arg, Color_Arg, Rectangle_Arg, BoundingRectangle_Arg);
        }
      };
    Thread1.start();
  } // fireAsyncPaint()


  /**
   * fire the event asynchronously
   *
   * @param Source_Arg      Source of the event. Should be the BiSlider
   * @param Graphics_Arg    
   * @param Min_Arg                The minimum value of this piece of data
   * @param Max_Arg                The maximum value of this piece of data
   * @param Index_Arg              The index of the segment
   * @param Color_Arg              The color normally associated with these values
   * @param Rectangle_Arg          The rectangle to fill with painting
   * @param BoundingRectangle_Arg  The rectangle in which the other rectangle is
   */
  private void internFireAsyncPaint(
    final Object Source_Arg,
    Graphics Graphics_Arg,
    double Min_Arg,
    double Max_Arg,
    int Index_Arg,    
    Color Color_Arg,
    Rectangle Rectangle_Arg,
    Rectangle BoundingRectangle_Arg) {

    Vector  Vector1;
    synchronized (this) {
      Vector1 = (Vector)ContentPainterListeners.clone();
    }

    // Fire the event to all listeners.
    int     count    = Vector1.size();
    for (int i = 0; i < count; i++) {
      ContentPainterListener  listener  = (ContentPainterListener)Vector1.elementAt(i);
      listener.paint(new ContentPainterEvent(Source_Arg, Graphics_Arg, Min_Arg, Max_Arg, Index_Arg, Color_Arg, Rectangle_Arg, BoundingRectangle_Arg));
    }
  } // internFireAsyncPaint()


  /**
   * fire the event
   *
   * @param Source_Arg      Source of the event. Should be the BiSlider
   * @param Graphics_Arg    
   * @param Min_Arg                The minimum value of this piece of data
   * @param Max_Arg                The maximum value of this piece of data
   * @param Index_Arg              The index of the segment
   * @param Color_Arg              The color normally associated with these values
   * @param Rectangle_Arg          The rectangle to fill with painting
   * @param BoundingRectangle_Arg  The rectangle in which the other rectangle is
   */
  public void firePaint(
    Object Source_Arg,
    Graphics Graphics_Arg,
    double Min_Arg,
    double Max_Arg,
    int Index_Arg,        
    Color Color_Arg,
    Rectangle Rectangle_Arg,
    Rectangle BoundingRectangle_Arg) {

    // Make a copy of the listener object vector so that
    // it cannot be changed while we are firing events.
    Vector  Vector1;
    synchronized (this) {
      Vector1 = (Vector)ContentPainterListeners.clone();
    }

    // Fire the event to all listeners.
    int     count    = Vector1.size();
    for (int i = 0; i < count; i++) {
      ContentPainterListener  listener  = (ContentPainterListener)Vector1.elementAt(i);
      listener.paint(new ContentPainterEvent(Source_Arg, Graphics_Arg, Min_Arg, Max_Arg, Index_Arg, Color_Arg, Rectangle_Arg, BoundingRectangle_Arg));
    }
  } // firePaint()
}

