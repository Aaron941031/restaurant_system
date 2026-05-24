package group19.restaurant_system.repository;

import group19.restaurant_system.model.GroupSession;
import group19.restaurant_system.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class GroupMemberRepository {

    private final JdbcTemplate jdbcTemplate;

    public GroupMemberRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public static class MemberInfo {
        public Integer userId;
        public String name;

        public MemberInfo(Integer userId, String name) {
            this.userId = userId;
            this.name = name;
        }
    }

    private final RowMapper<GroupSession> groupSessionRowMapper = (rs, rowNum) -> {
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

    public boolean existsBySessionIdAndUserId(Integer sessionId, Integer userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM group_members WHERE sessionId = ? AND userId = ?",
                Integer.class,
                sessionId,
                userId
        );
        return count != null && count > 0;
    }

    public void addMember(Integer sessionId, Integer userId) {
        jdbcTemplate.update(
                "INSERT INTO group_members (sessionId, userId) VALUES (?, ?)",
                sessionId,
                userId
        );
    }

    public List<Integer> findMemberIdsBySessionId(Integer sessionId) {
        return jdbcTemplate.queryForList(
                "SELECT userId FROM group_members WHERE sessionId = ?",
                Integer.class,
                sessionId
        );
    }

    public List<MemberInfo> findMembersWithNameBySessionId(Integer sessionId) {
        return jdbcTemplate.query(
                "SELECT gm.userId, u.name " +
                        "FROM group_members gm " +
                        "JOIN users u ON u.userId = gm.userId " +
                        "WHERE gm.sessionId = ?",
                (rs, rowNum) -> new MemberInfo(
                        rs.getInt("userId"),
                        rs.getString("name")
                ),
                sessionId
        );
    }

    public List<GroupSession> findSessionsByUserId(Integer userId) {
        return jdbcTemplate.query(
                "SELECT gs.sessionId, gs.creatorId, gs.inviteCode, gs.createdAt, gs.status " +
                        "FROM group_sessions gs " +
                        "JOIN group_members gm ON gm.sessionId = gs.sessionId " +
                        "WHERE gm.userId = ? " +
                        "ORDER BY gs.createdAt DESC",
                groupSessionRowMapper,
                userId
        );
    }
}