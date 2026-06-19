package com.studyroom.client;

import com.studyroom.entity.StudyRoom;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@FeignClient(name = "study-room-room")
public interface RoomFeignClient {
    @GetMapping("/api/v1/rooms/internal/list")
    List<StudyRoom> getRooms();
}
