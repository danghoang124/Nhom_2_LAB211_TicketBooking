package experiment;

import controller.SimulatorController;
import controller.SimulatorController.SimulationResult;
import main.AppContext;
import model.enums.LockMechanism;

public class RunExperiment {

    public static void main(String[] args) {
        int[] threadCounts = {10, 50, 100, 200, 500, 1000};
        LockMechanism[] mechanisms = LockMechanism.values();
        String matchId = "MATCH001";

        System.out.println("Initializing AppContext...");
        AppContext appContext = new AppContext();
        SimulatorController simulatorController = appContext.getSimulatorController();

        System.out.println("Getting available seats...");
        var availableSeats = simulatorController.getAvailableSeats(matchId);
        if (availableSeats.isEmpty()) {
            System.out.println("No available seats! Please run DataGenerator first.");
            return;
        }

        String seatId = availableSeats.get(0).getSeatId();
        System.out.println("Using seat: " + seatId + " for match " + matchId);

        SimulationResult[] allResults = new SimulationResult[threadCounts.length * mechanisms.length];
        int runCounter = 0;

        System.out.println("\n" + "=".repeat(120));
        System.out.printf("%-6s | %-15s | %-8s | %-8s | %-12s | %-10s | %-10s%n",
                "RUN", "MECHANISM", "THREADS", "SUCCESS", "FAILED",
                "DBLE_BKD", "TIME(ms)", "TPS");
        System.out.println("=".repeat(120));

        for (int threads : threadCounts) {
            for (LockMechanism mechanism : mechanisms) {
                System.out.printf("[%d] %s %d threads...",
                        runCounter + 1, mechanism, threads);
                System.out.flush();

                simulatorController.resetData(matchId);
                SimulationResult result = simulatorController.runSimulation(
                        threads, matchId, seatId, mechanism);

                allResults[runCounter++] = result;

                System.out.printf(" OK (%d success, %d failed, %d double, %dms, %.2f TPS)%n",
                        result.success, result.failed, result.doubleBooked,
                        result.timeMs, result.tps);
            }
        }

        System.out.println("\n\n=== FINAL RESULTS TABLE ===");
        System.out.println("=".repeat(100));
        System.out.printf("%-15s | %-10s | %-10s | %-10s | %-15s | %-10s | %-10s%n",
                "MECHANISM", "THREADS", "SUCCESS", "FAILED",
                "DOUBLE BOOKED", "TIME (ms)", "TPS");
        System.out.println("-".repeat(100));

        for (SimulationResult r : allResults) {
            if (r != null) {
                System.out.printf("%-15s | %-10d | %-10d | %-10d | %-15d | %-10d | %-10.2f%n",
                        r.mechanism, r.threads, r.success, r.failed,
                        r.doubleBooked, r.timeMs, r.tps);
            }
        }

        System.out.println("=".repeat(100));

        System.out.println("\nExporting results to data/simulation_results.csv...");
        simulatorController.exportResultsToCsv(allResults);
        System.out.println("Done!");
    }
}
