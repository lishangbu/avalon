# Derive Trainer records from Match Results

Trainer 表不冗余维护胜、负、平或总场次数；Match Result 是战绩的唯一事实来源，列表与未来统计按 Result 查询或投影。这样避免 Forfeit、Timeout、No Contest 与并发终态导致双写计数漂移；排行榜与高性能汇总以后可增加可重建投影，但不能成为裁决事实。
