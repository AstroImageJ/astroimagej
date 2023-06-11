package util;

import java.awt.*;

public class ColorUtil {
    public static Color makeBrighter(Color color) {
        var c = mixColorsWithContrast(color, Color.WHITE);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), color.getAlpha());
    }

    public static Color mixColorsWithContrast(Color color1, Color color2) {
        double brightness1 = calculatePerceivedBrightness(color1);
        double brightness2 = calculatePerceivedBrightness(color2);

        // Determine the mixing ratio based on the brightness of the colors
        double ratio = brightness1 / (brightness1 + brightness2);

        // Force black to behave
        if (ratio == 0 || ratio == 1) {//todo use midpoint instead of radio?
            ratio = 0.6;
        } /*else {
            ratio *= 1.055; // Bias towards the first (non-white) color
        }*/

        // Mix the colors with the calculated ratio
        int red = (int) (color1.getRed() * ratio + color2.getRed() * (1 - ratio));
        int green = (int) (color1.getGreen() * ratio + color2.getGreen() * (1 - ratio));
        int blue = (int) (color1.getBlue() * ratio + color2.getBlue() * (1 - ratio));

        return new Color(red, green, blue);
    }

    // Assumes sRGB, range 0 (black)-100 (white)
    // From https://stackoverflow.com/a/56678483
    private static double calculatePerceivedBrightness(Color color) {
        var colors = color.getColorComponents(null);

        for (int i = 0; i < colors.length; i++) {
            if (colors[i] <= 0.04045) {
                colors[i] /= 12.92;
            } else {
                colors[i] = (float) Math.pow(((colors[i] + 0.055)/1.055), 2.4);
            }
        }

        var Y = (0.2126 * colors[0] + 0.7152 * colors[1] + 0.0722 * colors[2]);

        if ( Y <= (216D/24389D)) {       // The CIE standard states 0.008856 but 216/24389 is the intent for 0.008856451679036
            return Y * (24389D/27D);  // The CIE standard states 903.3, but 24389/27 is the intent, making 903.296296296296296
        } else {
            return Math.pow(Y,(1D/3D)) * 116 - 16;
        }
    }
}
