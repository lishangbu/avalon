# Archive Trainers instead of deleting them

每个账户最多拥有三个有效 Trainer，Trainer 只能归档而不能物理删除；归档释放创建名额，但全局唯一 displayName 不释放，唯一 Trainer Team 与 Match History 继续保留。所属账户可以在有效 Trainer 少于三个时恢复归档 Trainer，恢复不自动建立 Trainer Session，且 Team 必须重新通过当前规则才能挑战；当前 Trainer 或仍有 Active Match 的 Trainer 不能归档，归档事务会先取消其全部 Pending Challenge。
