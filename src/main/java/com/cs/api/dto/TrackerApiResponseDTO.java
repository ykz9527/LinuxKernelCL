package com.cs.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.List;

/**
 * 外部tracker API响应DTO
 * 用于解析trackMethod接口的返回数据
 * 
 * @author YK
 * @since 1.0.0
 */
public class TrackerApiResponseDTO {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("code")
    private String code;

    @JsonProperty("msg")
    private String msg;

    @JsonProperty("data")
    private Map<String, List<TrackerCommitInfoDTO>> data;

    public TrackerApiResponseDTO() {}

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Map<String, List<TrackerCommitInfoDTO>> getData() {
        return data;
    }

    public void setData(Map<String, List<TrackerCommitInfoDTO>> data) {
        this.data = data;
    }

    /**
     * 内部类：tracker API返回的commit信息
     */
    public static class TrackerCommitInfoDTO {
        
        @JsonProperty("id")
        private Long id;
        
        @JsonProperty("commitId")
        private String commitId;
        
        @JsonProperty("authorName")
        private String authorName;
        
        @JsonProperty("committerName")
        private String committerName;
        
        @JsonProperty("authorTime")
        private String authorTime;
        
        @JsonProperty("commitTime")
        private String commitTime;
        
        @JsonProperty("commitTitle")
        private String commitTitle;
        
        @JsonProperty("added")
        private Integer added;
        
        @JsonProperty("deleted")
        private Integer deleted;
        
        @JsonProperty("company")
        private String company;
        
        @JsonProperty("version")
        private String version;
        
        @JsonProperty("repo")
        private String repo;
        
        @JsonProperty("h1")
        private String h1;
        
        @JsonProperty("h2")
        private String h2;
        
        @JsonProperty("text")
        private String text;
        
        @JsonProperty("newbiesVersion")
        private String newbiesVersion;
        
        @JsonProperty("featureId")
        private String featureId;

        // 构造函数
        public TrackerCommitInfoDTO() {}

        // Getter和Setter方法
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getCommitId() {
            return commitId;
        }

        public void setCommitId(String commitId) {
            this.commitId = commitId;
        }

        public String getAuthorName() {
            return authorName;
        }

        public void setAuthorName(String authorName) {
            this.authorName = authorName;
        }

        public String getCommitterName() {
            return committerName;
        }

        public void setCommitterName(String committerName) {
            this.committerName = committerName;
        }

        public String getAuthorTime() {
            return authorTime;
        }

        public void setAuthorTime(String authorTime) {
            this.authorTime = authorTime;
        }

        public String getCommitTime() {
            return commitTime;
        }

        public void setCommitTime(String commitTime) {
            this.commitTime = commitTime;
        }

        public String getCommitTitle() {
            return commitTitle;
        }

        public void setCommitTitle(String commitTitle) {
            this.commitTitle = commitTitle;
        }

        public Integer getAdded() {
            return added;
        }

        public void setAdded(Integer added) {
            this.added = added;
        }

        public Integer getDeleted() {
            return deleted;
        }

        public void setDeleted(Integer deleted) {
            this.deleted = deleted;
        }

        public String getCompany() {
            return company;
        }

        public void setCompany(String company) {
            this.company = company;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getRepo() {
            return repo;
        }

        public void setRepo(String repo) {
            this.repo = repo;
        }

        public String getH1() {
            return h1;
        }

        public void setH1(String h1) {
            this.h1 = h1;
        }

        public String getH2() {
            return h2;
        }

        public void setH2(String h2) {
            this.h2 = h2;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getNewbiesVersion() {
            return newbiesVersion;
        }

        public void setNewbiesVersion(String newbiesVersion) {
            this.newbiesVersion = newbiesVersion;
        }

        public String getFeatureId() {
            return featureId;
        }

        public void setFeatureId(String featureId) {
            this.featureId = featureId;
        }
    }
} 