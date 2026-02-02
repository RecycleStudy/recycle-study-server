package com.recyclestudy.cycle.service.resolver;

import com.recyclestudy.cycle.domain.DefaultCycleOption;
import com.recyclestudy.cycle.domain.selection.DefaultCycleSelection;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DefaultCycleSelectionResolver implements CycleSelectionResolver<DefaultCycleSelection> {

    @Override
    public Class<DefaultCycleSelection> getSupportedType() {
        return DefaultCycleSelection.class;
    }

    @Override
    public List<Duration> resolve(final DefaultCycleSelection selection) {
        return DefaultCycleOption.valueOf(selection.code()).getDurations();
    }
}
