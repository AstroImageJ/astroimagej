package astroj;

import ij.ImagePlus;
import ij.plugin.ZProjector;

import java.util.ArrayList;

public class FitsHeaderMerger {


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
        String[] newHeader = headers.get(0).clone();

        newHeader = FitsJ.addHistory("Stack of " + stackCnt + " images using method: " + ZProjector.METHODS[method], newHeader);
        newHeader = FitsJ.removeCards("EXPTIME", newHeader);
        newHeader = FitsJ.set("EXPTIME", String.valueOf(totalExposure), "Total luminance exposure time in seconds", newHeader);
        newHeader = FitsJ.removeCards("DATE-OBS", newHeader);
        newHeader = FitsJ.set("DATE-OBS", JulianDate.dateTime(lastJD), "DATE_OBS of last exposure in stack", newHeader);
        newHeader = FitsJ.set("JD-AVG", String.valueOf(avgJD), "Julian Day of the observation mid-point.", newHeader);
        newHeader = FitsJ.set("DATE-AVG", JulianDate.dateTime(avgJD), "", newHeader);

        // Remove the WCS
        String[] keysToRemove = {
                "CRPIX1", "CRPIX2", "CRVAL1", "CRVAL2", "CDELT1", "CDELT2",
                "CROTA1", "CROTA2", "CD1_1", "CD1_2", "CD2_1", "CD2_2", "PLTSOLVD"};
        for (String key : keysToRemove) {
            newHeader = FitsJ.removeCards(key, newHeader);
        }

        // Remove any comments that say "Solved"
        ArrayList<String> newHeaderList = new ArrayList<>();
        for (String card : newHeader) {
            if (!card.startsWith("COMMENT") || (!card.contains("Solved") && !card.contains("solved"))) {
                newHeaderList.add(card);
            }
        }
        newHeader = newHeaderList.toArray(new String[0]);

        // Add new header to the projected image.
        FitsJ.putHeader(projImage, newHeader);
    }

}
