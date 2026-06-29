package controller;

import model.entity.BookingTransaction;
import model.entity.Seat;
import model.entity.Ticket;
import model.enums.LockMechanism;
import model.enums.SeatStatus;
import model.enums.TicketStatus;
import model.enums.TransactionStatus;
import repository.SeatRepository;
import repository.TicketRepository;
import repository.TransactionRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

public class BookingController {
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final TransactionRepository transactionRepository;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public BookingController(SeatRepository seatRepository, 
                             TicketRepository ticketRepository, 
                             TransactionRepository transactionRepository) {
        this.seatRepository = seatRepository;
        this.ticketRepository = ticketRepository;
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
                System.out.println("Ghế không tồn tại: " + seatId);
                createTransaction(transactionId, fanId, matchId, 1, totalAmount, TransactionStatus.FAILED, mechanism, startTime);
                return false;
            }
            Seat seat = seatOpt.get();
            
            // Lấy giá vé (tạm thời gán mặc định, thực tế lấy từ SectionRepository)
            totalAmount = 500000;
            
            if (seat.getStatus() == SeatStatus.BOOKED || ticketRepository.existsBySeatAndMatch(seatId, matchId)) {
                System.out.println("Ghế đã được đặt: " + seatId);
                createTransaction(transactionId, fanId, matchId, 1, totalAmount, TransactionStatus.FAILED, mechanism, startTime);
                return false;
            }

            // 2. Đổi trạng thái Seat
            seat.setStatus(SeatStatus.BOOKED);
            seat.setVersion(seat.getVersion() + 1);
            seatRepository.save(seat);

            // 3. Sinh Ticket
            createTicket(fanId, matchId, seatId, transactionId, totalAmount);
            
            // 4. Sinh Transaction (Thành công)
            createTransaction(transactionId, fanId, matchId, 1, totalAmount, TransactionStatus.SUCCESS, mechanism, startTime);
            success = true;
            
        } catch (Exception e) {
            System.out.println("Lỗi hệ thống khi đặt vé: " + e.getMessage());
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
            System.out.println("Vé không hợp lệ hoặc đã bị hủy.");
            return false;
        }
        Ticket ticket = ticketOpt.get();

        // Hủy vé
        ticket.setStatus(TicketStatus.CANCELLED);
        ticketRepository.save(ticket);

        // Phục hồi ghế
        Optional<Seat> seatOpt = seatRepository.findById(ticket.getSeatId());
        if (seatOpt.isPresent()) {
            Seat seat = seatOpt.get();
            seat.setStatus(SeatStatus.AVAILABLE);
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
