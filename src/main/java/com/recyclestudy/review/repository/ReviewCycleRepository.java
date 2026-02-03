package com.recyclestudy.review.repository;

import com.recyclestudy.review.domain.NotificationStatus;
import com.recyclestudy.review.domain.ReviewCycle;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewCycleRepository extends JpaRepository<ReviewCycle, Long> {

    List<ReviewCycle> findAllByScheduledAt(LocalDateTime scheduledAt);

    @Query("""
            SELECT rc FROM ReviewCycle rc
            JOIN NotificationHistory nh ON rc.id = nh.reviewCycle.id
            GROUP BY rc
            HAVING SUM(CASE WHEN nh.status = :sentStatus THEN 1 ELSE 0 END) = 0
            AND COUNT(nh) < :maxRetryCount
            """)
    List<ReviewCycle> findAllRetryableCycles(
            @Param("maxRetryCount") long maxRetryCount,
            @Param("sentStatus") NotificationStatus sentStatus
    );
}
