package io.github.lishangbu.avalon.world.application.model;

/**
 * FarmQueryService 使用的独立数据投影，包含index、crop、name、bound等数据。 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param crop 作物代码
 * @param name 展示名称
 * @param bound 资产是否绑定
 * @param watered 是否已浇水
 * @param mature 是否已成熟
 * @param remainingSeconds 剩余秒数
 */
public record FarmPlot(int index, String crop, String name, boolean bound, boolean watered, boolean mature,
		long remainingSeconds) {
}
