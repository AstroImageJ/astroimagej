package nom.tam.fits;

import nom.tam.image.ImageTiler;
import nom.tam.util.ArrayDataOutput;
import nom.tam.util.ArrayFuncs;

import java.io.IOException;
import java.util.Arrays;

/**
 * Simple implementation that will cut a tile out to the given stream. Useful for web applications that provide a cutout
 * service. The idea is that the ImageData object will be extracted from an overlapping HDU (without first reading so as
 * not to fill up the memory), and one of these objects are created for the output. <code>
 *     Fits source = new Fits(myFile);
 *     ImageHDU imageHDU = source.getHDU(1);
 *     
 *     // We must manually adjust the header for the cutout image as necessary
 *     Header tileHeader = ...
 *     
 *     // Define the cutout region
 *     int[] tileStarts = new int[]{10, 10};
 *     int[] tileLengths = new int[]{45, 60};
 *     int[] tileSteps = new int[]{1, 1};
 *     
 *     // Create the cutout
 *     StreamingTileImageData streamingTileImageData = new StreamingTileImageData(tileHeader, imageHDU.getTiler(),
 *         tileStarts, tileLengths, tileSteps);
 *     
 *     // Write the cutout to the output
 *     Fits output = new Fits();
 *     output.addHDU(FitsFactory.hduFactory(tileHeader, streamingTileImageData));
 *     
 *     // The cutout happens at write time!
 *     output.write(outputStream);  
 * </code>
 *
 * @since 1.18
 */
public class StreamingTileImageData extends ImageData {
    private final int[] corners;
    private final int[] lengths;
    private final int[] steps;
    private final ImageTiler imageTiler;

    /**
     * Constructor for a tile image data object.
     *
     * @param header The header representing the desired cutout. It is the responsibility of the caller to adjust the
     *            header appropriately.
     * @param tiler The tiler to slice pixels out with.
     * @param corners The corners to start tiling.
     * @param lengths The count of values to extract.
     * @param steps The number of jumps to make to the next read. Optional, defaults to 1 for each axis.
     * 
     * @throws FitsException If the provided Header is unreadable
     */
    public StreamingTileImageData(final Header header, final ImageTiler tiler, final int[] corners, final int[] lengths,
            int[] steps) throws FitsException {
        super(header);

        if (ArrayFuncs.isEmpty(corners) || ArrayFuncs.isEmpty(lengths)) {
            throw new IllegalArgumentException(
                    "Cannot tile out with empty corners or lengths.  Use ImageData if no " + "tiling is desired.");
        } else if (ArrayFuncs.isEmpty(steps)) {
            this.steps = new int[corners.length];
            Arrays.fill(this.steps, 1);
        } else if (Arrays.stream(steps).anyMatch(i -> i < 1)) {
            throw new IllegalArgumentException("Negative or zero step values not supported.");
        } else {
            this.steps = steps;
        }

        this.imageTiler = tiler;
        this.corners = corners;
        this.lengths = lengths;
    }

    public int[] getSteps() {
        final int[] stepsCopy = new int[steps.length];
        System.arraycopy(steps, 0, stepsCopy, 0, stepsCopy.length);

        return stepsCopy;
    }

    @Override
    public void write(ArrayDataOutput o) throws FitsException {
        try {
            final ImageTiler tiler = this.imageTiler;
            if (tiler == null || getTrueSize() == 0) {
                // Defer writing of unknowns to the parent.
                super.write(o);
            } else {
                tiler.getTile(o, this.corners, this.lengths, this.steps);
                FitsUtil.pad(o, getTrueSize());
            }
        } catch (IOException ioException) {
            throw new FitsException(ioException.getMessage(), ioException);
        }
    }
}
