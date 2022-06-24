package au.id.simo.tap2trip.batch;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Load test with random data.
 */
public class BatchLT {
    
    /**
     * Main method entry point.
     * 
     * @param args 
     */
    public static void main(String[] args) throws IOException {
        new BatchLT().testLoad();
    }
    
    public void testLoad() throws IOException {
        // generate data
        // 
        BulkTapDataGenerator gen = new BulkTapDataGenerator();
        Path genDataCsv = Paths.get("generated-taps.csv");
        Path tripsCsv = Paths.get("trips.csv");
        Path errorCsv = Paths.get("error.csv");
        
        System.out.print("Generating 5,002,000 taps...");
        try (Writer writer = new FileWriter(genDataCsv.toFile())){
            gen.generateTaps(
                    5_000_000,
                    1000,
                    1000,
                    writer);
        }
        System.out.println("Done");
        
        System.out.println("Running Batch....");
        Batch.main(new String[]{genDataCsv.toString(), tripsCsv.toString(), errorCsv.toString()});
        System.out.println("Done");

        if (Files.deleteIfExists(genDataCsv)) {
            System.out.println("Deleted " + genDataCsv);
        }
        if (Files.deleteIfExists(tripsCsv)) {
            System.out.println("Deleted " + tripsCsv);
        }
        if (Files.deleteIfExists(errorCsv)) {
            System.out.println("Deleted " + errorCsv);
        }
    }
}
