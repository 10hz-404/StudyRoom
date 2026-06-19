package com.studyroom.controller;

import com.studyroom.common.response.R;
import com.studyroom.entity.AiAnalysisLog;
import com.studyroom.entity.StudyRoom;
import com.studyroom.service.AiAnalysisLogService;
import com.studyroom.service.DeepSeekService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/v1/ai") @RequiredArgsConstructor @Tag(name = "AI服务")
public class AiController {
    private final AiAnalysisLogService aiService;
    private final DeepSeekService deepSeekService;
    private final com.studyroom.client.RoomFeignClient roomFeignClient;
    private final com.studyroom.service.RecommendationService recommendationService;
    private final org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @GetMapping("/logs") @Operation(summary = "AI分析日志查询")
    public R<List<AiAnalysisLog>> logs() { return R.ok(aiService.list()); }

    @GetMapping("/logs/{bizType}") @Operation(summary = "按类型获取日志")
    public R<List<AiAnalysisLog>> byType(@PathVariable String bizType) {
        return R.ok(aiService.lambdaQuery().eq(AiAnalysisLog::getBizType, bizType).list());
    }

    @PostMapping("/recommend") @Operation(summary = "AI推荐自习室 (同步兼容模式)")
    public R<?> recommend(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<StudyRoom> rooms = roomFeignClient.getRooms();
        List<Map<String, Object>> roomData = new ArrayList<>();
        for (StudyRoom r : rooms) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId()); m.put("name", r.getRoomName()); m.put("location", r.getLocation());
            roomData.add(m);
        }
        try {
            return R.ok(recommendationService.getRecommendation(userId, roomData));
        } catch (Exception e) {
            return R.fail("推荐失败: " + e.getMessage());
        }
    }

    @PostMapping("/recommend/task") @Operation(summary = "AI推荐任务提交 (异步模式第一步)")
    public R<String> submitRecommendTask(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<StudyRoom> rooms = roomFeignClient.getRooms();
        List<Map<String, Object>> roomData = new ArrayList<>();
        for (StudyRoom r : rooms) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId()); m.put("name", r.getRoomName()); m.put("location", r.getLocation());
            roomData.add(m);
        }
        String taskId = UUID.randomUUID().toString();
        try {
            // 1. 在 Redis 初始化任务状态
            redisTemplate.opsForValue().set("ai_recommend:status:" + taskId, "PROCESSING", 10, java.util.concurrent.TimeUnit.MINUTES);
            
            // 2. 发送推荐请求到 RabbitMQ 队列
            Map<String, Object> message = Map.of("taskId", taskId, "userId", userId, "rooms", roomData);
            rabbitTemplate.convertAndSend(com.studyroom.common.config.RabbitMQConfig.AI_EXCHANGE, com.studyroom.common.config.RabbitMQConfig.AI_RECOMMEND_ROUTING_KEY, objectMapper.writeValueAsString(message));
            
            return R.ok(taskId);
        } catch (Exception e) {
            return R.fail("提交AI推荐任务失败: " + e.getMessage());
        }
    }

    @GetMapping("/recommend/result/{taskId}") @Operation(summary = "轮询获取AI推荐结果 (异步模式第二步)")
    public R<?> getRecommendResult(@PathVariable String taskId) {
        try {
            String status = redisTemplate.opsForValue().get("ai_recommend:status:" + taskId);
            if (status == null) {
                return R.fail("任务不存在或已过期");
            }
            if ("PROCESSING".equals(status)) {
                return R.ok(Map.of("status", "PROCESSING"));
            }
            if ("SUCCESS".equals(status)) {
                String resultJson = redisTemplate.opsForValue().get("ai_recommend:result:" + taskId);
                List<?> list = objectMapper.readValue(resultJson, List.class);
                return R.ok(Map.of("status", "SUCCESS", "data", list));
            }
            // 兜底（如果失败则采用规则推荐保底）
            return R.ok(Map.of("status", "FAILED"));
        } catch (Exception e) {
            return R.fail("获取推荐结果异常: " + e.getMessage());
        }
    }

    @PostMapping("/analyze") @Operation(summary = "AI考勤异常分析 - 基于DeepSeek")
    public R<?> analyze(HttpServletRequest request, @RequestParam Long reservationId, @RequestBody Map<String, Object> context) {
        Long userId = (Long) request.getAttribute("userId");
        try {
            return R.ok(deepSeekService.analyzeAnomaly(userId, reservationId, context));
        } catch (Exception e) {
            return R.fail("AI分析失败: " + e.getMessage());
        }
    }

    @GetMapping("/health") @Operation(summary = "检测AI服务健康")
    public R<?> health() {
        return R.ok(Map.of("status", "available", "model", "deepseek-chat", "note", "请在application.yml配置密钥"));
    }
}
