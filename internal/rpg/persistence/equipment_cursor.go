package persistence

import (
	"bytes"
	"crypto/sha256"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"io"
	"strings"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
	rpg "github.com/lishangbu/avalon/internal/rpg"
)

const maximumEquipmentCursorLength = 2048

type equipmentCursor struct {
	Kind       string `json:"kind"`
	FilterHash string `json:"filterHash"`
	ID         string `json:"id"`
	Time       string `json:"time,omitempty"`
}

func encodeEquipmentCursor(kind, filterHash string, id snowflake.ID, timestamp time.Time) (string, error) {
	if kind == "" || !id.IsValid() {
		return "", rpg.ErrInvalidEquipmentCursor
	}
	wire := equipmentCursor{Kind: kind, FilterHash: filterHash, ID: id.String()}
	if !timestamp.IsZero() {
		wire.Time = timestamp.UTC().Format(time.RFC3339Nano)
	}
	raw, err := json.Marshal(wire)
	if err != nil {
		return "", rpg.ErrInvalidEquipmentCursor
	}
	encoded := base64.RawURLEncoding.EncodeToString(raw)
	if len(encoded) > maximumEquipmentCursorLength {
		return "", rpg.ErrInvalidEquipmentCursor
	}
	return encoded, nil
}

func decodeEquipmentCursor(raw, kind, filterHash string, requireTime bool) (snowflake.ID, time.Time, error) {
	if raw == "" {
		return 0, time.Time{}, nil
	}
	if len(raw) > maximumEquipmentCursorLength {
		return 0, time.Time{}, rpg.ErrInvalidEquipmentCursor
	}
	decoded, err := base64.RawURLEncoding.DecodeString(raw)
	if err != nil {
		return 0, time.Time{}, rpg.ErrInvalidEquipmentCursor
	}
	decoder := json.NewDecoder(bytes.NewReader(decoded))
	decoder.DisallowUnknownFields()
	var wire equipmentCursor
	if err = decoder.Decode(&wire); err != nil || decoder.Decode(&struct{}{}) != io.EOF || wire.Kind != kind || wire.FilterHash != filterHash {
		return 0, time.Time{}, rpg.ErrInvalidEquipmentCursor
	}
	id, err := snowflake.Parse(wire.ID)
	if err != nil {
		return 0, time.Time{}, rpg.ErrInvalidEquipmentCursor
	}
	if !requireTime {
		if wire.Time != "" {
			return 0, time.Time{}, rpg.ErrInvalidEquipmentCursor
		}
		return id, time.Time{}, nil
	}
	timestamp, err := time.Parse(time.RFC3339Nano, wire.Time)
	if err != nil || timestamp.Location() != time.UTC {
		return 0, time.Time{}, rpg.ErrInvalidEquipmentCursor
	}
	return id, timestamp, nil
}

func equipmentFilterHash(values ...string) string {
	normalized := strings.Join(values, "\x00")
	digest := sha256.Sum256([]byte(normalized))
	return hex.EncodeToString(digest[:])
}

func equipmentOptionalIDText(id snowflake.ID) string {
	if !id.IsValid() {
		return ""
	}
	return id.String()
}

func equipmentOptionalBoolText(value *bool) string {
	if value == nil {
		return ""
	}
	if *value {
		return "true"
	}
	return "false"
}
