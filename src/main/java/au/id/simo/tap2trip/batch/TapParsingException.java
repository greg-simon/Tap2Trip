package au.id.simo.tap2trip.batch;

/**
 * Specialised exception for flagging errors in parsing Taps.
 */
public class TapParsingException extends Exception {

    public TapParsingException(String message) {
        super(message);
    }

    public TapParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
