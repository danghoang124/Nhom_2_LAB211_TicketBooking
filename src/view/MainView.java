package view;

import controller.BookingController;
import controller.FanController;
import controller.ReportController;
import controller.StadiumController;
import main.AppContext;
import model.entity.Fan;
import model.enums.Role;
import model.entity.Match;
import model.entity.Seat;
import model.entity.Section;
import model.entity.Ticket;
import model.enums.LockMechanism;
import model.enums.MatchStatus;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class MainView {

    private final AppContext appContext;
    private final Scanner scanner;

    private final FanController fanController;
    private final BookingController bookingController;
    private final ReportController reportController;
    private final StadiumController stadiumController;

    private final LoginView loginView;
    private final RegisterView registerView;
    private final BookingView bookingView;
    private final ReportView reportView;
    private final AdminView adminView;

    public MainView(AppContext appContext, Scanner scanner) {
        this.appContext = appContext;
        this.scanner = scanner;

        this.fanController = appContext.getFanController();
        this.bookingController = appContext.getBookingController();
        this.reportController = appContext.getReportController();
        this.stadiumController = appContext.getStadiumController();

        this.loginView = new LoginView(fanController, scanner);
        this.registerView = new RegisterView(fanController, scanner);
        this.bookingView = new BookingView(bookingController, fanController, scanner);
        this.reportView = new ReportView(reportController, scanner);
        this.adminView = new AdminView(appContext.getAdminController(), scanner);
    }

    public void start() {
        System.out.println("\n======================================================");
        System.out.println("          STADIUM TICKET BOOKING SYSTEM               ");
        System.out.println("             FPT UNIVERSITY - LAB211                  ");
        System.out.println("======================================================");

        while (true) {
            if (!fanController.isLoggedIn()) {
                boolean shouldExit = showGuestMenu();
                if (shouldExit)
                    break;
            } else {
                boolean loggedOut = showMainMenu();
                if (loggedOut)
                    continue;
            }
        }
    }

    private boolean showGuestMenu() {
        System.out.println("\n=== GUEST MENU ===");
        System.out.println("1. Login");
        System.out.println("2. Register new account");
        System.out.println("3. View match list");
        System.out.println("0. Return to main menu");
        System.out.print("Select an option: ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1":
                loginView.show();
                return false;
            case "2":
                registerView.show();
                return false;
            case "3":
                showMatchList();
                return false;
            case "0":
                return true;
            default:
                System.out.println("Invalid option. Please try again.");
                return false;
        }
    }

    private boolean showMainMenu() {
        Fan currentFan = fanController.getCurrentFan();
        boolean isAdmin = currentFan.getRole() == Role.ADMIN;

        while (true) {
            System.out.println("\n======================================");
            System.out.printf("  Welcome: %-25s  %n", currentFan.getFullName());
            System.out.println("--------------------------------------");
            System.out.println("  1. View match list                  ");
            System.out.println("  2. View seat map by match           ");
            System.out.println("  3. Book a ticket                    ");
            System.out.println("  4. View my tickets                  ");
            System.out.println("  5. Reports & Statistics             ");
            if (isAdmin) {
                System.out.println("  6. Admin Panel (CRUD)               ");
            }
            System.out.println("  7. Logout                           ");
            System.out.println("======================================");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    showMatchList();
                    break;
                case "2":
                    showSeatMap();
                    break;
                case "3":
                    handleBooking();
                    break;
                case "4":
                    showMyTickets();
                    break;
                case "5":
                    reportView.displayMenu(currentFan.getFanId());
                    break;
                case "6":
                    if (isAdmin) {
                        adminView.start();
                    } else {
                        System.out.println("Invalid option. Please try again.");
                    }
                    break;
                case "7":
                    fanController.logout();
                    System.out.println("[SUCCESS] Logged out successfully!");
                    return true;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void showMatchList() {
        System.out.println("\n==============================================================================");
        System.out.println("                             MATCH LIST                                       ");
        System.out.println("==============================================================================");

        List<Match> matches = stadiumController.getAllMatches();
        if (matches.isEmpty()) {
            System.out.println("No matches found in the system.");
            return;
        }

        System.out.printf("%-12s %-12s %-35s %-12s %-8s %-12s%n",
                "Match ID", "Stadium", "Title", "Date", "Time", "Status");
        System.out.println("-".repeat(95));

        for (Match match : matches) {
            System.out.printf("%-12s %-12s %-35s %-12s %-8s %-12s%n",
                    match.getMatchId(),
                    match.getStadiumId(),
                    match.getTitle(),
                    match.getMatchDate(),
                    match.getMatchTime(),
                    match.getStatus().name());
        }

        System.out.println("-".repeat(95));
        System.out.printf("Total: %d matches (%d available for booking)%n",
                matches.size(),
                matches.stream().filter(m -> m.getStatus() == MatchStatus.SCHEDULED).count());
    }

    private void showSeatMap() {
        System.out.println("\n--- VIEW SEAT MAP ---");

        System.out.print("Enter Match ID (e.g. MATCH001): ");
        String matchId = scanner.nextLine().trim();

        if (!stadiumController.matchExists(matchId)) {
            System.out.println("[FAILED] Match does not exist: " + matchId);
            return;
        }

        List<Section> sections = stadiumController.getSections();
        System.out.println("\nSeat Sections:");
        for (Section sec : sections) {
            int available = stadiumController.showAvailableSeats(sec.getSectionId(), matchId);
            System.out.printf("  %s - %-15s | Price: %,10d VND | Available: %d/%d seats%n",
                    sec.getSectionId(),
                    sec.getSectionType().name(),
                    sec.getBasePrice(),
                    available,
                    sec.getTotalCapacity());
        }

        System.out.print("\nEnter Section ID to view details (e.g. SEC001), or press Enter to skip: ");
        String sectionId = scanner.nextLine().trim();
        if (sectionId.isEmpty())
            return;

        Section selectedSection = sections.stream()
                .filter(s -> s.getSectionId().equals(sectionId))
                .findFirst().orElse(null);
        if (selectedSection == null) {
            System.out.println("No seats found in section " + sectionId + " for match " + matchId);
            return;
        }

        Seat[][] seatMap = stadiumController.buildSeatMap(sectionId, matchId);
        int availableCount = stadiumController.showAvailableSeats(sectionId, matchId);
        SeatMapView.displaySeatMap(seatMap, selectedSection, matchId, availableCount);
    }

    private void handleBooking() {
        Fan currentFan = fanController.getCurrentFan();

        System.out.println("\n======================================");
        System.out.println("             BOOK TICKET              ");
        System.out.println("======================================");

        List<Match> scheduledMatches = stadiumController.getMatches();
        if (scheduledMatches.isEmpty()) {
            System.out.println("No matches are currently open for booking.");
            return;
        }

        System.out.println("\nMatches available for booking:");
        for (Match m : scheduledMatches) {
            int available = stadiumController.getAvailableSeatsCount(m.getMatchId());
            System.out.printf("  %s | %-35s | %s %s | Available seats: %,d%n",
                    m.getMatchId(), m.getTitle(), m.getMatchDate(), m.getMatchTime(), available);
        }

        System.out.print("\nEnter Match ID: ");
        String matchId = scanner.nextLine().trim();

        Optional<Match> matchOpt = scheduledMatches.stream()
                .filter(m -> m.getMatchId().equals(matchId))
                .findFirst();

        if (!matchOpt.isPresent()) {
            System.out.println("[FAILED] Invalid Match ID or not open for booking.");
            return;
        }

        // ── Bước 1: Hiển thị và cho chọn Section ─────────────────────────────
        List<Section> sections = stadiumController.getSections();
        System.out.println("\nSeat Sections:");
        for (Section sec : sections) {
            List<Seat> availSeats = stadiumController.getAvailableSeatsBySectionAndMatch(
                    sec.getSectionId(), matchId);
            System.out.printf("  %s - %-15s | Price: %,10d VND | Available: %d seats%n",
                    sec.getSectionId(), sec.getSectionType().name(),
                    sec.getBasePrice(), availSeats.size());
        }

        System.out.print("\nEnter Section ID (e.g. SEC001): ");
        String sectionId = scanner.nextLine().trim();

        // Validate Section ID
        Optional<Section> sectionOpt = sections.stream()
                .filter(s -> s.getSectionId().equals(sectionId))
                .findFirst();
        if (!sectionOpt.isPresent()) {
            System.out.println("[FAILED] Invalid Section ID.");
            return;
        }

        // ── Bước 2: Hiển thị ghế trống trong Section vừa chọn ────────────────
        List<Seat> availableSeats = stadiumController.getAvailableSeatsBySectionAndMatch(
                sectionId, matchId);
        if (availableSeats.isEmpty()) {
            System.out.println("[FAILED] No available seats in this section.");
            return;
        }

        System.out.printf("\nAvailable seats in %s (%s - %,d VND):%n",
                sectionId,
                sectionOpt.get().getSectionType().name(),
                sectionOpt.get().getBasePrice());

        // In danh sách ghế trống theo hàng
        String currentRow = "";
        for (Seat s : availableSeats) {
            if (!s.getRowLabel().equals(currentRow)) {
                if (!currentRow.isEmpty())
                    System.out.println();
                System.out.print("  Row " + s.getRowLabel() + ": ");
                currentRow = s.getRowLabel();
            }
            System.out.print(s.getSeatId() + " ");
        }
        System.out.println();

        // ── Bước 3: Chọn Seat ID trong Section đó ────────────────────────────
        System.out.print("\nEnter Seat ID from the list above: ");
        String seatId = scanner.nextLine().trim();

        // Validate: ghế phải thuộc đúng Section vừa chọn
        boolean validSeat = availableSeats.stream()
                .anyMatch(s -> s.getSeatId().equals(seatId));
        if (!validSeat) {
            System.out.println("[FAILED] Seat ID không hợp lệ hoặc không thuộc section " + sectionId);
            return;
        }

        // ── Bước 1: LOCK ghế trước khi xác nhận ─────────────────────────────
        boolean locked = false;
        try {
            locked = bookingController.lockSeat(seatId);
        } catch (exception.SeatAlreadyBookedException e) {
            System.out.println("[FAILED] " + e.getMessage());
            return;
        }

        if (!locked) {
            System.out.println("[FAILED] Seat not found or unavailable: " + seatId);
            return;
        }

        // Lấy giá vé để hiển thị trước khi confirm
        long price = bookingController.getPriceForSeat(seatId);

        // ── Hiển thị thông tin xác nhận ──────────────────────────────────────
        System.out.println("\n======================================");
        System.out.println("       BOOKING CONFIRMATION           ");
        System.out.println("======================================");
        System.out.printf("  Fan    : %s%n", currentFan.getFullName());
        System.out.printf("  Match  : %s%n", matchId);
        System.out.printf("  Seat   : %s  [LOCKED - Held for you]%n", seatId);
        System.out.printf("  Price  : %,d VND%n", price);
        System.out.println("======================================");
        System.out.print("Confirm booking? (y/n): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (!"y".equals(confirm)) {
            // ── Bước 2b: User cancel → restore AVAILABLE ─────────────────────
            bookingController.cancelLockedSeat(seatId);
            System.out.println("[INFO] Booking cancelled. Seat " + seatId + " released.");
            return;
        }

        // ── Cho user chọn cơ chế đồng bộ ─────────────────────────────────────
        System.out.println("\nChoose synchronization mechanism:");
        System.out.println("  1. SYNCHRONIZED (JVM lock — default, safe)");
        System.out.println("  2. FILE_LOCK    (OS-level lock)");
        System.out.println("  3. OPTIMISTIC   (Version-based, retry on conflict)");
        System.out.println("  4. NO_LOCK      (No locking — unsafe, for testing only)");
        System.out.print("Select (1-4, default 1): ");
        String mechChoice = scanner.nextLine().trim();
        LockMechanism mechanism;
        switch (mechChoice) {
            case "2": mechanism = LockMechanism.FILE_LOCK;    break;
            case "3": mechanism = LockMechanism.OPTIMISTIC;   break;
            case "4": mechanism = LockMechanism.NO_LOCK;      break;
            default:  mechanism = LockMechanism.SYNCHRONIZED; break;
        }

        // ── Process Payment (giả lập) ────────────────────────────────────────
        System.out.println("\n======================================");
        System.out.println("          PROCESS PAYMENT             ");
        System.out.println("======================================");
        System.out.printf("  Amount to pay: %,d VND%n", price);
        System.out.println("======================================");
        System.out.print("Do you want to pay? (y/n): ");
        String paymentChoice = scanner.nextLine().trim().toLowerCase();

        if (!"y".equals(paymentChoice)) {
            bookingController.cancelLockedSeat(seatId);
            System.out.println("[INFO] Payment cancelled. Seat " + seatId + " released.");
            return;
        }

        // ── Bước 2a: CONFIRM → BOOKED ────────────────────────────────────────
        boolean success = false;
        try {
            success = bookingController.confirmBooking(
                    currentFan.getFanId(), matchId, seatId, mechanism);
        } catch (exception.SeatAlreadyBookedException e) {
            System.out.println("[FAILED] " + e.getMessage());
            return;
        }

        if (success) {
            System.out.println("======================================");
            System.out.println("     [SUCCESS] TICKET BOOKED!         ");
            System.out.println("======================================");
            System.out.printf("  Fan: %s%n  Match: %s%n  Seat: %s%n  Price: %,d VND%n",
                    currentFan.getFullName(), matchId, seatId, price);
        } else {
            System.out.println("[FAILED] Booking failed. Please try again.");
        }
    }

    private void showMyTickets() {
        Fan currentFan = fanController.getCurrentFan();
        List<Ticket> tickets = fanController.getMyTickets();

        System.out.println("\n======================================");
        System.out.println("       MY TICKET HISTORY (ALL)        ");
        System.out.println("======================================");

        if (tickets.isEmpty()) {
            System.out.println("You have no tickets.");
            return;
        }

        System.out.printf("%-16s %-12s %-14s %12s %-10s %-20s%n",
                "Ticket ID", "Match", "Seat", "Price (VND)", "Status", "Booking Date");
        System.out.println("-".repeat(90));

        long totalPrice = 0;
        for (Ticket t : tickets) {
            System.out.printf("%-16s %-12s %-14s %,12d %-10s %-20s%n",
                    t.getTicketId(),
                    t.getMatchId(),
                    t.getSeatId(),
                    t.getPrice(),
                    t.getStatus().name(),
                    t.getBookedAt());
            totalPrice += t.getPrice();
        }

        System.out.println("-".repeat(90));
        System.out.printf("Total: %d tickets | Total value: %,d VND%n", tickets.size(), totalPrice);

        // ── Wire BookingView: delegate sang BookingView để tái sử dụng logic hủy vé ──
        System.out.println("\nDo you want to cancel a ticket? (y to cancel, Enter to skip): ");
        System.out.print("> ");
        String input = scanner.nextLine().trim();
        if ("y".equalsIgnoreCase(input)) {
            bookingView.handleCancellation();
        }
    }
}
