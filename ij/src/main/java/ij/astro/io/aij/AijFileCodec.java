package ij.astro.io.aij;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public abstract class AijFileCodec {
    private final Map<Class<?>, Transformer<?, ?>> transformerMap = new HashMap<>();

    public AijFileCodec() {
        initialize();
    }

    public static Section readToSection(String contents) {
        Stack<Section> stack = new Stack<>();
        var root = new Section("root", true);
        stack.push(root);

        contents.lines().forEachOrdered(line -> readLine(stack, line));

        return root;
    }

    public static Section read(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            Stack<Section> stack = new Stack<>();
            var root = new Section("root", true);
            stack.push(root);

            String line;
            while ((line = reader.readLine()) != null) {
                readLine(stack, line);
            }

            return root;
        }
    }

    private static void readLine(Stack<Section> stack, String line) {
        if (line == null || line.trim().isEmpty()) {
            return;
        }

        // Skip Commented Lines
        if (line.startsWith("#")) {
            return;
        }

        int indentation = countLeadingTabs(line);
        String content = line.trim();

        while (stack.size() > indentation + 1) {
            stack.pop();
        }

        Section parent = stack.peek();

        if (content.contains("\t")) {
            var cs = content.split("\t");
            var newSection = new Section(cs[0]);
            newSection.setParameters(Arrays.asList(Arrays.copyOfRange(cs, 1, cs.length)));
            parent.addSubsection(newSection);
            stack.push(newSection);
        } else {
            var newSection = new Section(content);
            parent.addSubsection(newSection);
            stack.push(newSection);
        }
    }

    public static String write(Section section) {
        var writer = new SectionWriter();

        write(writer, section);

        return writer.getContent();
    }

    private static void write(SectionWriter writer, Section section) {
        var sectionHeader = new StringBuilder(section.name());
        for (String parameter : section.getParameters()) {
            sectionHeader.append('\t').append(parameter);
        }

        if (section.isRoot()) {
            for (Section subSection : section.getSubSections()) {
                write(writer, subSection);
            }
        } else {
            writer.writeLine(sectionHeader.toString());

            for (Section subSection : section.getSubSections()) {
                writer.enterSection();
                write(writer, subSection);
                writer.endSection();
            }
        }
    }

    private static int countLeadingTabs(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == '\t') {
            count++;
        }
        return count;
    }

    public abstract void initialize();

    public <T, P> void registerTransformer(Class<T> clazz, Transformer<T, P> transformer) {
        transformerMap.put(clazz, transformer);
    }

    public <T> T read(Class<T> clazz, Section section) {
        return read(clazz, section, (Void) null);
    }

    public <T, P> T read(Class<T> clazz, Section section, P parameters) {
        var transformer = transformerMap.get(clazz);

        if (transformer == null) {
            throw new IllegalStateException("Could not find transformer for " + clazz.getName());
        }

        try {
            return ((Transformer<T, P>) transformer).load(parameters, section);
        } catch (Exception e) {
            throw new RuntimeException("Error reading section: " + section, e);
        }
    }

    public <T> Section write(Class<T> clazz, T obj) {
        return write(clazz, obj, (Void) null);
    }

    public <T, P> Section write(Class<T> clazz, T obj, P parameters) {
        var transformer = (Transformer<T, P>) transformerMap.get(clazz);

        if (transformer == null) {
            throw new IllegalStateException("Could not find transformer for " + clazz.getName());
        }

        return transformer.write(parameters, obj);
    }
}
