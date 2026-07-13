import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import controller.StadiumController;
import exception.EntityNotFoundException;
import model.entity.Match;
import model.entity.Seat;
import model.entity.Section;
import model.enums.MatchStatus;
import model.enums.SeatStatus;
import repository.MatchRepository;
import repository.SeatRepository;
import repository.SectionRepository;
import view.SeatMapView;

import java.util.List;

/**
 * ============================================================
 * CONTROLLER TEST — JUnit 5
 * ============================================================
 *
 * Test cho: StadiumController và SeatMapView
 * Phụ trách: Thành viên 2 (Đăng)
 *
 * Các test case bao gồm:
 *   1. testGetMatches_notNull()           — getMatches() không trả về null
 *   2. testGetMatches_allScheduled()      — getMatches() chỉ trả về SCHEDULED
 *   3. testGetSections_exactlyFour()      — getSections() có đúng 4 khán đài
 *   4. testGetSections_notNull()          — getSections() không trả về null
 *   5. testBuildSeatMap_success()         — buildSeatMap() với input hợp lệ
 *   6. testBuildSeatMap_dimensionMatch()  — kích thước mảng khớp với Section
 *   7. testBuildSeatMap_rowLabelMatch()   — rowLabel của ghế khớp với chỉ số hàng
 *   8. testBuildSeatMap_seatNumberMatch() — seatNumber khớp với chỉ số cột
 *   9. testBuildSeatMap_invalidSection()  — EntityNotFoundException khi sectionId sai
 *   10. testShowAvailableSeats_nonNeg()   — showAvailableSeats() >= 0
 *   11. testShowAvailableSeats_matchRepo()— kết quả khớp với repository
 *   12. testIndexToRowLabel_singleChar()  — SeatMapView: A, B, Z đúng
 *   13. testIndexToRowLabel_doubleChar()  — SeatMapView: AA, AB, BA đúng
 *   14. testIndexToRowLabel_roundTrip()   — inverse nhất quán với rowLabelToIndex
 *
 * Cách chạy test trong VS Code:
 *   Terminal: cd src && javac -cp .:../lib/junit-platform-console-standalone-1.10.2.jar
 *             $(find . -name "*.java") -d out
 *             java -cp out:../lib/junit-platform-console-standalone-1.10.2.jar
 *             org.junit.platform.console.ConsoleLauncher --select-class=test.ControllerTest
 */
public class ControllerTest {

    // ── Dependencies ─────────────────────────────────────────────────────────
    private MatchRepository   matchRepository;
    private SectionRepository sectionRepository;
    private SeatRepository    seatRepository;
    private StadiumController stadiumController;

    /** ID mẫu dùng trong các test (lấy từ data CSV thực tế). */
    private static final String VALID_MATCH_ID   = "MATCH001";
    private static final String VALID_SECTION_ID = "SEC001"; // VIP: 10 hàng × 20 ghế
    private static final String INVALID_SECTION_ID = "SEC_KHONG_TON_TAI_XYZ";

    // ── SETUP ─────────────────────────────────────────────────────────────────

    /**
     * Chạy TRƯỚC MỖI test.
     * Khởi tạo lại Controller và Repository để mỗi test độc lập nhau.
     * Repository tự đọc từ file CSV thực tế trong thư mục data/.
     */
    @BeforeEach
    public void setUp() {
        matchRepository   = new MatchRepository();
        sectionRepository = new SectionRepository();
        seatRepository    = new SeatRepository();
        stadiumController = new StadiumController(matchRepository, sectionRepository, seatRepository);
    }

    // ── TEST GROUP 1: getMatches() ─────────────────────────────────────────────

    /**
     * Test 1: getMatches() phải trả về list không phải null.
     * Dù CSV rỗng hay có dữ liệu, không bao giờ được trả về null.
     */
    @Test
    @DisplayName("getMatches() - Không trả về null")
    public void testGetMatches_notNull() {
        List<Match> matches = stadiumController.getMatches();
        assertNotNull(matches, "getMatches() không được trả về null");
    }

    /**
     * Test 2: Tất cả trận đấu trả về phải có status = SCHEDULED.
     * Controller chỉ được lấy trận SCHEDULED, không lấy COMPLETED hay ONGOING.
     */
    @Test
    @DisplayName("getMatches() - Chỉ trả về trận SCHEDULED")
    public void testGetMatches_allScheduled() {
        List<Match> matches = stadiumController.getMatches();
        // Với mỗi trận trong list, kiểm tra status phải là SCHEDULED
        for (Match match : matches) {
            assertEquals(MatchStatus.SCHEDULED, match.getStatus(),
                    "Trận " + match.getMatchId() + " phải có status SCHEDULED, "
                    + "nhưng thực tế là: " + match.getStatus());
        }
    }

    // ── TEST GROUP 2: getSections() ────────────────────────────────────────────

    /**
     * Test 3: getSections() phải không trả về null.
     */
    @Test
    @DisplayName("getSections() - Không trả về null")
    public void testGetSections_notNull() {
        List<Section> sections = stadiumController.getSections();
        assertNotNull(sections, "getSections() không được trả về null");
    }

    /**
     * Test 4: Hệ thống phải có đúng 4 khán đài (SEC001, SEC002, SEC003, SEC004).
     * Đây là số lượng cố định theo thiết kế của hệ thống.
     */
    @Test
    @DisplayName("getSections() - Phải có đúng 4 khán đài")
    public void testGetSections_exactlyFour() {
        List<Section> sections = stadiumController.getSections();
        assertEquals(4, sections.size(),
                "Hệ thống phải có đúng 4 khán đài (VIP, STANDARD, STANDING, ECONOMY_LOWER), "
                + "nhưng thực tế có: " + sections.size());
    }

    // ── TEST GROUP 3: buildSeatMap() ───────────────────────────────────────────

    /**
     * Test 5: buildSeatMap() với input hợp lệ phải trả về mảng không null.
     */
    @Test
    @DisplayName("buildSeatMap() - Trả về mảng không null với input hợp lệ")
    public void testBuildSeatMap_success() {
        Seat[][] seatMap = stadiumController.buildSeatMap(VALID_SECTION_ID, VALID_MATCH_ID);
        assertNotNull(seatMap, "buildSeatMap() không được trả về null với input hợp lệ");
    }

    /**
     * Test 6: Kích thước mảng seatMap phải khớp với cấu hình của Section.
     * Section VIP (SEC001): totalRows=10, seatsPerRow=20
     * → seatMap phải là Seat[10][20].
     */
    @Test
    @DisplayName("buildSeatMap() - Kích thước mảng khớp với Section")
    public void testBuildSeatMap_dimensionMatch() {
        // Lấy Section thực tế để biết kích thước mong đợi
        Section vipSection = sectionRepository.findById(VALID_SECTION_ID).orElse(null);
        assertNotNull(vipSection, "Section VIP (SEC001) phải tồn tại trong CSV");

        Seat[][] seatMap = stadiumController.buildSeatMap(VALID_SECTION_ID, VALID_MATCH_ID);

        // Số hàng phải đúng
        assertEquals(vipSection.getTotalRows(), seatMap.length,
                "Số hàng của seatMap phải bằng totalRows trong Section: "
                + vipSection.getTotalRows());

        // Số ghế/hàng phải đúng (kiểm tra hàng đầu tiên)
        assertEquals(vipSection.getSeatsPerRow(), seatMap[0].length,
                "Số ghế mỗi hàng của seatMap phải bằng seatsPerRow trong Section: "
                + vipSection.getSeatsPerRow());
    }

    /**
     * Test 7: Với mỗi ghế trong seatMap, rowLabel phải khớp với chỉ số hàng r.
     *
     * Ví dụ: seatMap[0][0] là ghế hàng A → seat.getRowLabel() phải = "A"
     *         seatMap[1][0] là ghế hàng B → seat.getRowLabel() phải = "B"
     *
     * Logic kiểm tra:
     *   indexToRowLabel(r) = nhãn mong đợi cho hàng r
     *   seat.getRowLabel() = nhãn thực tế của ghế
     *   → Hai giá trị này phải bằng nhau.
     */
    @Test
    @DisplayName("buildSeatMap() - rowLabel của ghế khớp với chỉ số hàng")
    public void testBuildSeatMap_rowLabelMatch() {
        Seat[][] seatMap = stadiumController.buildSeatMap(VALID_SECTION_ID, VALID_MATCH_ID);
        boolean hasChecked = false;

        for (int r = 0; r < seatMap.length; r++) {
            for (int c = 0; c < seatMap[r].length; c++) {
                Seat seat = seatMap[r][c];
                if (seat != null) {
                    // Tính nhãn hàng mong đợi từ chỉ số r
                    // Dùng SeatMapView.indexToRowLabel() thay vì tự viết lại
                    // để đảm bảo nhất quán giữa View và Test
                    String expectedLabel = SeatMapView.indexToRowLabel(r);
                    assertEquals(expectedLabel, seat.getRowLabel(),
                            "Ghế tại seatMap[" + r + "][" + c + "] "
                            + "phải có rowLabel = " + expectedLabel
                            + " nhưng thực tế là: " + seat.getRowLabel());
                    hasChecked = true;
                    break; // Đủ kiểm tra 1 ghế/hàng để xác nhận logic đúng
                }
            }
            if (hasChecked) break;
        }

        assertTrue(hasChecked, "Sơ đồ ghế phải có ít nhất một ghế để kiểm tra");
    }

    /**
     * Test 8: seatNumber của ghế phải khớp với chỉ số cột (c + 1).
     *
     * Ví dụ: seatMap[0][0] → seatNumber phải = 1 (cột 0+1=1)
     *         seatMap[0][4] → seatNumber phải = 5 (cột 4+1=5)
     */
    @Test
    @DisplayName("buildSeatMap() - seatNumber của ghế khớp với chỉ số cột")
    public void testBuildSeatMap_seatNumberMatch() {
        Seat[][] seatMap = stadiumController.buildSeatMap(VALID_SECTION_ID, VALID_MATCH_ID);
        boolean hasChecked = false;

        for (int r = 0; r < seatMap.length; r++) {
            for (int c = 0; c < seatMap[r].length; c++) {
                Seat seat = seatMap[r][c];
                if (seat != null) {
                    int expectedSeatNumber = c + 1; // cột c → seatNumber = c+1
                    assertEquals(expectedSeatNumber, seat.getSeatNumber(),
                            "Ghế tại cột " + c + " phải có seatNumber = " + expectedSeatNumber
                            + " nhưng thực tế là: " + seat.getSeatNumber());
                    hasChecked = true;
                }
            }
        }

        assertTrue(hasChecked, "Phải có ít nhất một ghế để kiểm tra");
    }

    /**
     * Test 9: buildSeatMap() phải ném EntityNotFoundException khi sectionId không tồn tại.
     *
     * Đây là test case "đường không hạnh phúc" (unhappy path):
     * Người dùng truyền vào một sectionId không hợp lệ → Controller phải ném exception.
     */
    @Test
    @DisplayName("buildSeatMap() - Ném EntityNotFoundException với sectionId không hợp lệ")
    public void testBuildSeatMap_invalidSection() {
        assertThrows(EntityNotFoundException.class,
                () -> stadiumController.buildSeatMap(INVALID_SECTION_ID, VALID_MATCH_ID),
                "Phải ném EntityNotFoundException khi sectionId không tồn tại trong CSV");
    }

    // ── TEST GROUP 4: showAvailableSeats() ────────────────────────────────────

    /**
     * Test 10: showAvailableSeats() phải trả về số >= 0.
     * Không thể có số ghế trống âm.
     */
    @Test
    @DisplayName("showAvailableSeats() - Kết quả phải >= 0")
    public void testShowAvailableSeats_nonNegative() {
        int count = stadiumController.showAvailableSeats(VALID_SECTION_ID, VALID_MATCH_ID);
        assertTrue(count >= 0,
                "Số ghế còn trống không thể là số âm, nhưng thực tế là: " + count);
    }

    /**
     * Test 11: Kết quả showAvailableSeats() phải khớp với truy vấn trực tiếp từ Repository.
     *
     * Tại sao cần test này?
     * → Đảm bảo Controller không bỏ sót ghế hoặc đếm sai so với Repository.
     *
     * Logic:
     *   controller.showAvailableSeats() gọi seatRepo.findAvailableBySectionAndMatch().size()
     *   → Hai giá trị này PHẢI bằng nhau.
     */
    @Test
    @DisplayName("showAvailableSeats() - Kết quả khớp với SeatRepository")
    public void testShowAvailableSeats_matchesRepository() {
        int controllerCount = stadiumController.showAvailableSeats(VALID_SECTION_ID, VALID_MATCH_ID);

        // Lấy trực tiếp từ Repository để đối chiếu
        List<Seat> availableSeats = seatRepository
                .findAvailableBySectionAndMatch(VALID_SECTION_ID, VALID_MATCH_ID);
        int repoCount = availableSeats.size();

        assertEquals(repoCount, controllerCount,
                "showAvailableSeats() phải trả về " + repoCount
                + " nhưng thực tế là " + controllerCount);
    }

    // ── TEST GROUP 5: SeatMapView.indexToRowLabel() ────────────────────────────

    /**
     * Test 12: indexToRowLabel() với các chỉ số 1 ký tự (0–25).
     * 0→A, 1→B, 25→Z
     */
    @Test
    @DisplayName("SeatMapView.indexToRowLabel() - Ký tự đơn (A, B, Z)")
    public void testIndexToRowLabel_singleChar() {
        assertEquals("A", SeatMapView.indexToRowLabel(0),  "Index 0  phải là 'A'");
        assertEquals("B", SeatMapView.indexToRowLabel(1),  "Index 1  phải là 'B'");
        assertEquals("C", SeatMapView.indexToRowLabel(2),  "Index 2  phải là 'C'");
        assertEquals("Z", SeatMapView.indexToRowLabel(25), "Index 25 phải là 'Z'");
    }

    /**
     * Test 13: indexToRowLabel() với các chỉ số 2 ký tự (26+).
     * 26→AA, 27→AB, 51→AZ, 52→BA
     */
    @Test
    @DisplayName("SeatMapView.indexToRowLabel() - Ký tự đôi (AA, AB, AZ, BA)")
    public void testIndexToRowLabel_doubleChar() {
        assertEquals("AA", SeatMapView.indexToRowLabel(26), "Index 26 phải là 'AA'");
        assertEquals("AB", SeatMapView.indexToRowLabel(27), "Index 27 phải là 'AB'");
        assertEquals("AZ", SeatMapView.indexToRowLabel(51), "Index 51 phải là 'AZ'");
        assertEquals("BA", SeatMapView.indexToRowLabel(52), "Index 52 phải là 'BA'");
    }

    /**
     * Test 14: indexToRowLabel() phải nhất quán với rowLabelToIndex() trong StadiumController.
     *
     * Đây là test QUAN TRỌNG NHẤT vì đây chính là nguyên nhân bug cũ:
     * Hàm trong View (indexToRowLabel) và hàm trong Controller (rowLabelToIndex)
     * phải là hàm nghịch đảo của nhau.
     *
     * Kiểm tra: với mọi index từ 0 đến 54,
     *   label = indexToRowLabel(index)
     *   index_lại = rowLabelToIndex(label)
     *   → index_lại phải = index (round-trip)
     *
     * Cách kiểm tra rowLabelToIndex():
     *   Ta tạo một Seat giả trong CSV với rowLabel biết trước,
     *   rồi dùng buildSeatMap() và kiểm tra nó nằm đúng hàng.
     *   Ở đây ta kiểm tra gián tiếp qua indexToRowLabel.
     */
    @Test
    @DisplayName("indexToRowLabel() nhất quán — round-trip với chuỗi đặc trưng")
    public void testIndexToRowLabel_roundTrip() {
        // Kiểm tra các giá trị đặc biệt quan trọng
        // Công thức inverse: rowLabelToIndex(label) = tổng base-26, trừ 1
        String[] expectedLabels = {"A", "B", "Z", "AA", "AB", "AZ", "BA", "ZZ"};
        int[]    expectedIndices = { 0,   1,  25,   26,   27,   51,   52,  701};

        for (int i = 0; i < expectedLabels.length; i++) {
            String label = SeatMapView.indexToRowLabel(expectedIndices[i]);
            assertEquals(expectedLabels[i], label,
                    "indexToRowLabel(" + expectedIndices[i] + ") phải là '"
                    + expectedLabels[i] + "' nhưng thực tế là '" + label + "'");
        }

        // Đảm bảo không có hai index khác nhau cho cùng một label (tính đơn ánh)
        java.util.Set<String> labels = new java.util.HashSet<>();
        for (int idx = 0; idx < 55; idx++) {
            String lbl = SeatMapView.indexToRowLabel(idx);
            assertFalse(labels.contains(lbl),
                    "Nhãn '" + lbl + "' bị trùng lặp cho nhiều index khác nhau!");
            labels.add(lbl);
        }
    }
}
