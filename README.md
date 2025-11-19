# MemorySimulator

This Java program simulates memory allocation strategies (Best-Fit, Worst-Fit, Next-Fit) for process management in a fixed-size memory space. It demonstrates how different algorithms allocate and free memory for randomly generated processes.

## Features
- Simulates memory allocation for multiple processes
- Supports Best-Fit, Worst-Fit, and Next-Fit strategies
- Visualizes memory partitions and process lifetimes
- Configurable parameters via `config.txt`

## File Structure
- `src/main/java/MemorySimulator.java` — Main program file
- `src/main/java/config.txt` — Configuration file for memory and process parameters

## Notes
- The program uses random process sizes and lifetimes for each simulation run.
- The simulation pauses for 1 second per cycle to visualize changes.



## How to Run

### Using Maven (Recommended)
1. Open a terminal in the project root directory.
2. Compile and run:
   ```sh
   mvn compile
   mvn exec:java -Dexec.mainClass=MemorySimulator
   ```

### Using javac/java (Manual)
1.
   ```sh
   cd src/main/java
   javac MemorySimulator.java
   java MemorySimulator
   ```


## Output
The program prints the memory allocation simulation for each strategy, showing how processes are placed and removed over time.

##  Program Step by Step

1. The program sets up a pretend computer memory and creates a bunch of pretend tasks, each needing a random amount of memory and running for a random amount of time.
2. It tries three different ways to give out memory:
   - **Best-Fit:** Finds the smallest empty spot that’s big enough for the task.
   - **Worst-Fit:** Finds the biggest empty spot for the task.
   - **Next-Fit:** Starts looking from where it last gave out memory and finds the next empty spot that fits.
3. Every second, it:
   - Tries to fit new tasks into memory.
   - Shows you what the memory looks like (which parts are used, which are free).
   - Counts down the time for each task. When a task is done, it frees up its memory.
   - Combines any empty spots next to each other into bigger empty spots.
4. It repeats this until all tasks are done.


## Requirements
- obv >= Java 17
- Maven (if using Maven commands)


