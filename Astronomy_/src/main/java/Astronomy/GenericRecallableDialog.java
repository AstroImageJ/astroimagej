package Astronomy;// package ij.gui;  if integrated into ImageJ proper, would likely belong to the ij.gui group
import java.awt.*;
import java.awt.event.*;
import ij.*;
import ij.gui.*;
import java.util.*;
import ij.plugin.frame.Recorder;

/** Walter O'Dell PhD,  wodell@rochester.edu,   11/6/02
  * Overview of GenericDialogPlus and GenericRecallableDialog classes:
  *      these classes enable the specification of scrollbars, buttons, and
  *      the objects available in the GenericDialog class that remain active 
  *      until the dialog window is actively closed.
  *
  * Attributes:
  *      Scrollbars: enables scrollbars for integer, float and/or double values.
  *            float/double values are facilitated by maintaining a scaling factor 
  *            for each scrollbar, and an Ndigits parameter.
  *            Keyboard arrow keys adjust values of most recently mouse-activated scrollbar.
  *      Buttons: enables buttons that perform actions (besides 'exit')
  *
  *      rowOfItems(): enables placement of multiple objects in a row, 
  *            e.g. the scrollbar title, current value and the scrollbar itsself.
  *      addButtons(): enables easy specification of multiple buttons across a row
  *      getBooleanValue(int index_to_list); getNumericValue(); getButtonValue(); getScrollbarValue()
  *            enables access to the value of any individual object without having to go 
  *            through the entire list of like-objects with getNext...() functions.
  *
  * minor changes to the parent GenericDialog were needed, as described in the header 
  *   for the temporary GenericDialog2 class, namely:
   *   1. Changed 'private' attribute to 'protected' for several variables in parent 
   *          class to facilitate use of these variables by GenericDialogPlus class
   *      2. Added variables (int) x; // GridBagConstraints height variable
   *                           and  (Container) activePanel; // facilitates row of items option
   *            and code altered to initialize and update these new variables.
   * It is hoped that these modifications will be made to the parent GenericDialog class
   * in future versions of ImageJ, negating the need for the GenericDialog2 class
  **/
class GenericDialogPlus extends GenericDialog2 
               implements AdjustmentListener, KeyListener, FocusListener {
   /** Maximum number of each component (numeric field, checkbox, etc). */
   public static final int MAX_ITEMS = 20;
   protected Scrollbar[] scrollbars;
   protected double[] SBscales; 
   protected double[] SBcurValues; 
   private Label[] SBcurValueLabels; 
   protected int sbIndex, SBtotal;
   private int[] SBdigits; // Ndigits to right of decimal pt (0==integer)
   protected int SBlastTouched; // last scrollbar touched; needed for arrow key usage

   // second panel(s) enables placement of multiple items on same line (row)
   private Panel twoPanel;
   private GridBagConstraints tmpc;
   private GridBagLayout tmpgrid;
   private int tmpy;
   
   public GenericDialogPlus(String title) {
      super(title);
   }

   /** Creates a new GenericDialog using the specified title and parent frame. */
   public GenericDialogPlus(String title, Frame parent) {
      super(title, parent);
   }

   /** access the value of the i'th checkbox */
   public boolean getBooleanValue(int i) {
      if (checkbox==null)
         return false;
      // else
      Checkbox cb = (Checkbox)(checkbox.elementAt(i));
      return cb.getState();
   }
  
   /** access the value of the i'th numeric field */
   public double getNumericValue(int i) {
      if (numberField==null)
         return 0;
      // else
      TextField tf = (TextField)numberField.elementAt(i);
      String theText = tf.getText();
      String originalText = (String)defaultText.elementAt(i);
      double defaultValue = (Double) (defaultValues.elementAt(i));
      double value;
      if (theText.equals(originalText))
         value = defaultValue;
      else {
         Double d = getValue(theText);
         if (d!=null)
            value = d;
         else {
            // invalidNumber = true;
            value = 0.0;
         }
      }
      return value;
   }

   public void beginRowOfItems() {
      tmpc = c; tmpgrid = grid;  tmpy = y;
      twoPanel = new Panel();
      activePanel = twoPanel;
      grid = new GridBagLayout();
      twoPanel.setLayout(grid);
      c = new GridBagConstraints();
      x = y = 0;
   }
   public void endRowOfItems() {
      activePanel = this;
      c = tmpc;  grid = tmpgrid;  y = tmpy;
      c.gridwidth = 1;
      c.gridx = 0; c.gridy = y;
      c.anchor = GridBagConstraints.WEST;
      c.insets = new Insets(0, 0, 0, 0);
      grid.setConstraints(twoPanel, c);
      add(twoPanel);
      x = 0;
      y++;
   }

   /** Adds adjustable scrollbar field.
     * param label   the label
     * param defaultValue   initial state
     * param digits   the number of digits to the right of the decimal place
     * param minval   the range minimum (left side value of slider)
     * param maxval   the range maximum (right side value of slider)
    */    
   public void addScrollBar(String label, double defaultValue, int digits,
                  double minval, double maxval) {
      // use default 100 clicks
      addScrollBar(label, defaultValue, digits, minval, maxval, 100);
   }
   public void addScrollBar(String label, double defaultValue, int digits,
                  double minval, double maxval, int maxClicks) {
      if (sbIndex >= MAX_ITEMS) {
         IJ.write("  cannot add another slider, have maxed out at: "+sbIndex);
         return;
      }
      if (scrollbars==null) {
         scrollbars = new Scrollbar[MAX_ITEMS]; 
         SBscales = new double[MAX_ITEMS]; 
         SBcurValues = new double[MAX_ITEMS]; 
         SBcurValueLabels = new Label[MAX_ITEMS]; 
         SBdigits = new int[MAX_ITEMS];
      }          
      // create new panel that is 3 cells wide for SBlabel, SB, SBcurVal
      Panel sbPanel = new Panel();
      GridBagLayout sbGrid = new GridBagLayout();
      GridBagConstraints sbc  = new GridBagConstraints();
      sbPanel.setLayout(sbGrid);

      // label
      Label theLabel = new Label(label);
      sbc.insets = new Insets(5, 0, 0, 0);
      sbc.gridx = 0; sbc.gridy = 0;
      sbc.anchor = GridBagConstraints.WEST;
      sbGrid.setConstraints(theLabel, sbc);
      sbPanel.add(theLabel);
      
      // scrollbar: only works with integer values so use scaling to mimic float/double
      SBscales[sbIndex] = Math.pow(10.0, digits);
      SBcurValues[sbIndex] = defaultValue;
      int visible = (int)Math.round((maxval-minval)* SBscales[sbIndex]/10.0);
      scrollbars[sbIndex] = new Scrollbar(Scrollbar.HORIZONTAL, 
               (int)Math.round(defaultValue*SBscales[sbIndex]), 
               visible, /* 'visible' == width of bar inside slider == 
                    increment taken when click inside slider window */
               (int)Math.round(minval*SBscales[sbIndex]), 
               (int)Math.round(maxval*SBscales[sbIndex] +visible) );
               /* Note that the actual maximum value of the scroll bar is 
               the maximum minus the visible. The left side of the bubble 
               indicates the value of the scroll bar. */
      scrollbars[sbIndex].addAdjustmentListener(this);
      scrollbars[sbIndex].setUnitIncrement(Math.max(1,
            (int)Math.round((maxval-minval)*SBscales[sbIndex]/maxClicks)));
      sbc.gridx = 1;
      sbc.ipadx = 75; // set the scrollbar width (internal padding) to 75 pixels
      sbGrid.setConstraints(scrollbars[sbIndex], sbc);
      sbPanel.add(scrollbars[sbIndex]);
      sbc.ipadx = 0;  // reset
      
      // current value label
      SBdigits[sbIndex] = digits;
      SBcurValueLabels[sbIndex] = new Label(IJ.d2s(SBcurValues[sbIndex], digits));

      sbc.gridx = 2;
      sbc.insets = new Insets(5, 5, 0, 0);
      sbc.anchor = GridBagConstraints.EAST;
      sbGrid.setConstraints(SBcurValueLabels[sbIndex], sbc);
      sbPanel.add(SBcurValueLabels[sbIndex]);
      
      c.gridwidth = 2; // this panel will take up one grid in overall GUI
      c.gridx = x; c.gridy = y;
      c.insets = new Insets(0, 0, 0, 0);
      c.anchor = GridBagConstraints.CENTER;
      grid.setConstraints(sbPanel, c);
      activePanel.add(sbPanel);

      sbIndex++; 
      if (activePanel == this) { x=0; y++; }
      else x++;
      SBtotal = sbIndex;
    } // end scrollbar field
  
    public void setScrollBarUnitIncrement(int inc) {
      scrollbars[sbIndex-1].setUnitIncrement(inc);
    }
   
    /** Returns the contents of the next scrollbar field. */
    public double getNextScrollBar() {
      if (scrollbars[sbIndex]==null)
         return -1.0;
      else return SBcurValues[sbIndex++];
    }
    /** Returns the contents of scrollbar field 'i' */
    public double getScrollBarValue(int i) {
      if (i<0 || i>=SBtotal || scrollbars[i]==null)
         return -1.0;
      else return SBcurValues[i];
    }
    /** Sets the contents of scrollbar field 'i' to 'value' */
    public void setScrollBarValue(int i, double value) {
      if (i<0 || i>=SBtotal || scrollbars[i]==null)  return;
      scrollbars[i].setValue((int)Math.round(value*SBscales[i]));
      SBcurValues[i] = value;
      SBcurValueLabels[i].setText(IJ.d2s(SBcurValues[i], SBdigits[i]));
    }

    public synchronized void adjustmentValueChanged(AdjustmentEvent e) {
      for (int i=0; i<SBtotal; i++) {
        if (e.getSource()==scrollbars[i]) {
            SBcurValues[i] = scrollbars[i].getValue()/ SBscales[i];
          setScrollBarValue(i,SBcurValues[i]);
          SBlastTouched = i; // set keyboard input to be directed to this scrollbar
        }
      }
      sbIndex = 0; // reset for next call to getNextScrollBar()
    }
    
    /** Displays this dialog box. */
    public void showDialog() {
      sbIndex = 0; 
      super.showDialog();
   }
}  // end class GenericDialogPlus

/** create a GenericDialog that remains active to allow for repeated 
  * runs through target program, enabling the use of add-on buttons to 
  * perform different and repeatable functions.
  * See code at bottom and WindowLevelAdjuster class for implementation examples 
    */
public class GenericRecallableDialog extends GenericDialogPlus
         implements AdjustmentListener, KeyListener, FocusListener {
   private Button[] buttons = new Button[MAX_ITEMS]; 
   private boolean[] buttons_touched = new boolean[MAX_ITEMS]; 
   private int butIndex, butTot;
   Thread thread;
   public final int WEST=0, CENTER=1, EAST=2; // location flags

   public GenericRecallableDialog(String title) {
       super(title);
       setModal(false);
    }
    
    public GenericRecallableDialog(String title, Frame parent) {
      super(title, parent);
       setModal(false);
  }
    
  /** changes from parent showDialog(): remove accept button */
  public void showDialog() {
      nfIndex = 0;
      sfIndex = 0;
      cbIndex = 0;
      choiceIndex = 0;
      sbIndex = 0;
      butIndex = 0;
      if (macro) {
         //IJ.write("showDialog: "+macroOptions);
         dispose();
         return;
      }
    if (stringField!=null&&numberField==null) {
       TextField tf = (TextField)(stringField.elementAt(0));
       tf.selectAll();
    }
      cancel = new Button(" Done "); // changed from "Cancel"
      cancel.addActionListener(this);
      c.gridx = 0; c.gridy = y;
      c.anchor = GridBagConstraints.CENTER;
      c.insets = new Insets(5, 0, 0, 0); // top,left,bot,right coords
      grid.setConstraints(cancel, c);
      add(cancel);
    if (IJ.isMacintosh())
      setResizable(false);
      pack();
      GUI.center(this);
      setVisible(true);
      IJ.wait(250); // work around for Sun/WinNT bug
  }
  
   /** the keyboard input (arrow keys) will be caught by whichever button 
      * has the current focus, but will affect the scrollbar last touched   */
   public void keyPressed(KeyEvent e) {
      int keyCode = e.getKeyCode();
      IJ.setKeyDown(keyCode);
      if (scrollbars[SBlastTouched] != null) {
         // left is 37, right is 39;  numpad4 is 100, numpad6 is 102
         if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_NUMPAD4) {
            SBcurValues[SBlastTouched] -= 
                  scrollbars[SBlastTouched].getUnitIncrement()/SBscales[SBlastTouched];
            setScrollBarValue(SBlastTouched,SBcurValues[SBlastTouched]);
         }
         if (keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_NUMPAD6) {
            SBcurValues[SBlastTouched] += 
                  scrollbars[SBlastTouched].getUnitIncrement()/SBscales[SBlastTouched];
            setScrollBarValue(SBlastTouched,SBcurValues[SBlastTouched]);
         }
      }
   }

   public void actionPerformed(ActionEvent e) {
      wasCanceled = (e.getSource()==cancel);
      nfIndex = 0; // reset these so that call to getNext...() will work
      sfIndex = 0;
      cbIndex = 0;
      sbIndex = 0; 
      choiceIndex = 0;
      for (int i=0; i<butTot; i++)
         buttons_touched[i] = (e.getSource()==buttons[i]);
      butIndex = 0;
      if (wasCanceled) { setVisible(false); dispose(); }
   }

  /** Adds a button to the dialog window */
  public void addButton(String text1) {
      Panel butPanel = new Panel();
      GridBagLayout butGrid = new GridBagLayout();
      butPanel.setLayout(butGrid);
      addButtonToPanel(text1, butPanel, butGrid, 0);
      c.gridwidth = 1;
      c.gridx = x; c.gridy = y;
      c.insets = new Insets(0, 0, 0, 0);
      c.anchor = GridBagConstraints.CENTER;
      grid.setConstraints(butPanel, c);
      activePanel.add(butPanel);
      if (activePanel == this) { x=0; y++; }
      else x++;
   }
  /** adds 2(3,4) buttons in a row to the dialog window. 
     * Easily extendable to add more buttons */
  public void addButtons(String text1, String text2) {
      Panel butPanel = new Panel();
      GridBagLayout butGrid = new GridBagLayout();
      butPanel.setLayout(butGrid);
      addButtonToPanel(text1, butPanel, butGrid, 0);
      addButtonToPanel(text2, butPanel, butGrid, 1);
      c.gridwidth = 2;
      c.gridx = 0; c.gridy = y;
      c.anchor = GridBagConstraints.WEST;
      c.insets = new Insets(5, 0, 5, 0);
      grid.setConstraints(butPanel, c);
      activePanel.add(butPanel);
      y++;
   }
  public void addButtons(String text1, String text2, String text3) {
      Panel butPanel = new Panel();
      GridBagLayout butGrid = new GridBagLayout();
      butPanel.setLayout(butGrid);
      addButtonToPanel(text1, butPanel, butGrid, 0); // label, panel, row in grid
      addButtonToPanel(text2, butPanel, butGrid, 1);
      addButtonToPanel(text3, butPanel, butGrid, 2);
      c.gridwidth = 2;
      c.gridx = 0; c.gridy = y;
      c.anchor = GridBagConstraints.WEST;
      c.insets = new Insets(5, 0, 5, 0);
      grid.setConstraints(butPanel, c);
      activePanel.add(butPanel);
      y++;
   }
  public void addButtons(String text1, String text2, String text3, String text4){
      Panel butPanel = new Panel();
      GridBagLayout butGrid = new GridBagLayout();
      butPanel.setLayout(butGrid);
      addButtonToPanel(text1, butPanel, butGrid, 0); // label, panel, row in grid
      addButtonToPanel(text2, butPanel, butGrid, 1);
      addButtonToPanel(text3, butPanel, butGrid, 2);
      addButtonToPanel(text4, butPanel, butGrid, 3);
      c.gridwidth = 1;
      c.gridx = 0; c.gridy = y;
      c.anchor = GridBagConstraints.WEST;
      c.insets = new Insets(5, 0, 5, 0);
      grid.setConstraints(butPanel, c);
      activePanel.add(butPanel);
      y++;
   }
  public void addButtonToPanel(String text, Panel panel, 
                          GridBagLayout grid, int row) {
    if (butIndex >= MAX_ITEMS) {
         IJ.write("  cannot add another button, have maxed out at: "+butIndex);
         return;
      }
      GridBagConstraints butc  = new GridBagConstraints();
      buttons[butIndex] = new Button(text);
      buttons[butIndex].addActionListener(this);   
      buttons[butIndex].addKeyListener(this);   
      butc.gridx = row; butc.gridy = 0;
      butc.anchor = GridBagConstraints.WEST;
      butc.insets = new Insets(0, 5, 0, 10);
      grid.setConstraints(buttons[butIndex], butc);
      panel.add(buttons[butIndex]);
      buttons_touched[butIndex] = false;
      butIndex++; butTot = butIndex; 
  }
    
  /** Returns the contents of the next buttons_touched field. */
  public boolean getNextButton() {
      if (butIndex>=butTot)
         return false;
      if (buttons_touched[butIndex]) {
         buttons_touched[butIndex++] = false;
         return true;
      }
      butIndex++;
      return false; // else
  }
    
  /** Returns the contents of button 'i' field. */
  public boolean getButtonValue(int i) {
      if (i<0 || i>=butTot)
         return false;
      else if (!buttons_touched[i]) 
         return false; 
      buttons_touched[i] = false; // reset vale to false
      return true; // else
  }
}

/** Walter O'Dell PhD,  wodell@rochester.edu,   11/6/02
 *   alterations from original GenericDialog class in ImageJ v1.28 :
 *     changed global variable attributes from 'private' to 'protected'
 *            for variables: (Buttons) cancel, okay; 
 *                                  (boolean) wasCanceled, macro
 *                                  (int) y, nfIndex, sfIndex, cbIndex, choiceIndex
 *                                  (GridBagLayout) grid
 *                                  (GridBagConstraints) c
 *         added variables: (Container) activePanel
 *                                   (int) x; // height location of object in panel
 *      
 *   all changes within the code are denoted with "// WO " and are associated 
 *   with settings of the 'activePanel' and 'x' variables.  These variables are
 *   needed to implement having multiple objects occupying the same height  
 *   location within the dialog window -- a string of objects across the panel 
 *
 ** This class is a customizable modal dialog box. */
class GenericDialog2 extends Dialog implements ActionListener,
TextListener, FocusListener, ItemListener, KeyListener {

   protected Vector defaultValues,defaultText,numberField,stringField,checkbox,choice;
   protected Component theLabel;
   protected TextArea textArea1,textArea2;
   // WO 4/26/02 changed all from 'private' to 'protected'
   protected Button cancel, okay;
   protected boolean wasCanceled;
   protected int x; // WO added
   protected int y;
   protected int nfIndex, sfIndex, cbIndex, choiceIndex;
   protected GridBagLayout grid;
   protected GridBagConstraints c; 
   private boolean firstNumericField=true;
   private boolean invalidNumber;
   private boolean firstPaint = true;
   private Hashtable labels;
   protected boolean macro;
   private String macroOptions;

   protected Container activePanel; // WO added

    /** Creates a new GenericDialog with the specified title. Uses the current image
       image window as the parent frame or the ImageJ frame if no image windows
       are open. Dialog parameters are recorded by ImageJ's command recorder but
       this requires that the first word of each label be unique. */
   public GenericDialog2(String title) {
      this(title, WindowManager.getCurrentImage()!=null?
         (Frame)WindowManager.getCurrentImage().getWindow():IJ.getInstance());
   }

    /** Creates a new GenericDialog using the specified title and parent frame. */
  public GenericDialog2(String title, Frame parent) {
      super(parent, title, true);
      grid = new GridBagLayout();
      c = new GridBagConstraints();
      setLayout(grid);
      macroOptions = Macro.getOptions();
      macro = macroOptions!=null;
      addKeyListener(this);
      activePanel = this; // WO added
  }
    
   //void showFields(String id) {
   //   String s = id+": ";
   //   for (int i=0; i<maxItems; i++)
   //      if (numberField[i]!=null)
   //         s += i+"='"+numberField[i].getText()+"' ";
   //   IJ.write(s);
   //}

   /** Adds a numeric field. The first word of the label must be
      unique or command recording will not work.
   * @param label         the label
   * @param defaultValue   value to be initially displayed
   * @param digits         number of digits to right of decimal point
   */
  public void addNumericField(String label, double defaultValue, int digits) {
      Label theLabel = makeLabel(label);
      c.gridx = x; // WO prev: c.gridx = 0; 
      c.gridy = y;
      c.anchor = GridBagConstraints.EAST;
      c.gridwidth = 1;
      if (firstNumericField)
         c.insets = new Insets(5, 0, 3, 0);
      else
         c.insets = new Insets(0, 0, 3, 0);
      grid.setConstraints(theLabel, c);
      activePanel.add(theLabel); // WO prev: add(theLabel);

      if (numberField==null) {
         numberField = new Vector(5);
         defaultValues = new Vector(5);
         defaultText = new Vector(5);
      }
      TextField tf = new TextField(IJ.d2s(defaultValue, digits), 6);
      tf.addActionListener(this);
      tf.addTextListener(this);
      tf.addFocusListener(this);
   //   tf.addKeyListener(this);
      numberField.addElement(tf);
      defaultValues.addElement(defaultValue);
      defaultText.addElement(tf.getText());
      x++; c.gridx = x; // WO prev: c.gridx = 1; 
      c.gridy = y;
      c.anchor = GridBagConstraints.WEST;
      grid.setConstraints(tf, c);
      tf.setEditable(true);
      if (firstNumericField) tf.selectAll();
      firstNumericField = false;
      activePanel.add(tf); // WO prev: add(tf);
      if (Recorder.record || macro)
         saveLabel(tf, label);
      if (activePanel == this) { x=0; y++; } // WO prev: y++;
      else x++; // WO added
    }
    
    private Label makeLabel(String label) {
      // if (IJ.isMacintosh())
      //    label += " ";
      return new Label(label);
    }
    
    private void saveLabel(Component component, String label) {
       if (labels==null)
          labels = new Hashtable();
      labels.put(component, label);
    }
    
   /** Adds an 8 column text field.
   * @param label         the label
   * @param defaultText      the text initially displayed
   */
   public void addStringField(String label, String defaultText) {
      addStringField(label, defaultText, 8);
   }

   /** Adds a text field.
   * @param label         the label
   * @param defaultText      text initially displayed
   * @param columns         width of the text field
   */
   public void addStringField(String label, String defaultText, int columns) {
      Label theLabel = makeLabel(label);
      c.gridx = x; //WO prev: c.gridx = 0; 
      c.gridy = y;
      c.anchor = GridBagConstraints.EAST;
      c.gridwidth = 1;
      if (stringField==null) {
         stringField = new Vector(4);
         c.insets = new Insets(5, 0, 5, 0);
      } else
         c.insets = new Insets(0, 0, 5, 0);
      grid.setConstraints(theLabel, c);
      activePanel.add(theLabel); // WO prev: add(theLabel);

      TextField tf = new TextField(defaultText, columns);
      tf.addActionListener(this);
      tf.addTextListener(this);
      tf.addFocusListener(this);
//      tf.addKeyListener(this);
      x++; c.gridx = x; //WO prev: c.gridx = 1; 
      c.gridy = y;
      c.anchor = GridBagConstraints.WEST;
      grid.setConstraints(tf, c);
      tf.setEditable(true);
      activePanel.add(tf); // WO prev: add(tf);
      stringField.addElement(tf);
      if (Recorder.record || macro)
         saveLabel(tf, label);
      if (activePanel == this) { x=0; y++; } // WO added
      else x++; // WO prev: y++;
    }
    
   /** Adds a checkbox.
   * @param label         the label
   * @param defaultValue   the initial state
   */
    public void addCheckbox(String label, boolean defaultValue) {
       if (checkbox==null) {
          checkbox = new Vector(4);
         c.insets = new Insets(15, 20, 0, 0);
       } else
         c.insets = new Insets(0, 20, 0, 0);
      c.gridx = x; // WO prev: c.gridx = 0; 
      c.gridy = y;
      c.gridwidth = 2;
      c.anchor = GridBagConstraints.WEST;
      Checkbox cb = new Checkbox(label);
      grid.setConstraints(cb, c);
      cb.setState(defaultValue);
      cb.addItemListener(this);
      cb.addKeyListener(this);
      activePanel.add(cb); // WO prev: add(cb);
      checkbox.addElement(cb);
      //ij.IJ.write("addCheckbox: "+ y+" "+cbIndex);
      if (Recorder.record || macro)
         saveLabel(cb, label);
      if (activePanel == this) { x=0; y++; } // WO added
      else x++; // WO prev:  y++;
    }
    
   /** Adds a group of checkboxs using a grid layout.
   * @param rows         the number of rows
   * @param columns      the number of columns
   * @param labels         the labels
   * @param defaultValues   the initial states
   */
    public void addCheckboxGroup(int rows, int columns, String[] labels, boolean[] defaultValues) {
       Panel panel = new Panel();
       panel.setLayout(new GridLayout(rows,columns,10,0));
       int startCBIndex = cbIndex;
       int i1 = 0;
       int[] index = new int[labels.length];
       if (checkbox==null)
          checkbox = new Vector(12);
       for (int row=0; row<rows; row++) {
         for (int col=0; col<columns; col++) {
            int i2 = col*rows+row;
            if (i2>=labels.length)
               break;
            index[i1] = i2;
            Checkbox cb = new Checkbox(labels[i1]);
            checkbox.addElement(cb);
            cb.setState(defaultValues[i1]);
            if (Recorder.record || macro)
               saveLabel(cb, labels[i1]);
            panel.add(cb);
             i1++;
         }
      }
      c.gridx = x; // WO prev: c.gridx = 0; 
      c.gridy = y;
      c.gridwidth = 2;
      c.anchor = GridBagConstraints.WEST;
      c.insets = new Insets(10, 0, 0, 0);
      grid.setConstraints(panel, c);
      activePanel.add(panel); // WO prev: add(panel); 
      if (activePanel == this) { x=0; y++; } // WO added
      else x++; // WO prev: y++;
    }

    /** Adds a popup menu.
   * @param label   the label
   * @param items   the menu items
   * @param defaultItem   the menu item initially selected
   */
   public void addChoice(String label, String[] items, String defaultItem) {
      Label theLabel = makeLabel(label);
      c.gridx = x; // WO prev: c.gridx = 0; 
      c.gridy = y;
      c.anchor = GridBagConstraints.EAST;
      c.gridwidth = 1;
      if (choice==null) {
         choice = new Vector(4);
         c.insets = new Insets(5, 0, 5, 0);
      } else
         c.insets = new Insets(0, 0, 5, 0);
      grid.setConstraints(theLabel, c);
      activePanel.add(theLabel); // WO prev: add(theLabel);
      Choice thisChoice = new Choice();
      thisChoice.addKeyListener(this);
      thisChoice.addItemListener(this);
      for (int i=0; i<items.length; i++)
         thisChoice.addItem(items[i]);
      thisChoice.select(defaultItem);
      x++; c.gridx = x; // WO prev: c.gridx = 1; 
      c.gridy = y;
      c.anchor = GridBagConstraints.WEST;
      grid.setConstraints(thisChoice, c);
      activePanel.add(thisChoice);  // WO prev: add(thisChoice);
      choice.addElement(thisChoice);
      if (Recorder.record || macro)
         saveLabel(thisChoice, label);
      if (activePanel == this) { x=0; y++; } // WO added
      else x++; // WO prev: y++;
    }
    
    /** Adds a message consisting of one or more lines of text. */
    public void addMessage(String text) {
       if (text.indexOf('\n')>=0)
         theLabel = new MultiLineLabel(text);
      else
         theLabel = new Label(text);
      //theLabel.addKeyListener(this);
      c.gridx = x; // WO  prev: = 0; 
      c.gridy = y;
      c.gridwidth = 2;
      c.anchor = GridBagConstraints.WEST;
      c.insets = new Insets(text.equals("")?0:10, 20, 0, 0);
      grid.setConstraints(theLabel, c);
      activePanel.add(theLabel); // WO prev: add(theLabel);
      if (activePanel == this) { x=0; y++; } // WO added
      else x++; // WO prev: y++;
    }
    
   /** Adds one or two (side by side) text areas.
   * @param text1   initial contents of the first text area
   * @param text2   initial contents of the second text area or null
   * @param rows   the number of rows
   * @param rows   the number of columns
   */
    public void addTextAreas(String text1, String text2, int rows, int columns) {
       if (textArea1!=null)
          return;
       Panel panel = new Panel();
      textArea1 = new TextArea(text1,rows,columns,TextArea.SCROLLBARS_NONE);
      //textArea1.append(text1);
      panel.add(textArea1);
      if (text2!=null) {
         textArea2 = new TextArea(text2,rows,columns,TextArea.SCROLLBARS_NONE);
         //textArea2.append(text2);
         panel.add(textArea2);
      }
      c.gridx = x; // WO prev: = 0; 
      c.gridy = y;
      c.gridwidth = 2;
      c.anchor = GridBagConstraints.WEST;
      c.insets = new Insets(15, 20, 0, 0);
      grid.setConstraints(panel, c);
      activePanel.add(panel); // WO prev: add(panel);
      if (activePanel == this) { x=0; y++; } // WO added
      else x++; // WO prev: y++;
    }
    
   /** Returns true if the user clicks on "Cancel". */
    public boolean wasCanceled() {
       if (wasCanceled)
          Macro.abort();
       return wasCanceled;
    }
    
   /** Returns the contents of the next numeric field. */
   public double getNextNumber() {
      if (numberField==null)
         return -1.0;
      TextField tf = (TextField)numberField.elementAt(nfIndex);
      String theText = tf.getText();
      if (macro) {
         String label = (String)labels.get((Object)tf);
         theText = Macro.getValue(macroOptions, label, theText);
         //IJ.write("getNextNumber: "+label+"  "+theText);
      }   
      String originalText = (String)defaultText.elementAt(nfIndex);
      double defaultValue = (Double) (defaultValues.elementAt(nfIndex));
      double value;
      if (theText.equals(originalText))
         value = defaultValue;
      else {
         Double d = getValue(theText);
         if (d!=null)
            value = d;
         else {
            invalidNumber = true;
            value = 0.0;
         }
      }
      if (Recorder.record)
         recordOption(tf, trim(theText));
      nfIndex++;
      return value;
    }
    
   private String trim(String value) {
      if (value.endsWith(".0"))
         value = value.substring(0, value.length()-2);
      if (value.endsWith(".00"))
         value = value.substring(0, value.length()-3);
      return value;
   }

   private void recordOption(Component component, String value) {
      String label = (String)labels.get((Object)component);
      Recorder.recordOption(label, value);
   }

   private void recordCheckboxOption(Checkbox cb) {
      String label = (String)labels.get((Object)cb);
      if (cb.getState() && label!=null)
         Recorder.recordOption(label);
   }

    protected Double getValue(String theText) {
       Double d;
       try {d = Double.valueOf(theText);}
      catch (NumberFormatException e){
         d = null;
      }
      return d;
   }

   /** Returns true if one or more of the numeric fields contained an invalid number. */
   public boolean invalidNumber() {
       boolean wasInvalid = invalidNumber;
       invalidNumber = false;
       return wasInvalid;
    }
    
     /** Returns the contents of the next text field. */
   public String getNextString() {
         String theText;
      if (stringField==null)
         return "";
      TextField tf = (TextField)(stringField.elementAt(sfIndex));
      theText = tf.getText();
      if (macro) {
         String label = (String)labels.get((Object)tf);
         theText = Macro.getValue(macroOptions, label, theText);
         //IJ.write("getNextString: "+label+"  "+theText);
      }   
      if (Recorder.record)
         recordOption(tf, theText);
      sfIndex++;
      return theText;
    }
    
     /** Returns the state of the next checkbox. */
    public boolean getNextBoolean() {
      if (checkbox==null)
         return false;
      Checkbox cb = (Checkbox)(checkbox.elementAt(cbIndex));
      if (Recorder.record)
         recordCheckboxOption(cb);
      boolean state = cb.getState();
      if (macro) {
         String label = (String)labels.get((Object)cb);
         String key = Macro.trimKey(label);
         state = macroOptions.indexOf(key+" ")>=0;
         //IJ.write("getNextBoolean: "+label+"  "+state);
      }
      cbIndex++;
      return state;
    }
    
     /** Returns the selected item in the next popup menu. */
    public String getNextChoice() {
      if (choice==null)
         return "";
      Choice thisChoice = (Choice)(choice.elementAt(choiceIndex));
      String item = thisChoice.getSelectedItem();
      if (macro) {
         String label = (String)labels.get((Object)thisChoice);
         item = Macro.getValue(macroOptions, label, item);
         //IJ.write("getNextChoice: "+label+"  "+item);
      }   
      if (Recorder.record)
         recordOption(thisChoice, item);
      choiceIndex++;
      return item;
    }
    
     /** Returns the index of the selected item in the next popup menu. */
    public int getNextChoiceIndex() {
      if (choice==null)
         return -1;
      Choice thisChoice = (Choice)(choice.elementAt(choiceIndex));
      int index = thisChoice.getSelectedIndex();
      if (macro) {
         String label = (String)labels.get((Object)thisChoice);
         String oldItem = thisChoice.getSelectedItem();
         int oldIndex = thisChoice.getSelectedIndex();
         String item = Macro.getValue(macroOptions, label, oldItem);
         thisChoice.select(item);
         index = thisChoice.getSelectedIndex();
         if (index==oldIndex && !item.equals(oldItem)) {
            IJ.showMessage(getTitle(), "\""+item+"\" is not a vaid choice for \""+label+"\"");
            Macro.abort();
         }

      }   
      if (Recorder.record)
         recordOption(thisChoice, thisChoice.getSelectedItem());
      choiceIndex++;
      return index;
  }
    
  /** Returns the contents of the next text area. */
  public String getNextText() {
    String text;
    if (textArea1!=null) {
         textArea1.selectAll();
         text = textArea1.getText();
         textArea1 = null;
    } else if (textArea2!=null) {
         textArea2.selectAll();
         text = textArea2.getText();
         textArea2 = null;
      } else
         text = null;
      return text;
  }

  /** Displays this dialog box. */
  public void showDialog() {
      nfIndex = 0;
      sfIndex = 0;
      cbIndex = 0;
      choiceIndex = 0;
      if (macro) {
         //IJ.write("showDialog: "+macroOptions);
         dispose();
         return;
      }
    if (stringField!=null&&numberField==null) {
       TextField tf = (TextField)(stringField.elementAt(0));
       tf.selectAll();
    }
      Panel buttons = new Panel();
    buttons.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
      cancel = new Button("Cancel");
      cancel.addActionListener(this);
      okay = new Button("  OK  ");
      okay.addActionListener(this);
      if (IJ.isMacintosh()) {
         buttons.add(cancel);
         buttons.add(okay);
      } else {
         buttons.add(okay);
         buttons.add(cancel);
      }
      c.gridx = 0; c.gridy = y;
      c.anchor = GridBagConstraints.EAST;
      c.gridwidth = 2;
      c.insets = new Insets(15, 0, 0, 0);
      grid.setConstraints(buttons, c);
      activePanel.add(buttons); // WO prev: add(buttons); 
    if (IJ.isMacintosh())
      setResizable(false);
      pack();
      setup();
      GUI.center(this);
      IJ.wait(250); // work around for Sun/WinNT bug
  }
    
   protected void setup() {
   }

   public void actionPerformed(ActionEvent e) {
      wasCanceled = (e.getSource()==cancel);
      setVisible(false);
      dispose();
   }

   public void textValueChanged(TextEvent e) {
   }

   public void itemStateChanged(ItemEvent e) {
   }

   public void focusGained(FocusEvent e) {
      Component c = e.getComponent();
      if (c instanceof TextField)
         ((TextField)c).selectAll();
   }

   public void focusLost(FocusEvent e) {
      Component c = e.getComponent();
      if (c instanceof TextField)
         ((TextField)c).select(0,0);
   }

    public void keyPressed(KeyEvent e) {
      int keyCode = e.getKeyCode();
      IJ.setKeyDown(keyCode);
   }

   public void keyReleased(KeyEvent e) {
      IJ.setKeyUp(e.getKeyCode());
   }
      
   public void keyTyped(KeyEvent e) {}

   public Insets getInsets() {
       Insets i= super.getInsets();
       return new Insets(i.top+10, i.left+10, i.bottom+10, i.right+10);
   }

  public void paint(Graphics g) {
    super.paint(g);
      if (firstPaint && numberField!=null) {
         TextField tf = (TextField)(numberField.elementAt(0));
       tf.requestFocus();
       firstPaint = false;
    }
  }
       
}