# 战斗规则覆盖报告

`BattleRuleCoverageLedgerTests` 会把机器可读报告写入
`battle-engine/build/reports/battle-rule-coverage.json`。该生成文件供 CI 与本地诊断使用，代码中的规则账本仍是事实来源。

顶层契约包含：

- `totalRuleCount`：`312`。
- `coverageGroupCount`：`12`。
- `sandboxRuleHitField`：`ruleHits`。
- `sandboxRuleHitFamilyField`：`familyCode`。
- `sandboxRuleHitItemField`：`itemCode`。
- `sandboxRuleHitFamilyCodes`：沙盒规则命中公开的全部覆盖规则族 code。
- `groups[].code`：稳定的覆盖规则族 code。
- `groups[].ruleNumberRange`：包含首尾的账本编号区间。
- `groups[].testClassNames`：负责该规则族可执行覆盖的测试类。

该报告由测试套件重新生成，不应手工编辑。
