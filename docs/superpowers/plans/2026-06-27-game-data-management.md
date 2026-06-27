# 游戏资料管理实现计划

**目标：** 以最小可用方式完成独立游戏资料域、数据库结构、内置中性数据、后端 CRUD 和前端表格管理。

## 任务

- [ ] 新增 `game-data` Gradle 模块并接入 `app`。
- [ ] 新增 `002-game-data-schema.yaml`，创建三范式资料表和关系表，并带完整 remarks。
- [ ] 新增 `003-game-data-seed.yaml`，写入 `game-data:admin`、菜单、角色和中性样例数据。
- [ ] 为 `001-initial-schema.yaml` 现有表和字段补充 remarks。
- [ ] 后端为每张资料表新增独立 Controller、Service 和错误处理。
- [ ] 安全配置改为 `/api/game-data/** -> game-data:admin`。
- [ ] OpenAPI scope 增加 `game-data:admin`，默认 OAuth client 允许该 scope。
- [ ] 前端同步 OpenAPI，新增游戏资料菜单路由、service 和普通 CRUD 表格页。
- [ ] 运行后端迁移/API 测试和前端类型/页面测试。
