package com.cs.api.service.impl;

import com.cs.api.dto.CodeTraceResponseDTO;
import com.cs.api.dto.CodeTraceResultDTO;
import com.cs.api.dto.TrackerApiResponseDTO;
import com.cs.api.dto.CodeSearchResultDTO;
import com.cs.api.service.CodeTraceService;
import com.cs.api.service.analyzer.KernelCodeAnalyzer;
import com.cs.api.service.analyzer.CodeEvolutionAnalyzer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

/**
 * 代码追溯服务实现类
 * 
 * @author YK
 * @since 1.0.0
 */
@Service
public class CodeTraceServiceImpl implements CodeTraceService {

    private static final Logger logger = LoggerFactory.getLogger(CodeTraceServiceImpl.class);

    /**
     * 外部tracker API的基础URL
     * 可以通过application.yml配置文件进行配置
     */
    @Value("${tracker.api.base.url:http://10.176.34.96:7777}")
    private String trackerApiBaseUrl;

    /**
     * API请求超时时间（秒）
     */
    @Value("${tracker.api.timeout:30}")
    private int apiTimeoutSeconds;

    /**
     * 默认的仓库路径
     */
    @Value("${tracker.api.default.repo:linux-stable}")
    private String defaultRepoPath;

    /**
     * Linux内核源码根路径
     */
    @Value("${kernel.source.path:/home/fdse/ytest/codeMap/linux/repo}")
    private String kernelSourcePath;

    /**
     * HTTP客户端实例，用于调用外部API
     */
    private final RestTemplate restTemplate;

    /**
     * JSON解析器，用于解析API响应
     */
    private final ObjectMapper objectMapper;

    @Autowired
    private CodeEvolutionAnalyzer codeEvolutionAnalyzer;

    public CodeTraceServiceImpl() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        logger.info("CodeTraceServiceImpl初始化完成");
    }

    @Override
    public List<CodeTraceResponseDTO> traceMethodHistory(String filePath, String methodName, String version, String targetCommit) {
        logger.info("开始追溯方法历史: methodName={}, filePath={}, version={}, targetCommit={}", methodName, filePath, version, targetCommit);
        
        List<CodeTraceResponseDTO> responseList = new ArrayList<>();

        try {
            // 验证输入参数
            if (methodName == null || methodName.trim().isEmpty()) {
                logger.warn("方法名不能为空");
                return responseList;
            }

            if (filePath == null || filePath.isEmpty()) {
                logger.warn("文件路径列表不能为空");
                return responseList;
            }

            // 调用外部API获取commit历史信息
            List<CodeTraceResultDTO> commitHistoryList = fetchCommitHistory(methodName, filePath, version, targetCommit);

            for (CodeTraceResultDTO commitHistory : commitHistoryList){
            
                // 创建响应对象并设置基本信息
                CodeTraceResponseDTO response = new CodeTraceResponseDTO(commitHistory);
                response.setFilePath(filePath);
            
                // 使用KernelCodeAnalyzer获取代码片段和行号信息
                try {
                    // 优先使用commitHistory中的版本信息，如果没有则使用输入的version
                    String codeVersion = (commitHistory != null && commitHistory.getVersion() != null) 
                    ? commitHistory.getVersion() : version;
                    
                    logger.debug("开始使用KernelCodeAnalyzer获取代码信息: filePath={}, methodName={}, version={}", 
                        filePath, methodName, codeVersion);
                
                    // 使用增强的代码搜索方法
                    CodeSearchResultDTO codeResult = enhancedCodeSearch(filePath, methodName, codeVersion);
                
                    if (codeResult != null) {
                        // 将代码分析结果添加到响应中
                        response.setCodeSnippet(codeResult.getCodeSnippet());
                        response.setStartLine(codeResult.getStartLine());
                        response.setEndLine(codeResult.getEndLine());
                        response.setExplanation(codeResult.getExplanation());
                    
                        logger.info("✅ 成功获取代码片段: startLine={}, endLine={}, snippetLength={}", 
                            codeResult.getStartLine(), codeResult.getEndLine(), 
                            codeResult.getCodeSnippet() != null ? codeResult.getCodeSnippet().length() : 0);
                    } else {
                        logger.warn("所有搜索策略均未找到代码元素: methodName={}, filePath={}", methodName, filePath);
                        response.setExplanation(String.format("在文件 %s (版本: %s) 中未找到方法 %s 的代码定义。" +
                            "这可能是因为方法名格式不匹配、文件不存在或版本差异导致的。", filePath, codeVersion, methodName));
                    }
                    
                } catch (Exception codeAnalysisException) {
                    logger.warn("代码分析过程中发生异常: {}", codeAnalysisException.getMessage());
                    response.setExplanation("代码分析过程中发生异常: " + codeAnalysisException.getMessage());
                }

                responseList.add(response);
            }
            

            logger.info("方法历史追溯完成: methodName={}, 找到 {} 条 commit", methodName, responseList.size());

            logger.debug("Full Response:{}", responseList);
            
            return responseList;

        } catch (Exception e) {
            logger.error("追溯方法历史失败", e);
            CodeTraceResponseDTO errorResponse = new CodeTraceResponseDTO("追溯方法历史失败: " + e.getMessage());
            responseList.add(errorResponse);
            return responseList;
        }
    }
    
    /**
     * 调用外部tracker API获取commit历史
     * 
     * @param methodName 方法名称
     * @param filePaths 文件路径列表
     * @param version 版本号
     * @return commit历史记录列表
     */
    private List<CodeTraceResultDTO> fetchCommitHistory(String methodName, String filePaths, String version, String targetCommit) {
        logger.debug("开始获取commit历史: methodName={}, filePaths={}, version={}, targetCommit={}", methodName, filePaths, version, targetCommit);
        
        // 在现有的 logger.debug 语句后添加 version 预处理逻辑
        // 处理 version 参数，移除 "v" 开头
        if (version != null && !version.isEmpty() && version.startsWith("v") && version.length() > 1) {
            version = version.substring(1);
            logger.debug("移除版本号前缀'v': 处理后的版本号为 {}", version);
        }
        
        // CodeTraceResultDTO result = new CodeTraceResultDTO();
        List<CodeTraceResultDTO> result = new ArrayList<CodeTraceResultDTO>();
        
        try {
            
            // 构建API请求URL
            String apiUrl = buildTrackerApiUrl(methodName, filePaths, version, targetCommit);
            logger.info("调用tracker API: {}", apiUrl);
            // String apiUrl2 = "http://10.176.34.96:7777/tracker/trackMethod?repoPath=linux-stable&filePaths=mm/memory-failure.c&methodName=static%20void%20collect_procs_file(struct%20page%20*page,%20struct%20list_head%20*to_kill,int%20force_early)&version=6.4";
            // 执行HTTP请求
            ResponseEntity<String> response = executeTrackerApiRequest(apiUrl);
            
            if (response == null || response.getBody() == null) {
                logger.warn("tracker API返回空响应");
                return result;
            }
            
            // 解析API响应
            TrackerApiResponseDTO apiResponse = parseTrackerApiResponse(response.getBody());
            
            if (apiResponse == null) {
                logger.warn("无法解析tracker API响应");
                return result;
            }
            
            if (!apiResponse.isSuccess()) {
                logger.warn("tracker API调用失败: code={}, msg={}", apiResponse.getCode(), apiResponse.getMsg());
                return result;
            }
            
            // 转换数据格式
            result = convertToCodeTraceResult(apiResponse);
            
            logger.info("✅ 成功获取到 {} 条commit历史记录", result.size());
            
        } catch (Exception e) {
            logger.error("获取commit历史时发生异常: methodName={}, filePaths={}, version={}", 
                        methodName, filePaths, version, e);
        }
        
        return result;
    }

    /**
     * 构建tracker API请求URL
     */
    private String buildTrackerApiUrl(String methodName, String filePath, String version, String targetCommit) {
        try {
            // 构建URL参数
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(trackerApiBaseUrl)
                .path("/tracker/trackMethod")
                .queryParam("repoPath", defaultRepoPath)
                .queryParam("filePaths", filePath)
                .queryParam("methodName", methodName)
                .queryParam("version", version)
                .queryParam("targetCommit", targetCommit)
                .queryParam("trackNum", 0);
            
            return builder.build().encode().toUriString();
            
        } catch (Exception e) {
            logger.error("构建tracker API URL失败", e);
            throw new RuntimeException("构建API URL失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行tracker API HTTP请求
     */
    private ResponseEntity<String> executeTrackerApiRequest(String apiUrl) {
        try {
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36");
            headers.set("Accept", "application/json");
            headers.set("Content-Type", "application/json;charset=UTF-8");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // 执行GET请求
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.GET,
                entity,
                String.class
            );
            
            logger.debug("tracker API响应状态: {}, 响应体长度: {}", 
                        response.getStatusCode(), 
                        response.getBody() != null ? response.getBody().length() : 0);
            
            return response;
            
        } catch (HttpClientErrorException e) {
            logger.error("tracker API HTTP请求失败: 状态码={}, 响应体={}", 
                        e.getStatusCode().value(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            logger.error("tracker API请求异常", e);
            return null;
        }
    }

    
    /**
     * 解析tracker API响应JSON
     */
    private TrackerApiResponseDTO parseTrackerApiResponse(String responseBody) {
        try {
            TrackerApiResponseDTO apiResponse = objectMapper.readValue(responseBody, TrackerApiResponseDTO.class);
            logger.debug("成功解析tracker API响应: success={}, code={}, msg={}", 
                        apiResponse.isSuccess(), apiResponse.getCode(), apiResponse.getMsg());
            return apiResponse;
        } catch (Exception e) {
            logger.error("解析tracker API响应失败: {}", responseBody, e);
            return null;
        }
    }

    /**
     * 将tracker API响应数据转换为CodeTraceResultDTO列表
     */
    private List<CodeTraceResultDTO> convertToCodeTraceResult(TrackerApiResponseDTO apiResponse) {
        
        List<CodeTraceResultDTO> resultList = new ArrayList<>();
        
        if (apiResponse.getData() == null || apiResponse.getData().isEmpty()) {
            logger.debug("tracker API响应中没有数据");
            return resultList;
        }
        
        try {
            // 遍历data中的所有方法
            for (Map.Entry<String, List<TrackerApiResponseDTO.TrackerCommitInfoDTO>> entry : apiResponse.getData().entrySet()) {
                String methodSignature = entry.getKey();
                List<TrackerApiResponseDTO.TrackerCommitInfoDTO> commitInfos = entry.getValue();
                
                logger.debug("处理方法签名: {}, commit数量: {}", methodSignature, commitInfos.size());

                // 转换每个commit信息
                for (TrackerApiResponseDTO.TrackerCommitInfoDTO commitInfo : commitInfos) {
                    CodeTraceResultDTO result = convertToCodeTraceResult(commitInfo);
                    resultList.add(result);

                }
            }
            
            logger.debug("成功转换 {} 条commit记录", resultList.size());
            
        } catch (Exception e) {
            logger.error("转换tracker API响应数据失败", e);
        }
        
        return resultList;
    }

    /**
     * 增强的代码搜索方法
     * 当通过方法名无法找到代码时，尝试其他搜索策略
     * 
     * @param filePath 文件路径
     * @param methodName 方法名
     * @param version 版本
     * @return CodeSearchResultDTO 代码搜索结果
     */
    private CodeSearchResultDTO enhancedCodeSearch(String filePath, String methodName, String version) {
        logger.debug("开始增强代码搜索: filePath={}, methodName={}, version={}", filePath, methodName, version);
        
        try {
            // 1. 首先尝试通过完整方法名搜索
            CodeSearchResultDTO result = KernelCodeAnalyzer.findCodeElementByIdentifier(
                filePath, methodName, "function", kernelSourcePath, version);
            
            if (result != null) {
                logger.debug("通过完整方法名找到代码元素");
                return result;
            }
            
            // 2. 如果方法名包含参数或者修饰符，尝试提取简化的方法名
            String simplifiedMethodName = extractSimpleMethodName(methodName);
            if (!simplifiedMethodName.equals(methodName)) {
                logger.debug("尝试使用简化方法名搜索: {}", simplifiedMethodName);
                result = KernelCodeAnalyzer.findCodeElementByIdentifier(
                    filePath, simplifiedMethodName, "function", kernelSourcePath, version);
                    
                if (result != null) {
                    logger.debug("通过简化方法名找到代码元素");
                    return result;
                }
            }
            
            // 3. 尝试搜索结构体或其他类型的代码元素
            logger.debug("尝试搜索结构体类型");
            result = KernelCodeAnalyzer.findCodeElementByIdentifier(
                filePath, simplifiedMethodName, "struct", kernelSourcePath, version);
                
            if (result != null) {
                logger.debug("找到结构体定义");
                return result;
            }
            
            logger.debug("所有搜索策略均未找到代码元素");
            return null;
            
        } catch (Exception e) {
            logger.warn("增强代码搜索过程中发生异常: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 提取简化的方法名
     * 移除函数签名中的参数、返回类型等，只保留核心方法名
     * 
     * @param methodName 原始方法名
     * @return 简化的方法名
     */
    private String extractSimpleMethodName(String methodName) {
        if (methodName == null || methodName.trim().isEmpty()) {
            return methodName;
        }
        
        String simplified = methodName.trim();
        
        // 移除static、inline等修饰符
        simplified = simplified.replaceAll("^(static|inline|extern|void|int|long|char|struct)\\s+", "");
        
        // 如果包含括号，提取括号前的部分
        int parenIndex = simplified.indexOf('(');
        if (parenIndex != -1) {
            simplified = simplified.substring(0, parenIndex).trim();
        }
        
        // 移除返回类型（最后一个空格后的部分通常是方法名）
        String[] parts = simplified.split("\\s+");
        if (parts.length > 1) {
            simplified = parts[parts.length - 1];
        }
        
        // 移除指针标记
        simplified = simplified.replace("*", "").trim();
        
        logger.debug("方法名简化: {} -> {}", methodName, simplified);
        return simplified;
    }

    /**
     * 将单个TrackerCommitInfoDTO转换为CodeTraceResultDTO
     */
    private CodeTraceResultDTO convertToCodeTraceResult(TrackerApiResponseDTO.TrackerCommitInfoDTO commitInfo) {
        CodeTraceResultDTO result = new CodeTraceResultDTO();
        
        result.setId(commitInfo.getId());
        result.setCommitId(commitInfo.getCommitId());
        result.setAuthorName(commitInfo.getAuthorName());
        result.setCommitterName(commitInfo.getCommitterName());
        result.setAuthorTime(commitInfo.getAuthorTime());
        result.setCommitTime(commitInfo.getCommitTime());
        result.setCommitTitle(commitInfo.getCommitTitle());
        result.setAdded(commitInfo.getAdded());
        result.setDeleted(commitInfo.getDeleted());
        result.setCompany(commitInfo.getCompany());
        result.setVersion(commitInfo.getVersion());
        result.setRepo(commitInfo.getRepo());
        result.setH1(commitInfo.getH1());
        result.setH2(commitInfo.getH2());
        result.setText(commitInfo.getText());
        result.setNewbiesVersion(commitInfo.getNewbiesVersion());
        result.setFeatureId(commitInfo.getFeatureId());
        
        return result;
    }

    /**
     * 使用大模型分析 CodeTrace 的 Pipeline
     */
    private String analysisCodeTraceByLLM(List<CodeTraceResponseDTO> codeTraceHistroy){
        List<CodeTraceResponseDTO> keyCodeTraceHistory = codeEvolutionAnalyzer.filterKeyCommits(codeTraceHistroy);
        



    }

} 