package au.id.simo.tap2trip;

/**
 * Occurs when a ChargeCalculator is asked to calculate a trip between two stops
 * without being configured for at least one of those stops.
 */
public class UnknownChargeException extends Exception {

    public UnknownChargeException(String message) {
        super(message);
    }
}
