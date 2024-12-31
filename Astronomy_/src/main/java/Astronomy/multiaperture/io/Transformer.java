package Astronomy.multiaperture.io;

public interface Transformer<T, PARAM> {
    T load(PARAM parameter, Section section);

    Section write(PARAM parameter, T obj);

    default boolean hasRequiredSections(Section section, String... requiredSections) {
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

    default int readInt(String name, String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Could not convert parameter %s to int, received %s".formatted(name, s), e);
        }
    }

    default double readDouble(String name, String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Could not convert parameter %s to double, received %s".formatted(name, s), e);
        }
    }

    default boolean readBool(String name, String s) {
        try {
            return Boolean.parseBoolean(s);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Could not convert parameter %s to boolean, received %s".formatted(name, s), e);
        }
    }
}
