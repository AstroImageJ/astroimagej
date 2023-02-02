package astroj;

import ij.ImagePlus;
import ij.astro.util.ImageType;
import ij.plugin.ZProjector;

import java.util.ArrayList;

public class FitsHeaderMerger {


     /**
     * @see ZProjector#mergeFitsHeaders(int, int, int, int, ImagePlus, ImagePlus)
     */
    public static void mergeHeaders(int method, int startSlice, int stopSlice, int increment, ImagePlus origImage, ImagePlus projImage) {
        // Get all the headers
        int stackCnt = 0;
        ArrayList<String[]> headers = new ArrayList<>();
        for (int slice = startSlice; slice <= stopSlice; slice += increment) {
            String[] header = FitsJ.getHeader(origImage, slice);
            if (header != null) {
                headers.add(header);
            }
            stackCnt++;
        }

        // If no headers were found then this is probably not a stack of FITS files.  Just return
        if (headers.size() == 0) {
            return;
        }

        // Find the first and last JD; Get the exposure time of the last one.
        double firstJD = Double.MAX_VALUE;
        double lastJD = 0.0;
        double totalExposure = 0.0;
        double lastExposure = 0.0;
        for (String[] header : headers) {
            double jd = FitsJ.getJD(header);
            if (!Double.isNaN(jd)) {
                if (jd < firstJD)
                    firstJD = jd;
                if (jd > lastJD) {
                    lastJD = jd;
                    lastExposure = FitsJ.getExposureTime(header)/60/60/24;
                }
                totalExposure += FitsJ.getExposureTime(header);
            }
        }

        // Calculate midpoint of stacked exposure (This is patterned after ASTAP)
        double avgJD = (firstJD + (lastJD + lastExposure)) / 2;

        // Update the new header
        // Go through all the cards and delete any that are not in the list of cards to save
        String[] newHeader = headers.get(0).clone();

        String[] keysToSave = {
                "SIMPLE", "BITPIX", "NAXIS", "NAXIS1", "NAXIS2", "EXTEND", "BZERO", "BSCALE",
                "XBINNING", "YBINNING", "XPIXSZ", "YPIXSZ", "IMAGETYP", "GAIN", "OFFSET", "INSTRUME", "ROWORDER",
                "SITELAT", "SITELONG", "OBJCTRA", "OBJCTDEC", "FILTER", "SET-TEMP", "OBJECT", "EQUINOX"
        };
        ArrayList<String> newHeaderList = new ArrayList<>();
        for (String card : newHeader) {
            for (String key : keysToSave) {
                if (card.startsWith(key)) {
                    newHeaderList.add(card);
                    break;
                }
            }
        }
        newHeader = newHeaderList.toArray(new String[0]);

        newHeader = FitsJ.addHistory("Combined stack of " + stackCnt + " images using: " + ZProjector.METHODS[method], newHeader);
        newHeader = FitsJ.set("EXPTIME", String.valueOf(totalExposure), "Total luminance exposure time in seconds", newHeader);
        newHeader = FitsJ.set("DATE-OBS", "'" + JulianDate.dateTime(lastJD) + "'", "DATE_OBS of last exposure in stack", newHeader);
        newHeader = FitsJ.set("JD-AVG", String.valueOf(avgJD), "Julian Day of the observation mid-point.", newHeader);
        newHeader = FitsJ.set("DATE-AVG", "'" + JulianDate.dateTime(avgJD) + "'", "", newHeader);

        // Update BITPIX
        var newBitpix = ImageType.getType(projImage.getProcessor()).getExpectedBitpix();
        newHeader[FitsJ.findCardWithKey("BITPIX", newHeader)] = FitsJ.createCard("BITBIX", String.valueOf(newBitpix), "bits per data value");
        
        // Add new header to the projected image.
        FitsJ.putHeader(projImage, newHeader);
    }
}
