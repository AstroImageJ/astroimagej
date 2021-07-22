package ij.astro.logging;

import ij.IJ;
import ij.IJEventListener;
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

/**
 * AIJ Logger class. Supports opt-in auto-closing of log window, opening a new log window for each calling class's log
 * invoke, and adding calling the class as a prefix to a log message when falling back to {@link IJ#log(String)}.
 */
public class AIJLogger {
    private static int callerWidth = 0;
    private static final Map<String, TextPanel> aijLogPanels = new HashMap<>();
    /**
     * Stores a log's last modified time and if it should auto-close
     */
    private static final Map<String, ClosingConditions> aijLogPanelsTimer = new HashMap<>();
    public static final String USE_NEW_LOG_WINDOW_KEY = ".aij.useNewLogWindow";
    public static final String CERTAIN_LOGS_AUTO_CLOSE = ".aij.cLogsAutoClose";

    static {
        final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(AIJLogger::checkAndExecuteTimers, 1L, 1L, TimeUnit.SECONDS);

        // Reset width when log window is closed
        IJ.addEventListener(eventID -> {
            if (eventID == IJEventListener.LOG_WINDOW_CLOSED) {
                callerWidth = 0;
            }
        });
    }

    /**
     * Create a new log window for the caller, log the obj to a new line.
     */
    public static synchronized void log(Object obj) {
        log(obj, Prefs.getBoolean(USE_NEW_LOG_WINDOW_KEY, true));
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
        } else if (obj == null) {
            log("null", useNewWindow);
        } else {
            log(obj.toString(), useNewWindow);
        }
    }

    /**
     * Create a new log window for the caller, log the msg to a new line.
     */
    public static synchronized void log(String msg) {
        log(msg, Prefs.getBoolean(USE_NEW_LOG_WINDOW_KEY, true));
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
            IJ.log(padTitle(caller + ": ") + msg);
        }
    }

    /**
     * Sets whether the logger for the caller should auto-close or not
     */
    public static synchronized void setLogAutoCloses(boolean autoCloses) {
        aijLogPanelsTimer.put(getCallerName(), new ClosingConditions(autoCloses));
    }

    public static synchronized String getLog() {
        return aijLogPanels.containsKey(getCallerName()) ? aijLogPanels.get(getCallerName()).getText() : IJ.getLog();
    }

    protected static synchronized void removePanel(TextPanel textPanel) {
        aijLogPanels.values().removeAll(Collections.singleton(textPanel));
    }

    /**
     * Add padding to align messages in unified log window
     */
    private static synchronized String padTitle(String caller) {
        var logWindow = WindowManager.getWindow("Log");
        if (logWindow != null) {
            var met = logWindow.getFontMetrics(logWindow.getFont());
            if (met.stringWidth(caller) > callerWidth) callerWidth = met.stringWidth(caller);
            while (met.stringWidth(caller) < callerWidth - 3*met.stringWidth(" ")) {
                caller += " ";
            }
        }

        return caller;
    }

    private static synchronized String getCallerName() {
        var classOptional = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(s -> s.map(StackWalker.StackFrame::getDeclaringClass)
                        .filter(AIJLogger::predicateClassChecker).findFirst());
        var callingClass = classOptional.isPresent() ? classOptional.get() : AIJLogger.class;
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
        return !clazz.getName().contains("Thread") && !clazz.equals(AIJLogger.class);
    }

    /**
     * Evaluates all logs for expired ones and cleans them.
     */
    private static void checkAndExecuteTimers() {
        synchronized (AIJLogger.class) {
            aijLogPanelsTimer.forEach((caller, closingConditions) -> {
                if (!closingConditions.autoClose) return;
                if (System.currentTimeMillis() - closingConditions.lastModified > 5000) {
                    var lWin = ((LogWindow)WindowManager.getWindow(caller + " Log"));
                    if (lWin != null) lWin.close();
                }
            });
            // Remove outdated entries that don't need timing information
            for (Map.Entry<String, ClosingConditions> entry : aijLogPanelsTimer.entrySet()) {
                if (entry.getValue().autoClose) continue; // Persist entries that autoClose
                if (System.currentTimeMillis() - entry.getValue().lastModified > 10000) {
                    aijLogPanelsTimer.remove(entry.getKey());
                }
            }
        }
    }

    private record ClosingConditions(boolean autoClose, long lastModified) {
        ClosingConditions() {
            this(false);
        }

        ClosingConditions(boolean autoClose) {
            this(autoClose, System.currentTimeMillis());
        }
    }

}
