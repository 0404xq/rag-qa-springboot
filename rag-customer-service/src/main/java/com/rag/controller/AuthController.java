package com.rag.controller;

import com.rag.model.LoginRequest;
import com.rag.model.LoginResponse;
import com.rag.model.User;
import com.rag.service.UserService;
import com.rag.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = request.get("username");
            String password = request.get("password");
            String email = request.getOrDefault("email", "");

            if (username == null || username.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "用户名不能为空");
                return response;
            }
            if (password == null || password.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "密码不能为空");
                return response;
            }
            if (password.length() < 6) {
                response.put("success", false);
                response.put("message", "密码长度不能少于6位");
                return response;
            }

            User user = userService.register(username.trim(), password.trim(), email.trim());
            response.put("success", true);
            response.put("message", "注册成功，请登录");
            response.put("username", user.getUsername());
        } catch (Exception e) {
            log.error("注册失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = request.get("username");
            String password = request.get("password");

            if (username == null || username.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "用户名不能为空");
                return response;
            }
            if (password == null || password.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "密码不能为空");
                return response;
            }

            User user = userService.login(username.trim(), password.trim());
            String token = jwtUtil.generateToken(user.getUsername(), user.isAdmin());

            response.put("success", true);
            response.put("message", "登录成功");
            response.put("token", token);
            response.put("username", user.getUsername());
            response.put("admin", user.isAdmin());
        } catch (Exception e) {
            log.error("登录失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }
}