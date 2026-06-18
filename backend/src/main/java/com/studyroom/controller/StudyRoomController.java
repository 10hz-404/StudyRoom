package com.studyroom.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyroom.common.response.R;
import com.studyroom.entity.*;
import com.studyroom.mapper.*;
import com.studyroom.service.StudyRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/rooms") @RequiredArgsConstructor @Tag(name = "自习室")
public class StudyRoomController {
    private final StudyRoomService roomService;
    private final SeatMapper seatMapper;
    private final ReservationMapper reservationMapper;
    private final AttendanceMapper attendanceMapper;

    @GetMapping @Operation(summary = "获取所有未删除自习室")
    public R<List<StudyRoom>> list() { return R.ok(roomService.lambdaQuery().ne(StudyRoom::getStatus, "DELETED").list()); }

    @PostMapping @Operation(summary = "新增自习室")
    public R<StudyRoom> add(@RequestBody StudyRoom room) { roomService.save(room); return R.ok(room); }

    @PutMapping @Operation(summary = "更新自习室")
    public R<?> update(@RequestBody StudyRoom room) { roomService.updateById(room); return R.ok(); }

    @DeleteMapping("/{id}") @Operation(summary = "逻辑删除自习室（级联删除所有座位及取消未签到预约）") @Transactional
    public R<?> delete(@PathVariable Long id) {
        StudyRoom r = roomService.getById(id);
        if (r == null) return R.fail(404, "自习室不存在");
        // 查出该自习室所有未删除座位
        List<Seat> seats = seatMapper.selectList(new LambdaQueryWrapper<Seat>().eq(Seat::getRoomId, id).ne(Seat::getStatus, "DELETED"));
        for (Seat seat : seats) {
            // 查出该座位所有未签到/使用中的预约
            List<Reservation> resvs = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>().eq(Reservation::getSeatId, seat.getId()).in(Reservation::getStatus, "ACTIVE", "CHECKED_IN"));
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
            // 级联逻辑删除座位并重命名释放座位号
            seat.setStatus("DELETED");
            seat.setSeatNo(seat.getSeatNo() + "_" + seat.getId());
            seatMapper.updateById(seat);
        }
        r.setStatus("DELETED");
        r.setRoomName(r.getRoomName() + "_" + r.getId());
        roomService.updateById(r);
        return R.ok();
    }
}