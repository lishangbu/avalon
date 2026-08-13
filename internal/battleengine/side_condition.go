package battleengine

import "fmt"

// SideConditionSnapshot 是仅作用于一个阵营、会跨回合保存的权威侧状态。
//
// 它与全场的 EnvironmentSnapshot、成员易变状态以及未来特性和道具触发器保持独立。字段按具体规则命名，
// 不使用可任意装填的效果数组或 JSON，以确保重放、资料编译和事件都能准确区分顺风、屏障与入场危害。
type SideConditionSnapshot struct {
	// Reflect 是该方可选的反射壁持续效果；空值表示没有反射壁。它只减少本方成员承受的普通物理伤害，
	// 不影响特殊伤害、变化技能或对方阵营。
	Reflect *ReflectEffect `json:"reflect,omitempty"`
	// LightScreen 是该方可选的光墙持续效果；空值表示没有光墙。它只减少本方成员承受的普通特殊伤害。
	LightScreen *LightScreenEffect `json:"lightScreen,omitempty"`
	// AuroraVeil 是该方可选的极光幕持续效果；空值表示没有极光幕。它减少本方成员承受的普通物理和特殊伤害。
	AuroraVeil *AuroraVeilEffect `json:"auroraVeil,omitempty"`
	// SpikesLayers 是已布置在该方场地的撒菱层数；0 表示没有撒菱，最大为 3。
	// 它在成员换入时只伤害接地成员，不能表示为成员易变状态或全场环境。
	SpikesLayers uint8 `json:"spikesLayers"`
	// StealthRock 表示该方场地已布置隐形岩。它在每个成员换入时按冻结的岩石属性倍率造成伤害，
	// 与只影响接地成员、按层数结算的撒菱具有不同规则，必须使用独立布尔状态。
	StealthRock bool `json:"stealthRock"`
	// ToxicSpikesLayers 是该方场地已布置的毒菱层数；0 表示没有毒菱，最大为 2。
	// 接地毒属性成员会吸收全部层数，其它接地成员会获得普通中毒或剧毒，不能与撒菱层数复用。
	ToxicSpikesLayers uint8 `json:"toxicSpikesLayers"`
	// StickyWeb 表示该方场地已布置黏黏网。它在接地成员换入时降低速度能力阶级，
	// 不造成伤害也不施加主要异常，因此独立于三种入场危害状态保存。
	StickyWeb bool `json:"stickyWeb"`
	// Tailwind 是该方可选的顺风持续效果；空值表示没有顺风。它只影响本方后续回合的行动速度排序，
	// 不会回溯改变已经生成的当前回合行动计划。
	Tailwind *TailwindEffect `json:"tailwind,omitempty"`
}

// ReflectEffect 是已经写入一方阵营、持续减少该方普通物理伤害的反射壁状态。
type ReflectEffect struct {
	// TurnsRemaining 是包含当前结算回合在内的剩余完整回合数，必须为正数。
	TurnsRemaining uint8 `json:"turnsRemaining"`
}

// advanceTurn 推进一个完整回合后的反射壁持续状态；nil 表示持续回合已经耗尽。
func (effect ReflectEffect) advanceTurn() *ReflectEffect {
	if effect.TurnsRemaining <= 1 {
		return nil
	}
	effect.TurnsRemaining--
	return &effect
}

// ReflectApplication 描述一项技能成功后尝试在使用者一方建立反射壁的规则。
//
// 反射壁固定作用于使用者一方，不允许资料指定对方或任意一侧；这能防止本应保护己方物理防守的技能被错误编译
// 为对手状态。持续效果与触发概率分离后，已生效快照只保存当前权威回合数。
type ReflectApplication struct {
	// Effect 是成功建立时写入使用者阵营的完整反射壁持续状态。
	Effect ReflectEffect `json:"effect"`
	// ChancePercent 是反射壁建立的独立触发概率；100 表示必定且不消费概率随机数。
	ChancePercent uint8 `json:"chancePercent"`
}

// LightScreenEffect 是已经写入一方阵营、持续减少该方普通特殊伤害的光墙状态。
type LightScreenEffect struct {
	// TurnsRemaining 是包含当前结算回合在内的剩余完整回合数，必须为正数。
	TurnsRemaining uint8 `json:"turnsRemaining"`
}

// advanceTurn 推进一个完整回合后的光墙持续状态；nil 表示持续回合已经耗尽。
func (effect LightScreenEffect) advanceTurn() *LightScreenEffect {
	if effect.TurnsRemaining <= 1 {
		return nil
	}
	effect.TurnsRemaining--
	return &effect
}

// LightScreenApplication 描述一项技能成功后尝试在使用者一方建立光墙的规则。
type LightScreenApplication struct {
	// Effect 是成功建立时写入使用者阵营的完整光墙持续状态。
	Effect LightScreenEffect `json:"effect"`
	// ChancePercent 是光墙建立的独立触发概率；100 表示必定且不消费概率随机数。
	ChancePercent uint8 `json:"chancePercent"`
}

// AuroraVeilEffect 是已经写入一方阵营、持续减少该方普通物理和特殊伤害的极光幕状态。
type AuroraVeilEffect struct {
	// TurnsRemaining 是包含当前结算回合在内的剩余完整回合数，必须为正数。
	TurnsRemaining uint8 `json:"turnsRemaining"`
}

// advanceTurn 推进一个完整回合后的极光幕持续状态；nil 表示持续回合已经耗尽。
func (effect AuroraVeilEffect) advanceTurn() *AuroraVeilEffect {
	if effect.TurnsRemaining <= 1 {
		return nil
	}
	effect.TurnsRemaining--
	return &effect
}

// AuroraVeilApplication 描述一项技能成功后尝试在使用者一方建立极光幕的规则。
type AuroraVeilApplication struct {
	// Effect 是成功建立时写入使用者阵营的完整极光幕持续状态。
	Effect AuroraVeilEffect `json:"effect"`
	// ChancePercent 是极光幕建立的独立触发概率；100 表示必定且不消费概率随机数。
	ChancePercent uint8 `json:"chancePercent"`
}

// SpikesApplication 描述一项技能成功后尝试在目标一方场地增加一层撒菱的规则。
//
// 撒菱固定写入被选中对手所在阵营，而不是某名成员；层数上限、入场伤害比例和接地判定属于引擎固定语义，资料只
// 声明本次尝试的概率。它不能与毒菱共用层数应用，因为二者的最大层数与换入效果不同。
type SpikesApplication struct {
	// ChancePercent 是增加一层撒菱的独立触发概率；100 表示必定且不消费概率随机数。
	ChancePercent uint8 `json:"chancePercent"`
}

// StealthRockApplication 描述一项技能成功后尝试在目标一方场地布置隐形岩的规则。
//
// 隐形岩没有层数，并按岩石属性倍率影响所有换入成员，包括非接地成员；它不能复用撒菱或毒菱的层数应用。
type StealthRockApplication struct {
	// ChancePercent 是布置隐形岩的独立触发概率；100 表示必定且不消费概率随机数。
	ChancePercent uint8 `json:"chancePercent"`
}

// ToxicSpikesApplication 描述一项技能成功后尝试在目标一方场地增加一层毒菱的规则。
//
// 毒菱最多两层，接地毒属性成员会吸收全部层数，其他接地成员则获得普通中毒或剧毒；这些语义区别要求它独立于
// 撒菱和隐形岩保存。
type ToxicSpikesApplication struct {
	// ChancePercent 是增加一层毒菱的独立触发概率；100 表示必定且不消费概率随机数。
	ChancePercent uint8 `json:"chancePercent"`
}

// StickyWebApplication 描述一项技能成功后尝试在目标一方场地布置黏黏网的规则。
//
// 黏黏网没有层数，也不造成伤害或主要异常；它只会在接地成员换入时降低速度能力阶级，因此使用自己的强类型
// 应用规则和侧状态字段。
type StickyWebApplication struct {
	// ChancePercent 是布置黏黏网的独立触发概率；100 表示必定且不消费概率随机数。
	ChancePercent uint8 `json:"chancePercent"`
}

// RapidSpinApplication 描述一项成功造成伤害的快速旋转技能清除使用者一方全部入场危害的固定规则。
//
// 它只清除撒菱、隐形岩、毒菱和黏黏网，不清除任何屏障、顺风或全场场地；这些范围不能用通用删除指令代替。
type RapidSpinApplication struct{}

// DefogApplication 描述一项单体目标变化技能清除目标一方屏障与入场危害、并清除当前普通场地的固定规则。
//
// 清除浓雾不移除顺风：顺风是行动顺序侧状态而非屏障或危害。当前版本把普通场地作为清除浓雾的独立固定
// 结果，不允许资料用任意“删除效果列表”扩展语义。
type DefogApplication struct{}

// TailwindEffect 是已经写入一方阵营并持续影响行动排序的顺风状态。
type TailwindEffect struct {
	// TurnsRemaining 是包含当前结算回合在内的剩余完整回合数，必须为正数。
	//
	// 顺风在技能建立的回合末立即递减，因此标准四回合顺风建立事件记录 4，而该回合完成后的状态快照记录 3。
	TurnsRemaining uint8 `json:"turnsRemaining"`
}

// TailwindApplication 描述一项技能成功后尝试在使用者一方建立顺风的规则。
//
// 顺风固定作用于使用者一方，不允许资料指定对方或任意一侧；这能防止把本应是己方速度支援的技能误编译为
// 敌方减益。与持续效果分离后，资料可独立声明本次触发概率，而状态快照只保存已经生效的回合数。
type TailwindApplication struct {
	// Effect 是成功建立时写入使用者阵营的完整顺风持续状态。
	Effect TailwindEffect `json:"effect"`
	// ChancePercent 是顺风建立的独立触发概率；100 表示必定且不消费概率随机数。
	ChancePercent uint8 `json:"chancePercent"`
}

// advanceTurn 推进一个完整回合后的顺风持续状态；nil 表示持续回合已经耗尽。
func (effect TailwindEffect) advanceTurn() *TailwindEffect {
	if effect.TurnsRemaining <= 1 {
		return nil
	}
	effect.TurnsRemaining--
	return &effect
}

// validateSideConditions 校验初始状态或离线重放快照中的已知侧状态。
func validateSideConditions(conditions SideConditionSnapshot) error {
	if conditions.Reflect != nil && conditions.Reflect.TurnsRemaining == 0 {
		return fmt.Errorf("%w: 反射壁持续回合无效", ErrInvalidInitialState)
	}
	if conditions.LightScreen != nil && conditions.LightScreen.TurnsRemaining == 0 {
		return fmt.Errorf("%w: 光墙持续回合无效", ErrInvalidInitialState)
	}
	if conditions.AuroraVeil != nil && conditions.AuroraVeil.TurnsRemaining == 0 {
		return fmt.Errorf("%w: 极光幕持续回合无效", ErrInvalidInitialState)
	}
	if conditions.SpikesLayers > 3 {
		return fmt.Errorf("%w: 撒菱层数无效", ErrInvalidInitialState)
	}
	if conditions.ToxicSpikesLayers > 2 {
		return fmt.Errorf("%w: 毒菱层数无效", ErrInvalidInitialState)
	}
	if conditions.Tailwind != nil && conditions.Tailwind.TurnsRemaining == 0 {
		return fmt.Errorf("%w: 顺风持续回合无效", ErrInvalidInitialState)
	}
	return nil
}

// validateTailwindApplication 校验冻结到技能快照的顺风建立规则。
func validateTailwindApplication(application TailwindApplication) error {
	if application.Effect.TurnsRemaining == 0 || application.ChancePercent == 0 || application.ChancePercent > 100 {
		return fmt.Errorf("%w: 顺风建立规则无效", ErrInvalidInitialState)
	}
	return nil
}

// validateReflectApplication 校验冻结到技能快照的反射壁建立规则。
func validateReflectApplication(application ReflectApplication) error {
	if application.Effect.TurnsRemaining == 0 || application.ChancePercent == 0 || application.ChancePercent > 100 {
		return fmt.Errorf("%w: 反射壁建立规则无效", ErrInvalidInitialState)
	}
	return nil
}

// validateLightScreenApplication 校验冻结到技能快照的光墙建立规则。
func validateLightScreenApplication(application LightScreenApplication) error {
	if application.Effect.TurnsRemaining == 0 || application.ChancePercent == 0 || application.ChancePercent > 100 {
		return fmt.Errorf("%w: 光墙建立规则无效", ErrInvalidInitialState)
	}
	return nil
}

// validateAuroraVeilApplication 校验冻结到技能快照的极光幕建立规则。
func validateAuroraVeilApplication(application AuroraVeilApplication) error {
	if application.Effect.TurnsRemaining == 0 || application.ChancePercent == 0 || application.ChancePercent > 100 {
		return fmt.Errorf("%w: 极光幕建立规则无效", ErrInvalidInitialState)
	}
	return nil
}

// validateSpikesApplication 校验冻结到技能快照的撒菱建立规则。
func validateSpikesApplication(application SpikesApplication) error {
	if application.ChancePercent == 0 || application.ChancePercent > 100 {
		return fmt.Errorf("%w: 撒菱建立规则无效", ErrInvalidInitialState)
	}
	return nil
}

// validateStealthRockApplication 校验冻结到技能快照的隐形岩建立规则。
func validateStealthRockApplication(application StealthRockApplication) error {
	if application.ChancePercent == 0 || application.ChancePercent > 100 {
		return fmt.Errorf("%w: 隐形岩建立规则无效", ErrInvalidInitialState)
	}
	return nil
}

// validateToxicSpikesApplication 校验冻结到技能快照的毒菱建立规则。
func validateToxicSpikesApplication(application ToxicSpikesApplication) error {
	if application.ChancePercent == 0 || application.ChancePercent > 100 {
		return fmt.Errorf("%w: 毒菱建立规则无效", ErrInvalidInitialState)
	}
	return nil
}

// validateStickyWebApplication 校验冻结到技能快照的黏黏网建立规则。
func validateStickyWebApplication(application StickyWebApplication) error {
	if application.ChancePercent == 0 || application.ChancePercent > 100 {
		return fmt.Errorf("%w: 黏黏网建立规则无效", ErrInvalidInitialState)
	}
	return nil
}

// cloneSideConditions 深复制侧状态中所有指针字段，避免调用方篡改权威 State 或历史快照。
func cloneSideConditions(conditions SideConditionSnapshot) SideConditionSnapshot {
	if conditions.Reflect != nil {
		effect := *conditions.Reflect
		conditions.Reflect = &effect
	}
	if conditions.LightScreen != nil {
		effect := *conditions.LightScreen
		conditions.LightScreen = &effect
	}
	if conditions.AuroraVeil != nil {
		effect := *conditions.AuroraVeil
		conditions.AuroraVeil = &effect
	}
	if conditions.Tailwind != nil {
		effect := *conditions.Tailwind
		conditions.Tailwind = &effect
	}
	return conditions
}
