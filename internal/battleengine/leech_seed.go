package battleengine

import "fmt"

// LeechSeedApplication 描述技能命中后尝试在已选目标身上种下寄生种子的独立规则。
//
// 寄生种子不是普通易变状态：它不按固定回合数过期，目标离场时清除，并且回合末回复的是来源当时所在场上槽位
// 的当前成员，而不是种下它的稳定 MemberPosition。因此它使用专属模型，不能塞入 VolatileStatusApplication。
type LeechSeedApplication struct {
	// ChancePercent 是种子写入目标的独立触发概率；100 表示必定且不消费概率随机数。
	ChancePercent uint8 `json:"chancePercent"`
}

// validateLeechSeedApplication 校验冻结到技能快照的寄生种子规则。
func validateLeechSeedApplication(application LeechSeedApplication) error {
	if application.ChancePercent == 0 || application.ChancePercent > 100 {
		return fmt.Errorf("%w: 寄生种子触发概率无效", ErrInvalidInitialState)
	}
	return nil
}
