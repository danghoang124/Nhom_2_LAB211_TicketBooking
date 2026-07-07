package controller;

import exception.EntityNotFoundException;
import model.entity.Match;
import model.entity.Seat;
import model.entity.Section;
import model.enums.MatchStatus;
import model.enums.SeatStatus;
import repository.MatchRepository;
import repository.SeatRepository;
import repository.SectionRepository;

import java.util.List;

/**
 * ============================================================
 * STADIUM CONTROLLER — Tuần 5 (Controller Layer)
 * ============================================================
 *
 * Nhiệm vụ: Điều phối luồng dữ liệu giữa View và Repository
 * liên quan đến sân vận động, khán đài và sơ đồ ghế ngồi.
 *
 * Luồng MVC của Controller này:
 *
 *   [SeatMapView]  ──gọi method──►  [StadiumController]
 *                                          │
 *                    ┌──────────────────────┤
 *                    │                      │                      │
 *             [MatchRepository]  [SectionRepository]  [SeatRepository]
 *                    │                      │                      │
 *                [matches.csv]        [sections.csv]          [seats.csv]
 *
 * Quy tắc bắt buộc:
 *   - Controller KHÔNG đọc/ghi file CSV trực tiếp.
 *   - Mọi thao tác dữ liệu đều qua Repository.
 *   - Controller chỉ xử lý logic điều phối và trả kết quả về View.
 *
 * Dependencies:
 *   - MatchRepository    → truy vấn data/matches.csv
 *   - SectionRepository  → truy vấn data/sections.csv
 *   - SeatRepository     → truy vấn data/seats.csv
 */
public class StadiumController {

    // ── Dependencies (được inject qua Constructor) ──────────────────────────
    private final MatchRepository   matchRepository;
    private final SectionRepository sectionRepository;
    private final SeatRepository    seatRepository;

    // ── Constructor ──────────────────────────────────────────────────────────

    /**
     * Khởi tạo StadiumController với 3 Repository cần thiết.
     *
     * Tại sao inject qua constructor?
     * → Để dễ test (có thể truyền mock object vào khi viết JUnit).
     * → Tuân thủ Dependency Injection — Controller không tự tạo Repository.
     *
     * @param matchRepository   Repository quản lý trận đấu.
     * @param sectionRepository Repository quản lý khán đài.
     * @param seatRepository    Repository quản lý ghế ngồi.
     */
    public StadiumController(MatchRepository matchRepository,
                              SectionRepository sectionRepository,
                              SeatRepository seatRepository) {
        this.matchRepository   = matchRepository;
        this.sectionRepository = sectionRepository;
        this.seatRepository    = seatRepository;
    }

    // ── PUBLIC METHODS ───────────────────────────────────────────────────────

    /**
     * Lấy danh sách các trận đấu đang mở bán vé (trạng thái SCHEDULED).
     *
     * Tại sao chỉ lấy SCHEDULED?
     * → COMPLETED = trận đã xong, không bán vé nữa.
     * → ONGOING   = trận đang diễn ra, không cho đặt thêm.
     * → Chỉ SCHEDULED = trận sắp diễn ra, đang mở bán.
     *
     * Flow:
     *   View gọi getMatches()
     *     → Controller gọi matchRepository.findByStatus(SCHEDULED)
     *       → Repository đọc matches.csv, lọc dòng có status=SCHEDULED
     *         → Trả về List<Match> cho Controller → trả về View
     *
     * @return Danh sách các trận đấu SCHEDULED (có thể rỗng, không bao giờ null).
     */
    public List<Match> getMatches() {
        return matchRepository.findByStatus(MatchStatus.SCHEDULED);
    }

    /**
     * Lấy danh sách tất cả khán đài (Section) trong hệ thống.
     *
     * Hệ thống có 4 khán đài dùng chung cho tất cả sân vận động:
     *   SEC001 - VIP       : 10 hàng × 20 ghế = 200 ghế  (500,000đ/vé)
     *   SEC002 - STANDARD  : 20 hàng × 30 ghế = 600 ghế  (200,000đ/vé)
     *   SEC003 - STANDING      : 25 hàng × 35 ghế = 875 ghế  (100,000đ/vé)
     *   SEC004 - ECONOMY_LOWER : 25 hàng × 35 ghế = 875 ghế  ( 80,000đ/vé)
     *
     * Flow:
     *   View gọi getSections()
     *     → Controller gọi sectionRepository.findAll()
     *       → Repository đọc toàn bộ sections.csv
     *         → Trả về List<Section> cho Controller → trả về View
     *
     * @return Danh sách 4 Section.
     */
    public List<Section> getSections() {
        return sectionRepository.findAll();
    }

    /**
     * Xây dựng sơ đồ ghế dạng mảng 2 chiều (Seat[][]) cho một khán đài tại một trận.
     *
     * ── Dữ liệu ghế đến từ đâu? ──────────────────────────────────────────────
     * File seats.csv chứa từng ghế dưới dạng:
     *   seatId, sectionId, matchId, rowLabel, seatNumber, status, version
     * Ví dụ: SEC001-MATCH001-A-1, SEC001, MATCH001, A, 1, AVAILABLE, 0
     *
     * Mỗi ghế biết:
     *   - Nó thuộc hàng nào  → rowLabel  (A, B, C, ... Z, AA, AB, ...)
     *   - Nó là ghế số mấy  → seatNumber (1, 2, 3, ...)
     *
     * ── Cách xây dựng mảng 2 chiều ────────────────────────────────────────────
     * Bước 1: Lấy kích thước mảng từ Section (totalRows × seatsPerRow).
     * Bước 2: Tạo Seat[totalRows][seatsPerRow] rỗng (mặc định null).
     * Bước 3: Với mỗi ghế trong danh sách:
     *           rowIdx = rowLabelToIndex("A") = 0, "B" = 1, "AA" = 26, ...
     *           colIdx = seatNumber - 1       (vì seatNumber bắt đầu từ 1)
     *         → Điền seat vào seatMap[rowIdx][colIdx].
     *
     * ── Ví dụ minh họa (Section VIP: 3 hàng × 4 ghế) ─────────────────────────
     *
     *        Col: 1    2    3    4
     *   Hàng A: [ A1 ][ A2 ][ A3 ][ A4 ]   → seatMap[0][0..3]
     *   Hàng B: [ B1 ][ B2 ][ B3 ][ B4 ]   → seatMap[1][0..3]
     *   Hàng C: [ C1 ][ C2 ][ C3 ][ C4 ]   → seatMap[2][0..3]
     *
     * @param sectionId ID khán đài (ví dụ: "SEC001").
     * @param matchId   ID trận đấu (ví dụ: "MATCH001").
     * @return Mảng 2 chiều Seat[totalRows][seatsPerRow].
     *         Ô null = không có ghế tại vị trí đó (dữ liệu thiếu).
     * @throws EntityNotFoundException nếu sectionId không tồn tại trong CSV.
     */
    public Seat[][] buildSeatMap(String sectionId, String matchId) {
        // Bước 1: Lấy thông tin Section để biết kích thước mảng
        // Nếu sectionId không tồn tại → ném EntityNotFoundException
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy khán đài với ID: " + sectionId));

        // Bước 2: Lấy danh sách tất cả ghế của tổ hợp (sectionId × matchId)
        List<Seat> seats = seatRepository.findBySectionAndMatch(sectionId, matchId);

        // Bước 3: Khởi tạo mảng 2 chiều rỗng
        int totalRows   = section.getTotalRows();
        int seatsPerRow = section.getSeatsPerRow();
        Seat[][] seatMap = new Seat[totalRows][seatsPerRow];

        // Bước 4: Điền từng ghế vào đúng ô [rowIdx][colIdx]
        for (Seat seat : seats) {
            int rowIdx = rowLabelToIndex(seat.getRowLabel()); // "A"→0, "B"→1, "AA"→26
            int colIdx = seat.getSeatNumber() - 1;            // seatNumber là 1-indexed

            // Kiểm tra bounds để tránh ArrayIndexOutOfBoundsException
            boolean rowOK = rowIdx >= 0 && rowIdx < totalRows;
            boolean colOK = colIdx >= 0 && colIdx < seatsPerRow;
            if (rowOK && colOK) {
                seatMap[rowIdx][colIdx] = seat;
            }
        }

        return seatMap;
    }

    /**
     * Đếm số ghế còn trống (AVAILABLE) của một khán đài tại một trận đấu.
     *
     * View dùng method này để hiển thị dòng:
     *   "Còn X ghế trống trong khán đài VIP - MATCH001"
     *
     * Flow:
     *   View gọi showAvailableSeats("SEC001", "MATCH001")
     *     → Controller gọi seatRepository.findAvailableBySectionAndMatch(...)
     *       → Repository lọc seats.csv: sectionId=SEC001, matchId=MATCH001, status=AVAILABLE
     *         → Trả về số lượng (int) cho View
     *
     * @param sectionId ID khán đài.
     * @param matchId   ID trận đấu.
     * @return Số ghế còn trống (>= 0).
     */
    public int showAvailableSeats(String sectionId, String matchId) {
        return seatRepository.findAvailableBySectionAndMatch(sectionId, matchId).size();
    }

    /**
     * Lấy tất cả trận đấu (không lọc).
     */
    public List<Match> getAllMatches() {
        return matchRepository.findAll();
    }

    /**
     * Kiểm tra trận đấu có tồn tại không.
     */
    public boolean matchExists(String matchId) {
        return matchRepository.existsById(matchId);
    }

    /**
     * Lấy danh sách ghế theo khu vực và trận đấu.
     */
    public List<Seat> getSeatsBySectionAndMatch(String sectionId, String matchId) {
        return seatRepository.findBySectionAndMatch(sectionId, matchId);
    }

    /**
     * Lấy danh sách ghế còn trống theo khu vực và trận đấu.
     */
    public List<Seat> getAvailableSeatsBySectionAndMatch(String sectionId, String matchId) {
        return seatRepository.findAvailableBySectionAndMatch(sectionId, matchId);
    }

    /**
     * Đếm số ghế còn trống của một trận đấu.
     */
    public int getAvailableSeatsCount(String matchId) {
        return seatRepository.countAvailable(matchId);
    }

    // ── PRIVATE HELPER ────────────────────────────────────────────────────────

    /**
     * Chuyển nhãn hàng ghế (String) sang chỉ số mảng 0-indexed (int).
     *
     * Công thức: Xử lý như số hệ cơ số 26 (A=1, B=2, ..., Z=26).
     *
     * Ví dụ minh họa:
     *   "A"  → 1          - 1 = 0
     *   "B"  → 2          - 1 = 1
     *   "Z"  → 26         - 1 = 25
     *   "AA" → 1×26 + 1   - 1 = 26
     *   "AB" → 1×26 + 2   - 1 = 27
     *   "AZ" → 1×26 + 26  - 1 = 51
     *   "BA" → 2×26 + 1   - 1 = 52
     *
     * Lưu ý: Công thức này PHẢI khớp với DataGenerator và SeatMapView.
     *
     * @param label Nhãn hàng (ví dụ: "A", "B", "AA").
     * @return Chỉ số 0-indexed tương ứng.
     */
    private int rowLabelToIndex(String label) {
        if (label == null || label.isEmpty()) return 0;
        int index = 0;
        for (int i = 0; i < label.length(); i++) {
            index = index * 26 + (label.charAt(i) - 'A' + 1);
        }
        return index - 1; // chuyển về 0-indexed
    }
}
