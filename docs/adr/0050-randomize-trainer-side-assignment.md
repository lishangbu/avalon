# Randomize Trainer side assignment

Match 创建时由服务端以等概率把 Challenger 与目标 Trainer 分配到 Battle Session 两侧，并持久保存 Trainer-to-side 映射，不让发起方向固定影响引擎 side 顺序。玩家 Match View 不暴露内部 side ID，Battle Result 必须通过该映射转换为 winnerTrainerId；生产使用服务端随机源，测试注入确定性分配 Adapter。
