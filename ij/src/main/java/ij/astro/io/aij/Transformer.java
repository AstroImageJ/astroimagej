package ij.astro.io.aij;

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
}
