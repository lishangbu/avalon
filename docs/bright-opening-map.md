# 开篇道路、设施与解锁

本文件说明[30项开篇任务](bright-opening-design.md)使用的道路与设施，地点以正式内容目录的 opening.json 为准，现场动作以 scene-actions.json 为准。移动始终经过一条相邻边，不通过任务提交自动传送。

## 地点与前置

| 地点ID | 显示名 | 进入前置 | 常驻接待与用途 |
| --- | --- | --- | --- |
| VILLAGE_HOME | 晨露村家门 | 无 | 出生地与远行准备 |
| STONE_PATH | 石板路 | 无 | 道路观察，无需复制广场的特林 |
| SQUARE | 村中广场 | 无 | 特林、庆典筹备与成果呈现 |
| ELDER_HOUSE | 村长家 | 无 | 阿德、介绍信与远行交谈 |
| WEAPON_SHOP | 武器店 | 无 | 布鲁诺、信物与装备服务 |
| PET_CENTER | 幻兽中心 | 无 | 梅莎、结契、恢复与仓库存取 |
| SUPPLY_SHOP | 杂货铺 | 无 | 补给与一次性任务物领取 |
| RIVERSIDE | 河边小路 | 无 | 两个观察点、巢外散石与野外捕捉 |
| FOREST_EDGE | 东侧林口 | BQ009完成 | 林口引路人、货车痕迹、通向洞穴与平原 |
| CAVE_ENTRANCE | 山洞入口 | BQ010完成 | 为庆典货车观察洞旁窄路；完成脚印与攻击痕迹观察后可主动挑战，首次开战同步结算BQ011；不进入巢穴 |
| PLAIN_SIGN | 平原路标 | BQ014完成 | 商旅联系、环境观察与路标 |
| STATION_OUTSKIRTS | 小站歇脚处 | BQ015完成 | 看护人、水源与驮兽照护 |
| ACADEMY | 诺姆学院 | BQ016完成 | 克林、费恩与米菈按任务阶段接待 |
| ACADEMY_SHOP | 学院补给处 | BQ017完成 | 正式补给与独立标识的临时课程入口 |
| ACADEMY_PETS | 学院疗养处 | BQ017完成 | 恢复、仓库存取与临时课程入口 |
| GNOME_CITY | 诺姆城 | BQ024完成 | 市集商人与城市交流 |
| CITY_SHOP | 诺姆城商店 | BQ024完成 | 补给与装备服务 |
| CITY_PETS | 诺姆城幻兽中心 | BQ024完成 | 恢复与仓库存取 |
| CITY_OUTSKIRTS | 诺姆城外围 | BQ025完成 | 三个观察点；BQ026及清障完成后开放固定8级阻路遭遇，BQ029后开放11～15级普通探索及对应双怪、精英入口 |

任务接取地点与目的地门槛不能互锁：BQ010在林口完成后开洞口；BQ015在路标完成后开小站；BQ016在小站完成后开学院；BQ024在学院完成后开城；BQ025在城内完成后开郊野。不要求先取得BQ029冒险资格才能完成资格调查。

## 道路

所有下列边双向，目的地前置决定能否首次进入；已完成事实不撤销，返乡无新门槛。

```text
VILLAGE_HOME — STONE_PATH — SQUARE
STONE_PATH — RIVERSIDE — FOREST_EDGE
SQUARE — ELDER_HOUSE / WEAPON_SHOP / PET_CENTER / SUPPLY_SHOP
FOREST_EDGE — CAVE_ENTRANCE
FOREST_EDGE — PLAIN_SIGN — STATION_OUTSKIRTS — ACADEMY
ACADEMY — ACADEMY_SHOP / ACADEMY_PETS / GNOME_CITY
GNOME_CITY — CITY_SHOP / CITY_PETS / CITY_OUTSKIRTS
```

所有设施通过上述道路抵达，任务完成后家乡仍可回访。

地图当前地点展示实际可见的NPC与操作。多个接待者以稳定npcId绑定步骤，并按接待条件出现。人物对话与机构服务分离，课程导师不在每家商店复制出现。

## 观察与个人场景

| 地点 | 动作键 | 前置与结果 |
| --- | --- | --- |
| RIVERSIDE | BQ006_STONE_TRACE_A / BQ006_STONE_TRACE_B | 顺序观察两处石痕，齐全后确认巢域 |
| SQUARE | BQ008_COLLECT_STONES | 两处石痕已记录；从特林处领取一次性任务散石 |
| PET_CENTER | BQ009_SUPPLY_REVIEW | 散石已领取；阅读出行补给说明 |
| FOREST_EDGE | BQ010_CART_TRACE_A / BQ010_CART_TRACE_B | 已读补给说明；顺序调查两处车辙 |
| CAVE_ENTRANCE | BQ011_FOOTPRINTS / BQ011_ATTACK_MARKS | 车辙记录齐全；顺序调查脚印和攻击痕迹 |
| PLAIN_SIGN | BQ015_SIGN / BQ015_WATER_ROUTE | 阅读路标后确认饮水路线 |
| STATION_OUTSKIRTS | BQ016_INSPECT / BQ016_WATER / BQ016_DELIVER_CARE | 饮水路线已记录；依次检查驮兽、确认水源、交接用品 |
| ACADEMY | BQ017_ARRIVAL / BQ017_PRESENT_INTRO / BQ024_PLAN | 交接完成后查看门牌、出示介绍信；课程结束时选择调查计划 |
| GNOME_CITY | BQ025_CITY_ARRIVAL | 调查计划已记录；确认城内街口 |
| CITY_OUTSKIRTS | BQ026_HABITAT / BQ026_FOOD / BQ026_FLOWERS / BQ027_CLEAR_PATH | 确认城内街口后，自由选择顺序观察活动范围、食物、花期，三项齐全后清路 |
| ACADEMY | BQ028_REPORT | 已清路；提交保留巢域的报告 |
| GNOME_CITY | BQ030_MEAL | 报告已记录；参加晚餐 |

动作遵循[行动契约](bright-action-contract.md)。采集点按个人状态，不与其他玩家抢点。广场的装饰在BQ008后出现，庆典在BQ013完成后可回看；回看不再次发奖。每次描述由事实选定，不用服务器日期自动跳过庆典。

## 正式内容与存档边界

正式开篇使用这 19 个地点。出生地和城市的稳定编号为 VILLAGE_HOME、GNOME_CITY，内容、代码及测试夹具使用相同编号。

角色、会话与测试夹具统一使用正式地点编号。现场动作的完成不替代任务前置、课程完成或真实战斗胜利等额外判据。

## 验收

新号依次到达全部19个节点，任何时刻可返回已开放家乡。失败重试后可回恢复点；在五课任意完成顺序下均能通过BQ024进入城市。BQ029前能够完成城郊调查，BQ029后不会再次锁城。非相邻地点点击只展示信息，放大地图同样只允许相邻移动。

NPC身份绑定、观察幂等和满包任务奖励有服务测试覆盖；手机与桌面已通过BQ001～BQ030连续流程、自由观察顺序与返乡恢复持久化。地图表本身不替代实际验收，最新证据见[实现进度](implementation-progress.md)。
