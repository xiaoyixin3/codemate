# CodeMate RAG v2 离线对比

数据集：`paicoding-service/src/test/resources/codemate/rag-eval-v1.csv`，共 50 条固定站内技术问答。评测不调用外部模型，用于检索排序回归和报告链路验证。

| 策略 | Recall@3 | Recall@5 | MRR | 引用合法率 | 平均耗时 |
| --- | ---: | ---: | ---: | ---: | ---: |
| 旧 MySQL 向量候选排序 | 0.60 | 0.80 | 0.416667 | 1.00 | 18 ms |
| 关键词 + 向量 + 新鲜度重排 | 1.00 | 1.00 | 1.00 | 1.00 | 24 ms |

这些耗时来自固定夹具，只验证报告计算，不代表生产性能。运行时真实候选规模和耗时由 `codemate.rag.candidates`、`codemate.rag.retrieval.duration` 等 Micrometer 指标记录；接入真实文章和 Embedding 服务后应另做生产等价压测。
