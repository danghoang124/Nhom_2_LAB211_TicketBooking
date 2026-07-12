package repository;

import model.entity.Section;
import model.enums.SectionType;

import java.util.List;
import java.util.Optional;

/**
 * Repository cụ thể cho {@link Section} — đọc/ghi {@code data/sections.csv}.
 *
 * <p>Thiết kế hệ thống có 4 Section dùng chung cho tất cả sân vận động:
 * VIP, STANDARD, STANDING, ECONOMY_LOWER. Ghế được sinh theo tổ hợp
 * {@code (sectionId × matchId)}.
 */
public class SectionRepository extends CsvRepository<Section> {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static final SectionRepository INSTANCE = new SectionRepository();
    public static SectionRepository getInstance() { return INSTANCE; }

    private static final String FILE_PATH = "data/sections.csv";
    private static final String HEADER    = "sectionId,sectionType,totalRows,seatsPerRow,basePrice";

    // ── Abstract method implementations ───────────────────────────────────────

    @Override
    public String getFilePath() { return FILE_PATH; }

    @Override
    public String getCsvHeader() { return HEADER; }

    @Override
    protected Section parseFromCsvLine(String line) {
        return Section.fromCsvLine(line);
    }

    // ── Domain-specific query methods ─────────────────────────────────────────

    /**
     * Tìm Section theo loại khu vực (VIP / STANDARD / STANDING / ECONOMY_LOWER).
     *
     * @param type Loại khu vực cần tìm.
     * @return List các Section khớp (thường chỉ 1 phần tử).
     */
    public List<Section> findByType(SectionType type) {
        return findByCondition(s -> type != null && type == s.getSectionType());
    }

    /**
     * Lấy section đầu tiên tìm được theo loại (shortcut khi biết unique).
     *
     * @param type Loại khu vực.
     * @return Optional chứa Section nếu tìm thấy.
     */
    public Optional<Section> findFirstByType(SectionType type) {
        return findByCondition(s -> type != null && type == s.getSectionType())
               .stream().findFirst();
    }
}
