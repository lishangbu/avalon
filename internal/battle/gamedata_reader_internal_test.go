package battle

import (
	"errors"
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
	"github.com/lishangbu/avalon/internal/gamedata/skilldetail"
)

// TestLevelForFormatUsesFrozenNormalizationMechanic 验证已经编译进规则快照的等级机制优先决定本场等级，
// 而不是在回合期间再次读取可变资料。
func TestLevelForFormatUsesFrozenNormalizationMechanic(t *testing.T) {
	level := int32(50)
	value, err := levelForFormat(
		battleformat.Format{LevelRule: battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: &level}},
		50,
		87,
	)
	if err != nil {
		t.Fatalf("levelForFormat() error = %v", err)
	}
	if value != 50 {
		t.Fatalf("levelForFormat() = %d, want 50", value)
	}
	level = 49
	if _, err := levelForFormat(
		battleformat.Format{LevelRule: battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: &level}},
		50,
		87,
	); !errors.Is(err, ErrInitialStateCompilation) {
		t.Fatalf("mismatched levelForFormat() error = %v, want ErrInitialStateCompilation", err)
	}
}

// TestCompileBarrierApplicationsPreservesDistinctRules 验证三种屏障资料被逐项冻结到各自的引擎字段。
//
// 即使它们共享持续回合和概率的数值形状，编译器也不得把反射壁、光墙、极光幕收敛成通用类型，否则物理、特殊
// 与双防减伤范围会在战斗运行时丢失。
func TestCompileBarrierApplicationsPreservesDistinctRules(t *testing.T) {
	t.Parallel()

	compiled := battleengine.SkillSnapshot{TargetScope: battleengine.SkillTargetScopeSelectedTarget, DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, DamageClass: battleengine.DamageClassStatus}
	if err := compileReflectApplication(&compiled, &skilldetail.Reflect{TurnsRemaining: 5, ChancePercent: 100}, battleengine.SkillTargetScopeSelf); err != nil {
		t.Fatalf("compileReflectApplication() error = %v", err)
	}
	if err := compileLightScreenApplication(&compiled, &skilldetail.LightScreen{TurnsRemaining: 4, ChancePercent: 80}, battleengine.SkillTargetScopeSelf); err != nil {
		t.Fatalf("compileLightScreenApplication() error = %v", err)
	}
	if err := compileAuroraVeilApplication(&compiled, &skilldetail.AuroraVeil{TurnsRemaining: 3, ChancePercent: 60}, battleengine.SkillTargetScopeSelf); err != nil {
		t.Fatalf("compileAuroraVeilApplication() error = %v", err)
	}
	if compiled.ReflectApplication == nil || compiled.ReflectApplication.Effect.TurnsRemaining != 5 || compiled.ReflectApplication.ChancePercent != 100 {
		t.Fatalf("反射壁编译结果 = %+v", compiled.ReflectApplication)
	}
	if compiled.LightScreenApplication == nil || compiled.LightScreenApplication.Effect.TurnsRemaining != 4 || compiled.LightScreenApplication.ChancePercent != 80 {
		t.Fatalf("光墙编译结果 = %+v", compiled.LightScreenApplication)
	}
	if compiled.AuroraVeilApplication == nil || compiled.AuroraVeilApplication.Effect.TurnsRemaining != 3 || compiled.AuroraVeilApplication.ChancePercent != 60 {
		t.Fatalf("极光幕编译结果 = %+v", compiled.AuroraVeilApplication)
	}

	invalid := battleengine.SkillSnapshot{TargetScope: battleengine.SkillTargetScopeSelectedTarget, DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, DamageClass: battleengine.DamageClassPhysical}
	if err := compileLightScreenApplication(&invalid, &skilldetail.LightScreen{TurnsRemaining: 5, ChancePercent: 100}, battleengine.SkillTargetScopeSelf); !errors.Is(err, ErrInitialStateCompilation) {
		t.Fatalf("伤害技能建立光墙 error = %v，期望 ErrInitialStateCompilation", err)
	}
}

// TestCompileHazardAndClearApplicationsPreservesDistinctRules 验证四种入场危害和两种清场规则会冻结到各自的
// 引擎字段；范围或伤害类别不匹配时必须拒绝 Battle 启动。
func TestCompileHazardAndClearApplicationsPreservesDistinctRules(t *testing.T) {
	t.Parallel()

	hazard := battleengine.SkillSnapshot{TargetScope: battleengine.SkillTargetScopeSelectedTarget, DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, DamageClass: battleengine.DamageClassStatus}
	if err := compileSpikesApplication(&hazard, &skilldetail.Spikes{ChancePercent: 100}, battleengine.SkillTargetScopeSelectedTarget); err != nil {
		t.Fatalf("compileSpikesApplication() error = %v", err)
	}
	if err := compileStealthRockApplication(&hazard, &skilldetail.StealthRock{ChancePercent: 90}, battleengine.SkillTargetScopeSelectedTarget); err != nil {
		t.Fatalf("compileStealthRockApplication() error = %v", err)
	}
	if err := compileToxicSpikesApplication(&hazard, &skilldetail.ToxicSpikes{ChancePercent: 80}, battleengine.SkillTargetScopeSelectedTarget); err != nil {
		t.Fatalf("compileToxicSpikesApplication() error = %v", err)
	}
	if err := compileStickyWebApplication(&hazard, &skilldetail.StickyWeb{ChancePercent: 70}, battleengine.SkillTargetScopeSelectedTarget); err != nil {
		t.Fatalf("compileStickyWebApplication() error = %v", err)
	}
	if err := compileDefogApplication(&hazard, &skilldetail.Defog{Enabled: true}, battleengine.SkillTargetScopeSelectedTarget); err != nil {
		t.Fatalf("compileDefogApplication() error = %v", err)
	}
	if hazard.SpikesApplication == nil || hazard.SpikesApplication.ChancePercent != 100 ||
		hazard.StealthRockApplication == nil || hazard.StealthRockApplication.ChancePercent != 90 ||
		hazard.ToxicSpikesApplication == nil || hazard.ToxicSpikesApplication.ChancePercent != 80 ||
		hazard.StickyWebApplication == nil || hazard.StickyWebApplication.ChancePercent != 70 ||
		hazard.DefogApplication == nil {
		t.Fatalf("危害与清除浓雾编译结果 = %+v", hazard)
	}

	rapidSpin := battleengine.SkillSnapshot{TargetScope: battleengine.SkillTargetScopeSelectedTarget, DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, DamageClass: battleengine.DamageClassPhysical}
	if err := compileRapidSpinApplication(&rapidSpin, &skilldetail.RapidSpin{Enabled: true}, battleengine.SkillTargetScopeSelectedTarget); err != nil {
		t.Fatalf("compileRapidSpinApplication() error = %v", err)
	}
	if rapidSpin.RapidSpinApplication == nil {
		t.Fatalf("快速旋转编译结果 = %+v", rapidSpin)
	}
	if err := compileRapidSpinApplication(&hazard, &skilldetail.RapidSpin{Enabled: true}, battleengine.SkillTargetScopeSelectedTarget); !errors.Is(err, ErrInitialStateCompilation) {
		t.Fatalf("变化技能编译快速旋转 error = %v，期望 ErrInitialStateCompilation", err)
	}
	if err := compileDefogApplication(&rapidSpin, &skilldetail.Defog{Enabled: true}, battleengine.SkillTargetScopeSelectedTarget); !errors.Is(err, ErrInitialStateCompilation) {
		t.Fatalf("物理技能编译清除浓雾 error = %v，期望 ErrInitialStateCompilation", err)
	}
}
