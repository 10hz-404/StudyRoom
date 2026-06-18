<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../../api'
import { ElMessage } from 'element-plus'

const violations = ref<any[]>([])
const aiDialog = ref(false)
const aiResult = ref<any>(null)
const aiLoading = ref(false)

onMounted(() => load())
async function load() { try { const r: any = await api.get('/violations'); violations.value = r.data || [] } catch {} }
async function detect() { try { await api.post('/violations/detect'); ElMessage.success('扫描完成'); load() } catch {} }

async function process(id: number, status: string) {
  try { await api.put(`/violations/${id}/process`, { status, processRemark: status === 'PROCESSED' ? '管理员已确认' : '已驳回' }); ElMessage.success('处理完成'); load() } catch {}
}

async function aiAnalyze(v: any) {
  aiDialog.value = true
  aiResult.value = null
  aiLoading.value = true
  try {
    const r: any = await api.post(`/ai/analyze?reservationId=` + v.reservationId, {
      reservationId: v.reservationId,
      status: v.violationType,
      violationType: v.violationType,
      userId: v.userId
    })
    aiResult.value = r.data
  } catch (e: any) {
    aiResult.value = { anomalyType: '错误', reason: e.response?.data?.message || '分析失败', severity: '-', suggestion: '请重试', manualReview: true }
  } finally { aiLoading.value = false }
}

function typeTag(type: string) { return type === 'NO_SHOW' ? 'danger' : 'warning' }
function typeText(type: string) { return type === 'NO_SHOW' ? '未签到' : '未签退' }
function statusTag(status: string) { const m: any = { PENDING: 'warning', PROCESSED: 'success', DISMISSED: 'info' }; return m[status] || 'info' }
function statusText(status: string) { const m: any = { PENDING: '待处理', PROCESSED: '已确认', DISMISSED: '已驳回' }; return m[status] || status }
</script>
<template>
  <div>
    <div class="page-header"><h2>违规管理</h2><el-button type="primary" @click="detect">🔍 手动扫描违规</el-button></div>
    <div v-if="violations.length===0" style="text-align:center;padding:60px;color:#999;">暂无违规记录</div>
    <div v-else class="vio-list">
      <div v-for="v in violations" :key="v.id" class="vio-card">
        <div class="vio-info">
          <div class="vio-type"><el-tag :type="typeTag(v.violationType)" size="small">{{ typeText(v.violationType) }}</el-tag></div>
          <div class="vio-meta">用户 #{{ v.userId }} | 预约 #{{ v.reservationId }} | {{ v.createTime }}</div>
        </div>
        <div class="vio-status"><el-tag :type="statusTag(v.status)" size="small">{{ statusText(v.status) }}</el-tag></div>
        <div class="vio-actions" v-if="v.status==='PENDING'">
          <el-button type="success" size="small" @click="process(v.id, 'PROCESSED')">确认</el-button>
          <el-button type="danger" size="small" plain @click="process(v.id, 'DISMISSED')">驳回</el-button>
          <el-button type="info" size="small" @click="aiAnalyze(v)">🤖 AI分析</el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="aiDialog" title="AI 智能分析" width="500px">
      <div v-if="aiLoading" style="text-align:center;padding:30px;">分析中...</div>
      <div v-else-if="aiResult">
        <p><b>异常类型：</b>{{ aiResult.anomalyType }}</p>
        <p><b>严重程度：</b>{{ aiResult.severity }}</p>
        <p><b>原因：</b>{{ aiResult.reason }}</p>
        <p><b>建议：</b>{{ aiResult.suggestion }}</p>
        <p><b>需人工复核：</b>{{ aiResult.manualReview ? '是' : '否' }}</p>
      </div>
    </el-dialog>
  </div>
</template>
<style scoped>
.page-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; }
.page-header h2 { font-weight:600; color:#333; margin:0; }
.vio-list { display:flex; flex-direction:column; gap:12px; }
.vio-card { display:flex; align-items:center; background:#fff; border:1px solid #e8ecf1; border-radius:8px; padding:16px 20px; gap:16px; }
.vio-card:hover { border-color:#d0d5dd; }
.vio-info { flex:1; }
.vio-meta { font-size:12px; color:#999; margin-top:4px; }
.vio-actions { display:flex; gap:8px; }
</style>