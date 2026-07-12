package repository;

import model.entity.Match;
import model.enums.MatchStatus;

import java.util.List;

/**
 * Repository cụ thể cho {@link Match} — đọc/ghi {@code data/matches.csv}.
 *
 * <p>Hệ thống có 12 trận đấu (4 trận/sân × 3 sân). Trận đấu là trung tâm
 * liên kết ghế, vé, và giao dịch trong toàn bộ nghiệp vụ booking.
 */
public class MatchRepository extends CsvRepository<Match> {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static final MatchRepository INSTANCE = new MatchRepository();
    public static MatchRepository getInstance() { return INSTANCE; }

    private static final String FILE_PATH = "data/matches.csv";
    private static final String HEADER    =
        "matchId,stadiumId,homeTeam,awayTeam,matchDate,matchTime,status";

    // ── Abstract method implementations ───────────────────────────────────────

    @Override
    public String getFilePath() { return FILE_PATH; }

    @Override
    public String getCsvHeader() { return HEADER; }

    @Override
    protected Match parseFromCsvLine(String line) {
        return Match.fromCsvLine(line);
    }

    // ── Domain-specific query methods ─────────────────────────────────────────

    /**
     * Tìm tất cả trận đấu diễn ra tại một sân vận động.
     *
     * @param stadiumId ID sân vận động (ví dụ: "STD001").
     * @return List các Match (có thể rỗng).
     */
    public List<Match> findByStadium(String stadiumId) {
        return findByCondition(m -> stadiumId != null && stadiumId.equals(m.getStadiumId()));
    }

    /**
     * Tìm tất cả trận đấu theo trạng thái.
     *
     * @param status Trạng thái cần lọc (SCHEDULED / ONGOING / COMPLETED).
     * @return List các Match khớp.
     */
    public List<Match> findByStatus(MatchStatus status) {
        return findByCondition(m -> status != null && status == m.getStatus());
    }

    /**
     * Lấy tất cả trận đấu đang ở trạng thái SCHEDULED (có thể đặt vé).
     *
     * @return List các Match SCHEDULED.
     */
    public List<Match> findScheduledMatches() {
        return findByStatus(MatchStatus.SCHEDULED);
    }

    /**
     * Lấy tất cả trận đấu theo ngày thi đấu.
     *
     * @param matchDate Ngày thi đấu dạng {@code yyyy-MM-dd}.
     * @return List các Match trong ngày đó.
     */
    public List<Match> findByDate(String matchDate) {
        return findByCondition(m -> matchDate != null && matchDate.equals(m.getMatchDate()));
    }
}
