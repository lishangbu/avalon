package io.github.lishangbu.avalon.world.application.model;

/**
 * LifeCraftingService 使用的命令或结果数据，包含completed、produced、broken、stop等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param completed 是否完成
 * @param broken 是否已经断线
 * @param proficiency 熟练度
 */
public record LifeCraftingResult(int completed, int produced, int broken, String stop, double proficiency) {
}
