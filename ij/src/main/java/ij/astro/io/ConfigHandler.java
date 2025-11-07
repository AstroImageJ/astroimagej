package ij.astro.io;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import ij.IJ;
import ij.astro.logging.AIJLogger;

public class ConfigHandler {
    private ConfigHandler() {}

    public static List<Line> readOptions() {
        var p = configFilePath();
        ensureOverlayConfigExists(p);
        AIJLogger.log(p);

        if (!Files.exists(p)) {
            IJ.error("Config Editor", "Failed to find 'AstroImageJ.cfg'");
            return new ArrayList<>();
        }

        try (var lines = Files.lines(p)) {
            return lines.mapMulti((String l, Consumer<Line> c) -> {
                l = l.trim();

                if (l.isEmpty()) {
                    c.accept(new EmptyLine());
                    return;
                }

                if (l.startsWith("[") && l.endsWith("]")) {
                    c.accept(new Section(l.substring(1, l.length()-1)));
                } else {
                    c.accept(new Option(l));
                }
            }).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            IJ.error("Config Editor", "Failed to find 'AstroImageJ.cfg'");
            return new ArrayList<>();
        }
    }

    public static void modifyOptions(Consumer<List<Line>> consumer) {
        var lines = readOptions();
        consumer.accept(lines);
        writeOptions(lines);
    }

    public static void writeOptions(List<Line> lines) {
        var p = configFilePath();
        ensureOverlayConfigExists(p);

        try {
            Files.write(p, (Iterable<String>) () -> lines.stream().filter(Objects::nonNull).map(l -> {
                if (l instanceof Section s) {
                    return "[" + s.section() + "]";
                } else if (l instanceof Option o) {
                    return o.opt();
                } else if (l instanceof EmptyLine) {
                    return "";
                } else {
                    throw new IllegalStateException();
                }
            }).iterator());
        } catch (IOException e) {
            e.printStackTrace();
            IJ.error("Config Editor", "Failed to write 'AstroImageJ.cfg'");
        }
    }

    public static Option findOption(List<Line> lines, String section, String opt) {
        var inSection = false;
        for (Line line : lines) {
            if (line instanceof Section s) {
                inSection = s.section.equals(section);
            }

            if (inSection && line instanceof Option o) {
                if (o.opt().startsWith(opt)) {
                    return o;
                }
            }
        }

        return null;
    }

    public static String findValue(List<Line> lines, String section, String opt) {
        var o = findOption(lines, section, opt);

        if (o != null) {
            var v = o.getValue(opt);

            if (v == null) {
                return o.getValue(opt);
            }

            return v;
        }

        return null;
    }

    public static void setOption(List<Line> lines, String section, String key, String opt) {
        var o = findOption(lines, section, key);
        var i = -1;

        if (o != null) {
            i = lines.indexOf(o);
        }

        if (opt == null) {
            lines.remove(o);
            return;
        }

        if (i >= 0) {
            lines.set(i, new Option(opt));
        } else {
            var s = new Section(section);
            var si = lines.indexOf(s);

            if (si >= 0) {
                i = si + 1;
                lines.add(i, new Option(opt));
            } else {
                lines.add(s);
                lines.add(new Option(opt));
            }
        }
    }

    private static void ensureOverlayConfigExists(Path p) {
        try {
            // Ensure parent directories exist
            Path parent = p.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            // Ensure file exists
            if (Files.notExists(p)) {
                Files.createFile(p);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to ensure overlay config exists at: " + p, e);
        }
    }

    private static Path configFilePath() {
        // Use global config location, Why handles merging of the two configs
        if (IJ.isMacOSX()) {
            return Path.of(System.getProperty("user.home"), "Library", "Application Support", "AstroImageJ", "AstroImageJ_Overlay.cfg");
        } else if (IJ.isWindows()) {
            return Path.of(System.getenv("APPDATA"), "AstroImageJ", "AstroImageJ_Overlay.cfg");
        } else if (IJ.isLinux()) {
            return Path.of(System.getProperty("user.home"), ".local", "astroimagej", "AstroImageJ_Overlay.cfg");
        } else {
            // Modify app config directly
            try {
                return Path.of(ConfigHandler.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                        .getParent().resolve("AstroImageJ.cfg");
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public record Option(String opt) implements Line {

        /**
         * Returns the value of this option.
         * <p>
         * If {@code key} is not {@code null}, this method looks for either:
         * <ul>
         *   <li>{@code key=value} &ndash; matches if the option starts with {@code key=} and returns the part after the {@code =}</li>
         *   <li>{@code key<value>} &ndash; matches if the option starts with {@code key} and returns the remainder</li>
         * </ul>
         * If {@code key} is {@code null}, the method attempts to split the option on the first unquoted {@code =}
         * and returns the value part.
         *
         * @return the value associated with the key, or {@code null} if not matched
         */
        public String getValue() {
            return getValue(null);
        }

        /**
         * Returns the value of this option.
         * <p>
         * If {@code key} is not {@code null}, this method looks for either:
         * <ul>
         *   <li>{@code key=value} &ndash; matches if the option starts with {@code key=} and returns the part after the {@code =}</li>
         *   <li>{@code key<value>} &ndash; matches if the option starts with {@code key} and returns the remainder</li>
         * </ul>
         * If {@code key} is {@code null}, the method attempts to split the option on the first unquoted {@code =}
         * and returns the value part.
         *
         * @param key the key to match against, or {@code null} to extract value from any {@code key=value} form
         * @return the value associated with the key, or {@code null} if not matched
         */
        public String getValue(String key) {
            if (key != null) {
                int equalsIndex = indexOfUnquotedEquals(opt);
                if (equalsIndex >= 0) {
                    String foundKey = opt.substring(0, equalsIndex);
                    if (foundKey.equals(key)) {
                        return unquote(opt.substring(equalsIndex + 1));
                    } else {
                        return null;
                    }
                } else if (opt.startsWith(key)) {
                    return opt.substring(key.length());
                } else {
                    return null;
                }
            } else {
                int equalsIndex = indexOfUnquotedEquals(opt);
                if (equalsIndex >= 0) {
                    return unquote(opt.substring(equalsIndex + 1));
                } else {
                    return null;
                }
            }
        }

        private static int indexOfUnquotedEquals(String str) {
            boolean inQuotes = false;
            char quoteChar = 0;

            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                if (inQuotes) {
                    if (c == quoteChar) {
                        inQuotes = false;
                    } else if (c == '\\' && i + 1 < str.length()) {
                        i++; // skip escaped character
                    }
                } else {
                    if (c == '"' || c == '\'') {
                        inQuotes = true;
                        quoteChar = c;
                    } else if (c == '=') {
                        return i;
                    }
                }
            }

            return -1;
        }

        private static String unquote(String value) {
            if (value == null) {
                return null;
            }

            value = value.strip();
            if ((value.startsWith("\"") && value.endsWith("\"")) ||
                    (value.startsWith("'") && value.endsWith("'"))) {
                return value.substring(1, value.length() - 1);
            }

            return value;
        }
    }

    public record Section(String section) implements Line {}

    public record EmptyLine() implements Line {}

    public sealed interface Line permits Option, Section, EmptyLine {}
}
