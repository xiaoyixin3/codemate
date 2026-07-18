package com.github.paicoding.forum.service.taskplan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.paicoding.forum.api.model.enums.ai.AISourceEnum;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanCreateReq;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanModelResponseDTO;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanModelStepDTO;
import com.github.paicoding.forum.api.model.vo.taskplan.AiTaskPlanStepCreateReq;
import com.github.paicoding.forum.core.util.Md5Util;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/** 负责解析模型任务规划 JSON 并转换为持久化请求。 */
@Service
public class AiTaskPlanStructuredService {
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private AiTaskPlanService aiTaskPlanService;

    public Long parseAndSave(Long userId, String chatId, String rawJson) throws Exception {
        String json = unwrapJson(rawJson);
        AiTaskPlanModelResponseDTO model = objectMapper.readValue(json, AiTaskPlanModelResponseDTO.class);
        validate(model);

        AiTaskPlanCreateReq req = new AiTaskPlanCreateReq();
        req.setTitle(model.getTitle().trim());
        req.setGoal(model.getGoal().trim());
        req.setChatId(chatId);
        req.setSourceAiType(AISourceEnum.DEEP_SEEK.getCode());
        req.setPlanKey(Md5Util.encode(userId + ":" + chatId + ":" + json));
        req.setExtra(json);
        req.setSteps(toSteps(model.getSteps()));
        return aiTaskPlanService.createPlan(userId, req);
    }

    public String toMarkdown(Long planId, AiTaskPlanModelResponseDTO model) {
        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(model.getTitle()).append("\n\n")
                .append("**计划 ID：** ").append(planId).append("\n\n")
                .append("### 目标\n").append(model.getGoal()).append("\n\n")
                .append("### 范围\n").append(model.getScope()).append("\n\n")
                .append("### 概要\n").append(model.getSummary()).append("\n\n")
                .append("### 步骤\n");
        for (AiTaskPlanModelStepDTO step : model.getSteps()) {
            sb.append("\n").append(step.getStepNumber()).append(". **").append(step.getTitle()).append("**\n")
                    .append("   - 说明：").append(step.getDescription()).append("\n")
                    .append("   - 预期输出：").append(step.getExpectedOutput()).append("\n")
                    .append("   - 验证方式：").append(step.getValidationMethod()).append("\n")
                    .append("   - 风险：").append(step.getRiskNote()).append("\n");
        }
        appendList(sb, "风险", model.getRisks());
        appendList(sb, "验证方式", model.getValidationMethods());
        appendList(sb, "验收标准", model.getAcceptanceCriteria());
        return sb.toString();
    }

    public AiTaskPlanModelResponseDTO parse(String rawJson) throws Exception {
        AiTaskPlanModelResponseDTO model = objectMapper.readValue(unwrapJson(rawJson), AiTaskPlanModelResponseDTO.class);
        validate(model);
        return model;
    }

    private String unwrapJson(String rawJson) {
        String json = StringUtils.trimToEmpty(rawJson);
        if (json.startsWith("```")) {
            int firstLine = json.indexOf('\n');
            int lastFence = json.lastIndexOf("```");
            if (firstLine < 0 || lastFence <= firstLine) {
                throw new IllegalArgumentException("JSON代码块不完整");
            }
            json = json.substring(firstLine + 1, lastFence).trim();
        }
        return json;
    }

    private void validate(AiTaskPlanModelResponseDTO model) {
        if (model == null || StringUtils.isBlank(model.getTitle()) || StringUtils.isBlank(model.getGoal())
                || StringUtils.isBlank(model.getScope()) || StringUtils.isBlank(model.getSummary())
                || model.getSteps() == null || model.getSteps().isEmpty() || model.getSteps().size() > 30
                || model.getRisks() == null || model.getValidationMethods() == null || model.getAcceptanceCriteria() == null) {
            throw new IllegalArgumentException("任务计划缺少必填字段或步骤数量非法");
        }
        checkLength(model.getTitle(), 128, "title");
        checkLength(model.getGoal(), 4000, "goal");
        checkLength(model.getScope(), 4000, "scope");
        checkLength(model.getSummary(), 4000, "summary");
        int expectedStepNumber = 1;
        for (AiTaskPlanModelStepDTO step : model.getSteps()) {
            if (step == null || step.getStepNumber() == null || StringUtils.isBlank(step.getTitle())
                    || StringUtils.isBlank(step.getDescription()) || StringUtils.isBlank(step.getExpectedOutput())
                    || StringUtils.isBlank(step.getValidationMethod()) || StringUtils.isBlank(step.getRiskNote())) {
                throw new IllegalArgumentException("任务步骤缺少必填字段");
            }
            if (!step.getStepNumber().equals(expectedStepNumber++)) {
                throw new IllegalArgumentException("任务步骤编号必须从1开始连续递增");
            }
            checkLength(step.getTitle(), 256, "step.title");
            checkLength(step.getDescription(), 8000, "step.description");
            checkLength(step.getExpectedOutput(), 8000, "step.expectedOutput");
            checkLength(step.getValidationMethod(), 8000, "step.validationMethod");
            checkLength(step.getRiskNote(), 8000, "step.riskNote");
        }
    }

    private List<AiTaskPlanStepCreateReq> toSteps(List<AiTaskPlanModelStepDTO> modelSteps) {
        List<AiTaskPlanStepCreateReq> steps = new ArrayList<>();
        for (AiTaskPlanModelStepDTO step : modelSteps) {
            AiTaskPlanStepCreateReq req = new AiTaskPlanStepCreateReq();
            req.setTitle(step.getTitle());
            req.setContent(step.getDescription());
            req.setExpectedOutput(step.getExpectedOutput());
            req.setRisk(step.getRiskNote());
            req.setVerificationMethod(step.getValidationMethod());
            steps.add(req);
        }
        return steps;
    }

    private void appendList(StringBuilder sb, String title, List<String> items) {
        sb.append("\n### ").append(title).append("\n");
        if (items != null) {
            for (String item : items) {
                sb.append("- ").append(item).append("\n");
            }
        }
    }

    private void checkLength(String value, int max, String field) {
        if (value != null && value.length() > max) {
            throw new IllegalArgumentException(field + "长度超限");
        }
    }
}
