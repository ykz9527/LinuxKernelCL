package com.cs.api.service;

import java.util.List;

import com.cs.api.dto.CodeSearchResultDTO;
import com.cs.api.dto.ConceptExplanationResultDTO;
import com.cs.api.dto.TripleSearchResultDTO;

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

} 