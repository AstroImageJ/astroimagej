package astroj;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
/**
 *
 * @author Karen
 */
public class CurrentKeyboardState
    {

    private static boolean isShiftDown; 
    private static boolean isControlDown;
    private static boolean isAltDown;
    
 
    static { 
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher( 
            new KeyEventDispatcher() { 
                public boolean dispatchKeyEvent(KeyEvent e) { 
                    isShiftDown = e.isShiftDown(); 
                    isControlDown = e.isControlDown();
                    isAltDown = e.isAltDown();
                    return false; 
                } 
            }); 
    } 
 
    public static boolean isShiftDown() { 
        return isShiftDown; 
    } 
    
    public static boolean isControlDown() { 
        return isControlDown; 
        
    }   
    public static boolean isAltDown() { 
        return isAltDown; 
    }   
 
} 
   