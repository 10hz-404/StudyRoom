package com.studyroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("ai_analysis_log")
public class AiAnalysisLog {
    @TableId(type = IdType.AUTO) private Long id;
    private String bizType;
    private Long bizId;
    private Long userId;
    private String analysisType;
    private String inputSnapshot;
    private String resultSnapshot;
    private String model;
    private Integer latencyMs;
    private LocalDateTime createTime;
}
