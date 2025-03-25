package astroj;

import ij.ImagePlus;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

public class BulkPixelRoi extends PixelRoi {
    // The scale used to render the dot, must be an odd number. Increasing makes the dot smaller
    private static final int SCALE = 3;
    // HashSet relies on overridden hashcode in Pixel to only care about coordinates for #contains
    private final Set<Pixel> pixels = new HashSet<>(20);
    private final Rectangle bounds = new Rectangle(0, 0, -1, -1);
    private final Object lock = new Object();
    private BufferedImage bufferedImage;

    public BulkPixelRoi(ImagePlus imagePlus) {
        super(0, 0);
        setImage(imagePlus);
    }

    @Override
    public void draw(Graphics g) {
        boolean aij = false;
        var aijTransform = new AffineTransform();
        if (ic instanceof astroj.AstroCanvas) {
            aij = true;
            ac = (AstroCanvas) ic;
            canvTrans = ((Graphics2D) g).getTransform();
            ((Graphics2D) g).setTransform(ac.invCanvTrans);
            aijTransform = ac.canvTrans;
            netFlipX = ac.getNetFlipX();
            netFlipY = ac.getNetFlipY();
            netRotate = ac.getNetRotate();
        }

        if (showPixel) {
            /*int sx = screenXD(bounds.x + Centroid.PIXELCENTER);
            int sy = screenYD(bounds.y + Centroid.PIXELCENTER);*/

            // Create transform to screen space coordinates
            var srcRect = ic.getSrcRect();

            var scaleTransform = AffineTransform.getScaleInstance(ic.getMagnification(), ic.getMagnification());
            // Stay in IJ space so no 0.5 pixel offset to pixel center
            var translateTransform = AffineTransform.getTranslateInstance(-srcRect.x, -srcRect.y);

            var toScreenSpace = new AffineTransform(aijTransform);
            toScreenSpace.concatenate(scaleTransform);
            toScreenSpace.concatenate(translateTransform);
            //toScreenSpace.concatenate(AffineTransform.getScaleInstance(1 / (double) SCALE, 1 / (double) SCALE));

            synchronized (lock) {
                if (bufferedImage == null) {
                    bufferedImage = createImage();
                }

                if (g instanceof Graphics2D g2) {
                    g2.transform(toScreenSpace);

                    // Shrink image so that dots are smaller than a pixel
                    g2.transform(AffineTransform.getScaleInstance(1 / (double) SCALE, 1 / (double) SCALE));

                    g2.drawImage(bufferedImage, bounds.x * SCALE, bounds.y * SCALE, null);
                }

            }
        }

        if (aij) {
            ((Graphics2D) g).setTransform(canvTrans);
        }
    }

    private BufferedImage createImage() {
        //todo we could copy the old image into the new one to save having to loop over every pixel again
        BufferedImage image;
        synchronized (lock) {
            image = new BufferedImage((bounds.width + 1) * SCALE, (bounds.height + 1) * SCALE, BufferedImage.TYPE_INT_ARGB);

            for (Pixel pixel : pixels) {
                image.setRGB((pixel.x - bounds.x) * SCALE + (SCALE / 2), (pixel.y - bounds.y) * SCALE + (SCALE / 2), pixel.rgb());
            }
        }

        return image;
    }

    public void addPixel(double x, double y) {
        synchronized (lock) {
            var ip = imp.getProcessor();
            pixels.add(new Pixel((int) x, (int) y, ip.getPixelValue((int) x, (int) y) > ((ip.getMax() + ip.getMin()) / 2.0)));
            bounds.add((int) x, (int) y);
            bufferedImage = null;
        }
        //repaint();
    }

    private record Pixel(int x, int y, boolean isGreater) {
        public Color color() {
            return isGreater ? Color.BLACK : Color.WHITE;
        }

        public int rgb() {
            return color().getRGB();
        }

        // See point2D for hashcode
        // Used by the hashset storing pixels
        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            return result;
        }
    }
}
