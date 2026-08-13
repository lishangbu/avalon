package abilitydetail

// TerrainKind 是特性资料可声明的普通全场场地类别。
//
// 虽然运输层与技能资料使用相同稳定 code，本领域仍定义自己的封闭类型，避免不同触发时机共享可变效果结构。
type TerrainKind string

const (
	// TerrainKindElectric 表示电气场地。
	TerrainKindElectric TerrainKind = "electric"
	// TerrainKindGrassy 表示青草场地。
	TerrainKindGrassy TerrainKind = "grassy"
	// TerrainKindMisty 表示薄雾场地。
	TerrainKindMisty TerrainKind = "misty"
	// TerrainKindPsychic 表示精神场地。
	TerrainKindPsychic TerrainKind = "psychic"
)

// validTerrainKind 报告场地是否属于当前战斗引擎支持的封闭集合。
func validTerrainKind(value TerrainKind) bool {
	switch value {
	case TerrainKindElectric, TerrainKindGrassy, TerrainKindMisty, TerrainKindPsychic:
		return true
	default:
		return false
	}
}

// SwitchInTerrain 是特性持有成员进入场地时建立普通场地的独立资料规则。
//
// 它与技能场地、普通天气和强天气分开：本规则没有概率，持续回合只能由明确字段提供，不能通过特性名称
// 或任意效果参数推断。
type SwitchInTerrain struct {
	// Terrain 是进入场地后尝试建立的封闭普通场地种类。
	Terrain TerrainKind
	// TurnsRemaining 是场地建立时的正剩余完整回合数。
	TurnsRemaining int32
}

// cloneSwitchInTerrain 深复制可选的入场普通场地资料。
func cloneSwitchInTerrain(value *SwitchInTerrain) *SwitchInTerrain {
	if value == nil {
		return nil
	}
	return &SwitchInTerrain{Terrain: value.Terrain, TurnsRemaining: value.TurnsRemaining}
}

// validSwitchInTerrain 校验完整的入场普通场地资料。
func validSwitchInTerrain(value *SwitchInTerrain) bool {
	return value == nil || (validTerrainKind(value.Terrain) && value.TurnsRemaining >= 1 && value.TurnsRemaining <= 100)
}
