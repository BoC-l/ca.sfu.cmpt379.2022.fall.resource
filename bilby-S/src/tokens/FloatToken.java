package tokens;

import inputHandler.Locator;

public class FloatToken extends TokenImp {

    private double value;

    protected FloatToken(Locator locator, String lexeme) {
        super(locator, lexeme);
    }

    public static FloatToken make(Locator locator, String lexeme) {
        FloatToken result = new FloatToken(locator, lexeme);
        double value = Double.parseDouble(lexeme);
        if (value == Double.POSITIVE_INFINITY) {
            throw new NumberFormatException();
        }
        result.setValue(value);
        return result;
    }

    private void setValue(double value) {
        this.value = value;
    }

    @Override
    protected String rawString() {
        return "float: " + value;
    }

    public double getValue() {
        return value;
    }

}
