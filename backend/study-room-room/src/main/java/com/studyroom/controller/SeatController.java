package com.studyroom.controller;

import com.studyroom.common.response.R;
import com.studyroom.entity.*;
import com.studyroom.mapper.ReservationMapper;
import com.studyroom.mapper.AttendanceMapper;
import com.studyroom.service.SeatService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/api/v1/seats") @RequiredArgsConstructor @Tag(name = "座位")
public class SeatController {
    private final SeatService seatService;
    private final ReservationMapper reservationMapper;
    private final AttendanceMapper attendanceMapper;

    @GetMapping("/room/{roomId}") @Operation(summary = "获取自习室座位")
    public R<List<Seat>> listByRoom(@PathVariable Long roomId) { return R.ok(seatService.listByRoomId(roomId)); }

    @GetMapping("/room/{roomId}/available") @Operation(summary = "获取座位可用性(含颜色标记)")
    public R<List<Map<String, Object>>> available(@PathVariable Long roomId, @RequestParam String date, @RequestParam String startTime, @RequestParam String endTime) {
        return R.ok(seatService.listWithAvailability(roomId, date, startTime, endTime));
    }

    @PostMapping @Operation(summary = "新增座位")
    public R<Seat> add(@RequestBody Seat seat) { seatService.save(seat); return R.ok(seat); }

    @DeleteMapping("/{id}") @Operation(summary = "删除（停用）座位") @Transactional
    public R<?> delete(@PathVariable Long id) {
        Seat seat = seatService.getById(id);
        if (seat == null) return R.fail(404, "座位不存在");
        // 查出该座位所有 ACTIVE 和 CHECKED_IN 的预约
        List<Reservation> resvs = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>().eq(Reservation::getSeatId, id).in(Reservation::getStatus, "ACTIVE", "CHECKED_IN"));
        for (Reservation resv : resvs) {
            Attendance att = attendanceMapper.selectOne(new LambdaQueryWrapper<Attendance>().eq(Attendance::getReservationId, resv.getId()));
            if ("ACTIVE".equals(resv.getStatus())) {
                resv.setStatus("CANCELLED");
                if (att != null) { att.setStatus("ABNORMAL"); }
            } else if ("CHECKED_IN".equals(resv.getStatus())) {
                resv.setStatus("COMPLETED");
                if (att != null) {
                    att.setStatus("CHECKED_OUT");
                    att.setCheckOutTime(java.time.LocalDateTime.now());
                }
            }
            reservationMapper.updateById(resv);
            if (att != null) { attendanceMapper.updateById(att); }
        }
        seat.setStatus("DELETED");
        seat.setSeatNo(seat.getSeatNo() + "_" + seat.getId());
        seatService.updateById(seat);
        return R.ok();
    }

    @GetMapping("/internal/{id}") @Operation(summary = "内部接口：获取座位详情")
    public Seat getSeatByIdInternal(@PathVariable("id") Long id) {
        return seatService.getById(id);
    }

    @PutMapping("/internal/{id}/status") @Operation(summary = "内部接口：修改座位状态")
    public boolean updateSeatStatusInternal(@PathVariable("id") Long id, @RequestParam("status") String status) {
        Seat seat = seatService.getById(id);
        if (seat != null) {
            seat.setStatus(status);
            return seatService.updateById(seat);
        }
        return false;
    }
}