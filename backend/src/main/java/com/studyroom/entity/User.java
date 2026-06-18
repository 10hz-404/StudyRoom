package com.studyroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String studentNo;
    private String name;
    private String password;
    private String role;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
