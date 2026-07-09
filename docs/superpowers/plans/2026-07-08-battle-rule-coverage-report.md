# 战斗规则覆盖报告

战斗系统当前以单元测试作为规则正确性的事实源，机器可读报告由
`battle-engine/src/test/kotlin/io/github/lishangbu/battleengine/BattleRuleCoverageLedgerTests.kt`
生成到 `battle-engine/build/reports/battle-rule-coverage.json`。

本报告固定以下生产验收事实：

- `totalRuleCount` 必须为 `312`，表示现代主系列规则边界内的全部行为编号。
- `coverageGroupCount` 必须为 `12`，表示规则族总数。
- `sandboxRuleHitField` 必须为 `ruleHits`，表示沙盒响应中承载规则命中的字段。
- `sandboxRuleHitFamilyField` 必须为 `familyCode`，表示沙盒命中摘要里对应规则族 code 的字段。
- `sandboxRuleHitItemField` 必须为 `itemCode`，表示沙盒命中摘要里对应规则项 code 的字段。
- `sandboxRuleHitFamilyCodes` 是沙盒规则命中允许出现的 12 个规则族 code，必须与 `groups[].code` 保持同一集合。
- `groups[].code` 是规则族稳定编码。
- `groups[].ruleNumberRange` 是该规则族覆盖的连续规则编号区间。
- `groups[].ruleCount` 是该规则族规则数量，必须等于编号区间长度。
- `groups[].minimumNamedScenarioCount` 是该规则族至少保留的公开命名场景数。
- `groups[].testClassNames` 是承接该规则族的 Kotlin 行为测试类。

本地查看方式：

```bash
./gradlew --no-daemon :battle-engine:test --tests io.github.lishangbu.battleengine.BattleRuleCoverageLedgerTests
cat battle-engine/build/reports/battle-rule-coverage.json
```

新增规则时只允许同时修改账本测试、对应行为测试和本文档描述。普通资料新增不需要调整这里。
