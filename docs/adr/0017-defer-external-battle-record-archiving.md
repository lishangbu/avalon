# Defer external Battle Record archiving

首版 Session Runtime 在 `COMPLETED` 或 `TERMINATED` 时生成完整且不可变的 Battle Record，并只将它保存在对应 Recent Session 中；不新增数据库表、消息队列、后台线程或占位异步 sink，终态缓存淘汰时 Record 一并消失。测试验证 Record 内容和生成过程不会改变战斗结果。未来 Match 与消息基础设施明确后，可以为同一 Record 契约增加 Kafka、对象存储或其他异步适配器，而不改变活跃 Session 的执行路径。
