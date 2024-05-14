// Samantha Handal - 260983914

import java.util.Random;

public class q2 {
    public static void main(String[] args) {

        // Parse arguments, throw appropriate error for incorrect usage
        if (args.length < 3) {
            System.err.println("Usage: java q2 <n> <s> <d>");
            System.exit(1);
        }

        int n = Integer.parseInt(args[0]);  // # of vehicle-sized segments on road
        int s = Integer.parseInt(args[1]);  // ms between new vehicle introduced in the simulation
        int d = Integer.parseInt(args[2]);  // ms to move one road segment

        if (n <= 2 || s <= 20 || d <= 10) {
            System.err.println("Usage: n > 2, s > 20 and d > 10");
            System.exit(1);
        }

        // create monitor (traffic controller)
        TrafficController controller = new TrafficController(n, d, s);

        // start the thread to generate vehicles
        new VehicleGenerator(controller, s, d).start();
    }
}

class VehicleGenerator extends Thread {
    private final TrafficController controller;
    private final int s;
    private final int d;
    private final Random random = new Random();

    public VehicleGenerator(TrafficController controller, int s, int d) {
        this.controller = controller;
        this.s = s;
        this.d = d;
    }

    @Override
    public void run() {

        // assign unique id for each new vehicle
        int vehicleId = 1;
        while (!Thread.interrupted()) {
            try {

                // Every s ± 20 ms a vehicle is introduced in the simulation
                Thread.sleep(s + random.nextInt(41) - 20);

                // select entry point: 0 -> left end, n - 1 -> right end
                int entryPoint;

                // 1 -> right, -1 -> left
                int direction;

                // Random entry point decider (45/45/10)
                int entryDecision = random.nextInt(100);

                // 45% chance to enter at one end (left) of the street, must move towards the other end (right)
                if (entryDecision < 45) {
                    entryPoint = 0;
                    direction = 1;

                // Another 45% chance to enter at the other end (right) of the street, must move towards the opposite end (left)
                } else if (entryDecision < 90) {
                    entryPoint = controller.getRoadLength() - 1;
                    direction = -1; 

                // 10% chance to enter from a house in the middle of the road
                } else {

                    // Residents only enter from one of the interior road segments (i.e., not at either of the ends)
                    entryPoint = 1 + random.nextInt(controller.getRoadLength() - 2);
                    // 50/50 chance of turning left or right when exiting from a houses
                    direction = random.nextBoolean() ? 1 : -1;
                }

                // Start new vehicle thread, and add it to list of active vehicles
                controller.addVehicle(new Vehicle(vehicleId, entryPoint, direction, d, controller));
                vehicleId++;

            } catch (InterruptedException e) {
                break;
            }
        }
    }
}

// Represents a vehicle in the traffic simulation. 
// Each vehicle is modeled as a thread that enters, travels through, and exits a simulated road.
class Vehicle extends Thread {
    private final int id;                           // Unique identifier for the vehicle
    private final int entryPoint;                   // The segment of the road the vehicle will start at
    private final int direction;                    // The direction the vehicle will to move, 1 -> right, -1 -> left
    private final int delay;                        // Delay in milliseconds it takes for the vehicle to move from one segment to the next
    private final TrafficController controller;     // Reference to the traffic controller managing the road. Used to signal entering and exiting the road.


    public Vehicle(int id, int entryPoint, int direction, int delay, TrafficController controller) {
        this.id = id;
        this.entryPoint = entryPoint;
        this.direction = direction;
        this.delay = delay;
        this.controller = controller;
    }

    @Override
    public void run() {
        try {

            // Notify the traffic controller that this vehicle is attempting to enter the road.
            controller.enterRoad(this);

            // Simulate the vehicle's travel by sleeping for a certain delay for each segment it must cross.
            int segmentsToCross = calculateSegmentsToCross();
            for (int i = 0; i < segmentsToCross; i++) {
                sleep(delay);
            }

            // Notify the traffic controller that this vehicle should exit the road.
            controller.exitRoad(this);

        // Handle the case where the vehicle's thread is interrupted while it is traveling.
        } catch (InterruptedException e) {
            System.out.println("Vehicle " + id + " interrupted.");
        }
    }

    // Helper to calculate the number of road segments this vehicle needs to cross.
    private int calculateSegmentsToCross() {

        // For vehicles entering from a house in the middle, adjust the number of segments to cross
        if (entryPoint > 0 && entryPoint < controller.getRoadLength() - 1) {

            if (direction > 0) {
                return controller.getRoadLength() - entryPoint;
            } else {
                return entryPoint + 1;
            }
        }
        return controller.getRoadLength();
    }

    public int getEntryPoint() {
        return entryPoint;
    }

    public int getDirection() {
        return direction;
    }

    public int getVehicleId() {
        return id;
    }
}

class TrafficController {
    private final Object lock = new Object();
    private final int roadLength;
    private final int switchAfterVehicles;              // Number of vehicles to pass before considering direction switch
    private int direction = 0;                          // 1 for right to left, -1 for left to right, 0 for no traffic
    private int vehiclesInDirection = 0;                // Count vehicles moving in current direction
    private int vehiclesPassedSinceLastSwitch = 0;      // Count vehicles passed since the last direction switch
    private int waitingLeft = 0, waitingRight = 0;      // Count vehicles waiting to enter from each end
    private boolean entryFlag = true;                   // Flag to control flow through road

    public TrafficController(int roadLength, int d, int s) {
        this.roadLength = roadLength;

        // Set the switch threshold to be the number of new cars that could arrive during the total time it 
        // takes for one car to drive through the whole road
        this.switchAfterVehicles = ((roadLength * d) / s) * 2;
    }

    public int getRoadLength() {
        return roadLength;
    }

    public void addVehicle(Vehicle vehicle) {
        synchronized (lock) {

            // add direction of waiting vehicle to the queue
            if(vehicle.getDirection() == 1) waitingRight++;
            else if(vehicle.getDirection() == -1) waitingLeft++;

            // Print each time a car is generated
            System.out.println("car: " + vehicle.getVehicleId() + ", " + vehicle.getEntryPoint() + ", " + vehicle.getDirection());
            vehicle.start();
        }
    }

    public void enterRoad(Vehicle vehicle) throws InterruptedException {
        synchronized (lock) {

            // Vehicle waits if it cant enter due to the direction being occupied by another direction's traffic
            while (!canEnter(vehicle)) {
                lock.wait();
            }

            // remove vehicle from waiting queue, either on left or right
            if(vehicle.getDirection() == 1) waitingRight--;
            else if(vehicle.getDirection() == -1) waitingLeft--;

            // Add car to the road (increase count) and increment the number of cars which have enter
            // the road from the same side sequentially
            vehiclesInDirection++;
            vehiclesPassedSinceLastSwitch++;

            // extra condition added to include fairness, if too many cars have entered from the same side
            // sequentially, allow all the cars on the road to exit and swap sides allowing cars to enter from
            // the other side to enter based off the threshold calculated in the constructor
            if (vehiclesPassedSinceLastSwitch >= switchAfterVehicles && (waitingLeft > 0 || waitingRight > 0) && entryFlag) {
                requestSwitchDirection();
            }

            // Update current direction to that of the car on the road
            direction = vehicle.getDirection();

            // Print each time vehicle enters
            System.out.println("enter: " + vehicle.getVehicleId() + ", " + vehicle.getEntryPoint());
        }
    }

    public void exitRoad(Vehicle vehicle) {
        synchronized (lock) {

            // decrement each vehicle that exits
            vehiclesInDirection--;

            // Print each time vehicle leaves
            System.out.println("exit: " + vehicle.getVehicleId());

            // Exiting the road and potentially allowing vehicles from the opposite direction to enter
            if (vehiclesInDirection == 0) {
                if (!entryFlag) {
                    
                    // Reset direction to allow switch, reset counter and entry flag
                    direction = 0; 
                    vehiclesPassedSinceLastSwitch = 0; 
                    entryFlag = true;
                }

                // Notify possibly waiting vehicles to re-check their conditions and give them the opertunity to enter the road
                lock.notifyAll(); 
            }
        }
    }

    // Helper to see if a vehicle can enter
    private boolean canEnter(Vehicle vehicle) {

        // A vehicle can enter if the road is empty or if it's moving in the same direction as current traffic
        return (direction == 0 || direction == vehicle.getDirection()) && entryFlag;
    }

    // Request a direction switch based on the waiting counts
    private void requestSwitchDirection() {

        // If vehicles are waiting on other side (and a lot), allow side with more cars to be freed
        // if not, then we dont swap directions since there would be no point (due to monitor logic, cars would
        // automatically enter road if there's no contention)
        if (((direction == 1 && waitingLeft > 0) || (direction == -1 && waitingRight > 0)) && directionShouldSwitch()) {
            
            // Allow the side with fewer opportunities so far to go next
            if (waitingLeft > waitingRight) {
                direction = -1;
            } else {
                direction = 1;
            }

            // stop new cars from entering so we can switch directions
            entryFlag = false;
        }
    }

    // Check if there's a significant imbalance in waiting vehicles
    private boolean directionShouldSwitch() {
        return Math.abs(waitingLeft - waitingRight) > switchAfterVehicles;
    }
}