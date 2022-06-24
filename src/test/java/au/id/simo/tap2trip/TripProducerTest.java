package au.id.simo.tap2trip;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
public class TripProducerTest {

    private final AtomicLong tapId = new AtomicLong();

    @Test
    public void testHappyPath() throws UnknownChargeException {
        String stop1 = "stop1";
        String stop2 = "stop2";
        String cust1 = "cust1";
        
        ChargeCalculator chargeCalc = new ChargeCalculator()
                .addCharge(stop1, stop2, 10_00);
        
        // all trips produced will just be added to the tripList
        List<Trip> tripList = new ArrayList<>();
        TripProducer te = new TripProducer(chargeCalc, t -> tripList.add(t));

        te.addTap(tapOn(cust1, stop1));
        assertEquals(0, tripList.size(), "No trips should be produced yet");

        te.addTap(tapOff(cust1, stop2));
        assertEquals(1, tripList.size());
        
        Trip t1 = tripList.get(0);
        assertEquals(cust1, t1.getPAN());
        assertEquals(stop1, t1.getFromStopId());
        assertEquals(stop2, t1.getToStopId());
        assertEquals(1000, t1.getChargeAmount());
    }

    @Test
    public void testTwoTapOns() throws UnknownChargeException {
        String stop1 = "stop1";
        String stop2 = "stop2";
        String cust1 = "cust1";
        
        ChargeCalculator chargeCalc = new ChargeCalculator()
                .addCharge(stop1, stop2, 10_00);
        
        // all trips produced will just be added to the tripList
        List<Trip> tripList = new ArrayList<>();
        TripProducer te = new TripProducer(chargeCalc, t -> tripList.add(t));
        
        Tap tapOn1 = tapOn(cust1, stop1);
        Tap tapOn2 = tapOn(cust1, stop2);
        te.addTap(tapOn1);
        assertEquals(0, tripList.size(), "No trips should be produced yet");
        te.addTap(tapOn2);
        assertEquals(1, tripList.size(), "An imcomplete trip from the first tap on should be produced");
        assertEquals(tapOn1.getStopId(), tripList.get(0).getFromStopId());
        assertEquals(Trip.Status.INCOMPLETE, tripList.get(0).getStatus());
        assertNull(tripList.get(0).getToStopId(), "No 'to' stop is recorded in a incomplete tap on trip.");
    }
    
    @Test
    public void testTapOffWithNoTapOn() throws UnknownChargeException {
        String stop1 = "stop1";
        String stop2 = "stop2";
        String cust1 = "cust1";
        
        ChargeCalculator chargeCalc = new ChargeCalculator()
                .addCharge(stop1, stop2, 10_00);
        
        // all trips produced will just be added to the tripList
        List<Trip> tripList = new ArrayList<>();
        TripProducer te = new TripProducer(chargeCalc, t -> tripList.add(t));
        
        te.addTap(tapOff(cust1, stop2));
        assertEquals(1, tripList.size(), "An incomplete trip should be produced.");
        assertEquals(Trip.Status.INCOMPLETE, tripList.get(0).getStatus());
    }
    
    @Test
    public void testCancelledTrip() throws UnknownChargeException {
        String stop1 = "stop1";
        String stop2 = "stop2";
        String cust1 = "cust1";
        
        ChargeCalculator chargeCalc = new ChargeCalculator()
                .addCharge(stop1, stop2, 10_00);
        
        // all trips produced will just be added to the tripList
        List<Trip> tripList = new ArrayList<>();
        TripProducer te = new TripProducer(chargeCalc, t -> tripList.add(t));
        
        te.addTap(tapOn(cust1, stop1));
        te.addTap(tapOff(cust1, stop1));
        assertEquals(1, tripList.size());
        assertEquals(Trip.Status.CANCELLED, tripList.get(0).getStatus());
    }
    
    @Test
    public void testUnknownStops() throws UnknownChargeException {
        String stop1 = "stop1";
        String stop2 = "stop2";
        String cust1 = "cust1";
        
        ChargeCalculator chargeCalc = new ChargeCalculator()
                .addCharge(stop1, stop2, 10_00);
        
        // all trips produced will just be added to the tripList
        List<Trip> tripList = new ArrayList<>();
        TripProducer te = new TripProducer(chargeCalc, t -> tripList.add(t));
        
        te.addTap(tapOn(cust1, stop1));
        assertThrows(UnknownChargeException.class, ()-> {
            te.addTap(tapOff(cust1, "An unknown stop"));
        });
        assertEquals(0, tripList.size(), "There should be no trips in the list");
    }
    
    @Test
    public void testIncompleteTrips() throws UnknownChargeException {
        String stop1 = "stop1";
        String stop2 = "stop2";
        String cust1 = "cust1";
        
        ChargeCalculator chargeCalc = new ChargeCalculator()
                .addCharge(stop1, stop2, 10_00);
                
        
        // all trips produced will just be added to the tripList
        List<Trip> tripList = new ArrayList<>();
        TripProducer te = new TripProducer(chargeCalc, t -> tripList.add(t));
        
        te.addTap(tapOn(cust1, stop1));
        assertEquals(0, tripList.size(), "There should be no trips in the list after one tap");
        
        te.completePeriod();
        assertEquals(1, tripList.size());
        assertEquals(Trip.Status.INCOMPLETE, tripList.get(0).getStatus());
    }
    
    private Tap tapOn(String pan, String stop) {
        return new Tap(
                tapId.incrementAndGet(),
                Instant.EPOCH,
                Tap.Type.ON,
                stop,
                "company",
                "bus",
                pan
        );
    }

    private Tap tapOff(String pan, String stop) {
        return new Tap(
                tapId.incrementAndGet(),
                Instant.EPOCH.plus(10, ChronoUnit.MINUTES),
                Tap.Type.OFF,
                stop,
                "company",
                "bus",
                pan
        );
    }
}
