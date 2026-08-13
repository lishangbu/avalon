package rpg

import (
	"context"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/gamecreature"
	"github.com/lishangbu/avalon/ent/gamecurrency"
	"github.com/lishangbu/avalon/ent/gameitem"
	"github.com/lishangbu/avalon/ent/rpglocation"
	"github.com/lishangbu/avalon/ent/rpgnpc"
	"github.com/lishangbu/avalon/ent/rpgprofession"
	"github.com/lishangbu/avalon/ent/rpgprofessionskill"
	"github.com/lishangbu/avalon/ent/rpgquest"
	"github.com/lishangbu/avalon/ent/rpgquestobjective"
	"github.com/lishangbu/avalon/ent/rpgquestreward"
	"github.com/lishangbu/avalon/ent/rpgrecipe"
	"github.com/lishangbu/avalon/ent/rpgrecipeingredient"
	"github.com/lishangbu/avalon/ent/rpgrecipeoutput"
	"github.com/lishangbu/avalon/internal/gamedata/stablecode"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"strings"
	"time"
)

func ptrID(value *snowflake.ID) snowflake.ID {
	if value == nil {
		return 0
	}
	return *value
}

// ListQuests 返回任务、目标和奖励聚合。
func (s *EntWorldStore) ListQuests(ctx context.Context, size int) ([]AdminQuest, error) {
	c := s.pool.Client(ctx)
	rows, e := c.RpgQuest.Query().Order(rpgquest.ByCode(), rpgquest.ByID()).Limit(boundedPageSize(size, 200)).All(ctx)
	if e != nil {
		return nil, e
	}
	out := make([]AdminQuest, 0, len(rows))
	idx := map[snowflake.ID]int{}
	for _, r := range rows {
		idx[r.ID] = len(out)
		out = append(out, AdminQuest{ID: r.ID, StartNPCID: ptrID(r.StartNpcID), TurnInNPCID: ptrID(r.TurnInNpcID), PrerequisiteQuestID: ptrID(r.PrerequisiteQuestID), Code: r.Code, Name: r.Name, QuestType: r.QuestType, Description: r.Description, Repeatable: r.Repeatable, Enabled: r.Enabled, Version: r.Version, Objectives: []AdminQuestObjective{}, Rewards: []AdminQuestReward{}})
	}
	objectives, e := c.RpgQuestObjective.Query().Order(rpgquestobjective.ByQuestID(), rpgquestobjective.ByPosition(), rpgquestobjective.ByID()).All(ctx)
	if e != nil {
		return nil, e
	}
	for _, r := range objectives {
		if i, ok := idx[r.QuestID]; ok {
			out[i].Objectives = append(out[i].Objectives, AdminQuestObjective{ID: r.ID, Code: r.Code, Position: r.Position, ObjectiveType: r.ObjectiveType, TargetCreatureID: ptrID(r.TargetCreatureID), TargetItemID: ptrID(r.TargetItemID), TargetLocationID: ptrID(r.TargetLocationID), TargetNPCID: ptrID(r.TargetNpcID), RequiredCount: r.RequiredCount, Description: r.Description})
		}
	}
	rewards, e := c.RpgQuestReward.Query().Order(rpgquestreward.ByQuestID(), rpgquestreward.ByID()).All(ctx)
	if e != nil {
		return nil, e
	}
	for _, r := range rewards {
		if i, ok := idx[r.QuestID]; ok {
			out[i].Rewards = append(out[i].Rewards, AdminQuestReward{ID: r.ID, ItemID: ptrID(r.ItemID), CurrencyID: ptrID(r.CurrencyID), CreatureID: ptrID(r.CreatureID), Quantity: r.Quantity})
		}
	}
	return out, nil
}

// SaveQuest 使用父版本完整替换任务目标和奖励。
func (s *EntWorldStore) SaveQuest(ctx context.Context, c SaveQuestCommand) (AdminQuest, error) {
	v := c.Value
	v.Code, v.Name, v.QuestType, v.Description = strings.TrimSpace(v.Code), strings.TrimSpace(v.Name), strings.TrimSpace(v.QuestType), strings.TrimSpace(v.Description)
	update := v.ID.IsValid()
	types := map[string]bool{"main": true, "side": true, "daily": true, "profession": true}
	if !validAdminWrite(c.Write) || !validNamed(v.Code, v.Name) || !types[v.QuestType] || v.Description == "" || len([]rune(v.Description)) > 4000 || update && c.ExpectedVersion <= 0 || v.PrerequisiteQuestID == v.ID {
		return v, ErrInvalidAdminWorld
	}
	positions := map[int16]bool{}
	codes := map[string]bool{}
	objectiveTypes := map[string]bool{"collect": true, "capture": true, "defeat": true, "talk": true, "explore": true, "battle": true, "craft": true}
	for _, x := range v.Objectives {
		x.Code = strings.TrimSpace(x.Code)
		refs := boolCount(x.TargetCreatureID.IsValid(), x.TargetItemID.IsValid(), x.TargetLocationID.IsValid(), x.TargetNPCID.IsValid())
		if !stablecode.Valid(x.Code) || positions[x.Position] || codes[x.Code] || x.Position <= 0 || !objectiveTypes[x.ObjectiveType] || refs > 1 || x.RequiredCount <= 0 || strings.TrimSpace(x.Description) == "" || len([]rune(strings.TrimSpace(x.Description))) > 1000 {
			return v, ErrInvalidAdminWorld
		}
		positions[x.Position] = true
		codes[x.Code] = true
	}
	for _, x := range v.Rewards {
		if boolCount(x.ItemID.IsValid(), x.CurrencyID.IsValid(), x.CreatureID.IsValid()) != 1 || x.Quantity <= 0 {
			return v, ErrInvalidAdminWorld
		}
	}
	if !update {
		id, e := s.newID.Next(ctx)
		if e != nil {
			return v, e
		}
		v.ID, v.Version = id, 1
	} else {
		v.Version = c.ExpectedVersion + 1
	}
	return s.saveQuest(ctx, c, v, update)
}
func boolCount(values ...bool) int {
	n := 0
	for _, v := range values {
		if v {
			n++
		}
	}
	return n
}
func (s *EntWorldStore) saveQuest(ctx context.Context, c SaveQuestCommand, v AdminQuest, update bool) (AdminQuest, error) {
	digest, e := idempotency.Digest(struct {
		V AdminQuest
		E int64
	}{v, c.ExpectedVersion})
	if e != nil {
		return v, e
	}
	now := time.Now().UTC()
	req := idempotency.Request{ActorAccountID: c.Write.ActorAccountID, OperationID: "rpg.quest.save", Key: c.Write.IdempotencyKey, RequestDigest: digest, CreatedAt: now}
	e = s.pool.WithinTransaction(ctx, func(tx context.Context) error {
		client := s.pool.Client(tx)
		w := idempotency.NewPersistentWriter(idempotency.NewAdminEntRecords(client, s.newID))
		replay, e := idempotency.ClaimResponse(tx, w, req, &v)
		if e != nil || replay {
			return e
		}
		if e = validateQuestRefs(tx, client, v); e != nil {
			return e
		}
		var before *AdminQuest
		if !update {
			b := client.RpgQuest.Create().SetID(v.ID).SetCode(v.Code).SetName(v.Name).SetQuestType(v.QuestType).SetDescription(v.Description).SetRepeatable(v.Repeatable).SetEnabled(v.Enabled).SetVersion(1).SetCreatedAt(now).SetUpdatedAt(now)
			setQuestOptionalCreate(b, v)
			if _, e = b.Save(tx); e != nil {
				return adminWorldStoreError(e)
			}
		} else {
			r, e := client.RpgQuest.Query().Where(rpgquest.IDEQ(v.ID)).Only(tx)
			if e != nil {
				return adminWorldStoreError(e)
			}
			old := AdminQuest{ID: r.ID, Code: r.Code, Name: r.Name, QuestType: r.QuestType, Description: r.Description, Repeatable: r.Repeatable, Enabled: r.Enabled, Version: r.Version}
			before = &old
			b := client.RpgQuest.UpdateOne(r).Where(rpgquest.VersionEQ(c.ExpectedVersion)).SetCode(v.Code).SetName(v.Name).SetQuestType(v.QuestType).SetDescription(v.Description).SetRepeatable(v.Repeatable).SetEnabled(v.Enabled).SetVersion(v.Version).SetUpdatedAt(now)
			setQuestOptionalUpdate(b, v)
			if _, e = b.Save(tx); e != nil {
				return adminWorldStoreError(e)
			}
			if _, e = client.RpgQuestObjective.Delete().Where(rpgquestobjective.QuestIDEQ(v.ID)).Exec(tx); e != nil {
				return e
			}
			if _, e = client.RpgQuestReward.Delete().Where(rpgquestreward.QuestIDEQ(v.ID)).Exec(tx); e != nil {
				return e
			}
		}
		for i := range v.Objectives {
			id, e := s.newID.Next(tx)
			if e != nil {
				return e
			}
			v.Objectives[i].ID = id
			x := v.Objectives[i]
			b := client.RpgQuestObjective.Create().SetID(id).SetQuestID(v.ID).SetCode(x.Code).SetPosition(x.Position).SetObjectiveType(x.ObjectiveType).SetRequiredCount(x.RequiredCount).SetDescription(strings.TrimSpace(x.Description))
			if x.TargetCreatureID.IsValid() {
				b.SetTargetCreatureID(x.TargetCreatureID)
			}
			if x.TargetItemID.IsValid() {
				b.SetTargetItemID(x.TargetItemID)
			}
			if x.TargetLocationID.IsValid() {
				b.SetTargetLocationID(x.TargetLocationID)
			}
			if x.TargetNPCID.IsValid() {
				b.SetTargetNpcID(x.TargetNPCID)
			}
			if _, e = b.Save(tx); e != nil {
				return adminWorldStoreError(e)
			}
		}
		for i := range v.Rewards {
			id, e := s.newID.Next(tx)
			if e != nil {
				return e
			}
			v.Rewards[i].ID = id
			x := v.Rewards[i]
			b := client.RpgQuestReward.Create().SetID(id).SetQuestID(v.ID).SetQuantity(x.Quantity)
			if x.ItemID.IsValid() {
				b.SetItemID(x.ItemID)
			}
			if x.CurrencyID.IsValid() {
				b.SetCurrencyID(x.CurrencyID)
			}
			if x.CreatureID.IsValid() {
				b.SetCreatureID(x.CreatureID)
			}
			if _, e = b.Save(tx); e != nil {
				return adminWorldStoreError(e)
			}
		}
		return s.auditAndComplete(tx, w, req, c.Write, "rpg.quest.saved", "rpg_quest", v.ID, before, v, now)
	})
	if e != nil {
		return AdminQuest{}, e
	}
	return v, nil
}

func validateQuestRefs(ctx context.Context, c *avalonent.Client, v AdminQuest) error {
	for _, id := range []snowflake.ID{v.StartNPCID, v.TurnInNPCID} {
		if id.IsValid() {
			if _, e := c.RpgNpc.Query().Where(rpgnpc.IDEQ(id)).Only(ctx); e != nil {
				return adminWorldStoreError(e)
			}
		}
	}
	if v.PrerequisiteQuestID.IsValid() {
		if _, e := c.RpgQuest.Query().Where(rpgquest.IDEQ(v.PrerequisiteQuestID)).Only(ctx); e != nil {
			return adminWorldStoreError(e)
		}
	}
	for _, x := range v.Objectives {
		if x.TargetCreatureID.IsValid() {
			if _, e := c.GameCreature.Query().Where(gamecreature.IDEQ(x.TargetCreatureID)).Only(ctx); e != nil {
				return adminWorldStoreError(e)
			}
		}
		if x.TargetItemID.IsValid() {
			if _, e := c.GameItem.Query().Where(gameitem.IDEQ(x.TargetItemID)).Only(ctx); e != nil {
				return adminWorldStoreError(e)
			}
		}
		if x.TargetLocationID.IsValid() {
			if _, e := c.RpgLocation.Query().Where(rpglocation.IDEQ(x.TargetLocationID)).Only(ctx); e != nil {
				return adminWorldStoreError(e)
			}
		}
		if x.TargetNPCID.IsValid() {
			if _, e := c.RpgNpc.Query().Where(rpgnpc.IDEQ(x.TargetNPCID)).Only(ctx); e != nil {
				return adminWorldStoreError(e)
			}
		}
	}
	for _, x := range v.Rewards {
		if x.ItemID.IsValid() {
			if _, e := c.GameItem.Query().Where(gameitem.IDEQ(x.ItemID)).Only(ctx); e != nil {
				return adminWorldStoreError(e)
			}
		}
		if x.CurrencyID.IsValid() {
			if _, e := c.GameCurrency.Query().Where(gamecurrency.IDEQ(x.CurrencyID)).Only(ctx); e != nil {
				return adminWorldStoreError(e)
			}
		}
		if x.CreatureID.IsValid() {
			if _, e := c.GameCreature.Query().Where(gamecreature.IDEQ(x.CreatureID)).Only(ctx); e != nil {
				return adminWorldStoreError(e)
			}
		}
	}
	return nil
}
func setQuestOptionalCreate(b *avalonent.RpgQuestCreate, v AdminQuest) {
	if v.StartNPCID.IsValid() {
		b.SetStartNpcID(v.StartNPCID)
	}
	if v.TurnInNPCID.IsValid() {
		b.SetTurnInNpcID(v.TurnInNPCID)
	}
	if v.PrerequisiteQuestID.IsValid() {
		b.SetPrerequisiteQuestID(v.PrerequisiteQuestID)
	}
}
func setQuestOptionalUpdate(b *avalonent.RpgQuestUpdateOne, v AdminQuest) {
	if v.StartNPCID.IsValid() {
		b.SetStartNpcID(v.StartNPCID)
	} else {
		b.ClearStartNpcID()
	}
	if v.TurnInNPCID.IsValid() {
		b.SetTurnInNpcID(v.TurnInNPCID)
	} else {
		b.ClearTurnInNpcID()
	}
	if v.PrerequisiteQuestID.IsValid() {
		b.SetPrerequisiteQuestID(v.PrerequisiteQuestID)
	} else {
		b.ClearPrerequisiteQuestID()
	}
}

// ListRecipes 返回配方及其材料和产物聚合。
func (s *EntWorldStore) ListRecipes(ctx context.Context, size int) ([]AdminRecipe, error) {
	c := s.pool.Client(ctx)
	rows, err := c.RpgRecipe.Query().Order(rpgrecipe.ByCode(), rpgrecipe.ByID()).Limit(boundedPageSize(size, 200)).All(ctx)
	if err != nil {
		return nil, err
	}
	out := make([]AdminRecipe, 0, len(rows))
	index := make(map[snowflake.ID]int, len(rows))
	for _, row := range rows {
		index[row.ID] = len(out)
		out = append(out, AdminRecipe{ID: row.ID, Code: row.Code, Name: row.Name, RequiredProfessionCode: stringValue(row.RequiredProfessionCode), RequiredProfessionLevel: row.RequiredProfessionLevel, Enabled: row.Enabled, Version: row.Version, Ingredients: []AdminRecipeItem{}, Outputs: []AdminRecipeItem{}})
	}
	ingredients, err := c.RpgRecipeIngredient.Query().Order(rpgrecipeingredient.ByRecipeID(), rpgrecipeingredient.ByID()).All(ctx)
	if err != nil {
		return nil, err
	}
	for _, row := range ingredients {
		if i, ok := index[row.RecipeID]; ok {
			out[i].Ingredients = append(out[i].Ingredients, AdminRecipeItem{ID: row.ID, ItemID: row.ItemID, Quantity: row.Quantity})
		}
	}
	outputs, err := c.RpgRecipeOutput.Query().Order(rpgrecipeoutput.ByRecipeID(), rpgrecipeoutput.ByID()).All(ctx)
	if err != nil {
		return nil, err
	}
	for _, row := range outputs {
		if i, ok := index[row.RecipeID]; ok {
			out[i].Outputs = append(out[i].Outputs, AdminRecipeItem{ID: row.ID, ItemID: row.ItemID, Quantity: row.Quantity})
		}
	}
	return out, nil
}

func stringValue(value *string) string {
	if value == nil {
		return ""
	}
	return *value
}

// SaveRecipe 使用父版本完整替换配方材料和产物。
func (s *EntWorldStore) SaveRecipe(ctx context.Context, command SaveRecipeCommand) (AdminRecipe, error) {
	value := command.Value
	value.Code = strings.TrimSpace(value.Code)
	value.Name = strings.TrimSpace(value.Name)
	value.RequiredProfessionCode = strings.TrimSpace(value.RequiredProfessionCode)
	update := value.ID.IsValid()
	professionPresent := value.RequiredProfessionCode != ""
	levelPresent := value.RequiredProfessionLevel != nil
	if !validAdminWrite(command.Write) || !validNamed(value.Code, value.Name) || professionPresent != levelPresent || levelPresent && *value.RequiredProfessionLevel <= 0 || len(value.Outputs) == 0 || update && command.ExpectedVersion <= 0 {
		return value, ErrInvalidAdminWorld
	}
	if professionPresent && !stablecode.Valid(value.RequiredProfessionCode) || !validRecipeItems(value.Ingredients) || !validRecipeItems(value.Outputs) {
		return value, ErrInvalidAdminWorld
	}
	if !update {
		id, err := s.newID.Next(ctx)
		if err != nil {
			return value, err
		}
		value.ID, value.Version = id, 1
	} else {
		value.Version = command.ExpectedVersion + 1
	}
	return s.saveRecipe(ctx, command, value, update)
}

func validRecipeItems(items []AdminRecipeItem) bool {
	seen := make(map[snowflake.ID]bool, len(items))
	for _, item := range items {
		if !item.ItemID.IsValid() || item.Quantity <= 0 || seen[item.ItemID] {
			return false
		}
		seen[item.ItemID] = true
	}
	return true
}

func (s *EntWorldStore) saveRecipe(ctx context.Context, command SaveRecipeCommand, value AdminRecipe, update bool) (AdminRecipe, error) {
	digest, err := idempotency.Digest(struct {
		Value    AdminRecipe
		Expected int64
	}{value, command.ExpectedVersion})
	if err != nil {
		return value, err
	}
	now := time.Now().UTC()
	request := idempotency.Request{ActorAccountID: command.Write.ActorAccountID, OperationID: "rpg.recipe.save", Key: command.Write.IdempotencyKey, RequestDigest: digest, CreatedAt: now}
	err = s.pool.WithinTransaction(ctx, func(tx context.Context) error {
		client := s.pool.Client(tx)
		writer := idempotency.NewPersistentWriter(idempotency.NewAdminEntRecords(client, s.newID))
		replay, saveErr := idempotency.ClaimResponse(tx, writer, request, &value)
		if saveErr != nil || replay {
			return saveErr
		}
		if value.RequiredProfessionCode != "" {
			if _, saveErr = client.RpgProfession.Query().Where(rpgprofession.CodeEQ(value.RequiredProfessionCode)).Only(tx); saveErr != nil {
				return adminWorldStoreError(saveErr)
			}
		}
		for _, item := range append(append([]AdminRecipeItem{}, value.Ingredients...), value.Outputs...) {
			if _, saveErr = client.GameItem.Query().Where(gameitem.IDEQ(item.ItemID)).Only(tx); saveErr != nil {
				return adminWorldStoreError(saveErr)
			}
		}
		var before *AdminRecipe
		if !update {
			builder := client.RpgRecipe.Create().SetID(value.ID).SetCode(value.Code).SetName(value.Name).SetEnabled(value.Enabled).SetVersion(1).SetCreatedAt(now).SetUpdatedAt(now)
			setRecipeOptionalCreate(builder, value)
			if _, saveErr = builder.Save(tx); saveErr != nil {
				return adminWorldStoreError(saveErr)
			}
		} else {
			row, queryErr := client.RpgRecipe.Query().Where(rpgrecipe.IDEQ(value.ID)).Only(tx)
			if queryErr != nil {
				return adminWorldStoreError(queryErr)
			}
			old := AdminRecipe{ID: row.ID, Code: row.Code, Name: row.Name, RequiredProfessionCode: stringValue(row.RequiredProfessionCode), RequiredProfessionLevel: row.RequiredProfessionLevel, Enabled: row.Enabled, Version: row.Version}
			before = &old
			builder := client.RpgRecipe.UpdateOne(row).Where(rpgrecipe.VersionEQ(command.ExpectedVersion)).SetCode(value.Code).SetName(value.Name).SetEnabled(value.Enabled).SetVersion(value.Version).SetUpdatedAt(now)
			setRecipeOptionalUpdate(builder, value)
			if _, saveErr = builder.Save(tx); saveErr != nil {
				return adminWorldStoreError(saveErr)
			}
			if _, saveErr = client.RpgRecipeIngredient.Delete().Where(rpgrecipeingredient.RecipeIDEQ(value.ID)).Exec(tx); saveErr != nil {
				return saveErr
			}
			if _, saveErr = client.RpgRecipeOutput.Delete().Where(rpgrecipeoutput.RecipeIDEQ(value.ID)).Exec(tx); saveErr != nil {
				return saveErr
			}
		}
		for i := range value.Ingredients {
			id, idErr := s.newID.Next(tx)
			if idErr != nil {
				return idErr
			}
			value.Ingredients[i].ID = id
			item := value.Ingredients[i]
			if _, saveErr = client.RpgRecipeIngredient.Create().SetID(id).SetRecipeID(value.ID).SetItemID(item.ItemID).SetQuantity(item.Quantity).Save(tx); saveErr != nil {
				return adminWorldStoreError(saveErr)
			}
		}
		for i := range value.Outputs {
			id, idErr := s.newID.Next(tx)
			if idErr != nil {
				return idErr
			}
			value.Outputs[i].ID = id
			item := value.Outputs[i]
			if _, saveErr = client.RpgRecipeOutput.Create().SetID(id).SetRecipeID(value.ID).SetItemID(item.ItemID).SetQuantity(item.Quantity).Save(tx); saveErr != nil {
				return adminWorldStoreError(saveErr)
			}
		}
		return s.auditAndComplete(tx, writer, request, command.Write, "rpg.recipe.saved", "rpg_recipe", value.ID, before, value, now)
	})
	if err != nil {
		return AdminRecipe{}, err
	}
	return value, nil
}

func setRecipeOptionalCreate(builder *avalonent.RpgRecipeCreate, value AdminRecipe) {
	if value.RequiredProfessionCode != "" {
		builder.SetRequiredProfessionCode(value.RequiredProfessionCode).SetRequiredProfessionLevel(*value.RequiredProfessionLevel)
	}
}

func setRecipeOptionalUpdate(builder *avalonent.RpgRecipeUpdateOne, value AdminRecipe) {
	if value.RequiredProfessionCode == "" {
		builder.ClearRequiredProfessionCode().ClearRequiredProfessionLevel()
		return
	}
	builder.SetRequiredProfessionCode(value.RequiredProfessionCode).SetRequiredProfessionLevel(*value.RequiredProfessionLevel)
}

// ListProfessions 返回职业及其技能聚合。
func (s *EntWorldStore) ListProfessions(ctx context.Context, size int) ([]AdminProfession, error) {
	c := s.pool.Client(ctx)
	rows, err := c.RpgProfession.Query().Order(rpgprofession.ByCode(), rpgprofession.ByID()).Limit(boundedPageSize(size, 200)).All(ctx)
	if err != nil {
		return nil, err
	}
	out := make([]AdminProfession, 0, len(rows))
	index := make(map[snowflake.ID]int, len(rows))
	for _, row := range rows {
		index[row.ID] = len(out)
		out = append(out, AdminProfession{ID: row.ID, Code: row.Code, Name: row.Name, Description: stringValue(row.Description), MaximumLevel: row.MaximumLevel, Enabled: row.Enabled, Version: row.Version, Skills: []AdminProfessionSkill{}})
	}
	skills, err := c.RpgProfessionSkill.Query().Order(rpgprofessionskill.ByProfessionID(), rpgprofessionskill.ByRequiredLevel(), rpgprofessionskill.ByID()).All(ctx)
	if err != nil {
		return nil, err
	}
	for _, row := range skills {
		if i, ok := index[row.ProfessionID]; ok {
			out[i].Skills = append(out[i].Skills, AdminProfessionSkill{ID: row.ID, Code: row.Code, Name: row.Name, RequiredLevel: row.RequiredLevel, Description: stringValue(row.Description), Enabled: row.Enabled})
		}
	}
	return out, nil
}

// SaveProfession 使用父版本完整替换职业技能。
func (s *EntWorldStore) SaveProfession(ctx context.Context, command SaveProfessionCommand) (AdminProfession, error) {
	value := command.Value
	value.Code, value.Name, value.Description = strings.TrimSpace(value.Code), strings.TrimSpace(value.Name), strings.TrimSpace(value.Description)
	update := value.ID.IsValid()
	if !validAdminWrite(command.Write) || !validNamed(value.Code, value.Name) || len([]rune(value.Description)) > 4000 || value.MaximumLevel <= 0 || update && command.ExpectedVersion <= 0 {
		return value, ErrInvalidAdminWorld
	}
	seen := make(map[string]bool, len(value.Skills))
	for i := range value.Skills {
		value.Skills[i].Code = strings.TrimSpace(value.Skills[i].Code)
		value.Skills[i].Name = strings.TrimSpace(value.Skills[i].Name)
		value.Skills[i].Description = strings.TrimSpace(value.Skills[i].Description)
		skill := value.Skills[i]
		if !validNamed(skill.Code, skill.Name) || seen[skill.Code] || skill.RequiredLevel <= 0 || skill.RequiredLevel > value.MaximumLevel || len([]rune(skill.Description)) > 4000 {
			return value, ErrInvalidAdminWorld
		}
		seen[skill.Code] = true
	}
	if !update {
		id, err := s.newID.Next(ctx)
		if err != nil {
			return value, err
		}
		value.ID, value.Version = id, 1
	} else {
		value.Version = command.ExpectedVersion + 1
	}
	return s.saveProfession(ctx, command, value, update)
}

func (s *EntWorldStore) saveProfession(ctx context.Context, command SaveProfessionCommand, value AdminProfession, update bool) (AdminProfession, error) {
	digest, err := idempotency.Digest(struct {
		Value    AdminProfession
		Expected int64
	}{value, command.ExpectedVersion})
	if err != nil {
		return value, err
	}
	now := time.Now().UTC()
	request := idempotency.Request{ActorAccountID: command.Write.ActorAccountID, OperationID: "rpg.profession.save", Key: command.Write.IdempotencyKey, RequestDigest: digest, CreatedAt: now}
	err = s.pool.WithinTransaction(ctx, func(tx context.Context) error {
		client := s.pool.Client(tx)
		writer := idempotency.NewPersistentWriter(idempotency.NewAdminEntRecords(client, s.newID))
		replay, saveErr := idempotency.ClaimResponse(tx, writer, request, &value)
		if saveErr != nil || replay {
			return saveErr
		}
		var before *AdminProfession
		if !update {
			builder := client.RpgProfession.Create().SetID(value.ID).SetCode(value.Code).SetName(value.Name).SetMaximumLevel(value.MaximumLevel).SetEnabled(value.Enabled).SetVersion(1).SetCreatedAt(now).SetUpdatedAt(now)
			if value.Description != "" {
				builder.SetDescription(value.Description)
			}
			if _, saveErr = builder.Save(tx); saveErr != nil {
				return adminWorldStoreError(saveErr)
			}
		} else {
			row, queryErr := client.RpgProfession.Query().Where(rpgprofession.IDEQ(value.ID)).Only(tx)
			if queryErr != nil {
				return adminWorldStoreError(queryErr)
			}
			old := AdminProfession{ID: row.ID, Code: row.Code, Name: row.Name, Description: stringValue(row.Description), MaximumLevel: row.MaximumLevel, Enabled: row.Enabled, Version: row.Version}
			before = &old
			builder := client.RpgProfession.UpdateOne(row).Where(rpgprofession.VersionEQ(command.ExpectedVersion)).SetCode(value.Code).SetName(value.Name).SetMaximumLevel(value.MaximumLevel).SetEnabled(value.Enabled).SetVersion(value.Version).SetUpdatedAt(now)
			if value.Description == "" {
				builder.ClearDescription()
			} else {
				builder.SetDescription(value.Description)
			}
			if _, saveErr = builder.Save(tx); saveErr != nil {
				return adminWorldStoreError(saveErr)
			}
			if _, saveErr = client.RpgProfessionSkill.Delete().Where(rpgprofessionskill.ProfessionIDEQ(value.ID)).Exec(tx); saveErr != nil {
				return saveErr
			}
		}
		for i := range value.Skills {
			id, idErr := s.newID.Next(tx)
			if idErr != nil {
				return idErr
			}
			value.Skills[i].ID = id
			skill := value.Skills[i]
			builder := client.RpgProfessionSkill.Create().SetID(id).SetProfessionID(value.ID).SetCode(skill.Code).SetName(skill.Name).SetRequiredLevel(skill.RequiredLevel).SetEnabled(skill.Enabled)
			if skill.Description != "" {
				builder.SetDescription(skill.Description)
			}
			if _, saveErr = builder.Save(tx); saveErr != nil {
				return adminWorldStoreError(saveErr)
			}
		}
		return s.auditAndComplete(tx, writer, request, command.Write, "rpg.profession.saved", "rpg_profession", value.ID, before, value, now)
	})
	if err != nil {
		return AdminProfession{}, err
	}
	return value, nil
}
