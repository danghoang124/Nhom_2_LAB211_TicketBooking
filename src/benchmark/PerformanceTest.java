package benchmark;

import repository.SeatRepository;
import model.entity.Seat;

import java.util.List;

/**
 * Benchmark for reading large CSV files — ensures Week 4 deliverable:
 * <b>Read file >= 10,000 rows in &lt; 500ms</b>.
 *
 * <p>This test measures the execution time of {@code findAll()} on {@code data/seats.csv}
 * (~30,000 rows) and prints detailed statistics.
 *
 * <p>Compile &amp; run (from project root):
 * <pre>
 *   javac -encoding UTF-8 -cp src -d out $(find src -name "*.java" ! -path "src/test/*")
 *   java -cp out benchmark.PerformanceTest
 * </pre>
 */
public class PerformanceTest {

    /** Maximum allowed threshold (deliverable). */
    private static final long MAX_MS = 500L;

    /** Number of JVM warm-up rounds before official measurement. */
    private static final int WARMUP_ROUNDS = 2;

    /** Number of official measurement rounds for averaging. */
    private static final int MEASURE_ROUNDS = 5;

    public static void main(String[] args) {
        System.out.println("========================================================");
        System.out.println("   LAB211 - Performance Benchmark (Week 4)              ");
        System.out.println("   Deliverable: Read >=10k rows seats.csv < 500ms       ");
        System.out.println("========================================================");
        System.out.println();

        SeatRepository repo = SeatRepository.getInstance();

        // -- Warm-up (JVM JIT compile) ------------------------------------------
        System.out.printf("  [Warm-up] Running %d rounds for JVM JIT compilation...%n", WARMUP_ROUNDS);
        for (int i = 0; i < WARMUP_ROUNDS; i++) {
            List<Seat> warmup = repo.findAll();
            System.out.printf("    Warm-up %d: %,d records%n", i + 1, warmup.size());
        }
        System.out.println();

        // -- Official measurement ------------------------------------------------
        System.out.printf("  [Benchmark] Measuring %d rounds, calculating min/max/avg:%n", MEASURE_ROUNDS);
        long[] times = new long[MEASURE_ROUNDS];
        int recordCount = 0;

        for (int i = 0; i < MEASURE_ROUNDS; i++) {
            long start = System.currentTimeMillis();
            List<Seat> seats = repo.findAll();
            long end = System.currentTimeMillis();

            times[i] = end - start;
            recordCount = seats.size();

            System.out.printf("    Run %d: %,d records in %d ms%n",
                i + 1, recordCount, times[i]);
        }

        // -- Statistics ---------------------------------------------------------
        long min = Long.MAX_VALUE, max = Long.MIN_VALUE, sum = 0;
        for (long t : times) {
            if (t < min) min = t;
            if (t > max) max = t;
            sum += t;
        }
        double avg = (double) sum / MEASURE_ROUNDS;
        double throughput = recordCount / (avg / 1000.0); // records/second

        System.out.println();
        System.out.println("  -------------------------------------------------");
        System.out.printf("  %-25s %,d rows%n",    "Rows read:",            recordCount);
        System.out.printf("  %-25s %.1f ms%n",     "Average time:",         avg);
        System.out.printf("  %-25s %d ms%n",       "Min time:",             min);
        System.out.printf("  %-25s %d ms%n",       "Max time:",             max);
        System.out.printf("  %-25s %,.0f rows/s%n","Throughput:",           throughput);
        System.out.printf("  %-25s %d ms%n",       "Max threshold:",        MAX_MS);
        System.out.println("  -------------------------------------------------");

        // -- Check deliverable ---------------------------------------------------
        System.out.println();
        boolean rowRequirement = recordCount >= 10_000;
        boolean speedRequirement = avg < MAX_MS;

        printResult("Row count >= 10,000",
            rowRequirement,
            String.format("%,d rows", recordCount));

        printResult("Average time < 500ms",
            speedRequirement,
            String.format("%.1f ms", avg));

        printResult("Max time < 1000ms (worst-case)",
            max < 1000,
            String.format("%d ms", max));

        System.out.println();

        if (rowRequirement && speedRequirement) {
            System.out.println("  [PASS] WEEK 4 DELIVERABLE MET: Read " +
                String.format("%,d", recordCount) + " rows in " +
                String.format("%.1f", avg) + " ms < 500ms");
        } else {
            System.out.println("  [FAIL] DELIVERABLE NOT MET!");
            if (!rowRequirement)
                System.out.println("     -> Need at least 10,000 rows in seats.csv");
            if (!speedRequirement)
                System.out.printf("     -> Need < 500ms, currently %.1f ms%n", avg);
        }

        System.out.println();

        // -- Bonus: benchmark findByMatch for ALL matches -----------------------
        System.out.println("  [Bonus Benchmark] findByMatch for each match:");
        repository.MatchRepository matchRepo = repository.MatchRepository.getInstance();
        List<model.entity.Match> allMatches = matchRepo.findAll();

        if (allMatches.isEmpty()) {
            System.out.println("    (No matches found. Run DataGenerator first.)");
        } else {
            System.out.printf("    %-12s | %-8s | %-10s%n", "MATCH ID", "SEATS", "TIME (ms)");
            System.out.println("    " + "-".repeat(38));
            for (model.entity.Match match : allMatches) {
                long tMatch = System.currentTimeMillis();
                List<Seat> seats = repo.findByMatch(match.getMatchId());
                long elapsed = System.currentTimeMillis() - tMatch;
                System.out.printf("    %-12s | %-8d | %-10d%n",
                    match.getMatchId(), seats.size(), elapsed);
            }
        }

        System.out.println();
        System.out.println("========================================================");

        // Exit with non-zero code if deliverable not met
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
