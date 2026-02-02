package com.recyclestudy.cycle.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Duration;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;

@Entity
@Table(name = "cycle_option_duration")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldNameConstants(level = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode
public class CycleOptionDuration {

    @EmbeddedId
    private CycleOptionDurationId id;

    public static CycleOptionDuration of(final CycleOption cycleOption, final Duration duration) {
        return new CycleOptionDuration(CycleOptionDurationId.of(cycleOption, duration));
    }

    public Duration getDuration() {
        return this.getId().getDuration();
    }
}
