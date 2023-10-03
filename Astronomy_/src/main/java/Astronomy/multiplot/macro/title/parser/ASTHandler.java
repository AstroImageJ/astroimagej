package Astronomy.multiplot.macro.title.parser;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ASTHandler {
    private List<Token> tokens;
    private int currentIndex;

    public ASTHandler(List<Token> tokens) {
        this.tokens = tokens;
        this.currentIndex = 0;
    }

    public ASTNode buildAST() {
        ASTNode rootNode = new ASTNode(new Token(Token.TokenType.TEXT, "ROOT"));
        while (currentIndex < tokens.size()) {
            rootNode.addChild(parseExpression());
        }
        return rootNode;
    }

    private ASTNode parseExpression() {
        Token currentToken = tokens.get(currentIndex);

        if (currentToken.getType() == Token.TokenType.FUNCTION_HANDLE) {
            ASTNode functionNode = new ASTNode(currentToken);
            currentIndex++;

            var f = Functions.getFunction(currentToken.getValue());
            if (f != null) {
                for (int i = 0; i < f.paramCount(); i++) {
                    if (currentIndex < tokens.size()) {
                        var p = parseExpression();
                        functionNode.addChild(p);

                        // Whitespace cannot be function param, continue
                        if (p.getToken().getType() == Token.TokenType.WHITESPACE) {
                            i--;
                        }
                    }
                }
            }

            return functionNode;
        } else {
            currentIndex++;
            return new ASTNode(currentToken);
        }
    }

    public static void main(String... args) {
        String input = args[0];//"Hello @table Label 1 @today world";
        List<Token> tokens = StringLexer.tokenize(input);
        System.out.println(input);
        System.out.println(tokens);
        ASTHandler astHandler = new ASTHandler(tokens);
        ASTNode ast = astHandler.buildAST();
        printAST(ast, "");
        System.out.println(rebuildInputString(null, ast));
        System.out.println(input);
        System.out.println(evaluateFunction(null, ast));
        System.out.println(rebuildInputString(null, ast));

        var info = buildHighlightingInfo(null, ast);
        for (HighlightInfo highlightInfo : info) {
            if (highlightInfo.types.contains(HighlightType.WHITESPACE)) {
                continue;
            }
            System.out.println("%s : %s".formatted(input.substring(highlightInfo.beginIndex, highlightInfo.endIndex), highlightInfo.types));
        }
    }

    public static void printAST(ASTNode node, String indent) {
        System.out.println(indent + node.getToken().getValue());

        for (ASTNode child : node.getChildren()) {
            printAST(child, indent + "  ");
        }
    }

    public static StringBuilder rebuildInputString(StringBuilder sb, ASTNode node) {
        if (sb == null) {
            sb = new StringBuilder();
        } else {
            // in the else to avoid printing the root node
            sb.append(node.getToken().getValue());
        }

        for (ASTNode child : node.getChildren()) {
            rebuildInputString(sb, child);
        }

        return sb;
    }

    public static Functions.FunctionReturn evaluateFunction(ResolverContext ctx, ASTNode node) {
        var sb = new StringBuilder();

        ASTNode previousChild = null;
        var hasError = false;
        for (ASTNode child : node.getChildren()) {
            if (previousChild != null &&
                    previousChild.getToken().getType() == Token.TokenType.FUNCTION_HANDLE &&
                    child.getToken().getType() == Token.TokenType.WHITESPACE) {
                var whitespace = child.getToken().getValue().replaceFirst(" ", "");
                sb.append(whitespace);
                child.getToken().setModifiedWhitespace(whitespace.length() != child.getToken().getValue().length());
            } else {
                var p = child.process(ctx);
                sb.append(p.val());
                if (!hasError) {
                    hasError = p.isError() || child.getToken().isErr();
                }
            }
            previousChild = child;
        }

        return new Functions.FunctionReturn(sb.toString(), hasError);
    }

    /**
     * Must be run after function evaluation
     */
    public static Set<HighlightInfo> buildHighlightingInfo(StringBuilder sb, ASTNode node) {
        //todo was treeset, cannot use as the index it was sorting with apparently prevented some highlights from being added
        //todo need new sorting method?
        var highlightInfo = new HashSet<HighlightInfo>(/*Comparator.comparingInt(i -> i.endIndex - i.beginIndex)*/);
        var isRoot = sb == null;
        var beginIndex = 0;
        if (sb == null) {
            sb = new StringBuilder();//used for tracking index
        } else {
            // in the else to avoid printing the root node
            beginIndex = sb.length();
            sb.append(node.getToken().getValue());
            var endIndex = sb.length();
            var n = new HighlightInfo(beginIndex, endIndex, EnumSet.noneOf(HighlightType.class));
            if (node.getToken().isErr()) {
                n.types.add(HighlightType.ERROR);
            }
            if (node.getToken().getType() == Token.TokenType.FUNCTION_HANDLE) {
                n.types.add(HighlightType.FUNCTION_HANDLE);
            }
            if (node.getToken().getType() == Token.TokenType.WHITESPACE) {
                n.types.add(HighlightType.WHITESPACE);
            }
            if (node.getToken().isModifiedWhitespace()) {
                n.types.add(HighlightType.MODIFIED_WHITESPACE);
            }
            //todo if quoted, remove quotes?
            highlightInfo.add(n);
        }

        for (ASTNode child : node.getChildren()) {
            var ns = buildHighlightingInfo(sb, child);
            if (!isRoot) {
                ns.forEach(i -> i.types.add(HighlightType.PARAM));
            }
            highlightInfo.addAll(ns);
        }

        if (node.getToken().getType() == Token.TokenType.FUNCTION_HANDLE) {
            highlightInfo.add(new HighlightInfo(beginIndex, sb.length(), EnumSet.of(HighlightType.FUNCTION)));
        }

        return highlightInfo;
    }

    public record HighlightInfo(int beginIndex, int endIndex, EnumSet<HighlightType> types) {
        public boolean contains(HighlightInfo i2) {
            return i2.beginIndex >= beginIndex && endIndex >= i2.endIndex;
        }
    }

    public enum HighlightType {
        ERROR,
        FUNCTION_HANDLE,
        PARAM,
        FUNCTION,
        WHITESPACE,
        MODIFIED_WHITESPACE,
    }
}
