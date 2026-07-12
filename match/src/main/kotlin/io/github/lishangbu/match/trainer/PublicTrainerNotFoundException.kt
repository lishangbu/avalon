package io.github.lishangbu.match.trainer

/** 名称无效、Trainer 不存在或已归档时统一返回不存在，避免公开枚举额外状态。 */
class PublicTrainerNotFoundException : RuntimeException("Public Trainer not found")
