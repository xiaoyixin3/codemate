# 本地开发

## 环境要求

- JDK 17
- Maven 3.8+
- MySQL 8
- Redis 7
- Node.js 22（仅运行 Playwright E2E 时需要）

## 准备数据库

```sql
CREATE DATABASE pai_coding
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

开发环境配置位于：

```text
codemate-web/src/main/resources-env/dev/
```

主要文件：

- `application-dal.yml`：MySQL、Redis
- `application-image.yml`：图片上传
- `application-web.yml`：Web 配置

不要把真实密码、JWT 密钥或第三方模型密钥提交到 Git。

## 构建

在仓库根目录执行：

```bash
mvn clean install -DskipTests=true
```

仅构建可执行 Web 模块及其依赖：

```bash
mvn -pl codemate-web -am clean package -DskipTests -Pprod
```

产物：

```text
codemate-web/target/codemate-web-0.0.1-SNAPSHOT.jar
```

## 启动

启动 MySQL 和 Redis 后，运行：

```text
com.github.paicoding.forum.web.QuickForumApplication
```

首次启动时 Liquibase 会自动创建并升级数据库结构。

## 测试

运行全部 Maven 测试：

```bash
mvn test
```

运行单个测试类或方法：

```bash
mvn test -Dtest=ClassName
mvn test -Dtest=ClassName#methodName
```

Agent 演示闭环：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\run-codemate-demo.ps1
```

Agent 工作台 E2E：

```bash
npm ci
npx playwright install chromium
npm run test:e2e
```

## 提交前检查

```bash
mvn test
npm run test:e2e
docker compose --env-file deploy/.env.production.example \
  -f deploy/docker-compose.prod.yml config --quiet
```

Windows 环境如果未安装 Docker，可以让 GitHub Actions 完成 Compose 配置校验。
