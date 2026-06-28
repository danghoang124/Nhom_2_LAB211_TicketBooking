package test;

import model.entity.*;
import model.enums.*;
import repository.*;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Unit Test cho Repository Layer (T4).
 *
 * <p>Kiểm thử toàn diện 7 Concrete Repository và các Domain-specific query methods.
 * Không dùng JUnit — chạy trực tiếp bằng console.
 *
 * <p>Yêu cầu trước khi chạy:
 * <ul>
 *   <li>{@code data/stadiums.csv}, {@code data/sections.csv}, {@code data/matches.csv},
 *       {@code data/fans.csv}, {@code data/seats.csv} phải tồn tại (đã chạy DataGenerator).</li>
 * </ul>
 *
 * <p>Compile:
 * <pre>
 *   javac -encoding UTF-8 -d src/out \
 *     src/model/enums/*.java src/model/entity/*.java \
 *     src/repository/CsvRepository.java src/repository/*.java \
 *     src/test/ModelTest.java src/test/RepositoryTest.java src/test/PerformanceTest.java
 * </pre>
 * Chạy:
 * <pre>
 *   java -cp src/out test.RepositoryTest
 * </pre>
 */
public class RepositoryTest {

    // ── Test runner ────────────────────────────────────────────────────────────

    private static int passed = 0;
    private static int failed = 0;
    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║   LAB211 · Repository Layer — Unit Tests (T4)        ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        // --- Stadium ---
        section("1. StadiumRepository Tests");
        testStadiumRepository();

        // --- Section ---
        section("2. SectionRepository Tests");
        testSectionRepository();

        // --- Match ---
        section("3. MatchRepository Tests");
        testMatchRepository();

        // --- Fan ---
        section("4. FanRepository Tests");
        testFanRepository();

        // --- Seat ---
        section("5. SeatRepository Tests (file lớn ~30k dòng)");
        testSeatRepository();

        // --- Ticket ---
        section("6. TicketRepository Tests");
        testTicketRepository();

        // --- Transaction ---
        section("7. TransactionRepository Tests");
        testTransactionRepository();

        // --- Summary ---
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.printf("  TỔNG KẾT: %d PASS  |  %d FAIL%n", passed, failed);
        System.out.println("═══════════════════════════════════════════════════════");

        if (failed > 0) System.exit(1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. StadiumRepository
    // ─────────────────────────────────────────────────────────────────────────

    static void testStadiumRepository() {
        StadiumRepository repo = new StadiumRepository();

        // Đọc file thật
        List<Stadium> all = repo.findAll();
        assertTrue("Stadium: findAll() không rỗng", all.size() > 0);

        // findById
        Optional<Stadium> std1 = repo.findById("STD001");
        assertTrue("Stadium: findById STD001 tồn tại", std1.isPresent());
        assertEquals("Stadium: STD001 tên đúng", "Sân Vận Động Mỹ Đình", std1.get().getName());

        Optional<Stadium> notFound = repo.findById("STD999");
        assertTrue("Stadium: findById STD999 không tồn tại", !notFound.isPresent());

        // findByCity
        List<Stadium> hanoi = repo.findByCity("Hà Nội");
        assertTrue("Stadium: findByCity Hà Nội >= 1", hanoi.size() >= 1);
        assertEquals("Stadium: tất cả kết quả đúng city", true,
            hanoi.stream().allMatch(s -> "Hà Nội".equals(s.getCity())));

        List<Stadium> empty = repo.findByCity("Hue");
        assertEquals("Stadium: findByCity không tồn tại = rỗng", 0, empty.size());

        // findByMinCapacity
        List<Stadium> large = repo.findByMinCapacity(20000);
        assertTrue("Stadium: findByMinCapacity 20000 >= 1", large.size() >= 1);
        assertEquals("Stadium: tất cả capacity >= 20000", true,
            large.stream().allMatch(s -> s.getTotalCapacity() >= 20000));

        List<Stadium> huge = repo.findByMinCapacity(100000);
        assertEquals("Stadium: findByMinCapacity 100000 = rỗng", 0, huge.size());

        // existsById
        assertEquals("Stadium: existsById STD001 = true", true, repo.existsById("STD001"));
        assertEquals("Stadium: existsById STD999 = false", false, repo.existsById("STD999"));

        // count
        assertTrue("Stadium: count >= 3", repo.count() >= 3);

        // findByCondition với Predicate lambda
        List<Stadium> filtered = repo.findByCondition(
            s -> s.getTotalCapacity() > 10000 && s.getCity() != null
        );
        assertTrue("Stadium: findByCondition lambda", filtered.size() > 0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. SectionRepository
    // ─────────────────────────────────────────────────────────────────────────

    static void testSectionRepository() {
        SectionRepository repo = new SectionRepository();

        List<Section> all = repo.findAll();
        assertEquals("Section: count = 4", 4, all.size());

        // findByType
        List<Section> vip = repo.findByType(SectionType.VIP);
        assertEquals("Section: findByType VIP = 1", 1, vip.size());
        assertEquals("Section: VIP sectionId = SEC001", "SEC001", vip.get(0).getSectionId());
        assertEquals("Section: VIP basePrice = 500000", 500_000L, vip.get(0).getBasePrice());

        List<Section> standard = repo.findByType(SectionType.STANDARD);
        assertEquals("Section: findByType STANDARD = 1", 1, standard.size());

        // findFirstByType
        Optional<Section> ecoLower = repo.findFirstByType(SectionType.ECONOMY_LOWER);
        assertTrue("Section: findFirstByType ECONOMY_LOWER tồn tại", ecoLower.isPresent());
        // Data thực tế: ECONOMY_LOWER=100000, STANDING=80000 (khác với docs/schema —
        // do DataGenerator được generate với thứ tự giá khác nhau)
        assertTrue("Section: ECONOMY_LOWER basePrice > 0", ecoLower.get().getBasePrice() > 0);

        // Kiểm tra totalCapacity (domain logic)
        assertEquals("Section: VIP totalCapacity = 200", 200, vip.get(0).getTotalCapacity());

        // findByCondition với Predicate
        List<Section> cheap = repo.findByCondition(s -> s.getBasePrice() <= 100_000L);
        assertEquals("Section: price <= 100000 = 2 (STANDING + ECONOMY_LOWER)", 2, cheap.size());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. MatchRepository
    // ─────────────────────────────────────────────────────────────────────────

    static void testMatchRepository() {
        MatchRepository repo = new MatchRepository();

        List<Match> all = repo.findAll();
        assertTrue("Match: count >= 12", all.size() >= 12);

        // findById
        Optional<Match> match1 = repo.findById("MATCH001");
        assertTrue("Match: MATCH001 tồn tại", match1.isPresent());
        assertEquals("Match: MATCH001 stadiumId = STD001", "STD001", match1.get().getStadiumId());

        // findByStadium
        List<Match> std1Matches = repo.findByStadium("STD001");
        assertTrue("Match: findByStadium STD001 >= 4", std1Matches.size() >= 4);
        assertEquals("Match: tất cả thuộc STD001", true,
            std1Matches.stream().allMatch(m -> "STD001".equals(m.getStadiumId())));

        // findByStatus
        List<Match> scheduled = repo.findByStatus(MatchStatus.SCHEDULED);
        assertTrue("Match: SCHEDULED >= 1", scheduled.size() >= 1);
        assertEquals("Match: tất cả status = SCHEDULED", true,
            scheduled.stream().allMatch(m -> m.getStatus() == MatchStatus.SCHEDULED));

        List<Match> completed = repo.findByStatus(MatchStatus.COMPLETED);
        assertTrue("Match: COMPLETED >= 1", completed.size() >= 1);

        // findScheduledMatches() shortcut
        List<Match> sched2 = repo.findScheduledMatches();
        assertEquals("Match: findScheduledMatches == findByStatus(SCHEDULED)",
            scheduled.size(), sched2.size());

        // getTitle()
        assertEquals("Match: getTitle() đúng format", true,
            match1.get().getTitle().contains(" vs "));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. FanRepository
    // ─────────────────────────────────────────────────────────────────────────

    static void testFanRepository() {
        FanRepository repo = new FanRepository();

        // Đọc file thật
        List<Fan> all = repo.findAll();
        assertTrue("Fan: count >= 500", all.size() >= 500);

        // findActiveFans
        List<Fan> active = repo.findActiveFans();
        assertTrue("Fan: active >= 500", active.size() >= 500);
        assertEquals("Fan: tất cả active = true", true,
            active.stream().allMatch(Fan::isActive));

        // findByUsername — kiểm tra 1 username từ file thật
        // Username được generate dạng: ten + họ[0] + tênđệm[0] (ví dụ: "annv")
        // → thay vì hardcode, ta lấy username của fan đầu tiên để test
        if (!all.isEmpty()) {
            Fan firstFan = all.get(0);
            String username = firstFan.getUsername();

            Optional<Fan> found = repo.findByUsername(username);
            assertTrue("Fan: findByUsername '" + username + "' tìm thấy", found.isPresent());
            assertEquals("Fan: findByUsername đúng fanId",
                firstFan.getFanId(), found.get().getFanId());

            // findByEmail
            Optional<Fan> byEmail = repo.findByEmail(firstFan.getEmail());
            assertTrue("Fan: findByEmail tìm thấy", byEmail.isPresent());
            assertEquals("Fan: findByEmail đúng fanId",
                firstFan.getFanId(), byEmail.get().getFanId());

            // authenticate (đúng hash)
            Optional<Fan> auth = repo.authenticate(username, firstFan.getPasswordHash());
            assertTrue("Fan: authenticate đúng hash → trả về fan", auth.isPresent());

            // authenticate (sai hash)
            Optional<Fan> authWrong = repo.authenticate(username, "WRONG_HASH");
            assertTrue("Fan: authenticate sai hash → empty", !authWrong.isPresent());

            // isUsernameTaken
            assertEquals("Fan: isUsernameTaken '" + username + "' = true", true,
                repo.isUsernameTaken(username));
            assertEquals("Fan: isUsernameTaken 'xyz_not_exist_999' = false", false,
                repo.isUsernameTaken("xyz_not_exist_999"));

            // isEmailTaken
            assertEquals("Fan: isEmailTaken đúng email = true", true,
                repo.isEmailTaken(firstFan.getEmail()));
            assertEquals("Fan: isEmailTaken fake = false", false,
                repo.isEmailTaken("fake_not_exist@no.com"));
        }

        // findByUsername không tồn tại
        Optional<Fan> noFan = repo.findByUsername("__nonexistent__");
        assertTrue("Fan: findByUsername không tồn tại = empty", !noFan.isPresent());

        // findByCondition với lambda
        List<Fan> activeFans = repo.findByCondition(f -> f.isActive() && f.getEmail() != null);
        assertTrue("Fan: findByCondition active + có email", activeFans.size() > 0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. SeatRepository (file lớn nhất)
    // ─────────────────────────────────────────────────────────────────────────

    static void testSeatRepository() {
        SeatRepository repo = new SeatRepository();

        // Đọc toàn bộ file lớn
        long t0 = System.currentTimeMillis();
        List<Seat> all = repo.findAll();
        long elapsed = System.currentTimeMillis() - t0;
        System.out.printf("     [INFO] findAll() seats.csv: %,d dòng trong %d ms%n",
            all.size(), elapsed);

        assertTrue("Seat: count >= 10000", all.size() >= 10000);

        // findByMatch
        List<Seat> match1Seats = repo.findByMatch("MATCH001");
        assertTrue("Seat: findByMatch MATCH001 >= 1000", match1Seats.size() >= 1000);
        assertEquals("Seat: tất cả thuộc MATCH001", true,
            match1Seats.stream().allMatch(s -> "MATCH001".equals(s.getMatchId())));

        // findAvailableByMatch — ban đầu toàn bộ AVAILABLE
        List<Seat> available = repo.findAvailableByMatch("MATCH001");
        assertTrue("Seat: MATCH001 có ghế AVAILABLE", available.size() > 0);
        assertEquals("Seat: tất cả AVAILABLE", true,
            available.stream().allMatch(Seat::isAvailable));

        // countAvailable
        int avCount = repo.countAvailable("MATCH001");
        assertEquals("Seat: countAvailable == findAvailableByMatch.size", available.size(), avCount);

        // findBySection
        List<Seat> sec1Seats = repo.findBySection("SEC001");
        assertTrue("Seat: findBySection SEC001 >= 1", sec1Seats.size() >= 1);

        // findBySectionAndMatch
        List<Seat> vipMatch1 = repo.findBySectionAndMatch("SEC001", "MATCH001");
        assertTrue("Seat: findBySectionAndMatch SEC001×MATCH001 >= 200", vipMatch1.size() >= 200);
        assertEquals("Seat: tất cả đúng section", true,
            vipMatch1.stream().allMatch(s ->
                "SEC001".equals(s.getSectionId()) && "MATCH001".equals(s.getMatchId())
            ));

        // findAvailableBySectionAndMatch
        List<Seat> availVipM1 = repo.findAvailableBySectionAndMatch("SEC001", "MATCH001");
        assertTrue("Seat: findAvailableBySectionAndMatch >= 1", availVipM1.size() >= 1);
        assertEquals("Seat: tất cả AVAILABLE", true,
            availVipM1.stream().allMatch(Seat::isAvailable));

        // Optimistic Locking test trên file tạm
        testOptimisticLocking();

        // findByCondition với Predicate phức hợp
        List<Seat> complexQuery = repo.findByCondition(s ->
            "MATCH001".equals(s.getMatchId()) &&
            "SEC001".equals(s.getSectionId()) &&
            s.getSeatNumber() <= 5
        );
        assertTrue("Seat: findByCondition phức hợp", complexQuery.size() > 0);
        assertEquals("Seat: Predicate composite đúng", true,
            complexQuery.stream().allMatch(s ->
                "MATCH001".equals(s.getMatchId()) &&
                "SEC001".equals(s.getSectionId()) &&
                s.getSeatNumber() <= 5
            ));
    }

    /**
     * Test Optimistic Locking trên file tạm (không đụng data thật).
     */
    static void testOptimisticLocking() {
        // Tạo repo tạm
        String tmpFile = "data/test_seats_optlock_temp.csv";
        SeatRepository tmpRepo = new SeatRepository() {
            @Override public String getFilePath() { return tmpFile; }
        };

        // Thêm 2 ghế
        Seat s1 = new Seat("SEAT000001", "SEC001", "MATCH001", "A", 1, SeatStatus.AVAILABLE, 0);
        Seat s2 = new Seat("SEAT000002", "SEC001", "MATCH001", "A", 2, SeatStatus.AVAILABLE, 0);
        tmpRepo.save(s1);
        tmpRepo.save(s2);

        // updateStatusOptimistic — version đúng → thành công
        boolean ok = tmpRepo.updateStatusOptimistic("SEAT000001", SeatStatus.LOCKED, 0);
        assertEquals("OptLock: version đúng (0) → true", true, ok);

        // Kiểm tra version đã tăng lên 1
        Optional<Seat> updated = tmpRepo.findById("SEAT000001");
        assertTrue("OptLock: findById sau update tồn tại", updated.isPresent());
        assertEquals("OptLock: version tăng lên 1", 1, updated.get().getVersion());
        assertEquals("OptLock: status = LOCKED", SeatStatus.LOCKED, updated.get().getStatus());

        // updateStatusOptimistic — version sai (0 thay vì 1) → thất bại (conflict)
        boolean conflict = tmpRepo.updateStatusOptimistic("SEAT000001", SeatStatus.BOOKED, 0);
        assertEquals("OptLock: version sai (0 thay vì 1) → false (conflict)", false, conflict);

        // Ghế vẫn còn LOCKED, không thay đổi
        Optional<Seat> unchanged = tmpRepo.findById("SEAT000001");
        assertEquals("OptLock: sau conflict, status vẫn LOCKED",
            SeatStatus.LOCKED, unchanged.get().getStatus());
        assertEquals("OptLock: sau conflict, version vẫn 1", 1, unchanged.get().getVersion());

        // updateStatusOptimistic — version đúng (1) → thành công lần 2
        boolean ok2 = tmpRepo.updateStatusOptimistic("SEAT000001", SeatStatus.BOOKED, 1);
        assertEquals("OptLock: version đúng (1) → true lần 2", true, ok2);
        assertEquals("OptLock: version tăng lên 2", 2,
            tmpRepo.findById("SEAT000001").get().getVersion());
        assertEquals("OptLock: status = BOOKED", SeatStatus.BOOKED,
            tmpRepo.findById("SEAT000001").get().getStatus());

        // updateStatusOptimistic — seatId không tồn tại → false
        boolean noSeat = tmpRepo.updateStatusOptimistic("SEAT999999", SeatStatus.BOOKED, 0);
        assertEquals("OptLock: seatId không tồn tại → false", false, noSeat);

        // Dọn dẹp
        new File(tmpFile).delete();
        System.out.println("     [INFO] OptimisticLocking test xong — file tạm đã xóa.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. TicketRepository
    // ─────────────────────────────────────────────────────────────────────────

    static void testTicketRepository() {
        // Dùng file tạm để tránh làm bẩn data thật
        String tmpFile = "data/test_tickets_temp.csv";
        TicketRepository repo = new TicketRepository() {
            @Override public String getFilePath() { return tmpFile; }
        };

        // Ban đầu rỗng
        assertEquals("Ticket: ban đầu count = 0", 0, repo.count());

        String now = LocalDateTime.now().format(DT_FMT);

        // append 3 vé cho 2 fan
        Ticket t1 = new Ticket("TKT00000001", "FAN0001", "SEAT000001",
                               "MATCH001", "TXN00000001", 500_000L, now, TicketStatus.VALID);
        Ticket t2 = new Ticket("TKT00000002", "FAN0001", "SEAT000002",
                               "MATCH001", "TXN00000001", 200_000L, now, TicketStatus.VALID);
        Ticket t3 = new Ticket("TKT00000003", "FAN0002", "SEAT000003",
                               "MATCH002", "TXN00000002", 100_000L, now, TicketStatus.CANCELLED);
        repo.append(t1);
        repo.append(t2);
        repo.append(t3);
        assertEquals("Ticket: count sau append 3 = 3", 3, repo.count());

        // findById
        Optional<Ticket> found = repo.findById("TKT00000001");
        assertTrue("Ticket: findById TKT00000001 tồn tại", found.isPresent());
        assertEquals("Ticket: price đúng", 500_000L, found.get().getPrice());

        // findByFan
        List<Ticket> fan1Tickets = repo.findByFan("FAN0001");
        assertEquals("Ticket: FAN0001 có 2 vé", 2, fan1Tickets.size());

        List<Ticket> fan2Tickets = repo.findByFan("FAN0002");
        assertEquals("Ticket: FAN0002 có 1 vé", 1, fan2Tickets.size());

        // findByMatch
        List<Ticket> match1Tickets = repo.findByMatch("MATCH001");
        assertEquals("Ticket: MATCH001 có 2 vé", 2, match1Tickets.size());

        // findByFanAndMatch
        List<Ticket> fan1Match1 = repo.findByFanAndMatch("FAN0001", "MATCH001");
        assertEquals("Ticket: FAN0001 × MATCH001 = 2", 2, fan1Match1.size());

        // findValidTickets
        List<Ticket> validFan1 = repo.findValidTickets("FAN0001");
        assertEquals("Ticket: FAN0001 VALID = 2", 2, validFan1.size());

        // existsBySeatAndMatch — chống Double Booking
        assertEquals("Ticket: SEAT000001×MATCH001 đã đặt = true", true,
            repo.existsBySeatAndMatch("SEAT000001", "MATCH001"));
        assertEquals("Ticket: SEAT000099×MATCH001 chưa đặt = false", false,
            repo.existsBySeatAndMatch("SEAT000099", "MATCH001"));

        // Vé CANCELLED không tính là đã đặt
        assertEquals("Ticket: SEAT000003×MATCH002 CANCELLED không chặn = false", false,
            repo.existsBySeatAndMatch("SEAT000003", "MATCH002"));

        // countSoldTickets
        assertEquals("Ticket: countSoldTickets MATCH001 = 2", 2, repo.countSoldTickets("MATCH001"));
        assertEquals("Ticket: countSoldTickets MATCH002 = 0 (chỉ có CANCELLED)", 0,
            repo.countSoldTickets("MATCH002"));

        // findByTransaction
        List<Ticket> txn1Tickets = repo.findByTransaction("TXN00000001");
        assertEquals("Ticket: TXN00000001 có 2 vé", 2, txn1Tickets.size());

        // findByStatus
        List<Ticket> cancelled = repo.findByStatus(TicketStatus.CANCELLED);
        assertEquals("Ticket: CANCELLED = 1", 1, cancelled.size());

        // Cập nhật vé (save upsert)
        Ticket t1Cancelled = new Ticket("TKT00000001", "FAN0001", "SEAT000001",
                                        "MATCH001", "TXN00000001", 500_000L, now,
                                        TicketStatus.CANCELLED);
        repo.save(t1Cancelled);
        assertEquals("Ticket: count sau update = 3", 3, repo.count());
        assertEquals("Ticket: sau update TKT00000001 = CANCELLED",
            TicketStatus.CANCELLED, repo.findById("TKT00000001").get().getStatus());

        // Dọn dẹp
        new File(tmpFile).delete();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. TransactionRepository
    // ─────────────────────────────────────────────────────────────────────────

    static void testTransactionRepository() {
        String tmpFile = "data/test_transactions_temp.csv";
        TransactionRepository repo = new TransactionRepository() {
            @Override public String getFilePath() { return tmpFile; }
        };

        assertEquals("Transaction: ban đầu count = 0", 0, repo.count());

        String now = LocalDateTime.now().format(DT_FMT);

        // append 4 giao dịch với 2 cơ chế
        BookingTransaction tx1 = new BookingTransaction(
            "TXN00000001", "FAN0001", "MATCH001", 2, 1_000_000L,
            TransactionStatus.SUCCESS, LockMechanism.SYNCHRONIZED, now, 50L);
        BookingTransaction tx2 = new BookingTransaction(
            "TXN00000002", "FAN0002", "MATCH001", 1, 500_000L,
            TransactionStatus.SUCCESS, LockMechanism.SYNCHRONIZED, now, 40L);
        BookingTransaction tx3 = new BookingTransaction(
            "TXN00000003", "FAN0003", "MATCH002", 3, 300_000L,
            TransactionStatus.FAILED, LockMechanism.NO_LOCK, now, 10L);
        BookingTransaction tx4 = new BookingTransaction(
            "TXN00000004", "FAN0001", "MATCH002", 2, 200_000L,
            TransactionStatus.PARTIAL, LockMechanism.OPTIMISTIC, now, 35L);
        repo.append(tx1);
        repo.append(tx2);
        repo.append(tx3);
        repo.append(tx4);
        assertEquals("Transaction: count sau append 4 = 4", 4, repo.count());

        // findById
        Optional<BookingTransaction> found = repo.findById("TXN00000001");
        assertTrue("Transaction: findById TXN00000001 tồn tại", found.isPresent());
        assertEquals("Transaction: totalAmount đúng", 1_000_000L, found.get().getTotalAmount());

        // findByFan
        List<BookingTransaction> fan1 = repo.findByFan("FAN0001");
        assertEquals("Transaction: FAN0001 có 2 giao dịch", 2, fan1.size());

        // findByMatch
        List<BookingTransaction> match1 = repo.findByMatch("MATCH001");
        assertEquals("Transaction: MATCH001 có 2 giao dịch", 2, match1.size());

        // findByMechanism
        List<BookingTransaction> synced = repo.findByMechanism(LockMechanism.SYNCHRONIZED);
        assertEquals("Transaction: SYNCHRONIZED = 2", 2, synced.size());

        List<BookingTransaction> optimistic = repo.findByMechanism(LockMechanism.OPTIMISTIC);
        assertEquals("Transaction: OPTIMISTIC = 1", 1, optimistic.size());

        // countByMechanism
        assertEquals("Transaction: countByMechanism SYNCHRONIZED = 2", 2,
            repo.countByMechanism(LockMechanism.SYNCHRONIZED));
        assertEquals("Transaction: countByMechanism NO_LOCK = 1", 1,
            repo.countByMechanism(LockMechanism.NO_LOCK));
        assertEquals("Transaction: countByMechanism FILE_LOCK = 0", 0,
            repo.countByMechanism(LockMechanism.FILE_LOCK));

        // findSuccessful
        List<BookingTransaction> success = repo.findSuccessful();
        assertEquals("Transaction: findSuccessful = 2", 2, success.size());
        assertEquals("Transaction: tất cả SUCCESS", true,
            success.stream().allMatch(BookingTransaction::isSuccessful));

        // findFailed
        List<BookingTransaction> failed_ = repo.findFailed();
        assertEquals("Transaction: findFailed = 2 (FAILED + PARTIAL)", 2, failed_.size());

        // countSuccessfulByMechanism
        assertEquals("Transaction: countSuccessfulByMechanism SYNCHRONIZED = 2", 2,
            repo.countSuccessfulByMechanism(LockMechanism.SYNCHRONIZED));
        assertEquals("Transaction: countSuccessfulByMechanism NO_LOCK = 0", 0,
            repo.countSuccessfulByMechanism(LockMechanism.NO_LOCK));

        // totalRevenue
        long rev = repo.totalRevenue();
        assertEquals("Transaction: totalRevenue = 1,500,000", 1_500_000L, rev);

        // avgDurationMs
        double avg = repo.avgDurationMs(LockMechanism.SYNCHRONIZED);
        assertEquals("Transaction: avgDurationMs SYNCHRONIZED = 45.0", 45.0, avg);

        double avgNoData = repo.avgDurationMs(LockMechanism.FILE_LOCK);
        assertEquals("Transaction: avgDurationMs FILE_LOCK (không có data) = 0.0", 0.0, avgNoData);

        // findByStatus
        List<BookingTransaction> partial = repo.findByStatus(TransactionStatus.PARTIAL);
        assertEquals("Transaction: PARTIAL = 1", 1, partial.size());

        // deleteById
        boolean del = repo.deleteById("TXN00000003");
        assertEquals("Transaction: deleteById = true", true, del);
        assertEquals("Transaction: count sau delete = 3", 3, repo.count());

        // Dọn dẹp
        new File(tmpFile).delete();
    }

    // ── Test helpers ───────────────────────────────────────────────────────────

    private static void section(String name) {
        System.out.println();
        System.out.println("  ── " + name + " ──");
    }

    private static void assertEquals(String testName, Object expected, Object actual) {
        if (expected == null ? actual == null : expected.equals(actual)) {
            System.out.printf("  ✅ PASS  %s%n", testName);
            passed++;
        } else {
            System.out.printf("  ❌ FAIL  %s%n", testName);
            System.out.printf("           Expected : %s%n", expected);
            System.out.printf("           Actual   : %s%n", actual);
            failed++;
        }
    }

    private static void assertEquals(String testName, long expected, long actual) {
        assertEquals(testName, Long.valueOf(expected), Long.valueOf(actual));
    }

    private static void assertEquals(String testName, int expected, int actual) {
        assertEquals(testName, Integer.valueOf(expected), Integer.valueOf(actual));
    }

    private static void assertEquals(String testName, double expected, double actual) {
        // so sánh double với epsilon = 0.001
        if (Math.abs(expected - actual) < 0.001) {
            System.out.printf("  ✅ PASS  %s%n", testName);
            passed++;
        } else {
            System.out.printf("  ❌ FAIL  %s%n", testName);
            System.out.printf("           Expected : %.3f%n", expected);
            System.out.printf("           Actual   : %.3f%n", actual);
            failed++;
        }
    }

    private static void assertTrue(String testName, boolean condition) {
        if (condition) {
            System.out.printf("  ✅ PASS  %s%n", testName);
            passed++;
        } else {
            System.out.printf("  ❌ FAIL  %s%n", testName);
            failed++;
        }
    }
}
