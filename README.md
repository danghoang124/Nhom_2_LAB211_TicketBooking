# Nhóm 4 — LAB211: Stadium Ticket Booking Simulation

> **FPT University · LAB211 · Nhóm 4**  
> Mô phỏng hệ thống đặt vé sân vận động có khả năng xử lý đặt vé đồng thời (concurrent booking), so sánh hiệu năng 4 cơ chế đồng bộ hoá trên nền CSV thuần (không dùng database).

---

## Thành viên nhóm

| STT | Họ tên | Vai trò |
|-----|--------|---------|
| 1 | Hoàng Minh Hải Đăng | Entity · Enum · BaseEntity · CsvRepository · DataGenerator |
| 2 | Đặng Xuân Thiện | BookingController · SimulatorController · Fix logic |
| 3 | Đinh Vũ Phương Khánh | FanController · View Layer · AppContext · MVC Wiring |
| 4 | Đỗ Đình Văn | ReportController · Performance Analysis · Test |

---

## Mô tả dự án

Hệ thống mô phỏng đặt vé sân vận động bóng đá chạy trên console.  
Fan có thể đăng ký, đăng nhập, chọn khu vực, đặt ghế và xem lịch sử vé.  
Hệ thống hỗ trợ **4 cơ chế đồng bộ** để kiểm tra tình huống nhiều Fan đặt cùng một ghế cùng lúc (Concurrent Booking Simulator).

---

## Yêu cầu hệ thống

- **Java 17+** (đã test trên JDK 17.0.12)
- **Không cần database** — toàn bộ dữ liệu lưu trong file CSV
- **Không cần Maven/Gradle** — compile thủ công bằng `javac`
- Thư viện JUnit 5 có sẵn trong thư mục `lib/`

---

## Cách chạy

Mở Terminal (Ctrl+``  ``), đổi sang CMD nếu đang là PowerShell, rồi chạy:
```cmd
     chcp 65001
     javac -encoding UTF-8 -d out -cp "src\lib\*" src\main\Main.java
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "out;src\lib\*" main.Main
     Hoặc compile toàn bộ source một lần:
```cmd
     chcp 65001
     dir /s /b src\*.java > sources.txt
     dir /s /b test\*.java >> sources.txt
     javac -encoding UTF-8 -d out -cp "src\lib\*" @sources.txt
     java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "out;src\lib\*" main.Main

Sau khi chương trình khởi động, chọn **1**:

```
=== STADIUM TICKET BOOKING SIMULATION (LAB211) ===
1. Run Data Generator (DataGenerator)   ← chọn 1, chờ vài giây
```

Kết quả — các file CSV được tạo tự động trong `data/`:

| File CSV | Nội dung |
|----------|----------|
| `stadiums.csv` | 3 sân vận động |
| `sections.csv` | 4 khu vực ghế (dùng chung cho tất cả sân) |
| `matches.csv` | 12 trận đấu (4 trận/sân × 3 sân) |
| `seats.csv` | 34,440 ghế (≥ 10,000 theo yêu cầu ✓) |
| `fans.csv` | 500 fan mẫu (mật khẩu SHA-256) |
| `tickets.csv` | Trống — tạo khi Fan đặt vé |
| `transactions.csv` | Trống — tạo bởi Simulator |

---

## Menu chính

```
======================================================
            STADIUM TICKET BOOKING SIMULATION
                 FPT UNIVERSITY - LAB211
======================================================

=== STADIUM TICKET BOOKING SIMULATION (LAB211) ===
1. Run Data Generator (DataGenerator)
2. View System Configuration (Stadiums, Seats...)
3. Run Performance Benchmarks (PerformanceTest)
4. Enter Ticket System (Login / Book / Report)
5. Run Concurrent Simulator
0. Exit
```

---

## Tài khoản demo

Sau khi chạy DataGenerator (menu 1), dùng tài khoản seed:

| Role | Username | Password |
|------|----------|----------|
| Fan | `anv` | `password1` |
| Fan | `bnb` | `password2` |
| Admin | `admin` | `admin123` |

---

## Cấu trúc thư mục

```
Nhom_4_LAB211_TicketBooking-main/
├── src/
│   ├── main/
│   │   ├── Main.java                  ← Entry point — menu chính
│   │   └── AppContext.java            ← DI Container thủ công (wire tất cả dependencies)
│   ├── controller/
│   │   ├── FanController.java         ← register, login, logout, getMyTickets
│   │   ├── BookingController.java     ← bookSeat, cancelBooking (4 cơ chế khoá)
│   │   ├── AdminController.java       ← CRUD Stadium / Section / Match
│   │   ├── ReportController.java      ← thống kê vé, giao dịch, doanh thu
│   │   ├── SimulatorController.java   ← concurrent simulation, CountDownLatch
│   │   └── StadiumController.java
│   ├── view/
│   │   ├── MainView.java              ← Menu sau khi đăng nhập
│   │   ├── LoginView.java
│   │   ├── RegisterView.java
│   │   ├── BookingView.java
│   │   ├── AdminView.java
│   │   ├── ReportView.java
│   │   ├── SeatMapView.java
│   │   └── SimulatorView.java
│   ├── model/
│   │   ├── entity/                    ← Fan, Seat, Ticket, Match, Section, Stadium, BookingTransaction
│   │   └── enums/                     ← SeatStatus, TicketStatus, LockMechanism, SectionType, Role...
│   ├── repository/
│   │   ├── CsvRepository.java         ← Abstract base: findAll, findById, save, append, delete
│   │   ├── SeatRepository.java        ← + updateStatusOptimistic()
│   │   ├── TicketRepository.java      ← + existsBySeatAndMatch() (chống Double Booking)
│   │   └── ... (7 repository)
│   ├── exception/                     ← 6 custom exceptions
│   ├── generator/
│   │   └── DataGenerator.java         ← Sinh toàn bộ dữ liệu CSV ban đầu
│   ├── benchmark/
│   │   └── PerformanceTest.java       ← Đo tốc độ đọc ≥ 10,000 dòng < 500ms
│   └── experiment/
│       └── RunExperiment.java         ← Chạy benchmark tự động: 6 mức thread × 4 cơ chế
│                                         Xuất kết quả ra data/simulation_results.csv
├── test/                              ← JUnit 5 tests (ngoài src/)
│   ├── BookingTest.java
│   ├── ControllerTest.java
│   ├── MainViewIntegrationTest.java
│   ├── ModelTest.java
│   └── RepositoryTest.java
├── data/                              ← CSV files (tự tạo khi chọn menu 1)
├── lib/                               ← JUnit 5 jars
├── docs/
│   ├── csv_schema.md                  ← Schema tất cả CSV
│   ├── UseCase_diagram.png
│   ├── CLASS DIAGRAM (MODEL).png
│   ├── CLASS DIAGRAM (REPOSITORY).png
│   └── LAB211_TicketBooking_De_Tai.pdf
├── ai_logs/                           ← AI Audit Log từng thành viên
└── README.md
```

---

## Kiến trúc hệ thống

Dự án áp dụng **MVC Architecture** thuần console:

```
┌──────────────────────────────────────────────────┐
│                   VIEW LAYER                      │
│  MainView · LoginView · RegisterView · BookingView│
│  AdminView · ReportView · SimulatorView           │
│  Chỉ: nhận input từ Scanner, in output ra màn hình│
└───────────────────┬──────────────────────────────┘
                    │ gọi method
┌───────────────────▼──────────────────────────────┐
│                CONTROLLER LAYER                   │
│  FanController · BookingController               │
│  AdminController · ReportController              │
│  SimulatorController                             │
│  Xử lý: validate, hash, sinh ID, quản lý session │
└───────────────────┬──────────────────────────────┘
                    │ gọi method
┌───────────────────▼──────────────────────────────┐
│                REPOSITORY LAYER                   │
│  CsvRepository<T> (abstract) + 7 concrete repos  │
│  Duy nhất được đọc/ghi file CSV                  │
└───────────────────┬──────────────────────────────┘
                    │ đọc/ghi
┌───────────────────▼──────────────────────────────┐
│               CSV STORAGE (data/)                 │
│  fans · stadiums · sections · matches            │
│  seats · tickets · transactions                  │
└──────────────────────────────────────────────────┘
```

**Nguyên tắc cứng:**
- View **không** đọc/ghi CSV trực tiếp
- Controller **không** in ra màn hình
- Repository **không** chứa business logic

---

## Giá vé theo khu vực

| Khu vực | Mã | Giá (VND) | Hàng × Ghế/hàng | Tổng/trận |
|---------|----|-----------|-----------------|-----------|
| VIP | SEC001 | 500,000 | 10 × 20 | 200 |
| STANDARD | SEC002 | 200,000 | 20 × 30 | 600 |
| STANDING | SEC003 | 100,000 | 25 × 35 | 875 |
| ECONOMY_LOWER | SEC004 | 80,000 | 25 × 35 | 875 |
| **Tổng** | | | | **2,550/trận** |

---

## Concurrent Booking Simulator

Menu chính → chọn **5. Run Concurrent Simulator**

Mô phỏng N thread đồng thời tranh đặt cùng 1 ghế. So sánh 4 cơ chế:

| Cơ chế | Mô tả | Double Booking | Throughput |
|--------|-------|---------------|------------|
| `NO_LOCK` | Không khoá — race condition tự do | ❌ Xảy ra | ✅ Cao nhất |
| `FILE_LOCK` | Java NIO `FileLock` — khoá tầng OS | ✅ Ngăn chặn | ⚠️ Chậm nhất |
| `SYNCHRONIZED` | `synchronized` block trong Repository | ✅ Ngăn chặn | ✅ Tốt |
| `OPTIMISTIC` | So sánh `version` trước khi ghi (OCC) | ✅ Ngăn chặn | ✅ Tốt nhất |

> **Double Booking** = cùng 1 ghế bán cho 2 Fan khác nhau.

### Chạy benchmark tự động (RunExperiment)

Để lấy kết quả đầy đủ với 6 mức thread (10, 50, 100, 200, 500, 1000) × 4 cơ chế:

```cmd
java -Dfile.encoding=UTF-8 -cp "bin;lib\*" experiment.RunExperiment
```

Kết quả xuất ra `data/simulation_results.csv` và in bảng tổng hợp trực tiếp ra màn hình.

---

## Vòng đời trạng thái ghế

```
AVAILABLE ──→ LOCKED ──→ BOOKED
                 └──→ AVAILABLE  (nếu Fan huỷ giữ chỗ)
```

---

## Chạy JUnit Tests

>Lưu ý quan trọng: Project chỉ có 1 jar JUnit là junit-platform-console-standalone-1.10.2.jar nằm trong src/lib/. File này đã bao gồm toàn bộ JUnit 5 — đủ để chạy test, không cần thêm jar nào khác.
>Bước 1 — Compile tất cả (nếu chưa compile)
```cmd
--chcp 65001
--dir /s /b src\*.java > sources.txt
--dir /s /b test\*.java >> sources.txt
--javac -encoding UTF-8 -d out -cp "src\lib\*" @sources.txt
--Bước 2 — Chạy toàn bộ test
```
```cmd
--java -cp "out;src\lib\*" ^
--  org.junit.platform.console.ConsoleLauncher ^
--  --select-package=test
--Bước 3 — Chạy từng file test riêng lẻ (nếu cần)
```cmd
--java -cp "out;src\lib\*" ^
--  org.junit.platform.console.ConsoleLauncher ^
--  --select-class=test.BookingTest

--java -cp "out;src\lib\*" ^
--  org.junit.platform.console.ConsoleLauncher ^
--  --select-class=test.ModelTest

--java -cp "out;src\lib\*" ^
--  org.junit.platform.console.ConsoleLauncher ^
--  --select-class=test.RepositoryTest

--java -cp "out;src\lib\*" ^
--  org.junit.platform.console.ConsoleLauncher ^
--  --select-class=test.ControllerTest

| File test | Nội dung |
|-----------|---------|
| `ModelTest.java` | Entity round-trip CSV, enum parse, splitCsvLine |
| `RepositoryTest.java` | CsvRepository CRUD, findByCondition, append |
| `BookingTest.java` | bookSeat success/fail, cancelBooking, createTicket |
| `ControllerTest.java` | StadiumController, SeatMapView |
| `MainViewIntegrationTest.java` | Integration flow: Fan login → booking |

---

## Ghi chú AI

Dự án ghi nhận việc sử dụng AI hỗ trợ trong quá trình phát triển.  
Chi tiết xem tại thư mục `ai_logs/` — mỗi thành viên có file log riêng ghi rõ prompt, output và đánh giá phản biện.
