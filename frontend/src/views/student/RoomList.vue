<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../../api'
import { useRouter } from 'vue-router'

const router = useRouter()
const rooms = ref<any[]>([])
const aiRecommendations = ref<any[]>([])
const aiLoading = ref(true)

onMounted(async () => {
  try { const r: any = await api.get('/rooms'); rooms.value = r.data || [] } catch {}
  
  // 异步 AI 推荐轮询机制（每500ms轮询一次，最多10秒）
  try {
    const taskRes: any = await api.post('/ai/recommend/task', {})
    const taskId = taskRes.data
    if (taskId) {
      let attempts = 0
      const maxAttempts = 20
      const interval = setInterval(async () => {
        attempts++
        try {
          const res: any = await api.get(`/ai/recommend/result/${taskId}`)
          if (res.data && res.data.status === 'SUCCESS') {
            aiRecommendations.value = res.data.data || []
            clearInterval(interval)
            aiLoading.value = false
          } else if ((res.data && res.data.status === 'FAILED') || attempts >= maxAttempts) {
            clearInterval(interval)
            fallbackSyncRecommend()
          }
        } catch {
          clearInterval(interval)
          fallbackSyncRecommend()
        }
      }, 500)
    } else {
      fallbackSyncRecommend()
    }
  } catch {
    fallbackSyncRecommend()
  }
})

// 同步降级接口保底
async function fallbackSyncRecommend() {
  try {
    const r: any = await api.post('/ai/recommend', {})
    aiRecommendations.value = r.data || []
  } catch {
    aiRecommendations.value = []
  } finally {
    aiLoading.value = false
  }
}
</script>
<template>
  <div>
    <!-- AI 加载骨架 -->
    <div v-if="aiLoading" class="ai-section">
      <h3>🤖 AI 智能推荐</h3>
      <div class="ai-cards">
        <div v-for="i in 3" :key="i" class="ai-card skeleton">
          <div class="skeleton-line" style="width:60%"></div>
          <div class="skeleton-line" style="width:80%"></div>
          <div class="skeleton-line" style="width:100%"></div>
        </div>
      </div>
    </div>
    <!-- AI 推荐结果 -->
    <div v-else-if="aiRecommendations.length>0" class="ai-section">
      <h3>🤖 AI 智能推荐</h3>
      <div class="ai-cards">
        <div v-for="rec in aiRecommendations" :key="rec.id||rec.room_id" class="ai-card" @click="router.push('/student/rooms/'+(rec.id||rec.room_id))">
          <div class="ai-score">⭐ {{ typeof rec.score==='number' ? (rec.score*100).toFixed(0) : rec.score }}%</div>
          <div class="ai-name">{{ rooms.find(r=>r.id===(rec.id||rec.room_id))?.roomName || '自习室' }}</div>
          <div class="ai-reason">{{ rec.reason }}</div>
        </div>
      </div>
    </div>

    <div class="page-header"><h2>浏览自习室</h2><span class="subtitle">共 {{ rooms.length }} 个自习室开放中</span></div>
    <div class="room-grid">
      <div v-for="room in rooms" :key="room.id" class="room-card" @click="router.push('/student/rooms/'+room.id)">
        <div class="room-icon">🏢</div>
        <div class="room-name">{{ room.roomName }}</div>
        <div class="room-location">📍 {{ room.location }}</div>
        <div class="room-status"><span class="status-dot active"></span> 开放中</div>
      </div>
    </div>
  </div>
</template>
<style scoped>
.page-header { display:flex; align-items:baseline; gap:12px; margin-bottom:20px; }
.page-header h2 { font-weight:600; color:#333; margin:0; }
.subtitle { font-size:13px; color:#999; }
.ai-section { margin-bottom:24px; }
.ai-section h3 { margin-bottom:12px; }
.ai-cards { display:flex; gap:12px; flex-wrap:wrap; }
.ai-card { flex:1; min-width:200px; background:linear-gradient(135deg,#e8f0fe,#f0fdf4); border:1px solid #bbf7d0; border-radius:8px; padding:16px; cursor:pointer; transition:.2s; }
.ai-card:hover { transform:translateY(-2px); box-shadow:0 4px 12px rgba(0,0,0,.08); }
.ai-score { font-size:20px; font-weight:700; color:#1a73e8; }
.ai-name { font-size:14px; font-weight:600; margin:4px 0; }
.ai-reason { font-size:12px; color:#666; }
.skeleton { background:#f5f5f5; cursor:default; border-color:#e8e8e8; }
.skeleton:hover { transform:none; box-shadow:none; }
.skeleton-line { height:12px; background:#e0e0e0; border-radius:4px; margin:8px 0; animation:pulse 1.5s infinite; }
@keyframes pulse { 0%,100%{opacity:1} 50%{opacity:.5} }
.room-grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(240px,1fr)); gap:16px; }
.room-card { background:#fff; border:1px solid #e8ecf1; border-radius:8px; padding:24px; cursor:pointer; transition:all .2s; }
.room-card:hover { border-color:#1a73e8; box-shadow:0 4px 16px rgba(26,115,232,.08); transform:translateY(-2px); }
.room-icon { font-size:32px; margin-bottom:12px; }
.room-name { font-size:16px; font-weight:600; color:#333; margin-bottom:4px; }
.room-location { font-size:13px; color:#888; margin-bottom:8px; }
.status-dot { display:inline-block; width:8px; height:8px; border-radius:50%; margin-right:4px; }
.status-dot.active { background:#52c41a; }
</style>