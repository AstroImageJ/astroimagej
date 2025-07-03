package ij.astro.stack;

import ij.VirtualStack;

import java.awt.image.ColorModel;

public class OffHeapVirtualStack extends VirtualStack {
    public OffHeapVirtualStack() {
    }

    public OffHeapVirtualStack(int width, int height) {
        super(width, height);
    }

    public OffHeapVirtualStack(int width, int height, ColorModel cm, String path) {
        super(width, height, cm, path);
    }

    public OffHeapVirtualStack(int width, int height, int slices) {
        super(width, height, slices);
    }

    public OffHeapVirtualStack(int width, int height, int slices, String options) {
        super(width, height, slices, options);
    }


}
