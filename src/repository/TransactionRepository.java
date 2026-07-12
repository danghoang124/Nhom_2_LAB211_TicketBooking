package repository;

import model.entity.BookingTransaction;
import model.enums.LockMechanism;
import model.enums.TransactionStatus;

import java.util.List;

/**
 * Repository cụ thể cho {@link BookingTransaction} — đọc/ghi {@code data/transactions.csv}.
 *
 * <p>File này ban đầu rỗng và được tạo ra bởi Simulator tại runtime.
 * Mỗi giao dịch ghi lại: fan nào, trận nào, số vé, tổng tiền, kết quả,
 * cơ chế đồng bộ đã dùng và thời gian xử lý — dùng để phân tích hiệu năng T7.
 *
 * <p>Luôn dùng {@link #append(BookingTransaction)} để ghi giao dịch mới (nhanh hơn save()).
 */
public class TransactionRepository extends CsvRepository<BookingTransaction> {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static final TransactionRepository INSTANCE = new TransactionRepository();
    public static TransactionRepository getInstance() { return INSTANCE; }

    private static final String FILE_PATH = "data/transactions.csv";
    private static final String HEADER    =
        "transactionId,fanId,matchId,numberOfTickets,totalAmount,status,mechanism,createdAt,durationMs";

    // ── Abstract method implementations ───────────────────────────────────────

    @Override
    public String getFilePath() { return FILE_PATH; }

    @Override
    public String getCsvHeader() { return HEADER; }

    @Override
    protected BookingTransaction parseFromCsvLine(String line) {
        return BookingTransaction.fromCsvLine(line);
    }

    // ── Domain-specific query methods ─────────────────────────────────────────

    /**
     * Lấy tất cả giao dịch của một fan.
     *
     * @param fanId ID fan (ví dụ: "FAN0001").
     * @return List các BookingTransaction của fan đó.
     */
    public List<BookingTransaction> findByFan(String fanId) {
        return findByCondition(t -> fanId != null && fanId.equals(t.getFanId()));
    }

    /**
     * Lấy tất cả giao dịch của một trận đấu.
     *
     * @param matchId ID trận đấu (ví dụ: "MATCH001").
     * @return List các BookingTransaction của trận đó.
     */
    public List<BookingTransaction> findByMatch(String matchId) {
        return findByCondition(t -> matchId != null && matchId.equals(t.getMatchId()));
    }

    /**
     * Lọc giao dịch theo cơ chế đồng bộ (NO_LOCK / FILE_LOCK / SYNCHRONIZED / OPTIMISTIC).
     *
     * <p>Dùng để so sánh hiệu năng các cơ chế ở T7.
     *
     * @param mechanism Cơ chế đồng bộ cần lọc.
     * @return List các BookingTransaction dùng cơ chế đó.
     */
    public List<BookingTransaction> findByMechanism(LockMechanism mechanism) {
        return findByCondition(t -> mechanism != null && mechanism == t.getMechanism());
    }

    /**
     * Lấy tất cả giao dịch theo trạng thái (SUCCESS / FAILED / PARTIAL).
     *
     * @param status Trạng thái cần lọc.
     * @return List các BookingTransaction khớp.
     */
    public List<BookingTransaction> findByStatus(TransactionStatus status) {
        return findByCondition(t -> status != null && status == t.getStatus());
    }

    /**
     * Lấy tất cả giao dịch thành công (status = SUCCESS).
     *
     * @return List các BookingTransaction SUCCESS.
     */
    public List<BookingTransaction> findSuccessful() {
        return findByStatus(TransactionStatus.SUCCESS);
    }

    /**
     * Lấy tất cả giao dịch thất bại hoặc bị lỗi một phần (FAILED / PARTIAL).
     *
     * @return List các BookingTransaction không hoàn toàn thành công.
     */
    public List<BookingTransaction> findFailed() {
        return findByCondition(t ->
            t.getStatus() == TransactionStatus.FAILED ||
            t.getStatus() == TransactionStatus.PARTIAL
        );
    }

    /**
     * Đếm số giao dịch theo cơ chế đồng bộ.
     *
     * <p>Dùng để tạo báo cáo thống kê sau khi chạy Simulator.
     *
     * @param mechanism Cơ chế đồng bộ cần đếm.
     * @return Số giao dịch dùng cơ chế đó.
     */
    public int countByMechanism(LockMechanism mechanism) {
        return findByMechanism(mechanism).size();
    }

    /**
     * Đếm số giao dịch thành công theo cơ chế.
     *
     * @param mechanism Cơ chế đồng bộ.
     * @return Số giao dịch SUCCESS của cơ chế đó.
     */
    public int countSuccessfulByMechanism(LockMechanism mechanism) {
        return (int) findByMechanism(mechanism).stream()
                     .filter(BookingTransaction::isSuccessful)
                     .count();
    }

    /**
     * Tính tổng doanh thu từ tất cả giao dịch SUCCESS.
     *
     * @return Tổng tiền (VND).
     */
    public long totalRevenue() {
        return findSuccessful().stream()
               .mapToLong(BookingTransaction::getTotalAmount)
               .sum();
    }

    /**
     * Tính thời gian xử lý trung bình (ms) của một cơ chế.
     *
     * <p>Dùng để so sánh hiệu năng các cơ chế đồng bộ ở T7.
     *
     * @param mechanism Cơ chế đồng bộ.
     * @return Thời gian trung bình (ms), hoặc 0 nếu không có dữ liệu.
     */
    public double avgDurationMs(LockMechanism mechanism) {
        List<BookingTransaction> txList = findByMechanism(mechanism);
        if (txList.isEmpty()) return 0.0;
        return txList.stream()
                     .mapToLong(BookingTransaction::getDurationMs)
                     .average()
                     .orElse(0.0);
    }
}
