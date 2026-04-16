# Outbox 流程

## 1. 标准流程

```text
command
  -> application service
  -> local transaction
     -> 更新业务表
     -> 插入 outbox 行
  -> commit
  -> 异步 dispatcher 领取待发布记录
  -> publish
  -> 标记成功或安排重试
```

## 2. 写侧规则

在同一个本地事务中：

- 写入业务变更
- 写入 outbox 行

不要这样做：

- 把直接发布到 broker 当成一次额外的 best-effort 写入
- 在事务提交成功之前先行发布

## 3. Dispatcher 规则

`dispatcher` 应当：

- 轮询待发布记录
- 安全地领取任务
- 在原始业务事务之外执行发布
- 显式标记成功，或递增重试状态

## 4. 事件字段建议

事件负载应当满足：

- 语义明确
- 可版本化
- 不依赖持久化行模型的直接序列化细节
