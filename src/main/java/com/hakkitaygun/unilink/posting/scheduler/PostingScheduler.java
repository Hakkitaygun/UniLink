package com.hakkitaygun.unilink.posting.scheduler;

import com.hakkitaygun.unilink.posting.service.PostingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostingScheduler {

    private final PostingService postingService;

    @Scheduled(fixedDelayString = "${app.scheduling.close-expired-postings-ms:60000}", initialDelay = 10000)
    public void closeExpiredPostings() {
        int closed = postingService.closeExpired();
        if (closed > 0) {
            log.info("Süresi dolan {} ilan kapatıldı.", closed);
        }
    }
}