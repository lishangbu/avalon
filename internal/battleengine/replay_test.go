package battleengine_test

import (
	"errors"
	"os"
	"path/filepath"
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestReplayGoldenReproducesRecordedTurn 验证黄金样本能够在不读取系统随机数或时钟的前提下，
// 重现逐回合结构化事件和可审计状态摘要。
func TestReplayGoldenReproducesRecordedTurn(t *testing.T) {
	t.Parallel()

	replay, err := battleengine.LoadGoldenReplay(filepath.Join("testdata", "golden", "major-status-turn.v1.json"))
	if err != nil {
		t.Fatalf("LoadGoldenReplay() error = %v", err)
	}
	result, err := battleengine.ReplayGolden(replay)
	if err != nil {
		t.Fatalf("ReplayGolden() error = %v", err)
	}
	if result.ReplayedTurns != 1 || result.FinalState.TurnNumber != 1 || len(result.FinalState.Members) != 2 {
		t.Fatalf("ReplayGolden() = %+v, want one replayed turn with two members", result)
	}
	for _, member := range result.FinalState.Members {
		if member.CurrentHP != 94 || member.MajorStatus != battleengine.MajorStatusBurn || member.RemainingPP[0] != 9 {
			t.Fatalf("final member summary = %+v, want burn, 94 HP and 9 PP", member)
		}
	}
}

// TestReplayGoldenSuiteReplaysEveryApprovedScenario 验证目录中的每个已批准黄金样本
// 都会被严格加载和完整重放，避免新增样本后仍由单文件测试静默漏检。
func TestReplayGoldenSuiteReplaysEveryApprovedScenario(t *testing.T) {
	t.Parallel()

	result, err := battleengine.ReplayGoldenSuite(filepath.Join("testdata", "golden"))
	if err != nil {
		t.Fatalf("ReplayGoldenSuite() error = %v", err)
	}
	if result.ReplayedSamples < 1 || result.ReplayedTurns < result.ReplayedSamples {
		t.Fatalf("ReplayGoldenSuite() = %+v，期望每个样本至少包含一个已重放回合", result)
	}
}

// TestLoadGoldenReplayPreservesProvenance 验证黄金样本明确记录批准用例与场景标识，
// 使回放失败能够定位到稳定事实来源。
func TestLoadGoldenReplayPreservesProvenance(t *testing.T) {
	t.Parallel()

	replay, err := battleengine.LoadGoldenReplay(filepath.Join("testdata", "golden", "major-status-turn.v1.json"))
	if err != nil {
		t.Fatalf("LoadGoldenReplay() error = %v", err)
	}
	if replay.Provenance.CaseID != "major-status-burn-turn" ||
		replay.Provenance.Description != "状态技能施加灼伤并在回合末造成持续伤害" ||
		replay.Provenance.Scenario != "status-skill-applies-burn-and-end-turn-residual-damage" {
		t.Fatalf("黄金样本来源元数据 = %+v", replay.Provenance)
	}
}

// TestLoadGoldenReplayRejectsTrailingJSON 验证样本文件只能包含一个完整 JSON 文档，
// 防止意外拼接的第二份场景在 CI 中被忽略。
func TestLoadGoldenReplayRejectsTrailingJSON(t *testing.T) {
	t.Parallel()

	original, err := os.ReadFile(filepath.Join("testdata", "golden", "major-status-turn.v1.json"))
	if err != nil {
		t.Fatalf("ReadFile() error = %v", err)
	}
	path := filepath.Join(t.TempDir(), "concatenated.json")
	if err := os.WriteFile(path, append(original, []byte("\n{}\n")...), 0o600); err != nil {
		t.Fatalf("WriteFile() error = %v", err)
	}
	if _, loadErr := battleengine.LoadGoldenReplay(path); !errors.Is(loadErr, battleengine.ErrInvalidGoldenReplay) {
		t.Fatalf("LoadGoldenReplay() error = %v, want ErrInvalidGoldenReplay", loadErr)
	}
}

// TestReplayGoldenRejectsUnexpectedRecordedState 验证黄金样本不能通过只重放命令而忽略状态差异。
func TestReplayGoldenRejectsUnexpectedRecordedState(t *testing.T) {
	t.Parallel()

	replay, err := battleengine.LoadGoldenReplay(filepath.Join("testdata", "golden", "major-status-turn.v1.json"))
	if err != nil {
		t.Fatalf("LoadGoldenReplay() error = %v", err)
	}
	replay.Turns[0].ExpectedState.Members[0].CurrentHP = 95
	if _, replayErr := battleengine.ReplayGolden(replay); !errors.Is(replayErr, battleengine.ErrGoldenReplayDiverged) {
		t.Fatalf("ReplayGolden() error = %v, want ErrGoldenReplayDiverged", replayErr)
	}
}
