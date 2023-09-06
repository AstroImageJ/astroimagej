package Astronomy.multiplot;

import Astronomy.MultiAperture_;
import Astronomy.MultiPlot_;
import astroj.FitsJ;
import astroj.HelpPanel;
import astroj.MeasurementTable;
import astroj.json.simple.JSONObject;
import astroj.json.simple.parser.JSONParser;
import astroj.json.simple.parser.ParseException;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.astro.io.prefs.Property;
import ij.astro.types.Pair;
import ij.astro.util.UIHelper;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class PlotNameResolver {
    public static final Property<String> TITLE_MACRO = new Property<>("", PlotNameResolver.class);
    public static final Property<String> SUBTITLE_MACRO = new Property<>("", PlotNameResolver.class);
    private static final Pattern SIMPLE_VARIABLE = Pattern.compile("(\\$\\S+)");
    private static final Pattern VARIABLE = Pattern.compile("(\\$(" + // Variables start with $
            // simple word
            "\\w+|" +
            // word in quotes
            "((?=[\"'])(?:\"[^\"\\\\]*(?:\\\\[\\s\\S][^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\[\\s\\S][^'\\\\]*)*'))|" +
            // json with some depth, https://stackoverflow.com/a/68188893/8753755 with subroutines replaced to depth ~3
            "(\\{(?:[^{}]|((\\{(?:[^{}]|((\\{(?:[^{}]|((\\{(?:[^{}]|((\\{(?:[^{}]|((\\{(?:[^{}])*\\})))*\\})))*\\})))*\\})))*\\})))*\\})" +
            "))");
    private static final Pattern LABEL_VARIABLE = Pattern.compile("(\\$[0-9]+)");
    private static Pair.GenericPair<String, Boolean> lastTitleState;
    private static Pair.GenericPair<String, Boolean> lastSubtitleState;

    private PlotNameResolver() {
    }

    public static Pair.GenericPair<String, Boolean> resolvePlotTitle(MeasurementTable table) {
        try {
            return (lastTitleState = resolve(table, TITLE_MACRO.get()));
        } catch (Exception e) {
            return new Pair.GenericPair<>(TITLE_MACRO.get(), true);
        }
    }

    public static Pair.GenericPair<String, Boolean> resolvePlotSubtitle(MeasurementTable table) {
        try {
            return (lastSubtitleState = resolve(table, SUBTITLE_MACRO.get()));
        } catch (Exception e) {
            return new Pair.GenericPair<>(SUBTITLE_MACRO.get(), true);
        }
    }

    public static Pair.GenericPair<String, Boolean> lastTitle() {
        if (lastTitleState == null) {
            return new Pair.GenericPair<>(TITLE_MACRO.get(), true);
        }

        return lastTitleState;
    }

    public static Pair.GenericPair<String, Boolean> lastSubtitle() {
        if (lastSubtitleState == null) {
            return new Pair.GenericPair<>(SUBTITLE_MACRO.get(), true);
        }

        return lastSubtitleState;
    }

    private static Pair.GenericPair<String, Boolean> resolve(MeasurementTable table, String pattern) {
        // Variable replace
        if (pattern.contains("$")) {
            if (table == null) {
                return new Pair.GenericPair<>("<ERROR NO TABLE>", true);
            }

            // Escape $ with nothing following it
            pattern = pattern.replaceAll("(\\$[^\\w{]+)", "\\\\\\$");

            // Perform the variable substitution
            var m = VARIABLE.matcher(pattern);
            var errorState = new AtomicBoolean(false);
            return new Pair.GenericPair<>(m.replaceAll(matchResult -> {
                var v = matchResult.group(1).substring(1); // trim preceding $

                // Unwrap quoted column
                //todo handle escaped quotes
                if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                    v = v.substring(1, v.length()-1);
                }

                // Label isn't a real column
                if ("Label".equals(v)) {
                    return table.getLabel(0);
                }

                // Handle functions
                if (v.startsWith("{")) {
                    var f = parseFunction(v, table);
                    errorState.compareAndExchange(false, f.second());
                    return f.first();
                }

                if (!table.columnExists(v)) {
                    return "<ERROR NO COL.: '%s'>".formatted(v);
                }

                return table.getStringValue(v, 0);
            }), errorState.get());
        }

        return new Pair.GenericPair<>(pattern, false);
    }

    private static Pair.GenericPair<String, Boolean> parseFunction(String function, MeasurementTable table) {
        try {
            var p = new JSONParser().parse(function);
            if (p instanceof JSONObject o) {
                return functionRunner(o, table);
            }
            return new Pair.GenericPair<>("<Parse went wrong>", true);
        } catch (ParseException e) {
            return new Pair.GenericPair<>("<Not a function: JSON parse error>", true);
        } catch (Exception e) {
            e.printStackTrace();
            return new Pair.GenericPair<>("<An error occurred running script match>", true);
        }
    }

    @SuppressWarnings("unchecked")
    private static Pair.GenericPair<String, Boolean> functionRunner(JSONObject o, MeasurementTable table) {
        var lastError = false;

        // Pref function
        if (o.get("pref") instanceof String key) {
            var mappedKey = keyResolver(key);
            if (Prefs.containsKey(mappedKey)) {
                return new Pair.GenericPair<>(Prefs.get(mappedKey, "<default>"), false);
            }
            return new Pair.GenericPair<>("<Missing value for '%s' (%s)>".formatted(mappedKey, key), true);
        }

        // Format function
        if (o.get("format") instanceof String format) {
            String input = null;
            if (o.get("input") instanceof JSONObject j) {
                var f = functionRunner(j, table);
                input = f.first();
                lastError = f.second();
            } else if (o.get("input") instanceof String s) {
                if (table.columnExists(s)) {
                    input = table.getStringValue(s, 0);
                } else {
                    return new Pair.GenericPair<>("<Invalid col. name for input: '%s'>".formatted(s), true);
                }
            }

            if (input == null) {
                return new Pair.GenericPair<>("<Was not provided input>", true);
            }

            //MessageFormat.format(key, input);
            try {
                return new Pair.GenericPair<>(new DecimalFormat(format).format(Double.parseDouble(input.trim())), lastError);
            } catch (NumberFormatException e) {
                return new Pair.GenericPair<>("<Failed to convert '%s' into a double>".formatted(input), true);
            } catch (IllegalArgumentException e) {
                return new Pair.GenericPair<>("<Invalid format '%s'>".formatted(format), true);
            }
        }

        // Label function
        // This is a special case of the split function
        if (o.get("label") instanceof String lab) {
            var j = new JSONObject();
            j.put("output", lab);
            j.put("split", o.getOrDefault("splitter", "_"));
            return functionRunner(j, table);
        }

        // Title function
        // This is a special case of the split function
        if (o.get("title") instanceof String lab) {
            var j = new JSONObject();
            j.put("output", lab);
            j.put("split", o.getOrDefault("splitter", "_"));
            j.put("input", "title");
            return functionRunner(j, table);
        }

        // Header function
        if (o.get("hdr") instanceof String card) {
            var label = table.getLabel(0);
            var i = getImpForSlice(label);
            if (i != null) {
                var h = FitsJ.getHeader(i);
                int c;
                if (h != null && h.cards() != null && ((c = FitsJ.findCardWithKey(card, h)) > -1)) {
                    var val = FitsJ.getCardValue(h.cards()[c]).trim();
                    // Trim ' from val
                    if (val.startsWith("'") && val.endsWith("'")) {
                        val = val.substring(1, val.length() - 1).trim();
                    }
                    return new Pair.GenericPair<>(val, false);
                }
                return new Pair.GenericPair<>("<Failed to find card with key '%s'>".formatted(card), true);
            }
            return new Pair.GenericPair<>("<Found no matching image for '%s'>".formatted(label), true);
        }

        // Regex function
        if (o.get("regex") instanceof String regex) {
            // The string that will be output, containing any group references
            if (o.get("output") instanceof String output) {
                var m = SIMPLE_VARIABLE.matcher(output);

                // Find the input to pull the initial string to perform the regex match on
                var l = table.getLabel(0);
                if (o.get("input") instanceof String input) {
                    if (table.columnExists(input)) {
                        l = table.getStringValue(input, 0);
                    } else {
                        return new Pair.GenericPair<>("<Invalid col. name for input: '%s'>".formatted(input), true);
                    }
                } else if (o.get("input") instanceof JSONObject s) {
                    var f = functionRunner(s, table);
                    l = f.first();
                    lastError = f.second();
                }

                // The final output
                Matcher matcher;
                try {
                    matcher = Pattern.compile(regex).matcher(l);
                } catch (PatternSyntaxException e) {
                    return new Pair.GenericPair<>("<Pattern incomplete: %s>".formatted(e.getMessage()), true);
                }
                var errorState = new AtomicBoolean(lastError);
                return new Pair.GenericPair<>(m.replaceAll(matchResult -> {
                    matcher.reset();
                    var v = matchResult.group(1).substring(1).trim(); // trim preceding $

                    try {
                        var g = Integer.parseInt(v);
                        if (g < 0) {
                            errorState.set(true);
                            return "<Invalid group index: '%s'. Must be > 0>".formatted(g);
                        }
                        if (matcher.find()) {
                            if (g <= matcher.groupCount()) {
                                return matcher.group(g);
                            }
                            errorState.set(true);
                            return "<Invalid group index: '%s'>".formatted(g);
                        }
                        return "<Failed to match '%s'>".formatted(matcher.pattern().pattern());
                    } catch (NumberFormatException e) {
                        if (matcher.find()) {
                            try {
                                return matcher.group(v);
                            } catch (IllegalArgumentException ignored) {
                                errorState.set(true);
                                return "<Invalid group name: '%s'>".formatted(v);
                            }
                        }
                        errorState.set(true);
                        return "<Failed to match '%s'>".formatted(matcher.pattern().pattern());
                    }
                }), errorState.get());
            }
            return new Pair.GenericPair<>("<Regex mode match failed, missing 'output' text>", true);
        }

        // Split function
        if (o.get("split") instanceof String splitter) {
            String output;
            if (o.get("output") instanceof String op) {
                output = op;
            } else {
                return new Pair.GenericPair<>("<Split failed, missing 'output' text>", true);
            }

            // Find the input to pull the initial string to perform the split with
            var l = table.getLabel(0);
            if (o.get("input") instanceof String input) {
                if (input.equals("title")) {
                    var i = getImpForSlice(l);
                    if (i != null) {
                        l = i.getTitle();
                        if (l.isEmpty()) {
                            return new Pair.GenericPair<>("<Stack title was empty>", true);
                        }
                    } else {
                        return new Pair.GenericPair<>("<Found no matching image for '%s'>".formatted(l), true);
                    }
                } else if (table.columnExists(input)) {
                    l = table.getStringValue(input, 0);
                } else {
                    return new Pair.GenericPair<>("<Invalid col. name for input: '%s'>".formatted(input), true);
                }
            } else if (o.get("input") instanceof JSONObject s) {
                var f = functionRunner(s, table);
                l = f.first();
                lastError = f.second();
            }

            // The final output
            final var input = l;
            var s = input.split(splitter);
            var errorState = new AtomicBoolean(lastError);
            return new Pair.GenericPair<>(LABEL_VARIABLE.matcher(output).replaceAll(matchResult -> {
                var v = matchResult.group(1).substring(1).trim(); // trim preceding $

                try {
                    var g = Integer.parseInt(v) - 1;
                    if (g == -1) {
                        return input;
                    }
                    if (g > -1) {
                        if (g >= s.length) {
                            errorState.set(true);
                            return "<Split group greater than possible: '%s'>".formatted(g);
                        }
                        return s[g];
                    } else {
                        errorState.set(true);
                        return "<Split group must be greater than -1: '%s'>".formatted(g);
                    }
                } catch (NumberFormatException e) {
                    errorState.set(true);
                    return "<Failed to get split match number: '%s'>".formatted(v);
                }
            }), errorState.get());
        }

        return new Pair.GenericPair<>("<Failed to identify script mode>", true);
    }

    /**
     * Allows for easier access to pref. values without the user needing to know the key.
     */
    private static String keyResolver(String key) {
        return switch (key) {
            case "APLOADING" -> MultiAperture_.apLoading.getPropertyKey();
            case "APRADIUS" -> "aperture.lastradius";
            case "APSKYINNER" -> "aperture.lastrback1";
            case "APSKYOUTER" -> "aperture.lastrback2";
            case "APMODE" -> "multiaperture.apradius";
            case "APVARFWHM" -> "multiaperture.apfwhmfactor";
            case "APVARFLUXCUT" -> "multiaperture.automodefluxcutoff";
            case "LASTMA" -> "multiaperture.lastrun";
            default -> key;
        };
    }

    /**
     * Find an open stack that contains a slice that matches the label.
     */
    private static ImagePlus getImpForSlice(String label) {
        var ids = WindowManager.getIDList();
        if (ids != null) {
            for (int id : ids) {
                var i = WindowManager.getImage(id);
                if (i != null) {
                    var s = i.getImageStack();
                    //var n = s.getShortSliceLabel(0, 500);
                    for (String sliceLabel : s.getSliceLabels()) {
                        if (sliceLabel == null) {
                            break;
                        }
                        if (label.equals(sliceLabel.split("\n")[0])) {
                            return i;
                        }
                    }
                }
            }
        }

        return null;
    }

    public static void showHelpWindow() {
        UIHelper.setCenteredOnScreen(new HelpPanel("help/plotMacroHelp.html", "Plot Macros"), MultiPlot_.mainFrame);
    }
}
