import {expect, test} from '@playwright/test'
import {npcQuestMarker} from '../src/features/world/npc'
import type {QuestAction} from '../src/features/quest/types'

test('人物标记按当地稳定身份聚合且优先交付', () => {
    const base: QuestAction = {
        id: 'A',
        quest: 'A',
        title: '委托',
        location: 'SQUARE',
        npc: '同名人物',
        npcId: 'ONE',
        label: '交付',
        interactable: false,
        accepted: false,
        acceptAvailable: true,
    }
    expect(npcQuestMarker([base], 'ONE', 'SQUARE')?.symbol).toBe('!')
    const progress = {...base, id: 'B', accepted: true}
    expect(npcQuestMarker([progress], 'ONE', 'SQUARE')?.ready).toBe(false)
    const ready = {...progress, id: 'C', interactable: true}
    expect(npcQuestMarker([base, progress, ready], 'ONE', 'SQUARE')?.label).toBe('可交付任务')
    expect(npcQuestMarker([ready], 'TWO', 'SQUARE')).toBeUndefined()
    expect(npcQuestMarker([ready], 'ONE', 'OTHER')).toBeUndefined()
})
