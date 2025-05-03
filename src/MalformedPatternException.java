public class MalformedPatternException extends RuntimeException {
    public MalformedPatternException(char obj) {
        super("object '%c' cannot be owned by the server.".formatted(obj));
    }
}
