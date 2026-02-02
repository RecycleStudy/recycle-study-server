package com.recyclestudy.review.controller.request;

import com.recyclestudy.cycle.domain.selection.CycleSelection;

public record ReviewSaveRequest(String targetUrl, CycleSelection cycle) {
}
