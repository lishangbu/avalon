# 后端结构与验证

当前实现包含BQ001～074主线与至16级的自然成长流程，以及强化、进化、交易市场。技术栈采用Spring Boot + Maven，使用同步JDBC／虚拟线程、JPA、Liquibase、Spring内存调度、CosId与Lombok。
仓库使用 Spring Data JPA，特殊无实体存储使用 JdbcClient；完整架构见 [架构决策](adr/0001-spring-modular-monolith.md)。

## 应用入口与事务

- 任务：`QuestResource` 调用查询服务、接取服务及 `QuestRuntimeService`。`GET /api/quests` 返回 available、completed；
  `POST /api/quests/instances/commands` 按实例和轮次执行效果并保存请求重放凭证。
- 库存：`InventoryResource` 分别调用 `InventoryQueryService` 和 `InventoryCommandService`，提供 `GET /api/inventory` 和
  `POST /api/inventory/equipment/{id}/equip`。
- 伙伴：`CreatureResource` 调用 `CreatureQueryService` 提供 `GET /api/creatures`；发放伙伴由内部 `CreatureCommandService`
  完成，不开放任意领取接口。
- 前端按任务、库存和伙伴接口维护独立缓存，前后端随同一个应用发布。

写用例从认证身份获取 owner，经 `CharacterCommandService.lock` 获取角色行锁与内部 CharacterContext。Context 使用 Long
ID，不经过展示 DTO 字符串转换，不向其他模块暴露可修改的 JPA 实体。HTTP 展示 ID 仍是字符串。

任务领取是一个事务；内部库存/伙伴发放与交付要求已有事务，并在各命令中主动按账号取得角色锁。穿戴是独立命令事务入口。角色锁与来源唯一约束共同防重，不能仅依靠
MANDATORY 注解或调用者约定。移动和任务共用角色锁；查询使用普通 SELECT，不取得角色写锁。任务进度从一次凭证查询同时派生
available 和 completed，避免两次读取之间提交导致矛盾。多个 GET 不是同一笔跨请求快照，客户端在任务提交后统一刷新相关缓存；服务端奖励始终原子提交。

## 轻量 CQRS 与模块内分层

使用同一 PostgreSQL、同一套表和同步 Spring Bean 调用。没有独立读库、事件溯源、命令总线或消息最终一致性。现有
character、world、inventory、creature、identity 模块均按实际职责拆分：

```text
业务模块/
├── api/                    # Resource及HTTP入参、组合响应
├── application/
│   ├── command/            # 写事务、锁、幂等和用例编排
│   ├── query/              # 无写副作用的查询
│   └── model/              # 独立于Service和JPA的不可变结果
├── domain/                 # 有实际需要的纯规则
└── persistence/
    ├── entity/             # 模块内部JPA映射
    ├── repository/         # 实体查询与锁操作
    └── mapper/             # 存储JSON编码和读模型投影
```

没有对应职责的包不强制创建。共享内容目录和技术能力不机械拆成命令/查询。简单 API 可直接返回独立 application.model
结果，避免再复制一套相同响应 DTO；JPA 实体绝不作为 API 响应或跨模块接口。任务回执、装备属性、伙伴技能的 JSON 编解码已移入
persistence.mapper。

战斗存档直接调用 ObjectMapper；其余五个业务 Codec 直接注入应用统一配置的 ObjectMapper，保留返回类型、实体投影和市场回执节点比较。Jackson 3 解析异常直接以运行时异常传播，交由外层事务回滚；不重新定义存档格式，不用空值或默认奖励掩盖解析失败。

应用层允许直接依赖 Jackson，不为单纯 JSON 读写强制建立适配层。纯业务规则尽量接收类型化对象；架构检查继续禁止 HTTP 层直接访问持久化、查询层调用命令及跨模块导入实体。

认证也分离读写：SessionCommandService 签发、轮换、撤销；SessionQueryService 核验身份、查询设备；LoginCommandService 调用
Spring Security 密码验证。严格刷新重放撤销继续先提交再返回401。AccountCredentials 位于 identity 持久层，通过 Spring Data 只读仓库访问认证视图。

查询一致性以当前领域语义为前提：角色归属不可变且没有删除功能，查询先解析账号对应的角色编号，再读取该领域列表；列表由单条 SQL
返回。后续加入角色删除或转移时必须重新评估该边界。只读服务仍使用事务管理数据库会话，但不获取悲观写锁。

中文注释覆盖各层包契约、用例主要步骤、字段业务含义、边界条件和失败传播，前端源码补充状态归属与请求生命周期说明。生成代码及第三方库不作为业务实现重新分层。

`QuestRepository`、`InventoryRepository`、`CreatureRepository` 保存具体领域查询。`RefreshReceiptRepository` 管理刷新凭证，Spring 内存
调度适配只调用 identity 的维护服务，不直接访问身份领域实体。未引入通用 BaseService、万能 Repository 或插件式规则引擎。

## Java 类型与包组织

公开类型独立文件及私有辅助类型例外见 [开发约定](development-constraints.md#工程与数据)。命令请求与查询视图放入 `application.model`，HTTP 专用入参放入 `api.model`，内容模型放入对应内容领域的 `model` 子包；纯规则和内部存档数据保留在 `domain`。

展示模型、配置和实体同名时使用明确后缀区分，例如 `MarketListingView` 与 `MarketListing`、配置模型的 `Config` 后缀。调用方直接引用顶层类型；此次拆分改变 Java 引用路径，HTTP 字段、枚举值和 JSON 存档格式不变。

执行 `python3 scripts/check-java-imports.py` 检查公开类型组织、文件名、重复导入及正文中的全限定类型。

## 仓库调用边界

实体 CRUD、写锁、回执和分页查询由用例直接调用 Spring Data 仓库，不再通过额外的聚合仓库逐层转发。副本补给分别使用范围仓库和分配仓库，图鉴查询直接使用 CreatureDiscoveryRecords。
具有实际存储规则的操作仍集中维护：DungeonBattleMemberStore 在 JDBC 写入参战关系前 flush 战局；DungeonDailyStartStore 执行每日计数原子 upsert；CombatVitalsStore 处理共享主键资源记录的首次创建及更新。图鉴登记的去重逻辑和奖励出处防重仍保留原有边界。上述操作加入应用服务事务，不产生独立提交。

## 基础模块边界

`avalon-platform` 是 packaging=pom 的聚合入口，不生成 JAR，不作为业务模块的依赖。三个子工程直接继承 avalon-parent，版本继续使用统一 revision。

| 子工程 | 职责 | 依赖边界 |
|---|---|---|
| avalon-platform-core | BusinessException | 无 Spring、Web 或数据库运行依赖 |
| avalon-platform-persistence | EntityRepository、AssignedEntity、CosIdGenerated、生成器 | Spring Data JPA、Hibernate、CosId；不依赖 Web 或 Security |
| avalon-platform-web | CurrentAccount、BusinessExceptionMapper | core、Spring Web、Spring Security；不依赖持久化模块 |

业务模块显式声明所使用的基础子工程；identity、market 不直接依赖 platform-web。identity 自身提供认证和 HTTP 接口，直接声明 Spring Web 与 Security 依赖，并依赖 content 读取共用违禁词目录。既有业务模块间仍会传递部分框架依赖，不把本次基础模块拆分视为完整的 API／领域模块隔离。Java 包名保持稳定，模块边界由 Maven 依赖约束。

`content.text.ForbiddenWordsCatalog`只负责词表校验及是否命中，不依赖业务异常、认证或持久化。
注册、创角、设备会话创建与幻兽改名的命令负责在新文字写入前调用它，并决定字段错误和事务边界；
查询不扫描旧文本，登录账号的共用格式验证也不接入词表。匹配规则见[内容维护](content-json.md#玩家自填文字与违禁词)。
包含账号及角色约束名称的 PersistenceExceptionMapper 位于 avalon-app，由应用组装层统一转换数据库错误，不放入通用持久化模块。

## 主键生成

普通实体使用 `@CosIdGenerated` 接入 Hibernate 插入前主键生成，创建时不填充 ID，也不继承 AssignedEntity。生成器复用当前 Spring 上下文中的 CosId，save 返回时主键已可用于建立关联；事务回滚不会回收或复用已生成编号。
`AssignedEntity` 仅用于业务赋值主键：刷新令牌摘要 RefreshReceipt、复用战斗单位编号的 CombatVitals、复用首次库存编号的 InventoryGrant。补录奖励出处及市场拆分来源标识直接注入 Starter 的共享 CosId 生成器，不承担普通实体的手工赋值。

## 周期维护

`MaintenanceScheduling` 使用四线程 `ThreadPoolTaskScheduler`，在应用启动阶段注册内存任务。周期固定在代码中，不提供 YAML 或环境变量覆盖及禁用入口。

| 调度方法 | 固定间隔 | 业务 |
|---|---|---|
| SessionMaintenance.clean | 1h | 过期刷新记录清理 |
| MarketMaintenance.expire | 10s | 市场挂单到期 |
| DungeonRoundMaintenance.expire | 1s | 副本回合超时 |
| DungeonRoundMaintenance.expireLobbies | 3s | 组队确认超时 |

采用 fixedDelay：一次扫描结束后等待指定间隔，同一任务不重叠，不保证截止后恰好一秒内完成结算。市场、组队和副本每批最多查询 100 条，到期编号进入原有业务事务后再次校验；扫描失败会记录错误并在后续周期重试。
截止时间保存在业务表，应用重启后自动扫描，不重放停机期间的所有回合。内存调度当前限定单实例，扩容必须增加任务领取协调。

## 内容与规则

- `quests.json` / QuestCatalog：步骤、前置、NPC、效果以及跨目录编号引用。
- `items.json` / ItemCatalog：任务物品及装备模板，验证属性名称、基础值、职业加成值、职业引用和合并溢出。
- `creatures.json` / CreatureCatalog：伙伴与形态模板，保持特性槽、初始技能与等级约束。
  物种可选的 `image` 字段维护同源图片地址，例如 `/game-assets/creatures/claw-cat.webp`；文件位于
  `avalon-ui/public/game-assets/creatures/`，随前端构建发布。图鉴接口返回该字段，伙伴列表和详情按物种编号读取，
  不在前端硬编码图片映射、不写入玩家实例。未配置时不展示图片，进化形态不会自动沿用原形态图片。
  内容目录在启动时校验并加载，修改 JSON 后须重新构建并重启后端。
- `battle.json` / BattleCatalog：职业和开篇八物种成长、技能通道、八属性倍率、遭遇解锁、商品及独立权重掉落池。
- `GrowthRules`：人物与伙伴共用 `experience.json` 累计经验门槛；成长上限50级，对应23935620累计经验。人物满级不继续囤积经验。携带容量为5＋floor
  (人物等级/10)，不增加出战数量。

目录启动加载，坏配置拒绝启动，无热更或内容版本系统。公式属于程序规则；当前试玩上限与数据库约束绑定，扩级需一并迁移，不能只改一个配置数字。BQ004
仍先发人物经验，再按最新人物等级检查伙伴容量；赠送伙伴保持模板等级及对应累计经验，不追补此前人物经验。

装备已覆盖武器、头、身体、手、腿、脚、饰品七部位及品质、套装、补购和交易；背包包含待领。战斗已覆盖野外、学院训练和三人副本。强化、进化与市场系统已实现，正式等级门槛仍限制其开放，见第二阶段规格。

## 战斗边界

`BattleCommandService` 持有角色锁，协调本模块仓储与其他领域的应用接口。`BattleEngine` 只处理回合，资源扣除和捕获经窄接口进入外层事务。
`BattleRules` 集中成长、直接伤害、捕捉和经验公式。`BattleQueryService` 只读最近战局和地点服务；`BattleViews`
明确裁剪敌方特性、槽位、随机种子及选招，不直接序列化存档。

开战使用角色与请求号去重；回合使用战局与回合号去重，完整响应写入回执。角色的 activeBattleId
与战局同事务开启/清除。移动、交谈、换装、编队、恢复及购买均在持锁后检查战斗门禁，不能只依靠路由。捕获、扣球、战局推进、经验金币与掉落同事务提交，任何后置写入失败整笔回滚。单人断线没有自动行动或定时结算。

## 认证与异常

RegistrationService 的外层流程使用 NOT_SUPPORTED，校验并计算 bcrypt 后调用独立 Spring Bean RegistrationWriter。Writer
开启短事务，创建账号及首次会话；失败全部回滚。并发用户名冲突仍由数据库唯一约束兜底。Token 有效期、刷新轮换和重放撤销策略见后端架构。

PersistenceExceptionMapper 仅将 account_username_key、player_character_name_key、player_character_account_id_key
分类为409业务冲突。未知唯一约束、主键和内部来源冲突记录错误并返回通用500，不能冒充玩家重名。奖励幂等仍由持锁检查与保存原响应完成，不依赖捕获所有23505来实现。

会话清理删除到期刷新凭证，设备会话历史的保留期限尚未另行确定。

代码沿用项目格式约定，使用中文注释说明事务与业务边界。

## 验证与维护

LayerBoundaryTest 检查 API／应用／持久化和跨模块依赖；CqrsConcurrencyTest 检查查询不等待角色写锁且不读脏数据。
认证、奖励、库存、战斗、副本及市场测试使用真实 PostgreSQL，覆盖幂等、权限、并发与失败回滚。
当前完整验证结果见 [工程验证](engineering-acceptance.md)，功能索引见 [开发进度](implementation-progress.md)。
不以过往 BQ001～BQ007 的阶段记录代表当前全项目覆盖，也不据功能测试宣称在线容量。
