package io.github.lishangbu.avalon.world.application.model;

import java.time.Instant;

/**
 * FarmState 使用的独立数据投影，包含crop、bound、wateredAt等数据。 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param crop 作物代码
 * @param bound 资产是否绑定
 * @param wateredAt 浇水时刻
 */
public record FarmStateCrop(String crop, boolean bound, Instant wateredAt) {
}
