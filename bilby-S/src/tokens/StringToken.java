package tokens;

import inputHandler.LocatedChar;
import inputHandler.Locator;

public class StringToken extends TokenImp {

    private String value;

    protected StringToken(Locator locator, String lexeme) {
        super(locator, lexeme);
    }

    public static Token make(LocatedChar ch, String lexeme) {
        StringToken result = new StringToken(ch, lexeme);
        result.value = lexeme.substring(1, lexeme.length() - 1);
        return result;
    }

    @Override
    protected String rawString() {
        return "string, " + value;
    }

    public String getValue() {
        return value;
    }

}
