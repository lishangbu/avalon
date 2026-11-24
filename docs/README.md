# 项目文档

这里只维护与当前实现对应的规则、结构、操作入口和验证边界。内容数值以正式JSON为准，依赖版本以POM、Wrapper和前端锁文件为准。生成报告保留在本地outputs或target，不在此维护历史执行流水或未采用的候选表。

## 游戏与内容

- [当前实施状态](implementation-progress.md)
- [游戏规则概览](game-design.md)
- [内容事实来源](chapter-content-sources.md)
- [内容JSON与Schema](content-json.md)
- [命名规则](content-naming.md)
- [任务规则](quest-system.md)、[任务框架](quest-framework.md)、[任务目录](quest-catalog.md)、[目标统计](quest-objective-audit.md)
- [学院训练](academy-training.md)、[副本目录](dungeons.md)、[人物交互](npc-and-content-details.md)
- [生活专业](life-professions.md)、[生活框架](life-framework.md)
- [成长预算边界](high-level-balance.md)、[图片资源](creature-asset-delivery.md)
- [聊天与通知](chat.md)

## 工程与维护

- [开发约定](development-constraints.md)
- [后端结构](backend-structure.md)、[前端结构](frontend-structure.md)、[战斗框架](battle-framework.md)
- [Maven构建](maven-build.md)、[数据库维护](database-migration.md)、[Docker部署](docker-deployment.md)
- [测试流程](testing-workflow.md)、[脚本入口](scripts.md)、[工程验收](engineering-acceptance.md)、[任务验收入口](quest-system-validation.md)
- [模块化单体架构](adr/0001-spring-modular-monolith.md)、[Vue客户端架构](adr/0002-vue-client.md)
