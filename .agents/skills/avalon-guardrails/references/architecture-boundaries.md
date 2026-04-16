# 架构边界与归属

## 1. 项目形态

- 构建工具固定为 `Gradle Kotlin DSL`
- 当前形态固定为 `模块化单体`
- 不以“现在先快”为理由退化成单模块大泥球

推荐模块：

- `apps/avalon-app`
- `modules/shared-kernel`
- `modules/shared-application`
- `modules/shared-infra`
- `modules/identity-access`
- `modules/catalog`
- `modules/player`
- `modules/battle`

## 2. Bounded Context 规则

主上下文固定为：

- `IdentityAccess`
- `Catalog`
- `Player`
- `Battle`

规则：

- 一个业务变更必须先判断主归属上下文
- 跨上下文协作必须显式建边界
- 不允许把多个上下文的写逻辑揉进一个应用服务

### 2.1 Catalog 边界

`Catalog` 的定位固定为“共享定义型参考知识库”，不是“基础数据大桶”。

允许放入 `Catalog` 的内容：

- 物种、技能、道具、特性、属性克制、性格、成长率、进化链等定义型参考数据
- 被多个上下文共享的规则事实
- 读多写少、由内容或规则维护流程驱动的数据

不允许放入 `Catalog` 的内容：

- 玩家实例态数据，例如背包、已拥有生物、盒子
- `Battle` 过程态或会话态数据
- `Battle` 或 `Player` 的运行时规则模型
- 任何只属于单个上下文内部流程的数据

协作规则：

- 其他上下文可以读取 `Catalog` 的定义和规则事实
- `Battle`、`Player` 读取后，必须在本域内转换成自己的运行时模型
- 不要把 `Catalog` 直接做成承载所有“基础数据”的泛化模块

## 3. Shared 模块规则

`shared-kernel` 只放稳定、少量、业务中性的抽象，例如：

- `DomainEvent`
- `AggregateRoot`
- `Identifier`
- 通用领域异常基类

`shared-application` 只放稳定、少量、业务中性的应用层公共契约，例如：

- 分页查询模型
- 时间源契约
- 不绑定 HTTP、SQL、Quarkus runtime 的用例层值对象或端口

`shared-infra` 只放技术底座，例如：

- Flyway
- Redis
- outbox 基础设施
- trace/logging
- 幂等
- 时间源实现和 ID 生成器
- HTTP/SQL 等技术适配

禁止：

- 把任意“以后可能复用”的代码塞进 shared
- 把上下文专属模型上移到 shared
- 把应用层公共契约塞进 `shared-infra`
- 让 `shared-application` 反向依赖 `shared-infra`

## 4. Schema 规则

单体阶段先共享一个 PostgreSQL 实例，但按上下文分 schema。

推荐 schema：

- `iam`
- `catalog`
- `player`
- `battle`
- `integration`

硬规则：

- 不允许跨上下文强外键
- 不允许跨上下文直接连表做业务写入
- 允许通过 projection 或只读视图做查询整合，但必须显式说明归属

## 5. 依赖规则

硬规则：

- 不允许跨上下文 `repository` 直接调用
- 不允许跨上下文 entity 直接引用
- 不允许某个上下文直接依赖另一个上下文的内部 infrastructure

允许的跨上下文方式：

- 应用服务调用
- ACL
- 只读投影
- 领域事件
- outbox 集成事件

Shared 依赖方向：

- `shared-application -> shared-kernel` 可选
- `shared-infra -> shared-application -> shared-kernel` 允许，用于基础设施实现应用层契约
- bounded context 的 `application` 层可以依赖 `shared-application`
- bounded context 的 `application` 层不应直接依赖 `shared-infra` 的具体实现；需要运行时实现时，通过接口由外层注入
