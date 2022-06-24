package au.id.simo.tap2trip.batch;

import au.id.simo.tap2trip.Trip;
import java.io.IOException;
import java.io.Writer;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/**
 * Outputs in the following format:
 * <pre>
 * Started,Finished,DurationSecs,FromStopId,ToStopId,ChargeAmount,CompanyId,BusID,PAN,Status
 * </pre>
 * Line endings will always be {@code \n}
 */
public class TripCsvWriter implements Consumer<Trip>{

    private static final DateTimeFormatter DTF = DateTimeFormatter
            .ofPattern("dd-MM-yyyy HH:mm:ss")
            .withZone(ZoneId.of("UTC"));
    
    private final CSVPrinter csvPrinter;
    private final BatchMetrics metrics;

    public TripCsvWriter(Writer writer, BatchMetrics metrics) throws IOException {
        CSVFormat tripsCsvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(Batch.TripsCSVCols.class)
                .setRecordSeparator('\n')
                .build();
        this.csvPrinter = tripsCsvFormat.print(writer);
        this.metrics = metrics;
    }
    
    @Override
    public void accept(Trip t) {
        try {
            csvPrinter.printRecord(
                    emptyIfNull(t.getStarted(), v -> DTF.format(v)),
                    emptyIfNull(t.getFinished(), v -> DTF.format(v)),
                    emptyIfNull(t.getDuration(), v -> v.getSeconds()),
                    emptyIfNull(t.getFromStopId()),
                    emptyIfNull(t.getToStopId()),
                    emptyIfNull(t.getChargeAmount(), v -> {
                        int dollars = v / 100;
                        int cents = v % 100;
                        return String.format("$%d.%02d", dollars, cents); 
                    }),
                    emptyIfNull(t.getCompanyId()),
                    emptyIfNull(t.getBusId()),
                    emptyIfNull(t.getPAN()),
                    emptyIfNull(t.getStatus())
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Error in writing trip", ex);
        }
        metrics.incrementTripsWritten();
    }
    
    private String emptyIfNull(Object obj) {
        if (obj == null) {
            return "";
        }
        return String.valueOf(obj);
    }
    
    private <T> String emptyIfNull(T obj, Function<T,Object> func) {
        if (obj == null) {
            return "";
        }
        return String.valueOf(func.apply(obj));
    }
}
