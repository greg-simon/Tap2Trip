package au.id.simo.tap2trip.batch;

import au.id.simo.tap2trip.Trip;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class CsvTripConsumerTest {
    
    @Test
    public void testHappyOutput() throws IOException {
        Instant onInst = Instant.EPOCH;
        Instant offInst = Instant.EPOCH.plus(10, ChronoUnit.MINUTES);
        Duration duration = Duration.between(onInst, offInst);
        
        Trip trip = new Trip(
                onInst,
                offInst,
                duration,
                "fromStop",
                "toStop",
                100,
                "companyId",
                "busId",
                "PAN",
                Trip.Status.COMPLETED
        );
        
        StringWriter sw = new StringWriter();
        BatchMetrics metrics = new BatchMetrics();
        TripCsvWriter csvConsumer = new TripCsvWriter(sw, metrics);
        csvConsumer.accept(trip);
        
        String expected =
            "Started,Finished,DurationSecs,FromStopId,ToStopId,ChargeAmount,CompanyId,BusID,PAN,Status\n" +
            "01-01-1970 00:00:00,01-01-1970 00:10:00,600,fromStop,toStop,$1.00,companyId,busId,PAN,COMPLETED\n";
        assertEquals(expected, sw.toString());
        assertEquals(1, metrics.getTripsWritten());
    }
    
    @Test
    public void testEmptyIfNull() throws IOException {
        Trip nullTrip = new Trip(null, null,null,null,null,null,null,null,null,null);
        StringWriter sw = new StringWriter();
        BatchMetrics metrics = new BatchMetrics();
        TripCsvWriter csvConsumer = new TripCsvWriter(sw, metrics);
        csvConsumer.accept(nullTrip);
        
        // apache commons CSV will always quote the first empty field on a row.
        // https://issues.apache.org/jira/browse/CSV-63?attachmentSortBy=dateTime
        String expected =
            "Started,Finished,DurationSecs,FromStopId,ToStopId,ChargeAmount,CompanyId,BusID,PAN,Status\n" +
            "\"\",,,,,,,,,\n";
        assertEquals(expected, sw.toString());
        assertEquals(1, metrics.getTripsWritten());
    }
}
