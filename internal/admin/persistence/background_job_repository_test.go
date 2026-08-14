package persistence

import (
	"strings"
	"testing"
)

// TestSanitizedTextRemovesUnsafeControlCharacters 验证后台任务摘要不会把控制字符带入管理响应。
func TestSanitizedTextRemovesUnsafeControlCharacters(t *testing.T) {
	t.Parallel()

	message := sanitizedText("数据库连接\x00暂时不可用", maximumFailureReasonLength)
	if message != "数据库连接暂时不可用" {
		t.Fatalf("sanitizedText() = %q，期望移除不安全控制字符", message)
	}
}

// TestSanitizedTextBoundsReturnedText 验证不可信 Worker 错误不能无限膨胀管理响应。
func TestSanitizedTextBoundsReturnedText(t *testing.T) {
	t.Parallel()

	message := sanitizedText(strings.Repeat("错", maximumFailureReasonLength+1), maximumFailureReasonLength)
	if got := len([]rune(message)); got != maximumFailureReasonLength {
		t.Fatalf("sanitizedText() 字符数 = %d，期望 %d", got, maximumFailureReasonLength)
	}
}
