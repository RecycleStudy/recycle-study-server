package com.recyclestudy.cycle.domain;

import com.recyclestudy.exception.BadRequestException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToMany;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Embeddable
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@EqualsAndHashCode
public class CycleDurations {

    private static final Duration MIN_CYCLE_TERM = Duration.ofMinutes(10);
    private static final Duration MAX_CYCLE_TERM = Duration.ofDays(365);

    @OneToMany(mappedBy = "id.cycleOption", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CycleOptionDuration> values = new ArrayList<>();

    public static CycleDurations from(final List<CycleOptionDuration> values) {
        validate(values);
        return new CycleDurations(values);
    }

    public void replace(final List<CycleOptionDuration> newOptions) {
        validate(newOptions);
        this.values.clear();
        this.values.addAll(newOptions);
    }

    public List<CycleOptionDuration> getValues() {
        return Collections.unmodifiableList(values);
    }

    private static void validate(final List<CycleOptionDuration> cycleOptionDurations) {
        final List<Duration> durations = cycleOptionDurations.stream()
                .map(CycleOptionDuration::getDuration)
                .toList();

        if (durations.isEmpty()) {
            throw new BadRequestException("주기는 최소 1개 이상이어야 합니다.");
        }

        final boolean allTenMinutes = durations.stream()
                .allMatch(duration -> !duration.isNegative() &&
                        !duration.isZero() &&
                        duration.toMinutes() % MIN_CYCLE_TERM.toMinutes() == 0);
        if (!allTenMinutes) {
            throw new BadRequestException("주기는 10분 단위여야 합니다.");
        }

        if (durations.getFirst().compareTo(MIN_CYCLE_TERM) < 0) {
            throw new BadRequestException("첫 번째 주기는 최소 10분 이상이어야 합니다.");
        }

        final Duration lastDuration = durations.getLast();
        if (lastDuration.compareTo(MAX_CYCLE_TERM) > 0) {
            throw new BadRequestException("주기는 최대 1년 이내여야 합니다.");
        }
    }
}
