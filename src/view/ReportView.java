package view;

import controller.ReportController;
import model.entity.BookingTransaction;
import model.entity.Match;
import model.entity.Ticket;

import java.util.List;
import java.util.Scanner;

public class ReportView {

    private final ReportController reportController;
    private final Scanner scanner;

    public ReportView(ReportController reportController, Scanner scanner) {
        this.reportController = reportController;
        this.scanner = scanner;
    }

    public void displayMenu(String fanId) {
        while (true) {
            System.out.println("\n======================================");
            System.out.println("        REPORTS & STATISTICS          ");
            System.out.println("======================================");
            System.out.println("1. View my tickets");
            System.out.println("2. View my transaction history");
            System.out.println("3. System summary statistics");
            System.out.println("4. Seat statistics by match");
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
                    displaySystemSummary();
                    break;
                case "4":
                    displayMatchSeatSummary();
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    /**
     * Hiển thị toàn bộ lịch sử vé của Fan (cả VALID lẫn CANCELLED).
     *
     * <p>Nhất quán với {@code MainView.showMyTickets()} — cả hai cùng
     * gọi hàm lấy tất cả vé, không chỉ vé VALID.
     */
    public void displayMyTickets(String fanId) {
        // Dùng getAllTicketsByFan() để hiển thị đầy đủ lịch sử vé
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
        System.out.printf("  Total revenue:            %,d VND%n", totalRevenue);
    }

    public void displayMatchSeatSummary() {
        System.out.println("\n--- SEAT STATISTICS BY MATCH ---");

        List<Match> matches = reportController.getAllMatches();
        if (matches.isEmpty()) {
            System.out.println("No matches in the system.");
            return;
        }

        System.out.printf("%-12s %-35s %-12s %8s %8s %8s %8s%n",
                "Match ID", "Title", "Status", "Total", "Booked", "Avail", "Sold");
        System.out.println("-".repeat(100));

        for (Match match : matches) {
            int[] summary = reportController.getMatchSeatSummary(match.getMatchId());
            int soldTickets = reportController.getSoldTicketCount(match.getMatchId());

            System.out.printf("%-12s %-35s %-12s %,8d %,8d %,8d %,8d%n",
                    match.getMatchId(),
                    match.getTitle(),
                    match.getStatus().name(),
                    summary[0], 
                    summary[1], 
                    summary[2], 
                    soldTickets);
        }

        System.out.println("-".repeat(100));
    }
}
