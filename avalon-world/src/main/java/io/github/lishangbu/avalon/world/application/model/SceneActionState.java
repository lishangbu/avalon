package io.github.lishangbu.avalon.world.application.model;

import java.util.List;

/**
 * SceneActionQueryService 使用的独立数据投影，包含location、revision、actions、information等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param location 地点代码
 * @param revision 状态修订，供后续命令并发检查
 * @param actions 允许的行动声明
 */
public record SceneActionState(String location, long revision, List<SceneActionView> actions,
		List<SceneActionInformationView> information) {
}
