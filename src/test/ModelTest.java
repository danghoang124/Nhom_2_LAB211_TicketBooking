package test;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import model.entity.*;
import model.enums.*;
import repository.CsvRepository;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * JUnit 5 Unit Tests for Model Layer (T3).
 */
public class ModelTest {

    @Test
    public void testEnums() {
        // SeatStatus
        assertEquals(SeatStatus.AVAILABLE, SeatStatus.fromString("AVAILABLE"));
        assertEquals(SeatStatus.BOOKED, SeatStatus.fromString("booked"));
        assertEquals(SeatStatus.LOCKED, SeatStatus.fromString("  LOCKED  "));

        // MatchStatus
        assertEquals(MatchStatus.SCHEDULED, MatchStatus.fromString("SCHEDULED"));
        assertEquals(MatchStatus.COMPLETED, MatchStatus.fromString("completed"));

        // TicketStatus
        assertEquals(TicketStatus.VALID, TicketStatus.fromString("VALID"));
        assertEquals(TicketStatus.CANCELLED, TicketStatus.fromString("CANCELLED"));

        // TransactionStatus
        assertEquals(TransactionStatus.SUCCESS, TransactionStatus.fromString("success"));
        assertEquals(TransactionStatus.PARTIAL, TransactionStatus.fromString("PARTIAL"));
        assertEquals(TransactionStatus.FAILED, TransactionStatus.fromString("FAILED"));

        // SectionType
        assertEquals(SectionType.VIP, SectionType.fromString("VIP"));
        assertEquals(SectionType.ECONOMY_LOWER, SectionType.fromString("economy_lower"));

        // LockMechanism
        assertEquals(LockMechanism.NO_LOCK, LockMechanism.fromString("NO_LOCK"));
        assertEquals(LockMechanism.OPTIMISTIC, LockMechanism.fromString("OPTIMISTIC"));
        assertEquals(LockMechanism.SYNCHRONIZED, LockMechanism.fromString("synchronized"));

        // Kiểm tra IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            SeatStatus.fromString("INVALID_VALUE");
        });
    }

    @Test
    public void testSplitCsvLine() {
        String[] f1 = BaseEntity.splitCsvLine("A,B,C");
        assertEquals(3, f1.length);
        assertEquals("A", f1[0]);
        assertEquals("B", f1[1]);
        assertEquals("C", f1[2]);

        String[] f2 = BaseEntity.splitCsvLine("A,,C");
        assertEquals("", f2[1]);

        String[] f3 = BaseEntity.splitCsvLine("STD001,\"Phường Mỹ Đình II, Nam Từ Liêm\",40192");
        assertEquals(3, f3.length);
        assertEquals("Phường Mỹ Đình II, Nam Từ Liêm", f3[1]);

        String[] f4 = BaseEntity.splitCsvLine("A,\"He said \"\"Hello\"\"\",B");
        assertEquals("He said \"Hello\"", f4[1]);

        String[] f5 = BaseEntity.splitCsvLine("single");
        assertEquals(1, f5.length);
        assertEquals("single", f5[0]);
    }

    @Test
    public void testJoinAndSplit() {
        String line1 = joinCsv("STD001", "Hà Nội", 40192);
        assertEquals("STD001,Hà Nội,40192", line1);

        String line2 = joinCsv("STD001", "Phường Mỹ Đình II, Nam Từ Liêm", 40192);
        String[] parsed = BaseEntity.splitCsvLine(line2);
        assertEquals("Phường Mỹ Đình II, Nam Từ Liêm", parsed[1]);

        String line3 = joinCsv("A", null, "C");
        String[] f3 = BaseEntity.splitCsvLine(line3);
        assertEquals("", f3[1]);
    }

    @Test
    public void testStadiumRoundTrip() {
        Stadium s = new Stadium("STD001", "Sân Vận Động Mỹ Đình", "Hà Nội",
                "Phường Mỹ Đình II, Nam Từ Liêm, Hà Nội", 40192);
        String csv = s.toCsvLine();
        Stadium s2 = Stadium.fromCsvLine(csv);

        assertEquals("STD001", s2.getStadiumId());
        assertEquals("Sân Vận Động Mỹ Đình", s2.getName());
        assertEquals("Hà Nội", s2.getCity());
        assertEquals("Phường Mỹ Đình II, Nam Từ Liêm, Hà Nội", s2.getAddress());
        assertEquals(40192, s2.getTotalCapacity());
    }

    @Test
    public void testSectionRoundTrip() {
        Section s = new Section("SEC001", SectionType.VIP, 10, 20, 500_000L);
        Section s2 = Section.fromCsvLine(s.toCsvLine());

        assertEquals("SEC001", s2.getSectionId());
        assertEquals(SectionType.VIP, s2.getSectionType());
        assertEquals(10, s2.getTotalRows());
        assertEquals(20, s2.getSeatsPerRow());
        assertEquals(500_000L, s2.getBasePrice());
        assertEquals(200, s2.getTotalCapacity());
    }

    @Test
    public void testMatchRoundTrip() {
        Match m = new Match("MATCH001", "STD001", "Hà Nội FC", "Hoàng Anh Gia Lai",
                "2025-01-22", "19:30", MatchStatus.SCHEDULED);
        Match m2 = Match.fromCsvLine(m.toCsvLine());

        assertEquals("MATCH001", m2.getMatchId());
        assertEquals("STD001", m2.getStadiumId());
        assertEquals("Hà Nội FC", m2.getHomeTeam());
        assertEquals("Hoàng Anh Gia Lai", m2.getAwayTeam());
        assertEquals("2025-01-22", m2.getMatchDate());
        assertEquals("19:30", m2.getMatchTime());
        assertEquals(MatchStatus.SCHEDULED, m2.getStatus());
        assertEquals("Hà Nội FC vs Hoàng Anh Gia Lai", m2.getTitle());
    }

    @Test
    public void testSeatRoundTrip() {
        Seat s = new Seat("SEAT000001", "SEC001", "MATCH001", "A", 1, SeatStatus.AVAILABLE, 0);
        Seat s2 = Seat.fromCsvLine(s.toCsvLine());

        assertEquals("SEAT000001", s2.getSeatId());
        assertEquals("SEC001", s2.getSectionId());
        assertEquals("MATCH001", s2.getMatchId());
        assertEquals("A", s2.getRowLabel());
        assertEquals(1, s2.getSeatNumber());
        assertEquals(SeatStatus.AVAILABLE, s2.getStatus());
        assertEquals(0, s2.getVersion());
        assertTrue(s2.isAvailable());
        assertEquals("A-1", s2.getLabel());
    }

    @Test
    public void testFanRoundTrip() {
        Fan f = new Fan("FAN0001", "anv", "HASH123ABC", "Nguyễn Văn An",
                "anv@gmail.com", "0912345678", "2024-03-15 09:22:00", true);
        Fan f2 = Fan.fromCsvLine(f.toCsvLine());

        assertEquals("FAN0001", f2.getFanId());
        assertEquals("anv", f2.getUsername());
        assertEquals("HASH123ABC", f2.getPasswordHash());
        assertEquals("Nguyễn Văn An", f2.getFullName());
        assertEquals("anv@gmail.com", f2.getEmail());
        assertEquals("0912345678", f2.getPhone());
        assertEquals("2024-03-15 09:22:00", f2.getCreatedAt());
        assertTrue(f2.isActive());
        assertTrue(f2.checkPassword("HASH123ABC"));
        assertFalse(f2.checkPassword("WRONG"));
    }

    @Test
    public void testTicketRoundTrip() {
        Ticket t = new Ticket("TKT00000001", "FAN0001", "SEAT000123", "MATCH001",
                "TXN00000001", 500_000L, "2025-01-22 20:15:30", TicketStatus.VALID);
        Ticket t2 = Ticket.fromCsvLine(t.toCsvLine());

        assertEquals("TKT00000001", t2.getTicketId());
        assertEquals("FAN0001", t2.getFanId());
        assertEquals("SEAT000123", t2.getSeatId());
        assertEquals("MATCH001", t2.getMatchId());
        assertEquals("TXN00000001", t2.getTransactionId());
        assertEquals(500_000L, t2.getPrice());
        assertEquals("2025-01-22 20:15:30", t2.getBookedAt());
        assertEquals(TicketStatus.VALID, t2.getStatus());
        assertTrue(t2.isValid());
    }

    @Test
    public void testBookingTransactionRoundTrip() {
        BookingTransaction tx = new BookingTransaction(
                "TXN00000001", "FAN0001", "MATCH001", 2, 1_000_000L,
                TransactionStatus.SUCCESS, LockMechanism.SYNCHRONIZED,
                "2025-01-22 20:15:30", 45L);
        BookingTransaction tx2 = BookingTransaction.fromCsvLine(tx.toCsvLine());

        assertEquals("TXN00000001", tx2.getTransactionId());
        assertEquals("FAN0001", tx2.getFanId());
        assertEquals("MATCH001", tx2.getMatchId());
        assertEquals(2, tx2.getNumberOfTickets());
        assertEquals(1_000_000L, tx2.getTotalAmount());
        assertEquals(TransactionStatus.SUCCESS, tx2.getStatus());
        assertEquals(LockMechanism.SYNCHRONIZED, tx2.getMechanism());
        assertEquals("2025-01-22 20:15:30", tx2.getCreatedAt());
        assertEquals(45L, tx2.getDurationMs());
        assertTrue(tx2.isSuccessful());
    }

    @Test
    public void testRepositoryCRUD() {
        String testFile = "data/test_stadiums_temp.csv";
        try {
            CsvRepository<Stadium> repo = new CsvRepository<>() {
                @Override
                public String getFilePath() {
                    return testFile;
                }

                @Override
                public String getCsvHeader() {
                    return "stadiumId,name,city,address,totalCapacity";
                }

                @Override
                protected Stadium parseFromCsvLine(String line) {
                    return Stadium.fromCsvLine(line);
                }
            };

            assertEquals(0, repo.findAll().size());

            Stadium s1 = new Stadium("STD001", "Sân Mỹ Đình", "Hà Nội", "Mỹ Đình", 40192);
            Stadium s2 = new Stadium("STD002", "Sân Thống Nhất", "TP.HCM", "Thủ Đức", 15000);
            repo.save(s1);
            repo.save(s2);
            assertEquals(2, repo.count());

            Optional<Stadium> found = repo.findById("STD001");
            assertTrue(found.isPresent());
            assertEquals("Sân Mỹ Đình", found.get().getName());

            Optional<Stadium> notFound = repo.findById("STD999");
            assertFalse(notFound.isPresent());

            List<Stadium> hcm = repo.findByCondition(s -> "TP.HCM".equals(s.getCity()));
            assertEquals(1, hcm.size());
            assertEquals("STD002", hcm.get(0).getStadiumId());

            Stadium s1Updated = new Stadium("STD001", "Sân Mỹ Đình (Updated)", "Hà Nội", "Mỹ Đình", 50000);
            repo.save(s1Updated);
            assertEquals(2, repo.count());
            assertEquals("Sân Mỹ Đình (Updated)", repo.findById("STD001").get().getName());

            boolean deleted = repo.deleteById("STD002");
            assertTrue(deleted);
            assertEquals(1, repo.count());

            boolean deletedAgain = repo.deleteById("STD002");
            assertFalse(deletedAgain);

            Stadium s3 = new Stadium("STD003", "Sân Pleiku", "Gia Lai", "Pleiku", 12000);
            repo.append(s3);
            assertEquals(2, repo.count());

            assertTrue(repo.existsById("STD001"));
            assertFalse(repo.existsById("STD999"));
        } finally {
            new File(testFile).delete();
        }
    }

    @Test
    public void testCommaInField() {
        Stadium s = new Stadium("STD001", "Sân Vận Động Mỹ Đình", "Hà Nội",
                "Phường Mỹ Đình II, Nam Từ Liêm, Hà Nội", 40192);
        String csv = s.toCsvLine();
        Stadium s2 = Stadium.fromCsvLine(csv);
        assertEquals("Phường Mỹ Đình II, Nam Từ Liêm, Hà Nội", s2.getAddress());
    }

    @Test
    public void testQuotedField() {
        Fan f = new Fan("FAN0001", "user1", "HASH", "Nguyễn \"Quân\" Văn",
                "user@email.com", "0912345678", "2024-01-01 00:00:00", true);
        String csv = f.toCsvLine();
        Fan f2 = Fan.fromCsvLine(csv);
        assertEquals("Nguyễn \"Quân\" Văn", f2.getFullName());
    }

    @Test
    public void testSeatOptimisticLocking() {
        Seat seat = new Seat("SEAT000001", "SEC001", "MATCH001", "A", 1, SeatStatus.AVAILABLE, 0);
        assertEquals(0, seat.getVersion());

        seat.updateStatus(SeatStatus.LOCKED);
        assertEquals(1, seat.getVersion());
        assertEquals(SeatStatus.LOCKED, seat.getStatus());

        seat.updateStatus(SeatStatus.BOOKED);
        assertEquals(2, seat.getVersion());
        assertFalse(seat.isAvailable());

        Seat s2 = Seat.fromCsvLine(seat.toCsvLine());
        assertEquals(2, s2.getVersion());
        assertEquals(SeatStatus.BOOKED, s2.getStatus());
    }

    private static String joinCsv(Object... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0)
                sb.append(',');
            String val = (fields[i] == null) ? "" : String.valueOf(fields[i]);
            if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
                sb.append('"').append(val.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(val);
            }
        }
        return sb.toString();
    }
}
