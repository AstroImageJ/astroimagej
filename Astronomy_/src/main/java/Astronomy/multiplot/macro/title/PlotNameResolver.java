package Astronomy.multiplot.macro.title;

import Astronomy.MultiPlot_;
import Astronomy.multiplot.macro.title.parser.*;
import astroj.HelpPanel;
import astroj.MeasurementTable;
import ij.astro.io.prefs.Property;
import ij.astro.types.Pair;
import ij.astro.util.UIHelper;

import java.util.List;
import java.util.Set;

public class PlotNameResolver {
    public static final Property<String> TITLE_MACRO = new Property<>("", PlotNameResolver.class);
    public static final Property<String> SUBTITLE_MACRO = new Property<>("", PlotNameResolver.class);
    private static final HelpPanel helpPanel = new HelpPanel("help/plotMacroHelp.html", "Programmable Plot Titles");
    private static Pair.GenericPair<String, Boolean> lastTitleState;
    private static Pair.GenericPair<String, Boolean> lastSubtitleState;

    private PlotNameResolver() {
    }

    public static Pair.GenericPair<String, Boolean> resolvePlotTitle(MeasurementTable table) {
        try {
            return (lastTitleState = resolve(table, TITLE_MACRO.get()).state);
        } catch (Exception e) {
            e.printStackTrace();
            return new Pair.GenericPair<>(TITLE_MACRO.get(), true);
        }
    }

    public static Pair.GenericPair<String, Boolean> resolvePlotSubtitle(MeasurementTable table) {
        try {
            return (lastSubtitleState = resolve(table, SUBTITLE_MACRO.get()).state);
        } catch (Exception e) {
            e.printStackTrace();
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

    public static ResolveState resolve(MeasurementTable table, String input) {
        List<Token> tokens = StringLexer.tokenize(input);
        //System.out.println(input);
        //System.out.println(tokens);
        ASTHandler astHandler = new ASTHandler(tokens);
        ASTNode ast = astHandler.buildAST();
        //printAST(ast, "");
        var o = ASTHandler.evaluateFunction(new ResolverContext(table), ast);
        return new ResolveState(new Pair.GenericPair<>(o.val(), o.isError()), ASTHandler.buildHighlightingInfo(null, ast));
    }

    public record ResolveState(Pair.GenericPair<String, Boolean> state, Set<ASTHandler.HighlightInfo> highlightInfos) {}

    public static void showHelpWindow() {
        if (!helpPanel.isVisible()) {
            UIHelper.setCenteredOnScreen(helpPanel, MultiPlot_.mainFrame);
        }
        helpPanel.setVisible(true);
    }
}
