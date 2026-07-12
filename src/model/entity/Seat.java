package model.entity;

import model.enums.SeatStatus;

/**
 * Entity đại diện cho một Ghế ngồi trong một trận đấu cụ thể.
 *
 * <p>Tương ứng với file: {@code data/seats.csv} (~30,000 dòng)
 * <pre>
 *   Header: seatId,sectionId,matchId,rowLabel,seatNumber,status,version
 *   Ví dụ:  SEAT000001,SEC001,MATCH001,A,1,AVAILABLE,0
 * </pre>
 *
 * <p><b>Optimistic Locking:</b> Trường {@code version} tăng mỗi khi ghế đổi trạng thái.
 * Khi một thread muốn cập nhật, nó đọc version hiện tại, xử lý, rồi chỉ ghi lại
 * nếu version trong file vẫn bằng version đã đọc — nếu không (thread khác đã ghi trước)
 * thì thử lại (retry).
 *
 * <p><b>Ràng buộc toàn vẹn:</b> {@code (sectionId, matchId, rowLabel, seatNumber)} là UNIQUE.
 */
public class Seat extends BaseEntity {

    private String     seatId;     // PK — dạng SEAT000001
    private String     sectionId;  // FK → sections
    private String     matchId;    // FK → matches
    private String     rowLabel;   // A, B, …, Z, AA, AB, …
    private int        seatNumber; // 1, 2, 3, …
    private       SeatStatus status;     // mutable — thay đổi theo quá trình booking
    private       int        version;    // mutable — tăng mỗi lần update (Optimistic Locking)

    // ── Constructor ────────────────────────────────────────────────────────────

    public Seat(String seatId, String sectionId, String matchId,
                String rowLabel, int seatNumber, SeatStatus status, int version) {
        this.seatId     = seatId;
        this.sectionId  = sectionId;
        this.matchId    = matchId;
        this.rowLabel   = rowLabel;
        this.seatNumber = seatNumber;
        this.status     = status;
        this.version    = version;
    }

    // ── BaseEntity ─────────────────────────────────────────────────────────────

    @Override
    public String getId() { return seatId; }

    /** Tuần tự hoá: seatId, sectionId, matchId, rowLabel, seatNumber, status, version */
    @Override
    public String toCsvLine() {
        return joinCsv(seatId, sectionId, matchId, rowLabel, seatNumber, status.name(), version);
    }

    /** Tạo Seat từ một dòng CSV. */
    public static Seat fromCsvLine(String line) {
        String[] f = splitCsvLine(line);
        if (f.length < 7) {
            throw new IllegalArgumentException(
                "Seat CSV requires at least 7 columns, but only has " + f.length + ": " + line);
        }
        return new Seat(
            f[0].trim(),
            f[1].trim(),
            f[2].trim(),
            f[3].trim(),
            Integer.parseInt(f[4].trim()),
            SeatStatus.fromString(f[5].trim()),
            Integer.parseInt(f[6].trim())
        );
    }

    // ── Business logic ─────────────────────────────────────────────────────────

    /** Kiểm tra ghế có thể đặt không. */
    public boolean isAvailable() {
        return status == SeatStatus.AVAILABLE;
    }

    /**
     * Đổi trạng thái và tăng version.
     * Phải gọi method này thay vì setStatus() trực tiếp để đảm bảo version luôn đồng bộ.
     */
    public void updateStatus(SeatStatus newStatus) {
        this.status = newStatus;
        this.version++;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public String getSeatId() {
        return seatId;
    }

    public String getSectionId() {
        return sectionId;
    }

    public String getMatchId() {
        return matchId;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public int getVersion() {
        return version;
    }

    private void setStatus(SeatStatus status) {
        this.status = status;
    }

    private void setVersion(int version) {
        this.version = version;
    }

    /**
     * Reset ghế về trạng thái ban đầu — dùng cho Simulator/benchmark.
     * Encapsulate setStatus() + setVersion() để bên ngoài không thể set version tùy ý,
     * giữ nguyên bất biến của Optimistic Locking.
     *
     * @param newStatus Trạng thái cần reset về (thường là AVAILABLE).
     * @param version   Version khởi đầu (thường là 0).
     */
    public void reset(SeatStatus newStatus, int version) {
        this.status = newStatus;
        this.version = version;
    }

    /** Chuỗi hiển thị ngắn cho UI (ví dụ: "A-1"). */
    public String getLabel() {
        return rowLabel + "-" + seatNumber;
    }
}
