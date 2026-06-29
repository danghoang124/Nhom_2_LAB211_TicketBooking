package view;

import controller.BookingController;
import model.enums.LockMechanism;
import java.util.Scanner;

public class BookingView {
    private final BookingController bookingController;
    private final Scanner scanner;

    public BookingView(BookingController bookingController) {
        this.bookingController = bookingController;
        this.scanner = new Scanner(System.in);
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
        System.out.print("Nhập mã Fan (Fan ID): ");
        String fanId = scanner.nextLine();
        
        System.out.print("Nhập mã Trận đấu (Match ID): ");
        String matchId = scanner.nextLine();
        
        System.out.print("Nhập mã Ghế (Seat ID): ");
        String seatId = scanner.nextLine();
        
        // Trong hệ thống thật, cơ chế Lock có thể được cài đặt sẵn hoặc lựa chọn
        LockMechanism mechanism = LockMechanism.NO_LOCK; // Default cho demo cơ bản

        boolean result = bookingController.bookSeat(fanId, matchId, seatId, mechanism);
        
        if (result) {
            System.out.println("-> Đặt vé THÀNH CÔNG! Ghế " + seatId + " đã thuộc về bạn.");
        } else {
            System.out.println("-> Đặt vé THẤT BẠI! Ghế đã được đặt hoặc hệ thống gặp sự cố.");
        }
    }

    private void handleCancellation() {
        System.out.println("\n--- THỰC HIỆN HỦY VÉ ---");
        System.out.print("Nhập mã Vé (Ticket ID) cần hủy: ");
        String ticketId = scanner.nextLine();

        boolean result = bookingController.cancelBooking(ticketId);
        if (result) {
            System.out.println("-> Hủy vé THÀNH CÔNG! Ghế đã được trống.");
        } else {
            System.out.println("-> Hủy vé THẤT BẠI! Không tìm thấy vé hoặc vé đã bị hủy.");
        }
    }
}
