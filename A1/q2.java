/*
 * Assignment 1, Question 2
 * Samantha Handal - 260983914
 */

import java.awt.image.*;
import java.io.*;
import java.util.concurrent.ThreadLocalRandom;
import javax.imageio.*;

public class q2 {

    // an input image
    public static BufferedImage imgin1;

    // output image
    public static BufferedImage imgout;

    // Array of locks for each image segment
    public static Object[] stripeLocks;

    // parameters and their default values
    public static String imagebase = "cat" ; // base name of the input images; actual names append "1.png", "2.png" etc.
    public static int threads = 1; // number of threads to use
    public static int outputheight = 4096; // output image height
    public static int outputwidth = 4096; // output image width
    public static int attempts = 20; // number of failed attempts before a thread gives up

    // print out command-line parameter help and exit
    public static void help(String s) {
        System.out.println("Could not parse argument \""+s+"\".  Please use only the following arguments:");
        System.out.println(" -i inputimagebasename (string; current=\""+imagebase+"\")");
        System.out.println(" -h outputimageheight (integer; current=\""+outputheight+"\")");
        System.out.println(" -w outputimagewidth (integer; current=\""+outputwidth+"\")");
        System.out.println(" -a attempts (integer value >=1; current=\""+attempts+"\")");
        System.out.println(" -t threads (integer value >=1; current=\""+threads+"\")");
        System.exit(1);
    }

    // process command-line options
    public static void opts(String[] args) {
        int i = 0;

        try {
            for (;i<args.length;i++) {

                if (i==args.length-1)
                    help(args[i]);

                if (args[i].equals("-i")) {
                    imagebase = args[i+1];
                } else if (args[i].equals("-h")) {
                    outputheight = Integer.parseInt(args[i+1]);
                } else if (args[i].equals("-w")) {
                    outputwidth = Integer.parseInt(args[i+1]);
                } else if (args[i].equals("-t")) {
                    threads = Integer.parseInt(args[i+1]);
                } else if (args[i].equals("-a")) {
                    attempts = Integer.parseInt(args[i+1]);
                } else {
                    help(args[i]);
                }
                // an extra increment since our options consist of 2 pieces
                i++;
            }
        } catch (Exception e) {
            System.err.println(e);
            help(args[i]);
        }
    }

    // Helper method to check if an icon can be placed at a given position
    private static boolean canPlaceIcon(BufferedImage img, BufferedImage icon, int x, int y) {
       
        // 1 - Check if icon goes outside bounds
        // when placed at coordinates (x, y), checks if icon would extend beyond the right or bottom edges of the main image.
        if (x + icon.getWidth() > img.getWidth() || y + icon.getHeight() > img.getHeight()) {
            return false;
        }

        // 2 - Check for overlap with existing icons
        // Check if the pixel at (i, j) in the icon is not transparent and if it would overlap with a non-transparent pixel in the main image.
        for (int i = 0; i < icon.getWidth(); i++) {
            for (int j = 0; j < icon.getHeight(); j++) {

                // If icon pixel is not transparent and overlaps existing pixel, cannot place icon
                if ((icon.getRGB(i, j) & 0xFF000000) != 0x00 && (img.getRGB(x + i, y + j) & 0xFF000000) != 0x00) {
                    return false;
                }
            }
        }

        // If no overlapping or out-of-bound issues are found, return true
        return true;
    }

    // Task each thread executes, loading an icon and trying to place it at a random position in the output image. 
    // If the position leads to an overlap, it tries another position, up to a maximum number of attempts.
    static class IconPlacer implements Runnable {

        // Thread number (used to determine which icon to load)
        private final int threadNum;
        // Reference to the shared output image
        private final BufferedImage outputImage;
        // Maximum number of attempts to place the icon
        private final int maxAttempts;
    
        IconPlacer(int threadNum, BufferedImage outputImage, int maxAttempts) {
            this.threadNum = threadNum;
            this.outputImage = outputImage;
            this.maxAttempts = maxAttempts;
        }
    
        public void run() {
            try {

                // Load the icon specific to this thread, "imagebase" specifies the basename of these files
                // (so if the basename is “cat” then thread 1 loads “cat1.png”, thread 2 loads “cat2.png” etc.)
                BufferedImage icon = ImageIO.read(new File(imagebase + threadNum + ".png"));

                // Counter for the number of failed placement attempts
                int failedAttempts = 0;
    
                // Continue trying to add icon until the maximum number of failures is reached
                while (failedAttempts < maxAttempts) {

                    // Generate random coordinates for placement (top left corner)
                    // "the reason for the poor performance of java.util.Random in a multi-threaded environment
                    // is due to contention – given that multiple threads share the same Random instance."
                    // https://www.baeldung.com/java-thread-local-random
                    int x = ThreadLocalRandom.current().nextInt(outputImage.getWidth());
                    int y = ThreadLocalRandom.current().nextInt(outputImage.getHeight());
    
                    // Synchronize access to the shared output image...
                    // Each thread, when trying to place an icon, first determines which stripe it will be working on based on 
                    // the x-coordinate of the icon placement. It then acquires the lock for that specific stripe or is blocked
                    // if another thread is working on that segment. This means that multiple threads can operate concurrently as 
                    // long as they are working in different stripes.

                    // Determine the stripe
                    int stripeIndex = (x * stripeLocks.length) / outputImage.getWidth();

                    // Place icon on master image if segment is not locked
                    synchronized (stripeLocks[stripeIndex]) {

                        // Check if the icon can be placed at the generated position
                        if (canPlaceIcon(outputImage, icon, x, y)) {

                            // Place the icon, copy the image for the annotated output (from starter code)
                            for (int i = 0; i < icon.getWidth(); i++) {
                                for (int j = 0; j < icon.getHeight(); j++) {

                                    // When placing an icon with transparent elements on the output image, we want to copy only the
                                    // actual icon (the non-transparent part) and leave the rest of the output image as it is
                                    if ((icon.getRGB(i, j) & 0xFF000000) != 0x00) {
                                        outputImage.setRGB(x + i, y + j, icon.getRGB(i, j));
                                    }
                                }
                            }

                            // Reset failed attempts after a successful placement
                            failedAttempts = 0;
                        
                        // Increment failed attempts if the icon cannot be placed
                        } else {
                            failedAttempts++; 
                        }
                    }
                }

            // Handle exceptions if icon file cannot be loaded
            } catch (IOException e) {
                System.err.println("Thread " + threadNum + " failed to load icon: " + e.getMessage());
            }
        }
    }    

    // main.  we allow an IOException in case the image loading/storing fails.
    public static void main(String[] args) throws IOException {
        // process options
        opts(args);

        // create an output image
        BufferedImage outputimage = new BufferedImage(outputwidth, outputheight, BufferedImage.TYPE_INT_ARGB);

        // Find the maximum width of the icons
        int maxIconWidth = 0;
        for (int i = 1; i <= threads; i++) {
            BufferedImage icon = ImageIO.read(new File(imagebase + i + ".png"));
            maxIconWidth = Math.max(maxIconWidth, icon.getWidth());
        }

        // Adjust the number of stripes based on the maximum icon width
        int stripes = Math.max(threads, outputwidth / maxIconWidth);

        // Initialize stripe locks
        // To promote concurrency, divide the image into several vertical stripes and assign a lock to each stripe
        // This way, when a thread wants to place an icon, it needs to acquire the lock only for the stripe it's working on.
        // Number of stripes should be equal to or greater than the number of threads to minimize contention.
        stripeLocks = new Object[stripes];
        for (int i = 0; i < stripes; i++) {
            stripeLocks[i] = new Object();
        }

        // Create an array to hold the threads
        Thread[] threadArray = new Thread[threads];
        
        // Start timing
        long startTime = System.currentTimeMillis();

        // Create and start threads
        for (int i = 0; i < threads; i++) {
            threadArray[i] = new Thread(new IconPlacer(i + 1, outputimage, attempts));
            threadArray[i].start();
        }

        // Wait for all threads to finish
        for (int i = 0; i < threads; i++) {
            try {
                threadArray[i].join();
            } catch (InterruptedException e) {
                System.out.println("Thread interrupted: " + e.getMessage());
            }
        }

        // Calculate and print the total time taken
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("Total time: " + totalTime + " ms");

        // Write out the image
        File outputfile = new File("outputimage.png");
        ImageIO.write(outputimage, "png", outputfile);
    }
}