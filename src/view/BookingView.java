package view;

import controller.BookingController;
import controller.FanController;
import java.util.Scanner;

public class BookingView {
    private BookingController bookingController;
    private FanController fanController;
    private Scanner scanner;

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

    /**
     * Xử lý hủy vé — public để MainView có thể delegate sang đây
     * sau khi user xem danh sách vé và muốn hủy.
     */
    public void handleCancellation() {
        System.out.println("\n--- TICKET CANCELLATION ---");
        
        if (!fanController.isLoggedIn()) {
            System.out.println("[FAILED] You are not logged in.");
            return;
        }
        String fanId = fanController.getCurrentFan().getFanId();

        System.out.print("Enter Ticket ID to cancel: ");
        String ticketId = scanner.nextLine().trim();

        try {
            boolean result = bookingController.cancelBooking(fanId, ticketId);
            if (result) {
                System.out.println("--> [SUCCESS] Ticket cancelled successfully. Seat is now available.");
            } else {
                System.out.println("--> [FAILED] Ticket not found or already cancelled.");
            }
        } catch (Exception e) {
            System.out.println("--> [FAILED] " + e.getMessage());
        }
    }

}
