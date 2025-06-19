package com.cs.api.service.analyzer;

import com.cs.api.dto.BootlinSearchResultDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.converter.StringHttpMessageConverter;

import java.util.concurrent.CompletableFuture;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Bootlin代码搜索服务
 * 专门负责与Bootlin网站进行交互，查询Linux内核代码
 * 
 * @author YK
 * @since 1.0.0
 */
@Service
public class BootlinSearchService {

    private static final Logger logger = LoggerFactory.getLogger(BootlinSearchService.class);

    @Value("${bootlin.base.url:https://elixir.bootlin.com}")
    private String bootlinBaseUrl;

    @Value("${bootlin.timeout:10}")
    private int timeoutSeconds;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public BootlinSearchService() {
        this.restTemplate = new RestTemplate();
        
        // 配置UTF-8字符编码转换器
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        stringConverter.setWriteAcceptCharset(false); // 避免在Accept-Charset中加入charset
        this.restTemplate.getMessageConverters().set(1, stringConverter);
        
        this.objectMapper = new ObjectMapper();
        
        logger.debug("BootlinSearchService 初始化完成，配置UTF-8编码");
    }

    /**
     * 异步搜索Bootlin
     * 
     * @param entity 要搜索的实体/概念
     * @param version 内核版本
     * @return CompletableFuture包含搜索结果
     */
    public CompletableFuture<BootlinSearchResultDTO> searchAsync(String entity, String version) {
        return CompletableFuture.supplyAsync(() -> search(entity, version));
    }

    /**
     * 搜索Bootlin并返回结构化结果
     * 
     * @param entity 要搜索的实体/概念
     * @param version 内核版本
     * @return 搜索结果，如果失败返回null
     */
    public BootlinSearchResultDTO search(String entity, String version) {
        logger.info("开始搜索Bootlin: entity={}, version={}", entity, version);

        if (entity == null || entity.trim().isEmpty()) {
            logger.warn("搜索实体为空");
            return null;
        }

        try {
            String processedEntity = preprocessEntity(entity);
            
            String formattedVersion = formatVersion(version);
            
            // 构建搜索URL
            String searchUrl = buildSearchUrl(processedEntity, formattedVersion);
            
            logger.debug("搜索URL: {}", searchUrl);

            ResponseEntity<String> response = executeSearchRequest(searchUrl);
            
            if (response != null && response.getStatusCode().is2xxSuccessful()) {
                String jsonContent = response.getBody();
                logger.info("成功获取Bootlin响应内容，长度: {}", jsonContent != null ? jsonContent.length() : 0);
                
                // 创建基础DTO
                BootlinSearchResultDTO result = new BootlinSearchResultDTO(searchUrl, true, entity, version);
                
                // 解析JSON内容并填充详细信息
                parseBootlinJsonResponse(jsonContent, result);
                
                return result;
            } else {
                logger.warn("Bootlin 搜索失败，实体: {}, 状态码: {}", entity, 
                    response != null ? response.getStatusCode() : "null");
                return new BootlinSearchResultDTO(searchUrl, false, entity, version);
            }

        } catch (Exception e) {
            logger.error("Bootlin 搜索异常，实体: {}", entity, e);
            return null;
        }
    }

    /**
     * 预处理搜索实体
     * 去除末尾的括号，转换为下划线格式
     */
    private String preprocessEntity(String entity) {
        // 去除标识符末尾的括号
        String processed = entity;
        if (processed.endsWith("()")) {
            processed = processed.substring(0, processed.length() - 2);
        }

        // 转换为下划线格式
        processed = String.join("_", processed.split("\\s+"));
        
        return processed;
    }

    /**
     * 格式化版本号
     */
    private String formatVersion(String version) {
        if (version == null || version.trim().isEmpty()) {
            return "latest";
        } else {
            // 如果版本号不是以v开头，则添加v前缀
            return version.startsWith("v") ? version : "v" + version;
        }
    }

    /**
     * 构建搜索URL
     */
    private String buildSearchUrl(String entity, String version) {
        return String.format("%s/api/ident/linux/%s?version=%s", bootlinBaseUrl, entity, version);
    }

    /**
     * 执行搜索请求，返回完整的HTTP响应
     */
    private ResponseEntity<String> executeSearchRequest(String searchUrl) {
        try {
            // 设置HTTP请求头，请求JSON响应
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Chrome/91.0.4472.124 Safari/537.36");
            headers.set("Accept", "application/json, text/plain, */*");
            headers.set("Accept-Language", "en-US,en;q=0.9");
            headers.set("Accept-Charset", "UTF-8");
            // 暂时移除gzip压缩避免乱码问题
            // headers.set("Accept-Encoding", "gzip, deflate");
            headers.set("Cache-Control", "no-cache");
            
            HttpEntity<String> httpEntity = new HttpEntity<>(headers);

            // 发送HTTP GET请求
            ResponseEntity<String> response = restTemplate.exchange(
                searchUrl,
                HttpMethod.GET,
                httpEntity,
                String.class
            );

            logger.debug("Bootlin响应状态码: {}, 内容长度: {}", 
                response.getStatusCode(), 
                response.getBody() != null ? response.getBody().length() : 0);
        
            return response;

        } catch (HttpClientErrorException e) {
            logger.debug("Bootlin HTTP请求失败，状态码: {}, 响应体: {}", 
                e.getStatusCode().value(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            logger.warn("Bootlin HTTP请求异常", e);
            return null;
        }
    }
    /**
     * 解析Bootlin响应JSON内容并填充到DTO中
     */
    private void parseBootlinJsonResponse(String jsonContent, BootlinSearchResultDTO result) {
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            logger.warn("JSON内容为空");
            return;
        }

        try {
            // 解析JSON
            JsonNode rootNode = objectMapper.readTree(jsonContent);
            
            // 解析定义信息
            if (rootNode.has("definitions")) {
                JsonNode definitionsNode = rootNode.get("definitions");
                if (definitionsNode.isArray()) {
                    for (JsonNode defNode : definitionsNode) {
                        String path = defNode.has("path") ? defNode.get("path").asText() : "";
                        String lineStr = defNode.has("line") ? defNode.get("line").asText() : "";
                        String type = defNode.has("type") ? defNode.get("type").asText() : "unknown";
                        
                        List<String> lines = parseLineNumbers(lineStr);
                        for (String line : lines) {
                            result.getDefinitions().add(createSearchResultItem(path, line, type, ""));
                        }
                    }
                }
            }
            
            // 解析引用信息
            if (rootNode.has("references")) {
                JsonNode referencesNode = rootNode.get("references");
                if (referencesNode.isArray()) {
                    for (JsonNode refNode : referencesNode) {
                        String path = refNode.has("path") ? refNode.get("path").asText() : "";
                        String lineStr = refNode.has("line") ? refNode.get("line").asText() : "";
                        String type = refNode.has("type") ? refNode.get("type").asText() : "reference";
                        
                        List<String> lines = parseLineNumbers(lineStr);
                        for (String line : lines) {
                            result.getReferences().add(createSearchResultItem(path, line, type, ""));
                        }
                    }
                }
            }
            
            // 解析文档信息（通常是空数组，简单处理）
            if (rootNode.has("documentations")) {
                JsonNode documentationsNode = rootNode.get("documentations");
                if (documentationsNode.isArray()) {
                    for (JsonNode docNode : documentationsNode) {
                        String path = docNode.has("path") ? docNode.get("path").asText() : "";
                        String lineStr = docNode.has("line") ? docNode.get("line").asText() : "";
                        String type = "documentation";

                        String description = docNode.has("description") ? docNode.get("description").asText() : "";
                        
                        List<String> lines = parseLineNumbers(lineStr);
                        for (String line : lines) {
                            result.getDocumentations().add(createSearchResultItem(path, line, type, description));
                        }
                    }
                }
            }
            
            // 生成简单描述
            int defCount = result.getDefinitions().size();
            int refCount = result.getReferences().size();
            
            if (defCount == 0 && refCount == 0) {
                result.setDescription("未找到相关信息");
            } else {
                result.setDescription(String.format("找到 %d 个定义，%d 个引用", defCount, refCount));
            }
            
            logger.info("解析完成: 定义={}, 引用={}", defCount, refCount);

        } catch (Exception e) {
            logger.error("解析JSON异常", e);
            result.setDescription("解析失败");
        }
    }

    /**
     * 解析Bootlin API返回的行号字符串为数组
     * 例如："123,456,789" -> ["123", "456", "789"]
     */
    private List<String> parseLineNumbers(String lineStr) {
        if (lineStr == null || lineStr.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        // 按逗号分割行号字符串
        String[] parts = lineStr.split(",");
        List<String> lines = new ArrayList<>();
        
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }
        
        return lines;
    }

    /**
     * 创建SearchResultItem时正确处理行号数组
     */
    private BootlinSearchResultDTO.SearchResultItem createSearchResultItem(
            String path, String lineStr, String type, String description) {
        
        BootlinSearchResultDTO.SearchResultItem item = new BootlinSearchResultDTO.SearchResultItem();
        item.setPath(path);
        item.setLine(parseLineNumbers(lineStr)); // 解析为数组形式
        item.setType(type);
        item.setDescription(description);
        
        return item;
    }

} 