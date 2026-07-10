# Use a current-game-data snapshot

Avalon 将 NationalDex 的当前状态作为游戏资料的唯一权威视图，并通过重做基线排除版本、世代、历史、来源 URL 和同步状态。系统仍保留稳定 Internal Code，以及进化和战斗规则执行所必需的 Support Data；这换取了更小且一致的运行时模型，但不支持历史查询，并要求基线变化时重建数据库。
