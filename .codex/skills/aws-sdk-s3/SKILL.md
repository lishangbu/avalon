---
name: aws-sdk-s3
description: "AWS SDK for Java v2 S3 work in Avalon. Use for S3 clients, buckets, object keys, uploads, downloads, streaming, presigned requests, endpoint overrides, credentials, S3 abstractions, Spring Boot auto-configuration, starters, or S3 integration tests."
---

# AWS SDK S3

## 修改前

- 检查 `s3-core` 的公共抽象、自动配置条件、starter 依赖和现有测试替身。
- 从 version catalog 确认 AWS SDK v2 版本；不引入 v1 API。
- 涉及 Spring 配置或 Gradle 边界时同时加载对应 skill。

## 项目要求

- 业务调用依赖项目 S3 抽象，不在业务模块散落 AWS SDK client 调用。
- 对象 key 使用明确类型并校验空值、前导分隔符、路径穿越与编码边界。
- 上传与下载保持流式处理；除明确小对象外不把完整内容加载到内存。
- client、region、endpoint override 与 credentials 由自动配置注入；不在源码写入密钥或环境特例。
- 自动配置使用条件 bean，并允许消费者替换 client 或高层协作方。
- 异常转换成稳定项目错误，不向用户暴露 bucket 内部结构、签名或凭据。
- presigned URL 明确有效期、HTTP 方法和允许头；不生成无边界长期链接。

## 测试驱动

- 公共命令和 key 规则先写单元测试。
- 自动配置使用 context runner 或等价上下文测试覆盖启用、禁用与覆盖 bean。
- 集成测试使用本地兼容服务或受控替身；默认不访问真实云账户。

## 完成标准

- `s3-core`、autoconfigure 和 starter 的受影响测试通过。
- 公共 API 无 AWS SDK v1 泄漏、无秘密日志、无未关闭流。
- 配置 metadata、默认值和条件装配一致。
- 依赖变化额外运行 Gradle 完整测试。
