package rpg

import (
	"sort"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// RegionNode 是拓扑校验所需的最小 Region 资料。
type RegionNode struct {
	ID      snowflake.ID
	Code    string
	Enabled bool
}

// LocationNode 是拓扑校验所需的最小 Location 资料。
type LocationNode struct {
	ID, RegionID, ParentID snowflake.ID
	Code                   string
	Enabled, DefaultSpawn  bool
}

// ExitNode 是拓扑校验所需的最小有向出口资料。
type ExitNode struct {
	ID, SourceID, TargetID snowflake.ID
	Code                   string
	Enabled                bool
}

// IntegrityIssue 是稳定 reason code、资源身份和中文说明。
type IntegrityIssue struct {
	ReasonCode string
	ResourceID snowflake.ID
	Message    string
}

// IntegrityReport 是一次纯拓扑校验结果。
type IntegrityReport struct {
	Passed bool
	Issues []IntegrityIssue
}

// ValidateTopology 校验层级、出口引用和从出生点可达性；发现任一错误即失败。
func ValidateTopology(regions []RegionNode, locations []LocationNode, exits []ExitNode) IntegrityReport {
	issues := make([]IntegrityIssue, 0)
	regionByID := make(map[snowflake.ID]RegionNode, len(regions))
	for _, region := range regions {
		if _, exists := regionByID[region.ID]; exists {
			issues = append(issues, IntegrityIssue{"duplicate_region_id", region.ID, "Region Identifier 重复。"})
		}
		regionByID[region.ID] = region
	}
	locationByID := make(map[snowflake.ID]LocationNode, len(locations))
	codeOwner := make(map[string]snowflake.ID, len(locations))
	for _, location := range locations {
		if _, exists := locationByID[location.ID]; exists {
			issues = append(issues, IntegrityIssue{"duplicate_location_id", location.ID, "Location Identifier 重复。"})
		}
		locationByID[location.ID] = location
		if owner, exists := codeOwner[location.Code]; exists && owner != location.ID {
			issues = append(issues, IntegrityIssue{"duplicate_location_code", location.ID, "Location Stable Code 重复。"})
		} else {
			codeOwner[location.Code] = location.ID
		}
		if _, exists := regionByID[location.RegionID]; !exists {
			issues = append(issues, IntegrityIssue{"location_region_missing", location.ID, "Location 引用的 Region 不存在。"})
		}
		if location.ParentID != snowflake.ID(0) {
			parent, exists := locationByID[location.ParentID]
			if exists && parent.RegionID != location.RegionID {
				issues = append(issues, IntegrityIssue{"location_parent_cross_region", location.ID, "Location 父子节点必须属于同一 Region。"})
			}
			if location.ParentID == location.ID {
				issues = append(issues, IntegrityIssue{"location_parent_self", location.ID, "Location 不能将自身作为父节点。"})
			}
		}
	}
	// 二次检查父节点存在及任意长度层级环。
	for _, location := range locations {
		seen := map[snowflake.ID]bool{}
		for current := location; current.ParentID != snowflake.ID(0); {
			if seen[current.ID] {
				issues = append(issues, IntegrityIssue{"location_parent_cycle", location.ID, "Location 父级关系存在环。"})
				break
			}
			seen[current.ID] = true
			parent, exists := locationByID[current.ParentID]
			if !exists {
				issues = append(issues, IntegrityIssue{"location_parent_missing", location.ID, "Location 父节点不存在。"})
				break
			}
			current = parent
		}
	}
	for _, exit := range exits {
		if exit.SourceID == exit.TargetID {
			issues = append(issues, IntegrityIssue{"exit_self_loop", exit.ID, "Location Exit 不能连接自身。"})
		}
		source, sourceOK := locationByID[exit.SourceID]
		target, targetOK := locationByID[exit.TargetID]
		if !sourceOK {
			issues = append(issues, IntegrityIssue{"exit_source_missing", exit.ID, "Location Exit 的来源不存在。"})
		}
		if !targetOK {
			issues = append(issues, IntegrityIssue{"exit_target_missing", exit.ID, "Location Exit 的目标不存在。"})
		}
		if exit.Enabled && targetOK && (!target.Enabled || (sourceOK && !source.Enabled)) {
			issues = append(issues, IntegrityIssue{"exit_references_disabled_location", exit.ID, "启用出口不能引用停用地点。"})
		}
	}
	// 只对启用的地点要求从启用出生点可达。
	starts := make([]snowflake.ID, 0)
	for _, location := range locations {
		if location.Enabled && location.DefaultSpawn {
			starts = append(starts, location.ID)
		}
	}
	if len(starts) == 0 && len(locations) > 0 {
		issues = append(issues, IntegrityIssue{"default_spawn_missing", snowflake.ID(0), "不存在启用的默认出生地点。"})
	}
	adj := make(map[snowflake.ID][]snowflake.ID)
	for _, exit := range exits {
		if exit.Enabled {
			adj[exit.SourceID] = append(adj[exit.SourceID], exit.TargetID)
		}
	}
	reached := make(map[snowflake.ID]bool)
	queue := append([]snowflake.ID(nil), starts...)
	for len(queue) > 0 {
		id := queue[0]
		queue = queue[1:]
		if reached[id] {
			continue
		}
		reached[id] = true
		queue = append(queue, adj[id]...)
	}
	for _, location := range locations {
		if location.Enabled && !reached[location.ID] {
			issues = append(issues, IntegrityIssue{"enabled_location_unreachable", location.ID, "启用地点不能从默认出生地点到达。"})
		}
	}
	sort.SliceStable(issues, func(i, j int) bool {
		if issues[i].ReasonCode != issues[j].ReasonCode {
			return issues[i].ReasonCode < issues[j].ReasonCode
		}
		return issues[i].ResourceID < issues[j].ResourceID
	})
	return IntegrityReport{Passed: len(issues) == 0, Issues: issues}
}
