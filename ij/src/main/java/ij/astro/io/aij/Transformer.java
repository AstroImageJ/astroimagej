package ij.astro.io.aij;

import java.util.List;

import ij.astro.types.MultiMap;

public abstract class Transformer<T, PARAM> {
    protected final AijFileCodec codec;

    public Transformer(AijFileCodec codec) {
        this.codec = codec;
    }

    public abstract T load(PARAM parameter, Section section);

    public abstract Section write(PARAM parameter, T obj);

    public boolean hasRequiredSections(Section section, String... requiredSections) {
        for (String requiredSection : requiredSections) {
            var hasSection = false;
            subSection: for (Section subSection : section.getSubSections()) {
                if (subSection.name.equals(requiredSection)) {
                    hasSection = true;
                    break subSection;
                }
            }

            if (!hasSection) {
                return false;
            }
        }


        return true;
    }

    protected Section getUniqueSection(MultiMap<String, Section> view, String name) {
        return getUniqueSection(view, name, true);
    }

    protected Section getUniqueSection(MultiMap<String, Section> view, String name, boolean required) {
        var l = view.get(name);
        var c = l == null ? 0 : l.size();

        if ((required && c != 1) || (!required && c > 1)) {
            throw new IllegalStateException("File has %s %s(s)!".formatted(c, name));
        }

        return c == 0 ? null : l.getFirst();
    }

    protected List<Section> getRequiredSection(MultiMap<String, Section> view, String name) {
        if (!view.contains(name)) {
            throw new IllegalStateException("File missing required section: " + name);
        }

        return view.get(name);
    }
}
