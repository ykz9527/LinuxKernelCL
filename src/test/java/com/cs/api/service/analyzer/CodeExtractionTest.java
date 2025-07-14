package com.cs.api.service.analyzer;

import com.cs.api.dto.CodeSearchResultDTO;

/**
 * 代码块提取功能测试示例
 * 演示如何使用KernelCodeAnalyzer的Eclipse CDT功能
 */
public class CodeExtractionTest {
    
    public static void main(String[] args) {
        // 假设的Linux内核源码路径
        String kernelSourcePath = "/home/fdse/ytest/codeMap/linux/repo/";
        
        System.out.println("=== Eclipse CDT 代码分析测试 ===\n");
        
        // 测试1: 通过行号查找代码元素
        testFindByLineNumber(kernelSourcePath);
        
        // 测试2: 通过标识符查找代码元素
        testFindByIdentifier(kernelSourcePath);
    }
    
    /**
     * 测试通过行号查找代码元素
     */
    private static void testFindByLineNumber(String kernelSourcePath) {
        System.out.println("1. 通过行号查找代码元素:");
        
        CodeSearchResultDTO result = KernelCodeAnalyzer.findCodeElementByLineNumber(
            "include/linux/mm_types.h",     // 文件路径
            "folio",                        // 概念名称
            324,                           // 目标行号
            kernelSourcePath,              // 内核源码路径
            "7b230db3b8d373219f88a3d25c8fbbf12cc7f233"                        // 提交ID或版本标识符
        );
        
        if (result != null) {
            System.out.println("✅ 成功找到代码元素:");
            System.out.println("文件: " + result.getFilePath());
            System.out.println("行号: " + result.getStartLine() + "-" + result.getEndLine());
            System.out.println("类型: " + result.getType());
            System.out.println("说明: " + result.getExplanation());
        } else {
            System.out.println("❌ 未能找到代码元素");
        }
        System.out.println();
    }
    
    /**
     * 测试通过标识符查找代码元素
     */
    private static void testFindByIdentifier(String kernelSourcePath) {
        System.out.println("2. 通过标识符查找代码元素:");
        
        CodeSearchResultDTO result = KernelCodeAnalyzer.findCodeElementByIdentifier(
            "include/linux/mm_types.h",     // 文件路径
            "folio",                        // 标识符名称
            "struct",                       // 类型
            kernelSourcePath,               // 内核源码路径
            "v6.14"                         // 提交ID或版本标识符
        );
        
        if (result != null) {
            System.out.println("✅ 成功找到标识符:");
            System.out.println("文件: " + result.getFilePath());
            System.out.println("行号: " + result.getStartLine() + "-" + result.getEndLine());
            System.out.println("类型: " + result.getType());
            System.out.println("说明: " + result.getExplanation());
        } else {
            System.out.println("❌ 未能找到标识符");
        }
        System.out.println();
    }
} 