import assert from 'node:assert/strict'
import type {Battle, Order} from '../../src/features/battle/types'

/** 页面样本和接口训练使用相同的公开战局判断，保持选招及默认目标一致。 */
export function naturalOrders(battle: Battle): Order[] {
    const party = battle.units.filter((unit) => !unit.enemy && unit.hp > 0 && (unit.hero || unit.position < 2))
    const injured = [...party].sort((a, b) => a.hp / a.stats.health - b.hp / b.stats.health)[0]!
    return party.map((unit) => {
        const available = unit.actions.filter((action) => action.available)
        let action = battle.telegraph?.includes('即将')
            ? 'DEFEND'
            : (available.find((action) => action.mana > 0 && action.target === 'ENEMY')?.id ?? 'ATTACK')
        let target =
            action === 'DEFEND'
                ? unit.id
                : battle.units.find((enemy) => enemy.enemy && enemy.hp > 0 && !enemy.captured)?.id
        if (
            unit.hero &&
            injured.hp / injured.stats.health < 0.45 &&
            available.some((action) => action.id === 'HEALTH_POTION')
        ) {
            action = 'HEALTH_POTION'
            target = injured.id
        } else if (
            !unit.hero &&
            injured.hp / injured.stats.health < 0.55 &&
            available.some((action) => action.id === '微疗')
        ) {
            action = '微疗'
            target = injured.id
        }
        assert(target, `${unit.name}的${action}必须有合法目标`)
        return {actor: unit.id, action, target: target!}
    })
}
