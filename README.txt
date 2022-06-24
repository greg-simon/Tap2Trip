Building:
====
    mvn clean package

Testing:
====
Apart from unit testing, there is a load test of ~5 million taps. This can be
run using the following command, but does require a 3gb heap size.

    mvn exec:exec
    

Running:
====
    java -jar target/Tap2Trip-1.0-SNAPSHOT-jar-with-dependencies.jar taps.csv trips.csv errors.csv 

Where the three arguments are:
    1. The taps.csv file to read.
    2. The trips.csv file to write to.
    3. The errors.csv file to write taps that could not be read.


Assumptions:
====
* Tap data will be in date time order. i.e ONs will be listed before OFFs for
  each trip.

* This batch job will be run at the end of each day/period. Otherwise tap state
  will require to be persisted between runs.

* It is acceptable for a bus enthusiast to ride the same Bus for as long as it
  takes to loop back to the same stop and count that as a CANCELLED trip and not
  charge anything.
   i.e. The duration is not checked for a CANCELLED trip.
  
* OFF taps with no ON taps for a particular PAN is an INCOMPLETE Trip.

* ON taps with an existing ON tap for a particular PAN is an INCOMPLETE Trip for
  the first ON tap.

Limitations:
====
* In progress trip states are held in memory, if there are an excessive number
  of trips in progress it might be possible to run out of memory. Either memory
  capacity is planned and tested with a formula such as:
        Companies Max Bus Fleet Seat Capacity
      X (Tap object size in bytes + java.util.Map overhead in bytes)
      + Rest of application overhead
      + ~10% margin of error
      ----------------------
      Required Process Memory Size

  Alternatively, this trip state could be stored in a database in order to
  offload to disk storage.

* Money implementation is naively implemented as integer cents. It does not deal
  with different currencies, or fractions of cents. But it will not suffer from
  float rounding errors.

