package com.recyclestudy.review.domain;

import com.recyclestudy.common.BaseEntity;
import com.recyclestudy.common.NullValidator;
import com.recyclestudy.member.domain.Member;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Entity
@Table(name = "review")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldNameConstants(level = AccessLevel.PRIVATE)
@Getter
public class Review extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "url", nullable = false, columnDefinition = "TEXT"))
    private ReviewURL url;

    public static Review withoutId(final Member member, final ReviewURL url) {
        validateNotNull(member, url);
        return new Review(member, url);
    }

    private static void validateNotNull(final Member member, final ReviewURL url) {
        NullValidator.builder()
                .add(Fields.member, member)
                .add(Fields.url, url)
                .validate();
    }
}
