package bislider.com.visutools.nav.bislider;

import java.awt.event.*;
import java.awt.geom.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import java.io.*;
import java.text.*;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.beans.*;

/**
 * Vertical presentation of the graphical interface of the bean (drawing and mouse event handling). <br>
 * <br>
 *
 * <table border=1 width="90%">
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
 * <li> Format : 01/11/2001<br>
 * <li> Last Modif : 11/02/2003 <br>
 * <br>
 * <b>Bugs:</b> <br>
 * <li> ???
 * <br>
 * <b>To Do:</b> <br>
 * <li> alt or shift pressed -> repaint
 * <br>
 * @author    Frederic Vernier, Frederic.Vernier@laposte.net
 * @version   1.4.1
 * @created   16 f�vrier 2004
 */

public class BiSlider_V_Presentation
   extends BiSliderPresentation {
  //------------ MODIFIERS|-------------------- Type|----------------------------------------------- Name = Init value
  protected final static  javax.swing.text.html.parser.ParserDelegator  MAXIMUM_VARIABLE_SIZE_FOR_NAME  = null;

  final static  long     serialVersionUID  = -6027278644723645018L;

                boolean  MinOnTop          = true;


  /**
   * Contructor, create the polygons and other nested object then register mouse callbacks.
   *
   * @param Ctrl          The reference on the father. Generally the pac agent which create this agent.
   * @param MinOnTop_Arg  Description of the Parameter
   */
  public BiSlider_V_Presentation(
    BiSlider Ctrl,
    ContentPainterSupport ContentPainterSupport_Arg,
    boolean MinOnTop_Arg) {

    super(Ctrl, ContentPainterSupport_Arg);

    JPanel1.setLayout(new BorderLayout());
    JPanel1.setBorder(BorderFactory.createLineBorder(Ctrl.getForeground()));
    JSlider1.setOrientation(JSlider.HORIZONTAL);
    JSlider1.setMajorTickSpacing(50);
    JPanel1.add(JSlider1, BorderLayout.CENTER);
    JPanel1.add(JLabel1, BorderLayout.EAST);
    JLabel1.setHorizontalAlignment(SwingConstants.CENTER);
    JLabel1.setOpaque(true);
    JSlider1.setOpaque(true);
    JPanel1.revalidate();

    String      Text = Ctrl.getDecimalFormater().format(Ctrl.getMaximumValue())+".99";
    FontMetrics FM1  = JLabel1.getFontMetrics(JLabel1.getFont());

    JLabel1.setPreferredSize(new Dimension(FM1.stringWidth(Text), 8));
    JLabel1.setSize(new Dimension(FM1.stringWidth(Text), 8));

    Margin_Ruler_Top = 32;
    MinOnTop         = MinOnTop_Arg;

    JComponent1.setLayout(null);
    Ctrl.setPreferredSize(new Dimension(Margin_Ruler_Top + MARGIN_RULER_BOTTOM + RulerHeight, MARGIN_RULER_LEFT + MARGIN_RULER_RIGHT + 10 * Ctrl.getSegmentCount()));
  } // constructor()


  /**
   * Sets the rulers value if the new values are coherents.
   *
   * @param Min_Arg  the value of the min triangle
   * @param Max_Arg  the value of the max triangle
   */
  public void setRulerValues(
    double Min_Arg,
    double Max_Arg) {

    int  SegmentCount   = Ctrl.getSegmentCount();

    if (SegmentCount == 0) {
      SegmentCount = 1;
    }

    RulerWidth = JComponent1.getSize().height - MARGIN_RULER_LEFT - MARGIN_RULER_RIGHT;
    GraduationWidth = (RulerWidth * Ctrl.getSegmentSize()) / (Ctrl.getMaximumValue() - Ctrl.getMinimumValue());

    NeverDrawn = false;

    int  NewLeftValue   = (int)(MARGIN_RULER_LEFT + ((Min_Arg - Ctrl.getMinimumValue()) * RulerWidth) / (Ctrl.getMaximumValue() - Ctrl.getMinimumValue()));
    int  NewRightValue  = (int)(MARGIN_RULER_LEFT + ((Max_Arg - Ctrl.getMinimumValue()) * RulerWidth) / (Ctrl.getMaximumValue() - Ctrl.getMinimumValue()));

    if (NewLeftValue <= NewRightValue && NewLeftValue >= MARGIN_RULER_LEFT && NewRightValue <= MARGIN_RULER_LEFT + RulerWidth) {
      LeftValue  = NewLeftValue;
      RightValue = NewRightValue;

      int TriangleSide = RulerHeight / 2;
      TriangleSide = Math.max(Math.min(TriangleSide, 20), 10);

    if (UIManager.getLookAndFeel().getName().equals("Metal")) {
      int[] xp1 = {LeftValue,  LeftValue-7, LeftValue-7, LeftValue-6, LeftValue+6, LeftValue+7, LeftValue+7};
      int[] yp1 = {Margin_Ruler_Top + RulerHeight + 1-14,
                   Margin_Ruler_Top + RulerHeight + 1-7, Margin_Ruler_Top + RulerHeight +1, Margin_Ruler_Top + RulerHeight + 1+1,
                   Margin_Ruler_Top + RulerHeight + 1+1, Margin_Ruler_Top + RulerHeight +1, Margin_Ruler_Top + RulerHeight + 1-7};
      TheLeft_Polygon = new Polygon(xp1, yp1, 7);

      int[] xp2 = {RightValue-7, RightValue-7, RightValue-6, RightValue+6, RightValue+7, RightValue+7, RightValue}; 
      int[] yp2 = {Margin_Ruler_Top - 1+7, Margin_Ruler_Top - 1, Margin_Ruler_Top - 1-1, 
                   Margin_Ruler_Top - 1-1, Margin_Ruler_Top - 1, Margin_Ruler_Top - 1+7,
                   Margin_Ruler_Top - 1+14,};
      TheRight_Polygon = new Polygon(xp2, yp2, 7);

    } else { // Triangles by default
      TheLeft_Polygon = new Polygon();
      TheLeft_Polygon.addPoint(LeftValue ,                Margin_Ruler_Top + RulerHeight - RulerHeight/2 - 1);
      TheLeft_Polygon.addPoint(LeftValue + RulerHeight/2 + 2, Margin_Ruler_Top + RulerHeight + 1);
      TheLeft_Polygon.addPoint(LeftValue ,                Margin_Ruler_Top + RulerHeight + 1);
      TheLeft_Polygon.addPoint(LeftValue ,                Margin_Ruler_Top + RulerHeight - RulerHeight/2 - 1);

      TheRight_Polygon = new Polygon();
      TheRight_Polygon.addPoint(RightValue - 2 - RulerHeight/2, Margin_Ruler_Top - 1);
      TheRight_Polygon.addPoint(RightValue , Margin_Ruler_Top - 1);
      TheRight_Polygon.addPoint(RightValue , Margin_Ruler_Top + RulerHeight/2 + 1);
      TheRight_Polygon.addPoint(RightValue - 2 - RulerHeight/2, Margin_Ruler_Top - 1);      
    } 
    } else if (Ctrl.getSize().width == 0 || Ctrl.getSize().height == 0) {

    } else {
      System.err.println("\nsetRulerValues()");
      System.err.println("  Size              = " + Ctrl.getSize());
      System.err.println("  NewLeftValue      = " + NewLeftValue);
      System.err.println("  NewRightValue     = " + NewRightValue);
      System.err.println("  MARGIN_RULER_LEFT = " + MARGIN_RULER_LEFT);
      System.err.println("  MARGIN_RULER_LEFT + RulerWidth = " + (MARGIN_RULER_LEFT + RulerWidth));
      //Debug.debug(0, "");
    }
  } // setRulerValues()


  /**
   * Method called by the awt-swing mechanism when the area needs to be refreshed
   *
   * @param Graphics_Arg  the graphic context to draw things
   */
  public void paint(
    Graphics Graphics_Arg) {
    if (Graphics_Arg == null) {
      return;
    }

    //System.err.println("\n+paint()"+Ctrl.getSize()+"   "+Ctrl.getPreferredSize());
    //System.err.println("   Margin_Ruler_Top="+Margin_Ruler_Top);
    //System.err.println("   MARGIN_RULER_LEFT="+MARGIN_RULER_LEFT);

    Graphics2D      Graphics2          = (Graphics2D)Graphics_Arg;
    RenderingHints  RenderingHints2    = new RenderingHints(null);
    RenderingHints2.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    RenderingHints2.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    RenderingHints2.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    RenderingHints2.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
    Graphics2.setRenderingHints(RenderingHints2);

    AffineTransform  AffineTransform1  = new AffineTransform(Graphics2.getTransform());
    if (MinOnTop) {
      AffineTransform1.rotate(Math.PI / 2);
      AffineTransform1.translate(0, -JComponent1.getSize().width);
    } else {
      AffineTransform1.rotate(-Math.PI / 2);
      AffineTransform1.translate(-JComponent1.getSize().height, 0);
    }
    AffineTransform  AffineTransform2  = Graphics2.getTransform();
    Graphics2.setTransform(AffineTransform1);

    Font             Font1             = Ctrl.getFont();
    if (Font1==null)
      Font1 = new Font("Dialog", Font.PLAIN, 12);
    Font             Font2             = new Font(Font1.getName(), Font.BOLD, Font1.getSize());
    Shape            OldClip           = Graphics2.getClip();
    /*Shape            NewClip           = new Rectangle2D.Float(
                                            MARGIN_RULER_LEFT, Margin_Ruler_Top,
                                            RulerWidth, RulerHeight);*/
    Shape NewClip           = new RoundRectangle2D.Float(
      MARGIN_RULER_LEFT, Margin_Ruler_Top,
      RulerWidth, RulerHeight,
      Ctrl.getArcSize(), Ctrl.getArcSize());    

    int              SegmentCount      = Ctrl.getSegmentCount();
    FontMetrics      TheFontMetrics    = Graphics2.getFontMetrics();
    RulerWidth                         = JComponent1.getSize().height - MARGIN_RULER_LEFT - MARGIN_RULER_RIGHT;
    double           ValuesWidth       = Ctrl.getMaximumValue() - Ctrl.getMinimumValue();

    if (NeverDrawn) {
      LeftValue = (int)(MARGIN_RULER_LEFT + ((Ctrl.getMinimumColoredValue() - Ctrl.getMinimumValue()) * RulerWidth) / (Ctrl.getMaximumValue() - Ctrl.getMinimumValue()));
      RightValue = (int)(MARGIN_RULER_LEFT + ((Ctrl.getMaximumColoredValue() - Ctrl.getMinimumValue()) * RulerWidth) / (Ctrl.getMaximumValue() - Ctrl.getMinimumValue()));
      TheRight_Polygon.translate(RulerWidth, 0);
      NeverDrawn = false;
    }

    if (RulerWidth != JComponent1.getSize().height - MARGIN_RULER_LEFT - MARGIN_RULER_RIGHT) {
      int  NewRulerWidth  = JComponent1.getSize().height - MARGIN_RULER_LEFT - MARGIN_RULER_RIGHT;

      NewRulerWidth = NewRulerWidth - NewRulerWidth % SegmentCount;

      int  NewLeftValue   = MARGIN_RULER_LEFT + ((LeftValue - MARGIN_RULER_LEFT) * (NewRulerWidth)) / RulerWidth;
      int  NewRightValue  = MARGIN_RULER_LEFT + ((RightValue - MARGIN_RULER_LEFT) * (NewRulerWidth)) / RulerWidth;

      TheLeft_Polygon.translate(NewLeftValue - LeftValue, 0);
      TheRight_Polygon.translate(NewRightValue - RightValue, 0);

      LeftValue = NewLeftValue;
      RightValue = NewRightValue;

      RulerWidth = NewRulerWidth;

      GraduationWidth = (float)((RulerWidth * Ctrl.getSegmentSize()) / (Ctrl.getMaximumValue() - Ctrl.getMinimumValue()));
    }

    Color            BackColor         = Ctrl.getBackground();

    Graphics2.setColor(BackColor);
    Graphics2.fillRect(0, 0, JComponent1.getSize().height, JComponent1.getSize().width);
    Graphics2.setClip(NewClip);
    
    if (!UIManager.getLookAndFeel().getName().equals("Metal")) {
      Graphics2.setColor(Ctrl.getSliderBackground());
      Graphics2.fillRoundRect(MARGIN_RULER_LEFT, Margin_Ruler_Top, RulerWidth, RulerHeight, Ctrl.getArcSize(), Ctrl.getArcSize());
    } else {
      Object SliderBackColor = UIManager.getLookAndFeel().getDefaults().get("Slider.gradient");
      if (SliderBackColor!=null && SliderBackColor instanceof List){
        List GradientProperties = (List)SliderBackColor;
        Float Cut1 = (Float)GradientProperties.get(0);
        Float Cut2 = (Float)GradientProperties.get(1);
        Color Color1 = (Color)GradientProperties.get(2);
        Color Color2 = (Color)GradientProperties.get(3);
        Color Color3 = (Color)GradientProperties.get(4);
        float Rem = 1f-Cut2-2*Cut1;
        int x1= MARGIN_RULER_LEFT;
        int x2= RulerWidth+MARGIN_RULER_LEFT;
        int y1= Margin_Ruler_Top;
        int y2= Margin_Ruler_Top+RulerHeight;

        GradientPaint GradientPaint1 = new GradientPaint(x1, y1, Color3, x1, y1+(int)(RulerHeight*Rem), Color2);
        GradientPaint GradientPaint2 = new GradientPaint(x1, y1+(int)(RulerHeight*Rem), Color2, x1, y1+(int)(RulerHeight*(Rem+Cut1)), Color1);
        GradientPaint GradientPaint3 = new GradientPaint(x1, y1+(int)(RulerHeight*(Rem+Cut1+Cut2)), Color1, x1, y2, Color.WHITE);

        Graphics2.setPaint(GradientPaint1);
        Graphics2.fillRect(x1, y1, RulerWidth, (int)(RulerHeight*Rem));
        Graphics2.setPaint(GradientPaint2);
        Graphics2.fillRect(x1, y1+(int)(RulerHeight*Rem), RulerWidth, (int)(RulerHeight*Cut1)+1);
        Graphics2.setColor(Color1);
        Graphics2.fillRect(x1, y1+(int)(RulerHeight*(Rem+Cut1)), RulerWidth, (int)(RulerHeight*Cut2)+1);
        Graphics2.setPaint(GradientPaint3);
        Graphics2.fillRect(x1, y1+(int)(RulerHeight*(Rem+Cut1+Cut2)), RulerWidth, (int)(RulerHeight*Cut1)+1);
      }
    }
    
    //Graphics_Arg.setColor(Ctrl.getSliderBackground());
    //Graphics2.fillRoundRect(MARGIN_RULER_LEFT, Margin_Ruler_Top, RulerWidth, RulerHeight, Ctrl.getArcSize(), Ctrl.getArcSize());

    Graphics2.setColor(Ctrl.getSliderBackground().darker());
    Graphics2.drawArc(MARGIN_RULER_LEFT+1, Margin_Ruler_Top,
                         Ctrl.getArcSize(), Ctrl.getArcSize(), 90, 90);
    Graphics2.drawArc(MARGIN_RULER_LEFT+RulerWidth-Ctrl.getArcSize()-1, Margin_Ruler_Top,
                         Ctrl.getArcSize(), Ctrl.getArcSize(), 0, 90);

    Graphics2.setColor(Ctrl.getSliderBackground().brighter());
    Graphics2.drawArc(MARGIN_RULER_LEFT+RulerWidth-Ctrl.getArcSize()-1, Margin_Ruler_Top+RulerHeight-Ctrl.getArcSize(),
                         Ctrl.getArcSize(), Ctrl.getArcSize(), 270, 90);
    Graphics2.drawArc(MARGIN_RULER_LEFT+1, Margin_Ruler_Top+RulerHeight-Ctrl.getArcSize(),
                         Ctrl.getArcSize(), Ctrl.getArcSize(), 180, 90);

    Graphics2.setColor(Ctrl.getSliderBackground().darker());
    Graphics2.drawLine(MARGIN_RULER_LEFT+Ctrl.getArcSize()/2-1,            Margin_Ruler_Top+1,
                       MARGIN_RULER_LEFT+RulerWidth-Ctrl.getArcSize()/2+1, Margin_Ruler_Top+1);
    Graphics2.drawLine(MARGIN_RULER_LEFT+1,   Margin_Ruler_Top+Ctrl.getArcSize()/2,
                       MARGIN_RULER_LEFT+1,   Margin_Ruler_Top+RulerHeight-Ctrl.getArcSize()/2);
    Graphics2.setColor(Ctrl.getSliderBackground().brighter());
    Graphics2.drawLine(MARGIN_RULER_LEFT+Ctrl.getArcSize()/2-1,            Margin_Ruler_Top+RulerHeight-1,
                       MARGIN_RULER_LEFT+RulerWidth-Ctrl.getArcSize()/2+1, Margin_Ruler_Top+RulerHeight-1);
    Graphics2.drawLine(MARGIN_RULER_LEFT+RulerWidth-1, Margin_Ruler_Top+Ctrl.getArcSize()/2+1,
                       MARGIN_RULER_LEFT+RulerWidth-1, Margin_Ruler_Top+RulerHeight-Ctrl.getArcSize()/2);
    Graphics2.setClip(OldClip);

    int              LastMax           = 0;
    int              Max               = 0;

    NewClip           = new RoundRectangle2D.Float(
                              MARGIN_RULER_LEFT+2, Margin_Ruler_Top,
                              RulerWidth-3, RulerHeight,
                              Ctrl.getArcSize(), Ctrl.getArcSize());
    Graphics2.setClip(NewClip);
    int SegmentCountBefore = 0;
    double[][]       ColTable          = Ctrl.getColorTable();    
    String           Unit              = Ctrl.getUnit();
    
    for (int i = 0; i <= SegmentCount; i++) {
      double           Val               = Ctrl.getMinimumValue() + i * Ctrl.getSegmentSize();
      double           Val2              = Ctrl.getMinimumValue() + (i+1) * Ctrl.getSegmentSize();
      
      if (Val<=ColTable[0][0] && ColTable[0][0]<Val2)
        SegmentCountBefore = i;

      String           NumberString      = "";
      int              NumberWidth       = 0;
      int              x                 = 0;

      String           MaxNumberString   = "";
      if (Ctrl.getMaximumValue() == (long)Ctrl.getMaximumValue()) {
        MaxNumberString = "" + ((long)Ctrl.getMaximumValue()) + Unit;
      } else {
        MaxNumberString = "" + (((long)(Ctrl.getMaximumValue() * 10)) / 10) + Unit;
      }

      if (Val > Ctrl.getMaximumValue()) {
        Val = Ctrl.getMaximumValue();
        if (Unit.equals("alpha")) {
          NumberString = ""+(char)((int)Val+'A');
        } else if (Val == (long)Val) {
          NumberString = "" + ((long)Val) + Unit;
        } else {
          NumberString = "" + (((long)(Val * 10)) / 10f) + Unit;
        }
        NumberWidth = TheFontMetrics.stringWidth(NumberString);
        Max = Math.max(NumberWidth, Max);
        x = MARGIN_RULER_LEFT + RulerWidth - TheFontMetrics.getHeight() / 2;
      } else {
        if (Unit.equals("alpha")) {
          NumberString = ""+(char)((int)Val+'A');
        } else if (Val == (long)Val) {
          NumberString = "" + ((long)Val) + Unit;
        } else {
          NumberString = "" + (((long)(Val * 10)) / 10f) + Unit;
        }
        NumberWidth = TheFontMetrics.stringWidth(NumberString);
        Max = Math.max(NumberWidth, Max);
        x = MARGIN_RULER_LEFT + (int)(GraduationWidth * i) - (int)((TheFontMetrics.getHeight() / 2));
      }

      AffineTransform  AffineTransform3  = new AffineTransform(AffineTransform1);
      if (MinOnTop) {
        AffineTransform3.translate(x + TheFontMetrics.getHeight(), Margin_Ruler_Top - 1);
        AffineTransform3.rotate(-Math.PI / 2);
      } else {
        AffineTransform3.translate(x, 1);
        AffineTransform3.rotate(Math.PI / 2);
      }
      Graphics2.setTransform(AffineTransform3);

      // get the color
      Graphics2.setColor(Ctrl.getForeground());

      if (Val == Ctrl.getMaximumValue() || Val == Ctrl.getMinimumValue()) {
        Graphics2.setFont(Font2);
        Shape Shape00 = Graphics2.getClip();
        Graphics2.setClip(null);
        if (MinOnTop)
          Graphics2.drawString(NumberString, 1, 0);
        else
          Graphics2.drawString(NumberString, Margin_Ruler_Top - NumberWidth - 2, 0);
        Graphics2.setClip(Shape00);

        //restore the font
        Graphics2.setFont(Font1);

        LastMax = x + TheFontMetrics.getHeight();
      }  // if not too close to the last one or too close to the previous one
      else if (x >= LastMax + 2) {
        Shape Shape00 = Graphics2.getClip();
        Graphics2.setClip(null);
        if (MinOnTop) {
          Graphics2.drawString(NumberString, 0, 0);
        } else {
          Graphics2.drawString(NumberString, Margin_Ruler_Top - NumberWidth - 1, 0);
        }
        Graphics2.setClip(Shape00);
        LastMax = x + TheFontMetrics.getHeight();
      }
      Graphics2.setTransform(AffineTransform1);

      if (i == 0 && MinOnTop) {
        RectFirstLabel = new Rectangle(x + TheFontMetrics.getHeight() - TheFontMetrics.getAscent() + 1, Margin_Ruler_Top - NumberWidth - 2, TheFontMetrics.getAscent(), NumberWidth + 2);
      } else if (i == SegmentCount && MinOnTop) {
        RectLastLabel = new Rectangle(x + TheFontMetrics.getHeight() - TheFontMetrics.getAscent() + 1, Margin_Ruler_Top - NumberWidth - 2, TheFontMetrics.getAscent(), NumberWidth + 2);
      } else if (i == 0 && !MinOnTop) {
        RectFirstLabel = new Rectangle(x - 1, Margin_Ruler_Top - NumberWidth - 2, TheFontMetrics.getAscent(), NumberWidth + 2);
      } else if (i == SegmentCount && !MinOnTop) {
        RectLastLabel = new Rectangle(x - 1, Margin_Ruler_Top - NumberWidth - 2, TheFontMetrics.getAscent(), NumberWidth + 2);
      }

      if (LastMax == x + TheFontMetrics.getHeight()) {
        Graphics2.setColor(Ctrl.getSliderBackground().darker().darker());
      } else {
        Graphics2.setColor(Ctrl.getSliderBackground().darker());
      }

      if (ContentPainterSupport1.getPainterListenerNumber()>0 && Val < Ctrl.getMaximumValue()){
        int x0 = MARGIN_RULER_LEFT + (int)(GraduationWidth * i);
        int x3 = MARGIN_RULER_LEFT + (int)(GraduationWidth * (i+1));
        x3= Math.min(x3,MARGIN_RULER_LEFT+RulerWidth);

        Rectangle Rect1 = new Rectangle (x0, Margin_Ruler_Top + 2, x3 - x0, RulerHeight - 3);
        Shape ShapeClip = Graphics2.getClip();
        Color Color0    = Graphics2.getColor();
        Graphics2.setClip(NewClip);
        Graphics2.clip(Rect1);        
        ContentPainterSupport1.firePaint(Ctrl, Graphics2, Val, Val2, i, null, Rect1, Rect1);
        Graphics2.setClip(ShapeClip);
        Graphics2.setColor(Color0);
      }

      if (i!=0 && Val < Ctrl.getMaximumValue()) {
        if (ContentPainterSupport1.getPainterListenerNumber()==0)
          Graphics2.drawLine(MARGIN_RULER_LEFT + (int)(GraduationWidth * i), Margin_Ruler_Top + 2,
            MARGIN_RULER_LEFT + (int)(GraduationWidth * i), Margin_Ruler_Top + RulerHeight - 2);
      }

      if (MouseUnder == FIRST_LABEL) {
        Graphics2.draw(RectFirstLabel);
      }
      if (MouseUnder == LAST_LABEL) {
        Graphics2.draw(RectLastLabel);
      }
    }
    Ctrl.setPreferredSize(new Dimension(Max + 2 + MARGIN_RULER_BOTTOM + RulerHeight, MARGIN_RULER_LEFT + MARGIN_RULER_RIGHT + 10 * Ctrl.getSegmentCount()));
    if (Margin_Ruler_Top!=Max + 2) {
      Margin_Ruler_Top = Max + 2;
      Ctrl.repaint();
    }

    if (MouseUnder == SEGMENT && RectangleSegment != null) {
      Graphics2.setColor(SystemColor.control.darker());
      Graphics2.draw(RectangleSegment);
    }

    // colored segment
    for (int i = 0; i < ColTable.length; i++) {
      Graphics2.setColor(new Color((int)(ColTable[i][2])));
      if (ColTable[i][0] < ColTable[i][1]) {
        int  x1  = (int)(MARGIN_RULER_LEFT + (RulerWidth * (ColTable[i][0] - Ctrl.getMinimumValue())) / ValuesWidth) + 1;
        int  x2  = (int)(MARGIN_RULER_LEFT + (RulerWidth * ((ColTable[i][1]) - Ctrl.getMinimumValue())) / ValuesWidth);
        // the full segment
        int  x0  = MARGIN_RULER_LEFT + (int)(GraduationWidth * (int)ColTable[i][3]);
        int  x3  = MARGIN_RULER_LEFT + (int)(GraduationWidth * (int)(ColTable[i][3]+1));
        x3= Math.min(x3,MARGIN_RULER_LEFT+RulerWidth);

        int y1 = Margin_Ruler_Top + 2;
        int y2 = Margin_Ruler_Top + 2+RulerHeight - 3;

        if (!MinOnTop) {
          x1--;
          x2--;
        }else {
          y1--;
          y2--;
        }
        Rectangle Rect1 = new Rectangle (x1, y1, x2-x1, y2-y1);
        Rectangle Rect2 = new Rectangle (x0, y1, x3 - x0, y2-y1);

        if (ContentPainterSupport1.getPainterListenerNumber()==0){
          Graphics2.fill(Rect1);
          if (i!=0){
            Graphics2.setColor(new Color(Graphics2.getColor().getRed(), Graphics2.getColor().getGreen(), Graphics2.getColor().getBlue(), 192));

            if (!MinOnTop)
              Graphics2.drawLine(x1, Margin_Ruler_Top + 3, x1, Margin_Ruler_Top+RulerHeight - 3);
            else
              Graphics2.drawLine(x1-1, Margin_Ruler_Top + 3, x1-1, Margin_Ruler_Top+RulerHeight - 3);
          }
        } else {
          Shape ShapeClip = Graphics2.getClip();
          Color Color0    = Graphics2.getColor();
          Graphics2.setClip(NewClip);
          Graphics2.clip(Rect1);
          ContentPainterSupport1.firePaint(Ctrl, Graphics2, ColTable[i][0], ColTable[i][1], i+SegmentCountBefore,
                                           Graphics2.getColor(), Rect1, Rect2);
          Graphics2.setClip(ShapeClip);
          Graphics2.setColor(Color0);
        }
      }
    }

    Graphics2.setClip(OldClip);

    if (Dragging == ALT_LEFT_RULER || Dragging == ALT_RIGHT_RULER) {
      Rectangle    Rect1         = TheRight_Polygon.getBounds();
      Rectangle    Rect2         = TheLeft_Polygon.getBounds();
      Rectangle2D  Rectangle2D1  = Rect1.createUnion(Rect2);

      Graphics2.setColor(SystemColor.scrollbar.darker());
      Graphics2.fillRect(
        (int)(Rectangle2D1.getX() + Rectangle2D1.getWidth() / 2), Margin_Ruler_Top,
        2, RulerHeight);
    }

    //Graphics2.setColor(getOppositeColor(Ctrl.getBackground()));
    Graphics2.setColor(Color.BLACK);
    Graphics2.drawRoundRect(MARGIN_RULER_LEFT, Margin_Ruler_Top, RulerWidth, RulerHeight, Ctrl.getArcSize(), Ctrl.getArcSize());

    paintThumbs(Graphics2);    
    /*
    Color ColorBis = SystemColor.control;
    if (Dragging == RIGHT_RULER || Dragging == SHIFT_RIGHT_RULER || Dragging == ALT_RIGHT_RULER)
      ColorBis  = ColorBis.darker();
    else if (MouseUnder == RIGHT_POLYGON)
      ColorBis = new Color(SystemColor.control.getRed()-40, SystemColor.control.getGreen()-40, SystemColor.control.getBlue()-40);

    Graphics2.setColor(ColorBis);
    Graphics2.fillPolygon(TheRight_Polygon);
    
    // triangle shadow
    if ((!MinOnTop && MouseUnder != RIGHT_POLYGON)||
        (MinOnTop && MouseUnder == RIGHT_POLYGON))
      Graphics2.setColor(ColorBis.brighter());
    else
      Graphics2.setColor(ColorBis.darker());

    Graphics2.drawLine(TheRight_Polygon.xpoints[1]-1, TheRight_Polygon.ypoints[1]+1,
                          TheRight_Polygon.xpoints[2]-1, TheRight_Polygon.ypoints[2]-1);
    Graphics2.drawLine(TheRight_Polygon.xpoints[0]+1, TheRight_Polygon.ypoints[0]+1,
                          TheRight_Polygon.xpoints[1]-1, TheRight_Polygon.ypoints[1]+1);

    // triangle shadow
    if ((!MinOnTop && MouseUnder != RIGHT_POLYGON)||
        (MinOnTop && MouseUnder == RIGHT_POLYGON))
      Graphics2.setColor(ColorBis.darker());
    else
      Graphics2.setColor(ColorBis.brighter());
    Graphics2.drawLine(TheRight_Polygon.xpoints[0]+2, TheRight_Polygon.ypoints[0]+1,
                       TheRight_Polygon.xpoints[2]-1, TheRight_Polygon.ypoints[2]-2);


    ColorBis = SystemColor.control;
    if (Dragging == LEFT_RULER || Dragging == SHIFT_LEFT_RULER || Dragging == ALT_LEFT_RULER)
      ColorBis  = ColorBis.darker();
    else if (MouseUnder == LEFT_POLYGON)
      ColorBis = new Color(SystemColor.control.getRed()-40, SystemColor.control.getGreen()-40, SystemColor.control.getBlue()-40);
    Graphics2.setColor(ColorBis);

    Graphics2.fillPolygon(TheLeft_Polygon);

    if ((MinOnTop && MouseUnder != LEFT_POLYGON)||
        (!MinOnTop && MouseUnder == LEFT_POLYGON))
      Graphics2.setColor(ColorBis.brighter());
    else
      Graphics2.setColor(ColorBis.darker());
    Graphics2.drawLine(TheLeft_Polygon.xpoints[0]+1, TheLeft_Polygon.ypoints[0]+2,
                          TheLeft_Polygon.xpoints[2]+1, TheLeft_Polygon.ypoints[2]-1);
    Graphics2.drawLine(TheLeft_Polygon.xpoints[1]-2, TheLeft_Polygon.ypoints[1]-1,
                          TheLeft_Polygon.xpoints[2]+1, TheLeft_Polygon.ypoints[2]-1);

    if ((MinOnTop && MouseUnder != LEFT_POLYGON)||
        (!MinOnTop && MouseUnder == LEFT_POLYGON))
      Graphics2.setColor(ColorBis.darker());
    else
      Graphics2.setColor(ColorBis.brighter());
    Graphics2.drawLine(TheLeft_Polygon.xpoints[0]+1, TheLeft_Polygon.ypoints[0]+2,
                       TheLeft_Polygon.xpoints[1]-2, TheLeft_Polygon.ypoints[1]-1);

    if (MouseUnder == SELECTION) {
      Rectangle    Rect1         = TheRight_Polygon.getBounds();
      Rectangle    Rect2         = TheLeft_Polygon.getBounds();
      Rectangle2D  Rectangle2D1  = Rect1.createUnion(Rect2);

      Graphics2.draw(Rectangle2D1);
    }

    //Graphics2.setColor(getOppositeColor(Ctrl.getBackground()));
    Graphics2.setColor(Color.BLACK);
    Graphics2.drawPolygon(TheLeft_Polygon);
    Graphics2.drawPolygon(TheRight_Polygon);
    */
    if (Dragging == ALT_RIGHT_RULER || Dragging == ALT_LEFT_RULER || Dragging == RIGHT_RULER ||
      Dragging == SEGMENT || Dragging == SHIFT_SEGMENT || Dragging == SHIFT_RIGHT_RULER) {
      String ValString = "";
       if (Unit.equals("alpha")) {
        ValString = ""+(char)((int)Ctrl.getMaximumColoredValue()+'A');
      } else if (Math.abs(((int)Ctrl.getMaximumColoredValue()) - Ctrl.getMaximumColoredValue()) < 0.0001) {
        ValString = "" + (int)Ctrl.getMaximumColoredValue();
      } else {
        ValString = Ctrl.getDecimalFormater().format(Ctrl.getMaximumColoredValue());
      }

      AffineTransform  AffineTransform3  = new AffineTransform(AffineTransform1);
      if (MinOnTop) {
        AffineTransform3.translate(RightValue + 2 + TheFontMetrics.getAscent(), Margin_Ruler_Top + TheFontMetrics.stringWidth(ValString)+2);
        AffineTransform3.rotate(-Math.PI / 2);
      } else {
        int dx=Math.min(Margin_Ruler_Top + RulerHeight - TheFontMetrics.stringWidth(ValString) - 2, Margin_Ruler_Top+2);
        AffineTransform3.translate(RightValue + 2, dx);
        AffineTransform3.rotate(Math.PI / 2);
      }
      Graphics2.setTransform(AffineTransform3);
      Graphics2.setColor(getOppositeColor(Ctrl.getForeground()));
      Graphics2.drawString(ValString, 1, 1);
      Graphics2.setColor(Ctrl.getForeground());
      Graphics2.drawString(ValString, 0, 0);
      Graphics2.setTransform(AffineTransform1);
    }
    

    if (Dragging == ALT_RIGHT_RULER || Dragging == ALT_LEFT_RULER || Dragging == LEFT_RULER ||
      Dragging == SEGMENT || Dragging == SHIFT_SEGMENT || Dragging == SHIFT_LEFT_RULER) {
      String           ValString         = "";
      if (Unit.equals("alpha")) {
        ValString = ""+(char)((int)Ctrl.getMinimumColoredValue()+'A');
      } else if (Math.abs(((int)Ctrl.getMinimumColoredValue()) - Ctrl.getMinimumColoredValue()) < 0.0001) {
        ValString = "" + (int)Ctrl.getMinimumColoredValue();
      } else {
        ValString = Ctrl.getDecimalFormater().format(Ctrl.getMinimumColoredValue());
      }

      AffineTransform  AffineTransform3  = new AffineTransform(AffineTransform1);
      if (MinOnTop) {
        AffineTransform3.translate(LeftValue - 2 + TheFontMetrics.getAscent() - TheFontMetrics.getHeight(), Margin_Ruler_Top + RulerHeight - 2);
        AffineTransform3.rotate(-Math.PI / 2);
      } else {
        AffineTransform3.translate(LeftValue + 2 - TheFontMetrics.getHeight(), Margin_Ruler_Top + RulerHeight - TheFontMetrics.stringWidth(ValString) - 2);
        AffineTransform3.rotate(Math.PI / 2);
      }
      Graphics2.setTransform(AffineTransform3);
      Graphics_Arg.setColor(getOppositeColor(Ctrl.getForeground()));
      Graphics2.drawString(ValString, 0, 0);
      Graphics_Arg.setColor(Ctrl.getForeground());
      Graphics2.drawString(ValString, 1, 1);
      Graphics2.setTransform(AffineTransform1);
    }

    if (JTextFieldMax.isVisible()) {
      JTextFieldMax.repaint();
    }
    if (JTextFieldMin.isVisible()) {
      JTextFieldMin.repaint();
    }

    Graphics2.setTransform(AffineTransform2);
  } // paint()


  /**
   * Method called by the awt-swing mechanism when the user press the mouse button over this component
   *
   * @param MouseEvent_Arg  the mouse event generatedby the awt-swing mechanism
   */
  public void mousePressed(
    MouseEvent MouseEvent_Arg) {

    LastFiveEvents = new Vector<Point>();
    LastFiveEvents.add(MouseEvent_Arg.getPoint());

    int  MouseX  = MouseEvent_Arg.getY();
    int  MouseY  = JComponent1.getWidth() - MouseEvent_Arg.getX();

    if (!MinOnTop) {
      MouseX = JComponent1.getHeight() - MouseEvent_Arg.getY();
      MouseY = MouseEvent_Arg.getX();
    }

    if (JTextFieldMin.isVisible()) {
      JTextFieldMin.postActionEvent();
      while (JTextFieldMin.getActionListeners().length > 0) {
        JTextFieldMin.removeActionListener(JTextFieldMin.getActionListeners()[0]);
      }
      JTextFieldMin.setVisible(false);
      Ctrl.requestFocus();
    }
    if (JTextFieldMax.isVisible()) {
      JTextFieldMax.postActionEvent();
      while (JTextFieldMax.getActionListeners().length > 0) {
        JTextFieldMax.removeActionListener(JTextFieldMax.getActionListeners()[0]);
      }
      JTextFieldMax.setVisible(false);
      Ctrl.requestFocus();
    }
    if (Ctrl.isPrecise() && MouseEvent_Arg.getClickCount() > 1 && TheLeft_Polygon.contains(MouseX, MouseY)) {
      openPrecisionPopup(LEFT_POLYGON, new Point(MouseEvent_Arg.getX(), MouseEvent_Arg.getY()));
      return;
    } else if (Ctrl.isPrecise() && MouseEvent_Arg.getClickCount() > 1 && TheRight_Polygon.contains(MouseX, MouseY)) {
      openPrecisionPopup(RIGHT_POLYGON, new Point(MouseEvent_Arg.getX(), MouseEvent_Arg.getY()));
      return;
    }  // Clic the first number to drag it and change the SegmentCount
    else if (RectFirstLabel != null && MouseEvent_Arg.getClickCount() == 1 &&
      RectFirstLabel.contains(MouseX, MouseY) &&
      MouseEvent_Arg.isShiftDown()) {
      Dragging = SEGMENT_SIZE_INT;
    } else if (RectFirstLabel != null && MouseEvent_Arg.getClickCount() == 1 &&
      RectFirstLabel.contains(MouseX, MouseY) &&
      !MouseEvent_Arg.isShiftDown()) {
      Dragging = SEGMENT_SIZE;
    }  // The user want to modify the minimum by draging the left triangle
    else if (TheLeft_Polygon.contains(MouseX, MouseY) &&
      !MouseEvent_Arg.isAltDown() && !MouseEvent_Arg.isShiftDown()) {

      Dragging = LEFT_RULER;

      DeplBef = MouseX - LeftValue;
      DeplAft = RightValue - MouseX;

      JComponent1.repaint();
    } else if (TheLeft_Polygon.contains(MouseX, MouseY) &&
      !MouseEvent_Arg.isAltDown() && MouseEvent_Arg.isShiftDown()) {

      Dragging = SHIFT_LEFT_RULER;

      DeplBef = MouseX - LeftValue;
      DeplAft = RightValue - MouseX;

      JComponent1.repaint();
    }  // the user want to modify the maximum  by draging the left triangle
    else if (TheRight_Polygon.contains(MouseX, MouseY) &&
      !MouseEvent_Arg.isAltDown() && !MouseEvent_Arg.isShiftDown()) {
      Dragging = RIGHT_RULER;

      DeplBef = MouseX - LeftValue;
      DeplAft = RightValue - MouseX;

      JComponent1.repaint();
    }  // the user want to modify the maximum  by draging the left triangle
    else if (TheRight_Polygon.contains(MouseX, MouseY) &&
      !MouseEvent_Arg.isAltDown() && MouseEvent_Arg.isShiftDown()) {
      Dragging = SHIFT_RIGHT_RULER;

      DeplBef = MouseX - LeftValue;
      DeplAft = RightValue - MouseX;

      JComponent1.repaint();
    }  // The user may want to drag the segment.
    else if (MouseX > LeftValue &&
      MouseX < RightValue &&
      MouseY > Margin_Ruler_Top &&
      MouseY < Margin_Ruler_Top + RulerHeight &&
      !MouseEvent_Arg.isAltDown() && !MouseEvent_Arg.isShiftDown()
      ) {
      Dragging = SEGMENT;

      DeplBef = MouseX - LeftValue;
      DeplAft = RightValue - MouseX;

      JComponent1.repaint();
    }  // The user may want to drag the segment (but stay aligned on graduations)
    else if (MouseX > LeftValue &&
      MouseX < RightValue &&
      MouseY > Margin_Ruler_Top &&
      MouseY < Margin_Ruler_Top + RulerHeight &&
      !MouseEvent_Arg.isAltDown() && MouseEvent_Arg.isShiftDown()
      ) {
      Dragging = SHIFT_SEGMENT;

      DeplBef = MouseX - LeftValue;
      DeplAft = RightValue - MouseX;

      JComponent1.repaint();
    }  // The user may want to drag the segment.
    else if (TheRight_Polygon.contains(MouseX, MouseY) &&
      MouseEvent_Arg.isAltDown()) {
      Dragging = ALT_RIGHT_RULER;

      Center = (Ctrl.getMinimumColoredValue() + Ctrl.getMaximumColoredValue()) / 2;
      DeplBef = MouseX - LeftValue;
      DeplAft = RightValue - MouseX;

      JComponent1.repaint();
    }  // The user may want to drag the segment.
    else if (TheLeft_Polygon.contains(MouseX, MouseY) &&
      MouseEvent_Arg.isAltDown()) {
      Dragging = ALT_LEFT_RULER;

      Center = (Ctrl.getMinimumColoredValue() + Ctrl.getMaximumColoredValue()) / 2;
      DeplBef = MouseX - LeftValue;
      DeplAft = RightValue - MouseX;

      JComponent1.repaint();
    }  // Double click in a new segment with SHIFT pressed for concatenation of the segment
    else if (MouseEvent_Arg.isShiftDown() &&
      MouseEvent_Arg.getClickCount() > 1 &&
      MouseX > MARGIN_RULER_LEFT &&
      MouseX < MARGIN_RULER_LEFT + RulerWidth &&
      MouseY > Margin_Ruler_Top &&
      MouseY < Margin_Ruler_Top + RulerHeight) {

      int     d1               = MouseX - MARGIN_RULER_LEFT;
      int     GraduationCount  = (int)Math.floor(d1 / GraduationWidth);

      double  Min              = Ctrl.getMinimumValue() + (float)((GraduationCount * GraduationWidth) * (Ctrl.getMaximumValue() - Ctrl.getMinimumValue()) / RulerWidth);
      double  Max              = Ctrl.getMinimumValue() + (float)(((GraduationCount + 1) * GraduationWidth) * (Ctrl.getMaximumValue() - Ctrl.getMinimumValue()) / RulerWidth);

      // because the last segment is maybe smaller than a regular one
      if (Max > Ctrl.getMaximumValue()) {
        Max = Ctrl.getMaximumValue();
      }

      if (Min < Ctrl.getMinimumColoredValue()) {
        Ctrl.setColoredValues(Min, Ctrl.getMaximumColoredValue());
      } else if (Max > Ctrl.getMaximumColoredValue()) {
        Ctrl.setColoredValues(Ctrl.getMinimumColoredValue(), Max);
      }

      // Like that the user doesn't have to release the mouse to drag the segmentS
      Dragging = SHIFT_SEGMENT;
      DeplBef = MouseX - LeftValue;
      DeplAft = RightValue - MouseX;

      JComponent1.repaint();
    }  // Double click in a new segment
    else if (MouseEvent_Arg.getClickCount() > 1 &&
      MouseX > MARGIN_RULER_LEFT &&
      MouseX < MARGIN_RULER_LEFT + RulerWidth &&
      MouseY > Margin_Ruler_Top &&
      MouseY < Margin_Ruler_Top + RulerHeight) {

      int     d1               = MouseX - MARGIN_RULER_LEFT;
      int     GraduationCount  = (int)Math.floor(d1 / GraduationWidth);

      double  Min              = Ctrl.getMinimumValue() + (float)((GraduationCount * GraduationWidth) * (Ctrl.getMaximumValue() - Ctrl.getMinimumValue()) / RulerWidth);
      double  Max              = Ctrl.getMinimumValue() + (float)(((GraduationCount + 1) * GraduationWidth) * (Ctrl.getMaximumValue() - Ctrl.getMinimumValue()) / RulerWidth);

      // because the last segment is maybe smaller than a regular one
      if (Max > Ctrl.getMaximumValue()) {
        Max = Ctrl.getMaximumValue();
      }

      Ctrl.setColoredValues(Min, Max);

      // Like that the user doesn't have to release the mouse to drag the segment
      Dragging = SEGMENT;
      DeplBef = MouseX - LeftValue;
      DeplAft = RightValue - MouseX;
      JComponent1.repaint();
    }
  } // mousePressed()


  /**
   * Method called by the awt-swing mechanism when the user drag his mouse over this component
   *
   * @param MouseEvent_Arg  the mouse event generatedby the awt-swing mechanism
   */
  public void mouseDragged(
    MouseEvent MouseEvent_Arg) {

    if (((Point)LastFiveEvents.elementAt(0)).x != MouseEvent_Arg.getX() ||
      ((Point)LastFiveEvents.elementAt(0)).y != MouseEvent_Arg.getY()) {
      LastFiveEvents.add(0, (Point)MouseEvent_Arg.getPoint().clone());
    }
    if (LastFiveEvents.size() > 5) {
      LastFiveEvents.removeElementAt(5);
    }

    int  MouseX  = MouseEvent_Arg.getY();
    int  MouseY  = JComponent1.getWidth() - MouseEvent_Arg.getX();

    if (!MinOnTop) {
      MouseX = JComponent1.getHeight() - MouseEvent_Arg.getY();
      MouseY = MouseEvent_Arg.getX();
    }

    // In case the event is outside the bislider area
    if (MouseEvent_Arg.getX() > JComponent1.getSize().width) {
      MouseEvent_Arg.translatePoint(JComponent1.getSize().width - MouseEvent_Arg.getX(), 0);
    }
    if (MouseEvent_Arg.getX() < 0) {
      MouseEvent_Arg.translatePoint(MouseEvent_Arg.getX(), 0);
    }
    if (MouseEvent_Arg.getX() > JComponent1.getSize().height) {
      MouseEvent_Arg.translatePoint(0, JComponent1.getSize().height - MouseEvent_Arg.getY());
    }
    if (MouseEvent_Arg.getY() < 0) {
      MouseEvent_Arg.translatePoint(0, MouseEvent_Arg.getY());
    }

    // change the dragging mode during the drag !
    if (Dragging == LEFT_RULER && MouseEvent_Arg.isAltDown()) {
      Dragging = ALT_LEFT_RULER;
      Center = (Ctrl.getMinimumColoredValue() + Ctrl.getMaximumColoredValue()) / 2;
    }

    // idem
    if (Dragging == RIGHT_RULER && MouseEvent_Arg.isAltDown()) {
      Dragging = ALT_RIGHT_RULER;
      Center = (Ctrl.getMinimumColoredValue() + Ctrl.getMaximumColoredValue()) / 2;
    }

    // idem
    if (Dragging == LEFT_RULER && MouseEvent_Arg.isShiftDown()) {
      Dragging = SHIFT_LEFT_RULER;
    }

    // idem
    if (Dragging == RIGHT_RULER && MouseEvent_Arg.isShiftDown()) {
      Dragging = SHIFT_RIGHT_RULER;
    }

    // idem
    if (Dragging == SHIFT_SEGMENT && !MouseEvent_Arg.isShiftDown()) {
      Dragging = SEGMENT;
    }

    // idem
    if (Dragging == SEGMENT && MouseEvent_Arg.isShiftDown()) {
      Dragging = SHIFT_SEGMENT;
    }

    // idem
    if (Dragging == SEGMENT_SIZE && MouseEvent_Arg.isShiftDown()) {
      Dragging = SEGMENT_SIZE_INT;
    }

    // idem
    if (Dragging == SEGMENT_SIZE_INT && !MouseEvent_Arg.isShiftDown()) {
      Dragging = SEGMENT_SIZE;
    }

    // idem
    if (Dragging == ALT_LEFT_RULER && !MouseEvent_Arg.isAltDown()) {
      Dragging = LEFT_RULER;
    }

    // idem
    if (Dragging == ALT_RIGHT_RULER && !MouseEvent_Arg.isAltDown()) {
      Dragging = RIGHT_RULER;
    }

    // idem
    if (Dragging == SHIFT_LEFT_RULER && !MouseEvent_Arg.isShiftDown()) {
      Dragging = LEFT_RULER;
    }

    // idem
    if (Dragging == SHIFT_RIGHT_RULER && !MouseEvent_Arg.isShiftDown()) {
      Dragging = RIGHT_RULER;
    }

    // change the Segment size !
    if (Dragging == SEGMENT_SIZE) {
      double  SegSize  = (Ctrl.getMaximumValue() - Ctrl.getMinimumValue()) * (MouseX - MARGIN_RULER_LEFT) / RulerWidth;
      SegSize = Math.min(SegSize, Ctrl.getMaximumValue() - Ctrl.getMinimumValue());

      if (SegSize > 0) {
        Ctrl.setSegmentSize(SegSize);
      }
    }

    if (Dragging == SEGMENT_SIZE_INT) {
      double  SegSize  = Math.round((Ctrl.getMaximumValue() - Ctrl.getMinimumValue()) * (MouseX - MARGIN_RULER_LEFT) / RulerWidth);
      if (SegSize > 0 && SegSize < (Ctrl.getMaximumValue() - Ctrl.getMinimumValue())) {
        Ctrl.setSegmentSize(SegSize);
      }
    }

    // drag the minimum value with centering
    if (Dragging == ALT_LEFT_RULER) {
      double  NewValue  = Ctrl.getMinimumValue() + ((MouseX - DeplBef - MARGIN_RULER_LEFT) * (Ctrl.getMaximumValue() - Ctrl.getMinimumValue())) / RulerWidth;

      if (NewValue > Center) {
        NewValue = Center;
      }

      if (NewValue < Ctrl.getMinimumValue()) {
        NewValue = Ctrl.getMinimumValue();
      }

      if (Center * 2 - NewValue > Ctrl.getMaximumValue()) {
        NewValue = 2 * Center - Ctrl.getMaximumValue();
      }

      // change the value and call repaint to display the modifications
      Ctrl.setColoredValues(NewValue, 2 * Center - NewValue);

      JComponent1.repaint();
    }  // drag the maximum value with centering
    else if (Dragging == ALT_RIGHT_RULER) {
      double  NewValue  = Ctrl.getMinimumValue() + ((MouseX + DeplAft - MARGIN_RULER_LEFT) * (Ctrl.getMaximumValue() - Ctrl.getMinimumValue())) / RulerWidth;

      if (NewValue < Center) {
        NewValue = Center;
      }

      if (NewValue > Ctrl.getMaximumValue()) {
        NewValue = Ctrl.getMaximumValue();
      }

      if (Center * 2 - NewValue < Ctrl.getMinimumValue()) {
        NewValue = 2 * Center - Ctrl.getMinimumValue();
      }

      // change the value and call repaint to display the modifications
      Ctrl.setColoredValues(Center * 2 - NewValue, NewValue);
      JComponent1.repaint();
    }  // Drag the left triangle
    else if (Dragging == LEFT_RULER) {
      if (Ctrl.isPrecise() && isLastFiveEventsLeftRight()) {
        openPrecisionPopup(LEFT_POLYGON, new Point(MouseEvent_Arg.getX(), MouseEvent_Arg.getY()));
        return;
      }

      if (MouseX - DeplBef > RightValue) {
        Ctrl.setColoredValues(Ctrl.getMaximumColoredValue(), Ctrl.getMaximumColoredValue());
        JComponent1.repaint();
        return;
      } else if (MouseX - DeplBef < MARGIN_RULER_LEFT) {
        Ctrl.setColoredValues(Ctrl.getMinimumValue(), Ctrl.getMaximumColoredValue());
        JComponent1.repaint();
        return;
      }

      // change the value and call repaint to display the modifications
      double NewMinValue = Ctrl.getMinimumValue() + ((MouseX - DeplBef - MARGIN_RULER_LEFT) * (Ctrl.getMaximumValue() - Ctrl.getMinimumValue())) / RulerWidth;
      NewMinValue        = Math.min(NewMinValue, Ctrl.getMaximumColoredValue());
      Ctrl.setColoredValues(NewMinValue, Ctrl.getMaximumColoredValue());

      JComponent1.repaint();
      //  JComponent1.paintImmediately(0,0, JComponent1.getWidth(), JComponent1.getHeight());
    }  // Drag the left triangle aligned on graduation
    else if (Dragging == SHIFT_LEFT_RULER) {
      double  Size    = (float)(Ctrl.getMaximumColoredValue() - Ctrl.getMinimumColoredValue());

      if (MouseX - DeplBef > RightValue) {
        Ctrl.setColoredValues(Ctrl.getMaximumColoredValue(), Ctrl.getMaximumColoredValue());
        JComponent1.repaint();
        return;
      } else if (MouseX - DeplBef < MARGIN_RULER_LEFT) {
        Ctrl.setColoredValues(Ctrl.getMinimumValue(), Ctrl.getMaximumColoredValue());
        JComponent1.repaint();
        return;
      }

      double  DMin    = ((MouseX - DeplBef - MARGIN_RULER_LEFT) * (Ctrl.getMaximumValue() - Ctrl.getMinimumValue())) / RulerWidth;
      int     NewMin  = (int)Math.round(DMin / Ctrl.getSegmentSize());
      while (Ctrl.getMinimumValue() + (Ctrl.getSegmentSize() * NewMin) < Ctrl.getMinimumValue()) {
        NewMin++;
      }

      // change the value and call repaint to display the modifications
      Ctrl.setColoredValues(Ctrl.getMinimumValue() + (Ctrl.getSegmentSize() * NewMin),
        Ctrl.getMaximumColoredValue());

      JComponent1.repaint();
      //  JComponent1.paintImmediately(0,0, JComponent1.getWidth(), JComponent1.getHeight());
    }  // Drag the right triangle
    else if (Dragging == RIGHT_RULER) {
      if (Ctrl.isPrecise() && isLastFiveEventsLeftRight()) {
        openPrecisionPopup(RIGHT_POLYGON, new Point(MouseEvent_Arg.getX(), MouseEvent_Arg.getY()));
        return;
      }

      if (MouseX + DeplAft < LeftValue) {
        Ctrl.setColoredValues(Ctrl.getMinimumColoredValue(),
          Ctrl.getMinimumColoredValue());
        JComponent1.repaint();
        return;
      } else if (MouseX + DeplAft > MARGIN_RULER_LEFT + RulerWidth) {
        Ctrl.setColoredValues(Ctrl.getMinimumColoredValue(), Ctrl.getMaximumValue());
        JComponent1.repaint();
        return;
      }

      // change the value and call repaint to display the modifications
      double NewMaxValue = Ctrl.getMinimumValue() + ((MouseX + DeplAft - MARGIN_RULER_LEFT) * (Ctrl.getMaximumValue() - Ctrl.getMinimumValue())) / RulerWidth;
      NewMaxValue        = Math.max(NewMaxValue, Ctrl.getMinimumColoredValue());
      Ctrl.setColoredValues(Ctrl.getMinimumColoredValue(), NewMaxValue);

      JComponent1.repaint();
    }  // Drag the right triangle aligned on graduation
    else if (Dragging == SHIFT_RIGHT_RULER) {
      double  Size    = (float)(Ctrl.getMaximumColoredValue() - Ctrl.getMinimumColoredValue());

      if (MouseX + DeplAft < LeftValue) {
        Ctrl.setColoredValues(Ctrl.getMinimumColoredValue(), Ctrl.getMaximumValue());
        JComponent1.repaint();
        return;
      } else if (MouseX + DeplAft > MARGIN_RULER_LEFT + RulerWidth) {
        Ctrl.setColoredValues(Ctrl.getMinimumColoredValue(),
          Ctrl.getMaximumValue());
        JComponent1.repaint();
        return;
      }

      double  DMax    = ((MouseX + DeplAft + MARGIN_RULER_LEFT) * (Ctrl.getMaximumValue() - Ctrl.getMinimumValue())) / RulerWidth;
      int     NewMax  = (int)Math.round(DMax / Ctrl.getSegmentSize());
      while (Ctrl.getMinimumValue() + (Ctrl.getSegmentSize() * NewMax) > Ctrl.getMaximumValue()) {
        NewMax--;
      }

      // change the value and call repaint to display the modifications
      Ctrl.setColoredValues(Ctrl.getMinimumColoredValue(),
        Ctrl.getMinimumValue() + (Ctrl.getSegmentSize() * NewMax));

      JComponent1.repaint();
    }  // Drag and drop the segment but align the minimum with a graduation
    else if (Dragging == SHIFT_SEGMENT) {
      double  Size          = (float)(Ctrl.getMaximumColoredValue() - Ctrl.getMinimumColoredValue());

      // if the drag goes too left we must stop moving the segment
      if (MouseX - DeplBef <= MARGIN_RULER_LEFT) {
        Ctrl.setColoredValues(Ctrl.getMinimumValue(),
          Ctrl.getMinimumValue() + Size);
        JComponent1.repaint();
        return;
      }  // same at right
      else if (MouseX + DeplAft >= MARGIN_RULER_LEFT + RulerWidth) {
        Ctrl.setColoredValues(Ctrl.getMaximumValue() - Size,
          Ctrl.getMaximumValue());
        JComponent1.repaint();
        return;
      }

      double  DMin          = ((MouseX - DeplBef - MARGIN_RULER_LEFT) * (Ctrl.getMaximumValue() - Ctrl.getMinimumValue())) / RulerWidth;
      int     NewMin        = (int)Math.round(DMin / Ctrl.getSegmentSize());
      double  MagnetEffect  = (Ctrl.getSegmentSize() * NewMin) - DMin;

      // change the value and call repaint to display the modifications
      Ctrl.setColoredValues(Ctrl.getMinimumValue() + DMin + MagnetEffect,
        Ctrl.getMinimumValue() + DMin + MagnetEffect + Size);

      //  JComponent1.paintImmediately(0,0, JComponent1.getWidth(), JComponent1.getHeight());
      JComponent1.repaint();
    }  // Drag and drop the segment
    else if (Dragging == SEGMENT) {
      double  Size  = (float)(Ctrl.getMaximumColoredValue() - Ctrl.getMinimumColoredValue());

      // if the drag goes too left we must stop moving the segment
      if (MouseX - DeplBef <= MARGIN_RULER_LEFT) {
        Ctrl.setColoredValues(Ctrl.getMinimumValue(),
          Ctrl.getMinimumValue() + Size);
        JComponent1.repaint();
        return;
      }  // same at right
      else if (MouseX + DeplAft >= MARGIN_RULER_LEFT + RulerWidth) {
        Ctrl.setColoredValues(Ctrl.getMaximumValue() - Size,
          Ctrl.getMaximumValue());
        JComponent1.repaint();
        return;
      }

      double  DMin  = ((MouseX - DeplBef - MARGIN_RULER_LEFT) * (Ctrl.getMaximumValue() - Ctrl.getMinimumValue())) / RulerWidth;

      // change the value and call repaint to display the modifications
      Ctrl.setColoredValues(Ctrl.getMinimumValue() + DMin,
        Ctrl.getMinimumValue() + DMin + Size);

      //  JComponent1.paintImmediately(0,0, JComponent1.getWidth(), JComponent1.getHeight());
      JComponent1.repaint();
    }
  } // mouseDragged()


  /**
   * Method called by the awt-swing mechanism when the user release the mouse button over this component
   *
   * @param MouseEvent_Arg  the mouse event generatedby the awt-swing mechanism
   */
  public void mouseReleased(
    MouseEvent MouseEvent_Arg) {
    if (Dragging != PRECISE_LEFT_RULER && Dragging != PRECISE_RIGHT_RULER)
      Dragging = NONE;
    JComponent1.repaint();

    Ctrl.setColoredValues(Ctrl.getMinimumColoredValue(), Ctrl.getMaximumColoredValue());
  } // mouseReleased()


  /**
   * Method called by the awt-swing mechanism when the user move his mouse over this component
   *
   * @param MouseEvent_Arg  the mouse event generatedby the awt-swing mechanism
   */
  public void mouseMoved(
    MouseEvent MouseEvent_Arg) {

    int          MouseX               = MouseEvent_Arg.getY();
    int          MouseY               = JComponent1.getWidth() - MouseEvent_Arg.getX();

    if (!MinOnTop) {
      MouseX = JComponent1.getHeight() - MouseEvent_Arg.getY();
      MouseY = MouseEvent_Arg.getX();
    }

    int          OldMouseUnder        = MouseUnder;
    Rectangle2D  OldRectangleSegment  = RectangleSegment;

    // for mouseOver between the triangles
    Rectangle    Rect1                = TheRight_Polygon.getBounds();
    Rectangle    Rect2                = TheLeft_Polygon.getBounds();
    Rectangle2D  Rectangle2D1         = Rect1.createUnion(Rect2);

    // for segment mouse over
    int          d1                   = MouseX - MARGIN_RULER_LEFT;
    int          GraduationCount      = (int)Math.floor(d1 / GraduationWidth);

    int          LeftSegment          = MARGIN_RULER_LEFT + (int)((GraduationCount * GraduationWidth));
    int          RightSegment         = MARGIN_RULER_LEFT + (int)(((GraduationCount + 1) * GraduationWidth));
    if (RightSegment > MARGIN_RULER_LEFT + RulerWidth) {
      RightSegment = MARGIN_RULER_LEFT + RulerWidth;
    }

    /*
     * double       ValuesWidth          = Ctrl.getMaximumValue() - Ctrl.getMinimumValue();
     * int          d1                   = MouseEvent_Arg.getX() - MARGIN_RULER_LEFT;
     * double       d2                   = ((double)d1 * (Ctrl.getMaximumValue() - Ctrl.getMinimumValue())) / RulerWidth;
     * int          d4                   = ((int)(Ctrl.getMaximumValue() - Ctrl.getMinimumValue()) / Ctrl.getSegmentCount());
     * d1 = (int)(d2);
     * d1 = (int)Ctrl.getMinimumValue() + (d1 - (d1 % d4));
     * int          LeftSegment          = (int)(MARGIN_RULER_LEFT + (RulerWidth * (d1 - Ctrl.getMinimumValue())) / ValuesWidth);
     * int          RightSegment         = (int)(MARGIN_RULER_LEFT + (RulerWidth * (d1 + d4 - Ctrl.getMinimumValue())) / ValuesWidth);
     */
    if (MouseX >= MARGIN_RULER_LEFT && MouseX <= MARGIN_RULER_LEFT + RulerWidth) {
      RectangleSegment = new Rectangle(LeftSegment + 1, Margin_Ruler_Top + 1, RightSegment - LeftSegment - 2, RulerHeight - 2);
    } else {
      RectangleSegment = null;
    }

    if (MouseEvent_Arg.isShiftDown() && RectangleSegment != null) {
      RectangleSegment = Rectangle2D1.createUnion(RectangleSegment);
    }

    if (TheRight_Polygon.contains(MouseX, MouseY)) {
      MouseUnder = RIGHT_POLYGON;
    } else if (TheLeft_Polygon.contains(MouseX, MouseY)) {
      MouseUnder = LEFT_POLYGON;
    } else if (Rectangle2D1.contains(MouseX, MouseY)) {
      MouseUnder = SELECTION;
    } else if (RectFirstLabel != null && RectFirstLabel.contains(MouseX, MouseY)) {
      MouseUnder = FIRST_LABEL;
    } else if (RectLastLabel != null && RectLastLabel.contains(MouseX, MouseY)) {
      MouseUnder = LAST_LABEL;
    } else if (RectangleSegment != null && RectangleSegment.contains(MouseX, MouseY)) {
      MouseUnder = SEGMENT;
    } else {
      MouseUnder = NOTHING;
    }

    if (MouseUnder != OldMouseUnder) {
      JComponent1.repaint();
    }

    if (OldRectangleSegment != null && MouseUnder == SEGMENT && !OldRectangleSegment.equals(RectangleSegment)) {
      JComponent1.repaint();
    }
  } // mouseMoved()


  /**
   * wheel actions handler
   */
  public void mouseWheelMoved(MouseWheelEvent MouseWheelEvent_Arg) {    
    if (MinOnTop){
      double min = Ctrl.getMinimumColoredValue()+MouseWheelEvent_Arg.getWheelRotation()*Ctrl.getSegmentSize();
      double max = Ctrl.getMaximumColoredValue()+MouseWheelEvent_Arg.getWheelRotation()*Ctrl.getSegmentSize();
      if (min < Ctrl.getMinimumValue()){
        min = Ctrl.getMinimumValue();
        max = min+(Ctrl.getMaximumColoredValue()-Ctrl.getMinimumColoredValue());
      }
      if (max > Ctrl.getMaximumValue()){
        max = Ctrl.getMaximumValue();
        min = max-(Ctrl.getMaximumColoredValue()-Ctrl.getMinimumColoredValue());
      }      
      Ctrl.setColoredValues(min, max);    
    }else {
      super.mouseWheelMoved(MouseWheelEvent_Arg);
    }
  }
  
  
  /**
   * analyse the five last mouse position and tell if it's really following a vertical direction
   *
   * @return   The lastFiveEventsLeftRight value
   */
  private boolean isLastFiveEventsLeftRight() {
    if (LastFiveEvents.size() > 4) {
      Point  PointM0  = (Point)LastFiveEvents.elementAt(0);
      Point  PointM1  = (Point)LastFiveEvents.elementAt(1);
      Point  PointM2  = (Point)LastFiveEvents.elementAt(2);
      Point  PointM3  = (Point)LastFiveEvents.elementAt(3);
      Point  PointM4  = (Point)LastFiveEvents.elementAt(4);
      int    D1x      = PointM0.x - PointM1.x;
      int    D1y      = PointM0.y - PointM1.y;
      int    D2x      = PointM0.x - PointM2.x;
      int    D2y      = PointM0.y - PointM2.y;
      int    D3x      = PointM0.x - PointM3.x;
      int    D3y      = PointM0.y - PointM3.y;
      int    D4x      = PointM0.x - PointM4.x;
      int    D4y      = PointM0.y - PointM4.y;

      return ((Math.abs(D1x) > 5 && Math.abs(D1y) < 2) ||
        (Math.abs(D2x) > 5 && Math.abs(D2y) < 2) ||
        (Math.abs(D3x) > 5 && Math.abs(D3y) < 2) ||
        (Math.abs(D4x) > 5 && Math.abs(D4y) < 2));
    } else {
      return false;
    }
  } // isLastFiveEventsLeftRight()


  /**
   * open a JSlider in popup to precise the value
   *
   * @param Thumb_Arg  Description of the Parameter
   * @param Point_Arg  Description of the Parameter
   */
  private void openPrecisionPopup(
    int Thumb_Arg,
    Point Point_Arg) {

    JSlider1.setValue(0);
    JLabel1.setBackground(Ctrl.getBackground());
    JLabel1.setForeground(Ctrl.getForeground());
    JSlider1.setBackground(Ctrl.getSliderBackground());
    Hashtable<Integer,JLabel>      Hashtable1       = new Hashtable<Integer,JLabel>();
    DecimalFormat  DecimalFormater  = Ctrl.getDecimalFormater();

    double         MiddleVal        = Ctrl.getMinimumColoredValue();
    if (Thumb_Arg == RIGHT_POLYGON)
      MiddleVal = Ctrl.getMaximumColoredValue();

    double         Amplitude        = Ctrl.getSegmentSize() / Ctrl.getSegmentCount();

    if (Thumb_Arg == RIGHT_POLYGON && MiddleVal - Amplitude < Ctrl.getMinimumColoredValue()) {
      MiddleVal = Ctrl.getMinimumColoredValue() + Amplitude;
    } else if (Thumb_Arg == LEFT_POLYGON && MiddleVal + Amplitude > Ctrl.getMaximumColoredValue()) {
      MiddleVal = Ctrl.getMaximumColoredValue() - Amplitude;
    } else if (MiddleVal - Amplitude < Ctrl.getMinimumValue()) {
      MiddleVal = Ctrl.getMinimumValue() + Amplitude;
    } else if (MiddleVal + Amplitude > Ctrl.getMaximumValue()) {
      MiddleVal = Ctrl.getMaximumValue() - Amplitude;
    }

    for (int i = -100; i <= 100; i += 50) {
      double  Val  = 0d;
      Val = MiddleVal + ((double)i) / 100d * Amplitude;

      Hashtable1.put(new Integer(i), new JLabel(DecimalFormater.format(Val)));
    }
    JSlider1.setLabelTable(Hashtable1);

    PopupFactory   PopupFactory1    = PopupFactory.getSharedInstance();
    Point          Point1           = (Point)Point_Arg.clone();
    SwingUtilities.convertPointToScreen(Point1, Ctrl);
    if (Thumb_Arg == RIGHT_POLYGON) {
      JLabel1.setText(Ctrl.getDecimalFormater().format(Ctrl.getMaximumColoredValue()));
    } else if (Thumb_Arg == LEFT_POLYGON) {
      JLabel1.setText(Ctrl.getDecimalFormater().format(Ctrl.getMinimumColoredValue()));
    }

    Dimension ScreenSize  = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
    int       CenteredX   = Point1.x - JSlider1.getPreferredSize().width / 2;

    if (CenteredX+JPanel1.getPreferredSize().width>ScreenSize.width)
      CenteredX = ScreenSize.width-JPanel1.getPreferredSize().width;
    if (CenteredX<0)
      CenteredX = 0;

    final  Popup          Popup1           = PopupFactory1.getPopup(Ctrl, JPanel1, CenteredX, Point1.y - 8);
    Popup1.show();

    JSlider1.addMouseListener(
      new MouseAdapter() {
        public void mouseReleased(MouseEvent MouseEvent_Arg) {
          Popup1.hide();
          JSlider1.removeMouseListener(this);
          if (Dragging == PRECISE_RIGHT_RULER) {
            Ctrl.setMaximumColoredValue(Double.parseDouble(JLabel1.getText()));
          } else if (Dragging == PRECISE_LEFT_RULER) {
            Ctrl.setMinimumColoredValue(Double.parseDouble(JLabel1.getText()));
          }

          Ctrl.repaint();
        }
      });

    // This is the only trick I found to make the bislider lost the drag focus
    // and jump directly to the JSlider so the user doesn't have to release and re-click !
    try {
      Robot  Robot1  = new Robot();
      Robot1.mouseRelease(InputEvent.BUTTON1_MASK);
      Robot1.mouseMove(CenteredX + JSlider1.getPreferredSize().width / 2, Point1.y);
      Robot1.mousePress(InputEvent.BUTTON1_MASK);
    } catch (AWTException AWTException_Arg) {
      AWTException_Arg.printStackTrace();
    }

    if (Thumb_Arg == RIGHT_POLYGON) {
      Dragging = PRECISE_RIGHT_RULER;
      PreciseOpenedValue = Ctrl.getMaximumColoredValue();
    } else if (Thumb_Arg == LEFT_POLYGON) {
      Dragging = PRECISE_LEFT_RULER;
      PreciseOpenedValue = Ctrl.getMinimumColoredValue();
    }
  } // openPrecisionPopup()


  /**
   * Method called by the awt-swing mechanism when something/someone resize this component
   *
   * @param ComponentEvent_Arg  Description of the Parameter
   */
  public void componentResized(
    ComponentEvent ComponentEvent_Arg) {
    RulerHeight = Ctrl.getWidth()-Margin_Ruler_Top-MARGIN_RULER_BOTTOM;
    Ctrl.repaint();
  } // componentResized()


  /**
   * Description of the Method
   *
   * @param e  Description of the Parameter
   */
  public void componentShown(ComponentEvent e) {
  }


  /**
   * Description of the Method
   *
   * @param e  Description of the Parameter
   */
  public void componentHidden(ComponentEvent e) {
  }


  /**
   * Description of the Method
   *
   * @param e  Description of the Parameter
   */
  public void componentMoved(ComponentEvent e) {
  }

  

  /**
   * Description of the Method
   *
   * @param MouseEvent_Arg  Description of the Parameter
   */
  public void mouseClicked(MouseEvent MouseEvent_Arg) {

    int  MouseX  = MouseEvent_Arg.getY();
    int  MouseY  = JComponent1.getWidth() - MouseEvent_Arg.getX();

    if (!MinOnTop) {
      MouseX = JComponent1.getHeight() - MouseEvent_Arg.getY();
      MouseY = MouseEvent_Arg.getX();
    }
    // Double click on the first number. The user want to change the minimum value
    if (MouseEvent_Arg.getClickCount() > 1 &&
      RectFirstLabel.contains(MouseX, MouseY)) {

      Dragging = NONE;

      String  Text  = "" + Ctrl.getMinimumValue();
      if (Ctrl.getMinimumValue() == (int)Ctrl.getMinimumValue()) {
        Text = "" + (int)Ctrl.getMinimumValue();
      }

      JTextFieldMin.setText(Text);

      if (MinOnTop) {
        JTextFieldMin.setLocation((int)(JComponent1.getWidth() - RectFirstLabel.getY() - RectFirstLabel.getHeight()), (int)RectFirstLabel.getX());
      } else {
        JTextFieldMin.setLocation((int)RectFirstLabel.getY(), JComponent1.getHeight() - (int)RectFirstLabel.getX() - (int)RectFirstLabel.getWidth());
      }

      JTextFieldMin.setSize(JTextFieldMin.getPreferredSize().width + 10, JTextFieldMin.getPreferredSize().height);
      JTextFieldMin.setVisible(true);
      JTextFieldMin.requestFocus();

      JTextFieldMin.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            while (JTextFieldMin.getActionListeners().length > 0) {
              JTextFieldMin.removeActionListener(JTextFieldMin.getActionListeners()[0]);
            }
            changeMinValue(JTextFieldMin.getText());
            JTextFieldMin.setVisible(false);
          }
        });

      JComponent1.repaint();
    }  // Double click on the last number. The user want to change the maximum value
    else if (MouseEvent_Arg.getClickCount() > 1 &&
      RectLastLabel.contains(MouseX, MouseY)) {

      Dragging = NONE;
      String  Text  = "" + Ctrl.getMaximumValue();
      if (Ctrl.getMaximumValue() == (int)Ctrl.getMaximumValue()) {
        Text = "" + (int)Ctrl.getMaximumValue();
      }

      JTextFieldMax.setText(Text);
      if (MinOnTop) {
        JTextFieldMax.setLocation((int)(JComponent1.getWidth() - RectLastLabel.getY() - RectLastLabel.getHeight()), (int)RectLastLabel.getX());
      } else {
        JTextFieldMax.setLocation((int)RectLastLabel.getY(), JComponent1.getHeight() - (int)RectLastLabel.getX() - (int)RectLastLabel.getWidth());
      }

      //JTextFieldMax.setLocation(JComponent1.getWidth() - JTextFieldMax.getPreferredSize().width - MARGIN_RULER_RIGHT - 10, 0);
      JTextFieldMax.setSize(JTextFieldMax.getPreferredSize().width + 10, JTextFieldMax.getPreferredSize().height);
      JTextFieldMax.setVisible(true);
      JTextFieldMax.requestFocus();
      JTextFieldMax.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            while (JTextFieldMax.getActionListeners().length > 0) {
              JTextFieldMax.removeActionListener(JTextFieldMax.getActionListeners()[0]);
            }
            changeMaxValue(JTextFieldMax.getText());
            JTextFieldMax.setVisible(false);
          }
        });
    }
  } // mouseClicked()


  /**
   * Description of the Method
   *
   * @param MouseEvent_Arg  Description of the Parameter
   */
  public void mouseEntered(MouseEvent MouseEvent_Arg) {
  }


  /**
   * Description of the Method
   *
   * @param MouseEvent_Arg  Description of the Parameter
   */
  public void mouseExited(MouseEvent MouseEvent_Arg) {
    MouseUnder = NOTHING;
    JComponent1.repaint();
  }

}

