package com.cs.api.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 实体提取数据传输对象
 * 对应数据库表 entities_extraction
 * 
 * @author YK
 * @since 1.0.0
 */
@TableName("entities_extraction")
public class EntityExtractionDTO {
    
    /**
     * 唯一id，整型递增
     */
    @TableId(value = "eid", type = IdType.AUTO)
    private Long eid;
    
    /**
     * 英文名称
     */
    private String nameEn;
    
    /**
     * 中文名称
     */
    private String nameCn;
    
    /**
     * 来源
     */
    private String source;
    
    /**
     * 对概念的定义（英文）
     */
    private String definitionEn;
    
    /**
     * 对概念的定义（中文）
     */
    private String definitionCn;
    
    /**
     * 别名 json对象字符串
     * 格式类似于 [{"name_en":"xxx", "name_cn":"xxx", "source":""}, {"name_en":"yyy", "name_cn":"xxx"}]
     */
    private String aliases;
    
    /**
     * related descriptions，相关描述，json对象字符串
     * [{"desc_en":"", "desc_cn":"", "source":""}]
     */
    private String relDesc;
    
    /**
     * wikidata的id
     */
    private String wikidataId;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    
    /**
     * 特征ID
     */
    private Integer featureId;
    
    // 构造函数
    public EntityExtractionDTO() {}
    
    public EntityExtractionDTO(String nameEn, Integer featureId) {
        this.nameEn = nameEn;
        this.featureId = featureId;
    }
    
    // Getter 和 Setter 方法
    public Long getEid() {
        return eid;
    }
    
    public void setEid(Long eid) {
        this.eid = eid;
    }
    
    public String getNameEn() {
        return nameEn;
    }
    
    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }
    
    public String getNameCn() {
        return nameCn;
    }
    
    public void setNameCn(String nameCn) {
        this.nameCn = nameCn;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public String getDefinitionEn() {
        return definitionEn;
    }
    
    public void setDefinitionEn(String definitionEn) {
        this.definitionEn = definitionEn;
    }
    
    public String getDefinitionCn() {
        return definitionCn;
    }
    
    public void setDefinitionCn(String definitionCn) {
        this.definitionCn = definitionCn;
    }
    
    public String getAliases() {
        return aliases;
    }
    
    public void setAliases(String aliases) {
        this.aliases = aliases;
    }
    
    public String getRelDesc() {
        return relDesc;
    }
    
    public void setRelDesc(String relDesc) {
        this.relDesc = relDesc;
    }
    
    public String getWikidataId() {
        return wikidataId;
    }
    
    public void setWikidataId(String wikidataId) {
        this.wikidataId = wikidataId;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
    
    public Integer getFeatureId() {
        return featureId;
    }
    
    public void setFeatureId(Integer featureId) {
        this.featureId = featureId;
    }
    
    @Override
    public String toString() {
        return "EntityExtractionDTO{" +
                "eid=" + eid +
                ", nameEn='" + nameEn + '\'' +
                ", nameCn='" + nameCn + '\'' +
                ", source='" + source + '\'' +
                ", definitionEn='" + definitionEn + '\'' +
                ", definitionCn='" + definitionCn + '\'' +
                ", aliases='" + aliases + '\'' +
                ", relDesc='" + relDesc + '\'' +
                ", wikidataId='" + wikidataId + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", featureId=" + featureId +
                '}';
    }
} 