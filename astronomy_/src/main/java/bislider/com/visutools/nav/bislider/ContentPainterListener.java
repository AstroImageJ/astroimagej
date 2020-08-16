package bislider.com.visutools.nav.bislider;

/**
 * The listener interface for who want to receive events from the BiSlider. <br>
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
 * <li> <br>
 * <br>
 * <b>To Do:</b> <br>
 * <br>
 *
 * @author    Frederic Vernier, Frederic.Vernier@laposte.net
 * @version   1.4.1
 */

public interface ContentPainterListener extends java.util.EventListener {

  /**
   * ask a registered listener to paint himself the content of a rectangle for customisation purpose
   *
   * @param ContentPainterEvent_Arg  the event describing the event
   */
  public abstract void paint(ContentPainterEvent ContentPainterEvent_Arg);
}


