package generator;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * DataGenerator — LAB211 Stadium Ticket Booking Simulation
 * FPT University · OOP with Java · MVC Architecture
 *
 * <p>
 * Generates all CSV seed data files required by the system:
 * <ul>
 * <li>stadiums.csv — 3 stadiums</li>
 * <li>sections.csv — 4 sections (shared across all stadiums)</li>
 * <li>matches.csv — 12 matches (4 per stadium)</li>
 * <li>seats.csv — 34,440 rows ≥ 10,000 requirement ✓</li>
 * <li>fans.csv — 500 fans</li>
 * <li>tickets.csv — empty (populated at runtime)</li>
 * <li>transactions.csv — empty (populated by Simulator)</li>
 * </ul>
 *
 * <p>
 * Requires Java 17+ (uses records).
 *
 * <p>
 * Usage:
 * 
 * <pre>
 *   javac -d out src/generator/DataGenerator.java
 *   java  -cp out generator.DataGenerator
 * </pre>
 */
public class DataGenerator {

    // ── output directory ─────────────────────────────────────────────────────
    private static final String DATA_DIR = "data/";

    // ── date formatters ───────────────────────────────────────────────────────
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── reproducible randomness ───────────────────────────────────────────────
    private static final Random RNG = new Random(42);

    // ── domain lookup tables ──────────────────────────────────────────────────
    private static final String[] SECTION_TYPES = { "VIP", "STANDARD", "STANDING", "ECONOMY_LOWER" };

    private static final String[] TEAMS = {
            "Hà Nội FC", "Hoàng Anh Gia Lai", "TP.HCM FC", "Bình Dương FC",
            "Đà Nẵng FC", "Viettel FC", "Nam Định FC", "Thanh Hóa FC",
            "Hải Phòng FC", "Long An FC", "Sông Lam Nghệ An", "Cần Thơ FC"
    };

    private static final String[] HO = // họ (family names)
            { "Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Phan", "Vũ", "Bùi", "Đỗ", "Hồ" };
    private static final String[] TEN_DEM = // tên đệm (middle names)
            { "Văn", "Thị", "Minh", "Quốc", "Hữu", "Đức", "Thành", "Bảo", "Ngọc", "Xuân" };
    private static final String[] TEN = // tên (given names)
            { "An", "Bình", "Cường", "Dũng", "Phong", "Giang", "Hùng", "Khoa",
                    "Minh", "Nam", "Phúc", "Quân", "Sơn", "Tú", "Vinh", "Xuân", "Yến" };

    // ── section capacity blueprint: { totalRows, seatsPerRow } ───────────────
    // VIP=200, STANDARD=600, STANDING=875, ECONOMY_LOWER=875
    // 4 shared sections, seats generated per (section × match) for all stadiums
    private static final int[][] SECTION_DIMS = {
            { 10, 20 }, // VIP
            { 20, 30 }, // STANDARD
            { 25, 35 }, // STANDING
            { 25, 35 }, // ECONOMY_LOWER
    };
    // VIP=500k, STANDARD=200k, STANDING=100k, ECONOMY_LOWER=80k
    // Thứ tự phải khớp với SECTION_TYPES[] và docs/csv_schema.md
    private static final long[] SECTION_PRICES = { 500_000L, 200_000L, 100_000L, 80_000L };

    // ─────────────────────────────────────────────────────────────────────────
    // Records (Java 17)
    // ─────────────────────────────────────────────────────────────────────────

    record Stadium(String stadiumId, String name, String city,
            String address, int totalCapacity) {
    }

    record Section(String sectionId, String sectionType,
            int totalRows, int seatsPerRow, long basePrice) {
    }

    record Match(String matchId, String stadiumId, String homeTeam,
            String awayTeam, String matchDate, String matchTime, String status) {
    }

    record Seat(String seatId, String sectionId, String matchId,
            String rowLabel, int seatNumber, String status, int version) {
    }

    record Fan(String fanId, String username, String passwordHash,
            String fullName, String email, String phone,
            String createdAt, boolean isActive, String role) {
    }

    // ─────────────────────────────────────────────────────────────────────────
    // main
    // ─────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        printBanner();
        try {
            new File(DATA_DIR).mkdirs();

            // --- generate all entities ---
            step("Generating stadiums...");
            List<Stadium> stadiums = generateStadiums();

            step("Generating sections...");
            List<Section> sections = generateSections(stadiums);

            step("Generating matches...");
            List<Match> matches = generateMatches(stadiums);

            step("Generating seats (largest file, please wait)...");
            List<Seat> seats = generateSeats(sections, matches);

            step("Generating fans...");
            List<Fan> fans = generateFans(500);

            // --- write CSV files ---
            step("Writing CSV files...");

            writeCSV(DATA_DIR + "stadiums.csv",
                    "stadiumId,name,city,address,totalCapacity",
                    stadiums.stream().map(DataGenerator::stadiumToCsv).toList());

            writeCSV(DATA_DIR + "sections.csv",
                    "sectionId,sectionType,totalRows,seatsPerRow,basePrice",
                    sections.stream().map(DataGenerator::sectionToCsv).toList());

            writeCSV(DATA_DIR + "matches.csv",
                    "matchId,stadiumId,homeTeam,awayTeam,matchDate,matchTime,status",
                    matches.stream().map(DataGenerator::matchToCsv).toList());

            writeCSV(DATA_DIR + "seats.csv",
                    "seatId,sectionId,matchId,rowLabel,seatNumber,status,version",
                    seats.stream().map(DataGenerator::seatToCsv).toList());

            writeCSV(DATA_DIR + "fans.csv",
                    "fanId,username,passwordHash,fullName,email,phone,createdAt,isActive,role",
                    fans.stream().map(DataGenerator::fanToCsv).toList());

            // Empty files — populated at runtime by BookingController / SimulatorController
            writeCSV(DATA_DIR + "tickets.csv",
                    "ticketId,fanId,seatId,matchId,transactionId,price,bookedAt,status",
                    List.of());

            writeCSV(DATA_DIR + "transactions.csv",
                    "transactionId,fanId,matchId,numberOfTickets,totalAmount," +
                            "status,mechanism,createdAt,durationMs",
                    List.of());

            // --- summary ---
            printSummary(stadiums, sections, matches, seats, fans);

        } catch (Exception e) {
            System.err.println("\n❌  ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Generators
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates 3 representative Vietnamese football stadiums.
     */
    static List<Stadium> generateStadiums() {
        return List.of(
                new Stadium("STD001", "Sân Vận Động Mỹ Đình", "Hà Nội",
                        "Phường Mỹ Đình II, Nam Từ Liêm, Hà Nội", 40_192),
                new Stadium("STD002", "Sân Vận Động Thống Nhất", "TP.HCM",
                        "138 Đặng Văn Bi, Thủ Đức, TP.HCM", 15_000),
                new Stadium("STD003", "Sân Vận Động Pleiku", "Gia Lai",
                        "Trần Nhật Duật, Pleiku, Gia Lai", 12_000));
    }

    /**
     * Creates 4 shared sections (VIP / STANDARD / STANDING / ECONOMY_LOWER).
     * These sections are shared across all stadiums — seats are generated
     * per (section × match) combination.
     */
    static List<Section> generateSections(List<Stadium> stadiums) {
        List<Section> result = new ArrayList<>();
        for (int i = 0; i < SECTION_TYPES.length; i++) {
            result.add(new Section(
                    fmt("SEC%03d", i + 1),
                    SECTION_TYPES[i],
                    SECTION_DIMS[i][0],
                    SECTION_DIMS[i][1],
                    SECTION_PRICES[i]));
        }
        return result;
    }

    /**
     * Creates 4 matches per stadium (3 SCHEDULED + 1 COMPLETED).
     * 3 stadiums × 4 matches = 12 matches total.
     */
    static List<Match> generateMatches(List<Stadium> stadiums) {
        List<Match> result = new ArrayList<>();
        int counter = 1;
        LocalDate baseDate = LocalDate.of(2025, 1, 15);
        String[] times = { "16:00", "18:00", "19:30", "20:00" };
        String[] statuses = { "SCHEDULED", "SCHEDULED", "SCHEDULED", "COMPLETED" };

        for (Stadium s : stadiums) {
            // Shuffle teams so each stadium has a different set of matchups
            List<String> teams = new ArrayList<>(Arrays.asList(TEAMS));
            Collections.shuffle(teams, RNG);

            for (int i = 0; i < 4; i++) {
                String home = teams.get(i * 2 % teams.size());
                String away = teams.get((i * 2 + 1) % teams.size());
                LocalDate date = baseDate.plusWeeks(counter); // spread dates out
                result.add(new Match(
                        fmt("MATCH%03d", counter++),
                        s.stadiumId(), home, away,
                        date.format(DATE_FMT), times[i], statuses[i]));
            }
        }
        return result;
    }

    /**
     * Creates one seat record per (section × match × row × seatNumber).
     *
     * <p>
     * Formula: 2,870 seats/stadium × 4 matches × 3 stadiums = <b>34,440 rows</b>
     * — well above the 10,000 minimum requirement.
     *
     * <p>
     * All seats are initialised as AVAILABLE with version = 0.
     */
    static List<Seat> generateSeats(List<Section> sections, List<Match> matches) {
        List<Seat> result = new ArrayList<>(36_000); // pre-size for performance
        int seatCounter = 1;

        // Sections are shared across all stadiums:
        // generate seats for every (section × match) combination
        for (Section sec : sections) {
            for (Match match : matches) {
                for (int rowIdx = 0; rowIdx < sec.totalRows(); rowIdx++) {
                    String rowLabel = toRowLabel(rowIdx); // A, B, …, Z, AA, AB, …
                    for (int seatNum = 1; seatNum <= sec.seatsPerRow(); seatNum++) {
                        result.add(new Seat(
                                fmt("SEAT%06d", seatCounter++),
                                sec.sectionId(),
                                match.matchId(),
                                rowLabel,
                                seatNum,
                                "AVAILABLE", // initial status
                                0 // initial version (Optimistic Locking)
                        ));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Generates {@code count} fans with realistic Vietnamese names and unique
     * usernames, plus one default admin account (admin / admin123).
     * Passwords are SHA-256 hashed (hex, upper-case).
     */
    static List<Fan> generateFans(int count) {
        List<Fan> result = new ArrayList<>(count + 1);
        Set<String> usedUsernames = new HashSet<>();
        Set<String> usedPhones = new HashSet<>();

        for (int i = 1; i <= count; i++) {
            String ho = HO[RNG.nextInt(HO.length)];
            String tenDem = TEN_DEM[RNG.nextInt(TEN_DEM.length)];
            String ten = TEN[RNG.nextInt(TEN.length)];
            String fullName = ho + " " + tenDem + " " + ten;

            // Build ASCII username from name parts
            String base = ascii(ten).toLowerCase() + ascii(ho).toLowerCase().charAt(0)
                    + ascii(tenDem).toLowerCase().charAt(0);
            String username = base;
            int suffix = 1;
            while (usedUsernames.contains(username)) {
                username = base + suffix++;
            }
            usedUsernames.add(username);

            String email = username + "@gmail.com";

            // Generate a unique phone number (re-roll on collision)
            String phone;
            do {
                phone = "0" + (900_000_000 + RNG.nextInt(99_999_999));
            } while (usedPhones.contains(phone));
            usedPhones.add(phone);

            String created = LocalDateTime.now()
                    .minusDays(RNG.nextInt(730)) // registered within last 2 years
                    .format(DATETIME_FMT);
            String pwHash = sha256("password" + i);

            result.add(new Fan(
                    fmt("FAN%04d", i),
                    username, pwHash, fullName, email, phone, created, true, "FAN"));
        }

        // ── Default admin account ────────────────────────────────────────────
        String adminPwHash = sha256("admin123");
        String adminCreated = LocalDateTime.now().format(DATETIME_FMT);
        result.add(new Fan(
                fmt("FAN%04d", count + 1),
                "admin", adminPwHash, "System Administrator",
                "admin@system.vn", "0900000000", adminCreated, true, "ADMIN"));

        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CSV serialization (one method per record type for clarity)
    // ─────────────────────────────────────────────────────────────────────────

    private static String stadiumToCsv(Stadium s) {
        return joinCsv(s.stadiumId(), s.name(), s.city(), s.address(), s.totalCapacity());
    }

    private static String sectionToCsv(Section s) {
        return joinCsv(s.sectionId(), s.sectionType(),
                s.totalRows(), s.seatsPerRow(), s.basePrice());
    }

    private static String matchToCsv(Match m) {
        return joinCsv(m.matchId(), m.stadiumId(), m.homeTeam(), m.awayTeam(),
                m.matchDate(), m.matchTime(), m.status());
    }

    private static String seatToCsv(Seat s) {
        return joinCsv(s.seatId(), s.sectionId(), s.matchId(),
                s.rowLabel(), s.seatNumber(), s.status(), s.version());
    }

    private static String fanToCsv(Fan f) {
        return joinCsv(f.fanId(), f.username(), f.passwordHash(), f.fullName(),
                f.email(), f.phone(), f.createdAt(), f.isActive(), f.role());
    }

    /**
     * Joins fields into a RFC-4180-compliant CSV row.
     * Fields containing commas, double-quotes, or newlines are wrapped in
     * double-quotes
     * and any internal double-quotes are escaped as "".
     */
    private static String joinCsv(Object... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0)
                sb.append(',');
            String val = String.valueOf(fields[i]);
            if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
                sb.append('"').append(val.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(val);
            }
        }
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // File I/O
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Writes {@code header} + all {@code rows} to {@code path} in UTF-8.
     * Uses buffered I/O for performance on large files.
     */
    private static void writeCSV(String path, String header, List<String> rows)
            throws IOException {
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8),
                256 * 1024)) { // 256 KB buffer for large files
            bw.write(header);
            bw.newLine();
            for (String row : rows) {
                bw.write(row);
                bw.newLine();
            }
        }
        System.out.printf("   ✓  %-40s %,7d data rows%n", path, rows.size());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Converts a 0-based row index to an alphabetical label.
     * 0→A, 25→Z, 26→AA, 27→AB, …
     */
    private static String toRowLabel(int index) {
        if (index < 26) {
            return String.valueOf((char) ('A' + index));
        }
        return String.valueOf((char) ('A' + (index / 26) - 1))
                + (char) ('A' + (index % 26));
    }

    /** Strips Vietnamese diacritics → ASCII-safe string for username building. */
    private static String ascii(String s) {
        return s
                .replaceAll("[àáâãăạảấầẩẫậắằẳẵặ]", "a")
                .replaceAll("[ÀÁÂÃĂẠẢẤẦẨẪẬẮẰẲẴẶ]", "A")
                .replaceAll("[èéêẹẻẽếềểễệ]", "e")
                .replaceAll("[ÈÉÊẸẺẼẾỀỂỄỆ]", "E")
                .replaceAll("[ìíịỉĩ]", "i")
                .replaceAll("[ÌÍỊỈĨ]", "I")
                .replaceAll("[òóôõơọỏốồổỗộớờởỡợ]", "o")
                .replaceAll("[ÒÓÔÕƠỌỎỐỒỔỖỘỚỜỞỠỢ]", "O")
                .replaceAll("[ùúưụủũứừửữự]", "u")
                .replaceAll("[ÙÚƯỤỦŨỨỪỬỮỰ]", "U")
                .replaceAll("[ỳýỵỷỹ]", "y")
                .replaceAll("[ỲÝỴỶỸ]", "Y")
                .replaceAll("[đ]", "d")
                .replaceAll("[Đ]", "D")
                .replaceAll("[^a-zA-Z0-9]", "");
    }

    /** Returns upper-case hex SHA-256 of the input string. */
    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash)
                hex.append(String.format("%02X", b));
            return hex.toString();
        } catch (Exception e) {
            // Fallback: plain hex of hashCode (should never happen on any JVM)
            return String.format("%08X", input.hashCode()).toUpperCase();
        }
    }

    /** Shorthand for String.format. */
    private static String fmt(String pattern, Object... args) {
        return String.format(pattern, args);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Console output helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static void printBanner() {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║   LAB211 · Stadium Ticket Booking Simulation         ║");
        System.out.println("║   DataGenerator — CSV Seed Data Builder              ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();
    }

    private static void step(String msg) {
        System.out.println("▶  " + msg);
    }

    private static void printSummary(List<Stadium> stadiums, List<Section> sections,
            List<Match> matches, List<Seat> seats, List<Fan> fans) {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║                  GENERATION SUMMARY                 ║");
        System.out.println("╠══════════════════════════════════════════════════════╣");
        System.out.printf("║  %-20s  %,10d records             ║%n", "Stadiums", stadiums.size());
        System.out.printf("║  %-20s  %,10d shared sections     ║%n", "Sections", sections.size());
        System.out.printf("║  %-20s  %,10d records             ║%n", "Matches", matches.size());
        System.out.printf("║  %-20s  %,10d records  ✓ ≥10,000  ║%n", "Seats", seats.size());
        System.out.printf("║  %-20s  %,10d records  (+1 admin) ║%n", "Fans", fans.size() - 1);
        System.out.printf("║  %-20s  %,10d records  (runtime)  ║%n", "Tickets", 0);
        System.out.printf("║  %-20s  %,10d records  (runtime)  ║%n", "Transactions", 0);
        System.out.println("╠══════════════════════════════════════════════════════╣");
        System.out.println("║  Output dir: " + DATA_DIR + "                                    ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println("\n✅  Done! All CSV files are ready in: " + DATA_DIR);
    }
}
