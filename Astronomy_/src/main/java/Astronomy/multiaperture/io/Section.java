package Astronomy.multiaperture.io;

import ij.astro.types.MultiMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Section {
    protected final String name;
    private List<String> parameters = new ArrayList<>();
    private List<Section> subSections = new ArrayList<>();
    private final boolean isRoot;
    private Section parent;

    public Section(String name) {
        this(name, false);
    }

    public Section(String name, boolean isRoot) {
        this.name = name;
        this.isRoot = isRoot;
    }

    public void addSubsection(Section section) {
        subSections.add(Objects.requireNonNull(section));
        section.parent = this;
    }

    public void addParameter(String s) {
        parameters.add(Objects.requireNonNull(s));
    }

    public String getParameter(int i, String paramName) {
        if (i >= parameters.size()) {
            //System.out.println(this);
            throw new IllegalArgumentException("Could not get %s (%s) parameter for %s".formatted(paramName, i, name));
        }

        return parameters.get(i);
    }

    public String getParametersLine() {
        if (parameters.isEmpty()) {
            return "";
        }

        var sb = new StringBuilder(parameters.get(0));

        for (int i = 1; i < parameters.size(); i++) {
            sb.append('\t').append(parameters.get(i));
        }

        return sb.toString();
    }

    public List<String> getParameters() {
        return parameters;
    }

    public List<Section> getSubSections() {
        return subSections;
    }

    public boolean hasParameters() {
        return !parameters.isEmpty();
    }

    public boolean hasSubsections() {
        return !subSections.isEmpty();
    }

    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }

    public static Section createSection(String name, String... parameters) {
        var s = new Section(name);

        for (String parameter : parameters) {
            s.addParameter(parameter);
        }

        return s;
    }

    public MultiMap<String, Section> createMapView() {
        var map = new MultiMap<String, Section>();

        if (!subSections.isEmpty()) {
            for (Section subSection : subSections) {
                map.put(subSection.name, subSection);
            }
        }

        return map;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"name\":\"")
                .append(Objects.toString(name, "")).append('\"');

        sb.append(",\"parameters\":");
        if ((parameters) != null && !(parameters).isEmpty()) {
            sb.append("[");
            final int listSize = (parameters).size();
            for (int i = 0; i < listSize; i++) {
                final Object listValue = (parameters).get(i);
                if (listValue instanceof CharSequence) {
                    sb.append("\"").append(Objects.toString(listValue, "")).append("\"");
                } else {
                    sb.append(Objects.toString(listValue, ""));
                }
                if (i < listSize - 1) {
                    sb.append(",");
                } else {
                    sb.append("]");
                }
            }
        } else {
            sb.append("[]");
        }

        sb.append(",\"subSections\":");
        if ((subSections) != null && !(subSections).isEmpty()) {
            sb.append("[");
            final int listSize = (subSections).size();
            for (int i = 0; i < listSize; i++) {
                final Object listValue = (subSections).get(i);
                if (listValue instanceof CharSequence) {
                    sb.append("\"").append(Objects.toString(listValue, "")).append("\"");
                } else {
                    sb.append(Objects.toString(listValue, ""));
                }
                if (i < listSize - 1) {
                    sb.append(",");
                } else {
                    sb.append("]");
                }
            }
        } else {
            sb.append("[]");
        }

        sb.append(",\"isRoot\":")
                .append(isRoot);

        sb.append('}');
        return sb.toString();
    }

    public boolean isRoot() {
        return isRoot;
    }

    public String name() {
        return name;
    }

    public Section getParent() {
        return parent;
    }
}
