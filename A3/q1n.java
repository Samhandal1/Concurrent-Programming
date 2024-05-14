// Samantha Handal - 260983914

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;

public class q1n {

    static final int PAUSE_TIME = 20;                                           // milliseconds to pause after each move
    static AtomicInteger totalMoves = new AtomicInteger(0);        // AtomicInteger for move counting
    static AtomicInteger spawnPointIndex = new AtomicInteger(0);
    static final List<Character> characters = new ArrayList<>();                // List of all characters
    static final List<Thread> characterThreads = new ArrayList<>();             // Store threads for each character


    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java q1n <m> <n> <s>");
            System.exit(1);
        }

        int m = Integer.parseInt(args[0]);
        int n = Integer.parseInt(args[1]);
        int s = Integer.parseInt(args[2]);

        GameMap gameMap = new GameMap(m, n);

        // Initially spawn characters
        for (int i = 0; i < n; i++) {
            spawnCharacter(gameMap);
        }

        // Start all character threads at once
        for (Thread thread : characterThreads) {
            thread.start();
        }

        // Periodically spawn new characters
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(s);
                    spawnCharacter(gameMap);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();

        // Let the simulation run for approximately 10 seconds and then stop
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Stop all characters safely if needed
        synchronized (characters) {
            characters.forEach(Character::stop);
        }

        // End execution and print total moves
        System.out.println("Total successful moves: " + totalMoves.get());
        System.exit(0);
    }

    // Synchronized method for safely adding a character
    public static synchronized void addCharacter(Character character) {
        characters.add(character);
    }

    // Synchronized method for safely removing a character
    public static synchronized void removeCharacter(Character character) {
        characters.remove(character);
    }

    // Method to spawn a new character and add it to the simulation
    private static void spawnCharacter(GameMap gameMap) {
        int index = spawnPointIndex.getAndIncrement();
        Point spawnPoint = gameMap.getSpawnPoint(index);
        Character character = new Character(spawnPoint, gameMap);
        addCharacter(character);
        Thread characterThread = new Thread(character);
        characterThreads.add(characterThread);
    }

    // Character class that implements Runnable for concurrent execution
    static class Character implements Runnable {
        private Point currentLocation;
        private final GameMap gameMap;
        private volatile boolean running = true;

        public Character(Point spawnPoint, GameMap gameMap) {
            this.currentLocation = spawnPoint;
            this.gameMap = gameMap;
        }

        @Override
        public void run() {

            // Character's behavior loop
            while (running) {

                // Choose a random goal point no more than 10 cells away from their current location
                Point destination = gameMap.getRandomDestination(currentLocation);

                // Builds a movement plan consisting of the ordered sequence of steps they plan to make
                List<Point> path = BresenhamLine(currentLocation, destination);

                // Move along the planned path (enacts that plan)
                for (Point step : path) {

                    // Check if the character is still running (breakpoint for termination)
                    if (!running) break;

                    // Attempt to move the character to the next step
                    boolean moved = gameMap.moveCharacter(this, step);
                    if (moved) {
                        currentLocation = step;         // Update current location
                        totalMoves.incrementAndGet();   // Increment total moves

                        // After each step in their plan a character pauses for 20 ms before making another step
                        try {
                            Thread.sleep(PAUSE_TIME);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
        }

        // Method to stop the character's execution
        public void stop() {
            running = false;
        }
    }

    // Dummy placeholder for the GameMap class
    static class GameMap {
        private final int size;
        private final List<Point> spawnPoints = new ArrayList<>();

        // Use ConcurrentHashMap over regular hashmap to reduce scope of syncronization
        private final ConcurrentHashMap<Point, Character> occupiedPoints = new ConcurrentHashMap<>();

        // Init game map & generate initial unique spawn points
        public GameMap(int size, int n) {
            this.size = size;
            generateUniqueSpawnPoints(n);
        }

        // Method to generate unique spawn points on the map's perimeter:
        // There are n spawn points, each of which appear on the perimeter of the map
        private void generateUniqueSpawnPoints(int n) {
            Set<Point> uniquePoints = new HashSet<>();
            Random random = new Random();

            // Randomly select an edge and generate a point on that edge
            while (uniquePoints.size() < n) {
                int edge = random.nextInt(4);
                int x = 0, y = 0;
                switch (edge) {
                    case 0: // Top edge
                        x = random.nextInt(this.size);
                        y = 0;
                        break;
                    case 1: // Right edge
                        x = this.size - 1;
                        y = random.nextInt(this.size);
                        break;
                    case 2: // Bottom edge
                        x = random.nextInt(this.size);
                        y = this.size - 1;
                        break;
                    case 3: // Left edge
                        x = 0;
                        y = random.nextInt(this.size);
                        break;
                }
                uniquePoints.add(new Point(x, y));
            }
            spawnPoints.addAll(uniquePoints);
        }

        // Helper method to get a spawn point based on an index
        public Point getSpawnPoint(int index) {
            return spawnPoints.get(index % spawnPoints.size());
        }

        // Method to generate a random destination within 10 steps of the current location
        public Point getRandomDestination(Point currentLocation) {

            Random random = new Random();

            // Determine bounds for the random destination within 10 steps, need to 
            // make sure that the destination does not exit the game map boundaries.
            int minX = Math.max(0, currentLocation.x - 10);
            int maxX = Math.min(this.size - 1, currentLocation.x + 10);
            int minY = Math.max(0, currentLocation.y - 10);
            int maxY = Math.min(this.size - 1, currentLocation.y + 10);

            // Generate random destination within bounds
            int destinationX = minX + random.nextInt(maxX - minX + 1);
            int destinationY = minY + random.nextInt(maxY - minY + 1);

            return new Point(destinationX, destinationY);
        }

        // Method to attempt moving a character to a next step
        public boolean moveCharacter(Character character, Point nextStep) {

            // Check if next step is within the map bounds
            if (nextStep.x < 0 || nextStep.x >= size || nextStep.y < 0 || nextStep.y >= size) {
                return false;
            }
    
            // Collision detection
            Character existingCharacter = occupiedPoints.get(nextStep);                
            if (existingCharacter != null) {

                // Handle collision, remove both characters from the map
                occupiedPoints.remove(character.currentLocation);
                occupiedPoints.remove(existingCharacter.currentLocation);
                removeCharacter(character);
                removeCharacter(existingCharacter);

                // Stop both characters threads
                character.stop();
                existingCharacter.stop();
                return false;

            // Move character to next step
            } else {
                occupiedPoints.remove(character.currentLocation);   // Remove old position
                occupiedPoints.put(nextStep, character);            // Mark new position as occupied
                character.currentLocation = nextStep;               // Update character's current location
                return true;
            }
        }
    }

    // Class representing points on the map: (x,y) coordinates 
    static class Point {
        public final int x, y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        // must override equals() and hashCode() methods when we use 
        // custom objects like Point as keys in a HashMap...

        // the HashMap uses the object's memory address to determine equality, 
        // which means that two Point objects with the same x and y values are not 
        // considered equal unless they are the exact same instance.

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Point point = (Point) o;
            return x == point.x && y == point.y;
        }
    
        @Override
        public int hashCode() {
            return 31 * x + y;
        }
    }

    public static List<Point> BresenhamLine(Point start, Point end) {
        List<Point> line = new ArrayList<>();
        int dx = Math.abs(end.x - start.x);
        int dy = Math.abs(end.y - start.y);
        int sx = start.x < end.x ? 1 : -1;
        int sy = start.y < end.y ? 1 : -1;
        int err = dx - dy;
        int e2;

        int currentX = start.x;
        int currentY = start.y;

        while (true) {
            line.add(new Point(currentX, currentY));

            if (currentX == end.x && currentY == end.y) {
                break;
            }

            e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                currentX += sx;
            }

            if (e2 < dx) {
                err += dx;
                currentY += sy;
            }
        }

        return line;
    }
}