package com.cs.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.cs.api.common.Result;
import com.cs.api.dto.CodeTraceRequestDTO;
import com.cs.api.dto.CodeTraceResponseDTO;
import com.cs.api.service.CodeTraceService;

/**
 * Linux内核代码追溯控制器
 * 提供函数和结构体的演化历史追溯功能
 * 
 * @author YK
 * @since 1.0.0
 */
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/code-trace")
@Tag(name = "代码追溯", description = "Linux内核代码演化历史追溯相关接口")
public class CodeTraceController {

    private static final Logger logger = LoggerFactory.getLogger(CodeTraceController.class);
    
    @Autowired
    private CodeTraceService codeTraceService;
    
    /**
     * 追溯函数或结构体的演化历史
     */
    @PostMapping("/method/history")
    @Operation(
        summary = "追溯方法演化历史", 
        description = "根据文件路径、方法名和版本追溯函数或结构体的commit演化历史"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "追溯成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "404", description = "指定版本不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<CodeTraceResponseDTO> traceMethodHistory(
            @Valid @RequestBody CodeTraceRequestDTO traceRequest) {
        logger.info("接收代码追溯请求: {}", traceRequest);
        
        try {  
            // 执行代码追溯
            CodeTraceResponseDTO traceResult = codeTraceService.traceMethodHistory(
                traceRequest.getFilePath(), 
                traceRequest.getMethodName(), 
                traceRequest.getVersion(),
                traceRequest.getTargetCommit()
            );
            
            if (!traceResult.isSuccess()) {
                logger.warn("代码追溯失败: {}", traceResult.getErrorMessage());
                return Result.error(400, traceResult.getErrorMessage());
            }
            
            if (traceResult.getCommitHistory() == null) {
                logger.info("未找到匹配的commit历史: methodName={}, filePath={}, version={}", 
                    traceRequest.getMethodName(), traceRequest.getFilePath(), traceRequest.getVersion());
                return Result.success("未找到该方法的演化历史", traceResult);
            }
            
            logger.info("代码追溯成功，找到commit历史:{}", traceResult.getCommitHistory());
            return Result.success(traceResult);
            
        } catch (IllegalArgumentException e) {
            logger.error("请求参数错误: {}", e.getMessage());
            return Result.error(400, "请求体格式错误或缺少必须的参数: " + e.getMessage());
        } catch (Exception e) {
            logger.error("代码追溯服务异常", e);
            return Result.error(500, "服务器内部发生错误，代码追溯服务失败: " + e.getMessage());
        }
    }
    
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    @Operation(
        summary = "服务健康检查",
        description = "检查代码追溯服务的健康状态"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "服务正常"),
        @ApiResponse(responseCode = "503", description = "服务不可用")
    })
    public Result<String> healthCheck() {
        logger.debug("代码追溯服务健康检查");
        
        try {
            // 简单的健康检查逻辑
            return Result.success("代码追溯服务运行正常");
            
        } catch (Exception e) {
            logger.error("健康检查失败", e);
            return Result.error(503, "代码追溯服务不可用: " + e.getMessage());
        }
    }
} 