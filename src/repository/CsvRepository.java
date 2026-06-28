package repository;

import model.entity.BaseEntity;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Repository generic trừu tượng — xử lý đọc/ghi CSV cho mọi Entity.
 *
 * <p><b>Cách dùng:</b> Mỗi Entity cần một Repository cụ thể kế thừa lớp này
 * và implement 3 method abstract:
 * <pre>
 *   class SeatRepository extends CsvRepository&lt;Seat&gt; {
 *       {@literal @}Override public String getFilePath()    { return "data/seats.csv"; }
 *       {@literal @}Override public String getCsvHeader()   { return "seatId,sectionId,..."; }
 *       {@literal @}Override protected Seat parseFromCsvLine(String line) {
 *           return Seat.fromCsvLine(line);
 *       }
 *   }
 * </pre>
 *
 * <p><b>Luồng đọc:</b>
 * <pre>
 *   File CSV ──→ readLines() ──→ parseFromCsvLine() ──→ List&lt;T&gt;
 * </pre>
 *
 * <p><b>Luồng ghi:</b>
 * <pre>
 *   List&lt;T&gt; ──→ toCsvLine() ──→ writeLines() ──→ File CSV
 * </pre>
 *
 * <p><b>Thread Safety:</b> Lớp này KHÔNG thread-safe theo mặc định.
 * Các cơ chế đồng bộ (SYNCHRONIZED, FILE_LOCK, OPTIMISTIC) sẽ được
 * implement ở T7 bằng cách override các method phù hợp.
 *
 * @param <T> Kiểu Entity, phải kế thừa {@link BaseEntity}.
 */
public abstract class CsvRepository<T extends BaseEntity> {

    // ── Abstract methods — lớp con BẮT BUỘC implement ─────────────────────────

    /**
     * Đường dẫn tới file CSV. Ví dụ: {@code "data/seats.csv"}
     */
    public abstract String getFilePath();

    /**
     * Dòng header của file CSV. Ví dụ: {@code "seatId,sectionId,matchId,..."}
     * Dùng khi tạo file mới hoặc ghi lại toàn bộ file.
     */
    public abstract String getCsvHeader();

    /**
     * Tạo một đối tượng T từ một dòng CSV (không phải header).
     * Thường chỉ cần gọi lại static method của entity:
     * <pre>return Seat.fromCsvLine(line);</pre>
     *
     * @param line Dòng CSV chưa qua xử lý.
     * @return Entity đã parse.
     */
    protected abstract T parseFromCsvLine(String line);

    // ── Read operations ────────────────────────────────────────────────────────

    /**
     * Đọc toàn bộ file CSV và trả về List các entity.
     * Dòng header và dòng trống tự động bị bỏ qua.
     *
     * @return List các entity (không bao giờ null, có thể rỗng).
     * @throws RuntimeException nếu không đọc được file.
     */
    public List<T> findAll() {
        List<T> result = new ArrayList<>();
        File file = new File(getFilePath());

        // File chưa tồn tại → trả về list rỗng (chưa có data)
        if (!file.exists()) return result;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                // Bỏ qua header (dòng đầu)
                if (firstLine) { firstLine = false; continue; }
                // Bỏ qua dòng trống
                if (line.isBlank()) continue;

                try {
                    result.add(parseFromCsvLine(line));
                } catch (Exception e) {
                    // Ghi lỗi nhưng không dừng — bỏ qua dòng lỗi
                    System.err.println("[CsvRepository] Bỏ qua dòng lỗi trong "
                        + getFilePath() + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Không đọc được file: " + getFilePath(), e);
        }
        return result;
    }

    /**
     * Tìm một entity theo ID (khoá chính).
     *
     * @param id Giá trị ID cần tìm.
     * @return {@code Optional} chứa entity nếu tìm thấy, {@code Optional.empty()} nếu không.
     */
    public Optional<T> findById(String id) {
        if (id == null) return Optional.empty();
        return findAll().stream()
                        .filter(e -> id.equals(e.getId()))
                        .findFirst();
    }

    /**
     * Tìm tất cả entity thoả mãn điều kiện cho trước.
     *
     * <p>Ví dụ: tìm tất cả ghế của MATCH001:
     * <pre>
     *   List&lt;Seat&gt; seats = seatRepo.findByCondition(s -> "MATCH001".equals(s.getMatchId()));
     * </pre>
     *
     * @param predicate Điều kiện lọc (dùng Java Predicate — tương đương lambda).
     * @return List các entity thoả điều kiện (không bao giờ null).
     */
    public List<T> findByCondition(Predicate<T> predicate) {
        List<T> result = new ArrayList<>();
        for (T entity : findAll()) {
            if (predicate.test(entity)) result.add(entity);
        }
        return result;
    }

    /**
     * Kiểm tra entity với ID cho trước có tồn tại không.
     */
    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    /**
     * Đếm tổng số entity trong file.
     */
    public int count() {
        return findAll().size();
    }

    // ── Write operations ───────────────────────────────────────────────────────

    /**
     * Lưu (upsert) một entity vào file:
     * <ul>
     *   <li>Nếu chưa có entity với ID này → thêm dòng mới vào cuối file.</li>
     *   <li>Nếu đã có entity với ID này → cập nhật dòng đó.</li>
     * </ul>
     *
     * <p><b>Cảnh báo hiệu năng:</b> Với file lớn (≥10,000 dòng), mỗi lần save
     * sẽ đọc lại toàn bộ file. Dùng {@link #saveAll(List)} để batch write.
     *
     * @param entity Entity cần lưu.
     * @return {@code true} nếu thành công.
     * @throws RuntimeException nếu không ghi được file.
     */
    public boolean save(T entity) {
        List<T> all = findAll();

        // Tìm xem đã có chưa (dựa vào ID)
        boolean found = false;
        for (int i = 0; i < all.size(); i++) {
            if (entity.getId().equals(all.get(i).getId())) {
                all.set(i, entity); // update tại chỗ
                found = true;
                break;
            }
        }
        if (!found) all.add(entity); // thêm mới

        saveAll(all);
        return true;
    }

    /**
     * Xoá entity với ID cho trước khỏi file.
     *
     * @param id ID của entity cần xoá.
     * @return {@code true} nếu tìm thấy và xoá thành công, {@code false} nếu không tìm thấy.
     */
    public boolean deleteById(String id) {
        List<T> all = findAll();
        int before = all.size();
        all.removeIf(e -> id.equals(e.getId()));
        if (all.size() == before) return false; // không tìm thấy
        saveAll(all);
        return true;
    }

    /**
     * Ghi lại toàn bộ file với danh sách entity mới.
     * Xoá hết nội dung cũ và ghi lại từ đầu (gồm header).
     *
     * <p>Dùng cho batch update (ví dụ: đặt nhiều ghế cùng lúc).
     *
     * @param entities Danh sách entity cần ghi. Nếu rỗng → file chỉ còn header.
     * @throws RuntimeException nếu không ghi được file.
     */
    public void saveAll(List<T> entities) {
        // Đảm bảo thư mục tồn tại
        File file = new File(getFilePath());
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8),
                256 * 1024)) { // buffer 256KB cho hiệu năng

            bw.write(getCsvHeader());
            bw.newLine();

            for (T entity : entities) {
                bw.write(entity.toCsvLine());
                bw.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Không ghi được file: " + getFilePath(), e);
        }
    }

    /**
     * Append (thêm vào cuối) một entity vào file mà KHÔNG đọc lại toàn bộ file.
     * Nhanh hơn {@link #save(T)} rất nhiều với file lớn, nhưng KHÔNG kiểm tra trùng ID.
     *
     * <p>Dùng khi chắc chắn entity là mới (ví dụ: tạo ticket mới).
     *
     * @param entity Entity cần thêm.
     * @throws RuntimeException nếu không ghi được file.
     */
    public void append(T entity) {
        File file = new File(getFilePath());

        // Nếu file chưa tồn tại → tạo mới với header
        if (!file.exists()) {
            if (file.getParentFile() != null) file.getParentFile().mkdirs();
            try (BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                bw.write(getCsvHeader());
                bw.newLine();
            } catch (IOException e) {
                throw new RuntimeException("Không tạo được file: " + getFilePath(), e);
            }
        }

        // Append dòng mới
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(
                    new FileOutputStream(file, true), // true = append mode
                    StandardCharsets.UTF_8))) {
            bw.write(entity.toCsvLine());
            bw.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Không append được vào file: " + getFilePath(), e);
        }
    }
}
