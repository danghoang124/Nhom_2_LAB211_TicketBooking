package model.enums;

/**
 * Trạng thái của một vé đã đặt.
 *
 * <ul>
 *   <li>{@code VALID}      — vé hợp lệ, fan có thể vào sân.</li>
 *   <li>{@code CANCELLED}  — vé đã bị huỷ (hoàn tiền hoặc giao dịch thất bại).</li>
 * </ul>
 */
public enum TicketStatus {
    VALID,
    CANCELLED;

    /** Parse từ chuỗi (không phân biệt hoa/thường). */
    public static TicketStatus fromString(String value) {
        return valueOf(value.trim().toUpperCase());
    }
}
