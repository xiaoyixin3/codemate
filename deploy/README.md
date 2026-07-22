# CodeMate production deployment

Use an Ubuntu 22.04+ server with Docker Engine, Docker Compose plugin and Nginx.

```bash
cp deploy/.env.production.example deploy/.env
chmod 600 deploy/.env
# Edit deploy/.env and replace every placeholder.
docker compose --env-file deploy/.env -f deploy/docker-compose.prod.yml up -d --build
curl http://127.0.0.1:8080/
```

The repository also provides a manually triggered `Publish Container Image` GitHub Actions workflow. It builds the production JAR and publishes these immutable artifacts to GitHub Container Registry:

```text
ghcr.io/xiaoyixin3/codemate:<selected-tag>
ghcr.io/xiaoyixin3/codemate:sha-<commit-sha>
```

To deploy a published image instead of building on the server:

```bash
export APP_IMAGE=ghcr.io/xiaoyixin3/codemate:latest
docker login ghcr.io
docker compose --env-file deploy/.env -f deploy/docker-compose.prod.yml pull app
docker compose --env-file deploy/.env -f deploy/docker-compose.prod.yml up -d --no-build
curl --fail --retry 12 --retry-delay 5 http://127.0.0.1:8080/
```

Private packages require a GitHub token with `read:packages`. The legacy SSH script no longer contains a server address; set `DEPLOY_SSH_HOST` and `DEPLOY_WORK_DIR` explicitly if that deployment path is intentionally used.

Replace `example.com` in `nginx-codemate.conf`, enable it in Nginx and issue TLS with Certbot. Expose only ports 80 and 443 publicly; MySQL, Redis and actuator remain private. Liquibase runs automatically at startup.
