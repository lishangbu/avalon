# 宝可梦中文资料来源调查

> 核查日期：2026-08-03。链接与接口状态可能变化；版权结论只依据各站当前公开条款，不构成法律意见。

## 结论

有官方中文资源站，而且中国大陆、台湾和香港均有可用的官方图鉴；但它们首先是供人浏览的内容站，不是获得再分发授权的数据开放平台。

- 中国大陆首选[宝可梦官方网站](https://www.pokemon.cn/)及其[简体中文图鉴](https://www.pokemon.cn/play/pokedex)；台湾、香港分别有[台湾官方站](https://tw.portal-pokemon.com/)与[香港官方站](https://hk.portal-pokemon.com/)，也各有繁体中文图鉴。
- 官方图鉴覆盖名称、全国图鉴编号、属性、弱点、身高、体重、分类、性别、特性与说明、图鉴文本、能力分布、形态及进化关系；官方新闻站覆盖游戏、动画、商品、应用、卡牌、活动等资讯。
- 图鉴页面背后确实存在返回 JSON 的接口，但没有面向开发者的版本、稳定性、服务承诺或开放许可文档。官方使用条款明确把“数据”列入受保护内容，并禁止未经书面许可复制、改编、传播、出版和转帖。因此，不应把这些接口视为获准抓取或再分发的官方 API。
- 若目标是程序化导入，实用性最高的是第三方 [PokéAPI](https://pokeapi.co/docs/v2)：有 REST/GraphQL、静态 JSON 仓库、简繁体名称和文本，并要求本地缓存。它不是宝可梦官方服务；其代码和数据仓库采用 BSD-3-Clause，不代表任天堂、The Pokémon Company 等权利人的商标、角色、美术或原始文案被重新授权。
- [神奇宝贝百科（52Poké Wiki）](https://wiki.52poke.com/)最适合中文人工查证，内容广且支持简繁转换，也有 MediaWiki API；但原创文字仅按 CC BY-NC-SA 3.0 许可，图片、音频通常仍归宝可梦相关权利人，且 `robots.txt` 要求极长抓取间隔并屏蔽图片爬虫，不适合直接批量采集。
- 若关注卡牌，台湾和香港另有官方卡牌查询/卡组工具；第三方 TCGdex 提供多语言卡牌 API，但简体中文仍不完整，且卡图、插画和原始卡牌文案不能仅凭其 MIT 许可证推定已获权利方授权。

对 Avalon 的建议是：以 PokéAPI 的固定快照作为候选结构化数据源，以官方中文图鉴人工校验中文名称和关键事实；图片、音频、图鉴文案不要直接导入或公开再分发。若必须把官方站 JSON 或官方素材用于产品，应先取得书面许可。

## 官方来源

| 地区与入口 | 语言及内容 | 图鉴/机器能力 | 抓取与再分发判断 |
| --- | --- | --- | --- |
| [中国大陆宝可梦官方网站](https://www.pokemon.cn/) | 简体中文。新闻栏目包括游戏、动画、商品、卡牌等；[网站地图](https://www.pokemon.cn/sitemap)还列出商品、店铺和客户支持入口。使用条款称其为“中国大陆专用网站”。 | [图鉴入口](https://www.pokemon.cn/play/pokedex)当前重定向到 `dex.pokemon.cn`，可按名称、编号、属性、特性、地区、身高和体重查找。列表页内部使用 [`/play/pokedex/api/v1`](https://dex.pokemon.cn/play/pokedex/api/v1) JSON，但没有开发者文档或开放许可。 | [使用条款](https://www.pokemon.cn/termofuse)规定站内设计、数据、文字、图表、图像、声音、录像等仅限个人娱乐，禁止商业使用；未经书面许可不得复制、改编、传播、出版、转帖或建立镜像。因此可人工浏览、引用链接，不应批量抓取后导入或再发布。主站 [`robots.txt`](https://www.pokemon.cn/robots.txt)还禁止抓取上传目录；即使某个图鉴子域没有 robots 规则，也不等于获得版权许可。 |
| [台湾宝可梦官方网站](https://tw.portal-pokemon.com/) | 繁体中文，面向台湾。官方[站点地图](https://tw.portal-pokemon.com/sitemap.xml)列出最新消息、动画/电影、商品、应用、游戏、卡牌和“宝可梦是什么”等栏目。 | [繁体图鉴](https://tw.portal-pokemon.com/play/pokedex)提供列表和详情；例如[皮卡丘页面](https://tw.portal-pokemon.com/play/pokedex/0025)展示属性、弱点、身高、分类、体重、特性、图鉴文本、六项能力、形态与进化。其 [`/play/pokedex/api/v1`](https://tw.portal-pokemon.com/play/pokedex/api/v1) 返回 JSON，但属于页面内部接口，无公开 API 契约。 | [使用条款](https://tw.portal-pokemon.com/termofuse/)称资料仅供个人享用，并禁止拷贝、复制、修改、刊登、发布、传输或散布。[`robots.txt`](https://tw.portal-pokemon.com/robots.txt)禁止图片及 Next.js 媒体目录。可浏览和文字链接，不应据此批量复制数据或素材。 |
| [香港宝可梦官方网站](https://hk.portal-pokemon.com/) | 繁体中文，面向香港。其[站点地图](https://hk.portal-pokemon.com/sitemap.xml)所列栏目与台湾站相近，但新闻内容按地区发布。 | [繁体图鉴](https://hk.portal-pokemon.com/play/pokedex)及[详情页](https://hk.portal-pokemon.com/play/pokedex/0025)当前可访问；也存在内部 JSON [列表接口](https://hk.portal-pokemon.com/play/pokedex/api/v1)。 | [使用条款](https://hk.portal-pokemon.com/termofuse/)和 [`robots.txt`](https://hk.portal-pokemon.com/robots.txt)给出的限制与台湾站同类：内部 JSON 可访问不等于可再利用或有稳定性承诺。 |

繁体中文地区还有官方集换式卡牌[台湾卡牌查询](https://asia.pokemon-card.com/tw/card-search/)、[台湾卡组工具](https://asia.pokemon-card.com/tw/deck/)、[香港卡牌查询](https://asia.pokemon-card.com/hk/card-search/)和[香港卡组工具](https://asia.pokemon-card.com/hk/deck/)。这些是面向玩家的官方网页工具，不是带再利用许可的数据下载/API。

### 官方图鉴边界

2026-08-03 实测中国大陆与台湾图鉴列表均到全国图鉴编号 1025，列表 JSON 还包含地区形态、超级进化和超极巨化等子形态。列表接口主要给出编号、形态序号、中文名、身高、体重、图片路径和属性；更完整的特性、说明、能力与进化信息位于详情页面中。

这套接口具有以下风险：

1. 它是网页实现细节，不是官方宣布的公共 API；字段、路径、鉴权图片 URL 和可用性都可能随时变化。
2. 官方条款明确覆盖“数据”和图片。技术上可以发起 GET 请求，不能推出具有复制、存储、改编、商业使用或再分发授权。
3. `robots.txt`只表达自动抓取偏好，不授予版权或商标许可；反过来，未禁止某条路径也不是授权。

## 高质量第三方来源

### 1. PokéAPI：最适合程序化导入

- 官方性：第三方社区项目，不是 The Pokémon Company、Nintendo、Creatures 或 GAME FREAK 的官方 API。[About](https://pokeapi.co/about)说明它由 Paul Hallett 和社区贡献者创建，并声明 Pokémon 名称是 Nintendo 的商标。
- 内容：覆盖宝可梦、形态、种族、属性、招式、特性、道具、版本、世代、地点、遭遇、进化链、学习方式等游戏数据。[API v2 文档](https://pokeapi.co/docs/v2)说明为只读 GET API，无需认证；也提供 GraphQL。
- 中文：语言资源明确包含官方语言标记 [`zh-hans`](https://pokeapi.co/api/v2/language/12/) 和 [`zh-hant`](https://pokeapi.co/api/v2/language/4/)。例如 [`pokemon-species/25`](https://pokeapi.co/api/v2/pokemon-species/25/) 同时含“皮卡丘”的简繁名称、分类和多版本图鉴文本；[`ability/9`](https://pokeapi.co/api/v2/ability/9/)、[`move/85`](https://pokeapi.co/api/v2/move/85/) 和 [`item/1`](https://pokeapi.co/api/v2/item/1/)也有简繁名称与说明。
- 下载/API：线上 REST API 可分页读取；[`PokeAPI/api-data`](https://github.com/PokeAPI/api-data)提供按 API 路径组织的静态 JSON 和 JSON Schema，适合锁定提交后离线导入。服务的[公平使用政策](https://pokeapi.co/docs/v2)要求每次请求后在本地缓存，虽已取消固定限流，仍应控制频率。
- 许可：API 服务端代码和 `api-data` 仓库分别公开 [BSD-3-Clause 许可证](https://github.com/PokeAPI/pokeapi/blob/master/LICENSE.md)与 [BSD-3-Clause 许可证](https://github.com/PokeAPI/api-data/blob/master/LICENSE.txt)。许可证允许在保留通知和免责声明等条件下再分发项目内容，但仓库同时明确 Pokémon 名称属于 Nintendo 商标；许可证不能替第三方授予其并不拥有的宝可梦角色、美术、商标及官方原始文本权利。
- 风险判断：结构化数值和标识最适合作为工程输入；中文名称可用于检索、校对。若产品会公开或商业分发，长篇图鉴文案和图片仍应单独做权利审查。导入时固定 `api-data` commit，不要在构建或运行时高频请求线上 API。

### 2. 神奇宝贝百科（52Poké Wiki）：最适合中文查证

- 官方性：高质量社区百科，与宝可梦权利方没有关联；其[版权声明](https://wiki.52poke.com/wiki/神奇宝贝百科:版权声明)明确说明这一点。
- 内容与中文：[全国图鉴编号列表](https://wiki.52poke.com/wiki/宝可梦列表（按全国图鉴编号）)当前列出 1025 种，并覆盖地区形态；站点还维护招式、特性、道具、地点、游戏、动画和卡牌资料。MediaWiki 支持简体、繁体转换。
- 下载/API：站点运行 MediaWiki，并公开 [`api.php`](https://wiki.52poke.com/api.php?action=query&meta=siteinfo&siprop=general%7Crightsinfo&format=json)；可用标准 `query`、`parse` 等只读模块获取页面和修订信息。它不是面向稳定数据表的专用 API，模板与页面结构可能变化。
- 许可：原创文字默认使用 CC BY-NC-SA 3.0，要求署名、非商业、相同方式共享；图片和音频除特别注明外通常归 The Pokémon Company 及相关企业，百科自身仅主张合理使用。不能把整站内容或图片视为统一开放数据。
- 抓取风险：[`robots.txt`](https://wiki.52poke.com/robots.txt)禁止多类路径和图片爬虫，并对普通爬虫设置 `Crawl-delay: 500`。即使 MediaWiki API 可访问，大规模抓取也应先联系站方；对商业项目，CC 的“非商业”条款本身就使其不适合作为直接导入源。

### 3. TCGdex：卡牌专用补充源

- 官方性与内容：[TCGdex](https://tcgdex.dev/)是第三方卡牌数据库，提供 REST、GraphQL、多语言数据和 SDK；不是 Pokémon 官方服务。
- 中文/API：[`zh-tw` 系列接口](https://api.tcgdex.net/v2/zh-tw/sets)可返回繁体中文系列及卡牌数据；[`zh-cn` 系列接口](https://api.tcgdex.net/v2/zh-cn/sets)虽可访问，但项目语言状态仍将简体中文标为进行中，部分系列可能没有卡牌明细，不能假定完整。
- 下载与许可：[`tcgdex/cards-database`](https://github.com/tcgdex/cards-database)提供数据库仓库并采用 MIT 许可证。该许可可覆盖项目自身代码和整理成果，但没有证据表明它能转授 Pokémon 商标、卡面扫描、插画和官方卡牌文案的权利。
- 风险判断：适合卡牌索引原型和交叉核对，导入前做完整率检查；生产环境再分发卡图或文案需另行审权。

### 4. veekun/pokedex：可下载但老旧，仅作交叉校验

- [`veekun/pokedex`](https://github.com/veekun/pokedex)提供可构建 SQLite 的 CSV；[`pokemon_species_names.csv`](https://github.com/veekun/pokedex/blob/master/pokedex/data/csv/pokemon_species_names.csv)含名称和分类，语言表包含 `zh-Hant` 与 `zh-Hans`。
- 项目 [README](https://github.com/veekun/pokedex/blob/master/README.md)称数据来自游戏抓取，项目已缺乏持续维护；软件是 MIT，但游戏数据仍归 Nintendo、Creatures、GAME FREAK，作者仅主张粉丝参考用途的合理使用并提示自行承担法律风险。
- 风险判断：不适合作为当前主数据源或“授权清晰”的来源，只可用于离线核对历史数据。

## 选型建议

| 需求 | 推荐来源 | 做法 |
| --- | --- | --- |
| 核对简体官方名称、属性、特性、形态 | 中国大陆官方图鉴 | 人工核对并记录来源链接，不镜像页面、图片或图鉴文案。 |
| 核对繁体官方译名与文本差异 | 台湾/香港官方图鉴 | 选择目标地区版本；不要假设所有繁体地区措辞完全一致。 |
| 获取新闻、产品与活动资讯 | 对应地区官方站的“最新消息/Topics” | 作为内容链接和事实来源；新闻不是稳定资料数据库。 |
| 批量导入种族、属性、招式、特性等结构化数据 | PokéAPI `api-data` 固定 commit | 离线快照、校验 schema、保存来源 commit 与导入时间；图片和长文本单独排除或审权。 |
| 人工交叉核对复杂机制与中文术语 | 52Poké Wiki | 适合人工查阅；不要用无节制爬虫或直接导入受 NC-SA/第三方版权约束的内容。 |
| 导入卡牌索引 | TCGdex，辅以官方卡牌查询 | 先测目标语言和系列完整率；默认排除卡图及长文案。 |
| 核对旧世代 CSV | veekun/pokedex | 仅作次级校验，不作为当前权威或许可依据。 |
| 必须使用官方图鉴 JSON 或官方图片 | 先向权利方取得书面许可 | 不以“接口公开”“无登录”或“robots 未禁止”代替授权。 |

## 可执行的导入原则

1. 将来源身份、来源版本、抓取时间和原始主键写入导入审计，不把外部 URL 当作稳定业务身份。
2. 对 PokéAPI 采用仓库快照而非在线热依赖；对每个实体保留英文机器标识，再映射 `zh-hans`、`zh-hant` 展示名称。
3. 分离可验证事实（编号、数值、属性关系）与表达性内容（图鉴文案、插画、音频）；后一类默认不导入，除非完成权利审查。
4. 不直接下载官方站或百科的图片到产品对象存储；链接也应遵守官方条款，不绕过带时效签名的图片 URL。
5. 上线前由项目方确认最终用途是否商业，以及 Pokémon 商标和角色素材是否另有授权；第三方开源许可证无法解决全部知识产权问题。
