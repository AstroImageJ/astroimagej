package ij.astro.io.pixel_maps;

public record BpmHeader(int majorVersion, int minorVersion) {
    public static final int MAJOR_VERSION = 1;
    public static final int MINOR_VERSION = 0;

    public BpmHeader() {
        this(MAJOR_VERSION, MINOR_VERSION);
    }
}
