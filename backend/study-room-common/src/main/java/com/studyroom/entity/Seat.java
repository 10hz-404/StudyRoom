package com.studyroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("seat")
public class Seat {
    @TableId(type = IdType.AUTO) private Long id;
    private Long roomId;
    private String seatNo;
    private String status;
    private LocalDateTime createTime;
}
