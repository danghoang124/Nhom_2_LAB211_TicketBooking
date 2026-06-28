package repository;

import model.entity.Stadium;

import java.util.List;

/**
 * Repository cụ thể cho {@link Stadium} — đọc/ghi {@code data/stadiums.csv}.
 *
 * <p>Kế thừa toàn bộ CRUD từ {@link CsvRepository} và bổ sung
 * các domain-specific query methods cho nghiệp vụ sân vận động.
 *
 * <p>Sử dụng:
 * <pre>
 *   StadiumRepository repo = new StadiumRepository();
 *   List&lt;Stadium&gt; hanoiStadiums = repo.findByCity("Hà Nội");
 * </pre>
 */
public class StadiumRepository extends CsvRepository<Stadium> {

    private static final String FILE_PATH = "data/stadiums.csv";
    private static final String HEADER    = "stadiumId,name,city,address,totalCapacity";

    // ── Abstract method implementations ───────────────────────────────────────

    @Override
    public String getFilePath() { return FILE_PATH; }

    @Override
    public String getCsvHeader() { return HEADER; }

    @Override
    protected Stadium parseFromCsvLine(String line) {
        return Stadium.fromCsvLine(line);
    }

    // ── Domain-specific query methods ─────────────────────────────────────────

    /**
     * Tìm tất cả sân vận động tại một thành phố.
     *
     * <p>Ví dụ: {@code repo.findByCity("Hà Nội")} → danh sách sân ở Hà Nội.
     *
     * @param city Tên thành phố (case-sensitive).
     * @return List các Stadium (có thể rỗng).
     */
    public List<Stadium> findByCity(String city) {
        return findByCondition(s -> city != null && city.equals(s.getCity()));
    }

    /**
     * Tìm tất cả sân có sức chứa tối thiểu {@code minCapacity} chỗ.
     *
     * @param minCapacity Sức chứa tối thiểu (chỗ).
     * @return List các Stadium đủ điều kiện.
     */
    public List<Stadium> findByMinCapacity(int minCapacity) {
        return findByCondition(s -> s.getTotalCapacity() >= minCapacity);
    }
}
