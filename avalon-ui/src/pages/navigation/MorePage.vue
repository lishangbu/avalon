<script setup lang="ts">
/** 完整目录始终可达，设施不在这里提供跨地点操作。 */
import {IonItem, IonLabel, IonList, IonIcon} from '@ionic/vue'
import {optionsOutline, peopleOutline} from 'ionicons/icons'
import AppPage from '@/shared/ui/AppPage.vue'
import {navigationEntries} from '@/features/navigation/queries'
</script>
<template>
  <AppPage title="更多" tab="/more">
    <h2>角色与冒险</h2>
    <IonList>
      <IonItem router-link="/life" :detail="true"><IonLabel>生活技能</IonLabel></IonItem>
      <IonItem v-for="entry in navigationEntries" :key="entry.id" :router-link="entry.path" :detail="true">
        <IonIcon slot="start" :icon="entry.icon" aria-hidden="true" />
        <IonLabel>{{ entry.name }}</IonLabel>
      </IonItem>
    </IonList>
    <h2>社交</h2>
    <IonList>
      <IonItem router-link="/friends" :detail="true">
        <IonIcon slot="start" :icon="peopleOutline" aria-hidden="true" /><IonLabel>好友</IonLabel>
      </IonItem>
    </IonList>
    <h2>设置</h2>
    <IonList
      ><IonItem router-link="/more/navigation" :detail="true">
        <IonIcon slot="start" :icon="optionsOutline" aria-hidden="true" /><IonLabel>导航设置</IonLabel>
      </IonItem></IonList
    >
  </AppPage>
</template>
