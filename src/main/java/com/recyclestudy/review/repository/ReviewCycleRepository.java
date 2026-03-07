package com.recyclestudy.review.repository;

import com.recyclestudy.review.domain.NotificationStatus;
import com.recyclestudy.review.domain.ReviewCycle;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface ReviewCycleRepository extends JpaRepository<ReviewCycle, Long> {

    @Query("""
            SELECT rc FROM ReviewCycle rc
            JOIN FETCH rc.review r
            JOIN FETCH r.member
            JOIN NotificationHistory nh ON rc.id = nh.reviewCycle.id
            WHERE rc.scheduledAt <= :scheduledAt
            AND nh.status = :status
            """)
    List<ReviewCycle> findAllByScheduledAt(
            @Param("scheduledAt") LocalDateTime scheduledAt,
            @Param("status") NotificationStatus status
    );

    @Query("""
            SELECT rc FROM ReviewCycle rc
            JOIN FETCH rc.review r
            JOIN FETCH r.member
            JOIN NotificationHistory nh ON rc.id = nh.reviewCycle.id
            WHERE nh.status = :status
            AND nh.deadline > :now
            """)
    List<ReviewCycle> findAllRetryableCycles(
            @Param("status") NotificationStatus status,
            @Param("now") LocalDateTime now
    );

    Optional<ReviewCycle> findFirstByReview_IdAndScheduledAtGreaterThanOrderByScheduledAtAsc(
            Long reviewId,
            LocalDateTime currentScheduledAt
    );
}
