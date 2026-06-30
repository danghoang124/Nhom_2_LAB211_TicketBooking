package exception;

/**
 * Ném ra khi cố đặt ghế đã bị người khác đặt hoặc khóa trước đó.
 *
 * <p><b>Khi nào xảy ra:</b>
 * <ul>
 *   <li>Ghế có {@code status = BOOKED} hoặc {@code LOCKED} trong seats.csv.</li>
 *   <li>Đã tồn tại vé VALID với cùng {@code (seatId, matchId)} trong tickets.csv.</li>
 * </ul>
 *
 * <p><b>Nơi ném ra:</b> {@code BookingController.bookTickets()}
 */
public class SeatAlreadyBookedException extends TicketBookingException {

    public SeatAlreadyBookedException(String message) {
        super(message);
    }
}
