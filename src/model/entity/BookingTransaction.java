package model.entity;

import model.enums.LockMechanism;
import model.enums.TransactionStatus;

/**
 * Entity đại diện cho một Giao dịch đặt vé.
 *
 * <p>Tương ứng với file: {@code data/transactions.csv} (ban đầu rỗng, được tạo bởi Simulator)
 * <pre>
 *   Header: transactionId,fanId,matchId,numberOfTickets,totalAmount,status,mechanism,createdAt,durationMs
 *   Ví dụ:  TXN00000001,FAN0001,MATCH001,2,1000000,SUCCESS,SYNCHRONIZED,2025-01-22 20:15:30,45
 * </pre>
 *
 * <p>Mỗi giao dịch ghi lại: fan nào, trận nào, mua mấy vé, tốn bao nhiêu tiền,
 * kết quả thế nào, dùng cơ chế đồng bộ gì, và mất bao lâu (durationMs).
 * Đây là dữ liệu để phân tích hiệu năng của từng cơ chế trong Simulator.
 */
public class BookingTransaction extends BaseEntity {

    private final String            transactionId;   // PK — dạng TXN00000001
    private final String            fanId;           // FK → fans
    private final String            matchId;         // FK → matches
    private final int               numberOfTickets; // 1–4 vé/giao dịch
    private final long              totalAmount;     // tổng tiền (VND)
    private       TransactionStatus status;          // mutable — SUCCESS | FAILED | PARTIAL
    private final LockMechanism     mechanism;       // cơ chế đồng bộ đã dùng
    private final String            createdAt;       // yyyy-MM-dd HH:mm:ss
    private final long              durationMs;      // thời gian xử lý (milliseconds)

    // ── Constructor ────────────────────────────────────────────────────────────

    public BookingTransaction(String transactionId, String fanId, String matchId,
                              int numberOfTickets, long totalAmount,
                              TransactionStatus status, LockMechanism mechanism,
                              String createdAt, long durationMs) {
        this.transactionId   = transactionId;
        this.fanId           = fanId;
        this.matchId         = matchId;
        this.numberOfTickets = numberOfTickets;
        this.totalAmount     = totalAmount;
        this.status          = status;
        this.mechanism       = mechanism;
        this.createdAt       = createdAt;
        this.durationMs      = durationMs;
    }

    // ── BaseEntity ─────────────────────────────────────────────────────────────

    @Override
    public String getId() { return transactionId; }

    /**
     * Tuần tự hoá:
     * transactionId, fanId, matchId, numberOfTickets, totalAmount,
     * status, mechanism, createdAt, durationMs
     */
    @Override
    public String toCsvLine() {
        return joinCsv(
            transactionId, fanId, matchId, numberOfTickets, totalAmount,
            status.name(), mechanism.name(), createdAt, durationMs
        );
    }

    /** Tạo BookingTransaction từ một dòng CSV. */
    public static BookingTransaction fromCsvLine(String line) {
        String[] f = splitCsvLine(line);
        if (f.length < 9) {
            throw new IllegalArgumentException(
                "BookingTransaction CSV cần ít nhất 9 cột, nhưng chỉ có " + f.length + ": " + line);
        }
        return new BookingTransaction(
            f[0].trim(),
            f[1].trim(),
            f[2].trim(),
            Integer.parseInt(f[3].trim()),
            Long.parseLong(f[4].trim()),
            TransactionStatus.fromString(f[5].trim()),
            LockMechanism.fromString(f[6].trim()),
            f[7].trim(),
            Long.parseLong(f[8].trim())
        );
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public String            getTransactionId()   { return transactionId;   }
    public String            getFanId()            { return fanId;           }
    public String            getMatchId()          { return matchId;         }
    public int               getNumberOfTickets()  { return numberOfTickets; }
    public long              getTotalAmount()       { return totalAmount;     }
    public TransactionStatus getStatus()           { return status;          }
    public LockMechanism     getMechanism()        { return mechanism;       }
    public String            getCreatedAt()        { return createdAt;       }
    public long              getDurationMs()       { return durationMs;      }

    public void setStatus(TransactionStatus status) { this.status = status; }

    public boolean isSuccessful() { return status == TransactionStatus.SUCCESS; }
}
