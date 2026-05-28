package group19.restaurant_system.service;

import group19.restaurant_system.model.GroupSession;
import group19.restaurant_system.model.User;
import group19.restaurant_system.repository.GroupMemberRepository;
import group19.restaurant_system.repository.GroupSessionRepository;
import group19.restaurant_system.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class GroupSessionService {
    
    @Autowired
    private GroupSessionRepository groupSessionRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;
    
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
        GroupSession saved = groupSessionRepository.save(session);
        groupMemberRepository.addMember(saved.getSessionId(), userId);
        return saved;
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
        
        if (session.getCreator().getUserId().equals(userId)) {
            throw new Exception("無法加入自己的房間");
        }
        
        if (!groupMemberRepository.existsBySessionIdAndUserId(session.getSessionId(), userId)) {
            groupMemberRepository.addMember(session.getSessionId(), userId);
        }

        return session;
    }

    public boolean isMember(Integer sessionId, Integer userId) {
        return groupMemberRepository.existsBySessionIdAndUserId(sessionId, userId);
    }

    public java.util.List<Integer> getMemberIds(Integer sessionId) {
        return groupMemberRepository.findMemberIdsBySessionId(sessionId);
    }

    public java.util.List<GroupSession> getUserGroups(Integer userId) {
        return groupMemberRepository.findSessionsByUserId(userId);
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

    @Transactional
    public void leaveGroup(Integer sessionId, Integer userId) throws Exception {
        GroupSession session = groupSessionRepository.findById(sessionId)
            .orElseThrow(() -> new Exception("Group not found"));

        if (session.getCreator().getUserId().equals(userId)) {
            throw new Exception("建立者無法退出群組，請改為刪除群組");
        }

        if (!groupMemberRepository.existsBySessionIdAndUserId(sessionId, userId)) {
            throw new Exception("你不是此群組的成員");
        }

        groupMemberRepository.removeMember(sessionId, userId);
    }

    @Transactional
    public void deleteGroup(Integer sessionId, Integer userId) throws Exception {
        GroupSession session = groupSessionRepository.findById(sessionId)
            .orElseThrow(() -> new Exception("Group not found"));

        if (!session.getCreator().getUserId().equals(userId)) {
         throw new Exception("只有建立者可以刪除群組");
        }

        groupSessionRepository.deleteById(sessionId);
    }
}
