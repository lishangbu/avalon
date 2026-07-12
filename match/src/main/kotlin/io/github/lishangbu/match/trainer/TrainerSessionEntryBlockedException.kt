package io.github.lishangbu.match.trainer

/** 账户正在提交 Trainer 归档事务，暂时禁止建立新 Session。 */
class TrainerSessionEntryBlockedException : IllegalStateException("Trainer archive is in progress")
