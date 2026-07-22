package com.github.paicoding.forum.service.chatai.rag.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RagSearchResult {
    private Long articleId;
    private Integer chunkIndex;
    private String title;
    private String heading;
    private String content;
    private String contentType;
    private String category;
    private String tags;
    private double vectorScore;
    private double keywordScore;
    private double freshnessScore;
    private double score;
    private List<String> rankingReasons = new ArrayList<>();

    public RagSearchResult() {
    }

    public RagSearchResult(Long articleId, Integer chunkIndex, String title, String content, double score) {
        this.articleId = articleId;
        this.chunkIndex = chunkIndex;
        this.title = title;
        this.content = content;
        this.vectorScore = score;
        this.score = score;
    }
}
