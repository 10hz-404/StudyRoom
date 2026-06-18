package com.studyroom.service;

import java.util.List;
import java.util.Map;

public interface RecommendationStrategy {
    List<Map<String, Object>> recommend(Long userId, List<Map<String, Object>> rooms);
}
