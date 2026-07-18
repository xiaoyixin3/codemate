package com.github.paicoding.forum.service.chatai.agent;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AgentRequest {
    private AgentMode mode;
    private String input;
}
