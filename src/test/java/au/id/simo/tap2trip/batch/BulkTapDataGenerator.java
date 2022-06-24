package au.id.simo.tap2trip.batch;

import au.id.simo.tap2trip.Tap;
import au.id.simo.tap2trip.Trip;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/**
 * This class generates test data.
 *
 * Limitations:
 * <ol>
 * <li>The generated timestamps are not very realistic, though functional.
 * </ol>
 */
public class BulkTapDataGenerator {

    private static final DateTimeFormatter DTF = Batch.DTF;
    private static final Random RND = new Random();

    private static final Instant MIDDAY = Instant.parse("2022-01-01T12:00:00.00Z");
    private static final Instant START_OF_DAY = Instant.parse("2022-01-01T00:00:00.00Z");
    private static final Instant END_OF_DAY = Instant.parse("2022-01-01T23:59:59.00Z");
    
    public static void main(String[] args) throws IOException {
        // file size config
        // the generated taps file records count will be about 2x the total of
        // these three numbers.
        Integer tripsComplete = 10;
        Integer tripsIncomplete = 5;
        Integer tripsCancelled = 2;
        
        BulkTapDataGenerator dataGen = new BulkTapDataGenerator();
        dataGen.generateTaps(tripsComplete, tripsIncomplete, tripsCancelled, new FileWriter("generated-taps.csv"));
    }

    /**
     * Current instant. Used in generating timestamps. Trip starts only occur
     * before midday, and trip ends only occur after.
     */
    private Instant instant = START_OF_DAY.plusSeconds(1);

    /**
     * Generates basic data for testing purposes.
     * 
     * The number of records written is described by the formula:
     * {@code tripsComplete x2 + tripsCancelled x2 + tripsIncomplete}
     * 
     * Better for load testing than correctness testing.
     * 
     * @param tripsComplete Number of complete trips to generate.
     * @param tripsIncomplete Number of incomplete trips to generate.
     * @param tripsCancelled Number of ON taps without corresponding OFF taps.
     * @param writer where the generated taps are written to. Writer is closed.
     * @throws IOException if there is any exceptions thrown when writing to the
     * provided Writer.
     */
    public void generateTaps(Integer tripsComplete, Integer tripsIncomplete, Integer tripsCancelled, Writer writer) throws IOException {
        
        // data config
        List<String> stops = Arrays.asList("Stop1", "Stop2", "Stop3");
        List<String> companys = Arrays.asList("Company1");
        List<String> busses = Arrays.asList("Bus1", "Bus2", "Bus3");

        Map<Trip.Status, Integer> tripStatusCountMap = new LinkedHashMap<>();
        tripStatusCountMap.put(Trip.Status.COMPLETED, tripsComplete);
        tripStatusCountMap.put(Trip.Status.CANCELLED, tripsCancelled);
        tripStatusCountMap.put(Trip.Status.INCOMPLETE, tripsIncomplete);
        Integer tripsTotal = tripsComplete + tripsCancelled + tripsIncomplete;

        // contains all ON taps for trips intending to be COMPLETE.
        List<Map<Batch.TapCSVCols, String>> tripInProgressList = new ArrayList<>();

        CSVFormat tapsCsvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(Batch.TapCSVCols.class)
                .build();
        try (CSVPrinter tapsFile = tapsCsvFormat.print(writer)) {
            long tapIdCounter = 0;
            for (int i = 0; i < tripsTotal; i++) {
                // pick a trip status to create a tap record for
                Trip.Status status = randomPick(tripStatusCountMap.keySet());
                Integer statusCount = tripStatusCountMap.get(status);

                // ensure cols are ordered as they are defined in the enum when
                // Map.values() is called.
                Map<Batch.TapCSVCols, String> record = new TreeMap<>((o1, o2) -> {
                    return o1.ordinal() - o2.ordinal();
                });
                record.put(Batch.TapCSVCols.ID, Long.toString(++tapIdCounter));
                record.put(Batch.TapCSVCols.DateTimeUTC, nextOnDateTime());
                record.put(Batch.TapCSVCols.TapType, Tap.Type.ON.toString());
                record.put(Batch.TapCSVCols.StopId, randomPick(stops));
                record.put(Batch.TapCSVCols.CompanyId, randomPick(companys));
                record.put(Batch.TapCSVCols.BusID, randomPick(busses));
                record.put(Batch.TapCSVCols.PAN, randomPAN());

                tapsFile.printRecord(record.values());

                // status = INCOMPLETE will not be added to the tripInProgressList
                if (status == Trip.Status.COMPLETED) {
                    tripInProgressList.add(record);
                } else if (status == Trip.Status.CANCELLED) {
                    record.put(Batch.TapCSVCols.DateTimeUTC, nextOnDateTime());
                    record.put(Batch.TapCSVCols.TapType, Tap.Type.OFF.toString());
                    tapsFile.printRecord(record.values());
                }

                statusCount--;
                if (statusCount == 0) {
                    // this trip status has completed
                    tripStatusCountMap.remove(status);
                } else {
                    tripStatusCountMap.put(status, statusCount);
                }
            }
            // loop over trips in progress and tap off
            for (Map<Batch.TapCSVCols, String> record : tripInProgressList) {
                record.put(Batch.TapCSVCols.ID, Long.toString(++tapIdCounter));
                record.put(Batch.TapCSVCols.TapType, Tap.Type.OFF.toString());
                record.put(Batch.TapCSVCols.DateTimeUTC, nextOffDateTime());
                // pick a new stop that is not the current one.
                record.compute(Batch.TapCSVCols.StopId, (k, v) -> randomPick(stops, v));
                tapsFile.printRecord(record.values());
            }
        }
    }

    /**
     * Pick a random item from a collection.
     *
     * @param <T> the type of the collection passed in
     * @param pickFrom the collection to select an item from
     * @param excludes When picking an item from the provided list, any of these
     * will not be selected
     * @return An randomly selected item from the provided collection, which
     * will not be any of the provided exclude item(s)
     */
    @SuppressWarnings(value = "unchecked")
    public static <T> T randomPick(Collection<T> pickFrom, T... excludes) {
        ArrayList<T> pickList = new ArrayList<>(pickFrom);
        pickList.removeAll(Arrays.asList(excludes));
        int index = RND.nextInt(pickList.size());
        return pickList.get(index);
    }

    /**
     *
     * @return a randomly generated 16 digit number as a String, pretending to
     * be a credit card number.
     */
    public static String randomPAN() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(RND.nextInt(10));
        }
        return sb.toString();
    }

    public String nextOnDateTime() {
        String formattedTime = DTF.format(instant);
        if (instant.isBefore(MIDDAY)) {
            instant = instant.plusSeconds(1);
        }
        return formattedTime;
    }

    public String nextOffDateTime() {
        if (instant.isBefore(MIDDAY)) {
            instant = MIDDAY.plusSeconds(1);
        }
        String formattedTime = DTF.format(instant);
        if (instant.isBefore(END_OF_DAY)) {
            instant = instant.plusSeconds(1);
        }
        return formattedTime;
    }
}
