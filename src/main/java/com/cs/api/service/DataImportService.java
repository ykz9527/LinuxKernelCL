package com.cs.api.service;

import java.util.Map;

/**
 * 数据导入服务接口
 * 
 * @author YK
 * @since 1.0.0
 */
public interface DataImportService {
    
    /**
     * 从JSONL文件导入实体提取数据到数据库
     * 
     * @param jsonlFilePath JSONL文件路径
     * @return 导入结果统计信息
     */
    Map<String, Object> importEntitiesFromJsonl(String jsonlFilePath);
    
    /**
     * 获取导入进度状态
     * 
     * @return 导入进度信息
     */
    Map<String, Object> getImportProgress();
    
    /**
     * 清空实体提取表数据
     * 
     * @return 清理结果
     */
    Map<String, Object> clearEntitiesData();
} 