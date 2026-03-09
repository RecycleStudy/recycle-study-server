package com.recyclestudy.review.repository;

import com.recyclestudy.review.domain.NotificationHistory;
import com.recyclestudy.review.domain.NotificationStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE NotificationHistory nh
            SET nh.status = :status, nh.lastAttemptedAt = :now
            WHERE nh.reviewCycle.id IN :reviewCycleIds
            """)
    int updateStatus(
            @Param("reviewCycleIds") List<Long> reviewCycleIds,
            @Param("status") NotificationStatus status,
            @Param("now") LocalDateTime now
    );

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE NotificationHistory nh
            SET nh.status = :status, nh.failCount = nh.failCount + 1, nh.lastAttemptedAt = :now
            WHERE nh.reviewCycle.id IN :reviewCycleIds
            """)
    int updateStatusAndIncrementFailCount(
            @Param("reviewCycleIds") List<Long> reviewCycleIds,
            @Param("status") NotificationStatus status,
            @Param("now") LocalDateTime now
    );

    @Query("""
            SELECT nh FROM NotificationHistory nh
            JOIN FETCH nh.reviewCycle rc
            WHERE rc.review.member.id = :memberId
            AND nh.status = :status
            ORDER BY rc.scheduledAt ASC
            """)
    List<NotificationHistory> findAllByMemberAndStatus(
            @Param("memberId") Long memberId,
            @Param("status") NotificationStatus status
    );
}
