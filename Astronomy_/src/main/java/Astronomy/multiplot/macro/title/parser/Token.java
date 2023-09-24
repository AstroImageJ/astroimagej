package Astronomy.multiplot.macro.title.parser;

public class Token {
    public enum TokenType {
        TEXT,
        QUOTED_TEXT,
        FUNCTION_HANDLE,
        WHITESPACE,
    }

    private TokenType type;
    private String value;
    private boolean isErr;//todo mvoe these to distinct types?
    private boolean modifiedWhitespace;

    public Token(TokenType type, String value) {
        this.type = type;
        this.value = value;
    }

    public TokenType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public boolean isErr() {
        return isErr;
    }

    public void setErr(boolean err) {
        isErr = err;
    }

    public boolean isModifiedWhitespace() {
        return modifiedWhitespace;
    }

    public void setModifiedWhitespace(boolean modifiedWhitespace) {
        this.modifiedWhitespace = modifiedWhitespace;
    }

    @Override
    public String toString() {
        return "Token{" +
                "type=" + type +
                ", value='" + value + '\'' +
                '}';
    }
}
