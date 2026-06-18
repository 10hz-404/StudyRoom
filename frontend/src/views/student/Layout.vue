<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { computed } from 'vue'

const router = useRouter(); const route = useRoute(); const userStore = useUserStore()
const isActive = (path: string) => route.path === path

function logout() { userStore.logout(); router.push('/login') }
</script>
<template>
  <div class="app-layout">
    <aside class="sidebar">
      <div class="sidebar-brand" @click="router.push('/student')">📚 自习室预约</div>
      <nav class="sidebar-nav">
        <div :class="['nav-item', { active: isActive('/student') }]" @click="router.push('/student')">📊 首页</div>
        <div :class="['nav-item', { active: isActive('/student/rooms') }]" @click="router.push('/student/rooms')">🏢 浏览自习室</div>
        <div :class="['nav-item', { active: isActive('/student/reservations') }]" @click="router.push('/student/reservations')">📅 我的预约</div>
        <div :class="['nav-item', { active: isActive('/student/records') }]" @click="router.push('/student/records')">📋 考勤/违规</div>
      </nav>
      <div class="sidebar-footer">
        <div class="user-info">
          <div v-if="userStore.username" style="font-weight: 600; color: #333; margin-bottom: 4px;">{{ userStore.username }}</div>
          <span>👤 学生</span>
        </div>
        <el-button text size="small" @click="logout">退出登录</el-button>
      </div>
    </aside>
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>
<style scoped>
.app-layout { display:flex; min-height:100vh; background:#f5f7fa; }
.sidebar { width:220px; background:#fff; border-right:1px solid #e8ecf1; display:flex; flex-direction:column; padding:0; }
.sidebar-brand { padding:20px; font-size:16px; font-weight:700; color:#1a73e8; cursor:pointer; border-bottom:1px solid #f0f0f0; }
.sidebar-nav { flex:1; padding:8px 0; }
.nav-item { padding:12px 20px; font-size:14px; color:#555; cursor:pointer; transition:all .15s; border-left:3px solid transparent; }
.nav-item:hover { background:#f0f4ff; color:#1a73e8; }
.nav-item.active { background:#e8f0fe; color:#1a73e8; border-left-color:#1a73e8; font-weight:600; }
.sidebar-footer { padding:12px 20px; border-top:1px solid #f0f0f0; font-size:13px; }
.user-info { margin-bottom:4px; color:#666; }
.main-content { flex:1; padding:24px; overflow-y:auto; }
</style>
