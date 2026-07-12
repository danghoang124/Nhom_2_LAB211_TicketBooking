package view;

import model.entity.Match;
import model.entity.Seat;
import model.entity.Section;
import model.enums.SeatStatus;

import java.util.List;

/**
 * ============================================================
 * SEAT MAP VIEW — Tuần 6 (View Layer)
 * ============================================================
 * Nhiệm vụ: Hiển thị sơ đồ ghế ngồi dạng ASCII lên console.
 * Trách nhiệm của View (theo MVC):
 *   ✔ Nhận dữ liệu đã xử lý từ Controller (Seat[][], Section, ...).
 *   ✔ Định dạng và in dữ liệu ra màn hình.
 *   ✔ Nhận input từ người dùng (chọn trận, chọn khán đài).
 *   ✗ KHÔNG đọc/ghi file CSV.
 *   ✗ KHÔNG chứa logic nghiệp vụ (tính tiền, kiểm tra ghế...).
 * Ký hiệu ghế trong ASCII Seat Map:
 *   [ ]  → AVAILABLE (còn trống, có thể đặt)
 *   [X]  → BOOKED    (đã bán, không thể đặt)
 *   [L]  → LOCKED    (đang xử lý giao dịch khác)
 *   [.]  → null (ô không có ghế trong dữ liệu)
 * Ví dụ hiển thị (Section VIP, 3 hàng × 4 ghế):
 *   +------------------+
 *   |  HƯỚNG SÂN BÓNG  |
 *   +------------------+
 *         1    2    3    4
 *   A  [ ] [ ] [ ] [ ]  A
 *   B  [ ] [X] [ ] [X]  B
 *   C  [X] [X] [X] [X]  C
 */
public class SeatMapView {

    // ── CONSTANTS ─────────────────────────────────────────────────────────────

    // Ký hiệu hiển thị cho từng trạng thái ghế
    private static final String SYMBOL_AVAILABLE = "[ ]";
    private static final String SYMBOL_BOOKED    = "[X]";
    private static final String SYMBOL_LOCKED    = "[L]";
    private static final String SYMBOL_EMPTY     = "[.]";

    // ── PUBLIC METHODS ────────────────────────────────────────────────────────

    /**
     * Hiển thị danh sách trận đấu để người dùng chọn.
     * Ví dụ output:
     *   === DANH SÁCH TRẬN ĐẤU ===
     *   [1] MATCH001 | Hà Nội FC vs Hoàng Anh Gia Lai | 2025-01-22 16:00
     *   [2] MATCH002 | Nam Định FC vs Thanh Hóa FC     | 2025-01-29 18:00
     *
     * @param matches Danh sách trận đấu từ StadiumController.getMatches().
     */
    public static void displayMatchList(List<Match> matches) {
        System.out.println("\n╔══════════════════════════════════════════════════════╗");
        System.out.println("║         MATCHES CURRENTLY OPEN FOR BOOKING           ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");

        if (matches == null || matches.isEmpty()) {
            System.out.println("  [!] There are currently no matches open for booking.");
            return;
        }

        System.out.printf("  %-4s %-12s %-38s %-10s%n", "No.", "Match ID", "Teams", "Date & Time");
        System.out.println("  " + "-".repeat(68));

        for (int i = 0; i < matches.size(); i++) {
            Match m = matches.get(i);
            String matchInfo = m.getHomeTeam() + " vs " + m.getAwayTeam();
            String timeInfo  = m.getMatchDate() + " " + m.getMatchTime();
            System.out.printf("  [%-2d] %-12s %-38s %-16s%n",
                    (i + 1), m.getMatchId(), matchInfo, timeInfo);
        }
        System.out.println();
    }

    /**
     * Hiển thị danh sách khán đài để người dùng chọn.
     *
     * Ví dụ output:
     *   === DANH SÁCH KHÁN ĐÀI ===
     *   [1] SEC001 | VIP        | 200 ghế | 500,000 VND/vé
     *   [2] SEC002 | STANDARD   | 600 ghế | 200,000 VND/vé
     *
     * @param sections Danh sách khán đài từ StadiumController.getSections().
     */
    public static void displaySectionList(List<Section> sections) {
        System.out.println("\n╔══════════════════════════════════════════════════════╗");
        System.out.println("║                 AVAILABLE SECTIONS                   ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");

        if (sections == null || sections.isEmpty()) {
            System.out.println("  [!] No sections found in the system.");
            return;
        }

        System.out.printf("  %-4s %-8s %-14s %-12s %-20s%n",
                "No.", "Sec ID", "Type", "Capacity", "Price");
        System.out.println("  " + "-".repeat(62));

        for (int i = 0; i < sections.size(); i++) {
            Section s = sections.get(i);
            System.out.printf("  [%-2d] %-8s %-14s %-12d %,d VND%n",
                    (i + 1),
                    s.getSectionId(),
                    s.getSectionType().name(),
                    s.getTotalCapacity(),
                    s.getBasePrice());
        }
        System.out.println();
    }

    /**
     * Hiển thị sơ đồ ghế ngồi dạng ASCII lên console.
     *
     * Cách hoạt động:
     *   1. Nhận Seat[][] đã được buildSeatMap() trong Controller tạo ra.
     *   2. In tiêu đề khán đài (tên, kích thước, tổng ghế).
     *   3. In banner "HƯỚNG SÂN BÓNG" phía trên.
     *   4. In số cột (1, 2, 3, ...) làm header.
     *   5. Với mỗi hàng r: in nhãn hàng (A, B, ...) rồi in ký hiệu từng ghế.
     *   6. In số ghế còn trống.
     *
     * @param seatMap       Mảng 2 chiều Seat[hàng][cột] từ StadiumController.buildSeatMap().
     * @param section       Section tương ứng để lấy tên và kích thước.
     * @param matchId       ID trận đấu để hiển thị thêm thông tin.
     * @param availableCount Số ghế còn trống (từ StadiumController.showAvailableSeats()).
     */
    public static void displaySeatMap(Seat[][] seatMap, Section section,
                                       String matchId, int availableCount) {
        if (seatMap == null || section == null) {
            System.out.println("[!] No seat map data available to display.");
            return;
        }

        int rows = section.getTotalRows();
        int cols = section.getSeatsPerRow();

        // ── Header ───────────────────────────────────────────────────────────
        System.out.println("\n" + "=".repeat(60));
        System.out.printf("  SEAT MAP - SECTION: %s (%s) | Match: %s%n",
                section.getSectionId(), section.getSectionType().name(), matchId);
        System.out.printf("  Configuration: %d rows × %d seats/row (Total: %d seats)%n",
                rows, cols, section.getTotalCapacity());
        System.out.printf("  Available seats: %d / %d%n", availableCount, section.getTotalCapacity());
        System.out.println("=".repeat(60));

        // ── Chú thích ký hiệu ─────────────────────────────────────────────────
        System.out.printf("  Legend:   %s Available   %s Booked   %s Locked%n%n",
                SYMBOL_AVAILABLE, SYMBOL_BOOKED, SYMBOL_LOCKED);

        // ── Banner sân bóng ───────────────────────────────────────────────────
        printFieldBanner(cols);

        // ── Header số cột ─────────────────────────────────────────────────────
        System.out.print("       ");
        for (int col = 1; col <= cols; col++) {
            System.out.printf(" %2d ", col);
        }
        System.out.println("\n");

        // ── Từng hàng ghế ─────────────────────────────────────────────────────
        for (int r = 0; r < rows; r++) {
            String rowLabel = indexToRowLabel(r); // 0→"A", 1→"B", 26→"AA"
            System.out.printf("  %-4s ", rowLabel); // nhãn hàng bên trái

            for (int c = 0; c < cols; c++) {
                Seat seat = seatMap[r][c];
                if (seat == null) {
                    System.out.print(" " + SYMBOL_EMPTY);
                } else {
                    System.out.print(" " + statusToSymbol(seat.getStatus()));
                }
            }

            System.out.printf("  %-4s%n", rowLabel); // nhãn hàng bên phải
        }

        System.out.println("\n" + "=".repeat(60) + "\n");
    }

    /**
     * Phiên bản đơn giản hơn — không cần matchId và availableCount.
     * Dùng cho JUnit Test hoặc khi chỉ cần xem sơ đồ nhanh.
     *
     * @param seatMap Mảng 2 chiều Seat[][].
     * @param section Section tương ứng.
     */
    public static void displaySeatMap(Seat[][] seatMap, Section section) {
        displaySeatMap(seatMap, section, "N/A", -1);
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────────

    /**
     * Chuyển trạng thái ghế thành ký hiệu ASCII để hiển thị.
     *
     * @param status Trạng thái ghế (AVAILABLE / BOOKED / LOCKED).
     * @return Chuỗi ký hiệu tương ứng.
     */
    private static String statusToSymbol(SeatStatus status) {
        if (status == null) return SYMBOL_EMPTY;
        switch (status) {
            case AVAILABLE: return SYMBOL_AVAILABLE;
            case BOOKED:    return SYMBOL_BOOKED;
            case LOCKED:    return SYMBOL_LOCKED;
            default:        return "[?]";
        }
    }

    /**
     * Chuyển chỉ số hàng 0-indexed thành nhãn hàng (A, B, ..., Z, AA, AB, ...).
     *
     * Đây là hàm NGHỊCH ĐẢO của rowLabelToIndex() trong StadiumController.
     * Hai hàm này PHẢI nhất quán với nhau:
     *   indexToRowLabel(0)  = "A"   ↔ rowLabelToIndex("A")  = 0
     *   indexToRowLabel(25) = "Z"   ↔ rowLabelToIndex("Z")  = 25
     *   indexToRowLabel(26) = "AA"  ↔ rowLabelToIndex("AA") = 26
     *   indexToRowLabel(27) = "AB"  ↔ rowLabelToIndex("AB") = 27
     *   indexToRowLabel(51) = "AZ"  ↔ rowLabelToIndex("AZ") = 51
     *   indexToRowLabel(52) = "BA"  ↔ rowLabelToIndex("BA") = 52
     *
     * Thuật toán: Giống chuyển số thập phân sang hệ cơ số 26 (nhưng A=1, không phải A=0).
     *
     * @param index Chỉ số 0-indexed (0 = hàng A, 1 = hàng B, ...).
     * @return Nhãn hàng tương ứng (String).
     */
    public static String indexToRowLabel(int index) {
        // Chuyển về 1-indexed để tính toán dễ hơn
        int n = index + 1;
        StringBuilder sb = new StringBuilder();
        while (n > 0) {
            n--;                                    // A=0..25 trong modulo
            sb.insert(0, (char) ('A' + (n % 26))); // lấy ký tự tương ứng
            n /= 26;
        }
        return sb.toString();
    }

    /**
     * Vẽ banner đại diện cho hướng nhìn về sân bóng / sân khấu.
     * Tự động điều chỉnh độ rộng theo số cột.
     *
     * Ví dụ (cols = 20):
     *   +--------------------------------------------+
     *   |         HƯỚNG NHÌN VỀ SÂN BÓNG             |
     *   +--------------------------------------------+
     *
     * @param cols Số cột (ghế/hàng) để tính độ rộng banner.
     */
    private static void printFieldBanner(int cols) {
        // Mỗi ghế chiếm 4 ký tự (" [ ]"), cộng thêm 7 ký tự prefix ("  A    ")
        int bannerWidth = cols * 4 + 7;
        String border   = "  +" + "-".repeat(bannerWidth - 4) + "+";
        String text     = "FIELD DIRECTION (STAGE)";

        // Căn giữa text
        int padTotal    = bannerWidth - 4 - text.length();
        int padLeft     = padTotal / 2;
        int padRight    = padTotal - padLeft;
        String middle   = "  |" + " ".repeat(padLeft) + text + " ".repeat(padRight) + "|";

        System.out.println(border);
        System.out.println(middle);
        System.out.println(border);
        System.out.println();
    }
}
