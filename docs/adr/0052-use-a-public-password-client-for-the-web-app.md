# Use a public password client for the Web app

首版 Web 管理端与 Player Area 继续使用 Avalon 自定义 password grant，不迁移 Authorization Code + PKCE；但浏览器不能保守 client secret，因此新增 `client_authentication_method = none` 的公共 Web Client，token 请求只提交 client_id、用户名、密码和 scope，不使用 HTTP Basic。管理 API 继续依赖账户 authority 强制授权，玩家 API 只认证；生产部署必须使用 HTTPS，未来若改用 PKCE 需以新 ADR 取代本决定。
