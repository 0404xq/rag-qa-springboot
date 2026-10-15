package com.rag.controller;

import com.rag.config.ModelConfig;
import com.rag.service.AgnesAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/model")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ModelController {

    private final ModelConfig modelConfig;
    private final AgnesAIService agnesAIService;

    /**
     * 获取所有可用模型
     */
    @GetMapping("/list")
    public Map<String, Object> listModels() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("models", modelConfig.getModels());
        response.put("current", agnesAIService.getModel());
        return response;
    }

    /**
     * 切换模型
     */
    @PostMapping("/switch")
    public Map<String, Object> switchModel(@RequestParam String model) {
        Map<String, Object> response = new HashMap<>();
        try {
            agnesAIService.setModel(model);
            response.put("success", true);
            response.put("message", "已切换到模型: " + model);
            response.put("current", model);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * 获取当前模型
     */
    @GetMapping("/current")
    public Map<String, Object> getCurrentModel() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("current", agnesAIService.getModel());
        return response;
    }
}