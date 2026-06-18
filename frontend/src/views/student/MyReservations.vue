<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import api from '../../api'
import { ElMessage } from 'element-plus'

const reservations = ref<any[]>([])
const rooms = ref<any[]>([])
const seats = ref<any[]>([])
const seatMap = ref<Record<number,string>>({})
const seatRoomMap = ref<Record<number,number>>({})
const dlgVisible = ref(false)
function getInitialTimes() {
  const now = new Date()
  const currentHour = now.getHours()
  const currentMinute = now.getMinutes()
  const startHour = currentMinute < 30 ? currentHour : currentHour + 1
  const endHour = startHour + 1
  const startStr = (startHour % 24).toString().padStart(2, '0') + ':00'
  const endStr = (endHour % 24).toString().padStart(2, '0') + ':00'
  return { startTime: startStr, endTime: endStr }
}
const { startTime: initStart, endTime: initEnd } = getInitialTimes()
const form = ref({ roomId: null as number|null, seatId: null as number|null, reserveDate: new Date().toISOString().split('T')[0], startTime: initStart, endTime: initEnd })
const loading = ref(false)

watch(() => form.value.startTime, (newVal) => {
  if (newVal) {
    const [h, m] = newVal.split(':').map(Number)
    form.value.endTime = `${((h + 1) % 24).toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}`
  }
})

onMounted(async () => {
  await loadRooms()
  await loadSeatMap()
  await loadReservations()
})

async function loadReservations() { try { const r: any = await api.get('/reservations/my'); reservations.value = r.data || [] } catch {} }
async function loadRooms() { try { const r: any = await api.get('/rooms'); rooms.value = r.data || [] } catch {} }
async function loadSeatMap() {
  for (const r of rooms.value) {
    try { const s: any = await api.get('/seats/room/'+r.id); (s.data||[]).forEach((x:any)=>{ seatMap.value[x.id]=x.seatNo; seatRoomMap.value[x.id]=r.id }) } catch {}
  }
}

function getRoomName(seatId: number) { const rid = seatRoomMap.value[seatId]; if (!rid) return ''; const room = rooms.value.find(x=>x.id===rid); return room ? room.roomName : '' }
function getSeatLabel(id: number) { return seatMap.value[id] || '#'+id }

async function onRoomChange() {
  form.value.seatId = null
  if (!form.value.roomId) { seats.value = []; return }
  try { const r: any = await api.get('/seats/room/'+form.value.roomId+'/available?date='+form.value.reserveDate+'&startTime='+form.value.startTime+':00&endTime='+form.value.endTime+':00'); seats.value = r.data || [] } catch { seats.value = [] }
}

watch(() => form.value.roomId, () => onRoomChange())
watch([() => form.value.reserveDate, () => form.value.startTime, () => form.value.endTime], () => { if (form.value.roomId) onRoomChange() })

async function submit() {
  if (!form.value.seatId) { ElMessage.warning('请选择一个座位'); return }
  loading.value = true
  try {
    const r: any = await api.post('/reservations', { seatId: form.value.seatId, reserveDate: form.value.reserveDate, startTime: form.value.startTime+':00', endTime: form.value.endTime+':00' })
    if (r.code === 200) { ElMessage.success('预约成功！'); dlgVisible.value = false; loadReservations() }
    else { ElMessage.error(r.message || '预约失败') }
  } catch (e: any) { /* 错误已由 api 拦截器全局提示 */ }
  finally { loading.value = false }
}

async function cancel(id: number) { try { await api.put('/reservations/'+id+'/cancel'); loadReservations() } catch {} }
async function checkIn(id: number) { try { await api.post('/attendance/check-in/'+id); loadReservations() } catch {} }
async function checkOut(id: number) { try { await api.post('/attendance/check-out/'+id); loadReservations() } catch {} }

function statusTag(s: string) { const m: any = { ACTIVE: 'warning', CHECKED_IN: '', COMPLETED: 'success', CANCELLED: 'info', NO_SHOW: 'danger' }; return m[s] || '' }
function statusText(s: string) { const m: any = { ACTIVE: '待签到', CHECKED_IN: '使用中', COMPLETED: '已完成', CANCELLED: '已取消', NO_SHOW: '未签到' }; return m[s] || s }
function seatClass(s: any) { if (s.status==='MAINTENANCE') return 'mini-seat-maintenance'; return s.available ? 'mini-seat-available' : 'mini-seat-occupied' }
const pastHours = () => {
  if (form.value.reserveDate !== new Date().toISOString().split('T')[0]) return []
  const now = new Date()
  const curHour = now.getHours()
  const curMin = now.getMinutes()
  const maxDisabledHour = curMin < 30 ? curHour - 1 : curHour
  return Array.from({ length: maxDisabledHour + 1 }, (_, i) => i)
}
</script>
<template>
  <div>
    <div class="page-header"><h2>我的预约</h2><el-button type="primary" @click="dlgVisible=true">新建预约</el-button></div>
    <div v-if="reservations.length===0" class="empty-state">暂无预约记录</div>
    <div v-else class="resv-list">
      <div v-for="r in reservations" :key="r.id" class="resv-card">
        <div class="resv-info">
          <div class="resv-date">{{ r.reserveDate }} {{ r.startTime }}-{{ r.endTime }}</div>
          <div class="resv-seat">{{ getRoomName(r.seatId) }} {{ getSeatLabel(r.seatId) }}</div>
        </div>
        <div class="resv-status"><el-tag :type="statusTag(r.status)" size="small">{{ statusText(r.status) }}</el-tag></div>
        <div class="resv-actions">
          <el-button v-if="r.status==='ACTIVE'" type="success" size="small" @click="checkIn(r.id)">签到</el-button>
          <el-button v-if="r.status==='CHECKED_IN'" type="warning" size="small" @click="checkOut(r.id)">签退</el-button>
          <el-button v-if="r.status==='ACTIVE'" type="danger" size="small" plain @click="cancel(r.id)">取消</el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="dlgVisible" title="新建预约" width="800px">
      <el-row :gutter="16" style="margin-bottom:16px">
        <el-col :span="8"><el-select v-model="form.roomId" placeholder="选择自习室" style="width:100%"><el-option v-for="r in rooms" :key="r.id" :label="r.roomName" :value="r.id"/></el-select></el-col>
        <el-col :span="5"><el-date-picker v-model="form.reserveDate" type="date" value-format="YYYY-MM-DD" style="width:100%" :disabled-date="(d:Date)=>{const today=new Date();today.setHours(0,0,0,0);const max=new Date(today);max.setDate(today.getDate()+8);return d<today||d>max}"/></el-col>
        <el-col :span="5"><el-time-picker v-model="form.startTime" format="HH:mm" value-format="HH:mm" arrow-control :disabled-hours="pastHours" style="width:100%"/></el-col>
        <el-col :span="6"><el-time-picker v-model="form.endTime" format="HH:mm" value-format="HH:mm" arrow-control style="width:100%"/></el-col>
      </el-row>
      <div v-if="form.roomId && seats.length>0" class="mini-seat-grid">
        <div v-for="s in seats" :key="s.id" :class="['mini-seat-item', seatClass(s), { selected: form.seatId===s.id }]" @click="form.seatId=s.id">
          <div class="mini-seat-no">{{ s.seatNo }}</div>
          <div style="font-size:10px">{{ s.available ? '可选' : '已约' }}</div>
        </div>
      </div>
      <div v-else-if="form.roomId" style="padding:20px;text-align:center;color:#999">该时段没有可选座位</div>
      <template #footer><el-button @click="dlgVisible=false">取消</el-button><el-button type="primary" :loading="loading" @click="submit">确认预约</el-button></template>
    </el-dialog>
  </div>
</template>
<style scoped>
.page-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; }
.page-header h2 { font-weight:600; margin:0; }
.empty-state { text-align:center; padding:60px 0; color:#999; }
.resv-list { display:flex; flex-direction:column; gap:12px; }
.resv-card { display:flex; align-items:center; background:#fff; border:1px solid #e8ecf1; border-radius:8px; padding:14px 20px; gap:16px; }
.resv-info { flex:1; }
.resv-date { font-size:15px; font-weight:600; }
.resv-seat { font-size:12px; color:#999; margin-top:2px; }
.resv-actions { display:flex; gap:8px; }
.mini-seat-grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(70px,1fr)); gap:8px; }
.mini-seat-item { border-radius:6px; padding:10px 8px; text-align:center; cursor:pointer; border:2px solid transparent; transition:.15s; }
.mini-seat-item:hover { transform:translateY(-1px); }
.mini-seat-item.selected { border-color:#1a73e8; }
.mini-seat-available { background:#f0fdf4; border-color:#bbf7d0; color:#166534; }
.mini-seat-occupied { background:#fef2f2; border-color:#fecaca; color:#991b1b; cursor:not-allowed; }
.mini-seat-maintenance { background:#f3f4f6; border-color:#d1d5db; color:#6b7280; cursor:not-allowed; }
.mini-seat-no { font-size:14px; font-weight:700; }
</style>