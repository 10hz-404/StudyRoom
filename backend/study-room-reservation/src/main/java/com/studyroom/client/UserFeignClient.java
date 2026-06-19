package com.studyroom.client;

import com.studyroom.entity.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "study-room-user")
public interface UserFeignClient {
    @GetMapping("/api/v1/auth/internal/user/{id}")
    User getUserById(@PathVariable("id") Long id);
}
