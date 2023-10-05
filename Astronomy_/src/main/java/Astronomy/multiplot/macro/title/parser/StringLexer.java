package Astronomy.multiplot.macro.title.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringLexer {
    private static final String TEXT_REGEX = "(?<WORD>\\S+)";
    private static final String QUOTED_TEXT_REGEX = "(?<QUOTED>((?=[\"'])(?:\"[^\"\\\\]*(?:\\\\[\\s\\S][^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\[\\s\\S][^'\\\\]*)*')))";
    private static final String FUNCTION_REGEX = "(?<FUNCPREFIX>\\S+)*(?<FUNCNAME>(?<!\\\\)@ ?\\S*)";
    private static final String WHITESPACE_REGEX = "(?<WHITESPACE>\\s+)";
    private static final Pattern TOKENIZER_PATTERN = Pattern.compile(QUOTED_TEXT_REGEX + "|" + FUNCTION_REGEX + "|" + TEXT_REGEX + "|" + WHITESPACE_REGEX);

    public static List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        Matcher matcher = TOKENIZER_PATTERN.matcher(input);

        while (matcher.find()) {
            String tokenValue;
            if ((tokenValue = matcher.group("FUNCNAME")) != null) {
                String pre = matcher.group("FUNCPREFIX");
                if (pre != null) {
                    tokens.add(new Token(Token.TokenType.TEXT, pre));
                }
                tokens.add(new Token(Token.TokenType.FUNCTION_HANDLE, tokenValue));
            } else if ((tokenValue = matcher.group("QUOTED")) != null) {
                tokens.add(new Token(Token.TokenType.QUOTED_TEXT, tokenValue));
            } else if ((tokenValue = matcher.group("WORD")) != null) {
                tokens.add(new Token(Token.TokenType.TEXT, tokenValue));
            } else if ((tokenValue = matcher.group("WHITESPACE")) != null) {
                tokens.add(new Token(Token.TokenType.WHITESPACE, tokenValue));
            } else {
                //todo better handling
                System.out.println("Did not match group!");
            }
        }

        return tokens;
    }
}
