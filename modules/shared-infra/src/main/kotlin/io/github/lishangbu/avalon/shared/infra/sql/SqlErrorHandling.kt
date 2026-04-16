package io.github.lishangbu.avalon.shared.infra.sql

/**
 * 用统一样板包裹 SQL 执行，并把底层异常交给调用方自己的映射策略处理。
 *
 * shared-infra 只负责提供技术壳，不负责决定某个上下文应该抛出什么领域异常。
 *
 * @param mapError 调用方提供的异常翻译策略。
 * @param block 具体 SQL 执行逻辑。
 * @return 执行结果。
 */
suspend fun <T> translateSqlErrors(
    mapError: (Throwable) -> Throwable,
    block: suspend () -> T,
): T =
    try {
        block()
    } catch (exception: Throwable) {
        throw mapError(exception)
    }