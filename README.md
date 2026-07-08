# 🏟️ Stadium Ticket Booking Simulation — LAB211

> **FPT University · LAB211 · Nhóm 4**  
> Mô phỏng hệ thống đặt vé sân vận động có khả năng xử lý đặt vé đồng thời (concurrent booking), so sánh hiệu năng 4 cơ chế đồng bộ hoá trên nền CSV thuần (không dùng database).

---

## 👥 Thành viên nhóm

| STT | Họ tên | Vai trò |
|-----|--------|---------|
| 1 | Hoàng Minh Hải Đăng | Entity · Enum · BaseEntity · CsvRepository · DataGenerator |
| 2 | Đặng Xuân Thiện | BookingController · SimulatorController · fix logic |
| 3 | Đinh Vũ Phương Khánh | FanController · View Layer · AppContext · MVC Wiring |
| 4 | Đỗ Đình Văn | ReportController · Performance Analysis · Test |

---

## 📋 Mục lục

- [Tính năng](#-tính-năng)
- [Kiến trúc hệ thống](#-kiến-trúc-hệ-thống)
- [Cấu trúc thư mục](#-cấu-trúc-thư-mục)
- [Dữ liệu CSV](#-dữ-liệu-csv)
- [Cơ chế đồng bộ hoá](#-cơ-chế-đồng-bộ-hoá)
- [Cài đặt và chạy](#-cài-đặt-và-chạy)
- [Chạy JUnit Test](#-chạy-junit-test)
- [Sơ đồ Use Case](#-sơ-đồ-use-case)

---

## ✨ Tính năng

### Guest
- Đăng ký tài khoản Fan mới (mật khẩu SHA-256)

### Fan
- Đăng nhập / Đăng xuất (session in-memory)
- Xem danh sách trận đấu
- Xem sơ đồ ghế ngồi theo trận
- Đặt vé / Huỷ vé
- Xem lịch sử vé và giao dịch của bản thân

### Admin
- Quản lý Sân vận động (CRUD)
- Quản lý Khu vực ghế / Trận đấu (CRUD)
- Xem báo cáo thống kê hệ thống

### System
- Chạy mô phỏng đặt vé đồng thời (Concurrent Simulator)
- So sánh 4 cơ chế khoá: `NO_LOCK`, `FILE_LOCK`, `SYNCHRONIZED`, `OPTIMISTIC`
- Đo throughput (TPS) và tỉ lệ Double Booking

---

## 🏗️ Kiến trúc hệ thống

Dự án tuân theo mô hình **MVC thuần (console)**:

```
┌─────────────────────────────────────────────────────────┐
│                        VIEW LAYER                        │
│  MainView · LoginView · RegisterView · BookingView       │
│  AdminView · ReportView · SeatMapView · SimulatorView    │
└────────────────────┬────────────────────────────────────┘
                     │  gọi
┌────────────────────▼────────────────────────────────────┐
│                    CONTROLLER LAYER                      │
│  FanController · BookingController · AdminController     │
│  ReportController · SimulatorController                  │
└────────────────────┬────────────────────────────────────┘
                     │  gọi
┌────────────────────▼────────────────────────────────────┐
│                   REPOSITORY LAYER                       │
│  CsvRepository<T> (abstract base)                        │
│  FanRepo · SeatRepo · TicketRepo · TransactionRepo ...   │
└────────────────────┬────────────────────────────────────┘
                     │  đọc/ghi
┌────────────────────▼────────────────────────────────────┐
│                    CSV STORAGE (data/)                   │
│  fans · stadiums · sections · matches · seats            │
│  tickets · transactions                                  │
└─────────────────────────────────────────────────────────┘
```

**Nguyên tắc bắt buộc:**
- View → chỉ nhận input và hiển thị output, không có business logic
- Controller → xử lý nghiệp vụ, không đọc/ghi file trực tiếp
- Repository → duy nhất được thao tác CSV

---

## 📁 Cấu trúc thư mục

```
Nhom_4_LAB211_TicketBooking-main/
├── src/
│   ├── main/
│   │   ├── Main.java              ← Entry point
│   │   └── AppContext.java        ← Khởi tạo và wire tất cả dependencies
│   ├── model/
│   │   ├── entity/                ← Fan, Match, Seat, Ticket, Stadium, Section, BookingTransaction
│   │   └── enums/                 ← SeatStatus, TicketStatus, LockMechanism, MatchStatus ...
│   ├── repository/
│   │   ├── CsvRepository.java     ← Abstract base: findAll, findById, save, append, delete
│   │   ├── FanRepository.java
│   │   ├── SeatRepository.java    ← + updateStatusOptimistic()
│   │   ├── TicketRepository.java  ← + existsBySeatAndMatch() chống Double Booking
│   │   └── ...
│   ├── controller/
│   │   ├── FanController.java     ← register, login, logout, getMyTickets
│   │   ├── BookingController.java ← bookSeat, cancelBooking (4 cơ chế khoá)
│   │   ├── SimulatorController.java ← concurrent simulation với CountDownLatch
│   │   ├── ReportController.java
│   │   └── ...
│   ├── view/
│   │   ├── MainView.java          ← Menu chính sau khi đăng nhập
│   │   ├── LoginView.java
│   │   ├── RegisterView.java
│   │   └── ...
│   ├── exception/                 ← Custom exceptions
│   ├── generator/
│   │   └── DataGenerator.java     ← Sinh dữ liệu CSV ban đầu
│   └── test/
│       ├── BookingTest.java
│       ├── ModelTest.java
│       ├── RepositoryTest.java
│       ├── ControllerTest.java
│       └── MainViewIntegrationTest.java
├── data/
│   ├── stadiums.csv               ← 3 sân vận động
│   ├── sections.csv               ← 4 khu vực (dùng chung)
│   ├── matches.csv                ← 12 trận đấu
│   ├── seats.csv                  ← 34,440 ghế (≥ 10,000 yêu cầu ✓)
│   ├── fans.csv                   ← 500 fan (seed data)
│   ├── tickets.csv                ← Trống ban đầu, tạo lúc runtime
│   └── transactions.csv           ← Trống ban đầu, tạo bởi Simulator
├── lib/
│   ├── junit-jupiter-5.14.0.jar
│   └── ... (8 JUnit jars)
├── docs/
│   ├── csv_schema.md
│   ├── UseCase_diagram.png
│   └── CLASS DIAGRAM ...png
├── ai_logs/
│   └── AI_AuditLog_Thien.xlsx
└── .vscode/settings.json
```

---

## 🗄️ Dữ liệu CSV

### Seed data (sinh bởi DataGenerator)

| File | Số dòng | Ghi chú |
|------|---------|---------|
| `stadiums.csv` | 3 | Mỹ Đình, Thống Nhất, Pleiku |
| `sections.csv` | 4 | VIP · STANDARD · STANDING · ECONOMY_LOWER |
| `matches.csv` | 12 | 4 trận/sân × 3 sân |
| `seats.csv` | **34,440** | 2,870 ghế/trận × 12 trận |
| `fans.csv` | 500 | Tài khoản seed, mật khẩu SHA-256 |
| `tickets.csv` | 0 | Tạo khi Fan đặt vé |
| `transactions.csv` | 0 | Tạo bởi Simulator |

### Giá vé theo khu vực

| Khu vực | Giá (VND) | Hàng × Ghế/hàng | Tổng ghế/trận |
|---------|-----------|-----------------|---------------|
| VIP | 500,000 | 10 × 20 | 200 |
| STANDARD | 200,000 | 20 × 30 | 600 |
| STANDING | 100,000 | 25 × 35 | 875 |
| ECONOMY_LOWER | 80,000 | 25 × 35 | 875 |
| **Tổng** | — | — | **2,550/trận** |

---

## 🔒 Cơ chế đồng bộ hoá

Simulator chạy N thread đồng thời tranh đặt cùng 1 ghế, đo 2 chỉ số:

| Cơ chế | Mô tả | Double Booking | Throughput |
|--------|-------|---------------|------------|
| `NO_LOCK` | Không khoá — race condition tự do | ❌ Cao | ✅ Cao nhất |
| `FILE_LOCK` | Java NIO `FileLock` — khoá ở tầng OS | ✅ Không | ⚠️ Chậm nhất |
| `SYNCHRONIZED` | `synchronized` block trong Repository | ✅ Không | ✅ Tốt |
| `OPTIMISTIC` | So sánh `version` trước khi ghi (OCC) | ✅ Không | ✅ Tốt nhất trong khoá |

**Double Booking Rate** = số lần đặt thành công vượt quá 1 / tổng số thread  
**Throughput (TPS)** = số thread / thời gian xử lý (giây)

---

## 🚀 Cài đặt và chạy

### Yêu cầu
- **JDK 17+** (dùng Java Record, `String.repeat()`, `List.of()`)
- Không cần Maven/Gradle, không cần database

### Bước 1 — Sinh dữ liệu CSV

```cmd
chcp 65001
cd Nhom_4_LAB211_TicketBooking-main
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d bin -cp "lib\*" @sources.txt
java "-Dfile.encoding=UTF-8" "-Dstdout.encoding=UTF-8" -cp "bin;lib\*" main.Main
```

Chọn **1. Khởi chạy bộ tạo dữ liệu** để sinh CSV ban đầu.

### Bước 2 — Chạy ứng dụng

```cmd
java "-Dfile.encoding=UTF-8" "-Dstdout.encoding=UTF-8" -cp "bin;lib\*" main.Main
```

Hoặc dùng file `run.bat` đã có sẵn:

```cmd
run.bat
```

### Menu chính

```
=== HỆ THỐNG MÔ PHỎNG ĐẶT VÉ SÂN VẬN ĐỘNG (LAB211) ===
1. Khởi chạy bộ tạo dữ liệu (DataGenerator)
2. Xem cấu hình hệ thống
3. Chạy kiểm thử hiệu năng (Performance Benchmarks)
4. Đăng nhập (Fan / Admin)
5. Đăng ký tài khoản mới (Fan)
0. Thoát
```

### Tài khoản demo

| Role | Username | Password |
|------|----------|----------|
| Fan | `anv` | `password1` |
| Admin | `admin` | `admin123` |

---

## 🧪 Chạy JUnit Test

```cmd
java -jar lib\junit-platform-console-standalone-1.10.2.jar ^
     --class-path "bin;lib\*" ^
     --scan-class-path
```

| File test | Nội dung |
|-----------|---------|
| `ModelTest.java` | Entity round-trip CSV, enum parse, splitCsvLine |
| `RepositoryTest.java` | CsvRepository CRUD, findByCondition, append |
| `BookingTest.java` | bookSeat success/fail, cancelBooking, createTicket |
| `ControllerTest.java` | StadiumController CRUD, SeatMapView |
| `MainViewIntegrationTest.java` | Integration flow Fan login → booking |

---

## 📊 Sơ đồ Use Case

Xem file `docs/UseCase_diagram.png` để biết đầy đủ luồng của 3 actor:

```
Guest  → Register Account · View Match List
Fan    → Login · Logout · View Match List · Book Seat
         View Seat Map · Process Payment · View My Tickets
Admin  → CRUD Stadium/Section/Match · View Performance Report
System → Run Concurrent Simulator
```

---

## 📝 Ghi chú kỹ thuật

- **Không dùng database** — toàn bộ persistence qua file CSV với `BufferedReader`/`BufferedWriter` (256KB buffer)
- **Optimistic Locking** — trường `version` trong `Seat` tăng mỗi lần cập nhật, `SeatRepository.updateStatusOptimistic()` so sánh version trước khi ghi
- **Thread-safety** — `generateNextFanId()` dùng `synchronized`, ID sinh ra dạng `TKT%08d`/`TXN%08d` (AtomicLong counter)
- **Password security** — SHA-256 hex uppercase, không lưu plaintext
- **CSV encoding** — UTF-8 toàn bộ, RFC-4180 compliant (field có dấu phẩy được bọc nháy kép)
- **Performance** — đọc 34,440 dòng `seats.csv` trong < 500ms (deliverable T4 ✓)
