package test;

import controller.BookingController;
import controller.FanController;
import controller.ReportController;
import model.entity.*;
import model.enums.*;
import repository.*;

import org.junit.jupiter.api.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit Integration Test — kiểm tra luồng end-to-end từ đăng nhập đến đặt vé.
 *
 * <p>Test KHÔNG qua console (gọi trực tiếp Controller), sử dụng data thật
 * từ file CSV hiện có trong thư mục data/.
 *
 * <p>Luồng test chính:
 * <pre>
 *   Register → Login → Xem trận → Đặt vé → Kiểm tra vé → Hủy vé → Kiểm tra report
 * </pre>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MainViewIntegrationTest {

    // ── Shared state across ordered tests ────────────────────────────────────
    private static FanRepository fanRepo;
    private static MatchRepository matchRepo;
    private static SeatRepository seatRepo;
    private static SectionRepository sectionRepo;
    private static TicketRepository ticketRepo;
    private static TransactionRepository txnRepo;

    private static FanController fanController;
    private static BookingController bookingController;
    private static ReportController reportController;

    // Test user credentials
    private static final String TEST_USERNAME = "testuser_integration";
    private static final String TEST_PASSWORD = "Test@1234";
    private static final String TEST_FULLNAME = "Nguyen Van Test";
    private static final String TEST_EMAIL = "testuser_integration@test.com";
    private static final String TEST_PHONE = "0900000001";

    // State stored between tests
    private static String registeredFanId;
    private static String bookedTicketId;
    private static String testMatchId;
    private static String testSeatId;

    @BeforeAll
    static void setUp() {
        // Khởi tạo repositories
        fanRepo = new FanRepository();
        matchRepo = new MatchRepository();
        seatRepo = new SeatRepository();
        sectionRepo = new SectionRepository();
        ticketRepo = new TicketRepository();
        txnRepo = new TransactionRepository();

        // Khởi tạo controllers
        fanController = new FanController(fanRepo, ticketRepo);
        bookingController = new BookingController(seatRepo, ticketRepo, txnRepo);
        reportController = new ReportController(ticketRepo, txnRepo, matchRepo, seatRepo, sectionRepo);

        // Cleanup: xóa user test cũ nếu tồn tại
        Optional<Fan> existingFan = fanRepo.findByUsername(TEST_USERNAME);
        if (existingFan.isPresent()) {
            fanRepo.deleteById(existingFan.get().getFanId());
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // TEST 1: ĐĂNG KÝ TÀI KHOẢN
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("1. Đăng ký tài khoản mới → thành công")
    void testRegisterSuccess() {
        Optional<Fan> result = fanController.register(
                TEST_USERNAME, TEST_PASSWORD, TEST_FULLNAME, TEST_EMAIL, TEST_PHONE);

        assertTrue(result.isPresent(), "Đăng ký phải thành công");
        Fan fan = result.get();
        assertEquals(TEST_USERNAME, fan.getUsername());
        assertEquals(TEST_FULLNAME, fan.getFullName());
        assertEquals(TEST_EMAIL, fan.getEmail());
        assertTrue(fan.isActive());

        registeredFanId = fan.getFanId();
        assertNotNull(registeredFanId, "FanId không được null");
        System.out.println("[PASS] Đăng ký thành công: " + registeredFanId);
    }

    @Test
    @Order(2)
    @DisplayName("1b. Đăng ký trùng username → thất bại")
    void testRegisterDuplicateUsername() {
        Optional<Fan> result = fanController.register(
                TEST_USERNAME, "other123", "Another Name", "other@test.com", "0911111111");

        assertFalse(result.isPresent(), "Đăng ký trùng username phải thất bại");
        System.out.println("[PASS] Đăng ký trùng username bị từ chối");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // TEST 2: ĐĂNG NHẬP
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @Order(3)
    @DisplayName("2a. Đăng nhập đúng → thành công")
    void testLoginSuccess() {
        // Logout trước (từ register auto-login)
        fanController.logout();
        assertFalse(fanController.isLoggedIn());

        Optional<Fan> result = fanController.login(TEST_USERNAME, TEST_PASSWORD);
        assertTrue(result.isPresent(), "Đăng nhập phải thành công");
        assertTrue(fanController.isLoggedIn(), "Phải ở trạng thái đã đăng nhập");
        assertEquals(registeredFanId, fanController.getCurrentFan().getFanId());
        System.out.println("[PASS] Đăng nhập thành công: " + result.get().getFullName());
    }

    @Test
    @Order(4)
    @DisplayName("2b. Đăng nhập sai password → thất bại")
    void testLoginWrongPassword() {
        fanController.logout();
        Optional<Fan> result = fanController.login(TEST_USERNAME, "wrongpassword");
        assertFalse(result.isPresent(), "Đăng nhập sai password phải thất bại");
        assertFalse(fanController.isLoggedIn());
        System.out.println("[PASS] Đăng nhập sai password bị từ chối");
    }

    @Test
    @Order(5)
    @DisplayName("2c. Đăng nhập username không tồn tại → thất bại")
    void testLoginNonExistentUser() {
        Optional<Fan> result = fanController.login("nonexistent_user_xyz", "any");
        assertFalse(result.isPresent(), "Đăng nhập user không tồn tại phải thất bại");
        System.out.println("[PASS] Đăng nhập user không tồn tại bị từ chối");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // TEST 3: XEM TRẬN ĐẤU + GHẾ
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @Order(6)
    @DisplayName("3. Xem danh sách trận đấu → có dữ liệu")
    void testViewMatches() {
        List<Match> matches = matchRepo.findAll();
        assertFalse(matches.isEmpty(), "Phải có ít nhất 1 trận đấu");

        // Tìm trận SCHEDULED đầu tiên để đặt vé
        Optional<Match> scheduledMatch = matches.stream()
                .filter(m -> m.getStatus() == MatchStatus.SCHEDULED)
                .findFirst();

        assertTrue(scheduledMatch.isPresent(), "Phải có ít nhất 1 trận SCHEDULED");
        testMatchId = scheduledMatch.get().getMatchId();

        System.out.println("[PASS] Có " + matches.size() + " trận đấu, chọn trận: " + testMatchId);
    }

    @Test
    @Order(7)
    @DisplayName("3b. Xem ghế trống theo trận → có ghế available")
    void testViewAvailableSeats() {
        assertNotNull(testMatchId, "testMatchId phải được set ở test trước");

        List<Seat> availableSeats = seatRepo.findAvailableByMatch(testMatchId);
        assertFalse(availableSeats.isEmpty(), "Trận " + testMatchId + " phải có ghế trống");

        // Lấy ghế đầu tiên để test đặt vé
        testSeatId = availableSeats.get(0).getSeatId();
        System.out.println("[PASS] Trận " + testMatchId + " có " + availableSeats.size()
                + " ghế trống, chọn ghế: " + testSeatId);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // TEST 4: ĐẶT VÉ END-TO-END
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @Order(8)
    @DisplayName("4. End-to-end: Login → Đặt vé → Kiểm tra vé")
    void testEndToEndBooking() {
        // Re-login
        fanController.login(TEST_USERNAME, TEST_PASSWORD);
        assertTrue(fanController.isLoggedIn(), "Phải đăng nhập trước khi đặt vé");

        String fanId = fanController.getCurrentFan().getFanId();

        // Đặt vé
        boolean result = bookingController.bookSeat(fanId, testMatchId, testSeatId, LockMechanism.NO_LOCK);
        assertTrue(result, "Đặt vé phải thành công cho ghế trống");

        // Kiểm tra ghế đã đổi trạng thái
        Optional<Seat> seatOpt = seatRepo.findById(testSeatId);
        assertTrue(seatOpt.isPresent());
        assertEquals(SeatStatus.BOOKED, seatOpt.get().getStatus(),
                "Ghế phải chuyển sang BOOKED sau khi đặt vé");

        // Kiểm tra ticket đã được tạo
        List<Ticket> myTickets = ticketRepo.findValidTickets(fanId);
        assertFalse(myTickets.isEmpty(), "Phải có ít nhất 1 vé sau khi đặt");

        // Tìm ticket vừa đặt
        Optional<Ticket> newTicket = myTickets.stream()
                .filter(t -> t.getSeatId().equals(testSeatId) && t.getMatchId().equals(testMatchId))
                .findFirst();
        assertTrue(newTicket.isPresent(), "Phải tìm thấy vé mới đặt");
        bookedTicketId = newTicket.get().getTicketId();

        // Kiểm tra transaction
        List<BookingTransaction> txns = txnRepo.findByFan(fanId);
        assertFalse(txns.isEmpty(), "Phải có giao dịch sau khi đặt vé");

        System.out.println("[PASS] End-to-end booking thành công! Ticket: " + bookedTicketId);
    }

    @Test
    @Order(9)
    @DisplayName("4b. Đặt vé trùng ghế → thất bại (chống Double Booking)")
    void testDoubleBookingFail() {
        String fanId = fanController.getCurrentFan().getFanId();

        // Cố đặt lại cùng ghế
        boolean result = bookingController.bookSeat(fanId, testMatchId, testSeatId, LockMechanism.NO_LOCK);
        assertFalse(result, "Đặt vé trùng ghế phải thất bại (Double Booking prevention)");
        System.out.println("[PASS] Double booking bị chặn thành công");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // TEST 5: XEM VÉ ĐÃ MUA
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @Order(10)
    @DisplayName("5. Xem vé đã mua → hiển thị đúng")
    void testViewMyTickets() {
        List<Ticket> myTickets = fanController.viewMyTickets();
        assertFalse(myTickets.isEmpty(), "Phải có vé sau khi đặt");

        boolean found = myTickets.stream()
                .anyMatch(t -> t.getTicketId().equals(bookedTicketId));
        assertTrue(found, "Phải tìm thấy vé vừa đặt trong danh sách");

        System.out.println("[PASS] viewMyTickets trả về " + myTickets.size() + " vé, bao gồm " + bookedTicketId);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // TEST 6: BÁO CÁO
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @Order(11)
    @DisplayName("6. Report: thống kê giao dịch → có dữ liệu")
    void testReportTransactions() {
        String fanId = fanController.getCurrentFan().getFanId();

        List<BookingTransaction> txns = reportController.getTransactionsByFan(fanId);
        assertFalse(txns.isEmpty(), "ReportController phải trả về giao dịch");

        long revenue = reportController.getTotalRevenue();
        assertTrue(revenue >= 0, "Doanh thu phải >= 0");

        int[] seatSummary = reportController.getMatchSeatSummary(testMatchId);
        assertTrue(seatSummary[0] > 0, "Tổng ghế phải > 0");
        assertTrue(seatSummary[1] > 0, "Ghế đã đặt phải > 0 (vừa đặt 1 ghế)");

        System.out.println("[PASS] Report: " + txns.size() + " giao dịch, doanh thu: " + revenue + " VND");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // TEST 7: HỦY VÉ + KIỂM TRA
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @Order(12)
    @DisplayName("7. Hủy vé → ghế trở lại AVAILABLE")
    void testCancelBooking() {
        assertNotNull(bookedTicketId, "bookedTicketId phải được set ở test trước");

        boolean result = bookingController.cancelBooking(bookedTicketId);
        assertTrue(result, "Hủy vé phải thành công");

        // Kiểm tra vé đã bị hủy
        Optional<Ticket> ticketOpt = ticketRepo.findById(bookedTicketId);
        assertTrue(ticketOpt.isPresent());
        assertEquals(TicketStatus.CANCELLED, ticketOpt.get().getStatus(),
                "Vé phải chuyển sang CANCELLED");

        // Kiểm tra ghế đã trở lại AVAILABLE
        Optional<Seat> seatOpt = seatRepo.findById(testSeatId);
        assertTrue(seatOpt.isPresent());
        assertEquals(SeatStatus.AVAILABLE, seatOpt.get().getStatus(),
                "Ghế phải trở lại AVAILABLE sau khi hủy vé");

        System.out.println("[PASS] Hủy vé thành công, ghế " + testSeatId + " trở lại AVAILABLE");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // TEST 8: ĐĂNG XUẤT
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @Order(13)
    @DisplayName("8. Đăng xuất → session cleared")
    void testLogout() {
        assertTrue(fanController.isLoggedIn());
        fanController.logout();
        assertFalse(fanController.isLoggedIn(), "Sau đăng xuất phải không còn đăng nhập");
        assertNull(fanController.getCurrentFan(), "currentFan phải null sau đăng xuất");
        System.out.println("[PASS] Đăng xuất thành công");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // TEST 9: HASH PASSWORD CONSISTENCY
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @Order(14)
    @DisplayName("9. Hash password → nhất quán")
    void testHashPasswordConsistency() {
        String hash1 = fanController.hashPassword("Test@1234");
        String hash2 = fanController.hashPassword("Test@1234");
        assertEquals(hash1, hash2, "Cùng password phải cho cùng hash");

        String hash3 = fanController.hashPassword("DifferentPass");
        assertNotEquals(hash1, hash3, "Khác password phải cho khác hash");

        // Hash phải là uppercase hex, 64 ký tự (SHA-256)
        assertNotNull(hash1);
        assertEquals(64, hash1.length(), "SHA-256 hash phải dài 64 ký tự hex");
        assertTrue(hash1.matches("[0-9A-F]+"), "Hash phải là uppercase hex");

        System.out.println("[PASS] Hash password nhất quán: " + hash1.substring(0, 16) + "...");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CLEANUP
    // ════════════════════════════════════════════════════════════════════════════

    @AfterAll
    static void cleanup() {
        // Xóa test user để không ảnh hưởng data thật
        if (registeredFanId != null) {
            fanRepo.deleteById(registeredFanId);
            System.out.println("[CLEANUP] Đã xóa test user: " + registeredFanId);
        }
    }
}
