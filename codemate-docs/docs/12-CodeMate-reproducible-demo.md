# CodeMate Agent 可复现演示

## 目标

这套演示把 CodeMate 的核心 Agent 能力串成一个可自动验收的闭环：

```text
站内文章检索与详情读取
  → Agent 工具步骤与引用证据
  → 结构化 Bug 诊断预览
  → 用户确认
  → 创建修复计划
  → 更新并完成计划步骤
  → 查询完整 Agent Run 轨迹
```

自动演示不调用 DeepSeek、OpenAI 或其他付费模型。测试会写入带随机隔离标识的数据，完成后按专用用户 ID 清理，不污染正常用户、文章和任务。

## 一条命令验收

前置条件：JDK 17、MySQL 8 和 Redis 已启动，`pai_coding` 数据库可用。脚本优先使用 PATH 中的 Maven，找不到时回退到仓库 Maven Wrapper。

PowerShell 中只为当前终端设置数据库环境变量，不要把真实密码写入仓库：

```powershell
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = '<本机数据库密码>'
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\run-codemate-demo.ps1
```

`ExecutionPolicy Bypass` 只作用于这次新建的 PowerShell 进程，不修改用户或系统的长期执行策略。

成功标志：Maven 输出 `BUILD SUCCESS`，脚本最后输出 `CodeMate Agent demonstration passed.`。

对应自动化场景为：

```text
codemate-web/src/test/java/com/github/paicoding/forum/test/codemate/CodeMateDemoScenarioTest.java
```

它会验证：

- 公共文章工具能先搜索、再读取同一篇已发布文章；
- 两次工具调用均进入 Agent Run 步骤，参数只保存哈希摘要；
- 引用证据关联真实存在的文章；
- Bug 诊断在确认前停留于 `WAITING_CONFIRMATION`；
- 重复确认返回同一个计划，不产生重复写入；
- 每个计划步骤经过 `TODO → IN_PROGRESS → COMPLETED`，执行结果和审计记录完整；
- 最终 Run 为 `COMPLETED`，能够同时查看工具步骤和证据；
- 其他用户不能读取该 Run；
- 演示产生的数据库记录在测试结束后被清理。

## 浏览器面试演示

自动验收通过后，可以启动 `QuickForumApplication`，登录测试账号并打开 `/chat`：

1. 选择“知识问答”，提问：`站内关于熔断和超时治理有哪些建议？`
2. 在右侧 Agent 工作台展示 Run 状态、文章引用和工具步骤。
3. 选择“Bug 诊断”，输入：`调用下游接口持续超时并耗尽线程，请基于站内证据诊断。`
4. 展示结构化原因假设、支持证据、置信度、验证步骤、修复建议和回归方案。
5. 点击“保存为修复计划”，说明二次确认、归属校验、幂等键和写审计设计。
6. 打开返回的 `/task-plan/{planId}`，依次开始步骤、填写执行结果并完成步骤。
7. 回到 Agent 工作台，打开对应 Run，展示 `COMPLETED` 状态、工具调用和引用证据。

浏览器流程会调用实际配置的模型。演示前应通过环境变量配置模型密钥，并准备至少一篇与问题匹配的已发布文章；不要在投屏、命令历史或日志中展示密钥。

## 面试讲解重点

- 安全：模型不能传入 `userId`，后端从可信会话绑定用户；写操作必须二次确认。
- 一致性：诊断确认、计划创建和审计记录位于事务边界内，并使用幂等键防止重复提交。
- 可追踪：Run、工具步骤、证据、Token 和终态均持久化，可解释一次 Agent 请求做了什么。
- 可测试：自动演示用确定性诊断 JSON 替代外部模型，使 CI 不依赖网络、额度或模型随机性。
- 可演进：线上仍使用真实模型和混合 RAG，离线场景负责验证业务闭环与安全约束。

## 故障排查

- `Access denied`：检查当前 PowerShell 的 `DB_USERNAME`、`DB_PASSWORD`。
- `Connection refused`：确认 MySQL 监听 `localhost:3306`，Redis 监听 `localhost:6379`。
- 表不存在：先启动一次应用或运行 Liquibase 兼容性测试，使数据库升级到当前版本。
- 演示中断：测试的 `@AfterEach` 仍会按随机演示用户 ID 清理已创建数据；再次执行脚本即可。
