package Astronomy.multiplot;

import Astronomy.MultiAperture_;
import Astronomy.MultiPlot_;
import astroj.FitsJ;
import astroj.HelpPanel;
import astroj.MeasurementTable;
import flanagan.analysis.Stat;
import ij.ImagePlus;
import ij.Prefs;
import ij.VirtualStack;
import ij.WindowManager;
import ij.astro.io.prefs.Property;
import ij.astro.types.Pair;
import ij.astro.util.UIHelper;

import java.text.DecimalFormat;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class PlotNameResolver {
    public static final Property<String> TITLE_MACRO = new Property<>("", PlotNameResolver.class);
    public static final Property<String> SUBTITLE_MACRO = new Property<>("", PlotNameResolver.class);
    private static final Pattern OPERATOR = Pattern.compile("^(?<!\\\\)@");
    private static final Pattern TOKENIZER =
            Pattern.compile("((?=[\"'])(?:\"[^\"\\\\]*(?:\\\\[\\s\\S][^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\[\\s\\S][^'\\\\]*)*')|\\S+)");
    private static final Pattern SIMPLE_VARIABLE = Pattern.compile("(\\$\\S+)");
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
        var matcher = TOKENIZER.matcher(pattern);
        var tokens = matcher.results().map(MatchResult::group).toArray(String[]::new);
        var whitespace = TOKENIZER.splitAsStream(pattern).toArray(String[]::new);

        var stack = new Stack<String>();
        var hasErrored = false;
        var indexLastError = 0;
        for (int i = tokens.length - 1; i >= 0; i--) {
            if (i+1 < whitespace.length) {
                stack.push(whitespace[i+1]);
            }
            stack.push(tokens[i]);
            if (OPERATOR.matcher(tokens[i]).find()) {
                // Handle @ separated by whitespace
                if (tokens[i].equals("@")) {
                    indexLastError--;
                    stack.pop();
                    if (!stack.empty()) {
                        stack.pop();
                    }
                }

                var e = parseAndEvaluate(table, stack, indexLastError - i);
                if (e.first()) {
                    indexLastError = i - e.second();
                }
                if (!hasErrored) {
                    hasErrored = e.first();
                }
            }
        }

        var b = new StringBuilder();
        while (!stack.empty()) {
            var o = stack.pop();
            b.append(o);
        }

        return new Pair.GenericPair<>(b.toString(), hasErrored);
    }

    private static Pair.GenericPair<Boolean, Integer> parseAndEvaluate(MeasurementTable table, Stack<String> stack, int errorDelta) {
        if (stack.empty()) {
            stack.push("<Missing function name>");
            return new Pair.GenericPair<>(true, 0);
        }

        // Only peek, keep function name in case the input is errored,
        // in which case we don't want to parse it
        var func = stack.peek();

        // Handle case where @ was part of function token
        if (func.startsWith("@")) {
            func = func.substring(1);
        }

        var errorState = new AtomicBoolean(false);
        var expectedParams = switch (func) {
            case "header", "hdr", "h" -> evaluate(errorDelta, errorState, func, stack, new String[]{"key"}, ps -> {
                var card = ps[0];
                var i = getImpForSlice(table);
                if (i != null) {
                    var h = FitsJ.getHeader(i);
                    int c;
                    if (h != null && h.cards() != null && ((c = FitsJ.findCardWithKey(card, h)) > -1)) {
                        var val = FitsJ.getCardValue(h.cards()[c]).trim();
                        // Trim ' from val
                        if (val.startsWith("'") && val.endsWith("'")) {
                            val = val.substring(1, val.length() - 1).trim();
                        }
                        return val;
                    }
                    errorState.set(true);
                    return "<Failed to find card with key '%s'>".formatted(card);
                }
                errorState.set(true);
                return "<Found no matching image for '%s'>".formatted(table.getLabel(0));
            });
            case "comment", "cmt", "c" -> evaluate(errorDelta, errorState, func, stack, new String[]{"key"}, ps -> {
                var card = ps[0];
                var i = getImpForSlice(table);
                if (i != null) {
                    var h = FitsJ.getHeader(i);
                    int c;
                    if (h != null && h.cards() != null && ((c = FitsJ.findCardWithKey(card, h)) > -1)) {
                        var val = FitsJ.getCardComment(h.cards()[c]).trim();
                        // Trim ' from val
                        if (val.startsWith("'") && val.endsWith("'")) {
                            val = val.substring(1, val.length() - 1).trim();
                        }
                        return val;
                    }
                    errorState.set(true);
                    return "<Failed to find card with key '%s'>".formatted(card);
                }
                errorState.set(true);
                return "<Found no matching image for '%s'>".formatted(table.getLabel(0));
            });
            case "title", "ttl" -> evaluate(errorDelta, errorState, func, stack, new String[0], ps -> {
                var i = getImpForSlice(table);
                if (i != null) {
                    var t = i.getTitle();
                    if (t.isEmpty()) {
                        errorState.set(true);
                        return "<Stack title was empty>";
                    }
                    return t;
                } else {
                    errorState.set(true);
                    return "<Found no matching image for '%s'>".formatted(table.getLabel(0));
                }
            });
            case "pref", "prf", "p" -> evaluate(errorDelta, errorState, func, stack, new String[]{"key"}, ps -> {
                var mappedKey = keyResolver(ps[0]);
                if (Prefs.containsKey(mappedKey)) {
                    return Prefs.get(mappedKey, "<default>");
                }
                errorState.set(true);
                return "<Missing value for '%s' (%s)>".formatted(mappedKey, ps[0]);
            });
            case "table", "tbl", "t" -> evaluate(errorDelta, errorState, func, stack, new String[]{"col", "row"}, ps -> {
                if (!table.columnExists(ps[0]) && !"Label".equals(ps[0])) {
                    errorState.set(true);
                    return "<Invalid col. name for input: '%s'>".formatted(ps[0]);
                }

                // Special processors
                if (!"Label".equals(ps[0])) {
                    switch (ps[1]) {
                        case "$AVG" -> {
                            var c = table.getColumn(ps[0]);
                            return String.valueOf(Stat.mean(c));
                        }
                        case "$MIN" -> {
                            var c = table.getColumn(ps[0]);
                            return String.valueOf(new Stat(c).minimum());
                        }
                        case "$MAX" -> {
                            var c = table.getColumn(ps[0]);
                            return String.valueOf(new Stat(c).maximum());
                        }
                        case "$MED" -> {
                            var c = table.getColumn(ps[0]);
                            return String.valueOf(Stat.median(c));
                        }
                        default -> {}
                    }
                }

                try {
                    var row = switch (ps[1]) {
                        case "$F" -> 0;
                        case "$L" -> table.size() - 1;
                        default -> Integer.parseInt(ps[1]) - 1;
                    };

                    if (row < 0 || row >= table.size()) {
                        errorState.set(true);
                        return "<Row index out of bounds: %s>".formatted(row+1);
                    }

                    if ("Label".equals(ps[0])) {
                        return table.getLabel(row);
                    }

                    return String.valueOf(table.getValue(ps[0], row));
                } catch (Exception e) {
                    errorState.set(true);
                    return "<Failed to parse row: %s>".formatted(ps[1]);
                }
            });
            case "split", "spt", "s" -> evaluate(errorDelta, errorState, func, stack, new String[]{"splitter", "out", "in"}, ps -> {
                // The final output
                final var input = ps[2];
                var s = input.split(ps[0]);
                return LABEL_VARIABLE.matcher(ps[1]).replaceAll(matchResult -> {
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
                });
            });
            case "regex", "rgx", "r" -> evaluate(errorDelta, errorState, func, stack, new String[]{"exp", "out", "in"}, ps -> {
                var regex = ps[0];
                var m = SIMPLE_VARIABLE.matcher(ps[1]);
                // The final output
                Matcher matcher;
                try {
                    matcher = Pattern.compile(regex).matcher(ps[2]);
                } catch (PatternSyntaxException e) {
                    errorState.set(true);
                    return "<Pattern incomplete: %s>".formatted(e.getMessage());
                }

                return m.replaceAll(matchResult -> {
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
                });
            });
            case "format", "fmt", "f" -> evaluate(errorDelta, errorState, func, stack, new String[]{"exp", "in"}, ps -> {
                try {
                    return new DecimalFormat(ps[0]).format(Double.parseDouble(ps[1].trim()));
                } catch (NumberFormatException e) {
                    errorState.set(true);
                    return "<Failed to convert '%s' into a double>".formatted(ps[1]);
                } catch (IllegalArgumentException e) {
                    errorState.set(true);
                    return "<Invalid format '%s'>".formatted(ps[0]);
                }
            });
            default -> {
                errorState.set(true);
                stack.pop(); // Function name invalid, replace with error message
                stack.push("<Unknown function: %s>".formatted(func));
                yield 0;
            }
        };

        return new Pair.GenericPair<>(errorState.get(), expectedParams);
    }

    private static int evaluate(int errorDelta, AtomicBoolean errorState, String fName, Stack<String> stack,
                                String[] paramNames, Function<String[], String> function) {
        // Don't evaluate function, the input is an error message
        if (errorDelta < paramNames.length && errorDelta > 0) {
            return paramNames.length;
        }

        // Remove function name
        stack.pop();

        var e = extractParams(fName, stack, paramNames);
        if (e.missingParam) {
            stack.push(e.msg);
        } else {
            stack.push(function.apply(e.params));
        }

        errorState.compareAndExchange(false, e.missingParam);

        return paramNames.length;
    }

    private static Extraction extractParams(String function, Stack<String> stack, String[] paramNames) {
        var params = new String[paramNames.length];

        StringBuilder missingParams = new StringBuilder();
        for (int p = 0; p < params.length; p++) {
            if (!stack.empty()) {
                stack.pop(); // Remove separating whitespace
            }
            if (!stack.empty()) {
                var o = stack.pop();
                // Unwrap quoted column
                //todo handle escaped quotes
                if ((o.startsWith("\"") && o.endsWith("\"")) || (o.startsWith("'") && o.endsWith("'"))) {
                    o = o.substring(1, o.length()-1);
                }
                params[p] = o;
            } else {
                missingParams.append(missingParams.isEmpty() ? "" : ", ").append(paramNames[p]);
            }
        }

        if (!missingParams.isEmpty()) {
            return new Extraction(null, true, "<Missing parameter(s) for %s: %s>".formatted(function, missingParams.toString()));
        }

        return new Extraction(params, false, null);
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
    private static ImagePlus getImpForSlice(MeasurementTable table) {
        var ids = WindowManager.getIDList();
        var label = table.getLabel(0).split("\n")[0];
        if (ids != null) {
            for (int id : ids) {
                var i = WindowManager.getImage(id);
                if (i != null) {
                    var s = i.getImageStack();
                    //var n = s.getShortSliceLabel(0, 500);
                    var ls = s.getSliceLabels();
                    if (ls != null) {
                        for (String sliceLabel : s.getSliceLabels()) {
                            if (sliceLabel == null) {
                                break;
                            }
                            if (label.equals(sliceLabel.split("\n")[0])) {
                                return i;
                            }
                        }
                    } else if (s instanceof VirtualStack virtualStack) {
                        var l = virtualStack.getSliceLabel(i.getCurrentSlice());
                        if (l != null) {
                            l = l.split("\n")[0];
                        }
                        for (int row = 0; row < table.size(); row++) {
                            var sliceLabel = table.getLabel(0);
                            if (sliceLabel == null) {
                                continue;
                            }
                            if (table.getLabel(row).split("\n")[0].equals(l)) {
                                return i;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    public static void showHelpWindow() {
        UIHelper.setCenteredOnScreen(new HelpPanel("help/plotMacroHelp.html", "Programmable Plot Titles"), MultiPlot_.mainFrame);
    }

    record Extraction(String[] params, boolean missingParam, String msg) {}
}
