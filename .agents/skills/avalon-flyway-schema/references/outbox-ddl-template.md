# Outbox DDL 模板

## 1. 默认表

除非有很强的运维理由要求拆分，否则默认使用一个共享 outbox 表：

```text
integration.outbox_event
```

## 2. 推荐列

- `id`
- `event_id`
- `owner_context`
- `aggregate_type`
- `aggregate_id`
- `event_type`
- `payload`
- `headers`
- `status`
- `retry_count`
- `available_at`
- `occurred_at`
- `published_at`
- `last_error`
- `trace_id`

## 3. 推荐类型

- `id`：`bigint` 或 `UUID`，遵循项目统一策略
- `event_id`：带唯一性保证的 `UUID`
- `payload`：`JSONB`
- `headers`：需要时使用 `JSONB`
- 时间列：带时区的时间戳类型

## 4. 推荐索引

- 在 `event_id` 上建立唯一索引
- 为 `status, available_at` 建立轮询索引
- 为 `owner_context, aggregate_type, aggregate_id` 建立查询索引

## 5. 备注

- DDL 归这个技能负责
- 运行时发布流程归 `avalon-transaction-outbox` 负责
