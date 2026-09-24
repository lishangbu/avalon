import {nextTick, onBeforeUnmount, onMounted, type Ref, watch} from 'vue'
import {useRoute} from 'vue-router'
import {rememberedScroll, rememberScroll} from '@/app/pageState'
import {sessionGeneration} from '@/features/auth/session'

/**
 * Ionic 的滚动容器位于 Shadow DOM，不能用 window.scrollY 恢复列表。
 * 在内容尺寸变化后重试尚未到达的位置，覆盖异步列表加载；用户主动滚动会取消等待。
 */
export function useScrollMemory(
    content: Ref<
        | {
              $el: HTMLIonContentElement
          }
        | undefined
    >,
    key: () => string | undefined,
) {
    const route = useRoute(),
        path = route.path,
        generation = sessionGeneration()
    let scroll: HTMLElement | undefined, observer: ResizeObserver | undefined
    let pending: number | undefined
    let disposed = false
    const active = () => !disposed && route.path === path && generation === sessionGeneration()

    function restore() {
        if (!scroll || !active() || pending === undefined) return
        scroll.scrollTop = pending
        if (Math.abs(scroll.scrollTop - pending) < 1) pending = undefined
    }

    function save() {
        const id = key()
        if (id && scroll && active() && pending === undefined) rememberScroll(id, scroll.scrollTop)
    }

    function interrupt() {
        pending = undefined
    }

    watch(
        [key, () => route.path],
        async ([id]) => {
            if (!id || !active()) return
            pending = rememberedScroll(id)
            await nextTick()
            restore()
        },
        {flush: 'sync'},
    )
    onMounted(async () => {
        if (!key()) return
        scroll = await content.value?.$el.getScrollElement()
        if (disposed || !scroll) return
        scroll.addEventListener('scroll', save)
        scroll.addEventListener('touchstart', interrupt, {passive: true})
        scroll.addEventListener('wheel', interrupt, {passive: true})
        observer = new ResizeObserver(restore)
        observer.observe(scroll)
        const body = content.value?.$el.querySelector('.page-body')
        if (body) observer.observe(body)
        if (key()) pending = rememberedScroll(key()!)
        restore()
    })
    onBeforeUnmount(() => {
        save()
        disposed = true
        observer?.disconnect()
        scroll?.removeEventListener('scroll', save)
        scroll?.removeEventListener('touchstart', interrupt)
        scroll?.removeEventListener('wheel', interrupt)
    })
}
