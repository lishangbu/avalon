<a id="readme-top"></a>

<div align="center">

# 阿瓦隆 · Avalon

**面向桌面与移动浏览器的回合制文字冒险游戏**

探索地图、培养幻兽，与其他玩家组队战斗、采集、交易和制造。

[项目文档](docs/implementation-progress.md) · [报告问题](https://github.com/lishangbu/avalon/issues) · [提出建议](https://github.com/lishangbu/avalon/issues/new)

[![Docker 发布](https://github.com/lishangbu/avalon/actions/workflows/docker-publish.yaml/badge.svg?branch=main)](https://github.com/lishangbu/avalon/actions/workflows/docker-publish.yaml)
[![Docker Hub](https://img.shields.io/docker/v/slf4j/avalon/latest?label=Docker%20Hub&logo=docker&style=flat-square)](https://hub.docker.com/r/slf4j/avalon)
![Java](https://img.shields.io/badge/Java-27-ED8B00?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.1-6DB33F?logo=springboot&logoColor=white&style=flat-square)
![Vue](https://img.shields.io/badge/Vue-3-4FC08D?logo=vuedotjs&logoColor=white&style=flat-square)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18.6-4169E1?logo=postgresql&logoColor=white&style=flat-square)
[![License](https://img.shields.io/badge/License-AGPL--3.0-blue?style=flat-square)](LICENSE)

</div>

<details>
<summary>目录</summary>

- [项目介绍](#项目介绍)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [开发与测试](#开发与测试)
- [项目结构](#项目结构)
- [文档导航](#文档导航)
- [路线图](#路线图)
- [参与贡献](#参与贡献)
- [许可证](#许可证)
- [联系与致谢](#联系与致谢)

</details>

## 项目介绍

Avalon 将地图探索、剧情任务、幻兽培养和玩家协作放进浏览器。前端采用 Ionic Vue 的 iOS 视觉模式，后端作为模块化单体运行；生产构建将前端静态资源打入同一个可执行 JAR。

当前包含：

- **冒险与战斗**：四种人物职业、BQ001～BQ074剧情、回合制战斗、捕捉、技能学习与进化。
- **幻兽培养**：28种基础幻兽、共48种运行形态，以及编队、仓库、图鉴和随机遗忘。
- **玩家协作**：三人林间兽栏副本、好友邀请、招募匹配、掉线补员、装备与套装奖励。
- **生活技能**：种植、单指钓鱼、在线派宠采集及设施制造；十级熟练度、五档工具、专业分工与材料交易。
- **成长与交易**：人物上限50级，生活系统6级、市场11级、强化30级开放。

现有主线可自然推进至16级，16～50级已有野外成长、资源及制造装备，后续完整剧情与副本仍在开发。150形态设计规划不等于全部可获得。当前范围和剩余工作以[实现进度](docs/implementation-progress.md)为准。

浏览器手机视口验证已开展，实体手机手感及长时间真人体验尚待验证。

## 技术栈

<p>
  <img src="https://skillicons.dev/icons?i=java,spring,postgres,vue,ts,vite,maven,nodejs,docker,githubactions" alt="Java、Spring、PostgreSQL、Vue、TypeScript、Vite、Maven、Node.js、Docker、GitHub Actions" />
</p>

| 层次 | 技术 |
| --- | --- |
| 后端 | Java 27、Spring Boot、Spring Security、Spring Data JPA |
| 数据 | PostgreSQL、Liquibase、JSON与JSON Schema内容校验 |
| 前端 | Vue 3、TypeScript、Vite、Ionic Vue、TanStack Vue Query |
| 构建 | Maven Wrapper、frontend-maven-plugin、npm锁文件 |
| 验证 | JUnit、Testcontainers、Playwright |

后端采用分层与轻量CQRS，命令和查询共用数据库。当前调度面向单实例，部署边界见[后端结构](docs/backend-structure.md)和[部署说明](docs/docker-deployment.md)。

## 快速开始

### 环境要求

- JDK 27；建议用 [mise](https://mise.jdx.dev/) 安装项目 `mise.toml` 固定的版本。
- PostgreSQL 18.6；本地开发需自行准备数据库，集成测试通过Docker启动隔离数据库。
- Docker；容器部署还需Docker Compose或Podman及Compose执行器。
- 独立开发前端时需要Node.js 26.11.1；Maven完整构建会下载固定的Node.js与npm，无需全局安装Maven。

### 安装与运行

```bash
git clone git@github.com:lishangbu/avalon.git
cd avalon
mise install
mise exec -- ./mvnw -pl avalon-app -am -DskipTests package
```

准备独立的PostgreSQL 18.6开发数据库，通过环境变量配置连接信息后启动（替换下方地址、账号和密码）：

```bash
export SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/avalon'
export SPRING_DATASOURCE_USERNAME='你的开发数据库账号'
export SPRING_DATASOURCE_PASSWORD='你的开发数据库密码'
mise exec -- java @avalon-app/src/main/docker/jvm.options \
  -jar avalon-app/target/avalon-app-0.1.0-SNAPSHOT.jar \
  --spring.profiles.active=local
```

打开 [localhost:8080](http://localhost:8080)，注册账号并创建角色。环境变量覆盖 `application-local.yaml` 的数据库设置。`local`模式自动运行Liquibase迁移。

已正确配置JDK的环境可省略 `mise exec --`。生产环境使用统一Compose配置及独占机器号，具体迁移与启动步骤见[数据库迁移](docs/database-migration.md)和[Docker部署](docs/docker-deployment.md)。

生产部署统一使用[deploy/compose.yaml](deploy/compose.yaml)，包含PostgreSQL和应用两个服务，生产数据库手动维护，关闭自动迁移。应用镜像使用`docker.io/slf4j/avalon:latest`，执行`podman compose -f deploy/compose.yaml up -d`；Docker可将命令中的`podman`替换为`docker`。数据库密码独立保存在同级`deploy/postgres-password.txt`，通过文件来源的Compose secret供两个服务共用；密码文件权限为600且不纳入Git；数据目录为`deploy/postgresql/`，首次启动前运行`install -d -m 755 deploy/postgresql`。

## 开发与测试

### 前端开发

保持后端运行，另开终端执行：

```bash
npm --prefix avalon-ui ci
npm --prefix avalon-ui run dev
```

Vite使用 [localhost:5173](http://localhost:5173)，将API代理至8080。修改后端后需要重新构建并启动。

### 按改动范围验证

```bash
# 完整默认验证；集成测试使用隔离PostgreSQL容器
mise exec -- ./mvnw verify

# 日常选择相关测试，避免重复执行全量与长流程
mise exec -- ./mvnw -q -pl avalon-app -am -Dskip.frontend=true \
  -Dtest=LifeCraftingFlowTest,MarketListingTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

# 日常全接口模拟：新账号完成74步主线并自然成长至16级
npm --prefix avalon-ui ci
scripts/simulate-natural.sh

# 前端类型与格式
npm --prefix avalon-ui run build
npm --prefix avalon-ui run test:typecheck
npm --prefix avalon-ui run format:check
```

浏览器测试使用真实接口；需要业务夹具的用例必须经对应JUnit入口或显式开关启动，默认跳过不代表已验收。生活用例入口见[合作页面验证](docs/cooperation-browser-validation.md)，其他测试与布局范围见[工程验收](docs/engineering-acceptance.md)。请在测试数据库上执行会注册、交易和制造的用例。

Java格式由Maven校验；需要修正时执行`mise exec -- ./mvnw io.spring.javaformat:spring-javaformat-maven-plugin:0.0.48:apply`。前端编译会检查未使用的局部声明和参数。更多选择方法见[测试工作流](docs/testing-workflow.md)。

`outputs/`、测试报告、失败轨迹和本地部署配置不纳入Git。文档中引用这些目录的记录是历史执行证据，克隆后可按对应命令重新生成，不能视为远端已包含报告。

## 项目结构

| 目录 | 职责 |
| --- | --- |
| `avalon-dependencies` / `avalon-parent` | 依赖版本与构建约定 |
| `avalon-app` | 应用入口、HTTP资源、迁移与前端集成 |
| `avalon-platform` | 公共异常、持久化基础设施、请求身份 |
| `avalon-identity` / `avalon-character` | 账号会话、人物职业与位置 |
| `avalon-content` | 内容目录、Schema与引用校验 |
| `avalon-world` | 地图、任务、生活系统与命令协调 |
| `avalon-inventory` / `avalon-creature` | 背包装备、伙伴培养与图鉴 |
| `avalon-market` / `avalon-battle` | 托管交易、战斗与多人副本 |
| `avalon-ui` | Vue客户端与Playwright测试 |
| `docs` / `deploy` | 设计、验证记录与部署模板 |

物品固有属性集中维护于 `avalon-content/src/main/resources/content/items.json`；配方、商店和掉落分别维护关系。新增机制需要代码与测试，已有机制的新内容通过配置扩展。

## 文档导航

- [当前进度与交付边界](docs/implementation-progress.md) · [设计与实现差异](docs/design-implementation-gaps.md)
- [后端结构](docs/backend-structure.md) · [前端结构](docs/frontend-structure.md) · [Maven构建](docs/maven-build.md)
- [内容编辑](docs/content-json.md) · [任务系统](docs/quest-system.md) · [生活系统](docs/life-professions.md)
- [测试工作流](docs/testing-workflow.md) · [工程验收](docs/engineering-acceptance.md) · [生活页面验证](docs/cooperation-browser-validation.md)
- [数据库迁移](docs/database-migration.md) · [Docker部署](docs/docker-deployment.md)

## 路线图

- [x] 浏览器账号、剧情、战斗与幻兽培养主流程
- [x] 多人林间兽栏副本与玩家交易
- [x] 种植、钓鱼、采集与制造协作
- [ ] BQ075之后剧情及更高等级副本
- [ ] 其余规划幻兽与完整内容可达性
- [ ] 实体手机、长期供料及真人经济体验验证
- [ ] 公网部署前的限流、HTTPS、凭据管理与容量验收

进度以文档中的版本、环境和验证范围为准；Android原生封装暂缓。

## 参与贡献

欢迎通过 [Issues](https://github.com/lishangbu/avalon/issues) 提交可复现问题或讨论功能。提交Pull Request前，请阅读[开发约束](docs/development-constraints.md)，在独立分支完成修改，并运行受影响的测试。

PR请说明修改原因、行为变化和验证结果。不要提交运行凭据、数据库备份、构建产物或本地验收输出；已执行的Liquibase变更集通过新迁移演进，不改写历史。

## 许可证

项目沿用仓库已有的 [GNU Affero General Public License v3.0](LICENSE)。

## 联系与致谢

维护者：[lishangbu](https://github.com/lishangbu)；项目：[lishangbu/avalon](https://github.com/lishangbu/avalon)。

README组织方式参考 [Best-README-Template](https://github.com/othneildrew/Best-README-Template)。感谢项目使用的Spring、Vue、Ionic、PostgreSQL及测试工具生态。

<p align="right"><a href="#readme-top">返回顶部 ↑</a></p>

聊天首版的频道、战斗限制、通知事件与验证方式见[聊天与系统通知](docs/chat.md)。
