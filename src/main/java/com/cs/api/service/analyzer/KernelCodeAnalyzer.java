package com.cs.api.service.analyzer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cs.api.dto.CodeSearchResultDTO;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Linux内核代码分析器
 * 使用Eclipse CDT工具进行代码搜索和分析
 * 
 * @author YK
 * @since 2.0.0
 */
@Component
public class KernelCodeAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(KernelCodeAnalyzer.class);

    @Value("${kernel.analysis.timeout:30}")
    private int timeoutSeconds;

    /**
     * 使用Eclipse CDT工具通过文件路径和行号获取指定代码元素的完整结构
     * 
     * @param filePath 文件路径（相对于内核源码根目录）
     * @param lineNumber 目标行号
     * @param kernelSourcePath 内核源码根路径
     * @param version Git版本（commit ID, branch, or tag）
     * @return CodeSearchResultDTO 包含完整代码定义的结果，如果未找到返回null
     */
    public static CodeSearchResultDTO findCodeElementByLineNumber(String filePath, String concept, int lineNumber, String kernelSourcePath, String version) {
        logger.debug("使用Eclipse CDT查找代码元素: file={}, line={}, version={}", filePath, lineNumber, version);
        
        try {
            // 从Git仓库获取文件内容
            byte[] fileContentBytes = getFileContentFromGit(kernelSourcePath, filePath, version);
            if (fileContentBytes == null) {
                logger.debug("文件 {} 在版本 {} 中不存在", filePath, version);
                return null;
            }
            
            // 使用Eclipse CDT解析文件
            IASTTranslationUnit translationUnit = parseFile(filePath, fileContentBytes);
            if (translationUnit == null) {
                logger.debug("CDT解析失败: {}", filePath);
                return null;
            }
            
            // 查找指定行号的代码元素
            CodeElement element = findElementAtLine(translationUnit, lineNumber);
            if (element == null) {
                logger.debug("未找到第{}行的代码元素", lineNumber);
                return null;
            }
            // 提取完整的代码块
            String fileContent = new String(fileContentBytes);
            return extractCodeBlock(concept, element, filePath, fileContent, version);
            
        } catch (Exception e) {
            logger.warn("使用CDT查找代码元素失败: file={}, line={}, error={}", filePath, lineNumber, e.getMessage());
            return null;
        }
    }

    /**
     * 使用Eclipse CDT工具通过标识符名称查找代码元素
     * 
     * @param filePath 文件路径（相对于内核源码根目录）
     * @param concept 代码标识符（函数名、结构体名等）
     * @param kernelSourcePath 内核源码根路径
     * @param version Git版本（commit ID, branch, or tag）
     * @return CodeSearchResultDTO 包含完整代码定义的结果，如果未找到返回null
     */
    public static CodeSearchResultDTO findCodeElementByIdentifier(String filePath, String concept, String type, String kernelSourcePath, String version) {
        logger.debug("使用Eclipse CDT查找代码标识符: file={}, identifier={}, version={}", filePath, concept, version);
        
        try {
            // 从Git仓库获取文件内容
            byte[] fileContentBytes = getFileContentFromGit(kernelSourcePath, filePath, version);
            if (fileContentBytes == null) {
                logger.debug("文件 {} 在版本 {} 中不存在", filePath, version);
                return null;
            }
            
            // 使用Eclipse CDT解析文件
            IASTTranslationUnit translationUnit = parseFile(filePath, fileContentBytes);
            if (translationUnit == null) {
                logger.debug("CDT解析失败: {}", filePath);
                return null;
            }
            
            // 查找指定标识符的代码元素
            CodeElement element = findElementByName(translationUnit, concept);
            if (element == null) {
                logger.debug("未找到标识符: {}", concept);
                return null;
            }
            
            // 提取完整的代码块
            String fileContent = new String(fileContentBytes);
            return extractCodeBlock(concept, element, filePath, fileContent, version);
            
        } catch (Exception e) {
            logger.warn("使用CDT查找代码标识符失败: file={}, identifier={}, error={}", filePath, concept, e.getMessage());
            return null;
        }
    }

    /**
     * 使用JGit从Git仓库获取指定版本的文件内容
     *
     * @param repoPath Git仓库路径
     * @param filePath 文件路径
     * @param version Git版本（commit ID, branch, or tag）
     * @return 文件内容的字节数组，如果未找到则返回null
     * @throws IOException
     */
    private static byte[] getFileContentFromGit(String repoPath, String filePath, String version) throws IOException {
        File repoDir = new File(repoPath);
        if (!repoDir.isDirectory()) {
            logger.warn("Git仓库路径不存在或不是一个目录: {}", repoPath);
            return null;
        }

        ProcessBuilder pb = new ProcessBuilder("git", "show", version + ":" + filePath);
        pb.directory(repoDir);
        Process process = pb.start();

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

        try (InputStream stdout = process.getInputStream(); InputStream stderr = process.getErrorStream()) {
            stdout.transferTo(result);
            stderr.transferTo(errorStream);
        }

        boolean finished;
        try {
            // Using a default timeout of 30 seconds as we are in a static context
            finished = process.waitFor(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for `git show` command", e);
        }

        if (!finished) {
            process.destroyForcibly();
            throw new IOException("`git show` command timed out after 30 seconds for " + version + ":" + filePath);
        }

        if (process.exitValue() != 0) {
            String errorOutput = errorStream.toString();
            // Log non-fatal errors (like file not found) at a lower level
            if (errorOutput.contains("does not exist in") || errorOutput.contains("exists on disk, but not in")) {
                logger.debug("File not found in git: {}:{}", version, filePath);
                return null;
            }
            // Log other errors as warnings
            logger.warn("`git show` command failed for '{}:{}' with exit code {}. Stderr: {}", version, filePath, process.exitValue(), errorOutput);
            throw new IOException("Git command exited with code " + process.exitValue() + ": " + errorOutput);
        }
        
        return result.toByteArray();
    }

    /**
     * 使用Eclipse CDT解析C/C++文件内容
     * 
     * @param filePath 要解析的文件路径
     * @param fileContentBytes 文件内容
     * @return IASTTranslationUnit 解析结果
     */
    private static IASTTranslationUnit parseFile(String filePath, byte[] fileContentBytes) {
        try {
            FileContent fileContent = FileContent.create(filePath, new String(fileContentBytes).toCharArray());
            
            Map<String, String> definedSymbols = new HashMap<>();
            String[] includePaths = new String[0];
            
            IScannerInfo scannerInfo = new ScannerInfo(definedSymbols, includePaths);
            IncludeFileContentProvider includeProvider = IncludeFileContentProvider.getEmptyFilesProvider();
            
            // 创建解析器
            ILanguage language = GCCLanguage.getDefault();
            int parseOptions = 0;
            parseOptions |= ILanguage.OPTION_NO_IMAGE_LOCATIONS;
            parseOptions |= ILanguage.OPTION_PARSE_INACTIVE_CODE;
            // 解析文件: 比较耗时
            IASTTranslationUnit translationUnit = language.getASTTranslationUnit(
                fileContent, 
                scannerInfo, 
                includeProvider, 
                null,
                parseOptions,
                new DefaultLogService()
            );
            
            return translationUnit;
            
        } catch (Exception e) {
            logger.warn("CDT解析文件失败: {}, error={}", filePath, e.getMessage());
            return null;
        }
    }

    /**
     * 根据行号查找代码元素
     * 
     * @param translationUnit AST根节点
     * @param lineNumber 目标行号
     * @return CodeElement 找到的代码元素
     */
    private static CodeElement findElementAtLine(IASTTranslationUnit translationUnit, int lineNumber) {
        ElementFinder finder = new ElementFinder(lineNumber);
        translationUnit.accept(finder);
        return finder.getFoundElement();
    }

    /**
     * 根据标识符名称查找代码元素
     * 
     * @param translationUnit AST根节点
     * @param identifier 标识符名称
     * @return CodeElement 找到的代码元素
     */
    private static CodeElement findElementByName(IASTTranslationUnit translationUnit, String identifier) {
        NameFinder finder = new NameFinder(identifier);
        translationUnit.accept(finder);
        return finder.getFoundElement();
    }

    /**
     * 提取完整的代码块
     * 
     * @param element 代码元素
     * @param filePath 文件路径
     * @param fileContent 文件内容
     * @param version Git版本
     * @return CodeSearchResultDTO 代码搜索结果
     */
    private static CodeSearchResultDTO extractCodeBlock(String concept, CodeElement element, String filePath, String fileContent, String version) throws IOException {
        List<String> fileLines = Arrays.asList(fileContent.split("\\R"));
        
        // 计算实际的行号范围（CDT行号从1开始）
        int startLine = Math.max(1, element.startLine);
        int endLine = Math.min(fileLines.size(), element.endLine);
        
        // 提取代码片段
        StringBuilder codeSnippet = new StringBuilder();
        for (int i = startLine - 1; i < endLine; i++) {
            codeSnippet.append(fileLines.get(i)).append("\n");
        }
        
        String explanation = String.format(
            "通过Eclipse CDT工具找到%s `%s` 的完整定义。文件: %s, 第%d-%d行。",
            element.type,
            concept,
            filePath,
            startLine,
            endLine
        );
        
        return new CodeSearchResultDTO(
            filePath,
            codeSnippet.toString().trim(),
            startLine,
            startLine,
            endLine,
            explanation,
            version,
            element.type
        );
    }

    /**
     * 代码元素信息类
     */
    private static class CodeElement {
        final String type;
        final int startLine;
        final int endLine;
        
        CodeElement(String type, int startLine, int endLine) {
            this.type = type;
            this.startLine = startLine;
            this.endLine = endLine;
        }
    }

    /**
     * 基于行号的元素查找器
     */
    private static class ElementFinder extends ASTVisitor {
        private final int targetLine;
        private CodeElement foundElement;
        
        ElementFinder(int targetLine) {
            this.targetLine = targetLine;
            this.shouldVisitDeclarations = true;
        }
        
        @Override
        public int visit(IASTDeclaration declaration) {
            IASTFileLocation location = declaration.getFileLocation();
            if (location != null) {
                int startLine = location.getStartingLineNumber();
                int endLine = location.getEndingLineNumber();
                
                if (targetLine >= startLine && targetLine <= endLine) {
                    String type = getDeclarationType(declaration);
                    foundElement = new CodeElement(type, startLine, endLine);
                    return PROCESS_ABORT;
                }
            }
            return PROCESS_CONTINUE;
        }
        
        CodeElement getFoundElement() {
            return foundElement;
        }
    }

    /**
     * 基于名称的元素查找器
     */
    private static class NameFinder extends ASTVisitor {
        private final String targetName;
        private CodeElement foundElement;
        
        NameFinder(String targetName) {
            this.targetName = targetName;
            this.shouldVisitDeclarations = true;
        }
        
        @Override
        public int visit(IASTDeclaration declaration) {
            String name = extractDeclarationName(declaration);
            if (targetName.equals(name)) {
                IASTFileLocation location = declaration.getFileLocation();
                if (location != null) {
                    String type = getDeclarationType(declaration);
                    foundElement = new CodeElement(
                        type, 
                        location.getStartingLineNumber(), 
                        location.getEndingLineNumber()
                    );
                    return PROCESS_ABORT;
                }
            }
            return PROCESS_CONTINUE;
        }
        
        CodeElement getFoundElement() {
            return foundElement;
        }
    }

    /**
     * 提取声明的名称
     */
    private static String extractDeclarationName(IASTDeclaration declaration) {
        if (declaration instanceof IASTFunctionDefinition) {
            IASTFunctionDefinition func = (IASTFunctionDefinition) declaration;
            IASTDeclarator declarator = func.getDeclarator();
            if (declarator != null && declarator.getName() != null) {
                return declarator.getName().toString();
            }
        } else if (declaration instanceof IASTSimpleDeclaration) {
            IASTSimpleDeclaration simple = (IASTSimpleDeclaration) declaration;
            IASTDeclarator[] declarators = simple.getDeclarators();
            if (declarators.length > 0 && declarators[0].getName() != null) {
                return declarators[0].getName().toString();
            }
        }
        return "unknown";
    }

    /**
     * 获取声明的类型
     */
    private static String getDeclarationType(IASTDeclaration declaration) {
        if (declaration instanceof IASTFunctionDefinition) {
            return "function";
        } else if (declaration instanceof IASTSimpleDeclaration) {
            IASTSimpleDeclaration simple = (IASTSimpleDeclaration) declaration;
            IASTDeclSpecifier declSpec = simple.getDeclSpecifier();
            if (declSpec instanceof IASTCompositeTypeSpecifier) {
                IASTCompositeTypeSpecifier composite = (IASTCompositeTypeSpecifier) declSpec;
                switch (composite.getKey()) {
                    case IASTCompositeTypeSpecifier.k_struct:
                        return "struct";
                    case IASTCompositeTypeSpecifier.k_union:
                        return "union";
                    default:
                        return "composite";
                }
            } else if (declSpec instanceof IASTEnumerationSpecifier) {
                return "enum";
            }
            return "declaration";
        }
        return "unknown";
    }

    /**
     * 通过文件路径和行号查找并提取完整的注释块。
     *
     * @param filePath 文件路径
     * @param concept  相关概念
     * @param lineNumber 目标行号
     * @param kernelSourcePath 内核源码根路径
     * @param version Git版本
     * @return CodeSearchResultDTO 包含完整注释和上下文信息，如果未找到则返回null
     */
    public static CodeSearchResultDTO findCommentBlockByLineNumber(String filePath, String concept, int lineNumber, String kernelSourcePath, String version) {
        logger.debug("使用Eclipse CDT查找注释块: file={}, line={}, version={}", filePath, lineNumber, version);

        try {
            byte[] fileContentBytes = getFileContentFromGit(kernelSourcePath, filePath, version);
            if (fileContentBytes == null) {
                logger.debug("文件 {} 在版本 {} 中不存在", filePath, version);
                return null;
            }

            IASTTranslationUnit translationUnit = parseFile(filePath, fileContentBytes);
            if (translationUnit == null) {
                logger.debug("CDT解析失败: {}", filePath);
                return null;
            }

            IASTComment targetComment = null;
            for (IASTComment comment : translationUnit.getComments()) {
                IASTFileLocation location = comment.getFileLocation();
                if (location != null && lineNumber >= location.getStartingLineNumber() && lineNumber <= location.getEndingLineNumber()) {
                    targetComment = comment;
                    break;
                }
            }

            if (targetComment == null) {
                logger.debug("在文件 {} 的第 {} 行未找到注释", filePath, lineNumber);
                return null;
            }

            String fileContent = new String(fileContentBytes);
            List<String> fileLines = Arrays.asList(fileContent.split("\\R"));
            
            IASTFileLocation location = targetComment.getFileLocation();
            int startLine = location.getStartingLineNumber();
            int endLine = location.getEndingLineNumber();

            StringBuilder commentContent = new StringBuilder();
            for (int i = startLine - 1; i < endLine && i < fileLines.size(); i++) {
                commentContent.append(fileLines.get(i)).append("\n");
            }

            String explanation = String.format(
                "在文件 %s 的 %d-%d行 找到与 '%s' 相关的文档注释。",
                filePath,
                startLine,
                endLine,
                concept
            );

            return new CodeSearchResultDTO(
                filePath,
                commentContent.toString(),
                startLine,
                startLine,
                endLine,
                explanation,
                version,
                "documentation"
            );

        } catch (Exception e) {
            logger.warn("查找注释块失败: file={}, line={}, error={}", filePath, lineNumber, e.getMessage());
            return null;
        }
    }
}