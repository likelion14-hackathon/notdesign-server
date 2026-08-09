package com.likelionknu.notdesign.user.controller;

import com.likelionknu.notdesign.common.response.GlobalResponse;
import com.likelionknu.notdesign.common.security.SecurityUtil;
import com.likelionknu.notdesign.user.data.dto.response.UserResponseDto;
import com.likelionknu.notdesign.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PatchMapping("/me")
    @Operation(summary = "측정 동의")
    public GlobalResponse<UserResponseDto> agreeMeasurement() {
        return GlobalResponse.ok(userService.agreeMeasurement(SecurityUtil.getUsername()));
    }
}
