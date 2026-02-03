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

    @Test
    @DisplayName("재시도 대상(실패 이력만 있고 최대 횟수 미만)을 조회한다")
    void findAllRetryableCycles_success() {
        // given
        final Member member = memberRepository.save(Member.withoutId(Email.from("test@email.com")));
        final Review review = reviewRepository.save(Review.withoutId(member, ReviewURL.from("url")));
        final ReviewCycle cycle = reviewCycleRepository.save(ReviewCycle.withoutId(review, LocalDateTime.now()));

        notificationHistoryRepository.save(NotificationHistory.withoutId(cycle, NotificationStatus.PENDING));
        notificationHistoryRepository.save(NotificationHistory.withoutId(cycle, NotificationStatus.FAILED));

        // when
        final List<ReviewCycle> results = reviewCycleRepository.findAllRetryableCycles(3);

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(cycle.getId());
    }

    @Test
    @DisplayName("PENDING 상태만 있는 경우는 재시도 대상이 아니다")
    void findAllRetryableCycles_pendingOnly() {
        // given
        final Member member = memberRepository.save(Member.withoutId(Email.from("pending@email.com")));
        final Review review = reviewRepository.save(Review.withoutId(member, ReviewURL.from("url")));
        final ReviewCycle cycle = reviewCycleRepository.save(ReviewCycle.withoutId(review, LocalDateTime.now()));

        notificationHistoryRepository.save(NotificationHistory.withoutId(cycle, NotificationStatus.PENDING));

        // when
        final List<ReviewCycle> results = reviewCycleRepository.findAllRetryableCycles(3);

        // then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("이미 성공한 이력이 있으면 재시도 대상이 아니다")
    void findAllRetryableCycles_alreadySent() {
        // given
        final Member member = memberRepository.save(Member.withoutId(Email.from("sent@email.com")));
        final Review review = reviewRepository.save(Review.withoutId(member, ReviewURL.from("url")));
        final ReviewCycle cycle = reviewCycleRepository.save(ReviewCycle.withoutId(review, LocalDateTime.now()));

        notificationHistoryRepository.save(NotificationHistory.withoutId(cycle, NotificationStatus.PENDING));
        notificationHistoryRepository.save(NotificationHistory.withoutId(cycle, NotificationStatus.FAILED));
        notificationHistoryRepository.save(NotificationHistory.withoutId(cycle, NotificationStatus.SENT));

        // when
        final List<ReviewCycle> results = reviewCycleRepository.findAllRetryableCycles(3);

        // then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("최대 재시도 횟수에 도달하면 재시도 대상이 아니다")
    void findAllRetryableCycles_maxRetryReached() {
        // given
        final Member member = memberRepository.save(Member.withoutId(Email.from("max@email.com")));
        final Review review = reviewRepository.save(Review.withoutId(member, ReviewURL.from("url")));
        final ReviewCycle cycle = reviewCycleRepository.save(ReviewCycle.withoutId(review, LocalDateTime.now()));

        notificationHistoryRepository.save(NotificationHistory.withoutId(cycle, NotificationStatus.PENDING));
        notificationHistoryRepository.save(NotificationHistory.withoutId(cycle, NotificationStatus.FAILED));
        notificationHistoryRepository.save(NotificationHistory.withoutId(cycle, NotificationStatus.FAILED));
        notificationHistoryRepository.save(NotificationHistory.withoutId(cycle, NotificationStatus.FAILED));

        // when
        final List<ReviewCycle> results = reviewCycleRepository.findAllRetryableCycles(3);

        // then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("이력이 없는 경우 재시도 대상이 아니다")
    void findAllRetryableCycles_noHistory() {
        // given
        final Member member = memberRepository.save(Member.withoutId(Email.from("new@email.com")));
        final Review review = reviewRepository.save(Review.withoutId(member, ReviewURL.from("url")));
        reviewCycleRepository.save(ReviewCycle.withoutId(review, LocalDateTime.now()));

        // when
        final List<ReviewCycle> results = reviewCycleRepository.findAllRetryableCycles(3);

        // then
        assertThat(results).isEmpty();
    }
}
