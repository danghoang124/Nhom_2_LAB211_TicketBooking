package controller;

import model.entity.BookingTransaction;
import model.entity.Seat;
import model.entity.Section;
import model.entity.Ticket;
import model.enums.LockMechanism;
import model.enums.SeatStatus;
import model.enums.TicketStatus;
import model.enums.TransactionStatus;
import repository.SeatRepository;
import repository.SectionRepository;
import repository.TicketRepository;
import repository.TransactionRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller xử lý logic nghiệp vụ đặt vé và hủy vé.
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

    private final SeatRepository        seatRepository;
    private final SectionRepository     sectionRepository;
    private final TicketRepository      ticketRepository;
    private final TransactionRepository transactionRepository;
    private final DateTimeFormatter     formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Giá mặc định dùng khi không tra được Section (fallback). */
    private static final long DEFAULT_PRICE = 500_000L;

    public BookingController(SeatRepository seatRepository,
                             SectionRepository sectionRepository,
                             TicketRepository ticketRepository,
                             TransactionRepository transactionRepository) {
        this.seatRepository        = seatRepository;
        this.sectionRepository     = sectionRepository;
        this.ticketRepository      = ticketRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Thực hiện quy trình đặt vé.
     */
    public boolean bookSeat(String fanId, String matchId, String seatId, LockMechanism mechanism) {
        long startTime = System.currentTimeMillis();
        boolean success = false;
        long totalAmount = 0; // Tạm giả định giá vé, có thể lấy từ Seat hoặc Match
        String transactionId = "TXN" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        try {
            // 1. Kiểm tra ghế còn trống
            Optional<Seat> seatOpt = seatRepository.findById(seatId);
            if (!seatOpt.isPresent()) {
                System.out.println("Seat not found: " + seatId);
                createTransaction(transactionId, fanId, matchId, 1, totalAmount, TransactionStatus.FAILED, mechanism, startTime);
                return false;
            }
            Seat seat = seatOpt.get();
            
            // Lấy giá vé từ Section tương ứng với ghế.
            // seat.getSectionId() → tra SectionRepository → section.getBasePrice().
            // Nếu không tìm thấy Section (dữ liệu bất thường) thì dùng DEFAULT_PRICE.
            Optional<Section> sectionOpt = sectionRepository.findById(seat.getSectionId());
            totalAmount = sectionOpt.map(Section::getBasePrice).orElse(DEFAULT_PRICE);
            
            if (seat.getStatus() == SeatStatus.BOOKED || ticketRepository.existsBySeatAndMatch(seatId, matchId)) {
                System.out.println("Seat already booked: " + seatId);
                createTransaction(transactionId, fanId, matchId, 1, totalAmount, TransactionStatus.FAILED, mechanism, startTime);
                return false;
            }

            // 2. Đổi trạng thái Seat sang BOOKED theo cơ chế
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
                System.out.println("Booking failed due to conflict (Double Booking prevented): " + seatId);
                createTransaction(transactionId, fanId, matchId, 1, totalAmount, TransactionStatus.FAILED, mechanism, startTime);
                return false;
            }

            // 3. Sinh Ticket
            createTicket(fanId, matchId, seatId, transactionId, totalAmount);
            
            // 4. Sinh Transaction (Thành công)
            createTransaction(transactionId, fanId, matchId, 1, totalAmount, TransactionStatus.SUCCESS, mechanism, startTime);
            success = true;
            
        } catch (Exception e) {
            System.out.println("System error during booking: " + e.getMessage());
            createTransaction(transactionId, fanId, matchId, 1, totalAmount, TransactionStatus.FAILED, mechanism, startTime);
        }
        
        return success;
    }

    /**
     * Hủy vé
     */
    public boolean cancelBooking(String ticketId) {
        Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
        if (!ticketOpt.isPresent() || ticketOpt.get().isCancelled()) {
            System.out.println("Ticket is invalid or already cancelled.");
            return false;
        }
        Ticket ticket = ticketOpt.get();

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
        return true;
    }

    /**
     * Sinh đối tượng Ticket và lưu vào repository
     */
    public void createTicket(String fanId, String matchId, String seatId, String transactionId, long price) {
        String ticketId = "TKT" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String bookedAt = LocalDateTime.now().format(formatter);
        Ticket ticket = new Ticket(ticketId, fanId, seatId, matchId, transactionId, price, bookedAt, TicketStatus.VALID);
        ticketRepository.append(ticket); // Dùng append theo như ghi chú tối ưu của TicketRepository nếu có, hoặc dùng save
    }

    /**
     * Sinh đối tượng BookingTransaction và lưu vào repository
     */
    public void createTransaction(String transactionId, String fanId, String matchId, int numberOfTickets, 
                                  long totalAmount, TransactionStatus status, LockMechanism mechanism, long startTime) {
        String createdAt = LocalDateTime.now().format(formatter);
        long durationMs = System.currentTimeMillis() - startTime;
        BookingTransaction transaction = new BookingTransaction(
                transactionId, fanId, matchId, numberOfTickets, totalAmount, status, mechanism, createdAt, durationMs
        );
        transactionRepository.append(transaction);
    }
}
