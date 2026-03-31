package io.github.lishangbu.avalon.game.calculator.capture

/**
 * 捕捉率计算所需的额外上下文。
 *
 * 这些字段全部是纯数值/纯标志输入，调用方可以在 battle 层或 service 层准备好后再传进来，
 * 计算器本身不关心这些值是来自数据库、快照还是前端请求。
 */
data class CaptureContext(
    /** 目标物种是否已经被玩家捕获过，用于 `repeat-ball`。 */
    val alreadyCaught: Boolean = false,
    /** 是否为钓鱼遭遇，用于 `dive-ball`。 */
    val isFishingEncounter: Boolean = false,
    /** 是否为冲浪遭遇，用于 `dive-ball`。 */
    val isSurfEncounter: Boolean = false,
    /** 是否为夜晚环境，用于 `dusk-ball`。 */
    val isNight: Boolean = false,
    /** 是否为洞窟环境，用于 `dusk-ball`。 */
    val isCave: Boolean = false,
    /** 目标是否被视为究极异兽，用于 `beast-ball`。 */
    val isUltraBeast: Boolean = false,
    /** 目标等级，用于 `nest-ball`。 */
    val targetLevel: Int? = null,
    /**
     * 目标体重。
     *
     * 当前按项目内部既有约定直接使用数值，不在计算器里做单位换算。
     * 后续 battle 模块只需要保证传入的单位与球策略一致即可。
     */
    val targetWeight: Int? = null,
    /** 目标属性集合，用于 `net-ball`。 */
    val targetTypes: Set<String> = emptySet(),
)

/**
 * 球策略解析结果。
 *
 * 某些球是倍率修正，某些球是平坦捕获率修正，例如 `heavy-ball`。
 * `master-ball` 这类直接成功的球则通过 [directSuccess] 表达。
 */
data class BallResolution(
    /** 球本身是否绕过后续公式，直接成功。 */
    val directSuccess: Boolean = false,
    /** 球的倍率修正。 */
    val multiplier: Double = 1.0,
    /** 球提供的平坦捕获率修正。 */
    val flatCaptureRateBonus: Int = 0,
    /** 解析备注，方便调用方记录来源。 */
    val note: String? = null,
)

/**
 * 捕捉率计算输入。
 *
 * 公式结构参考常见主系列“四摇”计算思路：
 *
 * `a = effectiveCaptureRate * hpFactor * ballMultiplier * statusMultiplier`
 *
 * 其中：
 *
 * - `effectiveCaptureRate = max(1, captureRate + flatBonus)`
 * - `hpFactor = ((3 * maxHp) - (2 * currentHp)) / (3 * maxHp)`
 */
data class CaptureRateInput(
    /** 当前 HP。允许为 0，便于上层在特殊规则下自行决定是否传入濒死目标。 */
    val currentHp: Int,
    /** 最大 HP。必须大于 0。 */
    val maxHp: Int,
    /** 基础捕获率。 */
    val captureRate: Int,
    /**
     * 状态内部名称。
     *
     * 当前默认支持：
     *
     * - 睡眠/冰冻：`slp`、`sleep`、`frz`、`freeze`
     * - 麻痹/灼伤/中毒：`par`、`paralysis`、`brn`、`burn`、`psn`、`poison`、`tox`
     */
    val statusId: String? = null,
    /** 球的内部名称，例如 `poke-ball`、`great-ball`。 */
    val ballItemInternalName: String,
    /** 当前回合数，从 1 开始。 */
    val turn: Int = 1,
    /** 球策略所需的额外环境上下文。 */
    val captureContext: CaptureContext = CaptureContext(),
)

/**
 * 捕捉率计算输出。
 *
 * 这里同时返回“中间值”和“最终概率”，目的是后续 battle 模块既可以：
 *
 * - 直接展示理论捕捉率
 * - 也可以复用 `shakeCheckThreshold` 做实际随机摇晃判定
 */
data class CaptureRateResult(
    /** 是否因为球本身规则而直接成功，例如 `master-ball`。 */
    val directSuccess: Boolean,
    /** 是否已经达到必定捕获条件。 */
    val guaranteedSuccess: Boolean,
    /** 平坦修正后的有效捕获率。 */
    val effectiveCaptureRate: Int,
    /** 球的倍率修正。 */
    val ballMultiplier: Double,
    /** 球的平坦捕获率修正。 */
    val flatCaptureRateBonus: Int,
    /** 状态修正倍率。 */
    val statusMultiplier: Double,
    /** HP 因子。HP 越低，该值越大。 */
    val hpFactor: Double,
    /** 公式中常见的捕捉值 `a`。 */
    val captureValue: Double,
    /**
     * 按 `a / 255` 归一化后的百分比。
     *
     * 这是对中间值 `a` 的线性归一化，不等于实际四摇成功率；
     * 之所以保留，是为了方便对齐旧公式文档和调试日志。
     */
    val normalizedCaptureValueRate: Double,
    /**
     * 单次摇晃判定阈值。
     *
     * 若结果为 `null`，表示当前输入已经绕过摇晃判定直接成功。
     */
    val shakeCheckThreshold: Double?,
    /**
     * 单次摇晃成功概率，取值范围为 `0.0 .. 1.0`。
     *
     * 当前按离散整数随机源精确计算，而不是简单做连续值除法：
     * battle 中的 shake roll 是 `0..65535` 的整数。
     */
    val singleShakeSuccessProbability: Double,
    /**
     * 四摇全部通过的整体成功概率，取值范围为 `0.0 .. 1.0`。
     */
    val overallCaptureSuccessProbability: Double,
    /**
     * 整体成功率百分比，等于 [overallCaptureSuccessProbability] * 100。
     */
    val overallCaptureSuccessRate: Double,
    /** 结果备注，便于调用方记录判定来源。 */
    val note: String,
)
