package ij.astro.accessors;

import java.awt.*;
import java.util.ArrayList;

public interface IPlotObject {
    //These constants are copied from PlotObject - they should match it!

    /** The serialVersionUID should not be modified, otherwise saved plots won't be readable any more */
    long serialVersionUID = 1L;
    /** Constants for the type of objects. These are powers of two so one can use them as masks */
    int XY_DATA = 1, ARROWS = 2, LINE = 4, NORMALIZED_LINE = 8, DOTTED_LINE = 16,
            LABEL = 32, NORMALIZED_LABEL = 64, LEGEND = 128, AXIS_LABEL = 256, FRAME = 512, SHAPES = 1024;
    /** mask for recovering font style from the flags */
    int FONT_STYLE_MASK = 0x0f;
    /** flag for the data set passed with the constructor. Note that 0 to 0x0f are reserved for fonts modifiers, 0x010-0x800 are reserved for legend modifiers */
    int CONSTRUCTOR_DATA = 0x1000;
    /** flag for hiding a PlotObject */
    int HIDDEN = 0x2000;

    int getType();

    void setType(int type);

    int getFlags();

    void setFlags(int flags);

    String getOptions();

    void setOptions(String options);

    float[] getxValues();

    void setxValues(float[] xValues);

    float[] getyValues();

    void setyValues(float[] yValues);

    float[] getxEValues();

    void setxEValues(float[] xEValues);

    float[] getyEValues();

    void setyEValues(float[] yEValues);

    ArrayList getShapeData();

    void setShapeData(ArrayList shapeData);

    String getShapeType();

    void setShapeType(String shapeType);

    int getShape();

    void setShape(int shape);

    float getLineWidth();

    void setLineWidth(float lineWidth);

    Color getColor();

    void setColor(Color color);

    Color getColor2();

    void setColor2(Color color2);

    double getX();

    void setX(double x);

    double getY();

    void setY(double y);

    double getxEnd();

    void setxEnd(double xEnd);

    double getyEnd();

    void setyEnd(double yEnd);

    int getStep();

    void setStep(int step);

    String getLabel();
    void setLabel(String label);

    int getJustification();

    void setJustification(int justification);

    String getMacroCode();

    void setMacroCode(String macroCode);

    String getFontFamily();

    void setFontFamily(String fontFamily);

    float getFontSize();

    Font getFont();

    void setFontSize(float fontSize);

    boolean hasFlag(int hidden);

    boolean hasCurve();

    boolean hasFilledMarker();

    int getMarkerSize();

    boolean hasMarker();

    void setFont(Font font);
}
