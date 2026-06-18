<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../../api'

const logs: any = ref([])
const bizFilter = ref('')
onMounted(() => load())
async function load() {
  try {
    const url = bizFilter.value ? `/ai/logs/${bizFilter.value}` : '/ai/logs'
    const r: any = await api.get(url); logs.value = r.data || []
  } catch {}
}
function bizTag(type: string) { const m: any = { RECOMMEND: 'success', ANALYSIS: 'warning', BEHAVIOR: 'info' }; return m[type] || '' }
</script>
<template>
  <div>
    <div class="page-header"><h2>AI 分析日志</h2>
      <el-radio-group v-model="bizFilter" @change="load" size="small">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="RECOMMEND">推荐</el-radio-button>
        <el-radio-button value="ANALYSIS">分析</el-radio-button>
        <el-radio-button value="BEHAVIOR">行为</el-radio-button>
      </el-radio-group>
    </div>
    <el-table :data="logs" style="width:100%">
      <el-table-column prop="id" label="ID" width="60"/>
      <el-table-column prop="bizType" label="类型" width="90"><template #default="s"><el-tag :type="bizTag(s.row.bizType)" size="small">{{ s.row.bizType }}</el-tag></template></el-table-column>
      <el-table-column prop="analysisType" label="分析类型" width="160"/>
      <el-table-column prop="model" label="模型" width="120"/>
      <el-table-column prop="latencyMs" label="耗时" width="80"><template #default="s">{{ s.row.latencyMs }}ms</template></el-table-column>
      <el-table-column prop="createTime" label="时间" width="160"/>
    </el-table>
  </div>
</template>
<style scoped>
.page-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; }
.page-header h2 { font-weight:600; color:#333; margin:0; }
</style>
