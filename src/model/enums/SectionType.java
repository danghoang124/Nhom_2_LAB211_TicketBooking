package model.enums;

/**
 * Loại khu vực ghế ngồi trong sân vận động.
 *
 * <ul>
 *   <li>{@code VIP}           — khu VIP, giá cao nhất (500,000 VND).</li>
 *   <li>{@code STANDARD}      — khu thường (200,000 VND).</li>
 *   <li>{@code STANDING}      — khu đứng (100,000 VND).</li>
 *   <li>{@code ECONOMY_LOWER} — khu phổ thông thấp (80,000 VND).</li>
 * </ul>
 */
public enum SectionType {
    VIP,
    STANDARD,
    STANDING,
    ECONOMY_LOWER;

    /** Parse từ chuỗi (không phân biệt hoa/thường). */
    public static SectionType fromString(String value) {
        return valueOf(value.trim().toUpperCase());
    }
}
