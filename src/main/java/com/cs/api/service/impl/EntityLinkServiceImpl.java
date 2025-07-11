package com.cs.api.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cs.api.common.ai.AIService;
import com.cs.api.common.ai.Prompt;
import com.cs.api.dto.BootlinSearchResultDTO;
import com.cs.api.dto.CodeSearchResultDTO;
import com.cs.api.dto.CodeClusterResultDTO;
import com.cs.api.dto.ConceptExplanationResultDTO;
import com.cs.api.dto.ConceptRelationshipResultDTO;
import com.cs.api.dto.ConceptValidationResultDTO;
import com.cs.api.dto.TripleSearchResultDTO;
import com.cs.api.dto.KernelFeatureDTO;
import com.cs.api.dto.EntityExtractionDTO;
import com.cs.api.mapper.EntityExtractionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cs.api.service.EntityLinkService;
import com.cs.api.service.analyzer.BootlinSearchService;
import com.cs.api.service.analyzer.ConceptKnowledgeAnalyzer;
import com.cs.api.service.analyzer.KernelCodeAnalyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.Comparator;

/**
 * Linux内核代码搜索服务实现类
 * 现在使用实时分析本地Linux内核源码
 * 并集成Bootlin在线搜索作为辅助功能
 * 
 * @author YK
 * @since 1.0.0
 */
@Service
public class EntityLinkServiceImpl implements EntityLinkService {
    
    private static final Logger logger = LoggerFactory.getLogger(EntityLinkServiceImpl.class);
    
    @Autowired
    private BootlinSearchService bootlinSearchService;
    
    @Autowired
    private ConceptKnowledgeAnalyzer conceptKnowledgeAnalyzer;
    
    @Autowired
    private EntityExtractionMapper entityExtractionMapper;

    @Value("${kernel.source.path:/home/fdse/ytest/codeMap/linux/repo}")
    private String kernelSourcePath;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // JSONL文件路径
    private static final String JSONL_FILE_PATH = "/home/fdse/ytest/LinuxKernelKG/output/entity_extraction/extraction_results_20250509_0958.jsonl";

    @Override
    public List<CodeSearchResultDTO> searchCode(String concept, String context, String version) {
        logger.debug("执行代码搜索: concept={}, context={}, version={}", concept, context, version);
        
        List<CodeSearchResultDTO> results = new ArrayList<>();
        try {
            List<CodeSearchResultDTO> bootlinResults = searchWithBootlin(concept, context, version);
            results.addAll(bootlinResults);
            logger.info("Bootlin辅助搜索完成，新增{}个结果", bootlinResults.size()); 
        } catch (Exception e) {
            logger.warn("Bootlin辅助搜索失败", e);
        }
        
        return results;
    }
    
    /**
     * 使用Bootlin进行辅助搜索
     * 基于Python实现的逻辑，提取关键实体进行搜索
     */
    private List<CodeSearchResultDTO> searchWithBootlin(String concept, String context, String version) {
        List<CodeSearchResultDTO> bootlinResults = new ArrayList<>();
        
        if(version == null || version.trim().isEmpty()){
            version = "v6.14"; // 默认版本先选择为6.14后续再优化。
        }
        // 提取搜索实体（LLM） - 基于概念和上下文
        List<String> searchEntities = extractSearchEntities(concept, context);
        
        // 静态分析：规范化搜索实体格式
        searchEntities = normalizeSearchEntities(searchEntities);
        
        logger.debug("准备搜索的实体: {}", searchEntities);
        
        // 同步搜索多个实体
        for (String entity : searchEntities) {
            try {
                BootlinSearchResultDTO bootlinResult = bootlinSearchService.search(entity, version);
                if (bootlinResult != null && bootlinResult.isSuccess()) {
                    // 将Bootlin结果转换为CodeSearchResultDTO列表
                    List<CodeSearchResultDTO> codeResults = convertBootlinResult(bootlinResult, concept, context);
                    bootlinResults.addAll(codeResults);
                    logger.debug("Bootlin找到结果: entity={}, url={}, 提取到{}个代码片段", 
                        bootlinResult.getEntity(), bootlinResult.getUrl(), codeResults.size());
                }
            } catch (Exception e) {
                logger.warn("Bootlin搜索实体 '{}' 失败", entity, e);
            }
        }
        
        return bootlinResults;
    }
    
    /**
     * 从概念和上下文中提取搜索实体
     * 使用大模型生成概念变体，提高搜索准确性
     */
    private List<String> extractSearchEntities(String concept, String context) {
        List<String> entities = new ArrayList<>();
        entities.add(concept);
        
        try {
            
            // 1. 使用大模型生成概念变体
            logger.debug("使用AI模型生成概念变体: concept={}, context={}", concept, context);
            String prompt = Prompt.generateConceptVariants(concept, context);
            String response = AIService.deepseek(prompt);
            
            if (response != null && !response.trim().isEmpty()) {
                // 解析AI返回的逗号分隔的搜索词
                String[] aiEntities = response.trim().split(",");
                for (String entity : aiEntities) {
                    String cleanEntity = entity.trim();
                    if (cleanEntity.length() > 1 && !cleanEntity.contains(" ")) {
                        entities.add(cleanEntity);
                    }
                }
                logger.info("AI模型生成了{}个搜索实体: {}", entities.size(), entities);
            } else {
                logger.warn("AI模型返回空结果，使用后备策略");
            }
        } catch (Exception e) {
            logger.warn("AI模型调用失败，使用后备策略: {}", e.getMessage());
        }
        
        // 2. 使用启发式规则生成概念变体
        logger.debug("使用启发式规则生成概念变体");
        List<String> heuristicVariants = generateHeuristicVariants(concept);
        entities.addAll(heuristicVariants);
        logger.debug("启发式规则生成了{}个变体: {}", heuristicVariants.size(), heuristicVariants);
        
        // 3. 去重并限制数量（避免过多搜索实体影响性能）
        return entities.stream()
                .distinct()
                .limit(5)  // 限制最多5个搜索实体
                .toList();
    }
    
    /**
     * 使用启发式规则生成概念变体
     * 通过多种命名转换规则生成搜索变体，提高搜索命中率
     * @param concept 原始概念
     * @return 生成的概念变体列表
     */
    private List<String> generateHeuristicVariants(String concept) {
        if (concept == null || concept.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        Set<String> variants = new HashSet<>();
        String cleanConcept = concept.trim();
        
        // 1. 基本大小写变体
        variants.add(cleanConcept.toLowerCase());
        variants.add(cleanConcept.toUpperCase());
        variants.add(capitalizeFirstLetter(cleanConcept.toLowerCase()));
        
        // 2. 空格和下划线转换
        if (cleanConcept.contains(" ")) {
            // 空格转下划线
            variants.add(cleanConcept.replaceAll("\\s+", "_"));
            variants.add(cleanConcept.toLowerCase().replaceAll("\\s+", "_"));
            variants.add(cleanConcept.toUpperCase().replaceAll("\\s+", "_"));
            
            // 空格转连字符
            variants.add(cleanConcept.replaceAll("\\s+", "-"));
            variants.add(cleanConcept.toLowerCase().replaceAll("\\s+", "-"));
            
            // 去除空格
            variants.add(cleanConcept.replaceAll("\\s+", ""));
            variants.add(cleanConcept.toLowerCase().replaceAll("\\s+", ""));
        }
        
        if (cleanConcept.contains("_")) {
            // 下划线转空格
            variants.add(cleanConcept.replaceAll("_", " "));
            variants.add(cleanConcept.toLowerCase().replaceAll("_", " "));
            
            // 下划线转连字符
            variants.add(cleanConcept.replaceAll("_", "-"));
            variants.add(cleanConcept.toLowerCase().replaceAll("_", "-"));
            
            // 去除下划线
            variants.add(cleanConcept.replaceAll("_", ""));
            variants.add(cleanConcept.toLowerCase().replaceAll("_", ""));
        }
        
        if (cleanConcept.contains("-")) {
            // 连字符转下划线
            variants.add(cleanConcept.replaceAll("-", "_"));
            variants.add(cleanConcept.toLowerCase().replaceAll("-", "_"));
            
            // 连字符转空格
            variants.add(cleanConcept.replaceAll("-", " "));
            variants.add(cleanConcept.toLowerCase().replaceAll("-", " "));
            
            // 去除连字符
            variants.add(cleanConcept.replaceAll("-", ""));
            variants.add(cleanConcept.toLowerCase().replaceAll("-", ""));
        }
        
        // 3. 驼峰命名转换
        String camelToCamel = convertToCamelCase(cleanConcept);
        if (!camelToCamel.equals(cleanConcept)) {
            variants.add(camelToCamel);
        }
        
        String camelToSnake = convertCamelToSnakeCase(cleanConcept);
        if (!camelToSnake.equals(cleanConcept)) {
            variants.add(camelToSnake);
            variants.add(camelToSnake.toUpperCase());
        }
        
        String snakeToCamel = convertSnakeToCamelCase(cleanConcept);
        if (!snakeToCamel.equals(cleanConcept)) {
            variants.add(snakeToCamel);
            variants.add(capitalizeFirstLetter(snakeToCamel));
        }
        
        // 4. Linux内核常见前缀/后缀变体
        variants.addAll(generateKernelPrefixSuffixVariants(cleanConcept));
        
        // 5. 过滤和清理变体
        List<String> filteredVariants = variants.stream()
                .filter(v -> v != null && !v.trim().isEmpty())
                .filter(v -> v.length() > 1 && v.length() < 50) // 长度合理
                .filter(v -> !v.equals(cleanConcept)) // 排除原始概念
                .distinct()
                .limit(10) // 限制变体数量避免过多
                .collect(Collectors.toList());
        
        logger.debug("为概念'{}'生成{}个启发式变体", cleanConcept, filteredVariants.size());
        return filteredVariants;
    }
    
    /**
     * 首字母大写
     */
    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    /**
     * 转换为驼峰命名
     */
    private String convertToCamelCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        String[] words = str.split("[\\s_-]+");
        if (words.length <= 1) {
            return str;
        }
        
        StringBuilder camelCase = new StringBuilder();
        camelCase.append(words[0].toLowerCase());
        
        for (int i = 1; i < words.length; i++) {
            if (!words[i].isEmpty()) {
                camelCase.append(capitalizeFirstLetter(words[i].toLowerCase()));
            }
        }
        
        return camelCase.toString();
    }
    
    /**
     * 驼峰命名转下划线命名
     */
    private String convertCamelToSnakeCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        return str.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
    
    /**
     * 下划线命名转驼峰命名
     */
    private String convertSnakeToCamelCase(String str) {
        if (str == null || str.isEmpty() || !str.contains("_")) {
            return str;
        }
        
        String[] parts = str.split("_");
        if (parts.length <= 1) {
            return str;
        }
        
        StringBuilder camelCase = new StringBuilder();
        camelCase.append(parts[0].toLowerCase());
        
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                camelCase.append(capitalizeFirstLetter(parts[i].toLowerCase()));
            }
        }
        
        return camelCase.toString();
    }
    
    /**
     * 生成Linux内核常见前缀/后缀变体
     */
    private List<String> generateKernelPrefixSuffixVariants(String concept) {
        List<String> variants = new ArrayList<>();
        String lowerConcept = concept.toLowerCase();
        
        // 常见的Linux内核前缀
        String[] commonPrefixes = {"linux_", "kernel_", "sys_", "__", "struct ", "enum "};
        
        // 常见的Linux内核后缀  
        String[] commonSuffixes = {"_t", "_struct", "_s", "_info", "_data", "_ops", "_func"};
        
        // 尝试添加前缀
        for (String prefix : commonPrefixes) {
            if (!lowerConcept.startsWith(prefix.toLowerCase())) {
                variants.add(prefix + lowerConcept);
                variants.add(prefix + concept);
            }
        }
        
        // 尝试添加后缀
        for (String suffix : commonSuffixes) {
            if (!lowerConcept.endsWith(suffix.toLowerCase())) {
                variants.add(lowerConcept + suffix);
                variants.add(concept + suffix);
            }
        }
        
        // 尝试移除前缀（如果概念以这些前缀开头）
        for (String prefix : commonPrefixes) {
            String prefixLower = prefix.toLowerCase();
            if (lowerConcept.startsWith(prefixLower) && lowerConcept.length() > prefixLower.length()) {
                String withoutPrefix = lowerConcept.substring(prefixLower.length());
                if (withoutPrefix.length() > 1) {
                    variants.add(withoutPrefix);
                    variants.add(capitalizeFirstLetter(withoutPrefix));
                }
            }
        }
        
        // 尝试移除后缀（如果概念以这些后缀结尾）
        for (String suffix : commonSuffixes) {
            String suffixLower = suffix.toLowerCase();
            if (lowerConcept.endsWith(suffixLower) && lowerConcept.length() > suffixLower.length()) {
                String withoutSuffix = lowerConcept.substring(0, lowerConcept.length() - suffixLower.length());
                if (withoutSuffix.length() > 1) {
                    variants.add(withoutSuffix);
                    variants.add(capitalizeFirstLetter(withoutSuffix));
                }
            }
        }
        
        return variants.stream()
                .filter(v -> v != null && !v.trim().isEmpty())
                .filter(v -> !v.equals(concept) && !v.equals(lowerConcept))
                .distinct()
                .collect(Collectors.toList());
    }
    
    /**
     * 规范化搜索实体格式
     * 统一转换为小写，将空格替换为下划线
     * @param entities 原始搜索实体列表
     * @return 规范化后的搜索实体列表
     */
    private List<String> normalizeSearchEntities(List<String> entities) {
        if (entities == null || entities.isEmpty()) {
            return entities;
        }
        
        List<String> normalizedEntities = new ArrayList<>();
        
        for (String entity : entities) {
            if (entity != null && !entity.trim().isEmpty()) {
                // 1. 去除首尾空白
                String normalized = entity.trim();
                
                // 2. 转换为小写
                normalized = normalized.toLowerCase();
                
                // 3. 将空格替换为下划线
                normalized = normalized.replaceAll("\\s+", "_");
                
                // 4. 移除多余的下划线（连续的下划线合并为单个）
                normalized = normalized.replaceAll("_+", "_");
                
                // 5. 移除开头和结尾的下划线
                normalized = normalized.replaceAll("^_+|_+$", "");
                
                // 6. 确保规范化后的实体不为空且长度合理
                if (!normalized.isEmpty() && normalized.length() > 1) {
                    normalizedEntities.add(normalized);
                }
            }
        }
        
        logger.debug("实体规范化完成: 原始{}个 -> 规范化{}个", entities.size(), normalizedEntities.size());
        return normalizedEntities;
    }
    
    /**
     * 处理SearchResultItem中的行号数组
     */
    private List<CodeSearchResultDTO> processSearchResultItem(
            BootlinSearchResultDTO.SearchResultItem item, 
            String concept, String version,String type) {
        
        List<CodeSearchResultDTO> results = new ArrayList<>();
        
        if(!type.equals("struct") && !type.equals("function") && !type.equals("reference")){
            return results;
        }
        // 获取行号数组
        List<String> lines = item.getLine();
        
        if (lines != null && !lines.isEmpty()) {
            // 为每个行号创建一个代码搜索结果
            for (String lineStr : lines) {
                int lineNumber = parseLineNumber(lineStr);
                if (lineNumber > 0) {
                    CodeSearchResultDTO result = KernelCodeAnalyzer.findCodeElementByLineNumber(item.getPath(), concept, lineNumber, kernelSourcePath,version);
                    if (result != null) {
                        result.setType(type);
                        results.add(result);
                    }
                }
            }
        } else {
            // 对于没有行号的情况（如文档），创建一个通用结果
            CodeSearchResultDTO result = KernelCodeAnalyzer.findCodeElementByIdentifier(item.getPath(), concept, type, kernelSourcePath,version);
            if (result != null) {
                result.setType(type);
                results.add(result);
            }
        }
        
        return results;
    }
    
    /**
     * 简化的行号解析方法
     */
    private int parseLineNumber(String lineStr) {
        if (lineStr == null || lineStr.trim().isEmpty()) {
            return 0;
        }
        
        try {
            return Integer.parseInt(lineStr.trim());
        } catch (NumberFormatException e) {
            logger.debug("无法解析行号: {}", lineStr);
            return 0;
        }
    }
    
    /**
     * 更新convertBootlinResult方法使用新的处理逻辑
     */
    private List<CodeSearchResultDTO> convertBootlinResult(BootlinSearchResultDTO bootlinResult, String concept, String context) {
        List<CodeSearchResultDTO> results = new ArrayList<>();
        
        // 1. 处理定义信息
        if (bootlinResult.getDefinitions() != null && !bootlinResult.getDefinitions().isEmpty()) {
            for (BootlinSearchResultDTO.SearchResultItem def : bootlinResult.getDefinitions()) {
                results.addAll(processSearchResultItem(def, concept, bootlinResult.getVersion(), def.getType()));
            }
        }
        
        // 2. 处理引用信息
        if (bootlinResult.getReferences() != null && !bootlinResult.getReferences().isEmpty()) {
            List<BootlinSearchResultDTO.SearchResultItem> limitedRefs = bootlinResult.getReferences().stream()
                .limit(3)
                .collect(Collectors.toList());
                
            for (BootlinSearchResultDTO.SearchResultItem ref : limitedRefs) {
                results.addAll(processSearchResultItem(ref, concept, bootlinResult.getVersion(), "reference"));
            }
        }
        
        // 3. 处理文档信息
        if (bootlinResult.getDocumentations() != null && !bootlinResult.getDocumentations().isEmpty()) {
            for (BootlinSearchResultDTO.SearchResultItem doc : bootlinResult.getDocumentations()) {
                CodeSearchResultDTO result = createDocumentationResult(doc, concept, bootlinResult.getVersion());
                if (result != null) {
                    results.add(result);
                }
            }
        }
        
        return results;
    }
    
    /**
     * 创建文档信息结果 - 修正参数类型
     */
    private CodeSearchResultDTO createDocumentationResult(BootlinSearchResultDTO.SearchResultItem doc, 
                                                          String concept, String version) {
        
        List<String> lines = doc.getLine();
        int lineNumber = 0;
        if (lines != null && !lines.isEmpty()) {
            lineNumber = parseLineNumber(lines.get(0));
        }

        if (lineNumber > 0) {
            // 如果有行号，尝试从源代码中提取注释
            CodeSearchResultDTO result = KernelCodeAnalyzer.findCommentBlockByLineNumber(doc.getPath(), concept, lineNumber, kernelSourcePath, version);
            if (result != null) {
                return result;
            }
        }

        // 如果没有行号或提取失败，回退到旧的通用格式
        String explanation = String.format(
            "找到与'%s'相关的文档信息。" +
            "描述: %s",
            concept, 
            doc.getDescription() != null ? doc.getDescription() : "无描述"
        );
        
        String documentContent = String.format(
            "// 文档信息\n" +
            "// 路径: %s\n" +
            "// 描述: %s\n" +
            "/*\n" +
            " * 这是相关的文档信息\n" +
            " * 可以帮助理解功能的用途和使用方法\n" +
            " */",
            doc.getPath(),
            doc.getDescription() != null ? doc.getDescription() : "无描述"
        );
        
        return new CodeSearchResultDTO(
            "doc/" + doc.getPath(),
            concept,
            documentContent,
            1,
            1,
            10,
            explanation,
            version,
            "documentation"
        );
    }
    
    @Override
    public ConceptExplanationResultDTO getConceptExplanation(String concept, String context) {
        logger.info("获取概念解释: concept={}, context={}", concept, context);

        try {
            // 使用概念知识分析器从知识库查询解释
            ConceptKnowledgeAnalyzer.ConceptInfo result = 
                conceptKnowledgeAnalyzer.getConceptExplanation(concept, context);
            
            if (result != null) {
                // 构建完整的解释信息
                StringBuilder fullExplanation = new StringBuilder();
                // fullExplanation.append(result.getDescription());
                
                // 如果有上下文信息，添加到解释中
                if (!result.getContext().isEmpty() && 
                    !result.getDescription().contains(result.getContext())) {
                    fullExplanation.append(result.getContext());
                }
                
                // // 如果有来源URL，添加引用信息
                // if (!result.getUrl().isEmpty()) {
                //     fullExplanation.append("\n\n参考来源: ")
                //                   .append(result.getUrl());
                // }
                
                logger.debug("成功获取概念解释: concept={}, explanationLength={}", 
                    concept, fullExplanation.length());
                
                return new ConceptExplanationResultDTO(concept, fullExplanation.toString(), result.getUrl());
            }
            
        } catch (Exception e) {
            logger.error("获取概念解释失败: concept={}", concept, e);
        }
        
        // 后备方案：返回基本解释
        String fallbackExplanation = String.format(
            "关于 '%s' 的概念解释暂时无法获取。这可能是一个Linux内核相关的技术概念。" +
            "建议查阅相关文档或联系系统管理员获取更多信息。%s",
            concept,
            (context != null && !context.trim().isEmpty()) ? 
                "\n\n提供的上下文: " + context : ""
        );
        
        return new ConceptExplanationResultDTO(concept, fallbackExplanation, "");
    }

    @Override
    public List<TripleSearchResultDTO> searchTriples(String concept, String context) {
        logger.info("开始搜索三元组: concept={}, context={}", concept, context);
        
        try {
            // 1. 从数据库搜索相关实体，获取feature描述
            List<EntityExtractionDTO> relatedEntities = searchRelatedEntities(concept, context);
            logger.debug("找到{}个相关实体", relatedEntities.size());
            
            if (relatedEntities.isEmpty()) {
                logger.warn("未找到与概念'{}'相关的实体", concept);
                return new ArrayList<>();
            }
            
            // 2. 提取feature描述
            List<String> featureDescriptions = extractFeatureDescriptions(relatedEntities);
            logger.debug("提取到{}个feature描述", featureDescriptions.size());
            
            // 3. 分批处理大模型调用，提取三元组
            List<TripleSearchResultDTO> results = extractTriplesInBatches(concept, context, featureDescriptions);
            logger.info("三元组搜索完成，共提取{}个结果", results.size());
            
            return results;
            
        } catch (Exception e) {
            logger.error("搜索三元组失败: concept={}", concept, e);
            throw new RuntimeException("三元组搜索服务失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 搜索与概念相关的实体
     * @param concept 搜索概念
     * @param context 上下文信息
     * @return 相关实体列表
     */
    private List<EntityExtractionDTO> searchRelatedEntities(String concept, String context) {
        logger.debug("搜索相关实体: concept={}, context={}", concept, context);
        
        // 1. 精确匹配实体名称
        List<EntityExtractionDTO> exactMatches = entityExtractionMapper.findByNameEn(concept);
        
        // 2. 如果精确匹配结果较少，尝试扩展搜索
        if (exactMatches.size() < 5) {
            // 生成概念变体进行搜索 默认先不需要这一步
            // List<String> searchVariants = extractSearchEntities(concept, context);
            // for (String variant : searchVariants) {
            //     if (!variant.equals(concept)) {
            //         List<EntityExtractionDTO> variantMatches = entityExtractionMapper.findByNameEnLike(variant);
            //         exactMatches.addAll(variantMatches);
            //     }
            // }
        }
        
        // 3. 去重并限制数量
        List<EntityExtractionDTO> uniqueEntities = exactMatches.stream()
                .distinct()
                .limit(20) // 限制最多20个实体，避免数据量过大
                .collect(Collectors.toList());
        
        logger.debug("搜索到{}个唯一相关实体", uniqueEntities.size());
        return uniqueEntities;
    }
    
    /**
     * 从实体列表中提取feature描述
     * @param entities 实体列表
     * @return feature描述列表
     */
    private List<String> extractFeatureDescriptions(List<EntityExtractionDTO> entities) {
        return entities.stream()
                .map(EntityExtractionDTO::getDefinitionEn)
                .filter(desc -> desc != null && !desc.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
    
    /**
     * 分批处理大模型调用，提取三元组
     * @param concept 搜索概念
     * @param context 上下文信息
     * @param featureDescriptions feature描述列表
     * @param originalEntities 原始实体列表（用于构建结果）
     * @return 三元组搜索结果列表
     */
    private List<TripleSearchResultDTO> extractTriplesInBatches(String concept, String context, 
            List<String> featureDescriptions) {
        
        List<TripleSearchResultDTO> allResults = new ArrayList<>();
        final int BATCH_SIZE = 5; // 每批处理5个feature描述
        
        // 按批次处理
        for (int i = 0; i < featureDescriptions.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, featureDescriptions.size());
            List<String> batch = featureDescriptions.subList(i, endIndex);
            
            logger.debug("处理第{}批，包含{}个feature描述", (i / BATCH_SIZE + 1), batch.size());
            
            try {
                // 调用大模型提取三元组
                List<TripleSearchResultDTO> batchResults = extractTriplesFromBatch(concept, context, batch, i);
                allResults.addAll(batchResults);
                
                // 避免请求过于频繁，稍作延迟
                if (endIndex < featureDescriptions.size()) {
                    Thread.sleep(1000); // 1秒延迟
                }
                
            } catch (Exception e) {
                logger.warn("处理第{}批三元组提取失败: {}", (i / BATCH_SIZE + 1), e.getMessage());
                // 继续处理下一批，不中断整个流程
            }
        }
        
        return allResults;
    }
    
    /**
     * 从单个批次中提取三元组
     * @param concept 搜索概念
     * @param context 上下文信息
     * @param batch 当前批次的feature描述
     * @param originalEntities 原始实体列表
     * @param batchStartIndex 批次起始索引
     * @return 当前批次的三元组结果
     */
    private List<TripleSearchResultDTO> extractTriplesFromBatch(String concept, String context, 
            List<String> batch, int batchStartIndex) {
        
        try {
            // 把feature描述的几个句子组合成一个
            String batchContext = String.join(" ", batch);

            String prompt = Prompt.extractTriplesFromFeatures(concept, batchContext);
            
            // 调用大模型
            String aiResponse = AIService.deepseek(prompt);
            
            if (aiResponse == null || aiResponse.trim().isEmpty()) {
                logger.warn("AI模型返回空结果，批次: {}", batchStartIndex / 5 + 1);
                return new ArrayList<>();
            }
            
            // 解析三元组结果
            return parseTriplesResponse(concept, context, aiResponse, batch, batchStartIndex);
            
        } catch (Exception e) {
            logger.error("大模型调用失败，批次: {}, 错误: {}", batchStartIndex / 5 + 1, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 解析AI返回的三元组文本
     * @param concept 搜索概念
     * @param context 上下文信息
     * @param aiResponse AI响应文本
     * @param batch 当前批次的feature描述
     * @param originalEntities 原始实体列表
     * @param batchStartIndex 批次起始索引
     * @return 解析后的三元组结果列表
     */
    private List<TripleSearchResultDTO> parseTriplesResponse(String concept, String context, String aiResponse, 
            List<String> batch, int batchStartIndex) {
        
        List<TripleSearchResultDTO> results = new ArrayList<>();
        
        try {
            // 按行分割响应文本
            String[] lines = aiResponse.split("\n");
            
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || !line.startsWith("(") || !line.endsWith(")")) {
                    continue;
                }

                // 解析三元组格式: (head, relation, tail)
                String tripleContent = line.substring(1, line.length() - 1); 
                String[] parts = tripleContent.split(",", 3);
                
                if (parts.length == 3) {
                    // 目前先保证head和tail必须包含concept，后续再优化
                    String head = parts[0].trim();
                    String tail = parts[2].trim();
                    
                    if (head.equals(concept) || tail.equals(concept)) {
                        // 构建三元组结果
                        TripleSearchResultDTO result = buildTripleResult(concept, line);
                        if (result != null) {
                            results.add(result);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            logger.warn("解析三元组响应失败: {}", e.getMessage());
        }
        
        return results;
    }
    
    /**
     * 构建三元组搜索结果
     * @param concept 搜索概念
     * @param context 上下文信息
     * @param subject 主语
     * @param relation 关系
     * @param object 宾语
     * @param batch 相关的feature描述批次
     * @param originalEntities 原始实体列表
     * @param batchStartIndex 批次起始索引
     * @return 三元组搜索结果
     */
    private TripleSearchResultDTO buildTripleResult(String concept, String triple) {
        
        try {
            TripleSearchResultDTO result = new TripleSearchResultDTO();
            result.setConcept(concept);
            result.addTriple(triple);
            // 设置其他信息
            result.setVersion("v6.14"); // 默认版本
            return result;
            
        } catch (Exception e) {
            logger.warn("构建三元组结果失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 从JSONL文件导入实体提取数据到数据库
     * 将filtered_entities中的实体数据存储到entities_extraction表
     */
    public List<TripleSearchResultDTO> importEntityExtractionData() {
        logger.info("开始从JSONL文件导入实体提取数据: {}", JSONL_FILE_PATH);
        
        int totalProcessed = 0;
        int totalImported = 0;
        
        try {
            // 1. 读取JSONL文件
            List<String> lines = Files.readAllLines(Paths.get(JSONL_FILE_PATH));
            logger.debug("读取JSONL文件，共{}行", lines.size());
            
            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                try {
                    // 2. 解析JSON数据
                    KernelFeatureDTO featureData = objectMapper.readValue(line, KernelFeatureDTO.class);
                    
                    // 3. 处理filtered_entities
                    if (featureData.getExtractionResult() != null && 
                        featureData.getExtractionResult().getFilteredEntities() != null) {
                        
                        List<String> entities = featureData.getExtractionResult().getFilteredEntities();
                        Integer featureId = featureData.getFeatureId();
                        String featureDescription = featureData.getFeature() != null ? 
                            featureData.getFeature().getFeatureDescription() : "";
                        String version = featureData.getFeature() != null ? 
                            featureData.getFeature().getVersion() : "";
                        
                        // 4. 为每个实体创建EntityExtractionDTO对象并直接插入
                        for (String entityName : entities) {
                            if (entityName != null && !entityName.trim().isEmpty()) {
                                EntityExtractionDTO entity = new EntityExtractionDTO();
                                entity.setNameEn(entityName.trim());
                                entity.setFeatureId(featureId);
                                entity.setSource("JSONL_EXTRACTION_" + version);
                                entity.setDefinitionEn(featureDescription);
                                
                                // 构建相关描述JSON字符串
                                String relDescJson = String.format(
                                    "[{\"desc_en\":\"%s\", \"desc_cn\":\"%s\", \"source\":\"%s\"}]",
                                    featureDescription.replace("\"", "\\\""), 
                                    "",
                                    "JSONL_EXTRACTION"
                                );
                                entity.setRelDesc(relDescJson);
                                
                                totalProcessed++;
                                
                                // 直接插入单个实体
                                try {
                                    // 检查是否已存在
                                    EntityExtractionDTO existing = entityExtractionMapper.findByNameEnAndFeatureId(
                                        entity.getNameEn(), entity.getFeatureId());
                                    
                                    if (existing == null) {
                                        entityExtractionMapper.insert(entity);
                                        totalImported++;
                                        logger.debug("成功插入实体: name={}, featureId={}", 
                                            entity.getNameEn(), entity.getFeatureId());
                                    } else {
                                        logger.debug("实体已存在，跳过: name={}, featureId={}", 
                                            entity.getNameEn(), entity.getFeatureId());
                                    }
                                } catch (Exception ex) {
                                    logger.warn("插入实体失败: name={}, error={}", 
                                        entity.getNameEn(), ex.getMessage());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("解析JSONL行失败: {}", e.getMessage());
                }
            }
            
            logger.info("实体数据导入完成 - 总共处理: {}, 成功导入: {}", totalProcessed, totalImported);
            
            // 返回空列表，保持接口兼容性
            return new ArrayList<>();
            
        } catch (IOException e) {
            logger.error("读取JSONL文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("读取数据文件失败: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("实体数据导入失败", e);
            throw new RuntimeException("实体数据导入服务失败: " + e.getMessage(), e);
        }
    }

    @Override
    public ConceptRelationshipResultDTO analyzeConceptRelationships(String concept, String context, Integer analysisDepth) {
        logger.info("开始概念关系分析: concept={}, context={}, analysisDepth={}", concept, context, analysisDepth);
        
        if (analysisDepth == null || analysisDepth < 1 || analysisDepth > 3) {
            analysisDepth = 2; // 默认分析深度
        }
        
        try {
            ConceptRelationshipResultDTO result = new ConceptRelationshipResultDTO();
            result.setCoreConcept(concept);
            
            // 1. 搜索与概念相关的实体和feature
            List<EntityExtractionDTO> relatedEntities = searchRelatedEntities(concept, context);
            logger.debug("找到{}个相关实体", relatedEntities.size());
            
            if (relatedEntities.isEmpty()) {
                logger.warn("未找到与概念'{}'相关的实体", concept);
                result.setRelationshipSummary("未找到与该概念相关的feature和实体信息");
                result.setConfidenceScore(0.0);
                return result;
            }
            
            // 2. 收集feature描述和关联概念
            List<String> featureDescriptions = extractFeatureDescriptions(relatedEntities);
            result.setTotalFeatures(featureDescriptions.size());
            
            // 3. 从feature中发现其他概念（通过featureId关联）
            List<String> relatedConcepts = discoverRelatedConcepts(concept, relatedEntities, analysisDepth);
            logger.debug("发现{}个关联概念", relatedConcepts.size());
            
            if (relatedConcepts.isEmpty()) {
                result.setRelationshipSummary("该概念相对独立，未发现明显的关联概念");
                result.setConfidenceScore(0.3);
                return result;
            }
            
            // 4. 使用AI分析概念关系
            List<ConceptRelationshipResultDTO.RelatedConcept> analyzedRelationships = 
                analyzeRelationshipsWithAI(concept, relatedConcepts, featureDescriptions);
            
            // 5. 构建最终结果
            result.setRelatedConcepts(analyzedRelationships);
            result.setTotalRelatedConcepts(analyzedRelationships.size());
            
            // 6. 生成关系总结
            generateRelationshipSummary(result);
            
            logger.info("概念关系分析完成: concept={}, 发现{}个关系", concept, analyzedRelationships.size());
            return result;
            
        } catch (Exception e) {
            logger.error("概念关系分析失败: concept={}", concept, e);
            throw new RuntimeException("概念关系分析服务失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从相关实体中发现其他关联概念
     * @param coreConcept 核心概念
     * @param relatedEntities 相关实体列表
     * @param analysisDepth 分析深度
     * @return 发现的关联概念列表
     */
    private List<String> discoverRelatedConcepts(String coreConcept, List<EntityExtractionDTO> relatedEntities, Integer analysisDepth) {
        logger.debug("开始发现关联概念: coreConcept={}, entities={}, depth={}", coreConcept, relatedEntities.size(), analysisDepth);
        
        // 收集所有涉及的featureId
        List<Integer> featureIds = relatedEntities.stream()
                .map(EntityExtractionDTO::getFeatureId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        
        logger.debug("涉及的featureId: {}", featureIds);
        
        // 通过featureId查找其他概念
        List<String> discoveredConcepts = new ArrayList<>();
        
        for (Integer featureId : featureIds) {
            try {
                // 查找同一feature下的其他实体
                List<EntityExtractionDTO> sameFeatureEntities = entityExtractionMapper.findByFeatureId(featureId);
                
                for (EntityExtractionDTO entity : sameFeatureEntities) {
                    String entityName = entity.getNameEn();
                    // 排除核心概念本身和已发现的概念
                    if (entityName != null && 
                        !entityName.equals(coreConcept) && 
                        !discoveredConcepts.contains(entityName) &&
                        entityName.length() > 2) {  // 排除过短的概念
                        discoveredConcepts.add(entityName);
                    }
                }
            } catch (Exception e) {
                logger.warn("查询featureId={}的其他实体失败: {}", featureId, e.getMessage());
            }
        }
        
        // 根据分析深度限制返回数量
        int maxConcepts = Math.min(analysisDepth * 5, 15); // 深度1最多5个，深度2最多10个，深度3最多15个
        List<String> limitedConcepts = discoveredConcepts.stream()
                .distinct()
                .limit(maxConcepts)
                .collect(Collectors.toList());
        
        logger.debug("发现关联概念: {}", limitedConcepts);
        return limitedConcepts;
    }
    
    /**
     * 使用AI分析概念关系
     * @param coreConcept 核心概念
     * @param relatedConcepts 关联概念列表
     * @param featureDescriptions feature描述列表
     * @return 分析后的关系列表
     */
    private List<ConceptRelationshipResultDTO.RelatedConcept> analyzeRelationshipsWithAI(
            String coreConcept, List<String> relatedConcepts, List<String> featureDescriptions) {
        
        List<ConceptRelationshipResultDTO.RelatedConcept> analyzedRelationships = new ArrayList<>();
        
        try {
            // 构建AI分析提示词
            String prompt = Prompt.analyzeConceptRelationships(coreConcept, relatedConcepts, featureDescriptions);
            logger.debug("AI分析提示词构建完成，概念数量: {}", relatedConcepts.size());
            
            // 调用AI模型
            String aiResponse = AIService.deepseek(prompt);
            
            if (aiResponse == null || aiResponse.trim().isEmpty()) {
                logger.warn("AI模型返回空结果");
                return analyzedRelationships;
            }
            
            // 解析AI响应
            analyzedRelationships = parseRelationshipAnalysisResponse(aiResponse, featureDescriptions);
            logger.debug("AI分析完成，解析出{}个关系", analyzedRelationships.size());
            
        } catch (Exception e) {
            logger.error("AI关系分析失败: {}", e.getMessage());
            // 创建后备关系（简单的共现关系）
            analyzedRelationships = createFallbackRelationships(relatedConcepts, featureDescriptions);
        }
        
        return analyzedRelationships;
    }
    
    /**
     * 解析AI关系分析响应
     * @param aiResponse AI模型响应
     * @param featureDescriptions feature描述（用于关联）
     * @return 解析后的关系列表
     */
    private List<ConceptRelationshipResultDTO.RelatedConcept> parseRelationshipAnalysisResponse(
            String aiResponse, List<String> featureDescriptions) {
        
        List<ConceptRelationshipResultDTO.RelatedConcept> relationships = new ArrayList<>();
        
        try {
            String[] lines = aiResponse.split("\n");
            
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || !line.contains("|")) {
                    continue;
                }
                
                // 解析格式: [ConceptName]|[RelationshipType]|[Strength]|[Description]
                String[] parts = line.split("\\|", 4);
                if (parts.length >= 4) {
                    try {
                        String conceptName = parts[0].trim();
                        String relationshipType = parts[1].trim();
                        Double strength = Double.parseDouble(parts[2].trim());
                        String description = parts[3].trim();
                        
                        // 创建关系对象
                        ConceptRelationshipResultDTO.RelatedConcept relatedConcept = 
                            new ConceptRelationshipResultDTO.RelatedConcept(
                                conceptName, relationshipType, strength, description);
                        
                        // 添加相关的feature描述
                        addRelevantFeatureDescriptions(relatedConcept, conceptName, featureDescriptions);
                        
                        relationships.add(relatedConcept);
                        
                    } catch (NumberFormatException e) {
                        logger.warn("解析关系强度失败: {}", line);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.warn("解析AI关系分析响应失败: {}", e.getMessage());
        }
        
        return relationships;
    }
    
    /**
     * 为关联概念添加相关的feature描述
     * @param relatedConcept 关联概念对象
     * @param conceptName 概念名称
     * @param featureDescriptions 所有feature描述
     */
    private void addRelevantFeatureDescriptions(ConceptRelationshipResultDTO.RelatedConcept relatedConcept, 
                                               String conceptName, List<String> featureDescriptions) {
        // 找出包含该概念的feature描述
        List<String> relevantDescriptions = featureDescriptions.stream()
                .filter(desc -> desc.toLowerCase().contains(conceptName.toLowerCase()))
                .limit(3) // 限制数量
                .collect(Collectors.toList());
        
        relatedConcept.setFeatureDescriptions(relevantDescriptions);
        relatedConcept.setSharedFeatures(relevantDescriptions.size());
    }
    
    /**
     * 创建后备关系（当AI分析失败时）
     * @param relatedConcepts 关联概念列表
     * @param featureDescriptions feature描述列表
     * @return 简单的共现关系列表
     */
    private List<ConceptRelationshipResultDTO.RelatedConcept> createFallbackRelationships(
            List<String> relatedConcepts, List<String> featureDescriptions) {
        
        List<ConceptRelationshipResultDTO.RelatedConcept> fallbackRelationships = new ArrayList<>();
        
        for (String conceptName : relatedConcepts) {
            // 计算概念在feature描述中的出现频率作为关系强度
            long occurrenceCount = featureDescriptions.stream()
                    .mapToLong(desc -> countOccurrences(desc.toLowerCase(), conceptName.toLowerCase()))
                    .sum();
            
            double strength = Math.min(0.1 + (occurrenceCount * 0.1), 0.8); // 最低0.1，最高0.8
            
            ConceptRelationshipResultDTO.RelatedConcept relatedConcept = 
                new ConceptRelationshipResultDTO.RelatedConcept(
                    conceptName, "co-occurs", strength, 
                    "与核心概念在相同的feature描述中出现，可能存在功能关联");
            
            addRelevantFeatureDescriptions(relatedConcept, conceptName, featureDescriptions);
            fallbackRelationships.add(relatedConcept);
        }
        
        return fallbackRelationships;
    }
    
    /**
     * 计算字符串中子字符串的出现次数
     */
    private long countOccurrences(String text, String substring) {
        if (text == null || substring == null || substring.isEmpty()) {
            return 0;
        }
        
        long count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
    
    /**
     * 生成关系总结
     * @param result 概念关系分析结果
     */
    private void generateRelationshipSummary(ConceptRelationshipResultDTO result) {
        try {
            // 找出强关系（强度>=0.6）
            List<String> strongRelationships = result.getRelatedConcepts().stream()
                    .filter(rel -> rel.getRelationshipStrength() >= 0.6)
                    .map(rel -> rel.getConceptName() + "(" + rel.getRelationshipType() + ")")
                    .limit(5)
                    .collect(Collectors.toList());
            
            if (!strongRelationships.isEmpty()) {
                String summaryPrompt = Prompt.generateRelationshipSummary(
                    result.getCoreConcept(), 
                    result.getTotalRelatedConcepts(), 
                    strongRelationships);
                
                String aiSummary = AIService.deepseek(summaryPrompt);
                result.setRelationshipSummary(aiSummary != null && !aiSummary.trim().isEmpty() ? 
                    aiSummary.trim() : generateDefaultSummary(result));
            } else {
                result.setRelationshipSummary(generateDefaultSummary(result));
            }
            
            // 计算置信度（基于关系数量和强度）
            double avgStrength = result.getRelatedConcepts().stream()
                    .mapToDouble(ConceptRelationshipResultDTO.RelatedConcept::getRelationshipStrength)
                    .average()
                    .orElse(0.0);
            
            result.setConfidenceScore(Math.min(avgStrength * 0.8 + (result.getTotalRelatedConcepts() * 0.02), 1.0));
            
        } catch (Exception e) {
            logger.warn("生成关系总结失败: {}", e.getMessage());
            result.setRelationshipSummary(generateDefaultSummary(result));
            result.setConfidenceScore(0.5);
        }
    }
    
    /**
     * 生成默认关系总结
     */
    private String generateDefaultSummary(ConceptRelationshipResultDTO result) {
        return String.format(
            "分析了与'%s'相关的%d个feature，发现%d个关联概念。" +
            "这些关系主要涉及功能依赖、实现关联和系统交互等方面。",
            result.getCoreConcept(),
            result.getTotalFeatures(),
            result.getTotalRelatedConcepts()
        );
    }
  

    @Override
    public ConceptValidationResultDTO validateConcept(String concept, String context) {
        logger.info("开始验证概念: concept={}, context={}", concept, context);
        
        if (concept == null || concept.trim().isEmpty()) {
            throw new IllegalArgumentException("概念不能为空");
        }
        
        try {
            // 1. 注释掉概念翻译和标准化逻辑，后续使用其他方式实现
            // ConceptTranslationResult translationResult = translateAndStandardizeConcept(concept.trim(), context);
            // logger.debug("概念翻译结果: 原概念='{}', 主要术语='{}', 备选术语={}", 
            //     concept, translationResult.primaryTerm, translationResult.alternativeTerms);
            
            // 2. 直接使用原始概念进行数据库查询
            // List<String> searchTerms = buildSearchTerms(translationResult);
            List<EntityExtractionDTO> allMatches = new ArrayList<>();
            boolean hasExactMatch = false;
            
            String searchTerm = concept.trim();
            // 先尝试精确匹配
            List<EntityExtractionDTO> exactMatches = entityExtractionMapper.findByExactConcept(searchTerm);
            if (!exactMatches.isEmpty()) {
                hasExactMatch = true;
                allMatches.addAll(exactMatches);
                logger.debug("概念'{}'精确匹配{}个结果", searchTerm, exactMatches.size());
            } else {
                // 如果精确匹配失败，尝试模糊匹配
                List<EntityExtractionDTO> fuzzyMatches = entityExtractionMapper.findByConcept(searchTerm);
                allMatches.addAll(fuzzyMatches);
                logger.debug("概念'{}'模糊匹配{}个结果", searchTerm, fuzzyMatches.size());
            }
            
            // 去除重复项
            allMatches = allMatches.stream()
                .distinct()
                .collect(Collectors.toList());
            
            logger.debug("概念'{}'总查询结果: {}个匹配", concept, allMatches.size());
            
            // 3. 构建验证结果
            return buildConceptValidationResult(concept, context, allMatches, hasExactMatch);
            
        } catch (Exception e) {
            logger.error("概念验证失败: concept={}, error={}", concept, e.getMessage(), e);
            throw new RuntimeException("概念验证服务异常: " + e.getMessage(), e);
        }
    }
    
    /**
     * 构建概念验证结果
     */
    private ConceptValidationResultDTO buildConceptValidationResult(
            String concept, String context, List<EntityExtractionDTO> matches, boolean isExactMatch) {
        
        if (matches.isEmpty()) {
            // 没有找到匹配的概念
            return new ConceptValidationResultDTO(
                null,
                concept, 
                false, 
                0, 
                null, 
                null, 
                "在概念数据库中未找到匹配的概念"
            );
        }
        
        if (matches.size() == 1) {
            // 只有一个匹配的概念
            EntityExtractionDTO match = matches.get(0);
            String details = isExactMatch ? "在概念数据库中找到精确匹配的概念" : "在概念数据库中找到相似的概念";
                
            return new ConceptValidationResultDTO(
                match.getEid(),
                concept,
                true,
                1,
                match.getNameEn(),
                match.getDefinitionEn(),
                details
            );
        }
        
        // 有多个匹配的概念，需要使用AI判断最佳匹配
        return findBestMatchWithAI(concept, context, matches, isExactMatch);
    }
    
    /**
     * 使用AI判断多个概念中的最佳匹配
     */
    private ConceptValidationResultDTO findBestMatchWithAI(
            String concept, String context, List<EntityExtractionDTO> matches, boolean isExactMatch) {
        
        try {
            logger.debug("使用AI判断最佳匹配: concept={}, matches={}", concept, matches.size());
            
            // 构建AI提示词
            String prompt = buildConceptMatchingPrompt(concept, context, matches);
            
            // 调用AI进行判断
            String aiResponse = AIService.deepseek(prompt);
            
            if (aiResponse != null && !aiResponse.trim().isEmpty()) {
                // 解析AI响应，找到最佳匹配
                return parseAIMatchingResponse(concept, context, matches, aiResponse, isExactMatch);
            } else {
                logger.warn("AI判断返回空结果，使用默认策略");
                return createDefaultBestMatch(concept, context, matches, isExactMatch);
            }
            
        } catch (Exception e) {
            logger.error("AI判断最佳匹配失败，使用默认策略: {}", e.getMessage());
            logger.debug("AI判断失败的详细信息: concept={}, matches={}", concept, matches.size(), e);
            return createDefaultBestMatch(concept, context, matches, isExactMatch);
        }
    }

    /**
     * 构建概念匹配的AI提示词
     */
    private String buildConceptMatchingPrompt(String concept, String context, List<EntityExtractionDTO> matches) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("我需要你帮我判断哪个概念定义最匹配用户输入的概念和上下文。\n\n");
        prompt.append("用户输入的概念: ").append(concept).append("\n");
        if (context != null && !context.trim().isEmpty()) {
            prompt.append("用户提供的上下文: ").append(context).append("\n");
        }
        prompt.append("\n候选概念列表:\n");
        
        for (int i = 0; i < matches.size(); i++) {
            EntityExtractionDTO match = matches.get(i);
            prompt.append(String.format("%d. 概念名称: %s\n", i + 1, match.getNameEn()));
            if (match.getNameCn() != null && !match.getNameCn().trim().isEmpty()) {
                prompt.append(String.format("   中文名称: %s\n", match.getNameCn()));
            }
            if (match.getDefinitionEn() != null && !match.getDefinitionEn().trim().isEmpty()) {
                prompt.append(String.format("   英文定义: %s\n", match.getDefinitionEn()));
            }
            if (match.getDefinitionCn() != null && !match.getDefinitionCn().trim().isEmpty()) {
                prompt.append(String.format("   中文定义: %s\n", match.getDefinitionCn()));
            }
            prompt.append("\n");
        }
        
        prompt.append("请返回JSON格式的结果，不要使用markdown代码块包装，直接返回纯JSON:\n");
        prompt.append("{\n");
        prompt.append("  \"bestMatchIndex\": <最佳匹配的序号(1-based)>,\n");
        prompt.append("  \"reasoning\": \"<判断理由>\"\n");
        prompt.append("}\n\n");
        prompt.append("注意：请确保返回的是有效的JSON格式，不要添加任何```json或```标记。");
        
        return prompt.toString();
    }
    
    /**
     * 解析AI匹配响应
     */
    private ConceptValidationResultDTO parseAIMatchingResponse(
            String concept, String context, List<EntityExtractionDTO> matches, 
            String aiResponse, boolean isExactMatch) {
        
        try {
            // 清理AI响应中的markdown格式
            String cleanedResponse = cleanJsonResponse(aiResponse);
            logger.debug("清理后的AI响应: {}", cleanedResponse);
            
            // 尝试解析JSON响应
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> response = mapper.readValue(cleanedResponse, java.util.Map.class);
            
            Integer bestMatchIndex = (Integer) response.get("bestMatchIndex");
            String reasoning = (String) response.get("reasoning");
            
            if (bestMatchIndex != null && bestMatchIndex >= 1 && bestMatchIndex <= matches.size()) {
                EntityExtractionDTO bestMatch = matches.get(bestMatchIndex - 1);
                
                String details = String.format("在概念数据库中找到%d个匹配的概念。AI判断最佳匹配为第%d个概念，理由: %s", 
                    matches.size(), bestMatchIndex, reasoning);
                
                return new ConceptValidationResultDTO(
                    bestMatch.getEid(),
                    concept,
                    true,
                    matches.size(),
                    bestMatch.getNameEn(),
                    bestMatch.getDefinitionEn(),
                    details
                );
            }
            
        } catch (Exception e) {
            logger.warn("解析AI响应失败: {}", e.getMessage());
            logger.debug("原始AI响应: {}", aiResponse);
        }
        
        // 解析失败，使用默认策略
        return createDefaultBestMatch(concept, context, matches, isExactMatch);
    }
    
    /**
     * 清理AI响应中的markdown格式，提取纯JSON内容
     */
    private String cleanJsonResponse(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return aiResponse;
        }
        
        String cleaned = aiResponse.trim();
        
        // 移除开头的```json标记
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7).trim();
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3).trim();
        }
        
        // 移除结尾的```标记
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }
        
        // 移除其他可能的markdown标记
        cleaned = cleaned.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("\\n?```$", "");
        
        return cleaned.trim();
    }
    
    /**
     * 创建默认的最佳匹配结果（当AI判断失败时使用）
     */
    private ConceptValidationResultDTO createDefaultBestMatch(
            String concept, String context, List<EntityExtractionDTO> matches, boolean isExactMatch) {
        
        // 选择第一个匹配作为默认最佳匹配
        EntityExtractionDTO bestMatch = matches.get(0);
        
        String details = String.format("在概念数据库中找到%d个匹配的概念。由于AI判断失败，选择第一个匹配作为最佳结果", 
            matches.size());
        
        return new ConceptValidationResultDTO(
            bestMatch.getEid(),
            concept,
            true,
            matches.size(),
            bestMatch.getNameEn(),
            bestMatch.getDefinitionEn(),
            details
        );
    }

    @Override
    public CodeClusterResultDTO analyzeCodeClusters(String concept, String context, String version) {
        logger.info("开始代码聚类分析: concept={}, context={}, version={}", concept, context, version);
        
        if (version == null || version.trim().isEmpty()) {
            version = "v6.14"; // 默认版本
        }
        
        try {
            // 1. 直接获取Bootlin搜索结果，不获取完整代码片段
            List<BootlinSearchResultDTO> bootlinResults = getBootlinSearchResults(concept, context, version);
            
            if (bootlinResults.isEmpty()) {
                logger.warn("未找到相关代码进行聚类分析: concept={}", concept);
                return createEmptyClusterResult(concept, version);
            }
            
            // 2. 直接从Bootlin结果中提取单行代码信息
            List<CodeClusterResultDTO.CodeLineInfo> codeLines = extractCodeLinesFromBootlinResults(bootlinResults);
            logger.debug("提取到{}行代码进行分析", codeLines.size());
            
            if (codeLines.isEmpty()) {
                logger.warn("无法从Bootlin结果中提取有效代码行: concept={}", concept);
                return createEmptyClusterResult(concept, version);
            }
            
            // 3. 词袋模型处理
            Map<String, Integer> tokenFrequency = buildTokenFrequencyMap(codeLines);
            Set<String> filteredTokens = filterStopWords(tokenFrequency.keySet());
            List<String> coreTokens = identifyCoreConcepts(filteredTokens, tokenFrequency);
            
            // 4. 代码聚类
            List<CodeClusterResultDTO.ConceptCluster> clusters = performCodeClustering(
                concept, codeLines, coreTokens, tokenFrequency);
            
            // 5. 为每个聚类增强：获取完整代码片段、AI解释和汇总说明
            enhanceClusterAnalysis(clusters, concept, version);
            
            // 6. 构建结果
            String analysisSummary = generateClusterAnalysisSummary(concept, codeLines.size(), clusters.size());
            
            CodeClusterResultDTO result = new CodeClusterResultDTO(
                concept,
                clusters,
                codeLines.size(),
                clusters.size(),
                analysisSummary,
                version
            );
            
            logger.info("代码聚类分析完成: concept={}, 代码行数={}, 聚类数={}", 
                concept, codeLines.size(), clusters.size());
            
            return result;
            
        } catch (Exception e) {
            logger.error("代码聚类分析失败: concept={}, error={}", concept, e.getMessage(), e);
            throw new RuntimeException("代码聚类分析服务失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 创建空的聚类分析结果
     */
    private CodeClusterResultDTO createEmptyClusterResult(String concept, String version) {
        return new CodeClusterResultDTO(
            concept,
            new ArrayList<>(),
            0,
            0,
            "未找到相关代码进行聚类分析",
            version
        );
    }
    
    /**
     * 直接获取Bootlin搜索结果，不进行代码片段提取
     */
    private List<BootlinSearchResultDTO> getBootlinSearchResults(String concept, String context, String version) {
        List<BootlinSearchResultDTO> bootlinResults = new ArrayList<>();
        
        // 提取搜索实体（与searchWithBootlin中的逻辑相同）
        List<String> searchEntities = extractSearchEntities(concept, context);
        
        // 静态分析：规范化搜索实体格式
        searchEntities = normalizeSearchEntities(searchEntities);
        
        logger.debug("准备搜索的实体: {}", searchEntities);
        
        // 同步搜索多个实体
        for (String entity : searchEntities) {
            try {
                BootlinSearchResultDTO bootlinResult = bootlinSearchService.search(entity, version);
                if (bootlinResult != null && bootlinResult.isSuccess()) {
                    bootlinResults.add(bootlinResult);
                    logger.debug("Bootlin找到结果: entity={}, url={}", 
                        bootlinResult.getEntity(), bootlinResult.getUrl());
                }
            } catch (Exception e) {
                logger.warn("Bootlin搜索实体 '{}' 失败", entity, e);
            }
        }
        
        return bootlinResults;
    }
    
    /**
     * 从Bootlin搜索结果中直接提取单行代码信息
     */
    private List<CodeClusterResultDTO.CodeLineInfo> extractCodeLinesFromBootlinResults(List<BootlinSearchResultDTO> bootlinResults) {
        List<CodeClusterResultDTO.CodeLineInfo> codeLines = new ArrayList<>();
        
        for (BootlinSearchResultDTO bootlinResult : bootlinResults) {
            String version = bootlinResult.getVersion(); // 获取版本信息
            
            // 处理定义信息 - 标记为 "definitions"
            if (bootlinResult.getDefinitions() != null) {
                for (BootlinSearchResultDTO.SearchResultItem def : bootlinResult.getDefinitions()) {
                    if (def.getType().equals("struct") || def.getType().equals("function")) {
                        List<CodeClusterResultDTO.CodeLineInfo> defCodeLines = extractCodeLinesFromSearchItem(def, version, "definitions");
                        codeLines.addAll(defCodeLines);
                    }
                }
            }
            
            // 处理引用信息（限制数量） - 标记为 "references"
            if (bootlinResult.getReferences() != null) {
                List<BootlinSearchResultDTO.SearchResultItem> limitedRefs = bootlinResult.getReferences().stream()
                    .limit(100)
                    .collect(Collectors.toList());
                    
                for (BootlinSearchResultDTO.SearchResultItem ref : limitedRefs) {
                    List<CodeClusterResultDTO.CodeLineInfo> refCodeLines = extractCodeLinesFromSearchItem(ref, version, "references");
                    codeLines.addAll(refCodeLines);
                }
            }
        }
        
        // 去重处理：基于文件路径、行号和源类型去除重复的代码行
        List<CodeClusterResultDTO.CodeLineInfo> deduplicatedCodeLines = deduplicateCodeLines(codeLines);
        logger.debug("代码行去重处理完成: 原始{}行 -> 去重后{}行", codeLines.size(), deduplicatedCodeLines.size());
        
        return deduplicatedCodeLines;
    }
    
    /**
     * 对代码行进行去重处理
     * 基于文件路径、行号和源类型识别重复项，优先保留definitions类型的代码行
     * 
     * @param codeLines 原始代码行列表
     * @return 去重后的代码行列表
     */
    private List<CodeClusterResultDTO.CodeLineInfo> deduplicateCodeLines(List<CodeClusterResultDTO.CodeLineInfo> codeLines) {
        if (codeLines == null || codeLines.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 使用LinkedHashMap保持插入顺序，同时实现去重
        Map<String, CodeClusterResultDTO.CodeLineInfo> uniqueCodeLines = new LinkedHashMap<>();
        
        for (CodeClusterResultDTO.CodeLineInfo codeLine : codeLines) {
            // 创建基于文件路径和行号的唯一键
            String uniqueKey = generateUniqueKey(codeLine.getFilePath(), codeLine.getLineNumber());
            
            if (!uniqueCodeLines.containsKey(uniqueKey)) {
                // 首次出现，直接添加
                uniqueCodeLines.put(uniqueKey, codeLine);
            } else {
                // 已存在相同位置的代码行，优先保留definitions类型
                CodeClusterResultDTO.CodeLineInfo existingLine = uniqueCodeLines.get(uniqueKey);
                if ("definitions".equals(codeLine.getSourceType()) && 
                    !"definitions".equals(existingLine.getSourceType())) {
                    // 新的是definitions类型，现有的不是，则替换
                    uniqueCodeLines.put(uniqueKey, codeLine);
                    logger.debug("代码行去重：优先保留definitions类型 {}:{}", 
                        codeLine.getFilePath(), codeLine.getLineNumber());
                } else {
                    // 其他情况保持原有的代码行（先到先得）
                    logger.debug("代码行去重：跳过重复项 {}:{}", 
                        codeLine.getFilePath(), codeLine.getLineNumber());
                }
            }
        }
        
        return new ArrayList<>(uniqueCodeLines.values());
    }
    
    /**
     * 生成基于文件路径和行号的唯一键
     * 
     * @param filePath 文件路径
     * @param lineNumber 行号
     * @return 唯一键字符串
     */
    private String generateUniqueKey(String filePath, int lineNumber) {
        return filePath + ":" + lineNumber;
    }
    
    /**
     * 从单个搜索结果项中提取代码行
     */
    private List<CodeClusterResultDTO.CodeLineInfo> extractCodeLinesFromSearchItem(BootlinSearchResultDTO.SearchResultItem item, String version, String sourceType) {
        List<CodeClusterResultDTO.CodeLineInfo> codeLines = new ArrayList<>();
        
        if (item.getLine() != null && !item.getLine().isEmpty()) {
            for (String lineStr : item.getLine()) {
                int lineNumber = parseLineNumber(lineStr);
                if (lineNumber > 0) {
                    // 使用Git命令从指定版本读取文件的指定行
                    String singleLineCode = readSingleLineFromFile(item.getPath(), lineNumber, version);
                    if (singleLineCode != null && !singleLineCode.trim().isEmpty()) {
                        List<String> identifiers = extractIdentifiers(singleLineCode);
                        
                        CodeClusterResultDTO.CodeLineInfo codeLineInfo = new CodeClusterResultDTO.CodeLineInfo(
                            item.getPath(),
                            lineNumber,
                            singleLineCode.trim(),
                            identifiers,
                            sourceType  // 添加来源标记
                        );
                        codeLines.add(codeLineInfo);
                    }
                }
            }
        }
        
        return codeLines;
    }
    
    /**
     * 从指定文件的指定行号读取单行代码，使用Git命令获取指定版本的文件内容
     */
    private String readSingleLineFromFile(String filePath, int lineNumber, String version) {
        try {
            // 使用Git命令获取指定版本的文件内容
            List<String> lines = readFileFromGitVersion(filePath, version);
            
            if (lines != null && lineNumber > 0 && lineNumber <= lines.size()) {
                return lines.get(lineNumber - 1); // 行号从1开始，数组从0开始
            } else {
                logger.debug("行号超出范围或文件不存在: file={}, line={}, totalLines={}, version={}", 
                    filePath, lineNumber, lines != null ? lines.size() : 0, version);
                return null;
            }
        } catch (Exception e) {
            logger.warn("读取单行代码异常: file={}, line={}, version={}", filePath, lineNumber, version, e);
            return null;
        }
    }
    
    /**
     * 使用Git命令从指定版本读取文件内容
     * @param filePath 文件路径
     * @param version 版本标签（如v6.14）
     * @return 文件行列表，如果读取失败返回null
     */
    private List<String> readFileFromGitVersion(String filePath, String version) {
        try {
            // 构建Git命令：git show <version>:<file_path>
            ProcessBuilder pb = new ProcessBuilder("git", "show", version + ":" + filePath);
            pb.directory(new java.io.File(kernelSourcePath)); // 设置工作目录
            pb.redirectErrorStream(true); // 合并错误流和标准输出流
            
            Process process = pb.start();
            
            // 读取命令输出
            List<String> lines = new ArrayList<>();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
            
            // 等待命令完成
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logger.debug("Git命令成功: file={}, version={}, lines={}", filePath, version, lines.size());
                return lines;
            } else {
                logger.debug("Git命令失败: file={}, version={}, exitCode={}", filePath, version, exitCode);
                return null;
            }
            
        } catch (IOException e) {
            logger.debug("执行Git命令失败: file={}, version={}, error={}", filePath, version, e.getMessage());
            return null;
        } catch (InterruptedException e) {
            logger.debug("Git命令被中断: file={}, version={}", filePath, version);
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            logger.warn("Git命令异常: file={}, version={}", filePath, version, e);
            return null;
        }
    }
    
    /**
     * 从代码行中提取标识符
     * 使用正则表达式匹配C语言标识符
     */
    private List<String> extractIdentifiers(String codeLine) {
        List<String> identifiers = new ArrayList<>();
        
        // C语言标识符的正则表达式：字母或下划线开头，后跟字母、数字或下划线
        Pattern identifierPattern = Pattern.compile("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b");
        Matcher matcher = identifierPattern.matcher(codeLine);
        
        while (matcher.find()) {
            String identifier = matcher.group();
            // 过滤掉C语言关键字
            if (!isCKeyword(identifier) && identifier.length() > 1) {
                identifiers.add(identifier);
            }
        }
        
        return identifiers.stream().distinct().collect(Collectors.toList());
    }
    
    /**
     * 判断是否为C语言关键字
     */
    private boolean isCKeyword(String word) {
        Set<String> cKeywords = Set.of(
            "auto", "break", "case", "char", "const", "continue", "default", "do",
            "double", "else", "enum", "extern", "float", "for", "goto", "if",
            "int", "long", "register", "return", "short", "signed", "sizeof", "static",
            "struct", "switch", "typedef", "union", "unsigned", "void", "volatile", "while"
        );
        return cKeywords.contains(word.toLowerCase());
    }
    
    /**
     * 构建词汇频率映射表
     */
    private Map<String, Integer> buildTokenFrequencyMap(List<CodeClusterResultDTO.CodeLineInfo> codeLines) {
        Map<String, Integer> frequency = new HashMap<>();
        
        for (CodeClusterResultDTO.CodeLineInfo codeLine : codeLines) {
            for (String identifier : codeLine.getIdentifiers()) {
                // 进一步分词：处理驼峰命名和下划线命名
                List<String> tokens = tokenizeIdentifier(identifier);
                for (String token : tokens) {
                    frequency.put(token, frequency.getOrDefault(token, 0) + 1);
                }
            }
        }
        
        return frequency;
    }
    
    /**
     * 分词：处理驼峰命名和下划线命名
     */
    private List<String> tokenizeIdentifier(String identifier) {
        List<String> tokens = new ArrayList<>();
        
        // 先按下划线分割
        String[] underscoreParts = identifier.split("_");
        
        for (String part : underscoreParts) {
            if (part.isEmpty()) continue;
            
            // 再按驼峰命名分割
            List<String> camelCaseTokens = splitCamelCase(part);
            tokens.addAll(camelCaseTokens);
        }
        
        return tokens.stream()
                .filter(token -> token.length() > 1) // 过滤单字符
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.toList());
    }
    
    /**
     * 按驼峰命名法分割字符串
     */
    private List<String> splitCamelCase(String str) {
        List<String> tokens = new ArrayList<>();
        
        // 使用正则表达式分割驼峰命名
        Pattern pattern = Pattern.compile("([a-z])([A-Z])");
        String[] parts = pattern.matcher(str).replaceAll("$1 $2").split(" ");
        
        for (String part : parts) {
            if (!part.trim().isEmpty()) {
                tokens.add(part.trim());
            }
        }
        
        return tokens;
    }
    
    /**
     * 过滤停用词
     */
    private Set<String> filterStopWords(Set<String> tokens) {
        Set<String> stopWords = getStopWords();
        
        return tokens.stream()
                .filter(token -> !stopWords.contains(token.toLowerCase()))
                .filter(token -> token.length() > 2) // 过滤长度小于3的词
                .collect(Collectors.toSet());
    }
    
    /**
     * 获取停用词集合
     */
    private Set<String> getStopWords() {
        Set<String> stopWords = new HashSet<>();
        
        // 通用停用词
        stopWords.addAll(Set.of("the", "and", "for", "are", "but", "not", "you", "all", "can", "had", "her", "was", "one", "our", "out", "day", "get", "has", "him", "his", "how", "its", "may", "new", "now", "old", "see", "two", "who", "boy", "did", "use", "way", "she", "many", "some", "time"));
        
        // 代码领域常见词
        stopWords.addAll(Set.of("void", "int", "char", "long", "short", "unsigned", "signed", "const", "static", "extern", "auto", "register", "volatile", "inline", "return", "break", "continue", "goto", "case", "default", "switch", "while", "for", "do", "if", "else", "sizeof", "typedef", "struct", "union", "enum"));
        
        // Linux内核常见词
        stopWords.addAll(Set.of("kernel", "linux", "include", "define", "ifdef", "ifndef", "endif", "undef", "line", "file"));
        
        return stopWords;
    }
    
    /**
     * 识别核心概念
     */
    private List<String> identifyCoreConcepts(Set<String> filteredTokens, Map<String, Integer> frequency) {
        return filteredTokens.stream()
                .filter(token -> frequency.get(token) >= 2) // 至少出现2次
                .sorted((a, b) -> frequency.get(b) - frequency.get(a)) // 按频率降序
                .limit(10) // 取前10个核心概念
                .collect(Collectors.toList());
    }
    
    /**
     * 执行代码聚类
     */
    private List<CodeClusterResultDTO.ConceptCluster> performCodeClustering(
            String originalConcept, 
            List<CodeClusterResultDTO.CodeLineInfo> codeLines,
            List<String> coreTokens,
            Map<String, Integer> tokenFrequency) {
        
        Map<String, List<CodeClusterResultDTO.CodeLineInfo>> clusters = new HashMap<>();
        
        // 1. 首先创建默认聚类组用于存放 definitions 代码段
        String defaultClusterName = "核心定义";
        clusters.put(defaultClusterName, new ArrayList<>());
        
        // 2. 为每个核心概念创建聚类
        for (String coreToken : coreTokens) {
            clusters.put(coreToken, new ArrayList<>());
        }
        
        // 3. 优先将 definitions 代码行分配到默认聚类组
        List<CodeClusterResultDTO.CodeLineInfo> remainingCodeLines = new ArrayList<>();
        
        for (CodeClusterResultDTO.CodeLineInfo codeLine : codeLines) {
            if ("definitions".equals(codeLine.getSourceType())) {
                // definitions 代码行直接放入默认聚类组
                clusters.get(defaultClusterName).add(codeLine);
                logger.debug("将 definitions 代码行分配到默认聚类: {}:{}", 
                    codeLine.getFilePath(), codeLine.getLineNumber());
            } else {
                // 其他代码行留待后续处理
                remainingCodeLines.add(codeLine);
            }
        }
        
        // 4. 将剩余代码行（主要是 references）按原有逻辑分配到其他聚类
        for (CodeClusterResultDTO.CodeLineInfo codeLine : remainingCodeLines) {
            String bestMatch = findBestMatchingConcept(codeLine, coreTokens);
            if (bestMatch != null) {
                clusters.get(bestMatch).add(codeLine);
            }
        }
        
        // 5. 构建聚类结果
        List<CodeClusterResultDTO.ConceptCluster> result = new ArrayList<>();
        
        // 首先添加默认聚类组（definitions）
        List<CodeClusterResultDTO.CodeLineInfo> defaultClusterLines = clusters.get(defaultClusterName);
        if (!defaultClusterLines.isEmpty()) {
            CodeClusterResultDTO.ConceptCluster defaultCluster = new CodeClusterResultDTO.ConceptCluster(
                defaultClusterName,
                defaultClusterLines.size(), // 使用代码行数作为频次
                defaultClusterLines,
                List.of(originalConcept, "definition", "核心") // 设置相关核心词汇
            );
            result.add(defaultCluster);
            logger.debug("创建默认聚类组: {}, 包含{}行代码", defaultClusterName, defaultClusterLines.size());
        }
        
        // 然后添加其他概念聚类
        for (Map.Entry<String, List<CodeClusterResultDTO.CodeLineInfo>> entry : clusters.entrySet()) {
            String concept = entry.getKey();
            List<CodeClusterResultDTO.CodeLineInfo> clusterLines = entry.getValue();
            
            // 跳过默认聚类组（已处理）和空聚类
            if (concept.equals(defaultClusterName) || clusterLines.isEmpty()) {
                continue;
            }
            
            CodeClusterResultDTO.ConceptCluster cluster = new CodeClusterResultDTO.ConceptCluster(
                concept,
                tokenFrequency.get(concept),
                clusterLines,
                List.of(concept) // 简化版核心词汇
            );
            result.add(cluster);
            logger.debug("创建概念聚类: {}, 包含{}行代码", concept, clusterLines.size());
        }
        
        return result;
    }
    
    /**
     * 为代码行找到最佳匹配的核心概念
     */
    private String findBestMatchingConcept(CodeClusterResultDTO.CodeLineInfo codeLine, List<String> coreTokens) {
        for (String identifier : codeLine.getIdentifiers()) {
            List<String> tokens = tokenizeIdentifier(identifier);
            for (String token : tokens) {
                if (coreTokens.contains(token.toLowerCase())) {
                    return token.toLowerCase();
                }
            }
        }
        
        // 如果没有直接匹配，返回第一个核心概念（作为默认）
        return coreTokens.isEmpty() ? null : coreTokens.get(0);
    }

    /**
     * 为聚类结果进行增强分析
     * 获取完整代码片段、生成AI解释和汇总说明
     * 
     * @param clusters 聚类结果列表
     * @param concept 原始搜索概念
     * @param version Git版本
     */
    private void enhanceClusterAnalysis(List<CodeClusterResultDTO.ConceptCluster> clusters, String concept, String version) {
        logger.info("开始为{}个聚类进行增强分析", clusters.size());
        
        for (CodeClusterResultDTO.ConceptCluster cluster : clusters) {
            try {
                // 1. 获取完整代码片段
                List<CodeSearchResultDTO> codeSnippets = getCompleteCodeSnippets(cluster, version);
                cluster.setCodeSnippets(codeSnippets);
                logger.debug("聚类'{}'获取到{}个完整代码片段", cluster.getClusterConcept(), codeSnippets.size());
                
                // 2. 使用AI同时生成解释和汇总说明
                generateAIAnalysis(cluster, concept, codeSnippets);
                logger.debug("聚类'{}'生成AI分析完成", cluster.getClusterConcept());
                
            } catch (Exception e) {
                logger.warn("聚类'{}'增强分析失败: {}", cluster.getClusterConcept(), e.getMessage());
                // 设置默认值确保结构完整
                cluster.setCodeSnippets(new ArrayList<>());
                cluster.setClusterSummary("该聚类包含与'" + cluster.getClusterConcept() + "'相关的代码");
            }
        }
        
        logger.info("聚类增强分析完成");
    }
    
    /**
     * 获取聚类中每个代码行的完整代码片段 - 优化版本
     * 通过智能采样、文件级缓存和去重来提高性能
     * 
     * @param cluster 概念聚类
     * @param version Git版本
     * @return 完整代码片段列表
     */
    private List<CodeSearchResultDTO> getCompleteCodeSnippets(CodeClusterResultDTO.ConceptCluster cluster, String version) {
        List<CodeSearchResultDTO> codeSnippets = new ArrayList<>();
        
        // 1. 性能优化：限制处理的代码行数量，避免处理几百行
        List<CodeClusterResultDTO.CodeLineInfo> selectedLines = selectRepresentativeCodeLines(cluster.getCodeLines());
        logger.debug("从{}行代码中选择{}行进行详细分析", cluster.getCodeLines().size(), selectedLines.size());
        
        // 2. 按文件分组，实现文件级批量处理
        Map<String, List<CodeClusterResultDTO.CodeLineInfo>> fileGroups = groupCodeLinesByFile(selectedLines);
        logger.debug("代码行分布在{}个文件中", fileGroups.size());
        
        // 3. 为每个文件批量获取代码片段
        for (Map.Entry<String, List<CodeClusterResultDTO.CodeLineInfo>> entry : fileGroups.entrySet()) {
            String filePath = entry.getKey();
            List<CodeClusterResultDTO.CodeLineInfo> fileLines = entry.getValue();
            
            try {
                // 批量处理同一文件的多行
                List<CodeSearchResultDTO> fileSnippets = getCodeSnippetsForFile(filePath, fileLines, cluster.getClusterConcept(), version);
                codeSnippets.addAll(fileSnippets);
                logger.debug("文件{}成功获取{}个代码片段", filePath, fileSnippets.size());
            } catch (Exception e) {
                logger.warn("处理文件{}的代码片段失败: {}", filePath, e.getMessage());
                // 为失败的文件创建简化版本
                List<CodeSearchResultDTO> fallbackSnippets = createFallbackSnippetsForFile(filePath, fileLines, cluster.getClusterConcept(), version);
                codeSnippets.addAll(fallbackSnippets);
            }
        }
        
        // 4. 最终去重和排序
        List<CodeSearchResultDTO> finalResults = deduplicateAndSortCodeSnippets(codeSnippets);
        logger.debug("最终返回{}个去重后的代码片段", finalResults.size());
        
        return finalResults;
    }
    
    /**
     * 智能选择有代表性的代码行，避免处理过多行数
     * 
     * @param allCodeLines 所有代码行
     * @return 选择的代表性代码行
     */
    private List<CodeClusterResultDTO.CodeLineInfo> selectRepresentativeCodeLines(List<CodeClusterResultDTO.CodeLineInfo> allCodeLines) {
        if (allCodeLines.size() <= 10) {
            // 如果总数不多，直接返回所有行
            return new ArrayList<>(allCodeLines);
        }
        
        List<CodeClusterResultDTO.CodeLineInfo> selected = new ArrayList<>();
        
        // 1. 按文件分组
        Map<String, List<CodeClusterResultDTO.CodeLineInfo>> fileGroups = allCodeLines.stream()
                .collect(Collectors.groupingBy(CodeClusterResultDTO.CodeLineInfo::getFilePath));
        
        // 2. 为每个文件选择代表性行
        for (Map.Entry<String, List<CodeClusterResultDTO.CodeLineInfo>> entry : fileGroups.entrySet()) {
            List<CodeClusterResultDTO.CodeLineInfo> fileLines = entry.getValue();
            
            if (fileLines.size() <= 3) {
                // 文件行数较少，全部保留
                selected.addAll(fileLines);
            } else {
                // 文件行数较多，智能采样
                selected.addAll(sampleCodeLinesFromFile(fileLines));
            }
        }
        
        // 3. 如果选择的行数还是太多，进行全局采样
        if (selected.size() > 15) {
            selected = selected.stream()
                    .limit(15)  // 最多保留15行
                    .collect(Collectors.toList());
        }
        
        return selected;
    }
    
    /**
     * 从单个文件中采样代表性代码行
     * 
     * @param fileLines 文件中的所有代码行
     * @return 采样的代表性代码行
     */
    private List<CodeClusterResultDTO.CodeLineInfo> sampleCodeLinesFromFile(List<CodeClusterResultDTO.CodeLineInfo> fileLines) {
        List<CodeClusterResultDTO.CodeLineInfo> sampled = new ArrayList<>();
        
        // 按行号排序
        List<CodeClusterResultDTO.CodeLineInfo> sortedLines = fileLines.stream()
                .sorted(Comparator.comparingInt(CodeClusterResultDTO.CodeLineInfo::getLineNumber))
                .collect(Collectors.toList());
        
        // 采样策略：取开头、中间和结尾的代表性行
        int size = sortedLines.size();
        if (size >= 1) sampled.add(sortedLines.get(0));                    // 第一行
        if (size >= 3) sampled.add(sortedLines.get(size / 2));             // 中间行
        if (size >= 2) sampled.add(sortedLines.get(size - 1));             // 最后一行
        
        // 如果还有空间，添加更多样本
        if (sampled.size() < 5 && size > 3) {
            if (size >= 5) sampled.add(sortedLines.get(size / 4));         // 四分之一位置
            if (size >= 6) sampled.add(sortedLines.get(size * 3 / 4));     // 四分之三位置
        }
        
        return sampled.stream().distinct().collect(Collectors.toList());
    }
    
    /**
     * 按文件路径对代码行进行分组
     * 
     * @param codeLines 代码行列表
     * @return 按文件分组的Map
     */
    private Map<String, List<CodeClusterResultDTO.CodeLineInfo>> groupCodeLinesByFile(List<CodeClusterResultDTO.CodeLineInfo> codeLines) {
        return codeLines.stream()
                .collect(Collectors.groupingBy(CodeClusterResultDTO.CodeLineInfo::getFilePath));
    }
    
    /**
     * 为单个文件批量获取代码片段
     * 通过一次文件解析处理多行，提高效率
     * 
     * @param filePath 文件路径
     * @param fileLines 文件中的代码行
     * @param concept 概念名称
     * @param version Git版本
     * @return 代码片段列表
     */
    private List<CodeSearchResultDTO> getCodeSnippetsForFile(String filePath, List<CodeClusterResultDTO.CodeLineInfo> fileLines, 
                                                             String concept, String version) {
        List<CodeSearchResultDTO> snippets = new ArrayList<>();
        
        // 按行号排序并去重相邻行
        List<CodeClusterResultDTO.CodeLineInfo> processedLines = preprocessFileLines(fileLines);
        
        // 限制每个文件最多处理5个代码片段
        int processLimit = Math.min(processedLines.size(), 5);
        
        for (int i = 0; i < processLimit; i++) {
            CodeClusterResultDTO.CodeLineInfo codeLineInfo = processedLines.get(i);
            
            try {
                // 使用现有的KernelCodeAnalyzer获取完整代码片段
                CodeSearchResultDTO completeCodeSnippet = KernelCodeAnalyzer.findCodeElementByLineNumber(
                    codeLineInfo.getFilePath(),
                    concept,
                    codeLineInfo.getLineNumber(),
                    kernelSourcePath,
                    version
                );
                
                if (completeCodeSnippet != null) {
                    snippets.add(completeCodeSnippet);
                    logger.debug("成功获取完整代码片段: {}:{}", 
                        codeLineInfo.getFilePath(), codeLineInfo.getLineNumber());
                } else {
                    // 如果获取完整片段失败，创建一个基于单行的简化版本
                    CodeSearchResultDTO fallbackSnippet = createFallbackCodeSnippet(
                        codeLineInfo, concept, version);
                    if (fallbackSnippet != null) {
                        snippets.add(fallbackSnippet);
                    }
                }
                
            } catch (Exception e) {
                logger.warn("获取代码片段失败: {}:{}, error={}", 
                    codeLineInfo.getFilePath(), codeLineInfo.getLineNumber(), e.getMessage());
                
                // 创建后备片段
                CodeSearchResultDTO fallbackSnippet = createFallbackCodeSnippet(codeLineInfo, concept, version);
                if (fallbackSnippet != null) {
                    snippets.add(fallbackSnippet);
                }
            }
        }
        
        return snippets;
    }
    
    /**
     * 预处理文件中的代码行：排序、去重相邻行
     * 
     * @param fileLines 原始代码行
     * @return 处理后的代码行
     */
    private List<CodeClusterResultDTO.CodeLineInfo> preprocessFileLines(List<CodeClusterResultDTO.CodeLineInfo> fileLines) {
        // 按行号排序
        List<CodeClusterResultDTO.CodeLineInfo> sorted = fileLines.stream()
                .sorted(Comparator.comparingInt(CodeClusterResultDTO.CodeLineInfo::getLineNumber))
                .collect(Collectors.toList());
        
        // 去重相邻行（如果行号相差小于5，认为是相邻的）
        List<CodeClusterResultDTO.CodeLineInfo> deduplicated = new ArrayList<>();
        int lastLineNumber = -10;
        
        for (CodeClusterResultDTO.CodeLineInfo line : sorted) {
            if (line.getLineNumber() - lastLineNumber >= 5) {
                deduplicated.add(line);
                lastLineNumber = line.getLineNumber();
            }
        }
        
        return deduplicated;
    }
    
    /**
     * 为文件创建后备代码片段列表
     * 
     * @param filePath 文件路径
     * @param fileLines 文件中的代码行
     * @param concept 概念名称
     * @param version Git版本
     * @return 后备代码片段列表
     */
    private List<CodeSearchResultDTO> createFallbackSnippetsForFile(String filePath, List<CodeClusterResultDTO.CodeLineInfo> fileLines, 
                                                                    String concept, String version) {
        List<CodeSearchResultDTO> fallbackSnippets = new ArrayList<>();
        
        // 为每行创建简化的后备片段
        for (CodeClusterResultDTO.CodeLineInfo codeLineInfo : fileLines) {
            CodeSearchResultDTO fallbackSnippet = createFallbackCodeSnippet(codeLineInfo, concept, version);
            if (fallbackSnippet != null) {
                fallbackSnippets.add(fallbackSnippet);
            }
        }
        
        return fallbackSnippets;
    }
    
    /**
     * 创建后备代码片段（当无法获取完整片段时）
     */
    private CodeSearchResultDTO createFallbackCodeSnippet(CodeClusterResultDTO.CodeLineInfo codeLineInfo, 
                                                          String concept, String version) {
        String explanation = String.format(
            "无法获取完整代码片段，显示单行代码。文件: %s, 行号: %d",
            codeLineInfo.getFilePath(), codeLineInfo.getLineNumber()
        );
        
        return new CodeSearchResultDTO(
            codeLineInfo.getFilePath(),
            concept,
            codeLineInfo.getCodeContent(),
            codeLineInfo.getLineNumber(),
            codeLineInfo.getLineNumber(),
            codeLineInfo.getLineNumber(),
            explanation,
            version,
            "single_line"
        );
    }
    
    /**
     * 去重并排序代码片段
     * 
     * @param codeSnippets 原始代码片段列表
     * @return 去重并排序后的代码片段列表
     */
    private List<CodeSearchResultDTO> deduplicateAndSortCodeSnippets(List<CodeSearchResultDTO> codeSnippets) {
        List<CodeSearchResultDTO> deduplicated = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        
        for (CodeSearchResultDTO snippet : codeSnippets) {
            // 使用文件路径和行号范围作为去重key
            String key = String.format("%s:%d-%d", 
                snippet.getFilePath(), snippet.getStartLine(), snippet.getEndLine());
            
            if (!seen.contains(key)) {
                seen.add(key);
                deduplicated.add(snippet);
            }
        }
        
        // 按文件路径和起始行号排序
        return deduplicated.stream()
                .sorted(Comparator.comparing(CodeSearchResultDTO::getFilePath)
                        .thenComparingInt(CodeSearchResultDTO::getStartLine))
                .collect(Collectors.toList());
    }
    
    /**
     * 使用AI同时生成聚类解释和汇总说明
     * 
     * @param cluster 概念聚类
     * @param originalConcept 原始搜索概念
     * @param codeSnippets 完整代码片段列表
     */
    private void generateAIAnalysis(CodeClusterResultDTO.ConceptCluster cluster, 
                                   String originalConcept, List<CodeSearchResultDTO> codeSnippets) {
        try {
            if (codeSnippets.isEmpty()) {
                cluster.setClusterSummary(generateFallbackSummary(cluster, originalConcept));
                return;
            }
            
            // 构建合并的AI提示词
            String prompt = buildCombinedAnalysisPrompt(cluster, originalConcept, codeSnippets);
            
            // 调用AI生成分析
            String aiResponse = AIService.deepseek(prompt);
            
            if (aiResponse != null && !aiResponse.trim().isEmpty()) {
                logger.debug("AI分析响应内容: {}", aiResponse);
                // 解析AI响应，提取解释和汇总
                parseAIAnalysisResponse(cluster, aiResponse, originalConcept, codeSnippets);
                logger.info("成功解析AI分析，代码片段数: {}, 聚类概念: {}", 
                    codeSnippets.size(), cluster.getClusterConcept());
            } else {
                logger.warn("AI分析返回空响应，使用后备方案");
                // 使用后备方案
                cluster.setClusterSummary(generateFallbackSummary(cluster, originalConcept));
            }
            
        } catch (Exception e) {
            logger.warn("AI分析生成失败: cluster={}, error={}", cluster.getClusterConcept(), e.getMessage());
            // 设置后备内容
            cluster.setClusterSummary(generateFallbackSummary(cluster, originalConcept));
        }
    }
    
    /**
     * 构建合并的AI分析提示词，为每个代码片段生成单独解释和整体汇总
     */
    private String buildCombinedAnalysisPrompt(CodeClusterResultDTO.ConceptCluster cluster, 
                                              String originalConcept, List<CodeSearchResultDTO> codeSnippets) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请分析以下Linux内核代码片段，这些代码都与概念'").append(cluster.getClusterConcept())
              .append("'相关（在'").append(originalConcept).append("'的搜索上下文中）。\n\n");
        
        // 添加聚类基本信息
        prompt.append("聚类信息:\n");
        prompt.append("- 聚类概念: ").append(cluster.getClusterConcept()).append("\n");
        prompt.append("- 原始搜索概念: ").append(originalConcept).append("\n");
        prompt.append("- 代码行数: ").append(cluster.getCodeLines().size()).append("\n");
        prompt.append("- 概念频次: ").append(cluster.getConceptFrequency()).append("\n\n");
        
        // 添加代码片段信息
        int snippetCount = Math.min(codeSnippets.size(), 3); // 限制最多3个片段
        for (int i = 0; i < snippetCount; i++) {
            CodeSearchResultDTO snippet = codeSnippets.get(i);
            prompt.append("代码片段 ").append(i + 1).append(":\n");
            prompt.append("文件: ").append(snippet.getFilePath()).append("\n");
            prompt.append("行号: ").append(snippet.getStartLine()).append("-").append(snippet.getEndLine()).append("\n");
            prompt.append("类型: ").append(snippet.getType()).append("\n");
            prompt.append("代码内容:\n```c\n").append(snippet.getCodeSnippet()).append("\n```\n\n");
        }
        
        prompt.append("请严格按照以下JSON格式提供分析结果：\n\n");
        
        // JSON格式要求
        prompt.append("{\n");
        prompt.append("  \"optimizedConceptName\": \"<将'").append(cluster.getClusterConcept())
              .append("'替换为更容易理解的中文概念名称（3-8个字）>\",\n");
        prompt.append("  \"snippetExplanations\": [\n");
        for (int i = 0; i < snippetCount; i++) {
            prompt.append("    \"<针对代码片段").append(i + 1)
                  .append("的50-80字专业解释，说明其具体功能、与概念'").append(cluster.getClusterConcept())
                  .append("'的关系及在内核中的作用>\"");
            if (i < snippetCount - 1) {
                prompt.append(",");
            }
            prompt.append("\n");
        }
        prompt.append("  ],\n");
        prompt.append("  \"overallSummary\": \"<基于所有代码片段的100-150字整体功能说明，描述它们如何共同实现与'")
              .append(cluster.getClusterConcept()).append("'相关的功能>\"\n");
        prompt.append("}\n\n");
        
        prompt.append("要求说明：\n");
        prompt.append("1. optimizedConceptName: 优化概念名称示例 - 'from'→'数据来源', 'sched'→'任务调度', 'lock'→'锁机制'\n");
        prompt.append("2. snippetExplanations: 数组包含").append(snippetCount).append("个解释，对应").append(snippetCount).append("个代码片段\n");
        prompt.append("3. 请返回有效的JSON格式，不要使用markdown代码块包装\n");
        prompt.append("4. 用中文回答，语言简洁专业");
        
        return prompt.toString();
    }
    
    /**
     * 解析AI分析响应，提取优化的聚类概念名称、每个代码片段的解释和整体汇总
     */
    private void parseAIAnalysisResponse(CodeClusterResultDTO.ConceptCluster cluster, String aiResponse, 
                                        String originalConcept, List<CodeSearchResultDTO> codeSnippets) {
        try {
            // 清理AI响应中的markdown格式
            String cleanedResponse = cleanJsonResponse(aiResponse);
            logger.debug("清理后的AI响应: {}", cleanedResponse);
            
            // 解析JSON响应
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> responseMap = mapper.readValue(cleanedResponse, java.util.Map.class);
            
            // 1. 解析优化后的聚类概念名称（但保护默认聚类组）
            String originalConceptName = cluster.getClusterConcept();
            String optimizedConceptName = (String) responseMap.get("optimizedConceptName");
            
            // 如果是默认聚类组"核心定义"，不允许AI修改名称
            if (!"核心定义".equals(originalConceptName) && 
                optimizedConceptName != null && !optimizedConceptName.trim().isEmpty()) {
                cluster.setClusterConcept(optimizedConceptName.trim());
                logger.debug("成功解析优化聚类概念: {} -> {}", 
                    originalConceptName, optimizedConceptName.trim());
            } else {
                logger.debug("保护默认聚类组名称或优化概念为空，跳过概念优化: {}", originalConceptName);
            }
            
            // 2. 解析每个代码片段的解释并设置到对应的codeSnippets对象
            @SuppressWarnings("unchecked")
            java.util.List<String> snippetExplanations = (java.util.List<String>) responseMap.get("snippetExplanations");
            
            if (snippetExplanations != null) {
                // 将解释分别设置到对应的代码片段对象中
                int actualCount = Math.min(snippetExplanations.size(), codeSnippets.size());
                for (int i = 0; i < actualCount; i++) {
                    if (i < codeSnippets.size() && i < snippetExplanations.size()) {
                        String explanation = snippetExplanations.get(i);
                        if (explanation != null && !explanation.trim().isEmpty()) {
                            codeSnippets.get(i).setExplanation(explanation.trim());
                        } else {
                            codeSnippets.get(i).setExplanation(generateFallbackSnippetExplanation(i + 1));
                        }
                    }
                }
                logger.debug("成功解析{}个代码片段解释", actualCount);
                         } else {
                 logger.warn("JSON响应中未找到snippetExplanations字段");
                 // 为代码片段设置后备解释
                 for (int i = 0; i < Math.min(codeSnippets.size(), 3); i++) {
                     codeSnippets.get(i).setExplanation(generateFallbackSnippetExplanation(i + 1));
                 }
             }
            
            // 3. 解析整体汇总说明
            String overallSummary = (String) responseMap.get("overallSummary");
            if (overallSummary != null && !overallSummary.trim().isEmpty()) {
                cluster.setClusterSummary(overallSummary.trim());
                logger.debug("成功解析整体汇总，长度: {}", overallSummary.trim().length());
            } else {
                cluster.setClusterSummary(generateFallbackSummary(cluster, originalConcept));
                logger.debug("整体汇总为空，使用后备汇总");
            }
            
            logger.info("JSON解析成功完成：概念优化={}, 代码片段解释数={}, 整体汇总长度={}", 
                !optimizedConceptName.equals(originalConceptName), 
                snippetExplanations != null ? snippetExplanations.size() : 0, 
                overallSummary != null ? overallSummary.length() : 0);
            
        } catch (Exception e) {
            logger.warn("JSON解析AI分析响应失败: {}", e.getMessage());
            logger.debug("原始AI响应: {}", aiResponse);
            // 使用后备方案
            setFallbackExplanations(cluster, originalConcept, codeSnippets);
        }
    }
    

    

    

    
    /**
     * 生成后备的代码片段解释
     * @param snippetIndex 片段索引（1-based）
     * @return 后备解释文本
     */
    private String generateFallbackSnippetExplanation(int snippetIndex) {
        return String.format("代码片段%d包含相关的Linux内核实现，具体功能需要进一步分析。", snippetIndex);
    }
    

    
    /**
     * 设置后备解释（当AI解析完全失败时）
     */
    private void setFallbackExplanations(CodeClusterResultDTO.ConceptCluster cluster, 
                                        String originalConcept, List<CodeSearchResultDTO> codeSnippets) {
        // 1. 为聚类概念设置后备优化名称（保持原有名称，不进行优化）
        logger.debug("AI解析失败，保持原始聚类概念名称: {}", cluster.getClusterConcept());
        
        // 2. 为每个代码片段设置后备解释到其explanation字段
        int snippetCount = Math.min(codeSnippets.size(), 3);
        
        for (int i = 0; i < snippetCount; i++) {
            if (i < codeSnippets.size()) {
                CodeSearchResultDTO snippet = codeSnippets.get(i);
                String fallbackExplanation = String.format(
                    "该代码片段位于 %s 第%d-%d行，包含%s相关的实现。",
                    snippet.getFilePath(), snippet.getStartLine(), snippet.getEndLine(), snippet.getType()
                );
                snippet.setExplanation(fallbackExplanation);
            }
        }
        
        // 3. 设置后备的聚类总结
        cluster.setClusterSummary(generateFallbackSummary(cluster, originalConcept));
    }

    
    /**
     * 生成后备汇总说明
     */
    private String generateFallbackSummary(CodeClusterResultDTO.ConceptCluster cluster, String originalConcept) {
        return String.format("与'%s'相关的'%s'功能实现，包含%d行代码", 
            originalConcept, cluster.getClusterConcept(), cluster.getCodeLines().size());
    }
    
    /**
     * 生成分析摘要
     */
    private String generateClusterAnalysisSummary(String concept, int totalLines, int clusterCount) {
        return String.format(
            "基于概念'%s'的代码聚类分析完成。共分析了%d行代码，识别出%d个概念聚类。" +
            "通过词袋模型分析，提取了代码中的关键标识符并按照概念相似性进行了分组，" +
            "并为每个聚类获取了完整代码片段和AI解释。",
            concept, totalLines, clusterCount
        );
    }

} 