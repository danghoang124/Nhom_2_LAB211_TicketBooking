package view;

import controller.FanController;
import exception.InvalidCredentialsException;
import model.entity.Fan;

import java.util.Scanner;

/**
 * View xử lý giao diện đăng nhập.
 *
 * <p><b>Trách nhiệm của View:</b>
 * <ul>
 *   <li>Hiển thị form đăng nhập ra màn hình console.</li>
 *   <li>Nhận dữ liệu từ người dùng (username, password).</li>
 *   <li>Gọi Controller để xử lý logic.</li>
 *   <li>Hiển thị kết quả (thành công / thất bại).</li>
 * </ul>
 *
 * <p><b>View KHÔNG được:</b>
 * <ul>
 *   <li>Xử lý business logic (như hash password, validate email...).</li>
 *   <li>Đọc ghi file CSV trực tiếp.</li>
 *   <li>Gọi Repository trực tiếp.</li>
 * </ul>
 */
public class LoginView {

    private final FanController fanController;
    private final Scanner       scanner;

    /**
     * Khởi tạo LoginView.
     *
     * @param fanController Controller xử lý logic đăng nhập.
     * @param scanner       Scanner dùng chung để đọc input (tránh tạo nhiều Scanner).
     */
    public LoginView(FanController fanController, Scanner scanner) {
        this.fanController = fanController;
        this.scanner       = scanner;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HIỂN THỊ FORM ĐĂNG NHẬP
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Hiển thị màn hình đăng nhập và xử lý tương tác.
     *
     * <p>Cho phép người dùng thử tối đa {@code MAX_ATTEMPTS} lần.
     * Sau khi đăng nhập thành công, session được lưu trong Controller.
     *
     * @return {@code true} nếu đăng nhập thành công.
     */
    public boolean show() {
        final int maxAttempts = FanController.MAX_LOGIN_ATTEMPTS;

        printDivider();
        System.out.println("           ĐĂNG NHẬP HỆ THỐNG");
        printDivider();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            System.out.printf("%n[Lần thử %d/%d]%n", attempt, maxAttempts);

            // ── Nhận input từ người dùng ──────────────────────────────────
            System.out.print("  Tên đăng nhập : ");
            String username = scanner.nextLine();

            System.out.print("  Mật khẩu      : ");
            String password = scanner.nextLine();

            // ── Gọi Controller xử lý ─────────────────────────────────────────────
            boolean success = false;
            String errorMessage = null;
            try {
                success = fanController.login(username, password);
            } catch (InvalidCredentialsException e) {
                errorMessage = e.getMessage();
            }

            // ── Hiển thị kết quả ─────────────────────────────────────────────
            if (success) {
                Fan fan = fanController.getCurrentFan();
                System.out.println();
                printDivider();
                System.out.printf("  ✓ Đăng nhập thành công!%n");
                System.out.printf("  Xin chào, %s (ID: %s)%n", fan.getFullName(), fan.getFanId());
                printDivider();
                return true;
            } else {
                String msg = (errorMessage != null) ? errorMessage
                        : "Tên đăng nhập hoặc mật khẩu không đúng.";
                System.out.println("  ✗ " + msg);
                if (attempt < maxAttempts) {
                    System.out.printf("  Còn %d lần thử.%n", maxAttempts - attempt);
                }
            }
        }

        // Hết lần thử
        System.out.println();
        System.out.println("  ✗ Đăng nhập thất bại sau " + maxAttempts + " lần thử.");
        System.out.println("  Vui lòng thử lại sau hoặc đăng ký tài khoản mới.");
        printDivider();
        return false;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═════════════════════════════════════════════════════════════════════════

    private void printDivider() {
        System.out.println("─".repeat(50));
    }
}
