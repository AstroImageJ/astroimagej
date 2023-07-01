package ij.astro.gui.nstate;

public class TriStateCheckBox extends NStateButton<TriState> {

    public TriStateCheckBox() {
        this(TriState.DISABLED);
    }

    public TriStateCheckBox(TriState state) {
        super(state);
    }
}
