package com.tvz.mediaapp.backend.scheduler;

import com.tvz.mediaapp.backend.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class StorageCleanupScheduler {

    private final StorageService storageService;

    @Scheduled(fixedDelay = 3600000, initialDelay = 300000) // 1 hour = 3600000ms, 5 min = 300000ms
    public void cleanupPendingUploads() {
        log.info("Starting scheduled cleanup of pending uploads");
        try {
            storageService.cleanupPendingUploads();
            log.info("Completed scheduled cleanup of pending uploads");
        } catch (Exception e) {
            log.error("Error during scheduled cleanup of pending uploads", e);
        }
    }
}