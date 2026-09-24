import {useQuery} from '@tanstack/vue-query'
import type {Ref} from 'vue'
import {api} from '@/shared/http'

/** 正式角色常驻属性由服务器计算；浏览器不重复成长、装备和训练技能规则。 */
export interface CharacterCombatProfile {
    stats: {
        health: number
        mana: number
        physicalAttack: number
        magicAttack: number
        physicalDefense: number
        magicDefense: number
        speed: number
    }
    skills: {name: string; element: string | null; mana: number; cooldown: number}[]
}

export function useCharacterCombatProfile(enabled: Ref<boolean>) {
    return useQuery({
        queryKey: ['character-combat-profile'],
        queryFn: () => api<CharacterCombatProfile>('/api/battle/character-profile'),
        enabled,
        retry: false,
    })
}
