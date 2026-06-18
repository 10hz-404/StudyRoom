package com.studyroom.controller;

import com.studyroom.common.response.R;
import com.studyroom.dto.LoginDTO;
import com.studyroom.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/auth") @RequiredArgsConstructor @Tag(name = "认证")
public class AuthController {
    private final UserService userService;

    @PostMapping("/login") @Operation(summary = "登录")
    public R<String> login(@RequestBody LoginDTO dto) { return R.ok(userService.login(dto.getStudentNo(), dto.getPassword())); }
}