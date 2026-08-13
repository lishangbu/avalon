package rpg

import (
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

func TestValidateTopologyDetectsCyclesAndUnreachableLocations(t *testing.T) {
	report := ValidateTopology(
		[]RegionNode{{ID: snowflake.MustParse("1048576101"), Code: "main", Enabled: true}},
		[]LocationNode{{ID: snowflake.MustParse("1048576102"), RegionID: snowflake.MustParse("1048576101"), ParentID: snowflake.MustParse("1048576103"), Code: "a", Enabled: true, DefaultSpawn: true}, {ID: snowflake.MustParse("1048576103"), RegionID: snowflake.MustParse("1048576101"), ParentID: snowflake.MustParse("1048576102"), Code: "b", Enabled: true}},
		[]ExitNode{{ID: snowflake.MustParse("1048576104"), Code: "e", SourceID: snowflake.MustParse("1048576102"), TargetID: snowflake.MustParse("1048576102"), Enabled: true}},
	)
	if report.Passed || len(report.Issues) == 0 {
		t.Fatal("expected invalid topology")
	}
}

func TestValidateTopologyAcceptsReachableGraph(t *testing.T) {
	report := ValidateTopology(
		[]RegionNode{{ID: snowflake.MustParse("1048576101"), Code: "main", Enabled: true}},
		[]LocationNode{{ID: snowflake.MustParse("1048576102"), RegionID: snowflake.MustParse("1048576101"), Code: "a", Enabled: true, DefaultSpawn: true}, {ID: snowflake.MustParse("1048576103"), RegionID: snowflake.MustParse("1048576101"), ParentID: snowflake.MustParse("1048576102"), Code: "b", Enabled: true}},
		[]ExitNode{{ID: snowflake.MustParse("1048576104"), Code: "e", SourceID: snowflake.MustParse("1048576102"), TargetID: snowflake.MustParse("1048576103"), Enabled: true}},
	)
	if !report.Passed {
		t.Fatalf("unexpected issues: %+v", report.Issues)
	}
}
