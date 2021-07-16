package ij.astro.logging;

import ij.IJ;
import ij.Prefs;
import ij.text.TextPanel;

import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AIJ {
    private static final Map<String, TextPanel> aijLogPanels = new HashMap<>();
    public static final String key = ".aij.useNewLogWindow";

    /**
     * Create a new log window for the caller, log the obj to a new line.
     */
    public static synchronized void log(Object obj) {
        log(obj, Prefs.getBoolean(key, true));
    }

    /**
     * Object log. Will box primitives as overloads are messy.
     */
    public static synchronized void log(Object obj, boolean useNewWindow) {
        if (obj instanceof int[] objA) {
            log(Arrays.toString(objA), useNewWindow);
        } else if (obj instanceof double[] objA) {
            log(Arrays.toString(objA), useNewWindow);
        } else if (obj instanceof float[] objA) {
            log(Arrays.toString(objA), useNewWindow);
        } else if (obj instanceof boolean[] objA) {
            log(Arrays.toString(objA), useNewWindow);
        } else if (obj instanceof long[] objA) {
            log(Arrays.toString(objA), useNewWindow);
        } else if (obj instanceof Object[] objA) {
            log(Arrays.toString(objA), useNewWindow);
        } else if (obj instanceof byte[] objA) {
            log(Arrays.toString(objA), useNewWindow);
        } else if (obj instanceof char[] objA) {
            log(Arrays.toString(objA), useNewWindow);
        } else if (obj instanceof short[] objA) {
            log(Arrays.toString(objA), useNewWindow);
        }
        log(obj.toString(), useNewWindow);
    }

    /**
     * Create a new log window for the caller, log the msg to a new line.
     */
    public static synchronized void log(String msg) {
        log(msg, Prefs.getBoolean(key, true));
    }

    /**
     * Create a new log window for the caller if useNewWindow, log the msg to a new line.
     * Falls back to {@link IJ#log(String)} with caller name.
     */
    public static synchronized void log(String msg, boolean useNewWindow) {
        if ("".equals(msg)) return;

        var caller = getCallerName();

        if (useNewWindow) {
            aijLogPanels.values().removeAll(Collections.singleton(null));
            aijLogPanels.computeIfAbsent(caller,
                    s -> new LogWindow(caller + " Log", "",400, 250).getTextPanel());
            var panel = aijLogPanels.get(caller);
            panel.updateDisplay();
            panel.setFont(new Font("SansSerif", Font.PLAIN, 16));
            panel.appendLine(msg);
        } else {
            IJ.log(caller + ": " + msg);
        }
    }

    protected static synchronized void removePanel(TextPanel textPanel) {
        aijLogPanels.values().removeAll(Collections.singleton(textPanel));
    }

    public static synchronized String getLog() {
        return aijLogPanels.containsKey(getCallerName()) ? aijLogPanels.get(getCallerName()).getText() : IJ.getLog();
    }

    private static synchronized String getCallerName() {
        var classOptional = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(s -> s.map(StackWalker.StackFrame::getDeclaringClass)
                        .filter(AIJ::predicateClassChecker).findFirst());
        var callingClass = classOptional.isPresent() ? classOptional.get() : AIJ.class;
        return callingClass.isAnnotationPresent(Translation.class)
                ? callingClass.getAnnotation(Translation.class).value()
                : callingClass.getSimpleName();
    }

    /**
     * Ignore these classes in the callstack.
     */
    private static synchronized boolean predicateClassChecker(Class<?> clazz) {
        return !clazz.getName().contains("Thread") && !clazz.equals(AIJ.class);
    }

}
