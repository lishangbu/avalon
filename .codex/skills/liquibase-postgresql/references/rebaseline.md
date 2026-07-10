# 显式重做基线

只有用户明确使用“重做基线”等指令，并确认无需兼容已部署数据库时读取和执行。

1. 检查 Git tag、部署兼容要求与当前 changeset 历史。
2. 明确删除的历史/版本模型和保留的 Current Game Data、Support Data。
3. 同步重写初始 schema、seed、Jimmer 实体、Repository、API 和迁移测试。
4. 在空 PostgreSQL 上强制执行完整迁移，校验表/CSV 集合、唯一键、外键和 code 引用。
5. 在交付中明确数据库必须 drop/recreate。

出现发布 tag 或生产升级兼容要求后，禁止使用该分支，改为追加 changeset。
