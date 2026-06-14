package model.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp trừu tượng (abstract) là cha chung của mọi Entity trong hệ thống.
 *
 * <p><b>Trách nhiệm:</b>
 * <ul>
 *   <li>Bắt buộc mỗi Entity phải có {@code getId()} — trả về khoá chính (PK).</li>
 *   <li>Bắt buộc mỗi Entity phải có {@code toCsvLine()} — tuần tự hoá thành 1 dòng CSV.</li>
 *   <li>Cung cấp 2 utility dùng chung:
 *       {@code joinCsv()} để ghép fields thành CSV,
 *       {@code splitCsvLine()} để tách dòng CSV thành mảng fields.
 *   </li>
 * </ul>
 *
 * <p><b>Thiết kế quan trọng:</b><br>
 * Mỗi lớp con còn phải tự cài thêm một static factory method:
 * <pre>
 *   public static ConcreteEntity fromCsvLine(String line) { ... }
 * </pre>
 * Method này không thể abstract vì Java không cho phép abstract static method,
 * nhưng CsvRepository sẽ gọi nó gián tiếp qua {@code parseFromCsvLine()}.
 */
public abstract class BaseEntity {

    // ── Abstract methods mỗi Entity con BẮT BUỘC implement ───────────────────

    /**
     * Trả về khoá chính (Primary Key) của entity.
     * Ví dụ: "STD001", "FAN0042", "SEAT001234"
     */
    public abstract String getId();

    /**
     * Tuần tự hoá entity thành một dòng CSV không có header.
     * Thứ tự cột phải khớp đúng với CSV schema đã định nghĩa.
     *
     * <p>Ví dụ (Stadium):
     * <pre>
     *   STD001,"Sân Vận Động Mỹ Đình",Hà Nội,"Phường Mỹ Đình II, Nam Từ Liêm, Hà Nội",40192
     * </pre>
     */
    public abstract String toCsvLine();

    // ── Shared CSV utilities (protected static — dùng chung cho các lớp con) ─

    /**
     * Ghép các field thành một dòng CSV hợp lệ theo RFC-4180.
     *
     * <p>Quy tắc: nếu một field chứa dấu phẩy, dấu nháy kép, hoặc xuống dòng
     * → bọc field đó trong dấu nháy kép và escape nháy kép bên trong thành "".
     *
     * @param fields Các giá trị cần ghép (tự động gọi toString()).
     * @return Chuỗi CSV một dòng. Ví dụ: {@code STD001,"Sân Mỹ Đình",Hà Nội,40192}
     */
    protected static String joinCsv(Object... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append(',');
            String val = (fields[i] == null) ? "" : String.valueOf(fields[i]);
            // Cần wrap nháy kép nếu có ký tự đặc biệt
            if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
                sb.append('"').append(val.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(val);
            }
        }
        return sb.toString();
    }

    /**
     * Tách một dòng CSV thành mảng các field, xử lý đúng trường hợp:
     * <ul>
     *   <li>Field bọc trong dấu nháy kép: {@code "Sân Mỹ Đình, Hà Nội"}</li>
     *   <li>Dấu nháy kép bên trong được escape bằng "": {@code "He said ""Hello"""}}</li>
     *   <li>Field thông thường không có nháy kép</li>
     * </ul>
     *
     * @param line Một dòng CSV (không phải header).
     * @return Mảng String các field đã được unescape. Không bao giờ trả về null.
     */
    public static String[] splitCsvLine(String line) {
        if (line == null || line.isEmpty()) return new String[0];

        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    // Kiểm tra xem có phải escape "" không
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"'); // thêm 1 dấu nháy thật
                        i++;                 // bỏ qua dấu nháy thứ 2
                    } else {
                        inQuotes = false;    // kết thúc quoted field
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;         // bắt đầu quoted field
                } else if (c == ',') {
                    fields.add(current.toString()); // lưu field hiện tại
                    current.setLength(0);           // reset buffer
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString()); // field cuối cùng
        return fields.toArray(new String[0]);
    }

    // ── toString mặc định dễ debug ─────────────────────────────────────────────

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[id=" + getId() + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BaseEntity other = (BaseEntity) obj;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getId() == null ? 0 : getId().hashCode();
    }
}
