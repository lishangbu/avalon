package rpg

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/ent/rpglocation"
	"github.com/lishangbu/avalon/ent/rpglocationexit"
	"github.com/lishangbu/avalon/ent/rpgregion"
	"github.com/lishangbu/avalon/ent/rpgtopologyintegrityreport"
)

func boundedPageSize(size, maximum int) int {
	if size < 1 {
		return 50
	}
	if size > maximum {
		return maximum
	}
	return size
}

// ListRegions 读取完整 Region 资料，管理员 RPC 不裁剪 Discovery。
func (store *EntWorldStore) ListRegions(ctx context.Context, pageSize int) ([]AdminRegion, error) {
	rows, err := store.pool.Client(ctx).RpgRegion.Query().Order(rpgregion.ByCode(), rpgregion.ByID()).Limit(boundedPageSize(pageSize, 200)).All(ctx)
	if err != nil {
		return nil, fmt.Errorf("查询 Region: %w", err)
	}
	result := make([]AdminRegion, 0, len(rows))
	for _, row := range rows {
		description := ""
		if row.Description != nil {
			description = *row.Description
		}
		result = append(result, AdminRegion{ID: row.ID, Code: row.Code, Name: row.Name, Description: description, Enabled: row.Enabled, Version: row.Version})
	}
	return result, nil
}

// ListLocations 读取完整 Location 拓扑。
func (store *EntWorldStore) ListLocations(ctx context.Context, pageSize int) ([]AdminLocation, error) {
	rows, err := store.pool.Client(ctx).RpgLocation.Query().Order(rpglocation.ByCode(), rpglocation.ByID()).Limit(boundedPageSize(pageSize, 200)).All(ctx)
	if err != nil {
		return nil, fmt.Errorf("查询 Location: %w", err)
	}
	result := make([]AdminLocation, 0, len(rows))
	for _, row := range rows {
		parentID := snowflake.ID(0)
		if row.ParentID != nil {
			parentID = *row.ParentID
		}
		result = append(result, AdminLocation{ID: row.ID, RegionID: row.RegionID, ParentID: parentID, Code: row.Code, Name: row.Name, LocationType: row.LocationType, Enabled: row.Enabled, DefaultSpawn: row.DefaultSpawn, Version: row.Version})
	}
	return result, nil
}

// ListExits 读取完整有向出口及规范化规则 JSON。
func (store *EntWorldStore) ListExits(ctx context.Context, pageSize int) ([]AdminExit, error) {
	rows, err := store.pool.Client(ctx).RpgLocationExit.Query().Order(rpglocationexit.BySourceLocationID(), rpglocationexit.BySortOrder(), rpglocationexit.ByID()).Limit(boundedPageSize(pageSize, 200)).All(ctx)
	if err != nil {
		return nil, fmt.Errorf("查询 Location Exit: %w", err)
	}
	result := make([]AdminExit, 0, len(rows))
	for _, row := range rows {
		condition, _ := json.Marshal(row.Condition)
		effect, _ := json.Marshal(row.Effect)
		result = append(result, AdminExit{ID: row.ID, SourceLocationID: row.SourceLocationID, TargetLocationID: row.TargetLocationID, Code: row.Code, Name: row.Name, ConditionJSON: string(condition), EffectJSON: string(effect), Enabled: row.Enabled, Version: row.Version})
	}
	return result, nil
}

// ListIntegrityReports 读取最近的不可变拓扑报告及问题明细。
func (store *EntWorldStore) ListIntegrityReports(ctx context.Context, pageSize int) ([]AdminIntegrityReport, error) {
	rows, err := store.pool.Client(ctx).RpgTopologyIntegrityReport.Query().WithIssues().Order(rpgtopologyintegrityreport.ByCheckedAt(), rpgtopologyintegrityreport.ByID()).Limit(boundedPageSize(pageSize, 100)).All(ctx)
	if err != nil {
		return nil, fmt.Errorf("查询拓扑完整性报告: %w", err)
	}
	result := make([]AdminIntegrityReport, 0, len(rows))
	for _, row := range rows {
		report := AdminIntegrityReport{ID: row.ID, CheckedAt: row.CheckedAt.UTC(), Passed: row.State == "passed", Issues: make([]AdminIntegrityIssue, 0, len(row.Edges.Issues))}
		for _, issue := range row.Edges.Issues {
			resourceID := ""
			if issue.ResourceID != nil {
				resourceID = issue.ResourceID.String()
			}
			report.Issues = append(report.Issues, AdminIntegrityIssue{ReasonCode: issue.ReasonCode, ResourceID: resourceID, Message: issue.Message})
		}
		result = append(result, report)
	}
	return result, nil
}
