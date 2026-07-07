package test;

import controller.BookingController;
import model.entity.Seat;
import model.entity.Ticket;
import model.entity.BookingTransaction;
import model.enums.LockMechanism;
import model.enums.SeatStatus;
import model.enums.TicketStatus;
import model.enums.TransactionStatus;
import repository.SeatRepository;
import repository.SectionRepository;
import repository.TicketRepository;
import repository.TransactionRepository;

// Import thư viện chuẩn của JUnit 5
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.Optional;

public class BookingTest {
    private SeatRepository seatRepo;
    private SectionRepository sectionRepo;
    private TicketRepository ticketRepo;
    private TransactionRepository transactionRepo;
    private BookingController controller;

    @BeforeEach
    public void setup() {
        // Khởi tạo các Repository dùng file tạm thời để không ghi đè dữ liệu thật
        seatRepo = new SeatRepository() {
            @Override public String getFilePath() { return "data/test_seats.csv"; }
        };
        // SectionRepository dùng data thật (sections.csv) để tra giá vé đúng.
        // Trong test, giá vé lấy từ sections.csv thật nên test cần chạy ở thư mục
        // gốc project (nơi có data/sections.csv).
        sectionRepo = new SectionRepository();
        ticketRepo = new TicketRepository() {
            @Override public String getFilePath() { return "data/test_tickets.csv"; }
        };
        transactionRepo = new TransactionRepository() {
            @Override public String getFilePath() { return "data/test_transactions.csv"; }
        };

        // Dọn dẹp dữ liệu cũ (nếu có)
        new File("data/test_seats.csv").delete();
        new File("data/test_tickets.csv").delete();
        new File("data/test_transactions.csv").delete();

        controller = new BookingController(seatRepo, sectionRepo, ticketRepo, transactionRepo);
    }

    @Test
    public void testBookingSuccess() {
        // Arrange
        Seat seat = new Seat("SEAT01", "SEC01", "MATCH01", "A", 1, SeatStatus.AVAILABLE, 1);
        seatRepo.save(seat);

        // Act
        boolean result = controller.bookSeat("FAN01", "MATCH01", "SEAT01", LockMechanism.NO_LOCK);

        // Assert
        assertTrue(result, "Đặt vé thành công thì phải trả về true");
        
        Optional<Seat> updatedSeat = seatRepo.findById("SEAT01");
        assertEquals(SeatStatus.BOOKED, updatedSeat.get().getStatus(), "Trạng thái ghế phải chuyển thành BOOKED");
        assertEquals(2, updatedSeat.get().getVersion(), "Version của ghế phải tăng lên 1");
    }

    @Test
    public void testSeatAlreadyBooked() {
        // Arrange
        Seat seat = new Seat("SEAT02", "SEC01", "MATCH01", "A", 2, SeatStatus.BOOKED, 1);
        seatRepo.save(seat);

        // Act & Assert
        assertThrows(exception.SeatAlreadyBookedException.class, () -> {
            controller.bookSeat("FAN01", "MATCH01", "SEAT02", LockMechanism.NO_LOCK);
        });
        assertEquals(1, transactionRepo.count(), "Phải có 1 Transaction FAILED sinh ra");
        BookingTransaction txn = transactionRepo.findAll().get(0);
        assertEquals(TransactionStatus.FAILED, txn.getStatus(), "Transaction ghi nhận FAILED");
    }

    @Test
    public void testCancelBooking() {
        // Arrange
        Seat seat = new Seat("SEAT03", "SEC01", "MATCH01", "A", 3, SeatStatus.BOOKED, 1);
        seatRepo.save(seat);

        controller.createTicket("FAN01", "MATCH01", "SEAT03", "TXN001", 100000);
        Ticket createdTicket = ticketRepo.findAll().get(0);

        // Act
        boolean cancelResult = controller.cancelBooking(createdTicket.getTicketId());

        // Assert
        assertTrue(cancelResult, "Hủy vé hợp lệ phải trả về true");
        assertEquals(TicketStatus.CANCELLED, ticketRepo.findById(createdTicket.getTicketId()).get().getStatus(), "Trạng thái vé là CANCELLED");
        assertEquals(SeatStatus.AVAILABLE, seatRepo.findById("SEAT03").get().getStatus(), "Trạng thái ghế trở lại AVAILABLE");
    }

    @Test
    public void testCreateTicket() {
        // Act
        controller.createTicket("FAN01", "MATCH01", "SEAT04", "TXN002", 150000);

        // Assert
        assertEquals(1, ticketRepo.count(), "Phải sinh ra đúng 1 vé mới");
        Ticket ticket = ticketRepo.findAll().get(0);
        assertEquals("FAN01", ticket.getFanId());
        assertEquals("SEAT04", ticket.getSeatId());
        assertEquals(TicketStatus.VALID, ticket.getStatus());
    }

    @Test
    public void testCreateTransaction() {
        // Act
        controller.createTransaction("TXN003", "FAN01", "MATCH01", 1, 200000, TransactionStatus.SUCCESS, LockMechanism.SYNCHRONIZED, System.currentTimeMillis());

        // Assert
        assertEquals(1, transactionRepo.count(), "Phải sinh ra đúng 1 giao dịch mới");
        BookingTransaction txn = transactionRepo.findById("TXN003").get();
        assertEquals("TXN003", txn.getTransactionId());
        assertTrue(txn.isSuccessful());
    }
}
