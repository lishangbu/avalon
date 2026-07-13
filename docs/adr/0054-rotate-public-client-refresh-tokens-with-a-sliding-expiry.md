# Rotate public-client refresh tokens with a sliding expiry

公共 password client 的 access token 保持约三十分钟有效期，并签发每次成功刷新后重新获得八小时有效期的旋转 refresh token；一个 token family 从首次登录起最长存活七天，已轮换 token 再次出现时撤销整个 family。刷新不重建 Trainer Session；普通退出只撤销当前 family，修改或重置密码、账户禁用则撤销账户全部 family、Trainer Session 与 Presence，任何身份失效都不暂停 Match 的服务端期限。
