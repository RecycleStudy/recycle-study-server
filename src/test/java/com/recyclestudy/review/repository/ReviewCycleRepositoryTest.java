package com.recyclestudy.review.repository;

import com.recyclestudy.member.domain.Email;
import com.recyclestudy.member.domain.Member;
import com.recyclestudy.member.repository.MemberRepository;
import com.recyclestudy.review.domain.NotificationStatus;
import com.recyclestudy.review.domain.Review;
import com.recyclestudy.review.domain.ReviewCycle;
import com.recyclestudy.review.domain.ReviewURL;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ReviewCycleRepositoryTest {

    @Autowired
    private ReviewCycleRepository reviewCycleRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
    private static final LocalDateTime FUTURE_DEADLINE = NOW.plusHours(23);
    private static final LocalDateTime PAST_DEADLINE = NOW.minusHours(1);
    private static final LocalDateTime SCHEDULED_AT = NOW.minusDays(1);

    @Test
    @DisplayName("FAILED 상태이고 deadline이 현재보다 미래이면 재시도 대상에 포함된다")
    void findAllRetryableCycles_success() {
        // given
        final ReviewCycle cycle = saveCycle("retry@email.com", SCHEDULED_AT, FUTURE_DEADLINE);
        reviewCycleRepository.updateStatusAndIncrementFailCount(
                List.of(cycle.getId()), NotificationStatus.FAILED, NOW);

        // when
        final List<ReviewCycle> results = reviewCycleRepository.findAllRetryableCycles(
                NotificationStatus.FAILED, NOW);

        // then
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getId()).isEqualTo(cycle.getId());
    }

    @Test
    @DisplayName("PENDING 상태만 있는 경우는 재시도 대상이 아니다")
    void findAllRetryableCycles_pendingOnly() {
        // given
        saveCycle("pending@email.com", SCHEDULED_AT, FUTURE_DEADLINE);

        // when
        final List<ReviewCycle> results = reviewCycleRepository.findAllRetryableCycles(
                NotificationStatus.FAILED, NOW);

        // then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("이미 SENT 상태이면 재시도 대상이 아니다")
    void findAllRetryableCycles_alreadySent() {
        // given
        final ReviewCycle cycle = saveCycle("sent@email.com", SCHEDULED_AT, FUTURE_DEADLINE);
        reviewCycleRepository.updateStatus(
                List.of(cycle.getId()), NotificationStatus.SENT, NOW);

        // when
        final List<ReviewCycle> results = reviewCycleRepository.findAllRetryableCycles(
                NotificationStatus.FAILED, NOW);

        // then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("deadline이 현재보다 과거이면 재시도 대상이 아니다")
    void findAllRetryableCycles_deadlineExpired() {
        // given
        final ReviewCycle cycle = saveCycle("expired@email.com", SCHEDULED_AT, PAST_DEADLINE);
        reviewCycleRepository.updateStatusAndIncrementFailCount(
                List.of(cycle.getId()), NotificationStatus.FAILED, NOW);

        // when
        final List<ReviewCycle> results = reviewCycleRepository.findAllRetryableCycles(
                NotificationStatus.FAILED, NOW);

        // then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("PENDING 상태인 경우 재시도 대상이 아니다")
    void findAllRetryableCycles_pendingIsNotRetryable() {
        // given
        saveCycle("new@email.com", SCHEDULED_AT, FUTURE_DEADLINE);

        // when
        final List<ReviewCycle> results = reviewCycleRepository.findAllRetryableCycles(
                NotificationStatus.FAILED, NOW);

        // then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("failCount가 아무리 높아도 deadline이 미래이면 재시도 대상에 포함된다")
    void findAllRetryableCycles_failCountDoesNotAffectEligibility() {
        // given
        final ReviewCycle cycle = saveCycle("many-fails@email.com", SCHEDULED_AT, FUTURE_DEADLINE);
        for (int i = 0; i < 10; i++) {
            reviewCycleRepository.updateStatusAndIncrementFailCount(
                    List.of(cycle.getId()), NotificationStatus.FAILED, NOW);
        }

        // when
        final List<ReviewCycle> results = reviewCycleRepository.findAllRetryableCycles(
                NotificationStatus.FAILED, NOW);

        // then
        assertThat(results).hasSize(1);
    }

    private ReviewCycle saveCycle(
            final String email,
            final LocalDateTime scheduledAt,
            final LocalDateTime deadline
    ) {
        final Member member = memberRepository.save(Member.withoutId(Email.from(email)));
        final Review review = reviewRepository.save(Review.withoutId(member, ReviewURL.from("url")));
        return reviewCycleRepository.save(
                ReviewCycle.withoutId(review, scheduledAt, NotificationStatus.PENDING, deadline));
    }
}
