# Reserve Match capacity when accepting Challenges

Pending Challenge 不提前占用账户唯一的活跃 Match 名额，账户可以同时发出或收到多个待处理邀请，但同一对 Trainer 不分方向最多存在一个 Pending Challenge；相同方向的重复请求返回已有邀请，反向重复返回冲突。只有接受操作才在同一事务中原子检查并占用双方名额；接受成功后，双方账户涉及的其他 Pending Challenge 转为 `SUPERSEDED`，并发接受只能有一个成功。
