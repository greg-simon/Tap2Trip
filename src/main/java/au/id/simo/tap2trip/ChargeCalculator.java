package au.id.simo.tap2trip;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks the charges between each stop to calculate a charge for a given trip.
 */
public class ChargeCalculator {
    /**
     * Map of maps, indexed by stopId.
     * Map[fromStopId, Map[toStopId,amount]
     */
    private final Map<String, Map<String, Integer>> amountMap = new HashMap<>();
    /**
     * Keeps track of max charge amounts for incomplete trips.
     * Map[busStop, maxChangeAmount]
     */
    private final Map<String, Integer> maxChargeMap = new HashMap<>();
    
    /**
     * Records the charge between the two listed stops.
     * @param fromStopId the bus stop where the trip begins
     * @param toStopId the bus stop where the trip ends
     * @param amount the charge amount in cents for the trip
     * @return This instance. Used for method chaining.
     */
    public ChargeCalculator addCharge(String fromStopId, String toStopId, Integer amount) {
        // maps the charge in both travel directions
        mapCharge(fromStopId, toStopId, amount);
        mapCharge(toStopId, fromStopId, amount);
        return this;
    }
    
    private void mapCharge(String fromStopId, String toStopId, Integer amount) {
        Map<String, Integer> toAmountMap = amountMap.computeIfAbsent(fromStopId,(key) -> new HashMap<>());
        toAmountMap.put(toStopId, amount);
        
        // set the max charge for this stop, if the new amount is larger.
        maxChargeMap.compute(fromStopId, (key, value) -> {
            if (value == null){
                return amount;
            } 
            return Math.max(value, amount);
        });
    }
    
    /**
     * Calculates the charge amount between two stops.
     * @param fromStopId the stopId of the start of the trip.
     * @param toStopId the stopId of the end of the trip.
     * @return the change amount.
     * @throws UnknownChargeException if any of the provided bus stop IDs are
     * unknown.
     * 
     */
    public Integer getCharge(String fromStopId, String toStopId) throws UnknownChargeException {
        Map<String, Integer> stopMap = amountMap.get(fromStopId);
        if (stopMap == null) {
            throw new UnknownChargeException("Unknown stop ID: "+fromStopId);
        }
        Integer charge = stopMap.get(toStopId);
        if (charge == null) {
            throw new UnknownChargeException(
                    String.format(
                            "No charge is found between stops %s and %s",
                            fromStopId,
                            toStopId
                    )
            );
        }
        return charge;
    }
    
    /**
     * Obtains the max charge for incomplete trips for a given stop.
     * @param fromStopId the stop to use in calculating the max charge.
     * @return the max charge amount for the given stop.
     * @throws UnknownChargeException if the provided stop id is unknown.
     */
    public Integer getIncompleteCharge(String fromStopId) throws UnknownChargeException {
        Integer charge = maxChargeMap.get(fromStopId);
        if (charge == null) {
            throw new UnknownChargeException("Unknown stop ID: "+ fromStopId);
        }
        return charge;
    }
}
