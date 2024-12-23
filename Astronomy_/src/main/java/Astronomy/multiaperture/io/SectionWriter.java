package Astronomy.multiaperture.io;

import java.util.Stack;

public class SectionWriter {
    private final StringBuilder builder;
    private final Stack<String> indentStack;
    private String currentIndent;

    public SectionWriter() {
        this.builder = new StringBuilder();
        this.indentStack = new Stack<>();
        this.indentStack.push(""); // Initial indent level
        this.currentIndent = "";
    }

    public void startSection(String sectionName) {
        builder.append(currentIndent).append(sectionName).append("\n");
        indentStack.push(currentIndent + "\t");
        currentIndent = indentStack.peek();
    }

    public void enterSection() {
        builder.append(currentIndent);
        indentStack.push(currentIndent + "\t");
        currentIndent = indentStack.peek();
    }

    public void endSection() {
        if (indentStack.size() > 1) {
            indentStack.pop();
            currentIndent = indentStack.peek();
        } else {
            throw new IllegalStateException("Cannot end a section that hasn't been started.");
        }
    }

    public void writeLine(String line) {
        builder.append(currentIndent).append(line).append("\n");
    }

    public String getContent() {
        return builder.toString();
    }
}