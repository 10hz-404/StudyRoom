package com.studyroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.studyroom.entity.*;
import com.studyroom.mapper.*;
import com.studyroom.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class SeatServiceImpl extends ServiceImpl<SeatMapper, Seat> implements SeatService {
    private final ReservationMapper reservationMapper;

    @Override
    public List<Seat> listByRoomId(Long roomId) {
        return list(new LambdaQueryWrapper<Seat>().eq(Seat::getRoomId, roomId).ne(Seat::getStatus, "DELETED"));
    }

    @Override
    public List<Map<String, Object>> listWithAvailability(Long roomId, String date, String startTime, String endTime) {
        List<Seat> seats = listByRoomId(roomId);
        LocalDate d = LocalDate.parse(date);
        LocalTime st = LocalTime.parse(startTime);
        LocalTime et = LocalTime.parse(endTime);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Seat seat : seats) {
            int conflict = reservationMapper.countConflict(seat.getId(), d, st, et);
            Map<String, Object> m = new HashMap<>();
            m.put("id", seat.getId());
            m.put("seatNo", seat.getSeatNo());
            m.put("status", seat.getStatus());
            m.put("roomId", seat.getRoomId());
            m.put("available", conflict == 0 && "AVAILABLE".equals(seat.getStatus()));
            result.add(m);
        }
        return result;
    }
}