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
import ij.astro.util.UIHelper;

import javax.swing.*;
import java.awt.*;
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

    private PlotNameResolver() {
    }

    public static String resolvePlotTitle(MeasurementTable table) {
        try {
            return resolve(table, TITLE_MACRO.get());
        } catch (Exception e) {
            return TITLE_MACRO.get();
        }
    }

    public static String resolvePlotSubtitle(MeasurementTable table) {
        try {
            return resolve(table, SUBTITLE_MACRO.get());
        } catch (Exception e) {
            return SUBTITLE_MACRO.get();
        }
    }

    private static String resolve(MeasurementTable table, String pattern) {
        // Variable replace
        if (pattern.contains("$")) {
            if (table == null) {
                return "<ERROR NO TABLE>";
            }

            // Escape $ with nothing following it
            pattern = pattern.replaceAll("(\\$[^\\w{]+)", "\\\\\\$");

            var m = VARIABLE.matcher(pattern);
            return m.replaceAll(matchResult -> {
                var v = matchResult.group(1).substring(1); // trim preceding $

                // Unwrap quoted column
                //todo handle escaped quotes
                if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                    v = v.substring(1, v.length()-1);
                }

                if ("Label".equals(v)) {
                    return table.getLabel(0);
                }

                // Handle functions
                if (v.startsWith("{")) {
                    return parseFunction(v, table);
                }

                if (!table.columnExists(v)) {
                    return "<ERROR NO COL.: '%s'>".formatted(v);
                }

                return table.getStringValue(v, 0);
            });
        }

        return pattern;
    }

    private static String parseFunction(String function, MeasurementTable table) {
        try {
            var p = new JSONParser().parse(function);
            if (p instanceof JSONObject o) {
                return functionRunner(o, table);
            }
            return "<Parse went wrong>";
        } catch (ParseException e) {
            return "<Not a function: JSON parse error>";
        } catch (Exception e) {
            e.printStackTrace();
            return "<An error occurred running script match>";
        }
    }

    private static String functionRunner(JSONObject o, MeasurementTable table) {
        // Pref function
        if (o.get("pref") instanceof String key) {
            var mappedKey = keyResolver(key);
            return Prefs.get(mappedKey, "<Missing value for '%s' (%s)>".formatted(mappedKey, key));
        }

        // Header function
        if (o.get("hdr") instanceof String card) {
            var label = table.getLabel(0);
            var i = getImpForSlice(label);
            if (i != null) {
                var h = FitsJ.getHeader(i);
                int c;
                if (h != null && h.cards() != null && ((c = FitsJ.findCardWithKey(card, h)) > -1)) {
                    return FitsJ.getCardValue(h.cards()[c]).trim();
                }
                return "<Failed to find card with key '%s'>".formatted(card);
            }
            return "<Found no matching image for '%s'>".formatted(label);
        }

        // Regex function
        if (o.get("regex") instanceof String regex) {
            if (o.get("replace") instanceof String replace) {
                var m = SIMPLE_VARIABLE.matcher(replace);
                var l = table.getLabel(0);
                if (o.get("src") instanceof String src) {
                    if (table.columnExists(src)) {
                        l = table.getStringValue(src, 0);
                    } else {
                        return "<Invalid col. name for src: '%s'>".formatted(src);
                    }
                } else if (o.get("src") instanceof JSONObject s) {
                    l = functionRunner(s, table);
                }

                var matcher = Pattern.compile(regex).matcher(l);
                return m.replaceAll(matchResult -> {
                    matcher.reset();
                    var v = matchResult.group(1).substring(1).trim(); // trim preceding $

                    try {
                        var g = Integer.parseInt(v);
                        if (g < 0) {
                            return "<Invalid group index: '%s'. Must be > 0>".formatted(g);
                        }
                        if (matcher.find()) {
                            if (g <= matcher.groupCount()) {
                                return matcher.group(g);
                            }
                            return "<Invalid group index: '%s'>".formatted(g);
                        }
                        return "<Failed to match '%s'>".formatted(matcher.pattern().pattern());
                    } catch (NumberFormatException e) {
                        if (matcher.find()) {
                            try {
                                return matcher.group(v);
                            } catch (IllegalArgumentException ignored) {
                                return "<Invalid group name: '%s'>".formatted(v);
                            }
                        }
                        return "<Failed to match '%s'>".formatted(matcher.pattern().pattern());
                    }
                });
            }
            return "<Regex mode match failed, missing 'replace' text>";
        }

        // Split function
        if (o.get("split") instanceof String split) {
            var splitter = "_";
            if (o.get("splitter") instanceof String s) {
                splitter = s;
            }

            var l = table.getLabel(0);
            if (o.get("src") instanceof String src) {
                if (src.equals("title")) {
                    var i = getImpForSlice(l);
                    if (i != null) {
                        l = i.getTitle();
                        if (l.isEmpty()) {
                            return "<Stack title was empty>";
                        }
                    } else {
                        return "<Found no matching image for '%s'>".formatted(l);
                    }
                } else if (table.columnExists(src)) {
                    l = table.getStringValue(src, 0);
                } else {
                    return "<Invalid col. name for src: '%s'>".formatted(src);
                }
            } else if (o.get("src") instanceof JSONObject s) {
                l = functionRunner(s, table);
            }

            final var src = l;
            var s = src.split(splitter);
            return LABEL_VARIABLE.matcher(split).replaceAll(matchResult -> {
                var v = matchResult.group(1).substring(1).trim(); // trim preceding $

                try {
                    var g = Integer.parseInt(v) - 1;
                    if (g == -1) {
                        return src;
                    }
                    if (g > -1) {
                        if (g >= s.length) {
                            return "<Split group greater than possible: '%s'>".formatted(g);
                        }
                        return s[g];
                    } else {
                        return "<Split group must be greater than -1: '%s'>".formatted(g);
                    }
                } catch (NumberFormatException e) {
                    return "<Failed to get split match number: '%s'>".formatted(v);
                }
            });
        }

        return "<Failed to identify script mode>";
    }

    private static String keyResolver(String key) {
        return switch (key) {
            case "APLOADING" -> MultiAperture_.apLoading.getPropertyKey();
            case "APRADIUS" -> "aperture.lastradius";
            case "APSKYINNER" -> "aperture.lastrback1";
            case "APSKYOUTER" -> "aperture.lastrback2";
            case "APMODE" -> "multiaperture.apradius";
            case "APVARFWHM" -> "multiaperture.apfwhmfactor";
            case "APVARFLUXCUT" -> "multiaperture.automodefluxcutoff";
            default -> key;
        };
    }

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
        tp.setFocusable(false);
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
