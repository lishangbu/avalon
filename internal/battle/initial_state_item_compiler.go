package battle

import (
	"github.com/lishangbu/avalon/internal/battleengine"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// itemForcedSwitchRules 编译一件持有道具的三种独立强制换人规则。
//
// 缺少道具或详情合法地表示三种规则均关闭。一个道具只能有一条详情；重复详情会让受伤自换、受伤换攻击者和
// 降能力自换的来源不确定，因而必须阻止新 Battle，而不是按数据库返回顺序任选一条。
func (compiler *initialMemberCompiler) itemForcedSwitchRules(itemID *snowflake.ID) (bool, bool, bool, error) {
	if itemID == nil {
		return false, false, false, nil
	}
	var damagedForceSelfSwitch bool
	var damagedForceAttackerSwitch bool
	var negativeStatStageForceSelfSwitch bool
	foundDetail := false
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail {
			return false, false, false, ErrInitialStateCompilation
		}
		foundDetail = true
		damagedForceSelfSwitch = detail.DamagedForceSelfSwitch
		damagedForceAttackerSwitch = detail.DamagedForceAttackerSwitch
		negativeStatStageForceSelfSwitch = detail.NegativeStatStageForceSelfSwitch
	}
	return damagedForceSelfSwitch, damagedForceAttackerSwitch, negativeStatStageForceSelfSwitch, nil
}

// itemSwitchRestrictionImmunity 编译一件持有道具提供的敌方主动换人限制豁免开关。
//
// 没有道具或没有详情时返回 false。一个道具有多条详情会让该豁免的资料来源不确定，因此和其它道具战斗规则
// 一样拒绝启动新对局，而不是按数据库返回顺序任选一条。
func (compiler *initialMemberCompiler) itemSwitchRestrictionImmunity(itemID *snowflake.ID) (bool, error) {
	if itemID == nil {
		return false, nil
	}
	foundDetail := false
	var result bool
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail {
			return false, ErrInitialStateCompilation
		}
		foundDetail = true
		result = detail.SwitchRestrictionImmunity
	}
	return result, nil
}

// itemContactSideEffectImmunity 编译一件持有道具提供的接触反制副作用免疫开关。
//
// 道具只保护攻击者不受目标因有效接触触发的反制效果，绝不改写技能自身的接触标签。一个道具有多条详情
// 会使冻结来源不确定，必须拒绝启动对局而不是依赖资料返回顺序。
func (compiler *initialMemberCompiler) itemContactSideEffectImmunity(itemID *snowflake.ID) (bool, error) {
	if itemID == nil {
		return false, nil
	}
	foundDetail := false
	var result bool
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail {
			return false, ErrInitialStateCompilation
		}
		foundDetail = true
		result = detail.ContactSideEffectImmunity
	}
	return result, nil
}

// itemContactDamageToAttackerDenominator 编译持有道具在成员受到有效接触本体伤害后反伤攻击者的固定比例分母。
//
// 0 表示未声明该道具规则；正值必须适配 Battle Engine 的 uint16 冻结表示。资料中出现重复详情或超出范围的
// 数值都会使来源不确定或发生截断，因此必须在 Battle 启动前拒绝。
func (compiler *initialMemberCompiler) itemContactDamageToAttackerDenominator(itemID *snowflake.ID) (uint16, error) {
	if itemID == nil {
		return 0, nil
	}
	foundDetail := false
	var result uint16
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail || detail.ContactDamageToAttackerDenominator < 0 || detail.ContactDamageToAttackerDenominator > 65535 {
			return 0, ErrInitialStateCompilation
		}
		foundDetail = true
		result = uint16(detail.ContactDamageToAttackerDenominator)
	}
	return result, nil
}

// itemEndTurnHealDenominator 编译持有道具在回合末按最大生命固定比例回复的分母。
//
// 零表示未声明回复规则；正值必须能无损转换为 Battle Engine 的 uint16。重复详情会使同一道具的回复比例不确定，
// 因此 Battle 必须在启动前拒绝，而不能在运行时从资料名称或效果文本推断。
func (compiler *initialMemberCompiler) itemEndTurnHealDenominator(itemID *snowflake.ID) (uint16, error) {
	if itemID == nil {
		return 0, nil
	}
	foundDetail := false
	var result uint16
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail || detail.EndTurnHealDenominator < 0 || detail.EndTurnHealDenominator > 65535 {
			return 0, ErrInitialStateCompilation
		}
		foundDetail = true
		result = uint16(detail.EndTurnHealDenominator)
	}
	return result, nil
}

// itemEndTurnHealForElement 编译持有道具仅在成员拥有指定当前有效属性时生效的回合末回复规则。
//
// 属性 Identifier 和分母必须同时存在；属性还必须属于本场已冻结并启用的属性字典。这样引擎只需读取 MemberSnapshot
// 的当前 ElementIDs，无需在回合末访问实时资料，也不会把太晶化、形态变化或道具属性身份错误当作静态资料。
func (compiler *initialMemberCompiler) itemEndTurnHealForElement(itemID *snowflake.ID) (snowflake.ID, uint16, error) {
	if itemID == nil {
		return 0, 0, nil
	}
	foundDetail := false
	var elementID *snowflake.ID
	var denominator int32
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail {
			return 0, 0, ErrInitialStateCompilation
		}
		foundDetail = true
		elementID = detail.EndTurnHealForElementID
		denominator = detail.EndTurnHealForElementDenominator
	}
	if elementID == nil && denominator == 0 {
		return 0, 0, nil
	}
	if elementID == nil || *elementID == snowflake.ID(0) || denominator <= 0 || denominator > 65535 {
		return 0, 0, ErrInitialStateCompilation
	}
	if _, enabled := compiler.elementIDEnabled(*elementID); !enabled {
		return 0, 0, ErrInitialStateCompilation
	}
	return *elementID, uint16(denominator), nil
}

// itemEndTurnDamageDenominator 编译持有道具在回合末按最大生命固定比例造成间接伤害的分母。
//
// 零表示未声明自伤规则；正值必须能无损转换为 Battle Engine 的 uint16。重复详情会使同一道具的伤害比例不确定，
// 因此 Battle 必须在启动前拒绝，而不能在运行期从资料名称或效果文本推断。
func (compiler *initialMemberCompiler) itemEndTurnDamageDenominator(itemID *snowflake.ID) (uint16, error) {
	if itemID == nil {
		return 0, nil
	}
	foundDetail := false
	var result uint16
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail || detail.EndTurnDamageDenominator < 0 || detail.EndTurnDamageDenominator > 65535 {
			return 0, ErrInitialStateCompilation
		}
		foundDetail = true
		result = uint16(detail.EndTurnDamageDenominator)
	}
	return result, nil
}

// itemEndTurnDamageWithoutElement 编译持有道具仅在成员不具备指定当前有效属性时生效的回合末自伤规则。
//
// 属性 Identifier 和分母必须同时存在；属性还必须属于本场已冻结并启用的属性字典。战斗引擎据此只读取成员当前
// ElementIDs，无需回查实时资料，太晶化、形态变化和道具属性身份也自然会影响条件判断。
func (compiler *initialMemberCompiler) itemEndTurnDamageWithoutElement(itemID *snowflake.ID) (snowflake.ID, uint16, error) {
	if itemID == nil {
		return 0, 0, nil
	}
	foundDetail := false
	var elementID *snowflake.ID
	var denominator int32
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail {
			return 0, 0, ErrInitialStateCompilation
		}
		foundDetail = true
		elementID = detail.EndTurnDamageWithoutElementID
		denominator = detail.EndTurnDamageWithoutElementDenominator
	}
	if elementID == nil && denominator == 0 {
		return 0, 0, nil
	}
	if elementID == nil || *elementID == snowflake.ID(0) || denominator <= 0 || denominator > 65535 {
		return 0, 0, ErrInitialStateCompilation
	}
	if _, enabled := compiler.elementIDEnabled(*elementID); !enabled {
		return 0, 0, ErrInitialStateCompilation
	}
	return *elementID, uint16(denominator), nil
}

// itemConsumableElementDamageBoost 编译一次性属性威力强化道具的技能属性与倍率。
//
// 三个字段必须同时缺失或同时有效：空配置表示普通道具；有效配置要求属性已在本场资料中启用、分子和分母均为
// 可无损冻结为 uint16 的正数。消费时机由 Battle Engine 在真实本体伤害写入后判断，因此 Battle 只冻结事实，
// 不在启动期推断该道具是否会被消耗。
func (compiler *initialMemberCompiler) itemConsumableElementDamageBoost(itemID *snowflake.ID) (snowflake.ID, uint16, uint16, error) {
	if itemID == nil {
		return 0, 0, 0, nil
	}
	foundDetail := false
	var elementID *snowflake.ID
	var numerator, denominator int32
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail {
			return 0, 0, 0, ErrInitialStateCompilation
		}
		foundDetail = true
		elementID = detail.ConsumableElementDamageBoostElementID
		numerator = detail.ConsumableElementDamageBoostNumerator
		denominator = detail.ConsumableElementDamageBoostDenominator
	}
	if elementID == nil && numerator == 0 && denominator == 0 {
		return 0, 0, 0, nil
	}
	if elementID == nil || *elementID == snowflake.ID(0) || numerator <= 0 || numerator > 65535 || denominator <= 0 || denominator > 65535 {
		return 0, 0, 0, ErrInitialStateCompilation
	}
	if _, enabled := compiler.elementIDEnabled(*elementID); !enabled {
		return 0, 0, 0, ErrInitialStateCompilation
	}
	return *elementID, uint16(numerator), uint16(denominator), nil
}

// itemContactTransferToAttacker 编译持有道具在有效接触本体伤害后转移给无道具攻击者的触发开关。
//
// 一个道具有多条详情会令转移资格不确定，必须拒绝；没有道具或未声明该规则则返回 false。引擎只读取该冻结
// 事实与当前 ItemID，不会在对局中查询或依赖 Item Metadata 的显示文本。
func (compiler *initialMemberCompiler) itemContactTransferToAttacker(itemID *snowflake.ID) (bool, error) {
	if itemID == nil {
		return false, nil
	}
	foundDetail := false
	var result bool
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail {
			return false, ErrInitialStateCompilation
		}
		foundDetail = true
		result = detail.ContactTransferToAttacker
	}
	return result, nil
}

// itemChargeSkipOnce 编译持有道具的一次性蓄力跳过资格。
//
// 道具详情重复会使一次性消费来源不确定，必须拒绝；没有道具或没有声明该规则时返回 false。
func (compiler *initialMemberCompiler) itemChargeSkipOnce(itemID *snowflake.ID) (bool, error) {
	if itemID == nil {
		return false, nil
	}
	foundDetail := false
	var result bool
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail {
			return false, ErrInitialStateCompilation
		}
		foundDetail = true
		result = detail.ChargeSkipOnce
	}
	return result, nil
}

// itemSurviveFatalDamageAtFullHP 编译持有道具的一次性满生命保命资格。
//
// 同一 ItemID 存在重复详情时无法确定消费来源，必须拒绝；没有道具或未声明该规则时返回 false。
func (compiler *initialMemberCompiler) itemSurviveFatalDamageAtFullHP(itemID *snowflake.ID) (bool, error) {
	if itemID == nil {
		return false, nil
	}
	foundDetail := false
	result := false
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail {
			return false, ErrInitialStateCompilation
		}
		foundDetail = true
		result = detail.SurviveFatalDamageAtFullHP
	}
	return result, nil
}

// itemReflectTurnsRemaining 编译持有道具建立反射壁时允许的最大初始持续回合。
//
// 道具详情重复会使屏障时长来源不确定，必须拒绝；0 表示没有反射壁延长规则，正值必须可安全冻结为 uint8。
func (compiler *initialMemberCompiler) itemReflectTurnsRemaining(itemID *snowflake.ID) (uint8, error) {
	if itemID == nil {
		return 0, nil
	}
	foundDetail := false
	result := int32(0)
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail || detail.ReflectTurnsRemaining < 0 || detail.ReflectTurnsRemaining > 255 {
			return 0, ErrInitialStateCompilation
		}
		foundDetail = true
		result = detail.ReflectTurnsRemaining
	}
	return uint8(result), nil
}

// itemLightScreenTurnsRemaining 编译持有道具建立光墙时允许的最大初始持续回合。
//
// 该字段独立于反射壁和极光幕，0 表示不延长；重复或超出冻结范围的资料会拒绝整个初始状态编译。
func (compiler *initialMemberCompiler) itemLightScreenTurnsRemaining(itemID *snowflake.ID) (uint8, error) {
	if itemID == nil {
		return 0, nil
	}
	foundDetail := false
	result := int32(0)
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail || detail.LightScreenTurnsRemaining < 0 || detail.LightScreenTurnsRemaining > 255 {
			return 0, ErrInitialStateCompilation
		}
		foundDetail = true
		result = detail.LightScreenTurnsRemaining
	}
	return uint8(result), nil
}

// itemAuroraVeilTurnsRemaining 编译持有道具建立极光幕时允许的最大初始持续回合。
//
// 0 表示不延长；详情重复或值超出引擎运行态的 uint8 范围时，不能以不确定资料启动对战。
func (compiler *initialMemberCompiler) itemAuroraVeilTurnsRemaining(itemID *snowflake.ID) (uint8, error) {
	if itemID == nil {
		return 0, nil
	}
	foundDetail := false
	result := int32(0)
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail || detail.AuroraVeilTurnsRemaining < 0 || detail.AuroraVeilTurnsRemaining > 255 {
			return 0, ErrInitialStateCompilation
		}
		foundDetail = true
		result = detail.AuroraVeilTurnsRemaining
	}
	return uint8(result), nil
}

// itemRainTurnsRemaining 编译持有道具建立普通降雨时允许的最大初始持续回合。
//
// 0 表示不延长；详情重复或值超出引擎运行态的 uint8 范围时，对战不能从不确定资料启动。
func (compiler *initialMemberCompiler) itemRainTurnsRemaining(itemID *snowflake.ID) (uint8, error) {
	if itemID == nil {
		return 0, nil
	}
	foundDetail := false
	result := int32(0)
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail || detail.RainTurnsRemaining < 0 || detail.RainTurnsRemaining > 255 {
			return 0, ErrInitialStateCompilation
		}
		foundDetail = true
		result = detail.RainTurnsRemaining
	}
	return uint8(result), nil
}

// itemSandstormTurnsRemaining 编译持有道具建立普通沙暴时允许的最大初始持续回合。
//
// 0 表示不延长；同一 Item 详情重复或值超出引擎运行态的 uint8 范围时，拒绝以存在歧义的资料创建对战。
func (compiler *initialMemberCompiler) itemSandstormTurnsRemaining(itemID *snowflake.ID) (uint8, error) {
	if itemID == nil {
		return 0, nil
	}
	foundDetail := false
	result := int32(0)
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail || detail.SandstormTurnsRemaining < 0 || detail.SandstormTurnsRemaining > 255 {
			return 0, ErrInitialStateCompilation
		}
		foundDetail = true
		result = detail.SandstormTurnsRemaining
	}
	return uint8(result), nil
}

// itemSnowTurnsRemaining 编译持有道具建立普通降雪时允许的最大初始持续回合。
//
// 0 表示不延长；发现重复详情或越界值时拒绝启动，防止同一对战冻结出不确定的降雪持续时间。
func (compiler *initialMemberCompiler) itemSnowTurnsRemaining(itemID *snowflake.ID) (uint8, error) {
	if itemID == nil {
		return 0, nil
	}
	foundDetail := false
	result := int32(0)
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail || detail.SnowTurnsRemaining < 0 || detail.SnowTurnsRemaining > 255 {
			return 0, ErrInitialStateCompilation
		}
		foundDetail = true
		result = detail.SnowTurnsRemaining
	}
	return uint8(result), nil
}

// itemSunTurnsRemaining 编译持有道具建立普通日照时允许的最大初始持续回合。
//
// 0 表示不延长；重复详情和不在 uint8 范围内的值都会使对战初始资料失效，而不会由运行时猜测取值。
func (compiler *initialMemberCompiler) itemSunTurnsRemaining(itemID *snowflake.ID) (uint8, error) {
	if itemID == nil {
		return 0, nil
	}
	foundDetail := false
	result := int32(0)
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail || detail.SunTurnsRemaining < 0 || detail.SunTurnsRemaining > 255 {
			return 0, ErrInitialStateCompilation
		}
		foundDetail = true
		result = detail.SunTurnsRemaining
	}
	return uint8(result), nil
}

// itemTerrainTurnsRemaining 编译持有道具建立任一普通场地时允许的最大初始持续回合。
//
// 它对应固定的全场地规则；0 表示不延长，重复详情或越界值会拒绝对战启动，避免运行态选择任意一项。
func (compiler *initialMemberCompiler) itemTerrainTurnsRemaining(itemID *snowflake.ID) (uint8, error) {
	if itemID == nil {
		return 0, nil
	}
	foundDetail := false
	result := int32(0)
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail || detail.TerrainTurnsRemaining < 0 || detail.TerrainTurnsRemaining > 255 {
			return 0, ErrInitialStateCompilation
		}
		foundDetail = true
		result = detail.TerrainTurnsRemaining
	}
	return uint8(result), nil
}

// itemSandstormDamageImmunity 编译道具提供的回合末普通沙暴伤害免疫事实。
//
// 无道具时返回 false；重复的同道具详情会使对战初始资料失效，避免运行时从多个矛盾值中任选其一。
func (compiler *initialMemberCompiler) itemSandstormDamageImmunity(itemID *snowflake.ID) (bool, error) {
	if itemID == nil {
		return false, nil
	}
	foundDetail := false
	result := false
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail {
			return false, ErrInitialStateCompilation
		}
		foundDetail = true
		result = detail.SandstormDamageImmunity
	}
	return result, nil
}

// itemEntryHazardImmunity 编译道具提供的自身换入危害免疫事实。
//
// 无道具时返回 false；发现同一 Item 有多个详情时拒绝对战启动，避免冻结不确定的道具效果。
func (compiler *initialMemberCompiler) itemEntryHazardImmunity(itemID *snowflake.ID) (bool, error) {
	if itemID == nil {
		return false, nil
	}
	foundDetail := false
	result := false
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail {
			return false, ErrInitialStateCompilation
		}
		foundDetail = true
		result = detail.EntryHazardImmunity
	}
	return result, nil
}

// itemWeightHalf 编译道具提供的有效体重减半事实。
//
// 无道具时返回 false；同一道具存在多个详情时拒绝对战启动，保证冻结的动态威力输入唯一且可审计。
func (compiler *initialMemberCompiler) itemWeightHalf(itemID *snowflake.ID) (bool, error) {
	if itemID == nil {
		return false, nil
	}
	foundDetail := false
	result := false
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail {
			return false, ErrInitialStateCompilation
		}
		foundDetail = true
		result = detail.WeightHalf
	}
	return result, nil
}

// itemCuresParalysis 编译道具在成员成功获得麻痹后立即消耗并净化的冻结事实。
//
// 无道具时返回 false；同一 Item 有多个详情时拒绝启动对战，避免道具是否消费由矛盾资料决定。
func (compiler *initialMemberCompiler) itemCuresParalysis(itemID *snowflake.ID) (bool, error) {
	if itemID == nil {
		return false, nil
	}
	foundDetail := false
	result := false
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail {
			return false, ErrInitialStateCompilation
		}
		foundDetail = true
		result = detail.CuresParalysis
	}
	return result, nil
}

// itemCuresSleep 编译道具在成员成功获得睡眠后立即消耗并净化的冻结事实。
//
// 无道具时返回 false；同一 Item 有多个详情时拒绝启动对战，保证睡眠道具的消费语义不存在歧义。
func (compiler *initialMemberCompiler) itemCuresSleep(itemID *snowflake.ID) (bool, error) {
	if itemID == nil {
		return false, nil
	}
	foundDetail := false
	result := false
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail {
			return false, ErrInitialStateCompilation
		}
		foundDetail = true
		result = detail.CuresSleep
	}
	return result, nil
}

// itemCuresPoison 编译道具在成员成功获得普通中毒或剧毒后立即消耗并净化的冻结事实。
//
// 无道具时返回 false；同一 Item 有多个详情时拒绝启动对战，避免两种中毒状态的消费语义不确定。
func (compiler *initialMemberCompiler) itemCuresPoison(itemID *snowflake.ID) (bool, error) {
	if itemID == nil {
		return false, nil
	}
	foundDetail := false
	result := false
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail {
			return false, ErrInitialStateCompilation
		}
		foundDetail = true
		result = detail.CuresPoison
	}
	return result, nil
}

// itemCuresBurn 编译道具在成员成功获得灼伤后立即消耗并净化的冻结事实。
//
// 无道具时返回 false；同一 Item 有多个详情时拒绝启动对战，避免灼伤消费语义不确定。
func (compiler *initialMemberCompiler) itemCuresBurn(itemID *snowflake.ID) (bool, error) {
	if itemID == nil {
		return false, nil
	}
	foundDetail := false
	result := false
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail {
			return false, ErrInitialStateCompilation
		}
		foundDetail = true
		result = detail.CuresBurn
	}
	return result, nil
}

// itemCuresFreeze 编译道具在成员成功获得冰冻后立即消耗并净化的冻结事实。
//
// 无道具时返回 false；同一 Item 有多个详情时拒绝启动对战，避免冰冻消费语义不确定。
func (compiler *initialMemberCompiler) itemCuresFreeze(itemID *snowflake.ID) (bool, error) {
	if itemID == nil {
		return false, nil
	}
	foundDetail := false
	result := false
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail {
			return false, ErrInitialStateCompilation
		}
		foundDetail = true
		result = detail.CuresFreeze
	}
	return result, nil
}

// itemCuresAllMajorStatuses 编译道具在成员成功获得任一种主要异常后立即消耗并净化的冻结事实。
//
// 无道具时返回 false；同一 Item 有多个详情时拒绝启动对战，避免全范围异常消费语义不确定。
func (compiler *initialMemberCompiler) itemCuresAllMajorStatuses(itemID *snowflake.ID) (bool, error) {
	if itemID == nil {
		return false, nil
	}
	foundDetail := false
	result := false
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail {
			return false, ErrInitialStateCompilation
		}
		foundDetail = true
		result = detail.CuresAllMajorStatuses
	}
	return result, nil
}

// itemCuresConfusion 编译道具在成员成功获得混乱后立即消耗并净化的冻结事实。
//
// 无道具时返回 false；同一 Item 有多个详情时拒绝启动对战，避免混乱消费语义不确定。
func (compiler *initialMemberCompiler) itemCuresConfusion(itemID *snowflake.ID) (bool, error) {
	if itemID == nil {
		return false, nil
	}
	foundDetail := false
	result := false
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail {
			return false, ErrInitialStateCompilation
		}
		foundDetail = true
		result = detail.CuresConfusion
	}
	return result, nil
}

// itemPunchBasedSkillPowerBoost 编译道具对拳击类技能提供固定威力强化的冻结事实。
//
// 无道具时返回 false；同一 Item 具有多个详情时拒绝启动对战，避免同一持有道具的伤害语义不确定。
func (compiler *initialMemberCompiler) itemPunchBasedSkillPowerBoost(itemID *snowflake.ID) (bool, error) {
	if itemID == nil {
		return false, nil
	}
	foundDetail := false
	result := false
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail {
			return false, ErrInitialStateCompilation
		}
		foundDetail = true
		result = detail.PunchBasedSkillPowerBoost
	}
	return result, nil
}

// itemDamageClassPowerBoosts 编译持有道具对普通物理和普通特殊直接伤害提供的固定威力强化开关。
//
// 两个效果分别冻结，避免引擎按道具名称推断力量头带或博识眼镜。缺少详情合法地表示均不强化；同一道具出现
// 多条详情会让规则来源不确定，因此必须在 Battle 启动前拒绝。
func (compiler *initialMemberCompiler) itemDamageClassPowerBoosts(itemID *snowflake.ID) (bool, bool, error) {
	if itemID == nil {
		return false, false, nil
	}
	foundDetail := false
	var physical, special bool
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail {
			return false, false, ErrInitialStateCompilation
		}
		foundDetail = true
		physical = detail.PhysicalDamagePowerBoost
		special = detail.SpecialDamagePowerBoost
	}
	return physical, special, nil
}

// itemElementDamageReduction 编译一次性抗性道具匹配的技能属性和严格克制条件。
//
// 属性必须在本场冻结资料中启用；空属性与 false 合法地表示没有规则，空属性却要求严格克制则属于损坏资料。
// 同一道具出现重复详情时拒绝 Battle，避免运行时按数组顺序挑选任意抗性来源。
func (compiler *initialMemberCompiler) itemElementDamageReduction(itemID *snowflake.ID) (snowflake.ID, bool, error) {
	if itemID == nil {
		return 0, false, nil
	}
	foundDetail := false
	var elementID *snowflake.ID
	var requiresSuperEffective bool
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail {
			return 0, false, ErrInitialStateCompilation
		}
		foundDetail = true
		elementID = detail.ElementDamageReductionElementID
		requiresSuperEffective = detail.ElementDamageReductionRequiresSuperEffective
	}
	if elementID == nil {
		if requiresSuperEffective {
			return 0, false, ErrInitialStateCompilation
		}
		return 0, false, nil
	}
	if *elementID == snowflake.ID(0) {
		return 0, false, ErrInitialStateCompilation
	}
	if _, enabled := compiler.elementIDEnabled(*elementID); !enabled {
		return 0, false, ErrInitialStateCompilation
	}
	return *elementID, requiresSuperEffective, nil
}

// itemConditionalDamageBoosts 编译达人带与生命宝珠的独立冻结开关。
//
// 两条规则共享伤害阶段但触发条件和后效不同，必须分别保存；重复详情仍视为资料损坏，不能按道具名称兜底。
func (compiler *initialMemberCompiler) itemConditionalDamageBoosts(itemID *snowflake.ID) (bool, bool, error) {
	if itemID == nil {
		return false, false, nil
	}
	foundDetail := false
	var superEffective, withRecoil bool
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail {
			return false, false, ErrInitialStateCompilation
		}
		foundDetail = true
		superEffective = detail.SuperEffectiveDamageBoost
		withRecoil = detail.DamageBoostWithRecoil
	}
	return superEffective, withRecoil, nil
}

// optionalIdentifier 把可选稳定 Identifier 转为 Battle Engine 使用的零值语义。
func optionalIdentifier(value *snowflake.ID) snowflake.ID {
	if value == nil {
		return 0
	}
	return *value
}

// validOptionalItemStatStageBoost 验证 Battle 编译入口看到的道具能力提升规则完整且可由引擎表达。
func validOptionalItemStatStageBoost(stat battleengine.Stat, delta int32) bool {
	if stat == "" || delta == 0 {
		return stat == "" && delta == 0
	}
	return stat.Valid() && delta > 0
}

// itemUtilityEffects 从唯一道具详情冻结伤害回复、命中、接地、速度和突击背心规则。
func (compiler *initialMemberCompiler) itemUtilityEffects(itemID *snowflake.ID) (itemUtilityEffectFacts, error) {
	if itemID == nil {
		return itemUtilityEffectFacts{}, nil
	}
	foundDetail := false
	var result itemUtilityEffectFacts
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail {
			return itemUtilityEffectFacts{}, ErrInitialStateCompilation
		}
		if detail.BindingTurns < 0 || detail.BindingTurns > 255 ||
			detail.BindingDamageDenominator < 0 || detail.BindingDamageDenominator > 65535 ||
			detail.AccuracyMissStatStageBoostDelta < 0 || detail.AccuracyMissStatStageBoostDelta > 6 ||
			!validOptionalItemStatStageBoost(detail.AccuracyMissStatStageBoostStat, detail.AccuracyMissStatStageBoostDelta) ||
			detail.AdditionalFlinchChancePercent < 0 || detail.AdditionalFlinchChancePercent > 100 ||
			detail.RandomActionOrderBoostChancePercent < 0 || detail.RandomActionOrderBoostChancePercent > 100 {
			return itemUtilityEffectFacts{}, ErrInitialStateCompilation
		}
		foundDetail = true
		result = itemUtilityEffectFacts{
			damageDealtHeal: detail.DamageDealtHeal, drainHealingBoost: detail.DrainHealingBoost,
			accuracyBoost: detail.AccuracyBoost, opponentAccuracyReduction: detail.OpponentAccuracyReduction,
			criticalHitStageBoost: detail.CriticalHitStageBoost, airborneUntilDamaged: detail.AirborneUntilDamaged,
			forceGrounded: detail.ForceGrounded, speedHalf: detail.SpeedHalf,
			specialDefenseBoost: detail.SpecialDefenseBoost, statusSkillRestriction: detail.StatusSkillRestriction,
			physicalDamagePowerBoost50: detail.PhysicalDamagePowerBoost50, specialDamagePowerBoost50: detail.SpecialDamagePowerBoost50,
			choiceSkillLock: detail.ChoiceSkillLock, speedBoost50: detail.SpeedBoost50,
			accuracyAfterTargetActedBoost: detail.AccuracyAfterTargetActedBoost, typeImmunitySuppression: detail.TypeImmunitySuppression,
			opponentStatStageReductionImmunity: detail.OpponentStatStageReductionImmunity, negativeStatStageReset: detail.NegativeStatStageReset,
			abilityStatReductionSpeedBoost: detail.AbilityStatReductionSpeedBoost, opponentPositiveStatStageCopy: detail.OpponentPositiveStatStageCopy,
			damagingSkillSecondaryEffectImmunity: detail.DamagingSkillSecondaryEffectImmunity,
			bindingTurns:                         uint8(detail.BindingTurns), bindingDamageDenominator: uint16(detail.BindingDamageDenominator),
			accuracyMissStatStageBoostStat: detail.AccuracyMissStatStageBoostStat, accuracyMissStatStageBoostDelta: int8(detail.AccuracyMissStatStageBoostDelta), weaknessPolicy: detail.WeaknessPolicy,
			waterDamageSpecialAttackBoostElementID:  optionalIdentifier(detail.WaterDamageSpecialAttackBoostElementID),
			electricDamageAttackBoostElementID:      optionalIdentifier(detail.ElectricDamageAttackBoostElementID),
			waterDamageSpecialDefenseBoostElementID: optionalIdentifier(detail.WaterDamageSpecialDefenseBoostElementID),
			iceDamageAttackBoostElementID:           optionalIdentifier(detail.IceDamageAttackBoostElementID),
			additionalFlinchChancePercent:           uint8(detail.AdditionalFlinchChancePercent), randomActionOrderBoostChancePercent: uint8(detail.RandomActionOrderBoostChancePercent),
			forcedLastActionOrder: detail.ForcedLastActionOrder, lowHPActionOrderBoost: detail.LowHPActionOrderBoost,
			fieldSpeedOrderSpeedStageDrop: detail.FieldSpeedOrderSpeedStageDrop, consecutiveSkillDamageBoost: detail.ConsecutiveSkillDamageBoost,
		}
	}
	return result, nil
}

// itemPunchBasedContactSuppression 编译道具对拳击类接触技能取消有效接触的冻结事实。
//
// 无道具时返回 false；同一 Item 有多个详情时拒绝启动对战，避免保护穿透、反制和接触伤害的语义不确定。
func (compiler *initialMemberCompiler) itemPunchBasedContactSuppression(itemID *snowflake.ID) (bool, error) {
	if itemID == nil {
		return false, nil
	}
	foundDetail := false
	result := false
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail {
			return false, ErrInitialStateCompilation
		}
		foundDetail = true
		result = detail.PunchBasedContactSuppression
	}
	return result, nil
}

// itemPowderSkillImmunity 编译道具对粉末或孢子类技能提供命中前免疫的冻结事实。
//
// 无道具时返回 false；同一 Item 有多个详情时拒绝启动对战，避免同一技能是否消耗命中随机数的语义不确定。
func (compiler *initialMemberCompiler) itemPowderSkillImmunity(itemID *snowflake.ID) (bool, error) {
	if itemID == nil {
		return false, nil
	}
	foundDetail := false
	result := false
	for _, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if foundDetail {
			return false, ErrInitialStateCompilation
		}
		foundDetail = true
		result = detail.PowderSkillImmunity
	}
	return result, nil
}

// itemMultiHitRangeOverride 编译道具对连续命中技能的完整区间覆盖冻结事实。
//
// 无道具或未声明覆盖时四个返回值均为零；同一 Item 的多个详情、只填写部分边界、倒置区间或扩张原始技能范围都会
// 拒绝启动对战，避免 Battle Engine 在运行时猜测资料配置意图。
func (compiler *initialMemberCompiler) itemMultiHitRangeOverride(itemID *snowflake.ID) (uint8, uint8, uint8, uint8, error) {
	if itemID == nil {
		return 0, 0, 0, 0, nil
	}
	matchedIndex := -1
	for index, detail := range compiler.itemRules.Details {
		if detail.ItemID != *itemID {
			continue
		}
		if matchedIndex >= 0 {
			return 0, 0, 0, 0, ErrInitialStateCompilation
		}
		matchedIndex = index
	}
	if matchedIndex < 0 {
		return 0, 0, 0, 0, nil
	}
	detail := compiler.itemRules.Details[matchedIndex]
	values := []int32{
		detail.MultiHitCountMinimum,
		detail.MultiHitCountMaximum,
		detail.MultiHitRequiredMinimum,
		detail.MultiHitRequiredMaximum,
	}
	allZero := true
	allPositive := true
	for _, value := range values {
		allZero = allZero && value == 0
		allPositive = allPositive && value > 0 && value <= 100
	}
	if allZero {
		return 0, 0, 0, 0, nil
	}
	if !allPositive || detail.MultiHitCountMinimum > detail.MultiHitCountMaximum ||
		detail.MultiHitRequiredMinimum > detail.MultiHitRequiredMaximum ||
		detail.MultiHitCountMinimum < detail.MultiHitRequiredMinimum ||
		detail.MultiHitCountMaximum > detail.MultiHitRequiredMaximum {
		return 0, 0, 0, 0, ErrInitialStateCompilation
	}
	return uint8(detail.MultiHitCountMinimum), uint8(detail.MultiHitCountMaximum),
		uint8(detail.MultiHitRequiredMinimum), uint8(detail.MultiHitRequiredMaximum), nil
}
