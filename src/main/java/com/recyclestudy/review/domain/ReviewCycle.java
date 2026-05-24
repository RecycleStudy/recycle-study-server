package com.recyclestudy.review.domain;

import com.recyclestudy.common.BaseEntity;
import com.recyclestudy.common.NullValidator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Entity
@Table(name = "review_cycle")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldNameConstants(level = AccessLevel.PRIVATE)
@Getter
public class ReviewCycle extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NotificationStatus status;

    @Column(name = "fail_count", nullable = false)
    private int failCount;

    @Column(name = "last_attempted_at")
    private LocalDateTime lastAttemptedAt;

    @Column(name = "deadline", nullable = false)
    private LocalDateTime deadline;

    public static ReviewCycle withoutId(
            final Review review,
            final LocalDateTime scheduledAt,
            final NotificationStatus status,
            final LocalDateTime deadline
    ) {
        validateNotNull(review, scheduledAt, status, deadline);
        return new ReviewCycle(review, scheduledAt, status, 0, null, deadline);
    }

    private static void validateNotNull(
            final Review review,
            final LocalDateTime scheduledAt,
            final NotificationStatus status,
            final LocalDateTime deadline
    ) {
        NullValidator.builder()
                .add(Fields.review, review)
                .add(Fields.scheduledAt, scheduledAt)
                .add(Fields.status, status)
                .add(Fields.deadline, deadline)
                .validate();
    }
}
