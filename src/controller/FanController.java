package controller;

import exception.InvalidCredentialsException;
import exception.UserAlreadyExistsException;
import model.entity.Fan;
import model.entity.Ticket;
import model.enums.Role;
import repository.FanRepository;
import repository.TicketRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Controller xử lý logic nghiệp vụ cho Fan:
 * đăng ký, đăng nhập, đăng xuất và xem vé của mình.
 *
 * <p><b>Nguyên tắc MVC:</b>
 * <ul>
 *   <li>View gọi method của Controller với dữ liệu người dùng nhập.</li>
 *   <li>Controller xử lý logic, gọi Repository, trả kết quả về View.</li>
 *   <li>Controller KHÔNG in ra màn hình, KHÔNG đọc ghi file trực tiếp.</li>
 * </ul>
 *
 * <p><b>Session Management (đơn giản):</b>
 * Controller giữ biến {@code currentFan} để biết ai đang đăng nhập.
 * Không dùng session phức tạp vì đây là ứng dụng CLI đơn luồng.
 */
public class FanController {

    // ── Dependencies (tiêm qua constructor) ───────────────────────────────────
    private final FanRepository    fanRepository;
    private final TicketRepository ticketRepository;

    // ── Session state ─────────────────────────────────────────────────────────
    /** Fan đang đăng nhập. null nếu chưa đăng nhập. */
    private Fan currentFan = null;

    // ── Hằng số ───────────────────────────────────────────────────────────────
    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Khởi tạo FanController với 2 repository cần thiết.
     *
     * @param fanRepository    Repository để thao tác với fans.csv
     * @param ticketRepository Repository để thao tác với tickets.csv
     */
    public FanController(FanRepository fanRepository, TicketRepository ticketRepository) {
        this.fanRepository    = fanRepository;
        this.ticketRepository = ticketRepository;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 1. ĐĂNG KÝ (register)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Xử lý đăng ký tài khoản mới cho Fan.
     *
     * <p><b>Luồng xử lý:</b>
     * <ol>
     *   <li>Validate input không được rỗng.</li>
     *   <li>Kiểm tra username đã tồn tại chưa (UNIQUE).</li>
     *   <li>Kiểm tra email đã tồn tại chưa (UNIQUE).</li>
     *   <li>Validate format email và phone.</li>
     *   <li>Hash password bằng SHA-256.</li>
     *   <li>Sinh fanId mới (tự động tăng dần).</li>
     *   <li>Tạo đối tượng Fan và lưu vào repository.</li>
     * </ol>
     *
     * @param username Tên đăng nhập (phải unique).
     * @param password Mật khẩu gốc (sẽ được hash trước khi lưu).
     * @param fullName Họ và tên đầy đủ.
     * @param email    Địa chỉ email (phải unique).
     * @param phone    Số điện thoại (10 chữ số, bắt đầu bằng 0).
     * @return {@link RegisterResult} chứa trạng thái thành công/thất bại và thông báo.
     */
    public RegisterResult register(String username, String password,
                                   String fullName, String email, String phone) {

        // ── Bước 1: Validate input không được rỗng ─────────────────────────
        if (isBlank(username)) return RegisterResult.fail("Tên đăng nhập không được để trống.");
        if (isBlank(password)) return RegisterResult.fail("Mật khẩu không được để trống.");
        if (isBlank(fullName)) return RegisterResult.fail("Họ tên không được để trống.");
        if (isBlank(email))    return RegisterResult.fail("Email không được để trống.");
        if (isBlank(phone))    return RegisterResult.fail("Số điện thoại không được để trống.");

        // ── Bước 2: Validate độ dài và ký tự hợp lệ ───────────────────────
        if (username.trim().length() < 3) {
            return RegisterResult.fail("Tên đăng nhập phải có ít nhất 3 ký tự.");
        }
        if (password.length() < 6) {
            return RegisterResult.fail("Mật khẩu phải có ít nhất 6 ký tự.");
        }
        if (!isValidEmail(email.trim())) {
            return RegisterResult.fail("Email không đúng định dạng (ví dụ: abc@gmail.com).");
        }
        if (!isValidPhone(phone.trim())) {
            return RegisterResult.fail("Số điện thoại phải có 10 chữ số và bắt đầu bằng 0.");
        }

        // ── Bước 3: Kiểm tra username và email đã tồn tại chưa ────────────
        // Ném UserAlreadyExistsException (thay vì return fail) để caller
        // biết đây là lỗi nghiệp vụ rõ ràng — không phải lỗi validation thông thường.
        if (fanRepository.isUsernameTaken(username.trim())) {
            throw new UserAlreadyExistsException(
                "Tên đăng nhập '" + username.trim() + "' đã được sử dụng.");
        }
        if (fanRepository.isEmailTaken(email.trim())) {
            throw new UserAlreadyExistsException(
                "Email '" + email.trim() + "' đã được đăng ký bởi tài khoản khác.");
        }

        // ── Bước 4: Hash mật khẩu SHA-256 ─────────────────────────────────
        String passwordHash = sha256(password);
        if (passwordHash == null) {
            return RegisterResult.fail("Lỗi hệ thống: Không thể mã hóa mật khẩu.");
        }

        // ── Bước 5: Sinh fanId mới ─────────────────────────────────────────
        // Đọc tất cả fan, lấy ID lớn nhất + 1
        String newFanId = generateNextFanId();

        // ── Bước 6: Tạo Fan và lưu vào CSV ────────────────────────────────
        String createdAt = LocalDateTime.now().format(DATETIME_FMT);
        Fan newFan = new Fan(
                newFanId,
                username.trim(),
                passwordHash,
                fullName.trim(),
                email.trim(),
                phone.trim(),
                createdAt,
                true,  // isActive = true ngay khi tạo
                Role.FAN
        );

        fanRepository.save(newFan);

        return RegisterResult.success(
                "Đăng ký thành công! Chào mừng " + fullName.trim() + ".",
                newFan
        );
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 2. ĐĂNG NHẬP (login)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Xử lý đăng nhập cho Fan.
     *
     * <p><b>Luồng xử lý:</b>
     * <ol>
     *   <li>Validate input không được rỗng.</li>
     *   <li>Hash password nhập vào bằng SHA-256.</li>
     *   <li>Gọi {@code fanRepository.authenticate()} để xác thực.</li>
     *   <li>Nếu thành công, lưu Fan vào {@code currentFan} (session).</li>
     * </ol>
     *
     * <p><b>Bảo mật:</b> Không bao giờ so sánh plaintext password —
     * luôn hash trước khi so sánh.
     *
     * @param username Tên đăng nhập.
     * @param password Mật khẩu gốc người dùng nhập vào.
     * @return {@code true} nếu đăng nhập thành công.
     */
    public boolean login(String username, String password) {
        // Validate input cơ bản — không throw exception cho trường hợp rỗng
        if (isBlank(username) || isBlank(password)) return false;

        // Hash mật khẩu trước khi gửi xuống repository
        String passwordHash = sha256(password);
        if (passwordHash == null) return false;

        // Xác thực qua repository
        Optional<Fan> result = fanRepository.authenticate(username.trim(), passwordHash);

        if (result.isPresent()) {
            currentFan = result.get();  // Lưu session
            return true;
        }

        // Ném InvalidCredentialsException thay vì return false.
        // Giúp View phân biệt "thông tin sai" với "lỗi hệ thống".
        throw new InvalidCredentialsException(
            "Tên đăng nhập hoặc mật khẩu không đúng.");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 3. ĐĂNG XUẤT (logout)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Đăng xuất Fan hiện tại — xóa session.
     *
     * <p>Sau khi gọi method này, {@code currentFan} sẽ là null
     * và {@code isLoggedIn()} sẽ trả về false.
     */
    public void logout() {
        currentFan = null;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 4. XEM VÉ CỦA TÔI (getMyTickets)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Lấy danh sách tất cả vé của Fan đang đăng nhập.
     *
     * <p>Chỉ hoạt động khi Fan đã đăng nhập ({@code currentFan != null}).
     *
     * @return List các Ticket của Fan hiện tại.
     *         Trả về list rỗng nếu chưa đăng nhập hoặc chưa có vé.
     */
    public List<Ticket> getMyTickets() {
        if (currentFan == null) return List.of();
        return ticketRepository.findByFan(currentFan.getFanId());
    }

    /**
     * Lấy danh sách vé VALID (chưa hủy) của Fan đang đăng nhập.
     *
     * @return List Ticket còn hiệu lực.
     */
    public List<Ticket> getMyValidTickets() {
        if (currentFan == null) return List.of();
        return ticketRepository.findValidTickets(currentFan.getFanId());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 5. SESSION HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Kiểm tra có Fan nào đang đăng nhập không.
     *
     * @return {@code true} nếu đang có session đăng nhập.
     */
    public boolean isLoggedIn() {
        return currentFan != null;
    }

    /**
     * Đặt Fan hiện tại trực tiếp vào session mà không cần hash lại password.
     *
     * <p>Dùng ngay sau khi đăng ký thành công để tránh double-hash bug:
     * nếu gọi {@code login(username, password)} sau {@code register()},
     * password sẽ bị hash lần 2 ({@code sha256(sha256(password))}) và
     * không khớp với hash đã lưu trong CSV.
     *
     * @param fan Fan vừa được tạo (lấy từ {@link RegisterResult#getFan()}).
     */
    public void setCurrentFan(Fan fan) {
        this.currentFan = fan;
    }

    /**
     * Lấy thông tin Fan đang đăng nhập.
     *
     * @return Fan hiện tại, hoặc null nếu chưa đăng nhập.
     */
    public Fan getCurrentFan() {
        return currentFan;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Hash một chuỗi bằng SHA-256, trả về hex string in HOA.
     * Đây là thuật toán mã hóa một chiều — không thể giải mã ngược.
     *
     * <p>Ví dụ: sha256("abc123") → "6CA13D52CA70C883E0F0BB101E425A89E8624DE51DB2D2392593AF6A84118090"
     *
     * @param input Chuỗi cần hash (thường là password).
     * @return Chuỗi hex 64 ký tự (SHA-256 = 256 bit = 32 byte = 64 hex chars).
     *         Trả về null nếu hệ thống không hỗ trợ SHA-256 (rất hiếm).
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Chuyển mảng byte sang chuỗi hex
            StringBuilder hexBuilder = new StringBuilder();
            for (byte b : hashBytes) {
                hexBuilder.append(String.format("%02X", b)); // %02X = 2 ký tự hex in hoa
            }
            return hexBuilder.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 luôn có sẵn trong Java — đây chỉ là safety net
            return null;
        }
    }

    /**
     * Sinh FanID mới theo format FAN0001, FAN0002, ...
     * Đọc tất cả fan hiện tại, lấy số lớn nhất rồi +1.
     *
     * <p>Ví dụ: đang có FAN0001 đến FAN0050 → sinh ra FAN0051.
     *
     * <p><b>Thread-safety:</b> Được đánh dấu synchronized để tránh race condition
     * khi Simulator chạy nhiều threads đồng thời — nếu 2 thread cùng vào đây
     * cùng lúc, có thể sinh ra 2 FanID trùng nhau.
     *
     * @return FanID mới dạng "FANxxxx" (4 chữ số, có padding zero).
     */
    private synchronized String generateNextFanId() {
        List<Fan> allFans = fanRepository.findAll();

        int maxNum = 0;
        for (Fan fan : allFans) {
            String id = fan.getFanId(); // "FAN0001"
            if (id != null && id.startsWith("FAN")) {
                try {
                    int num = Integer.parseInt(id.substring(3)); // lấy "0001" → 1
                    if (num > maxNum) maxNum = num;
                } catch (NumberFormatException ignored) {
                    // Bỏ qua ID không đúng format
                }
            }
        }
        // Format: FAN + 4 chữ số có padding zero
        return String.format("FAN%04d", maxNum + 1);
    }

    /**
     * Kiểm tra chuỗi có rỗng/null không.
     */
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * Validate định dạng email đơn giản.
     * Kiểm tra có ký tự '@' và dấu chấm sau '@'.
     *
     * @param email Email cần kiểm tra.
     * @return true nếu email hợp lệ.
     */
    private boolean isValidEmail(String email) {
        if (email == null) return false;
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) return false;                         // không có '@' hoặc '@' ở đầu
        String domain = email.substring(atIndex + 1);
        return domain.contains(".") && domain.length() >= 3;   // phần sau '@' phải có dấu '.'
    }

    /**
     * Validate số điện thoại Việt Nam đơn giản.
     * Phải có đúng 10 chữ số và bắt đầu bằng '0'.
     *
     * @param phone Số điện thoại cần kiểm tra.
     * @return true nếu hợp lệ.
     */
    private boolean isValidPhone(String phone) {
        if (phone == null) return false;
        return phone.matches("^0\\d{9}$"); // bắt đầu 0, tiếp theo 9 chữ số
    }

    // ═════════════════════════════════════════════════════════════════════════
    // INNER CLASSES — Kết quả trả về
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Kết quả của thao tác đăng ký.
     * Dùng pattern Result Object để tránh throw Exception cho business logic.
     *
     * <p>Thay vì throw Exception, ta trả về object này.
     * View sẽ kiểm tra {@code isSuccess()} để biết kết quả.
     */
    public static class RegisterResult {
        private final boolean success;
        private final String  message;
        private final Fan     fan;      // null nếu thất bại

        private RegisterResult(boolean success, String message, Fan fan) {
            this.success = success;
            this.message = message;
            this.fan     = fan;
        }

        /** Tạo kết quả thành công. */
        public static RegisterResult success(String message, Fan fan) {
            return new RegisterResult(true, message, fan);
        }

        /** Tạo kết quả thất bại với thông báo lỗi. */
        public static RegisterResult fail(String message) {
            return new RegisterResult(false, message, null);
        }

        public boolean isSuccess() { return success; }
        public String  getMessage() { return message; }
        public Fan     getFan()     { return fan; }
    }
}
