package ij.astro.util;

import ij.IJ;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.File;

public class AIJFileChooser extends JFileChooser {
    private Container referenceWindow = IJ.getInstance();

    public AIJFileChooser() {
    }

    public AIJFileChooser(String currentDirectoryPath) {
        super(currentDirectoryPath);
    }

    public AIJFileChooser(File currentDirectory) {
        super(currentDirectory);
    }

    public AIJFileChooser(FileSystemView fsv) {
        super(fsv);
    }

    public AIJFileChooser(File currentDirectory, FileSystemView fsv) {
        super(currentDirectory, fsv);
    }

    public AIJFileChooser(String currentDirectoryPath, FileSystemView fsv) {
        super(currentDirectoryPath, fsv);
    }

    @Override
    protected JDialog createDialog(Component parent) throws HeadlessException {
        var dialog =  super.createDialog(parent);
        if (referenceWindow != null && parent == null) {
            UIHelper.setCenteredOnScreen(dialog, referenceWindow);
        }
        return dialog;
    }

    public Container getReferenceWindow() {
        return referenceWindow;
    }

    public void setReferenceWindow(Container referenceWindow) {
        this.referenceWindow = referenceWindow;
    }
}
