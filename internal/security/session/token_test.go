package session_test

import (
	"bytes"
	"encoding/hex"
	"testing"

	"github.com/lishangbu/avalon/internal/security/session"
)

// TestTokenIssuerStoresOnlyDomainSeparatedSHA256Digest 验证 256 位随机明文只在签发时返回，数据库摘要
// 与独立已知向量一致，且不会持久化令牌明文。
func TestTokenIssuerStoresOnlyDomainSeparatedSHA256Digest(t *testing.T) {
	t.Parallel()

	issuer := session.NewTokenIssuer(session.TokenPurposeSession, bytes.NewReader(bytes.Repeat([]byte{0x42}, 32)))
	issued, err := issuer.Issue()
	if err != nil {
		t.Fatalf("Issue() error = %v", err)
	}
	if issued.Plaintext == "" {
		t.Fatal("Plaintext is empty")
	}
	if bytes.Contains(issued.Digest, []byte(issued.Plaintext)) {
		t.Fatal("Digest contains plaintext token")
	}
	wantDigest, err := hex.DecodeString("e9d524fce17296f218e5a2a803d7118f9bb295a0b1deec7544311e837027a74f")
	if err != nil {
		t.Fatalf("解析已知摘要失败：%v", err)
	}
	if !bytes.Equal(issued.Digest, wantDigest) {
		t.Fatalf("Digest = %x，期望 %x", issued.Digest, wantDigest)
	}
	if !issuer.Verify(issued.Plaintext, issued.Digest) {
		t.Error("Verify(issued token) = false")
	}
	if issuer.Verify("different-token", issued.Digest) {
		t.Error("Verify(different token) = true")
	}
}
