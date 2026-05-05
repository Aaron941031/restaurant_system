package group19.restaurant_system.service;

import group19.restaurant_system.model.GroupSession;
import group19.restaurant_system.model.User;
import group19.restaurant_system.repository.GroupSessionRepository;
import group19.restaurant_system.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class GroupSessionService {
    
    @Autowired
    private GroupSessionRepository groupSessionRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Transactional
    public GroupSession createGroup(Integer userId) throws Exception {
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new Exception("User not found");
        }
        
        // Generate unique invite code
        String inviteCode = generateUniqueInviteCode();
        
        GroupSession session = new GroupSession(userOpt.get(), inviteCode);
        return groupSessionRepository.save(session);
    }

    public Optional<GroupSession> getGroupSessionById(Integer sessionId) {
        return groupSessionRepository.findById(sessionId);
    }

    public Optional<GroupSession> getGroupSessionByInviteCode(String inviteCode) {
        return groupSessionRepository.findByInviteCode(inviteCode);
    }

    @Transactional
    public GroupSession joinGroup(String inviteCode, Integer userId) throws Exception {
        Optional<GroupSession> sessionOpt = groupSessionRepository.findByInviteCode(inviteCode);
        if (!sessionOpt.isPresent()) {
            throw new Exception("Group not found or invite code invalid");
        }
        
        GroupSession session = sessionOpt.get();
        
        if (!"揪團中".equals(session.getStatus())) {
            throw new Exception("This group session has ended");
        }
        
        // Note: For now, we just verify the session exists
        // In a production system, you would need a GroupMember table to track members
        // and verify the user is not already a member
        
        return session;
    }

    @Transactional
    public void endGroup(Integer sessionId) throws Exception {
        Optional<GroupSession> sessionOpt = groupSessionRepository.findById(sessionId);
        if (!sessionOpt.isPresent()) {
            throw new Exception("Group session not found");
        }
        
        GroupSession session = sessionOpt.get();
        session.setStatus("已結束");
        groupSessionRepository.save(session);
    }

    private String generateUniqueInviteCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (groupSessionRepository.existsByInviteCode(code));
        
        return code;
    }
}
