package controller;

import model.entity.BookingTransaction;
import model.entity.Match;
import model.entity.Seat;
import model.entity.Section;
import model.entity.Ticket;
import model.enums.SeatStatus;
import model.enums.SectionType;
import model.enums.TransactionStatus;
import repository.MatchRepository;
import repository.SeatRepository;
import repository.SectionRepository;
import repository.TicketRepository;
import repository.TransactionRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    /** Lấy tất cả vé trong hệ thống (dùng cho Admin). */
    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    /** Lấy tất cả giao dịch trong hệ thống (dùng cho Admin). */
    public List<BookingTransaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

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

    /** Tổng doanh thu (VND) từ vé VALID của một trận. */
    public long getMatchRevenue(String matchId) {
        return ticketRepository.findByMatch(matchId).stream()
                .filter(Ticket::isValid)
                .mapToLong(Ticket::getPrice)
                .sum();
    }

    // ── Doanh thu theo Section Type ──────────────────────────────────────────

    /**
     * Tính doanh thu theo loại khu vực (Section Type) cho TOÀN HỆ THỐNG.
     *
     * <p>Dùng cho Admin System Summary — hiển thị bảng doanh thu theo VIP, STANDARD, STANDING, ECONOMY.
     *
     * @return Map với key = SectionType, value = long[]{soVeDaBan, tongDoanhThu}.
     */
    public Map<SectionType, long[]> getRevenueBySectionType() {
        return calculateRevenueBySectionType(null);
    }

    /**
     * Tính doanh thu theo loại khu vực (Section Type) cho MỘT TRẬN ĐẤU.
     *
     * <p>Dùng cho Admin Seat Statistics — hiển thị doanh thu từng loại ghế trong trận.
     *
     * @param matchId ID trận đấu.
     * @return Map với key = SectionType, value = long[]{soVeDaBan, tongDoanhThu}.
     */
    public Map<SectionType, long[]> getRevenueBySectionType(String matchId) {
        return calculateRevenueBySectionType(matchId);
    }

    /**
     * Method nội bộ — tính doanh thu theo Section Type.
     *
     * @param matchId null = toàn hệ thống, không null = theo trận.
     */
    private Map<SectionType, long[]> calculateRevenueBySectionType(String matchId) {
        // 1. Lấy danh sách vé VALID
        List<Ticket> tickets;
        if (matchId == null) {
            tickets = ticketRepository.findByStatus(model.enums.TicketStatus.VALID);
        } else {
            tickets = ticketRepository.findByMatch(matchId);
            tickets = tickets.stream().filter(Ticket::isValid).toList();
        }

        // 2. Load Seat map (seatId → SectionId) cho hiệu năng
        Map<String, String> seatToSection = new HashMap<>();
        List<Seat> seats = matchId == null ? seatRepository.findAll() : seatRepository.findByMatch(matchId);
        for (Seat seat : seats) {
            seatToSection.put(seat.getSeatId(), seat.getSectionId());
        }

        // 3. Load Section map (sectionId → SectionType)
        Map<String, SectionType> sectionToType = new HashMap<>();
        for (Section section : sectionRepository.findAll()) {
            sectionToType.put(section.getSectionId(), section.getSectionType());
        }

        // 4. Group by SectionType, sum price
        Map<SectionType, long[]> result = new HashMap<>();
        for (Ticket ticket : tickets) {
            String sectionId = seatToSection.get(ticket.getSeatId());
            if (sectionId == null) continue;

            SectionType type = sectionToType.get(sectionId);
            if (type == null) continue;

            long[] stats = result.computeIfAbsent(type, k -> new long[]{0, 0});
            stats[0]++;        // số vé
            stats[1] += ticket.getPrice(); // doanh thu
        }

        return result;
    }
}
