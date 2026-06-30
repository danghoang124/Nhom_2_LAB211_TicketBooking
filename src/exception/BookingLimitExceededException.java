package exception;

/**
 * Ném ra khi số vé trong một giao dịch không nằm trong khoảng hợp lệ [1, 4].
 *
 * <p><b>Khi nào xảy ra:</b>
 * <ul>
 *   <li>Fan chọn 0 ghế (danh sách ghế rỗng).</li>
 *   <li>Fan chọn hơn 4 ghế trong một lần đặt.</li>
 * </ul>
 *
 * <p><b>Nơi ném ra:</b> {@code BookingController.bookTickets()}
 */
public class BookingLimitExceededException extends TicketBookingException {

    public BookingLimitExceededException(String message) {
        super(message);
    }
}
