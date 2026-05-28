package group19.restaurant_system.service;

import group19.restaurant_system.model.Record;
import group19.restaurant_system.model.User;
import group19.restaurant_system.model.Restaurant;
import group19.restaurant_system.repository.RecordParticipantRepository;
import group19.restaurant_system.repository.RecordRepository;
import group19.restaurant_system.repository.UserRepository;
import group19.restaurant_system.repository.RestaurantRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RecordService {

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private RecordParticipantRepository recordParticipantRepository;

    public List<Record> getUserHistory(Integer userId) {
        List<Record> history = recordRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
        if (!history.isEmpty()) {
            List<Integer> recordIds = history.stream()
                    .map(Record::getRecordId)
                    .collect(Collectors.toList());
            Map<Integer, List<Record.ParticipantInfo>> participantsMap =
                    recordParticipantRepository.findByRecordIds(recordIds);
            for (Record record : history) {
                record.setParticipants(
                    participantsMap.getOrDefault(record.getRecordId(), new ArrayList<>())
                );
            }
        }
        return history;
    }

    @Transactional
    public Record saveRecord(Integer userId, Integer restaurantId, LocalDateTime visitDate,
                             String mealName, String note, List<Integer> participantIds,
                             Integer groupSessionId) throws Exception {
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) throw new Exception("User not found");

        Optional<Restaurant> restaurantOpt = restaurantRepository.findById(restaurantId);
        if (!restaurantOpt.isPresent()) throw new Exception("Restaurant not found");

        Record record = new Record(userOpt.get(), restaurantOpt.get(), visitDate, mealName, note);
        record.setGroupSessionId(groupSessionId);
        Record saved = recordRepository.save(record);

        if (participantIds != null && !participantIds.isEmpty()) {
            recordParticipantRepository.saveParticipants(saved.getRecordId(), participantIds);
            saved.setParticipants(recordParticipantRepository.findByRecordId(saved.getRecordId()));
        }

        return saved;
    }

    public List<Record> getGroupHistory(Integer groupSessionId) {
        List<Record> history = recordRepository.findByGroupSessionIdOrderByCreatedAtDesc(groupSessionId);
        if (!history.isEmpty()) {
            List<Integer> recordIds = history.stream()
                    .map(Record::getRecordId)
                    .collect(Collectors.toList());
            Map<Integer, List<Record.ParticipantInfo>> participantsMap =
                    recordParticipantRepository.findByRecordIds(recordIds);
            for (Record record : history) {
                record.setParticipants(
                    participantsMap.getOrDefault(record.getRecordId(), new ArrayList<>())
                );
            }
        }
        return history;
    }

    public Optional<Record> getRecordById(Integer recordId) {
        return recordRepository.findById(recordId);
    }

    @Transactional
    public void deleteRecord(Integer recordId) throws Exception {
        Optional<Record> recordOpt = recordRepository.findById(recordId);
        if (!recordOpt.isPresent()) throw new Exception("Record not found");
        recordRepository.delete(recordOpt.get());
    }
}
