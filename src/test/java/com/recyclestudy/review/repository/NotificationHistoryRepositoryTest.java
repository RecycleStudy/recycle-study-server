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
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DataJpaTest
class NotificationHistoryRepositoryTest {

    @Autowired
    private NotificationHistoryRepository notificationHistoryRepository;

    @Autowired
    private ReviewCycleRepository reviewCycleRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private MemberRepository memberRepository;

    private static final LocalDateTime T1 = LocalDateTime.of(2026, 3, 6, 9, 0);
    private static final LocalDateTime T2 = LocalDateTime.of(2026, 3, 7, 9, 0);

    @Test
    @DisplayName("본인의 PENDING NotificationHistory가 scheduledAt ASC 순으로 반환된다")
    void findAllPendingByMember_AscOrderAndStatus() {
        // given
        final Member member = saveMember("user@test.com");
        saveNh(member, T2, NotificationStatus.PENDING);
        saveNh(member, T1, NotificationStatus.PENDING);

        // when
        final List<NotificationHistory> result = notificationHistoryRepository
                .findAllByMemberAndStatus(member.getId(), NotificationStatus.PENDING);

        // then
        assertSoftly(softly -> {
            softly.assertThat(result).hasSize(2);
            softly.assertThat(result.get(0).getReviewCycle().getScheduledAt()).isEqualTo(T1);
            softly.assertThat(result.get(1).getReviewCycle().getScheduledAt()).isEqualTo(T2);
        });
    }

    @Test
    @DisplayName("타 멤버의 NotificationHistory는 포함되지 않는다")
    void findAllByMember_AndStatus_excludesOtherMembers() {
        // given
        final Member member = saveMember("user@test.com");
        final Member other = saveMember("other@test.com");
        saveNh(member, T1, NotificationStatus.PENDING);
        saveNh(other, T1, NotificationStatus.PENDING);

        // when
        final List<NotificationHistory> result = notificationHistoryRepository
                .findAllByMemberAndStatus(member.getId(), NotificationStatus.PENDING);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getReviewCycle().getScheduledAt()).isEqualTo(T1);
    }

    @Test
    @DisplayName("SENT/FAILED 상태는 조회에서 제외된다")
    void findAllPendingByMember_excludesNonAndStatus() {
        // given
        final Member member = saveMember("user@test.com");
        saveNh(member, T1, NotificationStatus.SENT);
        saveNh(member, T2, NotificationStatus.FAILED);

        // when
        final List<NotificationHistory> result = notificationHistoryRepository
                .findAllByMemberAndStatus(member.getId(), NotificationStatus.PENDING);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("NotificationHistory가 없으면 빈 리스트를 반환한다")
    void findAllByMember_AndStatus_empty() {
        // given
        final Member member = saveMember("user@test.com");

        // when
        final List<NotificationHistory> result = notificationHistoryRepository
                .findAllByMemberAndStatus(member.getId(), NotificationStatus.PENDING);

        // then
        assertThat(result).isEmpty();
    }

    private Member saveMember(final String email) {
        return memberRepository.save(Member.withoutId(Email.from(email)));
    }

    private NotificationHistory saveNh(final Member member, final LocalDateTime scheduledAt,
                                       final NotificationStatus status) {
        final Review review = reviewRepository.save(Review.withoutId(member, ReviewURL.from("https://example.com")));
        final ReviewCycle cycle = reviewCycleRepository.save(ReviewCycle.withoutId(review, scheduledAt));
        return notificationHistoryRepository.save(NotificationHistory.withoutId(cycle, status));
    }
}
