package group19.restaurant_system.repository;

import group19.restaurant_system.model.GroupSession;
import group19.restaurant_system.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class GroupSessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public GroupSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 👇 1. 在 RowMapper 讀取資料庫的 group_name
    private final RowMapper<GroupSession> rowMapper = (rs, rowNum) -> {
        GroupSession session = new GroupSession();
        session.setSessionId(rs.getInt("sessionId"));
        
        User creator = new User();
        creator.setUserId(rs.getInt("creatorId"));
        creator.setName(rs.getString("creatorName"));
        session.setCreator(creator);
        
        session.setInviteCode(rs.getString("inviteCode"));
        
        Timestamp createdAt = rs.getTimestamp("createdAt");
        if (createdAt != null) {
            session.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        session.setStatus(rs.getString("status"));
        
        // 從資料庫取得名稱並放回實體
        session.setGroupName(rs.getString("group_name"));
        
        return session;
    };

    public Optional<GroupSession> findById(Integer sessionId) {
        // 👇 2. SELECT 語法補上 group_name
        return queryForOptional(
            "SELECT gs.sessionId, gs.creatorId, u.name AS creatorName, gs.inviteCode, gs.status, gs.createdAt, gs.group_name FROM group_sessions gs JOIN users u ON u.userId = gs.creatorId WHERE gs.sessionId = ?", 
            sessionId
        );
    }

    public Optional<GroupSession> findByInviteCode(String inviteCode) {
        return queryForOptional(
            "SELECT gs.sessionId, gs.creatorId, u.name AS creatorName, gs.inviteCode, gs.status, gs.createdAt, gs.group_name FROM group_sessions gs JOIN users u ON u.userId = gs.creatorId WHERE gs.inviteCode = ?", 
            inviteCode
        );
    }
    
    public boolean existsByInviteCode(String inviteCode) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM group_sessions WHERE inviteCode = ?", 
                Integer.class, 
                inviteCode
        );
        return count != null && count > 0;
    }

    public GroupSession save(GroupSession session) {
        if (session.getSessionId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                // 👇 3. INSERT 語法補上 group_name 與對應的第五個問號 (?)
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO group_sessions (creatorId, inviteCode, status, createdAt, group_name) VALUES (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setInt(1, session.getCreator().getUserId());
                ps.setString(2, session.getInviteCode());
                ps.setString(3, session.getStatus());
                ps.setTimestamp(4, Timestamp.valueOf(session.getCreatedAt()));
                
                // 將群組名稱塞進第五個問號
                ps.setString(5, session.getGroupName());
                
                return ps;
            }, keyHolder);

            Number key = keyHolder.getKey();
            if (key != null) {
                return findById(key.intValue()).orElseGet(() -> {
                    session.setSessionId(key.intValue());
                    return session;
                });
            }
            return session;
        }

        // 👇 4. UPDATE 語法補上 group_name
        jdbcTemplate.update(
                "UPDATE group_sessions SET creatorId = ?, inviteCode = ?, status = ?, group_name = ? WHERE sessionId = ?",
                session.getCreator().getUserId(),
                session.getInviteCode(),
                session.getStatus(),
                session.getGroupName(), // 更新 group_name
                session.getSessionId()
        );
        return findById(session.getSessionId()).orElse(session);
    }

    public void deleteAllInBatch() {
        jdbcTemplate.update("DELETE FROM group_sessions");
    }

    private Optional<GroupSession> queryForOptional(String sql, Object... params) {
        List<GroupSession> results = jdbcTemplate.query(sql, rowMapper, params);
        return results.stream().findFirst();
    }

    public void deleteById(Integer sessionId) {
        jdbcTemplate.update("DELETE FROM group_sessions WHERE sessionId = ?", sessionId);
    }
}