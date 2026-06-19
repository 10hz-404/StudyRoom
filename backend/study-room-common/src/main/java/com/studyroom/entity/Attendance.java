package com.studyroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("attendance")
public class Attendance {
    @TableId(type = IdType.AUTO) private Long id;
    private Long reservationId;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
