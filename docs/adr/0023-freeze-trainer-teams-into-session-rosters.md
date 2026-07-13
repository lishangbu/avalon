# Freeze Trainer Teams into Session Rosters

真人 Match 不接受客户端临时拼装的生物与技能列表；每个 Trainer 同时至多拥有一支持久 Trainer Team，Team API 只提供读取、覆盖和删除当前 Team。Challenge 不接收 `teamId`，发起方创建时、接受方接受时由服务端读取各自唯一 Team，并按请求选择的 Lead 锁定 Snapshot；接受时重新校验双方，发起方 Snapshot 失效会取消 Challenge，接受方 Team 失效只拒绝本次请求。两份 Snapshot 就绪后直接创建 Match 与 Session Roster，Active Match 使用冻结状态且不受后续 Current Game Data 修改影响。
