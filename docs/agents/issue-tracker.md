# Issue tracker: GitHub

本仓库的 issue 与 PRD 存放在 GitHub Issues，使用 `gh` CLI 读取和写入。

仓库远端为 `github.com/lishangbu/avalon`。外部 Pull Request 不作为 `/triage` 的请求入口；只有 GitHub Issues 进入分诊状态机。

## 常用操作

- 创建：`gh issue create --title "..." --body "..."`
- 查看：`gh issue view <number> --comments`
- 列表：`gh issue list --state open --json number,title,body,labels,comments`
- 评论：`gh issue comment <number> --body "..."`
- 关闭：`gh issue close <number> --comment "..."`

需要发布到 issue tracker 时，创建 GitHub Issue；需要读取 ticket 时，同时读取正文、评论和 labels。
