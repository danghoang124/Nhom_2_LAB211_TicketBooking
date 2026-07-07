# DoItNowFinish — Danh sách việc cần làm để FINISH

> Tổng hợp từ đối chiếu toàn bộ code + docs + diagram + đề tài PDF.
> Cập nhật lần cuối: 2026-07-07

---

## MỤC LỤC

1. [🔴 CRITICAL — Lỗi mất điểm trực tiếp](#1--critical--lỗi-mất-điểm-trực-tiếp)
2. [🟠 MVC VIOLATIONS — Vi phạm kiến trúc MVC](#2--mvc-violations--vi-phạm-kiến-trúc-mvc)
3. [🔵 DEAD CODE — Code không được dùng](#3--dead-code--code-không-được-dùng)
4. [🟡 DATA INCONSISTENCY — Dữ liệu sai lệch](#4--data-inconsistency--dữ-liệu-sai-lệch-giữa-code-và-csv)
5. [🟠 HIGH — Hoàn thiện tính năng](#5--high--hoàn-thiện-tính-năng)
6. [🟢 LOW — Chất lượng code & tổ chức](#6--low--chất-lượng-code--tổ-chức)
7. [📄 DOCUMENTATION & SUBMISSION — Tài liệu và nộp bài](#7--documentation--submission--tài-liệu-và-nộp-bài)

---

## 1. 🔴 CRITICAL — Lỗi mất điểm trực tiếp

### 1.1. MVC Violation: `StadiumController` + `SeatMapView` hoàn toàn DEAD

**File:** `src/controller/StadiumController.java`, `src/view/SeatMapView.java`
**Mức độ:** Mất điểm Decomposition (20%) + MVC violation trừ -5%

**Vấn đề:**
- `StadiumController` không được tạo trong `AppContext.java`
- `SeatMapView` không được gọi từ bất kỳ View nào trong production code
- `MainView.showSeatMap()` (dòng 189-253) tự truy cập trực tiếp `matchRepository`, `sectionRepository`, `seatRepository` thay vì qua `StadiumController`
- `StadiumController.buildSeatMap()` có logic xây mảng 2 chiều Seat[][] nhưng không ai dùng
- `SeatMapView.displaySeatMap()` có render ASCII đẹp nhưng không ai gọi
- Cả 2 chỉ được dùng trong `ControllerTest.java`

**Hậu quả:**
1. View (MainView) gọi Repository trực tiếp → **vi phạm MVC nghiêm trọng** (trừ -5% mỗi lần phát hiện)
2. Mất điểm phần Abstraction vì class diagram có StadiumController nhưng code không dùng
3. Lãng phí code đã viết (SeatMapView render đẹp nhưng không dùng)

**Sửa:**
1. Thêm `StadiumController` vào `AppContext.java` (tạo instance + getter)
2. `MainView.showSeatMap()` phải gọi `StadiumController.buildSeatMap()` + `SeatMapView.displaySeatMap()` thay vì tự xử lý
3. `MainView.showMatchList()` nên gọi `stadiumController.getMatches()` thay vì `matchRepository.findAll()`

### 1.2. MVC Violation: `BookingController` tự in ra console

**File:** `src/controller/BookingController.java`
**Mức độ:** Trừ -5%/lần = 5 lần = -25% tiềm năng

**Vấn đề:**
`BookingController` có 5 chỗ gọi `System.out.println()`:
- Dòng 246: `"Seat not found: " + seatId`
- Dòng 288: `"Booking failed due to conflict..."`
- Dòng 303: `"System error during booking..."`
- Dòng 339: `"[WARN] " + e.getMessage()`
- Dòng 358: `"Ticket is invalid or already cancelled."`

Theo MVC, Controller KHÔNG được tự in ra màn hình. Việc này thuộc trách nhiệm của View.

**Sửa:**
- Xóa toàn bộ `System.out.println()` khỏi `BookingController`
- Trả về `boolean`/`String message` để View xử lý hiển thị
- Hoặc throw exception để View bắt và hiển thị

### 1.3. MVC Violation: `MainView` gọi Repository trực tiếp

**File:** `src/view/MainView.java` (dòng 163, 195, 200-211, 218, 262, 269-272, 288-296, 311-312)
**Mức độ:** Trừ -5%/lần

**Vấn đề:**
`MainView` có ít nhất 4 chỗ truy cập Repository trực tiếp (không qua Controller):
- `matchRepository.findAll()` (dòng 163) → phải qua `StadiumController.getMatches()`
- `sectionRepository.findAll()` (dòng 200) → phải qua `StadiumController.getSections()`
- `seatRepository.findBySectionAndMatch()` (dòng 203) → phải qua `StadiumController`
- `matchRepository.findByStatus()` (dòng 262) → phải qua `StadiumController`
- `seatRepository.countAvailable()` (dòng 270) → phải qua `StadiumController`

**Sửa:**
- Inject `StadiumController` vào `MainView`
- Thay tất cả các lời gọi Repository trực tiếp bằng lời gọi qua Controller

### 1.4. `DataGenerator` sinh giá vé STANDING/ECONOMY_LOWER sai

**File:** `src/generator/DataGenerator.java` (dòng 75)
**Mức độ:** Nếu chạy lại DataGenerator sẽ ghi đè sections.csv với giá sai

**Vấn đề:**
```java
private static final long[] SECTION_PRICES = { 500_000L, 200_000L, 80_000L, 100_000L };
// Chỉ mục:               VIP        STANDARD   STANDING   ECONOMY_LOWER
```

DataGenerator sinh: STANDING=80k, ECONOMY_LOWER=100k

Nhưng `data/sections.csv` và `docs/csv_schema.md` đều ghi:
- STANDING = 100,000 VND
- ECONOMY_LOWER = 80,000 VND

**Sửa:** Đổi dòng 75 thành:
```java
private static final long[] SECTION_PRICES = { 500_000L, 200_000L, 100_000L, 80_000L };
```

### 1.5. Javadoc/SectionType.java ghi sai giá vé

**File:** `src/model/enums/SectionType.java` (dòng 24-25)
**Mức độ:** Gây nhầm lẫn, documentation sai với thực tế

**Vấn đề:**
Javadoc ghi: STANDING=80k, ECONOMY_LOWER=100k
Thực tế data: STANDING=100k, ECONOMY_LOWER=80k

**Sửa:** Section type comment:
```java
//   <li>{@code STANDING}      — khu đứng (100,000 VND).</li>
//   <li>{@code ECONOMY_LOWER} — khu phổ thông thấp (80,000 VND).</li>
```

### 1.6. StadiumController.java comment sai giá vé

**File:** `src/controller/StadiumController.java` (dòng 97-100)
**Mức độ:** Documentation sai với thực tế

**Vấn đề:**
```java
//   SEC003 - STANDING  : ... ( 80,000đ/vé)
//   SEC004 - ECONOMY   : ... (100,000đ/vé)
```

**Sửa:** Đổi thành 100k (STANDING) và 80k (ECONOMY_LOWER).

### 1.7. `SeatRepository.updateStatusOptimistic` là `synchronized` — phá vỡ mục đích OPTIMISTIC

**File:** `src/repository/SeatRepository.java` (dòng 153)
**Mức độ:** 🔴 CRITICAL — cơ chế OPTIMISTIC hoạt động y hệt SYNCHRONIZED

**Vấn đề:**
```java
public synchronized boolean updateStatusOptimistic(String seatId, SeatStatus newStatus, int expectedVersion) {
```
Từ khóa `synchronized` khiến chỉ 1 thread được chạy method này tại một thời điểm. Kết quả:
- Version check (dòng 161-163) **KHÔNG BAO GIỜ** xảy ra conflict vì các threads đã xếp hàng chờ nhau
- OPTIMISTIC và SYNCHRONIZED cho kết quả hiệu năng **giống hệt nhau** (chỉ khác overhead)
- Mất điểm thiết kế vì đề tài yêu cầu 4 cơ chế riêng biệt

**Sửa:**
Bỏ `synchronized`, implement retry loop khi conflict:
```java
public boolean updateStatusOptimistic(String seatId, SeatStatus newStatus, int expectedVersion) {
    List<Seat> all = findAll();              // không block thread khác
    // ... kiểm tra version như hiện tại ...
    // Nếu conflict → trả về false để caller retry
}
```
Đồng thời thêm retry ở `BookingController.confirmBooking` khi gặp OPTIMISTIC conflict.

---

## 2. 🟠 MVC VIOLATIONS — Vi phạm kiến trúc MVC

### 2.1. `LoginView` có logic đếm số lần thử

**File:** `src/view/LoginView.java` (dòng 56, 62, 94-96)
**Mức:** Nhẹ, nhưng vi phạm MVC nếu giám khảo khó tính

**Vấn đề:**
`MAX_ATTEMPTS = 3` và vòng lặp thử lại được đặt trong View. Theo MVC nghiêm ngặt, Controller nên quyết định policy (số lần thử) và View chỉ hiển thị.

**Sửa:** Chuyển `MAX_ATTEMPTS` logic vào `FanController`.

### 2.2. `SimulatorController` không có trong AppContext

**File:** `src/main/AppContext.java`, `src/main/Main.java` (dòng 63-64)
**Mức:** Nhẹ

**Vấn đề:**
`SimulatorController` được tạo trực tiếp trong `Main.java` (dòng 64) thay vì qua `AppContext`. Gây khó khăn cho việc kiểm thử và bảo trì.

**Sửa:** Thêm `SimulatorController` vào `AppContext`.

### 2.3. `SimulatorView` gọi Repository trực tiếp (4 chỗ)

**File:** `src/view/SimulatorView.java` (dòng 51, 115, 126, 133)
**Mức:** 🔴 CRITICAL — MVC violation

**Vấn đề:**
`SimulatorView` gọi Repository trực tiếp thay vì qua Controller:
- Dòng 51: `seatRepository.findAvailableByMatch(matchId)` — lấy danh sách ghế
- Dòng 115: `seatRepository.findAll()` + `seatRepository.saveAll()` — reset ghế
- Dòng 126: `ticketRepository.findByCondition()` + `ticketRepository.saveAll()` — xóa tickets
- Dòng 133: `transactionRepository.findByCondition()` + `transactionRepository.saveAll()` — xóa transactions

Đây là **MVC violation thứ 3** (sau MainView và BookingController).

**Sửa:**
1. Thêm method vào `SimulatorController`: `resetData()`, `getAvailableSeats()`, `clearTickets()`, `clearTransactions()`
2. `SimulatorView` chỉ gọi `SimulatorController`, không gọi Repository trực tiếp

---

## 3. 🔵 DEAD CODE — Code không được dùng

### 3.1. `StadiumController` — Dead code toàn bộ

Đã phân tích ở mục 1.1.
**File:** `src/controller/StadiumController.java`

### 3.2. `SeatMapView` — Dead code toàn bộ

Đã phân tích ở mục 1.1.
**File:** `src/view/SeatMapView.java`

### 3.3. `TransactionStatus.PARTIAL` không bao giờ được set

**File:** `src/model/enums/TransactionStatus.java`
**File:** `src/controller/BookingController.java`

**Vấn đề:**
- `PARTIAL` được định nghĩa trong enum
- `bookMultipleSeats()` có thể trả về 1-3 (partial success) nhưng không set `PARTIAL`
- Chỉ có test code dùng `PARTIAL`

**Sửa:**
Trong `bookMultipleSeats()`: nếu successCount > 0 && successCount < seatIds.size() → set status = PARTIAL

### 3.4. `DisplaySystemInfo()` tạo Repository riêng

**File:** `src/main/Main.java` (dòng 89-93)

**Vấn đề:**
Tạo repository instance riêng thay vì dùng `AppContext`. Gây duplicate code.

**Sửa:** Dùng `AppContext` để lấy repository instances.

---

## 4. 🟡 DATA INCONSISTENCY — Dữ liệu sai lệch giữa code và CSV

### 4.1. Giá vé (đã phân tích ở 1.4)

### 4.2. `SimulatorView.resetForNextRun()` bypass `updateStatus()`

**File:** `src/view/SimulatorView.java` (dòng 118-119)

**Vấn đề:**
```java
seat.setStatus(SeatStatus.AVAILABLE);
seat.setVersion(0);
```
Dùng setter thay vì `updateStatus()`, bypass cơ chế tăng version.

**Sửa:**
```java
seat.updateStatus(SeatStatus.AVAILABLE);
seat.setVersion(0); // vẫn cần reset version về 0 cho lần chạy mới
```
Nhưng cần comment giải thích lý do `setVersion(0)`.

### 4.3. `resetForNextRun()` không reset ghế LOCKED

**File:** `src/view/SimulatorView.java` (dòng 112-141)
**Mức:** 🔴 CRITICAL — state sai giữa các lần chạy

**Vấn đề:**
`resetForNextRun()` chỉ reset duy nhất ghế `seatId`. Nếu chạy 100 threads trên ghế A:
- Một số thread LOCKED ghế A (thành công) nhưng chưa kịp BOOKED
- resetForNextRun() chỉ reset ghế A → reset đúng ghế đó
- Nhưng các ghế KHÁC không liên quan (ghế B, C) vẫn giữ nguyên AVAILABLE → không sao
- Vấn đề thực sự: nếu lần chạy trước để ghế A ở trạng thái LOCKED (thread crash), lần chạy sau không reset đúng → ghế A bị LOCKED vĩnh viễn

**Sửa:**
Reset tất cả ghế của match về AVAILABLE thay vì chỉ 1 ghế:
```java
List<Seat> allMatchSeats = seatRepository.findByMatch(matchId);
for (Seat seat : allMatchSeats) {
    seat.setStatus(SeatStatus.AVAILABLE);
    seat.setVersion(0);
}
seatRepository.saveAll(allMatchSeats);
```

### 4.4. MainView dùng `getMyValidTickets()` còn ReportView dùng `getAllTicketsByFan()`

**File:** `src/view/MainView.java` (dòng 407), `src/view/ReportView.java` (dòng 63)
**Mức:** Trung bình

**Vấn đề:**
- MainView.showMyTickets(): chỉ hiển thị vé VALID
- ReportView.displayMyTickets(): hiển thị ALL tickets (VALID + CANCELLED)
- Cả 2 gọi cùng tên "Xem vé" nhưng cho kết quả khác nhau

**Sửa:**
Thống nhất hành vi. Gợi ý:
- MainView.showMyTickets() → đổi tên "My Valid Tickets" và giữ logic hiện tại
- Hoặc MainView.showMyTickets() → đổi thành `getAllTicketsByFan()` cho nhất quán với ReportView

### 4.5. Scanner được tạo 2 lần

**File:** `src/main/Main.java` (dòng 12), `src/view/MainView.java` (dòng 44)

**Vấn đề:**
Cả `Main` và `MainView` đều tạo `new Scanner(System.in)`. Hai Scanner trên cùng một InputStream có thể gây trôi dòng (skipped input).

**Sửa:**
Tạo Scanner ở `Main` và truyền vào `MainView` qua constructor.

### 4.6. `BookingView` không dùng current fan + hardcoded `NO_LOCK`

**File:** `src/view/BookingView.java` (dòng 49-61)

**Vấn đề:**
```java
System.out.print("Nhập mã Fan (Fan ID): ");
String fanId = scanner.nextLine();           // TỰ NHẬP fanId — có thể nhập lung tung
// ...
LockMechanism mechanism = LockMechanism.NO_LOCK; // Hardcoded, không dùng mechanism config
```

Hậu quả:
- Bất kỳ ai cũng có thể đặt vé hộ người khác (gõ fanId bất kỳ)
- Luôn dùng NO_LOCK, bỏ qua 3 cơ chế đồng bộ còn lại

**Sửa:**
```java
// Lấy fanId từ session hiện tại
String fanId = FanController.getCurrentFan().getFanId();
// Cho user chọn mechanism hoặc dùng mặc định từ config
```

### 4.7. `bookMultipleSeats()` tạo N transaction riêng thay vì 1 transaction

**File:** `src/controller/BookingController.java` (dòng 320-343)

**Vấn đề:**
Gọi `bookSeat()` trong vòng lặp for, mỗi lần một ghế → mỗi ghế tạo 1 Transaction riêng.
Đề tài yêu cầu: "tối đa 4 vé/lần giao dịch" → 1 transaction chứa 4 vé.

Hậu quả:
- Không thể biết 4 vé đó thuộc cùng 1 lần đặt
- Không thể hủy 1 lúc 4 vé
- TransactionStatus.PARTIAL không bao giờ được dùng

**Sửa:**
Tạo 1 transaction duy nhất, gom cả 4 vé vào đó. Nếu 4 vé mà chỉ đặt được 2 → PARTIAL.

---

## 5. 🟠 HIGH — Hoàn thiện tính năng

### 5.1. Thiếu 4 Flowchart bắt buộc

**File:** `docs/flowcharts/` — **KHÔNG TỒN TẠI**
**Mức:** Mất 15% Abstraction (Rubric tiêu chí 2)

**Yêu cầu từ đề tài (trang 5):**
Vẽ bằng draw.io / PlantUML, lưu vào `docs/flowcharts/`:
- 4.1 Luồng Đặt Vé (Booking Flow)
- 4.2 Luồng Ngăn chặn Double Booking (Synchronization Flow)
- 4.3 Luồng Simulator Tool
- 4.4 Luồng Generate CSV Data (10.000 dòng)

### 5.2. Thiếu báo cáo Word + Slide

**File:** `docs/report.docx`, `docs/slide.pptx` — **KHÔNG TỒN TẠI**
**Mức:** Mất điểm toàn bộ phần nộp sản phẩm

**Yêu cầu từ đề tài (trang 5, 9, 10):**
- Báo cáo Word: bối cảnh, thiết kế, kết quả thực nghiệm (1000 threads × 4 mechanisms), kết luận
- Slide: ≥ 15 trang
- Biểu đồ so sánh throughput vs double booking rate

### 5.3. Admin Panel có thể vào mà không cần admin thật sự

**File:** `src/view/MainView.java` (dòng 105-106)

**Vấn đề:**
```java
boolean isAdmin = currentFan.getUsername() != null &&
    currentFan.getUsername().toLowerCase().startsWith("admin");
```
Bất kỳ user nào có username bắt đầu bằng "admin" đều vào được Admin Panel. Cần cơ chế phân quyền rõ ràng hơn (ví dụ: role field trong Fan entity, hoặc admin cứng).

### 5.4. Simulator có thể crash vì không tìm thấy ghế

**File:** `src/view/SimulatorView.java` (dòng 51-55)

**Vấn đề:**
Nếu không tìm thấy available seats (sau khi đã chạy các lần trước mà không reset), Simulator báo lỗi và thoát. Nên tự động reset hết seats về AVAILABLE khi bắt đầu.

### 5.5. `bookMultipleSeats()` không dùng lock mechanism đúng cách

**File:** `src/controller/BookingController.java` (dòng 332-343)

**Vấn đề:**
Gọi `bookSeat()` trong vòng lặp for, mỗi lần một ghế. Nếu đặt 4 ghế, ghế 1 thành công, ghế 2 thất bại → trạng thái không nhất quán (1 ghế booked, 3 ghế không).

Đề tài yêu cầu tối đa 4 vé/lần giao dịch, nhưng luồng đặt nhiều ghế không có transaction rollback.

---

## 📊 TUẦN 8 — SIMULATOR: Các lỗi về số liệu và hiệu năng

### 5.6. Công thức TPS trong Simulator bị sai

**File:** `src/controller/SimulatorController.java` (dòng 81)
**Mức:** Cao — sai số liệu báo cáo T9

**Vấn đề:**
```java
result.tps = timeMs > 0 ? (double) (numThreads) / timeMs * 1000 : 0;
```
Công thức đang tính **threads/giây**, không phải **transactions/giây** (vé/giây).
Ví dụ: 100 threads, 1 success, 99 failed → TPS = 100/ms*1000 (sai).
Phải tính dựa trên số SUCCESS mới phản ánh đúng throughput.

**Sửa:**
```java
result.tps = timeMs > 0 ? (double) success / timeMs * 1000 : 0;
```
Và thêm 1 field `throughput` (total requests/time) riêng để so sánh hiệu năng thô của từng cơ chế.

### 5.7. Thread pool kích thước bằng numThreads gây quá tải

**File:** `src/controller/SimulatorController.java` (dòng 27)
**Mức:** Trung bình

**Vấn đề:**
```java
ExecutorService executor = Executors.newFixedThreadPool(numThreads);
```
Nếu chạy 500 threads, tạo thread pool 500 → context switch quá nhiều, CPU thrashing.
Kết quả đo TPS sẽ không chính xác vì I/O bound bị ảnh hưởng bởi CPU contention.

**Sửa:**
```java
int poolSize = Math.min(numThreads, Runtime.getRuntime().availableProcessors() * 4);
ExecutorService executor = Executors.newFixedThreadPool(poolSize);
```

### 5.8. Simulator không kiểm tra MatchStatus

**File:** `src/controller/BookingController.java` — method `bookSeat()` (dòng 237)
**Mức:** Trung bình

**Vấn đề:**
`bookSeat()` (dùng cho Simulator) không kiểm tra match có status == SCHEDULED hay không.
Nếu đặt vé vào match COMPLETED hoặc ONGOING, vẫn cho đặt → lỗi logic nghiệp vụ.

**Sửa:**
Thêm kiểm tra:
```java
Match match = matchRepository.findById(matchId)...;
if (match.getStatus() != MatchStatus.SCHEDULED) {
    throw new IllegalStateException("Match is not open for booking");
}
```
Hoặc gọi `stadiumController.getMatches()` để chỉ lấy SCHEDULED matches.

### 5.9. `resetForNextRun()` chỉ reset 1 ghế, không reset toàn bộ

**File:** `src/view/SimulatorView.java` (dòng 112-141)
**Mức:** Trung bình

**Vấn đề:**
`resetForNextRun()` chỉ reset 1 ghế (`seatId`) về AVAILABLE. Nếu chạy TEST ALL 4 mechanisms:
- Mechanism 1: ghế A bị BOOKED
- Mechanism 2: reset ghế A → AVAILABLE → chạy lại
- Nhưng các transactions/tickets của mechanism 1 chỉ bị xoá 1 phần (FAN_SIM_*)

Hậu quả: dữ liệu giữa các lần chạy bị nhiễm chéo, kết quả so sánh không chính xác.

**Sửa:**
Reset tất cả ghế của match về AVAILABLE, xoá toàn bộ tickets + transactions của match:
```java
// Reset ALL seats for this match
List<Seat> allSeats = seatRepository.findByMatch(matchId);
for (Seat seat : allSeats) {
    seat.setStatus(SeatStatus.AVAILABLE);
    seat.setVersion(0);
}
seatRepository.saveAll(allSeats);
// Xoá tickets + transactions của match này
ticketRepository.saveAll(ticketRepository.findByCondition(t -> !t.getMatchId().equals(matchId)));
transactionRepository.saveAll(transactionRepository.findByCondition(t -> !t.getMatchId().equals(matchId)));
```

### 5.10. `updateStatusOptimistic` synchronized phá vỡ OPTIMISTIC

**File:** `src/repository/SeatRepository.java` (dòng 153)
**Mức:** 🔴 CRITICAL — 2 cơ chế (OPTIMISTIC + SYNCHRONIZED) cho kết quả giống nhau

**Vấn đề:**
`public synchronized boolean updateStatusOptimistic(...)` khiến OPTIMISTIC hoạt động như SYNCHRONIZED. Xem phân tích chi tiết ở mục 1.7.

**Sửa:**
Bỏ `synchronized`, implement retry loop trong `BookingController.confirmBooking()` khi dùng OPTIMISTIC.

### 5.11. Transaction ID cũ trong transactions.csv

**File:** `data/transactions.csv`
**Mức:** Thấp

**Vấn đề:**
Dữ liệu cũ dùng ID dạng `TXN9DABF245` (UUID hex substring — 9 ký tự hex).
Code hiện tại đã fix thành `String.format("TXN%08d", transactionCounter.getAndIncrement())` → dạng `TXN00000001`.
Cần regenerate transactions.csv để có ID nhất quán.

**Sửa:**
Chạy Simulator (xóa data cũ) để tạo lại file với format ID mới.

---

## 📈 TUẦN 9 — BIỂU ĐỒ SO SÁNH & BÁO CÁO (hoàn toàn chưa làm)

### 5.12. Chưa có biểu đồ so sánh Throughput vs Double Booking Rate

**Mức:** 20% Algorithm Design (Simulator) — trọng tâm đề tài

**Yêu cầu từ Rubric:**
> "Biểu đồ / bảng so sánh rõ ràng" — throughput (TPS) và double booking rate (%) cho 4 cơ chế

**Hiện tại:**
- `SimulatorView.printResultTable()` chỉ in bảng text ASCII (dòng 143-156)
- KHÔNG có code sinh biểu đồ PNG/JPG
- KHÔNG export dữ liệu ra file để vẽ bằng tool ngoài

**Cần làm:**
- **Cách 1 (khuyên dùng):** Export kết quả ra `data/simulation_results.csv` với format:
  ```
  mechanism,threads,success,failed,doubleBooked,timeMs,tps
  NO_LOCK,100,5,95,4,45,111.1
  SYNCHRONIZED,100,1,99,0,1200,0.8
  FILE_LOCK,100,1,99,0,3500,0.3
  OPTIMISTIC,100,1,99,0,8500,0.1
  ```
  Sau đó dùng Excel/Google Sheets vẽ biểu đồ cột so sánh (Insert Chart).

- **Cách 2 (nâng cao):** Dùng Java + JFreeChart để sinh PNG tự động.

### 5.13. Chưa chạy experiment 1000 threads × 4 mechanisms

**Yêu cầu từ đề tài T9:**
> "Chạy full experiment: 1000 threads × 4 mechanisms"

**Hiện tại:**
- Simulator hỗ trợ nhập số threads (dòng 36-44) và có option 5 "TEST ALL"
- Nhưng CHƯA chạy experiment chuẩn (1000 threads × 4 mechanisms) để thu thập dữ liệu cho báo cáo

**Cần làm:**
1. Chạy Simulator với: 10, 50, 100, 200, 500, 1000 threads × 4 mechanisms
2. Ghi kết quả vào bảng
3. Vẽ biểu đồ từ dữ liệu thu được

### 5.14. Thiếu phân tích Research Question trong báo cáo

**Yêu cầu từ đề tài (trang 1-2):**
> BIG QUESTION: "Cơ chế đồng bộ hóa nào đảm bảo không xảy ra Double Booking khi hàng nghìn Fan Threads cùng đặt vé cùng một lúc?"

---

## 6. 🟢 LOW — Chất lượng code & tổ chức

### 6.1. README.md quá sơ sài — cần viết lại hoàn chỉnh

**File:** `README.md`
**Mức:** 🔴 Yêu cầu bắt buộc (Trang 10 đề tài)

**Cấu trúc cần có trong README.md:**

```markdown
# STADIUM TICKET BOOKING SIMULATION — LAB211

## Giới thiệu
Hệ thống đặt vé xem bóng đá trực tuyến, mô phỏng đồng thời 4 cơ chế đồng bộ 
(NO_LOCK, SYNCHRONIZED, FILE_LOCK, OPTIMISTIC) để ngăn chặn Double Booking.
Sinh viên thực hiện: Nhóm 4 LAB211

## Công nghệ sử dụng
- Java (JDK 21+)
- CSV file làm cơ sở dữ liệu (~34,000 dòng seats)
- JUnit 5 (kiểm thử)
- SHA-256 (hash mật khẩu)
- Java NIO FileLock (đồng bộ cấp OS)
- CountDownLatch + ExecutorService (Simulator)

## Hướng dẫn compile
```bash
# Từ thư mục gốc project:
javac -encoding UTF-8 -cp "src:src/lib/junit-platform-console-standalone-1.10.2.jar" \
  -d out $(find src -name "*.java" ! -path "src/test/*")
```

## Cách chạy chương trình
```bash
java -cp out main.Main
```

### Menu chính có 6 options:
1. **Run Data Generator** — sinh dữ liệu mẫu (stadiums, sections, matches, seats, fans)
2. **View System Configuration** — xem thống kê (số stadium, section, match, seat, fan)
3. **Run Performance Benchmarks** — đo thời gian đọc file CSV lớn
4. **Enter Ticket System** — đăng nhập/đăng ký → đặt vé/hủy vé/xem báo cáo
5. **Run Concurrent Simulator** — mô phỏng N threads đặt vé đồng thời với 4 cơ chế
0. **Exit**

## Cách chạy Simulator
1. Chọn option 5 từ menu chính
2. Nhập số lượng threads (vd: 100, 500, 1000)
3. Nhập Match ID (vd: MATCH001)
4. Chọn cơ chế:
   - 1: NO_LOCK — không đồng bộ, dễ Double Booking
   - 2: FILE_LOCK — Java NIO FileLock cấp OS
   - 3: SYNCHRONIZED — synchronized block JVM
   - 4: OPTIMISTIC — version-based lock (retry khi conflict)
   - 5: TEST ALL — chạy benchmark cả 4 cơ chế, so sánh kết quả

## Cách chạy JUnit Tests (47 tests)
```bash
java -jar src/lib/junit-platform-console-standalone-1.10.2.jar \
  -cp out --select-package test
```

## Cấu trúc project
```
├── src/
│   ├── main/          ← Main.java, AppContext.java (entry point + DI)
│   ├── model/         ← Entity + Enum (business logic)
│   ├── repository/    ← CSV Repository (CsvRepository + 7 concrete repos)
│   ├── controller/    ← Business logic (Booking, Fan, Admin, Report, Stadium, Simulator)
│   ├── view/          ← Console UI (MainView, BookingView, LoginView, ...)
│   ├── exception/     ← Custom exceptions
│   ├── generator/     ← DataGenerator (sinh data mẫu)
│   ├── benchmark/     ← PerformanceTest (đo thời gian đọc CSV)
│   └── test/          ← JUnit tests
├── data/              ← CSV files (seats, tickets, transactions, ...)
├── docs/              ← Tài liệu: diagram, flowchart, báo cáo
├── ai_logs/           ← AI Reflection Log
├── lib/               ← JUnit JARs
└── README.md
```

## Dữ liệu mẫu
- `data/seats.csv`: ~30,600 dòng (≥ 10,000 ✓)
- `data/fans.csv`: ~500 tài khoản fan
- `data/matches.csv`: 12 trận đấu (4 SCHEDULED, 4 ONGOING, 4 COMPLETED)
- `data/stadiums.csv`: 3 sân vận động
- `data/sections.csv`: 4 khán đài (VIP, STANDARD, STANDING, ECONOMY_LOWER)

## Tính năng chính
- Đăng ký/đăng nhập fan (SHA-256 hash)
- Xem danh sách trận đấu + sơ đồ ghế
- Đặt vé 2 bước: LOCK → CONFIRM (tránh mất ghế khi đang xác nhận)
- Admin CRUD: quản lý sân, khu vực, trận đấu
- Hủy vé + phục hồi ghế
- Báo cáo thống kê (doanh thu, số vé, tỉ lệ thành công)
- **Simulator**: mô phỏng N threads đặt vé đồng thời, so sánh 4 cơ chế đồng bộ
```

### 6.2. Test code chưa được tách ra khỏi src/

**File:** `src/test/` → cần chuyển thành `test/`
**Mức:** Java convention chuẩn

### 6.3. Build output ở src/out/

**File:** `src/out/` → cần chuyển thành `out/` ở root
**Mức:** Java convention chuẩn
**Cập nhật:** `.classpath` đã reference `out/main` nhưng vẫn còn `src/out/`

### 6.4. Test transactions lạc chỗ

**File:**
- `src/data/test_transactions.csv` (167 bytes)
- `data/test_transactions.csv` (167 bytes)

**Sửa:** Xóa `src/data/test_transactions.csv`, chỉ giữ 1 bản ở `data/test_transactions.csv` hoặc xóa hoàn toàn.

### 6.5. package.json không liên quan

**File:** `package.json`
**Nội dung:** File mẫu GitHub demo (dependencies: @primer/css)

**Sửa:** Xóa hoặc thêm comment giải thích.

### 6.6. JUnit version không đồng bộ

**File:** `.classpath` vs `src/lib/`

**Vấn đề:**
- `.classpath` khai báo: `junit-platform-console-standalone-1.10.2.jar`
- `src/lib/` có nhiều version khác nhau: 1.10.2, 1.14.0, 5.14.0

**Sửa:** Dùng nhất quán một bộ. Gợi ý: xoá các JAR không dùng, chỉ giữ `junit-platform-console-standalone-1.10.2.jar`.

### 6.7. `Seat.setStatus()` và `Seat.setVersion()` vẫn để public

**File:** `src/model/entity/Seat.java` (dòng 125, 128)

**Vấn đề:**
```java
public void setStatus(SeatStatus status) { this.status = status; }
public void setVersion(int version) { this.version = version; }
```
Các setter thô bypass `updateStatus()`. Có thể đổi thành `protected` để chỉ cho phép subclass/package gọi, hoặc thêm kiểm tra.

**Sửa:** Đánh dấu `@Deprecated` hoặc đổi thành `protected`.

---

## 7. 📄 DOCUMENTATION & SUBMISSION — Tài liệu và nộp bài

### 7.1. Cấu trúc ZIP nộp bài (Trang 10 đề tài)

**Cấu trúc hiện tại:**

```
NHOM_04_LAB211_TicketBooking/
├── src/           ← source code Java          ✅
├── data/          ← CSV data                   ✅ (đủ 34k+ dòng)
├── docs/
│   ├── report.docx              ← ❌ THIẾU
│   ├── slide.pptx               ← ❌ THIẾU
│   ├── class_diagram.png        ← ✅ CÓ
│   └── flowcharts/              ← ❌ THIẾU
├── ai_logs/                     ← ✅ CÓ (nhưng sai định dạng .xlsx)
└── README.md                    ← ❌ QUÁ SƠ SÀI
```

### 7.2. Hướng dẫn viết `docs/report.docx`

**Mức:** 🔴 BẮT BUỘC — mất toàn bộ điểm sản phẩm nộp nếu thiếu

Dùng Google Docs / Microsoft Word, căn lề chuẩn (top 2cm, bottom 2cm, left 3cm, right 2cm), font Times New Roman 13, line spacing 1.5.

**Cấu trúc chi tiết từng phần:**

#### Trang bìa (Cover page)
- Trường: FPT University
- Môn: LAB211 — OOP with Java
- Đề tài: Stadium Ticket Booking Simulation
- Nhóm: 4
- Thành viên: [tên 4 thành viên + mã SV]
- Giảng viên hướng dẫn: [tên GV]
- Ngày tháng

#### Mục lục (Table of Contents) — tự động

#### 1. Bối cảnh & Giới thiệu (1-2 trang)
- Vấn đề: Hệ thống đặt vé bóng đá trực tuyến, nguy cơ Double Booking
- **BIG QUESTION:** "Cơ chế đồng bộ hóa nào đảm bảo không xảy ra Double Booking khi hàng nghìn Fan Threads cùng đặt vé cùng một lúc?"
- Mục tiêu: Xây dựng hệ thống MVC, so sánh 4 cơ chế đồng bộ

#### 2. Thiết kế hệ thống (3-4 trang)
- **Kiến trúc MVC**: Mô tả 3 lớp Model-View-Controller
- **Class Diagram**: Chèn ảnh (`docs/CLASS DIAGRAM*.png`), giải thích các class chính
- **Use Case Diagram**: Chèn ảnh (`docs/UseCase_diagram.png`), mô tả actors + use cases
- **Flowcharts** (4 cái): Booking Flow, Double Booking Prevention, Simulator, Data Generator
- **CSV Schema**: Mô tả 7 file CSV (stadiums, sections, matches, seats, fans, tickets, transactions)
  - Số lượng record: seats.csv ~30k dòng (≥10k ✓), fans.csv ~500 dòng

#### 3. Cài đặt & Công nghệ (1-2 trang)
- **Ngôn ngữ**: Java, kiến trúc MVC
- **Lưu trữ**: CSV files (custom CsvRepository generic)
- **Bảo mật**: SHA-256 hash mật khẩu (không lưu plaintext)
- **Mô phỏng đồng thời**: CountDownLatch + ExecutorService
- **4 cơ chế đồng bộ**:
  - *NO_LOCK*: Không khóa → cho phép Double Booking (baseline)
  - *SYNCHRONIZED*: `synchronized` method trong SeatRepository
  - *FILE_LOCK*: Java NIO `FileLock` trên file seats.csv
  - *OPTIMISTIC*: Version field trong Seat entity + check trước write

#### 4. Kết quả thực nghiệm từ Simulator (3-5 trang)
- **Cấu hình thí nghiệm**:
  - Số threads: 10, 50, 100, 200, 500, 1000
  - Mỗi thread là 1 Fan riêng biệt, cùng đặt 1 ghế
  - Mỗi cấu hình chạy 3 lần lấy trung bình
- **Bảng kết quả** (dạng table):
  | Mechanism | Threads | Success | Failed | Double Booked | Time (ms) | TPS |
  |-----------|---------|---------|--------|---------------|-----------|-----|
  | NO_LOCK   | 100     | N       | M      | N-1           | ...       | ... |
  | SYNCHRONIZED | ... | ...     | ...    | ...           | ...       | ... |
- **BIỂU ĐỒ 1: Throughput (TPS)** — Bar chart so sánh TPS của 4 mechanisms
  - X-axis: Số threads (10, 50, 100, 200, 500, 1000)
  - Y-axis: TPS (giao dịch/giây)
  - 4 bars màu khác nhau cho 4 mechanisms
- **BIỂU ĐỒ 2: Double Booking Rate** — Bar chart so sánh số lần Double Booking
  - X-axis: Số threads
  - Y-axis: Số vé double booked (0 = an toàn, >0 = lỗi)
  - NO_LOCK phải có double booking > 0, 3 mechanism còn lại = 0

#### 5. Phân tích & Nhận xét (1-2 trang)
- **NO_LOCK**: TPS cao nhất nhưng Double Booking xảy ra → không an toàn
- **SYNCHRONIZED**: An toàn, TPS trung bình (do blocking)
- **FILE_LOCK**: An toàn, TPS thấp nhất (do OS lock overhead)
- **OPTIMISTIC**: An toàn, TPS khá (nếu ít conflict)
- **Kết luận**: Cơ chế tối ưu phụ thuộc vào trade-off giữa TPS và safety
  - Cần safety tuyệt đối → SYNCHRONIZED hoặc FILE_LOCK
  - Cần TPS cao + ít conflict → OPTIMISTIC
- **Hạn chế**: Chạy trên máy local, không phản ánh đúng môi trường production

#### 6. Tài liệu tham khảo & AI Reflection (1 trang)
- ChatGPT / Claude đã được dùng ở đâu trong quá trình phát triển
- Prompt engineering, code generation, debug support
- Link tới AI Log files: `ai_logs/`

#### Phụ lục (nếu có)
- Mã nguồn các class chính (BookingController, SeatRepository, SimulatorController)

### 7.3. Hướng dẫn tạo `docs/slide.pptx`

**Mức:** 🔴 BẮT BUỘC — yêu cầu ≥ 15 slides

Sử dụng PowerPoint / Google Slides. Theme tối giản, font dễ đọc (Arial/Verdana 24+ cho title, 18+ cho content). Mỗi slide có header + nội dung ngắn gọn.

**Cấu trúc 18 slides:**

| Slide | Nội dung | Ghi chú |
|-------|----------|---------|
| 1 | **Title Slide**: "STADIUM TICKET BOOKING SIMULATION — LAB211", Nhóm 4, FPT University | Họ tên + MSSV 4 TV |
| 2 | **Agenda**: 1. Problem → 2. Design → 3. Implementation → 4. Experiment → 5. Conclusion | |
| 3 | **Problem Statement**: Double Booking khi nhiều fan cùng đặt 1 ghế. BIG QUESTION về cơ chế đồng bộ tối ưu | Ảnh minh họa |
| 4 | **System Architecture**: MVC pattern — Model (Entity+Enum) ↔ Controller ↔ View (Console UI) | Sơ đồ khối MVC |
| 5 | **Class Diagram**: Các class chính (Entity, Repository, Controller, View) | Chèn ảnh từ docs/ |
| 6 | **Use Case Diagram**: Actors = Fan, Admin. Use cases = Register, Login, Book Ticket, Cancel, CRUD, Report | Chèn ảnh |
| 7 | **Booking Flow**: 2 bước AVAILABLE → LOCKED → BOOKED. Cancel → AVAILABLE | Flowchart #1 |
| 8 | **4 Synchronization Mechanisms**: NO_LOCK, SYNCHRONIZED, FILE_LOCK, OPTIMISTIC | Flowchart #2 |
| 9 | **NO_LOCK**: Không khóa → TPS cao nhất, nhưng Double Booking > 0 | Code snippet ngắn |
| 10 | **SYNCHRONIZED**: synchronized method → An toàn, TPS trung bình | Code snippet |
| 11 | **FILE_LOCK**: Java NIO FileLock → An toàn nhất, TPS thấp nhất | Code snippet |
| 12 | **OPTIMISTIC**: Version field + retry → An toàn, TPS tốt nếu ít conflict | Code snippet |
| 13 | **Simulator Tool**: CountDownLatch + ExecutorService, N threads cùng đặt 1 ghế | Flowchart #3 + ảnh demo |
| 14 | **Data Generator**: Sinh 30,000+ dòng seats, 500 fans, 12 matches | Flowchart #4 |
| 15 | **Experiment Results — Throughput Chart**: Bar chart TPS vs Threads | CHÈN BIỂU ĐỒ THẬT |
| 16 | **Experiment Results — Double Booking Chart**: Bar chart Double Booking vs Threads | CHÈN BIỂU ĐỒ THẬT |
| 17 | **Analysis & Conclusion**: NO_LOCK = unsafe, 3 cơ chế còn lại đều an toàn. Trade-off giữa TPS và safety. | Bảng so sánh |
| 18 | **Thank You + Q&A**: "Thank you for listening! Questions & Answers" | |

**Mẹo làm slide:**
- Dùng SmartArt cho quy trình (Booking Flow, Synchronization)
- Code snippet: chỉ lấy 3-5 dòng quan trọng, font Consolas/Courier New 14
- Biểu đồ: vẽ trong Excel trước, copy paste vào slide
- Animation: tối thiểu (chỉ fade/basic)
- Màu sắc: 3 màu chủ đạo (xanh dương + trắng + xám), tránh lòe loẹt

### 7.5. Kiểm tra compile trước khi nộp

**Cần chạy thử:**
```bash
javac -encoding UTF-8 -cp "src:src/lib/junit-platform-console-standalone-1.10.2.jar" -d out $(find src -name "*.java")
java -cp out main.Main
```

Nếu không compile được → 0% toàn bài (Rubric).

### 7.6. JUnit Tests cần pass 47/47

Theo `Huong_dan_kiem_tra_TV3.md`, cần 47 tests pass:
```bash
java -jar src/lib/junit-platform-console-standalone-1.10.2.jar -cp out --select-package test
```

---

## BẢNG TỔNG HỢP MỨC ĐỘ ẢNH HƯỞNG ĐẾN ĐIỂM

| # | Mục | Mức độ | Điểm ảnh hưởng |
|---|-----|--------|----------------|
| 1 | MVC: MainView gọi Repository trực tiếp (4+ lần) | 🔴 CRITICAL | -5%/lần = -20% |
| 2 | MVC: BookingController tự in console (5 lần) | 🔴 CRITICAL | -5%/lần = -25% |
| 3 | Thiếu Flowchart (4 cái) | 🔴 CRITICAL | 15% Abstraction |
| 4 | DataGenerator sinh giá sai | 🔴 CRITICAL | Data sai khi regenerate |
| 5 | StadiumController + SeatMapView dead code | 🟠 HIGH | Decomposition 20% |
| 6 | Thiếu report.docx + slide.pptx + biểu đồ | 🔴 CRITICAL | 20% Simulator + sản phẩm nộp |
| 7 | Simulator: công thức TPS sai | 🔴 CRITICAL | Số liệu báo cáo sai |
| 8 | Simulator: reset không sạch | 🟠 HIGH | Kết quả so sánh nhiễu |
| 9 | Simulator: không check match status | 🟠 HIGH | Logic sai |
| 10 | AI Log sai định dạng .xlsx | 🟠 HIGH | Có thể 0% AI Reflection |
| 11 | README quá sơ sài | 🟠 HIGH | Yêu cầu bắt buộc Trang 10 |
| 12 | `updateStatusOptimistic` là `synchronized` | 🔴 CRITICAL | OPTIMISTIC = SYNCHRONIZED, mất điểm thiết kế |
| 13 | SimulatorView gọi Repository trực tiếp (4 chỗ) | 🔴 CRITICAL | MVC violation, -5%/lần |
| 14 | resetForNextRun() không reset LOCKED seats | 🔴 CRITICAL | State sai giữa các lần chạy |
| 15 | BookingView không dùng current fan + NO_LOCK cứng | 🟠 HIGH | Logic sai, bỏ qua 3 cơ chế |
| 16 | bookMultipleSeats() tạo N transaction riêng | 🟠 HIGH | Sai yêu cầu "tối đa 4 vé/giao dịch" |

---

## PRIORITY ORDER — Thứ tự ưu tiên làm

```
TUẦN NÀY — LÀM NGAY (trước khi nộp)
═══════════════════════════════════════
 1. Sửa DataGenerator.SECTION_PRICES (dòng 75) — tránh hỏng data nếu regenerate
 2. Xóa `synchronized` khỏi `updateStatusOptimistic` (dòng 153) — fix OPTIMISTIC
 3. Wiring: MainView → StadiumController + SeatMapView (sửa MVC violation lớn nhất)
 4. Sửa SimulatorView: không gọi Repository trực tiếp, qua SimulatorController
 5. Xóa System.out.println khỏi BookingController (5 chỗ)
 6. Vẽ 4 flowchart → lưu docs/flowcharts/
 7. Sửa Javadoc SectionType.java + StadiumController.java (giá vé)
 8. Sửa Simulator TPS formula (success/ms*1000, không phải numThreads/ms*1000)
 9. Sửa Simulator: thread pool size = min(numThreads, availProcessors*4)
10. Sửa Simulator: resetForNextRun() reset ALL seats, không chỉ 1 ghế + reset LOCKED
11. Sửa Simulator: bookSeat() kiểm tra match status == SCHEDULED
12. Sửa BookingView: dùng current fan (không prompt fanId), không hardcode NO_LOCK
13. Viết README.md đầy đủ

TUẦN SAU
═══════════════════════════════════════
14. Export dữ liệu Simulator ra data/simulation_results.csv
15. Chạy experiment: 10, 50, 100, 200, 500, 1000 threads × 4 mechanisms
16. Vẽ biểu đồ so sánh Throughput vs Double Booking Rate
17. Tạo report.docx (Word) + slide.pptx (≥15 trang)
18. Export AI Log sang .md
19. Xóa src/out/, chuyển test/ ra ngoài
20. Sửa Scanner double creation (Main + MainView)
21. Thống nhất MainView vs ReportView "Xem vé"
22. Fix bookMultipleSeats(): 1 transaction cho N vé, dùng PARTIAL

KHI CÓ THỜI GIAN
═══════════════════════════════════════
23. Thêm Admin role thực sự (không check startsWith("admin"))
24. Xóa package.json
25. Đồng bộ JUnit version
26. Xóa src/data/test_transactions.csv
27. Sửa Seat setters thành protected/@Deprecated
28. Thêm SimulatorController vào AppContext
```
