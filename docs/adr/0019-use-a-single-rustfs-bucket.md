# 使用单一 RustFS Bucket

## 状态

已接受。

## 背景

发布资源、Asset 和待确认对象使用相同的安全策略：完整对象键允许匿名 `GetObject`，列举、上传、覆盖和删除必须认证。单一 Bucket 可以减少部署配置、readiness、备份和策略漂移风险。

## 决策

- 每个 Avalon 部署只配置一个 `objectStorage.bucket`。
- 发布资源、Asset 和待确认对象均保存在该 Bucket，并通过稳定对象键前缀与 Snowflake Identifier 避免命名冲突。
- Bucket 只向匿名调用者授予 `s3:GetObject`；匿名列举、上传、覆盖和删除仍被拒绝。
- 服务账号、readiness、备份、恢复和真实 RustFS 集成测试只针对该 Bucket。

## 后果

部署只需创建、授权、监控和备份一个 Bucket。对象生命周期与可引用性仍由 PostgreSQL 中的 Asset 状态和资料引用控制，Bucket 拓扑不承担业务状态隔离。未来若出现必须保密或需要不同保留策略的对象类型，应以新的安全决策建立独立 Bucket，不预留当前不使用的配置。
