package com.cs.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cs.api.dto.EntityExtractionDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 实体提取数据访问层
 * 
 * @author YK
 * @since 1.0.0
 */
@Mapper
public interface EntityExtractionMapper extends BaseMapper<EntityExtractionDTO> {
    
    /**
     * 根据英文名称和特征ID查询实体
     * 
     * @param nameEn 英文名称
     * @param featureId 特征ID
     * @return 实体信息
     */
    @Select("SELECT * FROM entities_extraction WHERE name_en = #{nameEn} AND feature_id = #{featureId}")
    EntityExtractionDTO findByNameEnAndFeatureId(@Param("nameEn") String nameEn, @Param("featureId") Integer featureId);
    
    /**
     * 根据特征ID查询所有实体
     * 
     * @param featureId 特征ID
     * @return 实体列表
     */
    @Select("SELECT * FROM entities_extraction WHERE feature_id = #{featureId}")
    List<EntityExtractionDTO> findByFeatureId(@Param("featureId") Integer featureId);
    
    /**
     * 根据英文名称模糊查询实体
     * 
     * @param nameEn 英文名称
     * @return 实体列表
     */
    @Select("SELECT * FROM entities_extraction WHERE name_en = #{nameEn}")
    List<EntityExtractionDTO> findByNameEn(@Param("nameEn") String nameEn);
    
    /**
     * 统计总的实体数量
     * 
     * @return 实体总数
     */
    @Select("SELECT COUNT(*) FROM entities_extraction")
    Long countTotal();
    
    /**
     * 根据特征ID统计实体数量
     * 
     * @param featureId 特征ID
     * @return 指定特征的实体数量
     */
    @Select("SELECT COUNT(*) FROM entities_extraction WHERE feature_id = #{featureId}")
    Long countByFeatureId(@Param("featureId") Integer featureId);
    
    /**
     * 根据概念名称模糊查询实体（支持英文和中文名称）
     * 用于概念验证功能
     * 
     * @param concept 概念名称
     * @return 匹配的实体列表
     */
    @Select("SELECT * FROM entities_extraction WHERE name_en LIKE CONCAT('%', #{concept}, '%') OR name_cn LIKE CONCAT('%', #{concept}, '%') OR #{concept} LIKE CONCAT('%', name_en, '%') OR #{concept} LIKE CONCAT('%', name_cn, '%')")
    List<EntityExtractionDTO> findByConcept(@Param("concept") String concept);
    
    /**
     * 精确查询概念（英文或中文名称完全匹配）
     * 用于概念验证功能
     * 
     * @param concept 概念名称
     * @return 匹配的实体列表
     */
    @Select("SELECT * FROM entities_extraction WHERE name_en = #{concept} OR name_cn = #{concept}")
    List<EntityExtractionDTO> findByExactConcept(@Param("concept") String concept);
    
    /**
     * 批量插入实体数据（避免重复）
     * 使用 ON DUPLICATE KEY UPDATE 处理重复数据
     */
    @Insert({
        "<script>",
        "INSERT INTO entities_extraction (name_en, name_cn, source, definition_en, definition_cn, aliases, rel_desc, wikidata_id, feature_id)",
        "VALUES",
        "<foreach collection='entities' item='entity' separator=','>",
        "(#{entity.nameEn}, #{entity.nameCn}, #{entity.source}, #{entity.definitionEn}, #{entity.definitionCn}, #{entity.aliases}, #{entity.relDesc}, #{entity.wikidataId}, #{entity.featureId})",
        "</foreach>",
        "ON DUPLICATE KEY UPDATE",
        "name_cn = VALUES(name_cn),",
        "source = VALUES(source),",
        "definition_en = VALUES(definition_en),",
        "definition_cn = VALUES(definition_cn),",
        "aliases = VALUES(aliases),",
        "rel_desc = VALUES(rel_desc),",
        "wikidata_id = VALUES(wikidata_id),",
        "update_time = CURRENT_TIMESTAMP",
        "</script>"
    })
    int batchInsertEntities(@Param("entities") List<EntityExtractionDTO> entities);
} 