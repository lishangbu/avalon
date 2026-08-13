package rpg

import "testing"

// TestCompileEquipmentRulesNormalizesClosedTimings 验证编译器只接受闭集时机并输出稳定规范化 JSON。
func TestCompileEquipmentRulesNormalizesClosedTimings(t *testing.T) {
	compiled, err := CompileEquipmentRules([]byte(`{"onTurnEnd":[],"onBattleStart":[]}`))
	if err != nil {
		t.Fatal(err)
	}
	if string(compiled) != `{"onBattleStart":[],"onTurnEnd":[]}` {
		t.Fatalf("compiled = %s", compiled)
	}
	if _, err := CompileEquipmentRules([]byte(`{"onEquip":[]}`)); err != ErrEquipmentRulesInvalid {
		t.Fatalf("unknown timing error = %v", err)
	}
	for _, source := range []string{`{} {}`, `null`, `[]`, `{"onBattleStart":null}`} {
		if _, err := CompileEquipmentRules([]byte(source)); err != ErrEquipmentRulesInvalid {
			t.Fatalf("invalid rules %s error = %v", source, err)
		}
	}
}
