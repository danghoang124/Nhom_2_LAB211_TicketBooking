package repository;

import model.entity.Fan;

import java.util.List;
import java.util.Optional;

/**
 * Repository cụ thể cho {@link Fan} — đọc/ghi {@code data/fans.csv}.
 *
 * <p>Fan là người dùng đặt vé. Repository này cung cấp các phương thức
 * tìm kiếm theo username, email và xác thực mật khẩu.
 *
 * <p><b>Bảo mật:</b> Phương thức {@link #authenticate} nhận vào hash SHA-256
 * của mật khẩu (KHÔNG nhận plaintext). Việc hash mật khẩu phải được thực hiện
 * ở tầng Service trước khi gọi authenticate.
 */
public class FanRepository extends CsvRepository<Fan> {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static final FanRepository INSTANCE = new FanRepository();
    public static FanRepository getInstance() { return INSTANCE; }

    private static final String FILE_PATH = "data/fans.csv";
    private static final String HEADER    =
        "fanId,username,passwordHash,fullName,email,phone,createdAt,isActive,role";

    // ── Abstract method implementations ───────────────────────────────────────

    @Override
    public String getFilePath() { return FILE_PATH; }

    @Override
    public String getCsvHeader() { return HEADER; }

    @Override
    protected Fan parseFromCsvLine(String line) {
        return Fan.fromCsvLine(line);
    }

    // ── Domain-specific query methods ─────────────────────────────────────────

    /**
     * Tìm Fan theo username (UNIQUE).
     *
     * <p>Username là định danh đăng nhập duy nhất của mỗi fan.
     *
     * @param username Tên đăng nhập cần tìm (case-sensitive).
     * @return Optional chứa Fan nếu tìm thấy.
     */
    public Optional<Fan> findByUsername(String username) {
        return findByCondition(f -> username != null && username.equals(f.getUsername()))
               .stream().findFirst();
    }

    /**
     * Tìm Fan theo email (UNIQUE).
     *
     * @param email Địa chỉ email cần tìm.
     * @return Optional chứa Fan nếu tìm thấy.
     */
    public Optional<Fan> findByEmail(String email) {
        return findByCondition(f -> email != null && email.equalsIgnoreCase(f.getEmail()))
               .stream().findFirst();
    }

    /**
     * Lấy tất cả Fan đang hoạt động (isActive = true).
     *
     * @return List các Fan active.
     */
    public List<Fan> findActiveFans() {
        return findByCondition(Fan::isActive);
    }

    /**
     * Lấy tất cả Fan bị vô hiệu hóa (isActive = false).
     *
     * @return List các Fan inactive.
     */
    public List<Fan> findInactiveFans() {
        return findByCondition(f -> !f.isActive());
    }

    /**
     * Xác thực thông tin đăng nhập.
     *
     * <p><b>Quan trọng:</b> {@code passwordHash} phải là SHA-256 hex (upper-case)
     * của mật khẩu người dùng nhập. Việc hash mật khẩu PHẢI được thực hiện
     * ở tầng Service trước khi gọi method này.
     *
     * <p>Ví dụ:
     * <pre>
     *   String hash = sha256(userInputPassword);
     *   Optional&lt;Fan&gt; fan = fanRepo.authenticate("anv", hash);
     * </pre>
     *
     * @param username     Tên đăng nhập.
     * @param passwordHash SHA-256 hex của mật khẩu.
     * @return Optional chứa Fan nếu thông tin đúng và tài khoản active,
     *         Optional.empty() nếu sai hoặc tài khoản bị khóa.
     */
    public Optional<Fan> authenticate(String username, String passwordHash) {
        return findByUsername(username)
               .filter(f -> f.isActive() && f.checkPassword(passwordHash));
    }

    /**
     * Kiểm tra username đã được sử dụng chưa.
     *
     * @param username Tên đăng nhập cần kiểm tra.
     * @return {@code true} nếu username đã tồn tại.
     */
    public boolean isUsernameTaken(String username) {
        return findByUsername(username).isPresent();
    }

    /**
     * Kiểm tra email đã được đăng ký chưa.
     *
     * @param email Email cần kiểm tra.
     * @return {@code true} nếu email đã tồn tại.
     */
    public boolean isEmailTaken(String email) {
        return findByEmail(email).isPresent();
    }
}
