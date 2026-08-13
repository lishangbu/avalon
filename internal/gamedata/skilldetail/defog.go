package skilldetail

// Defog 描述技能成功后清除目标一方屏障、入场危害和当前普通场地的固定资料规则。
//
// 清除浓雾不会移除顺风。Enabled 显式写入 JSONB，区分未配置规则的空对象与已启用的无参数清场规则，并保证
// 它不与快速旋转或任意通用效果删除载荷混淆。
type Defog struct {
	// Enabled 必须为 true，表示该技能在成功后执行清除浓雾的固定清场规则。
	Enabled bool `json:"enabled"`
}

// validDefog 校验可选清除浓雾资料；存在时必须显式启用。
func validDefog(value *Defog) bool {
	return value == nil || value.Enabled
}

// cloneDefog 复制可选清除浓雾资料，隔离命令、审计快照和存储参数持有的可变地址。
func cloneDefog(value *Defog) *Defog {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}
