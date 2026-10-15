package com.rag.controller;

import com.rag.model.User;
import com.rag.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AdminController {

    private final UserService userService;

    /**
     * 获取所有用户列表（仅管理员）
     */
    @GetMapping("/users")
    public Map<String, Object> getAllUsers(@RequestParam String adminUsername) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 验证是否为管理员
            User admin = userService.findByUsername(adminUsername).orElse(null);
            if (admin == null || !admin.isAdmin()) {
                response.put("success", false);
                response.put("message", "无权限访问");
                return response;
            }

            List<User> users = userService.findAllUsers();
            // 移除密码字段
            users.forEach(user -> user.setPassword(null));

            response.put("success", true);
            response.put("data", users);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 删除用户（仅管理员）
     */
    @DeleteMapping("/users/{id}")
    public Map<String, Object> deleteUser(@PathVariable Long id, @RequestParam String adminUsername) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 验证是否为管理员
            User admin = userService.findByUsername(adminUsername).orElse(null);
            if (admin == null || !admin.isAdmin()) {
                response.put("success", false);
                response.put("message", "无权限访问");
                return response;
            }

            // 不能删除自己
            if (admin.getId().equals(id)) {
                response.put("success", false);
                response.put("message", "不能删除自己");
                return response;
            }

            userService.deleteUser(id);
            response.put("success", true);
            response.put("message", "用户已删除");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }
}