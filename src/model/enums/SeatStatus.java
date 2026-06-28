package model.enums;

/**
 * Trạng thái của một ghế ngồi trong một trận đấu.
 *
 * <p>Vòng đời hợp lệ:
 * <pre>
 *   AVAILABLE ──→ LOCKED ──→ BOOKED
 *                    └──→ AVAILABLE  (nếu timeout hoặc huỷ)
 * </pre>
 *
 * <ul>
 *   <li>{@code AVAILABLE}  — ghế còn trống, có thể đặt.</li>
 *   <li>{@code LOCKED}     — đang được một fan giữ chỗ (chờ xác nhận thanh toán).</li>
 *   <li>{@code BOOKED}     — đã được đặt thành công, không thể bán lại.</li>
 * </ul>
 */
public enum SeatStatus {
    AVAILABLE,
    LOCKED,
    BOOKED;

    /**
     * Parse từ chuỗi (không phân biệt hoa/thường).
     *
     * @throws IllegalArgumentException nếu chuỗi không hợp lệ
     */
    public static SeatStatus fromString(String value) {
        return valueOf(value.trim().toUpperCase());
    }
}
