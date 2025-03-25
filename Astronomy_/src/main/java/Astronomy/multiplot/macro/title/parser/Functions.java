package Astronomy.multiplot.macro.title.parser;

import Astronomy.MultiAperture_;
import astroj.FitsJ;
import astroj.JulianDate;
import astroj.MeasurementTable;
import ij.IJ;
import ij.Prefs;
import ij.util.ArrayUtil;
import org.hipparchus.special.Gamma;

import java.text.DecimalFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

enum Functions {
    // Data fetch options
    HEADER(Functions::header, new String[]{"key", "slice"}, "header", "hdr", "h"),
    COMMENT(Functions::comment, new String[]{"key", "slice"}, "comment", "cmt", "c"),
    PREFERENCE(Functions::preference, new String[]{"key"}, "pref", "prf", "p"),
    DATETIME_NOW(Functions::datetimeNow, new String[]{"zoneId", "type"}, "datetimenow", "dtn"),
    TITLE(Functions::title, new String[0], "title", "ttl"),
    TABLE(Functions::table, new String[]{"column", "row"}, "table", "tbl", "t"),
    // Data processing
    REGEX(Functions::regex, new String[]{"regex", "out", "in"}, "regex", "rgx", "r"),
    SPLIT(Functions::split, new String[]{"splitter", "out", "in"}, "split", "spt", "s"),
    FORMAT(Functions::format, new String[]{"format expression", "in"}, "format", "fmt", "f"),
    DATETIME_FORMAT(Functions::datetimeFormat,
            new String[]{"inFormat", "inLocale", "outFormat", "outLocale", "datetime"}, "datetimeformat", "dtf"),
    BINARY_OPERATOR(Functions::binaryMathOperator, new String[]{"operator", "a", "b"}, "math2", "m2", "m"),
    UNARY_OPERATOR(Functions::unaryMathOperator, new String[]{"operator", "a"}, "math1", "m1"),
    ;

    final String[] parameters;
    public final List<String> functionNames;
    private final BiFunction<ResolverContext, String[], FunctionReturn> function;
    public static final List<String> EXAMPLE_FUNCTION_NAMES = new ArrayList<>();
    private static final Pattern SIMPLE_VARIABLE = Pattern.compile("((?<!\\\\)\\$\\S+)");
    private static final Pattern LABEL_VARIABLE = Pattern.compile("((?<!\\\\)\\$[0-9]+)");

    Functions(BiFunction<ResolverContext, String[], FunctionReturn> function, String[] parameters, String... functionNames) {
        this.function = function;
        this.parameters = parameters;
        this.functionNames = Collections.unmodifiableList(Arrays.asList(functionNames));
        // Make sure paramNames array is entered correctly
        if (IJ.isAijDev()) {
            for (String paramName : parameters) {
                if (paramName.contains(",")) {
                    throw new IllegalArgumentException("parameters were not an array");
                }
            }
        }
    }

    static {
        for (Functions value : values()) {
            EXAMPLE_FUNCTION_NAMES.add(value.functionNames.get(0));
        }
    }

    public int paramCount() {
        return parameters.length;
    }

    public static Functions getFunction(String string) {
        // Remove @
        string = string.replaceAll("@ ?", "");
        for (Functions function : values()) {
            if (function.functionNames.contains(string)) {
                return function;
            }
        }

        return null;
    }

    // Data fetch functions
    private static FunctionReturn header(ResolverContext ctx, String... ps) {
        return processHeaderCard(FitsJ::getCardValue, ctx, ps);
    }

    private static FunctionReturn comment(ResolverContext ctx, String... ps) {
        return processHeaderCard(FitsJ::getCardComment, ctx, ps);
    }

    private static FunctionReturn processHeaderCard(Function<String, String> processor, ResolverContext ctx, String... ps) {
        var card = ps[0];
        var sliceS = ps[1];
        int slice;
        switch (sliceS) {
            case "_" -> slice = -1;
            case "F" -> slice = 1;
            case "L" -> slice = ctx.table.size();
            default -> {
                try {
                    slice = Integer.parseInt(sliceS);
                } catch (Exception e) {
                    return FunctionReturn.error("<Found not parse slice number '%s'>".formatted(sliceS));
                }
            }
        }
        if (ctx.getImp() != null) {
            var h = ctx.getHeader(slice);
            if (h != null) {
                int c;
                if (h.cards() != null && (c = FitsJ.findCardWithKey(card, h)) > -1) {
                    var val = processor.apply(h.cards()[c]).trim();
                    // Trim ' from val
                    if (val.startsWith("'") && val.endsWith("'")) {
                        val = val.substring(1, val.length() - 1).trim();
                    }
                    return new FunctionReturn(val);
                }
                return FunctionReturn.error("<Failed to find card with key '%s'>".formatted(card));
            }
            return FunctionReturn.error("<Found no matching header for slice '%s'>".formatted(sliceS));
        }
        return FunctionReturn.error("<Found no matching image with filename matching a label in the table>");
    }

    private static FunctionReturn preference(ResolverContext ctx, String... ps) {
        var mappedKey = keyResolver(ps[0]);
        if (Prefs.containsKey(mappedKey)) {
            return new FunctionReturn(Prefs.get(mappedKey, "<default>"));
        }
        return FunctionReturn.error("<Missing value for '%s' (%s)>".formatted(mappedKey, ps[0]));
    }

    private static FunctionReturn title(ResolverContext ctx, String... ps) {
        var i = ctx.getImp();
        if (i != null) {
            var title = i.getTitle();
            if (title.isEmpty()) {
                return FunctionReturn.error("<Stack title was empty>");
            }
            return new FunctionReturn(title);
        } else {
            var label = "No Label";
            if (ctx.table.size() > 0) {
                label = ctx.table.getLabel(0);
            }
            return FunctionReturn.error("<Found no matching image for '%s'>".formatted(label));
        }
    }

    private static FunctionReturn table(ResolverContext ctx, String... ps) {
        if (!ctx.table.columnExists(ps[0])) {
            if ("Name".equals(ps[0])) {
                return new FunctionReturn(MeasurementTable.longerName(ctx.table.shortTitle()));
            }

            if (!"Label".equals(ps[0])) {
                return FunctionReturn.error("<Invalid col. name for input: '%s'>".formatted(ps[0]));
            }
        }

        // Special processors
        if (!"Label".equals(ps[0])) {
            switch (ps[1]) {
                case "AVG" -> {
                    var c = ctx.table.getColumn(ps[0]);
                    return new FunctionReturn(String.valueOf(ArrayUtil.mean(c)));
                }
                case "MIN" -> {
                    var c = ctx.table.getColumn(ps[0]);
                    return new FunctionReturn(String.valueOf(ArrayUtil.min(c)));
                }
                case "MAX" -> {
                    var c = ctx.table.getColumn(ps[0]);
                    return new FunctionReturn(String.valueOf(ArrayUtil.max(c)));
                }
                case "MED" -> {
                    var c = ctx.table.getColumn(ps[0]);
                    return new FunctionReturn(String.valueOf(ArrayUtil.median(c)));
                }
                default -> {}
            }
        }

        try {
            var row = switch (ps[1]) {
                case "FIRST", "F" -> 0;
                case "LAST", "L" -> ctx.table.size() - 1;
                default -> Integer.parseInt(ps[1]) - 1;
            };

            if (row < 0 || row >= ctx.table.size()) {
                return FunctionReturn.error("<Row index out of bounds: %s>".formatted(row+1));
            }

            if ("Label".equals(ps[0])) {
                return new FunctionReturn(ctx.table.getLabel(row));
            }

            return new FunctionReturn(String.valueOf(ctx.table.getValue(ps[0], row)));
        } catch (Exception e) {
            return FunctionReturn.error("<Failed to parse row: %s>".formatted(ps[1]));
        }
    }

    private static FunctionReturn datetimeNow(ResolverContext resolverContext, String[] ps) {
        try {
            var zoneId = ps[0].equals("_") ? Clock.systemDefaultZone().getZone() : ZoneId.of(ps[0]);
            Temporal dt;
            switch (ps[1]) {
                case "date", "d" -> dt = LocalDate.now(zoneId);
                case "time", "t" -> dt = LocalTime.now(zoneId);
                case "datetime", "dt", "_" -> dt = LocalDateTime.now(zoneId);
                default -> {
                    return FunctionReturn.error("<Invalid type: '%s'>".formatted(ps[1]));
                }
            }
            return new FunctionReturn(String.valueOf(dt));
        } catch (Exception e) {
            return FunctionReturn.error("<Invalid zoneId: '%s'>".formatted(ps[0]));
        }
    }

    // Operating functions

    private static FunctionReturn regex(ResolverContext ctx, String... ps) {
        var regex = ps[0];
        var m = SIMPLE_VARIABLE.matcher(ps[1]);
        // The final output
        Matcher matcher;
        try {
            matcher = Pattern.compile(regex).matcher(ps[2]);
        } catch (PatternSyntaxException e) {
            return FunctionReturn.error("<Pattern incomplete: %s>".formatted(e.getMessage()));
        }
        
        var errorState = new AtomicBoolean();

        var processed =  m.replaceAll(matchResult -> {
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
        
        return new FunctionReturn(processed.replaceAll("(\\\\)\\$", "\\$"), errorState.get());
    }

    private static FunctionReturn split(ResolverContext ctx, String[] ps) {
        // The final output
        final var input = ps[2];
        var s = input.split(ps[0]);
        var errorState = new AtomicBoolean();
        
        var processed =  LABEL_VARIABLE.matcher(ps[1]).replaceAll(matchResult -> {
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

        return new FunctionReturn(processed.replaceAll("(\\\\)\\$", "\\$"), errorState.get());
    }

    private static FunctionReturn format(ResolverContext ctx, String[] ps) {
        try {
            return new FunctionReturn(new DecimalFormat(ps[0]).format(Double.parseDouble(ps[1].trim())));
        } catch (NumberFormatException e) {
            return FunctionReturn.error("<Failed to convert '%s' into a double>".formatted(ps[1]));
        } catch (IllegalArgumentException e) {
            return FunctionReturn.error("<Invalid format '%s'>".formatted(ps[0]));
        }
    }

    private static FunctionReturn datetimeFormat(ResolverContext ctx, String[] ps) {
        Locale inLocale;
        Locale outLocale;
        DateTimeFormatter inFormat;
        DateTimeFormatter outFormat;
        try {
            inLocale = ps[1].equals("_") ? Locale.getDefault() : Locale.forLanguageTag(ps[1]);
        } catch (Exception e) {
            return FunctionReturn.error("<Invalid inLocale: '%s'>".formatted(ps[1]));
        }

        try {
            outLocale = ps[3].equals("_") ? Locale.getDefault() : Locale.forLanguageTag(ps[3]);
        } catch (Exception e) {
            return FunctionReturn.error("<Invalid outLocale: '%s'>".formatted(ps[3]));
        }

        try {
            inFormat = switch (ps[0]) {
                case "date", "d" -> DateTimeFormatter.ofPattern("yyyy-MM-dd", inLocale);
                case "time", "t" ->
                        DateTimeFormatter.ofPattern("HH:mm:ss[.[SSSSSSSSS][SSSSSSSS][SSSSSSS][SSSSSS][SSSSS][SSSS][SSS][SS][S]]", inLocale);
                case "datetime", "dt", "mjd", "jd", "_" ->
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.[SSSSSSSSS][SSSSSSSS][SSSSSSS][SSSSSS][SSSSS][SSSS][SSS][SS][S]]", inLocale);
                default -> DateTimeFormatter.ofPattern(ps[0], inLocale);
            };
        } catch (Exception e) {
            return FunctionReturn.error("<Invalid inFormat: '%s'>".formatted(ps[0]));
        }

        try {
            outFormat = switch (ps[2]) {
                case "date", "d" -> DateTimeFormatter.ofPattern("yyyy-MM-dd", outLocale);
                case "weekdaydate", "wd" -> DateTimeFormatter.ofPattern("E, MMM dd yyyy", outLocale);
                case "time", "t" -> DateTimeFormatter.ofPattern("HH:mm:ss", outLocale);
                case "datetime", "dt", "_" -> DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", outLocale);
                default -> DateTimeFormatter.ofPattern(ps[2], outLocale);
            };
        } catch (Exception e) {
            return FunctionReturn.error("<Invalid outFormat: '%s'>".formatted(ps[2]));
        }

        try {
            // Handle formatting Julian Date
            if (ps[0].equals("mjd") || ps[0].equals("jd")) {
                try {
                    var time = Double.parseDouble(ps[4]);

                    // Handle MJD
                    if (ps[0].equals("mjd")) {
                        time += 2400000;
                    }

                    return new FunctionReturn(outFormat.format(inFormat.parseBest(JulianDate.dateTime(time), ZonedDateTime::from, LocalDateTime::from, LocalDate::from, LocalTime::from)));
                } catch (NumberFormatException e) {
                    return FunctionReturn.error("<Failed to parse (M)JD to double (%s)>".formatted(ps[4]));
                }
            }
            return new FunctionReturn(outFormat.format(inFormat.parseBest(ps[4], ZonedDateTime::from, LocalDateTime::from, LocalDate::from, LocalTime::from)));
        } catch (Exception e) {
            return FunctionReturn.error("<Failed to parse or format datetime (%s)>".formatted(ps[4]));
        }
    }

    private static FunctionReturn binaryMathOperator(ResolverContext ctx, String... ps) {
        var maybeA = numberResolver(ps[1]);
        if (maybeA.isEmpty()) {
            return FunctionReturn.error("<Failed to parse a to double (%s)>".formatted(ps[1]));
        }

        var maybeB = numberResolver(ps[2]);
        if (maybeB.isEmpty()) {
            return FunctionReturn.error("<Failed to parse b to double (%s)>".formatted(ps[2]));
        }

        var a = maybeA.getAsDouble();
        var b = maybeB.getAsDouble();

        // Perform op
        try {
            double result = switch (ps[0]) {
                case "mod", "%" -> a % b;
                case "add", "+" -> a + b;
                case "sub", "-" -> a - b;
                case "div", "/" -> a / b;
                case "mul", "*" -> a * b;
                case "exp", "^" -> Math.pow(a, b);
                case "log" -> Math.log(a) / Math.log(b);
                case "root" -> {
                    if (b == 2) {
                        yield Math.sqrt(a);
                    } else if (b == 3) {
                        yield Math.cbrt(a);
                    }
                    yield Math.pow(a, 1D / b);
                }
                case "atan2" -> Math.atan2(b, a);
                case "min" -> Math.min(a, b);
                case "max" -> Math.max(a, b);
                case "qsum", "hypot" -> Math.hypot(a, b);
                case "scalb" -> {
                    if (b != (int)b) {
                        throw new IllegalStateException();
                    }
                    yield Math.scalb(a, (int)b);
                }
                default -> throw new NumberFormatException();
            };

            return new FunctionReturn(Double.toString(result));
        } catch (NumberFormatException e) {
            return FunctionReturn.error("<Could not run unknown function %s>".formatted(ps[0]));
        } catch (IllegalStateException e) {
            return FunctionReturn.error("<Could not run scalb as b (%s) was not an integer!>".formatted(b));
        }
    }

    private static FunctionReturn unaryMathOperator(ResolverContext ctx, String... ps) {
        var maybeA = numberResolver(ps[1]);
        if (maybeA.isEmpty()) {
            return FunctionReturn.error("<Failed to parse a to double (%s)>".formatted(ps[1]));
        }

        var a = maybeA.getAsDouble();

        // Perform op
        try {
            double result = switch (ps[0]) {
                case "exp" -> Math.exp(a);
                case "log" -> Math.log10(a);
                case "ln" -> Math.log(a);
                case "deg" -> Math.toDegrees(a);
                case "rad" -> Math.toRadians(a);
                case "round" -> Math.rint(a);
                case "truncate", "trunc" -> (int)a;
                case "sin" -> Math.sin(a);
                case "cos" -> Math.cos(a);
                case "tan" -> Math.tan(a);
                case "asin" -> Math.asin(a);
                case "acos" -> Math.acos(a);
                case "atan" -> Math.atan(a);
                case "sinh" -> Math.sinh(a);
                case "cosh" -> Math.cosh(a);
                case "tanh" -> Math.tanh(a);
                case "asinh" -> Math.log(a + Math.sqrt(a*a + 1));
                case "acosh" -> Math.log(a + Math.sqrt(a*a - 1));
                case "atanh" -> 0.5 * Math.log((1+a)/(1-a));
                case "fact", "!" -> Gamma.gamma(a+1);
                case "abs" -> Math.abs(a);
                case "ceil" -> Math.ceil(a);
                case "floor" -> Math.floor(a);
                case "signum" -> Math.signum(a);
                default -> throw new NumberFormatException();
            };

            return new FunctionReturn(Double.toString(result));
        } catch (NumberFormatException e) {
            return FunctionReturn.error("<Could not run unknown function %s>".formatted(ps[0]));
        }
    }

    // Util

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

    private static OptionalDouble numberResolver(String s) {
        return switch (s) {
            case "pi", "PI" -> OptionalDouble.of(Math.PI);
            case "e" -> OptionalDouble.of(Math.E);
            case "tau", "TAU" -> OptionalDouble.of(2*Math.PI);
            default -> {
                try {
                    yield OptionalDouble.of(Double.parseDouble(s));
                } catch (NumberFormatException e) {
                    yield OptionalDouble.empty();
                }
            }
        };
    }

    public FunctionReturn evaluate(ResolverContext ctx, String[] ps) {
        if (ctx.isValid()) {
            return function.apply(ctx, ps);
        }

        return FunctionReturn.error("<Missing table>");
    }

    public record FunctionReturn(String val, boolean isError) {
        public FunctionReturn(String val) {
            this(val, false);
        }

        public static FunctionReturn error(String msg) {
            return new FunctionReturn(msg, true);
        }
    }
}
