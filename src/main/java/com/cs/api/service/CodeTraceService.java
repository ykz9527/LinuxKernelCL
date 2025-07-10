package com.cs.api.service;

import com.cs.api.dto.CodeTraceResponseDTO;
import java.util.List;


/**
 * 代码追溯服务接口
 * 提供函数和结构体的演化历史追溯功能
 * 
 * @author YK
 * @since 1.0.0
 */
public interface CodeTraceService {
    
    /**
     * 追溯指定方法/函数的演化历史
     * 
     * @param filePath 文件路径
     * @param methodName 方法/函数名称
     * @param version 代码版本
     * @param targetCommit 当前的commitId，用于追溯到上一次提交涉及的commitId
     * @return 代码追溯响应结果
     */
    List<CodeTraceResponseDTO> traceMethodHistory(String filePath, String methodName, String version, String targetCommit);

}