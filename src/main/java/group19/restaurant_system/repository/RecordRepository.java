package group19.restaurant_system.repository;

import group19.restaurant_system.model.Record;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecordRepository extends JpaRepository<Record, Integer> {
    List<Record> findByUserUserIdOrderByCreatedAtDesc(Integer userId);
}
