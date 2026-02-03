package com.recyclestudy.cycle.domain.selection;

import com.recyclestudy.cycle.domain.DefaultCycleOption;
import com.recyclestudy.exception.BadRequestException;

public record DefaultCycleSelection(String code) implements CycleSelection {

    public DefaultCycleSelection {
        validateCode(code);
    }

    private static void validateCode(final String code) {
        try {
            DefaultCycleOption.valueOf(code);
        } catch (final IllegalArgumentException e) {
            throw new BadRequestException("존재하지 않는 기본 주기입니다: " + code);
        }
    }
}
