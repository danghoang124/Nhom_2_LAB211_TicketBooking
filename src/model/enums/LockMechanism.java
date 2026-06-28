package model.enums;

/**
 * Cơ chế đồng bộ được dùng trong một giao dịch Simulator.
 *
 * <ul>
 *   <li>{@code NO_LOCK}      — không dùng khoá → có thể xảy ra Double Booking.</li>
 *   <li>{@code FILE_LOCK}    — dùng Java NIO {@code FileLock} → khoá ở tầng file OS.</li>
 *   <li>{@code SYNCHRONIZED} — dùng {@code synchronized} block trong Repository.</li>
 *   <li>{@code OPTIMISTIC}   — dùng trường {@code version} trong Seat (Optimistic Locking).</li>
 * </ul>
 */
public enum LockMechanism {
    NO_LOCK,
    FILE_LOCK,
    SYNCHRONIZED,
    OPTIMISTIC;

    /** Parse từ chuỗi (không phân biệt hoa/thường). */
    public static LockMechanism fromString(String value) {
        return valueOf(value.trim().toUpperCase());
    }
}
