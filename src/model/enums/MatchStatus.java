package model.enums;

/**
 * Trạng thái của một trận đấu bóng đá.
 *
 * <ul>
 *   <li>{@code SCHEDULED}  — trận chưa diễn ra, đang mở bán vé.</li>
 *   <li>{@code ONGOING}    — trận đang diễn ra, không cho đặt vé mới.</li>
 *   <li>{@code COMPLETED}  — trận đã kết thúc.</li>
 * </ul>
 */
public enum MatchStatus {
    SCHEDULED,
    ONGOING,
    COMPLETED;

    /** Parse từ chuỗi (không phân biệt hoa/thường). */
    public static MatchStatus fromString(String value) {
        return valueOf(value.trim().toUpperCase());
    }
}
