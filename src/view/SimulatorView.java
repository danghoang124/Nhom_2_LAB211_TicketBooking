package view;

import controller.SimulatorController;
import controller.SimulatorController.SimulationResult;
import model.entity.Seat;
import model.enums.LockMechanism;

import java.util.List;
import java.util.Scanner;

public class SimulatorView {

    private SimulatorController simulatorController;
    private Scanner scanner;

    public SimulatorView(SimulatorController simulatorController, Scanner scanner) {
        this.simulatorController = simulatorController;
        this.scanner = scanner;
    }

    public void start() {

        System.out.println("\n=== CONCURRENT BOOKING SIMULATOR ===");

        int numThreads = 0;
        while (numThreads <= 0) {
            try {
                System.out.print("Enter number of Fan threads (e.g. 100): ");
                numThreads = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer.");
            }
        }

        System.out.print("Enter Match ID (e.g. MATCH001): ");
        String matchId = scanner.nextLine().trim();
        if (matchId.isEmpty()) {
            matchId = "MATCH001";
        }

        List<Seat> availableSeats = simulatorController.getAvailableSeats(matchId);
        if (availableSeats.isEmpty()) {
            // Không còn ghế trống (có thể do lần chạy trước chưa reset) → tự động reset
            System.out.println("[INFO] No available seats found for match " + matchId
                    + ". Auto-resetting all seats to AVAILABLE...");
            simulatorController.resetData(matchId);
            availableSeats = simulatorController.getAvailableSeats(matchId);
        }
        if (availableSeats.isEmpty()) {
            System.out.println("[ERROR] Still no available seats after reset. Please run DataGenerator first.");
            return;
        }

        System.out.print("Enter Seat ID to test (leave blank to auto-select): ");
        String rawSeatId = scanner.nextLine().trim();
        String seatId;
        if (rawSeatId.isEmpty()) {
            seatId = availableSeats.get(0).getSeatId();
            System.out.println("Auto-selected seat: " + seatId);
        } else {
            String sid = rawSeatId;
            boolean validSeat = availableSeats.stream()
                    .anyMatch(s -> s.getSeatId().equals(sid));
            if (!validSeat) {
                System.out.println("Seat not found or already booked. Simulator cancelled.");
                return;
            }
            seatId = rawSeatId;
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
            System.out.println("Testing " + numThreads + " threads with all mechanisms...");
            System.out.println("Please wait, this may take a few seconds...");

            LockMechanism[] mechanisms = LockMechanism.values();
            SimulationResult[] results = new SimulationResult[mechanisms.length];

            for (int i = 0; i < mechanisms.length; i++) {
                simulatorController.resetData(matchId);
                System.out.println("  Running " + mechanisms[i] + "...");
                results[i] = simulatorController.runSimulation(
                        numThreads, matchId, seatId, mechanisms[i]);
            }

            printResultTable(results);
            simulatorController.exportResultsToCsv(results);
        } else {
            LockMechanism mechanism;
            switch (choice) {
                case "2":
                    mechanism = LockMechanism.FILE_LOCK;
                    break;
                case "3":
                    mechanism = LockMechanism.SYNCHRONIZED;
                    break;
                case "4":
                    mechanism = LockMechanism.OPTIMISTIC;
                    break;
                default:
                    mechanism = LockMechanism.NO_LOCK;
                    break;
            }
            System.out.println("Running with " + mechanism + "...");
            SimulationResult result = simulatorController.runSimulation(
                    numThreads, matchId, seatId, mechanism);
            printResultTable(new SimulationResult[]{result});
            simulatorController.exportResultsToCsv(new SimulationResult[]{result});
        }
    }

    private void printResultTable(SimulationResult[] results) {
        System.out.println("\n" + "=".repeat(95));
        System.out.printf("%-15s | %-10s | %-10s | %-10s | %-15s | %-10s | %-10s%n",
                "MECHANISM", "THREADS", "SUCCESS", "FAILED",
                "DOUBLE BOOKED", "TIME (ms)", "TPS");
        System.out.println("-".repeat(95));

        for (SimulationResult r : results) {
            if (r != null) {
                System.out.printf("%-15s | %-10d | %-10d | %-10d | %-15d | %-10d | %-10.2f%n",
                        r.mechanism, r.threads, r.success, r.failed,
                        r.doubleBooked, r.timeMs, r.tps);
            }
        }

        System.out.println("=".repeat(95));
        System.out.println("* Note: DOUBLE BOOKED > 0 means the mechanism FAILED to prevent selling 1 seat to multiple fans.");
    }
}
