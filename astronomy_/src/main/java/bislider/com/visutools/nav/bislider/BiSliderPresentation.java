package bislider.com.visutools.nav.bislider;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Shape;
import java.awt.SystemColor;
import javax.swing.*;
import javax.swing.event.*;
import java.io.*;
import java.text.*;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalLookAndFeel;

/**
 * The graphical interface of the bean (drawing and mouse event handling). <br>
 * <br>
 *
 * <table border=1 width="90%">
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
 *       cailles street, Paris, 75013, France. Tel: (+33) 871 747 387. eMail: Frederic.Vernier@laposte.net / Web site: http://vernier.frederic.free.fr
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
 * <li> Format : 01/11/2001
 * <li> Last Modif : 11/02/2003
 * <br>
 * <b>Bugs:</b> <br>
 * <li> ???
 * <br>
 * <b>To Do:</b> <br>
 * <li> alt or shift pressed -> repaint
 *
 * @author    Frederic Vernier, Frederic.Vernier@laposte.net
 * @version   1.4.1
 * @created   16 f�vrier 2004
 */

public class BiSliderPresentation extends ComponentUI 
implements Serializable, MouseListener, MouseMotionListener, MouseWheelListener, ComponentListener {
  //------------ MODIFIERS|-------------------- Type|----------------------------------------------- Name = Init value
  protected final static  javax.swing.text.html.parser.ParserDelegator  MAXIMUM_VARIABLE_SIZE_FOR_NAME  = null;
  
  final static            long                                          serialVersionUID                = 6420110040552724883L;
  
  protected               BiSlider                                      Ctrl                            = null;
  protected               JComponent                                    JComponent1                     = null;
  
  protected               int[][]                                       ColorTable                      = null;
  protected               long                                          j                               = 0;
  protected               int                                           NbAff                           = 0;
  
  // for feedback
  protected final         int                                           NOTHING                         = 200;
  protected final         int                                           RIGHT_POLYGON                   = 201;
  protected final         int                                           LEFT_POLYGON                    = 202;
  protected final         int                                           SELECTION                       = 203;
  protected final         int                                           FIRST_LABEL                     = 204;
  protected final         int                                           LAST_LABEL                      = 205;
  protected               int                                           MouseUnder                      = NOTHING;
  
  // Triangle polygons for the rulers
  protected               Polygon                                       TheLeft_Polygon                 = null;
  protected               Polygon                                       TheRight_Polygon                = null;
  
  protected               Rectangle2D                                   RectFirstLabel                  = null;
  protected               Rectangle2D                                   RectLastLabel                   = null;
  protected               Rectangle2D                                   RectangleSegment                = null;
  
  // indicate if the tool has already been painted on screen or never.
  protected               boolean                                       NeverDrawn                      = true;
  
  // graduation variables
  protected               double                                        GraduationWidth                 = 1;
  
  // we need to remember the precise value opened when the precise popup shows up because we will move around it
  protected               double                                        PreciseOpenedValue              = 0;
  
  // Constants for the Dragging variable
  protected final static  int                                           NONE                            = 100;
  protected final static  int                                           LEFT_RULER                      = 101;
  protected final static  int                                           RIGHT_RULER                     = 102;
  protected final static  int                                           SHIFT_LEFT_RULER                = 103;
  protected final static  int                                           SHIFT_RIGHT_RULER               = 104;
  protected final static  int                                           SEGMENT                         = 105;
  protected final static  int                                           SHIFT_SEGMENT                   = 106;
  protected final static  int                                           ALT_LEFT_RULER                  = 107;
  protected final static  int                                           ALT_RIGHT_RULER                 = 108;
  protected final static  int                                           SEGMENT_SIZE                    = 109;
  protected final static  int                                           SEGMENT_SIZE_INT                = 110;
  protected final static  int                                           PRECISE_LEFT_RULER              = 111;
  protected final static  int                                           PRECISE_RIGHT_RULER             = 112;
  protected final static  double                                        PRECISE_SCALE_FACTOR            = 1.0;

  protected               int                                           Dragging                        = NONE;
  protected               int                                           LeftValue                       = 0;
  protected               int                                           RightValue                      = 0;
  
  protected               int                                           DeplBef                         = 0;
  protected               int                                           DeplAft                         = 0;
  protected               double                                        Center                          = 0;
  
  // The width and height can change when the tool is resized
  protected               int                                           RulerWidth                      = 0;
  protected final static  int                                           MINIMUM_RULER_HEIGHT            = 20;
  protected final static  int                                           PREFERRED_RULER_HEIGHT          = 20;
  protected               int                                           RulerHeight                     = PREFERRED_RULER_HEIGHT;
  
  // but not the margins.
  protected final static  int                                           MARGIN_RULER_LEFT               = 10;
  protected               int                                           Margin_Ruler_Top                = 13;
  protected final static  int                                           MARGIN_RULER_RIGHT              = 10;
  protected final static  int                                           MARGIN_RULER_BOTTOM             = 3;
  
  //protected               Dimension                                     PreferredSize                     = new Dimension(5, 5);
  //protected               Dimension                                     MinimumSize                       = new Dimension(5, 5);
  
  protected               JTextField                                    JTextFieldMin                   = new JTextField("");
  protected               JTextField                                    JTextFieldMax                   = new JTextField("");
  
  protected               Vector<Point>                                 LastFiveEvents                  = null;
  protected               JPanel                                        JPanel1                         = new JPanel();
  protected               JSlider                                       JSlider1                        = new JSlider(JSlider.VERTICAL, -100, 100, 0);
  protected               JLabel                                        JLabel1                         = new JLabel("0");
  protected               ContentPainterSupport                         ContentPainterSupport1          = null;
  
  /**
   * Contructor, create the polygons and other nested object then register mouse callbacks.
   *
   * @param Ctrl_Arg  Description of the Parameter
   */
  public BiSliderPresentation(BiSlider Ctrl_Arg, ContentPainterSupport ContentPainterSupport_Arg) {
    this.Ctrl                   = Ctrl_Arg;
    this.ContentPainterSupport1 = ContentPainterSupport_Arg;
    
    Ctrl.setBackground(null);
        
    JPanel1.setLayout(new BorderLayout());
    JPanel1.setBorder(BorderFactory.createLineBorder(Ctrl.getForeground()));
    JSlider1.setPaintLabels(true);
    JSlider1.setMajorTickSpacing(20);
    JSlider1.setMinorTickSpacing(1);
    JPanel1.add(JSlider1, BorderLayout.CENTER);
    JPanel1.add(JLabel1, BorderLayout.SOUTH);
    JLabel1.setHorizontalAlignment(SwingConstants.CENTER);
    JLabel1.setOpaque(true);
    JSlider1.setOpaque(true);
    JPanel1.revalidate();
    
    /*JSlider1.addMouseListener(new MouseAdapter(){
      public void mousePressed(MouseEvent MouseEvent_Arg){
        System.out.println(""+MouseEvent_Arg);
      }
    });*/
    
    JSlider1.addChangeListener(
    new ChangeListener() {
      public void stateChanged(ChangeEvent ChangeEvent_Arg) {
        
        double  MiddleVal  = PreciseOpenedValue;
        
        double  Amplitude  = Ctrl.getSegmentSize() / Ctrl.getSegmentCount();
        if (Dragging == PRECISE_RIGHT_RULER && MiddleVal - Amplitude < Ctrl.getMinimumColoredValue()) {
          MiddleVal = Ctrl.getMinimumColoredValue() + Amplitude;
        } else if (Dragging == PRECISE_LEFT_RULER && MiddleVal + Amplitude > Ctrl.getMaximumColoredValue()) {
          MiddleVal = Ctrl.getMaximumColoredValue() - Amplitude;
        } else if (MiddleVal - Amplitude < Ctrl.getMinimumValue()) {
          MiddleVal = Ctrl.getMinimumValue() + Amplitude;
        } else if (MiddleVal + Amplitude > Ctrl.getMaximumValue()) {
          MiddleVal = Ctrl.getMaximumValue() - Amplitude;
        }
        
        double  Val        = MiddleVal + ((double)JSlider1.getValue()) / PRECISE_SCALE_FACTOR * Amplitude;
        JLabel1.setText(Ctrl.getDecimalFormater().format(Val));
        
        if (Dragging == PRECISE_LEFT_RULER)
          Ctrl.setMinimumColoredValue(Val);
        if (Dragging == PRECISE_RIGHT_RULER)
          Ctrl.setMaximumColoredValue(Val);
      }
    });
    
//    if ((UIManager.getLookAndFeel().getName().equals("Metal"))||(UIManager.getLookAndFeel().getName().contains("Windows"))) {
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
//
//    } else { // Triangles by default
//      TheLeft_Polygon = new Polygon();
//      TheLeft_Polygon.addPoint(LeftValue ,                Margin_Ruler_Top + RulerHeight - RulerHeight/2 - 1);
//      TheLeft_Polygon.addPoint(LeftValue + RulerHeight/2 + 2, Margin_Ruler_Top + RulerHeight + 1);
//      TheLeft_Polygon.addPoint(LeftValue ,                Margin_Ruler_Top + RulerHeight + 1);
//      TheLeft_Polygon.addPoint(LeftValue ,                Margin_Ruler_Top + RulerHeight - RulerHeight/2 - 1);
//
//      TheRight_Polygon = new Polygon();
//      TheRight_Polygon.addPoint(RightValue - 2 - RulerHeight/2, Margin_Ruler_Top - 1);
//      TheRight_Polygon.addPoint(RightValue , Margin_Ruler_Top - 1);
//      TheRight_Polygon.addPoint(RightValue , Margin_Ruler_Top + RulerHeight/2 + 1);
//      TheRight_Polygon.addPoint(RightValue - 2 - RulerHeight/2, Margin_Ruler_Top - 1);
//    }
    JComponent1 = Ctrl;
    
    JComponent1.addMouseListener(this);
    JComponent1.addMouseMotionListener(this);
    JComponent1.addMouseWheelListener(this);
    JComponent1.addComponentListener(this);
    
    JComponent1.setLayout(null);
    //PreferredSize =
    //MinimumSize   = );
    Ctrl.setPreferredSize(new Dimension(MARGIN_RULER_LEFT + MARGIN_RULER_RIGHT + 16 * Ctrl.getSegmentCount(), Margin_Ruler_Top + MARGIN_RULER_BOTTOM + MINIMUM_RULER_HEIGHT));
    Ctrl.setMinimumSize(new Dimension(MARGIN_RULER_LEFT + MARGIN_RULER_RIGHT + 8 * Ctrl.getSegmentCount(), Margin_Ruler_Top + MARGIN_RULER_BOTTOM + PREFERRED_RULER_HEIGHT));
    
    JTextFieldMin.setVisible(false);
    JTextFieldMax.setVisible(false);
    
    JTextFieldMin.getDocument().addDocumentListener(
    new DocumentListener() {
      public void changedUpdate(DocumentEvent DocumentEvent_Arg) { }
      
      public void insertUpdate(DocumentEvent DocumentEvent_Arg) {
        String  Text  = JTextFieldMin.getText();
        if (!Ctrl.getUnit().equals("alpha"))
          try {
            double  Val  = Double.parseDouble(Text);
            JTextFieldMin.setBackground(Color.WHITE);
          } catch (NumberFormatException NumberFormatException_Arg) {
            JTextFieldMin.setBackground(new Color(255, 128, 128));
          }
      }
      
      public void removeUpdate(DocumentEvent DocumentEvent_Arg) {
        String  Text  = JTextFieldMin.getText();
        if (!Ctrl.getUnit().equals("alpha"))
          try {
            double  Val  = Double.parseDouble(Text);
            JTextFieldMin.setBackground(Color.WHITE);
          } catch (NumberFormatException NumberFormatException_Arg) {
            JTextFieldMin.setBackground(new Color(255, 128, 128));
          }
      }
      
    });
    
    JTextFieldMin.addKeyListener(
    new KeyAdapter() {
      public void keyTyped(KeyEvent e) {
        if (e.getKeyChar() == KeyEvent.VK_ESCAPE) {
          while (JTextFieldMin.getActionListeners().length > 0) {
            JTextFieldMin.removeActionListener(JTextFieldMin.getActionListeners()[0]);
          }
          JTextFieldMin.setVisible(false);
        }
        
        JTextFieldMin.setSize(JTextFieldMin.getPreferredSize().width + 10, JTextFieldMin.getPreferredSize().height);
      }
    });
    
    JComponent1.add(JTextFieldMin);
    
    JTextFieldMax.getDocument().addDocumentListener(
    new DocumentListener() {
      public void changedUpdate(DocumentEvent DocumentEvent_Arg) { }
      
      
      public void insertUpdate(DocumentEvent DocumentEvent_Arg) {
        String  Text  = JTextFieldMax.getText();
        if (!Ctrl.getUnit().equals("alpha"))        
          try {
            double  Val  = Double.parseDouble(Text);
            JTextFieldMax.setBackground(Color.WHITE);
          } catch (NumberFormatException NumberFormatException_Arg) {
            JTextFieldMax.setBackground(new Color(255, 128, 128));
          }
      }
      
      
      public void removeUpdate(DocumentEvent DocumentEvent_Arg) {
        String  Text  = JTextFieldMax.getText();
        if (!Ctrl.getUnit().equals("alpha"))
          try {
            double  Val  = Double.parseDouble(Text);
            JTextFieldMax.setBackground(Color.WHITE);
          } catch (NumberFormatException NumberFormatException_Arg) {
            JTextFieldMax.setBackground(new Color(255, 128, 128));
          }
      }
      
    });
    
    JTextFieldMax.addKeyListener(
    new KeyAdapter() {
      public void keyTyped(KeyEvent e) {
        if (e.getKeyChar() == KeyEvent.VK_ESCAPE) {
          while (JTextFieldMax.getActionListeners().length > 0) {
            JTextFieldMax.removeActionListener(JTextFieldMax.getActionListeners()[0]);
          }
          JTextFieldMax.setVisible(false);
        }
        
        int  OldX      = JTextFieldMax.getLocation().x;
        int  OldWidth  = JTextFieldMax.getSize().width;
        JTextFieldMax.setSize(JTextFieldMax.getPreferredSize().width + 10, JTextFieldMax.getPreferredSize().height);
        JTextFieldMax.setLocation(OldX + (OldWidth - JTextFieldMax.getSize().width), JTextFieldMax.getLocation().y);
      }
    });
    
    JComponent1.add(JTextFieldMax);
    
    JComponent1.setDoubleBuffered(true);
    //JComponent1.setSize(300, 50);
  } // constructor()
  
  
  /**
   * @param Color_Arg  Description of the Parameter
   * @return           the opposite color to make shadow of text with it
   */
  protected Color getOppositeColor(Color Color_Arg) {
    int  R   = Color_Arg.getRed();
    int  G   = Color_Arg.getGreen();
    int  B   = Color_Arg.getBlue();
    R = 255 - R;
    int  R2  = (Color_Arg.getRed() + 128) % 255;
    if (Math.abs(R - Color_Arg.getRed()) < Math.abs(R2 - Color_Arg.getRed())) {
      R = R2;
    }
    
    G = 255 - G;
    int  G2  = (Color_Arg.getGreen() + 128) % 255;
    if (Math.abs(G - Color_Arg.getGreen()) < Math.abs(G2 - Color_Arg.getGreen())) {
      G = G2;
    }
    
    B = 255 - B;
    int  B2  = (Color_Arg.getBlue() + 128) % 255;
    if (Math.abs(B - Color_Arg.getBlue()) < Math.abs(B2 - Color_Arg.getBlue())) {
      B = B2;
    }
    
    return new Color(R, G, B);
  } // getOppositeColor()
  
  
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
    
    RulerWidth = JComponent1.getSize().width - MARGIN_RULER_LEFT - MARGIN_RULER_RIGHT;
    GraduationWidth = (RulerWidth * Ctrl.getSegmentSize()) / (Ctrl.getMaximumValue() - Ctrl.getMinimumValue());
    
    NeverDrawn = false;
    
    int  NewLeftValue   = (int)(MARGIN_RULER_LEFT + ((Min_Arg - Ctrl.getMinimumValue()) * RulerWidth) / (Ctrl.getMaximumValue() - Ctrl.getMinimumValue()));
    int  NewRightValue  = (int)(MARGIN_RULER_LEFT + ((Max_Arg - Ctrl.getMinimumValue()) * RulerWidth) / (Ctrl.getMaximumValue() - Ctrl.getMinimumValue()));
    
    if (NewLeftValue <= NewRightValue && NewLeftValue >= MARGIN_RULER_LEFT && NewRightValue <= MARGIN_RULER_LEFT + RulerWidth) {
      LeftValue = NewLeftValue;
      RightValue = NewRightValue;
      
      int TriangleSide = RulerHeight / 2;
      TriangleSide = Math.max(Math.min(TriangleSide, 20), 10);
      
      if ((UIManager.getLookAndFeel().getName().equals("Metal"))||(UIManager.getLookAndFeel().getName().contains("Windows"))) {
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
        TheLeft_Polygon.addPoint(LeftValue ,                Margin_Ruler_Top + RulerHeight - TriangleSide - 1);
        TheLeft_Polygon.addPoint(LeftValue + TriangleSide + 2, Margin_Ruler_Top + RulerHeight + 1);
        TheLeft_Polygon.addPoint(LeftValue ,                Margin_Ruler_Top + RulerHeight + 1);
        TheLeft_Polygon.addPoint(LeftValue ,                Margin_Ruler_Top + RulerHeight - TriangleSide - 1);

        TheRight_Polygon = new Polygon();
        TheRight_Polygon.addPoint(RightValue - 2 - TriangleSide, Margin_Ruler_Top - 1);
        TheRight_Polygon.addPoint(RightValue , Margin_Ruler_Top - 1);
        TheRight_Polygon.addPoint(RightValue , Margin_Ruler_Top + TriangleSide + 1);
        TheRight_Polygon.addPoint(RightValue - 2 - TriangleSide, Margin_Ruler_Top - 1);      
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
   * @param graphics_Arg  the graphic context to draw things
   */
  public void update(Graphics graphics_Arg, JComponent comp) {  
    Color BackColor = null;
    Object SliderBackColor = UIManager.getLookAndFeel().getDefaults().get("Slider.background");
    if (SliderBackColor!=null || !(SliderBackColor instanceof ColorUIResource))
      BackColor         = (Color)SliderBackColor;
    if (BackColor==null || Ctrl.getBackground()!=null)
      BackColor = Ctrl.getBackground();      

    if (comp.isOpaque() && BackColor!=null){      
      graphics_Arg.setColor(BackColor);
      graphics_Arg.fillRect(0, 0, JComponent1.getSize().width, JComponent1.getSize().height);
    }
    paint(graphics_Arg);
  }// update()
  
  
  /**
   * Method called by update()
   *
   * @param graphics_Arg  the graphic context to draw things
   */
  public void paint(Graphics graphics_Arg) {
    if (graphics_Arg == null) 
      return;
    
    Graphics2D      Graphics2          = (Graphics2D)graphics_Arg;
    RenderingHints  RenderingHints2    = new RenderingHints(null);
    RenderingHints2.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    RenderingHints2.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    RenderingHints2.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    RenderingHints2.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
    Graphics2.setRenderingHints(RenderingHints2);
    
    Font             Font1             = Ctrl.getFont();
    Font             Font2             = new Font(Font1.getName(), Font.BOLD, Font1.getSize());
    Shape            OldClip           = Graphics2.getClip();
    //Shape            NewClip           = new Rectangle2D.Float(MARGIN_RULER_LEFT, Margin_Ruler_Top,RulerWidth, RulerHeight);
    Shape NewClip           = new RoundRectangle2D.Float(
      MARGIN_RULER_LEFT, Margin_Ruler_Top,
      RulerWidth, RulerHeight,
      Ctrl.getArcSize(), Ctrl.getArcSize());
    //Graphics2.setColor(Color.RED);
    //Graphics2.fill(NewClip);
    
    int              SegmentCount      = Ctrl.getSegmentCount();
    FontMetrics      TheFontMetrics    = Graphics2.getFontMetrics();
    RulerWidth                         = JComponent1.getSize().width - MARGIN_RULER_LEFT - MARGIN_RULER_RIGHT;
    double           ValuesWidth       = Ctrl.getMaximumValue() - Ctrl.getMinimumValue();
    
    if (NeverDrawn) {
      LeftValue = (int)(MARGIN_RULER_LEFT + ((Ctrl.getMinimumColoredValue() - Ctrl.getMinimumValue()) * RulerWidth) / (Ctrl.getMaximumValue() - Ctrl.getMinimumValue()));
      RightValue = (int)(MARGIN_RULER_LEFT + ((Ctrl.getMaximumColoredValue() - Ctrl.getMinimumValue()) * RulerWidth) / (Ctrl.getMaximumValue() - Ctrl.getMinimumValue()));
      TheRight_Polygon.translate(RulerWidth, 0);
      NeverDrawn = false;
    }
    
    if (RulerWidth != JComponent1.getSize().width - MARGIN_RULER_LEFT - MARGIN_RULER_RIGHT) {
      int  NewRulerWidth  = JComponent1.getSize().width - MARGIN_RULER_LEFT - MARGIN_RULER_RIGHT;
      
      NewRulerWidth = NewRulerWidth - NewRulerWidth % SegmentCount;
      
      int  NewLeftValue   = MARGIN_RULER_LEFT + ((LeftValue - MARGIN_RULER_LEFT) * (NewRulerWidth)) / RulerWidth;
      int  NewRightValue  = MARGIN_RULER_LEFT + ((RightValue - MARGIN_RULER_LEFT) * (NewRulerWidth)) / RulerWidth;
      
      TheLeft_Polygon.translate(NewLeftValue - LeftValue, 0);
      TheRight_Polygon.translate(NewRightValue - RightValue, 0);
      
      LeftValue  = NewLeftValue;
      RightValue = NewRightValue;
      
      RulerWidth = NewRulerWidth;
      
      GraduationWidth = (float)((RulerWidth * Ctrl.getSegmentSize()) / (Ctrl.getMaximumValue() - Ctrl.getMinimumValue()));
    }
        
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
    
    int             LastMax          = 0;
    
    /*NewClip           = new RoundRectangle2D.Float(
    MARGIN_RULER_LEFT+2, Margin_Ruler_Top,
    RulerWidth-3, RulerHeight,
    Ctrl.getArcSize(), Ctrl.getArcSize());*/
    
    // it is the right opportunity to look for how many full non colored segments there are before the fisrt one
    int SegmentCountBefore = 0;
    double[][]       ColTable          = Ctrl.getColorTable();

    String           Unit              = Ctrl.getUnit();    

    //Graphics2.setClip(NewClip);
    for (int i = 0; i <= SegmentCount; i++) {
      double           Val               = Ctrl.getMinimumValue() + i * Ctrl.getSegmentSize();
      double           Val2              = Ctrl.getMinimumValue() + (i+1) * Ctrl.getSegmentSize();
      
      if (Val<=ColTable[0][0] && ColTable[0][0]<Val2)
        SegmentCountBefore = i;
      
      String           NumberString      = "";
      int              NumberWidth       = 0;
      int              x                 = 0;
      
      String           MaxNumberString   = "";
      if (Unit.equals("alpha")) {
        MaxNumberString = ""+(char)((int)Ctrl.getMaximumValue()+'A');
      } else if (Ctrl.getMaximumValue() == (long)Ctrl.getMaximumValue()) {
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
        
        x = JComponent1.getSize().width - MARGIN_RULER_RIGHT - NumberWidth;
      } else {
        if (Unit.equals("alpha")) {
          NumberString = ""+(char)((int)Val+'A');
        } else if (Val == (long)Val) {
          NumberString = "" + ((long)Val) + Unit;
        } else {
          NumberString = "" + (((long)(Val * 10)) / 10f) + Unit;
        }
        NumberWidth = TheFontMetrics.stringWidth(NumberString);
        
        x = MARGIN_RULER_LEFT + (int)(GraduationWidth * i) - (int)((NumberWidth * ((float)i / SegmentCount)));
      }
      
      // get the color
      graphics_Arg.setColor(Ctrl.getForeground());
      
      /*if(Unit.equals("alpha")&& Val>20){
        Graphics2.setColor(Color.RED);
        Graphics2.drawLine((JComponent1.getSize().width - MARGIN_RULER_RIGHT ), 0, (JComponent1.getSize().width - MARGIN_RULER_RIGHT - TheFontMetrics.stringWidth(MaxNumberString)), 30);
        System.out.println(""+(x + NumberWidth)+" < "+(JComponent1.getSize().width - MARGIN_RULER_RIGHT - TheFontMetrics.stringWidth(MaxNumberString)));
      }*/
      
      if (Val == Ctrl.getMaximumValue() || Val == Ctrl.getMinimumValue()) {
        Graphics2.setFont(Font2);
        Graphics2.drawString(NumberString, x, Margin_Ruler_Top - 1);
        
        //restore the font
        Graphics2.setFont(Font1);
        LastMax = x + NumberWidth;
      }  // if not too close to the last one or too close to the previous one
      else if (x + NumberWidth < JComponent1.getSize().width - MARGIN_RULER_RIGHT - TheFontMetrics.stringWidth(MaxNumberString) && x >= LastMax + 2) {
        Graphics2.drawString(NumberString, x, Margin_Ruler_Top - 1);
        LastMax = x + NumberWidth;
      }
      
      if (LastMax == x + NumberWidth) {
        Graphics2.setColor(Ctrl.getSliderBackground().darker().darker());
      } else {
        Graphics2.setColor(Ctrl.getSliderBackground().darker());
      }
      
      if (ContentPainterSupport1.getPainterListenerNumber()>0 && Val < Ctrl.getMaximumValue()){
        int x0 = MARGIN_RULER_LEFT + (int)(GraduationWidth * i);
        int x3 = MARGIN_RULER_LEFT + (int)(GraduationWidth * (i+1));
        Rectangle Rect1bis = new Rectangle(x0, Margin_Ruler_Top + 2, x3 - x0, RulerHeight - 3);
        
        x3= Math.min(x3,MARGIN_RULER_LEFT+RulerWidth);
        Rectangle Rect1 = new Rectangle(x0, Margin_Ruler_Top + 2, x3 - x0, RulerHeight - 3);
        
        Shape ShapeClip = Graphics2.getClip();
        Color Color0    = Graphics2.getColor();
        Graphics2.setClip(NewClip);
        Graphics2.clip(Rect1);
        
        ContentPainterSupport1.firePaint(Ctrl, Graphics2, Val, Val2, i, null, Rect1bis, Rect1bis);
        Graphics2.setClip(ShapeClip);
        Graphics2.setColor(Color0);
      }
      
      if (i!=0 && Val < Ctrl.getMaximumValue()) {
        if (ContentPainterSupport1.getPainterListenerNumber()==0)
          Graphics2.drawLine(MARGIN_RULER_LEFT + (int)(GraduationWidth * i), Margin_Ruler_Top + 2,
          MARGIN_RULER_LEFT + (int)(GraduationWidth * i), Margin_Ruler_Top + RulerHeight - 2);
      }
      
      if (i == 0) {
        RectFirstLabel = new Rectangle(x - 1, Margin_Ruler_Top - TheFontMetrics.getAscent(), NumberWidth + 1, TheFontMetrics.getAscent());
      }
      
      if (i == SegmentCount) {
        RectLastLabel = new Rectangle(x - 1, Margin_Ruler_Top - TheFontMetrics.getAscent(), NumberWidth + 1, TheFontMetrics.getAscent());
      }
      
      if (MouseUnder == FIRST_LABEL) {
        Graphics2.draw(RectFirstLabel);
      }
      if (MouseUnder == LAST_LABEL) {
        Graphics2.draw(RectLastLabel);
      }
    }
    
    if (MouseUnder == SEGMENT && RectangleSegment != null) {
      Graphics2.setColor(SystemColor.control.darker());
      Graphics2.draw(RectangleSegment);
    }
    
    //ColorisationEvent  CE1             = new ColorisationEvent(this, Ctrl.getColorTable());
    // colored segment
    for (int i = 0; i < ColTable.length; i++) {
      Graphics2.setColor(new Color((int)(ColTable[i][2])));
      if (ColTable[i][0] < ColTable[i][1]) {
        //the selection only
        int  x1  = (int)(MARGIN_RULER_LEFT + (RulerWidth * (ColTable[i][0] - Ctrl.getMinimumValue())) / ValuesWidth) + 1;
        int  x2  = (int)(MARGIN_RULER_LEFT + (RulerWidth * ((ColTable[i][1]) - Ctrl.getMinimumValue())) / ValuesWidth);
        
        // the full segment
        int  x0  = MARGIN_RULER_LEFT + (int)(GraduationWidth * (int)ColTable[i][3]);
        int  x3  = MARGIN_RULER_LEFT + (int)(GraduationWidth * (int)(ColTable[i][3]+1));
        Rectangle Rect1bis = new Rectangle(x1, Margin_Ruler_Top + 2, x2 - x1, RulerHeight - 3);
        
        x3= Math.min(x3,MARGIN_RULER_LEFT+RulerWidth);
        Rectangle Rect1    = new Rectangle(x1, Margin_Ruler_Top + 2, x2 - x1, RulerHeight - 3);
        Rectangle RectClip = new Rectangle(x1-1, Margin_Ruler_Top + 2, x2 - x1+1, RulerHeight - 3);
        Rectangle Rect2    = new Rectangle(x0, Margin_Ruler_Top + 2, x3 - x0, RulerHeight - 3);
        
        if (ContentPainterSupport1.getPainterListenerNumber()==0) {
          Graphics2.fill(Rect1);
          if (i!=0){
            Graphics2.setColor(new Color(Graphics2.getColor().getRed(), Graphics2.getColor().getGreen(), Graphics2.getColor().getBlue(), 192));
            Graphics2.drawLine(x1-1, Margin_Ruler_Top + 3, x1-1, Margin_Ruler_Top+RulerHeight - 3);
          }
        } else {
          Shape ShapeClip = Graphics2.getClip();
          Color Color0    = Graphics2.getColor();
          Graphics2.setClip(NewClip);
          Graphics2.clip(RectClip);
          ContentPainterSupport1.firePaint(Ctrl, Graphics2, ColTable[i][0], ColTable[i][1], i+SegmentCountBefore, 
                                            Graphics2.getColor(), Rect1bis, Rect2);
          Graphics2.setClip(OldClip);
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
      Graphics2.fillRect((int)(Rectangle2D1.getX() + Rectangle2D1.getWidth() / 2), Margin_Ruler_Top, 2, RulerHeight);
    }
    
    //Graphics2.setColor(getOppositeColor(Ctrl.getBackground()));
    Graphics2.setColor(Color.BLACK);
    Graphics2.drawRoundRect(MARGIN_RULER_LEFT, Margin_Ruler_Top, RulerWidth, RulerHeight, Ctrl.getArcSize(), Ctrl.getArcSize());
    
    paintThumbs(Graphics2);
    
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
      
      Graphics2.setColor(getOppositeColor(Ctrl.getForeground()));
      Graphics2.drawString(ValString, RightValue + 3, Margin_Ruler_Top + TheFontMetrics.getAscent() + 1);
      Graphics2.setColor(Ctrl.getForeground());
      Graphics2.drawString(ValString, RightValue + 2, Margin_Ruler_Top + TheFontMetrics.getAscent());
    }
    
    if (Dragging == ALT_RIGHT_RULER || Dragging == ALT_LEFT_RULER || Dragging == LEFT_RULER ||
    Dragging == SEGMENT || Dragging == SHIFT_SEGMENT || Dragging == SHIFT_LEFT_RULER) {
      String  ValString  = "";
      if (Unit.equals("alpha")) {
        ValString = ""+(char)((int)Ctrl.getMinimumColoredValue()+'A');
      } else if (Math.abs(((int)Ctrl.getMinimumColoredValue()) - Ctrl.getMinimumColoredValue()) < 0.0001) {
        ValString = "" + (int)Ctrl.getMinimumColoredValue();
      } else {
        ValString = Ctrl.getDecimalFormater().format(Ctrl.getMinimumColoredValue());
      }
      
      Graphics2.setColor(getOppositeColor(Ctrl.getForeground()));
      Graphics2.drawString(ValString, LeftValue - TheFontMetrics.stringWidth(ValString) - 1, Margin_Ruler_Top + RulerHeight - 1);
      Graphics2.setColor(Ctrl.getForeground());
      Graphics2.drawString(ValString, LeftValue - TheFontMetrics.stringWidth(ValString) - 2, Margin_Ruler_Top + RulerHeight - 2);
    }
    
    if (JTextFieldMax.isVisible()) {
      JTextFieldMax.repaint();
    }
    if (JTextFieldMin.isVisible()) {
      JTextFieldMin.repaint();
    }
  } // paint()
  
  
  /**
   * paint a thumb
   */
  public void paintThumbs(Graphics graphics_Arg)  {
    Graphics2D Graphics2 = (Graphics2D)graphics_Arg;
    //System.out.println("getLookAndFeel="+UIManager.getLookAndFeel().getName());
    //System.out.println("getCurrentTheme="+MetalLookAndFeel.getCurrentTheme().getName());
    if ((UIManager.getLookAndFeel().getName().equals("Metal"))||(UIManager.getLookAndFeel().getName().contains("Windows"))) {
      if (Dragging == LEFT_RULER || Dragging == SHIFT_LEFT_RULER || Dragging == ALT_LEFT_RULER)
        paintThumb(graphics_Arg, true, TheLeft_Polygon, 
                  MetalLookAndFeel.getPrimaryControlShadow(),
                  MetalLookAndFeel.getPrimaryControlDarkShadow(),
                  MetalLookAndFeel.getPrimaryControl());
      else if (MouseUnder == LEFT_POLYGON)
        paintThumb(graphics_Arg, true, TheLeft_Polygon, 
                  MetalLookAndFeel.getControlShadow(),
                  MetalLookAndFeel.getControlDarkShadow(),
                  MetalLookAndFeel.getControl());                 
      else 
        paintThumb(graphics_Arg, true, TheLeft_Polygon, 
                  MetalLookAndFeel.getControl(),
                  MetalLookAndFeel.getControlShadow(),
                  MetalLookAndFeel.getControlHighlight());

      if (Dragging == RIGHT_RULER || Dragging == SHIFT_RIGHT_RULER || Dragging == ALT_RIGHT_RULER)
        paintThumb(graphics_Arg, false, TheRight_Polygon,
                  MetalLookAndFeel.getPrimaryControlShadow(),
                  MetalLookAndFeel.getPrimaryControlDarkShadow(),
                  MetalLookAndFeel.getPrimaryControl());
      else if ((MouseUnder == RIGHT_POLYGON))
        paintThumb(graphics_Arg, false, TheRight_Polygon,
                  MetalLookAndFeel.getControlShadow(),
                  MetalLookAndFeel.getControlDarkShadow(),
                  MetalLookAndFeel.getControl());    
      else 
        paintThumb(graphics_Arg, false, TheRight_Polygon,
                  MetalLookAndFeel.getControl(),
                  MetalLookAndFeel.getControlShadow(),
                  MetalLookAndFeel.getControlHighlight());
        
    } else { // default thumb = triangle
      Graphics2.setColor(SystemColor.control);
      if (Dragging == LEFT_RULER || Dragging == SHIFT_LEFT_RULER || Dragging == ALT_LEFT_RULER)
        Graphics2.setColor(MetalLookAndFeel.getPrimaryControlShadow());
      else if (MouseUnder == LEFT_POLYGON)
        Graphics2.setColor(MetalLookAndFeel.getControlHighlight());
      Graphics2.fillPolygon(TheLeft_Polygon);

      Graphics2.setColor(SystemColor.control);
      if (Dragging == RIGHT_RULER || Dragging == SHIFT_RIGHT_RULER || Dragging == ALT_RIGHT_RULER)
        Graphics2.setColor(MetalLookAndFeel.getPrimaryControlShadow());
      else if (MouseUnder == RIGHT_POLYGON)
        Graphics2.setColor(MetalLookAndFeel.getControlHighlight());
      Graphics2.fillPolygon(TheRight_Polygon);

      // shadow
      Graphics2.setColor(SystemColor.control.brighter());
      Graphics2.drawLine(TheLeft_Polygon.xpoints[0]+1, TheLeft_Polygon.ypoints[0]+2,
      TheLeft_Polygon.xpoints[2]+1, TheLeft_Polygon.ypoints[2]-1);
      Graphics2.drawLine(TheRight_Polygon.xpoints[0]+1, TheRight_Polygon.ypoints[0]+1,
      TheRight_Polygon.xpoints[1]-1, TheRight_Polygon.ypoints[1]+1);
      Graphics2.setColor(SystemColor.control.darker());
      Graphics2.drawLine(TheLeft_Polygon.xpoints[1]-2, TheLeft_Polygon.ypoints[1]-1,
      TheLeft_Polygon.xpoints[2]+1, TheLeft_Polygon.ypoints[2]-1);
      Graphics2.drawLine(TheRight_Polygon.xpoints[1]-1, TheRight_Polygon.ypoints[1]+1,
      TheRight_Polygon.xpoints[2]-1, TheRight_Polygon.ypoints[2]-1);

      if (MouseUnder == SELECTION) {
        Rectangle    Rect1         = TheRight_Polygon.getBounds();
        Rectangle    Rect2         = TheLeft_Polygon.getBounds();
        Rectangle2D  Rectangle2D1  = Rect1.createUnion(Rect2);

        Graphics2.draw(Rectangle2D1);
      } else if (MouseUnder == LEFT_POLYGON) {
        //Graphics2.fillPolygon(TheLeft_Polygon);
        Graphics2.setColor(SystemColor.control.darker().darker());
        Graphics2.drawLine(TheLeft_Polygon.xpoints[0]+1, TheLeft_Polygon.ypoints[0]+2,
        TheLeft_Polygon.xpoints[2]+1, TheLeft_Polygon.ypoints[2]-1);
        Graphics2.setColor(SystemColor.control);
        Graphics2.drawLine(TheLeft_Polygon.xpoints[1]-2, TheLeft_Polygon.ypoints[1]-1,
        TheLeft_Polygon.xpoints[2]+1, TheLeft_Polygon.ypoints[2]-1);
      } else if (MouseUnder == RIGHT_POLYGON) {
        //Graphics2.fillPolygon(TheRight_Polygon);
        Graphics2.setColor(SystemColor.control.darker().darker());
        Graphics2.drawLine(TheRight_Polygon.xpoints[0]+1, TheRight_Polygon.ypoints[0]+1,
        TheRight_Polygon.xpoints[1]-1, TheRight_Polygon.ypoints[1]+1);
        Graphics2.setColor(SystemColor.control);
        Graphics2.drawLine(TheRight_Polygon.xpoints[1]-1, TheRight_Polygon.ypoints[1]+1,
        TheRight_Polygon.xpoints[2]-1, TheRight_Polygon.ypoints[2]-1);
      }

      //Graphics2.setColor(getOppositeColor(Ctrl.getBackground()));
      Graphics2.setColor(Color.BLACK);
      Graphics2.drawPolygon(TheLeft_Polygon);
      Graphics2.drawPolygon(TheRight_Polygon);
    } 
  }//paintThumb()
  
  
  // Parameters of the widget
  final private static int PADDING_TOP_BOTTOM = 20;  
  final private static int BTN_NONE = 0;
  final private static int BTN_UP = 1;
  final private static int BTN_MID = 2;
  final private static int BTN_DOWN = 3;
        
  /**
   * Paints shadowed dots on the given <tt>Graphics</tt> objects. The
   * coordinates of the points must be given in arrays.
   *
   * @param g the <tt>Graphics</tt> object on which we wand to paint the dots
   * @param xCoords array containing the X coordinates of the dots
   * @param yCoords array containing the Y coordinates of the dots
   * @param num the number of dots
   * @author Christophe Jacquet, Frederic Vernier
   */
  private void paintShadowedDots(Graphics g, int[] xCoords, int[] yCoords, int num, 
                            Color DarkColor_Arg, Color BrightColor_Arg) {
    for(int s=0; s<2; s++) {
      g.setColor(s==0 ? BrightColor_Arg : DarkColor_Arg);
      for(int i=0; i<num; i++) {
        g.drawLine(xCoords[i]+s, yCoords[i]+s, xCoords[i]+s, yCoords[i]+s);
      }
    }
  }// paintShadowedDots()
  
  
  /**
   * Draws an arrow (upward or downward) on a given <tt>Graphics</tt> object.
   *
   * @param g the <tt>Graphics</tt> object on which we want to paint the arrow
   * @param up if <tt>true</tt>, the arrow will be painted upward. If
   * @author Christophe Jacquet, Frederic Vernier
   * <tt>false</tt>, it will be painted downward
   */
  private void paintThumb(Graphics graphics_Arg, boolean up, Polygon Thumb_Arg,
                         Color MainColor_Arg, Color DarkColor_Arg, Color BrightColor_Arg) {
    
    Graphics2D G2 = (Graphics2D)graphics_Arg;
    //System.out.println("UIManager.getLookAndFeel() = "+UIManager.getLookAndFeel().getName());
    //System.out.println("MetalLookAndFeel.getCurrentTheme() = "+MetalLookAndFeel.getCurrentTheme().getName());
    
    if (UIManager.getLookAndFeel().getName().equals("Metal") && MetalLookAndFeel.getCurrentTheme().getName().equals("Ocean")) {
      Color BackColor = null;
      Object SliderBackColor = UIManager.getLookAndFeel().getDefaults().get("Slider.gradient");
      if (SliderBackColor!=null && SliderBackColor instanceof List){
        List GradientProperties = (List)SliderBackColor;
        Float Cut1 = (Float)GradientProperties.get(0);
        Float Cut2 = (Float)GradientProperties.get(1);
        Color Color1 = (Color)GradientProperties.get(2);
        Color Color2 = (Color)GradientProperties.get(3);
        Color Color3 = (Color)GradientProperties.get(4);
        float Rem = 1f-Cut2-2*Cut1;
        int x1 = up ? Thumb_Arg.xpoints[0] : Thumb_Arg.xpoints[6];
        int y1 = up ? Thumb_Arg.ypoints[0] : Thumb_Arg.ypoints[6];

        int x2 = up ? Thumb_Arg.xpoints[1] : Thumb_Arg.xpoints[5];
        int y2 = up ? Thumb_Arg.ypoints[1] : Thumb_Arg.ypoints[5];

        Shape Shape0 = G2.getClip(); 
        G2.setClip(Thumb_Arg);
        if (up) {
          GradientPaint GradientPaint1 = new GradientPaint(x1-7, y1, Color3, x1, y1+(int)(16f*Rem), Color1);
          GradientPaint GradientPaint2 = new GradientPaint(x1, y1+(int)(16f*Rem), Color1, x1, y1+(int)(16f*(Rem+Cut1)), Color2);
          GradientPaint GradientPaint3 = new GradientPaint(x1, y1+(int)(16f*(Rem+Cut1+Cut2)), Color2, x1, y1+16, Color3);

          G2.setPaint(GradientPaint1);
          G2.fillRect(x1-7, y1, 15, (int)(16f*Rem));
          G2.setPaint(GradientPaint2);
          G2.fillRect(x1-7, y1+(int)(16f*Rem), 15, (int)(16f*Cut1)+1);
          G2.setColor(Color2);
          G2.fillRect(x1-7, y1+(int)(16f*(Rem+Cut1)), 15, (int)(16f*Cut2)+1);
          G2.setPaint(GradientPaint3);
          G2.fillRect(x1-7, y1+(int)(16f*(Rem+Cut1+Cut2)), 15, (int)(16f*Cut1)+1);
        } else {
          GradientPaint GradientPaint1 = new GradientPaint(x1-7, y1, Color3, x1, y1-(int)(16f*Rem), Color1);
          GradientPaint GradientPaint2 = new GradientPaint(x1, y1-(int)(16f*Rem), Color1, x1, y1-(int)(16f*(Rem+Cut1)), Color2);
          GradientPaint GradientPaint3 = new GradientPaint(x1, y1-(int)(16f*(Rem+Cut1+Cut2)), Color2, x1, y1-16, Color3);

          G2.setPaint(GradientPaint1);
          G2.fillRect(x1-7, y1-(int)(16f*Rem), 15, (int)(16f*Rem));
          G2.setPaint(GradientPaint2);
          G2.fillRect(x1-7, y1-(int)(16f*(Rem+Cut1)), 15, (int)(16f*Cut1)+1);
          G2.setColor(Color2);
          G2.fillRect(x1-7, y1-(int)(16f*(Rem+Cut1+Cut2)), 15, (int)(16f*Cut2)+1);
          G2.setPaint(GradientPaint3);
          G2.fillRect(x1-7, y1-16, 15, (int)(16f*Cut1)+1);        
        }
        G2.setClip(Shape0);
      }

    } else {
      G2.setColor(new Color(MainColor_Arg.getRed(), MainColor_Arg.getGreen(), MainColor_Arg.getBlue(), 196));
      G2.fillPolygon(Thumb_Arg);
    }
    //System.out.println( ((OceanTheme)MetalLookAndFeel.getCurrentTheme()). getProperty("Button.gradient"));
      
    G2.setColor(Color.BLACK);
    G2.drawPolygon(Thumb_Arg);
    int yy = up ? Thumb_Arg.ypoints[0]+8 : Thumb_Arg.ypoints[6]-13;
    int xx = up ? Thumb_Arg.xpoints[0] : Thumb_Arg.xpoints[6];
    int[] xp = {xx-5, xx-1, xx+3, xx-3, xx+1, xx-5, xx-1, xx+3};
    int[] yp = {yy,   yy,   yy,   yy+2, yy+2, yy+4, yy+4, yy+4};
      
    paintShadowedDots(G2, xp, yp, 8, DarkColor_Arg, BrightColor_Arg);
  }// paintThumb()
    
  
  /**
   * Method called by the awt-swing mechanism when the user press the mouse button over this component
   *
   * @param MouseEvent_Arg  the mouse event generatedby the awt-swing mechanism
   */
  public void mousePressed(
  MouseEvent MouseEvent_Arg) {
    
    LastFiveEvents = new Vector<Point>();
    LastFiveEvents.add(MouseEvent_Arg.getPoint());
    
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
    
    if (Ctrl.isPrecise() && MouseEvent_Arg.getButton()==MouseEvent.BUTTON1 && MouseEvent_Arg.getClickCount() > 1 && TheLeft_Polygon.contains(MouseEvent_Arg.getX(), MouseEvent_Arg.getY())) {
      openPrecisionPopup(LEFT_POLYGON, new Point(MouseEvent_Arg.getX(), MouseEvent_Arg.getY()));
      return;
    } else if (Ctrl.isPrecise() && MouseEvent_Arg.getButton()==MouseEvent.BUTTON1 && MouseEvent_Arg.getClickCount() > 1 && TheRight_Polygon.contains(MouseEvent_Arg.getX(), MouseEvent_Arg.getY())) {
      openPrecisionPopup(RIGHT_POLYGON, new Point(MouseEvent_Arg.getX(), MouseEvent_Arg.getY()));
      return;
    }  // Clic the first number to drag it and change the SegmentCount
    else if (RectFirstLabel != null && MouseEvent_Arg.getClickCount() == 1 &&
    MouseEvent_Arg.getButton()==MouseEvent.BUTTON1 &&
    RectFirstLabel.contains(MouseEvent_Arg.getX(), MouseEvent_Arg.getY()) &&
    MouseEvent_Arg.isShiftDown()) {
      Dragging = SEGMENT_SIZE_INT;
    } else if (RectFirstLabel != null && MouseEvent_Arg.getClickCount() == 1 &&
    MouseEvent_Arg.getButton()==MouseEvent.BUTTON1 &&
    RectFirstLabel.contains(MouseEvent_Arg.getX(), MouseEvent_Arg.getY()) &&
    !MouseEvent_Arg.isShiftDown()) {
      Dragging = SEGMENT_SIZE;
    }  // The user want to modify the minimum by draging the left triangle
    else if (TheLeft_Polygon.contains(MouseEvent_Arg.getX(), MouseEvent_Arg.getY()) &&
    MouseEvent_Arg.getButton()==MouseEvent.BUTTON1 &&
    !MouseEvent_Arg.isAltDown() && !MouseEvent_Arg.isShiftDown()) {
      
      Dragging = LEFT_RULER;
      
      DeplBef = MouseEvent_Arg.getX() - LeftValue;
      DeplAft = RightValue - MouseEvent_Arg.getX();
      
      JComponent1.repaint();
    } else if (TheLeft_Polygon.contains(MouseEvent_Arg.getX(), MouseEvent_Arg.getY()) &&
    MouseEvent_Arg.getButton()==MouseEvent.BUTTON1 &&
    !MouseEvent_Arg.isAltDown() && MouseEvent_Arg.isShiftDown()) {
      
      Dragging = SHIFT_LEFT_RULER;
      
      DeplBef = MouseEvent_Arg.getX() - LeftValue;
      DeplAft = RightValue - MouseEvent_Arg.getX();
      
      JComponent1.repaint();
    }  // the user want to modify the maximum  by draging the left triangle
    else if (TheRight_Polygon.contains(MouseEvent_Arg.getX(), MouseEvent_Arg.getY()) &&
    MouseEvent_Arg.getButton()==MouseEvent.BUTTON1 &&
    !MouseEvent_Arg.isAltDown() && !MouseEvent_Arg.isShiftDown()) {
      Dragging = RIGHT_RULER;
      
      DeplBef = MouseEvent_Arg.getX() - LeftValue;
      DeplAft = RightValue - MouseEvent_Arg.getX();
      
      JComponent1.repaint();
    }  // the user want to modify the maximum  by draging the left triangle
    else if (TheRight_Polygon.contains(MouseEvent_Arg.getX(), MouseEvent_Arg.getY()) &&
    MouseEvent_Arg.getButton()==MouseEvent.BUTTON1 &&
    !MouseEvent_Arg.isAltDown() && MouseEvent_Arg.isShiftDown()) {
      Dragging = SHIFT_RIGHT_RULER;
      
      DeplBef = MouseEvent_Arg.getX() - LeftValue;
      DeplAft = RightValue - MouseEvent_Arg.getX();
      
      JComponent1.repaint();
    }  // The user may want to drag the segment.
    else if (MouseEvent_Arg.getX() > LeftValue &&
    MouseEvent_Arg.getButton()==MouseEvent.BUTTON1 &&
    MouseEvent_Arg.getX() < RightValue &&
    MouseEvent_Arg.getY() > Margin_Ruler_Top &&
    MouseEvent_Arg.getY() < Margin_Ruler_Top + RulerHeight &&
    !MouseEvent_Arg.isAltDown() && !MouseEvent_Arg.isShiftDown()
    ) {
      Dragging = SEGMENT;
      
      DeplBef = MouseEvent_Arg.getX() - LeftValue;
      DeplAft = RightValue - MouseEvent_Arg.getX();
      
      JComponent1.repaint();
    }  // The user may want to drag the segment (but stay aligned on graduations)
    else if (MouseEvent_Arg.getX() > LeftValue &&
    MouseEvent_Arg.getButton()==MouseEvent.BUTTON1 &&
    MouseEvent_Arg.getX() < RightValue &&
    MouseEvent_Arg.getY() > Margin_Ruler_Top &&
    MouseEvent_Arg.getY() < Margin_Ruler_Top + RulerHeight &&
    !MouseEvent_Arg.isAltDown() && MouseEvent_Arg.isShiftDown()
    ) {
      Dragging = SHIFT_SEGMENT;
      
      DeplBef = MouseEvent_Arg.getX() - LeftValue;
      DeplAft = RightValue - MouseEvent_Arg.getX();
      
      JComponent1.repaint();
    }  // The user may want to drag the segment.
    else if (TheRight_Polygon.contains(MouseEvent_Arg.getX(), MouseEvent_Arg.getY()) &&
    MouseEvent_Arg.getButton()==MouseEvent.BUTTON1 &&
    MouseEvent_Arg.isAltDown()) {
      Dragging = ALT_RIGHT_RULER;
      
      Center = (Ctrl.getMinimumColoredValue() + Ctrl.getMaximumColoredValue()) / 2;
      DeplBef = MouseEvent_Arg.getX() - LeftValue;
      DeplAft = RightValue - MouseEvent_Arg.getX();
      
      JComponent1.repaint();
    }  // The user may want to drag the segment.
    else if (TheLeft_Polygon.contains(MouseEvent_Arg.getX(), MouseEvent_Arg.getY()) &&
    MouseEvent_Arg.getButton()==MouseEvent.BUTTON1 &&
    MouseEvent_Arg.isAltDown()) {
      Dragging = ALT_LEFT_RULER;
      
      Center = (Ctrl.getMinimumColoredValue() + Ctrl.getMaximumColoredValue()) / 2;
      DeplBef = MouseEvent_Arg.getX() - LeftValue;
      DeplAft = RightValue - MouseEvent_Arg.getX();
      
      JComponent1.repaint();
    }  // Double click in a new segment with SHIFT pressed for concatenation of the segment
    else if (MouseEvent_Arg.isShiftDown() &&
    MouseEvent_Arg.getButton()==MouseEvent.BUTTON1 &&
    MouseEvent_Arg.getClickCount() > 1 &&
    MouseEvent_Arg.getX() > MARGIN_RULER_LEFT &&
    MouseEvent_Arg.getX() < MARGIN_RULER_LEFT + RulerWidth &&
    MouseEvent_Arg.getY() > Margin_Ruler_Top &&
    MouseEvent_Arg.getY() < Margin_Ruler_Top + RulerHeight) {
      
      int     d1               = MouseEvent_Arg.getX() - MARGIN_RULER_LEFT;
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
      DeplBef  = MouseEvent_Arg.getX() - LeftValue;
      DeplAft  = RightValue - MouseEvent_Arg.getX();
      
      JComponent1.repaint();
    }  // Double click in a new segment
    else if (MouseEvent_Arg.getClickCount() > 1 &&
      MouseEvent_Arg.getButton()==MouseEvent.BUTTON1 &&
      MouseEvent_Arg.getX() > MARGIN_RULER_LEFT &&
      MouseEvent_Arg.getX() < MARGIN_RULER_LEFT + RulerWidth &&
      MouseEvent_Arg.getY() > Margin_Ruler_Top &&
      MouseEvent_Arg.getY() < Margin_Ruler_Top + RulerHeight) {
      
      int     d1               = MouseEvent_Arg.getX() - MARGIN_RULER_LEFT;
      int     GraduationCount  = (int)Math.floor(d1 / GraduationWidth);
      
      double  Min              = Ctrl.getMinimumValue() + (float)((GraduationCount * GraduationWidth) * (Ctrl.getMaximumValue() - Ctrl.getMinimumValue()) / RulerWidth);
      double  Max              = Ctrl.getMinimumValue() + (float)(((GraduationCount + 1) * GraduationWidth) * (Ctrl.getMaximumValue() - Ctrl.getMinimumValue()) / RulerWidth);
      
      // because the last segment is maybe smaller than a regular one
      if (Max > Ctrl.getMaximumValue()) {
        Max = Ctrl.getMaximumValue();
      }
      
      /*
       * System.err.println("GW as a double :");
       * double GWd = 45.2D;
       * System.err.println(" GWd : "+GWd);
       * System.err.println(" 3*GWd : "+(3*GWd));
       * System.err.println(" 4*GWd : "+(4*GWd));
       * System.err.println(" 3*GWd == 135.6D : "+(3*GWd==135.6D));
       * System.err.println(" 3*GWd == 135.60000000000002D : "+(3*GWd==135.60000000000002D));
       * System.err.println(" 3*GWd == 135.6f : "+(3*GWd==135.6f));
       * System.err.println(" 3*GWd == 135.60000000000002f : "+(3*GWd==135.60000000000002f));
       * System.err.println("as a float :");
       * System.err.println("GW as a float :");
       * float GWf = 45.2f;
       * System.err.println(" GWf ="+GWf);
       * System.err.println(" 3*GWf ="+(3*GWf));
       * System.err.println(" 4*GWf ="+(4*GWf));
       * System.err.println(" 3*GWf == 135.6f :"+(3*GWf==135.6f));
       * System.err.println(" 3*GWf == 135.60000000000002f :"+(3*GWf==135.60000000000002f));
       * System.err.println(" 3*GWf == 135.6D : "+(3*GWf==135.6D));
       * System.err.println(" 3*GWf == 135.60000000000002D : "+(3*GWf==135.60000000000002D));
       * System.err.println(" GWf == GWd: "+(GWf == GWd));
       * System.err.println(" (3*GWf) == (3*GWd) : "+((3*GWf) == (3*GWd)));
       * System.err.println(" 135.6D == 135.6f : "+(135.6D == 135.6f));
       * System.err.println(" 135.6f == 135.6D : "+(135.6f == 135.6D));
       * System.err.println(" 0f == 0D : "+(135.6f == 135.6D));
       * System.err.println("Max ="+Max);
       */
      Ctrl.setColoredValues(Min, Max);
      
      // Like that the user doesn't have to release the mouse to drag the segment
      Dragging = SEGMENT;
      DeplBef = MouseEvent_Arg.getX() - LeftValue;
      DeplAft = RightValue - MouseEvent_Arg.getX();
      JComponent1.repaint();
    } else return;
    MouseEvent_Arg.consume();
  } // mousePressed()
  
  
  /**
   * Method called by the awt-swing mechanism when the user drag his mouse over this component
   *
   * @param MouseEvent_Arg  the mouse event generatedby the awt-swing mechanism
   */
  public void mouseDragged(
  MouseEvent MouseEvent_Arg) {
    
    //System.out.println("last="+LastFiveEvents.elementAt(LastFiveEvents.size()-1));
    //System.out.println("new="+MouseEvent_Arg.getPoint());
    if (((Point)LastFiveEvents.elementAt(0)).x != MouseEvent_Arg.getX() ||
    ((Point)LastFiveEvents.elementAt(0)).y != MouseEvent_Arg.getY()) {
      LastFiveEvents.add(0, (Point)MouseEvent_Arg.getPoint().clone());
    }
    if (LastFiveEvents.size() > 5) {
      LastFiveEvents.removeElementAt(5);
    }
    
    // In case the event is outside the bislider area
    if (MouseEvent_Arg.getX() > JComponent1.getSize().width) {
      MouseEvent_Arg.translatePoint(JComponent1.getSize().width - MouseEvent_Arg.getX(), 0);
    }
    if (MouseEvent_Arg.getX() < 0) {
      MouseEvent_Arg.translatePoint(MouseEvent_Arg.getX(), 0);
    }
    if (MouseEvent_Arg.getY() > JComponent1.getSize().height) {
      MouseEvent_Arg.translatePoint(0, JComponent1.getSize().height - MouseEvent_Arg.getY());
    }
    if (MouseEvent_Arg.getY() < 0) {
      MouseEvent_Arg.translatePoint(0, MouseEvent_Arg.getY());
    }
    
    //if (MouseX  != MouseEvent_Arg.getX())
    //  System.err.println("we changed mouseX from "+MouseX+" to "+MouseEvent_Arg.getX());
    int  MouseX  = MouseEvent_Arg.getX();
    
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
    
    else if (Dragging == SEGMENT_SIZE_INT) {
      double  SegSize  = Math.round((Ctrl.getMaximumValue() - Ctrl.getMinimumValue()) * (MouseX - MARGIN_RULER_LEFT) / RulerWidth);
      if (SegSize > 0 && SegSize < (Ctrl.getMaximumValue() - Ctrl.getMinimumValue())) {
        Ctrl.setSegmentSize(SegSize);
      }
    }
    
    // drag the minimum value with centering
    else if (Dragging == ALT_LEFT_RULER) {
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
      if (Ctrl.isPrecise() && isLastFiveEventsUpDown()) {
        openPrecisionPopup(LEFT_POLYGON, new Point(MouseEvent_Arg.getX(), MouseEvent_Arg.getY()));
        return;
      }
      
      if (MouseX - DeplBef > RightValue) {
        Ctrl.setColoredValues(Ctrl.getMaximumColoredValue(),
        Ctrl.getMaximumColoredValue());
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
      if (Ctrl.isPrecise() && isLastFiveEventsUpDown()) {
        openPrecisionPopup(RIGHT_POLYGON, new Point(MouseEvent_Arg.getX(), MouseEvent_Arg.getY()));
        return;
      }
      
      if (MouseX + DeplAft < LeftValue) {
        Ctrl.setColoredValues(Ctrl.getMinimumColoredValue(), Ctrl.getMinimumColoredValue());
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
        Ctrl.setColoredValues(Ctrl.getMinimumColoredValue(), Ctrl.getMaximumValue());
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
      
      if (Ctrl.getMinimumValue() + DMin + MagnetEffect + Size > Ctrl.getMaximumValue()) {
        DMin = Ctrl.getMaximumValue() - Ctrl.getMinimumValue() - MagnetEffect - Size;
      }
      
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
        Ctrl.setColoredValues(Ctrl.getMinimumValue(), Ctrl.getMinimumValue() + Size);
        JComponent1.repaint();
        return;
      }  // same at right
      else if (MouseX + DeplAft >= MARGIN_RULER_LEFT + RulerWidth) {
        Ctrl.setColoredValues(Ctrl.getMaximumValue() - Size, Ctrl.getMaximumValue());
        JComponent1.repaint();
        return;
      }
      
      double  DMin  = ((MouseX - DeplBef - MARGIN_RULER_LEFT) * (Ctrl.getMaximumValue() - Ctrl.getMinimumValue())) / RulerWidth;
      
      // change the value and call repaint to display the modifications
      Ctrl.setColoredValues(Ctrl.getMinimumValue() + DMin, Ctrl.getMinimumValue() + DMin + Size);
      
      //  JComponent1.paintImmediately(0,0, JComponent1.getWidth(), JComponent1.getHeight());
      JComponent1.repaint();
    }else return;
    
    MouseEvent_Arg.consume();
  } // mouseDragged()
  
  
  /**
   * Method called by the awt-swing mechanism when the user release the mouse button over this component
   *
   * @param MouseEvent_Arg  the mouse event generatedby the awt-swing mechanism
   */
  public void mouseReleased(
  MouseEvent MouseEvent_Arg) {
    if (Dragging != PRECISE_LEFT_RULER && Dragging != PRECISE_RIGHT_RULER) {
      Dragging = NONE;
    }
    
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
    int          OldMouseUnder        = MouseUnder;
    Rectangle2D  OldRectangleSegment  = RectangleSegment;
    
    // for mouseOver between the triangles
    Rectangle    Rect1                = TheRight_Polygon.getBounds();
    Rectangle    Rect2                = TheLeft_Polygon.getBounds();
    Rectangle2D  Rectangle2D1         = Rect1.createUnion(Rect2);
    
    // for segment mouse over
    int          d1                   = MouseEvent_Arg.getX() - MARGIN_RULER_LEFT;
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
    if (MouseEvent_Arg.getX() >= MARGIN_RULER_LEFT && MouseEvent_Arg.getX() <= MARGIN_RULER_LEFT + RulerWidth) {
      RectangleSegment = new Rectangle(LeftSegment + 1, Margin_Ruler_Top + 1, RightSegment - LeftSegment - 2, RulerHeight - 2);
    } else {
      RectangleSegment = null;
    }
    
    if (MouseEvent_Arg.isShiftDown() && RectangleSegment != null) {
      RectangleSegment = Rectangle2D1.createUnion(RectangleSegment);
    }
    
    if (TheRight_Polygon.contains(MouseEvent_Arg.getX(), MouseEvent_Arg.getY())) {
      MouseUnder = RIGHT_POLYGON;
    } else if (TheLeft_Polygon.contains(MouseEvent_Arg.getX(), MouseEvent_Arg.getY())) {
      MouseUnder = LEFT_POLYGON;
    } else if (Rectangle2D1.contains(MouseEvent_Arg.getX(), MouseEvent_Arg.getY())) {
      MouseUnder = SELECTION;
    } else if (RectFirstLabel != null && RectFirstLabel.contains(MouseEvent_Arg.getX(), MouseEvent_Arg.getY())) {
      MouseUnder = FIRST_LABEL;
    } else if (RectLastLabel != null && RectLastLabel.contains(MouseEvent_Arg.getX(), MouseEvent_Arg.getY())) {
      MouseUnder = LAST_LABEL;
    } else if (RectangleSegment != null && RectangleSegment.contains(MouseEvent_Arg.getX(), MouseEvent_Arg.getY())) {
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
    double min = Ctrl.getMinimumColoredValue()-MouseWheelEvent_Arg.getWheelRotation()*Ctrl.getSegmentSize();
    double max = Ctrl.getMaximumColoredValue()-MouseWheelEvent_Arg.getWheelRotation()*Ctrl.getSegmentSize();
    if (min < Ctrl.getMinimumValue()){
      min = Ctrl.getMinimumValue();
      max = min+(Ctrl.getMaximumColoredValue()-Ctrl.getMinimumColoredValue());
    }
    if (max > Ctrl.getMaximumValue()){
      max = Ctrl.getMaximumValue();
      min = max-(Ctrl.getMaximumColoredValue()-Ctrl.getMinimumColoredValue());
    }
    Ctrl.setColoredValues(min ,max); 
  }
  
  
  /**
   * analyse the five last mouse position and tell if it's really following a vertical direction
   *
   * @return   The lastFiveEventsUpDown value
   */
  private boolean isLastFiveEventsUpDown() {
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
      
      return ((Math.abs(D1x) < 2 && Math.abs(D1y) > 5) ||
      (Math.abs(D2x) < 2 && Math.abs(D2y) > 5) ||
      (Math.abs(D3x) < 2 && Math.abs(D3y) > 5) ||
      (Math.abs(D4x) < 2 && Math.abs(D4y) > 5));
    } else {
      return false;
    }
  } // isLastFiveEventsUpDown()
  
  
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
    Hashtable<Integer, JLabel>      Hashtable1       = new Hashtable<Integer, JLabel>();
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
    
    for (int i = -100; i <= 100; i += 20) {
      double  Val  = 0d;
      Val = MiddleVal + ((double)i) / PRECISE_SCALE_FACTOR * Amplitude;
      
      Hashtable1.put(new Integer(i), new JLabel(DecimalFormater.format(Val)));
    }
    JSlider1.setLabelTable(Hashtable1);
    JSlider1.validate();
    
    PopupFactory   PopupFactory1    = PopupFactory.getSharedInstance();
    Point          Point1           = (Point)Point_Arg.clone();
    SwingUtilities.convertPointToScreen(Point1, Ctrl);
    if (Thumb_Arg == RIGHT_POLYGON) {
      JLabel1.setText(Ctrl.getDecimalFormater().format(Ctrl.getMaximumColoredValue()));
    } else if (Thumb_Arg == LEFT_POLYGON) {
      JLabel1.setText(Ctrl.getDecimalFormater().format(Ctrl.getMinimumColoredValue()));
    }
    
    Dimension ScreenSize  = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
    int       CenteredY   = Point1.y - JSlider1.getPreferredSize().height / 2;
    
    if (CenteredY+JPanel1.getPreferredSize().height>ScreenSize.height)
      CenteredY = ScreenSize.height-JPanel1.getPreferredSize().height;
    if (CenteredY<0)
      CenteredY = 0;
    
    final  Popup          Popup1           = PopupFactory1.getPopup(Ctrl, JPanel1, Point1.x - 8, CenteredY);
    Popup1.show();
    
    JSlider1.addMouseListener(
    new MouseAdapter() {
      public void mouseReleased(MouseEvent MouseEvent_Arg) {
        Popup1.hide();
        JSlider1.removeMouseListener(this);
        double  MiddleVal  = PreciseOpenedValue;
        
        double  Amplitude  = Ctrl.getSegmentSize() / Ctrl.getSegmentCount();
        if (Dragging == PRECISE_RIGHT_RULER && MiddleVal - Amplitude < Ctrl.getMinimumColoredValue()) {
          MiddleVal = Ctrl.getMinimumColoredValue() + Amplitude;
        } else if (Dragging == PRECISE_LEFT_RULER && MiddleVal + Amplitude > Ctrl.getMaximumColoredValue()) {
          MiddleVal = Ctrl.getMaximumColoredValue() - Amplitude;
        } else if (MiddleVal - Amplitude < Ctrl.getMinimumValue()) {
          MiddleVal = Ctrl.getMinimumValue() + Amplitude;
        } else if (MiddleVal + Amplitude > Ctrl.getMaximumValue()) {
          MiddleVal = Ctrl.getMaximumValue() - Amplitude;
        }
        
        double  Val        = MiddleVal + ((double)JSlider1.getValue()) / PRECISE_SCALE_FACTOR * Amplitude;
        
        if (Dragging == PRECISE_RIGHT_RULER) {
          Ctrl.setMaximumColoredValue(Val);
        } else if (Dragging == PRECISE_LEFT_RULER) {      	
          Ctrl.setMinimumColoredValue(Val);
        }
        
        Ctrl.repaint();
      }
    });
    
    // This is the only trick I found to make the bislider lost the drag focus
    // and jump directly to the JSlider so the user doesn't have to release and re-click !
    try {
      Robot  Robot1  = new Robot();
      Robot1.mouseRelease(InputEvent.BUTTON1_MASK);
      Robot1.mouseMove(Point1.x , CenteredY + JSlider1.getPreferredSize().height / 2);
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
    RulerHeight = Ctrl.getHeight()-Margin_Ruler_Top-MARGIN_RULER_BOTTOM;
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
   * change the minimum value of the slider
   *
   * @param NewValue_Arg  Description of the Parameter
   */
  protected void changeMinValue(String NewValue_Arg) {
    if (!Ctrl.getUnit().equals("alpha"))
      try {
        double  NV  = Double.parseDouble(NewValue_Arg);

        if (NV < Ctrl.getMaximumValue()) {
          Ctrl.setMinimumValue(NV);
        }

        JComponent1.repaint();
      } catch (Exception Exception_Arg) {
        Exception_Arg.printStackTrace();
      }
    else {
      double  NV  = NewValue_Arg.toUpperCase().charAt(0)-'A';
      if (NV < Ctrl.getMaximumValue()) {
        Ctrl.setMinimumValue(NV);
        Ctrl.setSegmentSize(1);
      }
    }
    
    Ctrl.requestFocus();
  } // changeMinValue()
  
  
  /**
   * change the maximum value of the slider
   *
   * @param NewValue_Arg  Description of the Parameter
   */
  protected void changeMaxValue(String NewValue_Arg) {
    if (!Ctrl.getUnit().equals("alpha"))
      try {
        double  NV  = Double.parseDouble(NewValue_Arg);

        if (NV > Ctrl.getMinimumValue()) {
          Ctrl.setMaximumValue(NV);
        }

        JComponent1.repaint();
      } catch (Exception Exception_Arg) {
        Exception_Arg.printStackTrace();        
      }
    else{
      double  NV  = NewValue_Arg.toUpperCase().charAt(0)-'A';
      if (NV > Ctrl.getMinimumValue()) {
        Ctrl.setMaximumValue(NV);
      }    
    }
    
    Ctrl.requestFocus();
  } // changeMaxValue()
  
  
  /**
   * Description of the Method
   *
   * @param MouseEvent_Arg  Description of the Parameter
   */
  public void mouseClicked(MouseEvent MouseEvent_Arg) {
    final String           Unit              = Ctrl.getUnit();    
    
    // Double click on the first number. The user want to change the minimum value
    if (MouseEvent_Arg.getClickCount() > 1 &&
        RectFirstLabel.contains(MouseEvent_Arg.getX(), MouseEvent_Arg.getY())) {
      Dragging = NONE;
      String  Text  = "" + Ctrl.getMinimumValue();
      if (Unit.equals("alpha")) {
        Text = ""+(char)((int)Ctrl.getMinimumValue()+'A');          
      } else if (Ctrl.getMinimumValue() == (int)Ctrl.getMinimumValue()) {
        Text = "" + Ctrl.getMinimumValue();
      }
      
      JTextFieldMin.setText(Text);
      JTextFieldMin.setLocation(MARGIN_RULER_LEFT, 0);
      JTextFieldMin.setSize(JTextFieldMin.getPreferredSize().width + 10, JTextFieldMin.getPreferredSize().height);
      JTextFieldMin.setVisible(true);
      JTextFieldMin.requestFocus();
      
      JTextFieldMin.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          while (JTextFieldMin.getActionListeners().length > 0) {
            JTextFieldMin.removeActionListener(JTextFieldMin.getActionListeners()[0]);
          }
          /*if (Unit.equals("alpha")){
            if (JTextFieldMin.getText().length()==1 &&
                JTextFieldMin.getText().charAt(0)>='A' &&
                JTextFieldMin.getText().charAt(0)<='Z')
              changeMinValue(""+(JTextFieldMin.getText().charAt(0)-'A'));
            else if (JTextFieldMin.getText().length()==1 &&
                JTextFieldMin.getText().charAt(0)>='a' &&
                JTextFieldMin.getText().charAt(0)='z')
              changeMinValue(""+(JTextFieldMin.getText().charAt(0)-'a'));
               
          } else*/
            changeMinValue(JTextFieldMin.getText());
          
          JTextFieldMin.setVisible(false);
        }
      });
      
      JComponent1.repaint();
    }  // Double click on the last number. The user want to change the maximum value
    else if (MouseEvent_Arg.getClickCount() > 1 &&
    RectLastLabel.contains(MouseEvent_Arg.getX(), MouseEvent_Arg.getY())) {
      
      Dragging = NONE;
      String  Text  = "" + Ctrl.getMaximumValue();
      if (Unit.equals("alpha")) {
        Text = ""+(char)((int)Ctrl.getMaximumValue()+'A');          
      } else if (Ctrl.getMaximumValue() == (int)Ctrl.getMaximumValue()) {
        Text = "" + (int)Ctrl.getMaximumValue();
      }
      
      JTextFieldMax.setText(Text);
      JTextFieldMax.setLocation(JComponent1.getWidth() - JTextFieldMax.getPreferredSize().width - MARGIN_RULER_RIGHT - 10, 0);
      JTextFieldMax.setSize(JTextFieldMax.getPreferredSize().width + 10, JTextFieldMax.getPreferredSize().height);
      JTextFieldMax.setVisible(true);
      JTextFieldMax.requestFocus();
      JTextFieldMax.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          while (JTextFieldMax.getActionListeners().length > 0) {
            JTextFieldMax.removeActionListener(JTextFieldMax.getActionListeners()[0]);
          }
          changeMaxValue(JTextFieldMax.getText());
          JTextFieldMax.setVisible(false);
        }
      });
    } else return;
    MouseEvent_Arg.consume();
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

