package com.cs.api.dto;

/**
 * DTO for storing details of a specific commit, including message and patch.
 *
 * @author Yujia
 * @since 1.0.0
 */
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

    public CommitDetailsDTO() {
    }

    public CommitDetailsDTO(String commitId, String commitMessage, String patch) {
        this.commitId = commitId;
        this.commitMessage = commitMessage;
        this.patch = patch;
    }

    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }

    public String getPatch() {
        return patch;
    }

    public void setPatch(String patch) {
        this.patch = patch;
    }
}
