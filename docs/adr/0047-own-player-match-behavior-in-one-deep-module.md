# Own player Match behavior in one deep module

首版建立单个深 `match` Gradle Module，内部以 trainer、challenge、match package 保持 locality，但统一拥有 Trainer、唯一 Trainer Team、Trainer Session/Presence、Challenge、Match Runtime、持久化与玩家 interface，避免拆分后形成双向依赖。Match Module 自己定义小型 `BattleSessionHost` seam，`battle-rules` 提供生产 Adapter、测试提供内存 Adapter；`battle-session` 与 `battle-engine` 继续不知道账户、Trainer 或 Match。
