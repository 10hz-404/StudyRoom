package com.studyroom.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiLimiter {
    private final StringRedisTemplate redisTemplate;

    // JVM 本地降级备用熔断计数器（当 Redis 不可用时）
    private static final AtomicInteger localFailCount = new AtomicInteger(0);
    private static final AtomicLong localCircuitBreakerUntil = new AtomicLong(0L);
    private static final AtomicInteger localDailyCount = new AtomicInteger(0);
    private static String localCurrentDate = LocalDate.now().toString();

    private static final int FAIL_THRESHOLD = 5;
    private static final long CIRCUIT_BREAK_MS = 60 * 1000L;
    private static final int DAILY_LIMIT = 50;

    public boolean isDailyLimitExceeded() {
        String today = LocalDate.now().toString();
        try {
            String key = "ai:daily_count:" + today;
            String val = redisTemplate.opsForValue().get(key);
            if (val != null && Integer.parseInt(val) >= DAILY_LIMIT) {
                return true;
            }
            return false;
        } catch (Exception e) {
            // Redis 异常时使用本地内存
            synchronized (localDailyCount) {
                if (!today.equals(localCurrentDate)) {
                    localCurrentDate = today;
                    localDailyCount.set(0);
                }
                return localDailyCount.get() >= DAILY_LIMIT;
            }
        }
    }

    public void incrDailyCount() {
        String today = LocalDate.now().toString();
        try {
            String key = "ai:daily_count:" + today;
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, 2, TimeUnit.DAYS);
        } catch (Exception e) {
            synchronized (localDailyCount) {
                if (!today.equals(localCurrentDate)) {
                    localCurrentDate = today;
                    localDailyCount.set(0);
                }
                localDailyCount.incrementAndGet();
            }
        }
    }

    public boolean isCircuitBroken() {
        try {
            String brokenFlag = redisTemplate.opsForValue().get("ai:circuit_broken");
            return "true".equals(brokenFlag);
        } catch (Exception e) {
            return System.currentTimeMillis() < localCircuitBreakerUntil.get();
        }
    }

    public void recordFailure() {
        try {
            String countKey = "ai:fail_count";
            Long val = redisTemplate.opsForValue().increment(countKey);
            redisTemplate.expire(countKey, 5, TimeUnit.MINUTES);
            if (val != null && val >= FAIL_THRESHOLD) {
                log.warn("AI 接口连续失败达到 5 次，触发熔断 60 秒！");
                redisTemplate.opsForValue().set("ai:circuit_broken", "true", 60, TimeUnit.SECONDS);
                // 重置计数
                redisTemplate.delete(countKey);
            }
        } catch (Exception e) {
            int current = localFailCount.incrementAndGet();
            if (current >= FAIL_THRESHOLD) {
                log.warn("AI 接口连续失败达到 5 次（本地），触发熔断 60 秒！");
                localCircuitBreakerUntil.set(System.currentTimeMillis() + CIRCUIT_BREAK_MS);
                localFailCount.set(0);
            }
        }
    }

    public void resetFailCount() {
        try {
            redisTemplate.delete("ai:fail_count");
        } catch (Exception e) {
            localFailCount.set(0);
        }
    }
}
