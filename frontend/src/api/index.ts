import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({ baseURL: '/api/v1' })
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = token
  return config
})
api.interceptors.response.use(res => res.data, err => {
  if (err.response?.status === 401) {
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    localStorage.removeItem('username')
    ElMessage.error(err.response?.data?.message || '登录过期，请重新登录')
    import('../router').then(m => {
      m.default.push('/login')
    })
    return Promise.reject(err)
  }
  ElMessage.error(err.response?.data?.message || '请求失败')
  return Promise.reject(err)
})
export default api
