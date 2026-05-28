package group19.restaurant_system.repository;

import group19.restaurant_system.model.Record;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class RecordParticipantRepository {

    private final JdbcTemplate jdbcTemplate;

    public RecordParticipantRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveParticipants(Integer recordId, List<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) return;
        for (Integer userId : userIds) {
            jdbcTemplate.update(
                "INSERT IGNORE INTO record_participants (recordId, userId) VALUES (?, ?)",
                recordId, userId
            );
        }
    }

    public List<Record.ParticipantInfo> findByRecordId(Integer recordId) {
        return jdbcTemplate.query(
            "SELECT rp.userId, u.name FROM record_participants rp " +
            "JOIN users u ON u.userId = rp.userId WHERE rp.recordId = ?",
            (rs, rowNum) -> new Record.ParticipantInfo(rs.getInt("userId"), rs.getString("name")),
            recordId
        );
    }

    public Map<Integer, List<Record.ParticipantInfo>> findByRecordIds(List<Integer> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) return new HashMap<>();

        String placeholders = recordIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT rp.recordId, rp.userId, u.name FROM record_participants rp " +
            "JOIN users u ON u.userId = rp.userId WHERE rp.recordId IN (" + placeholders + ")",
            recordIds.toArray()
        );

        Map<Integer, List<Record.ParticipantInfo>> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Integer rid = ((Number) row.get("recordId")).intValue();
            Integer uid = ((Number) row.get("userId")).intValue();
            String name = (String) row.get("name");
            result.computeIfAbsent(rid, k -> new ArrayList<>())
                  .add(new Record.ParticipantInfo(uid, name));
        }
        return result;
    }
}
