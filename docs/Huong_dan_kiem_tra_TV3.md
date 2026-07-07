# Hướng Dẫn Kiểm Tra Tiến Trình TV3 (Đăng)

Tài liệu này hướng dẫn cách kiểm tra, chạy thử và xác nhận các tính năng đã thực hiện trong phần việc **TV3 (Đăng)**.

---

## 1. Danh Sách Các Phần Đã Sửa / Tạo Mới

### 🆕 File Tạo Mới:

1. **[AdminController.java](file:///Users/haidang/Nhom_4_LAB211_TicketBooking/src/controller/AdminController.java)**: Xử lý business logic CRUD cho Sân vận động (Stadium), Khán đài (Section), Trận đấu (Match).
2. **[AdminView.java](file:///Users/haidang/Nhom_4_LAB211_TicketBooking/src/view/AdminView.java)**: Màn hình giao diện CLI quản lý dành cho Admin.

### 🔧 File Đã Chỉnh Sửa:

1. **[BookingController.java](file:///Users/haidang/Nhom_4_LAB211_TicketBooking/src/controller/BookingController.java)**:
   - Thêm luồng đặt vé 2 bước (`lockSeat()` -> `confirmBooking()` / `cancelLockedSeat()`).
   - Throw các exception `SeatAlreadyBookedException`, `BookingLimitExceededException`.
2. **[FanController.java](file:///Users/haidang/Nhom_4_LAB211_TicketBooking/src/controller/FanController.java)**:
   - Throw `InvalidCredentialsException` (đăng nhập sai) và `UserAlreadyExistsException` (đăng ký trùng username/email).
3. **[MainView.java](file:///Users/haidang/Nhom_4_LAB211_TicketBooking/src/view/MainView.java)**:
   - Tích hợp **Admin Panel** vào Menu chính (lựa chọn 6).
   - Cập nhật luồng `handleBooking()` sang cơ chế Lock & Confirm 2 bước.
4. **[LoginView.java](file:///Users/haidang/Nhom_4_LAB211_TicketBooking/src/view/LoginView.java) & [RegisterView.java](file:///Users/haidang/Nhom_4_LAB211_TicketBooking/src/view/RegisterView.java)**:
   - Bắt các Exception mới ném từ Controller để hiển thị thông báo lỗi.
5. **[BookingView.java](file:///Users/haidang/Nhom_4_LAB211_TicketBooking/src/view/BookingView.java)**: Nhận `Scanner` qua constructor (Sửa lỗi L5).
6. **[AppContext.java](file:///Users/haidang/Nhom_4_LAB211_TicketBooking/src/main/AppContext.java)**: Đăng ký và tiêm `AdminController`.
7. **[BookingTest.java](file:///Users/haidang/Nhom_4_LAB211_TicketBooking/src/test/BookingTest.java) & [MainViewIntegrationTest.java](file:///Users/haidang/Nhom_4_LAB211_TicketBooking/src/test/MainViewIntegrationTest.java)**:
   - Cập nhật các khẳng định (assertions) kiểm tra Exception thay vì kiểm tra giá trị trả về `false`.

---

## 2. Hướng Dẫn Chạy Code Kiểm Tra Thủ Công

### Bước 2.1: Biên dịch chương trình

Mở Terminal tại thư mục gốc của dự án (`/Users/haidang/Nhom_4_LAB211_TicketBooking`) và chạy lệnh sau để biên dịch toàn bộ source code (bao gồm cả thư viện JUnit):

```bash
javac -encoding UTF-8 -cp "src:src/lib/junit-platform-console-standalone-1.10.2.jar" -d out $(find src -name "*.java")
```

### Bước 2.2: Chạy ứng dụng CLI

Chạy lệnh sau để khởi động menu chính của hệ thống:

```bash
java -cp out main.Main
```

### Bước 2.3: Các kịch bản kiểm tra thủ công

#### 📝 Kịch bản 1: Kiểm tra Exception Đăng nhập & Đăng ký

1. Chọn `4. Enter Ticket System (Login / Book / Report)`.
2. Chọn `1. Login`. Nhập sai tài khoản hoặc mật khẩu $\rightarrow$ Hệ thống phải báo lỗi rõ ràng (đã bắt từ `InvalidCredentialsException`).
3. Chọn `2. Register new account`. Nhập username đã tồn tại trong hệ thống (ví dụ: `fan1` hoặc tài khoản bạn đã tạo) $\rightarrow$ Hệ thống phải báo lỗi trùng lặp (đã bắt từ `UserAlreadyExistsException`).

#### 🎫 Kịch bản 2: Kiểm tra Luồng Đặt vé 2 bước (Lock & Confirm)

1. Đăng nhập vào tài khoản Fan hợp lệ.
2. Chọn `3. Book a ticket`.
3. Nhập ID Trận đấu và ID Ghế trống (ví dụ: trận mở bán `MATCH001`, ghế `SEAT000003`).
4. Hệ thống sẽ hiển thị bảng xác nhận đặt vé:
   ```
   ======================================
          BOOKING CONFIRMATION         
   ======================================
     Fan    : Nguyễn Văn A
     Match  : MATCH001
     Seat   : SEAT000003  [LOCKED - Held for you]
     Price  : 500,000 VND
   ======================================
   Confirm booking? (y/n):
   ```
5. **Thử nghiệm Cancel**: Nhập `n` $\rightarrow$ Ghế được giải phóng (`cancelLockedSeat`). Kiểm tra lại sơ đồ ghế (chọn `2. View seat map by match`), ghế này phải có ký hiệu `[O]` (AVAILABLE).
6. **Thử nghiệm Confirm**: Tiến hành đặt lại ghế đó, nhập `y` $\rightarrow$ Ghế chuyển sang `BOOKED`. Kiểm tra lại sơ đồ ghế, ghế này phải có ký hiệu `[X]` (BOOKED).

#### 🛡️ Kịch bản 3: Kiểm tra Admin Panel (CRUD)

1. Sau khi đăng nhập với bất kỳ tài khoản Fan nào, tại menu chính chọn:
   `6. Admin Panel (CRUD)`
2. Màn hình quản trị Admin hiển thị:
   ```
   ╔══════════════════════════════════════╗
   ║          ADMIN MANAGEMENT PANEL      ║
   ╠══════════════════════════════════════╣
   ║  1. Stadium Management               ║
   ║  2. Section Management               ║
   ║  3. Match Management                 ║
   ║  0. Back to Main Menu                ║
   ╚══════════════════════════════════════╝
   ```
3. Lần lượt kiểm tra các chức năng:
   - **Stadium Management**: Xem danh sách (`1`), Tạo sân mới (`3`), Sửa sân (`4`), Xóa sân (`5`).
   - **Match Management**: Xem danh sách trận đấu (`1`), Tạo trận đấu mới (`3`), Sửa thông tin trận (`4`), Thay đổi trạng thái trận đấu (`5`) (ví dụ từ `SCHEDULED` sang `ONGOING` hoặc `COMPLETED`).
4. Dữ liệu sau khi thêm/sửa/xóa sẽ tự động cập nhật vào các file CSV tương ứng trong thư mục `data/` (`stadiums.csv`, `matches.csv`, `sections.csv`).

---

## 3. Hướng Dẫn Chạy Kiểm Thử Tự Động (JUnit Tests)

Hệ thống đi kèm bộ kiểm thử tự động để bảo đảm các thay đổi không phá vỡ logic cũ. Để chạy toàn bộ 47 bài test, sử dụng lệnh sau:

```bash
java -jar src/lib/junit-platform-console-standalone-1.10.2.jar -cp out --select-package test
```

### Kết quả mong đợi:

Tất cả các bài test phải hiển thị màu xanh và báo cáo kết quả:

```
[        47 tests found           ]
[         0 tests skipped         ]
[        47 tests started         ]
[         0 tests aborted         ]
[        47 tests successful      ]
[         0 tests failed          ]
```

Các bài test đặc thù cho Exception như `testSeatAlreadyBooked`, `testRegisterDuplicateUsername`, `testLoginWrongPassword`, `testDoubleBookingFail` đều phải pass.
