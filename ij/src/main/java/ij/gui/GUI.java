package ij.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Scrollbar;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Menus;
import ij.Prefs;
import ij.astro.AstroImageJ;
import ij.text.TextPanel;

/** This class consists of static GUI utility methods. */
public class GUI {
	private static final Font DEFAULT_FONT = ImageJ.SansSerif12;
	private static Color lightGray = new Color(240,240,240);
	private static boolean isWindows8;
	private static Color scrollbarBackground = new Color(245,245,245);
	@AstroImageJ(reason = "Scale frames")
	private static boolean scaledMenu = false;

	static {
		if (IJ.isWindows()) {
			String osname = System.getProperty("os.name");
			isWindows8 = osname.contains("unknown") || osname.contains("8");
		}
	}

	/** Positions the specified window in the center of the screen that contains target. */
	public static void center(Window win, Component target) {
		if (win == null)
			return;
		Rectangle bounds = getMaxWindowBounds(target);
		Dimension window = win.getSize();
		if (window.width == 0)
			return;
		int left = bounds.x + Math.max(0, (bounds.width - window.width) / 2);
		int top = bounds.y + Math.max(0, (bounds.height - window.height) / 4);
		win.setLocation(left, top);
	}
	
	/** Positions the specified window in the center of the
		 screen containing the "ImageJ" window. */
	public static void centerOnImageJScreen(Window win) {
		center(win, IJ.getInstance());
	}

	public static void center(Window win) {
		center(win, win);
	}
	
	private static java.util.List<GraphicsConfiguration> getScreenConfigs() {
		java.util.ArrayList<GraphicsConfiguration> configs = new java.util.ArrayList<GraphicsConfiguration>();
		for (GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
			configs.add(device.getDefaultConfiguration());
		}
		return configs;
	}
	
	/**
	 * Get maximum bounds for the screen that contains a given point.
	 * @param point Coordinates of point.
	 * @param accountForInsets Deduct the space taken up by menu and status bars, etc. (after point is found to be inside bonds)
	 * @return Rectangle of bounds or <code>null</code> if point not inside of any screen.
	 */
	public static Rectangle getScreenBounds(Point point, boolean accountForInsets) {
		if (GraphicsEnvironment.isHeadless())
			return new Rectangle(0,0,0,0);
		for (GraphicsConfiguration config : getScreenConfigs()) {
			Rectangle bounds = config.getBounds();
			if (bounds != null && bounds.contains(point)) {
				Insets insets = accountForInsets ? Toolkit.getDefaultToolkit().getScreenInsets(config) : null;
				return shrinkByInsets(bounds, insets);
			}
		}
		return null;		
	}
	
	/**
	 * Get maximum bounds for the screen that contains a given component.
	 * @param component An AWT component located on the desired screen.
	 * If <code>null</code> is provided, the default screen is used.
	 * @param accountForInsets Deduct the space taken up by menu and status bars, etc.
	 * @return Rectangle of bounds.
	 */	
	public static Rectangle getScreenBounds(Component component, boolean accountForInsets) {
		if (GraphicsEnvironment.isHeadless())
			return new Rectangle(0,0,0,0);
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();		
		GraphicsConfiguration gc = component == null ? ge.getDefaultScreenDevice().getDefaultConfiguration() :
													   component.getGraphicsConfiguration();   
		Insets insets = accountForInsets ? Toolkit.getDefaultToolkit().getScreenInsets(gc) : null;
		return shrinkByInsets(gc.getBounds(), insets);
	}

	public static Rectangle getScreenBounds(Point point) {
		return getScreenBounds(point, false);
	}		

	public static Rectangle getScreenBounds(Component component) {
		return getScreenBounds(component, false);
	}			

	public static Rectangle getScreenBounds() {
		return getScreenBounds((Component)null);
	}			

	public static Rectangle getMaxWindowBounds(Point point) {
		return getScreenBounds(point, true);
	}

	public static Rectangle getMaxWindowBounds(Component component) {
		return getScreenBounds(component, true);
	}
	
	public static Rectangle getMaxWindowBounds() {
		return getMaxWindowBounds((Component)null);
	}
	
	private static Rectangle shrinkByInsets(Rectangle bounds, Insets insets) {
		Rectangle shrunk = new Rectangle(bounds);
		if (insets == null) return shrunk; 
		shrunk.x += insets.left;
		shrunk.y += insets.top;
		shrunk.width -= insets.left + insets.right;
		shrunk.height -= insets.top + insets.bottom;
		return shrunk;
	}
	
	public static Rectangle getZeroBasedMaxBounds() {
		for (GraphicsConfiguration config : getScreenConfigs()) {
			Rectangle bounds = config.getBounds();
			if (bounds != null && bounds.x == 0 && bounds.y == 0)
				return bounds;
		}
		return null;
	}
	
	public static Rectangle getUnionOfBounds() {
		Rectangle unionOfBounds = new Rectangle();
		for (GraphicsConfiguration config : getScreenConfigs()) {
			unionOfBounds = unionOfBounds.union(config.getBounds());
		}
		return unionOfBounds;
	}
	
    static private Frame frame;
    
    /** Obsolete */
    public static Image createBlankImage(int width, int height) {
        if (width==0 || height==0)
            throw new IllegalArgumentException("");
		if (frame==null) {
			frame = new Frame();
			frame.pack();
			frame.setBackground(Color.white);
		}
        Image img = frame.createImage(width, height);
        return img;
    }
    
    /** Lightens overly dark scrollbar background on Windows 8. */
    public static void fix(Scrollbar sb) {
    }
    
    public static boolean showCompositeAdvisory(ImagePlus imp, String title) {
    	if (imp==null || imp.getCompositeMode()!=IJ.COMPOSITE || imp.getNChannels()==1 || IJ.macroRunning())
    		return true;
    	String msg = "Channel "+imp.getC()+" of this color composite image will be processed.";
		GenericDialog gd = new GenericDialog(title);
		gd.addMessage(msg);
		gd.showDialog();
		return !gd.wasCanceled();
	}
	
	/**
	 * Scales an AWT component according to {@link Prefs#getGuiScale()}.
	 * @param component the AWT component to be scaled. If a container, scaling is applied to all its child components
	 */
	public static void scale(final Component component) {
		final float scale = (float)Prefs.getGuiScale();
		if (scale==1f)
			return;
		if (component instanceof Container)
			scaleComponents((Container)component, scale);
		else
			scaleComponent(component, scale);
	}

	private static void scaleComponents(final Container container, final float scale) {
		for (final Component child : container.getComponents()) {
			if (child instanceof Container)
				scaleComponents((Container) child, scale);
			else
				scaleComponent(child, scale);
		}
	}

	private static void scaleComponent(final Component component, final float scale) {
		Font font = component.getFont();
		if (font == null)
			font = DEFAULT_FONT;
		font = font.deriveFont(scale*font.getSize());
		component.setFont(font);
	}

	public static void scalePopupMenu(final PopupMenu popup) {
		//System.out.println("scalePopupMenu1: "+popup);
		if (Menus.getFontSize()!=0) {
            popup.setFont(Menus.getFont(false));
			//System.out.println("scalePopupMenu2: "+popup.getFont());
            return;
        }
		final float scale = (float)Prefs.getGuiScale();
		if (scale==1f)
			return;
		Font font=popup.getFont();
		if (font==null)
			font = new Font("SansSerif", Font.PLAIN, (int)(scale*13));
		else
			font = font.deriveFont(scale*font.getSize());
		popup.setFont(font);
		//System.out.println("scalePopupMenu3: "+popup.getFont());
	}
	
	/**
	 * Tries to detect if a Swing component is unscaled and scales it it according
	 * to {@link #getGuiScale()}.
	 * <p>
	 * This is mainly relevant to linux: Swing components scale automatically on
	 * most platforms, specially since Java 8. However there are still exceptions to
	 * this on linux: e.g., In Ubuntu, Swing components do scale, but only under the
	 * GTK L&F. (On the other hand AWT components do not scale <i>at all</i> on
	 * hiDPI screens on linux).
	 * </p>
	 * <p>
	 * This method tries to avoid exaggerated font sizes by detecting if a component
	 * has been already scaled by the UIManager, applying only
	 * {@link #getGuiScale()} to the component's font if not.
	 * </p>
	 *
	 * @param component the component to be scaled
	 * @return true, if component's font was resized
	 */
	public static boolean scale(final JComponent component) {
		final double guiScale = Prefs.getGuiScale();
		if (guiScale == 1d)
			return false;
		Font font = component.getFont();
		if (font == null && component instanceof JList)
			font = UIManager.getFont("List.font");
		else if (font == null && component instanceof JTable)
			font = UIManager.getFont("Table.font");
		else if (font == null)
			font = UIManager.getFont("Label.font");
		if (font.getSize() > DEFAULT_FONT.getSize())
			return false;
		if (component instanceof JTable)
			((JTable) component).setRowHeight((int) (((JTable) component).getRowHeight() * guiScale * 0.9));
		else if (component instanceof JList)
			((JList<?>) component).setFixedCellHeight((int) (((JList<?>) component).getFixedCellHeight() * guiScale * 0.9));
		component.setFont(font.deriveFont((float) guiScale * font.getSize()));
		return true;
	}

	@AstroImageJ(reason = "Scale frames")
	public static void scaleFrame(Dialog dialog, Component... processed) {
		scale(dialog, Prefs.getGuiScale(), new HashSet<>(Arrays.asList(processed)));
	}

	@AstroImageJ(reason = "Scale frames")
	public static void scaleFrame(Frame frame, Component... processed) {
		if (frame == null || Prefs.getGuiScale() <= 0 || Prefs.getGuiScale() == 1) {
			return;
		}

		var mb = frame.getMenuBar();
        if (mb != null) {
			mb.setFont(Menus.getFont(true));
        }

		if (!scaledMenu) {
			var font = (Font) UIManager.get("Menu.font");
			if (font != null) {
				var newSize = (float) (font.getSize2D() * Prefs.getGuiScale());
				UIManager.put("Menu.font", font.deriveFont(newSize));
			}

			font = (Font) UIManager.get("MenuBar.font");
			if (font != null) {
				var newSize = (float) (font.getSize2D() * Prefs.getGuiScale());
				UIManager.put("MenuBar.font", font.deriveFont(newSize));
			}

			font = (Font) UIManager.get("MenuItem.font");
			if (font != null) {
				var newSize = (float) (font.getSize2D() * Prefs.getGuiScale());
				UIManager.put("MenuItem.font", font.deriveFont(newSize));
			}

			font = (Font) UIManager.get("CheckBoxMenuItem.font");
			if (font != null) {
				var newSize = (float) (font.getSize2D() * Prefs.getGuiScale());
				UIManager.put("CheckBoxMenuItem.font", font.deriveFont(newSize));
			}

			font = (Font) UIManager.get("PopupMenu.font");
			if (font != null) {
				var newSize = (float) (font.getSize2D() * Prefs.getGuiScale());
				UIManager.put("PopupMenu.font", font.deriveFont(newSize));
			}

			font = (Font) UIManager.get("RadioButtonMenuItem.font");
			if (font != null) {
				var newSize = (float) (font.getSize2D() * Prefs.getGuiScale());
				UIManager.put("RadioButtonMenuItem.font", font.deriveFont(newSize));
			}

			scaledMenu = true;
		}

		scale(frame, Prefs.getGuiScale(), new HashSet<>(Arrays.asList(processed)));
	}

	@AstroImageJ(reason = "Scale frames")
	private static void scale(Component comp, double factor, Set<Component> processed) {
		if (comp == null || factor <= 0 || factor == 1) {
			return;
		}

		if (processed.contains(comp)) {
			return;
		}

		processed.add(comp);

		var font = comp.getFont();
		if (font != null && !scaledFont(font)) {
			var newSize = (float) (font.getSize2D() * factor);
			var scaledFont = font.deriveFont(newSize);
			comp.setFont(scaledFont);
			if (comp instanceof TextPanel textPanel) {
				textPanel.setFont(scaledFont, IJ.isMacOSX() || Prefs.get("tw.font.anti", true));
			}
		}

		if (comp instanceof JMenuBar) {
			return;
		}

        //noinspection rawtypes
		if (comp instanceof JComboBox comboBox) {
			var newItems = new java.util.ArrayList<>();
			for (int i = 0; i < comboBox.getItemCount(); i++) {
				var item = comboBox.getItemAt(i);
				if (item instanceof ImageIcon imageIcon) {
					item = scaleImageIcon(imageIcon, factor);
				}
				if (item instanceof Component component) {
					scale(component, factor, processed);
				}
				newItems.add(item);
			}

			//noinspection rawtypes,unchecked
			comboBox.setModel(new javax.swing.DefaultComboBoxModel(newItems.toArray(new Object[0])));
		}

        if (comp instanceof JSlider jSlider) {
			var table = jSlider.getLabelTable();
            //noinspection unchecked
            table.elements().asIterator().forEachRemaining(label -> {
                if (label instanceof JLabel jLabel) {
					scale(jLabel, factor, processed);
                }
			});
			jSlider.setLabelTable(table);
		}

		if (comp instanceof JTable jTable) {
			jTable.setRowHeight((int) ((jTable.getRowHeight() * factor * 0.9)));
			scale(jTable.getTableHeader(), factor, processed);
		}

		if (comp instanceof JList<?> jList) {
			jList.setFixedCellHeight((int) (jList.getFixedCellHeight() * factor * 0.9));
		}

        if (comp instanceof JComponent jComponent) {
            var border = jComponent.getBorder();
            if (border instanceof TitledBorder titledBorder) {
                font = titledBorder.getTitleFont();
				var newSize = (float) (font.getSize2D() * factor);
				titledBorder.setTitleFont(font.deriveFont(newSize));

				var b = titledBorder.getBorder();
				if (b instanceof LineBorder lb) {
					var thickness = Math.max(1, (int) Math.round(lb.getThickness() * factor));
					titledBorder.setBorder(new LineBorder(lb.getLineColor(), thickness, lb.getRoundedCorners()));
				}
            }

			if (border instanceof LineBorder lb) {
				var thickness = Math.max(1, (int) Math.round(lb.getThickness() * factor));
				jComponent.setBorder(new LineBorder(lb.getLineColor(), thickness, lb.getRoundedCorners()));
			}

			var popup = jComponent.getComponentPopupMenu();
            if (popup != null) {
                scale(popup, factor, processed);
            }
        }

        if (comp.isPreferredSizeSet()) {
			var size = comp.getPreferredSize();
            comp.setPreferredSize(new Dimension((int) Math.max(1, Math.round(size.width * factor)),
					(int) Math.max(1, Math.round(size.height * factor))));
        }

        if (comp.isMaximumSizeSet()) {
			var size = comp.getMaximumSize();
			comp.setMaximumSize(new Dimension((int) Math.max(1, Math.round(size.width * factor)),
					(int) Math.max(1, Math.round(size.height * factor))));
        }

		if (comp.isMinimumSizeSet()) {
			var size = comp.getMinimumSize();
			comp.setMinimumSize(new Dimension((int) Math.max(1, Math.round(size.width * factor)),
					(int) Math.max(1, Math.round(size.height * factor))));
		}

		if (comp instanceof AbstractButton button) {
			if (button.getDisabledIcon() instanceof ImageIcon imageIcon) {
				button.setDisabledIcon(scaleImageIcon(imageIcon, factor));
			}

			if (button.getPressedIcon() instanceof ImageIcon imageIcon) {
				button.setPressedIcon(scaleImageIcon(imageIcon, factor));
			}

			if (button.getRolloverIcon() instanceof ImageIcon imageIcon) {
				button.setRolloverIcon(scaleImageIcon(imageIcon, factor));
			}

			if (button.getDisabledSelectedIcon() instanceof ImageIcon imageIcon) {
				button.setDisabledSelectedIcon(scaleImageIcon(imageIcon, factor));
			}

			if (button.getRolloverSelectedIcon() instanceof ImageIcon imageIcon) {
				button.setRolloverIcon(scaleImageIcon(imageIcon, factor));
			}

			if (button.getSelectedIcon() instanceof ImageIcon imageIcon) {
				button.setSelectedIcon(scaleImageIcon(imageIcon, factor));
			}

			if (button.getIcon() instanceof ImageIcon imageIcon) {
				button.setIcon(scaleImageIcon(imageIcon, factor));
			}
		}

		if (comp instanceof JLabel label) {
			if (label.getIcon() instanceof ImageIcon imageIcon) {
				label.setIcon(scaleImageIcon(imageIcon, factor));
			}

			if (label.getDisabledIcon() instanceof ImageIcon imageIcon) {
				label.setDisabledIcon(scaleImageIcon(imageIcon, factor));
			}
		}

		if (comp instanceof Container container) {
			for (Component child : container.getComponents()) {
				scale(child, factor, processed);
			}
		}
	}

	private static boolean scaledFont(Font original) {
		if (!scaledMenu) {
			var font = (Font) UIManager.get("Menu.font");
            if (Objects.equals(font, original)) {
                return true;
            }

			font = (Font) UIManager.get("MenuBar.font");
			if (Objects.equals(font, original)) {
				return true;
			}

			font = (Font) UIManager.get("MenuItem.font");
			if (Objects.equals(font, original)) {
				return true;
			}

			font = (Font) UIManager.get("CheckBoxMenuItem.font");
			if (Objects.equals(font, original)) {
				return true;
			}

			font = (Font) UIManager.get("PopupMenu.font");
			if (Objects.equals(font, original)) {
				return true;
			}

			font = (Font) UIManager.get("RadioButtonMenuItem.font");
			if (Objects.equals(font, original)) {
				return true;
			}
		}

		return false;
	}

	@AstroImageJ(reason = "Scale frames")
	private static ImageIcon scaleImageIcon(ImageIcon in, double factor) {
		if (in == null || factor <= 0) {
			return in;
		}

		var img = in.getImage();
		if (img == null) {
			return in;
		}

		var w = img.getWidth(null);
		var h = img.getHeight(null);
		if (w <= 0 || h <= 0) {
			return in;
		}

		var nw = Math.max(1, (int) Math.round(w * factor));
		var nh = Math.max(1, (int) Math.round(h * factor));

		var bufferedImage = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
		var g2 = bufferedImage.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.drawImage(img, 0, 0, nw, nh, null);
		g2.dispose();

		return new ImageIcon(bufferedImage);
	}

	/** Works around an OpenJDK bug on Windows that
	 * causes the scrollbar thumb color and background
	 * color to be almost identical.
	*/
	public static final void fixScrollbar(Scrollbar sb) {
		if (IJ.isWindows())
			sb.setBackground(scrollbarBackground);
	}
	
	/** Returns a new NonBlockingGenericDialog with the given title,
	 *  except when Java is running in headless mode, in which case
	 *  a GenericDialog is be returned.
	*/
	public static GenericDialog newNonBlockingDialog(String title) {
		if (GraphicsEnvironment.isHeadless())
			return new GenericDialog(title);
		else
			return new NonBlockingGenericDialog(title);
	}

	/** Returns a new NonBlockingGenericDialog with the given title
	 * if Prefs.nonBlockingFilterDialogs is 'true' and 'imp' is
	 * displayed, otherwise returns a GenericDialog.
	 * @param title Dialog title
	 * @param imp The image associated with this dialog
	*/
	public static GenericDialog newNonBlockingDialog(String title, ImagePlus imp) {
		if (Prefs.nonBlockingFilterDialogs && imp!=null && imp.getWindow()!=null) {
			NonBlockingGenericDialog gd = new NonBlockingGenericDialog(title);
			gd.imp = imp;
			return gd;
		} else
			return new GenericDialog(title);
	}

	
}
