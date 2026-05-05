package group19.restaurant_system.repository;

import group19.restaurant_system.model.UserExclusion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserExclusionRepository extends JpaRepository<UserExclusion, Integer> {
    List<UserExclusion> findByUserUserId(Integer userId);
    boolean existsByUserUserIdAndDishCategoryId(Integer userId, Integer categoryId);
    void deleteByUserUserIdAndDishCategoryId(Integer userId, Integer categoryId);
}
