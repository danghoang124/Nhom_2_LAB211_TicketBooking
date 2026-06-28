package model.entity;

import model.enums.TicketStatus;

/**
 * Entity đại diện cho một Vé đặt chỗ.
 *
 * <p>Tương ứng với file: {@code data/tickets.csv} (ban đầu rỗng, được tạo lúc runtime)
 * <pre>
 *   Header: ticketId,fanId,seatId,matchId,transactionId,price,bookedAt,status
 *   Ví dụ:  TKT00000001,FAN0001,SEAT000123,MATCH001,TXN00000001,500000,2025-01-22 20:15:30,VALID
 * </pre>
 *
 * <p><b>Ràng buộc toàn vẹn:</b> Cặp {@code (seatId, matchId)} phải UNIQUE trong file —
 * đây là cơ chế chính ngăn chặn Double Booking.
 */
public class Ticket extends BaseEntity {

    private final String       ticketId;       // PK — dạng TKT00000001
    private final String       fanId;          // FK → fans
    private final String       seatId;         // FK → seats
    private final String       matchId;        // FK → matches
    private final String       transactionId;  // FK → transactions
    private final long         price;          // giá thực tế thanh toán (VND)
    private final String       bookedAt;       // yyyy-MM-dd HH:mm:ss
    private       TicketStatus status;         // mutable — VALID | CANCELLED

    // ── Constructor ────────────────────────────────────────────────────────────

    public Ticket(String ticketId, String fanId, String seatId, String matchId,
                  String transactionId, long price, String bookedAt, TicketStatus status) {
        this.ticketId      = ticketId;
        this.fanId         = fanId;
        this.seatId        = seatId;
        this.matchId       = matchId;
        this.transactionId = transactionId;
        this.price         = price;
        this.bookedAt      = bookedAt;
        this.status        = status;
    }

    // ── BaseEntity ─────────────────────────────────────────────────────────────

    @Override
    public String getId() { return ticketId; }

    /** Tuần tự hoá: ticketId, fanId, seatId, matchId, transactionId, price, bookedAt, status */
    @Override
    public String toCsvLine() {
        return joinCsv(ticketId, fanId, seatId, matchId, transactionId, price, bookedAt, status.name());
    }

    /** Tạo Ticket từ một dòng CSV. */
    public static Ticket fromCsvLine(String line) {
        String[] f = splitCsvLine(line);
        if (f.length < 8) {
            throw new IllegalArgumentException(
                "Ticket CSV cần ít nhất 8 cột, nhưng chỉ có " + f.length + ": " + line);
        }
        return new Ticket(
            f[0].trim(),
            f[1].trim(),
            f[2].trim(),
            f[3].trim(),
            f[4].trim(),
            Long.parseLong(f[5].trim()),
            f[6].trim(),
            TicketStatus.fromString(f[7].trim())
        );
    }

    // ── Business logic ─────────────────────────────────────────────────────────

    public boolean isValid() {
        return status == TicketStatus.VALID;
    }

    public boolean isCancelled() {
        return status == TicketStatus.CANCELLED;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public String getTicketId() {
        return ticketId;
    }

    public String getFanId() {
        return fanId;
    }

    public String getSeatId() {
        return seatId;
    }

    public String getMatchId() {
        return matchId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public long getPrice() {
        return price;
    }

    public String getBookedAt() {
        return bookedAt;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }
}
