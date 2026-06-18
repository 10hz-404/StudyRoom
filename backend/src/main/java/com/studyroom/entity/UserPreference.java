package com.studyroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("user_preference")
public class UserPreference {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private Long preferRoomId;
    private String preferTimePeriod;
    private Integer preferQuietLevel;
    private Integer totalReservations;
    private LocalDateTime updatedTime;
}
