<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const studentNo = ref('S2024001')
const password = ref('123456')

async function handleLogin() {
  try {
    const role = await userStore.login(studentNo.value, password.value)
    ElMessage.success('登录成功')
    router.push(role === 'ADMIN' ? '/admin' : '/student')
  } catch { /* interceptor handles */ }
}
</script>
<template>
  <div class="login-page bg-gradient-to-br from-blue-600 to-purple-700 flex items-center justify-center min-h-screen">
    <div class="login-panel flex rounded-xl overflow-hidden shadow-2xl bg-white">
      <div class="login-brand">
        <div class="login-icon">📚</div>
        <h1>校园自习室预约系统</h1>
        <p>高效预约 · 智能推荐 · 便捷管理</p>
      </div>
      <div class="login-form">
        <h2>登录</h2>
        <el-form @submit.prevent="handleLogin" label-position="top">
          <el-form-item label="学号 / 工号">
            <el-input v-model="studentNo" placeholder="请输入学号" size="large" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="password" type="password" placeholder="请输入密码" show-password size="large" />
          </el-form-item>
          <el-button type="primary" size="large" @click="handleLogin" class="login-btn w-full mt-2 bg-blue-600 hover:bg-blue-700 transition-colors">登 录</el-button>
        </el-form>
        <div class="login-hint">测试账号：学生 S2024001 / 管理员 A001 · 密码 123456</div>
      </div>
    </div>
  </div>
</template>
<style scoped>
.login-page { display:flex; justify-content:center; align-items:center; height:100vh;overflow:hidden; background:linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.login-panel { display:flex; width:800px; background:#fff; border-radius:12px; overflow:hidden; box-shadow:0 20px 60px rgba(0,0,0,.15); }
.login-brand { flex:1; background:linear-gradient(135deg, #1a73e8, #0d47a1); color:#fff; padding:48px 40px; display:flex; flex-direction:column; justify-content:center; align-items:center; text-align:center; }
.login-brand h1 { font-size:22px; margin:12px 0 8px; font-weight:600; }
.login-brand p { opacity:.85; font-size:13px; }
.login-icon { font-size:48px; }
.login-form { flex:1; padding:48px 40px; display:flex; flex-direction:column; justify-content:center; }
.login-form h2 { margin-bottom:24px; font-size:20px; color:#333; }
.login-btn { width:100%; margin-top:8px; }
.login-hint { margin-top:16px; font-size:11px; color:#999; text-align:center; }
</style>`r`n<style>body { margin:0 !important; overflow:hidden !important; }</style>
