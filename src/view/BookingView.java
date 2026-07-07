package view;

import controller.BookingController;
import controller.FanController;
import model.enums.LockMechanism;
import java.util.Scanner;

public class BookingView {
    private final BookingController bookingController;
    private final FanController fanController;
    private final Scanner scanner;

    /**
     * Khởi tạo BookingView.
     *
     * @param bookingController Controller xử lý booking logic.
     * @param fanController     Controller quản lý fan — lấy currentFan từ session.
     * @param scanner           Scanner dùng chung — nhận từ ngoài, không tự tạo mới
     *                          để tránh tạo nhiều Scanner cùng đọc System.in.
     */
    public BookingView(BookingController bookingController, FanController fanController, Scanner scanner) {
        this.bookingController = bookingController;
        this.fanController = fanController;
        this.scanner = scanner;
    }

    public void displayMenu() {
        while (true) {
            System.out.println("\n=== HỆ THỐNG ĐẶT VÉ ===");
            System.out.println("1. Đặt vé");
            System.out.println("2. Hủy vé");
            System.out.println("0. Thoát");
            System.out.print("Chọn chức năng: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    handleBooking();
                    break;
                case "2":
                    handleCancellation();
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Lựa chọn không hợp lệ, vui lòng chọn lại.");
            }
        }
    }

    private void handleBooking() {
        System.out.println("\n--- THỰC HIỆN ĐẶT VÉ ---");

        String fanId = fanController.getCurrentFan().getFanId();

        System.out.print("Nhập mã Trận đấu (Match ID): ");
        String matchId = scanner.nextLine();
        
        System.out.print("Nhập mã Ghế (Seat ID): ");
        String seatId = scanner.nextLine();

        LockMechanism mechanism = chooseMechanism();

        boolean result = bookingController.bookSeat(fanId, matchId, seatId, mechanism);
        
        if (result) {
            System.out.println("-> Đặt vé THÀNH CÔNG! Ghế " + seatId + " đã thuộc về bạn.");
        } else {
            System.out.println("-> Đặt vé THẤT BẠI! Ghế đã được đặt hoặc hệ thống gặp sự cố.");
        }
    }

    /**
     * Xử lý hủy vé — public để MainView có thể delegate sang đây
     * sau khi user xem danh sách vé và muốn hủy.
     */
    public void handleCancellation() {
        System.out.println("\n--- THỰC HIỆN HỦY VÉ ---");
        System.out.print("Nhập mã Vé (Ticket ID) cần hủy: ");
        String ticketId = scanner.nextLine();

        boolean result = bookingController.cancelBooking(ticketId);
        if (result) {
            System.out.println("--> Hủy vé THÀNH CÔNG! Ghế đã được trống.");
        } else {
            System.out.println("--> Hủy vé THẤT BẠI! Không tìm thấy vé hoặc vé đã bị hủy.");
        }
    }

    private LockMechanism chooseMechanism() {
        System.out.println("\nChọn cơ chế đồng bộ:");
        System.out.println("  1. NO_LOCK      — không khóa (dễ Double Booking)");
        System.out.println("  2. SYNCHRONIZED — synchronized block");
        System.out.println("  3. FILE_LOCK    — Java NIO FileLock");
        System.out.println("  4. OPTIMISTIC   — version-based lock");
        System.out.print("Chọn (1-4, mặc định 1): ");
        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "2": return LockMechanism.SYNCHRONIZED;
            case "3": return LockMechanism.FILE_LOCK;
            case "4": return LockMechanism.OPTIMISTIC;
            default:  return LockMechanism.NO_LOCK;
        }
    }
}
