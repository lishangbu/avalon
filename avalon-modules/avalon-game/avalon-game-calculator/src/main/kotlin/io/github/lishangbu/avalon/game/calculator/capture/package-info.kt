/**
 * 捕捉率计算包
 *
 * 提供面向战斗模块复用的纯数值捕捉率计算模型、球修正策略与默认实现。
 *
 * 当前实现采用项目固定的四摇判定规则：
 *
 * 1. 先根据球、状态和 HP 计算捕捉值 `a`
 * 2. 若 `a >= 255`，则直接判定为必定捕获
 * 3. 否则再推导单次摇晃阈值与整体成功概率
 *
 * 这一层只负责数学计算，不依赖 battle session、数据库、库存、玩家资产或其他业务模块。
 */
@file:org.jspecify.annotations.NullMarked
package io.github.lishangbu.avalon.game.calculator.capture
