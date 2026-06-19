package com.studyroom.service;

import com.studyroom.entity.AiAnalysisLog;
import com.studyroom.mapper.AiAnalysisLogMapper;
import com.studyroom.mapper.UserPreferenceMapper;
import com.studyroom.entity.UserPreference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j @Service @lombok.RequiredArgsConstructor
public class DeepSeekService {
    private final AiAnalysisLogMapper aiLogMapper;
    private final UserPreferenceMapper prefMapper;
    private final AiLimiter aiLimiter;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${deepseek.api-key:demo-key}")
    private String apiKey;
    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;
    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    /** 自习室智能推荐 (无降级的原始方法，由 AI 推荐策略直接调用) */
    public List<Map<String, Object>> recommendRoomsRaw(Long userId, List<Map<String, Object>> rooms) {
        if (rooms.isEmpty()) return List.of();
        UserPreference pref = prefMapper.selectById(userId);
        String prompt = buildRecommendPrompt(rooms, pref);
        String result = callDeepSeek(prompt, "ROOM_RECOMMEND", userId);
        try {
            return objectMapper.readValue(result, List.class);
        } catch (Exception e) {
            throw new RuntimeException("解析AI推荐结果失败: " + e.getMessage(), e);
        }
    }

    /** 自习室智能推荐 */
    public List<Map<String, Object>> recommendRooms(Long userId, List<Map<String, Object>> rooms) {
        try {
            return recommendRoomsRaw(userId, rooms);
        } catch (Exception e) {
            log.warn("AI recommend parse failed, fallback");
            return ruleBasedRecommend(rooms);
        }
    }

    /** 考勤异常分析 */
    public Map<String, Object> analyzeAnomaly(Long userId, Long reservationId, Map<String, Object> context) {
        String prompt = buildAnalysisPrompt(context);
        String result = callDeepSeek(prompt, "ANOMALY_ANALYSIS", userId);
        try { Map<String,Object> m = objectMapper.readValue(result, Map.class); if (m.isEmpty()) return Map.of("anomalyType","UNKNOWN","severity","LOW","reason","AI返回空结果","suggestion","请人工审核","manualReview",true); return m; }
        catch (Exception e) { return Map.of("anomalyType","UNKNOWN","severity","LOW","reason","AI分析暂时不可用","suggestion","请人工审核","manualReview",true); }
    }

    private String callDeepSeek(String prompt, String analysisType, Long userId) {
        // 1. 判断是否超出每日调用上限
        if (aiLimiter.isDailyLimitExceeded()) {
            throw new RuntimeException("今日AI调用次数已达上限（50次），自动拦截并降级");
        }
        // 2. 判断是否处于熔断状态
        if (aiLimiter.isCircuitBroken()) {
            throw new RuntimeException("AI服务处于熔断状态，自动拦截并降级");
        }

        long start = System.currentTimeMillis();
        AiAnalysisLog logEntity = new AiAnalysisLog();
        logEntity.setBizType(analysisType.contains("RECOMMEND") ? "RECOMMEND" : "ANALYSIS");
        logEntity.setUserId(userId); logEntity.setAnalysisType(analysisType); logEntity.setModel(model);
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("messages", List.of(Map.of("role","user","content",prompt)));
            body.put("max_tokens", 600); body.put("temperature", 0.7);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            logEntity.setInputSnapshot(objectMapper.writeValueAsString(body));
            ResponseEntity<String> resp = restTemplate.postForEntity(baseUrl+"/v1/chat/completions", new HttpEntity<>(body,headers), String.class);
            logEntity.setLatencyMs((int)(System.currentTimeMillis()-start));
            logEntity.setResultSnapshot(resp.getBody());
            aiLogMapper.insert(logEntity);
            
            // 成功后，重置失败计数并增加今日计数
            aiLimiter.resetFailCount();
            aiLimiter.incrDailyCount();

            Map<String, Object> respMap = objectMapper.readValue(resp.getBody(), Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) respMap.get("choices");
            if (choices!=null && !choices.isEmpty()) {
                return (String)((Map<String,Object>)choices.get(0).get("message")).get("content");
            }
            return "[]";
        } catch (Exception e) {
            log.error("DeepSeek API failed: {}", e.getMessage());
            logEntity.setLatencyMs((int)(System.currentTimeMillis()-start));
            logEntity.setResultSnapshot("{\"error\":\""+e.getMessage()+"\"}");
            aiLogMapper.insert(logEntity);
            
            // 失败后记录失败计数
            aiLimiter.recordFailure();

            throw new RuntimeException("AI服务调用失败: " + e.getMessage(), e);
        }
    }

    private String buildRecommendPrompt(List<Map<String, Object>> rooms, UserPreference pref) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是校园自习室推荐助手。根据以下信息为学生推荐最合适的3个自习室。\n");
        sb.append("可用自习室："); sb.append(rooms); sb.append("\n");
        if (pref != null) {
            sb.append("学生偏好：安静等级").append(pref.getPreferQuietLevel()).append("/5, 偏好时段").append(pref.getPreferTimePeriod()).append(", 累计预约").append(pref.getTotalReservations()).append("次\n");
        }
        sb.append("请严格返回JSON数组格式：[{\"room_id\":编号,\"score\":评分0~1,\"reason\":\"推荐理由\"}]。只返回JSON，不要其他内容。");
        return sb.toString();
    }

    private String buildAnalysisPrompt(Map<String, Object> ctx) {
        return "你是考勤异常分析助手。根据以下上下文分析异常原因和严重程度。\n上下文："+ctx+
               "\n请严格返回JSON：{\"anomalyType\":\"异常类型\",\"severity\":\"LOW/MEDIUM/HIGH\",\"reason\":\"原因\",\"suggestion\":\"处理建议\",\"manualReview\":true/false}。只返回JSON。";
    }

    public List<Map<String, Object>> ruleBasedRecommend(List<Map<String, Object>> rooms) {
        return rooms.stream().limit(3).map(r -> {
            Map<String, Object> m = new LinkedHashMap<>(r);
            m.put("score",0.5); m.put("reason","基础推荐（AI暂不可用）"); return m;
        }).toList();
    }
}