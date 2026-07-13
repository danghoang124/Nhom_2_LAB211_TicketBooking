package controller;

import exception.BookingLimitExceededException;
import exception.SeatAlreadyBookedException;
import model.entity.BookingTransaction;
import model.entity.Match;
import model.entity.Seat;
import model.entity.Section;
import model.entity.Ticket;
import model.enums.LockMechanism;
import model.enums.MatchStatus;
import model.enums.SeatStatus;
import model.enums.TicketStatus;
import model.enums.TransactionStatus;
import repository.MatchRepository;
import repository.SeatRepository;
import repository.SectionRepository;
import repository.TicketRepository;
import repository.TransactionRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Controller xử lý logic nghiệp vụ đặt vé và hủy vé.
 *
 * <p><b>Luồng đặt vé 2 bước (AVAILABLE → LOCKED → BOOKED):</b>
 * <ol>
 *   <li>Gọi {@link #lockSeat} → ghế chuyển AVAILABLE → LOCKED.</li>
 *   <li>Nếu user confirm: gọi {@link #confirmBooking} → LOCKED → BOOKED.</li>
 *   <li>Nếu user cancel:  gọi {@link #cancelLockedSeat} → LOCKED → AVAILABLE.</li>
 * </ol>
 *
 * <p><b>Dependencies:</b>
 * <ul>
 *   <li>{@link SeatRepository}        — kiểm tra và cập nhật trạng thái ghế.</li>
 *   <li>{@link SectionRepository}     — tra giá vé theo khu vực (basePrice).</li>
 *   <li>{@link TicketRepository}      — tạo và quản lý vé.</li>
 *   <li>{@link TransactionRepository} — ghi lại lịch sử giao dịch.</li>
 * </ul>
 */
public class BookingController {

    private SeatRepository        seatRepository;
    private SectionRepository     sectionRepository;
    private TicketRepository      ticketRepository;
    private TransactionRepository transactionRepository;
    private MatchRepository       matchRepository;
    private DateTimeFormatter     formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Counter sinh Ticket ID — dạng TKT00000001, TKT00000002, ...
     * Khởi tạo bằng max ID hiện có trong CSV để không bao giờ trung sau khi restart.
     * Thread-safe: AtomicLong.getAndIncrement() là atomic operation.
     */
    private AtomicLong ticketCounter;

    /**
     * Counter sinh Transaction ID — dạng TXN00000001, TXN00000002, ...
     * Khởi tạo bằng max ID hiện có trong CSV để không bao giờ trùng sau khi restart.
     * Thread-safe: AtomicLong.getAndIncrement() là atomic operation.
     */
    private AtomicLong transactionCounter;

    /** Giá mặc định dùng khi không tra được Section (fallback). */
    private static final long DEFAULT_PRICE = 500_000L;

    /** Số vé tối đa cho phép trong một giao dịch. */
    private static final int MAX_TICKETS_PER_TRANSACTION = 4;

    public BookingController(SeatRepository seatRepository,
                             SectionRepository sectionRepository,
                             TicketRepository ticketRepository,
                             TransactionRepository transactionRepository,
                             MatchRepository matchRepository) {
        this.seatRepository        = seatRepository;
        this.sectionRepository     = sectionRepository;
        this.ticketRepository      = ticketRepository;
        this.transactionRepository = transactionRepository;
        this.matchRepository       = matchRepository;

        // Khởi tạo counter từ max ID đang có trong CSV
        // Đảm bảo khởi động lại app không bao giờ sinh ra ID trùng với records cũ
        this.ticketCounter      = new AtomicLong(initCounterFromTickets(ticketRepository));
        this.transactionCounter = new AtomicLong(initCounterFromTransactions(transactionRepository));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LUỒNG 2 BƯỚC: LOCK → CONFIRM / CANCEL
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Bước 1 — Khoá ghế (AVAILABLE → LOCKED) để giữ chỗ cho user xác nhận.
     *
     * <p>Sau khi lock thành công, ghế được đánh dấu LOCKED trong CSV.
     * View phải hiển thị thông tin xác nhận cho user, rồi gọi
     * {@link #confirmBooking} hoặc {@link #cancelLockedSeat}.
     *
     * @param seatId ID ghế cần khoá.
     * @return {@code true} nếu khoá thành công.
     * @throws SeatAlreadyBookedException nếu ghế đã BOOKED hoặc LOCKED bởi người khác.
     */
    public boolean lockSeat(String seatId) {
        Optional<Seat> seatOpt = seatRepository.findById(seatId);
        if (!seatOpt.isPresent()) {
            return false;
        }
        Seat seat = seatOpt.get();

        // Kiểm tra trạng thái — chỉ cho phép lock ghế AVAILABLE
        if (seat.getStatus() == SeatStatus.BOOKED) {
            throw new SeatAlreadyBookedException(
                "Ghế " + seatId + " đã được đặt bởi người khác. Vui lòng chọn ghế khác.");
        }
        if (seat.getStatus() == SeatStatus.LOCKED) {
            throw new SeatAlreadyBookedException(
                "Ghế " + seatId + " đang được giữ chỗ. Vui lòng chọn ghế khác.");
        }

        // Chuyển AVAILABLE → LOCKED
        seat.updateStatus(SeatStatus.LOCKED);
        seatRepository.save(seat);
        return true;
    }

    /**
     * Bước 2a — Xác nhận đặt vé (LOCKED → BOOKED), sinh Ticket và Transaction.
     *
     * <p>Phải gọi {@link #lockSeat} trước. Nếu không, ghế chưa ở trạng thái LOCKED
     * thì method này sẽ thất bại.
     *
     * @param fanId   ID fan đặt vé.
     * @param matchId ID trận đấu.
     * @param seatId  ID ghế đã được lock trước đó.
     * @param mechanism Cơ chế đồng bộ (NO_LOCK, SYNCHRONIZED, FILE_LOCK, OPTIMISTIC).
     * @return {@code true} nếu confirm thành công.
     * @throws SeatAlreadyBookedException nếu ghế không ở trạng thái LOCKED (đã bị người khác đặt mất).
     */
    public boolean confirmBooking(String fanId, String matchId, String seatId,
                                   LockMechanism mechanism) {
        long startTime = System.currentTimeMillis();
        String transactionId = generateTransactionId();

        Optional<Seat> seatOpt = seatRepository.findById(seatId);
        if (!seatOpt.isPresent()) {
            createTransaction(transactionId, fanId, matchId, 1, 0L,
                    TransactionStatus.FAILED, mechanism, startTime);
            return false;
        }

        // Business logic validation: Cannot confirm booking if the match is no longer SCHEDULED
        Optional<Match> matchOpt = matchRepository.findById(matchId);
        if (matchOpt.isPresent() && matchOpt.get().getStatus() != MatchStatus.SCHEDULED) {
            createTransaction(transactionId, fanId, matchId, 1, 0L,
                    TransactionStatus.FAILED, mechanism, startTime);
            throw new IllegalStateException(
                "Match is no longer open for booking (Status: " + matchOpt.get().getStatus().name() + ").");
        }

        // Anti-scalping limit: Max 4 valid tickets per user per match
        long validTicketCount = ticketRepository.findValidTickets(fanId).stream()
                .filter(t -> matchId.equals(t.getMatchId()))
                .count();
        if (validTicketCount >= 4) {
            createTransaction(transactionId, fanId, matchId, 1, 0L,
                    TransactionStatus.FAILED, mechanism, startTime);
            throw new exception.BookingLimitExceededException(
                "You have reached the maximum limit of 4 tickets for this match.");
        }

        Seat seat = seatOpt.get();

        // Lấy giá vé từ Section
        Optional<Section> sectionOpt = sectionRepository.findById(seat.getSectionId());
        long totalAmount = sectionOpt.map(Section::getBasePrice).orElse(DEFAULT_PRICE);

        // Kiểm tra ghế phải ở LOCKED (đã được giữ từ bước 1)
        if (seat.getStatus() == SeatStatus.BOOKED
                || ticketRepository.existsBySeatAndMatch(seatId, matchId)) {
            createTransaction(transactionId, fanId, matchId, 1, totalAmount,
                    TransactionStatus.FAILED, mechanism, startTime);
            throw new SeatAlreadyBookedException(
                "Ghế " + seatId + " đã được đặt trong lúc bạn xác nhận. Vui lòng chọn ghế khác.");
        }

        // Chuyển LOCKED → BOOKED theo cơ chế đồng bộ
        boolean updateSuccess = false;
        switch (mechanism) {
            case NO_LOCK:
                seat.updateStatus(SeatStatus.BOOKED);
                seatRepository.save(seat);
                updateSuccess = true;
                break;
            case SYNCHRONIZED:
                updateSuccess = seatRepository.updateStatusSynchronized(seatId, SeatStatus.BOOKED);
                break;
            case FILE_LOCK:
                updateSuccess = seatRepository.updateStatusFileLock(seatId, SeatStatus.BOOKED);
                break;
            case OPTIMISTIC:
                updateSuccess = seatRepository.updateStatusOptimistic(seatId, SeatStatus.BOOKED, seat.getVersion());
                break;
            default:
                updateSuccess = false;
        }

        if (!updateSuccess) {
            createTransaction(transactionId, fanId, matchId, 1, totalAmount,
                    TransactionStatus.FAILED, mechanism, startTime);
            return false;
        }

        // Sinh Ticket và Transaction thành công
        createTicket(fanId, matchId, seatId, transactionId, totalAmount);
        createTransaction(transactionId, fanId, matchId, 1, totalAmount,
                TransactionStatus.SUCCESS, mechanism, startTime);
        return true;
    }

    /**
     * Bước 2b — Huỷ giữ chỗ (LOCKED → AVAILABLE).
     *
     * <p>Gọi khi user chọn không đặt sau khi đã lock ghế,
     * hoặc khi timeout xảy ra trước khi user xác nhận.
     *
     * @param seatId ID ghế đang ở trạng thái LOCKED.
     * @return {@code true} nếu restore thành công.
     */
    public boolean cancelLockedSeat(String seatId) {
        Optional<Seat> seatOpt = seatRepository.findById(seatId);
        if (!seatOpt.isPresent()) return false;

        Seat seat = seatOpt.get();
        // Chỉ restore nếu ghế đang LOCKED
        if (seat.getStatus() != SeatStatus.LOCKED) return false;

        seat.updateStatus(SeatStatus.AVAILABLE);
        seatRepository.save(seat);
        return true;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LEGACY — bookSeat (dùng cho Simulator, không dùng flow 2 bước)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Thực hiện quy trình đặt vé trực tiếp (dùng chủ yếu bởi Simulator).
     *
     * <p>Khác với luồng 2 bước, method này đặt thẳng AVAILABLE → BOOKED.
     * View tương tác với user nên dùng {@link #lockSeat} + {@link #confirmBooking}.
     *
     * @param fanId     ID fan.
     * @param matchId   ID trận đấu.
     * @param seatId    ID ghế.
     * @param mechanism Cơ chế đồng bộ.
     * @return {@code true} nếu đặt thành công.
     * @throws SeatAlreadyBookedException nếu ghế đã được đặt hoặc khoá.
     * @throws BookingLimitExceededException (chưa áp dụng ở đây vì chỉ 1 ghế)
     */
    public boolean bookSeat(String fanId, String matchId, String seatId, LockMechanism mechanism) {
        long startTime = System.currentTimeMillis();
        String transactionId = generateTransactionId();
        long totalAmount = 0;

        try {
            // 1. Kiểm tra ghế tồn tại
            Optional<Seat> seatOpt = seatRepository.findById(seatId);
            if (!seatOpt.isPresent()) {
                createTransaction(transactionId, fanId, matchId, 1, totalAmount,
                        TransactionStatus.FAILED, mechanism, startTime);
                return false;
            }
            Seat seat = seatOpt.get();

            // Lấy giá vé từ Section
            Optional<Section> sectionOpt = sectionRepository.findById(seat.getSectionId());
            totalAmount = sectionOpt.map(Section::getBasePrice).orElse(DEFAULT_PRICE);

            // 2. Kiểm tra match đang mở bán vé
            Optional<Match> matchOpt = matchRepository.findById(matchId);
            if (matchOpt.isPresent() && matchOpt.get().getStatus() != MatchStatus.SCHEDULED) {
                createTransaction(transactionId, fanId, matchId, 1, totalAmount,
                        TransactionStatus.FAILED, mechanism, startTime);
                return false;
            }

            // 3. Kiểm tra ghế còn trống — throw nếu đã BOOKED
            if (seat.getStatus() == SeatStatus.BOOKED
                    || ticketRepository.existsBySeatAndMatch(seatId, matchId)) {
                createTransaction(transactionId, fanId, matchId, 1, totalAmount,
                        TransactionStatus.FAILED, mechanism, startTime);
                throw new SeatAlreadyBookedException(
                    "Seat " + seatId + " is already booked (Double Booking prevented).");
            }

            // 3. Đổi trạng thái Seat sang BOOKED theo cơ chế
            boolean updateSuccess = false;
            switch (mechanism) {
                case NO_LOCK:
                    seat.updateStatus(SeatStatus.BOOKED);
                    seatRepository.save(seat);
                    updateSuccess = true;
                    break;
                case SYNCHRONIZED:
                    updateSuccess = seatRepository.updateStatusSynchronized(seatId, SeatStatus.BOOKED);
                    break;
                case FILE_LOCK:
                    updateSuccess = seatRepository.updateStatusFileLock(seatId, SeatStatus.BOOKED);
                    break;
                case OPTIMISTIC:
                    // Retry loop: đọc lại version mới nhất sau mỗi lần conflict
                    int maxRetries = 20;
                    for (int attempt = 0; attempt < maxRetries; attempt++) {
                        Optional<Seat> freshSeat = seatRepository.findByIdForUpdate(seatId);
                        if (!freshSeat.isPresent()) break;
                        Seat latestSeat = freshSeat.get();
                        // Nếu ghế đã bị đặt bởi thread khác → dừng retry
                        if (latestSeat.getStatus() == SeatStatus.BOOKED
                                || ticketRepository.existsBySeatAndMatch(seatId, matchId)) {
                            break;
                        }
                        updateSuccess = seatRepository.updateStatusOptimistic(
                            seatId, SeatStatus.BOOKED, latestSeat.getVersion());
                        if (updateSuccess) break;
                        // conflict → thử lại với version mới nhất
                    }
                    break;
                default:
                    updateSuccess = false;
            }

            if (!updateSuccess) {
                createTransaction(transactionId, fanId, matchId, 1, totalAmount,
                        TransactionStatus.FAILED, mechanism, startTime);
                return false;
            }

            // 4. Sinh Ticket & Transaction
            createTicket(fanId, matchId, seatId, transactionId, totalAmount);
            createTransaction(transactionId, fanId, matchId, 1, totalAmount,
                    TransactionStatus.SUCCESS, mechanism, startTime);
            return true;

        } catch (SeatAlreadyBookedException e) {
            throw e; // re-throw để Simulator/caller xử lý
        } catch (Exception e) {
            createTransaction(transactionId, fanId, matchId, 1, totalAmount,
                    TransactionStatus.FAILED, mechanism, startTime);
            return false;
        }
    }

    /**
     * Đặt nhiều ghế cùng lúc — tối đa {@value #MAX_TICKETS_PER_TRANSACTION} vé/giao dịch.
     *
     * <p><b>Thiết kế 1 transaction cho N vé:</b> Một {@code transactionId} duy nhất được
     * sinh ra trước khi vòng lặp bắt đầu. Tất cả {@link model.entity.Ticket} tạo ra bên
     * trong vòng lặp đều gắn cùng {@code transactionId} đó. Cuối cùng chỉ có
     * <b>1 bản ghi {@link model.entity.BookingTransaction}</b> được ghi vào CSV, phản ánh
     * kết quả toàn bộ giao dịch:
     * <ul>
     *   <li>{@link TransactionStatus#SUCCESS}  — tất cả ghế đặt thành công.</li>
     *   <li>{@link TransactionStatus#PARTIAL}  — một phần ghế thành công (1 ≤ success &lt; total).</li>
     *   <li>{@link TransactionStatus#FAILED}   — không đặt được ghế nào.</li>
     * </ul>
     *
     * @param fanId     ID fan.
     * @param matchId   ID trận đấu.
     * @param seatIds   Danh sách seat ID cần đặt (1–{@value #MAX_TICKETS_PER_TRANSACTION} ghế).
     * @param mechanism Cơ chế đồng bộ.
     * @return Số ghế đặt thành công.
     * @throws BookingLimitExceededException nếu seatIds rỗng hoặc vượt quá giới hạn.
     */
    public int bookMultipleSeats(String fanId, String matchId,
                                  List<String> seatIds, LockMechanism mechanism) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new BookingLimitExceededException(
                "Phải chọn ít nhất 1 ghế để đặt vé.");
        }
        if (seatIds.size() > MAX_TICKETS_PER_TRANSACTION) {
            throw new BookingLimitExceededException(
                "Không thể đặt quá " + MAX_TICKETS_PER_TRANSACTION
                + " vé trong một giao dịch. Bạn đã chọn " + seatIds.size() + " ghế.");
        }

        long startTime = System.currentTimeMillis();
        String transactionId = generateTransactionId();
        int successCount = 0;
        long totalAmount = 0;

        for (String seatId : seatIds) {
            try {
                Optional<Seat> seatOpt = seatRepository.findById(seatId);
                if (!seatOpt.isPresent()) continue;
                Seat seat = seatOpt.get();

                Optional<Section> sectionOpt = sectionRepository.findById(seat.getSectionId());
                long price = sectionOpt.map(Section::getBasePrice).orElse(DEFAULT_PRICE);

                Optional<Match> matchOpt = matchRepository.findById(matchId);
                if (matchOpt.isPresent() && matchOpt.get().getStatus() != MatchStatus.SCHEDULED) continue;

                if (seat.getStatus() == SeatStatus.BOOKED
                        || ticketRepository.existsBySeatAndMatch(seatId, matchId)) continue;

                boolean updateSuccess = false;
                switch (mechanism) {
                    case NO_LOCK:
                        seat.updateStatus(SeatStatus.BOOKED);
                        seatRepository.save(seat);
                        updateSuccess = true;
                        break;
                    case SYNCHRONIZED:
                        updateSuccess = seatRepository.updateStatusSynchronized(seatId, SeatStatus.BOOKED);
                        break;
                    case FILE_LOCK:
                        updateSuccess = seatRepository.updateStatusFileLock(seatId, SeatStatus.BOOKED);
                        break;
                    case OPTIMISTIC:
                        updateSuccess = seatRepository.updateStatusOptimistic(seatId, SeatStatus.BOOKED, seat.getVersion());
                        break;
                }

                if (!updateSuccess) continue;

                createTicket(fanId, matchId, seatId, transactionId, price);
                successCount++;
                totalAmount += price;

            } catch (Exception e) {
                // skip failed seat, continue with next
            }
        }

        TransactionStatus status;
        if (successCount == seatIds.size()) {
            status = TransactionStatus.SUCCESS;
        } else if (successCount > 0) {
            status = TransactionStatus.PARTIAL;
        } else {
            status = TransactionStatus.FAILED;
        }

        createTransaction(transactionId, fanId, matchId, successCount, totalAmount,
                status, mechanism, startTime);

        return successCount;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // KIỂM TRA GIỚI HẠN VÉ
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Đếm số vé VALID (chưa huỷ) của fan trong một trận đấu.
     *
     * @param fanId   ID fan.
     * @param matchId ID trận đấu.
     * @return Số vé còn hiệu lực.
     */
    public int getValidTicketCountForMatch(String fanId, String matchId) {
        return (int) ticketRepository.findValidTickets(fanId).stream()
                .filter(t -> matchId.equals(t.getMatchId()))
                .count();
    }

    /**
     * Kiểm tra fan có thể đặt thêm vé cho trận này không (giới hạn {@value #MAX_TICKETS_PER_TRANSACTION} vé/trận).
     *
     * @param fanId   ID fan.
     * @param matchId ID trận đấu.
     * @return {@code true} nếu fan chưa đạt giới hạn.
     */
    public boolean canBookMoreTickets(String fanId, String matchId) {
        return getValidTicketCountForMatch(fanId, matchId) < MAX_TICKETS_PER_TRANSACTION;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GHI GIAO DỊCH THẤT BẠI
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Ghi lại một giao dịch thất bại (FAILED) — ví dụ: từ chối thanh toán.
     *
     * @param fanId           ID fan.
     * @param matchId         ID trận đấu.
     * @param numberOfTickets Số vé.
     * @param totalAmount     Tổng tiền.
     * @param mechanism       Cơ chế đồng bộ.
     */
    public void recordFailedTransaction(String fanId, String matchId,
                                         int numberOfTickets, long totalAmount,
                                         LockMechanism mechanism) {
        long startTime = System.currentTimeMillis();
        String transactionId = generateTransactionId();
        createTransaction(transactionId, fanId, matchId, numberOfTickets, totalAmount,
                TransactionStatus.FAILED, mechanism, startTime);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HUỶ VÉ
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Hủy vé đã đặt (VALID → CANCELLED), phục hồi ghế (BOOKED → AVAILABLE)
     * và ghi giao dịch hoàn tiền (REFUNDED).
     *
     * @param fanId    ID của user đang yêu cầu hủy (dùng để kiểm tra quyền sở hữu).
     * @param ticketId ID vé cần hủy.
     * @return {@code true} nếu hủy thành công.
     */
    public boolean cancelBooking(String fanId, String ticketId) {
        Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
        if (!ticketOpt.isPresent() || ticketOpt.get().isCancelled()) {
            return false;
        }
        Ticket ticket = ticketOpt.get();

        // Security validation: Only the ticket owner can cancel it
        if (!ticket.getFanId().equals(fanId)) {
            throw new IllegalArgumentException("Access Denied: You do not own this ticket.");
        }

        // Business logic validation: Cannot cancel a ticket if the match is no longer SCHEDULED
        Optional<Match> matchOpt = matchRepository.findById(ticket.getMatchId());
        if (matchOpt.isPresent() && matchOpt.get().getStatus() != MatchStatus.SCHEDULED) {
            throw new IllegalStateException("Cannot cancel a ticket for a match that is already " + matchOpt.get().getStatus().name() + ".");
        }

        long startTime = System.currentTimeMillis();

        // Hủy vé
        ticket.setStatus(TicketStatus.CANCELLED);
        ticketRepository.save(ticket);

        // Phục hồi ghế về AVAILABLE.
        // Dùng updateStatus() để tăng version — nếu dùng setStatus() thô thì version
        // không tăng, Optimistic Locking sẽ bị lệch ở các lần booking tiếp theo.
        Optional<Seat> seatOpt = seatRepository.findById(ticket.getSeatId());
        if (seatOpt.isPresent()) {
            Seat seat = seatOpt.get();
            seat.updateStatus(SeatStatus.AVAILABLE);
            seatRepository.save(seat);
        }

        // Ghi giao dịch hoàn tiền
        String transactionId = generateTransactionId();
        createTransaction(transactionId, fanId, ticket.getMatchId(), 1, ticket.getPrice(),
                TransactionStatus.REFUNDED, LockMechanism.SYNCHRONIZED, startTime);

        return true;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Lấy giá vé của một ghế dựa trên sectionId.
     *
     * @param seatId ID ghế.
     * @return Giá vé (đơn vị VND), hoặc DEFAULT_PRICE nếu không tra được.
     */
    public long getPriceForSeat(String seatId) {
        Optional<Seat> seatOpt = seatRepository.findById(seatId);
        if (!seatOpt.isPresent()) return DEFAULT_PRICE;
        Optional<Section> sectionOpt = sectionRepository.findById(seatOpt.get().getSectionId());
        return sectionOpt.map(Section::getBasePrice).orElse(DEFAULT_PRICE);
    }

    /**
     * Sinh đối tượng Ticket và lưu vào repository.
     */
    public void createTicket(String fanId, String matchId, String seatId,
                              String transactionId, long price) {
        String ticketId = generateTicketId();
        String bookedAt = LocalDateTime.now().format(formatter);
        Ticket ticket = new Ticket(ticketId, fanId, seatId, matchId,
                transactionId, price, bookedAt, TicketStatus.VALID);
        ticketRepository.append(ticket);
    }

    /**
     * Sinh đối tượng BookingTransaction và lưu vào repository.
     */
    public void createTransaction(String transactionId, String fanId, String matchId,
                                   int numberOfTickets, long totalAmount,
                                   TransactionStatus status, LockMechanism mechanism,
                                   long startTime) {
        String createdAt = LocalDateTime.now().format(formatter);
        long durationMs = System.currentTimeMillis() - startTime;
        BookingTransaction transaction = new BookingTransaction(
                transactionId, fanId, matchId, numberOfTickets,
                totalAmount, status, mechanism, createdAt, durationMs);
        transactionRepository.append(transaction);
    }

    /**
     * Sinh Ticket ID dạng TKT00000001 dùng AtomicLong counter.
     * Không bao giờ trùng — ngay cả khi 500 thread gọi đồng thời.
     */
    private String generateTicketId() {
        return String.format("TKT%08d", ticketCounter.getAndIncrement());
    }

    /**
     * Sinh Transaction ID dạng TXN00000001 dùng AtomicLong counter.
     * Không bao giờ trùng — ngay cả khi 500 thread gọi đồng thời.
     */
    private String generateTransactionId() {
        return String.format("TXN%08d", transactionCounter.getAndIncrement());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COUNTER INIT HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Đọc tất cả ticketId hiện có trong CSV, tìm số lớn nhất rồi +1.
     * VD: max(TKT00000042, TKT00000017) = 42 → counter bắt đầu từ 43.
     *
     * @return Giá trị khởi đầu cho ticketCounter (tối thiểu là 1).
     */
    private static long initCounterFromTickets(TicketRepository repo) {
        long max = 0;
        try {
            for (model.entity.Ticket t : repo.findAll()) {
                String id = t.getTicketId(); // "TKT00000042"
                if (id != null && id.startsWith("TKT") && id.length() > 3) {
                    try {
                        long num = Long.parseLong(id.substring(3));
                        if (num > max) max = num;
                    } catch (NumberFormatException ignored) { }
                }
            }
        } catch (Exception ignored) { }
        return max + 1;
    }

    /**
     * Đọc tất cả transactionId hiện có trong CSV, tìm số lớn nhất rồi +1.
     *
     * @return Giá trị khởi đầu cho transactionCounter (tối thiểu là 1).
     */
    private static long initCounterFromTransactions(TransactionRepository repo) {
        long max = 0;
        try {
            for (model.entity.BookingTransaction tx : repo.findAll()) {
                String id = tx.getTransactionId(); // "TXN00000017"
                if (id != null && id.startsWith("TXN") && id.length() > 3) {
                    try {
                        long num = Long.parseLong(id.substring(3));
                        if (num > max) max = num;
                    } catch (NumberFormatException ignored) { }
                }
            }
        } catch (Exception ignored) { }
        return max + 1;
    }
}
