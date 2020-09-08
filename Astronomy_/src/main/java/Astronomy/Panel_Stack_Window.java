package Astronomy;

import ij.plugin.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

public class Panel_Stack_Window implements PlugIn {

    public void run(String arg) {
        ImagePlus imp = IJ.createImage("Stack", "16-bit Ramp", 400, 400, 50);
        CustomCanvas cc = new CustomCanvas(imp);
        new CustomStackWindow(imp, cc);
    }


    class CustomCanvas extends ImageCanvas {
    
        CustomCanvas(ImagePlus imp) {
            super(imp);
        }
    
        public void mousePressed(MouseEvent e) {
            super.mousePressed(e);
            IJ.log("mousePressed: ("+offScreenX(e.getX())+","+offScreenY(e.getY())+")"  +imp);
        }
    
    } // CustomCanvas inner class
    
    
    class CustomStackWindow extends StackWindow implements ActionListener {
    
        private Button button1, button2, button3;
        ImagePlus imp2 = IJ.createImage("Stack", "16-bit Ramp", 600, 300, 60);
        ImagePlus imp3 = IJ.createImage("Image", "16-bit Ramp", 600, 300, 1);
        boolean firstRun = true;
       
        CustomStackWindow(ImagePlus imp, ImageCanvas ic) {
            super(imp, ic);

            addPanel();
        }
    
        void addPanel() {
            Panel panel = new Panel();
            panel.setLayout(new FlowLayout(FlowLayout.RIGHT));
            button1 = new Button("600x300x60");
            button1.addActionListener(this);
            panel.add(button1);
            button2 = new Button("600x300x1");
            button2.addActionListener(this);
            panel.add(button2);
            button3 = new Button("300x300x40");
            button3.addActionListener(this);
            panel.add(button3);
            add(panel);
            pack();
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            Point loc = getLocation();
            Dimension size = getSize();
            if (loc.y+size.height>screen.height)
                getCanvas().zoomOut(0, 0);
         }
      
        public void actionPerformed(ActionEvent e) {
            Object b = e.getSource();
            if (b==button1) {
                if (firstRun) {
                    firstRun = false;
                } else {
                    imp2 = imp;
                }
                ImageProcessor ip2 = imp2.getProcessor();
                StackProcessor sp2 = new StackProcessor(imp2.getStack(), ip2);
                ImageStack s2 = null;
                s2 = sp2.rotateLeft();
                imp.setStack(null, s2);
                IJ.log(""+imp);
            }
            else if (b == button2) {
                if (firstRun) {
                    firstRun = false;
                } else {
                    imp3 = imp;
                }
                ImageProcessor ip3 = imp3.getProcessor();
                StackProcessor sp3 = new StackProcessor(imp3.getStack(), ip3);
                ImageStack s3 = null;
                s3 = sp3.rotateLeft();
                imp.setStack(null, s3);
                IJ.log(""+imp);
            }
            else {
                ImagePlus imp2 = IJ.createImage("Stack", "16-bit Ramp", 300, 300, 40);
                imp.setStack(imp2.getStack());
                IJ.log(""+imp);
            }
    
        }
        
    } // CustomStackWindow inner class

} // Panel_Stack_Window class
