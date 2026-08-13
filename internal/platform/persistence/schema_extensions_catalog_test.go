package persistence

import (
	"strings"
	"testing"
)

// TestSchemaForeignKeyExtensionsOnlyContainsUnsupportedRelations 防止普通外键回到原生 SQL 扩展目录。
func TestSchemaForeignKeyExtensionsOnlyContainsUnsupportedRelations(t *testing.T) {
	want := map[string]struct{}{
		"fk_battle_turn_submission_battle_id_side":                        {},
		"fk_battle_turn_submission_battle_id_state_version":               {},
		"fk_player_character_profession_skill_player_character__f52b2e54": {},
		"fk_player_character_quest_objective_player_character_i_d886e8fe": {},
		"fk_player_character_team_member_skill_team_id_member_p_96c52a3b": {},
		"fk_player_character_team_member_stat_team_id_member_po_6eb75f33": {},
		"fk_active_player_character_account_id_id":                        {},
		"fk_active_player_character_team_player_character_id_id":          {},
		"fk_battle_participant_reservation_player_character_id_id":        {},
		"fk_battle_runtime_lease_battle_id_id":                            {},
		"fk_battle_authoritative_summary_battle_id_id":                    {},
	}
	for _, definition := range schemaForeignKeyExtensions() {
		if _, ok := want[definition.name]; !ok {
			t.Errorf("unexpected PostgreSQL foreign-key exception %q", definition.name)
		}
		delete(want, definition.name)
	}
	for name := range want {
		t.Errorf("missing PostgreSQL foreign-key exception %q", name)
	}
}

// TestSchemaObjectNamesFollowConvention 防止索引和外键名称偏离统一约定或超过 PostgreSQL 限制。
func TestSchemaObjectNamesFollowConvention(t *testing.T) {
	for _, definition := range schemaIndexDefinitions {
		if len(definition.name) > 63 || (!strings.HasPrefix(definition.name, "idx_") && !strings.HasPrefix(definition.name, "uk_")) {
			t.Errorf("invalid index name %q", definition.name)
		}
		if !strings.Contains(definition.statement, " "+definition.name+" ON ") {
			t.Errorf("index statement does not use declared name %q", definition.name)
		}
	}
	for _, definition := range schemaForeignKeyDefinitions {
		if len(definition.name) > 63 || !strings.HasPrefix(definition.name, "fk_") {
			t.Errorf("invalid foreign-key name %q", definition.name)
		}
	}
}
