# CSV Schema — Stadium Ticket Booking Simulation
## LAB211 · FPT University · MVC with Java

---

## 1. `stadiums.csv`

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| `stadiumId` | `STRING` | PK, NOT NULL, `STD\d{3}` | Mã sân vận động |
| `name` | `STRING` | NOT NULL | Tên sân |
| `city` | `STRING` | NOT NULL | Thành phố |
| `address` | `STRING` | NOT NULL | Địa chỉ đầy đủ |
| `totalCapacity` | `INT` | > 0 | Sức chứa tổng |

**Ví dụ:**
```
stadiumId,name,city,address,totalCapacity
STD001,"Sân Vận Động Mỹ Đình",Hà Nội,"Phường Mỹ Đình II, Nam Từ Liêm, Hà Nội",40192
STD002,"Sân Vận Động Thống Nhất",TP.HCM,"138 Đặng Văn Bi, Thủ Đức, TP.HCM",15000
STD003,"Sân Vận Động Pleiku",Gia Lai,"Trần Nhật Duật, Pleiku, Gia Lai",12000
```

---

## 2. `sections.csv`

> **Thiết kế:** 4 khu vực dùng chung cho tất cả sân vận động — ghế được tạo theo tổ hợp `(section × match)`.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| `sectionId` | `STRING` | PK, NOT NULL, `SEC\d{3}` | Mã khu vực |
| `sectionType` | `ENUM` | `VIP\|STANDARD\|STANDING\|ECONOMY_LOWER` | Loại khu vực |
| `totalRows` | `INT` | > 0 | Số hàng ghế |
| `seatsPerRow` | `INT` | > 0 | Số ghế/hàng |
| `basePrice` | `LONG` | > 0, VND | Giá vé cơ bản (đồng) |

**Giá vé theo loại:**
| SectionType | Giá (VND) |
|-------------|-----------|
| VIP | 500,000 |
| STANDARD | 200,000 |
| STANDING | 100,000 |
| ECONOMY_LOWER | 80,000 |

**Ví dụ:**
```
sectionId,sectionType,totalRows,seatsPerRow,basePrice
SEC001,VIP,10,20,500000
SEC002,STANDARD,20,30,200000
SEC003,STANDING,25,35,100000
SEC004,ECONOMY_LOWER,25,35,80000
```

---

## 3. `matches.csv`

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| `matchId` | `STRING` | PK, NOT NULL, `MATCH\d{3}` | Mã trận đấu |
| `stadiumId` | `STRING` | FK → stadiums, NOT NULL | Sân tổ chức |
| `homeTeam` | `STRING` | NOT NULL | Đội nhà |
| `awayTeam` | `STRING` | NOT NULL | Đội khách |
| `matchDate` | `DATE` | `yyyy-MM-dd`, NOT NULL | Ngày thi đấu |
| `matchTime` | `STRING` | `HH:mm`, NOT NULL | Giờ thi đấu |
| `status` | `ENUM` | `SCHEDULED\|ONGOING\|COMPLETED` | Trạng thái trận |

**Ví dụ:**
```
matchId,stadiumId,homeTeam,awayTeam,matchDate,matchTime,status
MATCH001,STD001,Hà Nội FC,Hoàng Anh Gia Lai,2025-01-15,19:30,SCHEDULED
MATCH002,STD001,Viettel FC,TP.HCM FC,2025-01-22,18:00,COMPLETED
```

---

## 4. `seats.csv` *(file chính — ≥ 10,000 dòng)*

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| `seatId` | `STRING` | PK, NOT NULL, `SEAT\d{6}` | Mã ghế (unique per match) |
| `sectionId` | `STRING` | FK → sections, NOT NULL | Thuộc khu vực nào |
| `matchId` | `STRING` | FK → matches, NOT NULL | Cho trận đấu nào |
| `rowLabel` | `STRING` | NOT NULL, A–Z, AA–ZZ | Nhãn hàng ghế |
| `seatNumber` | `INT` | > 0 | Số thứ tự ghế trong hàng |
| `status` | `ENUM` | `AVAILABLE\|LOCKED\|BOOKED` | Trạng thái ghế |
| `version` | `INT` | ≥ 0, tăng mỗi lần update | Dùng cho Optimistic Locking |

> **⚠ Thiết kế quan trọng:** `seatId` là duy nhất theo cặp `(sectionId, matchId, rowLabel, seatNumber)`.  
> Mỗi trận đấu tạo ra một bộ ghế riêng biệt → tránh xung đột trạng thái giữa các trận.  
> `version` tăng mỗi khi ghế đổi trạng thái — cơ chế Optimistic Locking so sánh version trước khi ghi.

**Ví dụ:**
```
seatId,sectionId,matchId,rowLabel,seatNumber,status,version
SEAT000001,SEC001,MATCH001,A,1,AVAILABLE,0
SEAT000002,SEC001,MATCH001,A,2,AVAILABLE,0
SEAT000003,SEC001,MATCH001,A,3,BOOKED,1
```

**Thống kê dòng (được generate):**
| Đối tượng | Số lượng | Ghế/stadium | Trận/stadium | Tổng dòng seats.csv |
|-----------|----------|-------------|--------------|---------------------|
| Stadiums | 3 | 2,870 | 4 | **34,440** |

Breakdown ghế/stadium:
- VIP (10 hàng × 20 ghế) = 200
- STANDARD (20 × 30) = 600  
- ECONOMY (25 × 35) = 875  
- ECONOMY_LOWER (25 × 35) = 875
- STANDING (8 × 40) = 320
- **Tổng: 2,870 ghế × 4 trận × 3 sân = 34,440 dòng**

---

## 5. `fans.csv`

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| `fanId` | `STRING` | PK, NOT NULL, `FAN\d{4}` | Mã fan |
| `username` | `STRING` | UNIQUE, NOT NULL | Tên đăng nhập |
| `passwordHash` | `STRING` | NOT NULL | Mật khẩu đã hash (SHA-256 prefix) |
| `fullName` | `STRING` | NOT NULL | Họ tên đầy đủ |
| `email` | `STRING` | UNIQUE, format email | Email liên hệ |
| `phone` | `STRING` | `0\d{9}` | Số điện thoại |
| `createdAt` | `DATETIME` | `yyyy-MM-dd HH:mm:ss` | Ngày đăng ký |
| `isActive` | `BOOLEAN` | `true\|false` | Tài khoản hoạt động |

**Ví dụ:**
```
fanId,username,passwordHash,fullName,email,phone,createdAt,isActive
FAN0001,anv,HASH_3A2F1C,Nguyễn Văn An,anv@email.com,0912345678,2024-03-15 09:22:00,true
```

---

## 6. `tickets.csv` *(ban đầu rỗng — được tạo khi booking)*

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| `ticketId` | `STRING` | PK, `TKT\d{8}` | Mã vé |
| `fanId` | `STRING` | FK → fans, NOT NULL | Fan sở hữu vé |
| `seatId` | `STRING` | FK → seats, NOT NULL | Ghế được đặt |
| `matchId` | `STRING` | FK → matches, NOT NULL | Trận đấu |
| `transactionId` | `STRING` | FK → transactions | Giao dịch tạo ra vé này |
| `price` | `LONG` | > 0, VND | Giá thực tế thanh toán |
| `bookedAt` | `DATETIME` | `yyyy-MM-dd HH:mm:ss` | Thời điểm đặt vé |
| `status` | `ENUM` | `VALID\|CANCELLED` | Trạng thái vé |

> **Ràng buộc toàn vẹn:** `(seatId, matchId)` phải UNIQUE → ngăn Double Booking.

---

## 7. `transactions.csv` *(ban đầu rỗng — được tạo bởi Simulator)*

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| `transactionId` | `STRING` | PK, `TXN\d{8}` | Mã giao dịch |
| `fanId` | `STRING` | FK → fans | Fan thực hiện |
| `matchId` | `STRING` | FK → matches | Trận đấu |
| `numberOfTickets` | `INT` | 1–4 | Số vé trong giao dịch |
| `totalAmount` | `LONG` | ≥ 0, VND | Tổng tiền |
| `status` | `ENUM` | `SUCCESS\|FAILED\|PARTIAL` | Kết quả giao dịch |
| `mechanism` | `ENUM` | `NO_LOCK\|FILE_LOCK\|SYNCHRONIZED\|OPTIMISTIC` | Cơ chế đồng bộ dùng |
| `createdAt` | `DATETIME` | `yyyy-MM-dd HH:mm:ss` | Thời điểm giao dịch |
| `durationMs` | `LONG` | ≥ 0 | Thời gian xử lý (ms) |

---

## Sơ đồ quan hệ tóm tắt

```
sections     seats                            stadiums
──────────   ──────────────────────────────   ─────────
sectionId ──→ sectionId  (FK)                stadiumId
sectionType   seatId  (PK, unique per match)  name
totalRows     matchId    (FK → matches)       city
seatsPerRow   rowLabel                         address
basePrice     seatNumber                       totalCapacity
              status  [AVAILABLE|LOCKED|BOOKED]     │
              version (Optimistic Locking)           ↓
                                              matches
                                              ─────────────────────
                                              matchId ──→ (FK seats)
                                              stadiumId (FK → stadiums)
                                              homeTeam
                                              awayTeam
                                              matchDate
                                              matchTime
                                              status

fans          tickets              transactions
──────────    ──────────────────   ─────────────────────
fanId ─────→  ticketId             transactionId
username      fanId    (FK)        fanId
passwordHash  seatId   (FK)        matchId
fullName      matchId  (FK)        numberOfTickets
email         transactionId (FK)   totalAmount
phone         price                status
createdAt     bookedAt             mechanism
isActive      status               createdAt
                                   durationMs
```
