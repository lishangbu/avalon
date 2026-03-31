package io.github.lishangbu.avalon.game.battle.engine.loader

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * 关注点：
 * - `JsonEffectDefinitionSchemaValidator` 是否能在加载前准确接受合法文档、拒绝非法文档。
 *
 * 这类测试是 loader 与 runtime 之前的第一道数据质量防线。
 */
class JsonEffectDefinitionSchemaValidatorTest {
    private val validator = JsonEffectDefinitionSchemaValidator()

    /**
     * 验证：
     * - 一个结构完整且字段合法的 effect 文档会通过 schema 校验。
     *
     * 这个测试是校验器的正向基线，确保当前规则不会把合法文档误判为非法。
     */
    @Test
    fun shouldAcceptValidFixtureDocumentWhenSchemaIsSatisfied() {
        val raw =
            """
            {
              "id": "recover",
              "kind": "move",
              "name": "自我再生",
              "hooks": {
                "on_hit": [
                  {
                    "then": [
                      {
                        "type": "heal",
                        "target": "self",
                        "mode": "max_hp_ratio",
                        "value": 0.5
                      }
                    ]
                  }
                ]
              }
            }
            """.trimIndent()

        assertDoesNotThrow { validator.validate(raw) }
    }

    /**
     * 验证：
     * - 缺少顶层必填字段 `id` 的文档会被拒绝。
     *
     * 这保证最基础的 effect 标识约束被守住，避免无 id 的文档进入 loader 和仓库。
     */
    @Test
    fun shouldRejectDocumentWhenIdIsMissing() {
        val raw =
            """
            {
              "kind": "move",
              "name": "坏数据",
              "hooks": {}
            }
            """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            validator.validate(raw)
        }
    }

    /**
     * 验证：
     * - 未注册的 action type 会在 schema 阶段被拒绝，而不是拖到运行时才爆炸。
     *
     * 这个测试保证动作白名单是生效的。
     */
    @Test
    fun shouldRejectDocumentWhenActionTypeIsUnsupported() {
        val raw =
            """
            {
              "id": "bad-action",
              "kind": "move",
              "name": "坏动作",
              "hooks": {
                "on_hit": [
                  {
                    "then": [
                      {
                        "type": "unknown_action"
                      }
                    ]
                  }
                ]
              }
            }
            """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            validator.validate(raw)
        }
    }

    /**
     * 验证：
     * - 未知的 effect kind 会被 schema 校验拒绝。
     *
     * 这保证顶层 kind 不会脱离当前 battle DSL 的统一 effect 模型。
     */
    @Test
    fun shouldRejectDocumentWhenEffectKindIsUnsupported() {
        val raw =
            """
            {
              "id": "bad-kind",
              "kind": "unknown_kind",
              "name": "坏类型",
              "hooks": {}
            }
            """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            validator.validate(raw)
        }
    }

    /**
     * 验证：
     * - hooks 中使用未支持的 hook 名称会被拒绝。
     *
     * 这能防止数据作者随意发明 hook 名称，导致 battle flow 根本不会消费这些规则。
     */
    @Test
    fun shouldRejectDocumentWhenHookNameIsUnsupported() {
        val raw =
            """
            {
              "id": "bad-hook",
              "kind": "move",
              "name": "坏 Hook",
              "hooks": {
                "on_bad_hook": []
              }
            }
            """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            validator.validate(raw)
        }
    }

    /**
     * 验证：
     * - condition 中非法 actor 会被拒绝。
     *
     * 这保证条件求值只会针对 battle runtime 认可的上下文对象执行。
     */
    @Test
    fun shouldRejectDocumentWhenActorIsUnsupported() {
        val raw =
            """
            {
              "id": "bad-actor",
              "kind": "move",
              "name": "坏 Actor",
              "hooks": {
                "on_hit": [
                  {
                    "if": {
                      "type": "has_status",
                      "actor": "unknown_actor",
                      "value": "brn"
                    },
                    "then": []
                  }
                ]
              }
            }
            """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            validator.validate(raw)
        }
    }

    /**
     * 验证：
     * - action 中非法 target selector 会被拒绝。
     *
     * 这保证 mutation apply 层只会接收到可解析的 selector。
     */
    @Test
    fun shouldRejectDocumentWhenTargetSelectorIsUnsupported() {
        val raw =
            """
            {
              "id": "bad-target",
              "kind": "move",
              "name": "坏 Target",
              "hooks": {
                "on_hit": [
                  {
                    "then": [
                      {
                        "type": "heal",
                        "target": "unknown_target",
                        "value": 0.5
                      }
                    ]
                  }
                ]
              }
            }
            """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            validator.validate(raw)
        }
    }

    /**
     * 验证：
     * - `trigger_event` 里引用未知 hook 时会被 schema 阶段拦住。
     *
     * 这样可以避免 battle flow 在运行时碰到不存在的 phase 名称。
     */
    @Test
    fun shouldRejectDocumentWhenTriggeredHookNameIsUnsupported() {
        val raw =
            """
            {
              "id": "bad-trigger",
              "kind": "move",
              "name": "坏 Trigger",
              "hooks": {
                "on_hit": [
                  {
                    "then": [
                      {
                        "type": "trigger_event",
                        "hookName": "on_unknown_hook"
                      }
                    ]
                  }
                ]
              }
            }
            """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            validator.validate(raw)
        }
    }
}
