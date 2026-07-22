package com.github.paicoding.forum.api.model.vo.memory;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class TechnicalMemoryVo implements Serializable {
    private Long id;
    private String memoryType;
    private String content;
    private String sourceType;
    private String sourceRef;
    private Double confidence;
    private Date createTime;
    private Date updateTime;
    private Date expiresAt;
}
