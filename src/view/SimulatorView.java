package view;

import controller.SimulatorController;
import controller.SimulatorController.SimulationResult;
import model.enums.LockMechanism;
import model.entity.Seat;

import java.util.List;
import java.util.Scanner;

public class SimulatorView {
    private final SimulatorController simulatorController;

    public SimulatorView(SimulatorController simulatorController) {
        this.simulatorController = simulatorController;
    }

    public void start() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n=== CONCURRENT BOOKING SIMULATOR ===");

        int numThreads = 0;
        while (numThreads < 1) {
            System.out.print("Enter number of Fan threads (e.g. 100): ");
            try {
                numThreads = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer.");
            }
        }

        System.out.print("Enter Match ID (e.g. MATCH001): ");
        String matchId = scanner.nextLine().trim();
        if (matchId.isEmpty()) matchId = "MATCH001";

        List<Seat> availableSeats = simulatorController.getAvailableSeats(matchId);
        if (availableSeats.isEmpty()) {
            System.out.println("No available seats for match " + matchId + ". Please run Data Generator (option 1) first.");
            return;
        }

        System.out.print("Enter Seat ID to test (leave blank to auto-select): ");
        String seatId = scanner.nextLine().trim();
        if (seatId.isEmpty()) {
            seatId = availableSeats.get(0).getSeatId();
            System.out.println("Auto-selected seat: " + seatId);
        } else {
            String finalSeatId = seatId;
            boolean valid = availableSeats.stream().anyMatch(s -> s.getSeatId().equals(finalSeatId));
            if (!valid) {
                System.out.println("Seat not found or already booked. Simulator cancelled.");
                return;
            }
        }

        System.out.println("\n--- SELECT LOCK MECHANISM ---");
        System.out.println("1. NO_LOCK (No locking)");
        System.out.println("2. FILE_LOCK (OS-level file lock)");
        System.out.println("3. SYNCHRONIZED (JVM object lock)");
        System.out.println("4. OPTIMISTIC (Version-based lock)");
        System.out.println("5. TEST ALL (Compare all 4 mechanisms)");
        System.out.print("Select: ");
        String choice = scanner.nextLine().trim();

        if ("5".equals(choice)) {
            System.out.println("\nRunning benchmark for all 4 mechanisms (" + numThreads + " threads)...");
            System.out.println("Please wait, this may take a few seconds...");

            LockMechanism[] mechanisms = LockMechanism.values();
            SimulationResult[] results = new SimulationResult[mechanisms.length];

            for (int i = 0; i < mechanisms.length; i++) {
                simulatorController.resetData(matchId);
                System.out.println("- Running " + mechanisms[i] + "...");
                results[i] = simulatorController.runSimulation(numThreads, matchId, seatId, mechanisms[i]);
            }
            printResultTable(results);

        } else {
            LockMechanism mechanism = LockMechanism.NO_LOCK;
            switch (choice) {
                case "2": mechanism = LockMechanism.FILE_LOCK; break;
                case "3": mechanism = LockMechanism.SYNCHRONIZED; break;
                case "4": mechanism = LockMechanism.OPTIMISTIC; break;
            }

            System.out.println("\nRunning Simulator with mechanism: " + mechanism);
            SimulationResult result = simulatorController.runSimulation(numThreads, matchId, seatId, mechanism);
            printResultTable(new SimulationResult[]{result});
        }
    }

    private void printResultTable(SimulationResult[] results) {
        System.out.println("\n" + "=".repeat(95));
        System.out.printf("%-15s | %-10s | %-10s | %-10s | %-15s | %-10s | %-10s%n",
                "MECHANISM", "THREADS", "SUCCESS", "FAILED", "DOUBLE BOOKED", "TIME (ms)", "TPS");
        System.out.println("-".repeat(95));
        for (SimulationResult r : results) {
            if (r != null) {
                System.out.printf("%-15s | %-10d | %-10d | %-10d | %-15d | %-10d | %-10.2f%n",
                        r.mechanism, r.threads, r.success, r.failed, r.doubleBooked, r.timeMs, r.tps);
            }
        }
        System.out.println("=".repeat(95));
        System.out.println("* Note: DOUBLE BOOKED > 0 means the mechanism FAILED to prevent selling 1 seat to multiple fans.");
    }
}
