package group19.restaurant_system.controller;

import group19.restaurant_system.dto.ApiResponse;
import group19.restaurant_system.dto.RecordRequest;
import group19.restaurant_system.model.Record;
import group19.restaurant_system.service.RecordService;
import group19.restaurant_system.util.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@CrossOrigin(origins = "*")
public class HistoryController {
    
    @Autowired
    private RecordService recordService;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

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

    @GetMapping("/me")
    public ResponseEntity<?> getUserHistory(@RequestHeader("Authorization") String authHeader) {
        try {
            Integer userId = getUserIdFromHeader(authHeader);
            List<Record> history = recordService.getUserHistory(userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "History retrieved", history));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveRecord(@RequestHeader("Authorization") String authHeader,
                                        @RequestBody RecordRequest request) {
        try {
            Integer userId = getUserIdFromHeader(authHeader);
            Record record = recordService.saveRecord(userId, request.getRestaurantId(), 
                    request.getVisitDate(), request.getMealName(), request.getNote());
            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, "Record saved", record));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    @DeleteMapping("/{recordId}")
    public ResponseEntity<?> deleteRecord(@RequestHeader("Authorization") String authHeader,
                                          @PathVariable Integer recordId) {
        try {
            Integer userId = getUserIdFromHeader(authHeader);
            recordService.deleteRecord(recordId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Record deleted"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
        }
    }
}
