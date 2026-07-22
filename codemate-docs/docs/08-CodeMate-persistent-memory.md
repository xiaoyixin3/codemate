# CodeMate 持久化记忆

## 三层结构

第五阶段把进程内 `MessageWindowChatMemory` 改造成三层上下文：

1. `ai_chat_memory.messages_json` 保存最近 N 条 LangChain4j 消息，是服务重启和多实例恢复的权威来源；
2. 消息被窗口淘汰时，确定性摘要器把内容合并到 `conversation_summary`，摘要长度由配置限制；
3. `ai_user_technical_memory` 保存用户主动维护的长期工程偏好，仅允许常用语言、技术栈、回答风格和当前项目四类信息。

请求上下文按以下顺序组装：系统指令、用户技术偏好、会话摘要、最近消息、当前问题。偏好和摘要均标记为不可信数据，不能覆盖系统指令。

## 配置

```text
AGENT_MEMORY_MAX_MESSAGES=20
AGENT_MEMORY_SUMMARY_MAX_CHARS=4000
AGENT_MEMORY_PREFERENCE_MAX_ITEMS=20
```

最近消息和摘要写入 MySQL，因此应用实例不需要会话粘滞。现有 Redis 聊天列表继续用于页面历史兼容，不再作为 Agent Memory 的唯一来源。

## 长期记忆接口

接口均要求登录，用户 ID 只从服务端可信会话获取：

```text
GET    /chat/api/memories
POST   /chat/api/memories
PUT    /chat/api/memories/{memoryId}
DELETE /chat/api/memories/{memoryId}
```

`memoryType` 只允许 `LANGUAGE`、`TECH_STACK`、`RESPONSE_STYLE`、`CURRENT_PROJECT`。记录包含来源、置信度、创建/更新时间和可选过期时间。写入会拒绝密码、API Key、密钥、Cookie、Token 和私钥模式。

删除会话时同步删除 Redis 消息列表、`user_ai_history`、该会话所有 Agent 短期记忆和摘要，以及来源指向该会话的长期记忆。长期记忆的查看、修改和删除条件始终包含当前用户 ID。

## 验证

定向测试不调用外部模型：

```bash
mvn -pl codemate-service -am "-Dtest=PersistentChatMemoryStoreTest,TechnicalMemoryServiceTest,MemoryContextAssemblerTest,ConversationMemoryCleanupServiceTest,CodeMateChatMemoryProviderTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

覆盖进程重建恢复、跨实例读取、窗口摘要、上下文顺序、敏感信息拒绝、用户隔离和会话隐私清理。
