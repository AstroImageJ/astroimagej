package bislider.com.visutools.nav.bislider;

import java.io.*;
import java.awt.*;
import java.text.*;

/**
 * The abstraction (data, state and algorythms) of the bean. <br>
 * <br>
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
 *       cailles street, Paris, 75013, France. Tel: (+33) 871 747 387. eMail: Frederic.Vernier@laposte.net 1/ Web site: http://vernier.frederic.free.fr
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
 * <li> Format : 01/11/2001<br>
 * <li> Last Modif : 11/02/2003 <br>
 * <br>
 * <b>Bugs:</b> <br>
 * <li> big numbers close to the limit<br>
 * <br>
 * <b>To Do:</b> <br>
 * <li> multiple segments
 * <br>
 *
 * @author    Frederic Vernier, Frederic.Vernier@laposte.net
 * @version   1.4.1
 * @created   19 f�vrier 2004
 */
public class BiSliderAbstraction implements Serializable {

  //---------- MODIFIERS|-------------------- Type|----------------------------------------------- Name = Init value
  protected final static  javax.swing.text.html.parser.ParserDelegator  MAXIMUM_VARIABLE_SIZE_FOR_NAME  = null;

  static    final         long                                          serialVersionUID = 4827806780191894336L;

  // Attribut members of this bean
                          double                                        MinimumValue                    = 0;
                          double                                        MaximumValue                    = 100;
                          double                                        MinimumColoredValue             = MinimumValue;
                          double                                        MaximumColoredValue             = MaximumValue;
                          int                                           SegmentCount                    = 10;
                          double                                        SegmentSize                     = 10.0f;

                          // Used for painting the nn selected gap
                          Color                                         SliderBackground                = SystemColor.scrollbar;

                          // for color table, waht is the color of non selected values ?
                          Color                                         DefaultColor                    = Color.WHITE;
                          Color                                         MinimumColor                    = Color.RED;
                          Color                                         MiddleColor                     = Color.WHITE;
                          Color                                         MaximumColor                    = Color.BLUE;
                          DecimalFormat                                 DecimalFormater                 = new DecimalFormat("###.##");
                          String                                        Unit                            = "";
                          boolean                                       UniformSegment                  = false;
                          int                                           ArcSize                         = 0;
                          boolean                                       Sound                           = false;
                          boolean                                       Precise                         = false;
                          String                                        InterpolationMode               = BiSlider.CENTRAL;



  /**
   * Constructor
   */
  public BiSliderAbstraction() { }


  /**
   * change the size of the color segments
   *
   * @param SegmentSize_Arg  The new segmentSize value
   */
  public void setSegmentSize(double SegmentSize_Arg) {
    SegmentSize = SegmentSize_Arg;
    SegmentCount = (int)Math.ceil((MaximumValue - MinimumValue) / SegmentSize_Arg);
  }


  /**
   * @param DesiredSegmentCount_Arg  the SegmentCount we would like to have (if the gap would be divisible by that)
   * @return                         the closer possible SegmentCount according a desired SegmentCount
   */
  public int searchSegmentCount(
    int DesiredSegmentCount_Arg) {
    // dumb cases
    if (DesiredSegmentCount_Arg < 1 || ((int)(MaximumValue - MinimumValue)) != (MaximumValue - MinimumValue))
      return 1;

    if (DesiredSegmentCount_Arg > (MaximumValue - MinimumValue))
      return (int)(MaximumValue - MinimumValue);

    // The desired SegmentCount is possible !
    if (((MaximumValue - MinimumValue) % DesiredSegmentCount_Arg) == 0)
      return DesiredSegmentCount_Arg;

    for (int i = 1; DesiredSegmentCount_Arg - i > 1 || DesiredSegmentCount_Arg + i < ((MaximumValue - MinimumValue) / 2); i++)
      if (DesiredSegmentCount_Arg - i > 1 && ((MaximumValue - MinimumValue) % (DesiredSegmentCount_Arg - i)) == 0)
        return DesiredSegmentCount_Arg - i;

      else if (DesiredSegmentCount_Arg + i < ((MaximumValue - MinimumValue) / 2) && ((MaximumValue - MinimumValue) % (DesiredSegmentCount_Arg + i)) == 0)
        return DesiredSegmentCount_Arg + i;
    return 1;
  } // searchSegmentCount()


  /**
   * return a color table to make the correxpondance between values and colors.
   *
   * @return   a table of {min value, max value, R, G, B} for each different color zone.
   */
  public double[][] getColorTable() {

    if (SegmentCount == 1) {
      double  ColorTable[][]  = new double[1][4];
      ColorTable[0][0] = MinimumColoredValue;
      ColorTable[0][1] = MaximumColoredValue;
      ColorTable[0][2] = MinimumColor.getRGB();
      ColorTable[0][3] = 0; // The segment index
      return ColorTable;
    }

    float   Portion               = 1f / (SegmentCount - 1);

    int     Mini                  = (int)Math.floor((MinimumColoredValue - MinimumValue) / SegmentSize);
    int     Maxi                  = (int)Math.ceil((MaximumColoredValue - MinimumValue) / SegmentSize);
    int     SelectedSegmentCount  = Maxi - Mini;
    if (SelectedSegmentCount<=0)
      SelectedSegmentCount = 1;
    double  ColorTable[][]            = new double[SelectedSegmentCount][4];

    /*
     * if (InterpolationMode == BiSlider.RGB) {
     * System.err.println( " Portion="+Portion);
     * System.err.println( " Maxi="+Maxi+"   Mini="+Mini+"   nbseg="+(Maxi-Mini));
     * System.err.println( " Portion="+Portion);
     * System.err.println( " MinimumValue="+MinimumValue);
     * System.err.println( " MaximumValue="+MaximumValue);
     * System.err.println( " MinimumColoredValue="+MinimumColoredValue);
     * System.err.println( " MaximumColoredValue="+MaximumColoredValue);
     * System.err.println( " SegmentSize="+SegmentSize);
     * System.err.println( " SegmentCount="+SegmentCount);
     * System.err.println( " SelectedSegmentCount="+SelectedSegmentCount);
     * System.err.println( " MinimumColor.getRed()="+ MinimumColor.getRed());
     * }
     */
    for (int i = Mini; i < Maxi; i++) {
      ColorTable[i - Mini][0] = MinimumValue + (SegmentSize * i);
      ColorTable[i - Mini][1] = MinimumValue + (SegmentSize * (i + 1));
      ColorTable[i - Mini][3] = i;

      Color  NewColor  = MinimumColor;
      if (InterpolationMode.equals(BiSlider.RGB)) {
        float  dR  = ((MaximumColor.getRed() - MinimumColor.getRed()) * Portion);
        float  dG  = ((MaximumColor.getGreen() - MinimumColor.getGreen()) * Portion);
        float  dB  = ((MaximumColor.getBlue() - MinimumColor.getBlue()) * Portion);
        try {
          NewColor = new Color((int)(MinimumColor.getRed() + i * dR),
            (int)(MinimumColor.getGreen() + i * dG),
            (int)(MinimumColor.getBlue() + i * dB));
        } catch (java.lang.IllegalArgumentException IllegalArgumentException_Arg) {
          System.err.println("Error for i="+i+"dRGB="+dR+","+dG+","+dB+":"+MinimumColor+":"+SegmentSize+":"+SegmentCount);
          IllegalArgumentException_Arg.printStackTrace();
        }
      }
      else if (InterpolationMode.equals(BiSlider.HSB)) {
        float  hsb0[]  = Color.RGBtoHSB(MinimumColor.getRed(), MinimumColor.getGreen(), MinimumColor.getBlue(), null);
        float  hsb1[]  = Color.RGBtoHSB(MaximumColor.getRed(), MaximumColor.getGreen(), MaximumColor.getBlue(), null);

        //Portion = 1f / (SegmentCount - 1);
        float  dh    = (hsb1[0] - hsb0[0]) * Portion;
        float  ds    = (hsb1[1] - hsb0[1]) * Portion;
        float  db    = (hsb1[2] - hsb0[2]) * Portion;
        /*
         * System.err.println( "  hsb1[0]="+hsb1[0]);
         * System.err.println( "  hsb0[0]="+hsb0[0]);
         * System.err.println( "  i      ="+i);
         * System.err.println( "  dh     ="+dh);
         * System.err.println( "  i*dh   ="+(dh*i));
         */
        NewColor = Color.getHSBColor(hsb0[0] + dh * i,
          hsb0[1] + ds * i,
          hsb0[2] + db * i);
      }
      else if (InterpolationMode.equals(BiSlider.CENTRAL)) {
        Portion = 2f / (SegmentCount - 1);
        float  Portion2 = 2f / (SegmentCount);        
        float  dR1  = ((MaximumColor.getRed() - MiddleColor.getRed()) * Portion);
        float  dG1  = ((MaximumColor.getGreen() - MiddleColor.getGreen()) * Portion);
        float  dB1  = ((MaximumColor.getBlue() - MiddleColor.getBlue()) * Portion);

        float  dR2  = ((MiddleColor.getRed() - MinimumColor.getRed()) * Portion2);
        float  dG2  = ((MiddleColor.getGreen() - MinimumColor.getGreen()) * Portion2);
        float  dB2  = ((MiddleColor.getBlue() - MinimumColor.getBlue()) * Portion2);

        if (SegmentCount == 2 && i == 0)
          NewColor = MinimumColor;
        else if (SegmentCount == 2 && i == 1)
          NewColor = MaximumColor;

        else if (i > SegmentCount / 2) {
          int  k  = (int)(i - SegmentCount / 2);
          try {
            NewColor = new Color((int)(MiddleColor.getRed() + k * dR1),
                                 (int)(MiddleColor.getGreen() + k * dG1),
                                 (int)(MiddleColor.getBlue() + k * dB1));
          } catch(Exception Exception_Arg){
            Exception_Arg.printStackTrace();
            System.out.println("error1 with ");
            System.out.println("  SegmentCount = "+SegmentCount);
            System.out.println("  dR1 = "+dR1);
            System.out.println("  dG1 = "+dG1);
            System.out.println("  dB1 = "+dB1);
            System.out.println("  Portion = "+Portion);
            System.out.println("  i = "+i);
            NewColor = Color.WHITE;
          }
        }
        else
          //System.err.println( " i="+i);
          //System.err.println( " dR2="+dR2);
          //System.err.println( " i*dR2="+(dR2*i));
          //System.err.println( " red="+((int)(MinimumColor.getRed() + i * dR2)));
          //System.err.flush();
          try {
            NewColor = new Color((int)(MinimumColor.getRed() + i * dR2),
                                 (int)(MinimumColor.getGreen() + i * dG2),
                                 (int)(MinimumColor.getBlue() + i * dB2));
          } catch(Exception Exception_Arg){
            Exception_Arg.printStackTrace();
            System.out.println("error2 with ");
            System.out.println("  SegmentCount = "+SegmentCount);
            System.out.println("  dR2 = "+dR2);
            System.out.println("  dG2 = "+dG2);
            System.out.println("  dB2 = "+dB2);
            System.out.println("  Portion = "+Portion);
            NewColor = Color.WHITE;
          }          

      }

      ColorTable[i - Mini][2] = NewColor.getRGB();

    }

    // xxx bug ici a corriger 0 =out of bound
    if (ColorTable[0][0] != MinimumColoredValue)
      ColorTable[0][0] = MinimumColoredValue;

    if (ColorTable[SelectedSegmentCount - 1][1] != MaximumColoredValue)
      ColorTable[SelectedSegmentCount - 1][1] = MaximumColoredValue;

    return ColorTable;
  } // getColorTable()


  /**
   * display the table of values as a String
   *
   * @return   Description of the Return Value
   */
  public String toString() {

    double[][]    ColorArray  = getColorTable();

    StringBuffer  SB1         = new StringBuffer();
    SB1.append("Color table with " + ColorArray.length + " segments\n");
    SB1.append("MinimumValue        = " + MinimumValue + "\n");
    SB1.append("MinimumColoredValue = " + MinimumColoredValue + "\n");
    SB1.append("MaximumColoredValue = " + MaximumColoredValue + "\n");
    SB1.append("MaximumValue        = " + MaximumValue + "\n");
    SB1.append("SegmentSize         = " + SegmentSize + "\n");
    SB1.append("SegmentCount        = " + SegmentCount + "\n");

    for (int k = 0; k < ColorArray.length; k++) {
      Color  Color1  = new Color((int)ColorArray[k][2]);
      SB1.append(ColorArray[k][0] + "-->" + ColorArray[k][1] + " with R=" + Color1.getRed() + ", G=" + Color1.getGreen() + ", B=" + Color1.getBlue() + "\n");
    }
    SB1.append("\n");

    return SB1.toString();
  } // toString()
}

