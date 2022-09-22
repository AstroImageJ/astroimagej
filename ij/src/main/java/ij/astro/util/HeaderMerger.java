package ij.astro.util;

import ij.ImagePlus;

import java.util.HashSet;
import java.util.Set;

public class HeaderMerger {
    private final int method;
    private Set<String> headers = new HashSet<>();

    public HeaderMerger(int method) {
        this.method = method;
    }

    public void addHeader(String header) {
        headers.add(header);
    }

    public void addHeader(ImagePlus imp, int slice) {
        //todo call astroj.FitsJ.getHeader(ij.ImagePlus, int) or other
    }

    public void setHeader(ImagePlus imp) {
        //todo impl, if no headers (ie not a FITS file, don't add a fake header)
        mergeHeaders();
    }

    private String mergeHeaders() {
        //todo impl, handle method
        return null;
    }
}
