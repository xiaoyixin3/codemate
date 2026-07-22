# CodeMate Agent 离线评测报告

- 版本：`agent-eval-v1`
- 固定随机种子：`20260722`
- 外部模型：禁用

## RAG

用例数：52

| 指标 | 数值 |
|---|---:|
| recallAt3 | 1.0000 |
| recallAt5 | 1.0000 |
| mrr | 1.0000 |
| citationLegalRate | 1.0000 |
| noEvidenceRefusalRate | 0.5000 |
| averageLatencyMillis | 23.2500 |

## 任务规划

用例数：2

| 指标 | 数值 |
|---|---:|
| jsonParseSuccessRate | 0.5000 |
| stepCompletenessRate | 0.5000 |
| verificationStepRate | 0.5000 |
| stateMachineLegalRate | 0.5000 |

## 工具调用

用例数：3

| 指标 | 数值 |
|---|---:|
| toolSelectionAccuracy | 0.6667 |
| parameterValidityRate | 0.6667 |
| invocationSuccessRate | 0.6667 |
| duplicateInvocationRate | 0.3333 |
| unauthorizedBlockRate | 1.0000 |

## Bug 诊断

用例数：2

| 指标 | 数值 |
|---|---:|
| jsonParseSuccessRate | 0.5000 |
| hypothesisPresenceRate | 0.5000 |
| evidencePresenceRate | 0.5000 |
| verificationPresenceRate | 0.5000 |
| regressionPresenceRate | 0.5000 |
| completeDiagnosisRate | 0.5000 |

> 本报告包含故意设置的失败样本，用于验证指标能够识别非法 JSON、非法状态转换、错误工具选择、重复调用和不完整诊断；它不是生产模型质量基准。
