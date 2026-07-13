package view;

import controller.ReportController;
import model.entity.BookingTransaction;
import model.entity.Match;
import model.entity.Ticket;
import model.enums.SectionType;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ReportView {

    private ReportController reportController;
    private Scanner scanner;

    public ReportView(ReportController reportController, Scanner scanner) {
        this.reportController = reportController;
        this.scanner = scanner;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FAN REPORTS MENU
    // ═════════════════════════════════════════════════════════════════════════

    public void displayMenu(String fanId) {
        while (true) {
            System.out.println("\n======================================");
            System.out.println("        REPORTS & STATISTICS          ");
            System.out.println("======================================");
            System.out.println("1. View my tickets");
            System.out.println("2. View my transaction history");
            System.out.println("3. Seat statistics by match");
            System.out.println("0. Back");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    displayMyTickets(fanId);
                    break;
                case "2":
                    displayMyTransactions(fanId);
                    break;
                case "3":
                    displayMatchSeatSummary();
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ADMIN REPORTS MENU
    // ═════════════════════════════════════════════════════════════════════════

    public void displayAdminMenu() {
        while (true) {
            System.out.println("\n======================================");
            System.out.println("       VIEW PERFORMANCE REPORT       ");
            System.out.println("======================================");
            System.out.println("1. System summary statistics");
            System.out.println("2. Seat statistics by match");
            System.out.println("3. View all user tickets");
            System.out.println("4. View all transactions");
            System.out.println("0. Back");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    displaySystemSummary();
                    break;
                case "2":
                    displayMatchSeatSummary();
                    break;
                case "3":
                    displayAllTickets();
                    break;
                case "4":
                    displayAllTransactions();
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FAN-SPECIFIC METHODS
    // ═════════════════════════════════════════════════════════════════════════

    public void displayMyTickets(String fanId) {
        List<Ticket> tickets = reportController.getAllTicketsByFan(fanId);

        System.out.println("\n--- MY TICKET HISTORY (ALL) ---");
        if (tickets.isEmpty()) {
            System.out.println("You do not have any tickets.");
            return;
        }

        System.out.printf("%-16s %-12s %-14s %12s %-10s %-20s%n",
                "Ticket ID", "Match", "Seat", "Price (VND)", "Status", "Booking Date");
        System.out.println("-".repeat(90));

        for (Ticket t : tickets) {
            System.out.printf("%-16s %-12s %-14s %,12d %-10s %-20s%n",
                    t.getTicketId(),
                    t.getMatchId(),
                    t.getSeatId(),
                    t.getPrice(),
                    t.getStatus().name(),
                    t.getBookedAt());
        }

        System.out.println("-".repeat(90));
        System.out.printf("Total: %d tickets%n", tickets.size());
    }

    public void displayMyTransactions(String fanId) {
        List<BookingTransaction> transactions = reportController.getTransactionsByFan(fanId);

        System.out.println("\n--- TRANSACTION HISTORY ---");
        if (transactions.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }

        System.out.printf("%-14s %-12s %6s %12s %-10s %-12s %8s%n",
                "Txn ID", "Match", "Tickets", "Amount", "Status", "Mechanism", "Time");
        System.out.println("-".repeat(85));

        for (BookingTransaction t : transactions) {
            System.out.printf("%-14s %-12s %6d %,12d %-10s %-12s %6dms%n",
                    t.getTransactionId(),
                    t.getMatchId(),
                    t.getNumberOfTickets(),
                    t.getTotalAmount(),
                    t.getStatus().name(),
                    t.getMechanism().name(),
                    t.getDurationMs());
        }

        System.out.println("-".repeat(85));
        System.out.printf("Total: %d transactions%n", transactions.size());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ADMIN-SPECIFIC METHODS
    // ═════════════════════════════════════════════════════════════════════════

    public void displayAllTickets() {
        List<Ticket> tickets = reportController.getAllTickets();

        System.out.println("\n--- ALL USER TICKETS ---");
        if (tickets.isEmpty()) {
            System.out.println("No tickets in the system.");
            return;
        }

        System.out.printf("%-16s %-10s %-12s %-14s %12s %-10s %-20s%n",
                "Ticket ID", "Fan", "Match", "Seat", "Price (VND)", "Status", "Booking Date");
        System.out.println("-".repeat(100));

        for (Ticket t : tickets) {
            System.out.printf("%-16s %-10s %-12s %-14s %,12d %-10s %-20s%n",
                    t.getTicketId(),
                    t.getFanId(),
                    t.getMatchId(),
                    t.getSeatId(),
                    t.getPrice(),
                    t.getStatus().name(),
                    t.getBookedAt());
        }

        System.out.println("-".repeat(100));
        System.out.printf("Total: %d tickets%n", tickets.size());
    }

    public void displayAllTransactions() {
        List<BookingTransaction> transactions = reportController.getAllTransactions();

        System.out.println("\n--- ALL TRANSACTIONS ---");
        if (transactions.isEmpty()) {
            System.out.println("No transactions in the system.");
            return;
        }

        System.out.printf("%-14s %-10s %-12s %6s %12s %-10s %-12s %8s%n",
                "Txn ID", "Fan", "Match", "Tickets", "Amount", "Status", "Mechanism", "Time");
        System.out.println("-".repeat(95));

        for (BookingTransaction t : transactions) {
            System.out.printf("%-14s %-10s %-12s %6d %,12d %-10s %-12s %6dms%n",
                    t.getTransactionId(),
                    t.getFanId(),
                    t.getMatchId(),
                    t.getNumberOfTickets(),
                    t.getTotalAmount(),
                    t.getStatus().name(),
                    t.getMechanism().name(),
                    t.getDurationMs());
        }

        System.out.println("-".repeat(95));
        System.out.printf("Total: %d transactions%n", transactions.size());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SHARED METHODS (cả Fan và Admin đều dùng)
    // ═════════════════════════════════════════════════════════════════════════

    public void displaySystemSummary() {
        System.out.println("\n======================================");
        System.out.println("       SYSTEM SUMMARY STATISTICS      ");
        System.out.println("======================================");

        int totalTxn = reportController.getTotalTransactionCount();
        int successCount = reportController.getSuccessCount();
        int failedCount = reportController.getFailedCount();
        long totalRevenue = reportController.getTotalRevenue();

        System.out.printf("  Total transactions:       %,d%n", totalTxn);
        System.out.printf("  Successful transactions:  %,d%n", successCount);
        System.out.printf("  Failed transactions:      %,d%n", failedCount);
        if (totalTxn > 0) {
            System.out.printf("  Success rate:             %.1f%%%n", (successCount * 100.0 / totalTxn));
        }

        // ── Bảng doanh thu theo Section Type ─────────────────────────────
        System.out.println("\n--- REVENUE BY SEAT TYPE ---");
        System.out.printf("  %-15s | %12s | %15s%n", "Section Type", "Tickets Sold", "Revenue (VND)");
        System.out.println("  " + "-".repeat(48));

        Map<SectionType, long[]> revenueByType = reportController.getRevenueBySectionType();
        long totalTickets = 0;
        long grandTotalRevenue = 0;

        for (SectionType type : SectionType.values()) {
            long[] stats = revenueByType.get(type);
            if (stats != null) {
                System.out.printf("  %-15s | %,12d | %,15d%n", type.name(), stats[0], stats[1]);
                totalTickets += stats[0];
                grandTotalRevenue += stats[1];
            } else {
                System.out.printf("  %-15s | %,12d | %,15d%n", type.name(), 0, 0);
            }
        }

        System.out.println("  " + "-".repeat(48));
        System.out.printf("  %-15s | %,12d | %,15d%n", "TOTAL", totalTickets, grandTotalRevenue);
    }

    public void displayMatchSeatSummary() {
        System.out.println("\n--- SEAT STATISTICS BY MATCH ---");

        List<Match> matches = reportController.getAllMatches();
        if (matches.isEmpty()) {
            System.out.println("No matches in the system.");
            return;
        }

        System.out.printf("%-12s %-35s %-12s %8s %8s %8s %15s%n",
                "Match ID", "Title", "Status", "Total", "Booked", "Sold", "Revenue (VND)");
        System.out.println("-".repeat(105));

        for (Match match : matches) {
            int[] summary = reportController.getMatchSeatSummary(match.getMatchId());
            int soldTickets = reportController.getSoldTicketCount(match.getMatchId());

            // Tính doanh thu theo Section Type cho trận này
            Map<SectionType, long[]> revenueByType = reportController.getRevenueBySectionType(match.getMatchId());
            long matchRevenue = revenueByType.values().stream()
                    .mapToLong(stats -> stats[1])
                    .sum();

            System.out.printf("%-12s %-35s %-12s %,8d %,8d %,8d %,15d%n",
                    match.getMatchId(),
                    match.getTitle(),
                    match.getStatus().name(),
                    summary[0],    // Total
                    summary[1],    // Booked
                    soldTickets,   // Sold
                    matchRevenue); // Revenue
        }

        System.out.println("-".repeat(105));
    }
}
