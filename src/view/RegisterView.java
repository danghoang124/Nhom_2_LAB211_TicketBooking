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

    private final FanController fanController;
    private final Scanner       scanner;

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
        System.out.println("  (Nhấn Enter để bỏ trống — sẽ hiện thông báo lỗi)");
        System.out.println();

        // ── Nhận input từ người dùng ──────────────────────────────────────
        System.out.print("  Tên đăng nhập (ít nhất 3 ký tự)  : ");
        String username = scanner.nextLine();

        System.out.print("  Mật khẩu (ít nhất 6 ký tự)       : ");
        String password = scanner.nextLine();

        System.out.print("  Họ và tên đầy đủ                  : ");
        String fullName = scanner.nextLine();

        System.out.print("  Email (ví dụ: abc@gmail.com)      : ");
        String email = scanner.nextLine();

        System.out.print("  Số điện thoại (10 số, bắt đầu 0)  : ");
        String phone = scanner.nextLine();

        System.out.println();

        // ── Gọi Controller xử lý ─────────────────────────────────────────
        RegisterResult result = null;
        try {
            result = fanController.register(username, password, fullName, email, phone);
        } catch (UserAlreadyExistsException e) {
            // Username hoặc email trùng — hiển thị thông báo rõ ràng
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

            // Auto-login sau khi đăng ký thành công.
            // KHÔNG gọi fanController.login(username, password) vì login() sẽ
            // sha256(password) một lần nữa → sha256(sha256(password)) không khớp
            // với sha256(password) đã lưu trong CSV → luôn thất bại (double-hash bug).
            // Thay vào đó: set currentFan trực tiếp từ Fan vừa tạo.
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
