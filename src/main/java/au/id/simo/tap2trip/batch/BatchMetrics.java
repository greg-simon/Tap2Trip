package au.id.simo.tap2trip.batch;

import java.io.PrintStream;

/**
 * A set of batch metrics to be incremented while processing.
 * 
 * Not thread safe.
 */
public class BatchMetrics {
    private long tapReadErrors = 0;
    private long tapsRead = 0;
    private long tripsWritten = 0;
    
    public void incrementTapReadErrors() {
        tapReadErrors++;
    }
    
    public void incrementTapsRead() {
        tapsRead++;
    }
    
    public void incrementTripsWritten() {
        tripsWritten++;
    }

    public long getTapReadErrors() {
        return tapReadErrors;
    }

    public long getTapsRead() {
        return tapsRead;
    }

    public long getTripsWritten() {
        return tripsWritten;
    }
    
    public void printCounts(PrintStream ps) {
        ps.println("Tap records read:    " + tapsRead);
        ps.println("Taps failed to read: " + tapReadErrors);
        ps.println("Trips written:       " + tripsWritten);
    }
}
