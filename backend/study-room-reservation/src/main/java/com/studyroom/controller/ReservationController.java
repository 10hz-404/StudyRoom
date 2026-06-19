package com.studyroom.controller;

import com.studyroom.common.response.R;
import com.studyroom.dto.ReservationDTO;
import com.studyroom.entity.Reservation;
import com.studyroom.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/reservations") @RequiredArgsConstructor @Tag(name = "预约")
public class ReservationController {
    private final ReservationService reservationService;

    @PostMapping @Operation(summary = "提交预约")
    public R<Reservation> create(HttpServletRequest request, @RequestBody ReservationDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(reservationService.create(userId, dto));
    }

    @GetMapping("/my") @Operation(summary = "我的预约")
    public R<List<Reservation>> my(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(reservationService.listByUser(userId));
    }

    @GetMapping("/all") @Operation(summary = "全部预约（管理员）")
    public R<List<Reservation>> all() {
        return R.ok(reservationService.list());
    }

    @PutMapping("/{id}/cancel") @Operation(summary = "取消预约")
    public R<?> cancel(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        reservationService.cancel(id, userId);
        return R.ok();
    }

    @GetMapping("/internal/{id}") @Operation(summary = "内部接口：获取预约详情")
    public Reservation getReservationByIdInternal(@PathVariable("id") Long id) {
        return reservationService.getById(id);
    }

    @PutMapping("/internal/{id}/status") @Operation(summary = "内部接口：修改预约状态")
    public boolean updateReservationStatusInternal(@PathVariable("id") Long id, @RequestParam("status") String status) {
        Reservation r = reservationService.getById(id);
        if (r != null) {
            r.setStatus(status);
            return reservationService.updateById(r);
        }
        return false;
    }
}