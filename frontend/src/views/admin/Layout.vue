<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../../stores/user'

const router = useRouter(); const route = useRoute(); const userStore = useUserStore()

function logout() { userStore.logout(); router.push('/login') }
const isActive = (p: string) => route.path === p
</script>
<template>
  <div class="app-layout">
    <aside class="sidebar">
      <div class="sidebar-brand" @click="router.push('/admin')">⚙️ 管理后台</div>
      <nav class="sidebar-nav">
        <div :class="['nav-item', { active: isActive('/admin') }]" @click="router.push('/admin')">📊 首页</div>
        <div :class="['nav-item', { active: isActive('/admin/rooms') }]" @click="router.push('/admin/rooms')">🏢 自习室管理</div>
        <div :class="['nav-item', { active: isActive('/admin/reservations') }]" @click="router.push('/admin/reservations')">📅 预约管理</div>
        <div :class="['nav-item', { active: isActive('/admin/violations') }]" @click="router.push('/admin/violations')">⚠️ 违规管理</div>
        <div :class="['nav-item', { active: isActive('/admin/ai') }]" @click="router.push('/admin/ai')">🤖 AI 分析</div>
      </nav>
      <div class="sidebar-footer">
        <div v-if="userStore.username" style="font-weight: 600; color: #fff; margin-bottom: 4px;">{{ userStore.username }}</div>
        <div style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
          <span>👤 管理员</span>
          <el-button text size="small" @click="logout">退出</el-button>
        </div>
      </div>
    </aside>
    <main class="main-content"><router-view /></main>
  </div>
</template>
<style scoped>
.app-layout { display:flex; min-height:100vh; background:#f0f2f5; }
.sidebar { width:220px; background:#1e293b; color:#cbd5e1; display:flex; flex-direction:column; }
.sidebar-brand { padding:20px; font-size:16px; font-weight:700; color:#fff; cursor:pointer; border-bottom:1px solid #334155; }
.sidebar-nav { flex:1; padding:8px 0; }
.nav-item { padding:12px 20px; font-size:14px; cursor:pointer; transition:all .15s; border-left:3px solid transparent; }
.nav-item:hover { background:#334155; color:#fff; }
.nav-item.active { background:#1e3a5f; color:#60a5fa; border-left-color:#3b82f6; font-weight:600; }
.sidebar-footer { padding:12px 20px; border-top:1px solid #334155; font-size:13px; display:flex; flex-direction:column; gap:4px; }
.main-content { flex:1; padding:24px; overflow-y:auto; }
</style>
