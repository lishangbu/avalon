package account

import (
	"crypto/subtle"
	"encoding/base64"
	"errors"
	"fmt"
	"io"
	"strings"

	"golang.org/x/crypto/argon2"
)

const (
	passwordMemory      = 64 * 1024
	passwordIterations  = 3
	passwordParallelism = 4
	passwordSaltLength  = 16
	passwordKeyLength   = 32
	passwordMinLength   = 12
	passwordMaxLength   = 1024
	passwordAlgorithm   = "argon2id"
	passwordParameters  = `{"memoryKiB":65536,"iterations":3,"parallelism":4,"saltBytes":16,"hashBytes":32}`
)

// PasswordCredential 是可持久化的密码摘要及其显式算法元数据。
type PasswordCredential struct {
	Encoded    string
	Algorithm  string
	Parameters []byte
}

// ErrInvalidPassword 表示密码长度不满足服务端资源与安全边界。
var ErrInvalidPassword = errors.New("密码长度无效")

// PasswordHasher 使用固定参数版本生成和验证 Argon2id 密码摘要。
type PasswordHasher struct {
	random io.Reader
}

// NewPasswordHasher 创建从指定安全随机源读取盐值的密码摘要器。
func NewPasswordHasher(random io.Reader) *PasswordHasher {
	return &PasswordHasher{random: random}
}

// Hash 生成包含算法和参数版本的 PHC 格式 Argon2id 摘要。
func (h *PasswordHasher) Hash(password string) (string, error) {
	if len(password) < passwordMinLength || len(password) > passwordMaxLength {
		return "", ErrInvalidPassword
	}
	salt := make([]byte, passwordSaltLength)
	if _, err := io.ReadFull(h.random, salt); err != nil {
		return "", fmt.Errorf("生成密码盐值: %w", err)
	}
	hash := argon2.IDKey(
		[]byte(password),
		salt,
		passwordIterations,
		passwordMemory,
		passwordParallelism,
		passwordKeyLength,
	)
	return fmt.Sprintf(
		"$argon2id$v=%d$m=%d,t=%d,p=%d$%s$%s",
		argon2.Version,
		passwordMemory,
		passwordIterations,
		passwordParallelism,
		base64.RawStdEncoding.EncodeToString(salt),
		base64.RawStdEncoding.EncodeToString(hash),
	), nil
}

// HashCredential 生成密码摘要及与其一致的持久化算法元数据。
func (h *PasswordHasher) HashCredential(password string) (PasswordCredential, error) {
	encoded, err := h.Hash(password)
	if err != nil {
		return PasswordCredential{}, err
	}
	return PasswordCredential{
		Encoded:    encoded,
		Algorithm:  passwordAlgorithm,
		Parameters: []byte(passwordParameters),
	}, nil
}

// Verify 使用摘要中经过边界校验的参数验证候选密码。
func (h *PasswordHasher) Verify(password string, encoded string) (bool, error) {
	if len(password) > passwordMaxLength {
		return false, ErrInvalidPassword
	}
	parts := strings.Split(encoded, "$")
	if len(parts) != 6 || parts[1] != "argon2id" {
		return false, errors.New("密码摘要格式无效")
	}
	var version int
	if _, err := fmt.Sscanf(parts[2], "v=%d", &version); err != nil || version != argon2.Version {
		return false, errors.New("密码摘要版本无效")
	}
	var memory, iterations uint32
	var parallelism uint8
	if _, err := fmt.Sscanf(parts[3], "m=%d,t=%d,p=%d", &memory, &iterations, &parallelism); err != nil {
		return false, errors.New("密码摘要参数无效")
	}
	if memory != passwordMemory || iterations != passwordIterations || parallelism != passwordParallelism {
		return false, errors.New("密码摘要参数版本不受支持")
	}
	salt, err := base64.RawStdEncoding.DecodeString(parts[4])
	if err != nil || len(salt) != passwordSaltLength {
		return false, errors.New("密码摘要盐值无效")
	}
	want, err := base64.RawStdEncoding.DecodeString(parts[5])
	if err != nil || len(want) != passwordKeyLength {
		return false, errors.New("密码摘要值无效")
	}
	got := argon2.IDKey([]byte(password), salt, iterations, memory, parallelism, uint32(len(want)))
	return subtle.ConstantTimeCompare(got, want) == 1, nil
}

// VerifyUnknownAccount 为不存在或格式无效的用户名执行与密码校验等价的 Argon2id 工作量。
// 固定盐值只用于消除账号枚举的明显时序差异，不产生也不验证任何真实凭据。
func (h *PasswordHasher) VerifyUnknownAccount(password string) {
	// 与 Verify 保持相同的输入上限，防止未知账号路径被超长输入放大工作量。
	if len(password) > passwordMaxLength {
		return
	}
	got := argon2.IDKey(
		[]byte(password),
		make([]byte, passwordSaltLength),
		passwordIterations,
		passwordMemory,
		passwordParallelism,
		passwordKeyLength,
	)
	_ = subtle.ConstantTimeCompare(got, make([]byte, passwordKeyLength))
}
