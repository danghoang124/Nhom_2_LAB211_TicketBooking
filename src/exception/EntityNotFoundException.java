package exception;

/**
 * Ném ra khi không tìm thấy thực thể (entity) cần truy vấn trong file CSV.
 *
 * <p><b>Khi nào xảy ra:</b>
 * <ul>
 *   <li>Tìm Section bằng ID không tồn tại.</li>
 *   <li>Tìm Match bằng ID không tồn tại.</li>
 *   <li>Tìm Fan bằng ID không tồn tại.</li>
 * </ul>
 *
 * <p><b>Nơi ném ra:</b> {@code StadiumController.buildSeatMap()},
 * {@code BookingController.bookTickets()}
 */
public class EntityNotFoundException extends TicketBookingException {

    public EntityNotFoundException(String message) {
        super(message);
    }
}
