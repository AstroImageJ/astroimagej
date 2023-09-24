package Astronomy.multiplot.macro.title.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ASTNode {
    private Token token;
    private List<ASTNode> children;

    public ASTNode(Token token) {
        this.token = token;
        this.children = new ArrayList<>();
    }

    public void addChild(ASTNode child) {
        children.add(child);
    }

    public Token getToken() {
        return token;
    }

    public List<ASTNode> getChildren() {
        return children;
    }

    public Functions.FunctionReturn process(ResolverContext ctx) {
        var sb = new StringBuilder();
        var error = false;
        if (getToken().getType() == Token.TokenType.FUNCTION_HANDLE) {
            var func = Functions.getFunction(getToken().getValue());
            if (func != null) {
                var children = this.children.stream().filter(c -> c.getToken().getType() != Token.TokenType.WHITESPACE).toList();
                if (func.paramCount() != children.size()) {
                    children.forEach(c -> c.getToken().setErr(true));
                    getToken().setErr(true);
                    return new Functions.FunctionReturn("<%s missing params: %s>"
                            .formatted(getToken().getValue(), Arrays.toString(
                                    Arrays.copyOfRange(func.parameters, children.size(), func.paramCount()))), true);
                }

                var ps = new String[func.paramCount()];
                var p = 0;
                for (ASTNode child : children) {
                    var cfr = child.process(ctx);
                    ps[p++] = cfr.val();
                    error = cfr.isError();
                    if (error) {
                        break;
                    }
                }

                if (!error) {
                    var o = func.function.apply(ctx, ps);
                    if (o.isError()) {
                        // Only set here, we want to highlight the function that caused the error
                        token.setErr(true);
                    }

                    return o;
                }

                children.forEach(c -> c.getToken().setErr(true));
            } else {
                // Unknown functions should have no children
                return new Functions.FunctionReturn("<Unknown function: '%s'>".formatted(getToken().getValue()), true);
            }
        } else if (getToken().getType() == Token.TokenType.QUOTED_TEXT) {
            var o = getToken().getValue();
            // Unwrap quoted column
            //todo handle escaped quotes
            if (((o.startsWith("\"") && o.endsWith("\"")) || (o.startsWith("'") && o.endsWith("'"))) && o.length() > 1) {
                o = o.substring(1, o.length()-1);
            }
            return new Functions.FunctionReturn(o, false);
        }

        // An error occurred in the stack, don't eval function
        sb.append(getToken().getValue());
        for (ASTNode child : getChildren()) {
            sb.append(child.process(ctx).val());
        }

        return new Functions.FunctionReturn(sb.toString(), error);
    }
}
