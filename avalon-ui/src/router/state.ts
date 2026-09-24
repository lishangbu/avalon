/**
 * 导航错误由守卫写入，组件通过 readonly 引用展示；开始下一次准入检查时清除。
 */
import {readonly, ref} from 'vue'

const error = ref('')
export const navigationError = readonly(error)

/** 守卫开始准入检查时清除上一次导航错误。 */
export function beginNavigation() {
    error.value = ''
}

export function failNavigation(cause: unknown) {
    error.value = cause instanceof Error ? cause.message : '读取旅程失败，请重试'
}
