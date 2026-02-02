package com.recyclestudy.cycle.domain.selection;

import com.recyclestudy.exception.BadRequestException;

public record CustomCycleSelection(Long id) implements CycleSelection {

    public CustomCycleSelection {
        validateId(id);
    }

    private static void validateId(final Long id) {
        if (id == null || id <= 0) {
            throw new BadRequestException("유효하지 않은 커스텀 주기 ID입니다: " + id);
        }
    }
}
