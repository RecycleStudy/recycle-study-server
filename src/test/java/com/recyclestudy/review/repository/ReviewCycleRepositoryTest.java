package com.recyclestudy.review.repository;

import com.recyclestudy.member.domain.Email;
import com.recyclestudy.member.domain.Member;
import com.recyclestudy.member.repository.MemberRepository;
import com.recyclestudy.review.domain.NotificationHistory;
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
    private NotificationHistoryRepository notificationHistoryRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    private static final LocalDateTime CUTOFF = LocalDateTime.now().minusDays(1);
    private static final LocalDateTime LONG_AGO = LocalDateTime.now().minusDays(2);
    private static final LocalDateTime RECENT = LocalDateTime.now().minusHours(1);

    @Test
    @DisplayName("FAILED 상태이고 failCount가 최대 횟수 미만이면 재시도 대상에 포함된다")
    void findAllRetryableCycles_success() {
        // given
        final ReviewCycle cycle = saveCycle("retry@email.com", LONG_AGO);
        notificationHistoryRepository.save(NotificationHistory.withoutId(cycle, NotificationStatus.PENDING));

        notificationHistoryRepository.updateStatusWithIncrementFailCount(
                List.of(cycle.getId()), NotificationStatus.FAILED, LocalDateTime.now());

        // when
        final List<ReviewCycle> results = reviewCycleRepository.findAllRetryableCycles(
                NotificationStatus.FAILED, 3, CUTOFF);

        // then
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getId()).isEqualTo(cycle.getId());
    }

    @Test
    @DisplayName("PENDING 상태만 있는 경우는 재시도 대상이 아니다")
    void findAllRetryableCycles_pendingOnly() {
        // given
        final ReviewCycle cycle = saveCycle("pending@email.com", LONG_AGO);
        notificationHistoryRepository.save(NotificationHistory.withoutId(cycle, NotificationStatus.PENDING));

        // when
        final List<ReviewCycle> results = reviewCycleRepository.findAllRetryableCycles(
                NotificationStatus.FAILED, 3, CUTOFF);

        // then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("이미 SENT 상태이면 재시도 대상이 아니다")
    void findAllRetryableCycles_alreadySent() {
        // given
        final ReviewCycle cycle = saveCycle("sent@email.com", LONG_AGO);
        notificationHistoryRepository.save(NotificationHistory.withoutId(cycle, NotificationStatus.PENDING));

        notificationHistoryRepository.updateStatus(
                List.of(cycle.getId()), NotificationStatus.SENT, LocalDateTime.now());

        // when
        final List<ReviewCycle> results = reviewCycleRepository.findAllRetryableCycles(
                NotificationStatus.FAILED, 3, CUTOFF);

        // then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("failCount가 최대 재시도 횟수에 도달하면 재시도 대상이 아니다")
    void findAllRetryableCycles_maxRetryReached() {
        // given
        final ReviewCycle cycle = saveCycle("max@email.com", LONG_AGO);
        notificationHistoryRepository.save(NotificationHistory.withoutId(cycle, NotificationStatus.PENDING));

        // failCount를 3으로 만들기 위해 3번 increment
        for (int i = 0; i < 3; i++) {
            notificationHistoryRepository.updateStatusWithIncrementFailCount(
                    List.of(cycle.getId()), NotificationStatus.FAILED, LocalDateTime.now());
        }

        // when
        final List<ReviewCycle> results = reviewCycleRepository.findAllRetryableCycles(
                NotificationStatus.FAILED, 3, CUTOFF);

        // then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("notification_history가 없는 경우 재시도 대상이 아니다")
    void findAllRetryableCycles_noHistory() {
        // given
        saveCycle("new@email.com", LONG_AGO);

        // when
        final List<ReviewCycle> results = reviewCycleRepository.findAllRetryableCycles(
                NotificationStatus.FAILED, 3, CUTOFF);

        // then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("scheduledAt이 cutoffDateTime보다 최근인 단기 주기는 재시도 대상이 아니다")
    void findAllRetryableCycles_shortCycle() {
        // given
        final ReviewCycle cycle = saveCycle("short@email.com", RECENT);
        notificationHistoryRepository.save(NotificationHistory.withoutId(cycle, NotificationStatus.PENDING));

        notificationHistoryRepository.updateStatusWithIncrementFailCount(
                List.of(cycle.getId()), NotificationStatus.FAILED, LocalDateTime.now());

        // when
        final List<ReviewCycle> results = reviewCycleRepository.findAllRetryableCycles(
                NotificationStatus.FAILED, 3, CUTOFF);

        // then
        assertThat(results).isEmpty();
    }

    private ReviewCycle saveCycle(final String email, final LocalDateTime scheduledAt) {
        final Member member = memberRepository.save(Member.withoutId(Email.from(email)));
        final Review review = reviewRepository.save(Review.withoutId(member, ReviewURL.from("url")));
        return reviewCycleRepository.save(ReviewCycle.withoutId(review, scheduledAt));
    }
}
