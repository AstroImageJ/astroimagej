package Astronomy;

import ij.plugin.PlugIn;
import util.JreUpdater;

public class JreUpdatePlugin implements PlugIn {
    @Override
    public void run(String arg) {
        JreUpdater.updateJre();
    }
}
