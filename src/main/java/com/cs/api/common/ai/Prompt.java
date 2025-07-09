package com.cs.api.common.ai;

import java.util.List;

/**
 * AI提示词生成工具类
 * 包含各种场景下的prompt字符串生成方法
 */
public class Prompt {
    
    /**
     * 生成系统角色提示词
     * @param role 角色描述，如"助手"、"专家"等
     * @param expertise 专业领域，如"编程"、"翻译"等
     * @return 完整的系统提示词
     */
    public static String systemRole(String role, String expertise) {
        return String.format("你是一个专业的%s，擅长%s领域。请用专业、友好的语气回答用户的问题，" +
                "确保回答准确、有用且易于理解。", role, expertise);
    }
    
    /**
     * 生成用户对话提示词
     * @param userName 用户名称
     * @param question 用户的问题
     * @return 格式化的对话提示词
     */
    public static String userQuestion(String userName, String question) {
        return String.format("用户 %s 提问：%s\n\n请根据问题内容给出详细的回答。", userName, question);
    }
    
    /**
     * 生成代码分析提示词
     * @param language 编程语言
     * @param codeSnippet 代码片段
     * @return 代码分析提示词
     */
    public static String codeAnalysis(String language, String codeSnippet) {
        return String.format("请分析以下%s代码：\n\n```%s\n%s\n```\n\n" +
                "请说明代码功能、可能的问题和改进建议。", language, language, codeSnippet);
    }
    
    /**
     * 生成翻译提示词
     * @param sourceText 原文
     * @param sourceLang 源语言
     * @param targetLang 目标语言
     * @return 翻译提示词
     */
    public static String translation(String sourceText, String sourceLang, String targetLang) {
        return String.format("请将以下%s文本翻译成%s：\n\n%s\n\n" +
                "要求：保持原意，语言自然流畅。", sourceLang, targetLang, sourceText);
    }
    
    /**
     * 生成文档生成提示词
     * @param functionName 函数名
     * @param parameters 参数列表
     * @param functionality 功能描述
     * @return 文档生成提示词
     */
    public static String generateDoc(String functionName, String parameters, String functionality) {
        return String.format("请为函数 %s 生成详细的文档注释。\n" +
                "参数：%s\n功能：%s\n\n" +
                "要求：包含参数说明、返回值说明、使用示例。", functionName, parameters, functionality);
    }
    
    /**
     * 生成错误处理提示词
     * @param errorMessage 错误信息
     * @param context 错误上下文
     * @return 错误处理提示词
     */
    public static String errorAnalysis(String errorMessage, String context) {
        return String.format("遇到了以下错误：\n错误信息：%s\n发生环境：%s\n\n" +
                "请分析错误原因并提供解决方案。", errorMessage, context);
    }
    
    /**
     * 生成学习指导提示词
     * @param topic 学习主题
     * @param level 学习水平（初级/中级/高级）
     * @return 学习指导提示词
     */
    public static String learningGuide(String topic, String level) {
        return String.format("我想学习%s，目前水平是%s。\n\n" +
                "请为我制定一个循序渐进的学习计划，包含：\n" +
                "1. 学习路径\n2. 重点知识点\n3. 实践建议\n4. 学习资源", topic, level);
    }
    
    /**
     * 生成总结提示词
     * @param content 需要总结的内容
     * @param maxLength 总结最大长度
     * @return 总结提示词
     */
    public static String summarize(String content, int maxLength) {
        return String.format("请对以下内容进行总结，总结长度不超过%d字：\n\n%s\n\n" +
                "要求：提取核心要点，保持逻辑清晰。", maxLength, content);
    }
    
    /**
     * 生成概念变体提示词
     * @param concept 核心概念
     * @param context 概念的上下文描述
     * @return 用于生成概念变体的英文提示词
     */
    public static String generateConceptVariants(String concept, String context) {
        return String.format(
            "You are a technical terminology expert specializing in Linux kernel concepts. " +
            "Your task is to generate accurate semantic variants for a given concept.\n\n" +
            "Concept: %s\n" +
            "Context: %s\n\n" +
            "Generate 0-5 precise variants of this concept that express the same or closely related meaning. " +
            "Use the context to understand the specific meaning and scope of the concept.\n\n" +
            "Include these types of variants:\n" +
            "- Synonyms and alternative terms\n" +
            "- Different technical expressions for the same concept\n" +
            "- Commonly used abbreviations or acronyms\n" +
            "- Related terminology that refers to the same underlying concept\n\n" +
            "Requirements:\n" +
            "- Each variant must be semantically accurate and relevant\n" +
            "- Avoid generic or overly broad terms\n" +
            "- Avoid completely unrelated concepts\n" +
            "- Focus on terms that would be used in technical documentation or code\n" +
            "- Prioritize commonly used variants over obscure ones\n\n" +
            "IMPORTANT: Accuracy is critical. Only generate variants you are confident about. " +
            "It's better to generate fewer accurate variants than many inaccurate ones.\n\n" +
            "Output format: Return ONLY a comma-separated list of variants.\n" +
            "Example for concept 'memory allocation': malloc,memory_alloc,alloc_mem,kmalloc,allocation\n\n" +
            "Variants:",
            concept, context != null ? context : "Linux kernel context"
        );
    }

    /**
     * 生成概念翻译和标准化提示词（专门用于概念验证）
     * @param concept 输入的概念（可能是中文或非标准英文）
     * @param context 概念的上下文描述
     * @return 用于将概念转换为Linux内核标准英文术语的提示词
     */
    public static String translateConceptToKernelTerms(String concept, String context) {
        return String.format(
            "You are a Linux kernel technical terminology expert. " +
            "Your task is to translate and standardize concepts into precise Linux kernel English terminology.\n\n" +
            "Input Concept: %s\n" +
            "Context: %s\n\n" +
            "Please provide:\n" +
            "1. The most accurate Linux kernel English term for this concept\n" +
            "2. Alternative standard terms commonly used in kernel documentation\n" +
            "3. Brief explanation of why this translation is accurate\n\n" +
            "Translation Guidelines:\n" +
            "- If input is Chinese, translate to the precise Linux kernel English equivalent\n" +
            "- If input is already English, standardize to official kernel terminology\n" +
            "- Use terms found in actual Linux kernel source code, documentation, or man pages\n" +
            "- Prefer widely-used standard terms over obscure variations\n" +
            "- Consider the context to determine the most specific appropriate term\n\n" +
            "Examples:\n" +
            "- '进程调度' → 'process scheduling' or 'task scheduling'\n" +
            "- '内存管理' → 'memory management'\n" +
            "- '文件系统' → 'file system' or 'filesystem'\n" +
            "- '中断处理' → 'interrupt handling'\n\n" +
            "Output Format (JSON):\n" +
            "{\n" +
            "  \"primaryTerm\": \"<most accurate kernel term>\",\n" +
            "  \"alternativeTerms\": [\"<alt1>\", \"<alt2>\"],\n" +
            "  \"explanation\": \"<brief explanation>\"\n" +
            "}\n\n" +
            "Translation:",
            concept, context != null ? context : "Linux kernel context"
        );
    }

    /**
     * 生成概念三元组提取提示词
     * @param concept 核心概念（在context中被提到）
     * @param context 包含该概念的feature描述句子
     * @return 用于从feature描述句子中提取概念相关三元组的英文提示词
     */
    public static String extractTriplesFromFeatures(String concept, String context) {
        return String.format("""
            You are a technical knowledge extraction expert specializing in Linux kernel feature analysis.
            Your task is to extract precise triplets from feature description sentences that mention a specific concept.

            Target Concept: %s
            Feature Description: %s

            From the given feature description sentence(s) that mention the concept '%s', extract relevant triplets (head, relation, tail).
            
            CRITICAL REQUIREMENT: Every triplet MUST have the EXACT target concept '%s' as either the head or the tail.
            The concept must match EXACTLY - not a substring, not a variation, but the precise same text.

            Examples of triplet extraction from feature descriptions:

            Example 1:
            Concept: "memory management"
            Description: "Enhanced memory management system with new allocation algorithms for better performance"
            Extracted triplets:
            (memory management, enhanced_with, new allocation algorithms)
            (memory management, has, allocation algorithms)
            (memory management, improves, performance)

            Example 2:
            Concept: "file system"
            Description: "Improved file system caching mechanism to reduce disk I/O operations and increase throughput"
            Extracted triplets:
            (file system, has, caching mechanism)
            (file system, reduces, disk I/O operations)
            (file system, increases, throughput)

            Example 3:
            Concept: "scheduler"
            Description: "Updated process scheduler with priority-based task queuing for multi-core processors"
            Extracted triplets:
            (scheduler, updated_with, priority-based task queuing)
            (scheduler, uses, task queuing)
            (multi-core processors, supported_by, scheduler)

            Extraction Guidelines:
            1. MANDATORY: Every triplet must contain the EXACT target concept '%s' as head or tail
            2. Do NOT use variations like "memory management system" if concept is "memory management"
            3. Do NOT use substrings or partial matches
            4. Extract triplets that directly involve the exact concept
            5. Focus on what the feature description explicitly states
            6. Use precise technical terminology from the description
            7. Maintain consistency with the original sentence meaning

            CRITICAL: Only extract relationships that are clearly described in the feature sentences.
            Do not create triplets based on general knowledge or assumptions.

            Output format: Return ONLY triplets in format (head,relation,tail), one per line.
            Each triplet must contain the exact concept '%s' as either head or tail.
            
            Triplets:""",
            concept, context != null ? context : "", concept, concept, concept, concept
        );
    }

    /**
     * 生成概念关系分析提示词
     * @param coreConcept 核心概念
     * @param relatedConcepts 从feature中发现的相关概念列表
     * @param featureDescriptions 相关的feature描述
     * @return 用于分析概念关系的英文提示词
     */
    public static String analyzeConceptRelationships(String coreConcept, List<String> relatedConcepts, List<String> featureDescriptions) {
        String conceptList = String.join(", ", relatedConcepts);
        String featureContext = String.join("\n", featureDescriptions);
        
        return String.format("""
            You are a technical knowledge analyst specializing in Linux kernel concept relationships.
            Your task is to analyze relationships between a core concept and related concepts found in feature descriptions.

            Core Concept: %s
            Related Concepts Found: %s
            
            Feature Context:
            %s

            For each related concept listed above, analyze its relationship with the core concept '%s'.
            Use the feature descriptions as context to understand how these concepts relate to each other.

            For each relationship, provide:
            1. Relationship Type (choose from: contains, implements, uses, extends, configures, supports, manages, communicates_with, depends_on, optimizes, other)
            2. Relationship Strength (0.0-1.0, where 1.0 is strongest possible relationship)
            3. Brief relationship description explaining how they are connected

            Analysis Guidelines:
            - Focus on direct, technical relationships described in the feature contexts
            - Consider functional dependencies, implementation relationships, and system interactions
            - Base strength scores on how closely the concepts work together or depend on each other
            - Be precise and avoid speculation beyond what the feature descriptions indicate
            - If a concept appears unrelated to the core concept, assign low strength (< 0.3)

            Output Format:
            For each related concept, output one line in this exact format:
            [ConceptName]|[RelationshipType]|[Strength]|[Description]

            Example:
            memory_allocator|uses|0.8|The core concept utilizes memory allocation mechanisms for resource management
            file_system|depends_on|0.7|The core concept requires file system operations to function properly

            Relationship Analysis:""",
            coreConcept, conceptList, featureContext, coreConcept
        );
    }

    /**
     * 生成关系总结提示词
     * @param coreConcept 核心概念
     * @param relationshipCount 发现的关系数量
     * @param strongRelationships 强关系列表
     * @return 用于生成关系总结的英文提示词
     */
    public static String generateRelationshipSummary(String coreConcept, int relationshipCount, List<String> strongRelationships) {
        String strongRelList = String.join(", ", strongRelationships);
        
        return String.format("""
            You are a technical documentation specialist. Create a concise summary of concept relationships.

            Core Concept: %s
            Total Relationships Found: %d
            Key Strong Relationships: %s

            Create a brief, professional summary (2-3 sentences) that describes:
            1. The overall role and position of the core concept in the Linux kernel ecosystem
            2. The most important relationships and dependencies
            3. The technical significance of these relationships

            Focus on practical implications and system architecture aspects.
            Use technical language appropriate for kernel developers.用中文回答

            Summary:""",
            coreConcept, relationshipCount, strongRelList
        );
    }
}
