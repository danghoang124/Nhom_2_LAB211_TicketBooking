package view;

import controller.FanController;
import controller.FanController.RegisterResult;
import exception.UserAlreadyExistsException;
import java.util.Scanner;

/**
 * View xử lý giao diện đăng ký tài khoản mới và menu chính sau đăng nhập.
 *
 * <p><b>Trách nhiệm:</b>
 * <ul>
 *   <li>Hiển thị form đăng ký (username, password, fullName, email, phone).</li>
 *   <li>Hiển thị menu chính sau khi đăng nhập thành công.</li>
 *   <li>Hiển thị danh sách vé của Fan.</li>
 *   <li>Xử lý đăng xuất.</li>
 * </ul>
 *
 * <p>View chỉ nhận input và hiển thị output —
 * mọi logic đều được delegate sang {@link FanController}.
 */
public class RegisterView {

    private FanController fanController;
    private Scanner       scanner;

    /**
     * Khởi tạo RegisterView.
     *
     * @param fanController Controller xử lý đăng ký và quản lý session.
     * @param scanner       Scanner dùng chung.
     */
    public RegisterView(FanController fanController, Scanner scanner) {
        this.fanController = fanController;
        this.scanner       = scanner;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HIỂN THỊ FORM ĐĂNG KÝ
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Hiển thị form đăng ký và xử lý tương tác.
     *
     * <p>Sau khi nhận input, gọi Controller để validate và lưu.
     * Hiển thị thông báo lỗi cụ thể (từ Controller) nếu thất bại,
     * hoặc thông báo thành công và tự động đăng nhập nếu đăng ký xong.
     *
     * @return {@code true} nếu đăng ký thành công và đã auto-login.
     */
    public boolean show() {
        printDivider();
        System.out.println("           REGISTER NEW ACCOUNT");
        printDivider();
        System.out.println("  (Enter 0 to cancel registration)");
        System.out.println();

        // ── Nhận và validate input từng trường ─────────────────────────────

        // 1. Tên đăng nhập
        String username = readUsername();
        if (username == null) return false;

        // 2. Mật khẩu
        String password = readPassword();
        if (password == null) return false;

        // 3. Họ và tên đầy đủ
        String fullName = readFullName();
        if (fullName == null) return false;

        // 4. Email
        String email = readEmail();
        if (email == null) return false;

        // 5. Số điện thoại
        String phone = readPhone();
        if (phone == null) return false;

        System.out.println();

        // ── Gọi Controller xử lý ─────────────────────────────────────────
        RegisterResult result = null;
        try {
            result = fanController.register(username, password, fullName, email, phone);
        } catch (UserAlreadyExistsException e) {
            printDivider();
            System.out.println("  ✗ Registration failed: " + e.getMessage());
            printDivider();
            return false;
        }

        // ── Hiển thị kết quả ─────────────────────────────────────────────
        if (result != null && result.isSuccess()) {
            printDivider();
            System.out.println("  ✓ " + result.getMessage());
            System.out.printf("  Your ID: %s%n", result.getFan().getFanId());
            System.out.println("  You have been automatically logged in.");
            printDivider();

            fanController.setCurrentFan(result.getFan());
            return true;

        } else {
            printDivider();
            String msg = (result != null) ? result.getMessage() : "Unknown error.";
            System.out.println("  ✗ Registration failed: " + msg);
            printDivider();
            return false;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PHƯƠNG THỨC NHẬP DỮ LIỆU TỪNG TRƯỜNG
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Nhập tên đăng nhập với validation:
     * - Không được bỏ trống
     * - Chỉ chứa chữ cái và chữ số (không dấu, không khoảng trắng, không ký tự đặc biệt)
     * - Ít nhất 3 ký tự
     * - Nhấn 0 để thoát
     */
    private String readUsername() {
        while (true) {
            System.out.print("  Username (alphanumeric, >=3 chars) : ");
            String input = scanner.nextLine();

            if ("0".equals(input)) {
                return null;
            }

            if (input.trim().isEmpty()) {
                System.out.println("  ✗ Please enter a username!");
                System.out.println();
                continue;
            }

            if (!fanController.isValidUsername(input.trim())) {
                System.out.println("  ✗ Invalid format! Username must be alphanumeric, no spaces, no special characters.");
                System.out.println("    Please try again.");
                System.out.println();
                continue;
            }

            if (input.trim().length() < 3) {
                System.out.println("  ✗ Username must be at least 3 characters.");
                System.out.println("    Please try again.");
                System.out.println();
                continue;
            }

            return input.trim();
        }
    }

    /**
     * Nhập mật khẩu với validation:
     * - Không được bỏ trống
     * - Ít nhất 6 ký tự
     * - Nhấn 0 để thoát
     */
    private String readPassword() {
        while (true) {
            System.out.print("  Password (at least 6 characters)      : ");
            String input = scanner.nextLine();

            if ("0".equals(input)) {
                return null;
            }

            if (input.trim().isEmpty()) {
                System.out.println("  ✗ Please enter a password!");
                System.out.println();
                continue;
            }

            if (input.length() < 6) {
                System.out.println("  ✗ Password must be at least 6 characters.");
                System.out.println("    Please try again.");
                System.out.println();
                continue;
            }

            return input;
        }
    }

    /**
     * Nhập họ tên với validation:
     * - Không được bỏ trống
     * - Nhấn 0 để thoát
     */
    private String readFullName() {
        while (true) {
            System.out.print("  Full Name                             : ");
            String input = scanner.nextLine();

            if ("0".equals(input)) {
                return null;
            }

            if (input.trim().isEmpty()) {
                System.out.println("  ✗ Please enter your full name!");
                System.out.println();
                continue;
            }

            return input.trim();
        }
    }

    /**
     * Nhập email với validation:
     * - Không được bỏ trống
     * - Phải đúng định dạng email
     * - Nhấn 0 để thoát
     */
    private String readEmail() {
        while (true) {
            System.out.print("  Email (e.g. abc@gmail.com)             : ");
            String input = scanner.nextLine();

            if ("0".equals(input)) {
                return null;
            }

            if (input.trim().isEmpty()) {
                System.out.println("  ✗ Please enter your email!");
                System.out.println();
                continue;
            }

            if (!input.trim().contains("@") || !input.trim().contains(".")) {
                System.out.println("  ✗ Invalid email format (e.g. abc@gmail.com).");
                System.out.println("    Please try again.");
                System.out.println();
                continue;
            }

            return input.trim();
        }
    }

    /**
     * Nhập số điện thoại với validation:
     * - Không được bỏ trống
     * - Phải 10 chữ số, bắt đầu bằng 0
     * - Nhấn 0 để thoát
     */
    private String readPhone() {
        while (true) {
            System.out.print("  Phone number (10 digits, starts with 0): ");
            String input = scanner.nextLine();

            if ("0".equals(input)) {
                return null;
            }

            if (input.trim().isEmpty()) {
                System.out.println("  ✗ Please enter your phone number!");
                System.out.println();
                continue;
            }

            if (!input.trim().matches("^0\\d{9}$")) {
                System.out.println("  ✗ Phone number must have 10 digits and start with 0.");
                System.out.println("    Please try again.");
                System.out.println();
                continue;
            }

            return input.trim();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═════════════════════════════════════════════════════════════════════════

    private void printDivider() {
        System.out.println("─".repeat(50));
    }
}
