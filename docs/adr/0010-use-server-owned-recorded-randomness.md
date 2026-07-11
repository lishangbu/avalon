# Use server-owned recorded randomness

正式 Battle Session 不接受调用方提供 randomSeed。每个 Turn Command 由 Session Runtime 的安全随机源产生随机值，并通过 RecordingBattleRandom 捕获实际消费的 Random Trace；新权威状态、事件与随机轨迹由单所有者 Runtime 原子提交到内存 Turn Record。已经成功的 commandId 重试返回原始回合结果；更早回合的结果使用已记录轨迹按需重放，不再次消费安全随机源，也不改变当前 Session。失败的命令不留下回合记录或推进 Session。这样未来 Match 只能提交行动选择，无法通过挑选或反复尝试种子影响结果，同时能在 Recent Session 存活期间使用已记录轨迹复盘，并可在以后接入外部归档。
