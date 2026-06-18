package com.studyroom;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
@EnableScheduling
@EnableRabbit
public class StudyRoomApplication {
    public static void main(String[] args) {
        SpringApplication.run(StudyRoomApplication.class, args);
    }

    @Bean
    public CommandLineRunner runner(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                // 1. 物理删除以前遗留的“维护中”状态的座位
                int deleted = jdbcTemplate.update("DELETE FROM seat WHERE status = 'MAINTENANCE'");
                if (deleted > 0) {
                    System.out.println("====== [STUDY_ROOM] 启动初始化：成功物理清理维护状态的历史座位记录共 " + deleted + " 条 ======");
                }
                
                // 2. 兼容处理：把已处于逻辑删除（DELETED）但 seat_no 没被修改过的历史记录加上 _id 标志释放名称
                int updated = jdbcTemplate.update(
                    "UPDATE seat SET seat_no = CONCAT(seat_no, '_', id) " +
                    "WHERE status = 'DELETED' AND seat_no NOT LIKE '%\\_%'"
                );
                if (updated > 0) {
                    System.out.println("====== [STUDY_ROOM] 启动初始化：成功释放历史软删除占用的座位名称共 " + updated + " 条 ======");
                }
            } catch (Exception e) {
                System.err.println("====== [STUDY_ROOM] 启动初始化数据清理失败: " + e.getMessage());
            }
        };
    }
}
