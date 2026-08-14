package persistence

import (
	"context"
	"encoding/json/jsontext"
	"errors"
	"fmt"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"entgo.io/ent/dialect/sql"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgtype"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/gameskill"
	"github.com/lishangbu/avalon/ent/predicate"
	"github.com/lishangbu/avalon/internal/gamedata/battlerules"
	"github.com/lishangbu/avalon/internal/gamedata/skill"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

const (
	createSkillOperationID = "game-data.skill.create"
	updateSkillOperationID = "game-data.skill.update"
	deleteSkillOperationID = "game-data.skill.delete"
)

// skillTransactionRepository 隔离技能主体资料 Writer 的方法集合。
type skillTransactionRepository struct {
	parent   *Adapters
	client   *avalonent.Client
	executor database.Transaction
}

// optionalEntIdentifier 将领域可空 Identifier 转成 Ent 所需的可空 Identifier 指针。
func optionalEntIdentifier(value *snowflake.ID) *snowflake.ID {
	if value == nil {
		return nil
	}
	converted := *value
	return &converted
}

// pgIdentifierPointer 将 Ent 可空 Identifier 转成既有领域转换函数使用的 pgtype Identifier。
func pgIdentifierPointer(value *snowflake.ID) pgtype.Int8 {
	if value == nil {
		return pgtype.Int8{}
	}
	return pgIdentifier(*value)
}

// Create 鍦ㄥ簲鐢ㄦ湇鍔″凡寮€鍚殑浜嬪姟鍐呭垱寤虹ǔ瀹氳韩浠姐€佸疄鏃惰祫鏂欎慨璁€佸璁″拰骞傜瓑鍝嶅簲銆?
func (w *skillTransactionRepository) Create(ctx context.Context, record skill.CreateRecord) (skill.Skill, error) {
	// 鎽樿鍙寘鍚鎴风鍙噸澶嶆彁浜ょ殑涓氬姟浜嬪疄锛涙湇鍔＄鐢熸垚鐨?Identifier 鍜屽垵濮嬬増鏈笉鑳藉弬涓庤姹傝韩浠姐€?
	digest, err := idempotency.Digest(struct {
		OptionalValues skill.OptionalValues
		Code           string
		Name           string
		Priority       int32
		Enabled        bool
	}{
		OptionalValues: record.Skill.OptionalValues,
		Code:           record.Skill.Code,
		Name:           record.Skill.Name,
		Priority:       record.Skill.Priority,
		Enabled:        record.Skill.Enabled,
	})
	if err != nil {
		return skill.Skill{}, fmt.Errorf("璁＄畻鎶€鑳借祫鏂欏垱寤哄箓绛夋憳瑕? %w", err)
	}
	request := idempotency.Request{
		ActorAccountID: record.ActorAccountID, OperationID: createSkillOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.CreatedAt,
	}
	created := record.Skill
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &created)
	if err != nil {
		return skill.Skill{}, fmt.Errorf("认领技能资料创建幂等键: %w", err)
	}
	if replay {
		return created, nil
	}
	rulesPayload, err := battlerules.SkillJSON(created.Rules)
	if err != nil {
		return skill.Skill{}, skill.ErrInvalidSkill
	}
	builder := w.client.GameSkill.Create().SetID(created.ID).SetCode(created.Code).SetName(created.Name).SetPriority(created.Priority).SetRules(jsontext.Value(rulesPayload)).SetEnabled(created.Enabled).SetVersion(created.Version).SetCreatedAt(record.CreatedAt.UTC()).SetUpdatedAt(record.CreatedAt.UTC())
	builder.SetNillableElementID(optionalEntIdentifier(created.ElementID)).SetNillableDamageClassID(optionalEntIdentifier(created.DamageClassID)).SetNillableAccuracy(created.Accuracy).SetNillablePower(created.Power).SetNillablePp(created.PP).SetNillableEffectChance(created.EffectChance).SetNillableEffect(created.Effect).SetNillableShortEffect(created.ShortEffect).SetNillableDescription(created.Description)
	row, err := builder.Save(ctx)
	if err != nil {
		return skill.Skill{}, mapSkillWriteError(err, "鎻掑叆鎶€鑳借祫鏂?瀹炴椂璧勬枡淇")
	}
	created, err = skillFromEnt(row)
	if err != nil {
		return skill.Skill{}, fmt.Errorf("读取新建技能规则: %w", err)
	}
	if err := w.parent.recordGameDataAudit(
		ctx, w.executor, record.ActorAccountID, "game-data.skill.created", "game_skill",
		created.ID, record.RequestID, record.CreatedAt, nil, &created,
	); err != nil {
		return skill.Skill{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, created); err != nil {
		return skill.Skill{}, fmt.Errorf("淇濆瓨鎶€鑳借祫鏂欏垱寤哄箓绛夌粨鏋? %w", err)
	}
	return created, nil
}

// GetSkill 璇诲彇褰撳墠瀹炴椂璧勬枡涓寚瀹氱ǔ瀹氳韩浠界殑鎶€鑳戒富浣撹祫鏂欍€?
func (s *Adapters) GetSkill(ctx context.Context, skillID snowflake.ID) (skill.Skill, error) {
	row, err := s.pool.Client(ctx).GameSkill.Query().Where(gameskill.IDEQ(skillID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return skill.Skill{}, skill.ErrSkillNotFound
	}
	if err != nil {
		return skill.Skill{}, fmt.Errorf("鏌ヨ鎶€鑳借祫鏂? %w", err)
	}
	value, err := skillFromEnt(row)
	if err != nil {
		return skill.Skill{}, fmt.Errorf("解析技能战斗规则: %w", err)
	}
	return value, nil
}

// ListSkills 杩斿洖褰撳墠瀹炴椂璧勬枡涓鍚堟樉寮忕瓫閫夋潯浠剁殑鎶€鑳戒富浣撹祫鏂欓〉銆?
func (s *Adapters) ListSkills(ctx context.Context, query skill.ListQuery) (skill.Page, error) {
	filters := make([]predicate.GameSkill, 0, 10)
	if query.Q != "" {
		filters = append(filters, gameskill.Or(gameskill.CodeContainsFold(query.Q), gameskill.NameContainsFold(query.Q)))
	}
	if query.Code != "" {
		filters = append(filters, gameskill.CodeContainsFold(query.Code))
	}
	if query.Name != "" {
		filters = append(filters, gameskill.NameContainsFold(query.Name))
	}
	if query.ElementID != nil {
		filters = append(filters, gameskill.ElementIDEQ(*query.ElementID))
	}
	if query.DamageClassID != nil {
		filters = append(filters, gameskill.DamageClassIDEQ(*query.DamageClassID))
	}
	if query.Accuracy != nil {
		filters = append(filters, gameskill.AccuracyEQ(*query.Accuracy))
	}
	if query.Power != nil {
		filters = append(filters, gameskill.PowerEQ(*query.Power))
	}
	if query.PP != nil {
		filters = append(filters, gameskill.PpEQ(*query.PP))
	}
	if query.Priority != nil {
		filters = append(filters, gameskill.PriorityEQ(*query.Priority))
	}
	if query.EffectChance != nil {
		filters = append(filters, gameskill.EffectChanceEQ(*query.EffectChance))
	}
	if query.Enabled != nil {
		filters = append(filters, gameskill.EnabledEQ(*query.Enabled))
	}
	client := s.pool.Client(ctx)
	total, err := client.GameSkill.Query().Where(filters...).Count(ctx)
	if err != nil {
		return skill.Page{}, fmt.Errorf("缁熻鎶€鑳借祫鏂? %w", err)
	}
	order := []gameskill.OrderOption{gameskill.ByCode(), gameskill.ByID()}
	if query.Sort == skill.SortCodeDescending {
		order = []gameskill.OrderOption{gameskill.ByCode(sql.OrderDesc()), gameskill.ByID(sql.OrderDesc())}
	}
	if query.Sort == skill.SortNameAscending {
		order = []gameskill.OrderOption{gameskill.ByName(), gameskill.ByID()}
	}
	if query.Sort == skill.SortNameDescending {
		order = []gameskill.OrderOption{gameskill.ByName(sql.OrderDesc()), gameskill.ByID(sql.OrderDesc())}
	}
	rows, err := client.GameSkill.Query().Where(filters...).Order(order...).Limit(int(query.PageSize)).Offset(int(query.Page-1) * int(query.PageSize)).All(ctx)
	if err != nil {
		return skill.Page{}, fmt.Errorf("查询技能资料页: %w", err)
	}
	items := make([]skill.Skill, len(rows))
	for index, row := range rows {
		items[index], err = skillFromEnt(row)
		if err != nil {
			return skill.Page{}, fmt.Errorf("解析技能战斗规则: %w", err)
		}
	}
	return skill.Page{Items: items, Total: int64(total), Page: query.Page, PageSize: query.PageSize}, nil
}

// Update 鍦ㄥ簲鐢ㄦ湇鍔″凡寮€鍚殑浜嬪姟鍐呮洿鏂?瀹炴椂璧勬枡淇銆佸璁″拰骞傜瓑鍝嶅簲銆?
func (w *skillTransactionRepository) Update(ctx context.Context, record skill.UpdateRecord) (skill.Skill, error) {
	digest, err := idempotency.Digest(struct {
		Skill           skill.Skill
		Changes         skill.OptionalChanges
		ExpectedVersion int64
	}{record.Skill, record.Changes, record.ExpectedVersion})
	if err != nil {
		return skill.Skill{}, fmt.Errorf("璁＄畻鎶€鑳借祫鏂欐洿鏂板箓绛夋憳瑕? %w", err)
	}
	request := idempotency.Request{
		ActorAccountID: record.ActorAccountID, OperationID: updateSkillOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.UpdatedAt,
	}
	updated := record.Skill
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &updated)
	if err != nil {
		return skill.Skill{}, fmt.Errorf("认领技能资料更新幂等键: %w", err)
	}
	if replay {
		return updated, nil
	}
	currentRow, err := w.client.GameSkill.Query().Where(gameskill.IDEQ(record.Skill.ID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return skill.Skill{}, skill.ErrSkillNotFound
	}
	if err != nil {
		return skill.Skill{}, fmt.Errorf("閿佸畾寰呮洿鏂版妧鑳借祫鏂? %w", err)
	}
	current, err := skillFromEnt(currentRow)
	if err != nil {
		return skill.Skill{}, fmt.Errorf("解析待更新技能战斗规则: %w", err)
	}
	if current.Version != record.ExpectedVersion {
		return skill.Skill{}, skill.ErrSkillVersionConflict
	}
	rulesPayload, err := battlerules.SkillJSON(updated.Rules)
	if err != nil {
		return skill.Skill{}, skill.ErrInvalidSkill
	}
	builder := w.client.GameSkill.UpdateOne(currentRow).Where(gameskill.VersionEQ(record.ExpectedVersion)).SetCode(updated.Code).SetName(updated.Name).SetPriority(updated.Priority).SetRules(jsontext.Value(rulesPayload)).SetEnabled(updated.Enabled).SetVersion(updated.Version).SetUpdatedAt(record.UpdatedAt.UTC())
	if record.Changes.ElementID.Specified {
		if record.Changes.ElementID.Value == nil {
			builder.ClearElementID()
		} else {
			builder.SetElementID(*record.Changes.ElementID.Value)
		}
	}
	if record.Changes.DamageClassID.Specified {
		if record.Changes.DamageClassID.Value == nil {
			builder.ClearDamageClassID()
		} else {
			builder.SetDamageClassID(*record.Changes.DamageClassID.Value)
		}
	}
	if record.Changes.Accuracy.Specified {
		if record.Changes.Accuracy.Value == nil {
			builder.ClearAccuracy()
		} else {
			builder.SetAccuracy(*record.Changes.Accuracy.Value)
		}
	}
	if record.Changes.Power.Specified {
		if record.Changes.Power.Value == nil {
			builder.ClearPower()
		} else {
			builder.SetPower(*record.Changes.Power.Value)
		}
	}
	if record.Changes.PP.Specified {
		if record.Changes.PP.Value == nil {
			builder.ClearPp()
		} else {
			builder.SetPp(*record.Changes.PP.Value)
		}
	}
	if record.Changes.EffectChance.Specified {
		if record.Changes.EffectChance.Value == nil {
			builder.ClearEffectChance()
		} else {
			builder.SetEffectChance(*record.Changes.EffectChance.Value)
		}
	}
	if record.Changes.Effect.Specified {
		if record.Changes.Effect.Value == nil {
			builder.ClearEffect()
		} else {
			builder.SetEffect(*record.Changes.Effect.Value)
		}
	}
	if record.Changes.ShortEffect.Specified {
		if record.Changes.ShortEffect.Value == nil {
			builder.ClearShortEffect()
		} else {
			builder.SetShortEffect(*record.Changes.ShortEffect.Value)
		}
	}
	if record.Changes.Description.Specified {
		if record.Changes.Description.Value == nil {
			builder.ClearDescription()
		} else {
			builder.SetDescription(*record.Changes.Description.Value)
		}
	}
	row, err := builder.Save(ctx)
	if avalonent.IsNotFound(err) {
		return skill.Skill{}, skill.ErrSkillVersionConflict
	}
	if err != nil {
		return skill.Skill{}, mapSkillWriteError(err, "鏇存柊鎶€鑳借祫鏂?瀹炴椂璧勬枡淇")
	}
	updated, err = skillFromEnt(row)
	if err != nil {
		return skill.Skill{}, fmt.Errorf("解析已更新技能战斗规则: %w", err)
	}
	if err := w.parent.recordGameDataAudit(
		ctx, w.executor, record.ActorAccountID, "game-data.skill.updated", "game_skill",
		updated.ID, record.RequestID, record.UpdatedAt, &current, &updated,
	); err != nil {
		return skill.Skill{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, updated); err != nil {
		return skill.Skill{}, fmt.Errorf("淇濆瓨鎶€鑳借祫鏂欐洿鏂板箓绛夌粨鏋? %w", err)
	}
	return updated, nil
}

// Delete 鍦ㄥ簲鐢ㄦ湇鍔″凡寮€鍚殑浜嬪姟鍐呯鐢?瀹炴椂璧勬枡淇銆佽褰曞璁″苟淇濆瓨骞傜瓑鍝嶅簲銆?
func (w *skillTransactionRepository) Disable(ctx context.Context, record skill.DisableRecord) error {
	digest, err := idempotency.Digest(struct {
		SkillID         snowflake.ID
		ExpectedVersion int64
	}{record.SkillID, record.ExpectedVersion})
	if err != nil {
		return fmt.Errorf("璁＄畻鎶€鑳借祫鏂欑鐢ㄥ箓绛夋憳瑕? %w", err)
	}
	request := idempotency.Request{
		ActorAccountID: record.ActorAccountID, OperationID: deleteSkillOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.DisabledAt,
	}
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	response := struct {
		Disabled bool `json:"disabled"`
	}{}
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &response)
	if err != nil {
		return fmt.Errorf("认领技能资料禁用幂等键: %w", err)
	}
	if replay {
		return nil
	}
	currentRow, err := w.client.GameSkill.Query().Where(gameskill.IDEQ(record.SkillID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return skill.ErrSkillNotFound
	}
	if err != nil {
		return fmt.Errorf("閿佸畾寰呯鐢ㄦ妧鑳借祫鏂? %w", err)
	}
	current, err := skillFromEnt(currentRow)
	if err != nil {
		return fmt.Errorf("解析待禁用技能战斗规则: %w", err)
	}
	if current.Version != record.ExpectedVersion {
		return skill.ErrSkillVersionConflict
	}
	if _, err := w.client.GameSkill.UpdateOne(currentRow).Where(gameskill.VersionEQ(record.ExpectedVersion)).SetEnabled(false).SetVersion(record.ExpectedVersion + 1).SetUpdatedAt(record.DisabledAt.UTC()).Save(ctx); avalonent.IsNotFound(err) {
		return skill.ErrSkillVersionConflict
	} else if err != nil {
		var databaseError *pgconn.PgError
		if errors.As(err, &databaseError) && databaseError.Code == "23503" {
			return skill.ErrSkillReferenced
		}
		return fmt.Errorf("绂佺敤鎶€鑳借祫鏂?瀹炴椂璧勬枡淇: %w", err)
	}
	if err := w.parent.recordGameDataAudit(
		ctx, w.executor, record.ActorAccountID, "game-data.skill.disabled", "game_skill",
		current.ID, record.RequestID, record.DisabledAt, &current, nil,
	); err != nil {
		return err
	}
	response.Disabled = true
	if err := idempotency.Complete(ctx, writer, request, response); err != nil {
		return fmt.Errorf("淇濆瓨鎶€鑳借祫鏂欑鐢ㄥ箓绛夌粨鏋? %w", err)
	}
	return nil
}

func mapSkillWriteError(err error, action string) error {
	var databaseError *pgconn.PgError
	if errors.As(err, &databaseError) {
		switch databaseError.Code {
		case "23505":
			return skill.ErrSkillCodeConflict
		case "23503":
			return skill.ErrSkillDependencyNotFound
		}
	}
	return fmt.Errorf("%s: %w", action, err)
}
