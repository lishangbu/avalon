// Package session 定义可撤销认证会话使用的令牌原语。
package session

import (
	"crypto/sha256"
	"crypto/subtle"
	"encoding/base64"
	"fmt"
	"io"
)

const tokenLength = 32

// TokenPurpose 是加入摘要输入的稳定领域分隔符，防止不同凭据类型共享摘要空间。
type TokenPurpose string

const (
	// TokenPurposeSession 标识 Bearer 会话凭据。
	TokenPurposeSession TokenPurpose = "session"
)

// IssuedToken 包含仅在签发时返回的令牌明文及其不可逆持久化摘要。
type IssuedToken struct {
	// Plaintext 是 Base64URL 编码的 256 位随机凭据，只能返回给当前登录或轮换请求。
	Plaintext string
	// Digest 是加入用途前缀后计算的 SHA-256，数据库只保存该字段。
	Digest []byte
}

// TokenIssuer 使用密码学安全随机源签发高熵 opaque token，并计算领域隔离的 SHA-256 摘要。
type TokenIssuer struct {
	// purpose 隔离不同会话域的摘要空间，不是秘密配置。
	purpose TokenPurpose
	// random 提供签发每个 Token 所需的 32 字节密码学随机数据。
	random io.Reader
}

// NewTokenIssuer 创建使用指定领域用途和安全随机源的令牌签发器。
func NewTokenIssuer(purpose TokenPurpose, random io.Reader) *TokenIssuer {
	return &TokenIssuer{purpose: purpose, random: random}
}

// Issue 生成 256 位随机令牌，并返回明文与可持久化的领域隔离 SHA-256 摘要。
func (i *TokenIssuer) Issue() (IssuedToken, error) {
	raw := make([]byte, tokenLength)
	if _, err := io.ReadFull(i.random, raw); err != nil {
		return IssuedToken{}, fmt.Errorf("生成会话令牌: %w", err)
	}
	plaintext := base64.RawURLEncoding.EncodeToString(raw)
	return IssuedToken{
		Plaintext: plaintext,
		Digest:    i.Digest(plaintext),
	}, nil
}

// Verify 使用常量时间比较验证令牌明文与已存摘要是否匹配。
func (i *TokenIssuer) Verify(plaintext string, digest []byte) bool {
	computed := i.Digest(plaintext)
	return len(digest) == sha256.Size && subtle.ConstantTimeCompare(computed, digest) == 1
}

// Digest 计算数据库查询和持久化使用的领域隔离 SHA-256 摘要。
func (i *TokenIssuer) Digest(plaintext string) []byte {
	digest := sha256.Sum256([]byte(string(i.purpose) + "\x00" + plaintext))
	return digest[:]
}
