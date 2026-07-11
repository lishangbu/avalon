# Make Battle Session creation server authoritative

创建 Battle Session 时，调用方只提交 `formatCode` 和 Session Roster，包括资料标识、等级、技能、特性、道具、个体值、努力值、性格及初始上场位置。Session Runtime 生成会话内稳定的 sideId 和 actorId，执行完整准备校验，从 Current Game Data 解析可执行规则，冻结本次 Runtime 使用的初始状态并立即启动会话。创建接口不接受 BattleState、规则 JSON、历史状态或随机种子；Runtime 存活期间的资料变化不会改变已经创建的 Session。未来 Match 使用同一创建契约提交已选阵容，并以响应中的场内标识建立玩家映射。
