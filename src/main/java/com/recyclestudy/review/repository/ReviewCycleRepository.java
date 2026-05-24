package com.recyclestudy.review.repository;

import com.recyclestudy.review.domain.NotificationStatus;
import com.recyclestudy.review.domain.ReviewCycle;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface ReviewCycleRepository extends JpaRepository<ReviewCycle, Long> {

    @Query("""
            SELECT rc FROM ReviewCycle rc
            JOIN FETCH rc.review r
            JOIN FETCH r.member
            WHERE rc.scheduledAt <= :scheduledAt
            AND rc.status = :status
            """)
    List<ReviewCycle> findAllByScheduledAt(
            @Param("scheduledAt") LocalDateTime scheduledAt,
            @Param("status") NotificationStatus status
    );

    @Query("""
            SELECT rc FROM ReviewCycle rc
            JOIN FETCH rc.review r
            JOIN FETCH r.member
            WHERE rc.status = :status
            AND rc.deadline > :now
            """)
    List<ReviewCycle> findAllRetryableCycles(
            @Param("status") NotificationStatus status,
            @Param("now") LocalDateTime now
    );

    @Query("""
            SELECT rc FROM ReviewCycle rc
            JOIN FETCH rc.review r
            WHERE r.member.id = :memberId
            AND rc.status = :status
            ORDER BY rc.scheduledAt ASC
            """)
    List<ReviewCycle> findAllByMemberAndStatus(
            @Param("memberId") Long memberId,
            @Param("status") NotificationStatus status
    );

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE ReviewCycle rc
            SET rc.status = :status, rc.lastAttemptedAt = :now
            WHERE rc.id IN :ids
            """)
    int updateStatus(
            @Param("ids") List<Long> ids,
            @Param("status") NotificationStatus status,
            @Param("now") LocalDateTime now
    );

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE ReviewCycle rc
            SET rc.status = :status, rc.failCount = rc.failCount + 1, rc.lastAttemptedAt = :now
            WHERE rc.id IN :ids
            """)
    int updateStatusAndIncrementFailCount(
            @Param("ids") List<Long> ids,
            @Param("status") NotificationStatus status,
            @Param("now") LocalDateTime now
    );
}
