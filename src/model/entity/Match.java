package model.entity;

import model.enums.MatchStatus;

/**
 * Entity đại diện cho một Trận đấu bóng đá.
 *
 * <p>Tương ứng với file: {@code data/matches.csv}
 * <pre>
 *   Header: matchId,stadiumId,homeTeam,awayTeam,matchDate,matchTime,status
 *   Ví dụ:  MATCH001,STD001,Hà Nội FC,Hoàng Anh Gia Lai,2025-01-22,19:30,SCHEDULED
 * </pre>
 */
public class Match extends BaseEntity {

    private final String      matchId;    // PK — dạng MATCH001
    private final String      stadiumId;  // FK → stadiums
    private final String      homeTeam;
    private final String      awayTeam;
    private final String      matchDate;  // yyyy-MM-dd
    private final String      matchTime;  // HH:mm
    private       MatchStatus status;     // mutable — có thể đổi SCHEDULED → ONGOING → COMPLETED

    // ── Constructor ────────────────────────────────────────────────────────────

    public Match(String matchId, String stadiumId, String homeTeam, String awayTeam,
                 String matchDate, String matchTime, MatchStatus status) {
        this.matchId   = matchId;
        this.stadiumId = stadiumId;
        this.homeTeam  = homeTeam;
        this.awayTeam  = awayTeam;
        this.matchDate = matchDate;
        this.matchTime = matchTime;
        this.status    = status;
    }

    // ── BaseEntity ─────────────────────────────────────────────────────────────

    @Override
    public String getId() { return matchId; }

    /** Tuần tự hoá: matchId, stadiumId, homeTeam, awayTeam, matchDate, matchTime, status */
    @Override
    public String toCsvLine() {
        return joinCsv(matchId, stadiumId, homeTeam, awayTeam, matchDate, matchTime, status.name());
    }

    /** Tạo Match từ một dòng CSV. */
    public static Match fromCsvLine(String line) {
        String[] f = splitCsvLine(line);
        if (f.length < 7) {
            throw new IllegalArgumentException(
                "Match CSV cần ít nhất 7 cột, nhưng chỉ có " + f.length + ": " + line);
        }
        return new Match(
            f[0].trim(),
            f[1].trim(),
            f[2].trim(),
            f[3].trim(),
            f[4].trim(),
            f[5].trim(),
            MatchStatus.fromString(f[6].trim())
        );
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public String      getMatchId()   { return matchId;   }
    public String      getStadiumId() { return stadiumId; }
    public String      getHomeTeam()  { return homeTeam;  }
    public String      getAwayTeam()  { return awayTeam;  }
    public String      getMatchDate() { return matchDate; }
    public String      getMatchTime() { return matchTime; }
    public MatchStatus getStatus()    { return status;    }

    public void setStatus(MatchStatus status) { this.status = status; }

    /** Tiện ích: trả về chuỗi mô tả trận đấu. */
    public String getTitle() { return homeTeam + " vs " + awayTeam; }
}
