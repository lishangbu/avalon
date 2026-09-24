/**
 * 装备穿戴和卸下通过库存领域接口执行，客户端不提交人物等级或物品归属。成功后刷新库存缓存；互斥只覆盖游戏写操作，真正部位唯一性和卸装顺序由后端事务保证。
 */
import {useQueryClient} from '@tanstack/vue-query'
import {api} from '@/shared/http'
import {useAction} from '@/shared/useAction'
import {inventoryKey} from './queries'

export function useEquipment() {
    const client = useQueryClient()
    const action = useAction(true)

    async function change(id: string, operation: 'equip' | 'unequip') {
        return action.run(async () => {
            await api(`/api/inventory/equipment/${id}/${operation}`, 'POST')
            // 装备同时改变资源上限预览，剩余生命魔力不由前端补满。
            await Promise.all([
                client.invalidateQueries({queryKey: inventoryKey}),
                client.invalidateQueries({queryKey: ['battle-lobby']}),
            ])
        })
    }

    return {...action, equip: (id: string) => change(id, 'equip'), unequip: (id: string) => change(id, 'unequip')}
}
