package battleengine

// OpponentSwitchRestriction 是由当前特性冻结到战斗成员的敌方主动换人限制规则。
//
// nil 表示该成员不提供限制。规则只在目标主动换人时读取，绝不影响倒下补位、技能或道具造成的强制换人；
// 这三种生命周期必须保持独立，避免限制特性阻断战斗推进。
type OpponentSwitchRestriction struct {
	// RequiredTargetElementID 是目标必须拥有的可选当前属性稳定 Identifier。
	// 空字符串表示不按属性筛选；属性替换、太晶化和形态变化会自然影响当前 ElementIDs 的匹配结果。
	RequiredTargetElementID Identifier `json:"requiredTargetElementId,omitempty"`
	// RequiresGroundedTarget 表示只有接地目标会受到主动换人限制。
	// 接地计算集中复用 memberGrounded，后续浮游、气球或重力机制扩展时无需修改本规则的判定入口。
	RequiresGroundedTarget bool `json:"requiresGroundedTarget"`
	// SameEffectGrantsImmunity 表示目标当前具有与本规则完全相同的限制规则时，本规则不会限制它。
	// 它比较全部三个规则字段，不能把任意其它限制规则误作同一种效果；道具豁免由 SwitchRestrictionImmunity 独立表达。
	SameEffectGrantsImmunity bool `json:"sameEffectGrantsImmunity"`
}

func cloneOpponentSwitchRestriction(value *OpponentSwitchRestriction) *OpponentSwitchRestriction {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

func validOpponentSwitchRestriction(value *OpponentSwitchRestriction) bool {
	return value == nil || value.RequiredTargetElementID == 0 || value.RequiredTargetElementID.IsValid()
}

// opponentSwitchRestrictionPreventsSwitch 报告指定成员的主动换人是否被任一存活上场对手限制。
//
// 双打时按当前上场的所有对手求“任一阻止”而不是选定一个来源；这与限制的存在性语义一致，也保证任一
// 限制来源离场后会在下一次命令校验即时失效。函数只服务主动换人路径，调用者不得用于倒下补位或强制换人。
func opponentSwitchRestrictionPreventsSwitch(state State, targetSide Side, target MemberSnapshot) bool {
	if target.SwitchRestrictionImmunity {
		return false
	}
	for _, side := range state.sides {
		if side.Side == targetSide {
			continue
		}
		for _, position := range side.ActiveMembers {
			source, found := state.member(side.Side, position)
			if !found || source.CurrentHP == 0 || source.OpponentSwitchRestriction == nil {
				continue
			}
			if source.OpponentSwitchRestriction.SameEffectGrantsImmunity &&
				opponentSwitchRestrictionEqual(source.OpponentSwitchRestriction, target.OpponentSwitchRestriction) {
				continue
			}
			if source.OpponentSwitchRestriction.RequiresGroundedTarget && !memberGrounded(state.rules, target) {
				continue
			}
			if requiredElementID := source.OpponentSwitchRestriction.RequiredTargetElementID; requiredElementID != 0 &&
				!memberHasElement(target, requiredElementID) {
				continue
			}
			return true
		}
	}
	return false
}

// opponentSwitchRestrictionEqual 报告两条限制规则是否表达相同的完整效果。
//
// SameEffectGrantsImmunity 的语义来自规则本身而非“都属于主动换人限制”这一大类：属性、接地条件和免疫开关
// 任一不同都可能导致目标集合不同，因此不能只比较两条规则是否非空。
func opponentSwitchRestrictionEqual(left, right *OpponentSwitchRestriction) bool {
	if left == nil || right == nil {
		return false
	}
	return left.RequiredTargetElementID == right.RequiredTargetElementID &&
		left.RequiresGroundedTarget == right.RequiresGroundedTarget &&
		left.SameEffectGrantsImmunity == right.SameEffectGrantsImmunity
}

func memberHasElement(member MemberSnapshot, elementID Identifier) bool {
	for _, currentElementID := range member.ElementIDs {
		if currentElementID == elementID {
			return true
		}
	}
	return false
}
