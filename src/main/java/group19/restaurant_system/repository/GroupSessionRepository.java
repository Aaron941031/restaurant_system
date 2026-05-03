package group19.restaurant_system.repository;

import group19.restaurant_system.model.GroupSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupSessionRepository extends JpaRepository<GroupSession, Integer> {
    Optional<GroupSession> findByInviteCode(String inviteCode);
    boolean existsByInviteCode(String inviteCode);
}
