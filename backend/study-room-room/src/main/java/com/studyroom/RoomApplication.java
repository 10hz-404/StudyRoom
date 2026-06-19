package com.studyroom;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.studyroom.mapper")
public class RoomApplication {
    public static void main(String[] args) {
        SpringApplication.run(RoomApplication.class, args);
    }

    @org.springframework.context.annotation.Bean
    public org.springframework.boot.CommandLineRunner initData(org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                jdbcTemplate.execute("DELETE FROM seat WHERE status = 'MAINTENANCE'");
                jdbcTemplate.execute("UPDATE seat SET seat_no = CONCAT(seat_no, '_', id) WHERE status = 'DELETED' AND seat_no NOT LIKE '%\\_%'");
            } catch (Exception e) {
                // ignore
            }
        };
    }
}
