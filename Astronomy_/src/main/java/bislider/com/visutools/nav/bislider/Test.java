package bislider.com.visutools.nav.bislider;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.text.NumberFormat;
import java.util.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.basic.BasicBorders;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;
import javax.swing.plaf.metal.OceanTheme;
/**
 * 5 examples of BiSlider in a JFrame to see the widget at work.
 * <br><br>
 * <table border=1 width = "90%">
 *   <tr>
 *     <td>
 *       Copyright 1997-2005 Frederic Vernier. All Rights Reserved.<br>
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
 *   <li>Creation    : 1997<br>
 *   <li>Format      : 15/02/2004<br>
 *   <li>Last Modif  : 19/06/2005 <br>
 *<br>
 * <b>Bugs:</b><br>
 * <li><br>
 *<br>
 * <b>To Do:</b><br>
 *  <li><br>
 *<br>
 * @author Frederic Vernier, Frederic.Vernier@laposte.net
 * @version 1.4.1
 **/

public class Test {
  // all the properties of the application (parameters)
  protected static          Properties                                    BiSliderProperties              = new Properties();
  protected static          float                                         LineX                           = 0f;

  protected static MetalTheme [] metalThemes = {
    new OceanTheme(),
    new DefaultMetalTheme()
  };


  public static void main(String[] Args) {
    try {
      File PropFileName = new File("properties/properties_BiSlider");
      System.out.println("Using property file :"+PropFileName);

      if (PropFileName.exists()){
        FileInputStream FileInputStream1 = new FileInputStream(PropFileName);
        BiSliderProperties.load(FileInputStream1);
        FileInputStream1.close();
      } else {
        BiSliderProperties.setProperty("Application.Name", "BiSlider");
        BiSliderProperties.setProperty("Application.Version", "1.0");
        BiSliderProperties.setProperty("Application.Build", "0001");
        FileOutputStream FileOutputStream1 = new FileOutputStream(PropFileName);
        BiSliderProperties.store(FileOutputStream1, "Values of the parameters of the application");
        FileOutputStream1.close();
      }
    } catch(IOException IOException_Arg){
      //IOException_Arg.printStackTrace();
    }


    final JFrame JFrame1 = new JFrame(" Examples of "+BiSliderProperties.getProperty("Application.Name")+
      " v"+BiSliderProperties.getProperty("Application.Version")+
      " build"+BiSliderProperties.getProperty("Application.Build"));
    JFrame1.setIconImage(new BiSliderBeanInfo().getIcon(BiSliderBeanInfo.ICON_COLOR_32x32));

    JFrame1.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    JFrame1.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent evt) {
        JFrame1.setVisible(false);
        JFrame1.dispose();
        System.exit(0);
      }
    });

    final BiSlider BiSlider1 = new BiSlider(BiSlider.HSB);
    BiSlider1.setVisible(true);
    BiSlider1.setMinimumValue(-193);
    BiSlider1.setMaximumValue(227);
    BiSlider1.setSegmentSize(20);
    BiSlider1.setMinimumColor(Color.ORANGE);
    BiSlider1.setMaximumColor(Color.BLUE);
    BiSlider1.setUnit("$");
    BiSlider1.setPrecise(true);

    final JPopupMenu JPopupMenu1 = BiSlider1.createPopupMenu();
    BiSlider1.addMouseListener(new MouseAdapter(){
      public void mousePressed(MouseEvent MouseEvent_Arg){
        if (MouseEvent_Arg.getButton()==MouseEvent.BUTTON3){
          JPopupMenu1.show(BiSlider1, MouseEvent_Arg.getX(), MouseEvent_Arg.getY());
        }
      }
    });
    
    
    BiSlider1.addMouseListener(new MouseAdapter(){
      public void mousePressed(MouseEvent MouseEvent_Arg){
        if (!MouseEvent_Arg.isConsumed() && MouseEvent_Arg.getButton()==MouseEvent.BUTTON1 && MouseEvent_Arg.getClickCount()==2){
          Color Color1 = JColorChooser.showDialog(BiSlider1, "Choose a color for minimum values", BiSlider1.getMinimumColor());
          if (Color1!=null)
            BiSlider1.setMinimumColor(Color1);
          BiSlider1.repaint();
        }
      }
    });

    final BiSlider BiSlider2 = new BiSlider(BiSlider.RGB);
    BiSlider2.setToolTipText("No line to highlight the ticks here");
    BiSlider2.setMinimumValue(-5);
    BiSlider2.setMaximumValue(5);
    BiSlider2.setSegmentSize(3);
    BiSlider2.setMinimumColor(Color.RED);
    BiSlider2.setMaximumColor(Color.GREEN);
    BiSlider2.setColoredValues(-1, 4);
    BiSlider2.setUnit("%");
    BiSlider2.setBackground(Color.GRAY);
    BiSlider2.setOpaque(true);
    BiSlider2.setSliderBackground(new Color(152, 152, 192));
    BiSlider2.setForeground(Color.WHITE);
    BiSlider2.setSound(true);
    BiSlider2.setArcSize(14);
    BiSlider2.setFont(new Font("SansSerif", Font.ITALIC|Font.BOLD, 12));
    BiSlider2.setPrecise(true);
    BiSlider2.addContentPainterListener(new ContentPainterListener(){
      public void paint(ContentPainterEvent ContentPainterEvent_Arg){
        Graphics2D Graphics2 = (Graphics2D)ContentPainterEvent_Arg.getGraphics();
        Rectangle Rect1 = ContentPainterEvent_Arg.getRectangle();
        Rectangle Rect2 = ContentPainterEvent_Arg.getBoundingRectangle();
        if (ContentPainterEvent_Arg.getColor()!=null) {
          Graphics2.setColor(ContentPainterEvent_Arg.getColor());
          Graphics2.setPaint(new GradientPaint(Rect1.x, Rect1.y, ContentPainterEvent_Arg.getColor(),
            Rect1.x+(int)(LineX*Rect1.width), Rect1.y, ContentPainterEvent_Arg.getColor().brighter()));
          Graphics2.fillRect(Rect1.x, Rect1.y, (int)(LineX*Rect1.width), Rect1.height);

          Graphics2.setPaint(new GradientPaint(Rect1.x+(int)(LineX*Rect1.width), Rect1.y, ContentPainterEvent_Arg.getColor().brighter(),
            Rect1.x+Rect1.width, Rect1.y, ContentPainterEvent_Arg.getColor()));
          Graphics2.fillRect(Rect1.x+(int)(LineX*Rect1.width), Rect1.y, Rect1.width-(int)(LineX*Rect1.width), Rect1.height);
        }
              }
    });

    Thread Thread1 = new Thread(){
      public void run() {
        while (true){
          while (LineX<1){
            LineX += 0.01f;
            try {sleep(20);} catch(InterruptedException ignored){}
            BiSlider2.repaint();
            Thread.yield();
          }
          while (LineX>0) {
            LineX -= 0.01f;
            try {sleep(20);} catch(InterruptedException ignored){}
            BiSlider2.repaint();
            Thread.yield();
          }
        }
      }
    };
    Thread1.setPriority(Thread.MIN_PRIORITY);
    Thread1.start();


    final JPopupMenu JPopupMenu2 = BiSlider2.createPopupMenu();
    BiSlider2.addMouseListener(new MouseAdapter(){
      public void mousePressed(MouseEvent MouseEvent_Arg){
        if (MouseEvent_Arg.getButton()==MouseEvent.BUTTON3){
          JPopupMenu2.show(BiSlider2, MouseEvent_Arg.getX(), MouseEvent_Arg.getY());
        }
      }
    });

    final BiSlider BiSlider3 = new BiSlider(BiSlider.CENTRAL);
    BiSlider3.setToolTipText("Use a gradient of color to highlight the segments");
    BiSlider3.setUniformSegment(true);
    BiSlider3.setVisible(true);
    BiSlider3.setMinimumValue(0);
    BiSlider3.setMaximumValue(25);
    BiSlider3.setSegmentSize(1);
    BiSlider3.setMinimumColor(Color.YELLOW);
    BiSlider3.setMiddleColor(Color.BLACK);
    BiSlider3.setMaximumColor(Color.BLUE);
    BiSlider3.setColoredValues(0, 25);
    BiSlider3.setSound(true);
    BiSlider3.setUnit("alpha");
    BiSlider3.setPrecise(false);
    BiSlider3.setArcSize(18);

    BiSlider3.addContentPainterListener(new ContentPainterListener(){
      public void paint(ContentPainterEvent ContentPainterEvent_Arg){
        Graphics2D Graphics2 = (Graphics2D)ContentPainterEvent_Arg.getGraphics();
        Rectangle Rect1 = ContentPainterEvent_Arg.getRectangle();
        Rectangle Rect2 = ContentPainterEvent_Arg.getBoundingRectangle();
        if (ContentPainterEvent_Arg.getColor()!=null) {
          Graphics2.setColor(ContentPainterEvent_Arg.getColor());
          Graphics2.setPaint(new GradientPaint(Rect2.x, Rect2.y, ContentPainterEvent_Arg.getColor().brighter(),
            Rect2.x+Rect2.width, Rect2.y+Rect2.height, ContentPainterEvent_Arg.getColor().darker()));
          Graphics2.fillRect(Rect1.x, Rect1.y, Rect1.width, Rect1.height);
        }
      }
    });

    final JPopupMenu JPopupMenu3 = BiSlider3.createPopupMenu();
    BiSlider3.addMouseListener(new MouseAdapter(){
      public void mousePressed(MouseEvent MouseEvent_Arg){
        if (MouseEvent_Arg.getButton()==MouseEvent.BUTTON3){
          JPopupMenu3.show(BiSlider3, MouseEvent_Arg.getX(), MouseEvent_Arg.getY());
        }
      }
    });

    final BiSlider BiSlider4 = new BiSlider(BiSlider.RGB, false);
    BiSlider4.setToolTipText("Like a thermometer");
    BiSlider4.setUniformSegment(true);
    BiSlider4.setVisible(true);
    BiSlider4.setMinimumValue(0);
    BiSlider4.setMaximumValue(100);
    BiSlider4.setSegmentSize(5);
    BiSlider4.setMinimumColor(Color.BLUE);
    BiSlider4.setMaximumColor(Color.RED);
    BiSlider4.setColoredValues(0, 100);
    BiSlider4.setUnit(""+(char)(0xB0)); // 167
    BiSlider4.setPrecise(true);
    BiSlider4.setArcSize(18);

    final JPopupMenu JPopupMenu4 = BiSlider4.createPopupMenu();
    BiSlider4.addMouseListener(new MouseAdapter(){
      public void mousePressed(MouseEvent MouseEvent_Arg){
        if (MouseEvent_Arg.getButton()==MouseEvent.BUTTON3){
          JPopupMenu4.show(BiSlider4, MouseEvent_Arg.getX(), MouseEvent_Arg.getY());
        }
      }
    });

    final BiSlider BiSlider5 = new BiSlider(BiSlider.RGB, true);
    BiSlider5.setToolTipText("Like a sea-depth meter");
    BiSlider5.setUniformSegment(true);
    BiSlider5.setVisible(true);
    BiSlider5.setMinimumValue(0);
    BiSlider5.setMaximumValue(100);
    BiSlider5.setSegmentSize(10);
    BiSlider5.setMinimumColor(Color.BLUE);
    BiSlider5.setMaximumColor(Color.BLACK);
    BiSlider5.setColoredValues(20, 70);
    BiSlider5.setBackground(Color.BLACK);
    BiSlider5.setSliderBackground(new Color(96, 96, 156));
    BiSlider5.setForeground(Color.YELLOW);
    BiSlider5.setFont(new Font("Serif", Font.ITALIC|Font.BOLD, 12));
    BiSlider5.setUnit("m");
    BiSlider5.setPrecise(true);
    BiSlider5.setArcSize(10);

    final JPopupMenu JPopupMenu5 = BiSlider5.createPopupMenu();
    BiSlider5.addMouseListener(new MouseAdapter(){
      public void mousePressed(MouseEvent MouseEvent_Arg){
        if (MouseEvent_Arg.getButton()==MouseEvent.BUTTON3){
          JPopupMenu5.show(BiSlider5, MouseEvent_Arg.getX(), MouseEvent_Arg.getY());
        }
      }
    });


    final BiSlider BiSlider6 = new BiSlider(BiSlider.CENTRAL);
    BiSlider6.setToolTipText("When the background is a repartition histogramm");
    BiSlider6.setUniformSegment(true);
    BiSlider6.setVisible(true);
    BiSlider6.setMinimumValue(0);
    BiSlider6.setMaximumValue(100);
    BiSlider6.setSegmentSize(4);
    BiSlider6.setMinimumColor(Color.RED);
    BiSlider6.setMiddleColor(Color.BLACK);
    BiSlider6.setMaximumColor(Color.RED);
    BiSlider6.setColoredValues(0, 100);
    BiSlider6.setUnit("");
    BiSlider6.setPrecise(true);
    BiSlider6.setArcSize(18);
    BiSlider6.addContentPainterListener(new ContentPainterListener() {
      public void paint(ContentPainterEvent ContentPainterEvent_Arg){
        Graphics2D Graphics2 = (Graphics2D)ContentPainterEvent_Arg.getGraphics();
        Rectangle Rect1 = ContentPainterEvent_Arg.getRectangle();
        Rectangle Rect2 = ContentPainterEvent_Arg.getBoundingRectangle();
        //double BarHeight = Math.abs(Math.cos(Math.PI*(Rect2.x+Rect2.width/2) / BiSlider6.getWidth()));
        //BarHeight = (double)(Rect2.x+Rect2.width/2) / BiSlider6.getWidth();
        //BarHeight = Math.random();
        float X = ((float)Rect2.x-BiSlider6.getWidth()/2f)/BiSlider6.getWidth()*6;
        double BarHeight = 1-Math.exp((-1*X*X)/2);
        X = ((float)(Rect2.x-Rect2.width)-BiSlider6.getWidth()/2f)/BiSlider6.getWidth()*6;
        double BarHeight2 = 1-Math.exp((-1*X*X)/2);

        if (ContentPainterEvent_Arg.getColor()!=null) {
          Graphics2.setColor(BiSlider6.getSliderBackground());
          //Graphics2.fillRect(Rect2.x, Rect2.y, Rect2.width, (int)((BarHeight*Rect2.height)));
          Graphics2.setColor(ContentPainterEvent_Arg.getColor());
          Graphics2.fillRect(Rect2.x, Rect2.y+(int)((BarHeight*Rect2.height)), Rect2.width+1, 1+(int)(((1-BarHeight)*Rect2.height)));
          //Graphics2.drawRect(Rect2.x, Rect2.y+(int)((BarHeight*Rect2.height)), Rect2.width+1, 1+(int)(((1-BarHeight)*Rect2.height)));
        } else {
          Graphics2.setColor(new Color(255, 255, 218, 64));
          Graphics2.fillRect(Rect2.x, Rect2.y+(int)((BarHeight*Rect2.height)), Rect2.width+1, 1+(int)(((1-BarHeight)*Rect2.height)));
        }
        Graphics2.setColor(Color.BLACK);
        //Graphics2.drawRect(Rect2.x, Rect2.y+(int)((BarHeight*Rect2.height)), Rect2.width-1, (int)(((1-BarHeight)*Rect2.height)));
        Graphics2.drawLine(Rect2.x, Rect2.y+(int)((BarHeight*Rect2.height)), Rect2.x+Rect2.width-1, Rect2.y+(int)((BarHeight*Rect2.height)));
        Graphics2.drawLine(Rect2.x, Rect2.y+(int)((Math.min(BarHeight, BarHeight2)*Rect2.height)), Rect2.x, Rect2.y+Rect2.height);
      }
    });

    final JPopupMenu JPopupMenu6 = BiSlider6.createPopupMenu();
    BiSlider6.addMouseListener(new MouseAdapter(){
      public void mousePressed(MouseEvent MouseEvent_Arg){
        if (MouseEvent_Arg.getButton()==MouseEvent.BUTTON3){
          JPopupMenu6.show(BiSlider6, MouseEvent_Arg.getX(), MouseEvent_Arg.getY());
        }
      }
    });

    JFrame1.getContentPane().setLayout(new BorderLayout(2, 2));
    JPanel JPanel1= new JPanel();
    JPanel JPanel2= new JPanel();
    JPanel1.setLayout(new BoxLayout(JPanel1, BoxLayout.Y_AXIS));
    JPanel2.setLayout(new BorderLayout(2, 2));

    final String initialText = "\n\n\n Use this BiSlider to see the events generated\n";
    JPanel2.add(BiSlider6, BorderLayout.CENTER);
    final JTextArea JTextArea5 = new  JTextArea(initialText);
    JPanel2.add(JTextArea5, BorderLayout.SOUTH);
    BiSlider6.addBiSliderListener(new BiSliderAdapter(){
      /** something changed that modified the color gradient between min and max */
      public void newColors(BiSliderEvent BiSliderEvent_Arg) {
        String newText = JTextArea5.getText();
        newText = newText.substring(newText.indexOf("\n")+1)+"newColors \n";
        JTextArea5.setText(newText);      
      }
      /**  min or max values changed  */  
      public void newValues(BiSliderEvent BiSliderEvent_Arg) {
        String newText = JTextArea5.getText();
        newText = newText.substring(newText.indexOf("\n")+1)+"newValues "+BiSliderEvent_Arg.getMinimum()+" <->"+BiSliderEvent_Arg.getMaximum()+"\n";
        JTextArea5.setText(newText);      
      }
      /**  min selected value changed  */
      public void newMinValue(BiSliderEvent BiSliderEvent_Arg) {
        String newText = JTextArea5.getText();
        newText = newText.substring(newText.indexOf("\n")+1)+"newMinValue "+BiSliderEvent_Arg.getMinimum()+"\n";
        JTextArea5.setText(newText);         
      }
      /**  max selected value changed  */
      public void newMaxValue(BiSliderEvent BiSliderEvent_Arg) {
        String newText = JTextArea5.getText();
        newText = newText.substring(newText.indexOf("\n")+1)+"newMaxValue "+BiSliderEvent_Arg.getMaximum()+"\n";
        JTextArea5.setText(newText);         
      }
      /**  selected segments changed  */
      public void newSegments(BiSliderEvent BiSliderEvent_Arg) {
        String newText = JTextArea5.getText();
        newText = newText.substring(newText.indexOf("\n")+1)+"newSegments \n";
        JTextArea5.setText(newText);      
       }
    });
    
    JSplitPane JSplitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, JPanel1, JPanel2);
    JSplitPane JSplitPane1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, JSplitPane2, BiSlider5);

    JFrame1.getContentPane().add(BorderLayout.CENTER, JSplitPane1);
    JFrame1.getContentPane().add(BorderLayout.WEST, BiSlider4);

    JTextArea JTextArea1 = new  JTextArea(
      "* Six Examples of BiSlider Bean by Frederic Vernier. October 2005. Version 1.4.1\n"+
      "* This bean can be imported in a graphical java Interface Builder.\n"+
      "* Read the ReadMe.txt File for legal notice and javadoc for API.\n"+
      "* Right click to bring a popup menu with user's options.\n"+
      "* Double click the triangles to bring a popup slider to set values with precision since version 1.3.5."
      );
    JTextArea1.setEditable(false);
    JPanel1.add(JTextArea1);
    JPanel1.add(BiSlider1);

    JTextArea JTextArea2 = new  JTextArea(
      "+ It now works for integer or double values since version 1.3.3\n"+
      "+ We force the gaps to be uniform in the 3rd, 4th and 5th exemples (= int division).\n"+
      "+ There are 3 different kinds of color interpolation: HSB, CENTRAL_BLACK and RGB\n"+
      "+ If SegmentCount equals the range this bean behaves like a range slider.\n"+
      "+ Since v1.3.5 corners can be rounded and foreground color can be changed."
      );
    JTextArea2.setEditable(false);
    JPanel1.add(JTextArea2);
    JPanel1.add(BiSlider2);

    JTextArea JTextArea3 = new  JTextArea(
      "- Double click a non-colored gap to select it as a the segment.\n"+
      "- Click and drag the maximum or the minimum triangle to change them.\n"+
      "- Shift+double click to extend the selection with a new gap.\n"+
      "- Shift click triangle or segment will align it on graduation.\n"+
      "- While dragging a triangle, turn 90� to open the precision popup without releasing the mouse button."
      );
    JTextArea3.setEditable(false);
    JPanel1.add(JTextArea3);
    JPanel1.add(BiSlider3);

    JTextArea JTextArea4 = new  JTextArea(
      "- Alt+click+drag a triangle to select a range around the central value.\n"+
      "- Drag&Drop the minimum value (text in bold) of the legend to set the SegmentCount.\n"+
      "- Shift Drag&Drop the minimum value (text) to stay on the int values.\n"+
      "- Double click the minimum or maximum value (text) to change the scope of the bean.\n"+
      "- Font painting is now anti-aliased and font can be changed since v1.3.5"
      );
    JTextArea4.setEditable(false);
    //JPanel1.add(JTextArea4);
    JSlider JSlider1 = new JSlider();
    JSlider1.setPaintLabels(true);
    JSlider1.setPaintTicks(true);
    JSlider1.setPaintTrack(true);
    JSlider1.setSnapToTicks(true);
    JSlider1.setMajorTickSpacing(10);
    JSlider1.setMinorTickSpacing(1);
    JPanel1.add(JSlider1);

    JTextArea4.setEditable(false);
    JPanel1.add(JTextArea4);

    //System.out.println("BiSlider1 = "+BiSlider1);
    JMenuBar JMenuBar1         = new JMenuBar();
    JMenu JMenuPLaF            = new JMenu("Look&Feel");
    ButtonGroup LnFButtonGroup = new ButtonGroup();

    JMenuPLaF.getAccessibleContext().setAccessibleDescription("This menu supports looks and feel selection");
    JMenuPLaF.setMnemonic(KeyEvent.VK_L);

    //boolean FirstMetal = true;
    class PLaFAction extends AbstractAction {
      private String LnFClassName;
      private String Title;

      public PLaFAction (String LnFClassName_Arg, String Title_Arg){
        super(Title_Arg);
        LnFClassName = LnFClassName_Arg;
        Title        = Title_Arg;
      }
      public void actionPerformed(ActionEvent ActionEvent_Arg) {
        try {
          if (Title.startsWith("Metal") && Title.endsWith("Steel")) {
            MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme());
          } else if (Title.startsWith("Metal") && Title.endsWith("Ocean")) {
            Class Class1   = Class.forName("javax.swing.plaf.metal.OceanTheme");
            Object Object1 = Class1.newInstance();
            MetalLookAndFeel.setCurrentTheme((MetalTheme)Object1);
          } else if (Title.startsWith("Metal") && Title.endsWith("Aqua")) {
            MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme(){
              public String getName() { return "Aqua"; }
              private final ColorUIResource primary1 = new ColorUIResource(102, 153, 153);
              private final ColorUIResource primary2 = new ColorUIResource(128, 192, 192);
              private final ColorUIResource primary3 = new ColorUIResource(159, 235, 235);
              protected ColorUIResource getPrimary1() { return primary1; }
              protected ColorUIResource getPrimary2() { return primary2; }
              protected ColorUIResource getPrimary3() { return primary3; }

          });
          } else if (Title.startsWith("Metal") && Title.endsWith("Charcoal")) {
            MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme(){
            public String getName() { return "Charcoal"; }
            private final ColorUIResource primary1 = new ColorUIResource(66, 33, 66);
            private final ColorUIResource primary2 = new ColorUIResource(90, 86, 99);
            private final ColorUIResource primary3 = new ColorUIResource(99, 99, 99);

            private final ColorUIResource secondary1 = new ColorUIResource(0, 0, 0);
            private final ColorUIResource secondary2 = new ColorUIResource(51, 51, 51);
            private final ColorUIResource secondary3 = new ColorUIResource(102, 102, 102);

            private final ColorUIResource black = new ColorUIResource(222, 222, 222);
            private final ColorUIResource white = new ColorUIResource(0, 0, 0);

            protected ColorUIResource getPrimary1() { return primary1; }
            protected ColorUIResource getPrimary2() { return primary2; }
            protected ColorUIResource getPrimary3() { return primary3; }

            protected ColorUIResource getSecondary1() { return secondary1; }
            protected ColorUIResource getSecondary2() { return secondary2; }
            protected ColorUIResource getSecondary3() { return secondary3; }

            protected ColorUIResource getBlack() { return black; }
            protected ColorUIResource getWhite() { return white; }
          });
          } else if (Title.startsWith("Metal") && Title.endsWith("Contrast")) {
            MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme() {
              public String getName() { return "Contrast"; }

              private final ColorUIResource primary1 = new ColorUIResource(0, 0, 0);
              private final ColorUIResource primary2 = new ColorUIResource(204, 204, 204);
              private final ColorUIResource primary3 = new ColorUIResource(255, 255, 255);
              private final ColorUIResource primaryHighlight = new ColorUIResource(102,102,102);

              private final ColorUIResource secondary2 = new ColorUIResource(204, 204, 204);
              private final ColorUIResource secondary3 = new ColorUIResource(255, 255, 255);
              private final ColorUIResource controlHighlight = new ColorUIResource(102,102,102);

              protected ColorUIResource getPrimary1() { return primary1; }
              protected ColorUIResource getPrimary2() { return primary2; }
              protected ColorUIResource getPrimary3() { return primary3; }
              public ColorUIResource getPrimaryControlHighlight() { return primaryHighlight;}

              protected ColorUIResource getSecondary2() { return secondary2; }
              protected ColorUIResource getSecondary3() { return secondary3; }
              public ColorUIResource getControlHighlight() { return super.getSecondary3(); }

              public ColorUIResource getFocusColor() { return getBlack(); }

              public ColorUIResource getTextHighlightColor() { return getBlack(); }
              public ColorUIResource getHighlightedTextColor() { return getWhite(); }

              public ColorUIResource getMenuSelectedBackground() { return getBlack(); }
              public ColorUIResource getMenuSelectedForeground() { return getWhite(); }
              public ColorUIResource getAcceleratorForeground() { return getBlack(); }
              public ColorUIResource getAcceleratorSelectedForeground() { return getWhite(); }


              public void addCustomEntriesToTable(UIDefaults table) {
                  Border blackLineBorder = new BorderUIResource(new LineBorder( getBlack() ));
                  Object textBorder = new BorderUIResource( new CompoundBorder(
                                                                 blackLineBorder,
                                                                 new BasicBorders.MarginBorder()));
                  table.put( "ToolTip.border", blackLineBorder);
                  table.put( "TitledBorder.border", blackLineBorder);
                  table.put( "TextField.border", textBorder);
                  table.put( "PasswordField.border", textBorder);
                  table.put( "TextArea.border", textBorder);
                  table.put( "TextPane.border", textBorder);
                  table.put( "EditorPane.border", textBorder);
              }
            });
          } else if (Title.startsWith("Metal") && Title.endsWith("Ruby")) {
            MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme() {
              private final ColorUIResource primary1 = new ColorUIResource(80, 10, 22);
              private final ColorUIResource primary2 = new ColorUIResource(193, 10, 44);
              private final ColorUIResource primary3 = new ColorUIResource(244, 10, 66);

              protected ColorUIResource getPrimary1() { return primary1; }
              protected ColorUIResource getPrimary2() { return primary2; }
              protected ColorUIResource getPrimary3() { return primary3; }
            });

          } else if (Title.startsWith("Metal") && Title.endsWith("Emerald")) {
            MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme() {
              public String getName() { return "Emerald"; }

              private final ColorUIResource primary1 = new ColorUIResource(51, 142, 71);
              private final ColorUIResource primary2 = new ColorUIResource(102, 193, 122);
              private final ColorUIResource primary3 = new ColorUIResource(153, 244, 173);

              protected ColorUIResource getPrimary1() { return primary1; }
              protected ColorUIResource getPrimary2() { return primary2; }
              protected ColorUIResource getPrimary3() { return primary3; }
            });
          }

          UIManager.setLookAndFeel(LnFClassName);
          SwingUtilities.updateComponentTreeUI(JFrame1);
          JFrame1.pack();
          Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
          JFrame1.setLocation((screenSize.width-JFrame1.getWidth())/2,(screenSize.height-JFrame1.getHeight())/2);

        } catch (Exception Exception_Arg) {
          System.out.println("Failed loading L&F: " + LnFClassName);
          Exception_Arg.printStackTrace();
        }
      }
    };

    boolean FirstMetal = true;
    UIManager.LookAndFeelInfo[]  lafis  = UIManager.getInstalledLookAndFeels();
    //for (int i = 0; i < lafis.length; i++)
      //System.out.println("Plaf: " + lafis[i].getClassName());
    if (lafis != null)
      for (int i = 0; i < lafis.length; i++)
        try {
          final LookAndFeel  currLAF  = (LookAndFeel)Class.forName(lafis[i].getClassName()).newInstance();
          // System.out.println("Plaf: " + lafis[i].getClassName());
          if (currLAF.isSupportedLookAndFeel()) {
            JRadioButtonMenuItem  jMenuItemLnF  = new JRadioButtonMenuItem(lafis[i].getName());
            final  String     LnFClassName  = lafis[i].getClassName();
            LnFButtonGroup.add(jMenuItemLnF);

            if (currLAF instanceof MetalLookAndFeel && FirstMetal && System.getProperty("java.version").startsWith("1.5")) {
              jMenuItemLnF.setSelected(true);
              jMenuItemLnF.setText("Metal Ocean");
              jMenuItemLnF.setAction(new PLaFAction(LnFClassName, jMenuItemLnF.getText()));
               FirstMetal = false;
              i--;
              JMenuPLaF.add(jMenuItemLnF);
            }
            else if (currLAF instanceof MetalLookAndFeel && !FirstMetal) {
              jMenuItemLnF.setText("Metal Steel");
              jMenuItemLnF.setAction(new PLaFAction(LnFClassName, jMenuItemLnF.getText()));
              JMenuPLaF.add(jMenuItemLnF);

              jMenuItemLnF  = new JRadioButtonMenuItem("Metal Aqua");
              LnFButtonGroup.add(jMenuItemLnF);
              jMenuItemLnF.setAction(new PLaFAction(LnFClassName, jMenuItemLnF.getText()));
              JMenuPLaF.add(jMenuItemLnF);

              jMenuItemLnF  = new JRadioButtonMenuItem("Metal Charcoal");
              LnFButtonGroup.add(jMenuItemLnF);
              jMenuItemLnF.setAction(new PLaFAction(LnFClassName, jMenuItemLnF.getText()));
              JMenuPLaF.add(jMenuItemLnF);

              jMenuItemLnF  = new JRadioButtonMenuItem("Metal High Contrast");
              LnFButtonGroup.add(jMenuItemLnF);
              jMenuItemLnF.setAction(new PLaFAction(LnFClassName, jMenuItemLnF.getText()));
              JMenuPLaF.add(jMenuItemLnF);

              jMenuItemLnF  = new JRadioButtonMenuItem("Metal Ruby");
              LnFButtonGroup.add(jMenuItemLnF);
              jMenuItemLnF.setAction(new PLaFAction(LnFClassName, jMenuItemLnF.getText()));
              JMenuPLaF.add(jMenuItemLnF);

              jMenuItemLnF  = new JRadioButtonMenuItem("Metal Emerald");
              LnFButtonGroup.add(jMenuItemLnF);
              jMenuItemLnF.setAction(new PLaFAction(LnFClassName, jMenuItemLnF.getText()));
              JMenuPLaF.add(jMenuItemLnF);
            } else{
              jMenuItemLnF.setAction(new PLaFAction(LnFClassName, jMenuItemLnF.getText()));
              JMenuPLaF.add(jMenuItemLnF);
            }

          }
        } catch (Exception Exception_Arg) {
          System.out.println("Failed loading L&F list ");
          Exception_Arg.printStackTrace();
        }

    JMenuBar1.add(JMenuPLaF);
    JFrame1.setJMenuBar(JMenuBar1);

    //System.out.println("BiSlider1 = "+BiSlider1);
    JFrame1.pack();
    JFrame1.setSize(JFrame1.getWidth(), JFrame1.getHeight()+50);
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    JFrame1.setLocation((screenSize.width-JFrame1.getWidth())/2,(screenSize.height-JFrame1.getHeight())/2);

    JFrame1.setVisible(true);
  }// main()

}