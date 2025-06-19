package com.cs.api.dto;

import java.util.List;

import java.util.ArrayList;

/**
 * Bootlin搜索结果DTO - 简化版
 * 
 * @author YK
 * @since 1.0.0
 */
public class BootlinSearchResultDTO {
    
    // 基础信息
    private String url;
    private boolean success;
    private String entity;
    private String version;
    
    // 核心数据
    private List<SearchResultItem> definitions;
    private List<SearchResultItem> references;
    private List<SearchResultItem> documentations;
    
    // 简单统计
    private String description;

    public BootlinSearchResultDTO() {
        this.definitions = new ArrayList<>();
        this.references = new ArrayList<>();
        this.documentations = new ArrayList<>();
    }

    public BootlinSearchResultDTO(String url, boolean success, String entity, String version) {
        this();
        this.url = url;
        this.success = success;
        this.entity = entity;
        this.version = version;
    }

    // Getters and Setters
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getEntity() { return entity; }
    public void setEntity(String entity) { this.entity = entity; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public List<SearchResultItem> getDefinitions() { return definitions; }
    public void setDefinitions(List<SearchResultItem> definitions) { this.definitions = definitions; }

    public List<SearchResultItem> getReferences() { return references; }
    public void setReferences(List<SearchResultItem> references) { this.references = references; }

    public List<SearchResultItem> getDocumentations() { return documentations; }
    public void setDocumentations(List<SearchResultItem> documentations) { this.documentations = documentations; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return String.format("BootlinSearchResult{entity='%s', success=%s, definitions=%d, references=%d}", 
            entity, success, definitions.size(), references.size());
    }

    // 统一的搜索结果项数据结构
    public static class SearchResultItem {
        private String path;
        private List<String> line;    // 修改为数组类型，支持多个行号
        private String type;
        private String description;  // 用于文档描述
        
        public SearchResultItem() {}
        
        public SearchResultItem(String path, List<String> line, String type) {
            this.path = path;
            this.line = line;
            this.type = type;
        }
        
        // getters and setters
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public List<String> getLine() { return line; }
        public void setLine(List<String> line) { this.line = line; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}