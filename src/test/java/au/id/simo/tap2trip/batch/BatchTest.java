package au.id.simo.tap2trip.batch;

import au.id.simo.tap2trip.ChargeCalculator;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
public class BatchTest {

    @Test
    public void testExample() throws Exception {
        Batch batch = new Batch(new ChargeCalculator().addCharge("Stop1", "Stop2", 325));
        StringReader tapsCsv = new StringReader(String.join("\n",
            "ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN",
            "1, 22-01-2018 13:00:00, ON, Stop1, Company1, Bus37, 5500005555555559",
            "2, 22-01-2018 13:05:00, OFF, Stop2, Company1, Bus37, 5500005555555559"
        ));
        StringWriter tripsCsv = new StringWriter();
        StringWriter errorCsv = new StringWriter();
        batch.process(tapsCsv, tripsCsv, errorCsv);
        
        // pasted from given example.
        // Though changes made to match tap record are:
        // * Bus 'B37' changes to 'Bus37'
        // * 900 seconds duration to 300 (5 mins x 60 = 300)
        assertEquals(
            String.join("\n",
                "Started,Finished,DurationSecs,FromStopId,ToStopId,ChargeAmount,CompanyId,BusID,PAN,Status",
                "22-01-2018 13:00:00,22-01-2018 13:05:00,300,Stop1,Stop2,$3.25,Company1,Bus37,5500005555555559,COMPLETED",
                "" // final newline
            ),
            tripsCsv.toString()
        );
        
        String[] errorLines = errorCsv.toString().split("\n");
        assertEquals(1, errorLines.length, "Only the error file column headers should be written");
    }
    
    @Test
    public void testTruncatedSecondTapLine() throws Exception {
        Batch batch = new Batch(new ChargeCalculator().addCharge("Stop1", "Stop2", 325));
        StringReader tapsCsv = new StringReader(String.join("\n",
            "ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN",
            "1, 22-01-2018 13:00:00, ON, Stop1, Company1, Bus37, 5500005555555559",
            "2, 22-01-2018 13:05:00, OFF, Stop2,"
        ));
        StringWriter tripsCsv = new StringWriter();
        StringWriter errorCsv = new StringWriter();
        batch.process(tapsCsv, tripsCsv, errorCsv);
        
        // Incomplete due to trunctated tap off that was never parsed correctly.
        assertEquals(
            String.join("\n",
                "Started,Finished,DurationSecs,FromStopId,ToStopId,ChargeAmount,CompanyId,BusID,PAN,Status",
                "22-01-2018 13:00:00,,,Stop1,,$3.25,Company1,Bus37,5500005555555559,INCOMPLETE",
                "" // final newline
            ),
            tripsCsv.toString()
        );
        
        String[] errorLines = errorCsv.toString().split("\n");
        assertEquals(2, errorLines.length, "The column headers and another error row should be written");
    }
}
