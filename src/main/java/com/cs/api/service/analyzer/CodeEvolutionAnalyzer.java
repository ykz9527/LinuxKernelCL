package com.cs.api.service.analyzer;

import com.cs.api.dto.CommitDetailsDTO;
import com.cs.api.dto.TrackerApiResponseDTO;
import com.cs.api.dto.CodeTraceResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Analyzes the evolution of a piece of code by identifying key commits.
 *
 * @author Yujia
 * @since 1.0.0
 */
@Service
public class CodeEvolutionAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(CodeEvolutionAnalyzer.class);

    /**
     * 使用大语言模型获取关键的变更.
     * TODO: 可以先通过启发式规则，减少大模型的调用次数.
     * 
     * @param commitHistory 代码变更的完整历史.
     * @return 关键的版本历史.
     */
    public List<CodeTraceResponseDTO> filterKeyCommits(List<CodeTraceResponseDTO> codeTraceHistroy) {

        
        return codeTraceHistroy;
    }


    /**
     * Retrieves the commit message and patch (diff) for a specific commit and file.
     *
     * @param kernelSourcePath The absolute path to the kernel source code repository.
     * @param filePath         The path of the file relative to the repository root.
     * @param commitId         The ID of the commit.
     * @return A DTO containing the commit message and patch, or null if an error occurs.
     */
    public CommitDetailsDTO getCommitDetails(String kernelSourcePath, String filePath, String commitId) {
        logger.debug("Getting details for commit: {}, file: {}", commitId, filePath);

        // The command to get the commit message and the patch for the specific file
        ProcessBuilder pb = new ProcessBuilder("git", "show", "--pretty=fuller", "--patch", commitId, "--", filePath);
        pb.directory(new File(kernelSourcePath));

        try {
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                logger.warn("Git show command timed out for commit {}", commitId);
                return null;
            }

            if (process.exitValue() != 0) {
                logger.warn("Git show command failed for commit {} with exit code {}", commitId, process.exitValue());
                return null;
            }

            String fullOutput = output.toString();
            String commitMessage = extractCommitMessage(fullOutput);
            String patch = extractPatch(fullOutput);

            return new CommitDetailsDTO(commitId, commitMessage, patch);

        } catch (IOException | InterruptedException e) {
            logger.error("Error while executing git show for commit {}: {}", commitId, e.getMessage());
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private String extractCommitMessage(String gitShowOutput) {
        int patchStart = gitShowOutput.indexOf("\ndiff --git");
        if (patchStart != -1) {
            return gitShowOutput.substring(0, patchStart).trim();
        }
        return gitShowOutput.trim(); // No patch found, the whole output is the message
    }

    private String extractPatch(String gitShowOutput) {
        int patchStart = gitShowOutput.indexOf("\ndiff --git");
        if (patchStart != -1) {
            return gitShowOutput.substring(patchStart).trim();
        }
        return ""; // No patch found
    }
}
