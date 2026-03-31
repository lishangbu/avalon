package io.github.lishangbu.avalon.game.battle.engine.loader

/**
 * 原始文档 schema 校验接口。
 *
 * 设计意图：
 * - 在反序列化前或反序列化后执行结构合法性校验。
 * - 把 schema 校验职责从数据加载器中拆出来。
 */
interface SchemaValidator {
    /**
     * 校验原始文档是否符合 schema 约束。
     */
    fun validate(rawDocument: String)
}
