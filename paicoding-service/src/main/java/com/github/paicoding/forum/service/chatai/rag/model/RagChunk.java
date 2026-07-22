package com.github.paicoding.forum.service.chatai.rag.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RagChunk {
    private String heading;
    private String contentType;
    private String content;
}
