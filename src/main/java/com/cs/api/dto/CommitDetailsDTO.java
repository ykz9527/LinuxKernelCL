package com.cs.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for storing details of a specific commit, including message and patch.
 *
 * @author Yujia
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommitDetailsDTO {

    private String commitId;

    /**
     * The full commit message, including title and body.
     */
    private String commitMessage;

    /**
     * The diff/patch for the specified file in this commit.
     */
    private String patch;
}
