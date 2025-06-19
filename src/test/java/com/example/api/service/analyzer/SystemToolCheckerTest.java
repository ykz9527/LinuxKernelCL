package com.example.api.service.analyzer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cs.api.service.analyzer.SystemToolChecker;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SystemToolChecker单元测试
 */
@ExtendWith(MockitoExtension.class)
class SystemToolCheckerTest {

    @InjectMocks
    private SystemToolChecker systemToolChecker;

    @BeforeEach
    void setUp() {
        // 每次测试前重置状态
    }

    @Test
    void testCheckAllTools() {
        // 执行工具检查
        Map<String, Boolean> result = systemToolChecker.checkAllTools();
        
        // 验证返回结果包含所有必要工具
        assertNotNull(result);
        assertTrue(result.containsKey("ripgrep"));
        assertTrue(result.containsKey("git"));
        assertTrue(result.containsKey("ctags"));
        
        // 打印实际结果用于调试
        System.out.println("工具检查结果: " + result);
    }

    @Test
    void testIsToolAvailable() {
        // 先执行检查
        systemToolChecker.checkAllTools();
        
        // 测试工具可用性查询
        Boolean ripgrepAvailable = systemToolChecker.isToolAvailable("ripgrep");
        Boolean gitAvailable = systemToolChecker.isToolAvailable("git");
        Boolean ctagsAvailable = systemToolChecker.isToolAvailable("ctags");
        
        // 验证返回值不为null
        assertNotNull(ripgrepAvailable);
        assertNotNull(gitAvailable);
        assertNotNull(ctagsAvailable);
        
        System.out.println("ripgrep可用: " + ripgrepAvailable);
        System.out.println("git可用: " + gitAvailable);
        System.out.println("ctags可用: " + ctagsAvailable);
    }

    @Test
    void testGetInstallationSuggestions() {
        // 先执行检查
        systemToolChecker.checkAllTools();
        
        // 获取安装建议
        String suggestions = systemToolChecker.getInstallationSuggestions();
        
        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty());
        
        System.out.println("安装建议:");
        System.out.println(suggestions);
    }

    @Test
    void testIsToolAvailableForUnknownTool() {
        // 测试未知工具
        Boolean unknownTool = systemToolChecker.isToolAvailable("unknown-tool");
        
        assertFalse(unknownTool);
    }
} 