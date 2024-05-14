// Samantha Handal - 260983914

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

// Represents a single grid cell in the 3D space
class GridCell {
    private final ReentrantLock lock = new ReentrantLock();
    private SeaCreature occupant;

    // Reentrant lock
    // Ref: https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/locks/ReentrantLock.html
    public void occupy(SeaCreature creature) {

        // block until condition holds
        lock.lock();
        try {

            if (this.occupant == null) {
                this.occupant = creature;
            }

        } finally {
            lock.unlock();
        }
    }    

    public void release() {

        lock.lock();
        try {
            this.occupant = null;
        } finally {
            lock.unlock();
        }
    }

    public boolean isOccupied() {
        lock.lock();
        try {
            return occupant != null;
        } finally {
            lock.unlock();
        }
    }

    public SeaCreature getOccupant() {
        lock.lock();
        try {
            return occupant;
        } finally {
            lock.unlock();
        }
    }

    public ReentrantLock getLock() {
        return lock;
    }
}

// Represents a sea creature
class SeaCreature implements Runnable {

    private int id;
    private int type;
    private int[][] shape;                  // The shape of the creature represented by an array of coordinates
    private GridCell[][][] grid;            // Reference to the grid
    private int[] currentPosition;          // Current position of the creature (x, y, z)
    private Random random = new Random();

    public SeaCreature(int id, int type, int[][] shape, GridCell[][][] grid, int[] currentPosition) {
        this.id = id;
        this.type = type;
        this.shape = shape;
        this.grid = grid;
        this.currentPosition = currentPosition;
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // Sleep for a random time between 10 and 50 milliseconds before attempting to move.
                Thread.sleep(random.nextInt(41) + 10);

                // Determine the new reference coordinate based on the movement attempt.
                int dx = random.nextInt(3) - 1; // Generate -1, 0, or 1 for movement in the x-direction
                int dy = random.nextInt(3) - 1; // Generate -1, 0, or 1 for movement in the y-direction
                int dz = random.nextInt(3) - 1; // Generate -1, 0, or 1 for movement in the z-direction

                // Attempt to move creature
                moveCreature(dx, dy, dz);
                
            // Handle interrupt (e.g., for shutting down the simulation).
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }    

    private void moveCreature(int dx, int dy, int dz) {

        // New position (current position + random offset (-1, 0, 1))
        int newX = currentPosition[0] + dx;
        int newY = currentPosition[1] + dy;
        int newZ = currentPosition[2] + dz;

        // Collect all the cells that will be affected by the move.
        List<GridCell> affectedCells = new ArrayList<>();

        // Add current and target cells to the list
        for (int[] block : shape) {

            int blockX = newX + block[0];
            int blockY = newY + block[1];
            int blockZ = newZ + block[2];

            // Check boundaries, don't even attempt if new position is out of bounds
            if (blockX < 0 || blockY < 0 || blockZ < 0 || blockX >= grid.length || blockY >= grid[0].length || blockZ >= grid[0][0].length) {
                System.out.printf("Creature ID: %d, Type: %d, Previous Coordinate: (%d, %d, %d), Destination Coordinate: (%d, %d, %d)%n",
                id, type, currentPosition[0], currentPosition[1], currentPosition[2], currentPosition[0], currentPosition[1], currentPosition[2]);
                return;
            }

            affectedCells.add(grid[currentPosition[0] + block[0]][currentPosition[1] + block[1]][currentPosition[2] + block[2]]);
            affectedCells.add(grid[blockX][blockY][blockZ]);
        }

        // Attempt to lock all affected cells in a consistent global order to avoid deadlocks
        // Coffman’s condition - dependent cycles, order to help
        affectedCells.sort(Comparator.comparingInt(System::identityHashCode));
        for (GridCell cell : affectedCells) {
            cell.getLock().lock();
        }

        // After locking, check if the move is valid
        if (isValidMove(newX, newY, newZ)) {

            System.out.printf("Creature ID: %d, Type: %d, Previous Coordinate: (%d, %d, %d), Destination Coordinate: (%d, %d, %d)%n",
            id, type, currentPosition[0], currentPosition[1], currentPosition[2], newX, newY, newZ);

            // Execute the move
            for (int[] block : shape) {
                grid[currentPosition[0] + block[0]][currentPosition[1] + block[1]][currentPosition[2] + block[2]].release();
                grid[newX + block[0]][newY + block[1]][newZ + block[2]].occupy(this);
            }

            // update current position to new position
            currentPosition[0] = newX;
            currentPosition[1] = newY;
            currentPosition[2] = newZ;

        // if invalid, don't move
        } else {

            System.out.printf("Creature ID: %d, Type: %d, Previous Coordinate: (%d, %d, %d), Destination Coordinate: (%d, %d, %d)%n",
            id, type, currentPosition[0], currentPosition[1], currentPosition[2], currentPosition[0], currentPosition[1], currentPosition[2]);

        }

        // Release locks
        for (GridCell cell : affectedCells) {
            cell.getLock().unlock();
        }
        
    }

    private boolean isValidMove(int newX, int newY, int newZ) {

        for (int[] block : shape) {

            int blockX = newX + block[0];
            int blockY = newY + block[1];
            int blockZ = newZ + block[2];

            // Check if new area is occupied cube per cube
            if (grid[blockX][blockY][blockZ].isOccupied() && grid[blockX][blockY][blockZ].getOccupant() != this){
                return false;
            }
        }

        // valid position
        return true;
    }
}

public class q1 {

    // sea creature shapes
    private static final int[][][] shapes = {
        {{0, 0, 0}, {0, 0, 1}, {0, 0, 2}},                                              // Shape (1)
        {{1, 1, 0}, {1, 0, 1}, {0, 1, 1}, {1, 1, 1}, {2, 1, 1}, {1, 2, 1}, {1, 1, 2}},  // Shape (2)
        {{0, 0, 0}, {1, 0, 0}, {0, 0, 1}, {0, 0, 2}, {0, 1, 2}},                        // Shape (3)
        {{0, 0, 0}, {2, 0, 0}, {0, 0, 1}, {1, 0, 1}, {2, 0, 1}, {1, 0, 2}}              // Shape (4)
    };

    // Method to check if a creature can be placed at the given coordinates
    private static boolean canPlaceCreature(int[][] shape, int startX, int startY, int startZ, GridCell[][][] grid) {
        for (int[] block : shape) {
            int x = startX + block[0];
            int y = startY + block[1];
            int z = startZ + block[2];
    
            // Check if the block is outside the grid bounds
            if (x < 0 || y < 0 || z < 0 || x >= grid.length || y >= grid[0].length || z >= grid[0][0].length) {
                
                // The shape extends outside the grid
                return false;
            }
    
            // Check if the block is already occupied
            if (grid[x][y][z].isOccupied()) {

                // The shape overlaps with an occupied cell
                return false;
            }
        }

        // The shape fits within the grid and does not overlap with other creatures
        return true;
    }    
    
    public static void main(String[] args) {

        // Collect command line arguments
        if (args.length < 2) {
            System.out.println("Usage: java q1 <k> <n>");
            return;
        }
        
        int k = Integer.parseInt(args[0]);
        int n = Integer.parseInt(args[1]);

        // Initialize the simulation with a 3D grid of size 5 * k
        GridCell[][][] grid = new GridCell[5 * k][5 * k][5 * k];
        for (int x = 0; x < grid.length; x++) {
            for (int y = 0; y < grid[x].length; y++) {
                for (int z = 0; z < grid[x][y].length; z++) {
                    grid[x][y][z] = new GridCell();
                }
            }
        }
        
        // Populate the grid with sea creatures
        Random random = new Random();
        List<SeaCreature> creatures = new ArrayList<>();
        for (int id = 0; id < k; id++) {

            // Round robin creature selection
            int type = id % shapes.length;
            // Flag to continue checking for open start positions
            boolean placed = false;

            // Placing sea creatures in random positions before threads are launched
            while (!placed) {

                // Select random positions
                int startX = random.nextInt(grid.length);
                int startY = random.nextInt(grid[0].length);
                int startZ = random.nextInt(grid[0][0].length);
                
                // Check if random (x,y,z) coordinate is valid
                if (canPlaceCreature(shapes[type], startX, startY, startZ, grid)) {

                    // Create sea creature
                    int[] startPosition = new int[]{startX, startY, startZ};
                    SeaCreature creature = new SeaCreature(id, type, shapes[type], grid, startPosition);
                    
                    // Occupy the cells with the creature (place creature)
                    for (int[] block : shapes[type]) {
                        grid[startX + block[0]][startY + block[1]][startZ + block[2]].occupy(creature);
                    }

                    creatures.add(creature);
                    placed = true;
                }
            }            
        }

        // Start a thread for each sea creature
        List<Thread> threads = new ArrayList<>();
        for (SeaCreature creature : creatures) {
            Thread thread = new Thread(creature);
            threads.add(thread);
            thread.start();
        }

        // Let the simulation run for 'n' seconds
        try {
            TimeUnit.SECONDS.sleep(n);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Stop all creatures' threads after 'n' seconds
        for (Thread thread : threads) {
            thread.interrupt();
        }

        // Wait for all threads to finish
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Simulation complete in " + n + " seconds.");
    }
}