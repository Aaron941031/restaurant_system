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

    private final RowMapper<GroupSession> rowMapper = (rs, rowNum) -> {
        GroupSession session = new GroupSession();
        session.setSessionId(rs.getInt("sessionId"));
        User creator = new User();
        creator.setUserId(rs.getInt("creatorId"));
        session.setCreator(creator);
        session.setInviteCode(rs.getString("inviteCode"));
        Timestamp createdAt = rs.getTimestamp("createdAt");
        if (createdAt != null) {
            session.setCreatedAt(createdAt.toLocalDateTime());
        }
        session.setStatus(rs.getString("status"));
        return session;
    };

    public Optional<GroupSession> findById(Integer sessionId) {
        return queryForOptional("SELECT sessionId, creatorId, inviteCode, createdAt, status FROM group_sessions WHERE sessionId = ?", sessionId);
    }

    public Optional<GroupSession> findByInviteCode(String inviteCode) {
        return queryForOptional("SELECT sessionId, creatorId, inviteCode, createdAt, status FROM group_sessions WHERE inviteCode = ?", inviteCode);
    }

    public boolean existsByInviteCode(String inviteCode) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM group_sessions WHERE inviteCode = ?", Integer.class, inviteCode);
        return count != null && count > 0;
    }

    public GroupSession save(GroupSession session) {
        if (session.getSessionId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO group_sessions (creatorId, inviteCode, status) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setInt(1, session.getCreator().getUserId());
                ps.setString(2, session.getInviteCode());
                ps.setString(3, session.getStatus());
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

        jdbcTemplate.update(
                "UPDATE group_sessions SET creatorId = ?, inviteCode = ?, status = ? WHERE sessionId = ?",
                session.getCreator().getUserId(),
                session.getInviteCode(),
                session.getStatus(),
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
}
