package repository;

import model.entity.Ticket;
import model.enums.TicketStatus;

import java.util.List;

/**
 * Repository cụ thể cho {@link Ticket} — đọc/ghi {@code data/tickets.csv}.
 *
 * <p>File này ban đầu chỉ chứa header (rỗng) và được thêm dữ liệu tại runtime
 * khi fan đặt vé thành công. Luôn dùng {@link #append(Ticket)} để tạo vé mới
 * (không bao giờ dùng {@code save()} cho vé mới — chậm không cần thiết).
 *
 * <p><b>Ràng buộc toàn vẹn quan trọng:</b> Cặp {@code (seatId, matchId)} phải UNIQUE
 * trong file — đây là cơ chế chính ngăn chặn Double Booking.
 * Luôn kiểm tra {@link #existsBySeatAndMatch} trước khi tạo vé mới.
 */
public class TicketRepository extends CsvRepository<Ticket> {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static final TicketRepository INSTANCE = new TicketRepository();
    public static TicketRepository getInstance() { return INSTANCE; }

    private static final String FILE_PATH = "data/tickets.csv";
    private static final String HEADER    =
        "ticketId,fanId,seatId,matchId,transactionId,price,bookedAt,status";

    // ── Abstract method implementations ───────────────────────────────────────

    @Override
    public String getFilePath() { return FILE_PATH; }

    @Override
    public String getCsvHeader() { return HEADER; }

    @Override
    protected Ticket parseFromCsvLine(String line) {
        return Ticket.fromCsvLine(line);
    }

    // ── Domain-specific query methods ─────────────────────────────────────────

    /**
     * Lấy tất cả vé của một fan.
     *
     * @param fanId ID fan (ví dụ: "FAN0001").
     * @return List các Ticket của fan đó.
     */
    public List<Ticket> findByFan(String fanId) {
        return findByCondition(t -> fanId != null && fanId.equals(t.getFanId()));
    }

    /**
     * Lấy tất cả vé của một trận đấu.
     *
     * @param matchId ID trận đấu (ví dụ: "MATCH001").
     * @return List các Ticket của trận đó.
     */
    public List<Ticket> findByMatch(String matchId) {
        return findByCondition(t -> matchId != null && matchId.equals(t.getMatchId()));
    }

    /**
     * Lấy tất cả vé của một fan trong một trận đấu cụ thể.
     *
     * @param fanId   ID fan.
     * @param matchId ID trận đấu.
     * @return List các Ticket thỏa cả hai điều kiện.
     */
    public List<Ticket> findByFanAndMatch(String fanId, String matchId) {
        return findByCondition(t ->
            fanId   != null && fanId.equals(t.getFanId()) &&
            matchId != null && matchId.equals(t.getMatchId())
        );
    }

    /**
     * Lấy tất cả vé VALID (chưa hủy) của một fan.
     *
     * @param fanId ID fan.
     * @return List các Ticket có status = VALID.
     */
    public List<Ticket> findValidTickets(String fanId) {
        return findByCondition(t ->
            fanId != null && fanId.equals(t.getFanId()) && t.isValid()
        );
    }

    /**
     * Lấy tất cả vé thuộc một giao dịch.
     *
     * @param transactionId ID giao dịch.
     * @return List các Ticket trong giao dịch đó.
     */
    public List<Ticket> findByTransaction(String transactionId) {
        return findByCondition(t ->
            transactionId != null && transactionId.equals(t.getTransactionId())
        );
    }

    // ── Integrity checks ──────────────────────────────────────────────────────

    /**
     * Kiểm tra một ghế trong một trận đã được đặt chưa (chống Double Booking).
     *
     * <p>Ràng buộc: cặp {@code (seatId, matchId)} phải UNIQUE trong file tickets.csv.
     * Chỉ tính vé VALID — vé CANCELLED không cản trở booking lại.
     *
     * @param seatId  ID ghế.
     * @param matchId ID trận đấu.
     * @return {@code true} nếu ghế đó đã có vé VALID trong trận này (đã được đặt).
     */
    public boolean existsBySeatAndMatch(String seatId, String matchId) {
        return findByCondition(t ->
            seatId  != null && seatId.equals(t.getSeatId()) &&
            matchId != null && matchId.equals(t.getMatchId()) &&
            t.isValid()  // chỉ tính vé còn hiệu lực
        ).size() > 0;
    }

    /**
     * Lấy vé VALID của một ghế trong một trận.
     *
     * @param seatId  ID ghế.
     * @param matchId ID trận đấu.
     * @return List Ticket (thường rỗng hoặc 1 phần tử).
     */
    public List<Ticket> findBySeatAndMatch(String seatId, String matchId) {
        return findByCondition(t ->
            seatId  != null && seatId.equals(t.getSeatId()) &&
            matchId != null && matchId.equals(t.getMatchId())
        );
    }

    /**
     * Đếm tổng số vé VALID của một trận đấu.
     *
     * @param matchId ID trận đấu.
     * @return Số vé đã bán.
     */
    public int countSoldTickets(String matchId) {
        return (int) findByMatch(matchId).stream()
                     .filter(Ticket::isValid)
                     .count();
    }

    /**
     * Lấy tất cả vé theo trạng thái.
     *
     * @param status Trạng thái vé (VALID / CANCELLED).
     * @return List Ticket.
     */
    public List<Ticket> findByStatus(TicketStatus status) {
        return findByCondition(t -> status != null && status == t.getStatus());
    }
}
