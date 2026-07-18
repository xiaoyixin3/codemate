# CodeMate 个人化配置清单

CodeMate 的展示品牌和本地上传路径已完成替换。以下配置与第三方账号、域名或敏感凭据绑定，必须在部署前由项目所有者填入自己的值；不要继续使用原项目的地址或密钥。

## 必须替换

| 配置项 | 位置 | 需要填写的内容 |
| --- | --- | --- |
| 生产域名 `host` | `resources-env/prod/application-config.yml` | 你的 HTTPS 域名，例如 `https://codemate.example.com` |
| 图片存储 `oss`、`cdn-host` | 各环境 `application-image.yml` | 你的对象存储/CDN 地址与存储桶配置 |
| 图片路径前缀 | 各环境 `application-image.yml` | 已设为 `codemate/`，用于后续新上传文件 |
| 联系二维码 | `application-config.yml`、生产 `application-config.yml` | 你的公众号或客服二维码地址 |
| 知识星球链接 | `zsxqUrl` | 你的会员页；未使用该功能时需在页面层隐藏入口 |
| 微信登录回调 | 各环境 `application-login.yml` | 你的 OAuth 回调地址 |
| 支付回调 | 各环境 `application-pay.yml` | 你的支付平台回调地址 |
| 域名白名单 | 各环境 `application-web.yml` | 你的域名与本地开发地址 |
| 数据库、Redis、邮件、AI Key | 对应环境配置 | 你的服务账号与密钥；不要提交真实密钥 |

## 初始内容

初始化 SQL 位于 `paicoding-web/src/main/resources/liquibase/data/`。在创建自己的演示环境前，应替换其中的原始文章、用户资料、分类、外链和运营素材，再在新的空数据库中执行 Liquibase 初始化。

## 上线前检查

1. 使用自己的 MySQL、Redis 与对象存储。
2. 将生产域名、登录回调、支付回调统一为自己的 HTTPS 域名。
3. 验证注册、登录、图片上传、文章发布和 AI 对话。
4. 删除测试账号、示例支付数据和原项目推广链接。
