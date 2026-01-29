package com.recyclestudy.cycle.service;

import com.recyclestudy.cycle.domain.CycleOption;
import com.recyclestudy.cycle.domain.CycleOptionDuration;
import com.recyclestudy.cycle.domain.OptionType;
import com.recyclestudy.cycle.repository.CycleOptionRepository;
import com.recyclestudy.cycle.service.input.CycleOptionSaveInput;
import com.recyclestudy.cycle.service.output.CycleOptionFindOutput;
import com.recyclestudy.cycle.service.output.CycleOptionSaveOutput;
import com.recyclestudy.exception.BadRequestException;
import com.recyclestudy.exception.UnauthorizedException;
import com.recyclestudy.member.domain.DeviceIdentifier;
import com.recyclestudy.member.domain.Member;
import com.recyclestudy.member.repository.MemberRepository;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CycleOptionService {

    private final MemberRepository memberRepository;
    private final CycleOptionRepository cycleOptionRepository;

    @Transactional(readOnly = true)
    public CycleOptionFindOutput findAllCycleOptions(final DeviceIdentifier identifier) {
        final Member member = memberRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new UnauthorizedException("유효하지 않은 디바이스입니다"));
        final List<CycleOption> cycleOptions = cycleOptionRepository.findAllByMember(member);
        return CycleOptionFindOutput.from(cycleOptions);
    }

    @Transactional
    public CycleOptionSaveOutput saveCycleOption(final DeviceIdentifier identifier, final CycleOptionSaveInput input) {
        final Member member = memberRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new UnauthorizedException("유효하지 않은 디바이스입니다"));

        validateCycleOptionCount(member);
        validateDurations(input.durations());

        final CycleOption cycleOption = CycleOption.withoutId(member, input.title(), OptionType.CUSTOM);
        final List<CycleOptionDuration> durations = input.durations().stream()
                .map(duration -> CycleOptionDuration.of(cycleOption, duration))
                .toList();

        cycleOption.addDurations(durations);

        final CycleOption savedCycleOption = cycleOptionRepository.save(cycleOption);
        return CycleOptionSaveOutput.from(savedCycleOption);
    }

    private void validateCycleOptionCount(final Member member) {
        final List<CycleOption> cycleOptions = cycleOptionRepository.findAllByMember(member);
        long customCount = cycleOptions.stream()
                .filter(option -> option.getOptionType() == OptionType.CUSTOM)
                .count();

        if (customCount >= 5) {
            throw new BadRequestException("커스텀 주기는 최대 5개까지만 생성 가능합니다.");
        }
    }

    private void validateDurations(final List<Duration> durations) {
        if (durations.isEmpty()) {
            throw new BadRequestException("주기는 최소 1개 이상이어야 합니다.");
        }

        boolean allTenMinutes = durations.stream()
                .allMatch(d -> d.toMinutes() % 10 == 0 && d.toMinutes() > 0);
        if (!allTenMinutes) {
            throw new BadRequestException("주기는 10분 단위여야 합니다.");
        }

        if (durations.get(0).toMinutes() < 10) {
            throw new BadRequestException("첫 번째 주기는 최소 10분 이상이어야 합니다.");
        }

        Duration lastDuration = durations.get(durations.size() - 1);
        if (lastDuration.toDays() > 365) {
            throw new BadRequestException("주기는 최대 1년 이내여야 합니다.");
        }
    }
}