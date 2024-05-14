/*
 * Assignment 1, Question 1
 * Samantha Handal - 260983914
 */

public class q1 {
    public static void main(String[] args) {

        // Check if the correct number of arguments is provided + usage instructions
        if (args.length != 2) {
            System.out.println("Usage: java q1 <n> <q>");
            System.out.println("  where:");
            System.out.println("    <n> is the maximum capacity of people in the metro system (n >= 10)");
            System.out.println("    <q> is the time in milliseconds for the overseer to recheck the counts (q >= 10)");
            return;
        }

        // Collect command line arguments
        //  n = Max capacity of the metro
        //  p = Interval for overseer to recheck counts (assume q is evenly divisible by 10)
        int n = Integer.parseInt(args[0]);  
        int q = Integer.parseInt(args[1]); 

        // Additional checks for n and q
        if (n < 10 || q < 10) {
            System.out.println("Error: n and q must be 10 or greater.");
            return;
        }

        // Creating and starting metro station threads
        MetroStation[] stations = new MetroStation[4];
        for (int i = 0; i < stations.length; i++) {
            stations[i] = new MetroStation();

            // Start a new thread for each station
            new Thread(stations[i]).start();
        }

        // Creating the overseer thread
        Overseer overseer = new Overseer(stations, n, q);
        // Start the overseer thread
        new Thread(overseer).start();
    }

    // MetroStation class represents a single metro station
    static class MetroStation implements Runnable {

        // Count of people in the station, initally 0
        private int count = 0;
        // Lock object for synchronization
        private final Object lock = new Object();
        // Flag to control entry, "volatile" used since changes to this variable are immediately visible to all threads
        private volatile boolean allowEntry = true;

        // Run method executes when the station thread starts
        public void run() {

            // Station thread behavior, passenger enters/exits every 10ms (51% enter, 49% leave)
            while (true) {
                try {

                    // Simulate time for a person to enter/leave
                    Thread.sleep(10);

                    // Randomize whether a person enters or leaves
                    boolean isEntering = Math.random() < 0.51;

                    // If person wants to enter and overseer has not blocked entrance, allow person to enter
                    if (isEntering && allowEntry) {

                        // Increment count if a person enters, sync to avoid data race
                        synchronized (lock) {
                            count++;
                        }
                    
                    // If person wants to leave (independent of overseer blocking)
                    } else if (!isEntering) {

                        // Decrement count if a person leaves, sync to avoid data race
                        synchronized (lock) {
                            count--;
                        }
                    }

                // Try-catch loop for debugging
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        // Setter method to control the station's entry rights
        public void setAllowEntry(boolean allow) {
            allowEntry = allow;
        }

        // Method to get the current count of the station, sync to avoid data race
        public int getCount() {
            synchronized (lock) {
                return count;
            }
        }
    }

    // Overseer class represents the overseer of the metro system
    // no synchronizing needed in this class since there is only one overseer thread
    static class Overseer implements Runnable {

        // Initialization of constants for the class, including
        // list of references to all 4 metro stations, max capacity of metro system, and interval for rechecking counts
        private final MetroStation[] stations;
        private final int n;
        private final int q;
        private boolean isReducedCapacity = false;

        public Overseer(MetroStation[] stations, int n, int q) {
            this.stations = stations;
            this.n = n;
            this.q = q;
        }

        // Run method executes when the overseer thread starts
        public void run() {
            try {
                while (true) {
                    
                    // Reset totals at every count
                    int total = 0;

                    // Calculating the total count of all stations
                    System.out.println("Current station counts:");
                    for (int i = 0; i < stations.length; i++) {
                        int stationCount = stations[i].getCount();
                        total += stationCount;

                        // Print station count
                        System.out.println("Station " + (i + 1) + ": " + stationCount);
                    }

                    // Print the total count
                    System.out.println("Total count: " + total);

                    // Logic for handling when total count >= n
                    // If total ≥ n, disallow entry, only allow exit, and re-check every q/10 ms
                    if (total >= n && !isReducedCapacity) {

                        // Disallow entry in all stations if total count reaches or exceeds n
                        for (int i = 0; i < stations.length; i++) {
                            stations[i].setAllowEntry(false);
                        }

                        // Set reduced capacity flag to true
                        isReducedCapacity = true;
                        // Increase frequency of rechecking
                        Thread.sleep(q / 10);

                    // Allow entry in all stations if total count is below 0.75n
                    } else if (total < Math.floor(0.75 * n) && isReducedCapacity) {
                        for (int i = 0; i < stations.length; i++) {
                            stations[i].setAllowEntry(true);
                        }

                        // Set reduced capacity flag to true
                        isReducedCapacity = false;
                        // Return to normal frequency of rechecking
                        Thread.sleep(q); 

                    // Maintain current state, set sleep based of reduced capacity flag
                    } else {
                        if (isReducedCapacity) {
                            Thread.sleep(q / 10);
                        } else {
                            Thread.sleep(q);
                        }
                    }
                }

            // Try-catch loop for debugging
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}