import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

public class MemorySimulator {

    // Configurable values (loaded from config file)
    static int MEMORY_MAX;
    static int PROC_SIZE_MAX;
    static int NUM_PROC;
    static int MAX_PROC_TIME;

    // For Next-Fit
    static int lastPosition = 0;

    static List<Partition> memory = new ArrayList<>();
    static Queue<Process> waitingQueue = new LinkedList<>();
    static List<Process> runningProcesses = new ArrayList<>();

    // Partition representing memory region
    static class Partition {
        int start;
        int size;
        String processId; // null means free

        Partition(int start, int size, String processId) {
            this.start = start;
            this.size = size;
            this.processId = processId;
        }

        boolean isFree() {
            return processId == null;
        }

        @Override
        public String toString() {
            return isFree()
                    ? "| FREE (" + size + "KB) |"
                    : "| " + processId + " (" + size + "KB) |";
        }
    }

    static class Process {
        String id;
        int size;
        int lifetime; // in seconds

        Process(String id, int size, int lifetime) {
            this.id = id;
            this.size = size;
            this.lifetime = lifetime;
        }
    }

    public static void main(String[] args) throws Exception {
        loadConfig("config.txt");
        MAX_PROC_TIME = MAX_PROC_TIME / 1000; // Convert ms → seconds

        System.out.println("===== CONFIGURATION =====");
        System.out.println("MEMORY_MAX = " + MEMORY_MAX + " KB");
        System.out.println("PROC_SIZE_MAX = " + PROC_SIZE_MAX + " KB");
        System.out.println("NUM_PROC = " + NUM_PROC);
        System.out.println("MAX_PROC_TIME = " + MAX_PROC_TIME + " s");
        System.out.println("=========================\n");

        // Best Fit
        initialize();
        runSimulation("BEST");

        // Worst Fit
        initialize();
        runSimulation("WORST");

        // Next Fit
        initialize();
        runSimulation("NEXT");
    }

    // CONFIG FILE LOADING
    static void loadConfig(String filename) throws Exception {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.contains("=")) continue;

                String[] parts = line.split("=");
                String key = parts[0].trim();
                String rawValue = parts[1].trim().replaceAll("[^0-9]", ""); // Remove KB/ms

                int value = Integer.parseInt(rawValue);

                switch (key) {
                    case "MEMORY_MAX" -> MEMORY_MAX = value;
                    case "PROC_SIZE_MAX" -> PROC_SIZE_MAX = value;
                    case "NUM_PROC" -> NUM_PROC = value;
                    case "MAX_PROC_TIME" -> MAX_PROC_TIME = value;
                }
            }
        }
    }

    // INITIALIZATION PER STRATEGY
    static void initialize() {
        memory.clear();
        waitingQueue.clear();
        runningProcesses.clear();
        memory.add(new Partition(0, MEMORY_MAX, null));

        Random rand = new Random();
        for (int i = 1; i <= NUM_PROC; i++) {
            waitingQueue.add(new Process(
                    "P" + i,
                    rand.nextInt(PROC_SIZE_MAX - 1) + 1,
                    rand.nextInt(MAX_PROC_TIME - 1) + 1
            ));
        }
    }

    // SIMULATION ENGINE
    static void runSimulation(String strategy) throws InterruptedException {
        System.out.println("\n--- Running " + strategy + " FIT Simulation ---");

        int time = 0;
        while (!waitingQueue.isEmpty() || !runningProcesses.isEmpty()) {
            time++;

            allocateProcesses(strategy);
            displayMemory(time);

            Thread.sleep(1000); // Simulates 1 second

            updateLifetimes();
            mergeFreePartitions();
        }
    }

    // MEMORY ALLOCATION METHODS
    static void allocateProcesses(String strategy) {
        Queue<Process> tempQueue = new LinkedList<>(waitingQueue);

        for (Process p : tempQueue) {
            boolean success = switch (strategy) {
                case "BEST" -> allocateBestFit(p);
                case "WORST" -> allocateWorstFit(p);
                case "NEXT" -> allocateNextFit(p);
                default -> false;
            };

            if (success) {
                waitingQueue.remove(p);
                runningProcesses.add(p);
            }
        }
    }

    static boolean allocateBestFit(Process p) {
        Partition best = null;
        for (Partition part : memory) {
            if (part.isFree() && part.size >= p.size &&
                    (best == null || part.size < best.size)) {
                best = part;
            }
        }
        return allocate(p, best);
    }

    static boolean allocateWorstFit(Process p) {
        Partition worst = null;
        for (Partition part : memory) {
            if (part.isFree() && part.size >= p.size &&
                    (worst == null || part.size > worst.size)) {
                worst = part;
            }
        }
        return allocate(p, worst);
    }

    static boolean allocateNextFit(Process p) {
        for (int i = 0; i < memory.size(); i++) {
            int index = (lastPosition + i) % memory.size();
            Partition part = memory.get(index);
            if (part.isFree() && part.size >= p.size) {
                lastPosition = index;
                return allocate(p, part);
            }
        }
        return false;
    }

    static boolean allocate(Process p, Partition hole) {
        if (hole == null) return false;
        memory.remove(hole);
        memory.add(new Partition(hole.start, p.size, p.id));
        if (hole.size > p.size) {
            memory.add(new Partition(hole.start + p.size, hole.size - p.size, null));
        }
        memory.sort(Comparator.comparingInt(a -> a.start));
        return true;
    }



    // PROCESS DEATH + MEMORY FREE
    static void updateLifetimes() {
        List<Process> finished = new ArrayList<>();
        for (Process p : runningProcesses) {
            p.lifetime--;
            if (p.lifetime <= 0) finished.add(p);
        }
        for (Process p : finished) {
            freeMemory(p.id);
            runningProcesses.remove(p);
        }
    }

    static void freeMemory(String pid) {
        for (Partition part : memory) {
            if (pid.equals(part.processId)) part.processId = null;
        }
    }

    // MERGE HOLES
    static void mergeFreePartitions() {
        List<Partition> merged = new ArrayList<>();

        for (Partition part : memory) {
            if (!merged.isEmpty() && merged.get(merged.size() - 1).isFree() && part.isFree()) {
                merged.get(merged.size() - 1).size += part.size;
            } else {
                merged.add(part);
            }
        }
        memory = merged;
    }


    static void displayMemory(int time) {
        System.out.println("\nTime: " + time + "s");
        
        // display memory partitions
        for (Partition part : memory) {
            if (part.isFree()) {
                System.out.print("| Free (" + part.size + "KB) |");
            } else {
                // find the process to display its remaining lifetime
                Process assigned = runningProcesses.stream()
                        .filter(p -> p.id.equals(part.processId))
                        .findFirst()
                        .orElse(null);

                if (assigned != null) {
                    System.out.print("| " + part.processId + " [" + assigned.lifetime + "s] (" + part.size + "KB) |");
                } else {
                    System.out.print("| " + part.processId + " (" + part.size + "KB) |");
                }
            }
        }
        System.out.println("\n======================================");

        // statistics
        int holeCount = 0;
        int totalFreeSize = 0;
        for (Partition part : memory) {
            if (part.isFree()) {
                holeCount++;
                totalFreeSize += part.size;
            }
        }
        double averageHole = holeCount > 0 ? (double) totalFreeSize / holeCount : 0;

        System.out.printf(
            "Stats → Holes: %d | Avg Hole Size: %.2f KB | Total Free: %d KB | Free: %.2f%%\n",
            holeCount, averageHole, totalFreeSize, (totalFreeSize * 100.0 / MEMORY_MAX)
        );
    }
}
