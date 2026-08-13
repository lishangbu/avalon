package worker

import (
	"encoding/json"
	"strings"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// TestTaskPayloadUsesStringSnowflakeIdentifier 验证 Outbox 与 Asynq 之间的任务 Identifier
// 始终使用十进制 JSON 字符串，且拒绝数值和旧 UUID 形状。
func TestTaskPayloadUsesStringSnowflakeIdentifier(t *testing.T) {
	t.Parallel()

	jobID := snowflake.MustParse("1048576213")
	payload, err := json.Marshal(taskPayload{JobID: jobID})
	if err != nil {
		t.Fatalf("json.Marshal() error = %v", err)
	}
	if got, want := string(payload), `{"jobId":"1048576213"}`; got != want {
		t.Fatalf("任务载荷 = %s，期望 %s", got, want)
	}
	for _, invalid := range []string{`{"jobId":1048576213}`, `{"jobId":"019fbfc5-3400-79f7-9deb-725f066b35e8"}`} {
		var decoded taskPayload
		if err := json.Unmarshal([]byte(invalid), &decoded); err == nil {
			t.Fatalf("非规范任务载荷 %s 不应通过解码", invalid)
		}
	}
}

// TestDueOccurrencesAppliesMissedRunPolicies 验证动态调度对错过周期分别执行跳过、合并和追赶，
// 同时始终把下一次触发时间推进到当前时间之后。
func TestDueOccurrencesAppliesMissedRunPolicies(t *testing.T) {
	start := time.Date(2026, time.August, 6, 0, 0, 0, 0, time.UTC)
	now := start.Add(35 * time.Second)
	for _, testCase := range []struct {
		name       string
		policy     string
		wantCount  int
		wantFirst  time.Time
		wantNextAt time.Time
	}{
		{name: "跳过", policy: "skip", wantCount: 0, wantNextAt: start.Add(40 * time.Second)},
		{name: "合并", policy: "coalesce", wantCount: 1, wantFirst: start.Add(30 * time.Second), wantNextAt: start.Add(40 * time.Second)},
		{name: "追赶", policy: "catch_up", wantCount: 4, wantFirst: start, wantNextAt: start.Add(40 * time.Second)},
	} {
		t.Run(testCase.name, func(t *testing.T) {
			occurrences, nextAt, err := dueOccurrences(scheduleRow{
				scheduleKind: "interval", intervalSeconds: int32Pointer(10),
				missedPolicy: testCase.policy, nextRunAt: start,
			}, now)
			if err != nil {
				t.Fatalf("dueOccurrences() error = %v", err)
			}
			if len(occurrences) != testCase.wantCount {
				t.Fatalf("触发次数 = %d，期望 %d", len(occurrences), testCase.wantCount)
			}
			if testCase.wantCount > 0 && !occurrences[0].Equal(testCase.wantFirst) {
				t.Fatalf("首次触发时间 = %v，期望 %v", occurrences[0], testCase.wantFirst)
			}
			if !nextAt.Equal(testCase.wantNextAt) {
				t.Fatalf("下次触发时间 = %v，期望 %v", nextAt, testCase.wantNextAt)
			}
		})
	}
}

// TestDueOccurrencesRejectsInvalidSchedule 验证无表达式和未知错过周期策略不会静默产生任务。
func TestDueOccurrencesRejectsInvalidSchedule(t *testing.T) {
	now := time.Date(2026, time.August, 6, 0, 0, 0, 0, time.UTC)
	if _, _, err := dueOccurrences(scheduleRow{scheduleKind: "interval", missedPolicy: "catch_up", nextRunAt: now}, now); err == nil {
		t.Fatal("缺少固定间隔时 dueOccurrences() 应返回错误")
	}
	if _, _, err := dueOccurrences(scheduleRow{
		scheduleKind: "interval", intervalSeconds: int32Pointer(10),
		missedPolicy: "unknown", nextRunAt: now,
	}, now); err == nil {
		t.Fatal("未知错过周期策略时 dueOccurrences() 应返回错误")
	}
}

func int32Pointer(value int32) *int32 { return &value }

// TestBoundedExponentialBackoffCapsDelay 验证任务和 Outbox 的退避从一秒开始并受最大值约束。
func TestBoundedExponentialBackoffCapsDelay(t *testing.T) {
	if got := boundedExponentialBackoff(1, 15*time.Minute); got != time.Second {
		t.Fatalf("首次退避 = %v，期望 1s", got)
	}
	if got := boundedExponentialBackoff(30, 15*time.Minute); got != 15*time.Minute {
		t.Fatalf("封顶退避 = %v，期望 15m", got)
	}
}

// TestSanitizedTextRemovesControlCharactersAndLimitsRunes 验证持久化错误摘要不会保留不可见控制字符，
// 并按 Unicode 字符而不是 UTF-8 字节安全截断。
func TestSanitizedTextRemovesControlCharactersAndLimitsRunes(t *testing.T) {
	got := sanitizedText("  错\x00误\n详情  ", 3)
	if got != "错误\n" {
		t.Fatalf("sanitizedText() = %q，期望 %q", got, "错误\n")
	}
	if strings.ContainsRune(got, '\x00') {
		t.Fatal("sanitizedText() 保留了 NUL 控制字符")
	}
}
