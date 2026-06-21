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
public class UserApplication {
    public static void main(String[] args) {
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        String hash1 = "$2a$12$LJ3m4ys3Lg3Gxj6fzP5KoeUyWEiOmFSPQhjYgkqKpE7XvGy1AHu6e";
        String hash2 = "$2a$10$vKss9mS1z6TszJc2qR/I3.X2WfOQ9j6CpxR0.M0e0zHj2aG1O2vKm";
        System.out.println("=== DIAGNOSTIC: Hash1 matches 123456: " + encoder.matches("123456", hash1));
        System.out.println("=== DIAGNOSTIC: Hash2 matches 123456: " + encoder.matches("123456", hash2));
        SpringApplication.run(UserApplication.class, args);
    }
}
