package com.cs.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cs.api.common.Result;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 * 
 * @author YK
 * @since 1.0.0
 */
@RestController
@RequestMapping("/health")
@Tag(name = "健康检查", description = "应用健康状态检查接口")
public class HealthController {

    /**
     * 健康检查接口
     */
    @GetMapping
    @Operation(summary = "健康检查", description = "检查应用运行状态")
    public Result<Map<String, Object>> health() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("timestamp", LocalDateTime.now());
        healthInfo.put("application", "Java API Backend");
        healthInfo.put("version", "1.0.0");
        
        return Result.success("应用运行正常", healthInfo);
    }
} 