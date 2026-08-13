---
status: accepted
---

# 使用版本化随机源与不可变遭遇事实

Encounter 和 Battle 不使用系统全局随机、客户端 seed 或按重试重新抽样。首期服务端使用 `hmac-sha256-v1`：由密码学安全随机源生成 32 字节 seed，以用途和单调 draw 序号派生均匀整数；算法标识、seed、每次 draw 的输入摘要和结果写入不可变事实。

Traversal 事务冻结 Encounter Table/Entry 版本、Creature/Form/等级结果和必要 Projection；Pending Encounter 保存来源 Traversal、期限、状态和关联 Battle。终态永久保留，重复命令只能返回首次结果。该设计让断线恢复、离线重放和审计可以验证同一随机轨迹，代价是需要限制快照大小并维护算法版本注册表。
