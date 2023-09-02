package Astronomy.multiplot;

import astroj.FitsJ;
import astroj.MeasurementTable;
import astroj.json.simple.JSONObject;
import astroj.json.simple.parser.JSONParser;
import astroj.json.simple.parser.ParseException;
import ij.ImagePlus;
import ij.WindowManager;
import ij.astro.io.prefs.Property;

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
        return resolve(table, TITLE_MACRO.get());
    }

    public static String resolvePlotSubtitle(MeasurementTable table) {
        return resolve(table, SUBTITLE_MACRO.get());
    }

    private static String resolve(MeasurementTable table, String pattern) {
        // Variable replace
        if (pattern.contains("$")) {
            if (table == null) {
                return "<ERROR NO TABLE>";
            }

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
            e.printStackTrace();//todo don't log this
            return "<JSON Parse Error>";
        } catch (Exception e) {
            e.printStackTrace();
            return "<An error occurred running script match>";
        }
    }

    private static String functionRunner(JSONObject o, MeasurementTable table) {
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

        // Label function
        if (o.get("lab") instanceof String lab) {
            var split = "_";
            if (o.get("split") instanceof String s) {
                split = s;
            }
            var s = table.getLabel(0).split(split);
            return LABEL_VARIABLE.matcher(lab).replaceAll(matchResult -> {
                var v = matchResult.group(1).substring(1).trim(); // trim preceding $

                try {
                    var g = Integer.parseInt(v) - 1;
                    if (g > -1) {
                        if (g >= s.length) {
                            return "<Label group greater than possible: '%s'>".formatted(g);
                        }
                        return s[g];
                    } else {
                        return "<Label group must be greater than 0: '%s'>".formatted(g);
                    }
                } catch (NumberFormatException e) {
                    return "<Failed to get label match number: '%s'>".formatted(v);
                }
            });
        }

        // Title match
        if (o.get("title") instanceof String title) {
            var split = "_";
            if (o.get("split") instanceof String s) {
                split = s;
            }

            var label = table.getLabel(0);
            var i = getImpForSlice(label);
            if (i != null) {
                var t = i.getTitle();
                if (!t.isEmpty()) {
                    var s = t.split(split);
                    return LABEL_VARIABLE.matcher(title).replaceAll(matchResult -> {
                        var v = matchResult.group(1).substring(1).trim(); // trim preceding $

                        try {
                            var g = Integer.parseInt(v) - 1;
                            if (g > -1) {
                                if (g >= s.length) {
                                    return "<Title group greater than possible: '%s'>".formatted(g);
                                }
                                return s[g];
                            } else {
                                return "<Title group must be greater than 0: '%s'>".formatted(g);
                            }
                        } catch (NumberFormatException e) {
                            return "<Failed to get title match number: '%s'>".formatted(v);
                        }
                    });
                }
                return "<Stack title was empty>";
            }
            return "<Found no matching image for '%s'>".formatted(label);
        }

        return "<Failed to identify script mode>";
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
}
