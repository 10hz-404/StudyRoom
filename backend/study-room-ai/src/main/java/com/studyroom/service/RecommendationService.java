package com.studyroom.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final RecommendationStrategy aiDeepSeekStrategy;
    private final RecommendationStrategy ruleBasedStrategy;
    private final AiLimiter aiLimiter;

    public List<Map<String, Object>> getRecommendation(Long userId, List<Map<String, Object>> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            return List.of();
        }

        // 1. 判断是否超出每日调用上限
        if (aiLimiter.isDailyLimitExceeded()) {
            log.info("AI推荐策略切换：今日调用已达上限（50次），自动降级为 RuleBasedStrategy");
            return ruleBasedStrategy.recommend(userId, rooms);
        }

        // 2. 判断是否处于熔断状态
        if (aiLimiter.isCircuitBroken()) {
            log.info("AI推荐策略切换：AI服务处于熔断状态，自动降级为 RuleBasedStrategy");
            return ruleBasedStrategy.recommend(userId, rooms);
        }

        // 3. 执行 AI 推荐
        try {
            List<Map<String, Object>> result = aiDeepSeekStrategy.recommend(userId, rooms);
            // 成功后，重置失败计数
            aiLimiter.resetFailCount();
            // 增加今日调用次数
            aiLimiter.incrDailyCount();
            return result;
        } catch (Exception e) {
            log.error("AI 推荐调用失败，进行熔断计数并降级", e);
            // 记录失败
            aiLimiter.recordFailure();
            // 降级为规则推荐
            return ruleBasedStrategy.recommend(userId, rooms);
        }
    }
}
