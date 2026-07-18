package com.github.paicoding.forum.service.chatai.rag.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RagSearchResult {
    private Long articleId;
    private Integer chunkIndex;
    private String title;
    private String content;
    private double score;
}
