package com.studyroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("study_room")
public class StudyRoom {
    @TableId(type = IdType.AUTO) private Long id;
    private String roomName;
    private String location;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
