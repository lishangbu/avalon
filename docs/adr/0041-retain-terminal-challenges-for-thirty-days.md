# Retain terminal Challenges for thirty days

`ACCEPTED`、`REJECTED`、`CANCELLED`、`EXPIRED` 与 `SUPERSEDED` Challenge 保留三十天，供双方查看近期邀请并支持幂等重试；列表只返回方向、对方开局 displayName、状态、队伍人数和时间。收到方在接受前看不到发起方生物、Lead 或战术配置，发起方详情只可查看自己的 Snapshot；Accepted Challenge 返回 `matchId`，双方 Snapshot 转由 Match History 永久保留，其他终态不公开额外 Team 信息。三十天清理终态 Challenge 时，同时删除所有未转归 Match 的 Trainer Team Snapshot。
