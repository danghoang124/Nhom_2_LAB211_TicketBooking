package view;

import controller.BookingController;
import controller.FanController;
import model.enums.LockMechanism;
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

    public void displayMenu() {
        while (true) {
            System.out.println("\n=== TICKET BOOKING SYSTEM ===");
            System.out.println("1. Book a ticket");
            System.out.println("2. Cancel a ticket");
            System.out.println("0. Exit");
            System.out.print("Select an option: ");
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
                    System.out.println("Invalid option, please try again.");
            }
        }
    }

    private void handleBooking() {
        System.out.println("\n--- BOOK A TICKET ---");

        // Lấy fanId từ session hiện tại — không prompt để tránh giả mạo fan khác
        if (!fanController.isLoggedIn()) {
            System.out.println("[FAILED] You are not logged in.");
            return;
        }
        String fanId = fanController.getCurrentFan().getFanId();

        System.out.print("Enter Match ID: ");
        String matchId = scanner.nextLine().trim();

        System.out.print("Enter Seat ID: ");
        String seatId = scanner.nextLine().trim();

        LockMechanism mechanism = chooseMechanism();

        boolean result = bookingController.bookSeat(fanId, matchId, seatId, mechanism);

        if (result) {
            System.out.println("-> [SUCCESS] Ticket booked! Seat " + seatId + " is yours.");
        } else {
            System.out.println("-> [FAILED] Seat already booked or system error.");
        }
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

    private LockMechanism chooseMechanism() {
        System.out.println("\nChoose synchronization mechanism:");
        System.out.println("  1. NO_LOCK      - No lock (susceptible to double booking)");
        System.out.println("  2. SYNCHRONIZED - Synchronized block");
        System.out.println("  3. FILE_LOCK    - Java NIO FileLock");
        System.out.println("  4. OPTIMISTIC   - Version-based lock");
        System.out.print("Select (1-4, default 1): ");
        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "2": return LockMechanism.SYNCHRONIZED;
            case "3": return LockMechanism.FILE_LOCK;
            case "4": return LockMechanism.OPTIMISTIC;
            default:  return LockMechanism.NO_LOCK;
        }
    }
}
