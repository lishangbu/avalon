package battleengine

import (
	"hash/fnv"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

func testID(label string) snowflake.ID {
	hash := fnv.New64a()
	_, _ = hash.Write([]byte(label))
	return snowflake.ID((hash.Sum64() & ((1 << 62) - 1)) + 1)
}

func testIDs(values ...any) []snowflake.ID {
	identifiers := make([]snowflake.ID, len(values))
	for index, value := range values {
		switch value := value.(type) {
		case string:
			identifiers[index] = testID(value)
		case snowflake.ID:
			identifiers[index] = value
		default:
			panic("测试 Identifier 类型无效")
		}
	}
	return identifiers
}
