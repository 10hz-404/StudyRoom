package com.studyroom.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.studyroom.entity.Seat;
import java.util.List;
import java.util.Map;

public interface SeatService extends IService<Seat> {
    List<Seat> listByRoomId(Long roomId);
    List<Map<String, Object>> listWithAvailability(Long roomId, String date, String startTime, String endTime);
}