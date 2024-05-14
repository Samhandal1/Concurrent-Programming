import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Random;

public class q2 {

    // Main class for running stack operation tests
    public static void main(String[] args) {
        if (args.length != 6) {
            System.out.println("Usage: java q2 x t n s e w");
            System.exit(1);
        }

        int x = Integer.parseInt(args[0]);
        int t = Integer.parseInt(args[1]);
        int n = Integer.parseInt(args[2]);
        int s = Integer.parseInt(args[3]);
        int e = Integer.parseInt(args[4]);
        int w = Integer.parseInt(args[5]);

        Stack stack;
        // x is a 0 if a regular lock-free stack is used
        if (x == 0) {
            stack = new LockFreeStack();

        // and 1 if an elimination stack is to be used
        } else {
            stack = new EliminationStack(e, w);
        }

        // Initialize the t threads and create t StackTesters
        Thread[] threads = new Thread[t];
        for (int i = 0; i < t; i++) {
            threads[i] = new Thread(new StackTester(stack, n, s));
        }

        // Start timer
        long startTime = System.currentTimeMillis();

        // Start t all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait until all threads finish
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        // record and print time & size
        long endTime = System.currentTimeMillis();
        System.out.println((endTime - startTime) + " " + stack.size());
    }
}

// Stack interface defining basic stack operations
interface Stack {
    void push(Node newHead);
    Node pop();
    int size();
}

// Node structure
class Node {
    final int item;
    volatile Node next;

    public Node(int item) {
        this.item = item;
        this.next = null;
    }
}

// Helper class to break out of loop early if stack is empty
class EmptyStackException extends Exception {
    public EmptyStackException(String message) {
        super(message);
    }
}

// Implementation of a lock-free stack using atomic references
class LockFreeStack implements Stack {

    // Top node of the stack
    private AtomicReference<Node> top = new AtomicReference<>(null);

    // Current size of the stack
    private AtomicReference<Integer> size = new AtomicReference<>(0);

    // Pop and push taken from class notes
    public void push(Node newHead) {
        Node oldHead;
        do {
            oldHead = top.get();
            newHead.next = oldHead;

        // Attempt to set the new node as the new top of the stack. This operation is atomic.
        // If the top has not changed since retrieving it (still equals oldHead), the new node is successfully set as top.
        // If the top has changed (due to another thread's operation), the loop retries the operation with the updated top.
        } while (!top.compareAndSet(oldHead, newHead));

        // Increment the size of the stack atomically after successfully adding the new node.
        size.getAndSet(size.get() + 1);
    }

    public Node pop() {
        Node oldHead;
        Node newHead;
        do {
            oldHead = top.get();

            // Check if the stack is empty. If it is, return null indicating there's nothing to pop.
            if (oldHead == null) {
                return null;
            }
            newHead = oldHead.next;

        // Attempt to set the newHead as the top of the stack. This operation is atomic.
        // If the top has not changed since retrieving it (still equals oldHead), the top is successfully updated.
        // If the top has changed (due to another thread's operation), the loop retries the operation with the updated top.
        } while (!top.compareAndSet(oldHead, newHead));

        // Decrement the size of the stack atomically after successfully removing the node.
        size.getAndSet(size.get() - 1);
        return oldHead;
    }

    // tryPush and tryPop have the same logic as Push and Pop but tested once
    // rather than in a loop (for the EliminationStack class)

    public boolean tryPush(Node newNode) {
        Node oldHead = top.get();
        newNode.next = oldHead;

        // Try to set the new node as the new head of the stack
        // Increment size if push was successful
        if (top.compareAndSet(oldHead, newNode)) {
            size.getAndSet(size.get() + 1);
            return true;
        }

        // If the atomic operation fails (likely due to another thread modifying the top),
        // return false indicating the push operation was unsuccessful due to contention.
        return false;
    }

    public Node tryPop() throws EmptyStackException {
        Node oldHead = top.get();

        // If the stack is empty, throw an EmptyStackException. This exception is used to immediately 
        // indicate to the caller that the stack is empty, which is useful in EliminationStack where we 
        // would break the while loop upon an empty stack so not to be confused with a failed operation.
        if (oldHead == null) {
            throw new EmptyStackException("Stack is empty"); 
        }

        Node newHead = oldHead.next;

        // Try to update the top to the next node, effectively popping the current top.
        // Decrement size if pop was successful.
        if (top.compareAndSet(oldHead, newHead)) {
            size.getAndSet(size.get() - 1); 
            return oldHead;
        }
        
        // If the atomic operation fails (likely due to another thread modifying the top),
        // return false indicating the push operation was unsuccessful due to contention.
        return null;
    }

    // helper to get the size of the stack
    public int size() {
        return size.get();
    }
}

// Logic taken from the text book: The Art of Multiprocessor Program- ming (second edition), Chapter 11
class EliminationStack extends LockFreeStack {

    // Elimination array
    private final Exchanger<Node>[] exchangers;

    // Timeout for the elimination array
    private final int timeout;
    private final Random random = new Random();

    @SuppressWarnings("unchecked")
    public EliminationStack(int capacity, int timeout) {
        this.exchangers = (Exchanger<Node>[]) new Exchanger[capacity];

        // Initialize elimination/exchanger array to size e (capacity)
        for (int i = 0; i < capacity; i++) {
            exchangers[i] = new Exchanger<>();
        }
        this.timeout = timeout;
    }

    @Override
    public void push(Node node) {

        while (true) {

            // Attempt normal push operation first.
            if (super.tryPush(node)) {
                return;

            // If normal push fails, try the elimination array
            } else try {
                
                // Attempt the exchange operation with the determined value (using Java's exchanger)
                // Source: https://www.geeksforgeeks.org/java-util-concurrent-exchanger-class-with-examples/
                int slot = random.nextInt(exchangers.length);
                Node result = exchangers[slot].exchange(node, timeout, TimeUnit.MILLISECONDS);

                // Successfully exchanged a pop with a push
                if (result == null) {
                    return;
                }

            // Handle the case where the current thread was interrupted during exchange
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;

            // Handle the case where the timeout expires before an exchange could be made.
            } catch (java.util.concurrent.TimeoutException e) {
                return;
            }
        }
    }

    @Override
    public Node pop() {

        while (true) {

            try {

                // Try to pop
                Node node = super.tryPop();

                // If node is not null, then normal pop successful
                if (node != null) { 
                    return node;

                // Try to exchange (pop) through the elimination array.
                } else try {
                
                    // Attempt the exchange operation with the determined value (using Java's exchanger)
                    // Source: https://www.geeksforgeeks.org/java-util-concurrent-exchanger-class-with-examples/
                    int slot = random.nextInt(exchangers.length);
                    Node result = exchangers[slot].exchange(null, timeout, TimeUnit.MILLISECONDS);
        
                    // Successfully exchanged a pop with a push
                    if (result != null) {
                        return result;
                    }
        
                // Handle the case where the current thread was interrupted during exchange
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
        
                // Handle the case where the timeout expires before an exchange could be made.
                } catch (java.util.concurrent.TimeoutException e) {
                    return null;
                }

            // Stack was empty
            } catch (EmptyStackException e) {
                return null;
            }

        }
    }
}

class StackTester implements Runnable {
    private final Stack stack;
    private final int operations;
    private final int sleepTime;
    private final ArrayList<Node> poppedNodes = new ArrayList<>();
    private final Random random = new Random();

    public StackTester(Stack stack, int operations, int sleepTime) {
        this.stack = stack;
        this.operations = operations;
        this.sleepTime = sleepTime;
    }

    @Override
    public void run() {

        // execute n operations
        for (int i = 0; i < operations; i++) {

            // Push
            if (random.nextBoolean()) {

                // Reuse a popped node 50% of the time if available.
                if (!poppedNodes.isEmpty() && random.nextBoolean()) {
                    Node reuseNode = poppedNodes.get(0);
                    stack.push(reuseNode);
                    poppedNodes.remove(reuseNode);
                    
                // Push a new random int value.
                } else {
                    Node newHead = new Node(random.nextInt(1000));
                    stack.push(newHead);
                }

            // Pop
            } else {
                Node poppedValue = stack.pop();

                // Immediately set the object’s next field to be null
                if (poppedValue != null) {
                    poppedValue.next = null;

                    // Store the popped value if it's not indicating an empty stack
                    if (poppedNodes.size() <= 50) {
                        poppedNodes.add(poppedValue);
                    }
                }
            }

            // Thread sleeps for a random time, 0–s ms
            try {
                Thread.sleep(random.nextInt(sleepTime));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}