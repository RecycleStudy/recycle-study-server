package com.recyclestudy.cycle.service.output;

import com.recyclestudy.cycle.domain.CycleOption;
import com.recyclestudy.cycle.domain.CycleOptionDuration;
import com.recyclestudy.cycle.domain.CycleOptionDurationId;
import com.recyclestudy.cycle.domain.DefaultCycleOption;
import java.time.Duration;
import java.util.List;

public record CycleOptionFindOutput(
        List<DefaultOptionElement> defaultOptions,
        List<CustomOptionElement> customOptions
) {

    public static CycleOptionFindOutput of(
            final List<DefaultCycleOption> defaultCycleOptions,
            final List<CycleOption> customCycleOptions
    ) {
        final List<DefaultOptionElement> defaultElements = defaultCycleOptions.stream()
                .map(option -> new DefaultOptionElement(
                        option.name(),
                        option.getTitle(),
                        option.getDurations()
                ))
                .toList();

        final List<CustomOptionElement> customElements = customCycleOptions.stream()
                .map(option -> new CustomOptionElement(
                        option.getId(),
                        option.getTitle().getValue(),
                        option.getDurations().stream()
                                .map(CycleOptionDuration::getId)
                                .map(CycleOptionDurationId::getDuration)
                                .toList()
                ))
                .toList();

        return new CycleOptionFindOutput(defaultElements, customElements);
    }

    public record DefaultOptionElement(String code, String title, List<Duration> durations) {
    }

    public record CustomOptionElement(Long id, String title, List<Duration> durations) {
    }
}
