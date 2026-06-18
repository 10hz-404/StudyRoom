package com.studyroom.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyroom.common.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiRecommendListener {
    private final RecommendationService recommendationService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = RabbitMQConfig.AI_RECOMMEND_QUEUE)
    public void handleRecommendTask(String messageJson) {
        log.info("收到 AI 推荐异步任务消息：{}", messageJson);
        try {
            Map<String, Object> msg = objectMapper.readValue(messageJson, Map.class);
            String taskId = (String) msg.get("taskId");
            Number userIdNum = (Number) msg.get("userId");
            Long userId = userIdNum != null ? userIdNum.longValue() : null;
            List<Map<String, Object>> rooms = (List<Map<String, Object>>) msg.get("rooms");

            if (taskId == null) {
                log.error("AI 推荐异步任务缺失 taskId");
                return;
            }

            // 执行推荐策略（已集成熔断和限流降级）
            List<Map<String, Object>> result = recommendationService.getRecommendation(userId, rooms);

            // 将结果写入 Redis，保存 5 分钟
            redisTemplate.opsForValue().set("ai_recommend:status:" + taskId, "SUCCESS", 5, TimeUnit.MINUTES);
            redisTemplate.opsForValue().set("ai_recommend:result:" + taskId, objectMapper.writeValueAsString(result), 5, TimeUnit.MINUTES);
            log.info("AI 推荐异步任务处理成功，taskId: {}", taskId);
        } catch (Exception e) {
            log.error("AI 推荐异步任务处理异常", e);
            try {
                Map<String, Object> msg = objectMapper.readValue(messageJson, Map.class);
                String taskId = (String) msg.get("taskId");
                if (taskId != null) {
                    redisTemplate.opsForValue().set("ai_recommend:status:" + taskId, "FAILED", 5, TimeUnit.MINUTES);
                }
            } catch (Exception ex) {
                // ignore
            }
        }
    }
}
