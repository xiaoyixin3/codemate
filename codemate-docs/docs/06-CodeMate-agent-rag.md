# CodeMate Agent 与站内知识 RAG

## 能力范围

CodeMate 目前包含普通编程问答、Bug 排查 Agent、任务规划 Agent 和站内知识问答 Agent。任务规划结果使用结构化 JSON 解析并持久化；站内知识问答会把已发布文章切分、生成 Embedding、执行余弦相似度 Top-K 检索，再把命中的片段注入 DeepSeek 系统 Prompt。

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
