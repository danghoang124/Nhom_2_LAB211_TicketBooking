# DoItNow — Danh sách việc cần làm (LAB211 TicketBooking)

> Tổng hợp từ kết quả Audit toàn diện + phân tích đề tài PDF + Use Case Diagram + Rubric chấm điểm.
> Cập nhật lần cuối: 2026-07-04

---

## TỔNG QUAN TIẾN ĐỘ

| Tuần | Milestone | Trạng thái |
|------|-----------|-----------|
| T1–T2 | Phân tích yêu cầu, Use Case, Class Diagram, DataGenerator ≥10k dòng | ✅ Đạt |
| T3 | Model Layer: Entity, Enum, BaseEntity, CsvRepository | ✅ Đạt |
| T4 | Repository Layer: CRUD, đọc ≥10k dòng < 500ms | ✅ Đạt |
| T5 | Controller Layer: FanController, StadiumController, BookingController (NO_LOCK) | ✅ Đạt (có lỗi) |
| T6 | View Layer + MVC wiring | ✅ Đạt (BookingView chưa wire) |
| T7 | Synchronization: FILE_LOCK, SYNCHRONIZED, OPTIMISTIC | ❌ Chưa làm |
| T8 | Simulator Tool: CountDownLatch + ExecutorService, 100–500 threads | ❌ Chưa làm |
| T9 | Research & Report: 1000 threads × 4 mechanisms, biểu đồ so sánh | ❌ Chưa làm |
| T10 | AI Reflection & Nộp bài | ⏳ Đang làm |

---

## 🔴 CRITICAL — Sửa ngay (ảnh hưởng tính đúng đắn cơ bản)

### Bug #1 — Double Hash trong auto-login sau đăng ký
- **File:** `src/view/RegisterView.java` (~dòng 75)
- **Vấn đề:** Sau `register()`, code gọi `fanController.login(username, password)`. Nhưng `login()` sẽ `sha256(password)` một lần nữa → so sánh `sha256(sha256(password))` với `sha256(password)` trong DB → **luôn thất bại**, người dùng không được auto-login.
- **Sửa:** Thay vì gọi `login()`, set `currentFan` trực tiếp từ `result.getFan()`:
  ```java
  // Thêm method vào FanController:
  public void setCurrentFan(Fan fan) { this.currentFan = fan; }
  
  // Trong RegisterView.show(), thay:
  fanController.login(username, password);
  // bằng:
  fanController.setCurrentFan(result.getFan());
  ```

### Bug #2 — Giá vé hardcode 500,000 VND cho mọi ghế
- **File:** `src/controller/BookingController.java` (dòng 46)
- **Vấn đề:** `totalAmount = 500000` — luôn tính giá VIP dù ghế là STANDARD/STANDING/ECONOMY_LOWER.
- **Sửa:** Inject `SectionRepository` vào `BookingController`, tra `seat.getSectionId()` → `section.getBasePrice()`:
  ```java
  // Thêm dependency:
  private final SectionRepository sectionRepository;
  
  // Trong bookSeat(), thay hardcode:
  Optional<Section> sectionOpt = sectionRepository.findById(seat.getSectionId());
  totalAmount = sectionOpt.map(Section::getBasePrice).orElse(500_000L);
  ```
- **Cũng cần:** Cập nhật constructor `BookingController` và `AppContext` để inject thêm `SectionRepository`.

### Bug #3 — `bookSeat()` dùng `setStatus()` + `setVersion()` thô
- **File:** `src/controller/BookingController.java` (dòng 52–54)
- **Vấn đề:** Bypass method `updateStatus()` làm vỡ Optimistic Locking logic.
  ```java
  // SAI (hiện tại):
  seat.setStatus(SeatStatus.BOOKED);
  seat.setVersion(seat.getVersion() + 1);
  
  // ĐÚNG:
  seat.updateStatus(SeatStatus.BOOKED);
  ```

### Bug #4 — `cancelBooking()` không tăng version khi restore ghế
- **File:** `src/controller/BookingController.java` (~dòng 72)
- **Vấn đề:** Khi hủy vé, ghế được restore về AVAILABLE nhưng `setStatus()` không tăng version → Optimistic Locking bị lệch.
  ```java
  // SAI (hiện tại):
  seat.setStatus(SeatStatus.AVAILABLE);
  
  // ĐÚNG:
  seat.updateStatus(SeatStatus.AVAILABLE);
  ```

---

## 🔥 CAO NHẤT VỀ ĐIỂM — Simulator & Synchronization (20% rubric)

> Đây là trọng tâm của đề tài. BIG QUESTION: *"Cơ chế đồng bộ hóa nào đảm bảo không xảy ra Double Booking khi hàng nghìn Fan Threads cùng đặt vé cùng một lúc?"*

### S1 — Implement FILE_LOCK (Java NIO FileLock)
- **File:** `src/repository/SeatRepository.java` hoặc `src/controller/BookingController.java`
- Dùng `java.nio.channels.FileChannel` + `FileLock` bao quanh toàn bộ thao tác đọc/ghi `seats.csv`
- Thêm nhánh `case FILE_LOCK:` trong `bookSeat()` khi `mechanism == LockMechanism.FILE_LOCK`

### S2 — Implement SYNCHRONIZED
- **File:** `src/repository/SeatRepository.java`
- Thêm `synchronized` keyword hoặc `synchronized(this)` block bao quanh `save()` và `findById()` trong SeatRepository
- Thêm nhánh `case SYNCHRONIZED:` trong `bookSeat()`

### S3 — Wire OPTIMISTIC Locking vào BookingController
- `updateStatusOptimistic()` đã có trong `SeatRepository` nhưng chưa được gọi từ `BookingController`
- Thêm nhánh `case OPTIMISTIC:` gọi `seatRepository.updateStatusOptimistic(seatId, BOOKED, seat.getVersion())`
- Nếu trả về `false` (conflict) → retry hoặc return FAILED

### S4 — Tạo SimulatorController
- **File mới:** `src/controller/SimulatorController.java`
- Dùng `ExecutorService` + `CountDownLatch` để chạy N threads đồng thời
- Mỗi thread đặt cùng một ghế (để demo double booking)
- Hỗ trợ cấu hình: số thread (100–500), matchId, cơ chế đồng bộ
- Ghi kết quả: số SUCCESS, số FAILED, TPS, double booking rate

### S5 — Tạo SimulatorView
- **File mới:** `src/view/SimulatorView.java`
- Cho phép chọn: số threads, matchId, cơ chế (NO_LOCK / FILE_LOCK / SYNCHRONIZED / OPTIMISTIC)
- Hiển thị bảng ASCII kết quả sau mỗi run
- In biểu đồ so sánh TPS vs double booking rate giữa các cơ chế

### S6 — Tích hợp Simulator vào Main
- Thêm option `5. Run Concurrent Simulator` vào `Main.java`
- Sau chạy xong, ghi kết quả vào `data/transactions.csv`

---

## 🟠 HIGH — Tránh trừ điểm + hoàn thiện Use Case

### A1 — Admin CRUD (Use Case Diagram yêu cầu, hiện hoàn toàn thiếu)
- **Cần tạo mới:** `AdminController.java`, `AdminView.java`
- Create / Update / Delete / Read: Stadium, Section, Match
- View Performance Report (tổng hợp từ `ReportController`)

### A2 — 3 Flowchart bắt buộc nộp (Trang 5 đề tài)
- **4.1** Luồng Đặt Vé (Booking Flow): AVAILABLE → LOCKED → confirm → BOOKED
- **4.2** Luồng Ngăn chặn Double Booking (Synchronization Flow): check → lock → write → unlock
- **4.3** Luồng Simulator Tool: init threads → CountDownLatch → run → collect results
- Vẽ bằng draw.io hoặc PlantUML, lưu vào `docs/flowcharts/`

### A3 — Wire BookingView đúng cách
- `BookingView.displayMenu()` hiện là dead code — `MainView` xử lý booking trực tiếp
- Quyết định: hoặc xóa `BookingView`, hoặc tích hợp nó vào `MainView` thay thế logic inline

### A4 — Đảm bảo ≥5 exception được throw thực sự
- Hiện chỉ `EntityNotFoundException` được throw thực sự trong `StadiumController.buildSeatMap()`
- Cần throw thêm ≥4 exception nữa:
  - `SeatAlreadyBookedException` trong `BookingController.bookSeat()` thay `return false`
  - `InvalidCredentialsException` trong `FanController.login()` thay `return false`
  - `UserAlreadyExistsException` trong `FanController.register()` thay `RegisterResult.fail()`
  - `BookingLimitExceededException` khi số vé > 4

---

## 🟡 MEDIUM — Hoàn thiện nghiệp vụ

### M1 — Implement luồng AVAILABLE → LOCKED → BOOKED
- Khi `bookSeat()` được gọi: set ghế → `LOCKED` trước
- Sau khi xác nhận (hoặc timeout): set → `BOOKED`
- Nếu timeout hoặc cancel: restore → `AVAILABLE`
- Liên quan đến use case **Process Payment** và **Cancel Locked Seat**

### M2 — Cancel Locked Seat use case
- Hiện `cancelBooking()` chỉ xử lý vé VALID (BOOKED) → set CANCELLED
- Cần thêm: cancel ghế đang ở trạng thái LOCKED → restore AVAILABLE

### M3 — Xử lý MatchStatus.ONGOING
- Trong `MainView.handleBooking()`: kiểm tra `status != ONGOING` ngoài `SCHEDULED`
- Ghế của trận ONGOING không cho đặt mới

### M4 — Thống nhất "Xem vé"
- `MainView.showMyTickets()` → `getMyTickets()` trả về tất cả (cả CANCELLED)
- `ReportView.displayMyTickets()` → `findValidTickets()` chỉ trả về VALID
- Hai màn hình cùng tên nhưng cho kết quả khác nhau → đổi tên hoặc thống nhất logic

### M5 — Sửa mâu thuẫn giá STANDING vs ECONOMY_LOWER
- `docs/csv_schema.md`: STANDING=100k, ECONOMY_LOWER=80k
- `data/sections.csv` thực tế: STANDING=80k, ECONOMY_LOWER=100k (ngược lại)
- Chọn 1 chuẩn và đồng bộ cả data lẫn docs

### M6 — Dùng ID dài hơn cho Ticket/Transaction
- Hiện dùng `UUID.substring(0, 8)` → 32-bit entropy → dễ collision sau ~65k records
- Thay bằng counter-based ID (đọc max + 1) hoặc format `TKT + timestamp + random(4)`

### M7 — Fix FanID generation race condition
- `generateNextFanId()` đọc max rồi +1 → không thread-safe trong Simulator
- Thêm `synchronized` hoặc dùng `AtomicInteger`

---

## 🟢 LOW — Chất lượng code & tổ chức

### L1 — README.md đầy đủ (bắt buộc theo đề tài, Trang 10)
- Hướng dẫn compile: `javac -encoding UTF-8 -cp src -d out $(find src -name "*.java")`
- Cách chạy DataGenerator
- Cách chạy chương trình chính
- Cách chạy JUnit tests
- Cách chạy Simulator

### L2 — Tách test code ra khỏi source
- Chuyển `src/test/` → `test/` ở root (standard Java convention)
- Cập nhật `.classpath`

### L3 — Chuyển build output ra ngoài src
- `src/out/` → `out/` ở root
- Cập nhật `.classpath`

### L4 — Xóa file test data lạc chỗ
- `src/data/test_transactions.csv` → chuyển sang `data/` hoặc xóa

### L5 — BookingView nhận Scanner từ ngoài
- Hiện `BookingView` tự tạo `new Scanner(System.in)` → bad practice
- Truyền Scanner từ `MainView` vào

### L6 — Xóa/giải thích package.json
- `package.json` là file mẫu GitHub demo, không liên quan Java
- Gây nhầm lẫn → nên xóa hoặc thêm comment giải thích

### L7 — Đồng bộ version JUnit trong .classpath
- `.classpath` khai báo `junit-platform-console-standalone-1.10.2.jar`
- `src/lib/` có `junit-jupiter-5.14.0.jar`
- Cần dùng nhất quán một bộ

### L8 — Cấu trúc ZIP nộp bài (Trang 10 đề tài)
```
NHOM_04_LAB211_TicketBooking.zip
├── src/                    ← source code Java
├── data/                   ← tất cả CSV đã generate
├── docs/
│   ├── report.docx
│   ├── slide.pptx
│   ├── class_diagram.png
│   └── flowcharts/         ← 3 flowchart bắt buộc
├── ai_logs/                ← AI Log từng thành viên (đã có)
└── README.md
```

---

## 📋 CHECKLIST THỰC THI (theo thứ tự ưu tiên)

```
CRITICAL — Sửa ngay
[ ] Bug #1 — Sửa auto-login double hash (RegisterView.java + FanController.java)
[ ] Bug #2 — Sửa giá vé hardcode → Section.getBasePrice() (BookingController.java)
[ ] Bug #3 — Dùng seat.updateStatus() trong bookSeat() (BookingController.java)
[ ] Bug #4 — Dùng seat.updateStatus() trong cancelBooking() (BookingController.java)

SIMULATOR — 20% điểm, trọng tâm đề tài
[ ] S1  — Implement FILE_LOCK (NIO FileLock)
[ ] S2  — Implement SYNCHRONIZED (synchronized block)
[ ] S3  — Wire OPTIMISTIC vào BookingController
[ ] S4  — Tạo SimulatorController (CountDownLatch + ExecutorService)
[ ] S5  — Tạo SimulatorView (bảng so sánh ASCII)
[ ] S6  — Tích hợp Simulator vào Main.java

HIGH — Tránh trừ điểm
[ ] A1  — Admin CRUD (AdminController + AdminView)
[ ] A2  — 3 Flowchart (Booking / Sync / Simulator) → docs/flowcharts/
[ ] A3  — Xử lý BookingView dead code
[ ] A4  — Throw đủ ≥5 exception đúng chỗ

MEDIUM
[ ] M1  — Luồng AVAILABLE → LOCKED → BOOKED
[ ] M2  — Cancel Locked Seat
[ ] M3  — Handle ONGOING match status
[ ] M4  — Thống nhất hành vi "Xem vé"
[ ] M5  — Đồng bộ giá STANDING/ECONOMY_LOWER
[ ] M6  — Sửa Ticket/Transaction ID generation
[ ] M7  — Fix FanID race condition

LOW
[ ] L1  — Viết README.md đầy đủ
[ ] L2  — Tách test/ ra ngoài src/
[ ] L3  — Chuyển out/ ra ngoài src/
[ ] L4  — Xóa src/data/test_transactions.csv
[ ] L5  — BookingView nhận Scanner từ ngoài
[ ] L6  — Xóa/giải thích package.json
[ ] L7  — Đồng bộ version JUnit
[ ] L8  — Chuẩn bị cấu trúc ZIP nộp bài
```

---

## ⚠️ CÁC LỖI BỊ TRỪ ĐIỂM THEO RUBRIC (cần tránh)

| Lỗi | Mức trừ |
|-----|---------|
| Logic nghiệp vụ trong View (vi phạm MVC) | -5%/lần phát hiện |
| Truy cập CSV trực tiếp từ Controller (không qua Model layer) | -5% |
| DataGenerator không đủ 10.000 dòng tổng | -5% |
| Simulator không dùng CountDownLatch (threads không đồng thời thực sự) | -8% |
| AI Log không tồn tại hoặc rõ ràng là giả mạo | 0% phần AI Reflection |
| Không compile được | 0% toàn bài |
