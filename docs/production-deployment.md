# 生产部署

当前生产环境使用 Ubuntu、Docker Compose、MySQL、Redis 和 Nginx。应用容器仅绑定 `127.0.0.1:8080`，公网流量由 Nginx 通过 80/443 端口转发。

## 服务器目录

推荐目录：

```text
/opt/codemate/
├── deploy/
│   ├── .env
│   ├── docker-compose.prod.yml
│   └── docker-compose.manual.yml
└── manual-release/
    └── codemate.jar
```

## 方式一：Docker Compose 构建

```bash
cd /opt/codemate
cp deploy/.env.production.example deploy/.env
chmod 600 deploy/.env
```

编辑 `.env`，替换所有示例值：

```text
DB_USERNAME
DB_PASSWORD
MYSQL_ROOT_PASSWORD
REDIS_PASSWORD
JWT_SECRET
CODEMATE_SECURITY_SALT
SITE_HOST
STATIC_RESOURCE_HOST
```

启动：

```bash
docker compose --env-file deploy/.env \
  -f deploy/docker-compose.prod.yml up -d --build

curl --fail --retry 12 --retry-delay 5 http://127.0.0.1:8080/
```

## 方式二：从 GHCR 发布

在 GitHub Actions 手动运行 `Publish Container Image`，镜像格式为：

```text
ghcr.io/xiaoyixin3/codemate:<tag>
ghcr.io/xiaoyixin3/codemate:sha-<commit-sha>
```

服务器部署：

```bash
export APP_IMAGE=ghcr.io/xiaoyixin3/codemate:latest
docker login ghcr.io
docker compose --env-file deploy/.env \
  -f deploy/docker-compose.prod.yml pull app
docker compose --env-file deploy/.env \
  -f deploy/docker-compose.prod.yml up -d --no-build
```

国内网络访问 GHCR 可能出现下载慢或 `connection reset by peer`，此时使用下面的 JAR 发布方式。

## 方式三：手动上传 JAR

本地构建：

```bash
mvn -pl codemate-web -am clean package -DskipTests -Pprod
```

上传文件：

```text
codemate-web/target/codemate-web-0.0.1-SNAPSHOT.jar
```

服务器保存为：

```bash
sudo mkdir -p /opt/codemate/manual-release
sudo mv /home/ubuntu/upload/codemate-web-0.0.1-SNAPSHOT.jar \
  /opt/codemate/manual-release/codemate.jar
sudo chown ubuntu:ubuntu /opt/codemate/manual-release/codemate.jar
```

在 `/opt/codemate/manual-release/Dockerfile` 写入：

```dockerfile
FROM deploy-app:latest
USER root
COPY codemate.jar /app/codemate.jar
RUN chown codemate:codemate /app/codemate.jar
USER codemate
```

`deploy-app:latest` 是服务器已有的旧应用基础镜像，复用其中的 JRE、用户和启动命令，避免重新拉取大镜像。构建新版本时使用不可变标签：

```bash
cd /opt/codemate/manual-release
docker build --pull=false -t codemate:manual-<git-sha> .
```

在 `/opt/codemate/deploy/docker-compose.manual.yml` 写入：

```yaml
services:
  app:
    image: codemate:manual-<git-sha>
```

部署：

```bash
cd /opt/codemate/deploy
docker compose --env-file .env \
  -f docker-compose.prod.yml \
  -f docker-compose.manual.yml \
  up -d --no-build

docker compose --env-file .env \
  -f docker-compose.prod.yml \
  -f docker-compose.manual.yml ps

docker compose --env-file .env \
  -f docker-compose.prod.yml \
  -f docker-compose.manual.yml logs --tail=200 app

curl -I http://127.0.0.1:8080/
```

当前服务器已通过该方式部署 `codemate:manual-030aa064`。

## Nginx 与 HTTPS

仓库提供 `deploy/nginx-codemate.conf`。配置域名后：

```bash
sudo nginx -t
sudo systemctl reload nginx
sudo certbot --nginx -d your-domain.example
```

公网仅开放 80 和 443。MySQL、Redis、Actuator 和应用 8080 端口不得直接暴露。

## 回滚

将 `docker-compose.manual.yml` 中的镜像改回上一个已验证标签，然后重新执行：

```bash
docker compose --env-file .env \
  -f docker-compose.prod.yml \
  -f docker-compose.manual.yml \
  up -d --no-build
```

回滚前不要删除旧镜像、数据库卷和图片卷。
