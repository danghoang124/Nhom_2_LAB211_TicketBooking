package controller;

import model.entity.BookingTransaction;
import model.entity.Match;
import model.entity.Ticket;
import model.enums.LockMechanism;
import repository.MatchRepository;
import repository.TicketRepository;
import repository.TransactionRepository;

import java.util.List;

/**
 * Controller xử lý logic tổng hợp dữ liệu cho ReportView:
 * danh sách vé đã bán, danh sách giao dịch, thống kê doanh thu.
 *
 * <p><b>Nguyên tắc MVC:</b>
 * <ul>
 *   <li>ReportView KHÔNG được tự tính toán/lọc/sắp xếp dữ liệu.</li>
 *   <li>Mọi logic tổng hợp (sum, filter, group) đều nằm ở đây.</li>
 *   <li>Controller KHÔNG in ra màn hình, KHÔNG đọc ghi file trực tiếp —
 *       chỉ gọi Repository và trả dữ liệu thô (List, long, double...) về View.</li>
 * </ul>
 */
public class ReportController {

    private final TicketRepository ticketRepository;
    private final TransactionRepository transactionRepository;
    private final MatchRepository matchRepository;

    public ReportController(TicketRepository ticketRepository,
                             TransactionRepository transactionRepository,
                             MatchRepository matchRepository) {
        this.ticketRepository = ticketRepository;
        this.transactionRepository = transactionRepository;
        this.matchRepository = matchRepository;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 1. BÁO CÁO VÉ
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Lấy toàn bộ danh sách vé đã bán trong hệ thống (mọi fan, mọi trận).
     *
     * @return List tất cả Ticket hiện có.
     */
    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    /**
     * Lấy danh sách vé đã bán của một trận đấu cụ thể.
     *
     * @param matchId ID trận đấu.
     * @return List Ticket thuộc trận đó.
     */
    public List<Ticket> getTicketsByMatch(String matchId) {
        return ticketRepository.findByMatch(matchId);
    }

    /**
     * Đếm tổng số vé VALID (chưa hủy) đang có trong hệ thống.
     *
     * @return Số vé VALID.
     */
    public long countValidTickets() {
        return ticketRepository.findAll().stream()
                .filter(Ticket::isValid)
                .count();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 2. BÁO CÁO GIAO DỊCH
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Lấy toàn bộ danh sách giao dịch trong hệ thống.
     *
     * @return List tất cả BookingTransaction.
     */
    public List<BookingTransaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    /**
     * Lấy danh sách giao dịch của một fan cụ thể.
     *
     * @param fanId ID fan.
     * @return List BookingTransaction của fan đó.
     */
    public List<BookingTransaction> getTransactionsByFan(String fanId) {
        return transactionRepository.findByFan(fanId);
    }

    /**
     * Tính tổng doanh thu từ tất cả giao dịch thành công (SUCCESS).
     *
     * @return Tổng tiền (VND).
     */
    public long getTotalRevenue() {
        return transactionRepository.totalRevenue();
    }

    /**
     * Đếm số giao dịch thành công theo cơ chế đồng bộ — dùng để so sánh
     * hiệu năng NO_LOCK / SYNCHRONIZED / OPTIMISTIC / FILE_LOCK ở Tuần 7.
     *
     * @param mechanism Cơ chế đồng bộ cần thống kê.
     * @return Số giao dịch SUCCESS dùng cơ chế đó.
     */
    public int countSuccessfulByMechanism(LockMechanism mechanism) {
        return transactionRepository.countSuccessfulByMechanism(mechanism);
    }

    /**
     * Tính thời gian xử lý trung bình (ms) của một cơ chế đồng bộ.
     *
     * @param mechanism Cơ chế đồng bộ.
     * @return Thời gian trung bình (ms).
     */
    public double getAvgDurationMs(LockMechanism mechanism) {
        return transactionRepository.avgDurationMs(mechanism);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 3. BÁO CÁO TRẬN ĐẤU
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Lấy danh sách tất cả trận đấu đang mở bán vé (SCHEDULED).
     *
     * @return List Match SCHEDULED.
     */
    public List<Match> getScheduledMatches() {
        return matchRepository.findScheduledMatches();
    }

    /**
     * Lấy danh sách tất cả trận đấu trong hệ thống.
     *
     * @return List tất cả Match.
     */
    public List<Match> getAllMatches() {
        return matchRepository.findAll();
    }
}
