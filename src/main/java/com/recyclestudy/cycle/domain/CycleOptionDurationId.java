package com.recyclestudy.cycle.domain;

import com.recyclestudy.common.NullValidator;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Duration;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldNameConstants(level = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode
public class CycleOptionDurationId {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_option_id", nullable = false)
    private CycleOption cycleOption;

    @Column(name = "duration", nullable = false, columnDefinition = "bigint")
    private Duration duration;

    public static CycleOptionDurationId of(final CycleOption cycleOption, final Duration duration) {
        validateNotNull(cycleOption, duration);
        return new CycleOptionDurationId(cycleOption, duration);
    }

    private static void validateNotNull(final CycleOption cycleOption, final Duration duration) {
        NullValidator.builder()
                .add(Fields.cycleOption, cycleOption)
                .add(Fields.duration, duration)
                .validate();
    }
}
