package ij.astro.gui;

public interface RadioEnum {
    default String optionText() {
        if (this instanceof Enum<?> e) {
            return e.name();
        }

        return "";
    }

    default String tooltip() {
        return null;
    }
}
