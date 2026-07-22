package com.github.paicoding.forum.api.model.vo.memory;

import lombok.Data;

import java.util.Date;

@Data
public class TechnicalMemoryCreateReq {
    private String memoryType;

    private String content;

    private Double confidence;

    private Date expiresAt;
}
