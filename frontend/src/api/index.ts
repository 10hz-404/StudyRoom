import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({ baseURL: '/api/v1' })
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = token
  return config
})
api.interceptors.response.use(res => res.data, err => {
  ElMessage.error(err.response?.data?.message || '请求失败')
  return Promise.reject(err)
})
export default api
