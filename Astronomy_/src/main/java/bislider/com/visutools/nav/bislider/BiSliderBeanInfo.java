package bislider.com.visutools.nav.bislider;

import java.awt.*;
import java.beans.*;

/**
 * The bean info entry point.
 * <br><br>
 * <table border=1 width = "90%">
 *   <tr>
 *     <td>
 *       Copyright 1997-2006 Frederic Vernier. All Rights Reserved.<br>
 *       <br>
 *       Permission to use, copy, modify and distribute this software and its documentation for educational, research and
 *       non-profit purposes, without fee, and without a written agreement is hereby granted, provided that the above copyright
 *       notice and the following three paragraphs appear in all copies.<br>
 *       <br>
 *       To request Permission to incorporate this software into commercial products contact
 *       Frederic Vernier, 19 butte aux cailles street, Paris, 75013, France. Tel: (+33) 871 747 387.
 *       eMail: Frederic.Vernier@laposte.net / Web site: http://vernier.frederic.free.fr
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
 * <b>Project related :</b>  FiCell, FieldExplorer<br>
 * <br>
 * <b>Dates:</b><br>
 *   <li>Creation    : XXX<br>
 *   <li>Format      : 15/02/2004<br>
 *   <li>Last Modif  : 17/06/2006<br>
 *<br>
 * <b>Bugs:</b><br>
 *<br>
 * <b>To Do:</b><br>
 *<br>
 * @author Frederic Vernier, Frederic.Vernier@laposte.net
 * @version 1.4.1
 **/

public class BiSliderBeanInfo extends SimpleBeanInfo {
  private final static Class myClass = BiSlider.class;  
  
  /**
   * return the icon of the bean depending on the size asked
   */
  public Image getIcon(int iconKind) {
    java.awt.Image icon = null;
    switch (iconKind)
    {
      case ICON_COLOR_16x16:
        // The "/" is very important. It doesn't mean the image must
        // be found at the root of the HD but at the root of the
        // classpath entries (the jar of the bean is a kind of root then !)
        icon = loadImage("/images/BiSlider.png");
        break;

      case ICON_COLOR_32x32:
        // The "/" is very important. It doesn't mean the image must
        // be found at the root of the HD but at the root of the
        // classpath entries (the jar of the bean is a kind of root then !)
        icon = loadImage("/images/BiSliderL.png");
        break;

      default:
        icon = loadImage("/images/BiSliderVL.png");
        break;
    }
    return icon;
  }// getIcon()


  /**
   * return the set of editable properties
   */
  public PropertyDescriptor[] getPropertyDescriptors() {
    try {
      PropertyDescriptor pd1  = new PropertyDescriptor("ArcSize", myClass);
      PropertyDescriptor pd2  = new PropertyDescriptor("DecimalFormater", myClass);
      PropertyDescriptor pd3  = new PropertyDescriptor("DefaultColor", myClass);
      PropertyDescriptor pd4  = new PropertyDescriptor("Horizontal", myClass);
      PropertyDescriptor pd5  = new PropertyDescriptor("MaximumColor", myClass);
      PropertyDescriptor pd6  = new PropertyDescriptor("MaximumColoredValue", myClass);
      PropertyDescriptor pd7  = new PropertyDescriptor("MaximumValue", myClass);
      PropertyDescriptor pd8  = new PropertyDescriptor("MinOnTop", myClass);
      PropertyDescriptor pd9  = new PropertyDescriptor("MinimumColor", myClass);
      PropertyDescriptor pd10 = new PropertyDescriptor("MinimumColoredValue", myClass);
      PropertyDescriptor pd11 = new PropertyDescriptor("MinimumValue", myClass);
      PropertyDescriptor pd12 = new PropertyDescriptor("Precise", myClass);
      PropertyDescriptor pd13 = new PropertyDescriptor("SegmentSize", myClass);
      PropertyDescriptor pd14 = new PropertyDescriptor("SliderBackground", myClass);
      PropertyDescriptor pd15 = new PropertyDescriptor("Sound", myClass);      
      PropertyDescriptor pd16 = new PropertyDescriptor("UniformSegment", myClass);      
      PropertyDescriptor pd17 = new PropertyDescriptor("Unit", myClass);      
      PropertyDescriptor pd18 = new PropertyDescriptor("InterpolationMode", myClass);
      pd18.setPropertyEditorClass(BiSliderInterpolationModeEditor.class);
      PropertyDescriptor[] list = { pd1, pd2, pd3, pd4, pd5, pd6, pd7, pd8, pd9, pd10, pd11, pd12, pd13, pd14, pd15, pd16, pd17, pd18 };
      return list;
    } catch (IntrospectionException iexErr) {
      iexErr.printStackTrace();
      return super.getPropertyDescriptors();
      //throw new Error(iexErr.toString());
    }
  }// getPropertyDescriptors()
}

/* BiSliderBeanInfo.java */
