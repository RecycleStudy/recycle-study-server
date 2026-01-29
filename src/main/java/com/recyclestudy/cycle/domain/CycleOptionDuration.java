package com.recyclestudy.cycle.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import java.time.Duration;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode
public class CycleOptionDuration {

    @EmbeddedId
    private CycleOptionDurationId id;

    public static CycleOptionDuration of(final CycleOption cycleOption, final Duration duration) {
        return new CycleOptionDuration(CycleOptionDurationId.of(cycleOption, duration));
    }
}
