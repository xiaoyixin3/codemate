# CodeMate Agent 自动评测体系

## 设计目标

第七阶段建立不依赖人工逐条阅读的离线评测套件。生产代码只包含指标计算、报告输出和版本比较逻辑；固定测试数据放在 `paicoding-service/src/test/resources/codemate`，不会打包成业务数据或触发线上模型调用。

统一报告覆盖四类能力：

- RAG：Recall@3、Recall@5、MRR、引用合法率、无证据拒答率、检索耗时；
- 任务规划：JSON 解析率、步骤完整率、验证步骤覆盖率、状态机合法率；
- 工具调用：工具选择、参数合法、执行成功、重复调用、越权拦截；
- Bug 诊断：假设、证据、验证步骤、回归方案及完整诊断率。

## 代码结构

```text
service/chatai/eval/
├── AgentOfflineEvaluationSuite       # 统一执行入口
├── TaskPlanningOfflineEvaluator      # 任务规划指标
├── ToolCallingOfflineEvaluator       # 工具调用指标
├── BugDiagnosisOfflineEvaluator      # Bug 诊断指标
├── AgentEvaluationReportWriter       # JSON 与 Markdown
└── AgentEvaluationComparator         # 基线/候选版本差异与回归项
```

原有 `RagOfflineEvaluator` 保持兼容，并增加无证据拒答率。无证据样本不进入 Recall 和 MRR 分母，避免把正确拒答错误计为检索失败。

## 可重复性与外部模型

```text
AGENT_EVAL_RANDOM_SEED=20260722
AGENT_EVAL_EXTERNAL_MODEL_ENABLED=false
```

默认值禁止外部模型评测，普通 `mvn test` 不需要 API Key，也不会产生付费请求。只有显式设置 `AGENT_EVAL_EXTERNAL_MODEL_ENABLED=true` 后，后续接入的真实模型数据采集器才允许运行；离线指标计算器本身始终只消费已经固定的输入和输出。

## 版本比较

`AgentEvaluationComparator` 将基线和候选报告展开为稳定的 `分组.指标` 名称并计算候选减基线的差值。准确率、成功率和完整率下降会标记为回归；耗时、重复率和错误率上升会标记为回归。阈值由调用方传入，可用于 CI 门禁。

机器报告：[agent-eval-v1-report.json](../evaluation/agent-eval-v1-report.json)

人工报告：[agent-eval-v1-report.md](../evaluation/agent-eval-v1-report.md)

当前样本中包含故意失败的数据，用于证明评测器能够识别回归，因此 0.5 或 0.6667 等数值不能解释为当前线上模型真实质量。真实模型基准需要在独立环境采集后再形成 v2 报告。

## 验证命令

```bash
mvn -pl paicoding-service -am "-Dtest=RagOfflineEvaluatorTest,AgentOfflineEvaluationSuiteTest,EvaluationPropertiesTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl paicoding-service -am test
mvn install -DskipTests=true
```
