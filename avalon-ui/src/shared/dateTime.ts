/** 游戏日期统一按简体中文、东八区显示，不受玩家设备时区影响。 */
const formatter = new Intl.DateTimeFormat('zh-CN', {
    timeZone: 'Asia/Shanghai',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hourCycle: 'h23',
})

export function formatDateTime(value: string): string {
    return formatter.format(new Date(value))
}
