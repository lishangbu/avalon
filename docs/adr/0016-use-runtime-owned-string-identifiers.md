# Use Runtime-owned string identifiers

Session Runtime 使用服务端生成且不复用的 UUID v4 字符串作为 sessionId，使无数据库的会话可以直接按哈希分片路由；sideId 和 actorId 分别按会话内的 side 与阵容顺序生成稳定字符串，例如 `side-1` 与 `side-1-actor-1`；Turn Command 的 commandId 由调用方生成 UUID 字符串以关联网络重试。所有这些值在 API 中均为不透明字符串，不参与数值运算，也不适用数据库实体的 CosId 长整型 Identifier 约定。
