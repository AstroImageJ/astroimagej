package ij.astro.logging;

import ij.IJ;
import ij.IJEventListener;
import ij.Prefs;
import ij.WindowManager;
import ij.astro.io.prefs.Property;
import ij.text.TextPanel;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private static final String separator = "&#&";
    public static final Property<Boolean> MIRROR_LOGS_TO_FILE = new Property<>(true, AIJLogger.class);
    private static final ExecutorService LOGGING_THREAD = Executors.newSingleThreadExecutor();

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

    // No instantiating
    private AIJLogger(){}

    /**
     * Create a new log window for the caller, log the obj to a new line.
     */
    public static synchronized void log(Object obj) {
        log(obj, Prefs.getBoolean(USE_NEW_LOG_WINDOW_KEY, true));
    }

    /**
     * Create a new log window for the caller, log the obj(s) to a new line.
     */
    public static synchronized void multiLog(Object... obj) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < obj.length; i++) {
            str.append(object2String(obj[i]));
            if (i != obj.length - 1 && !(obj[i] instanceof String)) str.append("; ");
        }
        log(str.toString(), Prefs.getBoolean(USE_NEW_LOG_WINDOW_KEY, true));
    }

    /**
     * Object log. Will box primitives as overloads are messy.
     */
    public static synchronized void log(Object obj, boolean useNewWindow) {
        log(object2String(obj), useNewWindow);
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
        if (msg == null) msg = "null";

        var caller = getCallerName();
        var callerTitle = caller.split(separator)[0];

        if (useNewWindow) {
            String finalMsg = msg;
            if (MIRROR_LOGS_TO_FILE.get()) {
                LOGGING_THREAD.submit(() -> System.out.println(padTitle(callerTitle + ": ") + finalMsg));
            }
            SwingUtilities.invokeLater(() -> {
                aijLogPanels.values().removeAll(Collections.singleton(null));
                aijLogPanels.computeIfAbsent(caller,
                        caller1 -> new LogWindow(callerTitle + " Log", "",400, 250).getTextPanel());
                var panel = aijLogPanels.get(caller);
                panel.updateDisplay();
                panel.appendLine(finalMsg);
                aijLogPanelsTimer.computeIfPresent(caller,
                        (caller_, closingConditions) -> new ClosingConditions(closingConditions.autoClose));
                aijLogPanelsTimer.putIfAbsent(caller, new ClosingConditions());
            });
        } else {
            var s = padTitle(callerTitle + ": ") + msg;
            if (MIRROR_LOGS_TO_FILE.get()) {
                LOGGING_THREAD.submit(() -> System.out.println(s));
            }
            IJ.log(s);
        }
    }

    /**
     * Prints the stacktrace of the caller in a log window.
     * Disables auto-closing for the caller.
     * <p>The first method in the stacktrace is the caller of this method. {@link AIJLogger}'s methods
     * are filtered out.</p>
     */
    public static synchronized void logStacktrace() {
        setLogAutoCloses(false);
        log("Stacktrace: " +
                StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(AIJLogger::walk2List));
    }

    /**
     * Prints the stacktrace of the caller in the console.
     * Disables auto-closing for the caller.
     * <p>The first method in the stacktrace is the caller of this method. {@link AIJLogger}'s methods
     * are filtered out.</p>
     */
    public static void logStacktraceC() {
        var stack = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(stackFrameStream -> {
                    return stackFrameStream.filter(stackFrame -> !stackFrame.getDeclaringClass().equals(AIJLogger.class))
                            .map(StackWalker.StackFrame::toStackTraceElement)
                            .toArray(StackTraceElement[]::new);
                });

        if (stack.length == 0) {
            System.out.println("Logged, but no stack available!");
        }

        System.out.println("Stacktrace: " + stack[0]);
        for (int i = 1; i < stack.length; i++) {
            System.out.println("\tby " + stack[i]);
        }
    }

    private static synchronized List<StackWalker.StackFrame> walk2List(Stream<StackWalker.StackFrame> stackFrameStream) {
        return stackFrameStream.filter(stackFrame -> !stackFrame.getDeclaringClass().equals(AIJLogger.class))
                .collect(Collectors.toList());
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
                ? callingClass.getAnnotation(Translation.class).value() +
                    (callingClass.getAnnotation(Translation.class).trackThread()
                        ? separator + Thread.currentThread().getId() : "")
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
                var lWin = ((LogWindow)WindowManager.getWindow(caller + " Log"));
                if (!closingConditions.autoClose) {
                    lWin.setAlwaysOnTop(false);
                    return;
                }
                lWin.setAlwaysOnTop(true);
                if (System.currentTimeMillis() - closingConditions.lastModified > 5000) {
                    if (lWin != null) SwingUtilities.invokeLater(lWin::close);
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

    public static String object2String(Object obj) {
        if (obj instanceof int[] objA) {
            return Arrays.toString(objA);
        } else if (obj instanceof double[] objA) {
            return Arrays.toString(objA);
        } else if (obj instanceof float[] objA) {
            return Arrays.toString(objA);
        } else if (obj instanceof boolean[] objA) {
            return Arrays.toString(objA);
        } else if (obj instanceof long[] objA) {
            return Arrays.toString(objA);
        } else if (obj instanceof Object[] objA) {
            return Arrays.deepToString(objA);
        } else if (obj instanceof byte[] objA) {
            return Arrays.toString(objA);
        } else if (obj instanceof char[] objA) {
            return Arrays.toString(objA);
        } else if (obj instanceof short[] objA) {
            return Arrays.toString(objA);
        } else {
            return String.valueOf(obj);
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
