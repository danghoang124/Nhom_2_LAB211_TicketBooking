package repository;

import model.entity.Seat;
import model.enums.SeatStatus;

import java.util.List;
import java.util.Optional;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * Repository cụ thể cho {@link Seat} — đọc/ghi {@code data/seats.csv}.
 *
 * <p><b>File lớn nhất trong hệ thống:</b> ~30,000 dòng (2,870 ghế × 4 trận × ~3 sân).
 * Mọi query đều đi qua {@link #findAll()} với BufferedReader 256KB nên đủ nhanh (&lt;500ms).
 *
 * <p><b>Optimistic Locking:</b> Phương thức {@link #updateStatusOptimistic} thực hiện
 * read-modify-write an toàn bằng cách so sánh {@code version} trước khi ghi.
 * Đây là nền tảng cho cơ chế đồng bộ OPTIMISTIC ở Tuần 7.
 */
public class SeatRepository extends CsvRepository<Seat> {

    private static final String FILE_PATH = "data/seats.csv";
    private static final String HEADER    =
        "seatId,sectionId,matchId,rowLabel,seatNumber,status,version";

    // ── Abstract method implementations ───────────────────────────────────────

    @Override
    public String getFilePath() { return FILE_PATH; }

    @Override
    public String getCsvHeader() { return HEADER; }

    @Override
    protected Seat parseFromCsvLine(String line) {
        return Seat.fromCsvLine(line);
    }

    // ── Domain-specific query methods ─────────────────────────────────────────

    /**
     * Lấy tất cả ghế của một trận đấu.
     *
     * <p>Ví dụ: {@code seatRepo.findByMatch("MATCH001")} → ~2,870 ghế.
     *
     * @param matchId ID trận đấu (ví dụ: "MATCH001").
     * @return List các Seat thuộc trận đó.
     */
    public List<Seat> findByMatch(String matchId) {
        return findByCondition(s -> matchId != null && matchId.equals(s.getMatchId()));
    }

    /**
     * Lấy tất cả ghế AVAILABLE của một trận đấu (ghế có thể đặt).
     *
     * @param matchId ID trận đấu.
     * @return List các Seat có status = AVAILABLE.
     */
    public List<Seat> findAvailableByMatch(String matchId) {
        return findByCondition(s ->
            matchId != null && matchId.equals(s.getMatchId()) && s.isAvailable()
        );
    }

    /**
     * Lấy tất cả ghế thuộc một khu vực (section).
     *
     * @param sectionId ID khu vực (ví dụ: "SEC001").
     * @return List các Seat thuộc khu vực đó.
     */
    public List<Seat> findBySection(String sectionId) {
        return findByCondition(s -> sectionId != null && sectionId.equals(s.getSectionId()));
    }

    /**
     * Lấy tất cả ghế thuộc tổ hợp (section × match) cụ thể.
     *
     * <p>Đây là đơn vị seat block nhỏ nhất để đặt vé (ví dụ: toàn bộ ghế VIP
     * trong MATCH001).
     *
     * @param sectionId ID khu vực.
     * @param matchId   ID trận đấu.
     * @return List các Seat thuộc tổ hợp đó.
     */
    public List<Seat> findBySectionAndMatch(String sectionId, String matchId) {
        return findByCondition(s ->
            sectionId != null && sectionId.equals(s.getSectionId()) &&
            matchId   != null && matchId.equals(s.getMatchId())
        );
    }

    /**
     * Lấy các ghế AVAILABLE của một tổ hợp (section × match).
     *
     * @param sectionId ID khu vực.
     * @param matchId   ID trận đấu.
     * @return List các Seat khả dụng.
     */
    public List<Seat> findAvailableBySectionAndMatch(String sectionId, String matchId) {
        return findByCondition(s ->
            sectionId != null && sectionId.equals(s.getSectionId()) &&
            matchId   != null && matchId.equals(s.getMatchId()) &&
            s.isAvailable()
        );
    }

    /**
     * Đếm số ghế AVAILABLE của một trận đấu.
     *
     * @param matchId ID trận đấu.
     * @return Số ghế còn trống.
     */
    public int countAvailable(String matchId) {
        return findAvailableByMatch(matchId).size();
    }

    /**
     * Đếm số ghế theo status của một trận đấu.
     *
     * @param matchId ID trận đấu.
     * @param status  Trạng thái cần đếm.
     * @return Số ghế có trạng thái đó.
     */
    public int countByStatus(String matchId, SeatStatus status) {
        return (int) findByMatch(matchId).stream()
                     .filter(s -> status == s.getStatus())
                     .count();
    }

    // ── Optimistic Locking ────────────────────────────────────────────────────

    /**
     * Cập nhật trạng thái ghế theo cơ chế Optimistic Locking.
     *
     * <p><b>Thuật toán:</b>
     * <ol>
     *   <li>Đọc toàn bộ file → tìm ghế theo {@code seatId}.</li>
     *   <li>So sánh {@code version} hiện tại với {@code expectedVersion}.</li>
     *   <li>Nếu khớp: cập nhật status + tăng version → ghi lại file → trả về {@code true}.</li>
     *   <li>Nếu không khớp (conflict): không ghi gì → trả về {@code false}.</li>
     * </ol>
     *
     * <p><b>Thread Safety:</b> KHÔNG dùng synchronized/blocking — các thread đọc
     * file đồng thời, nếu version thay đổi giữa chừng thì conflict được phát hiện
     * qua version check. Caller phải retry khi nhận {@code false}.
     *
     * @param seatId          ID ghế cần cập nhật.
     * @param newStatus       Trạng thái mới.
     * @param expectedVersion Version mà caller đọc được trước đó.
     * @return {@code true} nếu cập nhật thành công, {@code false} nếu bị conflict (caller retry).
     */
    public boolean updateStatusOptimistic(String seatId, SeatStatus newStatus, int expectedVersion) {
        List<Seat> all = findAll();

        boolean updated = false;
        for (int i = 0; i < all.size(); i++) {
            Seat seat = all.get(i);
            if (seatId.equals(seat.getSeatId())) {
                // Kiểm tra version — nếu lệch nghĩa là thread khác đã ghi trước
                if (seat.getVersion() != expectedVersion) {
                    return false; // Optimistic Lock conflict
                }
                // Version khớp → áp dụng update
                seat.updateStatus(newStatus); // tự động tăng version
                all.set(i, seat);
                updated = true;
                break;
            }
        }

        if (!updated) return false; // không tìm thấy seatId

        saveAll(all); // ghi lại toàn bộ file
        return true;
    }

    /**
     * Lấy thông tin một ghế kèm version hiện tại (dùng cho Optimistic Lock workflow).
     *
     * <p>Caller nên lưu {@code seat.getVersion()} rồi truyền vào
     * {@link #updateStatusOptimistic} khi cần cập nhật.
     *
     * @param seatId ID ghế.
     * @return Optional chứa Seat nếu tìm thấy.
     */
    public Optional<Seat> findByIdForUpdate(String seatId) {
        return findById(seatId);
    }

    // ── Synchronized and File Lock ────────────────────────────────────────────

    /**
     * Cập nhật trạng thái ghế sử dụng cơ chế Synchronized block.
     * Đảm bảo không có 2 thread nào có thể đọc và ghi đồng thời.
     */
    public synchronized boolean updateStatusSynchronized(String seatId, SeatStatus newStatus) {
        List<Seat> all = findAll();
        boolean updated = false;
        for (int i = 0; i < all.size(); i++) {
            Seat seat = all.get(i);
            if (seatId.equals(seat.getSeatId())) {
                if (seat.getStatus() == SeatStatus.BOOKED) {
                    return false; // Đã được đặt
                }
                seat.updateStatus(newStatus);
                all.set(i, seat);
                updated = true;
                break;
            }
        }
        if (updated) {
            saveAll(all);
            return true;
        }
        return false;
    }

    /**
     * Cập nhật trạng thái ghế sử dụng cơ chế File Lock.
     * Khoá ở tầng OS đảm bảo không thread nào khác (thậm chí tiến trình khác) can thiệp.
     */
    public boolean updateStatusFileLock(String seatId, SeatStatus newStatus) {
        try (RandomAccessFile file = new RandomAccessFile(FILE_PATH, "rw");
             FileChannel channel = file.getChannel()) {
            
            // Lấy exclusive lock trên file
            FileLock lock = channel.lock();
            try {
                // Đọc toàn bộ (Vì đã có OS lock, ta có thể dùng findAll đọc trực tiếp)
                List<Seat> all = findAll();
                boolean updated = false;
                for (int i = 0; i < all.size(); i++) {
                    Seat seat = all.get(i);
                    if (seatId.equals(seat.getSeatId())) {
                        if (seat.getStatus() == SeatStatus.BOOKED) {
                            return false; // Đã được đặt
                        }
                        seat.updateStatus(newStatus);
                        all.set(i, seat);
                        updated = true;
                        break;
                    }
                }
                
                if (updated) {
                    // Xóa file cũ và ghi lại nội dung mới
                    file.setLength(0); // Truncate the file to 0 bytes
                    file.writeBytes(getCsvHeader() + System.lineSeparator());
                    for (Seat s : all) {
                        file.writeBytes(s.toCsvLine() + System.lineSeparator());
                    }
                    return true;
                }
                return false;
            } finally {
                if (lock != null) {
                    lock.release();
                }
            }
        } catch (Exception e) {
            System.err.println("Error during FileLock update: " + e.getMessage());
            return false;
        }
    }
}
