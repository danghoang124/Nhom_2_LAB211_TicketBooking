package controller;

import model.entity.Seat;
import model.enums.LockMechanism;
import model.enums.SeatStatus;
import repository.MatchRepository;
import repository.SeatRepository;
import repository.TicketRepository;
import repository.TransactionRepository;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class SimulatorController {
    private final BookingController bookingController;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final TransactionRepository transactionRepository;
    private final MatchRepository matchRepository;

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

    public static class SimulationResult {
        public LockMechanism mechanism;
        public int threads;
        public int success;
        public int failed;
        public int doubleBooked;
        public long timeMs;
        public double tps;
    }

    public List<Seat> getAvailableSeats(String matchId) {
        return seatRepository.findAvailableByMatch(matchId);
    }

    public void resetData(String matchId) {
        List<Seat> allSeats = seatRepository.findByMatch(matchId);
        for (Seat seat : allSeats) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setVersion(0);
        }
        seatRepository.saveAll(allSeats);

        ticketRepository.saveAll(
            ticketRepository.findByCondition(t ->
                !t.getMatchId().equals(matchId)
            )
        );

        transactionRepository.saveAll(
            transactionRepository.findByCondition(t ->
                !t.getFanId().startsWith("FAN_SIM_")
            )
        );
    }

    public SimulationResult runSimulation(int numThreads, String matchId, String seatId, LockMechanism mechanism) {
        int poolSize = Math.min(numThreads, Runtime.getRuntime().availableProcessors() * 4);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numThreads; i++) {
            final String fanId = "FAN_SIM_" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    boolean result = bookingController.bookSeat(fanId, matchId, seatId, mechanism);
                    if (result) {
                        successCount.incrementAndGet();
                    } else {
                        failedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failedCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();

        try {
            endLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long endTime = System.currentTimeMillis();
        long timeMs = endTime - startTime;

        executor.shutdown();

        int success = successCount.get();
        int failed = failedCount.get();
        int doubleBooked = success > 1 ? success - 1 : 0;

        SimulationResult result = new SimulationResult();
        result.mechanism = mechanism;
        result.threads = numThreads;
        result.success = success;
        result.failed = failed;
        result.doubleBooked = doubleBooked;
        result.timeMs = timeMs;
        result.tps = timeMs > 0 ? (double) success / timeMs * 1000 : 0;

        return result;
    }
}
