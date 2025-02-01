package astroj;

public sealed interface Aperture permits ApertureRoi, FreeformPixelApertureRoi, ShapedApertureRoi {

    ApertureShape getApertureShape();

    boolean getIsCentroid();

    boolean getIsComparisonStar();

    public enum ApertureShape {
        CIRCULAR,
        FREEFORM_SHAPE,
        FREEFORM_PIXEL,
    }
}
