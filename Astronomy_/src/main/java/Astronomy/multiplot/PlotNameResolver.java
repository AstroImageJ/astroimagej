package Astronomy.multiplot;

import astroj.FitsJ;
import astroj.MeasurementTable;
import astroj.json.simple.JSONObject;
import astroj.json.simple.parser.JSONParser;
import astroj.json.simple.parser.ParseException;
import ij.WindowManager;
import ij.astro.io.prefs.Property;

import java.util.regex.Pattern;

public class PlotNameResolver {
    public static final Property<String> TITLE_MACRO = new Property<>("", PlotNameResolver.class);
    public static final Property<String> SUBTITLE_MACRO = new Property<>("", PlotNameResolver.class);
    private static final Pattern SIMPLE_VARIABLE = Pattern.compile("(\\$\\w+)");
    private static final Pattern VARIABLE = Pattern.compile("(\\$(" + // Variables start with $
            // simple word
            "\\w+|" +
            // word in quotes
            "((?=[\"'])(?:\"[^\"\\\\]*(?:\\\\[\\s\\S][^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\[\\s\\S][^'\\\\]*)*'))|" +
            // json with some depth, https://stackoverflow.com/a/68188893/8753755 with subroutines replaced to depth ~3
            "(\\{(?:[^{}]|(\\{(?:[^{}]|\\{(?:[^{}]|\\{(?:[^{}])*\\})*\\}))*\\})*\\})" +
            "))");

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

                if ("Labels".equals(v)) {
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
                var regex = o.get("regex");
                if (regex instanceof String r) {
                    r = r.replaceAll("\\\\", "\\\\\\\\");
                    var src = o.get("src");
                    if (src instanceof String s) {
                        var m = SIMPLE_VARIABLE.matcher(s);
                        //todo some special chars like \w don't work?
                        var matcher = Pattern.compile(r).matcher(table.getLabel(0));//todo allow specifying src in the json?
                        return m.replaceAll(matchResult -> {
                            matcher.reset();
                            var v = matchResult.group(1).substring(1).trim(); // trim preceding $

                            try {
                                var g = Integer.parseInt(v);
                                if (matcher.find()) {
                                    return matcher.group(g);//todo handle missing group
                                }
                                return "<Failed to match '%s'>".formatted(matcher.pattern().pattern());
                            } catch (NumberFormatException e) {
                                if (matcher.find()) {
                                    return matcher.group(v);
                                }
                                return "<Failed to match '%s'>".formatted(matcher.pattern().pattern());
                            }
                        });
                    }
                    return "<Regex mode match failed, missing 'src' text>";
                }

                var header = o.get("hdr");
                if (header instanceof String hdr) {
                    var l = table.getLabel(0);
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
                                    if (l.equals(sliceLabel.split("\n")[0])) {
                                        var h = FitsJ.getHeader(i);
                                        int c;
                                        if (h != null && h.cards() != null && ((c = FitsJ.findCardWithKey(hdr, h)) > -1)) {
                                            return FitsJ.getCardValue(h.cards()[c]).trim();
                                        }
                                        return "<Failed to find card with key '%s'>".formatted(hdr);
                                    }
                                }
                            }
                        }
                        return "<Found no matching image for '%s'>".formatted(l);
                    } else {
                        return "<No open images>";
                    }
                }

                var lab = o.get("lab");
                if (lab instanceof String l) {
                    var s = table.getLabel(0).split("_");
                    return Pattern.compile("(f[0-9]+)").matcher(l).replaceAll(matchResult -> {
                        var v = matchResult.group(1).substring(1).trim(); // trim preceding f

                        try {
                            var g = Integer.parseInt(v);
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
            }

            return "<Failed to identify script mode>";
        } catch (ParseException e) {
            e.printStackTrace();//todo don't log this
            return "<JSON Parse Error>";
        } catch (Exception e) {
            e.printStackTrace();
            return "<An error occurred running script match>";
        }
    }
}
