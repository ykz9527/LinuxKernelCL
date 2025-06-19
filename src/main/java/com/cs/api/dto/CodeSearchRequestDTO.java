package com.cs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 代码搜索请求DTO
 * 
 * @author YK
 * @since 1.0.0
 */
@Schema(description = "代码搜索请求参数")
public class CodeSearchRequestDTO {

    @Schema(description = "搜索概念/功能描述", example = "选择下一个要运行的任务", required = true)
    @NotBlank(message = "搜索概念不能为空")
    private String concept;

    @Schema(description = "搜索上下文/场景", example = "Linux 内核/进程调度场景")
    private String context;

    @Schema(description = "代码版本标签",defaultValue = "v6.14")
    private String version;

    public CodeSearchRequestDTO() {
    }

    public CodeSearchRequestDTO(String concept, String context, String version) {
        this.concept = concept;
        this.context = context;
        this.version = version;
    }

    public String getConcept() {
        return concept;
    }

    public void setConcept(String concept) {
        this.concept = concept;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "CodeSearchRequestDTO{" +
                "concept='" + concept + '\'' +
                ", context='" + context + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
} 