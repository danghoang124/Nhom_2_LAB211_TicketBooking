package repository;

import model.entity.Seat;
import model.enums.SeatStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
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

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static final SeatRepository INSTANCE = new SeatRepository();
    public static SeatRepository getInstance() { return INSTANCE; }

    /**
     * ReentrantLock dùng cho cơ chế FILE_LOCK — giải quyết OverlappingFileLockException.
     * Java FileLock chỉ hoạt động giữa các process, KHÔNG giữa các thread trong cùng JVM.
     * ReentrantLock đảm bảo chỉ 1 thread được phép lock file tại một thời điểm.
     */
    private static final ReentrantLock FILE_LOCK_GUARD = new ReentrantLock();

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
     * <p><b>Lưu ý:</b> Method này KHÔNG được đánh dấu {@code synchronized}
     * vì mục đích của OPTIMISTIC là cho phép các thread đọc đồng thời và
     * phát hiện conflict qua version, thay vì blocking như SYNCHRONIZED.
     *
     * @param seatId          ID ghế cần cập nhật.
     * @param newStatus       Trạng thái mới.
     * @param expectedVersion Version mà caller đọc được trước đó.
     * @return {@code true} nếu cập nhật thành công, {@code false} nếu bị conflict (caller retry).
     */
    public synchronized boolean updateStatusOptimistic(String seatId, SeatStatus newStatus, int expectedVersion) {
        // synchronized để serialise phần I/O file: đảm bảo read → check → write là atomic.
        // Ý nghĩa "Optimistic" vẫn giữ nguyên: version được dùng để detect conflict,
        // caller nhận false khi conflict và phải retry với version mới nhất.
        List<Seat> all = findAll();

        boolean updated = false;
        for (int i = 0; i < all.size(); i++) {
            Seat seat = all.get(i);
            if (seatId.equals(seat.getSeatId())) {
                // Kiểm tra version — nếu lệch nghĩa là thread khác đã ghi trước
                if (seat.getVersion() != expectedVersion) {
                    return false; // Optimistic Lock conflict → caller retry
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
     *
     * <p><b>Fix:</b> Dùng {@link ReentrantLock} bọc ngoài để tránh
     * {@code OverlappingFileLockException} khi nhiều thread trong cùng JVM
     * cùng gọi {@code channel.lock()}.
     * Java {@link FileLock} chỉ lock giữa các <b>process</b>, không lock giữa
     * các <b>thread</b> trong cùng JVM — nên cần ReentrantLock bổ sung.
     *
     * <p>Đồng thời đọc file trực tiếp qua {@link RandomAccessFile} thay vì
     * gọi {@code findAll()} (mở FileInputStream riêng) để tránh xung đột
     * file handle trên Windows.
     */
    public boolean updateStatusFileLock(String seatId, SeatStatus newStatus) {
        FILE_LOCK_GUARD.lock();
        try (RandomAccessFile raf = new RandomAccessFile(FILE_PATH, "rw");
             FileChannel channel = raf.getChannel()) {

            // OS-level lock — bảo vệ giữa các process khác nhau
            FileLock lock = channel.lock();
            try {
                // Đọc toàn bộ file qua cùng một handle (tránh mở FileInputStream riêng)
                byte[] data = new byte[(int) raf.length()];
                raf.readFully(data);
                String content = new String(data, java.nio.charset.StandardCharsets.UTF_8);
                String[] lines = content.split("\\r?\\n");

                // Parse tất cả seats từ file content
                List<Seat> all = new ArrayList<>();
                for (int idx = 1; idx < lines.length; idx++) {
                    String line = lines[idx].trim();
                    if (line.isEmpty()) continue;
                    try {
                        all.add(Seat.fromCsvLine(line));
                    } catch (Exception e) {
                        // Bỏ qua dòng lỗi
                    }
                }

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
                    // Ghi lại toàn bộ file qua cùng handle
                    raf.seek(0);
                    raf.setLength(0);
                    StringBuilder sb = new StringBuilder();
                    sb.append(getCsvHeader()).append(System.lineSeparator());
                    for (Seat s : all) {
                        sb.append(s.toCsvLine()).append(System.lineSeparator());
                    }
                    raf.write(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    return true;
                }
                return false;
            } finally {
                if (lock != null && lock.isValid()) {
                    lock.release();
                }
            }
        } catch (Exception e) {
            System.err.println("Error during FileLock update: "
                + e.getClass().getSimpleName() + " - "
                + (e.getMessage() != null ? e.getMessage() : "(no message)"));
            return false;
        } finally {
            FILE_LOCK_GUARD.unlock();
        }
    }
}
