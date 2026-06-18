<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../../api'

const reservations = ref<any[]>([])
onMounted(async () => {
  try { const r: any = await api.get('/reservations/all'); reservations.value = (r.data||[]).sort((a:any,b:any)=> (b.createTime||'').localeCompare(a.createTime||'')) } catch {}
})

function statusText(s: string) { const m: any = { ACTIVE: '待签到', CHECKED_IN: '已签到', COMPLETED: '已完成', CANCELLED: '已取消', NO_SHOW: '未签到违规' }; return m[s] || s }
function statusTag(s: string) { const m: any = { ACTIVE: 'warning', CHECKED_IN: '', COMPLETED: 'success', CANCELLED: 'info', NO_SHOW: 'danger' }; return m[s] || '' }
function fmtTime(t: string) { if (!t) return '-'; return t.length>5 ? t.substring(0,16) : t }
function fmtDate(d: string) { return d ? d.substring(0,10) : '-' }
</script>
<template>
  <div>
    <div class="page-header"><h2>全校预约记录</h2></div>
    <el-table :data="reservations">
      <el-table-column prop="id" label="ID" width="60"/>
      <el-table-column prop="userId" label="用户" width="80"/>
      <el-table-column prop="seatId" label="座位" width="80"/>
      <el-table-column label="日期" width="100"><template #default="s">{{ fmtDate(s.row.reserveDate) }}</template></el-table-column>
      <el-table-column label="时间" width="140"><template #default="s">{{ fmtTime(s.row.startTime) }} - {{ fmtTime(s.row.endTime) }}</template></el-table-column>
      <el-table-column label="状态" width="100"><template #default="s"><el-tag :type="statusTag(s.row.status)" size="small">{{ statusText(s.row.status) }}</el-tag></template></el-table-column>
      <el-table-column label="创建时间" width="150"><template #default="s">{{ fmtTime(s.row.createTime) }}</template></el-table-column>
    </el-table>
  </div>
</template>
<style scoped>
.page-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; }
.page-header h2 { font-weight:600; margin:0; }
</style>