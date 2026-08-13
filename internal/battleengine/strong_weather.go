package battleengine

import (
	"fmt"
	"sort"
)

// StrongWeatherKind 是只能由同级效果覆盖、并由场上来源持续维持的封闭强天气种类。
//
// 强天气不是普通 WeatherEffect 的无限时长变体：它没有回合计时器、会清除普通天气，且必须在最后一个
// 同类来源离场或倒下后才结束。因此它独立建模，不能写入普通天气字段或泛型环境效果列表。
type StrongWeatherKind string

const (
	// StrongWeatherKindHarshSunlight 表示终结之地式强日照；它让水属性伤害技能直接失败，并沿用日照的普通天气修正。
	StrongWeatherKindHarshSunlight StrongWeatherKind = "harshSunlight"
	// StrongWeatherKindHeavyRain 表示始源之海式强降雨；它让火属性伤害技能直接失败，并沿用降雨的普通天气修正。
	StrongWeatherKindHeavyRain StrongWeatherKind = "heavyRain"
	// StrongWeatherKindStrongWinds 表示德尔塔气流式强风；它只移除飞行属性造成的弱点贡献，不建立普通天气修正。
	StrongWeatherKindStrongWinds StrongWeatherKind = "strongWinds"
)

// valid 报告强天气种类是否属于纯战斗引擎能够解释的封闭集合。
func (kind StrongWeatherKind) valid() bool {
	return kind == StrongWeatherKindHarshSunlight || kind == StrongWeatherKindHeavyRain || kind == StrongWeatherKindStrongWinds
}

// effectiveWeatherKind 返回强天气为普通天气规则提供的等效天气；强风没有等效普通天气。
func (kind StrongWeatherKind) effectiveWeatherKind() (WeatherKind, bool) {
	switch kind {
	case StrongWeatherKindHarshSunlight:
		return WeatherKindSun, true
	case StrongWeatherKindHeavyRain:
		return WeatherKindRain, true
	default:
		return "", false
	}
}

// StrongWeatherState 记录当前强天气及实际维持它的场上成员。
//
// Source 使用稳定 MemberRef 而不是槽位：同一成员换入其它槽位仍能作为来源，而原来源离场后会由
// synchronizeStrongWeather 在其它仍在场的持有者之间接管或结束天气。
type StrongWeatherState struct {
	// Kind 是当前生效的封闭强天气种类。
	Kind StrongWeatherKind `json:"kind"`
	// Source 是当前实际维持强天气的存活场上成员。
	Source MemberRef `json:"source"`
}

// validateStrongWeatherState 校验已进入环境快照的强天气状态。
func validateStrongWeatherState(value StrongWeatherState) error {
	if !value.Kind.valid() || !value.Source.Side.Valid() || !value.Source.Position.Valid() {
		return fmt.Errorf("强天气状态无效: %+v", value)
	}
	return nil
}

// strongWeatherSkillBlocked 报告当前强天气是否会使伤害技能在命中判定前直接失败。
//
// 变化技能、无属性伤害以及被场上天气封锁特性暂停的强天气都不会被阻止；属性必须使用已经应用普通天气
// 属性覆盖后的结果，保证强日照与强降雨同样遵守资料化的 WeatherElementOverrides。
func strongWeatherSkillBlocked(state State, actor MemberSnapshot, skill SkillSnapshot) bool {
	strongWeather := effectiveStrongWeather(state)
	if strongWeather == nil || skill.DamageClass == DamageClassStatus {
		return false
	}
	elementID := effectiveSkillElementForMember(actor, skill, effectiveSkillWeather(state, actor))
	switch strongWeather.Kind {
	case StrongWeatherKindHarshSunlight:
		return elementID != 0 && elementID == state.rules.ElementIDs["water"]
	case StrongWeatherKindHeavyRain:
		return elementID != 0 && elementID == state.rules.ElementIDs["fire"]
	default:
		return false
	}
}

// strongWindsNeutralizeFlyingWeakness 报告强风是否应移除防守方飞行属性贡献的弱点倍率。
//
// 只有最终乘积中飞行属性单项是弱点时才改为中性；抗性、免疫及目标其它属性的倍率保持不变。
func strongWindsNeutralizeFlyingWeakness(state StrongWeatherState, rules RuleSnapshot, attackElementID, defenseElementID Identifier) bool {
	if state.Kind != StrongWeatherKindStrongWinds || defenseElementID != rules.ElementIDs["flying"] {
		return false
	}
	numerator, denominator := rules.effectiveness(attackElementID, defenseElementID)
	return numerator > denominator
}

// startStrongWeather 将指定来源的强天气写入环境，并清除任何普通天气。
//
// 同一种强天气的其它来源换入时也会更新 Source，确保随后只有实际来源离场才触发接管检查；不同强天气
// 可以直接覆盖现有强天气，而普通天气不能覆盖任何强天气。
func startStrongWeather(state State, source MemberRef, kind StrongWeatherKind) (State, []Event) {
	current := state.environment.StrongWeather
	if current != nil && current.Kind == kind && current.Source == source {
		return state, nil
	}
	state.environment.Weather = nil
	state.environment.StrongWeather = &StrongWeatherState{Kind: kind, Source: source}
	return state, []Event{StrongWeatherStartedEvent{
		Type: EventKindStrongWeatherStarted, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Source: source, StrongWeather: kind,
	}}
}

// activeStrongWeatherHolders 按冻结阵营和槽位顺序列出当前仍可维持强天气的成员。
func activeStrongWeatherHolders(state State) []StrongWeatherState {
	holders := make([]StrongWeatherState, 0, 4)
	for _, side := range state.sides {
		for _, position := range side.ActiveMembers {
			member, found := state.member(side.Side, position)
			if !found || member.CurrentHP == 0 || member.SwitchInStrongWeather == "" {
				continue
			}
			holders = append(holders, StrongWeatherState{
				Kind:   member.SwitchInStrongWeather,
				Source: MemberRef{Side: side.Side, Position: member.Position},
			})
		}
	}
	return holders
}

// synchronizeStrongWeather 在来源离场、倒下或换入新的强天气来源后修复强天气生命周期。
//
// 当前来源仍在场且仍声明同种天气时保持原状态；否则按照稳定阵营、槽位顺序由第一个持有者接管。没有
// 持有者时才结束强天气。该规则避免将“天气来源”错误绑定到已离场成员或过期槽位。
func synchronizeStrongWeather(state State) (State, []Event) {
	current := state.environment.StrongWeather
	if current == nil {
		return state, nil
	}
	for _, holder := range activeStrongWeatherHolders(state) {
		if holder == *current {
			return state, nil
		}
	}
	holders := activeStrongWeatherHolders(state)
	if len(holders) != 0 {
		return startStrongWeather(state, holders[0].Source, holders[0].Kind)
	}
	state.environment.StrongWeather = nil
	return state, []Event{StrongWeatherEndedEvent{
		Type: EventKindStrongWeatherEnded, SchemaVersion: 1, TurnNumber: state.turnNumber, StrongWeather: current.Kind,
	}}
}

// resolveStrongWeatherOnSwitchIn 结算成员实际换入且入场危害结束后的强天气特性。
//
// 先读取换入成员的独立强天气资料，再同步旧来源，使新入场来源可以直接覆盖旧天气；若换入成员因危害
// 倒下或没有强天气资料，则同步会让失去来源的原天气由其它持有者接管或结束。
func resolveStrongWeatherOnSwitchIn(state State, slot SlotRef) (State, []Event) {
	member, found := state.ActiveMember(slot)
	if found && member.CurrentHP > 0 && member.SwitchInStrongWeather != "" {
		state, events := startStrongWeather(state, MemberRef{Side: slot.Side, Position: member.Position}, member.SwitchInStrongWeather)
		return state, events
	}
	return synchronizeStrongWeather(state)
}

// initializeStrongWeather 从双方初始上场成员的冻结特性建立最终强天气。
//
// 初始入场特性按照构造时一次性冻结的有效速度顺序触发：通常环境从快到慢，速度顺序反转时从慢到快，
// 同速成员保持阵营与席位顺序。后触发来源覆盖先触发来源。排序键必须在写入任何强天气前全部算出，避免先触发
// 的天气反过来改变后续成员的天气速度倍率并重排同一次初始触发队列。
//
// State 构造没有事件流输出，因此第 0 回合只写入权威环境快照；后续换人会通过 StrongWeatherStartedEvent 或
// StrongWeatherEndedEvent 留下完整可重放生命周期事件。
func initializeStrongWeather(state State) State {
	if state.environment.StrongWeather != nil {
		return state
	}
	// initialHolder 保存排序前一次性计算的速度，确保排序比较函数保持纯粹且不读取正在变化的环境。
	type initialHolder struct {
		strongWeather StrongWeatherState
		speed         uint32
	}
	holders := activeStrongWeatherHolders(state)
	ordered := make([]initialHolder, 0, len(holders))
	for _, holder := range holders {
		member, found := state.member(holder.Source.Side, holder.Source.Position)
		if !found {
			continue
		}
		ordered = append(ordered, initialHolder{
			strongWeather: holder,
			speed:         state.effectiveActionSpeed(holder.Source.Side, member),
		})
	}
	reversed := state.environment.FieldSpeedOrder != nil && state.environment.FieldSpeedOrder.Kind.reversesSpeedOrder()
	sort.SliceStable(ordered, func(left, right int) bool {
		if reversed {
			return ordered[left].speed < ordered[right].speed
		}
		return ordered[left].speed > ordered[right].speed
	})
	for _, holder := range ordered {
		state, _ = startStrongWeather(state, holder.strongWeather.Source, holder.strongWeather.Kind)
	}
	return state
}
