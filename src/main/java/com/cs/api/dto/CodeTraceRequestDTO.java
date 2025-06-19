package com.cs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * 代码追溯搜索请求 DTO
 * 
 * @author YK
 * @since 1.0.0
 */
@Schema(description = "代码追溯搜索请求参数")
public class CodeTraceRequestDTO {

    @Schema(description = "文件路径", example = "mm/memory-failure.c")
    @NotEmpty(message = "文件路径不能为空")
    private String filePath;

    @Schema(description = "方法/函数名称", example = "collect_procs_file")
    @NotBlank(message = "方法名称不能为空")
    private String methodName;

    @Schema(description = "代码版本", example = "v5.19-rc4")
    @NotBlank(message = "代码版本不能为空")
    private String version;

    @Schema(description = "当前的commitId，用于追溯到上一次提交涉及的commitId", defaultValue = "",example = "bda807d4445414e8e77da704f116bb0880fe0c76")
    private String targetCommit;

    // 默认构造函数
    public CodeTraceRequestDTO() {}

    // 全参构造函数
    public CodeTraceRequestDTO(String filePath, String methodName, String version, String targetCommit) {
        this.filePath = filePath;
        this.methodName = methodName;
        this.version = version;
        this.targetCommit = targetCommit;
    }

    // Getter 和 Setter 方法
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTargetCommit() {
        return targetCommit;
    }

    public void setTargetCommit(String targetCommit) {
        this.targetCommit = targetCommit;
    }

    @Override
    public String toString() {
        return "CodeTraceRequestDTO{" +
                "filePath=" + filePath +
                ", methodName='" + methodName + '\'' +
                ", version='" + version + '\'' +
                ", targetCommit='" + targetCommit + '\'' +
                '}';
    }
} 