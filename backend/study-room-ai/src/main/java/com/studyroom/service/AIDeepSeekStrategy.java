package com.studyroom.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service("aiDeepSeekStrategy")
@RequiredArgsConstructor
public class AIDeepSeekStrategy implements RecommendationStrategy {
    private final DeepSeekService deepSeekService;

    @Override
    public List<Map<String, Object>> recommend(Long userId, List<Map<String, Object>> rooms) {
        return deepSeekService.recommendRoomsRaw(userId, rooms);
    }
}
