package io.github.alhusseinzaghloul.restfulbooker.utils;

/** An arrange step returned the wrong status (Rule 3.21). Reported under "Test setup failures". */
public class SetupFailedException extends RuntimeException {

    public SetupFailedException(String message) {
        super("Setup failed: " + message);
    }
}
