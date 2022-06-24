package au.id.simo.tap2trip.batch;

import java.io.IOException;
import java.io.Writer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/**
 * Writes Tap parsing and processing errors.
 * 
 * Format is a CSV with the following columns:
 * <pre>
 * Record No.,Message
 * </pre>
 */
public class TapErrorCsvWriter {
    
    private final CSVPrinter errorCsvPrinter;
    
    public TapErrorCsvWriter(Writer writer) throws IOException {
        CSVFormat tapsErrFormat = CSVFormat.DEFAULT.builder()
                .setHeader("Record No.", "Message")
                .build();
        errorCsvPrinter = tapsErrFormat.print(writer);
    }
    
    public void writeError(long recordNumber, Throwable exception) throws IOException{
        errorCsvPrinter.printRecord(
                recordNumber,
                buildExceptionMessage(exception)
        );
    }
    
    /**
     * Builds a one line message from a thrown exception that also includes all
     * causedBy exceptions in the chain.
     * 
     * @param ex the exception to make the message from.
     * @return the concatenated error messages of all exceptions in the caused
     * by chain.
     */
    private static String buildExceptionMessage(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        Throwable currEx = ex;
        do {
            sb.append(currEx.getMessage());
            sb.append(": ");
            currEx = currEx.getCause();
        } while(currEx != null);
        sb.delete(sb.length()-2, sb.length());
        return sb.toString();
    }
}
