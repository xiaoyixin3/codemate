package com.github.paicoding.forum.api.model.vo.chat;

import lombok.Data;

import java.io.Serializable;

@Data
public class AiCitationVo implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer citationIndex;
    private Long articleId;
    private Integer chunkIndex;
    private String title;
    private String heading;
    private String excerpt;
    private Double relevance;
}
