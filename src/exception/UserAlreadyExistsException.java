package exception;

/**
 * Ném ra khi đăng ký tài khoản với username hoặc email đã tồn tại.
 *
 * <p><b>Khi nào xảy ra:</b>
 * <ul>
 *   <li>Fan cố đăng ký với username mà người khác đã dùng.</li>
 *   <li>Fan cố đăng ký với email mà người khác đã dùng.</li>
 * </ul>
 *
 * <p><b>Nơi ném ra:</b> {@code FanController.register()}
 */
public class UserAlreadyExistsException extends TicketBookingException {

    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
