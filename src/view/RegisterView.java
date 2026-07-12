package view;

import controller.FanController;
import controller.FanController.RegisterResult;
import exception.UserAlreadyExistsException;
import model.entity.Ticket;

import java.util.List;
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
        System.out.println("           ĐĂNG KÝ TÀI KHOẢN MỚI");
        printDivider();
        System.out.println("  (Nhấn 0 + Enter để thoát đăng kí)");
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
            System.out.println("  ✗ Đăng ký thất bại: " + e.getMessage());
            printDivider();
            return false;
        }

        // ── Hiển thị kết quả ─────────────────────────────────────────────
        if (result != null && result.isSuccess()) {
            printDivider();
            System.out.println("  ✓ " + result.getMessage());
            System.out.printf("  ID của bạn: %s%n", result.getFan().getFanId());
            System.out.println("  Bạn đã được tự động đăng nhập vào hệ thống.");
            printDivider();

            fanController.setCurrentFan(result.getFan());
            return true;

        } else {
            printDivider();
            String msg = (result != null) ? result.getMessage() : "Lỗi không xác định.";
            System.out.println("  ✗ Đăng ký thất bại: " + msg);
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
            System.out.print("  Tên đăng nhập (chỉ chữ/số, ≥3 ký tự) : ");
            String input = scanner.nextLine();

            if ("0".equals(input)) {
                return null;
            }

            if (input.trim().isEmpty()) {
                System.out.println("  ✗ Vui lòng nhập tên đăng nhập.!");
                System.out.println();
                continue;
            }

            if (!fanController.isValidUsername(input.trim())) {
                System.out.println("  ✗ Định dạng không hợp lệ! Tên đăng nhập chỉ được chứa chữ cái và chữ số, không dấu cách, không ký tự đặc biệt.");
                System.out.println("    Vui lòng nhập lại.");
                System.out.println();
                continue;
            }

            if (input.trim().length() < 3) {
                System.out.println("  ✗ Tên đăng nhập phải có ít nhất 3 ký tự.");
                System.out.println("    Vui lòng nhập lại.");
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
            System.out.print("  Mật khẩu (ít nhất 6 ký tự)            : ");
            String input = scanner.nextLine();

            if ("0".equals(input)) {
                return null;
            }

            if (input.trim().isEmpty()) {
                System.out.println("  ✗ Vui lòng nhập mật khẩu.!");
                System.out.println();
                continue;
            }

            if (input.length() < 6) {
                System.out.println("  ✗ Mật khẩu phải có ít nhất 6 ký tự.");
                System.out.println("    Vui lòng nhập lại.");
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
            System.out.print("  Họ và tên đầy đủ                      : ");
            String input = scanner.nextLine();

            if ("0".equals(input)) {
                return null;
            }

            if (input.trim().isEmpty()) {
                System.out.println("  ✗ Vui lòng nhập họ và tên.!");
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
            System.out.print("  Email (ví dụ: abc@gmail.com)           : ");
            String input = scanner.nextLine();

            if ("0".equals(input)) {
                return null;
            }

            if (input.trim().isEmpty()) {
                System.out.println("  ✗ Vui lòng nhập email.!");
                System.out.println();
                continue;
            }

            if (!input.trim().contains("@") || !input.trim().contains(".")) {
                System.out.println("  ✗ Email không đúng định dạng (ví dụ: abc@gmail.com).");
                System.out.println("    Vui lòng nhập lại.");
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
            System.out.print("  Số điện thoại (10 số, bắt đầu 0)       : ");
            String input = scanner.nextLine();

            if ("0".equals(input)) {
                return null;
            }

            if (input.trim().isEmpty()) {
                System.out.println("  ✗ Vui lòng nhập số điện thoại.!");
                System.out.println();
                continue;
            }

            if (!input.trim().matches("^0\\d{9}$")) {
                System.out.println("  ✗ Số điện thoại phải có 10 chữ số và bắt đầu bằng 0.");
                System.out.println("    Vui lòng nhập lại.");
                System.out.println();
                continue;
            }

            return input.trim();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // MENU CHÍNH SAU ĐĂNG NHẬP
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Hiển thị menu chính dành cho Fan đã đăng nhập.
     *
     * <p>Menu bao gồm:
     * <ul>
     *   <li>Xem tất cả vé của tôi.</li>
     *   <li>Xem vé còn hiệu lực.</li>
     *   <li>Đăng xuất.</li>
     * </ul>
     *
     * <p>Method này chạy trong vòng lặp đến khi người dùng chọn đăng xuất.
     */
    public void showFanMenu() {
        if (!fanController.isLoggedIn()) {
            System.out.println("  [Lỗi] Bạn chưa đăng nhập.");
            return;
        }

        String fanName = fanController.getCurrentFan().getFullName();

        while (true) {
            System.out.println();
            printDivider();
            System.out.printf("  Xin chào, %s!%n", fanName);
            System.out.println("  MENU CHÍNH");
            printDivider();
            System.out.println("  1. Xem tất cả vé của tôi");
            System.out.println("  2. Xem vé còn hiệu lực (VALID)");
            System.out.println("  0. Đăng xuất");
            printDivider();
            System.out.print("  Chọn chức năng: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    showAllMyTickets();
                    break;
                case "2":
                    showValidTickets();
                    break;
                case "0":
                    handleLogout();
                    return; // thoát khỏi vòng lặp menu
                default:
                    System.out.println("  ✗ Lựa chọn không hợp lệ. Vui lòng chọn lại.");
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HIỂN THỊ DANH SÁCH VÉ
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Hiển thị tất cả vé (cả VALID lẫn CANCELLED) của Fan hiện tại.
     *
     * <p>Gọi {@code fanController.getMyTickets()} để lấy data,
     * rồi format và in ra màn hình.
     */
    private void showAllMyTickets() {
        List<Ticket> tickets = fanController.getMyTickets();
        System.out.println();
        printDivider();
        System.out.println("  TẤT CẢ VÉ CỦA TÔI");
        printDivider();

        if (tickets.isEmpty()) {
            System.out.println("  Bạn chưa có vé nào.");
        } else {
            System.out.printf("  Tổng số: %d vé%n%n", tickets.size());
            printTicketTableHeader();
            for (Ticket t : tickets) {
                printTicketRow(t);
            }
        }
        printDivider();
    }

    /**
     * Hiển thị chỉ các vé VALID (chưa hủy) của Fan hiện tại.
     */
    private void showValidTickets() {
        List<Ticket> tickets = fanController.getMyValidTickets();
        System.out.println();
        printDivider();
        System.out.println("  VÉ CÒN HIỆU LỰC (VALID)");
        printDivider();

        if (tickets.isEmpty()) {
            System.out.println("  Bạn không có vé nào còn hiệu lực.");
        } else {
            System.out.printf("  Tổng số: %d vé còn hiệu lực%n%n", tickets.size());
            printTicketTableHeader();
            for (Ticket t : tickets) {
                printTicketRow(t);
            }
        }
        printDivider();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // XỬ LÝ ĐĂNG XUẤT
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Xử lý đăng xuất: gọi Controller rồi hiển thị thông báo.
     */
    private void handleLogout() {
        String fanName = fanController.getCurrentFan().getFullName();
        fanController.logout();
        System.out.println();
        printDivider();
        System.out.printf("  ✓ Đăng xuất thành công. Tạm biệt, %s!%n", fanName);
        printDivider();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═════════════════════════════════════════════════════════════════════════

    /** In header bảng vé. */
    private void printTicketTableHeader() {
        System.out.printf("  %-14s %-12s %-10s %-20s %-10s%n",
                "TICKET ID", "MATCH ID", "SEAT ID", "ĐẶT LÚC", "TRẠNG THÁI");
        System.out.println("  " + "-".repeat(72));
    }

    /** In một dòng thông tin vé. */
    private void printTicketRow(Ticket t) {
        System.out.printf("  %-14s %-12s %-10s %-20s %-10s%n",
                t.getTicketId(),
                t.getMatchId(),
                t.getSeatId(),
                t.getBookedAt(),
                t.getStatus().name());
    }

    private void printDivider() {
        System.out.println("─".repeat(50));
    }
}
