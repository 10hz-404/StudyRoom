package com.studyroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("violation")
public class Violation {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private Long reservationId;
    private String violationType;
    private String status;
    private String processRemark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
