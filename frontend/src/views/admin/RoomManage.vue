<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const rooms = ref<any[]>([])
const dlgVisible = ref(false)
const seatDlgVisible = ref(false)
const form = ref({ id: null as number|null, roomName: '', location: '' })
const isEdit = ref(false)
const currentRoom = ref<any>(null)
const seats = ref<any[]>([])
const newSeatNo = ref('')

onMounted(() => load())
async function load() { try { const r: any = await api.get('/rooms'); rooms.value = r.data || [] } catch {} }

function openAdd() { isEdit.value = false; form.value = { id: null, roomName: '', location: '' }; dlgVisible.value = true }
function openEdit(r: any) { isEdit.value = true; form.value = { id: r.id, roomName: r.roomName, location: r.location }; dlgVisible.value = true }

async function submit() {
  try {
    if (isEdit.value) { await api.put('/rooms', { id: form.value.id, roomName: form.value.roomName, location: form.value.location, status: 'ACTIVE' }) }
    else { const r: any = await api.post('/rooms', { roomName: form.value.roomName, location: form.value.location, status: 'ACTIVE' }); form.value.id = r.data.id; isEdit.value = true }
    ElMessage.success(isEdit.value ? '已更新' : '已添加'); dlgVisible.value = false; load()
  } catch {}
}

async function openSeatManage(r: any) {
  currentRoom.value = r; newSeatNo.value = ''
  try { const s: any = await api.get('/seats/room/'+r.id); seats.value = s.data || [] } catch { seats.value = [] }
  seatDlgVisible.value = true
}

async function addSeat() {
  if (!newSeatNo.value.trim()) { ElMessage.warning('请输入座位编号'); return }
  try { await api.post('/seats', { roomId: currentRoom.value.id, seatNo: newSeatNo.value.trim(), status: 'AVAILABLE' }); ElMessage.success('座位已添加'); newSeatNo.value = ''; await refreshSeats() } catch {}
}

function removeSeat(seatId: number) {
  ElMessageBox.confirm('确定要删除该座位吗？删除后该座位未签到的预约将被取消，已签到的预约将自动变更为已完成。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await api.delete('/seats/' + seatId)
      ElMessage.success('座位已成功删除')
      await refreshSeats()
    } catch {}
  }).catch(() => {})
}

async function refreshSeats() {
  try { const s: any = await api.get('/seats/room/'+currentRoom.value.id); seats.value = s.data || [] } catch {}
}

function removeRoom(roomId: number) {
  ElMessageBox.confirm('确定要删除该自习室吗？删除后该自习室及下属座位将被彻底删除，未签到的预约将被取消，已签到的预约将变更为已完成。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await api.delete('/rooms/' + roomId)
      ElMessage.success('自习室已成功删除')
      load()
    } catch {}
  }).catch(() => {})
}
</script>
<template>
  <div>
    <div class="page-header"><h2>自习室管理</h2><el-button type="primary" @click="openAdd">新增自习室</el-button></div>
    <el-table :data="rooms">
      <el-table-column prop="id" label="ID" width="60"/>
      <el-table-column prop="roomName" label="名称"/>
      <el-table-column prop="location" label="位置"/>
      <el-table-column prop="status" label="状态" width="100"><template #default="s">{{ {ACTIVE:'启用',DISABLED:'停用'}[s.row.status]||s.row.status }}</template></el-table-column>
      <el-table-column label="操作" width="220"><template #default="s">
        <el-button size="small" @click="openEdit(s.row)">编辑</el-button>
        <el-button size="small" type="success" @click="openSeatManage(s.row)">座位管理</el-button>
        <el-button v-if="s.row.status==='ACTIVE'" size="small" type="danger" @click="removeRoom(s.row.id)">删除</el-button>
      </template></el-table-column>
    </el-table>

    <el-dialog v-model="dlgVisible" :title="isEdit?'编辑自习室':'新增自习室'" width="400px">
      <el-form :model="form" label-position="top">
        <el-form-item label="名称"><el-input v-model="form.roomName"/></el-form-item>
        <el-form-item label="位置"><el-input v-model="form.location"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="dlgVisible=false">取消</el-button><el-button type="primary" @click="submit">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="seatDlgVisible" :title="'座位管理 — '+(currentRoom?.roomName||'')" width="500px">
      <div style="margin-bottom:12px">
        <el-input v-model="newSeatNo" placeholder="座位编号（如A-006）" style="width:160px;margin-right:8px"/>
        <el-button type="primary" @click="addSeat">添加座位</el-button>
      </div>
      <el-table :data="seats" max-height="300">
        <el-table-column prop="seatNo" label="编号"/>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="s">{{ {AVAILABLE:'可用',OCCUPIED:'已占用',MAINTENANCE:'维护'}[s.row.status]||s.row.status }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80"><template #default="s">
          <el-button v-if="s.row.status==='AVAILABLE'" size="small" type="danger" @click="removeSeat(s.row.id)">删除</el-button>
        </template></el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>
<style scoped>
.page-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; }
.page-header h2 { font-weight:600; margin:0; }
</style>