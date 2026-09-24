# 2026-09-23产物与完整投入协议验收

2026-09-23。执行范围为该次JAR验收和制造／副本投入补账；未部署到用户运行环境，未调整正式配方或平衡数值。

后续代码与产物已有更新，本页哈希及用例数量仅对应下述历史构建。后续验证见[移动端操作验证](mobile-experience-validation.md)与[合作页面验证](cooperation-browser-validation.md)。

## 该次构建产物

`./mvnw -q -pl avalon-app -am -DskipTests package`重新构建前端及后端。
JAR SHA256：`b56375d678e03899daba7cf7f050af45e01dc7e01a9fd758929fdde9eff1f40f`。

- `PackagedContentIT`创建空PostgreSQL容器，真实JAR执行迁移并启动；注册、创角、物品详情和学院课程经认证HTTP验证。
- 学院生命药实际消耗12→11，普通库存不变；普通详情入口拒绝两种学院专用药品。
- 首页及14个入口静态资源可访问；JAR内69份内容配置和Schema逐字节匹配源码，无已删除料理或封印球资源。
- `LifeSkillsBrowserIT`用隔离存档准备边界数据，浏览器访问独立JAR进程：桌面／手机视口14项全部通过，覆盖学习、遗忘、制造、绑定材料、在线采集、农田、料理、钓鱼及材料市场；数字后缀药品显示为“药品”。
- 前端测试类型检查和600个Java文件导包检查通过。测试容器及子进程结束后清理。

复验（先打包）：

```bash
AVALON_PACKAGED_CONTENT=1 AVALON_LIFE_BROWSER=1 PLAYWRIGHT_CHROMIUM_EXECUTABLE=/usr/bin/google-chrome ./mvnw -q -pl avalon-app -am -Dskip.frontend=true -Dtest=PackagedContentIT,LifeSkillsBrowserIT -Dsurefire.failIfNoSpecifiedTests=false test
```

证据在`outputs/current-delivery/`：`package.log`、`packaged-tests.log`、`packaged-content-result.json`、`artifact-content-check.json`、`empty-database-boot.log`、`life-packaged.log`、`life-browser.log`。
浏览器视口不是实体手机验收；预置等级、资源和任务事实不代表自然成长流程。

## 从同一城镇出发并返回的投入协议

`LifeCooperationTest.fullTownProtocolIncludesReplacementLogisticsAndWaitingSensitivity`通过真实命令采集原料、恢复、移动、合并交易、制造并返回CITY_SHOP。15级伙伴，专业和初始工具已具备，不计首次学习与置办。

制造采用所有供应者的串行劳动累计：每条命令2秒，每秒采集心跳，全部供料者结束后恢复并返回城镇。原料采集144轮、58条命令、5次恢复，心跳1328秒，直接协议1444秒。采集中报废另外按每次5秒计停止、换备用工具、重新派遣及首个心跳；末轮也计，因此是保守上界。

五种铜工具补造均执行独立的真实采集→恢复→转场→交易→制造→交付小链，包括铜针依赖制药棒的循环。按每次操作的正式报废概率求解递归补货成本，并检查收敛残差；每把工具补造约91.42秒，包含再次报废。工具耗尽后停止、使用随身备用工具重启另有事务测试。这里使用有备用库存的长期补货策略，不能解释成任何新玩家空手起步的单次时长；单件小批补造的余料不折现，也不宣称最优路线。

副本以同等级固定队伍四职业组合各128次引擎样本为基准，每队伍回合5秒；队长入本创建、确认、准备、发起4条命令，本内14条。每天首次出发满资源，当天再次进入时增加入口休整；当天结束按实际8步路线经恢复点回城并恢复。全队总账另计其他成员的加入、确认、准备、行动、休整和移动，宝箱仅归有资格的发起者。

指定模板概率10%，期望10次成功发起；每日最多3次，期望跨越3.690个可发起日。日额度不换算为固定小时，也不与主动秒数相加。换队长会改变谁取得宝箱，不能据此忽略指定接收者的日额度。

| 同城镇协议 | 秒数 | 分钟 |
| --- | ---: | ---: |
| 赤铜长剑，含补造物流及保守换工具开销 | 1513.88 | 25.23 |
| 指定副本装备，队长主动投入 | 1005.06～1010.52 | 16.75～16.84 |
| 指定副本装备，全队累计主动劳动 | 2775.17～2791.57 | 46.25～46.53 |

制造总劳动／副本队长投入为**1.498～1.506**：三个组合达到1.5下界，一个略低，不能宣称全部达到1.5～2倍。
制造总劳动／副本全队劳动为**0.542～0.546**，这是同一劳动统计口径；前一个比值是既有目标口径，分子分母参与人数不同，不能称为人均比较。
实际多人并行可以缩短制造墙钟时间，报告的串行累计值不代表并行完成时间。

## 等待敏感性

等待作为显式情景参数：每次材料／工具交付等待0、60、300秒，每次副本组队等待0、60、300秒，共四职业36个场景。递归补造后期望交易4.208次。行情和玩家响应没有固定服务端时长，以下不是实测分布。

| 每次交易等待 | 每次组队等待 | 制造／队长协议投入 |
| ---: | ---: | ---: |
| 0秒 | 0秒 | 1.498～1.506 |
| 60秒 | 0秒 | 1.748～1.757 |
| 60秒 | 60秒 | 1.097～1.101 |
| 300秒 | 300秒 | 0.692～0.693 |

角色思考、找单、网络延迟可以追加到对应操作耗时或等待参数，不能虚构固定市场均价或等待时间。真人分布、缺货时无备用工具的停工和失败副本仍需实际样本。本轮已补齐可由代码和正式流程核算的成本，并证明旧基准不能支持“完整投入全部达标”。暂不为约0.1%的边界偏差调整配方。

复验：

```bash
./mvnw -q -pl avalon-app -am -Dskip.frontend=true -Dtest=LifeCooperationTest,ManufacturingInvestmentTest -Dsurefire.failIfNoSpecifiedTests=false test
```

最终7项通过，副本大厅、进度及采集专项此前本轮27项通过；首次补造核算发现漏计铜棒依赖，修正后重跑上述7项。
报告：`outputs/current-delivery/full-investment-protocol.json`、`copper-sword-town-operations.json`、`manufacturing-investment.json`、`investment-tests-final.log`。
旧基础原料模型仍用于属性及依赖检查，但已移除其不完整协议的1.5～2倍达标断言，完整协议逐场景输出`withinTarget`，不隐藏未达标结果。
