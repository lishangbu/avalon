import {ref, type Ref, watch} from 'vue'
import {authenticated} from '@/features/auth/session'

/**
 * 主入口使用替换导航，页面可能被卸载，因此分类记忆不能只放在组件内部。
 * 仅保存在本次登录的内存中；账号失效立即丢弃，不保存业务实体或操作草稿。
 */
const categories = new Map<string, Ref<string>>()
const scrollPositions = new Map<string, number>()
watch(
    authenticated,
    (signedIn) => {
        if (!signedIn) {
            categories.clear()
            scrollPositions.clear()
        }
    },
    {flush: 'sync'},
)

export function rememberedScroll(key: string) {
    return scrollPositions.get(key) ?? 0
}

export function rememberScroll(key: string, top: number) {
    if (authenticated.value) scrollPositions.set(key, top)
}

export function usePageCategory(page: string, initial: string) {
    let category = categories.get(page)
    if (!category) {
        category = ref(initial)
        categories.set(page, category)
    }
    return category
}
