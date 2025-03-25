package Astronomy.multiaperture.io.transformers;

import Astronomy.multiaperture.io.Section;
import Astronomy.multiaperture.io.Transformer;

public class Header implements Transformer<Header, Void> {
    public static final String HEADER = "AIJ APERTURES FILE";
    private int majorVersion;
    private int minorVersion;
    private static final Section.Parameter<Integer> VERSION_PARAMETER = new Section.Parameter<>("version", 0, Integer.TYPE);

    public Header() {
        this(2, 1);
    }

    public Header(int majorVersion, int minorVersion) {
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
    }

    @Override
    public Header load(Void params, Section section) {
        var view = section.createMapView();

        if (!view.contains("majorVersion")) {
            throw new IllegalStateException("Header missing majorVersion");
        }

        if (!view.contains("minorVersion")) {
            throw new IllegalStateException("Header missing minorVersion");
        }

        var majorVersionSection = view.get("majorVersion");
        var minorVersionSection = view.get("minorVersion");

        if (majorVersionSection.size() != 1) {
            throw new IllegalStateException("Header has %s majorVersions!".formatted(majorVersionSection.size()));
        }

        if (minorVersionSection.size() != 1) {
            throw new IllegalStateException("Header has %s minorVersions!".formatted(minorVersionSection.size()));
        }

        var majorVersion = majorVersionSection.get(0).getParameter(VERSION_PARAMETER);
        var minorVersion = minorVersionSection.get(0).getParameter(VERSION_PARAMETER);

        return new Header(majorVersion, minorVersion);
    }

    public Section write() {
        return write(null, this);
    }

    @Override
    public Section write(Void params, Header obj) {
        var headerSection = new Section(HEADER);

        headerSection.addSubsection(Section.createSection("majorVersion", VERSION_PARAMETER, obj.majorVersion));
        headerSection.addSubsection(Section.createSection("minorVersion", VERSION_PARAMETER, obj.minorVersion));

        return headerSection;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public void setMajorVersion(int majorVersion) {
        this.majorVersion = majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public void setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"majorVersion\":")
                .append(majorVersion);

        sb.append(",\"minorVersion\":")
                .append(minorVersion);

        sb.append('}');
        return sb.toString();
    }
}
