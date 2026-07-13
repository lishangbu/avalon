# Adjudicate missed turn deadlines in Match

Match 独立管理每回合的行动期限，首版默认九十秒并允许服务端配置；Match 进入 `ACTIVE` 或 Battle Session 产生下一回合要求时生成新的绝对时间 `turnDeadlineAt`，断线、重连、查询和失败请求都不续期。期限届满时，只有一方未提交则该方超时判负，双方都未提交则 Match 以 No Contest 结束，且永远不向 Battle Session 发送不完整 Turn Command。
