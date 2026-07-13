package view;

import controller.BookingController;
import controller.ReportController;
import model.entity.BookingTransaction;
import model.entity.Match;
import model.entity.Ticket;
import model.enums.SectionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

public class ReportView {

    private ReportController reportController;
    private BookingController bookingController;
    private Scanner scanner;

    public ReportView(ReportController reportController, BookingController bookingController, Scanner scanner) {
        this.reportController = reportController;
        this.bookingController = bookingController;
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

        System.out.println("\n======================================");
        System.out.println("       MY TICKET HISTORY (ALL)        ");
        System.out.println("======================================");

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

        // ── Hỏi huỷ vé ──
        System.out.print("\nDo you want to cancel any ticket? (y/n): ");
        if (!"y".equals(scanner.nextLine().trim().toLowerCase())) return;

        // Lọc vé có thể huỷ (VALID)
        List<Ticket> cancellable = tickets.stream().filter(Ticket::isValid).toList();
        if (cancellable.isEmpty()) {
            System.out.println("[INFO] No cancellable tickets found.");
            return;
        }

        // Hiển thị danh sách vé có thể huỷ
        System.out.println("\n--- CANCELLABLE TICKETS ---");
        System.out.printf("%-16s %-12s %-14s %12s %-20s%n",
                "Ticket ID", "Match", "Seat", "Price (VND)", "Booking Date");
        System.out.println("-".repeat(80));
        for (Ticket t : cancellable) {
            System.out.printf("%-16s %-12s %-14s %,12d %-20s%n",
                    t.getTicketId(), t.getMatchId(), t.getSeatId(),
                    t.getPrice(), t.getBookedAt());
        }
        System.out.println("-".repeat(80));

        // ── Nhập票ID từng dòng ──
        System.out.println("\n--- CANCEL TICKETS ---");
        System.out.println("Commands: 0 = Finish entering | 1 = Edit previous entry");

        List<String> ticketIdsToCancel = new ArrayList<>();
        int cancelNumber = 1;

        while (true) {
            System.out.printf("Cancel ticket %d - Enter Ticket ID: ", cancelNumber);
            String input = scanner.nextLine().trim();

            // ── Nhập 0: Kết thúc nhập ──
            if ("0".equals(input)) {
                break;
            }

            // ── Nhập 1: Sửa票trước (xóa票cuối cùng, quay lại nhập lại) ──
            if ("1".equals(input)) {
                if (!ticketIdsToCancel.isEmpty()) {
                    String removed = ticketIdsToCancel.remove(ticketIdsToCancel.size() - 1);
                    cancelNumber--;
                    System.out.println("[INFO] Removed ticket " + removed + ". Please re-enter.");
                } else {
                    System.out.println("[INFO] No ticket to edit.");
                }
                continue;
            }

            // ── Validate票ID ──
            String ticketId = input;

            // Kiểm tra tồn tại trong danh sách cancellable
            Optional<Ticket> found = cancellable.stream()
                    .filter(t -> t.getTicketId().equals(ticketId))
                    .findFirst();
            if (!found.isPresent()) {
                System.out.println("[FAILED] Invalid ticket ID or ticket does not exist.");
                continue;
            }

            // Kiểm tra trùng
            if (ticketIdsToCancel.contains(ticketId)) {
                System.out.println("[FAILED] Ticket " + ticketId + " already added to cancel list.");
                continue;
            }

            ticketIdsToCancel.add(ticketId);
            cancelNumber++;
        }

        // ── Hiển thị summary và xác nhận ──
        if (ticketIdsToCancel.isEmpty()) {
            System.out.println("[INFO] No tickets to cancel.");
            return;
        }

        System.out.printf("%n--- SUMMARY ---%n");
        System.out.printf("Enter %d ticket(s) to cancel: %s%n",
                ticketIdsToCancel.size(), String.join(", ", ticketIdsToCancel));
        System.out.print("Proceed with cancellation? (1 = Yes, 0 = No): ");
        String confirm = scanner.nextLine().trim();

        if (!"1".equals(confirm)) {
            System.out.println("[INFO] Cancellation aborted.");
            return;
        }

        // ── Huỷ từng vé ──
        int cancelCount = 0;
        for (String tid : ticketIdsToCancel) {
            try {
                if (bookingController.cancelBooking(fanId, tid)) {
                    System.out.println("[SUCCESS] Ticket " + tid
                            + " cancelled. Seat restored. Refund recorded.");
                    cancelCount++;
                } else {
                    System.out.println("[FAILED] Ticket " + tid
                            + " not found or already cancelled.");
                }
            } catch (Exception e) {
                System.out.println("[FAILED] " + tid + ": " + e.getMessage());
            }
        }
        System.out.printf("Cancelled %d ticket(s).%n", cancelCount);
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

    /**
     * Hiển thị thống kê ghế theo trận — đơn giản cho Fan.
     */
    public void displayMatchSeatSummary() {
        System.out.println("\n--- SEAT STATISTICS BY MATCH ---");

        List<Match> matches = reportController.getAllMatches().stream()
                .filter(m -> m.getStatus() == model.enums.MatchStatus.SCHEDULED)
                .sorted((a, b) -> a.getMatchDate().compareTo(b.getMatchDate()))
                .toList();
        if (matches.isEmpty()) {
            System.out.println("No scheduled matches in the system.");
            return;
        }

        System.out.printf("%-12s %-35s %-8s %-10s %-10s %-12s %-10s %-15s%n",
                "Match ID", "Title", "Time", "Total", "Booked", "Available", "Sold", "Revenue (VND)");
        System.out.println("-".repeat(115));

        for (Match match : matches) {
            int[] summary = reportController.getMatchSeatSummary(match.getMatchId());
            int total = summary[0];
            int booked = summary[1];
            int available = summary[2];
            int sold = reportController.getSoldTicketCount(match.getMatchId());
            long revenue = reportController.getMatchRevenue(match.getMatchId());

            System.out.printf("%-12s %-35s %-8s %-10d %-10d %-12d %-10d %,15d%n",
                    match.getMatchId(),
                    match.getTitle(),
                    match.getMatchTime(),
                    total,
                    booked,
                    available,
                    sold,
                    revenue);
        }

        System.out.println("-".repeat(115));
    }
}
