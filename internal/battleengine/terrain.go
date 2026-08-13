package battleengine

import "fmt"

// TerrainKind 是引擎支持的普通全场场地种类。
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

// valid 报告场地种类是否为当前引擎能够严格解释的封闭值。
func (kind TerrainKind) valid() bool {
	return kind == TerrainKindElectric || kind == TerrainKindGrassy || kind == TerrainKindMisty || kind == TerrainKindPsychic
}

// TerrainEffect 是已写入全场环境、会跨回合持续的普通场地。
type TerrainEffect struct {
	// Kind 是当前生效的封闭场地种类。
	Kind TerrainKind `json:"kind"`
	// TurnsRemaining 是包含当前结算回合在内的剩余完整场地回合数，必须为正数。
	TurnsRemaining uint8 `json:"turnsRemaining"`
}

// advanceTurn 推进一个完整回合后的场地；nil 表示场地在本回合末自然结束。
func (effect TerrainEffect) advanceTurn() *TerrainEffect {
	if effect.TurnsRemaining <= 1 {
		return nil
	}
	effect.TurnsRemaining--
	return &effect
}

// TerrainApplication 描述技能命中后尝试建立普通场地的独立规则。
type TerrainApplication struct {
	// Effect 是成功建立时写入全场环境的场地和持续回合。
	Effect TerrainEffect `json:"effect"`
	// ChancePercent 是场地建立的独立触发概率；100 表示必定建立且不消费概率随机数。
	ChancePercent uint8 `json:"chancePercent"`
}

// validateTerrainEffect 校验已经生效或即将生效的场地值。
func validateTerrainEffect(effect TerrainEffect) error {
	if !effect.Kind.valid() || effect.TurnsRemaining == 0 {
		return fmt.Errorf("%w: 场地效果无效", ErrInvalidInitialState)
	}
	return nil
}

// validateTerrainApplication 校验资料编译后冻结到技能快照的场地建立规则。
func validateTerrainApplication(application TerrainApplication) error {
	if application.ChancePercent == 0 || application.ChancePercent > 100 {
		return fmt.Errorf("%w: 场地触发概率无效", ErrInvalidInitialState)
	}
	return validateTerrainEffect(application.Effect)
}
