package com.studyroom.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service("ruleBasedStrategy")
public class RuleBasedStrategy implements RecommendationStrategy {
    @Override
    public List<Map<String, Object>> recommend(Long userId, List<Map<String, Object>> rooms) {
        return rooms.stream().limit(3).map(r -> {
            Map<String, Object> m = new LinkedHashMap<>(r);
            m.put("score", 0.5);
            m.put("reason", "基础推荐（AI服务暂时不可用）");
            return m;
        }).toList();
    }
}
