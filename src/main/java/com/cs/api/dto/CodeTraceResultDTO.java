package com.cs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 代码追溯结果 DTO
 * 包含函数或结构体的演化历史信息
 * 
 * @author YK
 * @since 1.0.0
 */
@Schema(description = "代码追溯结果")
public class CodeTraceResultDTO {

    @Schema(description = "记录ID", example = "283991")
    private Long id;

    @Schema(description = "提交ID", example = "c36e2024957120566efd99395b5c8cc95b5175c1")
    private String commitId;

    @Schema(description = "作者姓名", example = "ruansy.fnst@fujitsu.com")
    private String authorName;

    @Schema(description = "提交者姓名", example = "akpm@linux-foundation.org")
    private String committerName;

    @Schema(description = "作者提交时间", example = "2022-06-03 13:37:29")
    private String authorTime;

    @Schema(description = "提交时间", example = "2022-07-18 08:14:30")
    private String commitTime;

    @Schema(description = "提交标题", example = "mm: introduce mf_dax_kill_procs() for fsdax case")
    private String commitTitle;

    @Schema(description = "新增行数", example = "88")
    private Integer added;

    @Schema(description = "删除行数", example = "10")
    private Integer deleted;

    @Schema(description = "所属公司", example = "fujitsu.com")
    private String company;

    @Schema(description = "版本", example = "v5.19-rc4")
    private String version;

    @Schema(description = "代码库", example = "linux-stable")
    private String repo;

    @Schema(description = "H1级别描述")
    private String h1;

    @Schema(description = "H2级别描述")
    private String h2;

    @Schema(description = "描述文本")
    private String text;

    @Schema(description = "新手版本")
    private String newbiesVersion;

    @Schema(description = "特性ID")
    private String featureId;

    // 默认构造函数
    public CodeTraceResultDTO() {}

    // 全参构造函数
    public CodeTraceResultDTO(Long id, String commitId, String authorName, String committerName,
                             String authorTime, String commitTime, String commitTitle,
                             Integer added, Integer deleted, String company, String version,
                             String repo, String h1, String h2, String text,
                             String newbiesVersion, String featureId) {
        this.id = id;
        this.commitId = commitId;
        this.authorName = authorName;
        this.committerName = committerName;
        this.authorTime = authorTime;
        this.commitTime = commitTime;
        this.commitTitle = commitTitle;
        this.added = added;
        this.deleted = deleted;
        this.company = company;
        this.version = version;
        this.repo = repo;
        this.h1 = h1;
        this.h2 = h2;
        this.text = text;
        this.newbiesVersion = newbiesVersion;
        this.featureId = featureId;
    }

    // Getter 和 Setter 方法
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

    @Override
    public String toString() {
        return "CodeTraceResultDTO{" +
                "id=" + id +
                ", commitId='" + commitId + '\'' +
                ", authorName='" + authorName + '\'' +
                ", committerName='" + committerName + '\'' +
                ", authorTime='" + authorTime + '\'' +
                ", commitTime='" + commitTime + '\'' +
                ", commitTitle='" + commitTitle + '\'' +
                ", added=" + added +
                ", deleted=" + deleted +
                ", company='" + company + '\'' +
                ", version='" + version + '\'' +
                ", repo='" + repo + '\'' +
                ", h1='" + h1 + '\'' +
                ", h2='" + h2 + '\'' +
                ", text='" + text + '\'' +
                ", newbiesVersion='" + newbiesVersion + '\'' +
                ", featureId='" + featureId + '\'' +
                '}';
    }
}