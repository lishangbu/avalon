# Project Match state per Trainer

Match API 不直接返回完整 Battle Session Snapshot，而是按当前 Trainer 生成 Match View：己方信息完整可见，对方在 Match 开始时只公开生物种类、形态和队伍数量，技能、携带道具、特性、性格与具体能力值保持隐藏，并且只随公开战斗事件逐步揭示。Challenge 接受前只公开规则和队伍规模，不提前展示生物列表；Match 进入 `COMPLETED` 或 `INTERRUPTED` 后仍沿用相同投影，未在战斗中揭示的对方配置不会因终局或 Match History 查询而公开。
