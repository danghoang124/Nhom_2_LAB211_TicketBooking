# DoItNow — Kế hoạch hoàn thiện dự án LAB211 TicketBooking
> **Nhóm 4 — FPT University LAB211**  
> Tạo ngày: 2026-07-02  
> Mục tiêu: Checklist chi tiết để hoàn thiện dự án từ trạng thái hiện tại đến nộp bài

---

## TỔNG QUAN TÌNH TRẠNG

| Hạng mục | Trạng thái |
|---|---|
| Tiến độ ước tính | Tuần 6 / 10 tuần |
| Phần đã xong tốt | T1–T6: Model, Repository, Controller cơ bản, View, MVC wiring |
| Phần chưa làm | T7: Synchronization (FILE_LOCK, SYNCHRONIZED, OPTIMISTIC) |
| Phần chưa làm | T8: Simulator Tool (CountDownLatch + ExecutorService) |
| Phần chưa làm | T9: Research Report, biểu đồ so sánh |
| Bug nghiêm trọng | 4 bug critical cần sửa NGAY trước khi làm bất cứ điều gì khác |

---

## PHẦN 1 — BUG CRITICAL (Sửa trước tiên — ảnh hưởng đến tính đúng đắn cơ bản)

### Bug #1 — Auto-login sau đăng ký bị lỗi double-hash
**File:** `src/view/RegisterView.java` (~dòng 75)  
**Vấn đề:** Sau khi `register()` lưu password dưới dạng `sha256(password)` vào CSV, code gọi tiếp `fanController.login(username, password)`. Hàm `login()` lại hash password một lần nữa → tìm `sha256(sha256(password))` → **không khớp → auto-login luôn thất bại**.

```java
// HIỆN TẠI (SAI):
fanController.login(username, password); // double hash!

// SỬA: Thêm method setCurrentFan() vào FanController
// rồi gọi trực tiếp sau khi register thành công:
fanController.setCurrentFan(result.getFan());
```

**Bước sửa:**
1. Thêm method `public void setCurrentFan(Fan fan)` vào `FanController.java`
2. Thay dòng `fanController.login(username, password)` trong `RegisterView.java` thành `fanController.setCurrentFan(result.getFan())`

---

### Bug #2 — Giá vé hardcode 500,000 VND cho mọi loại ghế
**File:** `src/controller/BookingController.java` (dòng 46)  
**Vấn đề:** Tất cả vé đều bị tính giá 500,000 VND (giá VIP) bất kể ghế thuộc khu nào. Ghế STANDARD (200k), STANDING (80k), ECONOMY_LOWER (100k) đều bị tính sai.

```java
// HIỆN TẠI (SAI):
totalAmount = 500000; // hardcode!

// SỬA: Tra SectionRepository để lấy giá đúng
String sectionId = seat.getSectionId();
Optional<Section> sectionOpt = sectionRepository.findById(sectionId);
totalAmount = sectionOpt.map(Section::getBasePrice).orElse(500_000L);
```

**Bước sửa:**
1. Inject `SectionRepository` vào `BookingController` constructor
2. Cập nhật `AppContext.java` để truyền `sectionRepository` khi tạo `BookingController`
3. Thay dòng `totalAmount = 500000` bằng logic tra Section

---

### Bug #3 — `bookSeat()` dùng setStatus()/setVersion() thô thay vì updateStatus()
**File:** `src/controller/BookingController.java` (dòng 52–54)  
**Vấn đề:** Vi phạm encapsulation, không đảm bảo version đồng bộ nếu logic `updateStatus()` thay đổi sau này.

```java
// HIỆN TẠI (SAI):
seat.setStatus(SeatStatus.BOOKED);
seat.setVersion(seat.getVersion() + 1);
seatRepository.save(seat);

// SỬA:
seat.updateStatus(SeatStatus.BOOKED); // tự động tăng version
seatRepository.save(seat);
```

---

### Bug #4 — `cancelBooking()` không tăng version khi restore ghế
**File:** `src/controller/BookingController.java` (dòng ~72)  
**Vấn đề:** Khi hủy vé, ghế được restore về AVAILABLE bằng `seat.setStatus()` — không tăng version. Điều này làm Optimistic Locking hoạt động sai.

```java
// HIỆN TẠI (SAI):
seat.setStatus(SeatStatus.AVAILABLE);
seatRepository.save(seat);

// SỬA:
seat.updateStatus(SeatStatus.AVAILABLE); // version tăng đúng
seatRepository.save(seat);
```

---

## PHẦN 2 — TÍNH NĂNG CỐT LÕI CHƯA LÀM (Chiếm 20% điểm rubric)

> **ĐÂY LÀ PHẦN QUAN TRỌNG NHẤT CỦA ĐỀ TÀI.**  
> BIG QUESTION: *"Cơ chế đồng bộ hóa nào đảm bảo không xảy ra Double Booking khi hàng nghìn Fan Threads cùng đặt vé cùng một lúc?"*

### S1 — Implement FILE_LOCK (Java NIO FileLock)
**File mới/sửa:** `src/repository/SeatRepository.java`  
**Yêu cầu:** Khi `mechanism == FILE_LOCK`, dùng `java.nio.channels.FileChannel` + `FileLock` để lock file `seats.csv` trong suốt thao tác read-modify-write.

```java
// Thêm method vào SeatRepository:
public synchronized boolean bookSeatWithFileLock(String seatId, SeatStatus newStatus) {
    try (FileChannel channel = FileChannel.open(
            Paths.get(getFilePath()), StandardOpenOption.READ, StandardOpenOption.WRITE);
         FileLock lock = channel.lock()) {
        // Đọc, kiểm tra, cập nhật, ghi lại
        // ...
    } catch (IOException e) {
        return false;
    }
}
```

---

### S2 — Implement SYNCHRONIZED (synchronized block trong Repository)
**File sửa:** `src/repository/SeatRepository.java` hoặc `src/controller/BookingController.java`  
**Yêu cầu:** Bọc toàn bộ check+write trong `synchronized` block để serialize concurrent access.

```java
// Trong BookingController:
private final Object lock = new Object();

public boolean bookSeat(...) {
    if (mechanism == LockMechanism.SYNCHRONIZED) {
        synchronized (lock) {
            return doBooking(fanId, matchId, seatId, mechanism);
        }
    }
    return doBooking(fanId, matchId, seatId, mechanism); // NO_LOCK path
}
```

---

### S3 — Wire OPTIMISTIC Locking vào bookSeat()
**File sửa:** `src/controller/BookingController.java`  
**Vấn đề hiện tại:** `SeatRepository.updateStatusOptimistic()` đã có sẵn nhưng `bookSeat()` không gọi nó dù `mechanism == OPTIMISTIC`.

```java
// Trong bookSeat(), thêm nhánh OPTIMISTIC:
if (mechanism == LockMechanism.OPTIMISTIC) {
    int expectedVersion = seat.getVersion();
    boolean updated = seatRepository.updateStatusOptimistic(
        seatId, SeatStatus.BOOKED, expectedVersion);
    if (!updated) {
        // Conflict → retry hoặc return FAILED
        createTransaction(..., TransactionStatus.FAILED, ...);
        return false;
    }
    success = true;
}
```

---

### S4 — Tạo SimulatorController
**File mới:** `src/controller/SimulatorController.java`  
**Yêu cầu theo đề tài:**
- Dùng `CountDownLatch` để start tất cả thread đồng thời (quan trọng — thiếu sẽ bị trừ 8%)
- Dùng `ExecutorService` (thread pool) để quản lý N threads
- Mỗi thread: đặt vé cho 1 ghế, dùng 1 cơ chế lock cụ thể
- Thu thập kết quả: số SUCCESS, số FAILED (double booking), throughput (vé/giây), thời gian trung bình

```java
public SimulationResult runSimulation(String matchId, LockMechanism mechanism, int numThreads) {
    CountDownLatch startLatch = new CountDownLatch(1);      // bắn hiệu đồng thời
    CountDownLatch doneLatch  = new CountDownLatch(numThreads); // chờ xong
    ExecutorService pool = Executors.newFixedThreadPool(numThreads);
    
    List<Seat> availableSeats = seatRepo.findAvailableByMatch(matchId);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failCount    = new AtomicInteger(0);
    
    for (int i = 0; i < numThreads; i++) {
        final String seatId = availableSeats.get(i % availableSeats.size()).getSeatId();
        pool.submit(() -> {
            try {
                startLatch.await(); // chờ lệnh bắt đầu
                boolean result = bookingController.bookSeat("FAN_SIM", matchId, seatId, mechanism);
                if (result) successCount.incrementAndGet();
                else        failCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });
    }
    
    long start = System.currentTimeMillis();
    startLatch.countDown(); // BẮN HIỆU — tất cả thread start cùng lúc
    doneLatch.await();
    long elapsed = System.currentTimeMillis() - start;
    pool.shutdown();
    
    return new SimulationResult(mechanism, numThreads, successCount.get(), 
                                failCount.get(), elapsed);
}
```

---

### S5 — Tạo SimulatorView
**File mới:** `src/view/SimulatorView.java`  
**Yêu cầu:** In bảng so sánh ASCII đẹp, chạy ≥3 cơ chế, hiển thị TPS và double booking rate.

```
╔══════════════════════════════════════════════════════════════════╗
║              CONCURRENT BOOKING SIMULATOR — RESULTS             ║
╠══════════════════╦══════════╦══════════╦══════════╦═════════════╣
║ Mechanism        ║ Threads  ║ Success  ║ Failed   ║ TPS         ║
╠══════════════════╬══════════╬══════════╬══════════╬═════════════╣
║ NO_LOCK          ║    500   ║    423   ║    77    ║  1,204/s    ║
║ FILE_LOCK        ║    500   ║    500   ║     0    ║    312/s    ║
║ SYNCHRONIZED     ║    500   ║    500   ║     0    ║    876/s    ║
║ OPTIMISTIC       ║    500   ║    498   ║     2    ║  1,089/s    ║
╚══════════════════╩══════════╩══════════╩══════════╩═════════════╝
```

---

### S6 — Tích hợp Simulator vào Main
**File sửa:** `src/main/Main.java`, `src/main/AppContext.java`  
**Yêu cầu:** Thêm option "5. Run Concurrent Simulator" vào menu chính, khởi tạo SimulatorController qua AppContext.

---

## PHẦN 3 — CÁC TÍNH NĂNG CÒN THIẾU THEO USE CASE DIAGRAM

### A1 — Admin: CRUD Stadium, Section, Match
**Use Case Diagram yêu cầu:** Create/Update/Delete/Read cho Stadium, Section, Match  
**Trạng thái:** Chưa có AdminController, AdminView  
**Ưu tiên:** Medium (có thể làm đơn giản — chỉ cần thêm menu + gọi repository)

**Các file cần tạo:**
- `src/controller/AdminController.java`
- `src/view/AdminView.java`

**Chức năng tối thiểu:**
```
Admin Menu:
1. Quản lý Stadium (Create/Read/Update/Delete)
2. Quản lý Section  (Create/Read/Update/Delete)
3. Quản lý Match    (Create/Read/Update/Delete)
4. Xem Performance Report (tổng hợp từ ReportController)
0. Thoát
```

---

### A2 — Fan: Process Payment (luồng AVAILABLE → LOCKED → BOOKED)
**Use Case Diagram:** `Book Seat <<include>> Process Payment`  
**Trạng thái:** Code bỏ qua bước LOCKED, đi thẳng AVAILABLE → BOOKED  
**File sửa:** `src/controller/BookingController.java`

```java
// Luồng đúng:
// Bước 1: Set LOCKED (giữ chỗ)
seat.updateStatus(SeatStatus.LOCKED);
seatRepository.save(seat);

// Bước 2: Confirm payment (hoặc timeout)
// ...hiển thị thông tin thanh toán...

// Bước 3: Nếu xác nhận → BOOKED; nếu hủy → AVAILABLE
seat.updateStatus(SeatStatus.BOOKED); // hoặc AVAILABLE nếu hủy
seatRepository.save(seat);
```

---

### A3 — Fan: Cancel Locked Seat
**Use Case Diagram:** `Book Seat <<extend>> Cancel Locked Seat`  
**Trạng thái:** Chỉ có cancel BOOKED ticket, không có cancel LOCKED seat  
**File sửa:** `src/controller/BookingController.java`

Thêm method `cancelLockedSeat(String seatId)`:
- Tìm ghế theo seatId
- Nếu status == LOCKED → set AVAILABLE, không cần hủy ticket (ticket chưa tạo)
- Return true/false

---

### A4 — Đảm bảo ≥5 custom exception được throw thực sự
**Trạng thái:** Có 5 exception class nhưng hầu như không được throw — chỉ dùng `return false`

| Exception | Nơi throw | Hiện tại |
|---|---|---|
| `SeatAlreadyBookedException` | `BookingController.bookSeat()` khi ghế đã BOOKED | ❌ Không throw |
| `EntityNotFoundException` | `StadiumController.buildSeatMap()` khi sectionId sai | ✅ Đã throw |
| `BookingLimitExceededException` | `BookingController` khi >4 vé/giao dịch | ❌ Không throw |
| `InvalidCredentialsException` | `FanController.login()` khi sai password | ❌ Không throw |
| `UserAlreadyExistsException` | `FanController.register()` khi trùng username | ❌ Không throw |

---

## PHẦN 4 — LỖI LOGIC VÀ CODE QUALITY

### L1 — Mâu thuẫn giá STANDING vs ECONOMY_LOWER
**File:** `data/sections.csv` vs `docs/csv_schema.md`

| Section | Schema docs | Thực tế CSV |
|---|---|---|
| SEC003 STANDING | 100,000 VND | **80,000 VND** |
| SEC004 ECONOMY_LOWER | 80,000 VND | **100,000 VND** |

**Sửa:** Chọn 1 nguồn làm chuẩn. Nên sửa `docs/csv_schema.md` cho khớp với CSV thực tế (STANDING=80k, ECONOMY_LOWER=100k), hoặc chạy lại DataGenerator sau khi đổi `SECTION_PRICES` trong `DataGenerator.java`.

---

### L2 — Ticket/Transaction ID dùng UUID 8 ký tự — dễ trùng
**File:** `src/controller/BookingController.java`

```java
// HIỆN TẠI (RỦI RO):
"TXN" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
// 8 hex chars = 32-bit → birthday paradox: ~65k records có 1% trùng

// SỬA — Dùng timestamp + counter:
private final AtomicLong txnCounter = new AtomicLong(
    transactionRepository.count() + 1
);
String transactionId = String.format("TXN%08d", txnCounter.getAndIncrement());
```

---

### L3 — `BookingView` là dead code — không được gọi từ MainView
**File:** `src/view/BookingView.java`, `src/view/MainView.java`  
**Vấn đề:** `MainView` xử lý booking trực tiếp trong `handleBooking()`, không dùng `BookingView`. `BookingView.displayMenu()` không bao giờ được gọi.  
**Sửa:** Xóa `BookingView` hoặc thay `handleBooking()` trong `MainView` bằng lời gọi `bookingView.displayMenu()`.

---

### L4 — `BookingView` tạo Scanner riêng
**File:** `src/view/BookingView.java` (constructor)  
```java
// SAI: this.scanner = new Scanner(System.in);
// SỬA: Nhận Scanner từ tham số constructor, giống LoginView/RegisterView
```

---

### L5 — Race condition trong `generateNextFanId()`
**File:** `src/controller/FanController.java`  
**Vấn đề:** Đọc max ID rồi +1 — trong Simulator đa luồng, 2 fan đăng ký đồng thời nhận cùng FanID.  
**Sửa:** Thêm `synchronized` cho method này.

---

### L6 — `MainView.showMyTickets()` vs `ReportView.displayMyTickets()` không nhất quán
**Vấn đề:** `showMyTickets()` lấy TẤT CẢ vé (kể cả CANCELLED); `displayMyTickets()` chỉ lấy VALID.  
**Sửa:** Đổi tên hoặc thống nhất hành vi. Đề xuất: `showMyTickets()` hiển thị tất cả, `ReportView` hiển thị VALID có ghi chú rõ.

---

## PHẦN 5 — TÀI LIỆU VÀ NỘP BÀI

### D1 — Viết README.md đầy đủ
**File:** `README.md` (hiện chỉ có tên nhóm)  
**Nội dung cần thêm:**
```markdown
## Cách compile và chạy
javac -encoding UTF-8 -cp src -d out $(find src -name "*.java" ! -path "src/test/*")

## Bước 1: Generate data
java -cp out generator.DataGenerator

## Bước 2: Chạy chương trình
java -cp out main.Main

## Bước 3: Chạy Simulator (sau khi implement T7-T8)
Chọn option 5 trong menu chính

## Cách chạy test
java -cp out:src/lib/junit-platform-console-standalone-1.10.2.jar \
  org.junit.platform.console.ConsoleLauncher --scan-classpath
```

---

### D2 — Vẽ 3 Flowchart (bắt buộc theo đề tài trang 5)
Đề tài yêu cầu nộp kèm báo cáo:
1. **Flowchart Booking Flow** — luồng đặt vé từ chọn trận → chọn ghế → xác nhận → BOOKED
2. **Flowchart Synchronization Flow** — so sánh 4 cơ chế (NO_LOCK, FILE_LOCK, SYNCHRONIZED, OPTIMISTIC)
3. **Flowchart Simulator Tool** — luồng chạy N threads đồng thời với CountDownLatch

**Lưu vào:** `docs/flowcharts/`

---

### D3 — Cấu trúc ZIP nộp bài (theo đề tài trang 10)
```
NHOM_04_LAB211_TicketBooking.zip
├── src/                    ← toàn bộ source code Java
├── data/
│   ├── stadiums.csv
│   ├── sections.csv
│   ├── seats.csv           (≥ 10,000 dòng)
│   ├── fans.csv
│   ├── matches.csv
│   ├── tickets.csv
│   └── transactions.csv    (kết quả simulation)
├── docs/
│   ├── report.docx         ← báo cáo Word đầy đủ
│   ├── slide.pptx          ← slide trình bày
│   ├── class_diagram.png   ← UML Class Diagram (ĐÃ CÓ)
│   └── flowcharts/
│       ├── booking_flow.png
│       ├── sync_flow.png
│       └── simulator_flow.png
├── ai_logs/
│   ├── AI_AuditLog_Dang.xlsx   (ĐÃ CÓ)
│   ├── AI_AuditLog_Khanh.xlsx  (ĐÃ CÓ)
│   ├── AI_AuditLog_Thien.xlsx  (ĐÃ CÓ)
│   └── AI_AuditLog_Van.xlsx    (ĐÃ CÓ)
└── README.md               ← hướng dẫn compile, chạy, run simulator
```

---

## PHẦN 6 — CÁC LỖI BỊ TRỪ ĐIỂM RUBRIC (cần tránh)

| Lỗi | Mức trừ | Trạng thái hiện tại |
|---|---|---|
| Logic nghiệp vụ trong View (vi phạm MVC) | -5%/lần | ✅ Nhìn chung không có, cần kiểm tra lại |
| Truy cập CSV trực tiếp từ Controller (không qua Model) | -5% | ✅ Không có |
| DataGenerator không đủ 10,000 dòng | -5% | ✅ Có 34,440 dòng |
| **Simulator không dùng CountDownLatch** | **-8%** | ❌ Chưa có Simulator |
| Al Log không tồn tại hoặc rõ ràng là giả mạo | -0% AI Reflection | ✅ Có 4 file AI Log |
| Không compile được | -100% tổng bài | Cần kiểm tra sau khi sửa bug |

---

## CHECKLIST THỰC HIỆN THEO THỨ TỰ

```
NGAY HÔM NAY — Sửa bug trước khi làm bất cứ gì:
[ ] Bug #1 — Sửa auto-login double hash (RegisterView.java + FanController.java)
[ ] Bug #2 — Sửa giá vé hardcode → lấy từ Section (BookingController.java + AppContext.java)
[ ] Bug #3 — Dùng seat.updateStatus() trong bookSeat() (BookingController.java)
[ ] Bug #4 — Dùng seat.updateStatus() trong cancelBooking() (BookingController.java)

SAU KHI SỬA BUG — Tập trung vào 20% điểm (Simulator):
[ ] S1 — Implement FILE_LOCK trong SeatRepository/BookingController
[ ] S2 — Implement SYNCHRONIZED block trong BookingController
[ ] S3 — Wire OPTIMISTIC: gọi updateStatusOptimistic() khi mechanism==OPTIMISTIC
[ ] S4 — Tạo SimulatorController với CountDownLatch + ExecutorService
[ ] S5 — Tạo SimulatorView: bảng so sánh ASCII, ghi kết quả ra transactions.csv
[ ] S6 — Thêm option "Run Simulator" vào Main menu

TIẾP THEO — Hoàn thiện Use Case còn thiếu:
[ ] A1 — AdminController + AdminView: CRUD Stadium, Section, Match
[ ] A2 — Luồng AVAILABLE→LOCKED→BOOKED (Process Payment)
[ ] A3 — Cancel Locked Seat
[ ] A4 — Throw đúng exception ở ≥5 nơi (SeatAlreadyBookedException, InvalidCredentialsException...)

CODE QUALITY — Trước khi đóng gói nộp bài:
[ ] L1 — Đồng bộ giá STANDING/ECONOMY_LOWER giữa CSV và docs
[ ] L2 — Sửa ID generation: dùng counter thay vì UUID 8 ký tự
[ ] L3 — Xử lý BookingView dead code (wire vào MainView hoặc xóa)
[ ] L4 — BookingView nhận Scanner từ constructor thay vì tự tạo
[ ] L5 — Thêm synchronized cho generateNextFanId()
[ ] L6 — Thống nhất hành vi "xem vé" giữa MainView và ReportView

TÀI LIỆU VÀ NỘP BÀI:
[ ] D1 — Viết README.md đầy đủ (compile, run DataGenerator, run Simulator)
[ ] D2 — Vẽ 3 Flowchart: Booking / Synchronization / Simulator → lưu vào docs/flowcharts/
[ ] D3 — Đóng gói ZIP đúng cấu trúc theo yêu cầu đề tài
[ ] D4 — Chạy test toàn bộ trước khi nộp (đặc biệt MainViewIntegrationTest)
[ ] D5 — Chạy DataGenerator để đảm bảo transactions.csv có kết quả simulation thực tế
```

---

## GHI CHÚ KỸ THUẬT

### Thứ tự file cần tạo mới
1. `src/controller/SimulatorController.java`
2. `src/view/SimulatorView.java`
3. `src/controller/AdminController.java`
4. `src/view/AdminView.java`
5. `docs/flowcharts/booking_flow.png` (vẽ bằng draw.io/Lucidchart)
6. `docs/flowcharts/sync_flow.png`
7. `docs/flowcharts/simulator_flow.png`

### Thứ tự file cần sửa
1. `src/controller/BookingController.java` — Bug #2, #3, #4 + S1, S2, S3
2. `src/controller/FanController.java` — Bug #1 (thêm setCurrentFan), L5
3. `src/view/RegisterView.java` — Bug #1
4. `src/main/AppContext.java` — Inject SectionRepository vào BookingController
5. `src/main/Main.java` — Thêm menu option Simulator và Admin
6. `src/view/BookingView.java` — L3, L4
7. `src/view/MainView.java` — L6
8. `README.md` — D1

### Lưu ý quan trọng khi implement Simulator
- **PHẢI dùng `CountDownLatch`** — không dùng sẽ bị trừ 8%
- Thread pool nên là `Executors.newFixedThreadPool(numThreads)`
- Kết quả chạy simulation **phải ghi vào `transactions.csv`** để chứng minh
- Chạy ít nhất **3 cơ chế**: NO_LOCK (để thấy double booking), SYNCHRONIZED (0 double booking), OPTIMISTIC (gần 0 double booking)
- Demo target: **500 threads, 1 ghế** → NO_LOCK ra nhiều FAILED, SYNCHRONIZED ra 0 FAILED

---

*File này được tạo tự động bởi công cụ audit. Cập nhật lần cuối: 2026-07-02*
