package com.cs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;

/**
 * 代码聚类分析结果DTO
 * 基于词袋模型的代码概念聚类分析结果
 * 
 * @author YK
 * @since 1.0.0
 */
@Schema(description = "代码聚类分析结果")
public class CodeClusterResultDTO {

    @Schema(description = "核心概念", example = "schedule")
    private String coreConcept;

    @Schema(description = "聚类组列表")
    private List<ConceptCluster> clusters;

    @Schema(description = "总代码行数", example = "25")
    private Integer totalCodeLines;

    @Schema(description = "总聚类数", example = "3")
    private Integer totalClusters;

    @Schema(description = "分析摘要")
    private String analysisSummary;

    @Schema(description = "版本信息", example = "v6.14")
    private String version;

    /**
     * 概念聚类组
     */
    @Schema(description = "概念聚类组")
    public static class ConceptCluster {
        
        @Schema(description = "聚类核心概念", example = "fair_sched")
        private String clusterConcept;

        @Schema(description = "概念频次", example = "8")
        private Integer conceptFrequency;

        @Schema(description = "关联的代码行")
        private List<CodeLineInfo> codeLines;

        @Schema(description = "核心词汇", example = "['fair', 'sched', 'task', 'queue']")
        private List<String> coreTokens;

        @Schema(description = "完整代码片段列表（每个片段的explanation字段包含针对该片段的AI解释）")
        private List<CodeSearchResultDTO> codeSnippets;

        @Schema(description = "聚类特征总结（10-30字的一句话）")
        private String clusterSummary;

        public ConceptCluster() {}

        public ConceptCluster(String clusterConcept, Integer conceptFrequency, 
                             List<CodeLineInfo> codeLines, List<String> coreTokens) {
            this.clusterConcept = clusterConcept;
            this.conceptFrequency = conceptFrequency;
            this.codeLines = codeLines;
            this.coreTokens = coreTokens;
            this.codeSnippets = new ArrayList<>();
            this.clusterSummary = "";
        }

        // Getters and Setters
        public String getClusterConcept() {
            return clusterConcept;
        }

        public void setClusterConcept(String clusterConcept) {
            this.clusterConcept = clusterConcept;
        }

        public Integer getConceptFrequency() {
            return conceptFrequency;
        }

        public void setConceptFrequency(Integer conceptFrequency) {
            this.conceptFrequency = conceptFrequency;
        }

        public List<CodeLineInfo> getCodeLines() {
            return codeLines;
        }

        public void setCodeLines(List<CodeLineInfo> codeLines) {
            this.codeLines = codeLines;
        }

        public List<String> getCoreTokens() {
            return coreTokens;
        }

        public void setCoreTokens(List<String> coreTokens) {
            this.coreTokens = coreTokens;
        }

        public List<CodeSearchResultDTO> getCodeSnippets() {
            return codeSnippets;
        }

        public void setCodeSnippets(List<CodeSearchResultDTO> codeSnippets) {
            this.codeSnippets = codeSnippets;
        }

        public String getClusterSummary() {
            return clusterSummary;
        }

        public void setClusterSummary(String clusterSummary) {
            this.clusterSummary = clusterSummary;
        }
    }

    /**
     * 单行代码信息
     */
    @Schema(description = "单行代码信息")
    public static class CodeLineInfo {
        
        @Schema(description = "文件路径", example = "kernel/sched/fair.c")
        private String filePath;

        @Schema(description = "行号", example = "6415")
        private Integer lineNumber;

        @Schema(description = "代码内容", example = "struct sched_entity *se = &p->se;")
        private String codeContent;

        @Schema(description = "识别出的标识符", example = "['sched_entity', 'se', 'p']")
        private List<String> identifiers;
        
        @Schema(description = "代码行来源类型", example = "definitions")
        private String sourceType;

        public CodeLineInfo() {}

        public CodeLineInfo(String filePath, Integer lineNumber, String codeContent, 
                           List<String> identifiers) {
            this.filePath = filePath;
            this.lineNumber = lineNumber;
            this.codeContent = codeContent;
            this.identifiers = identifiers;
            this.sourceType = "unknown"; // 默认值
        }
        
        public CodeLineInfo(String filePath, Integer lineNumber, String codeContent, 
                           List<String> identifiers, String sourceType) {
            this.filePath = filePath;
            this.lineNumber = lineNumber;
            this.codeContent = codeContent;
            this.identifiers = identifiers;
            this.sourceType = sourceType;
        }

        // Getters and Setters
        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public Integer getLineNumber() {
            return lineNumber;
        }

        public void setLineNumber(Integer lineNumber) {
            this.lineNumber = lineNumber;
        }

        public String getCodeContent() {
            return codeContent;
        }

        public void setCodeContent(String codeContent) {
            this.codeContent = codeContent;
        }

        public List<String> getIdentifiers() {
            return identifiers;
        }

        public void setIdentifiers(List<String> identifiers) {
            this.identifiers = identifiers;
        }
        
        public String getSourceType() {
            return sourceType;
        }

        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
        }

    }

    public CodeClusterResultDTO() {}

    public CodeClusterResultDTO(String coreConcept, List<ConceptCluster> clusters, 
                               Integer totalCodeLines, Integer totalClusters, 
                               String analysisSummary, String version) {
        this.coreConcept = coreConcept;
        this.clusters = clusters;
        this.totalCodeLines = totalCodeLines;
        this.totalClusters = totalClusters;
        this.analysisSummary = analysisSummary;
        this.version = version;
    }

    // Getters and Setters
    public String getCoreConcept() {
        return coreConcept;
    }

    public void setCoreConcept(String coreConcept) {
        this.coreConcept = coreConcept;
    }

    public List<ConceptCluster> getClusters() {
        return clusters;
    }

    public void setClusters(List<ConceptCluster> clusters) {
        this.clusters = clusters;
    }

    public Integer getTotalCodeLines() {
        return totalCodeLines;
    }

    public void setTotalCodeLines(Integer totalCodeLines) {
        this.totalCodeLines = totalCodeLines;
    }

    public Integer getTotalClusters() {
        return totalClusters;
    }

    public void setTotalClusters(Integer totalClusters) {
        this.totalClusters = totalClusters;
    }

    public String getAnalysisSummary() {
        return analysisSummary;
    }

    public void setAnalysisSummary(String analysisSummary) {
        this.analysisSummary = analysisSummary;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "CodeClusterResultDTO{" +
                "coreConcept='" + coreConcept + '\'' +
                ", totalCodeLines=" + totalCodeLines +
                ", totalClusters=" + totalClusters +
                ", analysisSummary='" + analysisSummary + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
} 