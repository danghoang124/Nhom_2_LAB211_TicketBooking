package controller;

import model.enums.LockMechanism;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class SimulatorController {
    private final BookingController bookingController;

    public SimulatorController(BookingController bookingController) {
        this.bookingController = bookingController;
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

    public SimulationResult runSimulation(int numThreads, String matchId, String seatId, LockMechanism mechanism) {
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numThreads; i++) {
            final String fanId = "FAN_SIM_" + i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Chờ lệnh bắt đầu
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

        // Bắt đầu chạy tất cả các threads
        startLatch.countDown();

        try {
            endLatch.await(); // Chờ tất cả hoàn thành
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long endTime = System.currentTimeMillis();
        long timeMs = endTime - startTime;

        executor.shutdown();

        int success = successCount.get();
        int failed = failedCount.get();
        // Số vé vượt quá 1 là double booked
        int doubleBooked = success > 1 ? success - 1 : 0;
        
        SimulationResult result = new SimulationResult();
        result.mechanism = mechanism;
        result.threads = numThreads;
        result.success = success;
        result.failed = failed;
        result.doubleBooked = doubleBooked;
        result.timeMs = timeMs;
        result.tps = timeMs > 0 ? (double) (numThreads) / timeMs * 1000 : 0;

        return result;
    }
}
