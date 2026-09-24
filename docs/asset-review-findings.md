# 动画图像初筛结果与补图方向

本稿记录 [bsymg动画清单](bsymg-animation-assets.csv) 的实际目视结果，源为 `/home/baoying/文档/study/bsymg/gumballs_M143679_20260918_images_decrypted`。只审阅其中 `assets/res/images/animation` 的560张PNG；道具、UI、头像、角色大图等其他目录未由本稿覆盖。

剩余539张逐格看过18张联系表，16张难判图另行放大。原21张判据原样保留。清单现在为309张完整单体候选、165张待提取图集、44张物件、26张效果、16张难判。各项来源路径、SHA256、尺寸与透明像素标记均保留，public_path全部为空。本稿没有复制图片到前端，也不改变150形态、60家族预算。

## 哪些可以进入下一轮形象比较

下表文件名均位于上述animation目录，保留源名供核对；它们不是拟定的游戏名称。这里的“可比较”只表示目视具有完整构图，尚未完成黑白背景边缘、缩小可读性、风格一致和最终归属验收。

| 形态方向 | 目视完整候选示例 | 可以从图上确认的内容 | 不能从图上直接推出的内容 |
| --- | --- | --- | --- |
| 岩石与木石 | dungeon1021_6.png、sky_boss_colossus0.png、ancient_of_lore.png | 石块双臂、苔叶石身、巨树与附属结构可辨 | 三张不是天然三段进化；巨树图尺寸与细节更接近首领 |
| 鸟类 | dungeon1021_4.png、summon_365.png、summon_366.png、summon_367.png、summon_370.png | 金羽展翼鸟、长尾鸟、雄鸡、黑鹰、橙色火鸟均完整 | 配色不直接决定属性；不同鸟型不能随意接成同一家族 |
| 兽类 | lion2.png、huge_toad.png、pony.png、rabbit2.png、tripletail_fox.png、thunder_deer.png | 狮、蟾、幼马、兔、多尾狐、枝角鹿完整 | 不能为了凑缺图将一张狮子对应全部三段或把绿色鹿自动归雷 |
| 狼狐与虎兽 | summon_363.png、summon_364.png、summon_369.png、dungeon1024_1.png | 蓝白犬狐、金色多尾狐、蓝色卷尾虎、暗色虎完整 | 多尾、换色不自动代表更高种族值或进化等级 |
| 骨兽与游魂 | skeleton_dog.png、phantom.png、fear_soul.png、ghost1.png、ghost2.png | 骨犬、双臂幽魂与圆底灵体边界清晰 | 没有理由把每个带骨图同时算龙族与亡灵族 |
| 机械兽与机兵 | gimmick_beast.png、gimmick_puppet.png、robot_b17.png、terminator.png、micro_detector.png | 机械兽、持盾机体、钻角机体和探测机完整 | 驾驶员穿重甲不等于其本体属于机械族；载具不拆成两只幻兽 |
| 龙与蛇 | golden_drak.png、pterodactyl.png、hydra.png、outland_boss_elars.png、outland_boss_tyrannosaurus.png | 幼龙、翼龙、多首蛇、鳍翼蛇龙与大暴龙完整 | 单图多头仍是一个主体；编号和大小不能证明进化关系 |
| 甲壳与虫兽 | baneling2.png、fengkuangjuanzu.png、gaixiachongzu.png、spine.png、enigma_beast.png | 翼虫、长肢独眼虫、肥厚虫体、骨甲多足体与钳足体完整 | 蜘蛛类、虫类与水生甲壳体不能因节肢轮廓相似而硬接 |
| 可用首领形象 | S07_BOSS.png、S08_BOSS.png、four_arms_balrog.png、outland_boss_shadow_lord.png | 海盗合成姿态、赤色角翼重甲、四臂火怪、黑烟重甲灵体完整 | 文件的BOSS字样不授权取代巴尔萨克、莫德雷等既定人物设定 |
| 可用人物形象 | emergency_physician.png、S07_mechanic.png、white_robe2.png、woman_with_cloak.png | 机械医师、工具匠、白袍老者和绿斗篷人物完整 | 不凭外观就把具名剧情NPC换人；NPC不计捕捉物种规模 |

小型完整单体可优先做下一轮缩放与边缘验收，大型首领图单独比较，不强制所有素材缩成同一个50像素框。完整候选中很多是现代武器、航天、武侠或节庆角色，现阶段不因此把相应世界设定一起引入阿瓦隆。

## 已发现的同主体与变体关系

以下为目视相似结论，尚不是全目录完成的主体去重结果。抽查12对原图，RGBA逐像素比较均不完全相同，水平镜像后也非逐像素完全一致，证明仅靠文件SHA或精确像素去重会漏掉它们；不能将本表写成“源文件相同”。

| 来源关系 | 实际观察 | 采用时的处理 |
| --- | --- | --- |
| baxiaodie/baxiaodie.png ↔ baxiaodie.png | 同一角翼女性，尺寸与留边不同 | 作为同主体大小／构图候选，先选一版 |
| summon_353/summon_353.png ↔ summon_353.png；summon_354同结构一对 | 同一儿童或红袍人物的小图与较大留边图 | 每对不能占两个角色或物种名额 |
| taiyanghua/taiyanghua.png ↔ taiyanghua.png | 同一向日葵人物，不同尺寸与留边 | 成组比较后择一 |
| S07_mechanic.png ↔ sky_wosbark.png | 同一白发工具匠，小图125×114，大图228×207 | NPC主体合并看待，不能当机械两段进化 |
| gimmick_beast.png ↔ jiguan_beast.png | 同一绿铜机械兽，不同尺寸 | 不分配给同一家族两个成长阶段 |
| enigma_beast.png ↔ ghost3.png | 蓝色甲壳钳足造型高度相同 | 精确像素虽不同，也须先做主体去重 |
| boss_walking_dead.png ↔ boss_walking_dead2.png；licker.png ↔ licker2.png | 主体反向，近似镜像；非精确镜像像素 | 朝向变体不算新物种 |
| boss_gorilla_with_glass.png ↔ boss_gorilla_without_glass.png | 猩猩主体基本相同，眼镜不同 | 装扮变体，不当两种新首领物种 |
| robot_walle.png ↔ robot_walle2.png | 红蓝换色，同一履带钻臂结构 | 优先皮肤或敌我外观，不当进化体 |
| lollipop_child.png ↔ villager_child.png | 同一戴帽持糖杖圆身人物，尺寸不同 | NPC合并候选 |
| S07_trooper.png ↔ robot_g3.png | 箱头持枪结构相近且配色不同 | 尚未证明完全同主体，下一轮放大比较装备边界 |
| S07_biochemical_soldier.png 与 defective_variants系列 | 同一类面罩装甲，持刃与配色变化 | 装備变体优先；不按每张一只计数 |
| dungeon1019_c/d/g/j/m/q/s.png | 青色幽魂持锤盾弩剑杖枪角号 | 同系职责变化可以设计敌人兵种，不必建七条幻兽进化线 |

12对像素抽查覆盖表中前10行的具体配对（两组含两个独立配对）；最后三行是额外目视关系，不声称完成逐像素比较。后续主体去重应加入透明边裁切、比例、方向、细节和人眼复核，不能只设一个相似哈希阈值便删除源图。

## 现有缺图形态的比较建议

现行 [150形态](current-creature-design.md) 中仍有112项缺图。下面只提出成组审阅方向，**没有正式绑定任何一项**。最终要同时核外形、现有家族连续性、属性表现、已有OL风格与未来第三形态，不能图先占位后再修改所有物种设定迁就它。

| 现有家族／形态 | 优先查看候选 | 仍缺什么 |
| --- | --- | --- |
| STONE_GOLEM → STONE_ARMOR_GOLEM → ROCK_GIANT | dungeon1021_6.png、sky_boss_colossus0.png | 石块比例与苔叶特征能否延续，已有小岩怪不可无理由换造型；暂不足一条三段统一家族 |
| SPROUT家族的古木卫 | ancient_of_lore.png 与已经交付的OL树人候选 | 现有方案已有树人，先比对职责、尺寸与整体风格，勿覆盖已确认形象 |
| BONECLAW_BEAST → BONEARMOR_BEAST | skeleton_dog.png；原已审骨龙仅作另一结构参考 | 骨犬只有一张，进化后的骨甲强化形态仍缺；骨龙不能直接接到犬兽之后 |
| PATROL_AUTOMATON → SENTINEL_AUTOMATON | gimmick_puppet.png、robot_b17.png、原已审S07_pioneer_machinery.png | 守卫家族应确定同一躯干和关节语言；三张分别是不同机体，不自动构成进化 |
| COPPER_AUTOMATON → THUNDERBRONZE_AUTOMATON | 铜绿机体与原已审维修机械 | 雷属性视觉与铜质机体连续性尚未核定；维修机不能仅因有图就改成纯输出职责 |
| CRYSTALHORN_WHELP → CRYSTALCROWN_DRAGON | 原已审magic_dragon_1.png、magic_dragon_5.png | 角翼、晶甲结构与光属性能否一致，尚需成对大图核验 |
| MOLTENSCALE_WHELP → MOLTENARMOR_DRAGON | 原已审magic_dragon_6.png | 单张不能同时填幼体与进化体；保留至少一形态缺图，不硬接绿色龙 |
| WHITEWING_SERPENT → CLOUDWING_SERPENT | outland_boss_elars.png、hydra.png只作排除性比较 | 前者鳍翼且青色，后者多头无白翼，都不能直接填白翼风蛇；仍缺匹配形象 |
| SHADOWTAIL_FOX家族 | tripletail_fox.png、summon_363.png、summon_364.png | 颜色、尾形和耳型差异大，不能拿三个不同狐样硬凑暗属性三段 |
| GRAYMANE_LION家族 | lion2.png | 棕色单张狮与灰鬃、石鬃、岩鬃描述尚不相合；仅作体态参考 |
| BLUE_SPIDER家族 | spine.png、enigma_beast.png | 骨甲蜘蛛和钳足体与蓝甲蛛水属性设定都不直接相合，暂不绑定 |

这份建议宁可准确保留缺口，也不以“图片已看过”替代进化关系设计。需要新绘或统一改绘的家族应成组处理，后续正式文件使用语义名称和可请求URL，来源编号只留清单。

## 图集与难判项

165项图集中包含整只动物的多帧（如circus_tiger、pigeon）、三形态并列（tideng系列），也包含只有分离肢体与特效的骨骼图集。前者可以在匹配准确帧描述后择姿态，后者还需位置、锚点、旋转、层级与动作时刻重建。两类不能用同一种中心裁切一刀处理。原9项的435帧结构检查不覆盖其余156项。

16项难判为：black_chess_soul、city_hero、dark_armor、dungeon1022_6、earth_old、emil_head、nano_luozu、sadako、slime_shadow_samurai_summon、summon_407、summon_411、summon_421、sword_soul、ultraman、ultraman2、white_chess_soul（均为.png）。放大后仍存在头像与全身边界、浮空身体是否完整、灰色召唤投影或多主体组合的疑问，维持 NEEDS_VISUAL_CLARIFICATION。不能因已放大就计入可用单体。

公开交付前还需：确定实际主体、筛除换色／镜像重复、确定用途与现有家族、校验透明边缘与目标尺寸、必要时统一画风，然后转换并比较文件体积。源PNG不删除，不能假定WebP永远更小。本轮未重建前端、未重启预览、未导入后端内容。
