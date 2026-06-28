package model.entity;

/**
 * Entity đại diện cho một Fan (người dùng) trong hệ thống.
 *
 * <p>Tương ứng với file: {@code data/fans.csv}
 * <pre>
 *   Header: fanId,username,passwordHash,fullName,email,phone,createdAt,isActive
 *   Ví dụ:  FAN0001,anv,3A2F1C...,Nguyễn Văn An,anv@gmail.com,0912345678,2024-03-15 09:22:00,true
 * </pre>
 *
 * <p><b>Bảo mật:</b> {@code passwordHash} là SHA-256 hex của mật khẩu gốc.
 * Không bao giờ lưu mật khẩu plaintext.
 */
public class Fan extends BaseEntity {

    private final String  fanId;         // PK — dạng FAN0001
    private final String  username;      // UNIQUE
    private final String  passwordHash;  // SHA-256 hex, upper-case
    private final String  fullName;
    private final String  email;         // UNIQUE
    private final String  phone;         // 0xxxxxxxxx
    private final String  createdAt;     // yyyy-MM-dd HH:mm:ss
    private       boolean isActive;      // mutable — có thể bị deactivate

    // ── Constructor ────────────────────────────────────────────────────────────

    public Fan(String fanId, String username, String passwordHash,
               String fullName, String email, String phone,
               String createdAt, boolean isActive) {
        this.fanId        = fanId;
        this.username     = username;
        this.passwordHash = passwordHash;
        this.fullName     = fullName;
        this.email        = email;
        this.phone        = phone;
        this.createdAt    = createdAt;
        this.isActive     = isActive;
    }

    // ── BaseEntity ─────────────────────────────────────────────────────────────

    @Override
    public String getId() { return fanId; }

    /** Tuần tự hoá: fanId, username, passwordHash, fullName, email, phone, createdAt, isActive */
    @Override
    public String toCsvLine() {
        return joinCsv(fanId, username, passwordHash, fullName, email, phone, createdAt, isActive);
    }

    /** Tạo Fan từ một dòng CSV. */
    public static Fan fromCsvLine(String line) {
        String[] f = splitCsvLine(line);
        if (f.length < 8) {
            throw new IllegalArgumentException(
                "Fan CSV cần ít nhất 8 cột, nhưng chỉ có " + f.length + ": " + line);
        }
        return new Fan(
            f[0].trim(),
            f[1].trim(),
            f[2].trim(),
            f[3].trim(),
            f[4].trim(),
            f[5].trim(),
            f[6].trim(),
            Boolean.parseBoolean(f[7].trim())
        );
    }

    // ── Business logic ─────────────────────────────────────────────────────────

    /**
     * Kiểm tra mật khẩu đã hash có khớp không.
     * @param hashToCheck SHA-256 hex của mật khẩu người dùng nhập vào.
     */
    public boolean checkPassword(String hashToCheck) {
        return passwordHash != null && passwordHash.equalsIgnoreCase(hashToCheck);
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public String getFanId() {
        return fanId;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }
}
