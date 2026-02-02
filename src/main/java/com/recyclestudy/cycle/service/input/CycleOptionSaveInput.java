package com.recyclestudy.cycle.service.input;

import com.recyclestudy.cycle.domain.CycleOptionTitle;
import com.recyclestudy.exception.BadRequestException;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.List;

public record CycleOptionSaveInput(CycleOptionTitle title, List<Duration> durations) {

    public static CycleOptionSaveInput of(final String title, final List<String> durations) {
        final CycleOptionTitle cycleOptionTitle = CycleOptionTitle.from(title);
        try {
            final List<Duration> cycleOptionDurations = durations.stream()
                    .map(Duration::parse)
                    .toList();
            return new CycleOptionSaveInput(cycleOptionTitle, cycleOptionDurations);
        } catch (final DateTimeParseException e) {
            throw new BadRequestException("잘못된 주기 형식입니다: ");
        }
    }
}
