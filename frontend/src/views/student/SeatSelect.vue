<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../../api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const roomId = Number(route.params.roomId)
const seats = ref<any[]>([])
const room = ref<any>(null)
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

const date = ref(new Date().toISOString().split('T')[0])
const startTime = ref(initStart)
const endTime = ref(initEnd)
const selectedSeat = ref<any>(null)
const booking = ref(false)

watch(startTime, (newVal) => {
  if (newVal) {
    const [h, m] = newVal.split(':').map(Number)
    endTime.value = `${((h + 1) % 24).toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}`
  }
})

onMounted(async () => {
  try { const r: any = await api.get('/rooms'); room.value = (r.data||[]).find((x:any)=>x.id===roomId) } catch {}
  loadSeats()
})

async function loadSeats() {
  try {
    const r: any = await api.get(`/seats/room/${roomId}/available?date=${date.value}&startTime=${startTime.value}:00&endTime=${endTime.value}:00`)
    seats.value = r.data || []
  } catch { seats.value = [] }
}

watch([date, startTime, endTime], () => loadSeats())

async function book() {
  if (!selectedSeat.value || !selectedSeat.value.available) return
  booking.value = true
  try {
    await api.post('/reservations', {
      seatId: selectedSeat.value.id,
      reserveDate: date.value,
      startTime: startTime.value + ':00',
      endTime: endTime.value + ':00'
    })
    ElMessage.success('预约成功！')
    router.push('/student/reservations')
  } catch (e: any) {
    /* 错误已由 api 拦截器全局提示 */
  } finally { booking.value = false }
}

const disableDate = (d: Date) => { const today = new Date(); today.setHours(0,0,0,0); const maxDay = new Date(today); maxDay.setDate(today.getDate()+8); return d < today || d > maxDay }

const pastHours = () => {
  if (date.value !== new Date().toISOString().split('T')[0]) return []
  const now = new Date()
  const curHour = now.getHours()
  const curMin = now.getMinutes()
  const maxDisabledHour = curMin < 30 ? curHour - 1 : curHour
  return Array.from({ length: maxDisabledHour + 1 }, (_, i) => i)
}
function seatClass(seat: any) {
  if (seat.status === 'MAINTENANCE') return 'seat-maintenance'
  return seat.available ? 'seat-available' : 'seat-occupied'
}
</script>
<template>
  <div>
    <div class="header">
      <el-button @click="$router.back()" text>← 返回</el-button>
      <h2>{{ room?.roomName || '座位选择' }}</h2>
      <span style="color:#999">{{ room?.location }}</span>
    </div>

    <div class="time-bar">
      <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" :disabled-date="disableDate" style="width:160px"/>
      <el-time-picker v-model="startTime" format="HH:mm" value-format="HH:mm" placeholder="开始" arrow-control :disabled-hours="pastHours" style="width:140px"/>
      <span>—</span>
      <el-time-picker v-model="endTime" format="HH:mm" value-format="HH:mm" placeholder="结束" arrow-control style="width:140px"/>
      <el-tag type="success" size="small">绿色: 可选</el-tag>
      <el-tag type="danger" size="small">红色: 已预约</el-tag>
      <el-tag type="info" size="small">灰色: 维护中</el-tag>
    </div>

    <div class="seat-grid">
      <div v-for="seat in seats" :key="seat.id"
        :class="['seat-item', seatClass(seat), { selected: selectedSeat?.id === seat.id }]"
        @click="selectedSeat = seat">
        <div class="seat-no">{{ seat.seatNo }}</div>
        <div class="seat-label">{{ seat.available ? '可选' : seat.status === 'MAINTENANCE' ? '维护' : '已约' }}</div>
      </div>
    </div>

    <div v-if="selectedSeat" class="book-bar">
      <span>已选: <b>{{ selectedSeat.seatNo }}</b></span>
      <span>{{ date }} {{ startTime }}:00 - {{ endTime }}:00</span>
      <el-button type="primary" :disabled="!selectedSeat.available" :loading="booking" @click="book">
        {{ selectedSeat.available ? '立即预约' : '该座位不可预约' }}
      </el-button>
    </div>
  </div>
</template>
<style scoped>
.header { display:flex; align-items:center; gap:12px; margin-bottom:16px; }
.header h2 { margin:0; font-weight:600; }
.time-bar { display:flex; align-items:center; gap:10px; margin-bottom:24px; flex-wrap:wrap; }
.seat-grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(100px,1fr)); gap:12px; margin-bottom:24px; }
.seat-item { border-radius:8px; padding:20px 12px; text-align:center; cursor:pointer; transition:all .2s; border:2px solid transparent; }
.seat-item:hover { transform:translateY(-2px); box-shadow:0 4px 12px rgba(0,0,0,.1); }
.seat-item.selected { border-color:#1a73e8; box-shadow:0 0 0 2px rgba(26,115,232,.3); }
.seat-available { background:#f0fdf4; border-color:#bbf7d0; color:#166534; }
.seat-occupied { background:#fef2f2; border-color:#fecaca; color:#991b1b; cursor:not-allowed; }
.seat-maintenance { background:#f3f4f6; border-color:#d1d5db; color:#6b7280; cursor:not-allowed; }
.seat-no { font-size:18px; font-weight:700; }
.seat-label { font-size:12px; margin-top:4px; }
.book-bar { display:flex; align-items:center; gap:16px; background:#fff; border:1px solid #e8ecf1; border-radius:8px; padding:16px 20px; position:sticky; bottom:0; }
</style>