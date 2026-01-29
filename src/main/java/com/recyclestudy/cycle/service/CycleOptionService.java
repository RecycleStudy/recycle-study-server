package com.recyclestudy.cycle.service;

import com.recyclestudy.cycle.domain.CycleOption;
import com.recyclestudy.cycle.repository.CycleOptionRepository;
import com.recyclestudy.cycle.service.output.CycleOptionFindOutput;
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
}
