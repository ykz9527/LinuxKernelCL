package com.cs.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.cs.api.service.analyzer.SystemToolChecker;

import java.util.Map;

/**
 * 应用启动检查配置
 * 在应用启动时检查必要工具的可用性
 * 
 * @author YK
 * @since 1.0.0
 */
@Component
public class StartupCheckConfig implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(StartupCheckConfig.class);

    @Autowired
    private SystemToolChecker systemToolChecker;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("🚀 开始检查系统环境...");
        
        Map<String, Boolean> toolStatus = systemToolChecker.checkAllTools();
        
        boolean allToolsAvailable = toolStatus.values().stream().allMatch(Boolean::booleanValue);
        
        if (allToolsAvailable) {
            logger.info("✅ 所有工具检查通过，将使用实时代码分析模式");
            logger.info("📍 实时分析模式特点:");
            logger.info("   • 直接分析本地Linux内核源码");
            logger.info("   • 支持版本切换和实时搜索");
            logger.info("   • 提供更精确的代码片段");
        } else {
            logger.warn("⚠️  部分工具不可用，将使用静态数据模式");
            logger.warn("📋 工具状态: {}", toolStatus);
            logger.warn("💡 安装建议:");
            logger.warn(systemToolChecker.getInstallationSuggestions());
            logger.warn("📝 静态数据模式特点:");
            logger.warn("   • 使用预置的代码示例");
            logger.warn("   • 功能有限，仅供演示");
            logger.warn("   • 建议安装工具以获得完整功能");
        }
        
        logger.info("🎯 应用启动完成，可通过以下方式测试:");
        logger.info("   • API文档: http://localhost:8080/swagger-ui.html");
        logger.info("   • 健康检查: http://localhost:8080/health");
        logger.info("   • 工具状态: http://localhost:8080/api/code-search/statistics");
        logger.info("═══════════════════════════════════════════════");
    }
} 