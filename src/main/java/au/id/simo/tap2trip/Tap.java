package au.id.simo.tap2trip;

import java.time.Instant;

/**
 * A record of a customer taping their card.
 */
public class Tap {
    public static enum Type {
        ON, OFF
    }
    private final Long id;
    private final Instant dateTime;
    private final Type tapType;
    private final String stopId;
    private final String companyId;
    private final String busId;
    private final String PAN;

    public Tap(Long id, Instant dateTime, Type tapType, String stopId, String companyId, String busId, String PAN) {
        this.id = id;
        this.dateTime = dateTime;
        this.tapType = tapType;
        this.stopId = stopId;
        this.companyId = companyId;
        this.busId = busId;
        this.PAN = PAN;
    }

    public long getId() {
        return id;
    }

    public Instant getDateTime() {
        return dateTime;
    }

    public Type getTapType() {
        return tapType;
    }

    public String getStopId() {
        return stopId;
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
}
