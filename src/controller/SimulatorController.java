package controller;

import model.entity.Seat;
import model.enums.LockMechanism;
import model.enums.SeatStatus;
import repository.MatchRepository;
import repository.SeatRepository;
import repository.TicketRepository;
import repository.TransactionRepository;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class SimulatorController {

    private BookingController bookingController;
    private SeatRepository seatRepository;
    private TicketRepository ticketRepository;
    private TransactionRepository transactionRepository;
    private MatchRepository matchRepository;

    public SimulatorController(BookingController bookingController,
                                SeatRepository seatRepository,
                                TicketRepository ticketRepository,
                                TransactionRepository transactionRepository,
                                MatchRepository matchRepository) {
        this.bookingController = bookingController;
        this.seatRepository = seatRepository;
        this.ticketRepository = ticketRepository;
        this.transactionRepository = transactionRepository;
        this.matchRepository = matchRepository;
    }

    public List<Seat> getAvailableSeats(String matchId) {
        return seatRepository.findAvailableByMatch(matchId);
    }

    public void resetData(String matchId) {
        // Reset match status về SCHEDULED để simulation có thể đặt vé.
        // Nếu match đang ở COMPLETED/ONGOING, bookSeat() sẽ reject mọi request.
        java.util.Optional<model.entity.Match> matchOpt = matchRepository.findById(matchId);
        if (matchOpt.isPresent()) {
            model.entity.Match match = matchOpt.get();
            if (match.getStatus() != model.enums.MatchStatus.SCHEDULED) {
                match.setStatus(model.enums.MatchStatus.SCHEDULED);
                matchRepository.save(match);
            }
        }

        // Load TẤT CẢ seats (không chỉ theo matchId) để không ghi đè mất data của match khác.
        // Bug cũ: findByMatch() chỉ lấy một phần, saveAll() ghi lại toàn bộ file → xoá seats match khác.
        List<Seat> allSeats = seatRepository.findAll();
        for (Seat seat : allSeats) {
            if (matchId.equals(seat.getMatchId())) {
                // Reset ghế về trạng thái ban đầu cho benchmark
                seat.reset(SeatStatus.AVAILABLE, 0);
            }
        }
        seatRepository.saveAll(allSeats);

        ticketRepository.saveAll(
            ticketRepository.findByCondition(t -> !t.getMatchId().equals(matchId))
        );

        transactionRepository.saveAll(
            transactionRepository.findByCondition(t -> !t.getFanId().startsWith("FAN_SIM_"))
        );
    }

    public SimulationResult runSimulation(int numThreads, String matchId, String seatId, LockMechanism mechanism) {
        int poolSize = Math.min(numThreads, Runtime.getRuntime().availableProcessors() * 4);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numThreads; i++) {
            String fanId = "FAN_SIM_" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    try {
                        boolean ok = bookingController.bookSeat(fanId, matchId, seatId, mechanism);
                        if (ok) {
                            success.incrementAndGet();
                        } else {
                            failed.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failed.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    failed.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();

        try {
            doneLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long endTime = System.currentTimeMillis();
        long timeMs = endTime - startTime;

        executor.shutdown();

        int s = success.get();
        int f = failed.get();
        int db = s > 1 ? s - 1 : 0;

        SimulationResult result = new SimulationResult();
        result.mechanism = mechanism;
        result.threads = numThreads;
        result.success = s;
        result.failed = f;
        result.doubleBooked = db;
        result.timeMs = timeMs;
        result.tps = timeMs > 0 ? (double) s / timeMs * 1000 : 0;

        return result;
    }

    public void exportResultsToCsv(SimulationResult[] results) {
        String filePath = "data/simulation_results.csv";
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
            bw.write("mechanism,threads,success,failed,doubleBooked,timeMs,tps");
            bw.newLine();
            for (SimulationResult r : results) {
                if (r != null) {
                    bw.write(String.format("%s,%d,%d,%d,%d,%d,%.2f",
                            r.mechanism.name(), r.threads, r.success, r.failed,
                            r.doubleBooked, r.timeMs, r.tps));
                    bw.newLine();
                }
            }
            System.out.println("Done! Results exported to " + filePath);
        } catch (IOException e) {
            System.err.println("Error exporting results: " + e.getMessage());
        }
    }

    public static class SimulationResult {
        public LockMechanism mechanism;
        public int threads;
        public int success;
        public int failed;
        public int doubleBooked;
        public long timeMs;
        public double tps;
    }
}
