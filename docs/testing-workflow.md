# 按改动范围运行测试

按下表选择受影响的测试并合并执行；通过后仅在新增改动、失败或未覆盖风险时扩大验证。完整游戏模拟使用 `scripts/simulate-natural.sh`，页面交互使用对应浏览器专项。

先通过 `mise exec --` 使用 `mise.toml` 固定的工具链，或确认当前 shell 的 JDK 与父 POM 要求一致；不要通过降低编译版本绕过环境问题。以下命令可加 `mise exec --` 前缀。

从仓库根目录运行单个测试类：

```bash
./mvnw -q -pl avalon-app -am -Dskip.frontend=true -Dtest=BattleRulesTest -Dsurefire.failIfNoSpecifiedTests=false test
```

多个相关测试类用逗号合并，不分别启动 Maven：

```bash
./mvnw -q -pl avalon-app -am -Dskip.frontend=true -Dtest=LifeCraftingFlowTest,LifeCooperationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

单个方法使用 `-Dtest='测试类名#方法名'`。使用 `failIfNoSpecifiedTests=false` 时核对应用模块报告，确认目标用例实际执行。

| 修改 | 验证范围 |
| --- | --- |
| 文档、注释 | 检查内容与链接，不启动构建或测试 |
| 纯公式、概率、规则 | 对应单元测试类或方法 |
| 市场、扣料、并发、防重、迁移 | 对应真实数据库集成测试，不以单元测试替代 |
| Vue 页面 | 在 avalon-ui 运行 npm run build 和 npm run test:typecheck，交互改变另选对应浏览器用例 |
| 生活活动注册或生产公共协议 | 按[生活框架接入清单](life-framework.md#可复制的验证命令)选择目录、内核与真实浏览器验证；新增注册自动进入入口与刷新准入用例 |
| 多个相关领域 | 同一次 Maven 命令选择多个测试类 |
| 阶段收尾、共享基础设施大改 | 完整后端回归；发布前按需补浏览器、长流程和打包 |

前端生产构建已包含应用类型检查，不重复执行；test:typecheck检查浏览器及HTTP模拟代码。纯后端和纯HTTP检查跳过前端构建。浏览器检查须先保证前端产物是最新的。

纯计算测试直接构造正式规则和内容目录，不继承 SpringIntegrationTest。RuleTestFixture 仅组装内存对象，不启动 Spring、HTTP 或数据库。交易等集成测试继续使用隔离 PostgreSQL；因共享静态替身和容器，当前不启用全局并行，也不同时运行多个写入相同 target 目录的 Maven 进程。

保持断言与模拟样本完整；长流程仅在对应功能变更或阶段验收时执行。

## 完整游戏模拟

```bash
scripts/simulate-natural.sh
```

需要Docker、mise工具链和`avalon-ui`已安装的npm依赖。默认独立注册账号，完成BQ001～074并通过真实战斗达到16级；不启动浏览器、不注入进度。可选15级练级对照、日志与边界见[全接口日常模拟](quest-system-validation.md#全接口日常模拟)。
