# Concurrent Programming (COMP 409) Assignments

## Assignment 1

**Question 1: Metro System Simulation**

- **Objective:** Simulate a metro system with 4 stations managed by an overseer to ensure the total passenger count does not exceed capacity.
- **Implementation:** Use 5 threads (1 overseer, 4 stations). Each station tracks its local passenger count and updates the overseer periodically.
- **Key Requirements:** 
  - Synchronize shared variable access to avoid data races.
  - Allow maximal concurrency while maintaining correct behavior.
  - Input parameters: `n` (capacity), `q` (interval in ms).
  - Output: Print station counts and total sum periodically.
- **File:** `q1.java`

**Question 2: Image Filling with Icons**

- **Objective:** Fill an image with multiple thread-specific icons without overlap.
- **Implementation:** Multiple threads copy icons into an image at random positions.
- **Key Requirements:** 
  - Synchronize to prevent overlap.
  - Measure speedup for multithreaded implementation.
  - Input parameters: `i` (basename for icon files), `h` (image height), `w` (image width), `t` (number of threads), `a` (max failures before termination).
  - Output: Generate `outputimage.png` and print execution time.
  - Plot and explain speedup results.
- **File:** `q2.java`

---

## Assignment 2

**Question 1: Sea Creature Movement Simulation**

- **Objective:** Simulate movement of sea creatures in a 3D grid.
- **Implementation:** Use threads to control sea creatures moving in a 3D grid.
- **Key Requirements:** 
  - Use unique locks for grid cells.
  - Avoid deadlock and allow maximal concurrency.
  - Input parameters: `k` (number of creatures), `n` (duration in seconds).
  - Output: Print movement logs of each creature.
- **File:** `q1.java`

**Question 2: One-Way Traffic Simulation**

- **Objective:** Simulate traffic through a single-lane road under construction.
- **Implementation:** Use threads to manage vehicle movement through the lane.
- **Key Requirements:** 
  - Use monitors for synchronization.
  - Ensure fairness and efficiency.
  - Input parameters: `n` (road segments), `s` (vehicle introduction interval), `d` (segment travel time).
  - Output: Log vehicle generation, entry, and exit.
  - Experiment and justify synchronization strategy.
- **File:** `q2.java`

---

## Assignment 3

**Question 1: 2D Game Character Movement**

- **Objective:** Simulate movement of characters in a 2D grid using thread pools.
- **Implementation:** Compare non-pooled and pooled thread management.
- **Key Requirements:** 
  - Use Bresenham algorithm for movement.
  - Compare performance with varying parameters.
  - Input parameters: `m` (grid size), `n` (spawn points), `s` (spawn interval), `t` (thread pool size).
  - Output: Total successful moves.
  - Provide experimental data and analysis.
- **Files:** `q1n.java`, `q1p.java`, `q1.txt`

**Question 2: Lock-Free vs. Elimination Stack**

- **Objective:** Compare performance of a lock-free stack and an elimination stack.
- **Implementation:** Implement and test both stack types with multithreaded operations.
- **Key Requirements:** 
  - Measure and compare performance under varying parameters.
  - Input parameters: `x` (stack type), `t` (threads), `n` (operations), `s` (sleep time), `e` (elimination array size), `w` (max delay).
  - Output: Simulation time and remaining stack nodes.
  - Provide data and analysis on performance differences.
- **Files:** `q2.java`, `q2.txt`

---

## Assignment 4

**Question 1: Karatsuba Algorithm Implementation**

- **Objective:** Implement Karatsuba multiplication using fork-join thread pooling.
- **Implementation:** Use recursive divide-and-conquer strategy for large number multiplication.
- **Key Requirements:** 
  - Compare single-threaded and multithreaded performance.
  - Input parameters: `t` (threads), two large base-10 numbers.
  - Output: Performance data and speedup analysis.
  - Provide experimental results and explanation.
- **Files:** `q1.java`, `q1.txt`

**Question 2: Parallel Regular Expression Matching**

- **Objective:** Implement optimistic parallelization for regular expression matching using OpenMP.
- **Implementation:** Use DFA and optimistic threads to count regex matches in a string.
- **Key Requirements:** 
  - Generate long random strings for testing.
  - Demonstrate speedup with optimistic threads.
  - Input parameters: `t` (optimistic threads), `n` (string length).
  - Output: Input string, match count, and execution time.
  - Provide timing data and analysis.
- **Files:** `q2.c`, `q2.txt`
