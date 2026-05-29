package group19.restaurant_system.controller;

import group19.restaurant_system.dto.ApiResponse;
import group19.restaurant_system.model.GroupSession;
import group19.restaurant_system.model.Restaurant;
import group19.restaurant_system.repository.GroupMemberRepository;
import group19.restaurant_system.service.GroupSessionService;
import group19.restaurant_system.service.RestaurantService;
import group19.restaurant_system.util.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "*")
public class GroupController {

    @Autowired
    private GroupSessionService groupSessionService;

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    private Integer getUserIdFromHeader(String authHeader) throws Exception {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new Exception("Missing or invalid authorization header");
        }
        String jwt = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(jwt)) {
            throw new Exception("Invalid token");
        }
        return jwtTokenProvider.getUserIdFromToken(jwt);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createGroup(@RequestHeader("Authorization") String authHeader,
                                         @RequestBody(required = false) java.util.Map<String, String> payload) { // 👈 直接用 Map 接，絕對不會漏
        try {
            Integer userId = getUserIdFromHeader(authHeader);
            
            // 從 Map 中抓出 groupName
            String groupName = (payload != null) ? payload.get("groupName") : null;
            
            // 👇 加這行，讓你在 VS Code 終端機可以直接看到有沒有成功收到！
            System.out.println("========== 收到前端傳來的群組名稱: " + groupName + " ==========");
            
            GroupSession session = groupSessionService.createGroup(userId, groupName);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(true, "Group created", session));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinGroup(@RequestHeader("Authorization") String authHeader,
                                       @RequestParam String inviteCode) {
        try {
            Integer userId = getUserIdFromHeader(authHeader);
            GroupSession session = groupSessionService.joinGroup(inviteCode, userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Joined group", session));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getGroup(@PathVariable Integer id) {
        try {
            var sessionOpt = groupSessionService.getGroupSessionById(id);
            if (sessionOpt.isPresent()) {
                return ResponseEntity.ok(new ApiResponse<>(true, "Group retrieved", sessionOpt.get()));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Group not found"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<?> endGroup(@RequestHeader("Authorization") String authHeader,
                                      @PathVariable Integer id) {
        try {
            Integer userId = getUserIdFromHeader(authHeader);
            groupSessionService.endGroup(id);
            return ResponseEntity.ok(new ApiResponse<>(true, "Group ended"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyGroups(@RequestHeader("Authorization") String authHeader) {
        try {
            Integer userId = getUserIdFromHeader(authHeader);
            List<GroupSession> sessions = groupSessionService.getUserGroups(userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Groups retrieved", sessions));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    // 新增：取得群組成員（含名稱）
    @GetMapping("/{sessionId}/members")
    public ResponseEntity<?> getGroupMembers(@RequestHeader("Authorization") String authHeader,
                                             @PathVariable Integer sessionId) {
        try {
            Integer userId = getUserIdFromHeader(authHeader);
            if (!groupSessionService.isMember(sessionId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "Not a member of this group"));
            }
            List<GroupMemberRepository.MemberInfo> members =
                    groupMemberRepository.findMembersWithNameBySessionId(sessionId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Members retrieved", members));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    @GetMapping("/{id}/recommend")
    public ResponseEntity<?> getGroupRecommendations(@RequestHeader("Authorization") String authHeader,
                                                     @PathVariable Integer id) {
        try {
            Integer userId = getUserIdFromHeader(authHeader);
            if (!groupSessionService.isMember(id, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "Not a member of this group"));
            }
            List<Integer> memberIds = groupSessionService.getMemberIds(id);
            List<Restaurant> recommendations = restaurantService.getGroupRecommendations(id, memberIds);
            return ResponseEntity.ok(new ApiResponse<>(true, "Group recommendations retrieved", recommendations));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    @GetMapping("/{id}/recommend/remaining")
    public ResponseEntity<?> getGroupRecommendationsByRemaining(@RequestHeader("Authorization") String authHeader,
                                                                @PathVariable Integer id,
                                                                @RequestParam(required = false, defaultValue = "5") Integer limit) {
        try {
            Integer userId = getUserIdFromHeader(authHeader);
            if (!groupSessionService.isMember(id, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "Not a member of this group"));
            }
            List<Integer> memberIds = groupSessionService.getMemberIds(id);
            List<?> recommendations = restaurantService.getGroupRestaurantsByRemainingDishes(id, memberIds, limit);
            return ResponseEntity.ok(new ApiResponse<>(true, "Group restaurants by remaining dishes retrieved", recommendations));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/leave")
    public ResponseEntity<?> leaveGroup(@RequestHeader("Authorization") String authHeader,
                                        @PathVariable Integer id) {
        try {
            Integer userId = getUserIdFromHeader(authHeader);
            groupSessionService.leaveGroup(id, userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "已退出群組"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity deleteGroup(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Integer id) {
        try {
            Integer userId = getUserIdFromHeader(authHeader);
            groupSessionService.deleteGroup(id, userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Group deleted"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage()));
        }
    }

}


