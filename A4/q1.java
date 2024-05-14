// Samantha Handal - 260983914
import java.util.concurrent.*;

class q1  {

    private static ForkJoinPool pool;

    // note: to test very large number inputs (on linux), invoke as follows
    // (replacing 10000 with as many digits as you want):
    // java q1 `tr -dc "[:digit:]" < /dev/urandom | head -c 10000` `tr -dc "[:digit:]" < /dev/urandom | head -c 10000`

    public static void main(String[] args) {

        // Parse arguments: Your program should accept 3 parameters, consisting of first the number of threads 
        // to use in the pool (≥ 1), and then the two numbers in base-10.
        if (args.length < 3) {
            System.err.println("Usage: java q1 <numThreads> <number1> <number2>");
            System.exit(1);
        }

        int numThreads = Integer.parseInt(args[0]);
        String x = args[1];
        String y = args[2];

        // Initialize ForkJoinPool with numThreads
        pool = new ForkJoinPool(numThreads);

        // Start timer
        long startTime = System.nanoTime();

        // Start task
        String result = pool.invoke(new KaratsubaTask(x, y));

        // Stop timer and calculate elapsed time
        long endTime = System.nanoTime();
        long elapsedTime = endTime - startTime;

        // Convert nanoseconds to milliseconds
        double elapsedTimeInMs = elapsedTime / 1_000_000.0;
        
        // System.out.println("Input, x: " + x + ", y: " + y);
        // System.out.println("Product: " + prune(result));
        System.out.println("Execution time: " + elapsedTimeInMs + " ms");
    }

    // Karatsuba multiplication task for ForkJoinPool
    private static class KaratsubaTask extends RecursiveTask<String> {

        private String x;
        private String y;

        public KaratsubaTask(String x, String y) {
            this.x = x;
            this.y = y;
        }

        // Helper function to preform left shifts (10^2i and 10^i)
        private static String shiftLeft(String num, int positions) {

            // Check if the number is "0" since shifting "0" any number of positions should still give "0"
            if ("0".equals(num) || positions <= 0) {
                return num;
            }

            // Append "positions" number of 0s to the input number
            StringBuilder sb = new StringBuilder(num);
            for (int i = 0; i < positions; i++) {
                sb.append('0');
            }
            return sb.toString();
        }        

        @Override
        protected String compute() {

            // Input numbers are pruned to remove leading zeros
            x = prune(x);
            y = prune(y);

            // Base case: multiplying two single-digit numbers
            if (x.length() < 2 && y.length() < 2) {
                return Integer.toString(Integer.parseInt(x) * Integer.parseInt(y));
            }

            // Ensure n is half the length of the longer number, rounded up
            int n = (Math.max(x.length(), y.length()) + 1) / 2;

            // Splitting the numbers: If the length is not greater than n, the number doesn't have enough digits to split 
            // and the high part (H) is set to "0" because all digits fall into the low part (L)

            // Safe splitting for x
            String xH, xL;
            if (x.length() <= n) {
                xH = "0"; // High part is "0" if x is shorter than or equal to n
                xL = x;   // Low part is x itself
            } else {
                xH = x.substring(0, x.length() - n);
                xL = x.substring(x.length() - n);
            }

            // Safe splitting for y
            String yH, yL;
            if (y.length() <= n) {
                yH = "0"; // High part is "0" if y is shorter than or equal to n
                yL = y;   // Low part is y itself
            } else {
                yH = y.substring(0, y.length() - n);
                yL = y.substring(y.length() - n);
            }

            // Recursive tasks for Karatsuba multiplication
            KaratsubaTask aTask = new KaratsubaTask(xH, yH);                        // a = xH * yH
            KaratsubaTask bTask = new KaratsubaTask(xL, yL);                        // b = xL * yL
            KaratsubaTask cTask = new KaratsubaTask(add(xH, xL), add(yH, yL));      // c = (xH + xL) * (yH + yL)

            // Parallel execution
            aTask.fork();
            bTask.fork();

            // c = (xH + xL)(yH + yL) − a − b
            String a, b;
            String c = sub(sub(cTask.compute(), a = aTask.join()), b = bTask.join());

            // Assembling the final result based on the Karatsuba formula: x*y = a*10^2i + c*10^i + b
            // a*10^2i = append 2i zeros to a, c*10^i = append i zeros to c
            String result = add(add(shiftLeft(a, (2 * n)), shiftLeft(c, (n))), b);

            // Remove any leading zeros from the result
            return prune(result); 
        }

    }

    // returns the sum of 2 base-10 integers expressed as non-empty strings, perhaps with a leading "-".
    // e.g., add("0010","-9301") returns "-9291"
    // nb: returned string may have excess leading 0s.
    public static String add(String x,String y) {
        String r = "";
        if (x.charAt(0)=='-') {
            if (y.charAt(0)=='-') {
                // -x + -y === - (x+y)
                r = '-' + add(x.substring(1),y.substring(1));
                return r;
            }
            // -x + y === y - x
            r = sub(y,x.substring(1));
            return r;
        } else if (y.charAt(0)=='-') {
            // x + -y === x - y
            r = sub(x,y.substring(1));
            return r;
        }

        // can assume both positive here

        // make sure same length
        int slen = x.length();
        if (y.length()!=slen) {
            slen = (y.length() > slen) ? y.length() : slen;
            x = pad(x,slen);
            y = pad(y,slen);
        }
        int carry = 0;
        for (int i=x.length()-1;i>=0;i--) {
            int sum = Character.getNumericValue(x.charAt(i))+Character.getNumericValue(y.charAt(i))+carry;
            if (sum>=10) {
                sum -= 10;
                carry = 1;
            } else {
                carry = 0;
            }
            r = sum + r;
        }
        if (carry!=0)
            r = "1" + r;
        return r;
    }

    // returns the difference between 2 base-10 integers expressed as non-empty strings, perhaps with a leading "-".
    // e.g., sub("0010","-9301") returns "9311"
    // nb: returned string may have excess leading 0s.
    static String sub(String x,String y) {
        String r = "";
        if (x.charAt(0)=='-') {
            if (y.charAt(0)=='-') {
                // -x - -y  === -x + y  === y - x
                r = sub(y.substring(1),x.substring(1));
                return r;
            }
            // -x - y === - (x+y)
            r = add(x.substring(1),y);
            if (r.length()>0 && r.charAt(0)!='-')
                r = "-" + r;
            return r;
        } else if (y.charAt(0)=='-') {
            // x - -y === x + y
            r = add(x,y.substring(1));
            return r;
        }

        int slen = x.length();
        if (y.length()!=slen) {
            slen = (y.length() > slen) ? y.length() : slen;
            x = pad(x,slen);
            y = pad(y,slen);
        }
        int borrow = 0;
        for (int i=x.length()-1;i>=0;i--) {
            int diff = Character.getNumericValue(x.charAt(i))-borrow-Character.getNumericValue(y.charAt(i));
            //System.out.println("sum of "+x.charAt(i)+"+"+y.charAt(i)+"+"+carry+" = "+sum);
            if (diff<0) {
                borrow = 1;
                diff += 10;
            } else {
                borrow = 0;
            }
            r = diff + r;
        }
        if (borrow!=0) { // flip it around and try again
            r = "-"+sub(y,x);
        }
        return r;
    }

    // remove unnecessary leading 0s from a base-10 number expressed as a string.
    public static String prune(String s) {
        if (s.charAt(0)=='-') return "-"+prune(s.substring(1));
        s = s.replaceFirst("^00*","");
        if (s.length()==0) return "0";
        return s;
    }

    // add leading 0s to a base-10 number expressed as a string to ensure the string
    // is of the length given.
    // nb: assumes a positive number input.
    public static String pad(String s,int n) {
        return String.format("%"+n+"s",s).replace(' ','0');
    }

}