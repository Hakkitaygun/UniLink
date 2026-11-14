package com.hakkitaygun.unilink.security.risk;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SlidingWindowCounter {

    private static final int MAX_EVENTS_PER_KEY = 2000;
    private static final long IDLE_EVICTION_MS = 15 * 60 * 1000L;

    private final ConcurrentHashMap<String, ArrayDeque<Long>> windows = new ConcurrentHashMap<>();

    /** Olayı kaydeder, son windowMs içindeki toplam olay sayısını döner. */
    public int hit(String key, long nowMs, long windowMs) {
        ArrayDeque<Long> deque = windows.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            deque.addLast(nowMs);
            if (deque.size() > MAX_EVENTS_PER_KEY) {
                deque.pollFirst();
            }
            long cutoff = nowMs - windowMs;
            while (!deque.isEmpty() && deque.peekFirst() < cutoff) {
                deque.pollFirst();
            }
            return deque.size();
        }
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void evictIdle() {
        long cutoff = System.currentTimeMillis() - IDLE_EVICTION_MS;
        windows.entrySet().removeIf(entry -> {
            ArrayDeque<Long> deque = entry.getValue();
            synchronized (deque) {
                return deque.isEmpty() || deque.peekLast() < cutoff;
            }
        });
    }
}