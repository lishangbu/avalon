package battlerules_test

import (
	"encoding/json"
	"reflect"
	"testing"

	"github.com/lishangbu/avalon/internal/gamedata/abilitydetail"
	"github.com/lishangbu/avalon/internal/gamedata/battlerules"
	"github.com/lishangbu/avalon/internal/gamedata/skilldetail"
)

func TestSkillRulesRoundTrip(t *testing.T) {
	values := skilldetail.OptionalValues{
		DamageMode:        skilldetail.DamageModeFormula,
		MakesContact:      true,
		ForceTargetSwitch: true,
	}
	rules, ok := battlerules.NewSkill(values)
	if !ok {
		t.Fatal("NewSkill() rejected valid rules")
	}
	payload, err := json.Marshal(rules)
	if err != nil {
		t.Fatalf("Marshal() error = %v", err)
	}
	var decoded battlerules.Skill
	if err := json.Unmarshal(payload, &decoded); err != nil {
		t.Fatalf("Unmarshal() error = %v", err)
	}
	actual, ok := decoded.Values()
	expected, expectedOK := skilldetail.NormalizeForRules(values)
	if !ok || !expectedOK || !reflect.DeepEqual(actual, expected) {
		t.Fatalf("Values() = %+v, %v", actual, ok)
	}
}

func TestAbilityRulesRoundTripByExecutionTime(t *testing.T) {
	values := abilitydetail.OptionalValues{
		AccuracyAlwaysHits:                 true,
		SwitchInCopyOpponentAbility:        true,
		SwitchOutMajorStatusCure:           true,
		DamageCrossedHalfHPForceSelfSwitch: true,
		TerastallizationEnvironmentClear:   true,
	}
	rules, ok := battlerules.NewAbility(values)
	if !ok {
		t.Fatal("NewAbility() rejected valid rules")
	}
	if rules.Passive == nil || rules.OnSwitchIn == nil || rules.OnSwitchOut == nil || rules.OnDamage == nil || rules.OnTerastallization == nil {
		t.Fatalf("NewAbility() did not preserve event groups: %+v", rules)
	}
	payload, err := json.Marshal(rules)
	if err != nil {
		t.Fatalf("Marshal() error = %v", err)
	}
	var decoded battlerules.Ability
	if err := json.Unmarshal(payload, &decoded); err != nil {
		t.Fatalf("Unmarshal() error = %v", err)
	}
	actual, ok := decoded.Values()
	if !ok || !reflect.DeepEqual(actual, values) {
		t.Fatalf("Values() = %+v, %v", actual, ok)
	}
}

func TestParseAbilityRejectsRuleInWrongExecutionGroup(t *testing.T) {
	_, err := battlerules.ParseAbility([]byte(`{"passive":{"switchInCopyOpponentAbility":true}}`))
	if err == nil {
		t.Fatal("ParseAbility() accepted switch-in rule in passive group")
	}
}

func TestParseSkillRejectsUnknownField(t *testing.T) {
	_, err := battlerules.ParseSkill([]byte(`{"onUse":{"unknownRule":true}}`))
	if err == nil {
		t.Fatal("ParseSkill() accepted unknown field")
	}
}

func TestParseAbilityRejectsUnknownField(t *testing.T) {
	_, err := battlerules.ParseAbility([]byte(`{"passive":{"unknownRule":true}}`))
	if err == nil {
		t.Fatal("ParseAbility() accepted unknown field")
	}
}
