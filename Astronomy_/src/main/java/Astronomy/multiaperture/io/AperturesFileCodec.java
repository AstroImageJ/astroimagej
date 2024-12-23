package Astronomy.multiaperture.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Stack;

public class AperturesFileCodec {
    public static ApFile readContents(String contents) {
        try {
            return Transformers.read(ApFile.class, readContents0(contents));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ApFile readFile(String filePath) {
        try {
            return Transformers.read(ApFile.class, read(filePath));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String write(ApFile apFile) {
        return writeSection(Transformers.write(ApFile.class, apFile));
    }

    private static Section readContents0(String contents) {
        Stack<Section> stack = new Stack<>();
        var root = new Section("root", true);
        stack.push(root);

        contents.lines().forEachOrdered(line -> {
            if (line == null || line.trim().isEmpty()) {
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
        });

        return root;
    }

    private static Section read(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            Stack<Section> stack = new Stack<>();
            var root = new Section("root", true);
            stack.push(root);

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue; // Skip blank lines

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

            return root;
        }
    }

    private static String writeSection(Section section) {
        var writer = new SectionWriter();

        writeSection(writer, section);

        return writer.getContent();
    }

    private static void writeSection(SectionWriter writer, Section section) {
        var sectionHeader = new StringBuilder(section.name);
        for (String parameter : section.getParameters()) {
            sectionHeader.append('\t').append(parameter);
        }

        if (section.isRoot()) {
            for (Section subSection : section.getSubSections()) {
                writeSection(writer, subSection);
            }
        } else {
            writer.writeLine(sectionHeader.toString());

            for (Section subSection : section.getSubSections()) {
                writer.enterSection();
                writeSection(writer, subSection);
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
}
