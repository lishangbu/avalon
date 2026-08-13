---
status: accepted
---

# 只支持当前 Snowflake 空库基线

Avalon 尚未对外发布，现有资料基线已经固化为 node `0` 的正数 Snowflake Identifier，运行进程只从 PostgreSQL 租约表领取 node `1..254`，node `255` 保留且不分配。数据库只允许从当前 Ent Schema 和同一发布批次的受控资料基线在空库中建立；恢复只能使用同一标识协议的受控备份。

资料基线直接维护最终十进制 Identifier。Snowflake Identifier 只承担稳定身份；业务排序使用显式时间或序号。
