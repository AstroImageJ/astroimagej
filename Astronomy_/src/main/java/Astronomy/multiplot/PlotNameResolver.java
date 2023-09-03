package Astronomy.multiplot;

import Astronomy.MultiAperture_;
import Astronomy.MultiPlot_;
import astroj.FitsJ;
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

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

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

        // Lable function
        // This is a special case of the split function
        if (o.get("lab") instanceof String lab) {
            var j = new JSONObject();
            j.put("split", lab);
            j.put("splitter", o.getOrDefault("splitter", "_"));
            return functionRunner(j, table);
        }

        // Title function
        // This is a special case of the split function
        if (o.get("title") instanceof String lab) {
            var j = new JSONObject();
            j.put("split", lab);
            j.put("splitter", o.getOrDefault("splitter", "_"));
            j.put("src", "title");
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
                    return new Pair.GenericPair<>(FitsJ.getCardValue(h.cards()[c]).trim(), false);
                }
                return new Pair.GenericPair<>("<Failed to find card with key '%s'>".formatted(card), true);
            }
            return new Pair.GenericPair<>("<Found no matching image for '%s'>".formatted(label), true);
        }

        // Regex function
        if (o.get("regex") instanceof String regex) {
            // The string that will be output, containing any group references
            if (o.get("replace") instanceof String replace) {
                var m = SIMPLE_VARIABLE.matcher(replace);

                // Find the src to pull the initial string to perform the regex match on
                var l = table.getLabel(0);
                if (o.get("src") instanceof String src) {
                    if (table.columnExists(src)) {
                        l = table.getStringValue(src, 0);
                    } else {
                        return new Pair.GenericPair<>("<Invalid col. name for src: '%s'>".formatted(src), true);
                    }
                } else if (o.get("src") instanceof JSONObject s) {
                    var f = functionRunner(s, table);
                    l = f.first();
                    lastError = true;
                }

                // The final output
                var matcher = Pattern.compile(regex).matcher(l);
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
            return new Pair.GenericPair<>("<Regex mode match failed, missing 'replace' text>", true);
        }

        // Split function
        if (o.get("split") instanceof String split) {
            var splitter = "_";
            if (o.get("splitter") instanceof String s) {
                splitter = s;
            }

            // Find the src to pull the initial string to perform the split with
            var l = table.getLabel(0);
            if (o.get("src") instanceof String src) {
                if (src.equals("title")) {
                    var i = getImpForSlice(l);
                    if (i != null) {
                        l = i.getTitle();
                        if (l.isEmpty()) {
                            return new Pair.GenericPair<>("<Stack title was empty>", true);
                        }
                    } else {
                        return new Pair.GenericPair<>("<Found no matching image for '%s'>".formatted(l), true);
                    }
                } else if (table.columnExists(src)) {
                    l = table.getStringValue(src, 0);
                } else {
                    return new Pair.GenericPair<>("<Invalid col. name for src: '%s'>".formatted(src), true);
                }
            } else if (o.get("src") instanceof JSONObject s) {
                var f = functionRunner(s, table);
                l = f.first();
                lastError = true;
            }

            // The final output
            final var src = l;
            var s = src.split(splitter);
            var errorState = new AtomicBoolean(lastError);
            return new Pair.GenericPair<>(LABEL_VARIABLE.matcher(split).replaceAll(matchResult -> {
                var v = matchResult.group(1).substring(1).trim(); // trim preceding $

                try {
                    var g = Integer.parseInt(v) - 1;
                    if (g == -1) {
                        return src;
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
        var f = new JFrame("Title Macro Help");
        var tp = new JTextPane();
        tp.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tp.replaceSelection(
                """
                        Title macros work to dynamically create plot title and subtitle based on available information.
                        The macro is stored separately from the final finished title, allowing you to switch between them to
                        make any necessary edits. Errors that occur will be displayed in the title. All names are case sensitive.
                                        
                        Macros consist of 3 parts that can be combined in any way:
                            - Normal text. This text will appear as-is when rendered.
                            - Variables, that start with $. These variables will be substituted by the the first value in
                                the column specified.
                            - Functions, that start with $. They take the form of a JSON object to perform various operation on
                                text.
                                        
                        Variables
                            They must begin with a $, followed by the column name. The column name may be wrapped in "",
                            which is required for columns whose name carries whitespace or non-standard characters.
                            Examples:
                                Table: Column AIRMASS, first value -1
                                Macro: Hello $AIRMASS
                                Output: Hello -1
                                
                                Table: Column Label, first value processed_altair_21.fits
                                Macro: Hello $Label
                                Output: Hello processed_altair_21.fits
                            
                        Functions
                            They must begin with $, and take the form of a JSON object. Several function are available.
                            
                            Functions:
                                Split. Splits text, by default the first value of the Label column and allows selective
                                inclusion of those parts.
                                    Indexing begins at 1, and counts from the left.
                                    By default splits on _, but optionally can specify any character sequence.
                                    Can optionally specify a "src" to pull the text from. "title" will fetch the stack title,\
                                     can be any column or another function.
                                    Example:
                                        Table: Column Label, first value processed_altair_21.fits
                                        Macro: Hello ${"split":"Reversed: $3 $2$1", "splitter":"_"} // Split entry is optional
                                        Output: Hello Reversed: 21.fits altairprocessed
                                        
                                        Table: Stack title = Altair 23/14/01
                                        Macro: ${"split":"Observed on $2", "splitter":" ", "src": "title"}
                                        Output: Observed on 23/14/01
                                Title. A special case of split the automatically specifies the src as "title".
                                Lab. A special case of split that automatically specifies the src as "Label".
                                Header. Extracts values from the image header of the first slice. Image must be open.
                                    Example:
                                        Header: CCDTEMP = 23.5
                                        Macro: Hello ${"hdr":"CCDTEMP"}C
                                        Output: Hello 23.5C
                                Preferences. Read values from the preferences file. Some keywords are provided to aid in value
                                    extraction.
                                    Keywords:
                                        - "APLOADING": The method in which MA loaded apertures
                                        - "APRADIUS": The last used aperture radius
                                        - "APSKYINNER": The last used inner sky radius
                                        - "APSKYOUTER": The last used outer sky radius
                                        - "APMODE": The method MA used to calculate ap. radius
                                        - "APVARFWHM": The FWHM value used by var. ap.
                                        - "APVARFLUXCUT": The flux cutoff used by var. ap.
                                        - "LASTMA": The last MA run's settings including radii
                                    Example:
                                        Macro: MA: ${"pref": "APMODE"}, Radius: ${"pref": "APRADIUS"}px
                                        Output: MA: FIXED, Radius: 15px
                                Regex. Allows applying a regex find and replace on text, including the output of other functions.
                                    It has 3 parts:
                                        - regex: the regex expression to match with
                                        - replace: the text that will be returned, including any group references
                                        - src: Optional. Can be a column name, in which case it will extract the first
                                            value from it, or a function, in which case it will run that function first.
                                    Examples:
                                        Header: CCDTEMP = 23.5
                                        Table: Column Label, first value processed_altair_21.fits
                                        Macro: Regex with ${"regex": "_(\\w+)_", "replace": "$1"} and
                                                ${"regex": "([0-9]+)", "replace": "$1C", "src":{"hdr":"CCDTEMP"}}
                                        Output: Regex with altair and 23C
                        """);
        var s = new JScrollPane(tp);
        s.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        f.add(s);
        f.pack();
        UIHelper.setCenteredOnScreen(f, MultiPlot_.mainFrame);
        f.setVisible(true);
    }
}
