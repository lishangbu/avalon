package api

import (
	"context"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	rpgv1 "github.com/lishangbu/avalon/api/gen/go/avalon/rpg/v1"
	"github.com/lishangbu/avalon/internal/rpg"
)

// ListQuests 返回任务维护聚合。
func (s *AdminWorldService) ListQuests(ctx context.Context, request *rpgv1.ListQuestsRequest) (*rpgv1.ListQuestsResponse, error) {
	rows, err := s.store.ListQuests(ctx, int(request.GetPageSize()))
	if err != nil {
		return nil, adminError(err)
	}
	response := &rpgv1.ListQuestsResponse{Quests: make([]*rpgv1.AdminQuest, 0, len(rows))}
	for _, row := range rows {
		response.Quests = append(response.Quests, questMessage(row))
	}
	return response, nil
}

// SaveQuest 创建或更新任务及其目标和奖励。
func (s *AdminWorldService) SaveQuest(ctx context.Context, request *rpgv1.SaveQuestRequest) (*rpgv1.SaveQuestResponse, error) {
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	write, err := adminWriteContext(ctx, request.GetIdempotencyKey())
	if err != nil {
		return nil, err
	}
	id, err := optionalID(request.GetQuestId(), "INVALID_QUEST_ID", "Quest 标识无效")
	if err != nil {
		return nil, err
	}
	body := request.GetBody()
	startNPC, err := optionalID(body.GetStartNpcId(), "INVALID_NPC_ID", "起始 NPC 标识无效")
	if err != nil {
		return nil, err
	}
	turnInNPC, err := optionalID(body.GetTurnInNpcId(), "INVALID_NPC_ID", "交付 NPC 标识无效")
	if err != nil {
		return nil, err
	}
	prerequisite, err := optionalID(body.GetPrerequisiteQuestId(), "INVALID_QUEST_ID", "前置 Quest 标识无效")
	if err != nil {
		return nil, err
	}
	value := rpg.AdminQuest{ID: id, StartNPCID: startNPC, TurnInNPCID: turnInNPC, PrerequisiteQuestID: prerequisite, Code: body.GetCode(), Name: body.GetName(), QuestType: body.GetQuestType(), Description: body.GetDescription(), Repeatable: body.GetRepeatable(), Enabled: body.GetEnabled(), Objectives: make([]rpg.AdminQuestObjective, 0, len(body.GetObjectives())), Rewards: make([]rpg.AdminQuestReward, 0, len(body.GetRewards()))}
	for _, objective := range body.GetObjectives() {
		objectiveID, parseErr := optionalID(objective.GetQuestObjectiveId(), "INVALID_QUEST_OBJECTIVE_ID", "Quest Objective 标识无效")
		if parseErr != nil {
			return nil, parseErr
		}
		creature, parseErr := optionalID(objective.GetTargetCreatureId(), "INVALID_CREATURE_ID", "目标 Creature 标识无效")
		if parseErr != nil {
			return nil, parseErr
		}
		item, parseErr := optionalID(objective.GetTargetItemId(), "INVALID_ITEM_ID", "目标 Item 标识无效")
		if parseErr != nil {
			return nil, parseErr
		}
		location, parseErr := optionalID(objective.GetTargetLocationId(), "INVALID_LOCATION_ID", "目标 Location 标识无效")
		if parseErr != nil {
			return nil, parseErr
		}
		npc, parseErr := optionalID(objective.GetTargetNpcId(), "INVALID_NPC_ID", "目标 NPC 标识无效")
		if parseErr != nil {
			return nil, parseErr
		}
		value.Objectives = append(value.Objectives, rpg.AdminQuestObjective{ID: objectiveID, Code: objective.GetCode(), Position: int16(objective.GetPosition()), ObjectiveType: objective.GetObjectiveType(), TargetCreatureID: creature, TargetItemID: item, TargetLocationID: location, TargetNPCID: npc, RequiredCount: objective.GetRequiredCount(), Description: objective.GetDescription()})
	}
	for _, reward := range body.GetRewards() {
		item, parseErr := optionalID(reward.GetItemId(), "INVALID_ITEM_ID", "奖励 Item 标识无效")
		if parseErr != nil {
			return nil, parseErr
		}
		currency, parseErr := optionalID(reward.GetCurrencyId(), "INVALID_CURRENCY_ID", "奖励 Currency 标识无效")
		if parseErr != nil {
			return nil, parseErr
		}
		value.Rewards = append(value.Rewards, rpg.AdminQuestReward{ItemID: item, CurrencyID: currency, Quantity: reward.GetQuantity()})
	}
	saved, err := s.store.SaveQuest(ctx, rpg.SaveQuestCommand{Write: write, Value: value, ExpectedVersion: request.GetExpectedVersion()})
	if err != nil {
		return nil, adminError(err)
	}
	return &rpgv1.SaveQuestResponse{Quest: questMessage(saved)}, nil
}

func questMessage(value rpg.AdminQuest) *rpgv1.AdminQuest {
	message := &rpgv1.AdminQuest{Id: value.ID.String(), Code: value.Code, Name: value.Name, QuestType: value.QuestType, Description: value.Description, Repeatable: value.Repeatable, Enabled: value.Enabled, Version: value.Version, Objectives: make([]*rpgv1.AdminQuestObjective, 0, len(value.Objectives)), Rewards: make([]*rpgv1.AdminQuestReward, 0, len(value.Rewards))}
	message.StartNpcId = idString(value.StartNPCID)
	message.TurnInNpcId = idString(value.TurnInNPCID)
	message.PrerequisiteQuestId = idString(value.PrerequisiteQuestID)
	for _, objective := range value.Objectives {
		message.Objectives = append(message.Objectives, &rpgv1.AdminQuestObjective{Id: objective.ID.String(), Code: objective.Code, Position: int32(objective.Position), ObjectiveType: objective.ObjectiveType, TargetCreatureId: idString(objective.TargetCreatureID), TargetItemId: idString(objective.TargetItemID), TargetLocationId: idString(objective.TargetLocationID), TargetNpcId: idString(objective.TargetNPCID), RequiredCount: objective.RequiredCount, Description: objective.Description})
	}
	for _, reward := range value.Rewards {
		message.Rewards = append(message.Rewards, &rpgv1.AdminQuestReward{Id: reward.ID.String(), ItemId: idString(reward.ItemID), CurrencyId: idString(reward.CurrencyID), Quantity: reward.Quantity})
	}
	return message
}

// ListRecipes 返回制作配方维护聚合。
func (s *AdminWorldService) ListRecipes(ctx context.Context, request *rpgv1.ListRecipesRequest) (*rpgv1.ListRecipesResponse, error) {
	rows, err := s.store.ListRecipes(ctx, int(request.GetPageSize()))
	if err != nil {
		return nil, adminError(err)
	}
	response := &rpgv1.ListRecipesResponse{Recipes: make([]*rpgv1.AdminRecipe, 0, len(rows))}
	for _, row := range rows {
		response.Recipes = append(response.Recipes, recipeMessage(row))
	}
	return response, nil
}

// SaveRecipe 创建或更新制作配方及其材料和产物。
func (s *AdminWorldService) SaveRecipe(ctx context.Context, request *rpgv1.SaveRecipeRequest) (*rpgv1.SaveRecipeResponse, error) {
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	write, err := adminWriteContext(ctx, request.GetIdempotencyKey())
	if err != nil {
		return nil, err
	}
	id, err := optionalID(request.GetRecipeId(), "INVALID_RECIPE_ID", "Recipe 标识无效")
	if err != nil {
		return nil, err
	}
	body := request.GetBody()
	value := rpg.AdminRecipe{ID: id, Code: body.GetCode(), Name: body.GetName(), RequiredProfessionCode: body.GetRequiredProfessionCode(), RequiredProfessionLevel: body.RequiredProfessionLevel, Enabled: body.GetEnabled(), Ingredients: make([]rpg.AdminRecipeItem, 0, len(body.GetIngredients())), Outputs: make([]rpg.AdminRecipeItem, 0, len(body.GetOutputs()))}
	for _, ingredient := range body.GetIngredients() {
		item, parseErr := requiredID(ingredient.GetItemId(), "INVALID_ITEM_ID", "材料 Item 标识无效")
		if parseErr != nil {
			return nil, parseErr
		}
		value.Ingredients = append(value.Ingredients, rpg.AdminRecipeItem{ItemID: item, Quantity: ingredient.GetQuantity()})
	}
	for _, output := range body.GetOutputs() {
		item, parseErr := requiredID(output.GetItemId(), "INVALID_ITEM_ID", "产物 Item 标识无效")
		if parseErr != nil {
			return nil, parseErr
		}
		value.Outputs = append(value.Outputs, rpg.AdminRecipeItem{ItemID: item, Quantity: output.GetQuantity()})
	}
	saved, err := s.store.SaveRecipe(ctx, rpg.SaveRecipeCommand{Write: write, Value: value, ExpectedVersion: request.GetExpectedVersion()})
	if err != nil {
		return nil, adminError(err)
	}
	return &rpgv1.SaveRecipeResponse{Recipe: recipeMessage(saved)}, nil
}

func recipeMessage(value rpg.AdminRecipe) *rpgv1.AdminRecipe {
	message := &rpgv1.AdminRecipe{Id: value.ID.String(), Code: value.Code, Name: value.Name, RequiredProfessionCode: value.RequiredProfessionCode, RequiredProfessionLevel: value.RequiredProfessionLevel, Enabled: value.Enabled, Version: value.Version, Ingredients: make([]*rpgv1.AdminRecipeItem, 0, len(value.Ingredients)), Outputs: make([]*rpgv1.AdminRecipeItem, 0, len(value.Outputs))}
	for _, ingredient := range value.Ingredients {
		message.Ingredients = append(message.Ingredients, &rpgv1.AdminRecipeItem{Id: ingredient.ID.String(), ItemId: ingredient.ItemID.String(), Quantity: ingredient.Quantity})
	}
	for _, output := range value.Outputs {
		message.Outputs = append(message.Outputs, &rpgv1.AdminRecipeItem{Id: output.ID.String(), ItemId: output.ItemID.String(), Quantity: output.Quantity})
	}
	return message
}

// ListProfessions 返回职业维护聚合。
func (s *AdminWorldService) ListProfessions(ctx context.Context, request *rpgv1.ListProfessionsRequest) (*rpgv1.ListProfessionsResponse, error) {
	rows, err := s.store.ListProfessions(ctx, int(request.GetPageSize()))
	if err != nil {
		return nil, adminError(err)
	}
	response := &rpgv1.ListProfessionsResponse{Professions: make([]*rpgv1.AdminProfession, 0, len(rows))}
	for _, row := range rows {
		response.Professions = append(response.Professions, professionMessage(row))
	}
	return response, nil
}

// SaveProfession 创建或更新职业及其技能。
func (s *AdminWorldService) SaveProfession(ctx context.Context, request *rpgv1.SaveProfessionRequest) (*rpgv1.SaveProfessionResponse, error) {
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	write, err := adminWriteContext(ctx, request.GetIdempotencyKey())
	if err != nil {
		return nil, err
	}
	id, err := optionalID(request.GetProfessionId(), "INVALID_PROFESSION_ID", "Profession 标识无效")
	if err != nil {
		return nil, err
	}
	body := request.GetBody()
	value := rpg.AdminProfession{ID: id, Code: body.GetCode(), Name: body.GetName(), Description: body.GetDescription(), MaximumLevel: body.GetMaximumLevel(), Enabled: body.GetEnabled(), Skills: make([]rpg.AdminProfessionSkill, 0, len(body.GetSkills()))}
	for _, skill := range body.GetSkills() {
		value.Skills = append(value.Skills, rpg.AdminProfessionSkill{Code: skill.GetCode(), Name: skill.GetName(), RequiredLevel: skill.GetRequiredLevel(), Description: skill.GetDescription(), Enabled: skill.GetEnabled()})
	}
	saved, err := s.store.SaveProfession(ctx, rpg.SaveProfessionCommand{Write: write, Value: value, ExpectedVersion: request.GetExpectedVersion()})
	if err != nil {
		return nil, adminError(err)
	}
	return &rpgv1.SaveProfessionResponse{Profession: professionMessage(saved)}, nil
}

func professionMessage(value rpg.AdminProfession) *rpgv1.AdminProfession {
	message := &rpgv1.AdminProfession{Id: value.ID.String(), Code: value.Code, Name: value.Name, Description: value.Description, MaximumLevel: value.MaximumLevel, Enabled: value.Enabled, Version: value.Version, Skills: make([]*rpgv1.AdminProfessionSkill, 0, len(value.Skills))}
	for _, skill := range value.Skills {
		message.Skills = append(message.Skills, &rpgv1.AdminProfessionSkill{Id: skill.ID.String(), Code: skill.Code, Name: skill.Name, RequiredLevel: skill.RequiredLevel, Description: skill.Description, Enabled: skill.Enabled})
	}
	return message
}

func idString(id interface {
	IsValid() bool
	String() string
}) string {
	if !id.IsValid() {
		return ""
	}
	return id.String()
}
