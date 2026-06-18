<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../../api'
import { useRouter } from 'vue-router'
const router = useRouter()
const stats = ref({ rooms: 0, reservations: 0 })

onMounted(async () => {
  try { const r: any = await api.get('/rooms'); stats.value.rooms = r.data?.length || 0 } catch {}
  try { const r: any = await api.get('/reservations/my'); stats.value.reservations = (r.data||[]).filter((x:any)=>x.status==='ACTIVE'||x.status==='CHECKED_IN').length } catch {}
})
</script>
<template>
  <div>
    <h2 style="margin-bottom:20px;font-weight:600;color:#333;">学生首页</h2>
    <div class="stats-row">
      <div class="stat-card" @click="router.push('/student/rooms')"><div class="stat-num">{{ stats.rooms }}</div><div class="stat-label">可用自习室</div></div>
      <div class="stat-card" @click="router.push('/student/reservations')"><div class="stat-num">{{ stats.reservations }}</div><div class="stat-label">我的预约</div></div>
    </div>
    <el-card style="margin-top:20px;"><p style="color:#666;line-height:1.8;">欢迎使用校园自习室预约系统！快速开始：浏览自习室 → 选择座位 → 提交预约 → 到馆签到 → 离馆签退。</p></el-card>
  </div>
</template>
<style scoped>
.stats-row { display:flex; gap:16px; }
.stat-card { flex:1; background:#fff; border-radius:8px; padding:24px; text-align:center; border:1px solid #e8ecf1; cursor:pointer; transition:all .2s; }
.stat-card:hover { border-color:#1a73e8; box-shadow:0 4px 16px rgba(26,115,232,.08); }
.stat-num { font-size:36px; font-weight:700; color:#1a73e8; }
.stat-label { font-size:14px; color:#888; margin-top:4px; }
</style>
