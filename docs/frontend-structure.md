# 浏览器前端结构

当前采用 Vue 3、Ionic Vue Router、TanStack Vue Query 与 Ionic Vue，统一 iOS 模式，产物由 Spring Boot 同源提供。不引入 Pinia。当前运行时覆盖与限制见 [工程验证](engineering-acceptance.md)。

## 职责与依赖

- `src/App.vue`：应用容器、导航错误提示、路由出口。身份失效立即卸载业务页面。
- `src/router/index.ts`：创建 Router 并装配守卫；`routes.ts` 定义页面路由；`guards.ts` 恢复会话并依据服务端角色状态决定准入；
  `state.ts` 管理只读暴露的导航错误状态。
- `src/app/query-client.ts`：创建应用级 QueryClient，供路由、Vue Query 插件和身份失效清理共同使用。无需增加集中式 store 或
  Pinia，会话仍归 auth、服务端缓存仍归 Vue Query。
- `src/pages`：按 `auth`、`character`、`world`、`creature`、`inventory`、`battle`、`dungeon`、`navigation`、`quest`、`social`、`market`、`life` 分组，保留明确的 `*Page.vue` 文件名。组装页面、管理页面级弹窗开关，不决定奖励或位置是否合法。现有商品交易、设备读取等页面请求仍待整理到对应 features 用例。
- `src/features/navigation`：可选导航目录与账号偏好查询。底部固定冒险、更多，中间三个入口可替换且不能重复；完整目录始终保留在更多页。服务端仅接受已知入口标识，配置不能携带任意 URL 或绕过战斗、副本准入。
- `src/features/auth`：会话生命周期、刷新互斥、认证接口及设备协议。
- `src/features/character`：角色类型与创建操作。
- `src/features/world`：世界查询、移动操作、地图展示与地点服务。
- `src/features/quest`：任务查询、交互操作及当前位置 NPC 操作面板。
- `src/features/content`：道具、技能与特性的共享只读详情；背包、商店、任务奖励与战后物品使用相同说明入口。人物面板与文案规范见[人物交互与内容详情](npc-and-content-details.md)。
- `src/features/inventory`、`creature`：各自的数据类型和用例，以及放生确认等复用组件；装备操作由 inventory 管理。
- `src/features/battle`：战局协议、只读缓存、整轮提交、遭遇/补给/恢复操作及地点入口；`BattlePage` 只组装战场和指令草稿。
- `src/shared`：不依赖业务模型的 HTTP 请求器与操作状态辅助函数。请求器的认证能力由启动代码注入，避免与会话模块循环依赖。
- `src/shared/ui`：Ionic 页面壳、通知和底部弹层；`usePageActive` 管理缓存页面活动状态，`usePageClock` 停止隐藏页面倒计时。
- `src/styles/base.css`：主题、基础排版、Ionic 必要覆盖和共享工具样式。页面壳、通知、导航错误及敌方生命条规则归所属组件；页面与弹层共同使用的 `command-dock` 保持共享。`AppSheet` 的专用类规则保留非 scoped 样式，以兼容 Ionic 浮层挂载。

跨模块导入使用 `@/`（Vite 与 TypeScript 同时映射到 `src`），同目录导入保留 `./`。目录归组不改变路由 URL。

格式以 `avalon-ui/.prettierrc.json` 为准：`npm run format:check --prefix avalon-ui` 检查，`npm run format --prefix avalon-ui` 修正；覆盖源码、测试及前端配置，排除依赖、构建产物和锁文件。

业务模块按需要增加文件，不预建空目录；不要求一份展示数据经过多层透传。任务、库存、伙伴分别请求 `/api/quests`、
`/api/inventory`、`/api/creatures`，各自拥有一份 Vue Query 缓存。跨领域任务奖励提交成功后，任务操作使世界、任务、库存及伙伴缓存失效；穿戴和卸下刷新库存与战斗入口资源投影。

## 路由与状态

地址包括 `/login`、`/character/create`、`/world`、`/companions`、`/inventory`、`/character`、`/shop`、`/crafting`、`/battle`、`/dungeon-lobby`、`/dungeon/:id`、`/friends`、`/more`、`/more/navigation`、`/quests`、`/encyclopedia`、`/companions/:id`、`/companions/:id/learning`、`/evolution`、`/enhancement`、`/market`、`/life`、`/life/gathering`、`/life/farm`、`/life/food`、`/life/fishing`、`/life/production`
。未登录访问受保护页面进入登录页；已登录但无角色进入创角页。角色有进行中战局时强制进入战斗；结束后允许停留结算页，再返回地图。刷新和直接访问地址均经过会话恢复与服务端查询。Spring Boot
的 SPA 路由回退继续生效，未知 API 仍返回 404。

底栏固定冒险与更多，中间三个入口默认伙伴、背包、角色，允许从伙伴、背包、角色、图鉴、任务中选择且不重复；伙伴详情、学习、合成、商店、招募和战斗为独立页面。地点及不可逆操作确认使用底部弹层。地址不接受角色位置作为移动指令，真正移动仍提交到服务端并携带位置
revision。地图行列由地点坐标派生，但不表示已经实现大型地图或新的导航规则。

附近地图与全图采用相同的点击规则：相邻地点直接移动，当前位置不打开弹层，远处地点可预览详情但不可直接移动。页面不再提供重复的“查看地点”按钮。提交期间禁用节点，服务端确认后更新位置，不自动弹出抵达详情。失败显示原因并重新读取实际位置，仍使用原有 revision 与相邻道路校验。

服务端状态由 Vue Query 管理，会话状态由 auth 管理，表单及弹窗状态留在页面/组件。游戏写操作在当前浏览器内共享互斥，错误显示归属于对应操作；该互斥不能替代后端事务。登录、设备列表请求不占用游戏写锁。

移动成功或失败均刷新世界状态；任务提交成功刷新世界、任务、库存与伙伴；装备穿戴刷新库存与战斗入口资源投影。写请求发生业务错误时不自动重试，鉴权失败最多刷新
Token 并重放一次。

会话失效同步清除 QueryClient
并卸载业务页面，清除设备列表等本地状态。请求和刷新响应校验会话代际，防止旧身份的迟到响应污染当前页面或以新身份重放旧写操作。该代际是内存中的会话隔离标记，不是游戏内容版本或热更新机制。

## 当前边界

地图以地点名称为标题；服务入口依据当前位置或后端投影显示。当前不是完整的通用章节展示系统。

伙伴支持手动编队，捕获不会自动占第二出战位。战斗页面包含人物与两只同时出战、两只替补伙伴的整轮指令、药品、捕捉、换宠、免费倒下补位、认输和战报。库存与概率以服务端响应为准；隐藏特性捕获后才揭示，不显示误杀风险提示。开战/购买请求号在当前操作失败重试时复用，回合号由服务器提供。游戏内路由锁定之外，后端同样拒绝战外写操作。

## 验证

`./mvnw package` 执行前端生产构建并将静态文件打入 Boot JAR。`npm run format:check --prefix avalon-ui` 检查前端格式。
独立浏览器测试通过 `npm run test:e2e --prefix avalon-ui` 访问指定应用；数据库夹具和布局矩阵开关见 README。
Maven 显式浏览器验收由 PhaseTwoBrowserIT 启动隔离应用，直接访问 Spring Boot 提供的静态页面。
各浏览器测试类的执行范围与版本记录见[工程验证](engineering-acceptance.md)及[生活页面验证](cooperation-browser-validation.md)，不将某一批用例数量当作全部覆盖。
前端类型检查启用 `noUnusedLocals` 和 `noUnusedParameters`，构建时拒绝未使用的局部声明和参数。

手机模拟视口不等于真机或软键盘已验收，用户尚未提供真人试玩记录。Android 原生封装不属于当前交付。
