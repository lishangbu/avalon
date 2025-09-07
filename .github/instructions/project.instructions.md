---
applyTo: "**"
---

# 项目开发规范

- 你是一位后端开发专家，精通后端架构

## 技术栈

- 框架: Spring Boot 3
- ORM框架: Spring Data Jpa
- 代码校验与格式化: spotless
- 开发语言: Java
- 项目管理工具: maven
- APT辅助工具: lombok
- 日志: slf4j + logback
- 安全: spring-oauth2-authorization-server 
- 数据库连接池: HikariCP
- 缓存: caffeine
- 文档: springdoc-openapi + swagger-ui
- 单元测试: JUnit + Mockito
- 持续集成: GitHub Actions

## 目录结构

```sh
# avalon
├── avalon-application # 应用启动器，包含各个服务的实现
│   ├── avalon-admin-server # 管理后台服务
│   │   └── src # 管理后台服务的源码
│   └── avalon-standalone-server # 独立服务，all in one
│       └── src # 独立服务的源码
├── avalon-dependencies # 依赖管理，统一定义项目中依赖的版本
├── avalon-extensions # 各种第三方扩展
│   ├── avalon-dufs-spring-boot-starter # DUFS 文件存储支持
│   │   └── src # DUFS 支持库的源码
│   └── avalon-ip2location-spring-boot-starter # IP2Location 数据库支持
│       └── src # IP2Location 支持库的源码
├── avalon-modules # 业务模块
│   ├── avalon-authorization # 授权模块
│   │   └── src # 授权模块的源码
│   └── avalon-dataset # 数据集模块
│       ├── avalon-dataset-model # 数据集模块的模型
│       └── avalon-dataset-repository # 数据集模块的仓库
├── avalon-parent # 父工程，定义整个工程的一些公共行为
├── avalon-support # 无第三方依赖的各种支持库
│   ├── avalon-hibernate-support # hibernate 支持库
│   │   └── src # hibernate 支持库的源码
│   ├── avalon-json-support  # JSON 支持库
│   │   └── src # JSON 支持库的源码
│   ├── avalon-oauth2-support # OAuth2 支持库
│   │   ├── avalon-oauth2-authorization-server # OAuth2 授权服务器
│   │   ├── avalon-oauth2-common # OAuth2 公共模块
│   │   └── avalon-oauth2-resource-server # OAuth2 资源服务器
│   ├── avalon-poke-api-support # PokeAPI 支持库
│   │   └── src # PokeAPI 支持库的源码
│   └── avalon-web-support
│       └── src # Web 支持库的源码
└── scripts # 存放一些辅助脚本
│   ├── ip-data-downloader.sh # IP 数据库下载脚本
│   └── rsa-key-pair.sh # RSA 密钥对生成脚本
│   └── tree-print.sh # 树形目录生成脚本
```

- 保持目录结构清晰，遵循现有目录规范

## 代码

- 编写整洁不冗余、可读性强的代码，始终提取共用逻辑
- 编写对开发者友好的注释
- 代码必须能够立即运行，包含所有必要的导入和依赖
- 建议参考项目已有代码的编码风格

## 代码检查

- 使用 spotless 进行代码校验与格式化

## 其他

- 优先使用现有第三方依赖，避免重新发明轮子