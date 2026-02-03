package com.recyclestudy.email;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailRetryScheduler {

    private final EmailRetryService emailRetryService;

    @Scheduled(fixedDelay = 60_000)
    public void runRetry() {
        emailRetryService.retryFailedEmails();
    }
}
