package benchmark;

import repository.SeatRepository;
import model.entity.Seat;

import java.util.List;

/**
 * Benchmark đọc file CSV lớn — đảm bảo deliverable Tuần 4:
 * <b>Đọc file ≥ 10,000 dòng trong &lt; 500ms</b>.
 *
 * <p>Test này đo thời gian {@code findAll()} trên {@code data/seats.csv}
 * (~30,000 dòng) và in ra thống kê chi tiết.
 *
 * <p>Compile &amp; chạy (từ thư mục gốc project):
 * <pre>
 *   javac -encoding UTF-8 -cp src -d out $(find src -name "*.java" ! -path "src/test/*")
 *   java -cp out benchmark.PerformanceTest
 * </pre>
 */
public class PerformanceTest {

    /** Ngưỡng tối đa cho phép (deliverable). */
    private static final long MAX_MS = 500L;

    /** Số lần warm-up JVM trước khi đo chính thức. */
    private static final int WARMUP_ROUNDS = 2;

    /** Số lần đo chính thức để lấy trung bình. */
    private static final int MEASURE_ROUNDS = 5;

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║   LAB211 · Performance Benchmark (T4)               ║");
        System.out.println("║   Deliverable: Đọc ≥10k dòng seats.csv < 500ms     ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        SeatRepository repo = SeatRepository.getInstance();

        // ── Warm-up (JVM JIT compile) ─────────────────────────────────────────
        System.out.printf("  [Warm-up] Chạy %d lần để JVM biên dịch JIT...%n", WARMUP_ROUNDS);
        for (int i = 0; i < WARMUP_ROUNDS; i++) {
            List<Seat> warmup = repo.findAll();
            System.out.printf("    Warm-up %d: %,d records%n", i + 1, warmup.size());
        }
        System.out.println();

        // ── Đo chính thức ─────────────────────────────────────────────────────
        System.out.printf("  [Benchmark] Đo %d lần, lấy min/max/avg:%n", MEASURE_ROUNDS);
        long[] times = new long[MEASURE_ROUNDS];
        int recordCount = 0;

        for (int i = 0; i < MEASURE_ROUNDS; i++) {
            long start = System.currentTimeMillis();
            List<Seat> seats = repo.findAll();
            long end = System.currentTimeMillis();

            times[i] = end - start;
            recordCount = seats.size();

            System.out.printf("    Run %d: %,d records trong %d ms%n",
                i + 1, recordCount, times[i]);
        }

        // ── Thống kê ─────────────────────────────────────────────────────────
        long min = Long.MAX_VALUE, max = Long.MIN_VALUE, sum = 0;
        for (long t : times) {
            if (t < min) min = t;
            if (t > max) max = t;
            sum += t;
        }
        double avg = (double) sum / MEASURE_ROUNDS;
        double throughput = recordCount / (avg / 1000.0); // records/second

        System.out.println();
        System.out.println("  ─────────────────────────────────────────────────────");
        System.out.printf("  %-25s %,d dòng%n",   "Số dòng đọc được:",    recordCount);
        System.out.printf("  %-25s %.1f ms%n",     "Thời gian trung bình:", avg);
        System.out.printf("  %-25s %d ms%n",       "Tối thiểu:",            min);
        System.out.printf("  %-25s %d ms%n",       "Tối đa:",               max);
        System.out.printf("  %-25s %,.0f dòng/s%n","Thông lượng:",          throughput);
        System.out.printf("  %-25s %d ms%n",       "Ngưỡng tối đa:",        MAX_MS);
        System.out.println("  ─────────────────────────────────────────────────────");

        // ── Kiểm tra deliverable ──────────────────────────────────────────────
        System.out.println();
        boolean rowRequirement = recordCount >= 10_000;
        boolean speedRequirement = avg < MAX_MS;

        printResult("Số dòng >= 10,000",
            rowRequirement,
            String.format("%,d dòng", recordCount));

        printResult("Thời gian trung bình < 500ms",
            speedRequirement,
            String.format("%.1f ms", avg));

        printResult("Tối đa < 1000ms (worst-case acceptable)",
            max < 1000,
            String.format("%d ms", max));

        System.out.println();

        if (rowRequirement && speedRequirement) {
            System.out.println("  ✅ DELIVERABLE TUẦN 4 ĐẠT: Đọc " +
                String.format("%,d", recordCount) + " dòng trong " +
                String.format("%.1f", avg) + " ms < 500ms ✓");
        } else {
            System.out.println("  ❌ DELIVERABLE CHƯA ĐẠT!");
            if (!rowRequirement)
                System.out.println("     → Cần ít nhất 10,000 dòng trong seats.csv");
            if (!speedRequirement)
                System.out.printf("     → Cần < 500ms, hiện tại %.1f ms%n", avg);
        }

        System.out.println();

        // ── Thêm: benchmark findByMatch (query lọc) ───────────────────────────
        System.out.println("  [Bonus Benchmark] findByMatch(\"MATCH001\"):");
        long t0 = System.currentTimeMillis();
        List<Seat> byMatch = repo.findByMatch("MATCH001");
        long matchTime = System.currentTimeMillis() - t0;
        System.out.printf("    → %,d ghế của MATCH001 trong %d ms%n", byMatch.size(), matchTime);

        System.out.println();

        // ── Benchmark findBySectionAndMatch ───────────────────────────────────
        System.out.println("  [Bonus Benchmark] findBySectionAndMatch(\"SEC001\", \"MATCH001\"):");
        long t1 = System.currentTimeMillis();
        List<Seat> bySectionMatch = repo.findBySectionAndMatch("SEC001", "MATCH001");
        long secMatchTime = System.currentTimeMillis() - t1;
        System.out.printf("    → %,d ghế VIP×MATCH001 trong %d ms%n", bySectionMatch.size(), secMatchTime);

        System.out.println();
        System.out.println("╚══════════════════════════════════════════════════════╝");

        // Exit code non-zero nếu không đạt
        if (!rowRequirement || !speedRequirement) System.exit(1);
    }

    private static void printResult(String label, boolean pass, String detail) {
        System.out.printf("  %s %-40s [%s]%n",
            pass ? "✅" : "❌",
            label + ":",
            detail
        );
    }
}
