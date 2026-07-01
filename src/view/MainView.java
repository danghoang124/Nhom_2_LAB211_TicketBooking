package view;

import controller.BookingController;
import controller.FanController;
import controller.ReportController;
import main.AppContext;
import model.entity.Fan;
import model.entity.Match;
import model.entity.Seat;
import model.entity.Section;
import model.entity.Ticket;
import model.enums.LockMechanism;
import model.enums.MatchStatus;
import model.enums.SeatStatus;
import repository.MatchRepository;
import repository.SeatRepository;
import repository.SectionRepository;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class MainView {

    private final AppContext appContext;
    private final Scanner scanner;

    private final FanController fanController;
    private final BookingController bookingController;
    private final ReportController reportController;

    private final MatchRepository matchRepository;
    private final SeatRepository seatRepository;
    private final SectionRepository sectionRepository;

    private final LoginView loginView;
    private final RegisterView registerView;
    private final BookingView bookingView;
    private final ReportView reportView;

    public MainView(AppContext appContext) {
        this.appContext = appContext;
        this.scanner = new Scanner(System.in);

        this.fanController = appContext.getFanController();
        this.bookingController = appContext.getBookingController();
        this.reportController = appContext.getReportController();

        this.matchRepository = appContext.getMatchRepository();
        this.seatRepository = appContext.getSeatRepository();
        this.sectionRepository = appContext.getSectionRepository();

        this.loginView = new LoginView(fanController, scanner);
        this.registerView = new RegisterView(fanController, scanner);
        this.bookingView = new BookingView(bookingController);
        this.reportView = new ReportView(reportController, scanner);
    }

    public void start() {
        System.out.println("\n======================================================");
        System.out.println("          STADIUM TICKET BOOKING SYSTEM               ");
        System.out.println("             FPT UNIVERSITY - LAB211                  ");
        System.out.println("======================================================");

        while (true) {
            if (!fanController.isLoggedIn()) {
                boolean shouldExit = showGuestMenu();
                if (shouldExit) break;
            } else {
                boolean loggedOut = showMainMenu();
                if (loggedOut) continue;
            }
        }
    }

    private boolean showGuestMenu() {
        System.out.println("\n=== LOGIN MENU ===");
        System.out.println("1. Login");
        System.out.println("2. Register new account");
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
            case "0":
                return true; 
            default:
                System.out.println("Invalid option. Please try again.");
                return false;
        }
    }

    private boolean showMainMenu() {
        Fan currentFan = fanController.getCurrentFan();

        while (true) {
            System.out.println("\n======================================");
            System.out.printf("  Welcome: %-25s  %n", currentFan.getFullName());
            System.out.println("--------------------------------------");
            System.out.println("  1. View match list                  ");
            System.out.println("  2. View seat map by match           ");
            System.out.println("  3. Book a ticket                    ");
            System.out.println("  4. View my tickets                  ");
            System.out.println("  5. Reports & Statistics             ");
            System.out.println("  6. Logout                           ");
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

        List<Match> matches = matchRepository.findAll();
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

        if (!matchRepository.existsById(matchId)) {
            System.out.println("[FAILED] Match does not exist: " + matchId);
            return;
        }

        List<Section> sections = sectionRepository.findAll();
        System.out.println("\nSeat Sections:");
        for (Section sec : sections) {
            List<Seat> seatsInSection = seatRepository.findBySectionAndMatch(sec.getSectionId(), matchId);
            long availableCount = seatsInSection.stream().filter(Seat::isAvailable).count();
            System.out.printf("  %s - %-15s | Price: %,10d VND | Available: %d/%d seats%n",
                    sec.getSectionId(),
                    sec.getSectionType().name(),
                    sec.getBasePrice(),
                    availableCount,
                    seatsInSection.size());
        }

        System.out.print("\nEnter Section ID to view details (e.g. SEC001), or press Enter to skip: ");
        String sectionId = scanner.nextLine().trim();
        if (sectionId.isEmpty()) return;

        List<Seat> seats = seatRepository.findBySectionAndMatch(sectionId, matchId);
        if (seats.isEmpty()) {
            System.out.println("No seats found in section " + sectionId + " for match " + matchId);
            return;
        }

        System.out.printf("\n--- Seat Map: %s | Match: %s ---%n", sectionId, matchId);
        System.out.println("  [O] = Available    [X] = Booked    [L] = Locked\n");

        String currentRow = "";
        for (Seat seat : seats) {
            if (!seat.getRowLabel().equals(currentRow)) {
                if (!currentRow.isEmpty()) System.out.println();
                currentRow = seat.getRowLabel();
                System.out.printf("  Row %-3s: ", currentRow);
            }

            switch (seat.getStatus()) {
                case AVAILABLE:
                    System.out.print("[O] ");
                    break;
                case BOOKED:
                    System.out.print("[X] ");
                    break;
                case LOCKED:
                    System.out.print("[L] ");
                    break;
            }
        }
        System.out.println();

        long avail = seats.stream().filter(s -> s.getStatus() == SeatStatus.AVAILABLE).count();
        long booked = seats.stream().filter(s -> s.getStatus() == SeatStatus.BOOKED).count();
        System.out.printf("\n  Total: %d seats | Available: %d | Booked: %d%n", seats.size(), avail, booked);
    }

    private void handleBooking() {
        Fan currentFan = fanController.getCurrentFan();

        System.out.println("\n======================================");
        System.out.println("             BOOK TICKET              ");
        System.out.println("======================================");

        List<Match> scheduledMatches = matchRepository.findByStatus(MatchStatus.SCHEDULED);
        if (scheduledMatches.isEmpty()) {
            System.out.println("No matches are currently open for booking.");
            return;
        }

        System.out.println("\nMatches available for booking:");
        for (Match m : scheduledMatches) {
            int available = seatRepository.countAvailable(m.getMatchId());
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

        List<Section> sections = sectionRepository.findAll();
        System.out.println("\nSeat Sections:");
        for (Section sec : sections) {
            List<Seat> availSeats = seatRepository.findAvailableBySectionAndMatch(sec.getSectionId(), matchId);
            System.out.printf("  %s - %-15s | Price: %,10d VND | Available seats: %d%n",
                    sec.getSectionId(), sec.getSectionType().name(), sec.getBasePrice(), availSeats.size());
        }

        System.out.print("\nEnter Seat ID (e.g. SEAT000001): ");
        String seatId = scanner.nextLine().trim();

        System.out.printf("Confirm booking? Fan: %s | Match: %s | Seat: %s (y/n): ",
                currentFan.getFullName(), matchId, seatId);
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (!"y".equals(confirm)) {
            System.out.println("Booking cancelled.");
            return;
        }

        boolean success = bookingController.bookSeat(currentFan.getFanId(), matchId, seatId, LockMechanism.NO_LOCK);

        if (success) {
            System.out.println("======================================");
            System.out.println("     [SUCCESS] TICKET BOOKED!         ");
            System.out.println("======================================");
            System.out.printf("  Fan: %s%n  Match: %s%n  Seat: %s%n", currentFan.getFullName(), matchId, seatId);
        } else {
            System.out.println("[FAILED] Booking failed! Seat is already booked or does not exist.");
        }
    }

    private void showMyTickets() {
        Fan currentFan = fanController.getCurrentFan();
        List<Ticket> tickets = fanController.getMyTickets();

        System.out.println("\n======================================");
        System.out.println("           MY TICKETS                 ");
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
    }
}
