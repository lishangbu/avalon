# Issue tracker：GitHub

本仓库的需求、缺陷和 PRD 均使用 GitHub Issues 管理，远端仓库为 `github.com/lishangbu/avalon`。执行议题操作时使用 `gh` CLI，并从当前克隆自动识别仓库。

## 约定

- 创建议题：`gh issue create --title "..." --body "..."`。
- 阅读议题：`gh issue view <编号> --comments`，同时核对标签。
- 列出议题：使用 `gh issue list`，并按状态和标签筛选。
- 评论、改标签和关闭：使用 `gh issue comment`、`gh issue edit` 和 `gh issue close`。

## Pull request 作为请求入口

**Pull request 作为请求入口：否。**

外部 PR 不进入自动分诊队列；需要讨论或实现的工作应先通过 GitHub Issue 记录。

## 技能约定

当工程技能要求“发布到议题平台”时，创建 GitHub Issue；当要求“获取相关议题”时，运行 `gh issue view <编号> --comments`。
