package view;

import controller.AdminController;
import exception.EntityNotFoundException;
import model.entity.Match;
import model.entity.Section;
import model.entity.Stadium;
import model.enums.MatchStatus;
import model.enums.SectionType;

import java.util.List;
import java.util.Scanner;

/**
 * View quản trị hệ thống — CRUD Stadium, Section, Match.
 *
 * <p><b>Trách nhiệm:</b>
 * <ul>
 *   <li>Hiển thị menu Admin.</li>
 *   <li>Nhận input từ người dùng (Admin).</li>
 *   <li>Gọi {@link AdminController} để xử lý logic.</li>
 *   <li>Hiển thị kết quả ra màn hình.</li>
 * </ul>
 *
 * <p>View KHÔNG xử lý business logic — mọi validate và thao tác dữ liệu
 * đều được delegate sang {@link AdminController}.
 */
public class AdminView {

    private final AdminController adminController;
    private final Scanner         scanner;

    /**
     * Khởi tạo AdminView.
     *
     * @param adminController Controller xử lý admin logic.
     * @param scanner         Scanner dùng chung.
     */
    public AdminView(AdminController adminController, Scanner scanner) {
        this.adminController = adminController;
        this.scanner         = scanner;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MENU CHÍNH ADMIN
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Hiển thị menu Admin và xử lý điều hướng.
     */
    public void start() {
        while (true) {
            System.out.println("\n╔══════════════════════════════════════╗");
            System.out.println("║          ADMIN MANAGEMENT PANEL      ║");
            System.out.println("╠══════════════════════════════════════╣");
            System.out.println("║  1. Stadium Management               ║");
            System.out.println("║  2. Section Management               ║");
            System.out.println("║  3. Match Management                 ║");
            System.out.println("║  0. Back to Main Menu                ║");
            System.out.println("╚══════════════════════════════════════╝");
            System.out.print("Select option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1": stadiumMenu(); break;
                case "2": sectionMenu(); break;
                case "3": matchMenu();   break;
                case "0": return;
                default:
                    System.out.println("[ERROR] Invalid option.");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STADIUM MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    private void stadiumMenu() {
        while (true) {
            System.out.println("\n--- STADIUM MANAGEMENT ---");
            System.out.println("  1. List all stadiums");
            System.out.println("  2. View stadium details");
            System.out.println("  3. Create new stadium");
            System.out.println("  4. Update stadium");
            System.out.println("  5. Delete stadium");
            System.out.println("  0. Back");
            System.out.print("Select option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1": listStadiums();   break;
                case "2": viewStadium();    break;
                case "3": createStadium();  break;
                case "4": updateStadium();  break;
                case "5": deleteStadium();  break;
                case "0": return;
                default:
                    System.out.println("[ERROR] Invalid option.");
            }
        }
    }

    private void listStadiums() {
        List<Stadium> stadiums = adminController.listStadiums();
        System.out.println("\n=== STADIUM LIST (" + stadiums.size() + " total) ===");
        if (stadiums.isEmpty()) {
            System.out.println("  No stadiums found.");
            return;
        }
        System.out.printf("%-10s %-30s %-15s %-10s%n",
                "ID", "Name", "City", "Capacity");
        System.out.println("-".repeat(70));
        for (Stadium s : stadiums) {
            System.out.printf("%-10s %-30s %-15s %,10d%n",
                    s.getStadiumId(), s.getName(), s.getCity(), s.getTotalCapacity());
        }
    }

    private void viewStadium() {
        System.out.print("Enter Stadium ID: ");
        String id = scanner.nextLine().trim();
        try {
            Stadium s = adminController.getStadium(id);
            System.out.println("\n=== STADIUM DETAIL ===");
            System.out.println("  ID       : " + s.getStadiumId());
            System.out.println("  Name     : " + s.getName());
            System.out.println("  City     : " + s.getCity());
            System.out.println("  Address  : " + s.getAddress());
            System.out.printf("  Capacity : %,d seats%n", s.getTotalCapacity());
        } catch (EntityNotFoundException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private void createStadium() {
        System.out.println("\n--- CREATE STADIUM ---");
        System.out.print("Stadium ID (e.g. STD004): ");
        String id = scanner.nextLine().trim();
        System.out.print("Name: ");
        String name = scanner.nextLine().trim();
        System.out.print("City: ");
        String city = scanner.nextLine().trim();
        System.out.print("Address: ");
        String address = scanner.nextLine().trim();
        System.out.print("Total Capacity: ");
        int capacity = parseIntOrDefault(scanner.nextLine().trim(), 0);

        try {
            Stadium s = adminController.createStadium(id, name, city, address, capacity);
            System.out.println("[SUCCESS] Stadium created: " + s.getStadiumId() + " - " + s.getName());
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private void updateStadium() {
        System.out.println("\n--- UPDATE STADIUM ---");
        System.out.print("Stadium ID to update: ");
        String id = scanner.nextLine().trim();

        // Hiển thị thông tin hiện tại
        try {
            Stadium current = adminController.getStadium(id);
            System.out.printf("Current: %s | %s | %s | Capacity: %,d%n",
                    current.getName(), current.getCity(),
                    current.getAddress(), current.getTotalCapacity());
        } catch (EntityNotFoundException e) {
            System.out.println("[ERROR] " + e.getMessage());
            return;
        }

        System.out.print("New Name (Enter to keep): ");
        String name = scanner.nextLine().trim();
        System.out.print("New City (Enter to keep): ");
        String city = scanner.nextLine().trim();
        System.out.print("New Address (Enter to keep): ");
        String address = scanner.nextLine().trim();
        System.out.print("New Capacity (0 to keep): ");
        int capacity = parseIntOrDefault(scanner.nextLine().trim(), 0);

        // Dùng giá trị cũ nếu để trống
        try {
            Stadium current = adminController.getStadium(id);
            Stadium updated = adminController.updateStadium(
                    id,
                    name.isEmpty() ? current.getName() : name,
                    city.isEmpty() ? current.getCity() : city,
                    address.isEmpty() ? current.getAddress() : address,
                    capacity == 0 ? current.getTotalCapacity() : capacity
            );
            System.out.println("[SUCCESS] Stadium updated: " + updated.getStadiumId());
        } catch (EntityNotFoundException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private void deleteStadium() {
        System.out.print("Enter Stadium ID to delete: ");
        String id = scanner.nextLine().trim();
        System.out.print("Are you sure? (y/n): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (!"y".equals(confirm)) {
            System.out.println("[INFO] Delete cancelled.");
            return;
        }
        try {
            adminController.deleteStadium(id);
            System.out.println("[SUCCESS] Stadium deleted: " + id);
        } catch (EntityNotFoundException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    private void sectionMenu() {
        while (true) {
            System.out.println("\n--- SECTION MANAGEMENT ---");
            System.out.println("  1. List all sections");
            System.out.println("  2. View section details");
            System.out.println("  3. Create new section");
            System.out.println("  4. Update section");
            System.out.println("  5. Delete section");
            System.out.println("  0. Back");
            System.out.print("Select option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1": listSections();  break;
                case "2": viewSection();   break;
                case "3": createSection(); break;
                case "4": updateSection(); break;
                case "5": deleteSection(); break;
                case "0": return;
                default:
                    System.out.println("[ERROR] Invalid option.");
            }
        }
    }

    private void listSections() {
        List<Section> sections = adminController.listSections();
        System.out.println("\n=== SECTION LIST (" + sections.size() + " total) ===");
        if (sections.isEmpty()) {
            System.out.println("  No sections found.");
            return;
        }
        System.out.printf("%-10s %-20s %-10s %-14s %-15s%n",
                "ID", "Type", "Rows", "Seats/Row", "Base Price (VND)");
        System.out.println("-".repeat(75));
        for (Section s : sections) {
            System.out.printf("%-10s %-20s %-10d %-14d %,15d%n",
                    s.getSectionId(), s.getSectionType().name(),
                    s.getTotalRows(), s.getSeatsPerRow(), s.getBasePrice());
        }
    }

    private void viewSection() {
        System.out.print("Enter Section ID: ");
        String id = scanner.nextLine().trim();
        try {
            Section s = adminController.getSection(id);
            System.out.println("\n=== SECTION DETAIL ===");
            System.out.println("  ID          : " + s.getSectionId());
            System.out.println("  Type        : " + s.getSectionType().name());
            System.out.println("  Total Rows  : " + s.getTotalRows());
            System.out.println("  Seats/Row   : " + s.getSeatsPerRow());
            System.out.printf("  Base Price  : %,d VND%n", s.getBasePrice());
            System.out.printf("  Total Seats : %,d%n", s.getTotalCapacity());
        } catch (EntityNotFoundException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private void createSection() {
        System.out.println("\n--- CREATE SECTION ---");
        System.out.print("Section ID (e.g. SEC005): ");
        String id = scanner.nextLine().trim();
        System.out.println("Section Types: VIP, STANDARD, STANDING, ECONOMY_LOWER");
        System.out.print("Type: ");
        SectionType type = parseSectionType(scanner.nextLine().trim());
        if (type == null) {
            System.out.println("[ERROR] Invalid section type.");
            return;
        }
        System.out.print("Total Rows: ");
        int rows = parseIntOrDefault(scanner.nextLine().trim(), 0);
        System.out.print("Seats per Row: ");
        int seatsPerRow = parseIntOrDefault(scanner.nextLine().trim(), 0);
        System.out.print("Base Price (VND): ");
        long price = parseLongOrDefault(scanner.nextLine().trim(), 0L);

        try {
            Section s = adminController.createSection(id, type, rows, seatsPerRow, price);
            System.out.println("[SUCCESS] Section created: " + s.getSectionId()
                    + " (" + s.getSectionType().name() + ")");
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private void updateSection() {
        System.out.println("\n--- UPDATE SECTION ---");
        System.out.print("Section ID to update: ");
        String id = scanner.nextLine().trim();

        Section current;
        try {
            current = adminController.getSection(id);
            System.out.printf("Current: %s | %d rows | %d seats/row | %,d VND%n",
                    current.getSectionType().name(), current.getTotalRows(),
                    current.getSeatsPerRow(), current.getBasePrice());
        } catch (EntityNotFoundException e) {
            System.out.println("[ERROR] " + e.getMessage());
            return;
        }

        System.out.println("Section Types: VIP, STANDARD, STANDING, ECONOMY_LOWER");
        System.out.print("New Type (Enter to keep " + current.getSectionType().name() + "): ");
        String typeStr = scanner.nextLine().trim();
        SectionType type = typeStr.isEmpty()
                ? current.getSectionType()
                : parseSectionType(typeStr);
        if (type == null) {
            System.out.println("[ERROR] Invalid section type.");
            return;
        }

        System.out.print("New Rows (0 to keep " + current.getTotalRows() + "): ");
        int rows = parseIntOrDefault(scanner.nextLine().trim(), 0);
        System.out.print("New Seats/Row (0 to keep " + current.getSeatsPerRow() + "): ");
        int seatsPerRow = parseIntOrDefault(scanner.nextLine().trim(), 0);
        System.out.print("New Price (0 to keep " + current.getBasePrice() + "): ");
        long price = parseLongOrDefault(scanner.nextLine().trim(), 0L);

        try {
            Section updated = adminController.updateSection(
                    id, type,
                    rows == 0 ? current.getTotalRows() : rows,
                    seatsPerRow == 0 ? current.getSeatsPerRow() : seatsPerRow,
                    price == 0 ? current.getBasePrice() : price
            );
            System.out.println("[SUCCESS] Section updated: " + updated.getSectionId());
        } catch (EntityNotFoundException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private void deleteSection() {
        System.out.print("Enter Section ID to delete: ");
        String id = scanner.nextLine().trim();
        System.out.print("Are you sure? (y/n): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (!"y".equals(confirm)) {
            System.out.println("[INFO] Delete cancelled.");
            return;
        }
        try {
            adminController.deleteSection(id);
            System.out.println("[SUCCESS] Section deleted: " + id);
        } catch (EntityNotFoundException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MATCH MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    private void matchMenu() {
        while (true) {
            System.out.println("\n--- MATCH MANAGEMENT ---");
            System.out.println("  1. List all matches");
            System.out.println("  2. View match details");
            System.out.println("  3. Create new match");
            System.out.println("  4. Update match");
            System.out.println("  5. Update match status");
            System.out.println("  6. Delete match");
            System.out.println("  0. Back");
            System.out.print("Select option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1": listMatches();        break;
                case "2": viewMatch();          break;
                case "3": createMatch();        break;
                case "4": updateMatch();        break;
                case "5": updateMatchStatus();  break;
                case "6": deleteMatch();        break;
                case "0": return;
                default:
                    System.out.println("[ERROR] Invalid option.");
            }
        }
    }

    private void listMatches() {
        List<Match> matches = adminController.listMatches();
        System.out.println("\n=== MATCH LIST (" + matches.size() + " total) ===");
        if (matches.isEmpty()) {
            System.out.println("  No matches found.");
            return;
        }
        System.out.printf("%-12s %-10s %-35s %-12s %-8s %-12s%n",
                "Match ID", "Stadium", "Title", "Date", "Time", "Status");
        System.out.println("-".repeat(95));
        for (Match m : matches) {
            System.out.printf("%-12s %-10s %-35s %-12s %-8s %-12s%n",
                    m.getMatchId(), m.getStadiumId(), m.getTitle(),
                    m.getMatchDate(), m.getMatchTime(), m.getStatus().name());
        }
    }

    private void viewMatch() {
        System.out.print("Enter Match ID: ");
        String id = scanner.nextLine().trim();
        try {
            Match m = adminController.getMatch(id);
            System.out.println("\n=== MATCH DETAIL ===");
            System.out.println("  ID        : " + m.getMatchId());
            System.out.println("  Stadium   : " + m.getStadiumId());
            System.out.println("  Home Team : " + m.getHomeTeam());
            System.out.println("  Away Team : " + m.getAwayTeam());
            System.out.println("  Date      : " + m.getMatchDate());
            System.out.println("  Time      : " + m.getMatchTime());
            System.out.println("  Status    : " + m.getStatus().name());
        } catch (EntityNotFoundException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private void createMatch() {
        System.out.println("\n--- CREATE MATCH ---");
        System.out.print("Match ID (e.g. MATCH013): ");
        String id = scanner.nextLine().trim();
        System.out.print("Stadium ID (e.g. STD001): ");
        String stadiumId = scanner.nextLine().trim();
        System.out.print("Home Team: ");
        String homeTeam = scanner.nextLine().trim();
        System.out.print("Away Team: ");
        String awayTeam = scanner.nextLine().trim();
        System.out.print("Match Date (yyyy-MM-dd): ");
        String date = scanner.nextLine().trim();
        System.out.print("Match Time (HH:mm): ");
        String time = scanner.nextLine().trim();
        System.out.println("Status: SCHEDULED / ONGOING / COMPLETED");
        System.out.print("Status (default SCHEDULED): ");
        String statusStr = scanner.nextLine().trim();
        MatchStatus status = parseMatchStatus(statusStr);

        try {
            Match m = adminController.createMatch(id, stadiumId, homeTeam, awayTeam,
                    date, time, status);
            System.out.println("[SUCCESS] Match created: " + m.getMatchId()
                    + " | " + m.getTitle());
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private void updateMatch() {
        System.out.println("\n--- UPDATE MATCH ---");
        System.out.print("Match ID to update: ");
        String id = scanner.nextLine().trim();

        Match current;
        try {
            current = adminController.getMatch(id);
            System.out.printf("Current: %s vs %s | %s %s | Stadium: %s | Status: %s%n",
                    current.getHomeTeam(), current.getAwayTeam(),
                    current.getMatchDate(), current.getMatchTime(),
                    current.getStadiumId(), current.getStatus().name());
        } catch (EntityNotFoundException e) {
            System.out.println("[ERROR] " + e.getMessage());
            return;
        }

        System.out.print("New Stadium ID (Enter to keep): ");
        String stadiumId = scanner.nextLine().trim();
        System.out.print("New Home Team (Enter to keep): ");
        String homeTeam = scanner.nextLine().trim();
        System.out.print("New Away Team (Enter to keep): ");
        String awayTeam = scanner.nextLine().trim();
        System.out.print("New Date yyyy-MM-dd (Enter to keep): ");
        String date = scanner.nextLine().trim();
        System.out.print("New Time HH:mm (Enter to keep): ");
        String time = scanner.nextLine().trim();
        System.out.println("Status: SCHEDULED / ONGOING / COMPLETED");
        System.out.print("New Status (Enter to keep " + current.getStatus().name() + "): ");
        String statusStr = scanner.nextLine().trim();

        try {
            Match updated = adminController.updateMatch(
                    id,
                    stadiumId.isEmpty() ? current.getStadiumId() : stadiumId,
                    homeTeam.isEmpty()  ? current.getHomeTeam()  : homeTeam,
                    awayTeam.isEmpty()  ? current.getAwayTeam()  : awayTeam,
                    date.isEmpty()      ? current.getMatchDate()  : date,
                    time.isEmpty()      ? current.getMatchTime()  : time,
                    statusStr.isEmpty() ? current.getStatus() : parseMatchStatus(statusStr)
            );
            System.out.println("[SUCCESS] Match updated: " + updated.getMatchId());
        } catch (EntityNotFoundException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private void updateMatchStatus() {
        System.out.print("Enter Match ID: ");
        String id = scanner.nextLine().trim();
        System.out.println("Status options: SCHEDULED / ONGOING / COMPLETED");
        System.out.print("New Status: ");
        String statusStr = scanner.nextLine().trim();

        try {
            Match m = adminController.updateMatchStatus(id, parseMatchStatus(statusStr));
            System.out.println("[SUCCESS] Match " + id + " status → " + m.getStatus().name());
        } catch (EntityNotFoundException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private void deleteMatch() {
        System.out.print("Enter Match ID to delete: ");
        String id = scanner.nextLine().trim();
        System.out.print("Are you sure? (y/n): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (!"y".equals(confirm)) {
            System.out.println("[INFO] Delete cancelled.");
            return;
        }
        try {
            adminController.deleteMatch(id);
            System.out.println("[SUCCESS] Match deleted: " + id);
        } catch (EntityNotFoundException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    private int parseIntOrDefault(String s, int def) {
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return def; }
    }

    private long parseLongOrDefault(String s, long def) {
        try { return Long.parseLong(s); }
        catch (NumberFormatException e) { return def; }
    }

    private SectionType parseSectionType(String s) {
        try { return SectionType.valueOf(s.toUpperCase()); }
        catch (Exception e) { return null; }
    }

    private MatchStatus parseMatchStatus(String s) {
        try { return MatchStatus.valueOf(s.toUpperCase()); }
        catch (Exception e) { return MatchStatus.SCHEDULED; }
    }
}
