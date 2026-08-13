// Package requestid 生成仅用于日志关联和幂等审计的非实体请求标识。
package requestid

import (
	"crypto/rand"
	"encoding/hex"
	"strconv"
	"sync/atomic"
	"time"
)

var fallbackCounter atomic.Uint64

// New 返回不承载实体身份语义的短期请求标识。
func New() string {
	var random [16]byte
	if _, err := rand.Read(random[:]); err == nil {
		return "r_" + hex.EncodeToString(random[:])
	}
	return "r_" + strconv.FormatInt(time.Now().UTC().UnixNano(), 36) + "_" + strconv.FormatUint(fallbackCounter.Add(1), 36)
}
