package group19.restaurant_system.service;

import group19.restaurant_system.model.Record;
import group19.restaurant_system.model.User;
import group19.restaurant_system.model.Restaurant;
import group19.restaurant_system.repository.RecordRepository;
import group19.restaurant_system.repository.UserRepository;
import group19.restaurant_system.repository.RestaurantRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class RecordService {
    
    @Autowired
    private RecordRepository recordRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RestaurantRepository restaurantRepository;

    public List<Record> getUserHistory(Integer userId) {
        return recordRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public Record saveRecord(Integer userId, Integer restaurantId, LocalDate visitDate, String mealName, String note) throws Exception {
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new Exception("User not found");
        }
        
        Optional<Restaurant> restaurantOpt = restaurantRepository.findById(restaurantId);
        if (!restaurantOpt.isPresent()) {
            throw new Exception("Restaurant not found");
        }
        
        Record record = new Record(userOpt.get(), restaurantOpt.get(), visitDate, mealName, note);
        return recordRepository.save(record);
    }

    public Optional<Record> getRecordById(Integer recordId) {
        return recordRepository.findById(recordId);
    }

    @Transactional
    public void deleteRecord(Integer recordId) throws Exception {
        Optional<Record> recordOpt = recordRepository.findById(recordId);
        if (!recordOpt.isPresent()) {
            throw new Exception("Record not found");
        }
        
        recordRepository.delete(recordOpt.get());
    }
}
