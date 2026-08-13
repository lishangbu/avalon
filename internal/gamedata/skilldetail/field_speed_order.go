package skilldetail

// FieldSpeedOrderKind 是技能详情可以建立的全场速度顺序效果的封闭资料代码。
//
// 它只描述同一行动优先度内的速度比较方向，不会被成员速度、状态、道具或未来天气倍率复用。资料层与纯战斗
// 引擎都只接受本枚举的稳定值，禁止从技能名称或说明文本推断效果。
type FieldSpeedOrderKind string

const (
	// FieldSpeedOrderKindTrickRoom 表示戏法空间，使同优先度内有效速度较低的成员先行动。
	FieldSpeedOrderKindTrickRoom FieldSpeedOrderKind = "trick-room"
)

// Valid 报告全场速度顺序效果种类是否能由当前资料服务和纯战斗引擎共同解释。
func (kind FieldSpeedOrderKind) Valid() bool {
	return kind == FieldSpeedOrderKindTrickRoom
}

// FieldSpeedOrder 是技能成功后尝试建立的完整全场速度顺序资料。
//
// 它不是易变状态、天气或侧状态的泛化字段。再次成功使用同一个 Kind 时，引擎会解除而非刷新已存在的效果；
// 因此 Kind、持续回合与触发概率都必须作为独立业务事实持久化。
type FieldSpeedOrder struct {
	// Kind 是本技能尝试建立的封闭全场速度顺序效果。
	Kind FieldSpeedOrderKind `json:"kind"`
	// TurnsRemaining 是效果建立时声明的正持续回合数，取值为 1 至 100。
	TurnsRemaining int32 `json:"turnsRemaining"`
	// ChancePercent 是本项效果独立触发的概率，取值为 1 至 100。
	ChancePercent int32 `json:"chancePercent"`
}

// validFieldSpeedOrder 校验可选全场速度顺序资料的封闭种类和数值边界。
func validFieldSpeedOrder(value *FieldSpeedOrder) bool {
	return value == nil || value.Kind.Valid() && value.TurnsRemaining >= 1 && value.TurnsRemaining <= 100 &&
		value.ChancePercent >= 1 && value.ChancePercent <= 100
}

// cloneFieldSpeedOrder 复制可选资料，避免应用命令、审计快照和存储参数共享调用方可变内存。
func cloneFieldSpeedOrder(value *FieldSpeedOrder) *FieldSpeedOrder {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}
