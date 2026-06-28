package model.enums;

/**
 * Kết quả của một giao dịch đặt vé.
 *
 * <ul>
 *   <li>{@code SUCCESS}  — tất cả vé trong giao dịch được đặt thành công.</li>
 *   <li>{@code FAILED}   — giao dịch thất bại hoàn toàn (0 vé được đặt).</li>
 *   <li>{@code PARTIAL}  — một phần vé được đặt (ví dụ: xin 4 ghế nhưng chỉ còn 2).</li>
 * </ul>
 */
public enum TransactionStatus {
    SUCCESS,
    FAILED,
    PARTIAL;

    /** Parse từ chuỗi (không phân biệt hoa/thường). */
    public static TransactionStatus fromString(String value) {
        return valueOf(value.trim().toUpperCase());
    }
}
