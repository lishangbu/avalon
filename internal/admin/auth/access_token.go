// Package auth 实现管理员专用的短期 Bearer access token 与公开 JWKS。
package auth

import (
	"crypto/ed25519"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/security/authentication"
)

var (
	// ErrInvalidAccessToken 表示 Bearer token 的格式、签名或声明无效。
	ErrInvalidAccessToken = errors.New("access token 无效")
)

const accessTokenType = "access"

// AccessTokenIssuer 使用单个 Ed25519 密钥签发并验证管理员 access token。
type AccessTokenIssuer struct {
	privateKey ed25519.PrivateKey
	publicKey  ed25519.PublicKey
	keyID      string
	ttl        time.Duration
	now        func() time.Time
}

type tokenHeader struct {
	Algorithm string `json:"alg"`
	Type      string `json:"typ"`
	KeyID     string `json:"kid"`
}

type tokenClaims struct {
	Subject         snowflake.ID `json:"sub"`
	SessionID       snowflake.ID `json:"sid"`
	SessionFamilyID snowflake.ID `json:"sfid"`
	IssuedAt        int64        `json:"iat"`
	ExpiresAt       int64        `json:"exp"`
	Type            string       `json:"typ"`
}

// JWK 是管理员 access token 当前 Ed25519 公钥的 RFC 8037 表示。
type JWK struct {
	// KeyType 固定为 OKP。
	KeyType string `json:"kty"`
	// Curve 固定为 Ed25519。
	Curve string `json:"crv"`
	// Use 固定为 sig。
	Use string `json:"use"`
	// Algorithm 固定为 EdDSA。
	Algorithm string `json:"alg"`
	// KeyID 是 token header 与公钥之间的稳定关联标识。
	KeyID string `json:"kid"`
	// X 是无填充 base64url 编码的 Ed25519 公钥。
	X string `json:"x"`
}

// NewEphemeralAccessTokenIssuer 为当前进程生成 Ed25519 密钥。
// 未发布阶段使用进程级密钥；重启会使全部短期 access token 提前失效，但不影响 refresh token。
func NewEphemeralAccessTokenIssuer(ttl time.Duration, now func() time.Time) (*AccessTokenIssuer, error) {
	publicKey, privateKey, err := ed25519.GenerateKey(rand.Reader)
	if err != nil {
		return nil, fmt.Errorf("生成 Ed25519 密钥: %w", err)
	}
	digest := sha256.Sum256(publicKey)
	return &AccessTokenIssuer{privateKey: privateKey, publicKey: publicKey, keyID: base64.RawURLEncoding.EncodeToString(digest[:12]), ttl: ttl, now: now}, nil
}

// Issue 为已认证管理员签发只包含最小身份声明的短期 access token。
func (i *AccessTokenIssuer) Issue(principal authentication.Principal) (string, time.Time, error) {
	now := i.now().UTC()
	expiresAt := now.Add(i.ttl)
	header, err := json.Marshal(tokenHeader{Algorithm: "EdDSA", Type: "JWT", KeyID: i.keyID})
	if err != nil {
		return "", time.Time{}, err
	}
	claims, err := json.Marshal(tokenClaims{Subject: principal.AccountID, SessionID: principal.SessionID, SessionFamilyID: principal.SessionFamilyID, IssuedAt: now.Unix(), ExpiresAt: expiresAt.Unix(), Type: accessTokenType})
	if err != nil {
		return "", time.Time{}, err
	}
	unsigned := base64.RawURLEncoding.EncodeToString(header) + "." + base64.RawURLEncoding.EncodeToString(claims)
	signature := ed25519.Sign(i.privateKey, []byte(unsigned))
	return unsigned + "." + base64.RawURLEncoding.EncodeToString(signature), expiresAt, nil
}

// Verify 校验 Ed25519 签名、密钥标识、token 类型、时间与 Identifier 声明。
func (i *AccessTokenIssuer) Verify(token string) (authentication.Principal, error) {
	parts := strings.Split(token, ".")
	if len(parts) != 3 {
		return authentication.Principal{}, ErrInvalidAccessToken
	}
	var header tokenHeader
	if !decodeJSONPart(parts[0], &header) || header.Algorithm != "EdDSA" || header.Type != "JWT" || header.KeyID != i.keyID {
		return authentication.Principal{}, ErrInvalidAccessToken
	}
	signature, err := base64.RawURLEncoding.DecodeString(parts[2])
	if err != nil || !ed25519.Verify(i.publicKey, []byte(parts[0]+"."+parts[1]), signature) {
		return authentication.Principal{}, ErrInvalidAccessToken
	}
	var claims tokenClaims
	if !decodeJSONPart(parts[1], &claims) || claims.Type != accessTokenType {
		return authentication.Principal{}, ErrInvalidAccessToken
	}
	now := i.now().UTC().Unix()
	if claims.IssuedAt > now+30 || claims.ExpiresAt <= now || claims.ExpiresAt <= claims.IssuedAt {
		return authentication.Principal{}, ErrInvalidAccessToken
	}
	if !claims.Subject.IsValid() || !claims.SessionID.IsValid() || !claims.SessionFamilyID.IsValid() {
		return authentication.Principal{}, ErrInvalidAccessToken
	}
	return authentication.Principal{AccountID: claims.Subject, SessionID: claims.SessionID, SessionFamilyID: claims.SessionFamilyID}, nil
}

// JWK 返回不含私钥材料的当前 Ed25519 公钥。
func (i *AccessTokenIssuer) JWK() JWK {
	return JWK{KeyType: "OKP", Curve: "Ed25519", Use: "sig", Algorithm: "EdDSA", KeyID: i.keyID, X: base64.RawURLEncoding.EncodeToString(i.publicKey)}
}

func decodeJSONPart(part string, destination any) bool {
	value, err := base64.RawURLEncoding.DecodeString(part)
	if err != nil {
		return false
	}
	return json.Unmarshal(value, destination) == nil
}
