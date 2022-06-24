package au.id.simo.tap2trip;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Accepts Taps to produce Trips when a trip is detected.
 */
public class TripProducer {

    private final Map<String, Tap> inProgressMap;
    private final ChargeCalculator chargeCalc;
    private final Consumer<Trip> tripConsumer;

    /**
     * Constructor.
     * @param chargeCalc the ChargeCalculator used to calculate the trip fare
     * @param tripConsumer the function that is passed the produced Trips
     */
    public TripProducer(ChargeCalculator chargeCalc, Consumer<Trip> tripConsumer) {
        this.inProgressMap = new LinkedHashMap<>();
        this.chargeCalc = chargeCalc;
        this.tripConsumer = tripConsumer;
    }

    /**
     * Applies the provided Tap to the internal state. Potentially triggering
     * a Trip to be produced and passed to the Trip consumer.
     * @param tap The Tap to add to the internal state.
     * @throws UnknownChargeException if the stop ID in the provided tap, is
     * used to calculate a trip that the ChargeCalulator is not configured with.
     */
    public void addTap(Tap tap) throws UnknownChargeException {
        // verify incomplete charge exists for this stop, just in case it is
        // an INCOMPLETE trip later. An exception is thrown if no charge exists.
        chargeCalc.getIncompleteCharge(tap.getStopId());
        
        switch (tap.getTapType()) {
            case ON:
                Tap oldTap = inProgressMap.put(tap.getPAN(), tap);
                if (oldTap != null) {
                    // end and charge old trip as incomplete.
                    // assume customer failed to tap off.
                    Trip oldTrip = incompleteOnTrip(oldTap);
                    tripConsumer.accept(oldTrip);
                }
                break;
            case OFF:
                Tap on = inProgressMap.remove(tap.getPAN());
                Trip trip;
                if (on == null) {
                    // No tap on, but a tap off exists, mark as imcomplete
                    // assume customer failed to tap on.
                    trip = incompleteOffTrip(tap);
                } else {
                    trip = calcTrip(on, tap);
                }
                tripConsumer.accept(trip);
                break;
        }
    }
    
    /**
     * Creates the Trip based on the provided ON and OFF taps.
     * 
     * @param on tap on
     * @param off tap off
     * @return The calculated trip
     * @throws UnknownChargeException if the ChargeCalculator isn't configured
     * with the stops in the provided on and off Taps.
     */
    private Trip calcTrip(Tap on, Tap off) throws UnknownChargeException {
        String fromStopId = on.getStopId();
        String toStopId = off.getStopId();
        
        Trip.Status status = Trip.Status.COMPLETED;
        Integer charge;
        if (fromStopId.equals(toStopId)) {
            status = Trip.Status.CANCELLED;
            charge = 0;
        } else {
            charge = chargeCalc.getCharge(fromStopId,toStopId);
        }
        return new Trip(
                on.getDateTime(),
                off.getDateTime(),
                Duration.between(on.getDateTime(), off.getDateTime()),
                fromStopId,
                toStopId,
                charge,
                on.getCompanyId(),
                on.getBusId(),
                on.getPAN(),
                status);
    }
    
    /**
     * Creates a trip where the customer failed to tap off.
     * @param on the Tap on.
     * @return the incomplete Trip where the Tap off details are unknown.
     * @throws UnknownChargeException if the ChargeCalculator isn't configured
     * with the stop in the provided on Tap.
     */
    private Trip incompleteOnTrip(Tap on) throws UnknownChargeException {
        return new Trip(
                on.getDateTime(),
                null,
                null,
                on.getStopId(),
                null,
                chargeCalc.getIncompleteCharge(on.getStopId()),
                on.getCompanyId(),
                on.getBusId(),
                on.getPAN(),
                Trip.Status.INCOMPLETE);
    }
    
    /**
     * Creates a trip where the customer failed to tap on.
     * @param off the Tap off.
     * @return the incomplete trip where the Tap on details are unknown.
     * @throws UnknownChargeException if the ChargeCalculator isn't configured
     * with the stop in the provided on Tap.
     */
    private Trip incompleteOffTrip(Tap off) throws UnknownChargeException {
        return new Trip(
                null,
                off.getDateTime(),
                null,
                null,
                off.getStopId(),
                chargeCalc.getIncompleteCharge(off.getStopId()),
                off.getCompanyId(),
                off.getBusId(),
                off.getPAN(),
                Trip.Status.INCOMPLETE);
    }

    /**
     * Called when all taps for a period have been processed. This is to produce
     * INCOMPLETE trips from all the left over Tap-ons.
     */
    public void completePeriod() {
        for(Tap tap: inProgressMap.values()) {
            try {
                Trip trip = incompleteOnTrip(tap);
                tripConsumer.accept(trip);
            } catch(UnknownChargeException e) {
                // shouldn't occur due to all stop being checked on entry to
                // this class in the addTap() method.
            }
        }
    }
}
