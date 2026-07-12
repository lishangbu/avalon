package io.github.lishangbu.match.trainer

/** 对 Trainer 聚合执行条件写入时携带的预期版本。 */
data class RevisionRequest(var expectedRevision: Long? = null)
