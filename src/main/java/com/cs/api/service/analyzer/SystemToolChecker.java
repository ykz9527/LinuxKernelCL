package com.cs.api.service.analyzer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统工具检查器
 * 检查分析所需的外部工具是否可用
 * 
 * @author YK
 * @since 1.0.0
 */
@Component
public class SystemToolChecker {

    private static final Logger logger = LoggerFactory.getLogger(SystemToolChecker.class);

    private final Map<String, Boolean> toolAvailability = new HashMap<>();

    /**
     * 检查所有必要工具
     */
    public Map<String, Boolean> checkAllTools() {
        logger.info("开始检查系统工具可用性");
        
        toolAvailability.put("ripgrep", checkRipgrep());
        toolAvailability.put("git", checkGit());
        toolAvailability.put("ctags", checkCTags());
        
        logger.info("工具可用性检查完成: {}", toolAvailability);
        return new HashMap<>(toolAvailability);
    }

    /**
     * 检查ripgrep是否可用
     */
    private boolean checkRipgrep() {
        try {
            Process process = new ProcessBuilder("rg", "--version").start();
            int exitCode = process.waitFor();
            boolean available = exitCode == 0;
            
            if (available) {
                logger.info("✓ ripgrep 可用");
            } else {
                logger.warn("✗ ripgrep 不可用，请安装: sudo apt install ripgrep 或 brew install ripgrep");
            }
            
            return available;
        } catch (IOException | InterruptedException e) {
            logger.warn("✗ ripgrep 检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查git是否可用
     */
    private boolean checkGit() {
        try {
            Process process = new ProcessBuilder("git", "--version").start();
            int exitCode = process.waitFor();
            boolean available = exitCode == 0;
            
            if (available) {
                logger.info("✓ git 可用");
            } else {
                logger.warn("✗ git 不可用，请安装git");
            }
            
            return available;
        } catch (IOException | InterruptedException e) {
            logger.warn("✗ git 检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查ctags是否可用
     */
    private boolean checkCTags() {
        try {
            // 尝试universal-ctags
            Process process = new ProcessBuilder("ctags", "--version").start();
            int exitCode = process.waitFor();
            boolean available = exitCode == 0;
            
            if (available) {
                logger.info("✓ ctags 可用");
            } else {
                logger.warn("✗ ctags 不可用，请安装: sudo apt install universal-ctags 或 brew install universal-ctags");
            }
            
            return available;
        } catch (IOException | InterruptedException e) {
            logger.warn("✗ ctags 检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查工具是否可用
     */
    public boolean isToolAvailable(String toolName) {
        return toolAvailability.getOrDefault(toolName, false);
    }

    /**
     * 获取工具安装建议
     */
    public String getInstallationSuggestions() {
        StringBuilder suggestions = new StringBuilder();
        suggestions.append("缺失工具安装建议:\n");
        
        if (!isToolAvailable("ripgrep")) {
            suggestions.append("• ripgrep: sudo apt install ripgrep (Ubuntu) 或 brew install ripgrep (macOS)\n");
        }
        
        if (!isToolAvailable("git")) {
            suggestions.append("• git: sudo apt install git (Ubuntu) 或 brew install git (macOS)\n");
        }
        
        if (!isToolAvailable("ctags")) {
            suggestions.append("• ctags: sudo apt install universal-ctags (Ubuntu) 或 brew install universal-ctags (macOS)\n");
        }
        
        return suggestions.toString();
    }
} 