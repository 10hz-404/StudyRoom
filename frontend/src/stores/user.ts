import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const role = ref(localStorage.getItem('role') || '')
  const username = ref(localStorage.getItem('username') || '')

  async function login(studentNo: string, password: string) {
    const res: any = await api.post('/auth/login', { studentNo, password })
    token.value = 'Bearer ' + res.data
    const payload = JSON.parse(atob(res.data.split('.')[1]))
    role.value = payload.role
    username.value = studentNo
    localStorage.setItem('token', token.value)
    localStorage.setItem('role', role.value)
    localStorage.setItem('username', username.value)
    return role.value
  }

  function logout() {
    token.value = ''; role.value = ''; username.value = ''
    localStorage.clear()
  }

  return { token, role, username, login, logout }
})
