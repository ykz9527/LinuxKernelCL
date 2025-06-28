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
import com.cs.api.dto.ConceptExplanationResultDTO;
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
import java.util.List;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

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
        // 提取搜索实体 - 基于概念和上下文
        List<String> searchEntities = extractSearchEntities(concept, context);
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
        
            // // 根据概念直接匹配
            // for (Map.Entry<String, String[]> entry : conceptToEntities.entrySet()) {
            //     if (concept.contains(entry.getKey())) {
            //         for (String entity : entry.getValue()) {
            //             entities.add(entity);
            //         }
            //         break;
            //     }
            // }
            
        // }
        
        // 3. 去重并限制数量（避免过多搜索实体影响性能）
        return entities.stream()
                .distinct()
                .limit(5)  // 限制最多5个搜索实体
                .toList();
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
    

    


} 