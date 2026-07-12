package controller;

import exception.EntityNotFoundException;
import model.entity.Match;
import model.entity.Section;
import model.entity.Stadium;
import model.enums.MatchStatus;
import model.enums.SectionType;
import repository.MatchRepository;
import repository.SectionRepository;
import repository.StadiumRepository;

import java.util.List;

/**
 * Controller xử lý các tác vụ quản trị hệ thống (Admin CRUD).
 *
 * <p><b>Chức năng:</b>
 * <ul>
 *   <li>CRUD Stadium  — tạo, sửa, xóa, xem danh sách sân vận động.</li>
 *   <li>CRUD Section  — tạo, sửa, xóa, xem danh sách khu vực ghế.</li>
 *   <li>CRUD Match    — tạo, sửa, xóa, xem danh sách trận đấu.</li>
 * </ul>
 *
 * <p><b>Nguyên tắc MVC:</b>
 * <ul>
 *   <li>Controller KHÔNG in ra màn hình, KHÔNG đọc/ghi CSV trực tiếp.</li>
 *   <li>Mọi thao tác dữ liệu đều qua Repository.</li>
 *   <li>Controller throw exception cho các lỗi nghiệp vụ rõ ràng.</li>
 * </ul>
 */
public class AdminController {

    private StadiumRepository  stadiumRepository;
    private SectionRepository  sectionRepository;
    private MatchRepository    matchRepository;

    public AdminController(StadiumRepository stadiumRepository,
                           SectionRepository sectionRepository,
                           MatchRepository matchRepository) {
        this.stadiumRepository = stadiumRepository;
        this.sectionRepository = sectionRepository;
        this.matchRepository   = matchRepository;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STADIUM CRUD
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Lấy danh sách tất cả sân vận động.
     */
    public List<Stadium> listStadiums() {
        return stadiumRepository.findAll();
    }

    /**
     * Tìm sân vận động theo ID.
     *
     * @throws EntityNotFoundException nếu không tìm thấy.
     */
    public Stadium getStadium(String stadiumId) {
        return stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new EntityNotFoundException(
                    "Không tìm thấy sân vận động với ID: " + stadiumId));
    }

    /**
     * Tạo sân vận động mới.
     *
     * @param stadiumId    ID (dạng STD001).
     * @param name         Tên sân.
     * @param city         Thành phố.
     * @param address      Địa chỉ.
     * @param capacity     Sức chứa tổng.
     * @return Stadium vừa tạo.
     * @throws IllegalArgumentException nếu ID đã tồn tại.
     */
    public Stadium createStadium(String stadiumId, String name, String city,
                                  String address, int capacity) {
        if (stadiumRepository.existsById(stadiumId)) {
            throw new IllegalArgumentException(
                "Sân vận động với ID '" + stadiumId + "' đã tồn tại.");
        }
        Stadium stadium = new Stadium(stadiumId, name, city, address, capacity);
        stadiumRepository.save(stadium);
        return stadium;
    }

    /**
     * Cập nhật thông tin sân vận động.
     *
     * @throws EntityNotFoundException nếu stadiumId không tồn tại.
     */
    public Stadium updateStadium(String stadiumId, String name, String city,
                                  String address, int capacity) {
        if (!stadiumRepository.existsById(stadiumId)) {
            throw new EntityNotFoundException(
                "Không tìm thấy sân vận động với ID: " + stadiumId);
        }
        Stadium updated = new Stadium(stadiumId, name, city, address, capacity);
        stadiumRepository.save(updated);
        return updated;
    }

    /**
     * Xóa sân vận động theo ID.
     *
     * @throws EntityNotFoundException nếu không tìm thấy.
     */
    public void deleteStadium(String stadiumId) {
        if (!stadiumRepository.deleteById(stadiumId)) {
            throw new EntityNotFoundException(
                "Không tìm thấy sân vận động với ID: " + stadiumId);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION CRUD
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Lấy danh sách tất cả khu vực ghế.
     */
    public List<Section> listSections() {
        return sectionRepository.findAll();
    }

    /**
     * Tìm khu vực ghế theo ID.
     *
     * @throws EntityNotFoundException nếu không tìm thấy.
     */
    public Section getSection(String sectionId) {
        return sectionRepository.findById(sectionId)
                .orElseThrow(() -> new EntityNotFoundException(
                    "Không tìm thấy khu vực ghế với ID: " + sectionId));
    }

    /**
     * Tạo khu vực ghế mới.
     *
     * @param sectionId   ID (dạng SEC001).
     * @param type        Loại khu vực (VIP, STANDARD, STANDING, ECONOMY_LOWER).
     * @param totalRows   Số hàng ghế.
     * @param seatsPerRow Số ghế mỗi hàng.
     * @param basePrice   Giá vé cơ bản (VND).
     * @return Section vừa tạo.
     * @throws IllegalArgumentException nếu ID đã tồn tại.
     */
    public Section createSection(String sectionId, SectionType type,
                                  int totalRows, int seatsPerRow, long basePrice) {
        if (sectionRepository.existsById(sectionId)) {
            throw new IllegalArgumentException(
                "Khu vực ghế với ID '" + sectionId + "' đã tồn tại.");
        }
        Section section = new Section(sectionId, type, totalRows, seatsPerRow, basePrice);
        sectionRepository.save(section);
        return section;
    }

    /**
     * Cập nhật thông tin khu vực ghế.
     *
     * @throws EntityNotFoundException nếu sectionId không tồn tại.
     */
    public Section updateSection(String sectionId, SectionType type,
                                  int totalRows, int seatsPerRow, long basePrice) {
        if (!sectionRepository.existsById(sectionId)) {
            throw new EntityNotFoundException(
                "Không tìm thấy khu vực ghế với ID: " + sectionId);
        }
        Section updated = new Section(sectionId, type, totalRows, seatsPerRow, basePrice);
        sectionRepository.save(updated);
        return updated;
    }

    /**
     * Xóa khu vực ghế theo ID.
     *
     * @throws EntityNotFoundException nếu không tìm thấy.
     */
    public void deleteSection(String sectionId) {
        if (!sectionRepository.deleteById(sectionId)) {
            throw new EntityNotFoundException(
                "Không tìm thấy khu vực ghế với ID: " + sectionId);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MATCH CRUD
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Lấy danh sách tất cả trận đấu.
     */
    public List<Match> listMatches() {
        return matchRepository.findAll();
    }

    /**
     * Tìm trận đấu theo ID.
     *
     * @throws EntityNotFoundException nếu không tìm thấy.
     */
    public Match getMatch(String matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException(
                    "Không tìm thấy trận đấu với ID: " + matchId));
    }

    /**
     * Tạo trận đấu mới.
     *
     * @param matchId   ID (dạng MATCH001).
     * @param stadiumId ID sân vận động (FK).
     * @param homeTeam  Đội nhà.
     * @param awayTeam  Đội khách.
     * @param matchDate Ngày thi đấu (yyyy-MM-dd).
     * @param matchTime Giờ thi đấu (HH:mm).
     * @param status    Trạng thái ban đầu (thường là SCHEDULED).
     * @return Match vừa tạo.
     * @throws IllegalArgumentException nếu ID đã tồn tại.
     */
    public Match createMatch(String matchId, String stadiumId, String homeTeam,
                              String awayTeam, String matchDate, String matchTime,
                              MatchStatus status) {
        if (matchRepository.existsById(matchId)) {
            throw new IllegalArgumentException(
                "Trận đấu với ID '" + matchId + "' đã tồn tại.");
        }
        Match match = new Match(matchId, stadiumId, homeTeam, awayTeam,
                matchDate, matchTime, status);
        matchRepository.save(match);
        return match;
    }

    /**
     * Cập nhật thông tin trận đấu.
     *
     * @throws EntityNotFoundException nếu matchId không tồn tại.
     */
    public Match updateMatch(String matchId, String stadiumId, String homeTeam,
                              String awayTeam, String matchDate, String matchTime,
                              MatchStatus status) {
        if (!matchRepository.existsById(matchId)) {
            throw new EntityNotFoundException(
                "Không tìm thấy trận đấu với ID: " + matchId);
        }
        Match updated = new Match(matchId, stadiumId, homeTeam, awayTeam,
                matchDate, matchTime, status);
        matchRepository.save(updated);
        return updated;
    }

    /**
     * Cập nhật trạng thái một trận đấu (SCHEDULED → ONGOING → COMPLETED).
     *
     * @throws EntityNotFoundException nếu matchId không tồn tại.
     */
    public Match updateMatchStatus(String matchId, MatchStatus newStatus) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException(
                    "Không tìm thấy trận đấu với ID: " + matchId));
        match.setStatus(newStatus);
        matchRepository.save(match);
        return match;
    }

    /**
     * Xóa trận đấu theo ID.
     *
     * @throws EntityNotFoundException nếu không tìm thấy.
     */
    public void deleteMatch(String matchId) {
        if (!matchRepository.deleteById(matchId)) {
            throw new EntityNotFoundException(
                "Không tìm thấy trận đấu với ID: " + matchId);
        }
    }
}
