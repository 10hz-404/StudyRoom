<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../../api'

const stats = ref({ rooms: 0, reservations: 0, violations: 0 })
onMounted(async () => {
  try { const r: any = await api.get('/rooms'); stats.value.rooms = r.data?.length || 0 } catch {}
  try { const v: any = await api.get('/violations'); stats.value.violations = (v.data||[]).filter((x:any)=>x.status==='PENDING').length || 0 } catch {}
  try { const rv: any = await api.get('/reservations/all'); stats.value.reservations = rv.data?.length || 0 } catch {}
})
</script>
<template>
  <div>
    <h2 style="margin-bottom:20px;font-weight:600;color:#333;">管理后台首页</h2>
    <div class="stats-row">
      <div class="stat-card"><div class="stat-num">{{ stats.rooms }}</div><div class="stat-label">自习室总数</div></div>
      <div class="stat-card"><div class="stat-num">{{ stats.reservations }}</div><div class="stat-label">全校预约数</div></div>
      <div class="stat-card warning"><div class="stat-num">{{ stats.violations }}</div><div class="stat-label">待处理违规</div></div>
    </div>
  </div>
</template>
<style scoped>
.stats-row { display:flex; gap:16px; }
.stat-card { flex:1; background:#fff; border-radius:8px; padding:24px; text-align:center; border:1px solid #e8ecf1; }
.stat-card.warning { border-color:#fbbf24; background:#fffbeb; }
.stat-num { font-size:36px; font-weight:700; color:#1a73e8; }
.stat-card.warning .stat-num { color:#d97706; }
.stat-label { font-size:14px; color:#888; margin-top:4px; }
</style>