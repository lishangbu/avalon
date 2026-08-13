# Starllow 未映射招式与现代可用性核验

## 结论

Starllow Creature 学习列表最初包含 98 个无法直接映射到 Avalon 招式主体的名称。核验后的处理结果为：

- 21 项是现代可用招式的简体中文差异，或带有 `‡`、`USUM` 来源注记；生成器使用封闭别名表映射既有主体。
- 75 项虽然是历史正式招式，但当前规则数据全部不可用：73 项为 `Past`，`burn-up` 与 `v-create` 为 `Unobtainable`。Avalon 不创建这些主体，也不保存对应学习关系。
- 当前资料不包含 6 个不可用主体：`dragon-rage`、`heal-block`、`natures-madness`、`oblivion-wing`、`sonic-boom` 为 `Past`，`cut` 为 `Unobtainable`。
- 最终审计清单共有 81 个 Stable Code，其中 78 个为 `Past`、3 个为 `Unobtainable`；审查结果已经固化进 SQL 资料基线，不再保留供重新生成的来源 JSON。

因此，Starllow 的跨作品学习列表不能直接决定 Avalon 的启用招式集合。只有通过现代可用性门禁的招式才能形成主体和 Creature 学习关系。

## 核验口径

1. Starllow 固定快照用于确认来源名称、英文名和 Stable Code，不决定招式当前是否可用。
2. Pokémon Showdown 当前招式数据 [`moves.json`](https://play.pokemonshowdown.com/data/moves.json) 用于判定现代规则状态。核验日期为 2026-08-05；没有 `isNonstandard` 的记录视为当前标准可用，`Past` 与 `Unobtainable` 视为当前不可用。
3. PokéAPI [`move`](https://pokeapi.co/docs/v2#moves) 用于交叉核验历史正式身份与英文资源名。PokéAPI 和 Pokémon Showdown 均为社区维护资料，不冒充宝可梦公司的官方 API。
4. 当前产品明确选择“只保留现代规则可用招式”，所以历史正式身份不足以成为建档理由；规则状态变化时必须停机审查并更新 SQL 基线。

## 仍保留的 21 项显式映射

| Starllow 名称 | Avalon 名称 | Stable Code |
| --- | --- | --- |
| 爆音波‡ | 爆音波 | `boomburst` |
| 汲取 | 吸血 | `leech-life` |
| 极落钳 | 断头钳 | `guillotine` |
| 棘藤棒 | 藤棍乱打 | `ivy-cudgel` |
| 纠缠不休 | 死缠烂打 | `infestation` |
| 绝处逢生 | 起死回生 | `reversal` |
| 可怕面孔 | 鬼面 | `scary-face` |
| 空气之刃 | 空气斩 | `air-slash` |
| 泪眼汪汪USUM | 泪眼汪汪 | `tearful-look` |
| 烈火深渊 | 炼狱 | `inferno` |
| 磷火 | 鬼火 | `will-o-wisp` |
| 流沙深渊 | 流沙地狱 | `sand-tomb` |
| 森林咒术 | 森林诅咒 | `forests-curse` |
| 烧净 | 烧尽 | `incinerate` |
| 深渊突刺 | 地狱突刺 | `throat-chop` |
| 移花接木 | 欺诈 | `foul-play` |
| 玉石俱碎 | 自爆 | `self-destruct` |
| 再来一次‡ | 再来一次 | `encore` |
| 终焉之歌 | 灭亡之歌 | `perish-song` |
| 种子机关枪USUM | 种子机关枪 | `bullet-seed` |
| 咒术 | 诅咒 | `curse` |

其中 4 个只剥离注记，其余 17 个是已审查的译名差异。这里按来源写法逐项保留历史决策证据。

## 持久化约束

- 招式可用性按 Stable Code 判断，不按中文文案做模糊匹配。
- 当前不可用招式即使曾出现在升级、学习器或蛋招式列表中，也不生成主体或关系。
- 资料门禁测试保证不存在学习关系引用缺失或禁用主体，并防止不可用招式进入当前基线。
- `‡` 与 `USUM` 只表示来源注记，剥离后不会进入持久身份或展示名称。
