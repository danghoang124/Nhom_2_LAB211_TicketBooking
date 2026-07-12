package model.entity;

/**
 * Entity đại diện cho một Sân vận động.
 *
 * <p>Tương ứng với file: {@code data/stadiums.csv}
 * <pre>
 *   Header: stadiumId,name,city,address,totalCapacity
 *   Ví dụ:  STD001,"Sân Vận Động Mỹ Đình",Hà Nội,"Phường Mỹ Đình II, Nam Từ Liêm, Hà Nội",40192
 * </pre>
 */
public class Stadium extends BaseEntity {

    private String stadiumId;    // PK — dạng STD001
    private String name;
    private String city;
    private String address;
    private int    totalCapacity;

    // ── Constructor ────────────────────────────────────────────────────────────

    public Stadium(String stadiumId, String name, String city,
                   String address, int totalCapacity) {
        this.stadiumId     = stadiumId;
        this.name          = name;
        this.city          = city;
        this.address       = address;
        this.totalCapacity = totalCapacity;
    }

    // ── BaseEntity ─────────────────────────────────────────────────────────────

    @Override
    public String getId() {
        return stadiumId;
    }

    /**
     * Tuần tự hoá thành dòng CSV.
     * Thứ tự: stadiumId, name, city, address, totalCapacity
     */
    @Override
    public String toCsvLine() {
        return joinCsv(stadiumId, name, city, address, totalCapacity);
    }

    /**
     * Tạo một đối tượng Stadium từ một dòng CSV.
     *
     * @param line Dòng CSV (không phải header).
     * @return Stadium mới.
     * @throws IllegalArgumentException nếu dòng CSV không đúng định dạng.
     */
    public static Stadium fromCsvLine(String line) {
        String[] f = splitCsvLine(line);
        if (f.length < 5) {
            throw new IllegalArgumentException(
                "Stadium CSV cần ít nhất 5 cột, nhưng chỉ có " + f.length + ": " + line);
        }
        return new Stadium(
            f[0].trim(),           // stadiumId
            f[1].trim(),           // name
            f[2].trim(),           // city
            f[3].trim(),           // address
            Integer.parseInt(f[4].trim()) // totalCapacity
        );
    }

    // ── Getters ────────────────────────────────────────────────────────────────

    public String getStadiumId() {
        return stadiumId;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public String getAddress() {
        return address;
    }

    public int getTotalCapacity() {
        return totalCapacity;
    }
}
