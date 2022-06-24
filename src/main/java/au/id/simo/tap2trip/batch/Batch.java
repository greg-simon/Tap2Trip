package au.id.simo.tap2trip.batch;

import au.id.simo.tap2trip.ChargeCalculator;
import au.id.simo.tap2trip.Tap;
import au.id.simo.tap2trip.TripProducer;
import au.id.simo.tap2trip.UnknownChargeException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Reads Taps from a CSV file and writes calculated trips to an output CSV file.
 * 
 * Tag input file is of the format:
 * <pre>
 * ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN
 * 1, 22-01-2018 13:00:00, ON, Stop1, Company1, Bus37, 5500005555555559
 * 2, 22-01-2018 13:05:00, OFF, Stop2, Company1, Bus37, 5500005555555559
 * </pre>
 * 
 * Trip output is of the format:
 * <pre>
 * Started,Finished,DurationSecs,FromStopId,ToStopId,ChargeAmount,CompanyId,BusID,PAN,Status
 * 22-01-2018 13:00:00,22-01-2018 13:05:00,900,Stop1,Stop2,$3.25,Company1,B37,5500005555555559,COMPLETED
 * </pre>
 * 
 * Any parse errors with taps are recorded in an error file of the format:
 * <pre>
 * Record No.,Message
 * </pre>
 */
public class Batch {
    /**
     * Date time format in all CSV files.
     */
    public static final DateTimeFormatter DTF = DateTimeFormatter
            .ofPattern("dd-MM-yyyy HH:mm:ss")
            .withZone(ZoneId.of("UTC"));
    /**
     * Enum of all the CSV columns in the Taps file.
     */
    public static enum TapCSVCols {
        ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN
    }

    /**
     * Enum of all CSV cols in the Trips file.
     */
    public static enum TripsCSVCols {
        Started, Finished, DurationSecs, FromStopId, ToStopId, ChargeAmount, CompanyId, BusID, PAN, Status
    }
    /**
     * 
     * @param args [taps.csv] [trips.csv] [error.csv]
     */
    public static void main(String[] args) {
        String tapsCsv = "taps.csv";
        if (args.length >= 1) {
            tapsCsv = args[0];
        }
        String tripsCsv = "trips.csv";
        if (args.length >= 2) {
            tripsCsv = args[1];
        }
        String errorCsv = "errors.csv";
        if (args.length >= 3) {
            errorCsv = args[2];
        }

        ChargeCalculator chargeCalc = new ChargeCalculator();
        chargeCalc.addCharge("Stop1", "Stop2", 325);
        chargeCalc.addCharge("Stop2", "Stop3", 550);
        chargeCalc.addCharge("Stop1", "Stop3", 730);

        Batch batch = new Batch(chargeCalc);
        try {
            BatchMetrics metrics = batch.process(
                new FileReader(tapsCsv,StandardCharsets.UTF_8),
                new FileWriter(tripsCsv, StandardCharsets.UTF_8),
                new FileWriter(errorCsv, StandardCharsets.UTF_8)
            );
            metrics.printCounts(System.out);
        } catch (IOException ex) {
            System.exit(1);
        }
    }
    
    private final ChargeCalculator chargeCalc;
    
    public Batch(ChargeCalculator chargeCalc) {
        this.chargeCalc = chargeCalc;
    }
    
    /**
     * Runs the batch process.
     * 
     * @param tapsCsv A Reader for the taps CSV file. This will be closed.
     * @param tripsCsv A writer for the trips CSV file. This will be closed.
     * @param errorCsv A writer for the error CSV file. This will be closed.
     * @throws IOException when there is any issues in read or writing to files.
     */
    public BatchMetrics process(Reader tapsCsv, Writer tripsCsv, Writer errorCsv) throws IOException {
        CSVFormat tapsCsvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(TapCSVCols.class)
                .setSkipHeaderRecord(true)
                .build();
        
        BatchMetrics metrics = new BatchMetrics();
        
        try (Writer tapErrWriter = errorCsv;
             Writer tripWriter = tripsCsv;
             CSVParser tapsCsvParser = tapsCsvFormat.parse(tapsCsv)) {

            TapErrorCsvWriter tapErrCsvWriter = new TapErrorCsvWriter(tapErrWriter);
            TripProducer tripProducer = new TripProducer(
                    chargeCalc,
                    new TripCsvWriter(tripWriter, metrics)
            );
            for (CSVRecord record : tapsCsvParser) {
                try {
                    Tap tap = parseTap(record);
                    tripProducer.addTap(tap);
                    metrics.incrementTapsRead();
                } catch (UnknownChargeException | TapParsingException ex) {
                    metrics.incrementTapReadErrors();
                    tapErrCsvWriter.writeError(
                            record.getRecordNumber(),
                            ex
                    );
                }
            }
            tripProducer.completePeriod();
        } catch (IOException | IllegalStateException ex) {
            // apache csv library wrapps all parsing exceptions as 
            // IllegalStateExceptions, so it makes sense to catch them here too
            metrics.printCounts(System.err);
            throw new IOException(ex);
        }
        return metrics;
    }

    public static Tap parseTap(CSVRecord record) throws TapParsingException {
        try {
            return new Tap(
                    Long.parseLong(record.get(TapCSVCols.ID).trim()),
                    DTF.parse(record.get(TapCSVCols.DateTimeUTC).trim(), Instant::from),
                    Tap.Type.valueOf(record.get(TapCSVCols.TapType).trim()),
                    record.get(TapCSVCols.StopId).trim(),
                    record.get(TapCSVCols.CompanyId).trim(),
                    record.get(TapCSVCols.BusID).trim(),
                    record.get(TapCSVCols.PAN).trim()
            );
        } catch (RuntimeException e) {
            throw new TapParsingException("Error in parsing Tap", e);
        }
    }
}
