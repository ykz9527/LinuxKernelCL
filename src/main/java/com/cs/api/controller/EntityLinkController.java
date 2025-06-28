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
import com.cs.api.dto.CodeSearchRequestDTO;
import com.cs.api.dto.CodeSearchResultDTO;
import com.cs.api.dto.ConceptExplanationRequestDTO;
import com.cs.api.dto.ConceptExplanationResultDTO;
import com.cs.api.dto.TripleSearchRequestDTO;
import com.cs.api.dto.TripleSearchResultDTO;
import com.cs.api.service.EntityLinkService;

import java.util.ArrayList;
import java.util.List;

/**
 * Linux内核代码搜索控制器
 * 提供基于概念和上下文的代码搜索功能
 * 
 * @author YK
 * @since 1.0.0
 */
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/entity-link")
@Tag(name = "概念链接", description = "概念链接相关信息的接口")
public class EntityLinkController {

    private static final Logger logger = LoggerFactory.getLogger(EntityLinkController.class);
    
    @Autowired
    private EntityLinkService entityLinkService;
    
    /**
     * 搜索Linux内核代码
     */
    @PostMapping("/code/search")
    @Operation(
        summary = "搜索Linux内核代码", 
        description = "根据概念、上下文和版本搜索相关的Linux内核代码片段"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "搜索成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "404", description = "指定版本不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<CodeSearchResultDTO>> searchCode(
            @Valid @RequestBody CodeSearchRequestDTO searchRequest) {
        logger.info("接收代码搜索请求: {}", searchRequest);
        
        try {  
            // 执行代码搜索
            List<CodeSearchResultDTO> searchResults = entityLinkService.searchCode(
                searchRequest.getConcept(), 
                searchRequest.getContext(), 
                searchRequest.getVersion()
            );
            
            if (searchResults.isEmpty()) {
                logger.info("未找到匹配的代码: concept={}, context={}, version={}", 
                    searchRequest.getConcept(), searchRequest.getContext(), searchRequest.getVersion());
                return Result.success("未找到匹配的代码片段", new ArrayList<>());
            }
            
            logger.info("代码搜索成功，找到{}个结果", searchResults.size());
            return Result.success(searchResults);
            
        } catch (IllegalArgumentException e) {
            logger.error("请求参数错误: {}", e.getMessage());
            return Result.error(400, "请求体格式错误或缺少必须的参数: " + e.getMessage());
        } catch (Exception e) {
            logger.error("代码搜索服务异常", e);
            return Result.error(500, "服务器内部发生错误，代码搜索服务失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据概念搜索对应的概念文本解释
     */
    @PostMapping("/concept/explanation")
    @Operation(
        summary = "搜索概念的文本解释",
        description = "根据概念和上下文搜索对应的文本解释，来源为可靠的在线知识库"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "搜索成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<ConceptExplanationResultDTO> getConceptExplanation(
            @Valid @RequestBody ConceptExplanationRequestDTO request) {
        logger.info("接收概念解释请求: {}", request);
        
        try {
            ConceptExplanationResultDTO result = entityLinkService.getConceptExplanation(
                request.getConcept(),
                request.getContext()
            );
            
            logger.info("概念解释搜索成功: concept={}", request.getConcept());
            return Result.success(result);
            
        } catch (IllegalArgumentException e) {
            logger.error("请求参数错误: {}", e.getMessage());
            return Result.error(400, "请求体格式错误或缺少必须的参数: " + e.getMessage());
        } catch (Exception e) {
            logger.error("概念解释服务异常", e);
            return Result.error(500, "服务器内部发生错误，概念解释服务失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据概念搜索三元组
     */
    @PostMapping("/triples/search")
    @Operation(
        summary = "根据概念搜索三元组", 
        description = "在Linux内核知识库中搜索包含指定概念的数据，并使用AI分析提取相关三元组"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "搜索成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<List<TripleSearchResultDTO>> searchTriples(
            @Valid @RequestBody TripleSearchRequestDTO request) {
        logger.info("接收三元组搜索请求: {}", request);
        
        try {
            List<TripleSearchResultDTO> results = entityLinkService.searchTriples(
                request.getConcept(), 
                request.getContext()
            );
            
            if (results.isEmpty()) {
                logger.info("未找到匹配的三元组: concept={}, context={}", 
                    request.getConcept(), request.getContext());
                return Result.success("未找到包含该概念的数据", new ArrayList<>());
            }
            
            logger.info("三元组搜索成功，找到{}个结果", results.size());
            return Result.success(results);
            
        } catch (IllegalArgumentException e) {
            logger.error("请求参数错误: {}", e.getMessage());
            return Result.error(400, "请求体格式错误或缺少必须的参数: " + e.getMessage());
        } catch (Exception e) {
            logger.error("三元组搜索服务异常", e);
            return Result.error(500, "服务器内部发生错误，三元组搜索服务失败: " + e.getMessage());
        }
    }
} 