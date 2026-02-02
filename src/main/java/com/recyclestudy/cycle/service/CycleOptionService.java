package com.recyclestudy.cycle.service;

import com.recyclestudy.cycle.domain.CycleOption;
import com.recyclestudy.cycle.domain.OptionType;
import com.recyclestudy.cycle.repository.CycleOptionRepository;
import com.recyclestudy.cycle.service.input.CycleOptionSaveInput;
import com.recyclestudy.cycle.service.input.CycleOptionUpdateInput;
import com.recyclestudy.cycle.service.output.CycleOptionFindOutput;
import com.recyclestudy.cycle.service.output.CycleOptionSaveOutput;
import com.recyclestudy.exception.BadRequestException;
import com.recyclestudy.exception.ForbiddenException;
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
        final List<CycleOption> cycleOptions = cycleOptionRepository.findAllByMember(member);
        return CycleOptionFindOutput.from(cycleOptions);
    }

    @Transactional
    public CycleOptionSaveOutput saveCycleOption(final DeviceIdentifier identifier, final CycleOptionSaveInput input) {
        final Member member = memberRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new UnauthorizedException("유효하지 않은 디바이스입니다"));

        validateCycleOptionCount(member);

        final CycleOption cycleOption = CycleOption.withoutId(
                member,
                input.title(),
                OptionType.CUSTOM,
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

        final CycleOption cycleOption = cycleOptionRepository.findById(cycleOptionId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 주기 옵션입니다."));

        validateOwnership(cycleOption, member);
        validateOptionType(cycleOption);

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
        validateOptionType(cycleOption);

        cycleOptionRepository.delete(cycleOption);
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

    private void validateOwnership(final CycleOption cycleOption, final Member member) {
        if (!cycleOption.isOwner(member)) {
            throw new ForbiddenException("해당 주기 옵션에 대한 권한이 없습니다.");
        }
    }

    private void validateOptionType(final CycleOption cycleOption) {
        if (cycleOption.getOptionType() != OptionType.CUSTOM) {
            throw new ForbiddenException("기본 주기는 수정/삭제할 수 없습니다.");
        }
    }
}
