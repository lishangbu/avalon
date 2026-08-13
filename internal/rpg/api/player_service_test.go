package api

import (
	"context"
	"testing"
	"time"

	rpgv1 "github.com/lishangbu/avalon/api/gen/go/avalon/rpg/v1"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"github.com/lishangbu/avalon/internal/rpg"
	"github.com/lishangbu/avalon/internal/security/authentication"
)

type worldStub struct {
	mapValue      rpg.WorldMap
	traverseValue rpg.TraversalResult
	pendingValue  *rpg.PendingEncounter
	resolveValue  rpg.PendingEncounter
}

func (stub *worldStub) GetMap(context.Context, snowflake.ID) (rpg.WorldMap, error) {
	return stub.mapValue, nil
}
func (stub *worldStub) Traverse(context.Context, rpg.TraversalCommand) (rpg.TraversalResult, error) {
	return stub.traverseValue, nil
}
func (stub *worldStub) GetPendingEncounter(context.Context, snowflake.ID, time.Time) (*rpg.PendingEncounter, error) {
	return stub.pendingValue, nil
}
func (stub *worldStub) ResolvePendingEncounter(context.Context, rpg.ResolveEncounterCommand) (rpg.PendingEncounter, error) {
	return stub.resolveValue, nil
}
func (*worldStub) GetCheckpoint(context.Context, snowflake.ID) (*rpg.Checkpoint, error) {
	return nil, nil
}
func (*worldStub) SetCheckpoint(context.Context, rpg.SetCheckpointCommand) (rpg.Checkpoint, error) {
	return rpg.Checkpoint{}, nil
}
func (*worldStub) GetParty(context.Context, snowflake.ID) (rpg.Party, error) { return rpg.Party{}, nil }
func (*worldStub) ReplaceParty(context.Context, rpg.ReplacePartyCommand) (rpg.Party, error) {
	return rpg.Party{}, nil
}

func TestGetMapReturnsPositionAndDiscoverySubgraph(t *testing.T) {
	accountID := snowflake.MustParse("1048576199")
	locationID := snowflake.MustParse("1048576200")
	stub := &worldStub{mapValue: rpg.WorldMap{Position: rpg.Position{LocationID: locationID, Version: 1}, Locations: []rpg.WorldLocation{{ID: locationID, RegionID: accountID, Code: "spawn", Name: "出生点"}}}}
	service := NewPlayerService(stub, time.Now)
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})
	response, err := service.GetMap(ctx, &rpgv1.GetMapRequest{PageSize: 20})
	if err != nil {
		t.Fatal(err)
	}
	if response.GetPosition().GetLocationId() != locationID.String() || len(response.GetLocations()) != 1 {
		t.Fatalf("unexpected map response: %v", response)
	}
}

func TestResolvePendingEncounterReturnsCreatedBattleID(t *testing.T) {
	accountID := snowflake.MustParse("1048576199")
	encounterID := snowflake.MustParse("1048576201")
	entryID := snowflake.MustParse("1048576202")
	battleID := snowflake.MustParse("1048576203")
	stub := &worldStub{resolveValue: rpg.PendingEncounter{
		ID: encounterID, EncounterEntryID: entryID, BattleID: battleID,
		State: "accepted", ExpiresAt: time.Date(2026, time.August, 11, 12, 30, 0, 0, time.UTC),
	}}
	service := NewPlayerService(stub, time.Now)
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})
	response, err := service.ResolvePendingEncounter(ctx, &rpgv1.ResolvePendingEncounterRequest{
		PendingEncounterId: encounterID.String(),
		Resolution:         rpgv1.PendingEncounterResolution_PENDING_ENCOUNTER_RESOLUTION_ACCEPT,
		IdempotencyKey:     "accept-encounter-1",
	})
	if err != nil {
		t.Fatal(err)
	}
	if response.GetPendingEncounter().GetBattleId() != battleID.String() {
		t.Fatalf("battle_id = %q, want %q", response.GetPendingEncounter().GetBattleId(), battleID)
	}
}
