package ij.astro.logging;

import ij.IJ;
import ij.Prefs;
import ij.WindowManager;
import ij.text.TextPanel;

import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AIJ {
    private static final Map<String, TextPanel> aijLogPanels = new HashMap<>();
    /**
     * Stores a log's last modified time and if it should auto-close
     */
    private static final Map<String, ClosingConditions> aijLogPanelsTimer = new HashMap<>();
    public static final String key = ".aij.useNewLogWindow";

    static {
        final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(AIJ::checkAndExecuteTimers, 1L, 1L, TimeUnit.SECONDS);
    }

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
            aijLogPanelsTimer.computeIfPresent(caller,
                    (s, closingConditions) -> new ClosingConditions(closingConditions.autoClose));
            aijLogPanelsTimer.putIfAbsent(caller, new ClosingConditions());
        } else {
            IJ.log(caller + ": " + msg);
        }
    }

    /**
     * Sets whether the logger for the caller should auto-close or not
     */
    public static synchronized void setLogAutoCloses(boolean autoCloses) {
        aijLogPanelsTimer.put(getCallerName(), new ClosingConditions(autoCloses));
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
                : formatCaller(callingClass.getSimpleName());
    }


    /**
     * Rudimentary method takes class names and turns them into normal English.
     */
    private static synchronized String formatCaller(String name) {
        return name.replaceAll("(?<=[A-Z])(?=[A-Z][a-z])|(?<=[^A-Z])(?=[A-Z])|(?<=[A-Za-z])(?=[^A-Za-z])", " ")
                .replaceAll("_", " ").replaceAll("\s+", " ").trim();
    }

    /**
     * Ignore these classes in the callstack.
     */
    private static synchronized boolean predicateClassChecker(Class<?> clazz) {
        return !clazz.getName().contains("Thread") && !clazz.equals(AIJ.class);
    }

    /**
     * Evaluates all logs for expired ones and cleans them.
     */
    private static void checkAndExecuteTimers() {
        synchronized (AIJ.class) {
            aijLogPanelsTimer.forEach((caller, closingConditions) -> {
                if (!closingConditions.autoClose) return;
                if (System.currentTimeMillis() - closingConditions.lastModified > 5000) {
                    ((LogWindow)WindowManager.getWindow(caller + " Log")).close();
                }
            });
            // Remove old entries
            for (Map.Entry<String, ClosingConditions> entry : aijLogPanelsTimer.entrySet()) {
                if (!entry.getValue().autoClose) continue;
                if (System.currentTimeMillis() - entry.getValue().lastModified > 5000) {
                    aijLogPanelsTimer.remove(entry.getKey());
                }
            }
        }
    }

    private record ClosingConditions(boolean autoClose, Long lastModified) {
        ClosingConditions() {
            this(false, 0L);
        }

        ClosingConditions(boolean autoClose) {
            this(autoClose, System.currentTimeMillis());
        }
    }

}
