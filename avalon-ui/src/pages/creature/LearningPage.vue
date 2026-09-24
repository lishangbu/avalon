<script lang="ts" setup>
/** 学习预览显示全部遗忘结果及概率，玩家选定书籍后才提交命令，不预先随机。 */
import {computed, ref} from 'vue'
import {useRoute} from 'vue-router'
import {IonButton} from '@ionic/vue'
import AppPage from '@/shared/ui/AppPage.vue'
import AppNotice from '@/shared/ui/AppNotice.vue'
import {useLearning, useLearningAction} from '@/features/creature/learning'
import {usePageActive} from '@/shared/usePageActive'

const id = String(useRoute().params.id),
  active = usePageActive()
const query = useLearning(computed(() => (active.value ? id : null))),
  preview = query.data
const action = useLearningAction(),
  book = ref('')
const selected = computed(() =>
  preview.value?.options.find((option) => option.books.some((item) => item.id === book.value)),
)

async function confirm() {
  if (preview.value && book.value && (await action.learn(preview.value, book.value))) book.value = ''
}
</script>
<template>
  <AppPage :back="'/companions/' + id" title="学习技能">
    <AppNotice v-if="action.error.value || query.error.value" tone="danger">
      {{ action.error.value || query.error.value?.message }}
    </AppNotice>
    <AppNotice v-if="action.result.value" tone="success"
      >学会：{{ action.result.value.learned }}。遗忘：{{
        action.result.value.forgotten.join('、') || '无'
      }}。当前技能：{{ action.result.value.remaining.join('、') }}。
    </AppNotice>
    <template v-if="preview"
      ><h2>当前技能</h2>
      <p>{{ preview.current.join('、') || '无' }}</p>
      <h2>遗忘概率</h2>
      <div v-for="(chance, count) in preview.probabilities" :key="count" class="row-title row">
        <span>{{ count === 0 ? '不遗忘' : `遗忘 ${count} 个旧技能` }}</span
        ><strong>{{ chance }}%</strong>
      </div>
      <label class="form-field" for="learning-book"
        >技能书<select id="learning-book" v-model="book" :disabled="action.busy.value">
          <option value="">选择技能书</option>
          <template v-for="option in preview.options" :key="option.skill">
            <option v-if="!option.books.length" disabled>
              {{ option.skill }} · {{ option.unavailable ?? '缺少技能书' }}
            </option>
            <option v-for="item in option.books" :key="item.id" :disabled="!!option.unavailable" :value="item.id">
              {{ item.name }} ×{{ item.quantity }} · {{ item.bound ? '已绑定' : '未绑定'
              }}{{ option.unavailable ? ' · ' + option.unavailable : '' }}
            </option>
          </template>
        </select></label
      >
      <AppNotice v-if="selected && !selected.unavailable" tone="warning"
        >消耗技能书 1 本，学会「{{ selected.skill }}」。{{
          preview.probabilities[0] === 100
            ? '本次不会遗忘旧技能。'
            : `可能随机失去：${preview.current.join('、')}。不能指定遗忘目标，技能数量也可能减少。`
        }}
      </AppNotice>
    </template>
    <template #footer>
      <IonButton :disabled="action.busy.value || !selected || !!selected.unavailable" @click="confirm"
        >确认学习
      </IonButton>
    </template>
  </AppPage>
</template>
