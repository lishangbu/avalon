// Package rpg 包含 RPG 地图和遭遇使用的纯规则组件。
package rpg

import (
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"encoding/binary"
	"errors"
	"fmt"
)

const randomAlgorithm = "hmac-sha256-v1"

// RandomAlgorithm 返回当前遭遇随机算法的稳定持久化标识。
func RandomAlgorithm() string { return randomAlgorithm }

// RandomSource 是一次遭遇抽样使用的不可变随机源；seed 只在服务端内存中持有。
type RandomSource struct{ seed [32]byte }

// NewRandomSource 使用 CSPRNG 创建随机源。
func NewRandomSource() (RandomSource, error) {
	var seed [32]byte
	if _, err := rand.Read(seed[:]); err != nil {
		return RandomSource{}, fmt.Errorf("生成 RPG 随机 seed: %w", err)
	}
	return RandomSource{seed: seed}, nil
}

// NewRandomSourceFromSeed 仅供确定性测试使用，生产调用方不得把 seed 暴露给玩家。
func NewRandomSourceFromSeed(seed []byte) (RandomSource, error) {
	if len(seed) != 32 {
		return RandomSource{}, errors.New("RPG 随机 seed 必须为 32 字节")
	}
	var value [32]byte
	copy(value[:], seed)
	return RandomSource{seed: value}, nil
}

// Algorithm 返回持久化的随机算法版本。
func (RandomSource) Algorithm() string { return RandomAlgorithm() }

// Seed 返回仅供持久化适配器冻结遭遇事实使用的 seed 副本。
func (source RandomSource) Seed() []byte {
	return append([]byte(nil), source.seed[:]...)
}

// DrawUint32 按用途和 draw 序号派生 [0, upperBound) 的均匀整数。
func (source RandomSource) DrawUint32(purpose string, drawNumber uint64, upperBound uint32) (uint32, error) {
	if purpose == "" {
		return 0, errors.New("随机用途不能为空")
	}
	if upperBound == 0 {
		return 0, errors.New("随机上界必须大于零")
	}
	// rejection sampling 丢弃无法均匀映射到 upperBound 的尾部。
	limit := uint64(^uint32(0)) + 1
	limit -= limit % uint64(upperBound)
	for attempt := uint64(0); ; attempt++ {
		mac := hmac.New(sha256.New, source.seed[:])
		mac.Write([]byte(randomAlgorithm))
		mac.Write([]byte{0})
		mac.Write([]byte(purpose))
		var encoded [16]byte
		binary.BigEndian.PutUint64(encoded[:8], drawNumber)
		binary.BigEndian.PutUint64(encoded[8:], attempt)
		mac.Write(encoded[:])
		value := binary.BigEndian.Uint32(mac.Sum(nil)[:4])
		if uint64(value) < limit {
			return value % upperBound, nil
		}
	}
}
