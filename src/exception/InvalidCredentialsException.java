package exception;

/**
 * Ném ra khi đăng nhập thất bại do sai tên/mật khẩu hoặc tài khoản bị khóa.
 *
 * <p><b>Khi nào xảy ra:</b>
 * <ul>
 *   <li>Username không tồn tại trong hệ thống.</li>
 *   <li>Mật khẩu không khớp với hash đã lưu.</li>
 *   <li>Tài khoản có {@code isActive = false}.</li>
 * </ul>
 *
 * <p><b>Nơi ném ra:</b> {@code FanController.login()}
 */
public class InvalidCredentialsException extends TicketBookingException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
