package com.cs.api.service;

import java.util.List;

import com.cs.api.dto.CodeSearchResultDTO;
import com.cs.api.dto.CodeClusterResultDTO;
import com.cs.api.dto.ConceptExplanationResultDTO;
import com.cs.api.dto.ConceptRelationshipResultDTO;
import com.cs.api.dto.ConceptValidationResultDTO;
import com.cs.api.dto.TripleSearchResultDTO;
import com.cs.api.dto.EntityExtractionDTO;

/**
 * Linux内核代码搜索服务接口
 * 
 * @author YK
 * @since 1.0.0
 */
public interface EntityLinkService {
    
    /**
     * 执行代码搜索
     * 
     * @param concept 搜索概念
     * @param context 上下文信息
     * @param version 代码版本
     * @return 搜索结果列表
     */
    List<CodeSearchResultDTO> searchCode(String concept, String context, String version);

    /**
     * 执行基于词袋模型的代码聚类分析
     * 根据概念搜索相关代码，通过行号获取单行代码，使用词袋模型进行聚类分析
     * 
     * @param concept 搜索概念
     * @param context 上下文信息
     * @param version 代码版本
     * @return 代码聚类分析结果
     */
    CodeClusterResultDTO analyzeCodeClusters(String concept, String context, String version);

    /**
     * 获取概念的文本解释
     *
     * @param concept 搜索概念
     * @param context 上下文信息
     * @return 概念解释结果
     */
    ConceptExplanationResultDTO getConceptExplanation(String concept, String context);

    /**
     * 根据概念搜索三元组
     *
     * @param concept 搜索概念
     * @param context 上下文信息
     * @return 三元组搜索结果列表
     */
    List<TripleSearchResultDTO> searchTriples(String concept, String context);

    /**
     * 分析概念关系
     * 通过搜索相关feature和关联概念，分析概念间的关系类型和强度
     *
     * @param concept 核心概念
     * @param context 上下文信息
     * @param analysisDepth 分析深度级别(1-3)
     * @return 概念关系分析结果
     */
    ConceptRelationshipResultDTO analyzeConceptRelationships(String concept, String context, Integer analysisDepth);

    /**
     * 验证概念是否存在于概念列表中
     * 通过查询数据库中的概念表，判断输入的概念是否存在
     * 如果存在多个同名概念，则结合上下文和definition_en字段使用AI判断最佳匹配
     *
     * @param concept 要验证的概念
     * @param context 上下文信息，用于精确匹配
     * @return 概念验证结果
     */
    ConceptValidationResultDTO validateConcept(String concept, String context);

    /**
     * 通过eid查询概念信息
     * 
     * @param eid 概念ID
     * @return 概念实体信息
     */
    EntityExtractionDTO getConceptByEid(Long eid);

} 