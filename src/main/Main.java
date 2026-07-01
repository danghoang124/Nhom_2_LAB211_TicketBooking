package main;

import java.util.Scanner;
import repository.*;
import view.MainView;

public class Main {

    public static void main(String[] args) {
        printBanner();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n=== STADIUM TICKET BOOKING SIMULATION (LAB211) ===");
            System.out.println("1. Run Data Generator (DataGenerator)");
            System.out.println("2. View System Configuration (Stadiums, Seats...)");
            System.out.println("3. Run Performance Benchmarks (PerformanceTest)");
            System.out.println("4. Enter Ticket System (Login / Book / Report)");
            System.out.println("0. Exit");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine().trim();
            if ("0".equals(choice)) {
                System.out.println("Thank you for using the system!");
                break;
            }

            switch (choice) {
                case "1":
                    System.out.println("\n[INFO] Running DataGenerator...");
                    try {
                        generator.DataGenerator.main(new String[0]);
                    } catch (Exception e) {
                        System.err.println("Error running DataGenerator: " + e.getMessage());
                    }
                    break;
                case "2":
                    displaySystemInfo();
                    break;
                case "3":
                    System.out.println("\n[INFO] Running PerformanceTest...");
                    try {
                        test.PerformanceTest.main(new String[0]);
                    } catch (Exception e) {
                        System.err.println("Error running PerformanceTest: " + e.getMessage());
                    }
                    break;
                case "4":
                    System.out.println("\n[INFO] Initializing Ticket System...");
                    try {
                        AppContext appContext = new AppContext();
                        MainView mainView = new MainView(appContext);
                        mainView.start();
                    } catch (Exception e) {
                        System.err.println("Error starting Ticket System: " + e.getMessage());
                        e.printStackTrace();
                    }
                    break;
                default:
                    System.out.println("Invalid option. Please try again!");
            }
        }
        scanner.close();
    }

    private static void printBanner() {
        System.out.println("======================================================");
        System.out.println("            STADIUM TICKET BOOKING SIMULATION         ");
        System.out.println("                 FPT UNIVERSITY - LAB211              ");
        System.out.println("======================================================");
    }

    private static void displaySystemInfo() {
        System.out.println("\n--- CURRENT SYSTEM CONFIGURATION ---");
        try {
            StadiumRepository stadiumRepo = new StadiumRepository();
            SectionRepository sectionRepo = new SectionRepository();
            MatchRepository matchRepo = new MatchRepository();
            SeatRepository seatRepo = new SeatRepository();
            FanRepository fanRepo = new FanRepository();

            System.out.printf("- Stadium count: %d%n", stadiumRepo.count());
            System.out.printf("- Section count: %d%n", sectionRepo.count());
            System.out.printf("- Match count: %d%n", matchRepo.count());
            System.out.printf("- Total Seats: %d%n", seatRepo.count());
            System.out.printf("- Fan count: %d%n", fanRepo.count());
        } catch (Exception e) {
            System.out.println(
                    "[WARNING] Cannot read system configuration. Please select option (1) to generate CSV data first!");
        }
    }
}
