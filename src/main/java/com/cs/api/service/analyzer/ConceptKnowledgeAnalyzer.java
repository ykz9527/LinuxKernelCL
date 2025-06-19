package com.cs.api.service.analyzer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 概念知识分析器
 * 从Linux内核概念知识库中查询概念解释
 * 支持从JSON文件读取数据，并在需要时访问Wikipedia获取详细描述
 * 支持代理访问和优化的Wikipedia内容提取
 * 
 * @author YK
 * @since 1.0.0
 */
@Component
public class ConceptKnowledgeAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(ConceptKnowledgeAnalyzer.class);
    
    @Value("${knowledge.base.path:/home/fdse/ytest/LinuxKernelKG/output/processed_entities_for_CL.json}")
    private String knowledgeBasePath;
    
    // 代理配置
    @Value("${proxy.http.host:127.0.0.1}")
    private String proxyHost;
    
    @Value("${proxy.http.port:7890}")
    private int proxyPort;
    
    @Value("${proxy.enabled:true}")
    private boolean proxyEnabled;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpClient httpClient; // 改为非final，因为需要在@PostConstruct中初始化
    private final Map<String, ConceptInfo> conceptCache = new ConcurrentHashMap<>();

    public ConceptKnowledgeAnalyzer() {
        // 构造函数中不再初始化httpClient，等待@PostConstruct
    }
    
    /**
     * Spring完成依赖注入后初始化HttpClient
     * 此时@Value字段已经被正确注入
     */
    @PostConstruct
    public void init() {
        this.httpClient = createHttpClient();
        logger.info("ConceptKnowledgeAnalyzer初始化完成 - 代理配置: enabled={}, host={}, port={}", 
                   proxyEnabled, proxyHost, proxyPort);
    }
    
    /**
     * 创建配置了代理的HttpClient
     */
    private HttpClient createHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL);
        
        // 如果启用代理且配置了代理信息
        if (proxyEnabled && proxyHost != null && !proxyHost.trim().isEmpty() && proxyPort > 0) {
            logger.info("启用HTTP代理: {}:{}", proxyHost, proxyPort);
            builder.proxy(ProxySelector.of(new InetSocketAddress(proxyHost, proxyPort)));
        }else{
            logger.info("未启用HTTP代理，使用直连方式,proxyEnabled={}, proxyHost={}, proxyPort={}", proxyEnabled, proxyHost, proxyPort);
        }
        
        return builder.build();
    }
    
    /**
     * 查询概念解释
     * 
     * @param concept 要查询的概念
     * @param contextHint 上下文提示（用于提高匹配准确性）
     * @return 概念解释结果，包含上下文和描述
     */
    public ConceptInfo getConceptExplanation(String concept, String contextHint) {
        logger.debug("查询概念解释: concept={}, contextHint={}", concept, contextHint);
        
        // 1. 尝试从缓存获取
        // String cacheKey = concept + ":" + (contextHint != null ? contextHint : "");
        // ConceptInfo cachedInfo = conceptCache.get(cacheKey);
        // if (cachedInfo != null) {
        //     logger.debug("从缓存获取概念信息: {}", concept);
        //     return cachedInfo;
        // }
        
        // 2. 从JSON文件查询
        ConceptInfo conceptInfo = queryFromJsonFile(concept);
        if (conceptInfo == null) {
            logger.warn("未找到概念信息: {}", concept);
            return createNotFoundResult(concept);
        }
        
        // 3. 构建解释结果
        ConceptInfo result = buildExplanationResult(concept, conceptInfo);
        
        // // 4. 缓存结果
        // conceptCache.put(cacheKey, result);
        
        return result;
    }
    
    /**
     * 从JSON文件中查询概念信息
     */
    private ConceptInfo queryFromJsonFile(String concept) {
        try {
            File jsonFile = new File(knowledgeBasePath);
            if (!jsonFile.exists()) {
                logger.error("知识库文件不存在: {}", knowledgeBasePath);
                return null;
            }
            
            JsonNode rootNode = objectMapper.readTree(jsonFile);
            
            // 尝试精确匹配
            JsonNode conceptNode = rootNode.get(concept);
            if (conceptNode != null) {
                return parseConceptInfo(concept, conceptNode);
            }
            
            return null;
            
        } catch (IOException e) {
            logger.error("读取知识库文件失败: {}", knowledgeBasePath, e);
            return null;
        }
    }
    
    /**
     * 解析JSON节点为概念信息
     */
    private ConceptInfo parseConceptInfo(String concept, JsonNode conceptNode) {
        String context = getStringValue(conceptNode, "context");
        String description = getStringValue(conceptNode, "description");
        String url = getStringValue(conceptNode, "url");
        
        return new ConceptInfo(concept, context, description, url);
    }
    
    /**
     * 安全获取JSON字符串值
     */
    private String getStringValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        return (fieldNode != null && !fieldNode.isNull()) ? fieldNode.asText().trim() : "";
    }
    
    /**
     * 构建解释结果
     */
    private ConceptInfo buildExplanationResult(String concept, ConceptInfo conceptInfo) {
        String explanation = conceptInfo.getDescription();
        
        // 如果描述为空，尝试从URL获取
        if (explanation.isEmpty() && !conceptInfo.getUrl().isEmpty()) {
            logger.debug("描述为空，尝试从URL获取: {}", conceptInfo.getUrl());
            explanation = fetchDescriptionFromUrl(conceptInfo.getUrl());
        }
    
        // 最后的后备方案
        if (explanation.isEmpty()) {
            explanation = "暂未找到关于 '" + concept + "' 的详细解释。这是一个Linux内核相关的概念。";
        }
        
        return new ConceptInfo(
            concept,
            explanation,
            conceptInfo.getContext(),
            conceptInfo.getUrl()
        );
    }
    
    /**
     * 从URL获取描述信息（支持代理访问）
     * 优先使用Wikipedia API，失败时回退到HTML解析
     */
    private String fetchDescriptionFromUrl(String url) {
        try {
            logger.debug("正在从URL获取描述: {}", url);
            
            // 优先尝试使用Wikipedia API

            String apiContent = fetchFromWikipediaAPI(url);
            if (!apiContent.isEmpty()) {
                logger.info("✅ 使用Wikipedia API成功获取内容，长度: {}, 内容: {}", 
                            apiContent.length(), 
                            apiContent.length() > 150 ? apiContent.substring(0, 150) + "..." : apiContent);
                return apiContent;
            }
            
            
            // // 回退到原有的HTML解析方法
            // logger.debug("Wikipedia API失败，尝试HTML解析");
            // HttpRequest request = HttpRequest.newBuilder()
            //     .uri(URI.create(url))
            //     .timeout(Duration.ofSeconds(10))
            //     .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36")
            //     .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9")
            //     .header("Accept-Language", "en-US,en;q=0.8")
            //     .header("Accept-Charset", "UTF-8")
            //     .build();
            
            // HttpResponse<String> response = httpClient.send(request, 
            //     HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            
            // if (response.statusCode() == 200) {
            //     String content = extractWikipediaContentFallback(response.body());
            //     if (!content.isEmpty()) {
            //         logger.info("🔄 HTML解析成功，长度: {}, 内容: {}", content.length(), 
            //                   content.length() > 100 ? content.substring(0, 100) + "..." : content);
            //         return content;
            //     }
            // } else {
            //     logger.warn("HTTP请求失败，状态码: {}", response.statusCode());
            // }
            
        } catch (Exception e) {
            logger.warn("从URL获取描述失败: {}", url, e);
        }
        
        return "";
    }
    
    /**
     * 使用Wikipedia REST API获取页面摘要
     * API文档: https://en.wikipedia.org/api/rest_v1/page/summary/{title}
     */
    private String fetchFromWikipediaAPI(String wikipediaUrl) {
        try {
            // 从Wikipedia URL提取页面标题
            String title = extractWikipediaTitle(wikipediaUrl);
            if (title.isEmpty()) {
                logger.debug("无法从URL提取Wikipedia标题: {}", wikipediaUrl);
                return "";
            }
            
            // 构建API URL
            String apiUrl = String.format("https://en.wikipedia.org/api/rest_v1/page/summary/%s", 
                                        java.net.URLEncoder.encode(title, "UTF-8"));
            
            logger.debug("调用Wikipedia API: {}", apiUrl);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "LinuxKernelAnalyzer/1.0 (https://example.com/contact)")
                .header("Accept", "application/json")
                .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            
            if (response.statusCode() == 200) {
                // 解析JSON响应
                JsonNode jsonNode = objectMapper.readTree(response.body());
                String extract = getStringValue(jsonNode, "extract");
                
                if (!extract.isEmpty() && extract.length() > 20) {
                    logger.debug("✅ Wikipedia API返回摘要，长度: {}", extract.length());
                    return extract;
                } else {
                    logger.debug("Wikipedia API返回的摘要为空或太短");
                }
            } else {
                logger.debug("Wikipedia API请求失败，状态码: {}", response.statusCode());
            }
            
        } catch (Exception e) {
            logger.debug("Wikipedia API调用失败", e);
        }
        
        return "";
    }
    
    /**
     * 从Wikipedia URL中提取页面标题
     */
    private String extractWikipediaTitle(String url) {
        try {
            // 匹配形如 https://en.wikipedia.org/wiki/Page_Title 的URL
            Pattern titlePattern = Pattern.compile("wikipedia\\.org/wiki/([^#?&]+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = titlePattern.matcher(url);
            
            if (matcher.find()) {
                String title = matcher.group(1);
                // URL解码
                title = java.net.URLDecoder.decode(title, "UTF-8");
                return title;
            }
        } catch (Exception e) {
            logger.debug("提取Wikipedia标题失败", e);
        }
        
        return "";
    }
    
    /**
     * 创建未找到结果
     */
    private ConceptInfo createNotFoundResult(String concept) {
        return new ConceptInfo(
            concept,
            "未找到关于 '" + concept + "' 的相关信息。这可能是一个不常见的Linux内核概念，或者需要更新知识库。",
            "",
            ""
        );
    }
    
    /**
     * 清空缓存
     */
    public void clearCache() {
        conceptCache.clear();
        logger.info("概念缓存已清空");
    }
    
    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("cacheSize", conceptCache.size());
        stats.put("knowledgeBasePath", knowledgeBasePath);
        stats.put("proxyEnabled", proxyEnabled);
        if (proxyEnabled) {
            stats.put("proxyHost", proxyHost);
            stats.put("proxyPort", proxyPort);
        }
        return stats;
    }
    
    /**
     * 概念信息内部类
     */
    public static class ConceptInfo {
        private final String concept;   
        private final String context;
        private final String description;
        private final String url;
        
        public ConceptInfo(String concept, String context, String description, String url) {
            this.concept = concept != null ? concept : "";
            this.context = context != null ? context : "";
            this.description = description != null ? description : "";
            this.url = url != null ? url : "";
        }
        
        public String getConcept() { return concept; }
        public String getContext() { return context; }
        public String getDescription() { return description; }
        public String getUrl() { return url; }
    }

} 