package com.rag.config;

import com.rag.service.DocumentService;
import com.rag.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class InitConfig implements CommandLineRunner {

    private final DocumentService documentService;
    private final UserService userService;

    @Override
    public void run(String... args) throws Exception {
        // 1. 初始化 Elasticsearch 索引
        try {
            documentService.initIndex();
            log.info("✅ Elasticsearch 索引初始化完成");
        } catch (Exception e) {
            log.warn("⚠️ Elasticsearch 索引初始化失败: {}", e.getMessage());
        }

        // 2. 创建管理员用户
        try {
            userService.findByUsername("admin").ifPresent(user -> {
                userService.deleteUser(user.getId());
            });
            var admin = userService.register("admin", "123456", "admin@example.com");
            admin.setAdmin(true);
            userService.updateUser(admin);
            log.info("✅ 管理员创建成功: admin / 123456");
        } catch (Exception e) {
            log.warn("⚠️ 管理员创建失败: {}", e.getMessage());
        }

        log.info("========================================");
        log.info("✅ RAG Customer Service 启动成功！");
        log.info("🌐 访问地址: http://localhost:8080");
        log.info("👑 管理员账号: admin / 123456");
        log.info("========================================");
    }
}