package persistence

import (
	"context"
	"crypto/sha256"
	"encoding/json"
	"fmt"
	"sort"

	"time"

	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/battleparticipantreservation"
	"github.com/lishangbu/avalon/ent/playercharacter"
	"github.com/lishangbu/avalon/ent/playercharacterequipmentloadoutentry"
	"github.com/lishangbu/avalon/ent/playercharacterprofession"
	"github.com/lishangbu/avalon/ent/rpgprofession"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	rpg "github.com/lishangbu/avalon/internal/rpg"
)

// GetActiveProfessions 返回活动角色当前参与资格判定的职业集合。
func (adapter *Adapters) GetActiveProfessions(ctx context.Context, accountID snowflake.ID) ([]rpg.ActiveProfession, error) {
	client := adapter.pool.Client(ctx)
	playerID, err := activePlayerCharacterID(ctx, client, accountID)
	if err != nil {
		return nil, err
	}
	return readActiveProfessions(ctx, client, playerID)
}

// ReplaceActiveProfessions 锁定角色与当前 Loadout，先校验目标集合再原子切换 active 标志。
func (adapter *Adapters) ReplaceActiveProfessions(ctx context.Context, command rpg.ReplaceActiveProfessionsCommand) ([]rpg.ActiveProfession, error) {
	if !command.AccountID.IsValid() || len(command.ProfessionIDs) < 1 || len(command.ProfessionIDs) > 8 || !idempotency.ValidKey(command.IdempotencyKey) {
		return nil, rpg.ErrProfessionUnavailable
	}
	ids := append([]snowflake.ID(nil), command.ProfessionIDs...)
	sort.Slice(ids, func(i, j int) bool { return ids[i] < ids[j] })
	for index, id := range ids {
		if !id.IsValid() || index > 0 && id == ids[index-1] {
			return nil, rpg.ErrProfessionUnavailable
		}
	}
	now := command.Now.UTC()
	if command.Now.IsZero() {
		now = time.Now().UTC()
	}
	payload, _ := json.Marshal(ids)
	digest := sha256.Sum256(payload)
	result := []rpg.ActiveProfession{}
	err := adapter.pool.WithinTransaction(ctx, func(txctx context.Context) error {
		client := adapter.pool.Client(txctx)
		playerID, err := activePlayerCharacterID(txctx, client, command.AccountID)
		if err != nil {
			return err
		}
		replayed, err := adapter.claimPlayerResponse(txctx, client, playerID, "rpg.professions.active.replace", command.IdempotencyKey, digest[:], &result, now)
		if err != nil || replayed {
			return err
		}
		character, err := client.PlayerCharacter.Query().Where(playercharacter.IDEQ(playerID)).ForUpdate().Only(txctx)
		if err != nil {
			return fmt.Errorf("锁定职业所属角色: %w", err)
		}
		reserved, err := client.BattleParticipantReservation.Query().Where(battleparticipantreservation.IDEQ(playerID)).Exist(txctx)
		if err != nil {
			return fmt.Errorf("查询职业 Battle Reservation: %w", err)
		}
		if reserved {
			return rpg.ErrProfessionChangeInBattle
		}
		owned, err := client.PlayerCharacterProfession.Query().Where(playercharacterprofession.PlayerCharacterIDEQ(playerID), playercharacterprofession.ProfessionIDIn(ids...), playercharacterprofession.HasProfessionWith(rpgprofession.EnabledEQ(true))).WithProfession().ForUpdate().All(txctx)
		if err != nil || len(owned) != len(ids) {
			return rpg.ErrProfessionUnavailable
		}
		loadout, err := client.PlayerCharacterEquipmentLoadoutEntry.Query().Where(playercharacterequipmentloadoutentry.PlayerCharacterIDEQ(playerID)).WithEquipmentInstance(func(query *avalonent.PlayerCharacterEquipmentInstanceQuery) {
			query.WithEquipment(func(equipment *avalonent.GameEquipmentQuery) { equipment.WithProfessions() })
		}).All(txctx)
		if err != nil {
			return fmt.Errorf("读取职业变更 Equipment Loadout: %w", err)
		}
		candidates := make([]rpg.EquipmentLoadoutCandidate, 0, len(loadout))
		for _, entry := range loadout {
			instance := entry.Edges.EquipmentInstance
			if instance == nil || instance.Edges.Equipment == nil {
				return rpg.ErrEquipmentLoadoutConflict
			}
			equipment := instance.Edges.Equipment
			allowed := make([]snowflake.ID, 0, len(equipment.Edges.Professions))
			for _, relation := range equipment.Edges.Professions {
				allowed = append(allowed, relation.ProfessionID)
			}
			candidates = append(candidates, rpg.EquipmentLoadoutCandidate{Slot: rpg.EquipmentSlot(entry.Slot), InstanceID: instance.ID, SlotType: rpg.EquipmentSlotType(equipment.SlotType), Handedness: rpg.EquipmentHandedness(optionalString(equipment.Handedness)), MinimumLevel: equipment.MinimumLevel, ProfessionIDs: allowed})
		}
		if err := rpg.ValidateEquipmentLoadout(character.Level, ids, candidates); err != nil {
			return err
		}
		if _, err = client.PlayerCharacterProfession.Update().Where(playercharacterprofession.PlayerCharacterIDEQ(playerID)).SetActive(false).SetUpdatedAt(now).Save(txctx); err != nil {
			return fmt.Errorf("停用旧职业集合: %w", err)
		}
		if _, err = client.PlayerCharacterProfession.Update().Where(playercharacterprofession.PlayerCharacterIDEQ(playerID), playercharacterprofession.ProfessionIDIn(ids...)).SetActive(true).SetUpdatedAt(now).Save(txctx); err != nil {
			return fmt.Errorf("激活目标职业集合: %w", err)
		}
		result, err = readActiveProfessions(txctx, client, playerID)
		if err != nil {
			return err
		}
		operationID, err := adapter.newID.Next(txctx)
		if err != nil {
			return err
		}
		if err = adapter.createEquipmentOutbox(txctx, client, "rpg.professions.active-replaced.v1", operationID, playerID, now); err != nil {
			return err
		}
		return adapter.completePlayerResponse(txctx, client, playerID, "rpg.professions.active.replace", command.IdempotencyKey, result)
	})
	return result, err
}

// readActiveProfessions 按 Profession Identifier 稳定排序组装激活职业视图。
func readActiveProfessions(ctx context.Context, client *avalonent.Client, playerID snowflake.ID) ([]rpg.ActiveProfession, error) {
	rows, err := client.PlayerCharacterProfession.Query().Where(playercharacterprofession.PlayerCharacterIDEQ(playerID), playercharacterprofession.ActiveEQ(true)).WithProfession().Order(avalonent.Asc(playercharacterprofession.FieldProfessionID)).All(ctx)
	if err != nil {
		return nil, fmt.Errorf("读取激活职业集合: %w", err)
	}
	result := make([]rpg.ActiveProfession, 0, len(rows))
	for _, row := range rows {
		if row.Edges.Profession == nil {
			return nil, rpg.ErrProfessionUnavailable
		}
		result = append(result, rpg.ActiveProfession{ProfessionID: row.ProfessionID, Name: row.Edges.Profession.Name, Level: row.Level, Experience: row.Experience, Version: row.Version})
	}
	return result, nil
}
