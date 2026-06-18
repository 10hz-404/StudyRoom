package com.studyroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Data
@TableName("reservation")
public class Reservation {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private Long seatId;
    private LocalDate reserveDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
