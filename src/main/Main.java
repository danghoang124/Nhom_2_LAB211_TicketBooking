package main;

import repository.*;
import java.util.Scanner;

/**
 * Main application entry point for Stadium Ticket Booking Simulation.
 * Provides a welcome screen and CLI routing for the simulation.
 */
public class Main {

    public static void main(String[] args) {
        printBanner();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n=== HỆ THỐNG MÔ PHỎNG ĐẶT VÉ SÂN VẬN ĐỘNG (LAB211) ===");
            System.out.println("1. Khởi chạy bộ tạo dữ liệu (DataGenerator)");
            System.out.println("2. Xem cấu hình hệ thống (Sân vận động, Ghế...)");
            System.out.println("3. Chạy kiểm thử hiệu năng đọc ghi (Performance Benchmarks)");
            System.out.println("0. Thoát");
            System.out.print("Chọn chức năng: ");

            String choice = scanner.nextLine().trim();
            if ("0".equals(choice)) {
                System.out.println("Cảm ơn bạn đã sử dụng hệ thống!");
                break;
            }

            switch (choice) {
                case "1":
                    System.out.println("\n[INFO] Đang chạy bộ sinh dữ liệu DataGenerator...");
                    try {
                        generator.DataGenerator.main(new String[0]);
                    } catch (Exception e) {
                        System.err.println("Lỗi khi chạy DataGenerator: " + e.getMessage());
                    }
                    break;
                case "2":
                    displaySystemInfo();
                    break;
                case "3":
                    System.out.println("\n[INFO] Đang chạy kiểm thử hiệu năng PerformanceTest...");
                    try {
                        test.PerformanceTest.main(new String[0]);
                    } catch (Exception e) {
                        System.err.println("Lỗi khi chạy PerformanceTest: " + e.getMessage());
                    }
                    break;
                default:
                    System.out.println("Lựa chọn không hợp lệ. Vui lòng chọn lại!");
            }
        }
        scanner.close();
    }

    private static void printBanner() {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║            STADIUM TICKET BOOKING SIMULATION         ║");
        System.out.println("║                 FPT UNIVERSITY - LAB211              ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
    }

    private static void displaySystemInfo() {
        System.out.println("\n--- CẤU HÌNH HỆ THỐNG HIỆN TẠI ---");
        try {
            StadiumRepository stadiumRepo = new StadiumRepository();
            SectionRepository sectionRepo = new SectionRepository();
            MatchRepository matchRepo = new MatchRepository();
            SeatRepository seatRepo = new SeatRepository();
            FanRepository fanRepo = new FanRepository();

            System.out.printf("- Số lượng Sân vận động: %d%n", stadiumRepo.count());
            System.out.printf("- Số lượng Khán đài (Section): %d%n", sectionRepo.count());
            System.out.printf("- Số lượng Trận đấu: %d%n", matchRepo.count());
            System.out.printf("- Tổng số Ghế: %d%n", seatRepo.count());
            System.out.printf("- Số lượng Người hâm mộ (Fan): %d%n", fanRepo.count());
        } catch (Exception e) {
            System.out.println(
                    "[⚠️ LƯU Ý] Chưa thể đọc được cấu hình hệ thống. Vui lòng chọn chức năng (1) để sinh dữ liệu CSV trước!");
        }
    }
}
