package main.java.plinko.Exceptions;

public class TagFormatException extends RuntimeException {
    public TagFormatException(String strTag) {
        super("String '%s' does not correspond to any PatternTag.".formatted(strTag));
    }
}
