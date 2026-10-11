# 当前幻兽内容交付口径

[选表清单](current-creature-design.json)明确本轮1～50级设计采用哪些文件。它是设计交付入口，不是内容热更新版本，也不是可以直接导入服务器的运行配置。配置是否上线必须检查实际运行文件，不能由本清单推断。

## 当前规模

| 内容 | 数量 | 采用文件 |
| --- | ---: | --- |
| 七维成长与进化形态 | 150形态、60家族 | creature-regional-growth-candidates.csv |
| 单个体特性槽 | 450槽、72种效果 | creature-complete-ability-slots.csv |
| 破格与复制交互 | 72项 | complete-ability-interaction-matrix.csv |
| 学习资格 | 150形态 | creature-poison-learning-candidates.csv |
| 野外初始技能与书池 | 各60物种 | wild-poison-initial-skills.csv、wild-poison-loot-skill-candidates.csv |
| 技能标签及附带效果策略 | 23项，含普攻 | poison-skill-contact-candidates.csv、skill-secondary-effect-policy.csv |
| 普通遭遇 | 29池 | 清单中四份encounters文件 |
| 进化配方与材料 | 90转换、68材料 | evolution-wild-recipes.csv、evolution-wild-materials.csv |
| 研究服务 | 5站 | research-station-locations.csv、research-commissions.csv |
| 战斗天气 | 晴、雨、沙暴、雪 | battle-four-weather-candidates.csv |
| 战斗场地 | 电气、青草 | battle-terrain-candidates.csv |

基础18技能的威力与耗魔使用skill-mana-candidates.csv；两拳、连环刺、毒液分别读取punch-skill-candidates.csv、multihit-skill-candidates.csv、poison-skill-candidates.csv，普攻沿既定无属性物理50、零耗魔。这些片段尚未统一成包含全部执行字段的运行技能结构，不能只合并表头就当作完整技能配置。

## 使用约束

公开七维基础值及合计均为整数。设计生成器保留插值中间精度，只在交付基础值时四舍五入一次（0.5向上）；正式成长计算使用这些整数，不暗中读取取整前精度。最终面板沿成长公式向下取整，系数、倍率及复合比较指标可以保留小数。前置输入表中的小数仅是生成依赖，不是另一套公开属性。

每类采用上表的一份完整候选，不把32、48、52、54、57、58、61、62、66、69、71和72效果表同时加载。阶段槽位、阶段交互矩阵、旧材料配方及已失效的数值报告已删除。保留的生成输入只用于说明当前预算的推导，不是可任选的当前规格。天气表不包含学习池，所以不能因为文件名较新就用它替换无关内容。

物种身份来自id，家族来自evolution_family，特性身份来自ability_key；名称只负责展示。普通与隐藏槽均为一个效果，概率45／45／10；进化继承槽位。当前家族相邻进化均保留学习资格，不自动学会新增技能。

所有60基础种都有普通野生幻兽进化材料来源；90项转换与七维成长的相邻形态和最低等级对齐。材料每链通用6份＋家族6份；研究站是补缺渠道，不是所有进化的强制购买地点。

## 验证与剩余交付

运行`python3 scripts/current-creature-design-audit.py`，跨所选表检查：ID唯一、元素与等级范围、七维正值、家族相邻进化、学习继承、初始技能资格、特性名称与身份对应、槽位概率、遭遇池权重与全物种来源、材料配方和报价门槛、研究记录池与天气特性引用。

该命令不生成数据、不启动应用、不重跑旧内容生成器。结构检查通过后仍缺：统一动作与状态执行、所有物品正式注册、地图道路与任务可达性、图片覆盖、同级野生幻兽与首领完整战斗、多人资源经济、真实试玩时长，以及全部候选与运行配置的实施核对。天气和技能扩展使部分早期局部报告覆盖范围变窄，引用报告时必须检查其输入表，不能只引用通过次数。

当前40项经典代表机制均有适配规则，合并原32项自定义效果为72种候选。文件名complete仅指这批代表清单与槽位合并，不表示原作全部特性或项目整体设计已经完成。新增效果仍须同步交付入口、来源和交互矩阵；当前重点是统一执行、组合验证与其余主线/地图/经济交付。
