package astroj.astrometry.solvers;

import ij.astro.gui.ToolTipProvider;

import java.util.Collection;
import java.util.List;

public sealed interface PlateSolver extends AutoCloseable permits HttpAstrometryNet, WasmAstrometryNet {
    Result solve(double[] xs, double[] ys,
                 int imageWidth, int imageHeight,
                 double scaleLower, double scaleUpper,
                 double initialRa, double initialDec, double searchRadius,
                 boolean useDistortionOrder, double distortionOrder);

    /// If this solver requires the caller to wait after receiving a result before solving the next image
    boolean requiresAwait();

    sealed interface Result permits Result.Failure, Result.Success {
        record Success(String wcsHeader, Collection<AstrometryObject> objectsInField) implements Result {
            public Success {
                if (objectsInField == null) {
                    objectsInField = List.of();
                }
            }

            public Success(String wcsHeader) {
                this(wcsHeader, null);
            }
        }

        record Failure(String message, boolean cancelled) implements Result {
            public Failure(String message) {
                this(message, false);
            }
        }
    }

    record AstrometryObject(double radius, String type, Collection<String> names, double x, double y) {}

    enum SolverType implements ToolTipProvider {
        ASTROMETRY_NET_HTTP("Astrometry.net web api", """
                
                """),
        ASTROMETRY_NET_WASM("Bundled Astrometry.net", """
                
                """),
        ;

        final String toolTip;
        final String displayName;

        SolverType(String displayName, String toolTip) {
            this.toolTip = toolTip;
            this.displayName = displayName;
        }

        @Override
        public String getToolTip() {
            return toolTip;
        }

        @Override
        public String toString() {
            return displayName.isEmpty() ? super.toString() : displayName;
        }
    }
}
