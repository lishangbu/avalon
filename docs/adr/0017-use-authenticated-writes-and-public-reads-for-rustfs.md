---
status: accepted
---

# 采用 RustFS 认证写入与公开读取

## 背景

当前部署让 RustFS 直接承担静态对象分发：对象读取无需认证，写入仍必须经过受控服务账号和短期预签名请求。继续签发 GET 签名会引入无意义的到期时间、查询参数和缓存碎片。

## 决策

- 单一 Bucket 只向匿名调用者授予 `s3:GetObject`；匿名列举、上传、覆盖和删除一律拒绝。
- 后端使用最小权限 RustFS 服务账号执行 Bucket readiness、待确认对象读取和删除；客户端写入只使用短期预签名 PUT，并绑定 MIME、字节数、SHA-256 与 `If-None-Match: *`。
- Ready Asset 返回由统一 Endpoint、Bucket 和对象键组成的稳定公开 URL，不生成 GET 签名，不携带查询参数，也不声明读取过期时间。Endpoint 必须同时可被后端与读取客户端访问，不再配置独立 Public Endpoint。
- 首套授权资料图片保留 `pokedex/images/official|items/<原始文件名>` 来源层级；完整解码确认实际为 WebP 的 `.png` 来源改用 `.webp` 最终扩展名。图片字节由受控 RustFS 备份恢复，SQL 基线固化对象键、媒体类型、字节数、SHA-256、尺寸及资料引用；恢复过程不允许同名覆盖冲突。
- PostgreSQL 中的 Asset 归属、Pending/Ready 状态和资料引用仍是业务权威边界。公开读取只开放对象字节，不开放 Bucket 列举、数据库元数据、对象修改或管理 API。
- readiness 必须确认服务账号可以访问单一 Bucket、匿名 GetObject 已开放且匿名列举仍被拒绝。真实 RustFS 集成测试必须额外证明匿名 PUT 被拒绝。
- 部署顺序固定为恢复 `pokedex/images/**`、执行 SQL 基线、核对对象与数据库引用、退出停机维护。部署不访问外部资料站，也不依赖仓库内图片清单或同步工具。

## 后果

浏览器、CDN 和游戏客户端可以稳定缓存对象地址，不依赖管理会话或刷新下载签名。知道完整对象地址的调用者可以读取对象，因此对象键不能承载秘密；需要保密的未来对象类型必须使用独立 Bucket 和新的访问决策，不能复用本策略。
