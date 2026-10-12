package io.github.lishangbu.avalon.battle.api.model;

/**
 * AcademyTrainingResource 使用的命令或结果数据，包含ball等字段。 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param ball 封印球声明
 */
public record AcademyTrainingCaptureInfo(String ball) {
}
