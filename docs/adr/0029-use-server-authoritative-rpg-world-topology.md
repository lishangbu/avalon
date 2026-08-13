---
status: superseded
superseded_by: 0036
---

# 使用服务端权威的 RPG 世界拓扑

Avalon 使用 Region、层级 Location 与独立的有向 Location Exit 组成 World Topology，并以 PostgreSQL 关系事实作为移动判定的唯一权威。PlayerCharacter 只能通过服务端校验的 Location Traversal 改变当前位置；出口条件使用后端实现并统一校验的强类型规则原语，不执行任意脚本，也不接受客户端坐标或路径作为可达性依据。

Location 父子层级允许任意有限深度，现有 `world/settlement/route/wild/dungeon/interior/arena` 只作展示分类，不隐含规则。每条 Location Exit 拥有独立 Snowflake Identifier、全局 Stable Code、展示排序、源、目标、启用状态、版本、条件和效果；反向或跨 Region 路线都必须显式建另一条有向记录。

Exit Condition 固定由 `all`、`any`、`not` 逻辑节点组合等级、持有 Item、Quest Objective、Profession 与 World State 开关等有限叶子原语。Traversal Effect 也只允许修改当前 PlayerCharacter 的 World State 开关和 Quest Objective 进度；条件与副作用均由服务端解释并在保存资料时校验。

Map Discovery 作为 PlayerCharacter 的持久事实裁剪可见拓扑；成功的 Location Traversal 按当前 Encounter Table 使用服务端显式随机源产生 Pending Encounter。坐标、图标和背景只形成具有独立版本的 World Map Projection，允许单独调整展示布局，而不会改变 World Topology 或被战斗空间规则复用。

World Map Projection 首期使用整数 `x/y` 和可选 `z` 坐标、布局版本与 Asset 引用；同一拓扑可以保留多个历史布局，但读取只选择当前启用展示版本。发现 Location/Exit 的事实保存首次发现时间和可选来源 Traversal，Discovery 只能增长，资料停用或布局变更不抹除历史。

Checkpoint 是声明所属 Location、启用状态、可设置条件和恢复条件的独立稳定资料。当前位置引用的 Location 停用后仍保留原始 Player Position，但角色不能继续移动；维护者必须通过受控维护操作安置受影响角色，不能在读取或移动时静默改写位置。

地图资料启用前必须通过维护校验：父子 Location 同属 Region、层级无环、Exit 不自环且源/目标存在、启用目标可引用、Stable Code 唯一，并且所有启用节点从默认出生 Location 可达。每次校验生成包含状态、错误码、引用路径和运行时间的不可变报告；失败阻止发布，管理读取仍可返回完整历史。新 PlayerCharacter 由服务端在创建事务中选择默认启用出生 Location，写入位置序号 `0` 和首次 Map Discovery；不存在合法出生点时拒绝创建或资料发布。稳定 Location、Exit 和 Checkpoint 只允许停用，不物理删除；停用目标拒绝 Traversal。

RPG Map Read 与写命令分离：玩家服务只返回当前位置和 Discovery 裁剪后的安全字段，管理员服务只读完整拓扑、Exit Condition、Encounter 资料和 Topology Integrity Report；首期不提供在线地图编辑 RPC。读取使用有界 opaque cursor，并在游标中绑定读取时间、稳定排序键和必要的资源版本摘要，不持久化全局 topology revision；Traversal 使用出口身份、期望位置版本和幂等身份，重试只能重放原结果。

这一边界牺牲了自由坐标移动和客户端离线推演的灵活性，换取跨设备一致状态、防止绕过通行条件、可审计随机结果，以及不受地图换皮影响的稳定玩法规则。
