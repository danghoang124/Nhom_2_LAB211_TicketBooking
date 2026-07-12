package main;

import controller.AdminController;
import controller.BookingController;
import controller.FanController;
import controller.ReportController;
import controller.SimulatorController;
import controller.StadiumController;
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
    private FanRepository fanRepository;
    private MatchRepository matchRepository;
    private SeatRepository seatRepository;
    private SectionRepository sectionRepository;
    private StadiumRepository stadiumRepository;
    private TicketRepository ticketRepository;
    private TransactionRepository transactionRepository;

    // ── Controllers ──────────────────────────────────────────────
    private AdminController adminController;
    private FanController fanController;
    private BookingController bookingController;
    private ReportController reportController;
    private StadiumController stadiumController;
    private SimulatorController simulatorController;

    // ── Constructor — wiring tất cả thành phần ─────────────────────────────────
    public AppContext() {
        // 1. Khởi tạo Repositories (Singleton — luôn dùng getInstance())
        this.fanRepository = FanRepository.getInstance();
        this.matchRepository = MatchRepository.getInstance();
        this.seatRepository = SeatRepository.getInstance();
        this.sectionRepository = SectionRepository.getInstance();
        this.stadiumRepository = StadiumRepository.getInstance();
        this.ticketRepository = TicketRepository.getInstance();
        this.transactionRepository = TransactionRepository.getInstance();

        // 2. Khởi tạo Controllers (inject dependencies)
        this.adminController = new AdminController(
                stadiumRepository, sectionRepository, matchRepository);
        this.fanController = new FanController(fanRepository, ticketRepository);
        this.bookingController = new BookingController(
                seatRepository, sectionRepository, ticketRepository, transactionRepository, matchRepository);
        this.reportController = new ReportController(ticketRepository, transactionRepository,
                                                      matchRepository, seatRepository, sectionRepository);
        this.stadiumController = new StadiumController(
                matchRepository, sectionRepository, seatRepository);

        this.simulatorController = new SimulatorController(
                bookingController, seatRepository, ticketRepository, transactionRepository, matchRepository);
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
    public StadiumController getStadiumController() { return stadiumController; }
    public SimulatorController getSimulatorController() { return simulatorController; }
}
