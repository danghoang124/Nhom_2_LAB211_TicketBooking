package model.entity;

import model.enums.SectionType;

/**
 * Entity đại diện cho một Khu vực ghế ngồi trong sân vận động.
 *
 * <p>Tương ứng với file: {@code data/sections.csv}
 * <pre>
 *   Header: sectionId,sectionType,totalRows,seatsPerRow,basePrice
 *   Ví dụ:  SEC001,VIP,10,20,500000
 * </pre>
 *
 * <p><b>Thiết kế:</b> 4 Section dùng chung cho tất cả sân — ghế được sinh
 * theo tổ hợp {@code (sectionId × matchId)}.
 */
public class Section extends BaseEntity {

    private String      sectionId;    // PK — dạng SEC001
    private SectionType sectionType;  // VIP | STANDARD | STANDING | ECONOMY_LOWER
    private int         totalRows;
    private int         seatsPerRow;
    private long        basePrice;    // đơn vị VND

    // ── Constructor ────────────────────────────────────────────────────────────

    public Section(String sectionId, SectionType sectionType,
                   int totalRows, int seatsPerRow, long basePrice) {
        this.sectionId   = sectionId;
        this.sectionType = sectionType;
        this.totalRows   = totalRows;
        this.seatsPerRow = seatsPerRow;
        this.basePrice   = basePrice;
    }

    // ── BaseEntity ─────────────────────────────────────────────────────────────

    @Override
    public String getId() { return sectionId; }

    /** Tuần tự hoá: sectionId, sectionType, totalRows, seatsPerRow, basePrice */
    @Override
    public String toCsvLine() {
        return joinCsv(sectionId, sectionType.name(), totalRows, seatsPerRow, basePrice);
    }

    /**
     * Tạo Section từ một dòng CSV.
     *
     * @throws IllegalArgumentException nếu dữ liệu không hợp lệ.
     */
    public static Section fromCsvLine(String line) {
        String[] f = splitCsvLine(line);
        if (f.length < 5) {
            throw new IllegalArgumentException(
                "Section CSV cần ít nhất 5 cột, nhưng chỉ có " + f.length + ": " + line);
        }
        return new Section(
            f[0].trim(),
            SectionType.fromString(f[1].trim()),
            Integer.parseInt(f[2].trim()),
            Integer.parseInt(f[3].trim()),
            Long.parseLong(f[4].trim())
        );
    }

    // ── Getters ────────────────────────────────────────────────────────────────

    public String getSectionId() {
        return sectionId;
    }

    public SectionType getSectionType() {
        return sectionType;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public int getSeatsPerRow() {
        return seatsPerRow;
    }

    public long getBasePrice() {
        return basePrice;
    }

    /** Tổng số ghế trong section này (totalRows × seatsPerRow). */
    public int getTotalCapacity() {
        return totalRows * seatsPerRow;
    }
}
