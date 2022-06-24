package au.id.simo.tap2trip;

import java.time.Duration;
import java.time.Instant;

/**
 * A record of a customer taking a trip on a bus.
 */
public class Trip {
    public static enum Status {
        COMPLETED, INCOMPLETE, CANCELLED
    }
    private final Instant started;
    private final Instant finished;
    private final Duration duration;
    private final String fromStopId;
    private final String toStopId;
    /**
     * Charge amount in cents. i.e 100 is 1 dollar
     */
    private final Integer chargeAmount;
    private final String companyId;
    private final String busId;
    private final String PAN;
    private final Status status;

    public Trip(Instant started, Instant finished, Duration duration, String fromStopId, String toStopId, Integer chargeAmount, String companyId, String busId, String PAN, Status status) {
        this.started = started;
        this.finished = finished;
        this.duration = duration;
        this.fromStopId = fromStopId;
        this.toStopId = toStopId;
        this.chargeAmount = chargeAmount;
        this.companyId = companyId;
        this.busId = busId;
        this.PAN = PAN;
        this.status = status;
    }

    public Instant getStarted() {
        return started;
    }

    public Instant getFinished() {
        return finished;
    }

    public Duration getDuration() {
        return duration;
    }

    public String getFromStopId() {
        return fromStopId;
    }

    public String getToStopId() {
        return toStopId;
    }

    public Integer getChargeAmount() {
        return chargeAmount;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getBusId() {
        return busId;
    }

    public String getPAN() {
        return PAN;
    }

    public Status getStatus() {
        return status;
    }
}
