package ij.astro.util;

import java.awt.Rectangle;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

public final class WaylandMonitors {
    private static final Pattern INTERFACE_HEADER = Pattern.compile(
            "^\\s*interface:\\s*'([^']+)'(?:,.*name:\\s*(\\d+))?.*$"
    );
    private static final Pattern XDG_OUTPUT_ID = Pattern.compile("^\\s*output:\\s*(\\d+)\\s*$");
    private static final Pattern XDG_NAME = Pattern.compile("^\\s*name:\\s*'([^']+)'\\s*$");
    private static final Pattern XDG_LOGICAL_XY = Pattern.compile(
            "^\\s*logical_x:\\s*(-?\\d+),\\s*logical_y:\\s*(-?\\d+)\\s*$"
    );
    private static final Pattern XDG_LOGICAL_WH = Pattern.compile(
            "^\\s*logical_width:\\s*(\\d+),\\s*logical_height:\\s*(\\d+)\\s*$"
    );
    private static final Pattern WL_XY = Pattern.compile(
            "^\\s*x:\\s*(-?\\d+),\\s*y:\\s*(-?\\d+),.*$"
    );
    private static final Pattern WL_MODE_WH = Pattern.compile(
            "^\\s*width:\\s*(\\d+)\\s*px,\\s*height:\\s*(\\d+)\\s*px,.*$"
    );

    private static volatile List<Monitor> MONITORS = List.of();
    private static boolean triedWaylandMonitors = false;

    private WaylandMonitors() {
    }

    public static synchronized List<Monitor> query() {
        if (!MONITORS.isEmpty() || triedWaylandMonitors) {
            return MONITORS;
        }

        var monitors = queryWithWaylandInfo();
        if (!monitors.isEmpty()) {
            MONITORS = monitors;
            return monitors;
        }

        IO.println("No Wayland outputs available, using default monitor");
        triedWaylandMonitors = true;
        return MONITORS;
    }

    public static void centerOnDefaultMonitor(Window window) {
        var monitors = query();
        var chosen = defaultMonitor(monitors);
        centerOn(window, chosen.bounds());
    }

    public static void centerOnReferenceMonitor(Window window, Window reference) {
        var monitors = query();
        var ref = reference.getBounds();
        int cx = ref.x + ref.width / 2;
        int cy = ref.y + ref.height / 2;

        for (var out : monitors) {
            if (out.bounds().contains(cx, cy)) {
                centerOn(window, out.bounds());
                return;
            }
        }

        if (monitors.isEmpty()) {
            return;
        }

        centerOn(window, nearestOutput(monitors, cx, cy).bounds());
    }

    public static void centerOnMonitor(Window window, int monitor) {
        var monitors = query();
        if (monitor >= monitors.size()) {
            centerOnDefaultMonitor(window);
            return;
        }

        centerOn(window, monitors.get(monitor).bounds());
    }

    public static void centerOn(Window window, Rectangle monitorBounds) {
        var size = window.getSize();
        int x = monitorBounds.x + (monitorBounds.width - size.width) / 2;
        int y = monitorBounds.y + (monitorBounds.height - size.height) / 2;
        window.setLocation(x, y);
    }

    public static Monitor defaultMonitor(List<Monitor> monitors) {
        if (monitors.isEmpty()) {
            throw new IllegalArgumentException("No outputs available");
        }

        for (var out : monitors) {
            if (out.bounds().contains(0, 0)) {
                return out;
            }
        }

        return monitors.getFirst();
    }

    private static List<Monitor> queryWithWaylandInfo() {
        var pb = new ProcessBuilder("wayland-info");
        pb.redirectErrorStream(true);

        var builders = new LinkedHashMap<Integer, Builder>();

        try {
            var p = pb.start();

            try (var br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                String currentInterface = null;
                Integer currentWlOutputId = null;
                Integer currentXdgOutputId = null;

                while ((line = br.readLine()) != null) {
                    var interfaceHeader = INTERFACE_HEADER.matcher(line);
                    if (interfaceHeader.matches()) {
                        currentInterface = interfaceHeader.group(1);
                        currentWlOutputId = null;
                        currentXdgOutputId = null;

                        if ("wl_output".equals(currentInterface) && interfaceHeader.group(2) != null) {
                            currentWlOutputId = Integer.parseInt(interfaceHeader.group(2));
                            builders.computeIfAbsent(currentWlOutputId, Builder::new);
                        }
                        continue;
                    }

                    if (line.trim().equals("xdg_output_v1")) {
                        currentInterface = "xdg_output_v1";
                        currentXdgOutputId = null;
                        continue;
                    }

                    if (line.trim().equals("wl_output")) {
                        currentInterface = "wl_output";
                        currentWlOutputId = null;
                        continue;
                    }

                    if ("xdg_output_v1".equals(currentInterface)) {
                        var outputId = XDG_OUTPUT_ID.matcher(line);
                        if (outputId.matches()) {
                            currentXdgOutputId = Integer.parseInt(outputId.group(1));
                            builders.computeIfAbsent(currentXdgOutputId, Builder::new);
                            continue;
                        }

                        if (currentXdgOutputId != null) {
                            var b = builders.computeIfAbsent(currentXdgOutputId, Builder::new);

                            var name = XDG_NAME.matcher(line);
                            if (name.matches()) {
                                b.label = name.group(1);
                                continue;
                            }

                            var xy = XDG_LOGICAL_XY.matcher(line);
                            if (xy.matches()) {
                                b.x = Integer.parseInt(xy.group(1));
                                b.y = Integer.parseInt(xy.group(2));
                                continue;
                            }

                            var wh = XDG_LOGICAL_WH.matcher(line);
                            if (wh.matches()) {
                                b.w = Integer.parseInt(wh.group(1));
                                b.h = Integer.parseInt(wh.group(2));
                            }
                        }
                        continue;
                    }

                    if ("wl_output".equals(currentInterface) && currentWlOutputId != null) {
                        var b = builders.computeIfAbsent(currentWlOutputId, Builder::new);

                        var xy = WL_XY.matcher(line);
                        if (xy.matches()) {
                            b.x = Integer.parseInt(xy.group(1));
                            b.y = Integer.parseInt(xy.group(2));
                            continue;
                        }

                        var wh = WL_MODE_WH.matcher(line);
                        if (wh.matches()) {
                            b.w = Integer.parseInt(wh.group(1));
                            b.h = Integer.parseInt(wh.group(2));
                        }
                    }
                }
            }

            int exit = p.waitFor();
            if (exit != 0) {
                return List.of();
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            return List.of();
        }

        return builders.values().stream()
                .filter(Builder::isComplete)
                .map(Builder::toMonitor)
                .sorted(Comparator.comparingInt(m -> m.bounds().x))
                .toList();
    }

    private static Monitor nearestOutput(List<Monitor> monitors, int x, int y) {
        return monitors.stream()
                .min(Comparator.comparingLong(o -> distanceSquaredToCenter(o.bounds(), x, y)))
                .orElse(monitors.getFirst());
    }

    private static long distanceSquaredToCenter(Rectangle r, int x, int y) {
        var cx = r.x + (long) r.width / 2;
        var cy = r.y + (long) r.height / 2;
        var dx = cx - x;
        var dy = cy - y;
        return dx * dx + dy * dy;
    }

    private static final class Builder {
        final int id;
        String label;
        Integer x;
        Integer y;
        Integer w;
        Integer h;

        Builder(int id) {
            this.id = id;
        }

        boolean isComplete() {
            return x != null && y != null && w != null && h != null;
        }

        Monitor toMonitor() {
            String name = label != null ? label : "output-" + id;
            return new Monitor(name, new Rectangle(x, y, w, h));
        }
    }

    public record Monitor(String label, Rectangle bounds) {
        @Override
        public String toString() {
            return label + " " + bounds;
        }
    }
}