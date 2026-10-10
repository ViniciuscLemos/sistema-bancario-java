package bank;

/**
 * ANSI colors for the terminal menu: credits in green, debits and errors in red.
 * Same rule as in my battleship game: colors only on a real terminal, so redirecting the
 * output to a file doesn't fill it with escape codes. NO_COLOR turns them off and
 * FORCE_COLOR on.
 */
public final class Colors {

    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED   = "\u001B[31m";

    static final boolean ENABLED = System.getenv("NO_COLOR") == null
            && (System.getenv("FORCE_COLOR") != null || System.console() != null);

    private Colors() {}

    public static String green(String text) { return paint(text, GREEN, ENABLED); }
    public static String red(String text)   { return paint(text, RED, ENABLED); }

    static String paint(String text, String color, boolean enabled) {
        return enabled ? color + text + RESET : text;
    }
}
