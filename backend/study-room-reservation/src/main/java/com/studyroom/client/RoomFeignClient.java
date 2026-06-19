package com.studyroom.client;

import com.studyroom.entity.Seat;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "study-room-room")
public interface RoomFeignClient {
    @GetMapping("/api/v1/seats/internal/{id}")
    Seat getSeatById(@PathVariable("id") Long id);

    @PutMapping("/api/v1/seats/internal/{id}/status")
    boolean updateSeatStatus(@PathVariable("id") Long id, @RequestParam("status") String status);
}
