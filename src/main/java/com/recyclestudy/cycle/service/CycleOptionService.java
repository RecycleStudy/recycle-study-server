package com.recyclestudy.cycle.service;

import com.recyclestudy.cycle.domain.CycleOption;
import com.recyclestudy.cycle.domain.DefaultCycleOption;
import com.recyclestudy.cycle.repository.CycleOptionRepository;
import com.recyclestudy.cycle.service.input.CycleOptionSaveInput;
import com.recyclestudy.cycle.service.input.CycleOptionUpdateInput;
import com.recyclestudy.cycle.service.output.CycleOptionFindOutput;
import com.recyclestudy.cycle.service.output.CycleOptionSaveOutput;
import com.recyclestudy.exception.BadRequestException;
import com.recyclestudy.exception.NotFoundException;
import com.recyclestudy.exception.UnauthorizedException;
import com.recyclestudy.member.domain.DeviceIdentifier;
import com.recyclestudy.member.domain.Member;
import com.recyclestudy.member.repository.MemberRepository;
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

        final List<DefaultCycleOption> defaultOptions = DefaultCycleOption.getAll();
        final List<CycleOption> customOptions = cycleOptionRepository.findAllByMember(member);

        return CycleOptionFindOutput.of(defaultOptions, customOptions);
    }

    @Transactional
    public CycleOptionSaveOutput saveCycleOption(final DeviceIdentifier identifier, final CycleOptionSaveInput input) {
        final Member member = memberRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new UnauthorizedException("유효하지 않은 디바이스입니다"));

        validateCycleOptionCount(member);

        final CycleOption cycleOption = CycleOption.withoutId(
                member,
                input.title(),
                input.durations()
        );

        final CycleOption savedCycleOption = cycleOptionRepository.save(cycleOption);
        return CycleOptionSaveOutput.from(savedCycleOption);
    }

    @Transactional
    public CycleOptionSaveOutput updateCycleOption(
            final DeviceIdentifier identifier,
            final Long cycleOptionId,
            final CycleOptionUpdateInput input
    ) {
        final Member member = memberRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new UnauthorizedException("유효하지 않은 디바이스입니다"));

        final CycleOption cycleOption = cycleOptionRepository.findByIdWithDurations(cycleOptionId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 주기 옵션입니다."));

        validateOwnership(cycleOption, member);

        cycleOption.update(input.title(), input.durations());

        return CycleOptionSaveOutput.from(cycleOption);
    }

    @Transactional
    public void deleteCycleOption(final DeviceIdentifier identifier, final Long cycleOptionId) {
        final Member member = memberRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new UnauthorizedException("유효하지 않은 디바이스입니다"));

        final CycleOption cycleOption = cycleOptionRepository.findById(cycleOptionId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 주기 옵션입니다."));

        validateOwnership(cycleOption, member);

        cycleOptionRepository.delete(cycleOption);
    }

    private void validateCycleOptionCount(final Member member) {
        final long count = cycleOptionRepository.countByMember(member);
        if (count >= 5) {
            throw new BadRequestException("커스텀 주기는 최대 5개까지만 생성 가능합니다.");
        }
    }

    private void validateOwnership(final CycleOption cycleOption, final Member member) {
        if (!cycleOption.isOwner(member)) {
            throw new NotFoundException("존재하지 않는 주기 옵션입니다.");
        }
    }
}
