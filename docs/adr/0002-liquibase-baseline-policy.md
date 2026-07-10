# Evolve schemas by default and rebaseline explicitly

Avalon 默认通过新增 Liquibase changeset 演进数据库，避免破坏已经执行的迁移。只有用户明确要求重做基线且不需要兼容已部署数据库时，才允许重写初始 schema 与 seed，并要求重建数据库；一旦出现发布 tag 或生产兼容要求，后续只能追加迁移。
