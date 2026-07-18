# CodeMate production deployment

Use an Ubuntu 22.04+ server with Docker Engine, Docker Compose plugin and Nginx.

```bash
cp deploy/.env.production.example deploy/.env
chmod 600 deploy/.env
# Edit deploy/.env and replace every placeholder.
docker compose --env-file deploy/.env -f deploy/docker-compose.prod.yml up -d --build
curl http://127.0.0.1:8080/
```

Replace `example.com` in `nginx-codemate.conf`, enable it in Nginx and issue TLS with Certbot. Expose only ports 80 and 443 publicly; MySQL, Redis and actuator remain private. Liquibase runs automatically at startup.
