package main;

import controller.AdminController;
import controller.BookingController;
import controller.FanController;
import controller.ReportController;
import repository.*;

/**
 * Application Context — DI Container thủ công cho hệ thống.
 *
 * <p>Khởi tạo tập trung toàn bộ Repository và Controller,
 * đảm bảo mỗi Repository chỉ tạo 1 instance (singleton trong context).
 *
 * <p>Sử dụng:
 * <pre>
 *   AppContext ctx = new AppContext();
 *   MainView mainView = new MainView(ctx);
 *   mainView.start();
 * </pre>
 */
public class AppContext {

    // ── Repositories ───────────────────────────────────────────────────────────
    private final FanRepository fanRepository;
    private final MatchRepository matchRepository;
    private final SeatRepository seatRepository;
    private final SectionRepository sectionRepository;
    private final StadiumRepository stadiumRepository;
    private final TicketRepository ticketRepository;
    private final TransactionRepository transactionRepository;

    // ── Controllers ──────────────────────────────────────────────
    private final AdminController adminController;
    private final FanController fanController;
    private final BookingController bookingController;
    private final ReportController reportController;

    // ── Constructor — wiring tất cả thành phần ─────────────────────────────────
    public AppContext() {
        // 1. Khởi tạo Repositories
        this.fanRepository = new FanRepository();
        this.matchRepository = new MatchRepository();
        this.seatRepository = new SeatRepository();
        this.sectionRepository = new SectionRepository();
        this.stadiumRepository = new StadiumRepository();
        this.ticketRepository = new TicketRepository();
        this.transactionRepository = new TransactionRepository();

        // 2. Khởi tạo Controllers (inject dependencies)
        this.adminController = new AdminController(
                stadiumRepository, sectionRepository, matchRepository);
        this.fanController = new FanController(fanRepository, ticketRepository);
        this.bookingController = new BookingController(
                seatRepository, sectionRepository, ticketRepository, transactionRepository);
        this.reportController = new ReportController(ticketRepository, transactionRepository,
                                                      matchRepository, seatRepository, sectionRepository);
    }

    // ── Getters ────────────────────────────────────────────────────────────────

    public FanRepository getFanRepository() { return fanRepository; }
    public MatchRepository getMatchRepository() { return matchRepository; }
    public SeatRepository getSeatRepository() { return seatRepository; }
    public SectionRepository getSectionRepository() { return sectionRepository; }
    public StadiumRepository getStadiumRepository() { return stadiumRepository; }
    public TicketRepository getTicketRepository() { return ticketRepository; }
    public TransactionRepository getTransactionRepository() { return transactionRepository; }

    public AdminController getAdminController() { return adminController; }
    public FanController getFanController() { return fanController; }
    public BookingController getBookingController() { return bookingController; }
    public ReportController getReportController() { return reportController; }
}
