# Starllow「无法映射」特性核验

> 核查日期：2026-08-05。范围是当日授权导入中曾被列为无法映射的九个中文值；原始快照已在结果固化进 SQL 基线后移出运行仓库。

> 实施状态：同日已在首套资料中新增四个正式特性主体，并在 Creature Seed 中完成四面具消歧、两个译名映射和两个占位值过滤；原 9 项特性未映射告警已归零。

## 结论

“无法映射”是当时 Avalon 特性主体集合与来源快照之间的身份覆盖缺口，并不等同于来源错误。九个值中：

- **4 个是正式特性，且是《宝可梦传说 Z-A》新增超级进化形态的特性**：超级日光、贯穿钻、辣椒喷发、龙皮肤；应新增对应 `game_ability` 主体，而非丢弃。
- **1 个是正式特性且已能映射**：面影辉映；Avalon 已按厄诡椪面具形态拆成四个主体。
- **2 个是正式特性的中文译名差异**：诱爆应映射 `Aftermath`／引爆，咒术之躯应映射 `Cursed Body`／诅咒之躯。
- **2 个是占位值，不是特性**：未知、无；不得创建 `game_ability`。

这里的“正式”有两层可复核证据：PokéAPI 的固定数据接口给出了英文机器名、`zh-hans`／`zh-hant` 本地化名和关联 Pokémon；[Pokémon Showdown 的可审计数据源代码](https://github.com/smogon/pokemon-showdown/tree/6a1836dd71c0718e923206f3d089e61074410868/data)在同一批新增形态上使用相同英文特性名。二者均不是权利方发布的开发者 API 或授权声明；前者的使用边界见仓库已有的[中文资料来源调查](official-pokemon-chinese-resources.md)。官方中文图鉴可人工确认既有 Pokémon 与厄诡椪的面具形态（如[台湾官方厄鬼椪图鉴](https://tw.portal-pokemon.com/play/pokedex/1017)），但该网页图鉴当前未呈现本报告所涉的 Z-A 新超级形态，不能把“页面未列出”误判为特性不存在。

## 逐项核验

| Starllow 值 | 核验结论与应映射主体 | 英文名／中文名 | 关联宝可梦 | 逐项证据 |
| --- | --- | --- | --- | --- |
| 超级日光 | 正式新增特性；新增 `mega-sol` 主体。 | `Mega Sol`／超级日光（繁中：超級日光） | 超级大竺葵（`meganium-mega`） | [特性 310](https://pokeapi.co/api/v2/ability/mega-sol/)给出三种语言名；[形态资料](https://pokeapi.co/api/v2/pokemon/meganium-mega/)把该形态的特性指定为 `mega-sol`；[Showdown 特性定义](https://github.com/smogon/pokemon-showdown/blob/6a1836dd71c0718e923206f3d089e61074410868/data/abilities.ts#L2548-L2565)和[形态定义](https://github.com/smogon/pokemon-showdown/blob/6a1836dd71c0718e923206f3d089e61074410868/data/pokedex.ts#L3482-L3497)交叉一致。 |
| 贯穿钻 | 正式新增特性；新增 `piercing-drill` 主体。 | `Piercing Drill`／贯穿钻（繁中：貫穿鑽） | 超级龙头地鼠（`excadrill-mega`） | [特性 308](https://pokeapi.co/api/v2/ability/piercing-drill/)与[形态资料](https://pokeapi.co/api/v2/pokemon/excadrill-mega/)；[Showdown 特性定义](https://github.com/smogon/pokemon-showdown/blob/6a1836dd71c0718e923206f3d089e61074410868/data/abilities.ts#L3272-L3285)和[形态定义](https://github.com/smogon/pokemon-showdown/blob/6a1836dd71c0718e923206f3d089e61074410868/data/pokedex.ts#L9927-L9942)。 |
| 辣椒喷发 | 正式新增特性；新增 `spicy-spray` 主体。 | `Spicy Spray`／辣椒喷发（繁中：辣椒噴發） | 超级狠辣椒（`scovillain-mega`） | [特性 311](https://pokeapi.co/api/v2/ability/spicy-spray/)与[形态资料](https://pokeapi.co/api/v2/pokemon/scovillain-mega/)；[Showdown 特性定义](https://github.com/smogon/pokemon-showdown/blob/6a1836dd71c0718e923206f3d089e61074410868/data/abilities.ts#L4456-L4469)和[形态定义](https://github.com/smogon/pokemon-showdown/blob/6a1836dd71c0718e923206f3d089e61074410868/data/pokedex.ts#L18327-L18342)。 |
| 龙皮肤 | 正式新增特性；新增 `dragonize` 主体。 | `Dragonize`／龙皮肤（繁中：龍皮膚） | 超级大力鳄（`feraligatr-mega`） | [特性 309](https://pokeapi.co/api/v2/ability/dragonize/)与[形态资料](https://pokeapi.co/api/v2/pokemon/feraligatr-mega/)；[Showdown 特性定义](https://github.com/smogon/pokemon-showdown/blob/6a1836dd71c0718e923206f3d089e61074410868/data/abilities.ts#L1026-L1048)和[形态定义](https://github.com/smogon/pokemon-showdown/blob/6a1836dd71c0718e923206f3d089e61074410868/data/pokedex.ts#L3602-L3617)。 |
| 面影辉映 | 正式特性，**不是缺少主体**。保持四个既有形态主体：`embody-aspect-teal`、`-hearthflame`、`-wellspring`、`-cornerstone`。来源使用不带后缀的总称，需按 Creature Form 分派。 | `Embody Aspect`／面影辉映（繁中：面影輝映） | 厄诡椪太晶化的碧草、水井、火灶、础石面具 | [特性 303](https://pokeapi.co/api/v2/ability/embody-aspect/)给出名称；[官方厄鬼椪图鉴](https://tw.portal-pokemon.com/play/pokedex/1017)列出四个面具形态；[Showdown 的四个形态特性定义](https://github.com/smogon/pokemon-showdown/blob/6a1836dd71c0718e923206f3d089e61074410868/data/abilities.ts#L1188-L1239)区分每个面具的能力提升。这也与 Avalon 现有四个主体相符。 |
| 未知 | 来源占位值，不是特性；不建主体、不进行别名映射。 | 无 | Starllow 的 23 条“超级” Form 记录使用该值，包括超级阿勃梭鲁 Z、超级姆克鹰、超级烈咬陆鲨 Z、超级路卡利欧 Z、超级席多蓝恩、超级达克莱伊、超级蜈蚣王、超级头巾混混、超级麻麻鳗鱼王、超级火炎狮、超级乌贼王、超级龟足巨铠、超级毒藻龙、超级基格尔德、超级具甲武者、两种超级玛机雅娜、超级捷拉奥拉、超级列阵兵、三种超级米立龙与超级戟脊龙。 | Starllow 没有同名 `abilities/未知.json`，且该值没有可供映射的英文机器名或机制。相反，已公布的新增形态会在可审计资料中有明确特性字段，例如[超级大竺葵](https://pokeapi.co/api/v2/pokemon/meganium-mega/)。因此它是尚未补全的来源字段，不是名为“未知”的官方特性。 |
| 无 | 来源占位值，表示该 Form 没有可映射的特性主体；不建主体，也不擅自继承普通形态特性。 | 无 | 搭档皮卡丘、搭档伊布 | Starllow 没有 `abilities/无.json`，因此它不是名为“无”的特性实体。PokéAPI 的 [`pikachu-starter`](https://pokeapi.co/api/v2/pokemon/pikachu-starter/) 与 [`eevee-starter`](https://pokeapi.co/api/v2/pokemon/eevee-starter/) 会列出物种常规特性，但这不能证明《Let's Go》中的搭档形态实际启用了特性机制，故只把“无”作为不可建主体的来源占位值。 |
| 诱爆 | 译名差异；映射既有 `aftermath` 主体，Avalon 正式中文名为“引爆”。 | `Aftermath`／引爆（Starllow：诱爆；繁中：引爆） | 霹雳电球、顽皮雷弹（含洗翠形态）、飘飘球、随风球、臭鼬噗、坦克臭鼬、破破袋、灰尘山（含超极巨化） | [特性 106](https://pokeapi.co/api/v2/ability/aftermath/)同时给出英文、简繁中文及关联 Pokémon；[Showdown 定义](https://github.com/smogon/pokemon-showdown/blob/6a1836dd71c0718e923206f3d089e61074410868/data/abilities.ts#L78-L89)使用同一英文机器名。Avalon 生成脚本已显式将“诱爆”匹配为“引爆”。 |
| 咒术之躯 | 译名差异；映射既有 `cursed-body` 主体，Avalon 正式中文名为“诅咒之躯”。 | `Cursed Body`／诅咒之躯（Starllow：咒术之躯；繁中：詛咒之軀） | 耿鬼（含超极巨化）、阿罗拉嘎啦嘎啦、伽勒尔太阳珊瑚、怨影娃娃、诅咒娃娃、雪妖女、轻飘飘、胖嘟嘟、来悲茶、怖思壶、多龙梅西亚、多龙奇、多龙巴鲁托 | [特性 130](https://pokeapi.co/api/v2/ability/cursed-body/)给出英文、简繁中文及关联 Pokémon；[Showdown 定义](https://github.com/smogon/pokemon-showdown/blob/6a1836dd71c0718e923206f3d089e61074410868/data/abilities.ts#L774-L788)使用同一英文机器名。Avalon 生成脚本已显式将“咒术之躯”匹配为“诅咒之躯”。 |

## 对资料转换的含义

1. 将原“9 个无法映射”重分类为：4 个待新增正式主体、1 个现有形态拆分映射、2 个既有别名映射、2 个来源占位值。
2. 仅创建四个新增主体时，同时记录英文 Stable Code：`mega-sol`、`piercing-drill`、`spicy-spray`、`dragonize`；不要以中文显示名作身份。
3. 对“未知”“无”保留来源审计和待补全标记；在补齐具体 Form 的官方特性前，不以猜测或通用 Species 特性替代。
4. 本次结论只解决特性身份与名称；完整战斗规则仍须按 Avalon 的结构化规则和测试单独实现，不能直接把来源说明文案当作规则。
