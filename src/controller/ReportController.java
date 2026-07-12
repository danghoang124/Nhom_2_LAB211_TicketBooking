package controller;

import model.entity.BookingTransaction;
import model.entity.Match;
import model.entity.Ticket;
import model.enums.SeatStatus;
import model.enums.TransactionStatus;
import repository.MatchRepository;
import repository.SeatRepository;
import repository.SectionRepository;
import repository.TicketRepository;
import repository.TransactionRepository;

import java.util.List;

/**
 * Controller xử lý logic báo cáo và thống kê hệ thống.
 *
 * <p>Phụ trách: Thành viên 4 (Khánh)
 */
public class ReportController {

    private TicketRepository      ticketRepository;
    private TransactionRepository transactionRepository;
    private MatchRepository       matchRepository;
    private SeatRepository        seatRepository;
    private SectionRepository     sectionRepository;

    public ReportController(TicketRepository ticketRepository,
                            TransactionRepository transactionRepository,
                            MatchRepository matchRepository,
                            SeatRepository seatRepository,
                            SectionRepository sectionRepository) {
        this.ticketRepository      = ticketRepository;
        this.transactionRepository = transactionRepository;
        this.matchRepository       = matchRepository;
        this.seatRepository        = seatRepository;
        this.sectionRepository     = sectionRepository;
    }

    // ── Vé của Fan ───────────────────────────────────────────────────────────

    /** Lấy danh sách vé hợp lệ (chưa hủy) của Fan theo fanId. */
    public List<Ticket> getTicketsByFan(String fanId) {
        return ticketRepository.findValidTickets(fanId);
    }

    /**
     * Lấy tất cả vé của Fan (cả VALID lẫn CANCELLED).
     *
     * <p>Dùng cho màn hình "Lịch sử vé" — tương đương với
     * {@link controller.FanController#getMyTickets()} để đảm bảo nhất quán
     * giữa MainView và ReportView.
     */
    public List<Ticket> getAllTicketsByFan(String fanId) {
        return ticketRepository.findByFan(fanId);
    }

    // ── Lịch sử giao dịch ────────────────────────────────────────────────────

    /** Lấy danh sách giao dịch của Fan theo fanId. */
    public List<BookingTransaction> getTransactionsByFan(String fanId) {
        return transactionRepository.findByFan(fanId);
    }

    // ── Thống kê hệ thống ────────────────────────────────────────────────────

    /** Tổng số giao dịch trong hệ thống. */
    public int getTotalTransactionCount() {
        return transactionRepository.findAll().size();
    }

    /** Số giao dịch thành công. */
    public int getSuccessCount() {
        return transactionRepository.findSuccessful().size();
    }

    /** Số giao dịch thất bại. */
    public int getFailedCount() {
        return transactionRepository.findFailed().size();
    }

    /** Tổng doanh thu (VND). Chỉ tính từ vé hợp lệ (VALID). */
    public long getTotalRevenue() {
        return ticketRepository.totalRevenue();
    }

    // ── Thống kê ghế theo trận ───────────────────────────────────────────────

    /** Lấy tất cả trận đấu. */
    public List<Match> getAllMatches() {
        return matchRepository.findAll();
    }

    /**
     * Thống kê ghế của một trận đấu.
     *
     * @return int[3]: [0] = tổng ghế, [1] = đã đặt, [2] = còn trống
     */
    public int[] getMatchSeatSummary(String matchId) {
        int total    = seatRepository.findByMatch(matchId).size();
        int booked   = seatRepository.countByStatus(matchId, SeatStatus.BOOKED);
        int available = seatRepository.countAvailable(matchId);
        return new int[]{total, booked, available};
    }

    /** Số vé đã bán thành công cho một trận. */
    public int getSoldTicketCount(String matchId) {
        return ticketRepository.countSoldTickets(matchId);
    }
}
