# 运维与故障排查

## 日常检查

```bash
docker compose --env-file /opt/codemate/deploy/.env \
  -f /opt/codemate/deploy/docker-compose.prod.yml ps

docker logs --tail=200 deploy-app-1
sudo ss -lntp | grep -E ':80|:443|:8080'
df -h /
free -h
```

应用检查：

```bash
curl --fail http://127.0.0.1:8080/ >/dev/null
curl -I http://43.136.86.251/
```

## 数据备份

数据库备份：

```bash
mkdir -p "$HOME/codemate-backups"
backup_file="$HOME/codemate-backups/pai_coding-$(date +%Y%m%d-%H%M%S).sql"
docker exec deploy-mysql-1 sh -c \
  'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers --events pai_coding' \
  > "$backup_file"
gzip "$backup_file"
chmod 600 "$backup_file.gz"
```

生产配置备份：

```bash
config_backup="$HOME/codemate-backups/deploy-config-$(date +%Y%m%d-%H%M%S).tar.gz"
sudo tar -C /opt/codemate -czf "$config_backup" \
  deploy/docker-compose.prod.yml deploy/.env
sudo chown "$(id -u):$(id -g)" "$config_backup"
chmod 600 "$config_backup"
```

还应备份：

- `codemate-images` Volume
- Redis AOF 数据
- Nginx 站点配置和证书配置

建议每天自动备份，设置保留周期，并把副本同步到另一台机器或对象存储。只有实际完成恢复演练，备份才算有效。

## 常见问题

### GHCR 拉取缓慢或连接重置

现象：

```text
read: connection reset by peer
```

处理：

1. 不要反复删除已下载层。
2. 优先重试不可变的 `sha-<commit>` 镜像。
3. 网络持续失败时，改用“手动上传 JAR”部署。

### 容器启动但请求连接重置

新容器可能仍在启动。先查看日志：

```bash
docker logs -f --tail=200 deploy-app-1
```

确认出现 `Started QuickForumApplication` 后再执行 HTTP 检查。如果应用持续重启：

```bash
docker inspect deploy-app-1 --format '{{.State.Status}} {{.State.ExitCode}} {{.State.Error}}'
```

### MySQL 未就绪

```bash
docker inspect deploy-mysql-1 --format '{{json .State.Health}}'
docker logs --tail=200 deploy-mysql-1
```

不要删除 MySQL Volume 来解决普通启动问题。

### 内存不足

当前服务器内存约 2 GB，需要关注 Java 堆、Docker 和 MySQL 总占用：

```bash
docker stats --no-stream
free -h
```

应给 JVM 设置合理的容器内存比例，并避免长期依赖 Swap。

### Java 反射访问警告

如果出现 `SerializedLambda.capturingClass accessible` 警告但应用正常启动，可先记录并安排依赖升级。临时兼容参数：

```text
--add-opens java.base/java.lang.invoke=ALL-UNNAMED
```

优先升级触发警告的依赖，不应长期依赖开放 JDK 内部模块。

## 安全基线

- `.env` 权限保持为 `600`。
- 密钥不得写入仓库、镜像或截图。
- 定期轮换数据库、Redis、JWT 和模型服务密钥。
- 优先修复 GitHub Dependabot 的 Critical 与 High 告警。
- 仅开放 22、80、443 等必要端口，并限制 SSH 来源。
- 正式环境启用 HTTPS、安全 Cookie 和合理的 CORS 策略。

## 待建设监控

- JVM 堆、GC、线程和进程存活
- HTTP 延迟、吞吐量和 4xx/5xx
- MySQL、Redis 健康状态
- Docker 重启次数
- 磁盘容量、内存和 Swap
- 登录异常、Agent 调用失败和第三方模型错误
