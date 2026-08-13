package skilldetail

// RapidSpin 描述技能成功造成伤害后清除使用者一方全部入场危害的固定资料规则。
//
// 快速旋转不清除屏障、顺风或对方场地。Enabled 显式写入 JSONB，是为了区分“资料未配置”对应的空对象与
// “已启用这条无参数固定规则”，而不是把不同清场效果合并成通用删除列表。
type RapidSpin struct {
	// Enabled 必须为 true，表示该技能在成功造成伤害后执行快速旋转的固定清场规则。
	Enabled bool `json:"enabled"`
}

// validRapidSpin 校验可选快速旋转资料；存在时必须显式启用。
func validRapidSpin(value *RapidSpin) bool {
	return value == nil || value.Enabled
}

// cloneRapidSpin 复制可选快速旋转资料，隔离命令、审计快照和存储参数持有的可变地址。
func cloneRapidSpin(value *RapidSpin) *RapidSpin {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}
