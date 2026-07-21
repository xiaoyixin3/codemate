# CodeMate Agent 与站内知识 RAG

## 能力范围

CodeMate 目前包含普通编程问答、Bug 排查 Agent、任务规划 Agent 和站内知识问答 Agent。任务规划结果使用结构化 JSON 解析并持久化；站内知识问答会把已发布文章切分、生成 Embedding、执行余弦相似度 Top-K 检索，再把命中的片段注入 DeepSeek 系统 Prompt。

普通聊天 Agent 还可以通过安全只读工具先搜索已发布文章，再按返回的文章 ID 继续读取详情，也可以按分类或标签检索。登录用户可查询自己的任务计划和计划详情；模型不接收 `userId`，身份始终来自后端可信会话绑定。

## 配置

DeepSeek 负责流式回答，Embedding 服务需要兼容 OpenAI `/embeddings` 协议。不要把真实密钥写入仓库。

```text
DEEPSEEK_API_KEY=your-deepseek-key
RAG_ENABLED=true
EMBEDDING_API_HOST=https://api.openai.com/v1
EMBEDDING_API_KEY=your-embedding-key
EMBEDDING_MODEL=text-embedding-3-small
```

如果使用其他 OpenAI 兼容 Embedding 服务，只需替换 Host、Key 和模型名。未配置 RAG 时，普通聊天、Bug 排查和任务规划不受影响；选择“站内知识问答”会返回明确的配置提示。

## 建立知识索引

启动应用并以管理员身份登录后调用：

```text
POST /api/admin/ai/rag/index?articleId={articleId}
POST /api/admin/ai/rag/index-all
```

仅已发布且未删除的文章会进入知识库。重复索引同一篇文章时会用新分块和新向量原子替换旧记录。

可使用以下接口检查 Top-K 结果：

```text
GET /api/admin/ai/rag/search?question={question}
```

## 问答流程

1. 用户在 AI 聊天页选择“站内知识问答”。
2. 后端使用同一 Embedding 模型向量化问题。
3. 从 `ai_knowledge_chunk` 读取候选向量并计算余弦相似度。
4. 按阈值过滤并选取 Top-K 片段。
5. 将片段以 `<knowledge_context>` 边界注入 DeepSeek Prompt。
6. 模型基于站内资料回答，并使用 `[文章#ID《标题》]` 标注来源。

默认参数位于各环境的 `application-ai.yml`，包括分块大小、重叠长度、Top-K、候选上限和最低相似度。调整 Embedding 模型后应重新执行全量索引，避免不同维度的向量混用。

## Bug 诊断检索闭环

`BUG_DIAGNOSIS` 同时启用只读文章工具和 RAG。模型先检索可核验资料，再输出固定 JSON。写工具在诊断生成阶段按风险等级过滤，因此模型无法在预览前创建或修改计划。检索到的分块继续写入 Agent Run evidence；诊断 JSON 中的文章依据用于本阶段的可读预览，后续 RAG 阶段会将其升级为强约束 citations。

## 混合检索 v2

第四阶段将原有单一向量候选扫描升级为混合召回：

1. Markdown 感知切块优先按标题和段落组织，并尽量保持 fenced code block 与表格完整；
2. 分块保存小标题、分类、标签、内容类型、文章更新时间、SHA-256 内容哈希、Embedding 模型/维度、索引版本和索引时间；
3. 向量候选与 MySQL `LIKE` 关键词候选分别召回，按文章 ID 与分块序号合并去重；
4. 使用向量分、关键词分、新鲜度及标题/小标题命中奖励进行轻量重排；
5. LangChain4j 仅接收本次 Top-K 分块，聊天响应的 `citations` 也只由这些分块生成。

文章 `ONLINE` 事件在事务提交后异步增量索引；`REVIEW`、`OFFLINE` 和 `DELETE` 会移除索引。索引时逐块比较内容哈希，未变化分块直接复用已有向量，整篇未变化时不请求 Embedding。

新增配置：

```text
RAG_INDEX_VERSION=v2-hybrid
RAG_KEYWORD_CANDIDATES=200
RAG_VECTOR_CANDIDATES=1000
RAG_VECTOR_WEIGHT=0.65
RAG_KEYWORD_WEIGHT=0.25
RAG_FRESHNESS_WEIGHT=0.10
```

管理员调试接口 `GET /api/admin/ai/rag/search?question=...&limit=10` 返回向量分、关键词分、新鲜度分、最终分和排序原因；`GET /api/admin/ai/rag/status` 返回当前索引版本、候选上限和有效分块数。运行指标包括检索耗时、两路候选规模、结果数、索引更新/跳过次数和实际生成向量数。

固定 50 条评测数据及结果位于 `paicoding-service/src/test/resources/codemate/rag-eval-v1.csv` 和 `codemate-docs/evaluation/rag-v2-report.*`。
