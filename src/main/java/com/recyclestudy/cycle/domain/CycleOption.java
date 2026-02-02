package com.recyclestudy.cycle.domain;

import com.recyclestudy.common.BaseEntity;
import com.recyclestudy.common.NullValidator;
import com.recyclestudy.member.domain.Member;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Entity
@Table(name = "cycle_option")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldNameConstants(level = AccessLevel.PRIVATE)
@Getter
public class CycleOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "title", nullable = false))
    private CycleOptionTitle title;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "option_type", nullable = false)
    private OptionType optionType;

    @Embedded
    private CycleDurations durations;

    private CycleOption(
            final Member member,
            final CycleOptionTitle title,
            final OptionType optionType
    ) {
        this.member = member;
        this.title = title;
        this.optionType = optionType;
    }

    public static CycleOption withoutId(
            final Member member,
            final CycleOptionTitle title,
            final OptionType optionType,
            final List<Duration> durations
    ) {
        validateNotNull(member, title, optionType, durations);

        final CycleOption option = new CycleOption(member, title, optionType);

        final List<CycleOptionDuration> cycleOptionDurations = durations.stream()
                .map(duration -> CycleOptionDuration.of(option, duration))
                .toList();
        option.durations = new CycleDurations(cycleOptionDurations);

        return option;
    }

    public void update(final CycleOptionTitle title, final List<Duration> newDurationValues) {
        validateNotNull(member, title, optionType, newDurationValues);
        this.title = title;

        final List<CycleOptionDuration> newDurations = newDurationValues.stream()
                .map(duration -> CycleOptionDuration.of(this, duration))
                .toList();
        this.durations.replace(newDurations);
    }

    public boolean isOwner(final Member member) {
        return this.member.getId().equals(member.getId());
    }

    public List<CycleOptionDuration> getDurations() {
        return durations.getValues();
    }

    private static void validateNotNull(
            final Member member,
            final CycleOptionTitle title,
            final OptionType optionType,
            final List<Duration> durations
    ) {
        NullValidator.builder()
                .add(Fields.member, member)
                .add(Fields.title, title)
                .add(Fields.optionType, optionType)
                .add(Fields.durations, durations)
                .validate();
    }
}
