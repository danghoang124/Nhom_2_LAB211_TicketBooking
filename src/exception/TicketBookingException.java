package exception;

/**
 * Exception gốc (base) cho toàn bộ hệ thống đặt vé sân vận động.
 *
 * <p>Mọi custom exception khác trong hệ thống đều kế thừa lớp này,
 * giúp View có thể bắt gọn bằng một catch duy nhất khi cần.
 *
 * <p><b>Ví dụ sử dụng trong View:</b>
 * <pre>
 *   try {
 *       controller.bookTickets(...);
 *   } catch (SeatAlreadyBookedException e) {
 *       System.out.println("Ghế đã được đặt: " + e.getMessage());
 *   } catch (TicketBookingException e) {
 *       System.out.println("Lỗi hệ thống: " + e.getMessage());
 *   }
 * </pre>
 */
public class TicketBookingException extends RuntimeException {

    public TicketBookingException(String message) {
        super(message);
    }

    public TicketBookingException(String message, Throwable cause) {
        super(message, cause);
    }
}
