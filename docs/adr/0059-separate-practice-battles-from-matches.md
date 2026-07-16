# Separate Practice Battles from Matches

练习 Bot 通过独立 Practice Battle 协调服务端控制的一方并复用 Battle Session，不创建假 Account、Trainer、Presence 或 Challenge，也不写入正式 Match History、战绩和奖励。Bot Team Template 与策略分别表达内容和决策，Runtime 丢失时练习战直接中断；这保留单人可玩性而不污染真人竞技模型。
