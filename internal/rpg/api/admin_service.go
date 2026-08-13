package api

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/lishangbu/avalon/internal/platform/snowflake"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	rpgv1 "github.com/lishangbu/avalon/api/gen/go/avalon/rpg/v1"
	"github.com/lishangbu/avalon/internal/rpg"
	"google.golang.org/protobuf/types/known/timestamppb"
)

// AdminWorldService 是管理端只读地图 RPC 适配器。
type AdminWorldService struct{ store rpg.AdminWorldStore }

// NewAdminWorldService 创建管理端只读地图服务。
func NewAdminWorldService(store rpg.AdminWorldStore) *AdminWorldService {
	return &AdminWorldService{store: store}
}

// ListRegions 分页返回完整 Region。
func (service *AdminWorldService) ListRegions(ctx context.Context, request *rpgv1.ListRegionsRequest) (*rpgv1.ListRegionsResponse, error) {
	rows, err := service.store.ListRegions(ctx, int(request.GetPageSize()))
	if err != nil {
		return nil, adminError(err)
	}
	response := &rpgv1.ListRegionsResponse{Regions: make([]*rpgv1.Region, 0, len(rows))}
	for _, row := range rows {
		response.Regions = append(response.Regions, &rpgv1.Region{Id: row.ID.String(), Code: row.Code, Name: row.Name, Description: row.Description, Enabled: row.Enabled, Version: row.Version})
	}
	return response, nil
}

// ListLocations 分页返回完整 Location。
func (service *AdminWorldService) ListLocations(ctx context.Context, request *rpgv1.ListLocationsRequest) (*rpgv1.ListLocationsResponse, error) {
	rows, err := service.store.ListLocations(ctx, int(request.GetPageSize()))
	if err != nil {
		return nil, adminError(err)
	}
	response := &rpgv1.ListLocationsResponse{Locations: make([]*rpgv1.AdminLocation, 0, len(rows))}
	for _, row := range rows {
		parent := ""
		if row.ParentID != snowflake.ID(0) {
			parent = row.ParentID.String()
		}
		response.Locations = append(response.Locations, &rpgv1.AdminLocation{Id: row.ID.String(), RegionId: row.RegionID.String(), ParentId: parent, Code: row.Code, Name: row.Name, LocationType: row.LocationType, Enabled: row.Enabled, Version: row.Version, DefaultSpawn: row.DefaultSpawn})
	}
	return response, nil
}

// ListLocationExits 分页返回完整有向出口。
func (service *AdminWorldService) ListLocationExits(ctx context.Context, request *rpgv1.ListLocationExitsRequest) (*rpgv1.ListLocationExitsResponse, error) {
	rows, err := service.store.ListExits(ctx, int(request.GetPageSize()))
	if err != nil {
		return nil, adminError(err)
	}
	response := &rpgv1.ListLocationExitsResponse{Exits: make([]*rpgv1.AdminLocationExit, 0, len(rows))}
	for _, row := range rows {
		condition, conditionErr := conditionMessage([]byte(row.ConditionJSON))
		if conditionErr != nil {
			return nil, adminError(conditionErr)
		}
		effect, effectErr := effectMessage([]byte(row.EffectJSON))
		if effectErr != nil {
			return nil, adminError(effectErr)
		}
		response.Exits = append(response.Exits, &rpgv1.AdminLocationExit{Id: row.ID.String(), SourceLocationId: row.SourceLocationID.String(), TargetLocationId: row.TargetLocationID.String(), Code: row.Code, Name: row.Name, Condition: condition, Effect: effect, Enabled: row.Enabled, Version: row.Version})
	}
	return response, nil
}

// ListIntegrityReports 分页返回拓扑校验报告。
func (service *AdminWorldService) ListIntegrityReports(ctx context.Context, request *rpgv1.ListIntegrityReportsRequest) (*rpgv1.ListIntegrityReportsResponse, error) {
	rows, err := service.store.ListIntegrityReports(ctx, int(request.GetPageSize()))
	if err != nil {
		return nil, adminError(err)
	}
	response := &rpgv1.ListIntegrityReportsResponse{Reports: make([]*rpgv1.IntegrityReport, 0, len(rows))}
	for _, row := range rows {
		report := &rpgv1.IntegrityReport{Id: row.ID.String(), CheckedAt: timestamppb.New(row.CheckedAt), Passed: row.Passed, Issues: make([]*rpgv1.IntegrityIssue, 0, len(row.Issues))}
		for _, issue := range row.Issues {
			report.Issues = append(report.Issues, &rpgv1.IntegrityIssue{ReasonCode: issue.ReasonCode, ResourceId: issue.ResourceID, Message: issue.Message})
		}
		response.Reports = append(response.Reports, report)
	}
	return response, nil
}

func adminError(_ error) error {
	return kratoserrors.InternalServer("RPG_ADMIN_WORLD_FAILED", "地图资料读取失败")
}

type conditionJSON struct {
	Op       string            `json:"op"`
	Children []json.RawMessage `json:"children"`
	Child    json.RawMessage   `json:"child"`
	Key      string            `json:"key"`
	Value    int32             `json:"value"`
}

func conditionMessage(raw []byte) (*rpgv1.ExitCondition, error) {
	var value conditionJSON
	if err := json.Unmarshal(raw, &value); err != nil {
		return nil, err
	}
	message := &rpgv1.ExitCondition{}
	switch value.Op {
	case "all":
		node := &rpgv1.ConditionAll{}
		for _, child := range value.Children {
			item, err := conditionMessage(child)
			if err != nil {
				return nil, err
			}
			node.Children = append(node.Children, item)
		}
		message.Node = &rpgv1.ExitCondition_All{All: node}
	case "any":
		node := &rpgv1.ConditionAny{}
		for _, child := range value.Children {
			item, err := conditionMessage(child)
			if err != nil {
				return nil, err
			}
			node.Children = append(node.Children, item)
		}
		message.Node = &rpgv1.ExitCondition_Any{Any: node}
	case "not":
		child, err := conditionMessage(value.Child)
		if err != nil {
			return nil, err
		}
		message.Node = &rpgv1.ExitCondition_Not{Not: &rpgv1.ConditionNot{Child: child}}
	case "level_gte":
		message.Node = &rpgv1.ExitCondition_LevelAtLeast{LevelAtLeast: &rpgv1.LevelAtLeast{Value: value.Value}}
	case "item_count_gte":
		message.Node = &rpgv1.ExitCondition_ItemCountAtLeast{ItemCountAtLeast: &rpgv1.ItemCountAtLeast{ItemCode: value.Key, Value: value.Value}}
	case "quest_objective_gte":
		message.Node = &rpgv1.ExitCondition_QuestObjectiveAtLeast{QuestObjectiveAtLeast: &rpgv1.QuestObjectiveAtLeast{ObjectiveCode: value.Key, Value: value.Value}}
	case "profession":
		message.Node = &rpgv1.ExitCondition_HasProfession{HasProfession: &rpgv1.HasProfession{ProfessionCode: value.Key}}
	case "world_state":
		var boolean struct {
			Value bool `json:"value"`
		}
		if err := json.Unmarshal(raw, &boolean); err != nil {
			return nil, err
		}
		message.Node = &rpgv1.ExitCondition_WorldStateEquals{WorldStateEquals: &rpgv1.WorldStateEquals{Key: value.Key, Value: boolean.Value}}
	default:
		return nil, fmt.Errorf("未知 Exit Condition 操作 %q", value.Op)
	}
	return message, nil
}

func effectMessage(raw []byte) (*rpgv1.TraversalEffect, error) {
	if len(raw) == 0 || string(raw) == "null" {
		return nil, nil
	}
	var value struct {
		Op    string `json:"op"`
		Key   string `json:"key"`
		Value int32  `json:"value"`
	}
	if err := json.Unmarshal(raw, &value); err != nil {
		return nil, err
	}
	message := &rpgv1.TraversalEffect{}
	switch value.Op {
	case "set_world_state":
		var boolean struct {
			Value bool `json:"value"`
		}
		if err := json.Unmarshal(raw, &boolean); err != nil {
			return nil, err
		}
		message.Effect = &rpgv1.TraversalEffect_SetWorldState{SetWorldState: &rpgv1.SetWorldState{Key: value.Key, Value: boolean.Value}}
	case "increment_quest_objective":
		message.Effect = &rpgv1.TraversalEffect_IncrementQuestObjective{IncrementQuestObjective: &rpgv1.IncrementQuestObjective{ObjectiveCode: value.Key, Value: value.Value}}
	default:
		return nil, fmt.Errorf("未知 Traversal Effect 操作 %q", value.Op)
	}
	return message, nil
}
