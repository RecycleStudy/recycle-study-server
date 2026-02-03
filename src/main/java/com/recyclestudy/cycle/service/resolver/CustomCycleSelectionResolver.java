package com.recyclestudy.cycle.service.resolver;

import com.recyclestudy.cycle.domain.CycleOption;
import com.recyclestudy.cycle.domain.CycleOptionDuration;
import com.recyclestudy.cycle.domain.selection.CustomCycleSelection;
import com.recyclestudy.cycle.repository.CycleOptionRepository;
import com.recyclestudy.exception.NotFoundException;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomCycleSelectionResolver implements CycleSelectionResolver<CustomCycleSelection> {

    private final CycleOptionRepository cycleOptionRepository;

    @Override
    public Class<CustomCycleSelection> getSupportedType() {
        return CustomCycleSelection.class;
    }

    @Override
    public List<Duration> resolve(final CustomCycleSelection selection) {
        final CycleOption cycleOption = cycleOptionRepository.findById(selection.id())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 주기 옵션입니다."));

        return cycleOption.getDurations().stream()
                .map(CycleOptionDuration::getDuration)
                .toList();
    }
}
