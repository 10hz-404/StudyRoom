package com.studyroom.controller;

import com.studyroom.common.response.R;
import com.studyroom.entity.Attendance;
import com.studyroom.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/attendance") @RequiredArgsConstructor @Tag(name = "考勤")
public class AttendanceController {
    private final AttendanceService attendanceService;

    @PostMapping("/check-in/{reservationId}") @Operation(summary = "签到")
    public R<Attendance> checkIn(HttpServletRequest request, @PathVariable Long reservationId) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(attendanceService.checkIn(reservationId, userId));
    }

    @PostMapping("/check-out/{reservationId}") @Operation(summary = "签退")
    public R<Attendance> checkOut(HttpServletRequest request, @PathVariable Long reservationId) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(attendanceService.checkOut(reservationId, userId));
    }
}
