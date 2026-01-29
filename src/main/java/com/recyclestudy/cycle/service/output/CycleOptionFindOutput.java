package com.recyclestudy.cycle.service.output;

import com.recyclestudy.cycle.domain.CycleOption;
import com.recyclestudy.cycle.domain.CycleOptionDuration;
import com.recyclestudy.cycle.domain.CycleOptionDurationId;
import com.recyclestudy.cycle.domain.CycleOptionTitle;
import java.time.Duration;
import java.util.List;

public record CycleOptionFindOutput(List<CycleOptionElement> options) {

    public static CycleOptionFindOutput from(final List<CycleOption> cycleOptions) {
        final List<CycleOptionElement> optionElements = cycleOptions.stream()
                .map(option -> new CycleOptionElement(
                        option.getId(),
                        option.getTitle(),
                        option.getDurations().stream()
                                .map(CycleOptionDuration::getId)
                                .map(CycleOptionDurationId::getDuration)
                                .toList()
                ))
                .toList();
        return new CycleOptionFindOutput(optionElements);
    }

    public record CycleOptionElement(Long id, CycleOptionTitle title, List<Duration> durations) {
    }
}
