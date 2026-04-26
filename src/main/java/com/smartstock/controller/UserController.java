package com.smartstock.controller;

import com.smartstock.common.Result;
import com.smartstock.dto.PasswordChangeDTO;
import com.smartstock.dto.PhoneLoginDTO;
import com.smartstock.dto.PhoneRegisterDTO;
import com.smartstock.dto.UserLoginDTO;
import com.smartstock.dto.UserRegisterDTO;
import com.smartstock.dto.UserUpdateDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.smartstock.service.UserService;
import com.smartstock.util.UserContext;
import com.smartstock.vo.LoginVO;
import com.smartstock.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "User", description = "用户注册、登录与资料接口")
public class UserController {

    private final UserService userService;

    @PostMapping("/api/auth/register")
    public Result<UserVO> register(@Valid @RequestBody UserRegisterDTO dto) {
        return Result.ok(userService.register(dto));
    }

    @PostMapping("/api/auth/register/phone")
    public Result<UserVO> registerByPhone(@Valid @RequestBody PhoneRegisterDTO dto) {
        return Result.ok(userService.registerByPhone(dto));
    }

    @PostMapping("/api/auth/login")
    public Result<LoginVO> login(@Valid @RequestBody UserLoginDTO dto) {
        return Result.ok(userService.login(dto));
    }

    @PostMapping("/api/auth/login/phone")
    public Result<LoginVO> loginByPhone(@Valid @RequestBody PhoneLoginDTO dto) {
        return Result.ok(userService.loginByPhone(dto));
    }

    @PostMapping("/api/auth/logout")
    public Result<Void> logout(HttpServletRequest request) {
        userService.logout(UserContext.getToken(request));
        return Result.ok();
    }

    @GetMapping("/api/users/me")
    public Result<UserVO> getMe(HttpServletRequest request) {
        Long userId = UserContext.getUserId(request);
        return Result.ok(userService.getUserById(userId));
    }

    @PutMapping("/api/users/me")
    public Result<UserVO> updateMe(HttpServletRequest request, @Valid @RequestBody UserUpdateDTO dto) {
        Long userId = UserContext.getUserId(request);
        return Result.ok(userService.updateUser(userId, dto));
    }

    @PutMapping("/api/users/me/password")
    public Result<Void> changePassword(HttpServletRequest request, @Valid @RequestBody PasswordChangeDTO dto) {
        Long userId = UserContext.getUserId(request);
        userService.changePassword(userId, dto);
        return Result.ok();
    }
}
